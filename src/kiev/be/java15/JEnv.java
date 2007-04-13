/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.be.java15;

import kiev.Kiev;
import kiev.KievExt;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import java.io.*;

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
		Struct cl = (Struct)Env.resolveGlobalDNode(qname);
		// Load if not loaded or not resolved
		if( cl == null )
			cl = loadClazz(name);
		else if( !cl.isTypeDeclLoaded() && !cl.isAnonymouse() )
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
			foreach (Struct s; pkg.sub_decls; s.sname == nm) {
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
			foreach (Struct s; pkg.sub_decls; s.sname == nm) {
				ss = s;
				break;
			}
			if (ss == null)
				ss = Env.newStruct(nm, true, pkg, 0, null, false, null);
			pkg = ss;
			start = end+1;
			end = bc_name.indexOf('$', start);
		}
		String nm = bc_name.substr(start).toString().intern();
		//assert (!Character.isDigit((char)nm.byteAt(0)));
		foreach (Struct s; pkg.sub_decls; s.sname == nm)
			return s;
		return Env.newStruct(nm, true, pkg, 0, null, cleanup, null);
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
		Struct cl = null;
		if( clazz != null ) {
			cl = (Struct)Env.resolveGlobalDNode(name.name.toString().intern());
			if( cl == null || !cl.isTypeDeclLoaded() || cl.package_clazz.dnode==null ) {
				// Ensure the parent package/outer class is loaded
				Struct pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
				if( pkg == null ) {
					pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
					if( pkg == null )
						pkg = Env.newPackage(name.package_name().toString().intern());
				}
				if( !pkg.isTypeDeclLoaded() ) {
					pkg = loadStruct(ClazzName.fromBytecodeName(((JStruct)pkg).bname()));
				}
				if( cl == null ) {
					cl = makeStruct(name.bytecode_name,false);
					new FileUnit(name.src_name+".class", pkg).members.add(cl);
				}
			}
			cl = new Bytecoder(cl,clazz,null).readClazz();
			//Kiev.lockNodeTree(cl);
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose )
				Kiev.reportInfo("Loaded "+(
					cl.isPackage()?  "package   ":
					cl.isSyntax   ()?"syntax    ":
					cl.isInterface()?"interface ":
					                 "class     "
					)+name,diff_time);
		}
		return cl;
	}

}

