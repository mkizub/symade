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
package kiev.transf;

import syntax kiev.Syntax;

import kiev.ir.java15.RRuleMethod;

/**
 * @author Maxim Kizub
 *
 */

public final class LogicPlugin implements PluginFactory {
	public PluginDescr getPluginDescr(String name) {
		PluginDescr pd = null;
		if (name.equals("logic")) {
			pd = new PluginDescr("logic").depends("kiev");
			pd.proc(new ProcessorDescr("rewrite", "me", 0, KievME_RuleGenartion.class).after("kiev:me:dump-api").before("kiev:me:pre-generate"));
		}
		return pd;
	}
}

public final class KievME_RuleGenartion extends BackendProcessor {
	public KievME_RuleGenartion(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "Kiev rule-generation" }

	public void process(ASTNode node, Transaction tr) {
		if!(node instanceof CompilationUnit)
			return;
		//FileUnit fu = (FileUnit)node;
		//if (fu.scanned_for_interface_only)
		//	return;
		WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
		if (wthg.setProcessorRun((CompilationUnit)node,this))
			return;
		
		tr = Transaction.enter(tr,"KievME_RuleGenartion");
		try {
			node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n instanceof RuleMethod) {
						RuleMethod rm = (RuleMethod)n;
						((RRuleMethod)rm).ruleGenerate(env);
					}
					return true;
				}
			});
		} finally { tr.leave(); }
	}
}

