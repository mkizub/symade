package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.SynchronizedStat.SynchronizedStatImpl;
import kiev.vlang.SynchronizedStat.SynchronizedStatView;
import kiev.vlang.WithStat.WithStatImpl;
import kiev.vlang.WithStat.WithStatView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RSynchronizedStat of SynchronizedStatImpl extends SynchronizedStatView {

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			expr_var = new Var(pos,KString.Empty,Type.tpObject,0);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		setAbrupted(body.isAbrupted());
		setMethodAbrupted(body.isMethodAbrupted());
	}
}

@nodeview
public final view RWithStat of WithStatImpl extends WithStatView {

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			ENode e = expr;
			switch (e) {
			case LVarExpr:		var_or_field = ((LVarExpr)e).getVar();	break;
			case IFldExpr:		var_or_field = ((IFldExpr)e).var;		break;
			case SFldExpr:		var_or_field = ((SFldExpr)e).var;		break;
			case AssignExpr:	e = ((AssignExpr)e).lval;				goto case e;
			}
			if (var_or_field == null) {
				Kiev.reportError(this,"With statement needs variable or field argument");
				this.replaceWithNode(body);
				body.resolve(Type.tpVoid);
				return;
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
			return;
		}

		boolean is_forward = var_or_field.isForward();
		if (!is_forward) var_or_field.setForward(true);
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		} finally {
			if (!is_forward) var_or_field.setForward(false);
		}

		setAbrupted(body.isAbrupted());
		setMethodAbrupted(body.isMethodAbrupted());
	}
}
