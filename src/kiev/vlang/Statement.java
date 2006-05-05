package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.types.*;

import kiev.ir.java15.RNode;
import kiev.be.java15.JNode;
import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RInlineMethodStat;
import kiev.be.java15.JInlineMethodStat;
import kiev.ir.java15.RBlock;
import kiev.be.java15.JBlock;
import kiev.ir.java15.RExprStat;
import kiev.be.java15.JExprStat;
import kiev.ir.java15.RReturnStat;
import kiev.be.java15.JReturnStat;
import kiev.ir.java15.RThrowStat;
import kiev.be.java15.JThrowStat;
import kiev.ir.java15.RIfElseStat;
import kiev.be.java15.JIfElseStat;
import kiev.ir.java15.RCondStat;
import kiev.be.java15.JCondStat;
import kiev.ir.java15.RLabeledStat;
import kiev.be.java15.JLabeledStat;
import kiev.ir.java15.RBreakStat;
import kiev.be.java15.JBreakStat;
import kiev.ir.java15.RContinueStat;
import kiev.be.java15.JContinueStat;
import kiev.ir.java15.RGotoStat;
import kiev.be.java15.JGotoStat;
import kiev.ir.java15.RGotoCaseStat;
import kiev.be.java15.JGotoCaseStat;

import kiev.vlang.Method.VMethod;
import kiev.vlang.LoopStat.VLoopStat;
import kiev.vlang.LabeledStat.VLabeledStat;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class InlineMethodStat extends ENode implements ScopeOfNames {
	
	@dflow(in="root()", out="this:out()") private static class DFI {}

	public static final class ParamRedir {
		public FormPar		old_var;
		public FormPar		new_var;
		public ParamRedir(FormPar o, FormPar n) { old_var=o; new_var=n; }
	};

	@virtual typedef This  = InlineMethodStat;
	@virtual typedef VView = VInlineMethodStat;
	@virtual typedef JView = JInlineMethodStat;
	@virtual typedef RView = RInlineMethodStat;

	@att public Method			method;
	@ref public ParamRedir[]	params_redir;

	@nodeview
	public static final view VInlineMethodStat of InlineMethodStat extends VENode {
		public Method			method;
		public ParamRedir[]		params_redir;
	}

	public InlineMethodStat() {}

	public InlineMethodStat(int pos, Method m, Method in) {
		this.pos = pos;
		method = m;
		method.inlined_by_dispatcher = true;
		assert(m.params.length == in.params.length);
		params_redir = new ParamRedir[m.params.length];
		for (int i=0; i < m.params.length; i++) {
			params_redir[i] = new ParamRedir(m.params[i],in.params[i]);
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
		ParamRedir@	redir;
	{
		redir @= params_redir,
		redir.old_var.id.equals(name),
		$cut,
		node ?= redir.new_var
	}

	static class InlineMethodStatDFFuncIn extends DFFunc {
		final int res_idx;
		InlineMethodStatDFFuncIn(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl;
			DFState in = DFState.makeNewState();
			for(int i=0; i < node.params_redir.length; i++) {
				in = in.declNode(node.params_redir[i].new_var);
				in = in.addNodeType(new LvalDNode[]{node.params_redir[i].new_var},node.method.params[i].type);
			}
			res = in;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncIn(dfi);
	}

	static class InlineMethodStatDFFuncOut extends DFFunc {
		final int res_idx;
		InlineMethodStatDFFuncOut(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl;
			DataFlowInfo pdfi = ((ASTNode)node.parent()).getDFlow();
			res = DFFunc.calc(pdfi.getSocket(node.pslot().name).func_in, pdfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncOut(dfi);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		foreach (ParamRedir redir; params_redir)
			dmp.append("/* ")
			.append(redir.old_var.type.toString()).space().append(redir.old_var)
			.append('=').append(redir.new_var)
			.append(';').append(" */").newLine();
		dmp.append("/* Body of method "+method+" */").newLine();
		if (method.body == null)
			dmp.append(';');
		else
			dmp.append(method.body);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

@node
public class ExprStat extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ExprStat;
	@virtual typedef VView = VExprStat;
	@virtual typedef JView = JExprStat;
	@virtual typedef RView = RExprStat;

	@att public ENode	expr;

	@nodeview
	public static final view VExprStat of ExprStat extends VENode {
		public ENode		expr;
	}

	public ExprStat() {}

	public ExprStat(ENode expr) {
		this.expr = expr;
		if (expr != null)
			this.pos = expr.pos;
	}

	public ExprStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
	}

	public String toString() {
		if (expr != null)
			return expr+";";
		else
			return ";";
	}

	public Dumper toJava(Dumper dmp) {
		if( isHidden() ) dmp.append("/* ");
		if (expr != null)
			expr.toJava(dmp);
		dmp.append(';');
		if( isHidden() ) dmp.append(" */");
		return dmp.newLine();
	}
}

@node
public class ReturnStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ReturnStat;
	@virtual typedef VView = VReturnStat;
	@virtual typedef JView = JReturnStat;
	@virtual typedef RView = RReturnStat;

	@att public ENode	expr;

	@nodeview
	public static final view VReturnStat of ReturnStat extends VENode {
		public ENode		expr;
	}

	public ReturnStat() {}

	public ReturnStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public static void autoReturn(Type reqType, RENode expr) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(reqType, fun ()->ENode { return new ReturnStat(expr.pos, ~expr.getENode()); });
	}

	public static void autoReturn(Type reqType, ENode expr) {
		if (expr.parent() instanceof ReturnStat)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(reqType, fun ()->ENode { return new ReturnStat(expr.pos, ~expr); });
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("return");
		if( expr != null )
			dmp.space().append(expr);
		return dmp.append(';').newLine();
	}
}

@node
public class ThrowStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ThrowStat;
	@virtual typedef VView = VThrowStat;
	@virtual typedef JView = JThrowStat;
	@virtual typedef RView = RThrowStat;

	@att public ENode	expr;

	@nodeview
	public static final view VThrowStat of ThrowStat extends VENode {
		public ENode		expr;
	}

	public ThrowStat() {}

	public ThrowStat(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("throw").space().append(expr).append(';').newLine();
	}
}

@node
public class IfElseStat extends ENode {
	
	@dflow(out="join thenSt elseSt") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		thenSt;
	@dflow(in="cond:false")	ENode		elseSt;
	}

	@virtual typedef This  = IfElseStat;
	@virtual typedef VView = VIfElseStat;
	@virtual typedef JView = JIfElseStat;
	@virtual typedef RView = RIfElseStat;

	@att public ENode			cond;
	@att public ENode			thenSt;
	@att public ENode			elseSt;

	@nodeview
	public static final view VIfElseStat of IfElseStat extends VENode {
		public ENode		cond;
		public ENode		thenSt;
		public ENode		elseSt;
	}
	
	public IfElseStat() {}
	
	public IfElseStat(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		this.pos = pos;
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if(").space().append(cond).space()
			.append(')');
		if( thenSt instanceof Block || thenSt instanceof InlineMethodStat) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(thenSt);
		if( thenSt instanceof Block || thenSt instanceof InlineMethodStat) dmp.newLine();
		else dmp.newLine(-1);
		if( elseSt != null ) {
			dmp.append("else");
			if( elseSt instanceof IfElseStat || elseSt instanceof Block || elseSt instanceof InlineMethodStat ) dmp.forsed_space();
			else dmp.newLine(1);
			dmp.append(elseSt).newLine();
			if( elseSt instanceof IfElseStat || elseSt instanceof Block || elseSt instanceof InlineMethodStat ) dmp.newLine();
			else dmp.newLine(-1);
		}
		return dmp;
	}
}

@node
public class CondStat extends ENode {
	
	@dflow(out="cond:true") private static class DFI {
	@dflow(in="this:in")		ENode		cond;
	@dflow(in="cond:false")		ENode		message;
	}

	@virtual typedef This  = CondStat;
	@virtual typedef VView = VCondStat;
	@virtual typedef JView = JCondStat;
	@virtual typedef RView = RCondStat;

	@att public ENode			cond;
	@att public ENode			message;

	@nodeview
	public static final view VCondStat of CondStat extends VENode {
		public ENode		cond;
		public ENode		message;
	}
	
	public CondStat() {}

	public CondStat(int pos, ENode cond, ENode message) {
		this.pos = pos;
		this.cond = cond;
		this.message = message;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if( !(").append(cond)
			.append(") ) throw new kiev.stdlib.AssertionFailedException(")
			.append(message).append(");").newLine();
		return dmp;
	}
}

@node
public class LabeledStat extends ENode implements Named {
	
	@dflow(out="stat") private static class DFI {
	@dflow(in="this:in")	Label			lbl;
	@dflow(in="lbl")		ENode			stat;
	}

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	@virtual typedef This  = LabeledStat;
	@virtual typedef VView = VLabeledStat;
	@virtual typedef JView = JLabeledStat;
	@virtual typedef RView = RLabeledStat;

	@att                 public Symbol			id;
	@att(copyable=false) public Label			lbl;
	@att                 public ENode			stat;

	@nodeview
	public static final view VLabeledStat of LabeledStat extends VENode {
		public Symbol			id;
		public Label			lbl;
		public ENode			stat;
	}
	
	public LabeledStat() {
		this.lbl = new Label();
	}
	
	public Symbol getName() { return id; }

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(id).append(':').newLine(1).append(stat);
	}
}

@node
public class BreakStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = BreakStat;
	@virtual typedef VView = VBreakStat;
	@virtual typedef JView = JBreakStat;
	@virtual typedef RView = RBreakStat;

	@att public SymbolRef	ident;
	@ref public Label		dest;

	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}

	@nodeview
	public static final view VBreakStat of BreakStat extends VENode {
		public SymbolRef		ident;
		public Label			dest;

		public boolean mainResolveIn() {
			ASTNode p;
			if (dest != null) {
				dest.delLink((BreakStat)this);
				dest = null;
			}
			if( ident == null ) {
				for(p=parent(); !(p instanceof Method || p.isBreakTarget()); p = p.parent() );
				if( p instanceof Method || p == null ) {
					Kiev.reportError(this,"Break not within loop/switch statement");
				} else {
					if (p instanceof LoopStat) {
						Label l = p.lblbrk;
						if (l != null) {
							dest = l;
							l.addLink((BreakStat)this);
						}
					}
				}
			} else {
		label_found:
				for(p=parent(); !(p instanceof Method) ; p=p.parent() ) {
					if (p instanceof LabeledStat && p.id.equals(ident.name))
						throw new RuntimeException("Label "+ident+" does not refer to break target");
					if (!p.isBreakTarget()) continue;
					ASTNode pp = p;
					for(p=p.parent(); p instanceof LabeledStat; p = p.parent()) {
						if (p.id.equals(ident.name)) {
							p = pp;
							break label_found;
						}
					}
					p = pp;
				}
				if( p instanceof Method || p == null) {
					Kiev.reportError(this,"Break not within loop/switch statement");
				} else {
					if (p instanceof LoopStat) {
						Label l = p.lblbrk;
						if (l != null) {
							dest = l;
							l.addLink((BreakStat)this);
						}
					}
				}
			}
			return false; // don't pre-resolve
		}
	}
	
	public BreakStat() {}
	
	public Dumper toJava(Dumper dmp) {
		dmp.append("break");
		if( ident != null && ident.name != "" )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class ContinueStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = ContinueStat;
	@virtual typedef VView = VContinueStat;
	@virtual typedef JView = JContinueStat;
	@virtual typedef RView = RContinueStat;

	@att public SymbolRef	ident;
	@ref public Label		dest;

	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}

	@nodeview
	public static final view VContinueStat of ContinueStat extends VENode {
		public SymbolRef		ident;
		public Label			dest;

		public boolean mainResolveIn() {
			ASTNode p;
			if (dest != null) {
				dest.delLink((ContinueStat)this);
				dest = null;
			}
			if( ident == null ) {
				for(p=parent(); !(p instanceof LoopStat || p instanceof Method); p = p.parent() );
				if( p instanceof Method || p == null ) {
					Kiev.reportError(this,"Continue not within loop statement");
				} else {
					if (p instanceof LoopStat) {
						Label l = p.lblcnt;
						if (l != null) {
							dest = l;
							l.addLink((ContinueStat)this);
						}
					}
				}
			} else {
		label_found:
				for(p=parent(); !(p instanceof Method) ; p=p.parent() ) {
					if( p instanceof LabeledStat && p.id.equals(ident.name) )
						throw new RuntimeException("Label "+ident+" does not refer to continue target");
					if !(p instanceof LoopStat) continue;
					ASTNode pp = p;
					for(p=p.parent(); p instanceof LabeledStat; p = p.parent()) {
						if( p.id.equals(ident.name) ) {
							p = pp;
							break label_found;
						}
					}
					p = pp;
				}
				if( p instanceof Method || p == null) {
					Kiev.reportError(this,"Continue not within loop statement");
				} else {
					if (p instanceof LoopStat) {
						Label l = p.lblcnt;
						if (l != null) {
							dest = l;
							l.addLink((ContinueStat)this);
						}
					}
				}
			}
			return false; // don't pre-resolve
		}
	}
	
	public ContinueStat() {}
	
	public Dumper toJava(Dumper dmp) {
		dmp.append("continue");
		if( ident != null && ident.name != "" )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class GotoStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = GotoStat;
	@virtual typedef VView = VGotoStat;
	@virtual typedef JView = JGotoStat;
	@virtual typedef RView = RGotoStat;

	@att public SymbolRef	ident;
	@ref public Label		dest;

	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}

	@nodeview
	public static final view VGotoStat of GotoStat extends VENode {
		public SymbolRef		ident;
		public Label			dest;

		public boolean mainResolveIn() {
			if (dest != null) {
				dest.delLink((GotoStat)this);
				dest = null;
			}
			LabeledStat[] stats = resolveStat(ident.name,ctx_method.body, LabeledStat.emptyArray);
			if( stats.length == 0 ) {
				Kiev.reportError(this,"Label "+ident+" unresolved");
				return false;
			}
			if( stats.length > 1 ) {
				Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
			}
			LabeledStat stat = stats[0];
			if( stat == null ) {
				Kiev.reportError(this,"Label "+ident+" unresolved");
				return false;
			}
			dest = stat.lbl;
			dest.addLink((GotoStat)this);
			return false; // don't pre-resolve
		}
	}

	public GotoStat() {}
	
	public static LabeledStat[] resolveStat(String name, ASTNode st, LabeledStat[] stats) {
		int i;
		switch( st ) {
		case SwitchStat:
		{
			SwitchStat bst = (SwitchStat)st;
			for(int j=0; j < bst.cases.length; j++ ) {
				CaseLabel cl = (CaseLabel)bst.cases[j];
				for(i=0; i < cl.stats.length; i++ ) {
					stats = resolveStat(name,cl.stats[i],stats);
				}
			}
		}
			break;
		case Block:
		{
			Block bst = (Block)st;
			for(i=0; i < bst.stats.length; i++ ) {
				stats = resolveStat(name,bst.stats[i],stats);
			}
		}
			break;
		case TryStat:
		{
			TryStat tst = (TryStat)st;
			stats = resolveStat(name,tst.body,stats);
			for(i=0; i < tst.catchers.length; i++) {
				stats = resolveStat(name,((CatchInfo)tst.catchers[i]).body,stats);
			}
		}
			break;
		case WhileStat:
		{
			WhileStat wst = (WhileStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case DoWhileStat:
		{
			DoWhileStat wst = (DoWhileStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case ForStat:
		{
			ForStat wst = (ForStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case ForEachStat:
		{
			ForEachStat wst = (ForEachStat)st;
			stats = resolveStat(name,wst.body,stats);
		}
			break;
		case IfElseStat:
		{
			IfElseStat wst = (IfElseStat)st;
			stats = resolveStat(name,wst.thenSt,stats);
			if( wst.elseSt != null )
				stats = resolveStat(name,wst.elseSt,stats);
		}
			break;
		case LabeledStat:
		{
			LabeledStat lst = (LabeledStat)st;
			if( lst.id.equals(name) ) {
				stats = (LabeledStat[])Arrays.appendUniq(stats,lst);
			}
			stats = resolveStat(name,lst.stat,stats);
		}
			break;
		case GotoStat:			break;
		case GotoCaseStat:		break;
		case ReturnStat:		break;
		case ThrowStat:			break;
		case ExprStat:			break;
		case BreakStat:			break;
		case ContinueStat:		break;
		case DNode:				break;
		case SNode:				break;
		default:
			Kiev.reportWarning(st,"Unknown statement in label lookup: "+st.getClass());
		}
		return stats;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("goto").space().append(ident).append(';').newLine();
	}
}

@node
public class GotoCaseStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = GotoCaseStat;
	@virtual typedef VView = VGotoCaseStat;
	@virtual typedef JView = JGotoCaseStat;
	@virtual typedef RView = RGotoCaseStat;

	@att public ENode		expr;
	@ref public SwitchStat	sw;

	@nodeview
	public static final view VGotoCaseStat of GotoCaseStat extends VENode {
		public ENode		expr;
		public SwitchStat	sw;
	}
	
	public GotoCaseStat() {}
	
	public Dumper toJava(Dumper dmp) {
		dmp.append("goto");
		if( expr != null )
			dmp.append(" case ").append(expr);
		else
			dmp.space().append("default");
		return dmp.append(';').newLine();
	}
}

