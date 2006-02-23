package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.types.*;

import kiev.vlang.Method.MethodView;
import kiev.vlang.LoopStat.LoopStatView;
import kiev.vlang.LabeledStat.LabeledStatView;
import kiev.vlang.SwitchStat.SwitchStatView;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RInlineMethodStat;
import kiev.be.java.JInlineMethodStat;
import kiev.ir.java.RBlock;
import kiev.be.java.JBlock;
import kiev.ir.java.RExprStat;
import kiev.be.java.JExprStat;
import kiev.ir.java.RReturnStat;
import kiev.be.java.JReturnStat;
import kiev.ir.java.RThrowStat;
import kiev.be.java.JThrowStat;
import kiev.ir.java.RIfElseStat;
import kiev.be.java.JIfElseStat;
import kiev.ir.java.RCondStat;
import kiev.be.java.JCondStat;
import kiev.ir.java.RLabeledStat;
import kiev.be.java.JLabeledStat;
import kiev.ir.java.RBreakStat;
import kiev.be.java.JBreakStat;
import kiev.ir.java.RContinueStat;
import kiev.be.java.JContinueStat;
import kiev.ir.java.RGotoStat;
import kiev.be.java.JGotoStat;
import kiev.ir.java.RGotoCaseStat;
import kiev.be.java.JGotoCaseStat;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public class InlineMethodStat extends ENode implements ScopeOfNames {
	
	@dflow(in="root()", out="this:out()") private static class DFI {}

	public static final class ParamRedir {
		public FormPar		old_var;
		public FormPar		new_var;
		public ParamRedir(FormPar o, FormPar n) { old_var=o; new_var=n; }
	};

	@virtual typedef This  = InlineMethodStat;
	@virtual typedef NImpl = InlineMethodStatImpl;
	@virtual typedef VView = VInlineMethodStat;
	@virtual typedef JView = JInlineMethodStat;
	@virtual typedef RView = RInlineMethodStat;

	@nodeimpl
	public static final class InlineMethodStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = InlineMethodStat;
		@att public Method			method;
		@ref public ParamRedir[]	params_redir;
	}
	@nodeview
	public static view InlineMethodStatView of InlineMethodStatImpl extends ENodeView {
		public Method			method;
		public ParamRedir[]		params_redir;
	}
	@nodeview
	public static final view VInlineMethodStat of InlineMethodStatImpl extends InlineMethodStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public InlineMethodStat() {
		super(new InlineMethodStatImpl());
	}

	public InlineMethodStat(int pos, Method m, Method in) {
		this();
		this.pos = pos;
		method = m;
		method.inlined_by_dispatcher = true;
		assert(m.params.length == in.params.length);
		params_redir = new ParamRedir[m.params.length];
		for (int i=0; i < m.params.length; i++) {
			params_redir[i] = new ParamRedir(m.params[i],in.params[i]);
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		ParamRedir@	redir;
	{
		redir @= params_redir,
		redir.old_var.name.equals(name),
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
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl.getNode();
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
			InlineMethodStat node = (InlineMethodStat)dfi.node_impl.getNode();
			DataFlowInfo pdfi = node.parent.getDFlow();
			res = DFFunc.calc(pdfi.getSocket(node.pslot.name).func_in, pdfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new InlineMethodStatDFFuncOut(dfi);
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

@nodeset
public class ExprStat extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ExprStat;
	@virtual typedef NImpl = ExprStatImpl;
	@virtual typedef VView = VExprStat;
	@virtual typedef JView = JExprStat;
	@virtual typedef RView = RExprStat;

	@nodeimpl
	public static final class ExprStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = ExprStat;
		@att public ENode	expr;
	}
	@nodeview
	public static view ExprStatView of ExprStatImpl extends ENodeView {
		public ENode		expr;
	}
	@nodeview
	public static final view VExprStat of ExprStatImpl extends ExprStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public ExprStat() {
		super(new ExprStatImpl());
	}

	public ExprStat(ENode expr) {
		super(new ExprStatImpl());
		this.expr = expr;
		if (expr != null)
			this.pos = expr.pos;
	}

	public ExprStat(int pos, ENode expr) {
		this();
		this.pos = pos;
		this.expr = expr;
	}

	public String toString() {
		if (expr != null)
			return expr+";";
		else
			return ";";
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

@nodeset
public class ReturnStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ReturnStat;
	@virtual typedef NImpl = ReturnStatImpl;
	@virtual typedef VView = VReturnStat;
	@virtual typedef JView = JReturnStat;
	@virtual typedef RView = RReturnStat;

	@nodeimpl
	public static final class ReturnStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = ReturnStat;
		@att public ENode	expr;
	}
	@nodeview
	public static view ReturnStatView of ReturnStatImpl extends ENodeView {
		public ENode		expr;
	}
	@nodeview
	public static final view VReturnStat of ReturnStatImpl extends ReturnStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public ReturnStat() {
		super(new ReturnStatImpl());
	}

	public ReturnStat(int pos, ENode expr) {
		this();
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}
	
	public static void autoReturn(Type reqType, ENodeView expr) {
		if (expr.parent instanceof ReturnStatView)
			return;
		expr.setAutoReturnable(false);
		expr.replaceWithResolve(reqType, fun ()->ENode { return new ReturnStat(expr.pos, ~expr.getENode()); });
	}

	public static void autoReturn(Type reqType, ENode expr) {
		if (expr.parent_node instanceof ReturnStat)
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

@nodeset
public class ThrowStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = ThrowStat;
	@virtual typedef NImpl = ThrowStatImpl;
	@virtual typedef VView = VThrowStat;
	@virtual typedef JView = JThrowStat;
	@virtual typedef RView = RThrowStat;

	@nodeimpl
	public static final class ThrowStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = ThrowStat;
		@att public ENode	expr;
	}
	@nodeview
	public static view ThrowStatView of ThrowStatImpl extends ENodeView {
		public ENode		expr;
	}
	@nodeview
	public static final view VThrowStat of ThrowStatImpl extends ThrowStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public ThrowStat() {
		super(new ThrowStatImpl());
	}

	public ThrowStat(int pos, ENode expr) {
		this();
		this.pos = pos;
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("throw").space().append(expr).append(';').newLine();
	}
}

@nodeset
public class IfElseStat extends ENode {
	
	@dflow(out="join thenSt elseSt") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		thenSt;
	@dflow(in="cond:false")	ENode		elseSt;
	}

	@virtual typedef This  = IfElseStat;
	@virtual typedef NImpl = IfElseStatImpl;
	@virtual typedef VView = VIfElseStat;
	@virtual typedef JView = JIfElseStat;
	@virtual typedef RView = RIfElseStat;

	@nodeimpl
	public static class IfElseStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = IfElseStat;
		@att public ENode			cond;
		@att public ENode			thenSt;
		@att public ENode			elseSt;
	}
	@nodeview
	public static view IfElseStatView of IfElseStatImpl extends ENodeView {
		public ENode		cond;
		public ENode		thenSt;
		public ENode		elseSt;
	}
	@nodeview
	public static final view VIfElseStat of IfElseStatImpl extends IfElseStatView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public IfElseStat() {
		super(new IfElseStatImpl());
	}
	
	public IfElseStat(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		this();
		this.pos = pos;
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

@nodeset
public class CondStat extends ENode {
	
	@dflow(out="cond:true") private static class DFI {
	@dflow(in="this:in")		ENode		cond;
	@dflow(in="cond:false")		ENode		message;
	}

	@virtual typedef This  = CondStat;
	@virtual typedef NImpl = CondStatImpl;
	@virtual typedef VView = VCondStat;
	@virtual typedef JView = JCondStat;
	@virtual typedef RView = RCondStat;

	@nodeimpl
	public static class CondStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = CondStat;
		@att public ENode			cond;
		@att public ENode			message;
	}
	@nodeview
	public static view CondStatView of CondStatImpl extends ENodeView {
		public ENode		cond;
		public ENode		message;
	}
	@nodeview
	public static final view VCondStat of CondStatImpl extends CondStatView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public CondStat() {
		super(new CondStatImpl());
	}

	public CondStat(int pos, ENode cond, ENode message) {
		this();
		this.pos = pos;
		this.cond = cond;
		this.message = message;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if( !(").append(cond)
			.append(") ) throw new kiev.stdlib.AssertionFailedException(")
			.append(message).append(");").newLine();
		return dmp;
	}
}

@nodeset
public class LabeledStat extends ENode implements Named {
	
	@dflow(out="stat") private static class DFI {
	@dflow(in="this:in")	Label			lbl;
	@dflow(in="lbl")		ENode			stat;
	}

	public static LabeledStat[]	emptyArray = new LabeledStat[0];

	@virtual typedef This  = LabeledStat;
	@virtual typedef NImpl = LabeledStatImpl;
	@virtual typedef VView = VLabeledStat;
	@virtual typedef JView = JLabeledStat;
	@virtual typedef RView = RLabeledStat;

	@nodeimpl
	public static class LabeledStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = LabeledStat;
		@att                 public NameRef		ident;
		@att(copyable=false) public Label			lbl;
		@att                 public ENode			stat;
	}
	@nodeview
	public static view LabeledStatView of LabeledStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			lbl;
		public ENode			stat;
	}
	@nodeview
	public static final view VLabeledStat of LabeledStatImpl extends LabeledStatView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public LabeledStat() {
		super(new LabeledStatImpl());
		this.lbl = new Label();
	}
	
	public NodeName getName() { return new NodeName(ident.name); }

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(ident).append(':').newLine(1).append(stat);
	}
}

@nodeset
public class BreakStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = BreakStat;
	@virtual typedef NImpl = BreakStatImpl;
	@virtual typedef VView = VBreakStat;
	@virtual typedef JView = JBreakStat;
	@virtual typedef RView = RBreakStat;

	@nodeimpl
	public static class BreakStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = BreakStat;
		@att public NameRef		ident;
		@ref public Label		dest;

		public void callbackRootChanged() {
			if (dest != null && dest.ctx_root != this._self.ctx_root) {
				dest.delLink(this._self);
				dest = null;
			}
			super.callbackRootChanged();
		}
	}
	@nodeview
	public static view BreakStatView of BreakStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}

	@nodeview
	public static final view VBreakStat of BreakStatImpl extends BreakStatView {
		public boolean mainResolveIn() {
			NodeView p;
			if (dest != null) {
				dest.delLink(this.getNode());
				dest = null;
			}
			if( ident == null ) {
				for(p=parent; !(p instanceof MethodView || p.isBreakTarget()); p = p.parent );
				if( p instanceof MethodView || p == null ) {
					Kiev.reportError(this,"Break not within loop/switch statement");
				} else {
					if (p instanceof LoopStatView) {
						Label l = p.lblbrk;
						if (l != null) {
							dest = l;
							l.addLink(this.getNode());
						}
					}
				}
			} else {
		label_found:
				for(p=parent; !(p instanceof MethodView) ; p=p.parent ) {
					if (p instanceof LabeledStatView && p.ident.name.equals(ident.name))
						throw new RuntimeException("Label "+ident+" does not refer to break target");
					if (!p.isBreakTarget()) continue;
					ASTNode.NodeView pp = p;
					for(p=p.parent; p instanceof LabeledStatView; p = p.parent) {
						if (p.ident.name.equals(ident.name)) {
							p = pp;
							break label_found;
						}
					}
					p = pp;
				}
				if( p instanceof MethodView || p == null) {
					Kiev.reportError(this,"Break not within loop/switch statement");
				} else {
					if (p instanceof LoopStatView) {
						Label l = p.lblbrk;
						if (l != null) {
							dest = l;
							l.addLink(this.getNode());
						}
					}
				}
			}
			return false; // don't pre-resolve
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public BreakStat() {
		super(new BreakStatImpl());
	}
	
	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("break");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@nodeset
public class ContinueStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = ContinueStat;
	@virtual typedef NImpl = ContinueStatImpl;
	@virtual typedef VView = VContinueStat;
	@virtual typedef JView = JContinueStat;
	@virtual typedef RView = RContinueStat;

	@nodeimpl
	public static class ContinueStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = ContinueStat;
		@att public NameRef		ident;
		@ref public Label		dest;
	
		public void callbackRootChanged() {
			if (dest != null && dest.ctx_root != this._self.ctx_root) {
				dest.delLink(this._self);
				dest = null;
			}
			super.callbackRootChanged();
		}
	}
	@nodeview
	public static view ContinueStatView of ContinueStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}

	@nodeview
	public static final view VContinueStat of ContinueStatImpl extends ContinueStatView {
		public boolean mainResolveIn() {
			NodeView p;
			if (dest != null) {
				dest.delLink(this.getNode());
				dest = null;
			}
			if( ident == null ) {
				for(p=parent; !(p instanceof LoopStatView || p instanceof MethodView); p = p.parent );
				if( p instanceof MethodView || p == null ) {
					Kiev.reportError(this,"Continue not within loop statement");
				} else {
					if (p instanceof LoopStatView) {
						Label l = p.lblcnt;
						if (l != null) {
							dest = l;
							l.addLink(this.getNode());
						}
					}
				}
			} else {
		label_found:
				for(p=parent; !(p instanceof MethodView) ; p=p.parent ) {
					if( p instanceof LabeledStatView && p.ident.name.equals(ident.name) )
						throw new RuntimeException("Label "+ident+" does not refer to continue target");
					if !(p instanceof LoopStatView) continue;
					NodeView pp = p;
					for(p=p.parent; p instanceof LabeledStatView; p = p.parent) {
						if( p.ident.name.equals(ident.name) ) {
							p = pp;
							break label_found;
						}
					}
					p = pp;
				}
				if( p instanceof MethodView || p == null) {
					Kiev.reportError(this,"Continue not within loop statement");
				} else {
					if (p instanceof LoopStatView) {
						Label l = p.lblcnt;
						if (l != null) {
							dest = l;
							l.addLink(this.getNode());
						}
					}
				}
			}
			return false; // don't pre-resolve
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public ContinueStat() {
		super(new ContinueStatImpl());
	}
	
	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("continue");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@nodeset
public class GotoStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@virtual typedef This  = GotoStat;
	@virtual typedef NImpl = GotoStatImpl;
	@virtual typedef VView = VGotoStat;
	@virtual typedef JView = JGotoStat;
	@virtual typedef RView = RGotoStat;

	@nodeimpl
	public static class GotoStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = GotoStat;
		@att public NameRef		ident;
		@ref public Label		dest;
	
		public void callbackRootChanged() {
			if (dest != null && dest.ctx_root != this._self.ctx_root) {
				dest.delLink(this._self);
				dest = null;
			}
			super.callbackRootChanged();
		}
	}
	@nodeview
	public static view GotoStatView of GotoStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}

	@nodeview
	public static final view VGotoStat of GotoStatImpl extends GotoStatView {
		public boolean mainResolveIn() {
			if (dest != null) {
				dest.delLink(this.getNode());
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
			dest.addLink(this.getNode());
			return false; // don't pre-resolve
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public GotoStat() {
		super(new GotoStatImpl());
	}
	
	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public static LabeledStat[] resolveStat(KString name, ASTNode st, LabeledStat[] stats) {
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
			if( lst.ident.name.equals(name) ) {
				stats = (LabeledStat[])Arrays.appendUniq(stats,lst);
			}
			stats = resolveStat(name,lst.stat,stats);
		}
			break;
		case LocalStructDecl:	break;
		case VarDecl:			break;
		case GotoStat:			break;
		case GotoCaseStat:		break;
		case ReturnStat:		break;
		case ThrowStat:			break;
		case ExprStat:			break;
		case BreakStat:			break;
		case ContinueStat:		break;
		default:
			Kiev.reportWarning(st,"Unknown statement in label lookup: "+st.getClass());
		}
		return stats;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("goto").space().append(ident).append(';').newLine();
	}
}

@nodeset
public class GotoCaseStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = GotoCaseStat;
	@virtual typedef NImpl = GotoCaseStatImpl;
	@virtual typedef VView = VGotoCaseStat;
	@virtual typedef JView = JGotoCaseStat;
	@virtual typedef RView = RGotoCaseStat;

	@nodeimpl
	public static class GotoCaseStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = GotoCaseStat;
		@att public ENode		expr;
		@ref public SwitchStat	sw;
	}
	@nodeview
	public static view GotoCaseStatView of GotoCaseStatImpl extends ENodeView {
		public ENode		expr;
		public SwitchStat	sw;
	}
	@nodeview
	public static final view VGotoCaseStat of GotoCaseStatImpl extends GotoCaseStatView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public GotoCaseStat() {
		super(new GotoCaseStatImpl());
	}
	
	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("goto");
		if( expr != null )
			dmp.append(" case ").append(expr);
		else
			dmp.space().append("default");
		return dmp.append(';').newLine();
	}
}

