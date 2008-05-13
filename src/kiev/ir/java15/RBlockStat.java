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

@ViewOf(vcast=true, iface=true)
public final view RSynchronizedStat of SynchronizedStat extends RENode {
	public ENode			expr;
	public Var				expr_var;
	public ENode			body;

	public void resolve(Type reqType) {
		try {
			expr.resolve(null);
			expr_var = new LVar(pos,"",Type.tpObject,Var.VAR_LOCAL,0);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) { Kiev.reportError(this,e); }
		setAbrupted(body.isAbrupted());
		setMethodAbrupted(body.isMethodAbrupted());
	}
}

@ViewOf(vcast=true, iface=true)
public final view RWithStat of WithStat extends RENode {
	public ENode		expr;
	public ENode		body;
	public Var			var_or_field;

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
				this.replaceWithNodeResolve(reqType,body);
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

