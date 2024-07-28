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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ViewOf(vcast=true)
public final view RCallExpr of CallExpr extends RENode {

	public:ro	Method			func;
	public		ENode			obj;
	public:ro	TypeRef[]		targs;
	public:ro	ENode[]			args;
	
	public final CallType getCallType(Env env);

	public void resolveENode(Type reqType, Env env) {
		resolveENode(obj,null,env);
		Method func = this.func;
		CallType mt = this.getCallType(env);
		func.makeArgs(args, mt);
		assert (!(func instanceof Constructor));
		if (func.isRuleMethod()) {
			// Very special case for rule call from inside of RuleMethod
			ENode env_arg = new ConstNullExpr();
			ANode p = this.parent();
			if (p instanceof AssignExpr && p.lval.getType(env) ≡ env.tenv.tpRule)
				env_arg = new Copier().copyFull(p.lval);
			((CallExpr)this).setHiddenArg(new ArgExpr(func.getHiddenParam(Var.PARAM_RULE_ENV),env_arg));
		}
		if (func.isVarArgs()) {
			Type tn = func.getVarArgParam().getType(env);
			Type varg_tp = tn.resolveArg(0);
			int i=0;
			for(; i < func.mtype.arity-1; i++)
				resolveENode(args[i],Type.getRealType(obj.getType(env),func.mtype.arg(i)),env);
			if (args.length == i+1 && args[i].getType(env).isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				resolveENode(args[i],func.getVarArgParam().getType(env),env);
			} else {
				for(; i < args.length; i++)
					resolveENode(args[i],varg_tp,env);
			}
		} else {
			for (int i=0; i < args.length; i++)
				resolveENode(args[i],Type.getRealType(obj.getType(env),func.mtype.arg(i)),env);
		}
		if (func.isTypeUnerasable()) {
			CallExpr ce = (CallExpr)this;
			foreach (ArgExpr earg; ce.hargs; earg.var.kind >= Var.PARAM_METHOD_TYPEINFO)
				earg.detach();
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType(env));
				ENode earg = new TypeInfoExpr(tp);
				Var param = func.getMethodTypeInfoParam(td.sname);
				ce.setHiddenArg(new ArgExpr(param,earg));
				resolveENode(earg,null,env);
			}
		}
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Method) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.symbol = ((Method)n).symbol;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RCtorCallExpr of CtorCallExpr extends RENode {

	public:ro	Method			func;
	public:ro	ENode			obj;
	public		ENode			tpinfo;
	public:ro	ENode[]			args;
	
	public final CallType getCallType(Env env);

	public void resolveENode(Type reqType, Env env) {
		Method func = func;
		CallType mt = this.getCallType(env);
		func.makeArgs(args, mt);
		assert (func instanceof Constructor);
		if (func.getClassTypeInfoParam() != null) {
			Method mmm = ctx_method;
			Type tp = Env.ctxTDecl(mmm) != Env.ctxTDecl(func) ? ctx_tdecl.super_types[0].getType(env) : ctx_tdecl.getType(env);
			assert(ctx_method instanceof Constructor && !ctx_method.isStatic());
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getClassTypeInfoParam() != null)
				tpinfo = new LVarExpr(pos,mmm.getClassTypeInfoParam());
			else
				tpinfo = new TypeInfoExpr(tp);
			resolveENode(tpinfo,null,env);
		}
		if (func.isVarArgs()) {
			Type tn = func.getVarArgParam().getType(env);
			Type varg_tp = tn.resolveArg(0);
			int i=0;
			for(; i < func.mtype.arity-1; i++)
				resolveENode(args[i],func.mtype.arg(i),env);
			if (args.length == i+1 && args[i].getType(env).isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				resolveENode(args[i],func.getVarArgParam().getType(env),env);
			} else {
				for(; i < args.length; i++)
					resolveENode(args[i],varg_tp,env);
			}
		} else {
			for (int i=0; i < args.length; i++)
				resolveENode(args[i],func.mtype.arg(i),env);
		}
		if (func.isTypeUnerasable()) {
			CtorCallExpr ce = (CtorCallExpr)this;
			foreach (ArgExpr earg; ce.hargs; earg.var.kind >= Var.PARAM_METHOD_TYPEINFO)
				earg.detach();
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType(env));
				ENode earg = new TypeInfoExpr(tp);
				Var param = func.getMethodTypeInfoParam(td.sname);
				ce.setHiddenArg(new ArgExpr(param,earg));
				resolveENode(earg,null,env);
			}
		}
		{
			CtorCallExpr ce = (CtorCallExpr)this;
			foreach (ArgExpr ae; ce.hargs)
				resolveENode(ae,null,env);
		}
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Constructor) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.symbol = ((Constructor)n).symbol;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RClosureCallExpr of ClosureCallExpr extends RENode {
	public		ENode			expr;
	public:ro	ENode[]			args;
	public		ClosureCallKind	kind;

	public boolean isKindAuto();
	public Method getCallIt(CallType tp, Env env);
	
	public void resolveENode(Type reqType, Env env) throws RuntimeException {
		if( isResolved() ) return;
		resolveENode(expr,null,env);
		Type extp = expr.getType(env);
		if !(extp instanceof CallType)
			throw new CompilerException(expr,"Expression "+expr+" is not a closure");
		CallType tp = (CallType)extp;
		if (isKindAuto()) {
			if( reqType != null && reqType instanceof CallType )
				kind = ClosureCallKind.CURRY;
			else if( (reqType == null || !(reqType instanceof CallType)) && tp.arity==args.length )
				kind = ClosureCallKind.CALL;
			else
				kind = ClosureCallKind.CURRY;
		}
		for(int i=0; i < args.length; i++)
			resolveENode(args[i],tp.arg(i),env);
		Method call_it = getCallIt(tp,env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

