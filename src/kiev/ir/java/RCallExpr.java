package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.CallExpr.CallExprView;
import kiev.vlang.ClosureCallExpr.ClosureCallExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RCallExpr of CallExpr extends CallExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if (func.isStatic() && !(obj instanceof TypeRef))
			this.obj = new TypeRef(obj.getType());
		obj.resolve(null);
		func.makeArgs(args, reqType);
		if( func.name.equals(nameInit) && func.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null) {
			Method mmm = ctx_method;
			Type tp = mmm.ctx_clazz != func.ctx_clazz ? ctx_clazz.super_type : ctx_clazz.ctype;
			assert(ctx_method.name.equals(nameInit));
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null)
				temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO));
			else
				temp_expr = ((RStruct)ctx_clazz).accessTypeInfoField((CallExpr)this,tp,false);
			temp_expr.resolve(null);
			temp_expr = null;
		}
		if (func.isVarArgs()) {
			int i=0;
			for(; i < func.type.arity; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
			if (args.length == i+1 && args[i].getType().isInstanceOf(func.getVarArgParam().type)) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().type);
			} else {
				ArrayType varg_tp = (ArrayType)Type.getRealType(obj.getType(),func.getVarArgParam().type);
				for(; i < args.length; i++)
					args[i].resolve(varg_tp.arg);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
		}
		if (func.isTypeUnerasable()) {
			TypeDef[] targs = func.targs.toArray();
			for (int i=0; i < targs.length; i++) {
				Type tp = mt.resolve(targs[i].getAType());
				temp_expr = ((RStruct)ctx_clazz).accessTypeInfoField((CallExpr)this,tp,false);
				temp_expr.resolve(null);
			}
			temp_expr = null;
		}
		if !(func.parent_node instanceof Struct) {
			ASTNode n = func.parent_node;
			while !(n instanceof Method) n = n.parent_node;
			assert (n.parent_node instanceof Struct);
			func = (Method)n;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RClosureCallExpr of ClosureCallExpr extends ClosureCallExprView {

	public Method getCallIt(CallType tp) {
		KString call_it_name;
		Type ret;
		if( tp.ret().isReference() ) {
			call_it_name = KString.from("call_Object");
			ret = Type.tpObject;
		} else {
			call_it_name = KString.from("call_"+tp.ret());
			ret = tp.ret();
		}
		return Type.tpClosureClazz.resolveMethod(call_it_name, ret);
	}
	
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

