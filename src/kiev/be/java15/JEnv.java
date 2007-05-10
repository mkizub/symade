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
		TypeDecl td = Env.loadTypeDecl(pname,true);
		return loadClazz(ClazzName.fromOuterAndName((Struct)td, KString.from(qname.substring(p+1))));
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
		if (pkg.isTypeDeclNotLoaded())
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
		if ((td == null || td.isTypeDeclNotLoaded()) && clazz != null) {
			if (td == null)
				td = (Struct)Env.resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
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

	private synchronized byte[] loadClazzFromClasspath(String name) {
		trace(kiev.bytecode.Clazz.traceRules,"Loading data for clazz "+name);

		byte data[] = Env.classpath.read(name);
		trace(kiev.bytecode.Clazz.traceRules && data != null ,"Data for clazz "+name+" loaded");

		if( data == null || data.length == 0 )
			trace(kiev.bytecode.Clazz.traceRules,"Data for clazz "+name+" not found");

		return data;
	}
}

