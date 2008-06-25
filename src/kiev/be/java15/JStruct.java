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

import java.io.*;

import kiev.ir.java15.RStruct;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ViewOf(vcast=true, iface=true)
public final view JStruct of Struct extends JTypeDecl {

	public:ro	JNode[]				members;
	public:ro	KString				bytecode_name;
	public:ro	JStruct				iface_impl;
	public:ro	InnerStructInfo		inner_info;

	public final boolean isPizzaCase();
	public final boolean isHasCases();
	public final boolean isMembersGenerated();
	public final boolean isMembersPreGenerated();

	public final String qname();

	public final KString bname() {
		if (bytecode_name == null) {
			if (isTypeDeclNotLoaded())
				return KString.from(qname().replace('\u001f', '/'));
			throw new RuntimeException("Bytecode name is not generated for "+this);
		}
		return bytecode_name;
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
		foreach (JField f; this.members; f.sname == name)
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
			if( m.hasName(name) && m.mtype.getJType().java_signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			JStruct defaults = self.iface_impl;
			if( defaults != null ) {
				foreach (JMethod m; defaults.members) {
					if( m.hasName(name) && m.mtype.getJType().java_signature.equals(sign))
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
		
		InnerStructInfo inner_info = this.inner_info;
		JNode[] members = this.members;
		
		if (inner_info != null) {
			foreach (Struct sub; inner_info.inners)
				((JStruct)sub).generate();
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(this.xtype.getJType().java_signature);
		foreach (JType jst; super_types)
			constPool.addClazzCP(jst.java_signature);
		if (inner_info != null) {
			foreach (Struct sub; inner_info.inners)
				constPool.addClazzCP(sub.xtype.getJType().java_signature);
		}
		
		{
			String fu = jctx_file_unit.fname;
			SourceFileAttr sfa = new SourceFileAttr(KString.from(fu));
			this.addAttr(sfa);
		}
		
		if (inner_info != null) {
			int count = 0;
			DNode[] inners = inner_info.inners;
			foreach (Struct s; inners)
				count ++;
			if (count > 0) {
				InnerClassesAttr a = new InnerClassesAttr();
				JStruct[] inner = new JStruct[count];
				JStruct[] outer = new JStruct[count];
				short[] inner_access = new short[count];
				for(int i=0, j=0; j < inners.length; j++) {
					if (inners[j] instanceof Struct) {
						Struct inn = (Struct)inners[j];
						inner[i] = (JStruct)inn;
						outer[i] = this;
						inner_access[i] = inn.getJavaFlags();
						constPool.addClazzCP(inn.xtype.getJType().java_signature);
						i++;
					}
				}
				a.inner = inner;
				a.outer = outer;
				a.acc = inner_access;
				addAttr(a);
			}
		}

		if (hasRuntimeVisibleMetas())
			this.addAttr(new RVMetaAttr(this));
		if (hasRuntimeInvisibleMetas())
			this.addAttr(new RIMetaAttr(this));
		
		for(int i=0; jattrs!=null && i < jattrs.length; i++)
			jattrs[i].generate(constPool);
		foreach (JField f; this.members) {
			constPool.addAsciiCP(f.sname);
			constPool.addAsciiCP(f.vtype.getJType().java_signature);

			if (f.hasRuntimeVisibleMetas())
				f.addAttr(new RVMetaAttr(f));
			if (f.hasRuntimeInvisibleMetas())
				f.addAttr(new RIMetaAttr(f));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			if (f.jattrs != null) {
				foreach (Attr a; f.jattrs)
					a.generate(constPool);
			}
		}
		foreach (JMethod m; members) {
			if (m.isConstructor()) {
				if (m.isStatic())
					constPool.addAsciiCP(knameClassInit);
				else
					constPool.addAsciiCP(knameInit);
			} else {
				constPool.addAsciiCP(m.sname);
			}
			constPool.addAsciiCP(m.mtype.getJType().java_signature);
			if( m.etype != null )
				constPool.addAsciiCP(m.etype.getJType().java_signature);

			try {
				m.generate(constPool);

				JWBCCondition[] conditions = m.conditions;
				for(int j=0; j < conditions.length; j++) {
					if( conditions[j].definer.equals(m) ) {
						m.addAttr(conditions[j].code_attr);
					}
				}

				if (m.hasRuntimeVisibleMetas())
					m.addAttr(new RVMetaAttr(m));
				if (m.hasRuntimeInvisibleMetas())
					m.addAttr(new RIMetaAttr(m));
				boolean has_vis_pmeta = false;
				boolean has_invis_pmeta = false;
				foreach (Var p; ((Method)m).params; p.hasRuntimeVisibleMetas()) {
					m.addAttr(new RVParMetaAttr(((Method)m).params));
					break;
				}
				foreach (Var p; ((Method)m).params; p.hasRuntimeInvisibleMetas()) {
					m.addAttr(new RIParMetaAttr(((Method)m).params));
					break;
				}
				if (isAnnotation() && m.body instanceof MetaValue)
					m.addAttr(new DefaultMetaAttr((MetaValue)m.body));

				if (m.jattrs != null) {
					foreach (Attr a; m.jattrs)
						a.generate(constPool);
				}
			} catch(Exception e ) {
				Kiev.reportError(m,"Compilation error: "+e);
				m.generate(constPool);
				if (m.jattrs != null) {
					foreach (Attr a; m.jattrs)
						a.generate(constPool);
				}
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
		if (!isMacro())
			this.toBytecode(constPool);
	}

	static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}

	public void toBytecode(ConstPool constPool) {
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = "classes";
		String out_file = this.bname().replace('/',File.separatorChar).toString();
		try {
			DataOutputStream out;
			JStruct.make_output_dir(output_dir,out_file);
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
}

