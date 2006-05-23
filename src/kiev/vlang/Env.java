package kiev.vlang;

import kiev.Kiev;
import kiev.parser.Parser;
import kiev.parser.ParseException;
import kiev.parser.ParseError;
import kiev.transf.*;
import kiev.vlang.types.*;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.*;

import kiev.be.java15.Bytecoder;
import kiev.be.java15.Attr;
import kiev.be.java15.JStruct;
import kiev.be.java15.JEnv;

import static kiev.vlang.ProjectFileType.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public enum ProjectFileType {
	CLASS,
	INTERFACE,
	ENUM,
	SYNTAX,
	PACKAGE,
	METATYPE
}
@node
public final class ProjectFile extends ASTNode {

	@virtual typedef This  = ProjectFile;
	@virtual typedef VView = ProjectFileView;

	public ProjectFileType		type;
	public String				qname;
	public KString				bname;
	public File					file;
	public boolean				bad;

	@nodeview
	public static final view ProjectFileView of ProjectFile extends NodeView {
		public ProjectFileType		type;
		public String				qname;
		public KString				bname;
		public File					file;
		public boolean				bad;
	}

	public ProjectFile() {}
	
    public Dumper toJava(Dumper dmp) { return dmp; }

}


/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

@node
public class Env extends Struct {

	/** Hashtable of all defined and loaded classes */
	public static Hash<String>				classHashOfFails	= new Hash<String>();

	/** Hashtable for project file (class name + file name) */
	public static Hashtable<String,ProjectFile>	projectHash = new Hashtable<String,ProjectFile>();

	/** Root of package hierarchy */
	public static Env			root = new Env();

	/** Compiler properties */
	public static Properties	props = System.getProperties();
	
	/** Backend environment */
	public static JEnv			jenv;

	/** Private class constructor -
		really there may be no instances of this class
	 */
	private Env() {
		super();
		root = this;
		setPackage();
		setResolved(true);
		new CompaundMetaType(this);
	}

	public static void setProperty(String prop, String value) {
		props.put(prop,value);
	}

	public static void removeProperty(String prop) {
		props.remove(prop);
	}

	public static String getProperty(String prop) {
		return props.getProperty(prop);
	}
	
	public String toString() {
		return "<root>";
	}

	public static DNode resolveStruct(String qname) {
		Struct pkg = Env.root;
		int start = 0;
		int end = qname.indexOf('.', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			Struct ss = null;
			foreach (Struct s; pkg.sub_decls; s.id.equals(nm)) {
				ss = s;
				break;
			}
			if (ss == null)
				return null;
			pkg = ss;
			start = end+1;
			end = qname.indexOf('.', start);
		}
		String nm = qname.substring(start).intern();
		foreach (DNode dn; pkg.sub_decls; dn.id.equals(nm))
			return dn;
		return null;
	}
	
	public static Struct newStruct(String sname, Struct outer, int acces) {
		return newStruct(sname,true,outer,acces,false);
	}

	public static Struct newStruct(String sname, boolean direct, Struct outer, int acces, boolean cleanup)
	{
		assert(outer != null);
		Struct bcl = null;
		if (direct && sname != null) {
			foreach (Struct s; outer.sub_decls; s.id.equals(sname)) {
				bcl = s;
				break;
			}
		}
		if( bcl != null ) {
			Struct cl = (Struct)bcl;
			if( cleanup ) {
				cl.type_decl_version = 0;
				cl.flags = acces;
				cl.package_clazz = outer;
				cl.typeinfo_clazz = null;
				cl.view_of = null;
				cl.super_types.delAll();
				cl.args.delAll();
				cl.sub_decls.delAll();
				foreach(Method m; cl.members; m.isOperatorMethod() )
					Operator.cleanupMethod(m);
				cl.members.delAll();
			}
			outer.addSubStruct((Struct)cl);
			return cl;
		}
		Symbol name;
		if (direct) {
			name = new Symbol(sname);
		}
		else if (sname != null) {
			// Construct name of local class
			String uniq_name = outer.countAnonymouseInnerStructs()+"$"+sname;
			name = new Symbol(sname, uniq_name);
		}
		else {
			// Local anonymouse class
			String uniq_name = String.valueOf(outer.countAnonymouseInnerStructs());
			name = new Symbol(uniq_name, uniq_name);
		}
		Struct cl = new Struct(name,outer,acces);
		outer.addSubStruct(cl);
		return cl;
	}

	public static Struct newPackage(String qname) {
		if (qname == "")
			return Env.root;
		int end = qname.lastIndexOf('.');
		if (end < 0)
			return newPackage(qname,Env.root);
		else
			return newPackage(qname.substring(end+1).intern(),newPackage(qname.substring(0,end).intern()));
	}

	public static Struct newPackage(String sname, Struct outer) {
		Struct cl = null;
		foreach (Struct s; outer.sub_decls; s.id.equals(sname)) {
			cl = s;
			break;
		}
		if (cl == null)
			cl = newStruct(sname,outer,0);
		cl.setPackage();
		cl.setResolved(true);
		return cl;
	}

	public static TypeDecl newMetaType(Symbol id, Struct pkg, boolean cleanup) {
		if (pkg == null)
			pkg = Env.root;
		assert (pkg.isPackage());
		TypeDecl tdecl = null;
		foreach (TypeDecl pmt; pkg.sub_decls; pmt.id.equals(id)) {
			tdecl = pmt;
			break;
		}
		if (tdecl == null) {
			tdecl = new TypeDecl();
			tdecl.id = id;
			tdecl.package_clazz = pkg;
			tdecl.flags = ACC_MACRO;
			tdecl.type_decl_version = 1;
			tdecl.xmeta_type = new MetaType(tdecl);
			tdecl.xtype = tdecl.xmeta_type.make(TVarBld.emptySet);
			pkg.sub_decls.add(tdecl);
		}
		else if( cleanup ) {
			tdecl.type_decl_version++;
			tdecl.flags = ACC_MACRO;
			tdecl.package_clazz = pkg;
			tdecl.super_types.delAll();
			//tdecl.args.delAll();
			tdecl.members.delAll();
		}

		return tdecl;
	}

	/** Default environment initialization */
	public static  void InitializeEnv() {
	    InitializeEnv(System.getProperty("java.class.path"));
	}

	/** Environment initialization with specified CLASSPATH
		for the compiling classes
	 */
	public static void InitializeEnv(String path) {
		jenv = new JEnv(path);
		if( Kiev.project_file != null && Kiev.project_file.exists() ) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(Kiev.project_file));
				while(in.ready()) {
					String line = in.readLine();
					if( line==null ) continue;
					String[] args = line.trim().split("\\s+");
					if (args.length == 0)
						continue;
					int idx = 0;
					ProjectFile pf = new ProjectFile();
					pf.type = ProjectFileType.fromString(args[idx++]);
					switch (pf.type) {
					case CLASS:
					case INTERFACE:
					case ENUM:
					case SYNTAX:
					case PACKAGE:
						pf.qname = args[idx++].intern();
						pf.bname = KString.from(args[idx++]);
						pf.file = new File(args[idx++]);
						break;
					case METATYPE:
						pf.qname = args[idx++].intern();
						pf.file = new File(args[idx++]);
						break;
					}
					for (int i=idx; i < args.length; i++) {
						if (args[i].equals("bad"))
							pf.bad = true;
					}
					projectHash.put(pf.qname, pf);
				}
				in.close();
			} catch (EOFException e) {
				// OK
			} catch (IOException e) {
				Kiev.reportWarning("Error while project file reading: "+e);
			}
		}

		root.setPackage();
	}

	public static void dumpProjectFile() {
		if( Kiev.project_file == null ) return;
		try {
			PrintStream out = new PrintStream(new FileOutputStream(Kiev.project_file));
			Vector<String> strs = new Vector<String>();
			for(Enumeration<String> e=projectHash.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				ProjectFile pf = projectHash.get(key);
				DNode cl = resolveStruct(pf.qname);
				if (cl != null && cl.isBad()) pf.bad = true;
				if (cl instanceof Struct) {
					if      (cl.isSyntax())		pf.type = SYNTAX;
					else if (cl.isPackage())	pf.type = PACKAGE;
					else if (cl.isInterface())	pf.type = INTERFACE;
					else if (cl.isEnum())		pf.type = ENUM;
					else						pf.type = CLASS;
					strs.append(pf.type+" "+pf.qname+" "+pf.bname+" "+pf.file+(pf.bad?" bad":""));
				} else {
					pf.type = METATYPE;
					strs.append(pf.type+" "+pf.qname+" "+pf.file+(pf.bad?" bad":""));
				}
			}
			String[] sarr = (String[])strs;
			sortStrings(sarr);
			foreach(String s; sarr) out.println(s);
		} catch (IOException e) {
			Kiev.reportWarning("Error while project file writing: "+e);
		}
	}

	public static void createProjectInfo(TypeDecl tdecl, String f) {
		String qname = tdecl.qname();
		if (qname == null || qname == "")
			return;
		ProjectFile pf = projectHash.get(qname);
		if( pf == null ) {
			ProjectFile pf = new ProjectFile();
			pf.qname = qname;
			pf.file = new File(f);
			projectHash.put(qname,pf);
		}
		else {
			if( !pf.file.getName().equals(f) )
				pf.file = new File(f);
		}
		setProjectInfo(tdecl, false);
	}

	public static void setProjectInfo(TypeDecl tdecl, boolean good) {
		ProjectFile pf = projectHash.get(tdecl.qname());
		if (pf != null) {
			if (tdecl instanceof Struct)
				pf.bname = ((JStruct)(Struct)tdecl).bname();
			pf.bad = !good;
			if (tdecl instanceof Struct) {
				if      (tdecl.isSyntax())		pf.type = SYNTAX;
				else if (tdecl.isPackage())		pf.type = PACKAGE;
				else if (tdecl.isInterface())	pf.type = INTERFACE;
				else if (tdecl.isEnum())		pf.type = ENUM;
				else							pf.type = CLASS;
			} else {
				pf.type = METATYPE;
			}
		}
	}

	public static void sortStrings(String[] a) {
		String aux[] = (String[])a.clone();
		mergeSortStrings(aux, a, 0, a.length);
	}

	private static void mergeSortStrings(String src[], String dest[], int low, int high) {
		int length = high - low;

		// Insertion sort on smallest arrays
		if (length < 7) {
			for (int i=low; i<high; i++)
			for (int j=i; j > low && dest[j-1].compareTo(dest[j]) > 0 ; j--) {
//				swapStrings(dest, j, j-1);
				String tmp = dest[j];
				dest[j] = dest[j-1];
				dest[j-1] = tmp;
			}
			return;
		}

		// Recursively sort halves of dest into src
		int mid = (low + high)/2;
		mergeSortStrings(dest, src, low, mid);
		mergeSortStrings(dest, src, mid, high);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if( src[mid-1].compareTo(src[mid]) <= 0 ) {
			System.arraycopy(src, low, dest, low, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = low, p = low, q = mid; i < high; i++) {
			if (q>=high || p<mid && src[p].compareTo(src[q])<=0 )
				dest[i] = src[p++];
			else
				dest[i] = src[q++];
		}
	}

	public static boolean existsStruct(String qname) {
		if (qname == "") return true;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return false;
		Struct cl = resolveStruct(qname);
		if (cl != null)
			return true;
		// Check if not loaded
		return jenv.existsClazz(qname);
	}

	public static Struct loadStruct(String qname, boolean fatal) {
		Struct s = loadStruct(qname);
		if (fatal && s == null)
			throw new RuntimeException("Cannot find class "+qname);
		return s;
	}

	public static Struct loadStruct(String qname) {
		if (qname == "") return Env.root;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return null;
		Struct cl = resolveStruct(qname);
		// Load if not loaded or not resolved
		if (cl == null)
			cl = jenv.loadClazz(qname);
		else if (!cl.isResolved() && !cl.isAnonymouse())
			cl = jenv.loadClazz(cl);
		if (cl == null)
			classHashOfFails.put(qname);
		return cl;
	}

	public static Struct loadStruct(Struct cl) {
		if (cl == Env.root) return Env.root;
		// Load if not loaded or not resolved
		if (!cl.isResolved() && !cl.isAnonymouse())
			jenv.loadClazz(cl);
		return cl;
	}

	public static Dumper toJavaModifiers( Dumper dmp, short mods ) {
		if( (mods & ACC_PUBLIC		) > 0 ) dmp.append("public ");
		if( (mods & ACC_PRIVATE		) > 0 ) dmp.append("private ");
		if( (mods & ACC_PROTECTED	) > 0 ) dmp.append("protected ");

		if( (mods & ACC_FINAL		) > 0 ) dmp.append("final ");
		if( (mods & ACC_STATIC		) > 0 ) dmp.append("static ");
		if( (mods & ACC_ABSTRACT	) > 0 ) dmp.append("abstract ");
		if( (mods & ACC_NATIVE		) > 0 ) dmp.append("native ");

		if( (mods & ACC_SYNCHRONIZED) > 0 ) dmp.append("synchronized ");
		if( (mods & ACC_VOLATILE	) > 0 ) dmp.append("volatile ");
		if( (mods & ACC_TRANSIENT	) > 0 ) dmp.append("transient ");

		return dmp;
	}
}

