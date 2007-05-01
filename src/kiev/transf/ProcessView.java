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
package kiev.transf;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public class ViewFE_GenMembers extends TransfProcessor {
	private ViewFE_GenMembers() { super(KievExt.View); }
	public String getDescr() { "Class views members generation" }

	private Struct getViewImpl(TypeRef tr) {
		if (tr == null) return null;
		Struct clazz = tr.getStruct();
		if (clazz == null) return null;
		return getViewImpl(clazz);
	}
	private Struct getViewImpl(Struct clazz) {
		if !(clazz.isStructView())
			return null;
		return clazz.iface_impl;
	}

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members) {
				this.doProcess(dn);
			}
			return;
		}
		
		if (clazz.isForward() || getViewImpl(clazz) != null)
			return;
		
		clazz.meta.is_struct_interface = true; //clazz.setInterface();
		
		KievView kview = (KievView)clazz;
		TypeRef view_of = kview.view_of;

		// add a cast from clazz.view_of to this view
		boolean cast_found = false;
		foreach (Method dn; view_of.getStruct().members) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ clazz.xtype) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new MethodImpl("$cast", clazz.xtype, ACC_PUBLIC|ACC_SYNTHETIC);
			cast.aliases += new ASTOperatorAlias(nameCastOp);
			if (clazz.isAbstract()) {
				cast.setAbstract(true);
			} else {
				cast.body = new Block();
				cast.block.stats.add(new ReturnStat(0, new ConstBoolExpr()));
			}
			view_of.getStruct().addMethod(cast);
		}
		// add a cast from this view to the clazz
		cast_found = false;
		foreach (Method dn; clazz.members) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ view_of.getType()) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new MethodImpl("$cast", view_of.getType(), ACC_PUBLIC|ACC_SYNTHETIC|ACC_ABSTRACT);
			cast.aliases += new ASTOperatorAlias(nameCastOp);
			clazz.addMethod(cast);
		}
	}
}

@singleton
public class ViewME_PreGenerate extends BackendProcessor implements Constants {
	private ViewME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "Class views pre-generation" }

	private Struct getViewImpl(TypeRef tr) {
		if (tr == null) return null;
		Struct clazz = tr.getStruct();
		if (clazz == null) return null;
		return getViewImpl(clazz);
	}
	private Struct getViewImpl(Struct clazz) {
		if !(clazz.isStructView())
			return null;
		return clazz.iface_impl;
	}

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr);
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members)
				this.doProcess(dn);
			return;
		}
		
		if (clazz.isForward() || getViewImpl(clazz) != null)
			return;
		
		clazz = clazz.open();
		
		KievView kview = (KievView)clazz;
		TypeRef view_of = kview.view_of;

		// generate implementation
		Struct impl = Env.newStruct(nameIFaceImpl,true,clazz,ACC_PUBLIC|ACC_SYNTHETIC|ACC_FORWARD,new JavaClass(),true,null);
		impl = impl.open();
		impl.pos = clazz.pos;
		impl.setTypeDeclLoaded(true);
		if (clazz.isAbstract()) {
			clazz.setAbstract(false);
			impl.setAbstract(true);
		}
		if (clazz.isFinal()) {
			clazz.setFinal(false);
			impl.setFinal(true);
		}
		clazz.iface_impl = impl;
		{
			foreach (TypeRef st; clazz.super_types)
				doProcess(st.getStruct());
			Struct s = getViewImpl(clazz.super_types[0]);
			if (s != null)
				impl.super_types += new TypeRef(s.xtype);
			else
				impl.super_types += new TypeRef(Type.tpObject);
			impl.super_types += new TypeRef(clazz.xtype);
			
			if (clazz.super_types[0].getType() ≉ Type.tpObject)
				clazz.super_types.insert(0, new TypeRef(Type.tpObject));
		}
		clazz.members.append(impl);
		foreach (ASTNode dn; clazz.members) {
			if (dn instanceof Method && !(dn instanceof Constructor) && dn.isPublic() && !dn.isStatic()) {
				Method cm = dn;
				ENode b = cm.body;
				if (b != null)
					~b;
				Method m = cm.ncopy();
				m.setFinal(false);
				m.setPublic();
				m.setAbstract(true);
				impl.members.add(~cm);
				clazz.addMethod(m);
				if (b != null)
					cm.body = b;
				continue;
			}
			else if (dn instanceof DeclGroup && !(dn.meta.is_static && dn.meta.is_final)) {
				DeclGroup dg = dn;
				if (dg.meta.is_access != DNode.MASK_ACC_PUBLIC) {
					Kiev.reportWarning(dg, "Fields in "+clazz+" must be public");
					dg.setPublic();
				}
				DeclGroup d = dg.ncopy();
				foreach (Field f; d.decls)
					f.init = null;
				d.setPublic();
				d.setAbstract(true);
				impl.members.add(~dg);
				clazz.members.add(d);
				continue;
			}
			else if (dn instanceof Field && !(dn.isStatic() && dn.isFinal())) {
				Field cf = dn;
				if (!cf.isPublic()) {
					Kiev.reportWarning(cf, "Field "+clazz+'.'+cf+" must be public");
					cf.setPublic();
				}
				Field f = cf.ncopy();
				f.init = null;
				f.setPublic();
				f.setAbstract(true);
				impl.members.add(~cf);
				clazz.addField(f);
				continue;
			}
			if (dn instanceof Struct)
				continue;
			if (dn instanceof Method && !(dn instanceof Constructor) && !dn.isPublic() && !dn.isStatic()) {
				Kiev.reportWarning(dn, "Method "+clazz+'.'+dn+" must be public");
				dn.setPublic();
			}
			impl.members.add(~dn);
		}
		
		// generate a field for the object this view represents
		Field fview = impl.resolveField(nameImpl, false);
		if (fview == null)
			fview = impl.addField(new Field(nameImpl,view_of.getType(), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));

		// generate bridge methods
		foreach (Method m; impl.members) {
			if (m.isStatic() || m.isAbstract() || m.body != null)
				continue;
			CallType ct = m.type;
			Method vm;
			try {
				vm = view_of.getStruct().resolveMethod(m.u_name, ct.ret(), ct.params());
			} catch (CompilerException e) {
				Kiev.reportError(m, e.getMessage());
				m.setAbstract(true);
				continue;
			}
			m = m.open();
			m.body = new Block(m.pos);
			CallExpr ce = new CallExpr(m.pos,
				new CastExpr(m.pos, view_of.getType(),
					new IFldExpr(m.pos, new ThisExpr(m.pos), fview)
					),
				vm, ENode.emptyArray);
			foreach (Var fp; m.params)
				ce.args.append(new LVarExpr(fp.pos, fp));
			if (ct.ret() ≢ Type.tpVoid)
				m.block.stats.add(new ReturnStat(m.pos, ce));
			else
				m.block.stats.add(new ExprStat(m.pos, ce));
		}
		
		// generate getter/setter methods
		foreach (Field f; impl.getAllFields()) {
			Method mv_set = (Method)Field.SETTER_ATTR.get(f);
			if (mv_set != null && mv_set.isSynthetic()) {
				Method set_var = mv_set;
				Block body = new Block(f.pos);
				set_var = set_var.open();
				set_var.setFinal(true);
				set_var.body = body;
				Field view_fld = view_of.getType().getStruct().resolveField(f.sname);
				ENode val = new LVarExpr(f.pos,set_var.params[0]);
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,Operator.Assign,
						new IFldExpr(f.pos,
							new CastExpr(f.pos,
								view_of.getType(),
								new IFldExpr(f.pos,
									new ThisExpr(f.pos),
									fview
								)
							),
							view_fld
						),
						val
					)
				);
				body.stats.append(ass_st);
				body.stats.append(new ReturnStat(f.pos,null));
				if (f.getType().getAutoCastTo(view_fld.getType()) == null)
					val.replaceWith(fun ()->ASTNode { return new CastExpr(f.pos,view_fld.getType(),~val); });
				set_var.setAbstract(false);
			}
			Method mv_get = (Method)Field.GETTER_ATTR.get(f);
			if (mv_get != null && mv_get.isSynthetic()) {
				Method get_var = mv_get;
				Block body = new Block(f.pos);
				get_var = get_var.open();
				get_var.setFinal(true);
				get_var.body = body;
				ENode val = new IFldExpr(f.pos,
					new CastExpr(f.pos,
						view_of.getType(),
						new IFldExpr(f.pos,new ThisExpr(f.pos),fview)
					),
					view_of.getType().getStruct().resolveField(f.sname)
				);
				body.stats.add(new ReturnStat(f.pos,val));
				if (val.getType().getAutoCastTo(f.getType()) == null)
					val.replaceWith(fun ()->ASTNode { return new CastExpr(f.pos,f.getType(),~val); });
				get_var.setAbstract(false);
			}
		}
		
		// add a cast from clazz.view_of to this view
		assert (!view_of.getStruct().isResolved());
		foreach (Method dn; view_of.getStruct().members) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ clazz.xtype) {
				if (!dn.isAbstract() && dn.isSynthetic()) {
					ReturnStat rst = new ReturnStat(0, new NewExpr(0, impl.xtype, new ENode[]{new ThisExpr()}));
					dn.block.stats[0] = rst;
					Kiev.runProcessorsOn(rst);
				}
				break;
			}
		}
		// add a cast from this view to the clazz
		boolean cast_found = false;
		foreach (Method dn; impl.members) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ view_of.getType()) {
				if (dn.isSynthetic()) {
					dn = dn.open();
					dn.setAbstract(false);
					dn.body = new Block();
					dn.block.stats.add(new ReturnStat(0, new CastExpr(0, view_of.getType(), new IFldExpr(0, new ThisExpr(), fview))));
				}
				break;
			}
		}

		Kiev.runProcessorsOn(impl);
	}
	
}
