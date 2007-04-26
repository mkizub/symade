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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class JEnv {

	Struct loadStruct(ClazzName name) {
		if (name.name == KString.Empty) return Env.root;
		// Check class is already loaded
		String qname = name.name.toString().replace('.','\u001f');
		if (Env.classHashOfFails.get(qname) != null ) return null;
		Struct cl = (Struct)Env.resolveGlobalDNode(qname);
		// Load if not loaded or not resolved
		if( cl == null )
			cl = (Struct)loadClazz(name);
		else if( !cl.isTypeDeclLoaded() && !cl.isAnonymouse() )
			cl = (Struct)loadClazz(name);
		if( cl == null )
			Env.classHashOfFails.put(qname);
		return cl;
	}

	Struct makeStruct(KString bc_name, boolean cleanup) {
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

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(String qname) {
		return loadClazz(ClazzName.fromToplevelName(KString.from(qname.replace('\u001f','.'))));
	}

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(Struct cl) {
		return loadClazz(ClazzName.fromBytecodeName(((JStruct)cl).bname()));
	}

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(ClazzName name) {
		// Ensure the parent package/outer class is loaded
		Struct pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
		if (pkg == null)
			pkg = Env.newPackage(name.package_name().toString().replace('.','\u001f'));
		if (!pkg.isTypeDeclLoaded())
			pkg = loadStruct(ClazzName.fromBytecodeName(((JStruct)pkg).bname()));

		long curr_time = 0L, diff_time = 0L;
		diff_time = curr_time = System.currentTimeMillis();
		byte[] data = loadClazzFromClasspath(name.bytecode_name.toString());
		if (data == null)
			return null;
		TypeDecl td = null;
		kiev.bytecode.Clazz clazz = null;
		if (data.length > 7 && new String(data,0,7,"UTF-8").startsWith("<?xml")) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing XML data for clazz "+name);
			td = (TypeDecl)Env.loadFromXmlData(data, name.src_name.toString(), pkg);
		}
		else if (data.length > 4 && (data[0]&0xFF) == 0xCA && (data[1]&0xFF) == 0xFE && (data[2]&0xFF) == 0xBA && (data[3]&0xFF) == 0xBE) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing .class data for clazz "+name);
			clazz = new kiev.bytecode.Clazz();
			clazz.readClazz(data);                                                                                    
		}
		if ((td == null || !td.isTypeDeclLoaded()) && clazz != null) {
			if (td == null) {
				td = (Struct)Env.resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
				if (td == null)
					td = makeStruct(name.bytecode_name,false);
				if (!td.isAttached()) {
					FileUnit fu = new FileUnit(name.src_name+".class", pkg);
					fu.members.add(td);
				}
			}
			td = new Bytecoder((Struct)td,clazz,null).readClazz();
		}
		if (td != null) {
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose )
				Kiev.reportInfo("Loaded "+(
					td.isPackage()?   "package   ":
					td.isSyntax()?    "syntax    ":
					td.isInterface()? "interface ":
					                  "class     "
					)+name,diff_time);
		}
		return td;
	}

	private synchronized byte[] loadClazzFromClasspath(String name) {
		trace(kiev.bytecode.Clazz.traceRules,"Loading data for clazz "+name);

		byte data[] = Env.classpath.read(name);
		trace(kiev.bytecode.Clazz.traceRules && data != null ,"Data for clazz "+name+" loaded");

		if( data == null || data.length == 0 )
			trace(kiev.bytecode.Clazz.traceRules,"Data for clazz "+name+" not found");

		return data;
	}
}

