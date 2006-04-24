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
import kiev.be.java15.JEnv;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class ProjectFile extends ASTNode {

	@virtual typedef This  = ProjectFile;
	@virtual typedef VView = ProjectFileView;

	public ClazzName	name;
	public File			file;
	public boolean		bad = true;

	@nodeview
	public static final view ProjectFileView of ProjectFile extends NodeView {
		public ClazzName	name;
		public File			file;
		public boolean		bad;
	}

	public ProjectFile() {}
	
	public ProjectFile(ClazzName clname, File f) {
		name = clname;
		file = f;
	}

	public ProjectFile(ClazzName clname, String f) {
		this(clname,new File(f));
	}

	public ProjectFile(ClazzName clname, KString f) {
		this(clname, new File( f.toString() ));
	}

    public Dumper toJava(Dumper dmp) { return dmp; }

}


/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

@node
public class Env extends Struct {

	/** Hashtable of all defined and loaded classes */
	private static Hash<KString>				classHashOfFails	= new Hash<KString>();

	/** Hashtable for project file (class name + file name) */
	public static Hashtable<KString,ProjectFile>	projectHash = new Hashtable<KString,ProjectFile>();

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
		/*this.imeta_type =*/ new CompaundTypeProvider(this);
		this.super_bound = new TypeRef();
	}

	public Object copy() {
		throw new CompilerException(this,"Env node cannot be copied");
	};

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

	public static Struct resolveStruct(KString qname) {
		Struct pkg = Env.root;
		int start = 0;
		int end = qname.indexOf('.', start);
		while (end > 0) {
			KString nm = qname.substr(start, end);
			Struct ss = null;
			foreach (Struct s; pkg.sub_clazz; s.short_name.name == nm) {
				ss = s;
				break;
			}
			if (ss == null)
				return null;
			pkg = ss;
			start = end+1;
			end = qname.indexOf('.', start);
		}
		KString nm = qname.substr(start);
		foreach (Struct s; pkg.sub_clazz; s.short_name.name == nm)
			return s;
		return null;
	}
	
	public static Struct newStruct(KString sname, Struct outer, int acces) {
		return newStruct(sname,true,outer,acces,false);
	}

	public static Struct newStruct(KString sname, boolean direct, Struct outer, int acces, boolean cleanup) {
		Struct bcl = null;
		if (direct && sname != null) {
			foreach (Struct s; outer.sub_clazz; s.short_name.name == sname) {
				bcl = s;
				break;
			}
		}
		if( bcl != null ) {
			Struct cl = (Struct)bcl;
			if( cleanup ) {
				cl.flags = acces;
				cl.package_clazz = outer;
				cl.typeinfo_clazz = null;
				cl.view_of = null;
				cl.super_bound = new TypeRef();
				cl.interfaces.delAll();
				cl.args.delAll();
				cl.sub_clazz.delAll();
				foreach(Method m; cl.members; m.isOperatorMethod() )
					Operator.cleanupMethod(m);
				cl.members.delAll();
			}
			outer.addSubStruct((Struct)cl);
			return cl;
		}
		ClazzName name;
		if (direct) {
			name = ClazzName.fromOuterAndName(outer,sname,!outer.isPackage());
		}
		else if (sname != null) {
			// Construct name of local class
			KString bytecode_name =
				KString.from(outer.name.bytecode_name
					+"$"+outer.countAnonymouseInnerStructs()
					+"$"+sname);
			KString fixname = bytecode_name.replace('/','.');
			name = new ClazzName(fixname,sname,bytecode_name,false);
		}
		else {
			// Local anonymouse class
			KString bytecode_name =
				KString.from(outer.name.bytecode_name
					+"$"+outer.countAnonymouseInnerStructs());
			name = ClazzName.fromBytecodeName(bytecode_name);
		}
		Struct cl = new Struct(name,outer,acces);
		if( outer == null ) {
			if( name.name.equals(name.short_name) )
				outer = root;
			else
				outer = getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
		}
		if( outer != null )
			outer.addSubStruct((Struct)cl);
		return cl;
	}

	public static Struct newPackage(KString qname) {
		if (qname == KString.Empty)
			return Env.root;
		int end = qname.lastIndexOf('.');
		if (end < 0)
			return newPackage(qname,Env.root);
		else
			return newPackage(qname.substr(end+1),newPackage(qname.substr(0,end)));
	}

	public static Struct newPackage(KString sname, Struct outer) {
		Struct cl = null;
		foreach (Struct s; outer.sub_clazz; s.short_name.name == sname) {
			cl = s;
			break;
		}
		if (cl == null)
			cl = newStruct(sname,outer,0);
		cl.setPackage();
		cl.setResolved(true);
		return cl;
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
					StringTokenizer st = new StringTokenizer(line);
					if( !st.hasMoreTokens() ) continue;
					String class_name = st.nextToken();
					String class_bytecode_name = st.nextToken();
					String class_source_name = st.nextToken();
					String bad = null;
					if( st.hasMoreTokens() )
						bad = st.nextToken();
					String class_short_name;
					int idx = class_name.lastIndexOf('.');
					if (idx < 0) class_short_name = class_name;
					else class_short_name = class_name.substring(idx+1);
					ClazzName cn = new ClazzName(
						KString.from(class_name),
						KString.from(class_short_name),
						KString.from(class_bytecode_name),
						idx>0 && class_bytecode_name.charAt(idx)=='$'
						);
					ProjectFile value = new ProjectFile(cn,class_source_name);
					if( bad != null && bad.equals("bad") )
						value.bad = true;
					else
						value.bad = false;
					projectHash.put(KString.from(class_name), value);
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
			for(Enumeration<KString> e=projectHash.keys(); e.hasMoreElements();) {
				KString key = e.nextElement();
				ProjectFile value = projectHash.get(key);
				Struct cl = resolveStruct(value.name.name);
				if( cl != null && cl.isBad() ) value.bad = true;
				if !(cl instanceof Struct)
					continue;
				strs.append(value.name.name+" "+value.name.bytecode_name+" "+value.file+(value.bad?" bad":""));
			}
			String[] sarr = (String[])strs;
			sortStrings(sarr);
			foreach(String s; sarr) out.println(s);
		} catch (IOException e) {
			Kiev.reportWarning("Error while project file writing: "+e);
		}
	}

	public static void setProjectInfo(ClazzName name, KString f) {
		setProjectInfo(name,f.toString());
	}
	public static void setProjectInfo(ClazzName name, String f) {
		ProjectFile pf = projectHash.get(name.name);
		if( pf == null )
			projectHash.put(name.name,new ProjectFile(name,f));
		else {
			pf.bad = true;
			if( !pf.file.getName().equals(f) )
				pf.file = new File(f);
		}
	}

	public static void setProjectInfo(ClazzName name, boolean good) {
		ProjectFile pf = projectHash.get(name.name);
		if( pf != null ) pf.bad = !good;
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

	public static boolean existsStruct(KString name) throws RuntimeException {
		if( name.equals(KString.Empty) ) return true;
		// Check class is already loaded
		if( classHashOfFails.get(name) != null ) return false;
		Struct cl = resolveStruct(name);
		if (cl != null)
			return true;
		// Check if not loaded
		ClazzName clname = ClazzName.fromToplevelName(name);
		return jenv.existsClazz(clname);
	}

	public static Struct getStruct(KString name) throws RuntimeException {
		if( name.equals(KString.Empty) ) return Env.root;
		// Check class is already loaded
		if( classHashOfFails.get(name) != null ) return null;
		Struct cl = resolveStruct(name);
		// Load if not loaded or not resolved
		if( cl == null ) {
			ClazzName clname = ClazzName.fromToplevelName(name);
			cl = jenv.loadClazz(clname);
		}
		else if( !cl.isResolved() && !cl.isAnonymouse() ) {
			cl = jenv.loadClazz(cl.name);
		}
		if( cl == null ) {
			classHashOfFails.put(name);
//			throw new RuntimeException("Class "+name+" not found");
		}
		return (Struct)cl;
	}

	public static Struct getStruct(ClazzName name) throws RuntimeException {
		if( name.name.equals(KString.Empty) ) return Env.root;
		// Check class is already loaded
		if( classHashOfFails.get(name.name) != null ) return null;
		Struct cl = resolveStruct(name.name);
		// Load if not loaded or not resolved
		if( cl == null ) {
			cl = jenv.loadClazz(name);
		}
		else if( !cl.isResolved() && !cl.isAnonymouse() ) {
			cl = jenv.loadClazz(name);
		}
		if( cl == null ) {
//			cl = loadClazz(name);
			classHashOfFails.put(name.name);
//			throw new RuntimeException("Class "+name+" not found");
		}
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

