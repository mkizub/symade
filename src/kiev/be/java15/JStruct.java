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

import kiev.ir.java15.RStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final view JStruct of Struct extends JTypeDecl {

	public		KString				b_name;
	public:ro	JStruct				package_clazz;
	public:ro	JStruct				iface_impl;
	public:ro	JDNode[]			sub_decls;
	public		Attr[]				attrs;

	public final boolean isPizzaCase();
	public final boolean isHasCases();
	public final boolean isMembersGenerated();
	public final boolean isMembersPreGenerated();

	public final String qname();

	public final KString bname() {
		if (b_name != null)
			return b_name;
		JStruct pkg = package_clazz;
		if (pkg == null || ((Struct)pkg) == Env.root)
			b_name = KString.from(u_name);
		else if (pkg.isPackage())
			b_name = KString.from(pkg.bname()+"/"+u_name);
		else
			b_name = KString.from(pkg.bname()+"$"+u_name);
		return b_name;
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		for(int i=0; i < attrs.length; i++) {
			if(attrs[i].name == a.name) {
				attrs[i] = a;
				return a;
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		if( attrs != null )
			for(int i=0; i < attrs.length; i++)
				if( attrs[i].name.equals(name) )
					return attrs[i];
		return null;
	}

	public JENode accessTypeInfoField(JNode from, Type t, boolean from_gen) {
		assert (from_gen == true);
		return (JENode)((RStruct)((Struct)this)).accessTypeInfoField((ASTNode)from, t, from_gen);
	}
	
	public JField resolveField(String name) {
		return resolveField(name,true);
	}

	public JField resolveField(String name, boolean fatal) {
		checkResolved();
		foreach (JField f; this.getAllFields(); f.sname == name)
			return f;
		foreach (JType jt; this.super_types) {
			JField f = jt.getJStruct().resolveField(name, false);
			if (f != null)
				return f;
		}
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+this);
		return null;
	}


	public JMethod resolveMethod(String name, KString sign) {
		return resolveMethod(this,name,sign,this,true);
	}

	public JMethod resolveMethod(String name, KString sign, boolean fatal) {
		return resolveMethod(this,name,sign,this,fatal);
	}

	private static JMethod resolveMethod(@forward JStruct self, String name, KString sign, JStruct where, boolean fatal) {
		self.checkResolved();
		foreach (JMethod m; members) {
			if( m.hasName(name,true) && m.type.getJType().java_signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			JStruct defaults = self.iface_impl;
			if( defaults != null ) {
				foreach (JMethod m; defaults.members) {
					if( m.hasName(name,true) && m.type.getJType().java_signature.equals(sign))
						return m;
				}
			}
		}
		trace(Kiev.debug && Kiev.debugResolve,"Method "+name+" with signature "+sign+" unresolved in class "+self);
		JMethod m = null;
		foreach (JType jst; super_types) {
			m = resolveMethod(jst.getJStruct(),name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	public void generate() {
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+this+"]");
		if( Kiev.safe && isBad() ) return;
		
		JDNode[] sub_decls = this.sub_decls;
		JNode[] members = this.members;
		
		if( !isPackage() ) {
			foreach (JStruct sub; sub_decls)
				sub.generate();
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(this.xtype.getJType().java_signature);
		foreach (JType jst; super_types)
			constPool.addClazzCP(jst.java_signature);
		if( !isPackage() ) {
			foreach (JStruct sub; sub_decls) {
				sub.checkResolved();
				constPool.addClazzCP(sub.xtype.getJType().java_signature);
			}
		}
		
		if( !isPackage() ) {
			String fu = jctx_file_unit.name;
			int p = fu.lastIndexOf('/');
			if (p >= 0) fu = fu.substring(p+1);
			p = fu.lastIndexOf('\\');
			if (p >= 0) fu = fu.substring(p+1);
			SourceFileAttr sfa = new SourceFileAttr(KString.from(fu));
			this.addAttr(sfa);
		}
		
		if( !isPackage() && sub_decls.length > 0 ) {
			int count = 0;
			for(int j=0; j < sub_decls.length; j++) {
				if (sub_decls[j] instanceof JStruct)
					count ++;
			}
			if (count > 0) {
				InnerClassesAttr a = new InnerClassesAttr();
				JStruct[] inner = new JStruct[count];
				JStruct[] outer = new JStruct[count];
				short[] inner_access = new short[count];
				for(int i=0, j=0; j < sub_decls.length; j++) {
					if (sub_decls[j] instanceof JStruct) {
						inner[i] = (JStruct)sub_decls[j];
						outer[i] = this;
						inner_access[i] = sub_decls[j].getJavaFlags();
						constPool.addClazzCP(inner[i].xtype.getJType().java_signature);
						i++;
					}
				}
				a.inner = inner;
				a.outer = outer;
				a.acc = inner_access;
				addAttr(a);
			}
		}

		if (meta.hasRuntimeVisibles())
			this.addAttr(new RVMetaAttr(meta));
		if (meta.hasRuntimeInvisibles())
			this.addAttr(new RIMetaAttr(meta));
		
		for(int i=0; attrs!=null && i < attrs.length; i++) attrs[i].generate(constPool);
		foreach (JField f; getAllFields()) {
			constPool.addAsciiCP(f.u_name);
			constPool.addAsciiCP(f.type.getJType().java_signature);

			if( f.isAccessedFromInner()) {
				f.openForEdit();
				((Field)f).setPkgPrivate();
			}
			if (f.meta.hasRuntimeVisibles())
				f.addAttr(new RVMetaAttr(f.meta));
			if (f.meta.hasRuntimeInvisibles())
				f.addAttr(new RIMetaAttr(f.meta));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			foreach (Attr a; f.attrs)
				a.generate(constPool);
		}
		foreach (JMethod m; members) {
			constPool.addAsciiCP(m.u_name);
			constPool.addAsciiCP(m.type.getJType().java_signature);
			if( m.etype != null )
				constPool.addAsciiCP(m.etype.getJType().java_signature);

			try {
				m.generate(constPool);

				if( m.isAccessedFromInner()) {
					m.openForEdit();
					((Method)m).setPkgPrivate();
				}

				JWBCCondition[] conditions = m.conditions;
				for(int j=0; j < conditions.length; j++) {
					if( conditions[j].definer.equals(m) ) {
						m.addAttr(conditions[j].code_attr);
					}
				}

				if (m.meta.hasRuntimeVisibles())
					m.addAttr(new RVMetaAttr(m.meta));
				if (m.meta.hasRuntimeInvisibles())
					m.addAttr(new RIMetaAttr(m.meta));
				boolean has_vis_pmeta = false;
				boolean has_invis_pmeta = false;
				JVar[] params = m.params;
				foreach (JVar p; params; p.meta.hasRuntimeVisibles()) {
					MetaSet[] mss;
					mss = new MetaSet[params.length];
					for (int i=0; i < mss.length; i++)
						mss[i] = params[i].meta;
					m.addAttr(new RVParMetaAttr(mss));
					break;
				}
				foreach (JVar p; params; p.meta.hasRuntimeInvisibles()) {
					MetaSet[] mss;
					mss = new MetaSet[params.length];
					for (int i=0; i < mss.length; i++)
						mss[i] = params[i].meta;
					m.addAttr(new RIParMetaAttr(mss));
					break;
				}
				if (isAnnotation() && m.body instanceof MetaValue)
					m.addAttr(new DefaultMetaAttr((MetaValue)m.body));

				foreach (Attr a; m.attrs)
					a.generate(constPool);
			} catch(Exception e ) {
				Kiev.reportError(m,"Compilation error: "+e);
				m.generate(constPool);
				foreach (Attr a; m.attrs)
					a.generate(constPool);
			}
			if( Kiev.safe && isBad() ) return;
		}
		constPool.generate();
		foreach (JMethod m; members) {
			CodeAttr ca = (CodeAttr)m.getAttr(attrCode);
			if( ca != null ) {
				trace(Kiev.debug && Kiev.debugInstrGen," generating refs for CP for method "+this+"."+m);
				Code.patchCodeConstants(ca);
			}
		}
		if( Kiev.safe && isBad() )
			return;
		if !(isPackage() || isSyntax() || isMacro())
			this.toBytecode(constPool);
		Env.setProjectInfo((Struct)this, true);
	}

	public void toBytecode(ConstPool constPool) {
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = Kiev.javaMode ? "." : "classes";
		String out_file;
		if( Kiev.javaMode && output_dir == null )
			out_file = this.sname;
		else if( this.isPackage() )
			out_file = (this.bname()+"/package").replace('/',File.separatorChar);
		else
			out_file = this.bname().replace('/',File.separatorChar).toString();
		try {
			DataOutputStream out;
			JFileUnit.make_output_dir(output_dir,out_file);
			try {
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			}
			byte[] dump = new Bytecoder((Struct)this,null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+this.name+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
		}
	}

	public void cleanup() {
		if( !isPackage() ) {
			foreach (JStruct sub; this.sub_decls)
				sub.cleanup();
		}

		foreach (JDNode n; this.members) {
			if (n instanceof JField)
				n.attrs = Attr.emptyArray;
			else if (n instanceof JMethod)
				n.attrs = Attr.emptyArray;
		}
		this.attrs = Attr.emptyArray;
	}

}

