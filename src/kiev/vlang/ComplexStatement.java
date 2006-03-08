package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RCaseLabel;
import kiev.be.java.JCaseLabel;
import kiev.ir.java.RSwitchStat;
import kiev.be.java.JSwitchStat;
import kiev.ir.java.RCatchInfo;
import kiev.be.java.JCatchInfo;
import kiev.ir.java.RFinallyInfo;
import kiev.be.java.JFinallyInfo;
import kiev.ir.java.RTryStat;
import kiev.be.java.JTryStat;
import kiev.ir.java.RSynchronizedStat;
import kiev.be.java.JSynchronizedStat;
import kiev.ir.java.RWithStat;
import kiev.be.java.JWithStat;

import kiev.be.java.CodeLabel;
import kiev.be.java.CodeSwitch;
import kiev.be.java.CodeCatchInfo;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class CaseLabel extends ENode implements ScopeOfNames {
	
	@dflow(in="this:in()", out="stats") private static class DFI {
	@dflow(in="this:in", seq="true") Var[]		pattern;
	@dflow(in="pattern", seq="true") ENode[]	stats;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@virtual typedef This  = CaseLabel;
	@virtual typedef VView = VCaseLabel;
	@virtual typedef JView = JCaseLabel;
	@virtual typedef RView = RCaseLabel;

	@att public ENode			val;
	@ref public Type			type;
	@att public NArr<Var>		pattern;
	@att public NArr<ENode>		stats;
	@ref public CodeLabel		case_label;

	@nodeview
	public static final view VCaseLabel of CaseLabel extends VENode {
		public		ENode			val;
		public		Type			type;
		public:ro	NArr<Var>		pattern;
		public:ro	NArr<ENode>		stats;
	}

	public CaseLabel() {}

	public CaseLabel(int pos, ENode val, ENode[] stats) {
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
			if (cl.parent instanceof SwitchStat) {
				ENode sel = ((SwitchStat)cl.parent).sel;
				if (sel != null)
					res = sel.getDFlow().out();
			}
			if (cl.pprev != null) {
				DFState prev = cl.pprev.getDFlow().out();
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
				sb.append(pattern[i].vtype).append(' ').append(pattern[i].name);
				if( i < pattern.length-1 ) sb.append(',');
			}
			sb.append("):");
			return sb.toString();
		}
		return "case "+val+':';
	}

	public ENode addStatement(int i, ENode st) {
		if( st == null ) return null;
		stats.insert(st,i);
		return st;
	}

	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
		Var@ var;
		ASTNode@ n;
	{
		var @= pattern,
		var.name.equals(name),
		node ?= var
	;
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof VarDecl,
			((VarDecl)n).var.name.equals(name),
			node ?= ((VarDecl)n).var
		;	n instanceof LocalStructDecl,
			name.equals(((LocalStructDecl)n).clazz.name.short_name),
			node ?= ((LocalStructDecl)n).clazz
		;	n instanceof TypeDecl,
			name.equals(((TypeDecl)n).getName()),
			node ?= ((TypeDecl)n)
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}
	
	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:");
		else
			dmp.newLine(-1).append("case ").append(val).append(':');
		dmp.newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		return dmp;
	}
}

@node
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
	@virtual typedef VView = VSwitchStat;
	@virtual typedef JView = JSwitchStat;
	@virtual typedef RView = RSwitchStat;

	@att                 public int mode; /* = NORMAL_SWITCH; */
	@att                 public ENode					sel;
	@att                 public NArr<CaseLabel>		cases;
	@att                 public LVarExpr				tmpvar;
	@ref                 public CaseLabel				defCase;
	@ref                 public Field					typehash; // needed for re-resolving
	@att(copyable=false) public Label					lblcnt;
	@att(copyable=false) public Label					lblbrk;
	@att                 public CodeSwitch				cosw;

	@nodeview
	public static final view VSwitchStat of SwitchStat extends VENode {
		public		int						mode;
		public		ENode					sel;
		public:ro	NArr<CaseLabel>			cases;
		public		LVarExpr				tmpvar;
		public		CaseLabel				defCase;
		public		Field					typehash; // needed for re-resolving
		public:ro	Label					lblcnt;
		public:ro	Label					lblbrk;
	}

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

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("switch").space().append('(')
			.append(sel).space().append(')').space().append('{').newLine(1);
		for(int i=0; i < cases.length; i++) dmp.append(cases[i]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

@node
public class CatchInfo extends ENode implements ScopeOfNames {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	Var				arg;
	@dflow(in="arg")		ENode			body;
	}
	
	static CatchInfo[] emptyArray = new CatchInfo[0];

	@virtual typedef This  = CatchInfo;
	@virtual typedef VView = VCatchInfo;
	@virtual typedef JView = JCatchInfo;
	@virtual typedef RView = RCatchInfo;

	@att public Var				arg;
	@att public ENode			body;
	@att public CodeLabel		handler;
	@att public CodeCatchInfo	code_catcher;

	@nodeview
	public static final view VCatchInfo of CatchInfo extends VENode {
		public Var				arg;
		public ENode			body;
	}
	
	public CatchInfo() {}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
	{
		node ?= arg, ((Var)node).name.equals(name)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		arg.toJavaDecl(dmp).space().append(')').space().append(body);
		return dmp;
	}
}

@node
public class FinallyInfo extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode			body;
	}
	
	@virtual typedef This  = FinallyInfo;
	@virtual typedef VView = VFinallyInfo;
	@virtual typedef JView = JFinallyInfo;
	@virtual typedef RView = RFinallyInfo;

	@att public ENode			body;
	@att public Var				ret_arg;
	@att public CodeLabel		subr_label;
	@att public CodeLabel		handler;
	@att public CodeCatchInfo	code_catcher;

	@nodeview
	public static final view VFinallyInfo of FinallyInfo extends VENode {
		public ENode		body;
		public Var			ret_arg;
	}
	
	public FinallyInfo() {}

	public String toString() { return "finally"; }

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("finally").space().append(body).newLine();
		return dmp;
	}

}

@node
public class TryStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")				ENode			body;
	@dflow(in="this:in", seq="false")	CatchInfo[]		catchers;
	@dflow(in="this:in")				FinallyInfo		finally_catcher;
	}
	
	@virtual typedef This  = TryStat;
	@virtual typedef VView = VTryStat;
	@virtual typedef JView = JTryStat;
	@virtual typedef RView = RTryStat;

	@att public ENode				body;
	@att public NArr<CatchInfo>		catchers;
	@att public FinallyInfo			finally_catcher;
	@att public CodeLabel			end_label;

	@nodeview
	public static final view VTryStat of TryStat extends VENode {
		public		ENode				body;
		public:ro	NArr<CatchInfo>		catchers;
		public		FinallyInfo			finally_catcher;
	}

	public TryStat() {}

	public Dumper toJava(Dumper dmp) {
		dmp.append("try").space().append(body).newLine();
		for(int i=0; i < catchers.length; i++)
			dmp.append(catchers[i]).newLine();
		if(finally_catcher != null)
			dmp.append(finally_catcher).newLine();
		return dmp;
	}

}

@node
public class SynchronizedStat extends ENode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef This  = SynchronizedStat;
	@virtual typedef VView = VSynchronizedStat;
	@virtual typedef JView = JSynchronizedStat;
	@virtual typedef RView = RSynchronizedStat;

	@att public ENode			expr;
	@att public Var				expr_var;
	@att public ENode			body;
	@att public CodeLabel		handler;
	@att public CodeCatchInfo	code_catcher;
	@att public CodeLabel		end_label;

	@nodeview
	public static final view VSynchronizedStat of SynchronizedStat extends VENode {
		public ENode			expr;
		public Var				expr_var;
		public ENode			body;
	}

	public SynchronizedStat() {}

	public Dumper toJava(Dumper dmp) {
		dmp.append("synchronized").space().append('(').space().append(expr)
			.space().append(')').forsed_space().append(body).newLine();
		return dmp;
	}

}

@node
public class WithStat extends ENode {

	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="expr")		ENode		body;
	}
	
	@virtual typedef This  = WithStat;
	@virtual typedef VView = VWithStat;
	@virtual typedef JView = JWithStat;
	@virtual typedef RView = RWithStat;

	@att public ENode		expr;
	@att public ENode		body;
	@ref public LvalDNode	var_or_field;
	@att public CodeLabel	end_label;

	@nodeview
	public static final view VWithStat of WithStat extends VENode {
		public ENode		expr;
		public ENode		body;
		public LvalDNode	var_or_field;
	}

	public WithStat() {}

	public Dumper toJava(Dumper dmp) {
		dmp.append("/*with ").space().append('(').space().append(expr)
			.space().append(")*/").forsed_space().append(body).newLine();
		return dmp;
	}
}

