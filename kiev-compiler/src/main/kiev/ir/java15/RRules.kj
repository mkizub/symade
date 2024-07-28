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
public final view RRuleMethod of RuleMethod extends RMethod {
	public:ro	Var[]				localvars;
	public		int					base;
	public		int					max_depth;
	public		int					state_depth;
	public		int					max_vars;
	public		int					index;		// index counter for RuleNode.idx

	public void ruleGenerate(Env env) {
		RuleMethod rm = (RuleMethod)this;
		if (params.length == 0 || params[0].kind != Var.PARAM_RULE_ENV)
			rm.params.insert(0, new LVar(rm.pos,namePEnvParam,env.tenv.tpRule,Var.PARAM_RULE_ENV,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
		ENode b = this.body;
		if (b instanceof RuleBlock) {
			RuleBlock rb = (RuleBlock)b;
			((RRuleBlock)rb).ruleGenerate(env,rm);
			Struct frame = (Struct)rm.block.stats[0];
			Var pEnv = null;
			foreach (Var dn; rm.block.stats; dn.sname == namePEnvLVar) {
				pEnv = dn;
				break;
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnvLVar);
				return;
			}
			this.body.walkTree(null,null,new ITreeWalker() {
				public void post_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof LVarExpr) {
						Var var = n.getVarSafe();
						if (rm.localvars.indexOf(var) >= 0) {
							Field f = frame.resolveField(env,var.sname);
							n.replaceWithNode(new IFldExpr(n.pos, new LVarExpr(n.pos, pEnv), f), parent, slot);
						}
					}
				}
			});
			Kiev.runProcessorsOn(this.body);
		}
	}

	public void resolveDecl(Env env) {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving rule "+this);
		try {
			if( body != null ) {
				if( mtype.ret() ≡ env.tenv.tpVoid ) body.setAutoReturnable(true);
				resolveENode(body,env.tenv.tpVoid,env);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( mtype.ret() ≡ env.tenv.tpVoid ) {
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

@ViewOf(vcast=true)
public final view RRuleBlock of RuleBlock extends RENode {
	public ASTRuleNode		rnode;

	public boolean ruleGenerate(Env env, RuleMethod rule_method) {
		rnode.rnResolve(env, ((RuleBlock)this), RuleBlock.nodeattr$rnode);
		rnode.resolve1(env,null,null,false);
		((RuleBlock)this).generateRuleBlock(env, rule_method);
		return false;
	}
}

