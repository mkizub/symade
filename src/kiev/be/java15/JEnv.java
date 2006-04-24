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

	public Struct makeStruct(KString bc_name, boolean cleanup) {
		Struct pkg = Env.root;
		int start = 0;
		int end = bc_name.indexOf('/', start);
		while (end > 0) {
			KString nm = bc_name.substr(start, end);
			Struct ss = null;
			foreach (Struct s; pkg.sub_clazz; s.short_name.name == nm) {
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
			KString nm = bc_name.substr(start, end);
			assert (!Character.isDigit((char)nm.byteAt(0)));
			Struct ss = null;
			foreach (Struct s; pkg.sub_clazz; s.short_name.name == nm) {
				ss = s;
				break;
			}
			if (ss == null)
				ss = Env.newStruct(nm, true, pkg, 0, false);
			pkg = ss;
			start = end+1;
			end = bc_name.indexOf('$', start);
		}
		KString nm = bc_name.substr(start);
		//assert (!Character.isDigit((char)nm.byteAt(0)));
		foreach (Struct s; pkg.sub_clazz; s.short_name.name == nm)
			return s;
		return Env.newStruct(nm, true, pkg, 0, cleanup);
	}

	public boolean existsClazz(ClazzName name) {
		return stdClassLoader.existsClazz(name.bytecode_name.toString());
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
			Struct cl = Env.resolveStruct(name.name);
			if( cl == null || !cl.isResolved() || cl.package_clazz==null ) {
				// Ensure the parent package/outer class is loaded
				Struct pkg = Env.getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
				if( pkg == null ) {
					pkg = Env.getStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
					if( pkg == null )
						pkg = Env.newPackage(name.package_name());
				}
				if( !pkg.isResolved() ) {
					pkg = Env.getStruct(pkg.name);
					//pkg = loadClazz(pkg.name);
				}
				if( cl == null ) {
					cl = makeStruct(name.bytecode_name,false);
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
		ProjectFile pf = Env.projectHash.get(name.name);
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
		Struct cl = Env.resolveStruct(name.name);
		return cl;
	}

}

