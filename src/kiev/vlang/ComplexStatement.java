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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RCaseLabel;
import kiev.be.java15.JCaseLabel;
import kiev.ir.java15.RSwitchStat;
import kiev.be.java15.JSwitchStat;
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

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

@node(name="Case")
public class CaseLabel extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(in="this:in()", out="stats") private static class DFI {
	@dflow(in="this:in", seq="true") Var[]		pattern;
	@dflow(in="pattern", seq="true") ASTNode[]	stats;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@virtual typedef This  = CaseLabel;
	@virtual typedef JView = JCaseLabel;
	@virtual typedef RView = RCaseLabel;

	@att public ENode			val;
	@ref public Type			type;
	@att public Var[]			pattern;
	@att public ASTNode[]		stats;
	     public CodeLabel		case_label;

	public CaseLabel() {}

	public CaseLabel(int pos, ENode val, ASTNode[] stats) {
		this.pos = pos;
		this.val = val;
		this.stats.addAll(stats);
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
					res = sel.getDFlow().out();
			}
			if (ANode.getPrevNode(cl) != null) {
				DFState prev = ((ASTNode)ANode.getPrevNode(cl)).getDFlow().out();
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
				sb.append(pattern[i].vtype).append(' ').append(pattern[i].id);
				if( i < pattern.length-1 ) sb.append(',');
			}
			sb.append("):");
			return sb.toString();
		}
		return "case "+val+':';
	}

	public ASTNode addStatement(int i, ASTNode st) {
		if( st == null ) return null;
		stats.insert(i,st);
		return st;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
		DNode@ dn;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof DeclGroup,
			dn @= ((DeclGroup)n).decls,
			info.checkNodeName(dn),
			info.check(dn),
			node ?= dn
		;
			info.checkNodeName(n),
			node ?= n
		;
			info.isForwardsAllowed(),
			n instanceof Var && ((Var)n).isForward() && info.checkNodeName(n),
			info.enterForward((Var)n) : info.leaveForward((Var)n),
			n.getType().resolveNameAccessR(node,info)
		}
	;
		n @= pattern,
		info.checkNodeName(n),
		node ?= n
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof DeclGroup,
			((DeclGroup)n).resolveMethodR(node, info, mt)
		;
			n instanceof Var && ((Var)n).isForward(),
			info.enterForward((Var)n) : info.leaveForward((Var)n),
			((Var)n).getType().resolveCallAccessR(node,info,mt)
		}
	;
		info.isForwardsAllowed(),
		n @= pattern,
		n instanceof Var && ((Var)n).isForward(),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		((Var)n).getType().resolveCallAccessR(node,info,mt)
	}

}

@node(name="Switch")
public class SwitchStat extends ENode {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")			ENode			sel;
	@dflow(in="sel", seq="false")	CaseLabel[]		cases;
	@dflow(in="cases")				Label			lblcnt;
	@dflow(in="cases")				Label			lblbrk;
	}
	
	public static final int NORMAL_SWITCH = 0;
	public static final int PIZZA_SWITCH = 1;
	public static final int TYPE_SWITCH = 2;
	public static final int ENUM_SWITCH = 3;

	@virtual typedef This  = SwitchStat;
	@virtual typedef JView = JSwitchStat;
	@virtual typedef RView = RSwitchStat;

	                     public int mode; /* = NORMAL_SWITCH; */
	@att                 public ENode					sel;
	@att                 public CaseLabel[]			cases;
	@att                 public LVarExpr				tmpvar;
	@ref                 public CaseLabel				defCase;
	@ref                 public Field					typehash; // needed for re-resolving
	@att(copyable=false) public Label					lblcnt;
	@att(copyable=false) public Label					lblbrk;
	                     public CodeSwitch				cosw;

	public SwitchStat() {
		setBreakTarget(true);
		this.lblcnt = new Label();
		this.lblbrk = new Label();
	}

	public SwitchStat(int pos, ENode sel, CaseLabel[] cases) {
		this();
		this.pos = pos;
		this.sel = sel;
		this.cases.addAll(cases);
		defCase = null;
	}

	public String toString() { return "switch("+sel+")"; }
}

@node(name="Catch")
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	static CatchInfo[] emptyArray = new CatchInfo[0];

	@virtual typedef This  = CatchInfo;
	@virtual typedef JView = JCatchInfo;
	@virtual typedef RView = RCatchInfo;

	@att public Var				arg;
	@att public ENode			body;
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;

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

@node(name="Finally")
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

	public String toString() { return "finally"; }
}

@node(name="Try")
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
}

@node(name="Synchronized")
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
}

@node(name="With")
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
}

