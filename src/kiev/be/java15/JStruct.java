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

import java.io.*;

import kiev.ir.java15.RStruct;

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
	
	public JField resolveField(JEnv jenv, String name) {
		return resolveField(jenv,name,true);
	}

	public JField resolveField(JEnv jenv, String name, boolean fatal) {
		checkResolved();
		foreach (JField f; this.members; f.sname == name)
			return f;
		foreach (TypeRef t; this.super_types) {
			JField f = jenv.getJTypeEnv().getJType(t.getType()).getJStruct().resolveField(jenv, name, false);
			if (f != null)
				return f;
		}
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+this);
		return null;
	}


	public JMethod resolveMethod(JEnv jenv, String name, KString sign) {
		return resolveMethod(this,jenv,name,sign,this,true);
	}

	public JMethod resolveMethod(JEnv jenv, String name, KString sign, boolean fatal) {
		return resolveMethod(this,jenv,name,sign,this,fatal);
	}

	private static JMethod resolveMethod(@forward JStruct self, JEnv jenv, String name, KString sign, JStruct where, boolean fatal) {
		self.checkResolved();
		JTypeEnv jtenv = jenv.getJTypeEnv();
		foreach (JMethod m; members) {
			if( m.hasName(name) && jtenv.getJType(m.mtype).java_signature.equals(sign))
				return m;
		}
		if( isInterface() ) {
			JStruct defaults = self.iface_impl;
			if( defaults != null ) {
				foreach (JMethod m; defaults.members) {
					if( m.hasName(name) && jtenv.getJType(m.mtype).java_signature.equals(sign))
						return m;
				}
			}
		}
		trace(Kiev.debug && Kiev.debugResolve,"Method "+name+" with signature "+sign+" unresolved in class "+self);
		JMethod m = null;
		foreach (TypeRef jst; super_types) {
			m = resolveMethod(jenv.getJTypeEnv().getJType(jst.getType()).getJStruct(),jenv,name,sign,where,fatal);
			if( m != null ) return m;
		}
		if (fatal)
			throw new RuntimeException("Unresolved method "+name+sign+" in class "+where);
		return null;
	}

	public void generate(JEnv jenv) {
		//if( Kiev.verbose ) System.out.println("[ Generating cls "+this+"]");
		if( Kiev.safe && isBad() ) return;
		
		InnerStructInfo inner_info = this.inner_info;
		JNode[] members = this.members;
		
		if (inner_info != null) {
			foreach (Struct sub; inner_info.inners)
				((JStruct)sub).generate(jenv);
		}

		ConstPool constPool = new ConstPool();
		constPool.addClazzCP(jenv.getJTypeEnv().getJType(this.xtype).java_signature);
		foreach (TypeRef jst; super_types)
			constPool.addClazzCP(jenv.getJTypeEnv().getJType(jst.getType()).java_signature);
		if (inner_info != null) {
			foreach (Struct sub; inner_info.inners)
				constPool.addClazzCP(jenv.getJTypeEnv().getJType(sub.xtype).java_signature);
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
				InnerClassesAttr a = new InnerClassesAttr(jenv);
				JStruct[] inner = new JStruct[count];
				JStruct[] outer = new JStruct[count];
				short[] inner_access = new short[count];
				for(int i=0, j=0; j < inners.length; j++) {
					if (inners[j] instanceof Struct) {
						Struct inn = (Struct)inners[j];
						inner[i] = (JStruct)inn;
						outer[i] = this;
						inner_access[i] = inn.getJavaFlags();
						constPool.addClazzCP(jenv.getJTypeEnv().getJType(inn.xtype).java_signature);
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
			this.addAttr(new RVMetaAttr(jenv,this));
		if (hasRuntimeInvisibleMetas())
			this.addAttr(new RIMetaAttr(jenv,this));
		
		Attr[] jattrs = getJAttrs();
		for(int i=0; jattrs!=null && i < jattrs.length; i++)
			jattrs[i].generate(constPool);
		foreach (JField f; this.members) {
			constPool.addAsciiCP(f.sname);
			constPool.addAsciiCP(jenv.getJTypeEnv().getJType(f.vtype).java_signature);

			if (f.hasRuntimeVisibleMetas())
				f.addAttr(new RVMetaAttr(jenv,f));
			if (f.hasRuntimeInvisibleMetas())
				f.addAttr(new RIMetaAttr(jenv,f));
			if (f.isStatic() && f.init != null && f.init.isConstantExpr()) {
				Object co = f.init.getConstValue();
				if (co != null)
					f.addAttr(new ConstantValueAttr(co));
			}

			jattrs = f.getJAttrs();
			if (jattrs != null) {
				foreach (Attr a; jattrs)
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
			constPool.addAsciiCP(jenv.getJTypeEnv().getJType(m.mtype).java_signature);
			if( m.etype != null )
				constPool.addAsciiCP(jenv.getJTypeEnv().getJType(m.etype).java_signature);

			try {
				m.generate(jenv,constPool);

				foreach (WBCCondition cond; m.conditions(); cond.definer == (Method)m) {
					m.addAttr(((JWBCCondition)cond).getCodeAttr());
				}

				if (m.hasRuntimeVisibleMetas())
					m.addAttr(new RVMetaAttr(jenv,m));
				if (m.hasRuntimeInvisibleMetas())
					m.addAttr(new RIMetaAttr(jenv,m));
				boolean has_vis_pmeta = false;
				boolean has_invis_pmeta = false;
				foreach (Var p; ((Method)m).params; p.hasRuntimeVisibleMetas()) {
					m.addAttr(new RVParMetaAttr(jenv,((Method)m).params));
					break;
				}
				foreach (Var p; ((Method)m).params; p.hasRuntimeInvisibleMetas()) {
					m.addAttr(new RIParMetaAttr(jenv,((Method)m).params));
					break;
				}
				if (isAnnotation()) {
					ENode mbody = (ENode)m.body;
					if (mbody instanceof MetaValue)
						m.addAttr(new DefaultMetaAttr(jenv,(MetaValue)mbody));
				}

				jattrs = m.getJAttrs();
				if (jattrs != null) {
					foreach (Attr a; jattrs)
						a.generate(constPool);
				}
			} catch(Exception e ) {
				Kiev.reportError(m,"Compilation error: "+e);
				m.generate(jenv,constPool);
				jattrs = m.getJAttrs();
				if (jattrs != null) {
					foreach (Attr a; jattrs)
						a.generate(constPool);
				}
			}
			if( Kiev.safe && isBad() ) return;
		}
		constPool.generate(jenv);
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
			toBytecode(jenv, this, constPool);
	}

	private static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}

	private static void toBytecode(JEnv jenv, JStruct self, ConstPool constPool) {
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = "classes";
		String out_file = self.bname().replace('/',File.separatorChar).toString();
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
			byte[] dump = new Bytecoder(jenv,(Struct)self,null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+self.name+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
		}
	}
}

