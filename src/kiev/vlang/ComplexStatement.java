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
 * @version $Revision$
 *
 */

@node(name="Case")
public class CaseLabel extends ENode implements ScopeOfNames {
	
	@dflow(in="this:in()", out="stats") private static class DFI {
	@dflow(in="this:in", seq="true") Var[]		pattern;
	@dflow(in="pattern", seq="true") ASTNode[]	stats;
	}
	
	public static final CaseLabel[] emptyArray = new CaseLabel[0];

	@virtual typedef This  = CaseLabel;
	@virtual typedef VView = VCaseLabel;
	@virtual typedef JView = JCaseLabel;
	@virtual typedef RView = RCaseLabel;

	@att public ENode			val;
	@ref public Type			type;
	@att public Var[]			pattern;
	@att public ASTNode[]		stats;
	     public CodeLabel		case_label;

	@nodeview
	public static final view VCaseLabel of CaseLabel extends VENode {
		public		ENode			val;
		public		Type			type;
		public:ro	Var[]			pattern;
		public:ro	ASTNode[]		stats;
	}

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
			if (cl.pprev() != null) {
				DFState prev = ((ASTNode)cl.pprev()).getDFlow().out();
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

	public rule resolveNameR(ASTNode@ node, ResInfo info, String name)
		Var@ var;
		ASTNode@ n;
	{
		var @= pattern,
		var.id.equals(name),
		node ?= var
	;
		n @= new SymbolIterator(this.stats, info.space_prev),
		n.hasName(name),
		node ?= n
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof Var && ((Var)n).isForward() && ((Var)n).id.equals(name),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		n.getType().resolveNameAccessR(node,info,name)
	}
	
	public Dumper toJava(Dumper dmp) {
		if( val == null )
			dmp.newLine(-1).append("default:");
		else
			dmp.newLine(-1).append("case ").append(val).append(':');
		dmp.newLine(1);
		foreach (ASTNode s; stats)
			s.toJava(dmp);
		return dmp;
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
	@virtual typedef VView = VSwitchStat;
	@virtual typedef JView = JSwitchStat;
	@virtual typedef RView = RSwitchStat;

	@att                 public int mode; /* = NORMAL_SWITCH; */
	@att                 public ENode					sel;
	@att                 public CaseLabel[]			cases;
	@att                 public LVarExpr				tmpvar;
	@ref                 public CaseLabel				defCase;
	@ref                 public Field					typehash; // needed for re-resolving
	@att(copyable=false) public Label					lblcnt;
	@att(copyable=false) public Label					lblbrk;
	                     public CodeSwitch				cosw;

	@nodeview
	public static final view VSwitchStat of SwitchStat extends VENode {
		public		int						mode;
		public		ENode					sel;
		public:ro	CaseLabel[]				cases;
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

@node(name="Catch")
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
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;

	@nodeview
	public static final view VCatchInfo of CatchInfo extends VENode {
		public Var				arg;
		public ENode			body;
	}
	
	public CatchInfo() {}

	public String toString() {
		return "catch( "+arg+" )";
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
	{
		node ?= arg, ((Var)node).id.equals(name)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.newLine().append("catch").space().append('(').space();
		arg.toJavaDecl(dmp).space().append(')').space().append(body);
		return dmp;
	}
}

@node(name="Finally")
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
	     public CodeLabel		subr_label;
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;

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

@node(name="Try")
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
	@att public CatchInfo[]			catchers;
	@att public FinallyInfo			finally_catcher;
	     public CodeLabel			end_label;

	@nodeview
	public static final view VTryStat of TryStat extends VENode {
		public		ENode				body;
		public:ro	CatchInfo[]			catchers;
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

@node(name="Synchronized")
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
	     public CodeLabel		handler;
	     public CodeCatchInfo	code_catcher;
	     public CodeLabel		end_label;

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

@node(name="With")
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
	     public CodeLabel	end_label;

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

