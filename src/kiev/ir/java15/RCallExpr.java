package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RCallExpr of CallExpr extends RENode {

	static final AttrSlot ATTR = new DataAttrSlot("rcall temp expr",true,ENode.class);	

	public		ENode			obj;
	public:ro	SymbolRef		ident;
	public:ro	Method			func;
	public		CallType		mt;
	public:ro	NArr<ENode>		args;
	abstract
	public 		ENode			tmp_expr;
	
	@getter public final ENode get$tmp_expr() {
		return (ENode)this.getNodeData(ATTR);
	}
	@setter public final void set$tmp_expr(ENode e) {
		if (e != null)
			this.addNodeData(e, ATTR);
		else
			this.delNodeData(ATTR);
	}

	public void resolve(Type reqType) {
//		if (isResolved()) {
//			assert(func.parent instanceof Struct);
//			return;
//		}
		if (func.isStatic() && !(obj instanceof TypeRef))
			this.obj = new TypeRef(obj.getType());
		obj.resolve(null);
		func.makeArgs(args, reqType);
		if( func.id.equals(nameInit) && func.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null) {
			Method mmm = ctx_method;
			Type tp = mmm.ctx_clazz != func.ctx_clazz ? ctx_clazz.super_type : ctx_clazz.ctype;
			assert(ctx_method.id.equals(nameInit));
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null)
				tmp_expr = new LVarExpr(pos,mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO));
			else
				tmp_expr = ((RStruct)ctx_clazz).accessTypeInfoField((CallExpr)this,tp,false);
			tmp_expr.resolve(null);
			tmp_expr =  null;
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
				tmp_expr = ((RStruct)ctx_clazz).accessTypeInfoField((CallExpr)this,tp,false);
				tmp_expr.resolve(null);
				tmp_expr = null;
			}
		}
		if !(func.parent instanceof Struct) {
			ASTNode n = func.parent;
			while !(n instanceof Method) n = n.parent;
			assert (n.parent instanceof Struct);
			ident.symbol = ((Method)n).id;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RClosureCallExpr of ClosureCallExpr extends RENode {
	public		ENode			expr;
	public:ro	NArr<ENode>		args;
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

