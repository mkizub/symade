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
public abstract view RBoolExpr of BoolExpr extends RENode {

	public static void checkBool(ENode e, Env env) {
		Type et = e.getType(env);
		if (et.isBoolean())
			return;
		if (et ≡ env.tenv.tpRule) {
			((RENode)e).replaceWithResolve(env, env.tenv.tpBoolean, fun ()->ENode {
				return new BinaryBoolExpr(e.pos,env.coreFuncs.fObjectBoolNE,e,new ConstNullExpr());
			});
			return;
		}
		if (et instanceof CallType && e instanceof ClosureCallExpr && e.isKindAuto()) {
			CallType ct = (CallType)et;
			if (ct.arity == 0 && ct.ret().getAutoCastTo(env.tenv.tpBoolean) != null) {
				((ClosureCallExpr)e).kind = ClosureCallKind.CALL;
				return;
			}
		}
		throw new RuntimeException("Expression "+e+" must be of boolean type, but found "+e.getType(env));
	}
	
}

@ViewOf(vcast=true)
public final view RBinaryBooleanOrExpr of BinaryBooleanOrExpr extends RBoolExpr {
	public ENode		expr1;
	public ENode		expr2;

	public void resolveENode(Type reqType, Env env) {
		resolveENode(expr1,env.tenv.tpBoolean,env);
		RBoolExpr.checkBool(expr1, env);
		resolveENode(expr2,env.tenv.tpBoolean,env);
		RBoolExpr.checkBool(expr2, env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public final view RBinaryBooleanAndExpr of BinaryBooleanAndExpr extends RBoolExpr {
	public ENode		expr1;
	public ENode		expr2;

	public void resolveENode(Type reqType, Env env) {
		resolveENode(expr1,env.tenv.tpBoolean,env);
		RBoolExpr.checkBool(expr1, env);
		resolveENode(expr2,env.tenv.tpBoolean,env);
		RBoolExpr.checkBool(expr2, env);
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public view RBinaryBoolExpr of BinaryBoolExpr extends RBoolExpr {
	public ENode			expr1;
	public ENode			expr2;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;

		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if (m.isStatic()) {
			m.makeArgs(getEArgs(),reqType);
			resolveENode(expr1,m.params[0].getType(env),env);
			resolveENode(expr2,m.params[1].getType(env),env);
		} else {
			m.makeArgs(new ENode[]{expr2},reqType);
			resolveENode(expr1,((TypeDecl)m.parent()).getType(env),env);
			resolveENode(expr2,m.params[0].getType(env),env);
		}
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~expr1,~expr2}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~expr1,m,new ENode[]{~expr2}));
			return;
		}
		// Check if both expressions are constant
		if (isConstantExpr(env)) {
			replaceWithNodeResolve(env, reqType, ((CoreOperation)m).calc(this));
			return;
		}
		
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public view RInstanceofExpr of InstanceofExpr extends RBoolExpr {
	public ENode	expr;
	public TypeRef	itype;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		resolveENode(expr,null,env);
		Type tp = null;
		if( expr instanceof TypeRef )
			tp = ((TypeRef)expr).getType(env);
		if( tp != null ) {
			InstanceofExpr self = (InstanceofExpr)this;
			replaceWithNode(new ConstBoolExpr(tp.isInstanceOf(itype.getType(env))), self.parent(), self.pslot());
			return;
		} else {
			Type et = expr.getType(env);
			if (!expr.isForWrapper() && et instanceof CTimeType) {
				expr = et.makeUnboxedExpr(expr);
				expr.setForWrapper(true);
				resolveENode(expr,null,env);
			}
		}
		tp = itype.getType(env);
		if( !expr.getType(env).isCastableTo(tp) ) {
			throw new CompilerException(this,"Type "+expr.getType(env)+" is not castable to "+itype);
		}
		if (expr.getType(env).isInstanceOf(tp) && !tp.isUnerasable()) {
			replaceWithNodeResolve(env, reqType,
				new BinaryBoolExpr(pos, env.coreFuncs.fObjectBoolNE,~expr,new ConstNullExpr()));
			return;
		}
		while (tp instanceof CTimeType)
			tp = tp.getEnclosedType();
		if (tp.isUnerasable()) {
			replaceWithNodeResolve(env, reqType, new CallExpr(pos,
					new TypeInfoExpr(itype.getType(env)),
					env.tenv.tpTypeInfo.tdecl.resolveMethod(env,"$instanceof",env.tenv.tpBoolean,env.tenv.tpObject),
					new ENode[]{~expr}
					)
				);
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

@ViewOf(vcast=true)
public view RBooleanNotExpr of BooleanNotExpr extends RBoolExpr {
	public ENode		expr;

	public void resolveENode(Type reqType, Env env) {
		if( isResolved() ) return;
		
		Method m = resolveMethodAndNormalize(env);
		if (m == null)
			return; // error already reported
		if (m.isStatic()) {
			m.makeArgs(getEArgs(),reqType);
			resolveENode(expr,m.params[0].getType(env),env);
		} else {
			m.makeArgs(ENode.emptyArray,reqType);
			resolveENode(expr,((TypeDecl)m.parent()).getType(env),env);
		}
		if !(m instanceof CoreOperation) {
			// Not a standard operator
			if( m.isStatic() )
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,null,m,new ENode[]{~expr}));
			else
				replaceWithNodeResolve(env, reqType, new CallExpr(pos,~expr,m,ENode.emptyArray));
			return;
		}
		RBoolExpr.checkBool(expr, env);
		// Check if expression is a constant
		if (isConstantExpr(env)) {
			replaceWithNodeResolve(env, reqType, ((CoreOperation)m).calc(this));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			RReturnStat.autoReturn(reqType, this, env);
	}
}

