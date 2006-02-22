package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java.JNode;
import kiev.be.java.JDNode;
import kiev.be.java.JENode;
import kiev.be.java.JLoopStat;
import kiev.be.java.JLabel;
import kiev.ir.java.RWhileStat;
import kiev.be.java.JWhileStat;
import kiev.ir.java.RDoWhileStat;
import kiev.be.java.JDoWhileStat;
import kiev.ir.java.RForInit;
import kiev.be.java.JForInit;
import kiev.ir.java.RForStat;
import kiev.be.java.JForStat;
import kiev.ir.java.RForEachStat;
import kiev.be.java.JForEachStat;

import kiev.be.java.CodeLabel;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public abstract class LoopStat extends ENode implements ContinueTarget {
	@virtual typedef This  = LoopStat;
	@virtual typedef NImpl = LoopStatImpl;
	@virtual typedef VView = LoopStatView;
	@virtual typedef JView = JLoopStat;

	@nodeimpl
	public static abstract class LoopStatImpl extends ENodeImpl {
		@virtual typedef ImplOf = LoopStat;
		@att(copyable=false)	public Label		lblcnt;
		@att(copyable=false)	public Label		lblbrk;
	}
	@nodeview
	public static abstract view LoopStatView of LoopStatImpl extends ENodeView {
		public:ro	Label					lblcnt;
		public:ro	Label					lblbrk;
	}

	protected LoopStat(LoopStatImpl impl) {
		super(impl);
		impl.lblcnt = new Label();
		impl.lblbrk = new Label();
		setBreakTarget(true);
	}
}


@nodeset
public final class Label extends DNode {
	
	@dflow(out="this:out()") private static class DFI {}

	@virtual typedef This  = Label;
	@virtual typedef NImpl = LabelImpl;
	@virtual typedef VView = LabelView;
	@virtual typedef JView = JLabel;

	@nodeimpl
	public final static class LabelImpl extends DNodeImpl {
		@virtual typedef ImplOf = Label;
		@ref(copyable=false)	public List<ASTNode>	links = List.Nil;
								public CodeLabel		label;

		public final void callbackRootChanged() {
			ASTNode root = this._self.ctx_root;
			links = links.filter(fun (ASTNode n)->boolean { return n.ctx_root == root; });
			super.callbackRootChanged();
		}	
	}
	@nodeview
	public final static view LabelView of LabelImpl extends DNodeView {
		public List<ASTNode>		links;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public Label() {
		super(new LabelImpl());
	}
	
	public void addLink(ASTNode lnk) {
		if (links.contains(lnk))
			return;
		links = new List.Cons<ASTNode>(lnk, links);
	}

	public void delLink(ASTNode lnk) {
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
			Label node = (Label)dfi.node_impl.getNode();
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

@nodeset
public class WhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="body")		Label		lblcnt;
	@dflow(in="lblcnt")						ENode		cond;
	@dflow(in="cond:true")					ENode		body;
	@dflow(in="cond:false")					Label		lblbrk;
	}

	@virtual typedef This  = WhileStat;
	@virtual typedef NImpl = WhileStatImpl;
	@virtual typedef VView = VWhileStat;
	@virtual typedef JView = JWhileStat;
	@virtual typedef RView = RWhileStat;

	@nodeimpl
	public static final class WhileStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = WhileStat;
		@att public ENode		cond;
		@att public ENode		body;
	}
	@nodeview
	public static view WhileStatView of WhileStatImpl extends LoopStatView {
		public ENode		cond;
		public ENode		body;
	}
	@nodeview
	public static final view VWhileStat of WhileStatImpl extends WhileStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public WhileStat() {
		super(new WhileStatImpl());
	}

	public WhileStat(int pos, ENode cond, ENode body) {
		this();
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("while").space().append('(').space().append(cond)
			.space().append(')');
		if( body instanceof ExprStat || body instanceof Block ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof Block ) dmp.newLine();
		else dmp.newLine(-1);
		return dmp;
	}
}

@nodeset
public class DoWhileStat extends LoopStat {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in", links="cond:true")	ENode		body;
	@dflow(in="body")							Label		lblcnt;
	@dflow(in="lblcnt")							ENode		cond;
	@dflow(in="cond:false")						Label		lblbrk;
	}

	@virtual typedef This  = DoWhileStat;
	@virtual typedef NImpl = DoWhileStatImpl;
	@virtual typedef VView = VDoWhileStat;
	@virtual typedef JView = JDoWhileStat;
	@virtual typedef RView = RDoWhileStat;

	@nodeimpl
	public static final class DoWhileStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = DoWhileStat;
		@att public ENode		cond;
		@att public ENode		body;
	}
	@nodeview
	public static view DoWhileStatView of DoWhileStatImpl extends LoopStatView {
		public ENode		cond;
		public ENode		body;
	}
	@nodeview
	public static view VDoWhileStat of DoWhileStatImpl extends DoWhileStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public DoWhileStat() {
		super(new DoWhileStatImpl());
	}

	public DoWhileStat(int pos, ENode cond, ENode body) {
		this();
		this.pos = pos;
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("do");

		if( body instanceof ExprStat || body instanceof Block ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof Block ) dmp.newLine();
		else dmp.newLine(-1);

		dmp.append("while").space().append('(').space().append(cond).space().append(");").newLine();
		return dmp;
	}
}

@nodeset
public class ForInit extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="decls") private static class DFI {
	@dflow(in="", seq="true")	Var[]		decls;
	}

	@virtual typedef This  = ForInit;
	@virtual typedef NImpl = ForInitImpl;
	@virtual typedef VView = VForInit;
	@virtual typedef JView = JForInit;
	@virtual typedef RView = RForInit;

	@nodeimpl
	public static final class ForInitImpl extends ENodeImpl {
		@virtual typedef ImplOf = ForInit;
		@att public final NArr<Var>		decls;
	}
	@nodeview
	public static view ForInitView of ForInitImpl extends ENodeView {
		public:ro	NArr<Var>		decls;
	}
	@nodeview
	public static final view VForInit of ForInitImpl extends ForInitView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }


	public ForInit() {
		super(new ForInitImpl());
	}

	public ForInit(int pos) {
		this();
		this.pos = pos;
	}

	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
		Var@ var;
	{
		var @= decls,
		var.name.equals(name),
		node ?= var
	;	var @= decls,
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
		Var@ var;
	{
		var @= decls,
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}
	
	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < decls.length; i++) {
			decls[i].toJava(dmp);
			if( i < decls.length-1 ) dmp.append(',').space();
		}
		return dmp;
	}
}

@nodeset
public class ForStat extends LoopStat implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="lblbrk") private static class DFI {
	@dflow(in="this:in")				ENode		init;
	@dflow(in="init", links="iter")		ENode		cond;
	@dflow(in="cond:true")				ENode		body;
	@dflow(in="body")					Label		lblcnt;
	@dflow(in="lblcnt")					ENode		iter;
	@dflow(in="cond:false")				Label		lblbrk;
	}
	
	@virtual typedef This  = ForStat;
	@virtual typedef NImpl = ForStatImpl;
	@virtual typedef VView = VForStat;
	@virtual typedef JView = JForStat;
	@virtual typedef RView = RForStat;

	@nodeimpl
	public static final class ForStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = ForStat;
		@att public ENode		init;
		@att public ENode		cond;
		@att public ENode		body;
		@att public ENode		iter;
	}
	@nodeview
	public static view ForStatView of ForStatImpl extends LoopStatView {
		public ENode		init;
		public ENode		cond;
		public ENode		body;
		public ENode		iter;
	}
	@nodeview
	public static final view VForStat of ForStatImpl extends ForStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public ForStat() {
		super(new ForStatImpl());
	}
	
	public ForStat(int pos, ENode init, ENode cond, ENode iter, ENode body) {
		this();
		this.pos = pos;
		this.init = init;
		this.cond = cond;
		this.iter = iter;
		this.body = body;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
	{
		init instanceof ForInit,
		((ForInit)init).resolveNameR(node,path,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
		ASTNode@ n;
	{
		init instanceof ForInit,
		((ForInit)init).resolveMethodR(node,info,name,mt)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( init != null && init instanceof ENode ) dmp.append(init);
		else if( init != null ) {
			dmp.append(init).append(';');
		} else {
			dmp.append(';');
		}

		if( cond != null )
			dmp.append(cond);
		dmp.append(';');

		if( iter != null )
			dmp.append(iter);
		dmp.space().append(')').space();

		if( body instanceof ExprStat || body instanceof Block ) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(body);
		if( body instanceof ExprStat || body instanceof Block ) dmp.newLine();
		else dmp.newLine(-1);

		return dmp;
	}
}

@nodeset
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
	@virtual typedef NImpl = ForEachStatImpl;
	@virtual typedef VView = VForEachStat;
	@virtual typedef JView = JForEachStat;
	@virtual typedef RView = RForEachStat;

	@nodeimpl
	public static final class ForEachStatImpl extends LoopStatImpl {
		@virtual typedef ImplOf = ForEachStat;
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
	}
	@nodeview
	public static view ForEachStatView of ForEachStatImpl extends LoopStatView {
		public int			mode;
		public ENode		container;
		public Var			var;
		public Var			iter;
		public Var			iter_array;
		public ENode		iter_init;
		public ENode		iter_cond;
		public ENode		var_init;
		public ENode		cond;
		public ENode		body;
		public ENode		iter_incr;
	}
	@nodeview
	public static final view VForEachStat of ForEachStatImpl extends ForEachStatView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public ForEachStat() {
		super(new ForEachStatImpl());
	}
	
	public ForEachStat(int pos, Var var, ENode container, ENode cond, ENode body) {
		this();
		this.pos = pos;
		this.var = var;
		this.container = container;
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
	{
		{	node ?= var
		;	node ?= iter
		}, ((Var)node).name.equals(name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
		Var@ n;
	{
		{	n ?= var
		;	n ?= iter
		},
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("for").space().append('(');
		if( iter_init != null )
			dmp.append(iter_init).append(';');
		if( iter_cond != null )
			dmp.append(iter_cond).append(';');
		if( iter_incr != null )
			dmp.append(iter_incr);
		dmp.append(')').space();

		if( body instanceof ExprStat || body instanceof Block ) dmp.forsed_space();
		else dmp.newLine(1);
		if( var_init != null )
			dmp.append(var_init).newLine();
		if( cond != null )
			dmp.append("if !(").append(cond).append(") continue;").newLine();

		dmp.append(body);
		if( body instanceof ExprStat || body instanceof Block ) dmp.newLine();
		else dmp.newLine(-1);

		return dmp;
	}
}

