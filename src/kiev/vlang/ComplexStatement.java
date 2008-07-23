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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(name="Case", lang=CoreLang)
public class CaseLabel extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@DataFlowDefinition(in="this:in()", out="pattern") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode		val;
	@DataFlowDefinition(in="val", seq="true")	Var[]		pattern;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@nodeAttr public ENode			val;
	@nodeData public Type			ctype;
	@nodeAttr public Var∅			pattern;

	public CaseLabel() {}

	public CaseLabel(int pos, ENode val) {
		this.pos = pos;
		this.val = val;
	}

	static class CaseLabelDFFuncIn extends DFFunc {
		final int res_idx;
		CaseLabelDFFuncIn(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			CaseLabel cl = (CaseLabel)dfi.node_impl;
			if (cl.parent() instanceof SwitchStat) {
				ENode sel = ((SwitchStat)cl.parent()).sel;
				if (sel != null)
					res = DataFlowInfo.getDFlow(sel).out();
			}
			if (ANode.getPrevNode(cl) != null) {
				DFState prev = DataFlowInfo.getDFlow((ASTNode)ANode.getPrevNode(cl)).out();
				if (res != null)
					res = DFState.join(res,prev);
				else
					res = prev;
			}
			if (res != null)
				dfi.setResult(res_idx, res);
			else
				res = DFState.makeNewState();
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new CaseLabelDFFuncIn(dfi);
	}

	public String toString() {
		if( val == null )
			return "default:";
		else if(pattern.length > 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("case ").append(val).append('(');
			for(int i=0; i < pattern.length; i++) {
				sb.append(pattern[i].vtype).append(' ').append(pattern[i].sname);
				if( i < pattern.length-1 ) sb.append(',');
			}
			sb.append("):");
			return sb.toString();
		}
		return "case "+val+':';
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
	{
		n @= pattern,
		info.checkNodeName(n),
		node ?= n
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= pattern,
		n instanceof Var && ((Var)n).isForward(),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		((Var)n).getType().resolveCallAccessR(node,info,mt)
	}

}

@ThisIsANode(name="Switch", lang=CoreLang)
public class SwitchStat extends Block {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			sel;
	@DataFlowDefinition(in="sel", seq="true")	ENode[]			stats;
	@DataFlowDefinition(in="stats")			Label			lblcnt;
	@DataFlowDefinition(in="stats")			Label			lblbrk;
	}
	
	@nodeAttr public ENode                 sel;
	@nodeData public CaseLabel∅           cases;
	@nodeData public CaseLabel             defCase;
	@nodeAttr public ENode                 sel_to_int;
	@nodeAttr(copyable=false, ext_data=true)
	          public Label                 lblcnt;

	public SwitchStat() {}

	public SwitchStat(int pos, ENode sel) {
		this();
		this.pos = pos;
		this.sel = sel;
	}

	public final boolean isBreakTarget() {
		return true;
	}

	public String toString() { return "switch("+sel+")"; }
	
	public void preResolveOut() {
		Vector<CaseLabel> labels = new Vector<CaseLabel>();
		CaseLabel dflt = null;
		foreach (CaseLabel l; stats) {
			labels.append(l);
			if (l.val == null) {
				if (dflt != null)
					Kiev.reportError(l, "Multiple 'default' cases.");
				else
					dflt = l;
			}
		}
		if (dflt == defCase && labels.length == cases.length) {
			for (int i=0; i < labels.length; i++) {
				if (cases[i] != labels[i])
					goto update;
			}
			return;
		}
	update:
		defCase = dflt;
		cases.delAll();
		foreach (CaseLabel l; labels)
			cases += l;
	}
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (tp.meta_type.tdecl.isEnum()) {
			SwitchEnumStat sw = new SwitchEnumStat();
			sw.sel = ~this.sel;
			foreach (ASTNode st; stats.delToArray())
				sw.stats += st;
			this.replaceWithNodeReWalk(sw);
		}
		if (tp.isReference() && tp.meta_type.tdecl.isHasCases()) {
			MatchStat sw = new MatchStat();
			sw.sel = ~this.sel;
			foreach (ASTNode st; stats.delToArray())
				sw.stats += st;
			this.replaceWithNodeReWalk(sw);
		}
		if (tp.isReference()) {
			SwitchTypeStat sw = new SwitchTypeStat();
			sw.sel = ~this.sel;
			foreach (ASTNode st; stats.delToArray())
				sw.stats += st;
			this.replaceWithNodeReWalk(sw);
		}
		if (tp.getAutoCastTo(Type.tpInt) ≢ Type.tpInt)
			Kiev.reportError(this, "Type of switch selector must be int");
	}

}

@ThisIsANode(name="SwitchEnum", lang=CoreLang)
public class SwitchEnumStat extends SwitchStat {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			sel;
	@DataFlowDefinition(in="sel", seq="true")	ENode[]			stats;
	@DataFlowDefinition(in="stats")				Label			lblcnt;
	@DataFlowDefinition(in="stats")				Label			lblbrk;
	}
	
	public SwitchEnumStat() {
	}

	public SwitchEnumStat(int pos, ENode sel) {
		this(pos, sel);
	}

	public String toString() { return "switch-enum("+sel+")"; }
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (!tp.meta_type.tdecl.isEnum())
			Kiev.reportError(this, "Expected enum value as selector");
	}

}

@ThisIsANode(name="SwitchType", lang=CoreLang)
public class SwitchTypeStat extends SwitchStat {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			sel;
	@DataFlowDefinition(in="sel", seq="true")	ENode[]			stats;
	@DataFlowDefinition(in="stats")				Label			lblcnt;
	@DataFlowDefinition(in="stats")				Label			lblbrk;
	}
	
	public SwitchTypeStat() {
	}

	public String toString() { return "switch-type("+sel+")"; }
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (!tp.isReference())
			Kiev.reportError(this, "Expected value of reference type as selector");
	}

}

@ThisIsANode(name="Match", lang=CoreLang)
public class MatchStat extends SwitchStat {
	
	@DataFlowDefinition(out="lblbrk") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			sel;
	@DataFlowDefinition(in="sel", seq="true")	ENode[]			stats;
	@DataFlowDefinition(in="stats")				Label			lblcnt;
	@DataFlowDefinition(in="stats")				Label			lblbrk;
	}
	
	@nodeAttr public Var					tmp_var;

	public MatchStat() {
	}

	public String toString() { return "match("+sel+")"; }
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (!tp.isReference() || !tp.meta_type.tdecl.isHasCases())
			Kiev.reportError(this, "Expected value of type with cases as selector");
	}

}

@ThisIsANode(name="Catch", lang=CoreLang)
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")	Var				arg;
	@DataFlowDefinition(in="arg")		ENode			body;
	}
	
	public static final CatchInfo[] emptyArray = new CatchInfo[0];

	@nodeAttr public Var			arg;
	@nodeAttr public ENode			body;

	public CatchInfo() {}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		node ?= arg,
		info.checkNodeName(node)
	}
}

@ThisIsANode(name="Finally", lang=CoreLang)
public class FinallyInfo extends ENode {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			body;
	}
	
	@nodeAttr public ENode			body;
	@nodeAttr public Var			ret_arg;

	public FinallyInfo() {}

	public String toString() { return "finally"; }
}

@ThisIsANode(name="Try", lang=CoreLang)
public class TryStat extends ENode {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode			body;
	@DataFlowDefinition(in="this:in", seq="false")	CatchInfo[]		catchers;
	@DataFlowDefinition(in="this:in")				FinallyInfo		finally_catcher;
	}
	
	@nodeAttr public ENode				body;
	@nodeAttr public CatchInfo∅		catchers;
	@nodeAttr public FinallyInfo		finally_catcher;

	public TryStat() {}

}

@ThisIsANode(name="Synchronized", lang=CoreLang)
public class SynchronizedStat extends ENode {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	@DataFlowDefinition(in="expr")		ENode		body;
	}
	
	@nodeAttr public ENode			expr;
	@nodeAttr public Var				expr_var;
	@nodeAttr public ENode			body;

	public SynchronizedStat() {}

}

@ThisIsANode(name="With", lang=CoreLang)
public class WithStat extends ENode {

	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	@DataFlowDefinition(in="expr")		ENode		body;
	}
	
	@nodeAttr public ENode		expr;
	@nodeAttr public ENode		body;
	@nodeData public Var			var_or_field;

	public WithStat() {}

}

