/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.kiev020;
import kiev.parser.ParseException;
import kiev.parser.ParseError;
import kiev.parser.ASTFileUnit;
import kiev.transf.*;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Env.java,v 1.4.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.2 $
 *
 */

public class ProjectFile extends ASTNode {
	public ClazzName	name;
	public File			file;
	public boolean		bad = true;

	public ProjectFile(ClazzName clname, File f) {
		super(0,0);
		name = clname;
		file = f;
	}

	public ProjectFile(ClazzName clname, String f) {
		this(clname,new File(f));
	}

	public ProjectFile(ClazzName clname, KString f) {
		this(clname, new File( f.toString() ));
	}

	public void jjtAddChild(ASTNode n, int i) {}
    public Dumper toJava(Dumper dmp) { return dmp; }

}


/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

public class Env extends Struct {

	/** Hashtable of all defined and loaded classes */
	public static Hashtable<KString,Struct>	classHash = new Hashtable<KString,Struct>();
	public static Hash<KString>		classHashOfFails = new Hash<KString>();
	public static Hashtable<KString,Struct>	classHashDbg = new Hashtable<KString,Struct>();

	/** Hashtable for project file (class name + file name) */
	public static Hashtable<KString,ProjectFile>	projectHash = new Hashtable/*<KString,ProjectFile>*/();

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
		super(ClazzName.Empty);
		setPackage(true);
		setResolved(true);
		type = Type.tpVoid;
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
		Struct cl = classHash.get(name.name);
		if( cl != null ) {
			if( cleanup ) {
				cl.flags = access;
				cl.package_clazz = outer;
				cl.super_clazz = null;
				cl.interfaces = Type.emptyArray;
				cl.sub_clazz = Struct.emptyArray;
				cl.fields = Field.emptyArray;
				cl.virtual_fields = Field.emptyArray;
				cl.wrapped_field = null;
				if( cl.methods != null ) {
					foreach(Method m; cl.methods; m.isOperatorMethod() ) Operator.cleanupMethod(m);
				}
				cl.methods = Method.emptyArray;
				cl.imported.delAll();
				cl.attrs = Attr.emptyArray;
			}
			if( !cl.isArgument() )
				outer.addSubStruct((Struct)cl);
			return cl;
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated bytecode name "+name.bytecode_name+" of "+name.name);
		cl = new Struct(name,outer,access);
		classHash.put(cl.name.name,cl);
		classHashDbg.put(cl.name.bytecode_name,cl);
		if( outer == null ) {
			if( name.name.equals(name.short_name) )
				outer = root;
			else
				outer = getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name(),false));
		}
		outer.addSubStruct((Struct)cl);
		return cl;
	}

	public static Struct newInterface(ClazzName name,Struct outer/*,Typ sup*/,int access) {
		Struct cl = newStruct(name,outer,access);
		cl.setInterface(true);
		return cl;
	}

	public static Struct newPackage(KString name) {
		if( name.equals(KString.Empty) )
			return Env.root;
		Struct cl = classHash.get(name);
		if( cl != null ) {
			cl.setPackage(true);
			return cl;
		}
		return newPackage(ClazzName.fromToplevelName(name,false));
	}

	public static Struct newPackage(ClazzName name) {
		if( name.equals(ClazzName.Empty)) return Env.root;
		Struct cl = classHash.get(name.name);
		if( cl != null ) {
			cl.setPackage(true);
			return cl;
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated package name "+name.bytecode_name+" of "+name.name);
		return newPackage(name,newPackage(ClazzName.fromToplevelName(name.package_name(),false)));
	}

	public static Struct newPackage(ClazzName name,Struct outer) {
		Struct cl = newStruct(name,outer,0);
		cl.setPackage(true);
		cl.type = Type.newJavaRefType(cl);
		return cl;
	}

	public static Struct newArgument(KString nm,Struct outer) {
		// If outer is an inner class - this argument may be an argument
		// of it's outer class
		ClazzName name = ClazzName.fromOuterAndName(outer,nm,true,true);
		name.isArgument = true;
		Struct cl = classHash.get(name.name);
		if( cl != null ) {
			if( cl.isArgument() ) return cl;
			throw new RuntimeException("Class "+cl+" is not a class's argument");
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated class argument name "+name.bytecode_name+" of "+name.name);
		cl = new Struct(name,outer/*,sup*/,ACC_PUBLIC|ACC_STATIC|ACC_ARGUMENT);
		cl.setResolved(true);
		cl.super_clazz = Type.tpObject;
		cl.type = Type.newRefType(cl);
		classHash.put(cl.name.name,cl);
		classHashDbg.put(cl.name.bytecode_name,cl);
		return cl;
	}

	public static Struct newMethodArgument(KString nm, Struct outer) {
		// If outer is an inner class - this argument may be an argument
		// of it's outer class
		ClazzName name = ClazzName.fromBytecodeName(
			new KStringBuffer(outer.name.bytecode_name.len+8)
				.append_fast(outer.name.bytecode_name)
				.append_fast((byte)'$')
				.append(outer.anonymouse_inner_counter)
				.append((byte)'$')
				.append(nm)
				.toKString(),
				true
		);
		name.isArgument = true;
		Struct cl = classHash.get(name.name);
		if( cl != null ) {
			if( cl.isArgument() ) return cl;
			throw new RuntimeException("Class "+cl+" is not a class's argument");
		}
		assert(classHashDbg.get(name.bytecode_name)==null,"Duplicated method argument name "+name.bytecode_name+" of "+name.name);
		cl = new Struct(name,outer/*,sup*/,ACC_PUBLIC|ACC_STATIC|ACC_ARGUMENT);
		cl.setResolved(true);
		cl.super_clazz = Type.tpObject;
		cl.type = Type.newRefType(cl);
		classHash.put(cl.name.name,cl);
		classHash.put(cl.name.bytecode_name,cl);
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
		stdClassLoader.addHandler(new kiev.bytecode.KievAttributeHandler());

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
				Kiev.reportWarning(0,"Error while project file reading: "+e);
			}
		}

		root.setPackage(true);
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
				strs.append(value.name.name+" "+value.name.bytecode_name+" "+value.file+(value.bad?" bad":""));
			}
			String[] sarr = (String[])strs;
			sortStrings(sarr);
			foreach(String s; sarr) out.println(s);
		} catch (IOException e) {
			Kiev.reportWarning(0,"Error while project file writing: "+e);
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
		if (classHash.get(name) != null)
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
		return cl;
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
		return cl;
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
			Struct cl = classHash.get(name.name);
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
				if( cl == null )
					cl = newStruct(name,false);
			}
			cl = new Bytecoder(cl,clazz).readClazz();
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
			kiev020 k = Kiev.k;
			kiev020.interface_only = true;
			k.ReInit(fin);
			ASTFileUnit fu = k.FileUnit(Kiev.curFile.toString());
			try {
				fin.close();
			} catch (IOException ioe) {
				Kiev.reportError(0,ioe.getMessage());
			}
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Scanned file   "+filename,diff_time);
			System.gc();
			try {
				Kiev.files_scanned.append(fu);
				ExportJavaTop exporter = new ExportJavaTop();
				if ( Kiev.passGreaterEquals(TopLevelPass.passCreateTopStruct) )     exporter.pass1(fu, null);
				if ( Kiev.passGreaterEquals(TopLevelPass.passProcessSyntax) )       exporter.pass1_1(fu, null);
				if ( Kiev.passGreaterEquals(TopLevelPass.passArgumentInheritance) ) exporter.pass2(fu, null);
				if ( Kiev.passGreaterEquals(TopLevelPass.passStructInheritance) )	exporter.pass2_2(fu, null);
				if ( Kiev.passGreaterEquals(TopLevelPass.passCreateMembers) )		fu.pass3();
				if ( Kiev.passGreaterEquals(TopLevelPass.passAutoProxyMethods) )	fu.autoProxyMethods();
				if ( Kiev.passGreaterEquals(TopLevelPass.passResolveImports) )		fu.resolveImports();
				if ( Kiev.passGreaterEquals(TopLevelPass.passResolveFinalFields) )	fu.resolveFinalFields(false);
				if ( Kiev.passGreaterEquals(TopLevelPass.passGenerate) ) {
					if (Kiev.safe)
						Kiev.files.append(fu.file_unit);
					else
						fu.file_unit.cleanup();
				}
				fu = null;
			} catch(Exception e ) {
				Kiev.reportError(0,e);
			}
			System.gc();
		} catch ( ParseException e ) {
			Kiev.reportError(0,e);
		} catch ( ParseError e ) {
			System.out.println("Error while scanning input file:"+filename+":"+e);
		} finally {
			kiev020.interface_only = Kiev.interface_only;
			Kiev.curFile = cur_file;
		}
		Struct cl = classHash.get(name.name);
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
