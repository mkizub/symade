package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.InlineMethodStat.ParamRedir;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RInlineMethodStat of InlineMethodStat extends RENode {
	public Method			method;
	public ParamRedir[]		params_redir;

	public void resolve(Type reqType) {
		Type[] types = new Type[params_redir.length];
		for (int i=0; i < params_redir.length; i++) {
			types[i] = params_redir[i].new_var.type;
			params_redir[i].new_var.vtype.lnk = method.params[i].type;
		}
		try {
			method.resolveDecl();
			if( method.body.isAbrupted() ) setAbrupted(true);
			if( method.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			for (int i=0; i < params_redir.length; i++)
				params_redir[i].new_var.vtype.lnk = types[i];
		}
	}
}

@nodeview
public final view RExprStat of ExprStat extends RENode {
	public ENode		expr;

	public void resolve(Type reqType) {
		try {
			if (expr != null) {
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			}
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}
}

@nodeview
public final view RReturnStat of ReturnStat extends RENode {
	public ENode		expr;

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		if( expr != null ) {
			try {
				expr.resolve(ctx_method.type.ret());
			} catch(Exception e ) {
				Kiev.reportError(expr,e);
			}
		}
		if( ctx_method.type.ret() ≡ Type.tpVoid ) {
			if( expr != null && expr.getType() ≢ Type.tpVoid) {
				Kiev.reportError(this,"Can't return value in void method");
				expr = null;
			}
		} else {
			if( expr == null )
				Kiev.reportError(this,"Return must return a value in non-void method");
			else if (!expr.getType().isInstanceOf(ctx_method.type.ret()) && expr.getType() != Type.tpNull)
				Kiev.reportError(this,"Return expression is not of type "+ctx_method.type.ret());
		}
	}
}

@nodeview
public final view RThrowStat of ThrowStat extends RENode {
	public ENode		expr;

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		try {
			expr.resolve(Type.tpThrowable);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
		Type exc = expr.getType();
		if( !PassInfo.checkException((ThrowStat)this,exc) )
			Kiev.reportWarning(this,"Exception "+exc+" must be caught or declared to be thrown");
	}
}

@nodeview
public final view RIfElseStat of IfElseStat extends RENode {
	public ENode		cond;
	public ENode		thenSt;
	public ENode		elseSt;

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
	
		try {
			thenSt.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(thenSt,e);
		}
		if( elseSt != null ) {
			try {
				elseSt.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(elseSt,e);
			}
		}

		if (!cond.isConstantExpr()) {
			if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (cond.getConstValue() instanceof Boolean && ((Boolean)cond.getConstValue()).booleanValue()) {
			if( thenSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (elseSt != null){
			if( elseSt.isAbrupted() ) setAbrupted(true);
			if( elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
	}
}

@nodeview
public final view RCondStat of CondStat extends RENode {
	public ENode		cond;
	public ENode		message;

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		try {
			message.resolve(Type.tpString);
		} catch(Exception e ) {
			Kiev.reportError(message,e);
		}
	}
}

@nodeview
public final view RLabeledStat of LabeledStat extends RENode {
	public NameRef			ident;
	public Label			lbl;
	public ENode			stat;

	public void resolve(Type reqType) {
		try {
			stat.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
	}
}

@nodeview
public final view RBreakStat of BreakStat extends RENode {
	public NameRef			ident;
	public Label			dest;

	public void resolve(Type reqType) {
		setAbrupted(true);
		ASTNode p;
		if (dest != null) {
			dest.delLink((BreakStat)this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(p instanceof Method || p.isBreakTarget()); p = p.parent );
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
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if (p instanceof LabeledStat && p.ident.name.equals(ident.name))
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if (!p.isBreakTarget()) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if (p.ident.name.equals(ident.name)) {
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

@nodeview
public final view RContinueStat of ContinueStat extends RENode {
	public NameRef			ident;
	public Label			dest;

	public void resolve(Type reqType) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}
}

@nodeview
public final view RGotoStat of GotoStat extends RENode {
	public NameRef			ident;
	public Label			dest;

	public void resolve(Type reqType) {
		setAbrupted(true);
		if (dest != null) {
			dest.delLink((GotoStat)this);
			dest = null;
		}
		LabeledStat[] stats = GotoStat.resolveStat(ident.name, ctx_method.body, LabeledStat.emptyArray);
		if( stats.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		if( stats.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		LabeledStat stat = stats[0];
		if( stat == null ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		dest = stat.lbl;
		dest.addLink((GotoStat)this);
	}
}

@nodeview
public final view RGotoCaseStat of GotoCaseStat extends RENode {
	public ENode		expr;
	public SwitchStat	sw;

	public void resolve(Type reqType) {
		setAbrupted(true);
		for(ASTNode node = this.parent; node != null; node = node.parent) {
			if (node instanceof SwitchStat) {
				this.sw = (SwitchStat)node;
				break;
			}
			if (node instanceof Method)
				break;
		}
		if( this.sw == null )
			throw new CompilerException(this,"goto case statement not within a switch statement");
		if( expr != null ) {
			if( sw.mode == SwitchStat.TYPE_SWITCH ) {
				expr = new AssignExpr(pos,AssignOperator.Assign,
					new LVarExpr(pos,sw.tmpvar.getVar()),~expr);
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			} else {
				expr.resolve(sw.sel.getType());
			}
		}
	}
}

