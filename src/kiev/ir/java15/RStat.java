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
public final view RInlineMethodStat of InlineMethodStat extends RENode {
	public:ro Method					dispatched;
	public:ro Method					dispatcher;
	public:ro SymbolRef[]				old_vars;
	public:ro SymbolRef[]				new_vars;

	public void resolveENode(Type reqType, Env env) {
		Type[] types = new Type[new_vars.length];
		for (int i=0; i < new_vars.length; i++) {
			types[i] = new_vars[i].dnode.getType(env);
			if (((Var)new_vars[i].dnode).vtype.type_lnk != dispatched.params[i].getType(env))
				((Var)new_vars[i].dnode).vtype.type_lnk = dispatched.params[i].getType(env);
		}
		try {
			resolveDNode(dispatched,env);
			if( dispatched.body.isAbrupted() ) setAbrupted(true);
			if( dispatched.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			for (int i=0; i < new_vars.length; i++) {
				if (((Var)new_vars[i].dnode).vtype.type_lnk != types[i])
					((Var)new_vars[i].dnode).vtype.type_lnk = types[i];
			}
		}
	}
}

@ViewOf(vcast=true)
public final view RExprStat of ExprStat extends RENode {
	public ENode		expr;

	public void resolveENode(Type reqType, Env env) {
		try {
			if (expr != null) {
				resolveENode(expr,env.tenv.tpVoid,env);
				expr.setGenVoidExpr(true);
			}
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}
}

@ViewOf(vcast=true)
public final view RReturnStat of ReturnStat extends RENode {
	public ENode		expr;

	public void resolveENode(Type reqType, Env env) {
		setMethodAbrupted(true);
		if( expr != null ) {
			try {
				resolveENode(expr,ctx_method.mtype.ret(),env);
			} catch(Exception e ) {
				Kiev.reportError(expr,e);
			}
		}
		if( ctx_method.mtype.ret() ≡ env.tenv.tpVoid ) {
			if( expr != null && expr.getType(env) ≢ env.tenv.tpVoid) {
				Kiev.reportError(this,"Can't return value in void method");
				expr = null;
			}
		} else {
			if( expr == null )
				Kiev.reportError(this,"Return must return a value in non-void method");
			else if (!expr.getType(env).isInstanceOf(ctx_method.mtype.ret()) && expr.getType(env) != env.tenv.tpNull)
				Kiev.reportError(this,"Return expression is not of type "+ctx_method.mtype.ret());
		}
	}

	public static void autoReturn(Type reqType, RENode expr, Env env) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(env, reqType, fun ()->ENode { return new ReturnStat(expr.pos, ((ENode)expr).detach()); });
	}

	public static void autoReturn(Type reqType, ENode expr, Env env) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		((RENode)expr).replaceWithResolve(env, reqType, fun ()->ENode { return new ReturnStat(expr.pos, ~expr); });
	}
}

@ViewOf(vcast=true)
public final view RThrowStat of ThrowStat extends RENode {
	public ENode		expr;

	public void resolveENode(Type reqType, Env env) {
		setMethodAbrupted(true);
		try {
			resolveENode(expr,env.tenv.tpThrowable,env);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
		Type exc = expr.getType(env);
		if( !PassInfo.checkException((ThrowStat)this,exc,env) )
			Kiev.reportWarning(this,"Exception "+exc+" must be caught or declared to be thrown");
	}
}

@ViewOf(vcast=true)
public final view RIfElseStat of IfElseStat extends RENode {
	public ENode		cond;
	public ENode		thenSt;
	public ENode		elseSt;

	public void resolveENode(Type reqType, Env env) {
		try {
			resolveENode(cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(cond, env);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
	
		try {
			resolveENode(thenSt,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(thenSt,e);
		}
		if( elseSt != null ) {
			try {
				resolveENode(elseSt,env.tenv.tpVoid,env);
			} catch(Exception e ) {
				Kiev.reportError(elseSt,e);
			}
		}

		if (!cond.isConstantExpr(env)) {
			if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (cond.getConstValue(env) instanceof Boolean && ((Boolean)cond.getConstValue(env)).booleanValue()) {
			if( thenSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (elseSt != null){
			if( elseSt.isAbrupted() ) setAbrupted(true);
			if( elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
	}
}

@ViewOf(vcast=true)
public final view RCondStat of CondStat extends RENode {
	public ENode		enabled;
	public ENode		cond;
	public ENode		message;

	public void resolveENode(Type reqType, Env env) {
		try {
			if (enabled == null)
				enabled = new AssertEnabledExpr();
			resolveENode(enabled,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(enabled, env);
			resolveENode(cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(cond, env);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		try {
			resolveENode(message,env.tenv.tpString,env);
		} catch(Exception e ) {
			Kiev.reportError(message,e);
		}
	}
}

@ViewOf(vcast=true)
public final view RLabeledStat of LabeledStat extends RENode {
	public Label			lbl;
	public ENode			stat;

	public void resolveENode(Type reqType, Env env) {
		try {
			resolveENode(stat,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
	}
}

@ViewOf(vcast=true)
public final view RBreakStat of BreakStat extends RENode {
	public Label			dest;

	public void resolveENode(Type reqType, Env env) {
		setAbrupted(true);
		ASTNode p;
		if (dest != null) {
			dest.delLink((BreakStat)this);
			dest = null;
		}
		if (this.ident == null || this.ident == "") {
			for(p=(ASTNode)parent(); !(p instanceof Method || p.isBreakTarget()); p = (ASTNode)p.parent() );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink((BreakStat)this);
					}
				}
			}
		} else {
	label_found:
			for(p=(ASTNode)parent(); !(p instanceof Method) ; p=(ASTNode)p.parent() ) {
				if (p instanceof LabeledStat && p.lbl.sname == this.ident)
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if (!p.isBreakTarget()) continue;
				ASTNode pp = p;
				for(p=(ASTNode)p.parent(); p instanceof LabeledStat; p = (ASTNode)p.parent()) {
					if (p.lbl.sname == this.ident) {
						p = pp;
						break label_found;
					}
				}
				p = pp;
			}
			if( p instanceof Method || p == null) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink((BreakStat)this);
					}
				}
			}
		}
		if( p instanceof Method )
			Kiev.reportError(this,"Break not within loop/switch statement");
		((ENode)p).setBreaked(true);
	}
}

@ViewOf(vcast=true)
public final view RContinueStat of ContinueStat extends RENode {
	public Label			dest;

	public void resolveENode(Type reqType, Env env) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}
}

@ViewOf(vcast=true)
public final view RGotoStat of GotoStat extends RENode {
	public Label			dest;

	public void resolveENode(Type reqType, Env env) {
		setAbrupted(true);
		if (dest != null) {
			dest.delLink((GotoStat)this);
			dest = null;
		}
		Label[] labels = GotoStat.resolveStat(this.ident, ctx_method.body);
		if( labels.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		if( labels.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		Label label = labels[0];
		dest = label;
		dest.addLink((GotoStat)this);
	}
}

@ViewOf(vcast=true)
public final view RGotoCaseStat of GotoCaseStat extends RENode {
	public ENode		expr;
	public SwitchStat	sw;

	public void resolveENode(Type reqType, Env env) {
		setAbrupted(true);
		for(ASTNode node = (ASTNode)this.parent(); node != null; node = (ASTNode)node.parent()) {
			if (node instanceof SwitchStat) {
				if (this.sw != node)
					this.sw = (SwitchStat)node;
				break;
			}
			if (node instanceof Method)
				break;
		}
		if( this.sw == null )
			throw new CompilerException(this,"goto case statement not within a switch statement");
		if( expr != null )
			resolveENode(expr,sw.sel.getType(env),env);
	}
}

