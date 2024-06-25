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
public final view RCatchInfo of CatchInfo extends RENode {
	public Var				arg;
	public ENode			body;

	public void resolveENode(Type reqType, Env env) {
		try {
			resolveENode(body,env.tenv.tpVoid,env);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}
}

@ViewOf(vcast=true)
public final view RFinallyInfo of FinallyInfo extends RENode {
	public ENode		body;
	public Var			ret_arg;

	public void resolveENode(Type reqType, Env env) {
		if (ret_arg == null) {
			ret_arg = new LVar(pos,"",env.tenv.tpObject,Var.VAR_LOCAL,0);
		}
		try {
			resolveENode(body,env.tenv.tpVoid,env);
			if( body.isMethodAbrupted() ) setMethodAbrupted(true);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}
}

@ViewOf(vcast=true)
public final view RTryStat of TryStat extends RENode {
	public		ENode				body;
	public:ro	CatchInfo[]			catchers;
	public		FinallyInfo			finally_catcher;

	public void resolveENode(Type reqType, Env env) {
		for(int i=0; i < catchers.length; i++) {
			try {
				resolveENode(catchers[i],env.tenv.tpVoid,env);
			} catch(Exception e ) {
				Kiev.reportError(catchers[i],e);
			}
		}
		if(finally_catcher != null) {
			try {
				resolveENode(finally_catcher,env.tenv.tpVoid,env);
			} catch(Exception e ) {
				Kiev.reportError(finally_catcher,e);
			}
		}
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		// Check if abrupted
		if( finally_catcher!= null && finally_catcher.isMethodAbrupted())
			setMethodAbrupted(true);
		else if( finally_catcher!= null && finally_catcher.isAbrupted())
			setMethodAbrupted(false);
		else {
			// Check that the body and all cases are abrupted
			boolean has_unabrupted_catcher = false;
			if( !body.isMethodAbrupted() ) has_unabrupted_catcher = true;
			else {
				for(int i=0; i < catchers.length; i++) {
					if( !catchers[i].isMethodAbrupted() ) {
						has_unabrupted_catcher = true;
						break;
					}
				}
			}
			if( !has_unabrupted_catcher ) setMethodAbrupted(true);
		}
	}
}

