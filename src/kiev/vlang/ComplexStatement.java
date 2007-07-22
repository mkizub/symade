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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RCaseLabel;
import kiev.be.java15.JCaseLabel;
import kiev.ir.java15.RSwitchStat;
import kiev.be.java15.JSwitchStat;
import kiev.ir.java15.RSwitchEnumStat;
import kiev.be.java15.JSwitchEnumStat;
import kiev.ir.java15.RSwitchTypeStat;
import kiev.be.java15.JSwitchTypeStat;
import kiev.ir.java15.RMatchStat;
import kiev.be.java15.JMatchStat;
import kiev.ir.java15.RCatchInfo;
import kiev.be.java15.JCatchInfo;
import kiev.ir.java15.RFinallyInfo;
import kiev.be.java15.JFinallyInfo;
import kiev.ir.java15.RTryStat;
import kiev.be.java15.JTryStat;
import kiev.ir.java15.RSynchronizedStat;
import kiev.be.java15.JSynchronizedStat;
import kiev.ir.java15.RWithStat;
import kiev.be.java15.JWithStat;

import kiev.be.java15.CodeLabel;
import kiev.be.java15.CodeSwitch;
import kiev.be.java15.CodeCatchInfo;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Case", lang=CoreLang)
public class CaseLabel extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(in="this:in()", out="pattern") private static class DFI {
	@dflow(in="this:in")			ENode		val;
	@dflow(in="val", seq="true")	Var[]		pattern;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@virtual typedef This  = CaseLabel;
	@virtual typedef JView = JCaseLabel;
	@virtual typedef RView = RCaseLabel;

	@att public ENode			val;
	@ref public Type			type;
	@att public Var[]			pattern;
	     public CodeLabel		case_label;

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

	public boolean backendCleanup() {
		this.case_label = null;
		return true;
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

@node(name="Switch", lang=CoreLang)
public class SwitchStat extends Block {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="true")	ENode[]			stats;
	@dflow(in="stats")				Label			lblcnt;
	@dflow(in="stats")				Label			lblbrk;
	}
	
	@virtual typedef This  ≤ SwitchStat;
	@virtual typedef JView ≤ JSwitchStat;
	@virtual typedef RView ≤ RSwitchStat;

	@att public ENode					sel;
	@ref public CaseLabel[]				cases;
	@ref public CaseLabel				defCase;
	@att public ENode					sel_to_int;
	@att(copyable=false, ext_data=true)
	     public Label					lblcnt;
	     public CodeSwitch				cosw;

	public SwitchStat() {
		setBreakTarget(true);
	}

	public SwitchStat(int pos, ENode sel) {
		this();
		this.pos = pos;
		this.sel = sel;
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

	public boolean backendCleanup() {
		this.cosw = null;
		this.lblbrk = null;
		this.lblcnt = null;
		return true;
	}
}

@node(name="SwitchEnum", lang=CoreLang)
public class SwitchEnumStat extends SwitchStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="true")	ENode[]			stats;
	@dflow(in="stats")				Label			lblcnt;
	@dflow(in="stats")				Label			lblbrk;
	}
	
	@virtual typedef This  = SwitchEnumStat;
	@virtual typedef JView = JSwitchEnumStat;
	@virtual typedef RView = RSwitchEnumStat;

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

@node(name="SwitchType", lang=CoreLang)
public class SwitchTypeStat extends SwitchStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="true")	ENode[]			stats;
	@dflow(in="stats")				Label			lblcnt;
	@dflow(in="stats")				Label			lblbrk;
	}
	
	@virtual typedef This  = SwitchTypeStat;
	@virtual typedef JView = JSwitchTypeStat;
	@virtual typedef RView = RSwitchTypeStat;

	public SwitchTypeStat() {
	}

	public String toString() { return "switch-type("+sel+")"; }
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (!tp.isReference())
			Kiev.reportError(this, "Expected value of reference type as selector");
	}

}

@node(name="Match", lang=CoreLang)
public class MatchStat extends SwitchStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="true")	ENode[]			stats;
	@dflow(in="stats")				Label			lblcnt;
	@dflow(in="stats")				Label			lblbrk;
	}
	
	@virtual typedef This  = MatchStat;
	@virtual typedef JView = JMatchStat;
	@virtual typedef RView = RMatchStat;

	@att public Var					tmp_var;

	public MatchStat() {
	}

	public String toString() { return "match("+sel+")"; }
	
	public void mainResolveOut() {
		Type tp = sel.getType();
		if (!tp.isReference() || !tp.meta_type.tdecl.isHasCases())
			Kiev.reportError(this, "Expected value of type with cases as selector");
	}

}

@node(name="Catch", lang=CoreLang)
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	public static final CatchInfo[] emptyArray = new CatchInfo[0];

	@virtual typedef This  = CatchInfo;
	@virtual typedef JView = JCatchInfo;
	@virtual typedef RView = RCatchInfo;

	@att public Var				arg;
	@att public ENode			body;
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;

	public CatchInfo() {}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (arg == null) {
			arg = new LVar(0, "e", new TypeNameRef("Exception"), Var.VAR_LOCAL, 0);
			arg.initForEditor();
		}
		super.initForEditor();
	}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		node ?= arg,
		info.checkNodeName(node)
	}

	public boolean backendCleanup() {
		this.handler = null;
		this.code_catcher = null;
		return true;
	}
}

@node(name="Finally", lang=CoreLang)
public class FinallyInfo extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode			body;
	}
	
	@virtual typedef This  = FinallyInfo;
	@virtual typedef JView = JFinallyInfo;
	@virtual typedef RView = RFinallyInfo;

	@att public ENode			body;
	@att public Var				ret_arg;
	     public CodeLabel		subr_label;
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;

	public FinallyInfo() {}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		super.initForEditor();
	}

	public String toString() { return "finally"; }

	public boolean backendCleanup() {
		this.subr_label = null;
		this.handler = null;
		this.code_catcher = null;
		return true;
	}
}

@node(name="Try", lang=CoreLang)
public class TryStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")				ENode			body;
	@dflow(in="this:in", seq="false")	CatchInfo[]		catchers;
	@dflow(in="this:in")				FinallyInfo		finally_catcher;
	}
	
	@virtual typedef This  = TryStat;
	@virtual typedef JView = JTryStat;
	@virtual typedef RView = RTryStat;

	@att public ENode				body;
	@att public CatchInfo[]			catchers;
	@att public FinallyInfo			finally_catcher;
	     public CodeLabel			end_label;

	public TryStat() {}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (catchers.length == 0) {
			catchers += new CatchInfo();
			catchers[0].initForEditor();
		}
		if (finally_catcher == null) {
			finally_catcher = new FinallyInfo();
			finally_catcher.initForEditor();
		}
		super.initForEditor();
	}

	public boolean backendCleanup() {
		this.end_label = null;
		return true;
	}
}

@node(name="Synchronized", lang=CoreLang)
public class SynchronizedStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef This  = SynchronizedStat;
	@virtual typedef JView = JSynchronizedStat;
	@virtual typedef RView = RSynchronizedStat;

	@att public ENode			expr;
	@att public Var				expr_var;
	@att public ENode			body;
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;
	     public CodeLabel		end_label;

	public SynchronizedStat() {}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (expr == null) {
			expr = new ThisExpr();
			expr.initForEditor();
		}
		super.initForEditor();
	}

	public boolean backendCleanup() {
		this.handler = null;
		this.code_catcher = null;
		this.end_label = null;
		return true;
	}
}

@node(name="With", lang=CoreLang)
public class WithStat extends ENode {

	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef This  = WithStat;
	@virtual typedef JView = JWithStat;
	@virtual typedef RView = RWithStat;

	@att public ENode		expr;
	@att public ENode		body;
	@ref public Var			var_or_field;
	     public CodeLabel	end_label;

	public WithStat() {}

	public void initForEditor() {
		if (body == null) {
			body = new Block();
			body.initForEditor();
		}
		if (expr == null) {
			expr = new ThisExpr();
			expr.initForEditor();
		}
		super.initForEditor();
	}

	public boolean backendCleanup() {
		this.end_label = null;
		return true;
	}
}

