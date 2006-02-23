package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.Method.MethodView;
import kiev.vlang.LoopStat.LoopStatView;
import kiev.vlang.SwitchStat.SwitchStatView;

import kiev.vlang.InlineMethodStat.InlineMethodStatImpl;
import kiev.vlang.InlineMethodStat.InlineMethodStatView;
import kiev.vlang.ExprStat.ExprStatImpl;
import kiev.vlang.ExprStat.ExprStatView;
import kiev.vlang.ReturnStat.ReturnStatImpl;
import kiev.vlang.ReturnStat.ReturnStatView;
import kiev.vlang.ThrowStat.ThrowStatImpl;
import kiev.vlang.ThrowStat.ThrowStatView;
import kiev.vlang.IfElseStat.IfElseStatImpl;
import kiev.vlang.IfElseStat.IfElseStatView;
import kiev.vlang.CondStat.CondStatImpl;
import kiev.vlang.CondStat.CondStatView;
import kiev.vlang.LabeledStat.LabeledStatImpl;
import kiev.vlang.LabeledStat.LabeledStatView;
import kiev.vlang.BreakStat.BreakStatImpl;
import kiev.vlang.BreakStat.BreakStatView;
import kiev.vlang.ContinueStat.ContinueStatImpl;
import kiev.vlang.ContinueStat.ContinueStatView;
import kiev.vlang.GotoStat.GotoStatImpl;
import kiev.vlang.GotoStat.GotoStatView;
import kiev.vlang.GotoCaseStat.GotoCaseStatImpl;
import kiev.vlang.GotoCaseStat.GotoCaseStatView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RInlineMethodStat of InlineMethodStatImpl extends InlineMethodStatView {
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
public final view RExprStat of ExprStatImpl extends ExprStatView {
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
public final view RReturnStat of ReturnStatImpl extends ReturnStatView {
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
public final view RThrowStat of ThrowStatImpl extends ThrowStatView {
	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		try {
			expr.resolve(Type.tpThrowable);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
		Type exc = expr.getType();
		if( !PassInfo.checkException(this.getNode(),exc) )
			Kiev.reportWarning(this,"Exception "+exc+" must be caught or declared to be thrown");
	}
}

@nodeview
public final view RIfElseStat of IfElseStatImpl extends IfElseStatView {
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
public final view RCondStat of CondStatImpl extends CondStatView {
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
public final view RLabeledStat of LabeledStatImpl extends LabeledStatView {
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
public final view RBreakStat of BreakStatImpl extends BreakStatView {
	public void resolve(Type reqType) {
		setAbrupted(true);
		NodeView p;
		if (dest != null) {
			dest.delLink(this.getNode());
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(p instanceof MethodView || p.isBreakTarget()); p = p.parent );
			if( p instanceof MethodView || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStatView) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this.getNode());
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof MethodView) ; p=p.parent ) {
				if (p instanceof LabeledStatView && p.ident.name.equals(ident.name))
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if (!p.isBreakTarget()) continue;
				NodeView pp = p;
				for(p=p.parent; p instanceof LabeledStatView; p = p.parent) {
					if (p.ident.name.equals(ident.name)) {
						p = pp;
						break label_found;
					}
				}
				p = pp;
			}
			if( p instanceof MethodView || p == null) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStatView) {
					Label l = p.lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this.getNode());
					}
				}
			}
		}
		if( p instanceof MethodView )
			Kiev.reportError(this,"Break not within loop/switch statement");
		((ENodeView)p).setBreaked(true);
	}
}

@nodeview
public final view RContinueStat of ContinueStatImpl extends ContinueStatView {
	public void resolve(Type reqType) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}
}

@nodeview
public final view RGotoStat of GotoStatImpl extends GotoStatView {
	public void resolve(Type reqType) {
		setAbrupted(true);
		if (dest != null) {
			dest.delLink(this.getNode());
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
		dest.addLink(this.getNode());
	}
}

@nodeview
public final view RGotoCaseStat of GotoCaseStatImpl extends GotoCaseStatView {
	public void resolve(Type reqType) {
		setAbrupted(true);
		for(NodeView node = this.parent; node != null; node = node.parent) {
			if (node instanceof SwitchStatView) {
				this.sw = (SwitchStat)node.getNode();
				break;
			}
			if (node instanceof MethodView)
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

