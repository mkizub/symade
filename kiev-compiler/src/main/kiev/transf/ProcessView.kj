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

public final class ViewPlugin implements PluginFactory {
	public PluginDescr getPluginDescr(String name) {
		PluginDescr pd = null;
		if (name.equals("view")) {
			pd = new PluginDescr("view").depends("kiev");
			pd.proc(new ProcessorDescr("gen-members", "fe", 0, ViewFE_GenMembers.class).after("kiev:fe:pass3").before("kiev:fe:pre-resolve"));
			pd.proc(new ProcessorDescr("pre-generate", "me", 0, ViewME_PreGenerate.class).after("kiev:me:pre-generate").after("virt-fld:me:pre-generate"));
		}
		return pd;
	}
}

public final class ViewFE_GenMembers extends TransfProcessor {
	public ViewFE_GenMembers(Env env, int id) { super(env,id,KievExt.View); }
	public String getDescr() { "Class views members generation" }

	private Struct getViewImpl(TypeRef tr) {
		if (tr == null) return null;
		Struct clazz = tr.getStruct(env);
		if (clazz == null) return null;
		return getViewImpl(clazz);
	}
	private Struct getViewImpl(Struct clazz) {
		if !(clazz.isStructView())
			return null;
		return ((KievView)clazz).getViewImpl();
	}

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"ViewFE_GenMembers");
			try {
				doProcess(node);
			} finally { tr.leave(); }
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode dn; ss.members)
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
		UserMeta view_meta = (UserMeta)clazz.getMeta("kiev·stdlib·meta·ViewOf");

		if (view_meta != null && view_meta.getZ("iface"))
			clazz.mflags_is_struct_interface = true; //clazz.setInterface();
		
		// generate constructor
		{
			boolean ctor_found = false;
			foreach (Constructor dn; clazz.members; !dn.isStatic()) {
				ctor_found = true;
				break;
			}
			if (!ctor_found) {
				Constructor ctor = new Constructor(ACC_PUBLIC);
				ctor.params.append(new LVar(clazz.pos,nameImpl,view_of.getType(env),Var.VAR_LOCAL,ACC_FINAL|ACC_SYNTHETIC));
				ctor.body = new Block(clazz.pos);
				clazz.members.add(ctor);
			}
		}

		// add a cast from clazz.view_of to this view
		if (view_meta != null && view_meta.getZ("vcast")) {
			boolean cast_found = false;
			foreach (Method dn; view_of.getStruct(env).members) {
				if (dn.hasName(nameCastOp) && dn.mtype.ret() ≈ clazz.getType(env)) {
					cast_found = true;
					break;
				}
			}
			if (!cast_found) {
				Method cast = new MethodImpl("$cast", clazz.getType(env), ACC_PUBLIC|ACC_SYNTHETIC);
				cast.aliases += new OperatorAlias(nameCastOp, cast);
				if (clazz.isAbstract()) {
					cast.setAbstract(true);
				} else {
					cast.body = new Block();
				}
				view_of.getStruct(env).addMethod(cast);
			}
		}
		// add casts from this view to the clazz and a view instantiation cast
		{
			boolean cast_found = false;
			foreach (Method dn; clazz.members) {
				if (dn.hasName(nameCastOp) && dn.mtype.ret() ≈ view_of.getType(env)) {
					cast_found = true;
					break;
				}
			}
			if (!cast_found) {
				Method cast = new MethodImpl("$cast", view_of.getType(env), ACC_PUBLIC|ACC_SYNTHETIC|ACC_ABSTRACT);
				cast.aliases += new OperatorAlias(nameCastOp, cast);
				clazz.addMethod(cast);
				if (!clazz.isInterface()) {
					cast.setAbstract(false);
					cast.body = new Block();
				}
			}
			cast_found = false;
			foreach (Method dn; clazz.members) {
				if (dn.hasName("makeView") && dn.isStatic() && dn.mtype.ret() ≈ clazz.getType(env)) {
					cast_found = true;
					break;
				}
			}
			if (!cast_found) {
				Method cast = new MethodImpl("makeView", clazz.getType(env), ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC);
				cast.aliases += new OperatorAlias(nameCastOp, cast);
				cast.params.add(new LVar(0,"obj",view_of.getType(env),Var.VAR_LOCAL,ACC_FINAL));
				cast.body = new Block();
				clazz.addMethod(cast);
			}
		}
		
		clazz.setMembersGenerated(true);
	}
}

public final class ViewME_PreGenerate extends BackendProcessor implements Constants {
	public ViewME_PreGenerate(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "Class views pre-generation" }

	private Struct getViewImpl(TypeRef tr) {
		if (tr == null) return null;
		Struct clazz = tr.getStruct(env);
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
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"ViewME_PreGenerate");
			try {
				doProcess(node);
			} finally { tr.leave(); }
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode dn; ss.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members)
				this.doProcess(dn);
			return;
		}
		
		foreach (TypeRef st; clazz.super_types)
			doProcess(st.getStruct(env));
			
		if (clazz.isForward())
			return;
		Struct impl = getViewImpl(clazz);
		if (impl != null) {
			foreach (Constructor ctor; impl.members; ctor.body != null && (ctor.body instanceof NopExpr || ctor.block.stats.length > 0))
				return;
		}
		
		KievView kview = (KievView)clazz;
		TypeRef view_of = kview.view_of;
		Struct super_view_impl = null;

		// generate implementation
		UserMeta view_meta = (UserMeta)clazz.getMeta("kiev·stdlib·meta·ViewOf");
		if (view_meta != null && view_meta.getZ("iface")) {
			impl = this.env.newStruct(nameIFaceImpl,clazz,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC|ACC_FORWARD,new JavaClass(),null);
			if (clazz.isInterfaceOnly())
				impl.is_interface_only = true;
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
				impl.super_types += new TypeRef(super_view_impl.getType(env));
			else
				impl.super_types += new TypeRef(this.env.getTypeEnv().tpObject);
			impl.super_types += new TypeRef(clazz.getType(env));
			
			if (clazz.super_types[0].getType(env) ≉ this.env.getTypeEnv().tpObject)
				clazz.super_types.insert(0, new TypeRef(this.env.getTypeEnv().tpObject));
			clazz.members.append(~impl);

			CopyContext cc = new Copier();
			foreach (ASTNode dn; clazz.members) {
				if (dn instanceof Method && !(dn instanceof Constructor) && dn.isPublic() && !dn.isStatic()) {
					Method cm = (Method)dn;
					Method im = cc.copyRoot(cm);
					impl.members.add(im);
					cm.setFinal(false);
					cm.setPublic();
					cm.setAbstract(true);
					cm.body = null;
					continue;
				}
				else if (dn instanceof Field && !(dn.isStatic() && dn.isFinal())) {
					Field cf = (Field)dn;
					if (!cf.isPublic()) {
						Kiev.reportWarning(cf, "Field "+clazz+'.'+cf+" must be public");
						cf.setPublic();
					}
					Field f = cc.copyRoot(cf);
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
			if (super_view_impl != null && super_view_impl != clazz.super_types[0].getStruct(env))
				impl.super_types.insert(0, new TypeRef(super_view_impl.getType(env)));
		}
		
		// generate a field for the object this view represents
		Field fview = impl.resolveField(env, nameImpl, false);
		if (fview == null)
			fview = impl.addField(new Field(nameImpl,view_of.getType(env), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));

		foreach (Constructor ctor; impl.members; !ctor.isStatic()) {
			if (clazz.isInterfaceOnly()) {
				ctor.body = new NopExpr();
				continue;
			}
			Var pimpl = null;
			foreach (Var p; ctor.params; p.sname == nameImpl) {
				pimpl = p;
				break;
			}
			if (ctor.body == null)
				ctor.body = new Block();
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
						new AssignExpr(ctor.pos,
							new IFldExpr(ctor.pos,new ThisExpr(ctor.pos),fview),
							new LVarExpr(ctor.pos,pimpl)
						)
					)
				);
			}
		}

		// generate constructor
		if (clazz.isInterfaceOnly()) {
			Kiev.runProcessorsOn(impl);
			return;
		}

		// generate bridge methods
		foreach (Method m; impl.members) {
			if (m.isStatic() || m.isAbstract() || m.body != null)
				continue;
			CallType ct = m.mtype;
			Method vm;
			try {
				vm = view_of.getStruct(env).resolveMethod(env, m.sname, ct.ret(), ct.params());
			} catch (CompilerException e) {
				Kiev.reportError(m, e.getMessage());
				m.setAbstract(true);
				continue;
			}
			m.body = new Block(m.pos);
			CallExpr ce = new CallExpr(m.pos,
				new CastExpr(m.pos, view_of.getType(env),
					new IFldExpr(m.pos, new ThisExpr(m.pos), fview)
					),
				vm, ENode.emptyArray);
			foreach (Var fp; m.params)
				ce.args.append(new LVarExpr(fp.pos, fp));
			if (ct.ret() ≢ this.env.getTypeEnv().tpVoid)
				m.block.stats.add(new ReturnStat(m.pos, ce));
			else
				m.block.stats.add(new ExprStat(m.pos, ce));
		}
		
		// generate getter/setter methods
		foreach (Field f; impl.members; f != fview) {
			Method mv_set = f.getSetterMethod();
			assert (mv_set == null || mv_set.parent() == impl);
			if (mv_set != null && (mv_set.body == null || mv_set.body.isAutoGenerated())) {
				Method set_var = mv_set;
				Block body = new Block(f.pos);
				set_var.setFinal(true);
				set_var.body = body;
				Field view_fld = view_of.getType(env).getStruct().resolveField(env,f.sname);
				ENode val = new LVarExpr(f.pos,set_var.params[0]);
				if (f.getType(env).getAutoCastTo(view_fld.getType(env)) == null)
					val = new CastExpr(f.pos,view_fld.getType(env),val);
				AssignExpr ae = new AssignExpr(f.pos,
						new IFldExpr(f.pos,
							new CastExpr(f.pos,
								view_of.getType(env),
								new IFldExpr(f.pos,
									new ThisExpr(f.pos),
									fview
								)
							),
							view_fld
						),
						val
					);
				body.stats.append(new ExprStat(f.pos, ae));
				body.stats.append(new ReturnStat(f.pos,null));
				set_var.setAbstract(false);
			}
			Method mv_get = f.getGetterMethod();
			assert (mv_get == null || mv_get.parent() == impl);
			if (mv_get != null && (mv_get.body == null || mv_get.body.isAutoGenerated())) {
				Method get_var = mv_get;
				Block body = new Block(f.pos);
				get_var.setFinal(true);
				get_var.body = body;
				ENode val = new IFldExpr(f.pos,
					new CastExpr(f.pos,
						view_of.getType(env),
						new IFldExpr(f.pos,new ThisExpr(f.pos),fview)
					),
					view_of.getType(env).getStruct().resolveField(env,f.sname)
				);
				if (val.getType(env).getAutoCastTo(f.getType(env)) == null)
					val = new CastExpr(f.pos,f.getType(env),val);
				body.stats.add(new ReturnStat(f.pos,val));
				get_var.setAbstract(false);
			}
		}
		
		// implement the cast from clazz.view_of to this view
		assert (!view_of.getStruct(env).isResolved());
		foreach (Method dn; view_of.getStruct(env).members; !dn.isStatic()) {
			if (dn.hasName(nameCastOp) && dn.mtype.ret() ≈ clazz.getType(env)) {
				if (!dn.isAbstract() && dn.isSynthetic()) {
					ReturnStat rst = new ReturnStat(0, new NewExpr(0, impl.getType(env), new ENode[]{new ThisExpr()}));
					dn.block.stats.add(rst);
					Kiev.runProcessorsOn(rst);
				}
				break;
			}
		}
		// implement the cast from this view to the clazz
		foreach (Method dn; impl.members; !dn.isStatic()) {
			if (dn.hasName(nameCastOp) && dn.mtype.ret() ≈ view_of.getType(env)) {
				if (dn.isSynthetic()) {
					dn.setAbstract(false);
					dn.body = new Block();
					dn.block.stats.add(new ReturnStat(0, new CastExpr(0, view_of.getType(env), new IFldExpr(0, new ThisExpr(), fview))));
					Kiev.runProcessorsOn(dn.body);
				}
				break;
			}
		}
		// implement the view instantiation
		foreach (Method dn; impl.members; dn.isStatic()) {
			if (dn.hasName(nameCastOp) && dn.params.length == 1 && dn.mtype.ret() ≈ clazz.getType(env)) {
				if (dn.isSynthetic()) {
					ENode stat = new IfElseStat(0,
						new BinaryBoolExpr(0, env.coreFuncs.fObjectBoolEQ,new LVarExpr(0, dn.params[0]),new ConstNullExpr()),
						new ReturnStat(0, new ConstNullExpr()),
						new ReturnStat(0, new NewExpr(0, impl.getType(env), new ENode[]{new LVarExpr(0, dn.params[0])}))
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
