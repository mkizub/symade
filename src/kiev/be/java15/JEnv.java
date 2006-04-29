package kiev.be.java15;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.vlang.NArr.JArr;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class JEnv {

	/** StandardClassLoader */
	private final kiev.bytecode.StandardClassLoader		stdClassLoader;

	public JEnv(String path) {
		if( path == null ) path = System.getProperty("java.class.path");
		stdClassLoader = new kiev.bytecode.StandardClassLoader(path);
	}

	Struct loadStruct(ClazzName name) throws RuntimeException {
		if (name.name == KString.Empty) return Env.root;
		// Check class is already loaded
		String qname = name.name.toString().intern();
		if (Env.classHashOfFails.get(qname) != null ) return null;
		Struct cl = Env.resolveStruct(qname);
		// Load if not loaded or not resolved
		if( cl == null )
			cl = loadClazz(name);
		else if( !cl.isResolved() && !cl.isAnonymouse() )
			cl = loadClazz(name);
		if( cl == null )
			Env.classHashOfFails.put(qname);
		return cl;
	}

	public Struct makeStruct(KString bc_name, boolean cleanup) {
		Struct pkg = Env.root;
		int start = 0;
		int end = bc_name.indexOf('/', start);
		while (end > 0) {
			String nm = bc_name.substr(start, end).toString().intern();
			Struct ss = null;
			foreach (Struct s; pkg.sub_clazz; s.id.equals(nm)) {
				ss = s;
				break;
			}
			if (ss == null)
				ss = Env.newPackage(nm, pkg);
			pkg = ss;
			start = end+1;
			end = bc_name.indexOf('/', start);
		}
		end = bc_name.indexOf('$', start);
		while (end > 0) {
			String nm = bc_name.substr(start, end).toString().intern();
			assert (!Character.isDigit(nm.charAt(0)));
			Struct ss = null;
			foreach (Struct s; pkg.sub_clazz; s.id.equals(nm)) {
				ss = s;
				break;
			}
			if (ss == null)
				ss = Env.newStruct(nm, true, pkg, 0, false);
			pkg = ss;
			start = end+1;
			end = bc_name.indexOf('$', start);
		}
		String nm = bc_name.substr(start).toString().intern();
		//assert (!Character.isDigit((char)nm.byteAt(0)));
		foreach (Struct s; pkg.sub_clazz; s.id.equals(nm))
			return s;
		return Env.newStruct(nm, true, pkg, 0, cleanup);
	}

	public boolean existsClazz(String qname) {
		return stdClassLoader.existsClazz(ClazzName.fromToplevelName(KString.from(qname)).bytecode_name.toString());
	}

	/** Actually load class from specified file and dir */
	public Struct loadClazz(String qname) throws RuntimeException {
		return loadClazz(ClazzName.fromToplevelName(KString.from(qname)),false);
	}

	/** Actually load class from specified file and dir */
	public Struct loadClazz(Struct cl) throws RuntimeException {
		return loadClazz(ClazzName.fromBytecodeName(((JStruct)cl).bname()),false);
	}

	/** Actually load class from specified file and dir */
	public Struct loadClazz(ClazzName name) throws RuntimeException {
		return loadClazz(name,false);
	}

	/** Actually load class from specified file and dir */
	public Struct loadClazz(ClazzName name, boolean force) throws RuntimeException {
		long curr_time = 0L, diff_time = 0L;
		diff_time = curr_time = System.currentTimeMillis();
		kiev.bytecode.Clazz clazz = stdClassLoader.loadClazz(name.bytecode_name.toString());
		if( clazz != null ) {
			Struct cl = Env.resolveStruct(name.name.toString().intern());
			if( cl == null || !cl.isResolved() || cl.package_clazz==null ) {
				// Ensure the parent package/outer class is loaded
				Struct pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
				if( pkg == null ) {
					pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
					if( pkg == null )
						pkg = Env.newPackage(name.package_name().toString().intern());
				}
				if( !pkg.isResolved() ) {
					pkg = loadStruct(ClazzName.fromBytecodeName(((JStruct)pkg).bname()));
					//pkg = loadClazz(pkg.name);
				}
				if( cl == null ) {
					cl = makeStruct(name.bytecode_name,false);
					new FileUnit(KString.from(name.src_name+".class"), pkg).members.add(cl);
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
		ProjectFile pf = Env.projectHash.get(name.name.toString().intern());
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
		Struct cl = Env.resolveStruct(name.name.toString().intern());
		return cl;
	}

}

