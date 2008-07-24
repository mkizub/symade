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

	public void generateFile(FileUnit fu) {
		((JFileUnit)fu).generate();
	}
	
	public void backendCleanup(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof ASTNode)
					((JNode)(ASTNode)n).backendCleanup();
				return true;
			}
			public void post_exec(ANode n) {}
		});
	}
	

	public DNode loadDecl(ClazzName name) {
		if (name.name == KString.Empty) return Env.getRoot();
		// Check class is already loaded
		String qname = name.name.toString().replace('.','\u001f');
		if (Env.classHashOfFails.get(qname) != null ) return null;
		DNode cl = Env.getRoot().resolveGlobalDNode(qname);
		// Load if not loaded or not resolved
		if (cl == null)
			cl = actuallyLoadDecl(name);
		else if (cl instanceof TypeDecl && cl.isTypeDeclNotLoaded())
			cl = actuallyLoadDecl(name);
		if (cl == null)
			Env.classHashOfFails.put(qname);
		return cl;
	}

	/** Actually load class from specified file and dir */
	public DNode actuallyLoadDecl(String qname) {
		int p = qname.lastIndexOf('\u001f');
		if (p < 0)
			return actuallyLoadDecl(ClazzName.fromToplevelName(KString.from(qname)));
		String pname = qname.substring(0,p);
		DNode dn = Env.getRoot().loadAnyDecl(pname);
		if (dn == null)
			throw new RuntimeException("Cannot find class/package "+pname.replace('\u001f','.'));
		// maybe an inner class is already loaded by outer class
		DNode cl = Env.getRoot().resolveGlobalDNode(qname);
		if (cl != null && !cl.isTypeDeclNotLoaded())
			return cl;
		return actuallyLoadDecl(ClazzName.fromOuterAndName(dn, KString.from(qname.substring(p+1))));
	}

	/** Actually load class from specified file and dir */
	public Struct actuallyLoadDecl(Struct cl) {
		KString bc_name = cl.bytecode_name;
		if (bc_name != null)
			return (Struct)actuallyLoadDecl(ClazzName.fromBytecodeName(bc_name));
		if (cl.sname == null)
			throw new RuntimeException("Anonymouse class cannot be loaded from bytecode");
		if (cl.parent() instanceof KievPackage)
			return (Struct)actuallyLoadDecl(ClazzName.fromOuterAndName((KievPackage)cl.parent(), KString.from(cl.sname)));
		else
			return (Struct)actuallyLoadDecl(ClazzName.fromOuterAndName(cl.ctx_tdecl, KString.from(cl.sname)));
	}

	/** Actually load class from specified file and dir */
	public DNode actuallyLoadDecl(ClazzName name) {
		// Ensure the parent package/outer class is loaded
		DNode pkg = loadDecl(ClazzName.fromBytecodeName(name.package_bytecode_name()));
		if (pkg == null)
			pkg = Env.getRoot().newPackage(name.package_name().toString().replace('.','\u001f'));
		if (pkg.isTypeDeclNotLoaded())
			pkg = loadDecl(ClazzName.fromBytecodeName(((JStruct)pkg).bname()));

		long curr_time = 0L, diff_time = 0L;
		diff_time = curr_time = System.currentTimeMillis();
		byte[] data = Env.getRoot().loadClazzFromClasspath(name.bytecode_name.toString());
		if (data == null)
			return null;
		DNode td = null;
		kiev.bytecode.Clazz clazz = null;
		if (data.length > 7 && new String(data,0,7,"UTF-8").startsWith("<?xml")) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing XML data for clazz "+name);
			DumpUtils.loadFromXmlData(data, name.src_name.toString(), pkg);
			td = Env.getRoot().resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
		}
		else if (data.length > 4 && (data[0]&0xFF) == 0xCA && (data[1]&0xFF) == 0xFE && (data[2]&0xFF) == 0xBA && (data[3]&0xFF) == 0xBE) {
			trace(kiev.bytecode.Clazz.traceRules,"Parsing .class data for clazz "+name);
			clazz = new kiev.bytecode.Clazz();
			clazz.readClazz(data);                                                                                    
		}
		if ((td == null || td.isTypeDeclNotLoaded()) && clazz != null) {
			if (td == null)
				td = Env.getRoot().resolveGlobalDNode(name.name.toString().replace('.','\u001f'));
			if (td == null || td.isTypeDeclNotLoaded())
				td = new Bytecoder((Struct)td,clazz,null).readClazz(name, pkg);
		}
		if (td != null) {
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose )
				Kiev.reportInfo("Loaded "+(
					td instanceof KievPackage ? "package   ":
					td.isInterface()          ? "interface ":
					td instanceof KievSyntax  ? "syntax    ":
					                            "class     "
					)+name,diff_time);
		}
		return td;
	}
}

