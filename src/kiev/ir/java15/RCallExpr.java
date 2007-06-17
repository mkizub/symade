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

public final view RCallExpr of CallExpr extends RENode {

	public:ro	Method			func;
	public		ENode			obj;
	public:ro	TypeRef[]		targs;
	public:ro	ENode[]			args;
	public:ro	ENode[]			eargs;
	
	public final CallType getCallType();

	public void resolve(Type reqType) {
		obj.resolve(null);
		Method func = this.func;
		CallType mt = this.getCallType();
		func.makeArgs(args, mt);
		assert (!(func instanceof Constructor));
		if (func.isVarArgs()) {
			Type varg_tp = func.getVarArgParam().type.tvars[0].unalias().result();
			int i=0;
			for(; i < func.type.arity-1; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().type);
			} else {
				for(; i < args.length; i++)
					args[i].resolve(varg_tp);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
		}
		if (func.isTypeUnerasable()) {
			((CallExpr)this).eargs.delAll();
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType());
				ENode earg = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CallExpr)this,tp,false);
				((CallExpr)this).eargs += earg;
				earg.resolve(null);
			}
		}
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Method) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.open();
			this.symbol = (Method)n;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

public final view RCtorCallExpr of CtorCallExpr extends RENode {

	public:ro	Method			func;
	public:ro	ENode			obj;
	public		ENode			tpinfo;
	public:ro	ENode[]			args;
	public:ro	ENode[]			eargs;
	
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
			this.open();
			if (mmm.getTypeInfoParam(Var.PARAM_TYPEINFO) != null)
				tpinfo = new LVarExpr(pos,mmm.getTypeInfoParam(Var.PARAM_TYPEINFO));
			else
				tpinfo = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CallExpr)this,tp,false);
			tpinfo.resolve(null);
		}
		if (func.isVarArgs()) {
			Type varg_tp = func.getVarArgParam().type.tvars[0].unalias().result();
			int i=0;
			for(; i < func.type.arity-1; i++)
				args[i].resolve(func.type.arg(i));
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().type);
			} else {
				for(; i < args.length; i++)
					args[i].resolve(varg_tp);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(func.type.arg(i));
		}
		if (func.isTypeUnerasable()) {
			foreach (TypeDef td; func.targs) {
				Type tp = mt.resolve(td.getAType());
				ENode earg = ((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((CallExpr)this,tp,false);
				((CtorCallExpr)this).eargs += earg;
			}
		}
		foreach (ENode earg; eargs)
			earg.resolve(null);
		if !(func.parent() instanceof TypeDecl) {
			ANode n = func.parent();
			while !(n instanceof Constructor) n = n.parent();
			assert (n.parent() instanceof TypeDecl);
			this.open();
			this.symbol = (Constructor)n;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

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
		this.open();
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

