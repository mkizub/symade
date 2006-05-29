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
public abstract view RBoolExpr of BoolExpr extends RENode {
}

@nodeview
public final view RBinaryBooleanOrExpr of BinaryBooleanOrExpr extends RBoolExpr {
	public ENode		expr1;
	public ENode		expr2;

	public void resolve(Type reqType) {
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		getDFlow().out();
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RBinaryBooleanAndExpr of BinaryBooleanAndExpr extends RBoolExpr {
	public ENode		expr1;
	public ENode		expr2;

	public void resolve(Type reqType) {
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public view RBinaryBoolExpr of BinaryBoolExpr extends RBoolExpr {
	public BinaryOperator	op;
	public ENode			expr1;
	public ENode			expr2;

	public:no,no,no,rw final boolean resolveExprs() {
		expr1.resolve(null);
		if (!expr1.isForWrapper() && expr1.getType() instanceof CTimeType) {
			expr1 = expr1.getType().makeUnboxedExpr(expr1);
			expr1.resolve(null);
		}

		expr2.resolve(null);
		if( expr2 instanceof TypeRef )
			getExprByStruct(((TypeRef)expr2).getType().getStruct());
		expr2.resolve(null);
		if (!expr2.isForWrapper() && expr2.getType() instanceof CTimeType) {
			expr2 = expr2.getType().makeUnboxedExpr(expr2);
			expr2.resolve(null);
		}
		return true;
	}

	public:no,no,no,rw final void getExprByStruct(Struct cas) {
		if( cas.isPizzaCase() ) {
			if( !(op==BinaryOperator.Equals || op==BinaryOperator.NotEquals) )
				throw new CompilerException(this,"Undefined operation "+op.image+" on cased class");
//			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
//			expr2 = new ConstIntExpr(ca.caseno);
			MetaPizzaCase meta = cas.getMetaPizzaCase();
			expr2 = new ConstIntExpr(meta.getTag());
			expr2.resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp instanceof CTimeType) {
				tp.makeUnboxedExpr(expr1);
				expr1.resolve(null);
				tp = expr1.getType();
			}
			if (tp.getStruct() == null || (!tp.getStruct().isPizzaCase() && !tp.getStruct().isHasCases()))
				throw new CompilerException(this,"Compare non-cased class "+tp+" with class's case "+cas);
			Method m = tp.getStruct().resolveMethod(nameGetCaseTag,Type.tpInt);
			expr1 = new CallExpr(expr1.pos,~expr1,m,ENode.emptyArray);
			expr1.resolve(Type.tpInt);
		} else {
			throw new CompilerException(this,"Class "+cas+" is not a cased class");
		}
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;

		if (ident == null)
			ident = new SymbolRef(pos, op.name);
		if (ident.symbol == null) {
			Method m = op.resolveMethod(this);
			if (m == null) {
				Kiev.reportError(this, "Unresolved method for operator "+op);
				return;
			}
			if (m instanceof CoreMethod && m.core_func != null) {
				try {
					m.normilizeExpr(this);
				} catch (ReWalkNodeException rne) {
					((ENode)rne.replacer).resolve(reqType);
					return;
				}
			} else {
				ident.symbol = m;
			}
		}
		Method m = (Method)ident.symbol;
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			expr1.resolve(m.params[0].getType());
			expr2.resolve(m.params[1].getType());
		} else {
			m.makeArgs(new ENode[]{expr2},reqType);
			expr1.resolve(((TypeDecl)m.parent()).xtype);
			expr2.resolve(m.params[0].getType());
		}
		if !(m instanceof CoreMethod) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~expr1,~expr2}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~expr1,m,new ENode[]{~expr2}));
			return;
		}
		// Check if both expressions are constant
		if( expr1.isConstantExpr() && expr2.isConstantExpr() ) {
			ConstExpr ce = ((CoreMethod)m).calc(this);
			replaceWithNodeResolve(reqType, ce);
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public view RInstanceofExpr of InstanceofExpr extends RBoolExpr {
	public ENode	expr;
	public TypeRef	type;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr.resolve(null);
		Type tp = null;
		if( expr instanceof TypeRef )
			tp = ((TypeRef)expr).getType();
		if( tp != null ) {
			replaceWithNode(new ConstBoolExpr(tp.isInstanceOf(type.getType())));
			return;
		} else {
			Type et = expr.getType();
			if (!expr.isForWrapper() && et instanceof CTimeType) {
				expr = et.makeUnboxedExpr(expr);
				expr.setForWrapper(true);
				expr.resolve(null);
			}
		}
		tp = type.getType();
		if( !expr.getType().isCastableTo(tp) ) {
			throw new CompilerException(this,"Type "+expr.getType()+" is not castable to "+type);
		}
		if (expr.getType().isInstanceOf(tp)) {
			replaceWithNodeResolve(reqType,
				new BinaryBoolExpr(pos, BinaryOperator.NotEquals,~expr,new ConstNullExpr()));
			return;
		}
		while (tp instanceof CTimeType)
			tp = tp.getEnclosedType();
		if (tp instanceof CompaundType) {
			CompaundType bt = (CompaundType)tp;
			if (tp.clazz.isTypeUnerasable()) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,
						((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((InstanceofExpr)this,type.getType(), false),
						Type.tpTypeInfo.clazz.resolveMethod("$instanceof",Type.tpBoolean,Type.tpObject),
						new ENode[]{~expr}
						)
					);
				return;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public view RBooleanNotExpr of BooleanNotExpr extends RBoolExpr {
	public ENode		expr;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		
		if (ident == null)
			ident = new SymbolRef(pos, getOp().name);
		if (ident.symbol == null) {
			Method m = getOp().resolveMethod(this);
			if (m == null) {
				Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
			if (m instanceof CoreMethod && m.core_func != null) {
				try {
					m.normilizeExpr(this);
				} catch (ReWalkNodeException rne) {
					((ENode)rne.replacer).resolve(reqType);
					return;
				}
			} else {
				ident.symbol = m;
			}
		}
		Method m = (Method)ident.symbol;
		if (m.isStatic()) {
			m.makeArgs(getArgs(),reqType);
			expr.resolve(m.params[0].getType());
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			expr.resolve(((TypeDecl)m.parent()).xtype);
		}
		if !(m instanceof CoreMethod) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(reqType, new CallExpr(pos,null,m,new ENode[]{~expr}));
			else
				replaceWithNodeResolve(reqType, new CallExpr(pos,~expr,m,ENode.emptyArray));
			return;
		}
		BoolExpr.checkBool(expr);
		// Check if expression is a constant
		if (expr.isConstantExpr()) {
			ConstExpr ce = ((CoreMethod)m).calc(this);
			replaceWithNodeResolve(reqType, ce);
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

