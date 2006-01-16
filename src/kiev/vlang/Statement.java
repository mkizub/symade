package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.types.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JInlineMethodStatView;
import kiev.be.java.JBlockStatView;
import kiev.be.java.JEmptyStatView;
import kiev.be.java.JExprStatView;
import kiev.be.java.JReturnStatView;
import kiev.be.java.JThrowStatView;
import kiev.be.java.JIfElseStatView;
import kiev.be.java.JCondStatView;
import kiev.be.java.JLabeledStatView;
import kiev.be.java.JBreakStatView;
import kiev.be.java.JContinueStatView;
import kiev.be.java.JGotoStatView;
import kiev.be.java.JGotoCaseStatView;

import kiev.be.java.CodeLabel;

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

	@node
	public static final class InlineMethodStatImpl extends ENodeImpl {
		@att public Method			method;
		@ref public ParamRedir[]	params_redir;
		public InlineMethodStatImpl() {}
		public InlineMethodStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view InlineMethodStatView of InlineMethodStatImpl extends ENodeView {
		public Method			method;
		public ParamRedir[]		params_redir;
	}

	@att public abstract virtual Method			method;
	@ref public abstract virtual ParamRedir[]		params_redir;
	
	public NodeView					getNodeView()				{ return new InlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new InlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }
	public InlineMethodStatView		getInlineMethodStatView()	{ return new InlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JInlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JInlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }
	public JInlineMethodStatView	getJInlineMethodStatView()	{ return new JInlineMethodStatView((InlineMethodStatImpl)this.$v_impl); }

	@getter public Method			get$method()				{ return this.getInlineMethodStatView().method; }
	@getter public ParamRedir[]		get$params_redir()			{ return this.getInlineMethodStatView().params_redir; }
	@setter public void		set$method(Method val)				{ this.getInlineMethodStatView().method = val; }
	@setter public void		set$params_redir(ParamRedir[] val)	{ this.getInlineMethodStatView().params_redir = val; }
	
	public InlineMethodStat() {
		super(new InlineMethodStatImpl());
	}

	public InlineMethodStat(int pos, Method m, Method in) {
		super(new InlineMethodStatImpl(pos));
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
		Type[] types = new Type[params_redir.length];
		for (int i=0; i < params_redir.length; i++) {
			types[i] = params_redir[i].new_var.type;
			params_redir[i].new_var.vtype.lnk = method.params[i].type;
		}
		try {
			method.resolveDecl();
			if( method.body.isAbrupted() ) setAbrupted(true);
			if( method.body.isMethodAbrupted() ) setMethodAbrupted(true);
		} finally {
			for (int i=0; i < params_redir.length; i++)
				params_redir[i].new_var.vtype.lnk = types[i];
		}
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
public class BlockStat extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		stats;
	}

	@node
	public static class BlockStatImpl extends ENodeImpl {
		@att public NArr<ENode>		stats;
		@ref public CodeLabel		break_label;
		public BlockStatImpl() {}
		public BlockStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view BlockStatView of BlockStatImpl extends ENodeView {
		public access:ro	NArr<ENode>		stats;
	}
	
	@att public abstract virtual access:ro	NArr<ENode>			stats;
	
	@getter public NArr<ENode>		get$stats()				{ return this.getBlockStatView().stats; }

	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new BlockStatView((BlockStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new BlockStatView((BlockStatImpl)this.$v_impl); }
	public BlockStatView		getBlockStatView()		alias operator(210,fy,$cast) { return new BlockStatView((BlockStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JBlockStatView((BlockStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JBlockStatView((BlockStatImpl)this.$v_impl); }
	public JBlockStatView		getJBlockStatView()		alias operator(210,fy,$cast) { return new JBlockStatView((BlockStatImpl)this.$v_impl); }
	
	public BlockStat() {
		super(new BlockStatImpl());
	}

	public BlockStat(BlockStatImpl $view) {
		super($view);
	}

	public BlockStat(int pos) {
		super(new BlockStatImpl(pos));
	}

	public BlockStat(int pos, NArr<ENode> sts) {
		super(new BlockStatImpl(pos));
		foreach (ENode st; sts) {
			this.stats.append(st);
		}
	}

	public BlockStat(int pos, ENode[] sts) {
		super(new BlockStatImpl(pos));
		foreach (ENode st; sts) {
			this.stats.append(st);
		}
	}

	public ENode addStatement(ENode st) {
		stats.append(st);
		return st;
	}

	public void addSymbol(Named sym) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ENode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.append(decl);
	}

	public void insertSymbol(Named sym, int idx) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ASTNode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}
	
	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof VarDecl,
			((VarDecl)n).var.name.equals(name),
			node ?= ((VarDecl)n).var
		;	n instanceof LocalStructDecl,
			name.equals(((LocalStructDecl)n).clazz.name.short_name),
			node ?= ((LocalStructDecl)n).clazz
		;	n instanceof TypeDef,
			name.equals(((TypeDef)n).name),
			node ?= ((TypeDef)n)
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		info.space_prev != null && info.space_prev.pslot.name == "stats",
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward(),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		((VarDecl)n).var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		assert (!isResolved());
		setResolved(true);
		resolveBlockStats(this, stats);
	}

	static class BlockStatDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		BlockStatDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("stats"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			BlockStat node = (BlockStat)dfi.node_impl.getNode();
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; node.stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
			if (vars.length > 0)
				res = DFFunc.calc(f, dfi).cleanInfoForVars(vars.toArray());
			else
				res = DFFunc.calc(f, dfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new BlockStatDFFunc(dfi);
	}

	public static void resolveBlockStats(ENode self, NArr<ENode> stats) {
		for(int i=0; i < stats.length; i++) {
			try {
				if( (i == stats.length-1) && self.isAutoReturnable() )
					stats[i].setAutoReturnable(true);
				if( self.isAbrupted() && (stats[i] instanceof LabeledStat) ) {
					self.setAbrupted(false);
				}
				if( self.isAbrupted() ) {
					//Kiev.reportWarning(stats[i].pos,"Possible unreachable statement");
				}
				if( stats[i] instanceof ENode ) {
					ENode st = stats[i];
					st.resolve(Type.tpVoid);
					st = stats[i];
					if( st.isAbrupted() && !self.isBreaked() ) self.setAbrupted(true);
					if( st.isMethodAbrupted() && !self.isBreaked() ) self.setMethodAbrupted(true);
				}
				else {
					stats[i].resolve(Type.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

}

@node
public class EmptyStat extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@node
	public static class EmptyStatImpl extends ENodeImpl {
		public EmptyStatImpl() {}
		public EmptyStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view EmptyStatView of EmptyStatImpl extends ENodeView {
		public EmptyStatView(EmptyStatImpl $view) { super($view); }
	}
	
	public NodeView				getNodeView()			{ return new EmptyStatView((EmptyStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new EmptyStatView((EmptyStatImpl)this.$v_impl); }
	public EmptyStatView		getEmptyStatView()		{ return new EmptyStatView((EmptyStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JEmptyStatView((EmptyStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JEmptyStatView((EmptyStatImpl)this.$v_impl); }
	public JEmptyStatView		getJEmptyStatView()		{ return new JEmptyStatView((EmptyStatImpl)this.$v_impl); }
	
	public EmptyStat() {
		super(new EmptyStatImpl());
	}

	public EmptyStat(int pos) {
		super(new EmptyStatImpl(pos));
	}

	public void resolve(Type reqType) {
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(';').newLine();
	}
}

@node
public class ExprStat extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@node
	public static final class ExprStatImpl extends ENodeImpl {
		@att public ENode	expr;
		public ExprStatImpl() {}
		public ExprStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ExprStatView of ExprStatImpl extends ENodeView {
		public ENode		expr;
	}

	@att public abstract virtual ENode expr;
	
	public NodeView			getNodeView()		{ return new ExprStatView((ExprStatImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new ExprStatView((ExprStatImpl)this.$v_impl); }
	public ExprStatView		getExprStatView()	{ return new ExprStatView((ExprStatImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JExprStatView((ExprStatImpl)this.$v_impl); }
	public JENodeView		getJENodeView()		{ return new JExprStatView((ExprStatImpl)this.$v_impl); }
	public JExprStatView	getJExprStatView()	{ return new JExprStatView((ExprStatImpl)this.$v_impl); }

	@getter public ENode	get$expr()				{ return this.getExprStatView().expr; }
	@setter public void		set$expr(ENode val)		{ this.getExprStatView().expr = val; }
	
	public ExprStat() {
		super(new ExprStatImpl());
	}

	public ExprStat(ENode expr) {
		super(new ExprStatImpl());
		this.expr = expr;
	}

	public ExprStat(int pos, ENode expr) {
		super(new ExprStatImpl(pos));
		this.expr = expr;
	}

	public String toString() {
		return "stat "+expr;
	}

	public void resolve(Type reqType) {
		try {
			expr.resolve(Type.tpVoid);
			expr.setGenVoidExpr(true);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
	}

	public Dumper toJava(Dumper dmp) {
		if( isHidden() ) dmp.append("/* ");
		expr.toJava(dmp).append(';');
		if( isHidden() ) dmp.append(" */");
		return dmp.newLine();
	}
}

@node
public class ReturnStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@node
	public static final class ReturnStatImpl extends ENodeImpl {
		@att public ENode	expr;
		public ReturnStatImpl() {}
		public ReturnStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ReturnStatView of ReturnStatImpl extends ENodeView {
		public ENode		expr;
	}

	@att public abstract virtual ENode expr;
	
	public NodeView			getNodeView()			{ return new ReturnStatView((ReturnStatImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new ReturnStatView((ReturnStatImpl)this.$v_impl); }
	public ReturnStatView	getReturnStatView()		{ return new ReturnStatView((ReturnStatImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JReturnStatView((ReturnStatImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JReturnStatView((ReturnStatImpl)this.$v_impl); }
	public JReturnStatView	getJReturnStatView()	{ return new JReturnStatView((ReturnStatImpl)this.$v_impl); }

	@getter public ENode	get$expr()				{ return this.getReturnStatView().expr; }
	@setter public void		set$expr(ENode val)		{ this.getReturnStatView().expr = val; }
	
	public ReturnStat() {
		super(new ReturnStatImpl());
	}

	public ReturnStat(int pos, ENode expr) {
		super(new ReturnStatImpl(pos));
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		if( expr != null ) {
			try {
				expr.resolve(ctx_method.type.ret);
			} catch(Exception e ) {
				Kiev.reportError(expr,e);
			}
		}
		if( ctx_method.type.ret ≡ Type.tpVoid ) {
			if( expr != null ) Kiev.reportError(this,"Can't return value in void method");
			expr = null;
		} else {
			if( expr == null )
				Kiev.reportError(this,"Return must return a value in non-void method");
			else if (!expr.getType().isInstanceOf(ctx_method.etype.ret) && expr.getType() != Type.tpNull)
				Kiev.reportError(this,"Return expression is not of type "+ctx_method.type.ret);
		}
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

	@node
	public static final class ThrowStatImpl extends ENodeImpl {
		@att public ENode	expr;
		public ThrowStatImpl() {}
		public ThrowStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ThrowStatView of ThrowStatImpl extends ENodeView {
		public ENode		expr;
	}

	@att public abstract virtual ENode expr;
	
	public NodeView			getNodeView()			{ return new ThrowStatView((ThrowStatImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new ThrowStatView((ThrowStatImpl)this.$v_impl); }
	public ThrowStatView	getThrowStatView()		{ return new ThrowStatView((ThrowStatImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JThrowStatView((ThrowStatImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JThrowStatView((ThrowStatImpl)this.$v_impl); }
	public JThrowStatView	getJThrowStatView()		{ return new JThrowStatView((ThrowStatImpl)this.$v_impl); }

	@getter public ENode	get$expr()				{ return this.getThrowStatView().expr; }
	@setter public void		set$expr(ENode val)		{ this.getThrowStatView().expr = val; }
	
	public ThrowStat() {
		super(new ThrowStatImpl());
	}

	public ThrowStat(int pos, ENode expr) {
		super(new ThrowStatImpl(pos));
		this.expr = expr;
		setMethodAbrupted(true);
	}

	public void resolve(Type reqType) {
		setMethodAbrupted(true);
		try {
			expr.resolve(Type.tpThrowable);
		} catch(Exception e ) {
			Kiev.reportError(expr,e);
		}
		Type exc = expr.getType();
		if( !PassInfo.checkException(this,exc) )
			Kiev.reportWarning(this,"Exception "+exc+" must be caught or declared to be thrown");
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

	@node
	public static class IfElseStatImpl extends ENodeImpl {
		@att public ENode			cond;
		@att public ENode			thenSt;
		@att public ENode			elseSt;
		public IfElseStatImpl() {}
		public IfElseStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view IfElseStatView of IfElseStatImpl extends ENodeView {
		public ENode		cond;
		public ENode		thenSt;
		public ENode		elseSt;
	}
	
	@att public abstract virtual ENode			cond;
	@att public abstract virtual ENode			thenSt;
	@att public abstract virtual ENode			elseSt;
	
	@getter public ENode		get$cond()				{ return this.getIfElseStatView().cond; }
	@getter public ENode		get$thenSt()			{ return this.getIfElseStatView().thenSt; }
	@getter public ENode		get$elseSt()			{ return this.getIfElseStatView().elseSt; }
	@setter public void			set$cond(ENode val)		{ this.getIfElseStatView().cond = val; }
	@setter public void			set$thenSt(ENode val)	{ this.getIfElseStatView().thenSt = val; }
	@setter public void			set$elseSt(ENode val)	{ this.getIfElseStatView().elseSt = val; }

	public NodeView				getNodeView()			{ return new IfElseStatView((IfElseStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new IfElseStatView((IfElseStatImpl)this.$v_impl); }
	public IfElseStatView		getIfElseStatView()		{ return new IfElseStatView((IfElseStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JIfElseStatView((IfElseStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JIfElseStatView((IfElseStatImpl)this.$v_impl); }
	public JIfElseStatView		getJIfElseStatView()	{ return new JIfElseStatView((IfElseStatImpl)this.$v_impl); }
	
	public IfElseStat() {
		super(new IfElseStatImpl());
	}
	
	public IfElseStat(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		super(new IfElseStatImpl(pos));
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
	
		try {
			thenSt.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(thenSt,e);
		}
		if( elseSt != null ) {
			try {
				elseSt.resolve(Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(elseSt,e);
			}
		}

		if (!cond.isConstantExpr()) {
			if( thenSt.isAbrupted() && elseSt!=null && elseSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() && elseSt!=null && elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (cond.getConstValue() instanceof Boolean && ((Boolean)cond.getConstValue()).booleanValue()) {
			if( thenSt.isAbrupted() ) setAbrupted(true);
			if( thenSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
		else if (elseSt != null){
			if( elseSt.isAbrupted() ) setAbrupted(true);
			if( elseSt.isMethodAbrupted() ) setMethodAbrupted(true);
		}
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("if(").space().append(cond).space()
			.append(')');
		if( /*thenSt instanceof ExprStat ||*/ thenSt instanceof BlockStat || thenSt instanceof InlineMethodStat) dmp.forsed_space();
		else dmp.newLine(1);
		dmp.append(thenSt);
		if( /*thenSt instanceof ExprStat ||*/ thenSt instanceof BlockStat || thenSt instanceof InlineMethodStat) dmp.newLine();
		else dmp.newLine(-1);
		if( elseSt != null ) {
			dmp.append("else");
			if( elseSt instanceof IfElseStat || elseSt instanceof BlockStat || elseSt instanceof InlineMethodStat ) dmp.forsed_space();
			else dmp.newLine(1);
			dmp.append(elseSt).newLine();
			if( elseSt instanceof IfElseStat || elseSt instanceof BlockStat || elseSt instanceof InlineMethodStat ) dmp.newLine();
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

	@node
	public static class CondStatImpl extends ENodeImpl {
		@att public ENode			cond;
		@att public ENode			message;
		public CondStatImpl() {}
		public CondStatImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CondStatView of CondStatImpl extends ENodeView {
		public ENode		cond;
		public ENode		message;
	}
	
	@att public abstract virtual ENode			cond;
	@att public abstract virtual ENode			message;
	
	@getter public ENode		get$cond()				{ return this.getCondStatView().cond; }
	@getter public ENode		get$message()			{ return this.getCondStatView().message; }
	@setter public void			set$cond(ENode val)		{ this.getCondStatView().cond = val; }
	@setter public void			set$message(ENode val)	{ this.getCondStatView().message = val; }

	public NodeView				getNodeView()			{ return new CondStatView((CondStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new CondStatView((CondStatImpl)this.$v_impl); }
	public CondStatView			getCondStatView()		{ return new CondStatView((CondStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JCondStatView((CondStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JCondStatView((CondStatImpl)this.$v_impl); }
	public JCondStatView		getJCondStatView()		{ return new JCondStatView((CondStatImpl)this.$v_impl); }
	
	public CondStat() {
		super(new CondStatImpl());
	}

	public CondStat(int pos, ENode cond, ENode message) {
		super(new CondStatImpl(pos));
		this.cond = cond;
		this.message = message;
	}

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		try {
			message.resolve(Type.tpString);
		} catch(Exception e ) {
			Kiev.reportError(message,e);
		}
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

	@node
	public static class LabeledStatImpl extends ENodeImpl {
		@att                 public NameRef		ident;
		@att(copyable=false) public Label			lbl;
		@att                 public ENode			stat;
		public LabeledStatImpl() {}
	}
	@nodeview
	public static view LabeledStatView of LabeledStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			lbl;
		public ENode			stat;
	}
	
	@att                 public abstract virtual NameRef		ident;
	@att(copyable=false) public abstract virtual Label			lbl;
	@att                 public abstract virtual ENode			stat;
	
	@getter public NameRef		get$ident()				{ return this.getLabeledStatView().ident; }
	@getter public Label		get$lbl()				{ return this.getLabeledStatView().lbl; }
	@getter public ENode		get$stat()				{ return this.getLabeledStatView().stat; }
	@setter public void			set$ident(NameRef val)	{ this.getLabeledStatView().ident = val; }
	@setter public void			set$lbl(Label val)		{ this.getLabeledStatView().lbl = val; }
	@setter public void			set$stat(ENode val)		{ this.getLabeledStatView().stat = val; }

	public NodeView				getNodeView()			{ return new LabeledStatView((LabeledStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new LabeledStatView((LabeledStatImpl)this.$v_impl); }
	public LabeledStatView		getLabeledStatView()	{ return new LabeledStatView((LabeledStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JLabeledStatView((LabeledStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JLabeledStatView((LabeledStatImpl)this.$v_impl); }
	public JLabeledStatView		getJLabeledStatView()	{ return new JLabeledStatView((LabeledStatImpl)this.$v_impl); }
	
	public LabeledStat() {
		super(new LabeledStatImpl());
		this.lbl = new Label();
	}
	
	public NodeName getName() { return new NodeName(ident.name); }

	public void resolve(Type reqType) {
		try {
			stat.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(stat,e);
		}
		if( stat.isAbrupted() ) setAbrupted(true);
		if( stat.isMethodAbrupted() ) setMethodAbrupted(true);
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.newLine(-1).append(ident).append(':').newLine(1).append(stat);
	}
}

@node
public class BreakStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@node
	public static class BreakStatImpl extends ENodeImpl {
		@att public NameRef		ident;
		@ref public Label		dest;
		public BreakStatImpl() {}
	}
	@nodeview
	public static view BreakStatView of BreakStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}
	
	@att public abstract virtual NameRef		ident;
	@ref public abstract virtual Label			dest;
	
	@getter public NameRef		get$ident()				{ return this.getBreakStatView().ident; }
	@getter public Label		get$dest()				{ return this.getBreakStatView().dest; }
	@setter public void			set$ident(NameRef val)	{ this.getBreakStatView().ident = val; }
	@setter public void			set$dest(Label val)		{ this.getBreakStatView().dest = val; }

	public NodeView				getNodeView()			{ return new BreakStatView((BreakStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new BreakStatView((BreakStatImpl)this.$v_impl); }
	public BreakStatView		getBreakStatView()		{ return new BreakStatView((BreakStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JBreakStatView((BreakStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JBreakStatView((BreakStatImpl)this.$v_impl); }
	public JBreakStatView		getJBreakStatView()		{ return new JBreakStatView((BreakStatImpl)this.$v_impl); }
	
	public BreakStat() {
		super(new BreakStatImpl());
	}
	
	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && ((BlockStat)p).isBreakTarget())
			 				); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat &&
					((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if( !(p instanceof BreakTarget || p instanceof BlockStat ) ) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(
				p instanceof BreakTarget
			 || p instanceof Method
			 || (p instanceof BlockStat && ((BlockStat)p).isBreakTarget())
			 				); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Break not within loop/switch statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat &&
					((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to break target");
				if( !(p instanceof BreakTarget || p instanceof BlockStat ) ) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).lblbrk;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		if( p instanceof Method )
			Kiev.reportError(this,"Break not within loop/switch statement");
		((ENode)p).setBreaked(true);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("break");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class ContinueStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@node
	public static class ContinueStatImpl extends ENodeImpl {
		@att public NameRef		ident;
		@ref public Label		dest;
		public ContinueStatImpl() {}
	}
	@nodeview
	public static view ContinueStatView of ContinueStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}
	
	@att public abstract virtual NameRef		ident;
	@ref public abstract virtual Label			dest;
	
	@getter public NameRef		get$ident()				{ return this.getContinueStatView().ident; }
	@getter public Label		get$dest()				{ return this.getContinueStatView().dest; }
	@setter public void			set$ident(NameRef val)	{ this.getContinueStatView().ident = val; }
	@setter public void			set$dest(Label val)		{ this.getContinueStatView().dest = val; }

	public NodeView				getNodeView()			{ return new ContinueStatView((ContinueStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new ContinueStatView((ContinueStatImpl)this.$v_impl); }
	public ContinueStatView		getContinueStatView()	{ return new ContinueStatView((ContinueStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JContinueStatView((ContinueStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JContinueStatView((ContinueStatImpl)this.$v_impl); }
	public JContinueStatView	getJContinueStatView()	{ return new JContinueStatView((ContinueStatImpl)this.$v_impl); }
	
	public ContinueStat() {
		super(new ContinueStatImpl());
	}
	
	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		ASTNode p;
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		if( ident == null ) {
			for(p=parent; !(p instanceof LoopStat || p instanceof Method); p = p.parent );
			if( p instanceof Method || p == null ) {
				Kiev.reportError(this,"Continue not within loop statement");
			} else {
				if (p instanceof LoopStat) {
					Label l = ((LoopStat)p).lblcnt;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		} else {
	label_found:
			for(p=parent; !(p instanceof Method) ; p=p.parent ) {
				if( p instanceof LabeledStat && ((LabeledStat)p).getName().equals(ident.name) )
					throw new RuntimeException("Label "+ident+" does not refer to continue target");
				if !(p instanceof LoopStat) continue;
				ASTNode pp = p;
				for(p=p.parent; p instanceof LabeledStat; p = p.parent) {
					if( ((LabeledStat)p).getName().equals(ident.name) ) {
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
					Label l = ((LoopStat)p).lblcnt;
					if (l != null) {
						dest = l;
						l.addLink(this);
					}
				}
			}
		}
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		// TODO: check label or loop statement available
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("continue");
		if( ident != null && !ident.name.equals(KString.Empty) )
			dmp.space().append(ident);
		return dmp.append(';').newLine();
	}
}

@node
public class GotoStat extends ENode {
	
	@dflow(jmp="this:in") private static class DFI {}

	@node
	public static class GotoStatImpl extends ENodeImpl {
		@att public NameRef		ident;
		@ref public Label		dest;
		public GotoStatImpl() {}
	}
	@nodeview
	public static view GotoStatView of GotoStatImpl extends ENodeView {
		public NameRef			ident;
		public Label			dest;
	}
	
	@att public abstract virtual NameRef		ident;
	@ref public abstract virtual Label			dest;
	
	@getter public NameRef		get$ident()				{ return this.getGotoStatView().ident; }
	@getter public Label		get$dest()				{ return this.getGotoStatView().dest; }
	@setter public void			set$ident(NameRef val)	{ this.getGotoStatView().ident = val; }
	@setter public void			set$dest(Label val)		{ this.getGotoStatView().dest = val; }

	public NodeView				getNodeView()			{ return new GotoStatView((GotoStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new GotoStatView((GotoStatImpl)this.$v_impl); }
	public GotoStatView			getGotoStatView()		{ return new GotoStatView((GotoStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JGotoStatView((GotoStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JGotoStatView((GotoStatImpl)this.$v_impl); }
	public JGotoStatView		getJGotoStatView()		{ return new JGotoStatView((GotoStatImpl)this.$v_impl); }
	
	public GotoStat() {
		super(new GotoStatImpl());
	}
	
	public void callbackRootChanged() {
		if (dest != null && dest.ctx_root != this.ctx_root) {
			dest.delLink(this);
			dest = null;
		}
		super.callbackRootChanged();
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		if (dest != null) {
			dest.delLink(this);
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
		dest.addLink(this);
		return false; // don't pre-resolve
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		if (dest != null) {
			dest.delLink(this);
			dest = null;
		}
		LabeledStat[] stats = resolveStat(ident.name,ctx_method.body, LabeledStat.emptyArray);
		if( stats.length == 0 ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		if( stats.length > 1 ) {
			Kiev.reportError(this,"Umbigouse label "+ident+" in goto statement");
		}
		LabeledStat stat = stats[0];
		if( stat == null ) {
			Kiev.reportError(this,"Label "+ident+" unresolved");
			return;
		}
		dest = stat.lbl;
		dest.addLink(this);
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
		case BlockStat:
		{
			BlockStat bst = (BlockStat)st;
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
		case EmptyStat: 		break;
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

@node
public class GotoCaseStat extends ENode {
	
	@dflow(jmp="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}


	@node
	public static class GotoCaseStatImpl extends ENodeImpl {
		@att public ENode		expr;
		@ref public SwitchStat	sw;
		public GotoCaseStatImpl() {}
	}
	@nodeview
	public static view GotoCaseStatView of GotoCaseStatImpl extends ENodeView {
		public ENode		expr;
		public SwitchStat	sw;
	}
	
	@att public abstract virtual ENode			expr;
	@ref public abstract virtual SwitchStat	sw;
	
	@getter public ENode		get$expr()				{ return this.getGotoCaseStatView().expr; }
	@getter public SwitchStat	get$sw()				{ return this.getGotoCaseStatView().sw; }
	@setter public void			set$expr(ENode val)		{ this.getGotoCaseStatView().expr = val; }
	@setter public void			set$sw(SwitchStat val)	{ this.getGotoCaseStatView().sw = val; }

	public NodeView				getNodeView()			{ return new GotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new GotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	public GotoCaseStatView		getGotoCaseStatView()	{ return new GotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JGotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JGotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	public JGotoCaseStatView	getJGotoCaseStatView()	{ return new JGotoCaseStatView((GotoCaseStatImpl)this.$v_impl); }
	
	public GotoCaseStat() {
		super(new GotoCaseStatImpl());
	}
	
	public void resolve(Type reqType) {
		setAbrupted(true);
		for(ASTNode node = this.parent; node != null; node = node.parent) {
			if (node instanceof SwitchStat) {
				this.sw = (SwitchStat)node;
				break;
			}
			if (node instanceof Method)
				break;
		}
		if( this.sw == null )
			throw new CompilerException(this,"goto case statement not within a switch statement");
		if( expr != null ) {
			if( sw.mode == SwitchStat.TYPE_SWITCH ) {
				expr = new AssignExpr(pos,AssignOperator.Assign,
					new LVarExpr(pos,sw.tmpvar.getVar()),(ENode)~expr);
				expr.resolve(Type.tpVoid);
				expr.setGenVoidExpr(true);
			} else {
				expr.resolve(sw.sel.getType());
			}
		}
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

