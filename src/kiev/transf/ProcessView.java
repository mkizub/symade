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
		return ((KievView)clazz).getViewImpl();
	}

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members) {
				this.doProcess(dn);
			}
			return;
		}
		
		if (clazz.isForward() || clazz.isMembersGenerated())
			return;
		
		KievView kview = (KievView)clazz;
		TypeRef view_of = kview.view_of;
		UserMeta view_meta = (UserMeta)clazz.getMeta("kiev\u001fstdlib\u001fmeta\u001fViewOf");

		if (view_meta != null && view_meta.getZ("iface"))
			clazz.meta.is_struct_interface = true; //clazz.setInterface();
		
		// generate constructor
		{
			Constructor ctor = new Constructor(ACC_PUBLIC);
			ctor.params.append(new LVar(clazz.pos,nameImpl,view_of.getType(),Var.PARAM_NORMAL,ACC_FINAL|ACC_SYNTHETIC));
			ctor.body = new Block(clazz.pos);
			clazz.members.add(ctor);
		}

		// add a cast from clazz.view_of to this view
		if (view_meta != null && view_meta.getZ("vcast")) {
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
				}
				view_of.getStruct().addMethod(cast);
			}
		}
		// add casts from this view to the clazz and a view instantiation cast
		{
			boolean cast_found = false;
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
			cast_found = false;
			foreach (Method dn; clazz.members) {
				if (dn.hasName("makeView",true) && dn.isStatic() && dn.type.ret() ≈ clazz.xtype) {
					cast_found = true;
					break;
				}
			}
			if (!cast_found) {
				Method cast = new MethodImpl("makeView", clazz.xtype, ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC);
				cast.aliases += new ASTOperatorAlias(nameCastOp);
				cast.params.add(new LVar(0,"obj",view_of.getType(),Var.PARAM_NORMAL,ACC_FINAL));
				cast.body = new Block();
				clazz.addMethod(cast);
			}
		}
		
		clazz.setMembersGenerated(true);
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
		return ((KievView)clazz).getViewImpl();
	}

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"ViewME_PreGenerate");
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}

	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members)
				this.doProcess(dn);
			return;
		}
		
		foreach (TypeRef st; clazz.super_types)
			doProcess(st.getStruct());
			
		if (clazz.isForward())
			return;
		Struct impl = getViewImpl(clazz);
		if (impl != null) {
			foreach (Constructor ctor; impl.members; ctor.block.stats.length > 0)
				return;
		}
		
		KievView kview = (KievView)clazz;
		TypeRef view_of = kview.view_of;
		Struct super_view_impl = null;

		// generate implementation
		UserMeta view_meta = (UserMeta)clazz.getMeta("kiev\u001fstdlib\u001fmeta\u001fViewOf");
		if (view_meta != null && view_meta.getZ("iface")) {
			impl = Env.getRoot().newStruct(nameIFaceImpl,true,clazz,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC|ACC_FORWARD,new JavaClass(),true,null);
			impl.pos = clazz.pos;
			if (clazz.isAbstract()) {
				clazz.setAbstract(false);
				impl.setAbstract(true);
			}
			if (clazz.isFinal()) {
				clazz.setFinal(false);
				impl.setFinal(true);
			}
			clazz.iface_impl = impl;
			super_view_impl = getViewImpl(clazz.super_types[0]);
			if (super_view_impl != null)
				impl.super_types += new TypeRef(super_view_impl.xtype);
			else
				impl.super_types += new TypeRef(Type.tpObject);
			impl.super_types += new TypeRef(clazz.xtype);
			
			if (clazz.super_types[0].getType() ≉ Type.tpObject)
				clazz.super_types.insert(0, new TypeRef(Type.tpObject));
			clazz.members.append(impl);

			ASTNode.CopyContext cc = new ASTNode.CopyContext();
			foreach (ASTNode dn; clazz.members) {
				if (dn instanceof Method && !(dn instanceof Constructor) && dn.isPublic() && !dn.isStatic()) {
					Method cm = dn;
					Method im = cm.ncopy(cc);
					impl.members.add(im);
					cm.setFinal(false);
					cm.setPublic();
					cm.setAbstract(true);
					cm.body = null;
					continue;
				}
				else if (dn instanceof Field && !(dn.isStatic() && dn.isFinal())) {
					Field cf = dn;
					if (!cf.isPublic()) {
						Kiev.reportWarning(cf, "Field "+clazz+'.'+cf+" must be public");
						cf.setPublic();
					}
					Field f = cf.ncopy(cc);
					cf.init = null;
					cf.setPublic();
					cf.setAbstract(true);
					impl.members.add(f);
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
			cc.updateLinks();
		} else {
			impl = clazz;
			super_view_impl = getViewImpl(clazz.super_types[0]);
			if (super_view_impl != null && super_view_impl != clazz.super_types[0].getStruct())
				impl.super_types.insert(0, new TypeRef(super_view_impl.xtype));
		}
		
		// generate a field for the object this view represents
		Field fview = impl.resolveField(nameImpl, false);
		if (fview == null)
			fview = impl.addField(new Field(nameImpl,view_of.getType(), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));

		// generate constructor
		boolean ctor_found = false;
		foreach (Constructor ctor; impl.members; !ctor.isStatic()) {
			Var pimpl = null;
			foreach (Var p; ctor.params; p.sname == nameImpl) {
				pimpl = p;
				break;
			}
			if (super_view_impl != null) {
				CtorCallExpr ctor_call = new CtorCallExpr(clazz.pos, new SuperExpr(), ENode.emptyArray);
				ctor_call.args.insert(0,new LVarExpr(clazz.pos, pimpl));
				ctor.block.stats.insert(0,new ExprStat(ctor_call));
			} else {
				assert (fview.parent() == impl);
				CtorCallExpr ctor_call = new CtorCallExpr(clazz.pos, new SuperExpr(), ENode.emptyArray);
				ctor.block.stats.insert(0,new ExprStat(ctor_call));
				ctor.block.stats.insert(1,
					new ExprStat(ctor.pos,
						new AssignExpr(ctor.pos,Operator.Assign,
							new IFldExpr(ctor.pos,new ThisExpr(ctor.pos),fview),
							new LVarExpr(ctor.pos,pimpl)
						)
					)
				);
			}
			ctor_found = true;
		}
		assert (ctor_found);

		// generate bridge methods
		foreach (Method m; impl.members) {
			if (m.isStatic() || m.isAbstract() || m.body != null)
				continue;
			CallType ct = m.type;
			Method vm;
			try {
				vm = view_of.getStruct().resolveMethod(m.sname, ct.ret(), ct.params());
			} catch (CompilerException e) {
				Kiev.reportError(m, e.getMessage());
				m.setAbstract(true);
				continue;
			}
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
		foreach (Field f; impl.members; f != fview) {
			Method mv_set = f.getSetterMethod();
			assert (mv_set == null || mv_set.parent() == impl);
			if (mv_set != null && mv_set.isSynthetic()) {
				Method set_var = mv_set;
				Block body = new Block(f.pos);
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
			Method mv_get = f.getGetterMethod();
			assert (mv_get == null || mv_get.parent() == impl);
			if (mv_get != null && mv_get.isSynthetic()) {
				Method get_var = mv_get;
				Block body = new Block(f.pos);
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
		
		// implement the cast from clazz.view_of to this view
		assert (!view_of.getStruct().isResolved());
		foreach (Method dn; view_of.getStruct().members; !dn.isStatic()) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ clazz.xtype) {
				if (!dn.isAbstract() && dn.isSynthetic()) {
					ReturnStat rst = new ReturnStat(0, new NewExpr(0, impl.xtype, new ENode[]{new ThisExpr()}));
					dn.block.stats.add(rst);
					Kiev.runProcessorsOn(rst);
				}
				break;
			}
		}
		// implement the cast from this view to the clazz
		foreach (Method dn; impl.members; !dn.isStatic()) {
			if (dn.hasName(nameCastOp,true) && dn.type.ret() ≈ view_of.getType()) {
				if (dn.isSynthetic()) {
					dn.setAbstract(false);
					dn.body = new Block();
					dn.block.stats.add(new ReturnStat(0, new CastExpr(0, view_of.getType(), new IFldExpr(0, new ThisExpr(), fview))));
					Kiev.runProcessorsOn(dn.body);
				}
				break;
			}
		}
		// implement the view instantiation
		foreach (Method dn; impl.members; dn.isStatic()) {
			if (dn.hasName(nameCastOp,true) && dn.params.length == 1 && dn.type.ret() ≈ clazz.xtype) {
				if (dn.isSynthetic()) {
					ENode stat = new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.Equals,new LVarExpr(0, dn.params[0]),new ConstNullExpr()),
						new ReturnStat(0, new ConstNullExpr()),
						new ReturnStat(0, new NewExpr(0, impl.xtype, new ENode[]{new LVarExpr(0, dn.params[0])}))
						);
					dn.block.stats.add(stat);
					Kiev.runProcessorsOn(stat);
				}
				break;
			}
		}

		Kiev.runProcessorsOn(impl);
	}
	
}
