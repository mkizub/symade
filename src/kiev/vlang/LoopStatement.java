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
package kiev.vlang;

import kiev.be.java15.CodeLabel;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(lang=CoreLang)
public abstract class LoopStat extends ENode {
	@virtual typedef This  ≤ LoopStat;

	@nodeAttr(copyable=false)	public Label		lblcnt;
	@nodeAttr(copyable=false)	public Label		lblbrk;

	protected LoopStat() {
		lblcnt = new Label();
		lblbrk = new Label();
		setBreakTarget(true);
	}
}


@ThisIsANode(name="Label", lang=CoreLang)
public final class Label extends DNode {
	
	@DataFlowDefinition(out="this:out()") private static class DFI {}

	@virtual typedef This  = Label;

	@nodeData(copyable=false)	public ASTNode[]		links;
							public CodeLabel		label;

	public boolean preResolveIn() {
		if (meta != null)
			meta.verify();
		return true;
	}

	public boolean preVerify() {
		ASTNode root = (ASTNode)this.ctx_root;
		foreach (ASTNode lnk; links; lnk.ctx_root != root) {
			links.detach(lnk);
		}
		return super.preVerify();
	}	

	public ANode doRewrite(RewriteContext ctx) {
		if (getMeta("kiev\u001fstdlib\u001fmeta\u001fextern") != null)
			return null;
		return super.doRewrite(ctx);
	}

	public void addLink(ASTNode lnk) {
		foreach (ASTNode l; links; l == lnk)
			return;
		links += lnk;
	}

	public void delLink(ASTNode lnk) {
		links.detach(lnk);
	}

	public boolean backendCleanup() {
		this.label = null;
		return true;
	}

	static class LabelDFFunc extends DFFunc {
		final int res_idx;
		LabelDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			if ((dfi.locks & 1) != 0)
				throw new DFLoopException(this);
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Label node = (Label)dfi.node_impl;
			DFState tmp = DataFlowInfo.getDFlow(node).in();
			dfi.locks |= 1;
			try {
				foreach (ASTNode lnk; node.links) {
					try {
						DFState s = DataFlowInfo.getDFlow(lnk).jmp();
						tmp = DFState.join(s,tmp);
					} catch (DFLoopException e) {
						if (e.label != this) throw e;
					}
				}
			} finally { dfi.locks &= ~1; }
			res = tmp;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new LabelDFFunc(dfi);
	}
}

@ThisIsANode(name="While", lang=CoreLang)
public class WhileStat extends LoopStat {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in", links="body")		Label		lblcnt;
	@DataFlowDefinition(in="lblcnt")						ENode		cond;
	@DataFlowDefinition(in="cond:true")					ENode		body;
	@DataFlowDefinition(in="cond:false")					Label		lblbrk;
	}

	@virtual typedef This  = WhileStat;

	@nodeAttr public ENode		cond;
	@nodeAttr public ENode		body;

	public WhileStat() {}

	public WhileStat(int pos, ENode cond, ENode body) {
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (cond == null) {
			cond = new ConstBoolExpr(false);
			cond.initForEditor();
		}
		super.initForEditor();
	}
}

@ThisIsANode(name="DoWhile", lang=CoreLang)
public class DoWhileStat extends LoopStat {

	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in", links="cond:true")	ENode		body;
	@DataFlowDefinition(in="body")							Label		lblcnt;
	@DataFlowDefinition(in="lblcnt")							ENode		cond;
	@DataFlowDefinition(in="cond:false")						Label		lblbrk;
	}

	@virtual typedef This  = DoWhileStat;

	@nodeAttr public ENode		cond;
	@nodeAttr public ENode		body;

	public DoWhileStat() {}

	public DoWhileStat(int pos, ENode cond, ENode body) {
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (cond == null) {
			cond = new ConstBoolExpr(false);
			cond.initForEditor();
		}
		super.initForEditor();
	}
}

@ThisIsANode(name="For", lang=CoreLang)
public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")		ASTNode[]	inits;
	@DataFlowDefinition(in="inits", links="iter")		ENode		cond;
	@DataFlowDefinition(in="cond:true")				ENode		body;
	@DataFlowDefinition(in="body")						Label		lblcnt;
	@DataFlowDefinition(in="lblcnt")					ENode		iter;
	@DataFlowDefinition(in="cond:false")				Label		lblbrk;
	}
	
	@virtual typedef This  = ForStat;

	@nodeAttr public ASTNode[]	inits;
	@nodeAttr public ENode		cond;
	@nodeAttr public ENode		body;
	@nodeAttr public ENode		iter;

	public ForStat() {}
	
	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (cond == null) {
			cond = new ConstBoolExpr(false);
			cond.initForEditor();
		}
		super.initForEditor();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		node @= inits,
		info.checkNodeName(node),
		info.check(node)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ n;
	{
		n @= inits,
		n instanceof Var,
		info.checkNodeName(n),
		((Var)n).isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,mt)
	}
}

@ThisIsANode(name="ForEach", lang=CoreLang)
public class ForEachStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")						ENode		container;
	@DataFlowDefinition(in="this:in")						Var			var;
	@DataFlowDefinition(in="var")							Var			iter;
	@DataFlowDefinition(in="iter")							Var			iter_array;
	@DataFlowDefinition(in="iter_array")						ENode		iter_init;
	@DataFlowDefinition(in="iter_init", links="iter_incr")	ENode		iter_cond;
	@DataFlowDefinition(in="iter_cond:true")					ENode		var_init;
	@DataFlowDefinition(in="var_init")						ENode		cond;
	@DataFlowDefinition(in="cond:true")						ENode		body;
	@DataFlowDefinition(in="body", links="cond:false")		Label		lblcnt;
	@DataFlowDefinition(in="lblcnt")							ENode		iter_incr;
	@DataFlowDefinition(in="iter_cond:false")				Label		lblbrk;
	}

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	@virtual typedef This  = ForEachStat;

	@nodeAttr public int			mode;
	@nodeAttr public ENode		container;
	@nodeAttr public Var			var;
	@nodeAttr public Var			iter;
	@nodeAttr public Var			iter_array;
	@nodeAttr public ENode		iter_init;
	@nodeAttr public ENode		iter_cond;
	@nodeAttr public ENode		var_init;
	@nodeAttr public ENode		cond;
	@nodeAttr public ENode		body;
	@nodeAttr public ENode		iter_incr;

	public ForEachStat() {}
	
	public ForEachStat(int pos, Var var, ENode container, ENode cond, ENode body) {
		this.pos = pos;
		this.var = var;
		this.container = container;
		this.cond = cond;
		this.body = body;
	}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		super.initForEditor();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
	{
		{	node ?= var
		;	node ?= iter
		},
		path.checkNodeName(node)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		Var@ n;
	{
		{	n ?= var
		;	n ?= iter
		},
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,mt)
	}
}

