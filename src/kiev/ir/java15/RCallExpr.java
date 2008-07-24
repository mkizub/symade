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

@ViewOf(vcast=true, iface=true)
public final view RCallExpr of CallExpr extends RENode {

	public:ro	Method			func;
	public		ENode			obj;
	public:ro	TypeRef[]		targs;
	public:ro	ENode[]			args;
	
	public final CallType getCallType();

	public void resolve(Type reqType) {
		obj.resolve(null);
		Method func = this.func;
		CallType mt = this.getCallType();
		func.makeArgs(args, mt);
		assert (!(func instanceof Constructor));
		if (func.isRuleMethod()) {
			// Very special case for rule call from inside of RuleMethod
			ENode env_arg = new ConstNullExpr();
			ANode p = this.parent();
			if (p instanceof AssignExpr && p.op == Operator.Assign && p.lval.getType() ≡ StdTypes.tpRule)
				env_arg = p.lval.ncopy();
			CallExpr.RULE_ENV_ARG.set(this,env_arg);
		}
		if (func.isVarArgs()) {
			Type tn = func.getVarArgParam().getType();
			Type varg_tp = tn.resolveArg(0);
			int i=0;
			for(; i < func.mtype.arity-1; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.mtype.arg(i)));
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().getType());
			} else {
				for(; i < args.length; i++)
					args[i].resolve(varg_tp);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.mtype.arg(i)));
		}
		if (func.isTypeUnerasable()) {
			CallExpr ce = (CallExpr)this;
			foreach (ANode earg; CallExpr.TI_EXT_ARG.iterate(ce))
				earg.detach();
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType());
				ENode earg = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField(ce,tp,false);
				CallExpr.TI_EXT_ARG.add(ce,earg);
				earg.resolve(null);
			}
		}
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Method) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.symbol = (Method)n;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RCtorCallExpr of CtorCallExpr extends RENode {

	public:ro	Method			func;
	public:ro	ENode			obj;
	public		ENode			tpinfo;
	public:ro	ENode[]			args;
	
	public final CallType getCallType();

	public void resolve(Type reqType) {
		Method func = func;
		CallType mt = this.getCallType();
		func.makeArgs(args, mt);
		assert (func instanceof Constructor);
		if (func.getTypeInfoParam(Var.PARAM_TYPEINFO) != null) {
			Method mmm = ctx_method;
			Type tp = mmm.ctx_tdecl != func.ctx_tdecl ? ctx_tdecl.super_types[0].getType() : ctx_tdecl.xtype;
			assert(ctx_method instanceof Constructor && !ctx_method.isStatic());
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam(Var.PARAM_TYPEINFO) != null)
				tpinfo = new LVarExpr(pos,mmm.getTypeInfoParam(Var.PARAM_TYPEINFO));
			else
				tpinfo = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CtorCallExpr)this,tp,false);
			tpinfo.resolve(null);
		}
		if (func.isVarArgs()) {
			Type tn = func.getVarArgParam().getType();
			Type varg_tp = tn.resolveArg(0);
			int i=0;
			for(; i < func.mtype.arity-1; i++)
				args[i].resolve(func.mtype.arg(i));
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().getType());
			} else {
				for(; i < args.length; i++)
					args[i].resolve(varg_tp);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(func.mtype.arg(i));
		}
		if (func.isTypeUnerasable()) {
			CtorCallExpr ce = (CtorCallExpr)this;
			foreach (ANode earg; CtorCallExpr.TI_EXT_ARG.iterate(ce))
				earg.detach();
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType());
				ENode earg = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField(ce,tp,false);
				CtorCallExpr.TI_EXT_ARG.add(ce,earg);
				earg.resolve(null);
			}
			foreach (ENode earg; CtorCallExpr.TI_EXT_ARG.iterate(ce))
				earg.resolve(null);
		}
		{
			CtorCallExpr ce = (CtorCallExpr)this;
			foreach (ENode earg; CtorCallExpr.ENUM_EXT_ARG.iterate(ce))
				earg.resolve(null);
		}
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Constructor) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.symbol = (Constructor)n;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@ViewOf(vcast=true, iface=true)
public final view RClosureCallExpr of ClosureCallExpr extends RENode {
	public		ENode			expr;
	public:ro	ENode[]			args;
	public		Boolean			is_a_call;

	public Method getCallIt(CallType tp);
	
	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		expr.resolve(null);
		Type extp = expr.getType();
		if !(extp instanceof CallType)
			throw new CompilerException(expr,"Expression "+expr+" is not a closure");
		CallType tp = (CallType)extp;
		if( reqType != null && reqType instanceof CallType )
			is_a_call = Boolean.FALSE;
		else if( (reqType == null || !(reqType instanceof CallType)) && tp.arity==args.length )
			is_a_call = Boolean.TRUE;
		else
			is_a_call = Boolean.FALSE;
		for(int i=0; i < args.length; i++)
			args[i].resolve(tp.arg(i));
		Method call_it = getCallIt(tp);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

