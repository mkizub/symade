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

/**
 * @author Maxim Kizub
 *
 */

@singleton
public class RewriteME_PreGenerate extends BackendProcessor {
	private RewriteME_PreGenerate() { super(KievBackend.Generic); }
	public String getDescr() { "Rewrite rules" }

	public boolean isEnabled() {
		return Kiev.enabled(KievExt.Rewrite);
	}

	public void process(ASTNode node, Transaction tr) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { return rewrite(n); }
		});
	}

	boolean rewrite(ANode:ANode node) {
		return true;
	}
	
	boolean rewrite(DNode:ANode dn) {
		if (dn.isMacro())
			return false;
		return true;
	}	

	boolean rewrite(AssignExpr:ANode ae) {
		Method m = (Method)ae.dnode;
		if (m != null && m.isMacro() && !m.isNative()) {
			if (m.body != null)
				doRewrite(m.body, ae, new Hashtable<String,Object>());
		}
		return true;
	}

	boolean rewrite(IFldExpr:ANode fa) {
		Field f = fa.var;
		if (f != null && f.isMacro() && !f.isNative()) {
			if (f.init != null)
				doRewrite(f.init, fa, new Hashtable<String,Object>());
		}
		return true;
	}

	boolean rewrite(CastExpr:ANode ce) {
		Method m = (Method)ce.dnode;
		if (m != null && m.isMacro() && !m.isNative()) {
			if (m.body != null) {
				int idx = 0;
				Hashtable<String,Object> args = new Hashtable<String,Object>();
				//foreach (Var fp; m.params; fp.kind == Var.PARAM_NORMAL)
				//	args.put(fp.sname, ce.args[idx++]);
				doRewrite(m.body, ce, args);
			}
		}
		return true;
	}

	boolean rewrite(CallExpr:ANode ce) {
		Method m = ce.func;
		if (m != null && m.isMacro() && !m.isNative()) {
			if (m.body != null) {
				int idx = 0;
				Hashtable<String,Object> args = new Hashtable<String,Object>();
				foreach (Var fp; m.params; fp.kind == Var.PARAM_NORMAL)
					args.put(fp.sname, ce.args[idx++]);
				doRewrite(ce, ce, args);
			}
		}
		return true;
	}

	private void doRewrite(ENode rewriter, ASTNode self, Hashtable<String,Object> args) {
		Transaction tr = Transaction.enter(Transaction.get(),"RewriteME_PreGenerate");
		try {
			if (rewriter instanceof RewriteMatch)
				rewriter = rewriter.matchCase(self);
			//rewriter = rewriter.ncopy();
			RewriteContext rctx = new RewriteContext(self, args);
			ASTNode rn = (ASTNode)rewriter.doRewrite(rctx);
			rn = self.replaceWithNode(~rn);
			Kiev.runProcessorsOn(rn);
			throw new ReWalkNodeException(rn);
		} finally { tr.leave(); }
	}
}

