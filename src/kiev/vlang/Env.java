package kiev.vlang;

import kiev.Kiev;
import kiev.parser.Parser;
import kiev.parser.ParseException;
import kiev.parser.ParseError;
import kiev.transf.*;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.*;

import kiev.be.java.Bytecoder;
import kiev.be.java.Attr;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class ProjectFile extends ASTNode {
	public ClazzName	name;
	public File			file;
	public boolean		bad = true;

	public ProjectFile(ClazzName clname, File f) {
		super(new NodeImpl());
		name = clname;
		file = f;
	}

	public ProjectFile(ClazzName clname, String f) {
		this(clname,new File(f));
	}

	public ProjectFile(ClazzName clname, KString f) {
		this(clname, new File( f.toString() ));
	}

	public Object copy() {
		throw new CompilerException(this,"ProjectFile node cannot be copied");
	};

    public Dumper toJava(Dumper dmp) { return dmp; }

}


/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

public class Env extends Struct {

	/** Hashtable of all defined and loaded classes */
	public static Hashtable<KString,Struct>	classHash			= new Hashtable<KString,Struct>();
	public static Hash<KString>						classHashOfFails	= new Hash<KString>();
	public static Hashtable<KString,Struct>	classHashDbg 		= new Hashtable<KString,Struct>();

	/** Hashtable for project file (class name + file name) */
	public static Hashtable<KString,ProjectFile>	projectHash = new Hashtable<KString,ProjectFile>();

	/** Root of package hierarchy */
	public static Env			root = new Env();

	/** StandardClassLoader */
	public static kiev.bytecode.StandardClassLoader		stdClassLoader;

	/** Compiler properties */
	public static Properties	props = System.getProperties();

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

	public static Struct newStruct(ClazzName name, boolean cleanup) {
		KString package_name = name.package_bytecode_name();
    	if( package_name.equals(KString.Empty) )
	    	return newStruct(name,Env.root,0,cleanup);
		else
			return newStruct(name,newStruct(ClazzName.fromBytecodeName(package_name,false)),0,cleanup);
	}

	public static Struct newStruct(ClazzName name) {
		return newStruct(name,false);
    }

	public static Struct newStruct(ClazzName name,Struct outer,int access) {
		return newStruct(name,outer,access,false);
	}

	public static Struct newStruct(ClazzName name,Struct outer,int access, boolean cleanup) {
		Struct bcl = classHash.get(name.name);
		if( bcl != null ) {
			if !(bcl instanceof Struct)
				throw new CompilerException("Cannot create struct "+name);
			Struct cl = (Struct)bcl;
			if( cleanup ) {
				cl.flags = access;
				cl.package_clazz = outer;
				cl.typeinfo_clazz = null;
				cl.view_of = null;
				cl.super_bound = new TypeRef();
				cl.interfaces.delAll();
				cl.args.delAll();
				cl.sub_clazz.delAll();
				foreach(ASTNode n; cl.members; n instanceof Method && ((Method)n).isOperatorMethod() )
					Operator.cleanupMethod((Method)n);
				cl.members.delAll();
				cl.imported.delAll();
			}
			outer.addSubStruct((Struct)cl);
			return cl;
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated bytecode name "+name.bytecode_name+" of "+name.name);
		Struct cl = new Struct(name,outer,access);
		classHash.put(cl.name.name,cl);
		classHashDbg.put(cl.name.bytecode_name,cl);
		if( outer == null ) {
			if( name.name.equals(name.short_name) )
				outer = root;
			else
				outer = getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name(),false));
		}
		if( outer != null )
			outer.addSubStruct((Struct)cl);
		return cl;
	}

	public static Struct newInterface(ClazzName name,Struct outer,int access) {
		Struct cl = newStruct(name,outer,access);
		cl.setInterface(true);
		return cl;
	}

	public static Struct newPackage(KString name) {
		if( name.equals(KString.Empty) )
			return Env.root;
		Struct bcl = classHash.get(name);
		if( bcl != null ) {
			if !(bcl instanceof Struct)
				throw new CompilerException("Cannot create struct "+name);
			bcl.setPackage();
			bcl.setResolved(true);
			return (Struct)bcl;
		}
		return newPackage(ClazzName.fromToplevelName(name,false));
	}

	public static Struct newPackage(ClazzName name) {
		if( name.equals(ClazzName.Empty)) return Env.root;
		Struct bcl = classHash.get(name.name);
		if( bcl != null ) {
			if !(bcl instanceof Struct)
				throw new CompilerException("Cannot create struct "+name);
			bcl.setPackage();
			bcl.setResolved(true);
			return (Struct)bcl;
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated package name "+name.bytecode_name+" of "+name.name);
		return newPackage(name,newPackage(ClazzName.fromToplevelName(name.package_name(),false)));
	}

	public static Struct newPackage(ClazzName name,Struct outer) {
		Struct cl = newStruct(name,outer,0);
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
		if( path == null ) path = System.getProperty("java.class.path");
		stdClassLoader = new kiev.bytecode.StandardClassLoader(path);
//		stdClassLoader.addHandler(new kiev.bytecode.KievAttributeHandler());

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
						false,
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
				Struct cl = classHash.get(value.name.name);
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
		Struct cl = classHash.get(name);
		if (cl != null)
			return true;
		// Check if not loaded
		ClazzName clname = ClazzName.fromToplevelName(name,false);
		return existsClazz(clname);
	}

	public static Struct getStruct(KString name) throws RuntimeException {
		if( name.equals(KString.Empty) ) return Env.root;
		// Check class is already loaded
		if( classHashOfFails.get(name) != null ) return null;
		Struct cl = classHash.get(name);
		// Load if not loaded or not resolved
		if( cl == null ) {
			ClazzName clname = ClazzName.fromToplevelName(name,false);
			cl = loadClazz(clname);
		}
		else if( !cl.isResolved() && !cl.isAnonymouse() ) {
			cl = loadClazz(cl.name);
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
		Struct cl = classHash.get(name.name);
		// Load if not loaded or not resolved
		if( cl == null ) {
			cl = loadClazz(name);
		}
		else if( !cl.isResolved() && !cl.isAnonymouse() ) {
			cl = loadClazz(name);
		}
		if( cl == null ) {
//			cl = loadClazz(name);
			classHashOfFails.put(name.name);
//			throw new RuntimeException("Class "+name+" not found");
		}
		return (Struct)cl;
	}

	public static boolean existsClazz(ClazzName name) {
		if( stdClassLoader==null ) InitializeEnv();
		return stdClassLoader.existsClazz(name.bytecode_name.toString());
	}

	/** Actually load class from specified file and dir */
	public static Struct loadClazz(ClazzName name) throws RuntimeException {
		return loadClazz(name,false);
	}

	/** Actually load class from specified file and dir */
	public static Struct loadClazz(ClazzName name, boolean force) throws RuntimeException {
		long curr_time = 0L, diff_time = 0L;
		diff_time = curr_time = System.currentTimeMillis();
		if( stdClassLoader==null ) InitializeEnv();
		kiev.bytecode.Clazz clazz = stdClassLoader.loadClazz(name.bytecode_name.toString());
		if( clazz != null ) {
			Struct cl = (Struct)classHash.get(name.name);
			if( cl == null || !cl.isResolved() || cl.package_clazz==null ) {
				// Ensure the parent package/outer class is loaded
				Struct pkg = getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name(),false));
				if( pkg == null ) {
					pkg = getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name(),false));
					if( pkg == null )
						pkg = newPackage(ClazzName.fromBytecodeName(name.package_bytecode_name(),false));
				}
				if( !pkg.isResolved() ) {
					pkg = getStruct(pkg.name);
					//pkg = loadClazz(pkg.name);
				}
				if( cl == null ) {
					cl = newStruct(name,false);
					new FileUnit(KString.from(name.short_name+".class"), pkg).members.add(cl);
				}
			}
			cl = new Bytecoder(cl,clazz,null).readClazz();
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose )
				Kiev.reportInfo("Loaded "+(
					cl.isPackage()?  "package   ":
					cl.isSyntax   ()?"syntax    ":
					cl.isInterface()?"interface ":
					                 "class     "
					)+name,diff_time);
			return cl;
		}
		// CLASSPATH is scanned, try project file
		ProjectFile pf = projectHash.get(name.name);
		File file = null;
		KString filename = null;
		KString cur_file = null;
		if( pf != null && pf.file != null) {
			File file = pf.file;
			if( file.exists() && file.canRead() ) {
				filename = KString.from(file.toString());
				cur_file = Kiev.curFile;
				Kiev.curFile = filename;
			}
		}
		if (file == null) {
			// Not found in project - lookup in CLASSPATH .java or .kiev file
			file = stdClassLoader.findSourceFile(name.bytecode_name.toString());
			if( file != null && file.exists() && file.canRead() ) {
				filename = KString.from(file.toString());
				cur_file = Kiev.curFile;
				Kiev.curFile = filename;
				if( Kiev.verbose ) Kiev.reportInfo("Found non-project source file "+file,0L);
			}
		}
		if (file == null)
			return null;
		Parser k = Kiev.k;
		try {
			diff_time = curr_time = System.currentTimeMillis();
			FileInputStream fin;
			try {
				fin = new FileInputStream(file);
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				fin = new FileInputStream(file);
			}
			k.interface_only = true;
			k.ReInit(fin);
			FileUnit fu = k.FileUnit(Kiev.curFile.toString());
			fu.scanned_for_interface_only = true;
			try {
				fin.close();
			} catch (IOException ioe) {
				Kiev.reportError(ioe);
			}
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Scanned file   "+filename,diff_time);
			System.gc();
			try {
				Kiev.files.append(fu);
				Kiev.runProcessorsOn(fu);
				fu = null;
			} catch(Exception e ) {
				Kiev.reportError(e);
			}
			System.gc();
		} catch ( ParseException e ) {
			Kiev.reportError(e);
		} catch ( ParseError e ) {
			System.out.println("Error while scanning input file:"+filename+":"+e);
		} finally {
			k.interface_only = Kiev.interface_only;
			Kiev.curFile = cur_file;
		}
		Struct cl = (Struct)classHash.get(name.name);
		return (Struct)cl;
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

