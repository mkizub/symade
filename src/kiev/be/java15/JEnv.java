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
		if (name.name == KString.Empty) return Env.getRoot();
		// Check class is already loaded
		String qname = name.name.toString().replace('.','\u001f');
		if (Env.classHashOfFails.get(qname) != null ) return null;
		Struct cl = (Struct)Env.getRoot().resolveGlobalDNode(qname);
		// Load if not loaded or not resolved
		if( cl == null )
			cl = (Struct)loadClazz(name);
		else if( cl.isTypeDeclNotLoaded() && !cl.isAnonymouse() )
			cl = (Struct)loadClazz(name);
		if( cl == null )
			Env.classHashOfFails.put(qname);
		return cl;
	}

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(String qname) {
		int p = qname.lastIndexOf('\u001f');
		if (p < 0)
			return loadClazz(ClazzName.fromToplevelName(KString.from(qname)));
		String pname = qname.substring(0,p);
		TypeDecl td = Env.getRoot().loadTypeDecl(pname,true);
		// maybe an inner class is already loaded by outer class
		TypeDecl cl = (TypeDecl)Env.getRoot().resolveGlobalDNode(qname);
		if (cl != null && !cl.isTypeDeclNotLoaded())
			return cl;
		return loadClazz(ClazzName.fromOuterAndName((Struct)td, KString.from(qname.substring(p+1))));
	}

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(Struct cl) {
		KString bc_name = cl.bytecode_name;
		if (bc_name != null)
			return loadClazz(ClazzName.fromBytecodeName(bc_name));
		if (cl.sname == null)
			throw new RuntimeException("Anonymouse class cannot be loaded from bytecode");
		return loadClazz(ClazzName.fromOuterAndName(cl.package_clazz.dnode, KString.from(cl.sname)));
	}

	/** Actually load class from specified file and dir */
	public TypeDecl loadClazz(ClazzName name) {
		// Ensure the parent package/outer class is loaded
		Struct pkg = loadStruct(ClazzName.fromBytecodeName(name.package_bytecode_name()));
		if (pkg == null)
			pkg = Env.getRoot().newPackage(name.package_name().toString().replace('.','\u001f'));
		if (pkg.isTypeDeclNotLoaded())
			pkg = loadStruct(ClazzName.fromBytecodeName(((JStruct)pkg).bname()));

		long curr_time = 0L, diff_time = 0L;
		diff_time = curr_time = System.currentTimeMillis();
		byte[] data = Env.getRoot().loadClazzFromClasspath(name.bytecode_name.toString());
		if (data == null)
			return null;
		TypeDecl td = null;
		kiev.bytecode.Clazz clazz = null;
		if (data.length > 7 && new String(data,0,7,"UTF-8").startsWith("<?xml")) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing XML data for clazz "+name);
			Env.getRoot().loadFromXmlData(data, name.src_name.toString(), pkg);
			td = (TypeDecl)Env.getRoot().resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
		}
		else if (data.length > 4 && (data[0]&0xFF) == 0xCA && (data[1]&0xFF) == 0xFE && (data[2]&0xFF) == 0xBA && (data[3]&0xFF) == 0xBE) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing .class data for clazz "+name);
			clazz = new kiev.bytecode.Clazz();
			clazz.readClazz(data);                                                                                    
		}
		if ((td == null || td.isTypeDeclNotLoaded()) && clazz != null) {
			if (td == null)
				td = (Struct)Env.getRoot().resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
			if (td == null || td.isTypeDeclNotLoaded())
				td = new Bytecoder((Struct)td,clazz,null).readClazz(name, pkg);
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
}

