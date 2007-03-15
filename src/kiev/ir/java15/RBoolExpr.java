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

public abstract view RBoolExpr of BoolExpr extends RENode {
}

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

public view RBinaryBoolExpr of BinaryBoolExpr extends RBoolExpr {
	public Operator			op;
	public ENode			expr1;
	public ENode			expr2;

	public void resolve(Type reqType) {
		if( isResolved() ) return;

		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (!ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
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
		if (expr.getType().isInstanceOf(tp) && !tp.isUnerasable()) {
			replaceWithNodeResolve(reqType,
				new BinaryBoolExpr(pos, Operator.NotEquals,~expr,new ConstNullExpr()));
			return;
		}
		while (tp instanceof CTimeType)
			tp = tp.getEnclosedType();
		if (tp.isUnerasable()) {
			replaceWithNodeResolve(reqType, new CallExpr(pos,
					((RStruct)(Struct)ctx_tdecl).accessTypeInfoField((InstanceofExpr)this,type.getType(), false),
					Type.tpTypeInfo.clazz.resolveMethod("$instanceof",Type.tpBoolean,Type.tpObject),
					new ENode[]{~expr}
					)
				);
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

public view RBooleanNotExpr of BooleanNotExpr extends RBoolExpr {
	public ENode		expr;

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		
		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (!ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
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

