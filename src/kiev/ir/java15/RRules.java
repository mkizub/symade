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

public final view RRuleMethod of RuleMethod extends RMethod {
	public:ro	Var[]				localvars;
	public		int					base;
	public		int					max_depth;
	public		int					state_depth;
	public		int					max_vars;
	public		int					index;		// index counter for RuleNode.idx

	public boolean preGenerate() {
		Var penv = params[0];
		assert(penv.sname == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
		if( body instanceof RuleBlock ) {
			body.preGenerate();
			Kiev.runProcessorsOn(body);
		}
		return true;
	}

	public void resolveDecl() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving rule "+this);
		try {
			Var penv = params[0];
			assert(penv.sname == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
			if( body != null ) {
				if( type.ret() ≡ Type.tpVoid ) body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret() ≡ Type.tpVoid ) {
					block.stats.append(new ReturnStat(pos,null));
					body.setAbrupted(true);
				} else {
					Kiev.reportError(body,"Return requared");
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
	}
}

public final view RRuleBlock of RuleBlock extends RENode {
	public ASTRuleNode		rnode;

	public boolean preGenerate() {
		rnode.rnResolve();
		rnode.resolve1(null,null,false);
		((RuleBlock)this).testGenerate(null, null);
		return false;
	}
}

