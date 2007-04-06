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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RLoopStat;
import kiev.be.java15.JLoopStat;
import kiev.ir.java15.RLabel;
import kiev.be.java15.JLabel;
import kiev.ir.java15.RWhileStat;
import kiev.be.java15.JWhileStat;
import kiev.ir.java15.RDoWhileStat;
import kiev.be.java15.JDoWhileStat;
import kiev.ir.java15.RForStat;
import kiev.be.java15.JForStat;
import kiev.ir.java15.RForEachStat;
import kiev.be.java15.JForEachStat;

import kiev.be.java15.CodeLabel;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public abstract class LoopStat extends ENode implements ContinueTarget {
	@virtual typedef This  ≤ LoopStat;
	@virtual typedef JView ≤ JLoopStat;
	@virtual typedef RView ≤ RLoopStat;

	@att(copyable=false)	public Label		lblcnt;
	@att(copyable=false)	public Label		lblbrk;

	protected LoopStat() {
		lblcnt = new Label();
		lblbrk = new Label();
		setBreakTarget(true);
	}
}


@node(name="Label")
public final class Label extends DNode {
	
	@dflow(out="this:out()") private static class DFI {}

	@virtual typedef This  = Label;
	@virtual typedef JView = JLabel;
	@virtual typedef RView = RLabel;

	@ref(copyable=false)	public List<ASTNode>	links = List.Nil;
							public CodeLabel		label;

	public boolean preVerify() {
		ASTNode root = this.ctx_root;
		List<ASTNode> tmp = links.filter(fun (ASTNode n)->boolean { return n.ctx_root == root; });
		if (this.links != tmp) {
			this = this.open();
			this.links = tmp;
		}
		return super.preVerify();
	}	

	public void addLink(ASTNode lnk) {
		if (links.contains(lnk))
			return;
		this = this.open();
		links = new List.Cons<ASTNode>(lnk, links);
	}

	public void delLink(ASTNode lnk) {
		this = this.open();
		links = links.diff(lnk);
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
			DFState tmp = node.getDFlow().in();
			dfi.locks |= 1;
			try {
				foreach (ASTNode lnk; node.links) {
					try {
						DFState s = lnk.getDFlow().jmp();
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

@node(name="While")
public class WhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="body")		Label		lblcnt;
	@dflow(in="lblcnt")						ENode		cond;
	@dflow(in="cond:true")					ENode		body;
	@dflow(in="cond:false")					Label		lblbrk;
	}

	@virtual typedef This  = WhileStat;
	@virtual typedef JView = JWhileStat;
	@virtual typedef RView = RWhileStat;

	@att public ENode		cond;
	@att public ENode		body;

	public WhileStat() {}

	public WhileStat(int pos, ENode cond, ENode body) {
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}
}

@node(name="DoWhile")
public class DoWhileStat extends LoopStat {

	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="cond:true")	ENode		body;
	@dflow(in="body")							Label		lblcnt;
	@dflow(in="lblcnt")							ENode		cond;
	@dflow(in="cond:false")						Label		lblbrk;
	}

	@virtual typedef This  = DoWhileStat;
	@virtual typedef JView = JDoWhileStat;
	@virtual typedef RView = RDoWhileStat;

	@att public ENode		cond;
	@att public ENode		body;

	public DoWhileStat() {}

	public DoWhileStat(int pos, ENode cond, ENode body) {
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}
}

@node(name="For")
public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")				ASTNode		init;
	@dflow(in="init", links="iter")		ENode		cond;
	@dflow(in="cond:true")				ENode		body;
	@dflow(in="body")					Label		lblcnt;
	@dflow(in="lblcnt")					ENode		iter;
	@dflow(in="cond:false")				Label		lblbrk;
	}
	
	@virtual typedef This  = ForStat;
	@virtual typedef JView = JForStat;
	@virtual typedef RView = RForStat;

	@att public ASTNode		init;
	@att public ENode		cond;
	@att public ENode		body;
	@att public ENode		iter;

	public ForStat() {}
	
	public ForStat(int pos, ASTNode init, ENode cond, ENode iter, ENode body) {
		this.pos = pos;
		this.init = init;
		this.cond = cond;
		this.iter = iter;
		this.body = body;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		DNode@ dn;
	{
		init instanceof DeclGroup,
		dn @= ((DeclGroup)init).decls,
		info.checkNodeName(dn),
		info.check(dn),
		node ?= dn
	;	init instanceof Var,
		info.checkNodeName(init),
		info.check(init),
		node ?= init
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ n;
	{
		init instanceof DeclGroup,
		((DeclGroup)init).resolveMethodR(node,info,mt)
	;	init instanceof Var,
		info.checkNodeName(init),
		((Var)init).isForward(),
		info.enterForward(init) : info.leaveForward(init),
		init.getType().resolveCallAccessR(node,info,mt)
	}
}

@node(name="ForEach")
public class ForEachStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")						ENode		container;
	@dflow(in="this:in")						Var			var;
	@dflow(in="var")							Var			iter;
	@dflow(in="iter")							Var			iter_array;
	@dflow(in="iter_array")						ENode		iter_init;
	@dflow(in="iter_init", links="iter_incr")	ENode		iter_cond;
	@dflow(in="iter_cond:true")					ENode		var_init;
	@dflow(in="var_init")						ENode		cond;
	@dflow(in="cond:true")						ENode		body;
	@dflow(in="body", links="cond:false")		Label		lblcnt;
	@dflow(in="lblcnt")							ENode		iter_incr;
	@dflow(in="iter_cond:false")				Label		lblbrk;
	}

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;
	public static final int	RULE  = 4;

	@virtual typedef This  = ForEachStat;
	@virtual typedef JView = JForEachStat;
	@virtual typedef RView = RForEachStat;

	@att public int			mode;
	@att public ENode		container;
	@att public Var			var;
	@att public Var			iter;
	@att public Var			iter_array;
	@att public ENode		iter_init;
	@att public ENode		iter_cond;
	@att public ENode		var_init;
	@att public ENode		cond;
	@att public ENode		body;
	@att public ENode		iter_incr;

	public ForEachStat() {}
	
	public ForEachStat(int pos, Var var, ENode container, ENode cond, ENode body) {
		this.pos = pos;
		this.var = var;
		this.container = container;
		this.cond = cond;
		this.body = body;
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

