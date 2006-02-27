package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.BoolExpr.BoolExprImpl;
import kiev.vlang.BoolExpr.BoolExprView;
import kiev.vlang.BinaryBooleanOrExpr.BinaryBooleanOrExprImpl;
import kiev.vlang.BinaryBooleanOrExpr.BinaryBooleanOrExprView;
import kiev.vlang.BinaryBooleanAndExpr.BinaryBooleanAndExprImpl;
import kiev.vlang.BinaryBooleanAndExpr.BinaryBooleanAndExprView;
import kiev.vlang.BinaryBoolExpr.BinaryBoolExprImpl;
import kiev.vlang.BinaryBoolExpr.BinaryBoolExprView;
import kiev.vlang.InstanceofExpr.InstanceofExprImpl;
import kiev.vlang.InstanceofExpr.InstanceofExprView;
import kiev.vlang.BooleanNotExpr.BooleanNotExprImpl;
import kiev.vlang.BooleanNotExpr.BooleanNotExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RBoolExpr of BoolExprImpl extends BoolExprView {
}

@nodeview
public final view RBinaryBooleanOrExpr of BinaryBooleanOrExprImpl extends BinaryBooleanOrExprView {

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
public final view RBinaryBooleanAndExpr of BinaryBooleanAndExprImpl extends BinaryBooleanAndExprView {

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
public view RBinaryBoolExpr of BinaryBoolExprImpl extends BinaryBoolExprView {

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
		resolveExprs();
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==BinaryOperator.LessThen
			||   op==BinaryOperator.LessEquals
			||   op==BinaryOperator.GreaterThen
			||   op==BinaryOperator.GreaterEquals
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanOrExpr(pos,~expr1,~expr2));
				return;
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanAndExpr(pos,~expr1,~expr2));
				return;
			}
		}
		else if(
			(	(et1.isNumber() && et2.isNumber())
			 || (et1.isReference() && et2.isReference())
			 || (et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean))
			 || (et1.getStruct() != null && et1.getStruct().isEnum() && et2.isIntegerInCode())
			 || (et1.isIntegerInCode() && et2.getStruct() != null && et2.getStruct().isEnum())
			) &&
			(   op==BinaryOperator.Equals
			||  op==BinaryOperator.NotEquals
			)
		) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) ) {
				if( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{~expr1,~expr2}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,expr1,opt.method,new ENode[]{~expr2}));
				return;
			}
		}
		throw new CompilerException(this,"Unresolved expression "+this);
	}
	
	public:no,no,no,rw final void resolve2(Type reqType) {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( t1 ≉ t2 ) {
			if( t1.isReference() != t2.isReference()) {
				if (t1.getStruct() != null && t1.getStruct().isEnum() && !t1.isIntegerInCode()) {
					expr1 = new CastExpr(expr1.pos,Type.tpInt,~expr1);
					expr1.resolve(Type.tpInt);
					t1 = expr1.getType();
				}
				if (t2.getStruct() != null && t2.getStruct().isEnum() && !t2.isIntegerInCode()) {
					expr2 = new CastExpr(expr2.pos,Type.tpInt,~expr2);
					expr2.resolve(Type.tpInt);
					t2 = expr2.getType();
				}
				if( t1.isReference() != t2.isReference() && t1.isIntegerInCode() != t2.isIntegerInCode())
					throw new CompilerException(this,"Boolean operator on reference and non-reference types");
			}
			if( !t1.isReference() && !t2.isReference()) {
				Type t;
				if      (t1 ≡ Type.tpDouble || t2 ≡ Type.tpDouble ) t = Type.tpDouble;
				else if (t1 ≡ Type.tpFloat  || t2 ≡ Type.tpFloat  ) t = Type.tpFloat;
				else if (t1 ≡ Type.tpLong   || t2 ≡ Type.tpLong   ) t = Type.tpLong;
				else t = Type.tpInt;

				if( t ≢ t1 && t1.isCastableTo(t) ) {
					expr1 = new CastExpr(pos,t,~expr1);
					expr1.resolve(t);
				}
				if( t ≢ t2 && t2.isCastableTo(t) ) {
					expr2 = new CastExpr(pos,t,~expr2);
					expr2.resolve(t);
				}
			}
			if( t1.isReference() && t2.isReference()) {
				if (t1 ≢ Type.tpNull && t2 ≢ Type.tpNull) {
					if (!t1.isInstanceOf(t2) && !t2.isInstanceOf(t1))
						Kiev.reportWarning(this, "Operation "+op+" on uncomparable types "+t1+" and "+t2);
					if (t1.getStruct() != null && t1.getStruct().isStructView())
						Kiev.reportWarning(this, "Operation "+op+" on a view type "+t1);
					if (t2.getStruct() != null && t2.getStruct().isStructView())
						Kiev.reportWarning(this, "Operation "+op+" on a view type "+t2);
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public view RInstanceofExpr of InstanceofExprImpl extends InstanceofExprView {

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
		if (tp instanceof WrapperType)
			tp = tp.getEnclosedType();
		if (tp instanceof CompaundType) {
			CompaundType bt = (CompaundType)tp;
			if (tp.clazz.isTypeUnerasable()) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,
						ctx_clazz.getRView().accessTypeInfoField(this.getNode(),type.getType(), false),
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("$instanceof"),Type.tpBoolean,Type.tpObject),
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
public view RBooleanNotExpr of BooleanNotExprImpl extends BooleanNotExprView {

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr);
		if( expr.isConstantExpr() ) {
			replaceWithNode(new ConstBoolExpr(!((Boolean)expr.getConstValue()).booleanValue()));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

