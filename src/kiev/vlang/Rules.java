package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.Method.MethodImpl;
import kiev.vlang.Method.MethodView;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class RuleMethod extends Method {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in")	Var[]			localvars;
	@dflow(in="this:in")	BlockStat		body;
	@dflow(in="this:in")	WBCCondition[] 	conditions;
	}

	@node
	public static final class RuleMethodImpl extends MethodImpl {
		@att public NArr<Var>			localvars;
		@att public int					base = 1;
		@att public int					max_depth;
		@att public int					state_depth;
		@att public int					max_vars;
		@att public int					index;		// index counter for RuleNode.idx
		public RuleMethodImpl() {}
		public RuleMethodImpl(int pos, int flags) { super(pos, flags); }
	}
	@nodeview
	public static final view RuleMethodView of RuleMethodImpl extends MethodView {
		public access:ro	NArr<Var>			localvars;
		public				int					base;
		public				int					max_depth;
		public				int					state_depth;
		public				int					max_vars;
		public				int					index;		// index counter for RuleNode.idx
	}

	@att public abstract virtual access:ro NArr<Var>			localvars;
	@att public abstract virtual			int					base;
	@att public abstract virtual			int					max_depth;
	@att public abstract virtual			int					state_depth;
	@att public abstract virtual			int					max_vars;
	@att public abstract virtual			int					index;		// index counter for RuleNode.idx
	
	public NodeView				getNodeView()			{ return new RuleMethodView((RuleMethodImpl)this.$v_impl); }
	public DNodeView			getDNodeView()			{ return new RuleMethodView((RuleMethodImpl)this.$v_impl); }
	public MethodView			getMethodView()			{ return new RuleMethodView((RuleMethodImpl)this.$v_impl); }
	public RuleMethodView		getRuleMethodView()		{ return new RuleMethodView((RuleMethodImpl)this.$v_impl); }

	@getter public NArr<Var>		get$localvars()		{ return this.getRuleMethodView().localvars; }
	@getter public int				get$base()			{ return this.getRuleMethodView().base; }
	@getter public int				get$max_depth()		{ return this.getRuleMethodView().max_depth; }
	@getter public int				get$state_depth()	{ return this.getRuleMethodView().state_depth; }
	@getter public int				get$max_vars()		{ return this.getRuleMethodView().max_vars; }
	@getter public int				get$index()			{ return this.getRuleMethodView().index; }

	@setter public void		set$base(int val)				{ this.getRuleMethodView().base = val; }
	@setter public void		set$max_depth(int val)			{ this.getRuleMethodView().max_depth = val; }
	@setter public void		set$state_depth(int val)		{ this.getRuleMethodView().state_depth = val; }
	@setter public void		set$max_vars(int val)			{ this.getRuleMethodView().max_vars = val; }
	@setter public void		set$index(int val)				{ this.getRuleMethodView().index = val; }
	
	public RuleMethod() {
		super(new RuleMethodImpl());
	}

	public RuleMethod(NameRef id, int fl) {
		super(new RuleMethodImpl(id.pos, fl), id.name, new TypeRef(Type.tpRule));
	}
	public RuleMethod(KString name, int fl) {
		super(new RuleMethodImpl(0, fl), name, Type.tpRule);
	}

	public int allocNewBase(int n) {
		int b = base;
		base += n;
		return b;
	}

	public int push() {
		state_depth++;
		if( state_depth > max_depth )
			max_depth = state_depth;
		return state_depth-1;
	}

	public int set_depth(int i) {
		state_depth = i;
		if( state_depth > max_depth )
			max_depth = state_depth;
		return state_depth;
	}

	public int add_iterator_var() {
		return max_vars++;
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		Var@ var;
	{
		inlined_by_dispatcher || path.space_prev.pslot.name == "targs",$cut,false
	;
		path.space_prev.pslot.name == "params" ||
		path.space_prev.pslot.name == "type_ref" ||
		path.space_prev.pslot.name == "dtype_ref",$cut,
		node @= targs,
		((TypeDef)node).name.name == name
	;
		var @= localvars,
		var.name.equals(name),
		node ?= var
	;
		inlined_by_dispatcher,$cut,false
	;
		var @= params,
		var.name.equals(name),
		node ?= var
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_clazz.concr_type.resolveNameAccessR(node,path,name)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for fields
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic();
			if( pbody == null ) setAbstract(true);
		}
		params.insert(0, new FormPar(pos,namePEnv,Type.tpRule,FormPar.PARAM_RULE_ENV,ACC_FORWARD|ACC_FINAL));
		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype == null)
				fp.stype = new TypeRef(fp.vtype.pos,fp.vtype.getType());
			if (fp.meta != null)
				fp.meta.verify();
		}
//		if( isVarArgs() ) {
//			FormPar va = new FormPar(pos,nameVarArgs, new ArrayType(Type.tpObject),FormPar.PARAM_VARARGS,ACC_FINAL);
//			params.append(va);
//		}
		foreach (Var lv; localvars)
			lv.setLocalRuleVar(true);
		trace(Kiev.debugMultiMethod,"Rule "+this+" has erased type "+this.etype);
		foreach(ASTAlias al; aliases) al.attach(this);

		foreach(WBCCondition cond; conditions)
			cond.definer = this;

		return this;
    }

	static class RuleMethodDFFunc extends DFFunc {
		final int res_idx;
		RuleMethodDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			RuleMethod m = (RuleMethod)dfi.node_impl.getNode();
			DFState in = DFState.makeNewState();
			for(int i=0; i < m.params.length; i++) {
				Var p = m.params[i];
				in = in.declNode(p);
			}
			for(int i=0; i < m.localvars.length; i++) {
				in = in.declNode(m.localvars[i]);
			}
			res = in;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new RuleMethodDFFunc(dfi);
	}

	public boolean preGenerate() {
		Var penv = params[0];
		assert(penv.name.name == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
		if( body instanceof RuleBlock ) {
			body.preGenerate();
			Kiev.runProcessorsOn(body);
			body.cleanDFlow();
		}
		return true;
	}
	
	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving rule "+this);
		try {
			Var penv = params[0];
			assert(penv.name.name == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
			if( body != null ) {
				if( type.ret() ≡ Type.tpVoid ) body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret() ≡ Type.tpVoid ) {
					((BlockStat)body).stats.append(new ReturnStat(pos,null));
					body.setAbrupted(true);
				} else {
					Kiev.reportError(body,"Return requared");
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		this.cleanDFlow();
	}


//	public boolean compare(KString name, CallType mt, Type tp, ResInfo info, boolean exact) {
//		if( !this.name.equals(name) ) return false;
//		int type_len = this.type.args.length - 1;
//		int args_len = mt.args.length;
//		if( type_len != args_len ) {
//			if( !isVarArgs() ) {
//				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
//					+" differ in number of params: "+type_len+" != "+args_len);
//				return false;
//			} else if( type_len-1 > args_len ) {
//				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
//					+" not match in number of params: "+type_len+" != "+args_len);
//				return false;
//			}
//		}
//		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
//		CallType rt = (CallType)Type.getRealType(tp,this.type);
//		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
//			if( exact && !mt.args[i].equals(rt.args[i+1]) ) {
//				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
//					+" differ in param # "+i+": "+rt.args[i+1]+" != "+mt.args[i]);
//				return false;
//			}
//			else if( !exact && !mt.args[i].isAutoCastableTo(rt.args[i+1]) ) {
//				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
//					+" differ in param # "+i+": "+mt.args[i]+" not auto-castable to "+rt.args[i+1]);
//				return false;
//			}
//		}
//		boolean match = false;
//		if( mt.ret == Type.tpAny )
//			match = true;
//		else if( exact &&  rt.ret.equals(mt.ret) )
//			match = true;
//		else if( !exact && rt.ret.isAutoCastableTo(mt.ret) )
//			match = true;
//		else
//			match = false;
//		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+(match?" match":" do not match"));
//		if (info != null && match)
//			info.mt = rt;
//		return match;
//	}

}

/*
	if true:
		if exists next check node
			if no more variants ( isthe )
				set new state and go there
			if more variants possible ( isoneof or call )
				push new state and jump there
		if no next check node
			if no more variants ( isthe )
				set always fail state and return true
			if more variants possible ( isoneof or call )
				return true
	if false
		if exists next backtrack node
			if top of stack
				set new state and go there
			if not top
				pop state and go there
		if no next backtrack node
			pop state (pop out method's state) and return false
*/

public /*immutable*/ class JumpNodes implements Cloneable {
	public /*final*/ boolean		more_check;
	public /*final*/ ASTRuleNode	next_check;
	public /*final*/ boolean		more_back;
	public /*final*/ ASTRuleNode	next_back;
	public /*final*/ boolean		jump_to_back;

	public JumpNodes(boolean mu, ASTRuleNode nu, boolean mbt, ASTRuleNode nbt, boolean jtb) {
		more_check = mu;
		next_check = nu;
		more_back = mbt;
		next_back = nbt;
		jump_to_back = jtb;
	}

	public Object clone() {
		return super.clone();
	}
}

/*
New scheme for prolog engine:

a) each rule method is passed with first argument - method frame;
b) method frame class is created for each rule, and
holds rule arguments, rule local vars, rule state stack
and rule temporary vars (for example, to implement restore
values on backtracking of assign operation);
c) initially the method frame pointer is null. If it's
null - rule creates new instanse and fills it with
arguments and initial values
d) if rule is successive, it returns it's own frame
object, if fails - returns null.
*/

@node
public abstract class ASTRuleNode extends ENode {
	public static ASTRuleNode[]	emptyArray = new ASTRuleNode[0];

	@node
	public static abstract class ASTRuleNodeImpl extends ENodeImpl {
		@att public JumpNodes			jn;
		@att public int					base;
		@att public int					idx;
		@att public int					depth = -1;
		public ASTRuleNodeImpl() {}
		public ASTRuleNodeImpl(int pos) { super(pos); }
	}
	@nodeview
	public static abstract view ASTRuleNodeView of ASTRuleNodeImpl extends ENodeView {
		public JumpNodes	jn;
		public int			base;
		public int			idx;
		public int			depth;
	}

	@att public abstract virtual JumpNodes	jn;
	@att public abstract virtual int					base;
	@att public abstract virtual int					idx;
	@att public abstract virtual int					depth;
	
	public abstract ASTRuleNodeView		getASTRuleNodeView();

	@getter public JumpNodes		get$jn()			{ return this.getASTRuleNodeView().jn; }
	@getter public int				get$base()			{ return this.getASTRuleNodeView().base; }
	@getter public int				get$idx()			{ return this.getASTRuleNodeView().idx; }
	@getter public int				get$depth()			{ return this.getASTRuleNodeView().depth; }

	@setter public void		set$jn(JumpNodes val)		{ this.getASTRuleNodeView().jn = val; }
	@setter public void		set$base(int val)			{ this.getASTRuleNodeView().base = val; }
	@setter public void		set$idx(int val)			{ this.getASTRuleNodeView().idx = val; }
	@setter public void		set$depth(int val)			{ this.getASTRuleNodeView().depth = val; }
	
	public ASTRuleNode(ASTRuleNodeImpl $view) { super($view); }

	public abstract 		void	createText(StringBuffer sb);
	public abstract 		void	resolve1(JumpNodes jn);

	public boolean preGenerate() { Kiev.reportError(this,"preGenerate of ASTRuleNode"); return false; }
	
	public void resolve(Type tp) { Kiev.reportError(this,"Resolving of ASTRuleNode"); }

	public String createTextUnification(LVarExpr var) {
		return "if( "+createTextVarAccess(var)+".$is_bound ) goto bound$"+idx+";\n";
	}

	public String createTextBacktrack(boolean load) {
		if (!jn.more_back)
			return "return null;\n";	// return false - no more solutions
		assert( ((RuleMethod)ctx_method).base != 1 || load==false);
		if (jn.next_back!=null && jn.jump_to_back) {
			if (load) return "bt$ = $env.bt$"+depth+"; goto enter$"+jn.next_back.idx+";\n";
			return "goto enter$"+jn.next_back.idx+";\n";
		}
		if (load)
			return "bt$ = $env.bt$"+depth+"; goto case bt$;\n"; // backtrack to saved address
		if (((RuleMethod)ctx_method).base == 1)
			return "return null;\n";
		return "goto case bt$;\n"; // backtrack to saved address
	}


	public String createTextMoreCheck(boolean force_goto) {
		if (!jn.more_check)
			return "$env.bt$=bt$; return $env;\n";				// return true - we've found a solution
		if (force_goto || jn.next_check.idx != (idx+1))
			return	"goto enter$"+jn.next_check.idx+";\n";		// jump to new check
		return "";
	}

	public String createTextVarAccess(LVarExpr v) {
		if( !v.getVar().isLocalRuleVar() ) return v.ident.toString();
		return "$env."+v;
	}

}


@node
public final class RuleBlock extends BlockStat {
	
	@dflow(out="node") private static class DFI {
	@dflow(in="this:in")	ASTRuleNode		node;
	}

	@node
	public static final class RuleBlockImpl extends BlockStatImpl {
		@att public ASTRuleNode		node;
		@att public StringBuffer	fields_buf;
		public RuleBlockImpl() {}
		public RuleBlockImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleBlockView of RuleBlockImpl extends BlockStatView {
		public ASTRuleNode		node;
		public StringBuffer		fields_buf;
	}

	@att public abstract virtual ASTRuleNode	node;
	@att public abstract virtual StringBuffer	fields_buf;
	
	public NodeView			getNodeView()		{ return new RuleBlockView((RuleBlockImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new RuleBlockView((RuleBlockImpl)this.$v_impl); }
	public BlockStatView	getBlockStatView()	{ return new RuleBlockView((RuleBlockImpl)this.$v_impl); }
	public RuleBlockView	getRuleBlockView()	{ return new RuleBlockView((RuleBlockImpl)this.$v_impl); }

	@getter public ASTRuleNode		get$node()				{ return this.getRuleBlockView().node; }
	@getter public StringBuffer		get$fields_buf()		{ return this.getRuleBlockView().fields_buf; }
	@setter public void		set$node(ASTRuleNode val)			{ this.getRuleBlockView().node = val; }
	@setter public void		set$fields_buf(StringBuffer val)	{ this.getRuleBlockView().fields_buf = val; }
	
	public RuleBlock() {
		super(new RuleBlockImpl());
	}

	public RuleBlock(int pos, ASTRuleNode n) {
		super(new RuleBlockImpl(pos));
		node = n;
	}

	public void resolve(Type tp) {
		throw new CompilerException(this,"Resolving of RuleBlock");
	}
	public boolean preGenerate() {
		node.resolve(Type.tpVoid);
		fields_buf = new StringBuffer();
		node.resolve1(new JumpNodes(false,null,false,null,false));
		StringBuffer sb = new StringBuffer(256);
		sb.append("{ ");
		// Declare private method frame class
		String tmpClassName = "frame$$";
		sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
		sb.append("int bt$;\n");
		RuleMethod rule_method = (RuleMethod)ctx_method;
		// Backtrace holders
		for (int i=0; i < rule_method.max_depth; i++)
			sb.append("int bt$").append(i).append(";\n");
		// Local variables
		foreach(Var v; rule_method.localvars) {
			String tp = Kiev.reparseType(v.type);
			if( v.type.isWrapper() )
				sb.append(tp+' '+v.name.name+" := new "+tp+"();\n");
			else
				sb.append(tp+' '+v.name.name+";\n");
		}
		// tmp variables inserted here
		sb.append(fields_buf.toString());
		fields_buf = null;
		sb.append("}\n");
		// Create new method frame or hash values from
		// existing one
		sb.append(tmpClassName).append(" $env;\n");
		sb.append("int bt$;\n");
		sb.append("if("+namePEnv+"==null) {\n");
		sb.append(" $env=new ").append(tmpClassName).append("(); bt$=0;\n");
		sb.append(" goto enter$1;\n");
		sb.append("}\n");
		if (rule_method.base != 1) {
			sb.append("else{\n");
			sb.append(" $env=($cast ").append(tmpClassName).append(")"+namePEnv+";\n");
			sb.append(" bt$=$env.bt$;\n");
			sb.append("}\n");
			sb.append("switch(bt$) {\ncase 0:\n");
		} else {
			// BUG!!!
			sb.append("else{\n$env=($cast ").append(tmpClassName).append(")"+namePEnv+";}\n");
		}
		sb.append("return null;\n");
		node.createText(sb);
		// Close method
		if (rule_method.base != 1)
			sb.append("}\nreturn null;\n");
		sb.append("}\n");
		trace(Kiev.debugRules,"Rule text generated:\n"+sb);
		BlockStat mbody = Kiev.parseBlock(this,sb);
		ctx_method.body = mbody;
		mbody.stats.addAll(stats);
		return false;
	}

}


@node
public final class RuleOrExpr extends ASTRuleNode {
	
	@dflow(out="rules") private static class DFI {
	@dflow(in="this:in", seq="false")	ASTRuleNode[]	rules;
	}

	@node
	public static final class RuleOrExprImpl extends ASTRuleNodeImpl {
		@att public NArr<ASTRuleNode>			rules;
		public RuleOrExprImpl() {}
		public RuleOrExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleOrExprView of RuleOrExprImpl extends ASTRuleNodeView {
		public access:ro	NArr<ASTRuleNode>			rules;
	}

	@att public abstract virtual access:ro NArr<ASTRuleNode>			rules;
	
	public NodeView				getNodeView()			{ return new RuleOrExprView((RuleOrExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleOrExprView((RuleOrExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleOrExprView((RuleOrExprImpl)this.$v_impl); }
	public RuleOrExprView		getRuleOrExprView()		{ return new RuleOrExprView((RuleOrExprImpl)this.$v_impl); }

	@getter public NArr<ASTRuleNode>	get$rules()		{ return this.getRuleOrExprView().rules; }

	public int get$base() {	return rules.length == 0 ? 0 : rules[0].get$base(); }
	public void set$base(int b) {}

	public int get$idx() {	return rules.length == 0 ? 0 : rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleOrExpr() {
		super(new RuleOrExprImpl());
	}

	public RuleOrExpr(ASTRuleNode first) {
		super(new RuleOrExprImpl());
		this.rules.add(first);
	}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		super(new RuleOrExprImpl(pos));
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].resolve(reqType);
    	}
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		int depth = ((RuleMethod)ctx_method).state_depth;
		int max_depth = depth;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(jn.more_check, jn.next_check, true, rules[i+1], true);
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, jn.more_back, jn.next_back, jn.jump_to_back);
			}
			((RuleMethod)ctx_method).set_depth(depth);
			rules[i].resolve1(j);
			max_depth = Math.max(max_depth,((RuleMethod)ctx_method).state_depth);
		}
		((RuleMethod)ctx_method).set_depth(max_depth);
	}
}

@node
public final class RuleAndExpr extends ASTRuleNode {
	
	@dflow(out="rules") private static class DFI {
	@dflow(in="this:in", seq="true")	ASTRuleNode[]	rules;
	}

	@node
	public static final class RuleAndExprImpl extends ASTRuleNodeImpl {
		@att public NArr<ASTRuleNode>			rules;
		public RuleAndExprImpl() {}
		public RuleAndExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleAndExprView of RuleAndExprImpl extends ASTRuleNodeView {
		public access:ro	NArr<ASTRuleNode>			rules;
	}

	@att public abstract virtual access:ro NArr<ASTRuleNode>			rules;
	
	public NodeView				getNodeView()			{ return new RuleAndExprView((RuleAndExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleAndExprView((RuleAndExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleAndExprView((RuleAndExprImpl)this.$v_impl); }
	public RuleAndExprView		getRuleAndExprView()	{ return new RuleAndExprView((RuleAndExprImpl)this.$v_impl); }

	@getter public NArr<ASTRuleNode>	get$rules()		{ return this.getRuleAndExprView().rules; }

	public int get$base() {	return rules.length == 0 ? 0 : rules[0].get$base();	}
	public void set$base(int b) {}

	public int get$idx() {	return rules.length == 0 ? 0 : rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleAndExpr() {
		super(new RuleAndExprImpl());
	}

	public RuleAndExpr(ASTRuleNode first) {
		super(new RuleAndExprImpl());
		this.rules.add(first);
	}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		super(new RuleAndExprImpl(pos));
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void resolve(Type reqType) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].resolve(reqType);
    	}
    	// combine simple boolean expressions
    	for(int i=0; i < (rules.length-1); i++) {
    		ASTRuleNode r1 = rules[i];
    		ASTRuleNode r2 = rules[i+1];
    		if (!(r1 instanceof RuleExpr)) continue;
    		if (!(r2 instanceof RuleExpr)) continue;
    		RuleExpr e1 = (RuleExpr)r1;
    		RuleExpr e2 = (RuleExpr)r2;
    		if (!e1.expr.getType().equals(Type.tpBoolean)) continue;
    		if (!e2.expr.getType().equals(Type.tpBoolean)) continue;
    		if (e1.bt_expr != null) continue;
    		if (e2.bt_expr != null) continue;
    		RuleExpr e = new RuleExpr(new BinaryBooleanAndExpr(e1.pos,(ENode)~e1.expr,(ENode)~e2.expr));
    		rules[i] = e;
			rules.del(i+1);
    		i--;
    	}
    	if (rules.length == 1)
    		replaceWithNode((ENode)~rules[0]);
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		JumpNodes j;
		boolean more_back = jn.more_back;
		ASTRuleNode next_back = jn.next_back;
		boolean jump_to_back = jn.jump_to_back;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				j = new JumpNodes(true, rules[i+1], more_back, next_back, jump_to_back);
				if (rules[i] instanceof RuleExpr) {
					RuleExpr re = (RuleExpr)rules[i];
					if (re.bt_expr != null) {
						more_back = true;
						next_back = rules[i];
						jump_to_back = false;
					}
				}
				else if (rules[i] instanceof RuleCutExpr) {
					more_back = false;
					next_back = null;
					jump_to_back = false;
				}
				else {
					more_back = true;
					next_back = rules[i];
					jump_to_back = false;
				}
			} else {
				j = new JumpNodes(jn.more_check, jn.next_check, more_back, next_back, jump_to_back);
			}
			rules[i].resolve1(j);
		}
	}
}

@node
public final class RuleIstheExpr extends ASTRuleNode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	@node
	public static final class RuleIstheExprImpl extends ASTRuleNodeImpl {
		@att public LVarExpr	var;		// variable of type PVar<...>
		@att public ENode		expr;		// expression to check/unify
		public RuleIstheExprImpl() {}
		public RuleIstheExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleIstheExprView of RuleIstheExprImpl extends ASTRuleNodeView {
		public LVarExpr	var;		// variable of type PVar<...>
		public ENode		expr;		// expression to check/unify
	}

	@att public abstract virtual LVarExpr	var;		// variable of type PVar<...>
	@att public abstract virtual ENode		expr;		// expression to check/unify
	
	public NodeView				getNodeView()			{ return new RuleIstheExprView((RuleIstheExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleIstheExprView((RuleIstheExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleIstheExprView((RuleIstheExprImpl)this.$v_impl); }
	public RuleIstheExprView	getRuleIstheExprView()	{ return new RuleIstheExprView((RuleIstheExprImpl)this.$v_impl); }

	@getter public LVarExpr		get$var()			{ return this.getRuleIstheExprView().var; }
	@getter public ENode		get$expr()			{ return this.getRuleIstheExprView().expr; }
	@setter public void		set$var(LVarExpr val)	{ this.getRuleIstheExprView().var = val; }
	@setter public void		set$expr(ENode val)		{ this.getRuleIstheExprView().expr = val; }

	public RuleIstheExpr() {
		super(new RuleIstheExprImpl());
	}

	public RuleIstheExpr(int pos, LVarExpr var, ENode expr) {
		super(new RuleIstheExprImpl(pos));
		this.var = var;
		this.expr = expr;
	}

    public void resolve(Type reqType) {
		var.resolve(null);
		expr.resolve(((WrapperType)var.var.type).getWrappedType());
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				createTextUnification(var)+

			// Unbound
				createTextVarAccess(var)+".$bind("+Kiev.reparseExpr(expr,true)+");\n"+
				"if( !"+createTextVarAccess(var)+".$is_bound ) {\n"+
					createTextBacktrack(false)+					// backtrack, bt$ already loaded
				"}\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
				createTextMoreCheck(true)+						// check next
			"case "+base+":\n"+									// backtracking, always fail state
				createTextVarAccess(var)+".$unbind();\n"+		// was binded here, unbind
				createTextBacktrack(true)+						// backtrack, bt$ needs to be loaded

			// Already bound
			"bound$"+idx+":;\n"+
				"if( ! "+createTextVarAccess(var)+".equals("+Kiev.reparseExpr(expr,true)+") ) {\n"+	// check
					createTextBacktrack(false)+					// backtrack, bt$ already loaded
				"}\n"+
				createTextMoreCheck(false)							// check next
		);
	}
}

@node
public final class RuleIsoneofExpr extends ASTRuleNode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	public static final int	ARRAY = 0;
	public static final int	KENUM = 1;
	public static final int	JENUM = 2;
	public static final int	ELEMS = 3;

	@node
	public static final class RuleIsoneofExprImpl extends ASTRuleNodeImpl {
		@att public LVarExpr	var;		// variable of type PVar<...>
		@att public ENode		expr;		// expression to check/unify
		@att public int			iter_var;	// iterator var
		@att public Type		itype;
		@att public int			mode;
		public RuleIsoneofExprImpl() {}
		public RuleIsoneofExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleIsoneofExprView of RuleIsoneofExprImpl extends ASTRuleNodeView {
		public LVarExpr		var;		// variable of type PVar<...>
		public ENode		expr;		// expression to check/unify
		public int			iter_var;	// iterator var
		public Type			itype;
		public int			mode;
	}

	@att public abstract virtual LVarExpr	var;		// variable of type PVar<...>
	@att public abstract virtual ENode		expr;		// expression to check/unify
	@att public abstract virtual int		iter_var;	// iterator var
	@att public abstract virtual Type		itype;
	@att public abstract virtual int		mode;
	
	public NodeView				getNodeView()				{ return new RuleIsoneofExprView((RuleIsoneofExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()				{ return new RuleIsoneofExprView((RuleIsoneofExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()		{ return new RuleIsoneofExprView((RuleIsoneofExprImpl)this.$v_impl); }
	public RuleIsoneofExprView	getRuleIsoneofExprView()	{ return new RuleIsoneofExprView((RuleIsoneofExprImpl)this.$v_impl); }

	@getter public LVarExpr		get$var()				{ return this.getRuleIsoneofExprView().var; }
	@getter public ENode		get$expr()				{ return this.getRuleIsoneofExprView().expr; }
	@getter public int			get$iter_var()			{ return this.getRuleIsoneofExprView().iter_var; }
	@getter public Type			get$itype()				{ return this.getRuleIsoneofExprView().itype; }
	@getter public int			get$mode()				{ return this.getRuleIsoneofExprView().mode; }
	@setter public void			set$var(LVarExpr val)	{ this.getRuleIsoneofExprView().var = val; }
	@setter public void			set$expr(ENode val)		{ this.getRuleIsoneofExprView().expr = val; }
	@setter public void			set$iter_var(int val)	{ this.getRuleIsoneofExprView().iter_var = val; }
	@setter public void			set$itype(Type val)		{ this.getRuleIsoneofExprView().itype = val; }
	@setter public void			set$mode(int val)		{ this.getRuleIsoneofExprView().mode = val; }

	public RuleIsoneofExpr() {
		super(new RuleIsoneofExprImpl());
	}

	public RuleIsoneofExpr(int pos, LVarExpr var, ENode expr) {
		super(new RuleIsoneofExprImpl(pos));
		this.var = var;
		this.expr = expr;
	}

    public void resolve(Type reqType) {
		var.resolve(null);
		expr.resolve(null);
    }

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(2);
		depth = ((RuleMethod)ctx_method).push();
		expr.resolve(null);
		Type ctype = expr.getType();
		Method@ elems;
		if( ctype.isArray() ) {
			TVarSet set = new TVarSet();
			set.append(Type.tpArrayEnumerator.clazz.args[0].getAType(), ((ArrayType)ctype).arg);
			itype = ((CompaundTypeProvider)Type.tpArrayEnumerator.meta_type).templ_type.bind(set);
			mode = ARRAY;
		} else if( ctype.isInstanceOf( Type.tpKievEnumeration) ) {
			itype = ctype;
			mode = KENUM;
		} else if( ctype.isInstanceOf( Type.tpJavaEnumeration) ) {
			itype = ctype;
			mode = JENUM;
		} else if( PassInfo.resolveBestMethodR(ctype,elems,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
				nameElements,new CallType(Type.emptyArray,Type.tpAny))
		) {
			itype = Type.getRealType(ctype,elems.type.ret());
			mode = ELEMS;
		} else {
			throw new CompilerException(expr,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+ctype+" found");
		}
		iter_var = ((RuleMethod)ctx_method).add_iterator_var();
		ASTNode rb = this.parent;
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.parent != null, "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent;
		}
		Debug.assert(rb != null);
		Debug.assert(rb instanceof RuleBlock);
		((RuleBlock)rb).fields_buf.append(itype)
			.append(' ').append("$iter$").append(iter_var).append(";\n");
	}

	private String createTextCheckUnbinded() {
		return "("+createTextVarAccess(var)+".$is_bound)";
	}

	private String createTextUnification() {
		return createTextUnification(var);
	}

	private String createTextNewIterator() {
		switch( mode ) {
		case ARRAY:
			return "new "+itype+"("+Kiev.reparseExpr(expr,true)+")";
		case KENUM:
			return Kiev.reparseExpr(expr,true);
		case JENUM:
			return Kiev.reparseExpr(expr,true);
		case ELEMS:
			return "("+Kiev.reparseExpr(expr,true)+").elements()";
		default:
			throw new RuntimeException("Unknown mode of iterator "+mode);
		}
	}

	private String createTextNewIterators() {
		return "$env.$iter$"+iter_var+"="+createTextNewIterator()+";\n";
	}

	private String createTextUnbindVars() {
		return "$env.$iter$"+iter_var+"=null;\n"+
				createTextVarAccess(var)+".$unbind();\n";
	}

	private String createTextCheckNext() {
		return "($env.$iter$"+iter_var+".hasMoreElements()"+
				" && "+createTextVarAccess(var)+".$rebind_chk($env.$iter$"+iter_var+".nextElement()))";
	}

	private String createTextContaince() {
		switch( mode ) {
		case ARRAY:
			return "kiev.stdlib.ArrayEnumerator.contains("+Kiev.reparseExpr(expr,true)+","+var.ident+".$var)";
		case KENUM:
			return "kiev.stdlib.PEnv.contains("+Kiev.reparseExpr(expr,true)+","+var.ident+".$var)";
		case JENUM:
			return "kiev.stdlib.PEnv.jcontains("+Kiev.reparseExpr(expr,true)+","+var.ident+".$var)";
		case ELEMS:
			return Kiev.reparseExpr(expr,true)+".contains("+var.ident+".$var)";
		default:
			throw new RuntimeException("Unknown mode of iterator "+mode);
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				createTextUnification()+

			// Bind here
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
				createTextNewIterators()+						// create iterators
			"case "+(base+0)+":\n"+								// backtracking, check next element
				"if( "+createTextCheckNext()+" ) {\n"+
					createTextMoreCheck(true)+
				"} else {\n"+
					createTextUnbindVars()+						// binded here, unbind
					createTextBacktrack(true)+					// backtrack, bt$ may needs to be loaded
				"}\n"+

			// Already binded
			"bound$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+(base+1)+";\n"+						// set new backtrack state to point itself
				"if( "+createTextContaince()+" ) {\n"+			// check
					createTextMoreCheck(true)+
				"}\n"+
			"case "+(base+1)+":\n"+
				createTextBacktrack(true)						// backtrack, bt$ may needs to be loaded
		);
	}
}

@node
public final class RuleCutExpr extends ASTRuleNode {
	
	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class RuleCutExprImpl extends ASTRuleNodeImpl {
		public RuleCutExprImpl() {}
		public RuleCutExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleCutExprView of RuleCutExprImpl extends ASTRuleNodeView {
		public RuleCutExprView(RuleCutExprImpl $view) { super($view); }
	}
	
	public NodeView				getNodeView()			{ return new RuleCutExprView((RuleCutExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleCutExprView((RuleCutExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleCutExprView((RuleCutExprImpl)this.$v_impl); }
	public RuleCutExprView		getRuleCutExprView()	{ return new RuleCutExprView((RuleCutExprImpl)this.$v_impl); }
	
	public RuleCutExpr() {
		super(new RuleCutExprImpl());
	}

	public RuleCutExpr(int pos) {
		super(new RuleCutExprImpl(pos));
	}

	public void resolve(Type reqType) {
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				"bt$ = 0;\n"+								// backtracking, always fail state, state 0 is 'return null'
				createTextMoreCheck(false)
		);
	}
}

@node
public final class RuleCallExpr extends ASTRuleNode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@node
	public static final class RuleCallExprImpl extends ASTRuleNodeImpl {
		@att public ENode				obj;
		@ref public Named				func;
		@att public NArr<ENode>			args;
		@att public int					env_var;
		public RuleCallExprImpl() {}
		public RuleCallExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleCallExprView of RuleCallExprImpl extends ASTRuleNodeView {
		public				ENode			obj;
		public				Named			func;
		public access:ro	NArr<ENode>		args;
		public				int				env_var;
	}
	
	@att public abstract virtual			ENode				obj;
	@ref public abstract virtual			Named				func;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			int					env_var;
	
	@getter public ENode			get$obj()				{ return this.getRuleCallExprView().obj; }
	@getter public Named			get$func()				{ return this.getRuleCallExprView().func; }
	@getter public NArr<ENode>		get$args()				{ return this.getRuleCallExprView().args; }
	@getter public int				get$env_var()			{ return this.getRuleCallExprView().env_var; }
	
	@setter public void		set$obj(ENode val)				{ this.getRuleCallExprView().obj = val; }
	@setter public void		set$func(Named val)				{ this.getRuleCallExprView().func = val; }
	@setter public void		set$env_var(int val)			{ this.getRuleCallExprView().env_var = val; }

	public NodeView				getNodeView()				{ return new RuleCallExprView((RuleCallExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()				{ return new RuleCallExprView((RuleCallExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()		{ return new RuleCallExprView((RuleCallExprImpl)this.$v_impl); }
	public RuleCallExprView		getRuleCallExprView()		{ return new RuleCallExprView((RuleCallExprImpl)this.$v_impl); }
	

	public RuleCallExpr() {
		super(new RuleCallExprImpl());
	}

	public RuleCallExpr(CallExpr expr) {
		super(new RuleCallExprImpl(expr.pos));
		this.obj = (ENode)~expr.obj;
		this.func = expr.func;
		this.args.addAll(expr.args.delToArray());
		this.setSuperExpr(expr.isSuperExpr());
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		super(new RuleCallExprImpl(expr.pos));
		this.obj = (ENode)~expr.expr;
		if( expr.expr instanceof LVarExpr )
			this.func = ((LVarExpr)expr.expr).getVar();
		else if( expr.expr instanceof SFldExpr )
			this.func = ((SFldExpr)expr.expr).var;
		else if( expr.expr instanceof IFldExpr ) {
			this.func = ((IFldExpr)expr.expr).var;
			this.obj = (ENode)~((IFldExpr)expr.expr).obj;
		}
		this.args.addAll(expr.args.delToArray());
		this.args.insert(0,new ConstNullExpr()/*expr.env_access*/);
	}

	public void resolve(Type reqType) {
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
		env_var = ((RuleMethod)ctx_method).add_iterator_var();
		ASTNode rb = this.parent;
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.parent != null, "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent;
		}
		Debug.assert(rb != null);
		Debug.assert(rb instanceof RuleBlock);
		((RuleBlock)rb).fields_buf.append("rule $rc$frame$")
			.append(env_var).append(";\n");
	}

	private String createTextCall() {
		StringBuffer sb = new StringBuffer();
		sb.append("($env.$rc$frame$").append(env_var).append("=");
		if( obj != null ) {
			if (this.isSuperExpr()) {
				assert (obj instanceof ThisExpr);
				sb.append("super.");
			} else {
				sb.append(Kiev.reparseExpr(obj,true)).append('.');
			}
		}
		else if (this.isSuperExpr()) {
			sb.append("super.");
		}
		sb.append(func.getName()).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(Kiev.reparseExpr(args[i],true));
			if( i < args.length-1) sb.append(',');
		}
		sb.append("))");
		return sb.toString();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			"enter$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
			"case "+base+":\n"+
				"if( ! "+createTextCall()+" ) {\n"+
					createTextBacktrack(true)+					// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
public abstract class RuleExprBase extends ASTRuleNode {

	@node
	public static abstract class RuleExprBaseImpl extends ASTRuleNodeImpl {
		@att public ENode				expr;
		@att public ENode				bt_expr;
		public RuleExprBaseImpl() {}
		public RuleExprBaseImpl(int pos) { super(pos); }
	}
	@nodeview
	public static abstract view RuleExprBaseView of RuleExprBaseImpl extends ASTRuleNodeView {
		public				ENode			expr;
		public				ENode			bt_expr;
	}
	
	@att public abstract virtual			ENode				expr;
	@att public abstract virtual			ENode				bt_expr;
	
	@getter public ENode			get$expr()				{ return this.getRuleExprBaseView().expr; }
	@getter public ENode			get$bt_expr()			{ return this.getRuleExprBaseView().bt_expr; }
	
	@setter public void		set$expr(ENode val)				{ this.getRuleExprBaseView().expr = val; }
	@setter public void		set$bt_expr(ENode val)			{ this.getRuleExprBaseView().bt_expr = val; }

	public abstract RuleExprBaseView	getRuleExprBaseView();

	public RuleExprBase(RuleExprBaseImpl $view) {
		super($view);
	}
	public RuleExprBase(RuleExprBaseImpl $view, ENode expr, ENode bt_expr) {
		super($view);
		this.expr = expr;
		this.bt_expr = bt_expr;
	}

	public void resolve(Type reqType) {
		expr.resolve(null);

		if( expr instanceof CallExpr ) {
			CallExpr e = (CallExpr)expr;
			if( e.func.type.ret() ≡ Type.tpRule ) {
				replaceWithNodeResolve(reqType, new RuleCallExpr((CallExpr)~e));
				return;
			}
		}
		else if( expr instanceof ClosureCallExpr ) {
			ClosureCallExpr e = (ClosureCallExpr)expr;
			Type tp = e.getType();
			if( tp ≡ Type.tpRule || (tp instanceof CallType && ((CallType)tp).ret() ≡ Type.tpRule && tp.arity == 0) ) {
				replaceWithNodeResolve(reqType, new RuleCallExpr((ClosureCallExpr)~e));
				return;
			}
		}
	}
}

@node
public final class RuleWhileExpr extends RuleExprBase {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="this:in")	ENode		bt_expr;
	}
	
	@node
	public static final class RuleWhileExprImpl extends RuleExprBaseImpl {
		public RuleWhileExprImpl() {}
		public RuleWhileExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleWhileExprView of RuleWhileExprImpl extends RuleExprBaseView {
		public RuleWhileExprView(RuleWhileExprImpl $view) { super($view); }
	}
	
	public NodeView				getNodeView()			{ return new RuleWhileExprView((RuleWhileExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleWhileExprView((RuleWhileExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleWhileExprView((RuleWhileExprImpl)this.$v_impl); }
	public RuleExprBaseView		getRuleExprBaseView()	{ return new RuleWhileExprView((RuleWhileExprImpl)this.$v_impl); }
	public RuleWhileExprView	getRuleWhileExprView()	{ return new RuleWhileExprView((RuleWhileExprImpl)this.$v_impl); }
	
	public RuleWhileExpr() {
		super(new RuleWhileExprImpl());
	}

	public RuleWhileExpr(ENode expr) {
		super(new RuleWhileExprImpl(), expr, null);
	}

	public RuleWhileExpr(ENode expr, ENode bt_expr) {
		super(new RuleWhileExprImpl(), expr, bt_expr);
	}

	public void resolve(Type reqType) {
		super.resolve(reqType);
		if (pslot == null) return; // check we were replaced
		if (!expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(expr,"Boolean expression is requared");
		if (bt_expr != null)
			bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
				"bt$ = "+base+";\n"+							// set new backtrack state to point itself
			"case "+base+":\n"+
				(bt_expr == null ?
					""
				:	Kiev.reparseExpr(bt_expr,true)+";\n"
				)+
				"if ( ! "+Kiev.reparseExpr(expr,true)+" ) {\n"+
					createTextBacktrack(true)+						// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
public final class RuleExpr extends RuleExprBase {
	
	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	@dflow(in="this:in")	ENode		bt_expr;
	}

	@node
	public static final class RuleExprImpl extends RuleExprBaseImpl {
		public RuleExprImpl() {}
		public RuleExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view RuleExprView of RuleExprImpl extends RuleExprBaseView {
		public RuleExprView(RuleExprImpl $view) { super($view); }
	}
	
	public NodeView				getNodeView()			{ return new RuleExprView((RuleExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new RuleExprView((RuleExprImpl)this.$v_impl); }
	public ASTRuleNodeView		getASTRuleNodeView()	{ return new RuleExprView((RuleExprImpl)this.$v_impl); }
	public RuleExprBaseView		getRuleExprBaseView()	{ return new RuleExprView((RuleExprImpl)this.$v_impl); }
	public RuleExprView			getRuleExprView()		{ return new RuleExprView((RuleExprImpl)this.$v_impl); }
	
	public RuleExpr() {
		super(new RuleExprImpl());
	}

	public RuleExpr(ENode expr) {
		super(new RuleExprImpl(), expr, null);
	}

	public RuleExpr(ENode expr, ENode bt_expr) {
		super(new RuleExprImpl(), expr, bt_expr);
	}

	public void resolve(Type reqType) {
		super.resolve(reqType);
		if (pslot == null) {
			if (bt_expr != null)
				throw new CompilerException(bt_expr,"Backtrace expression ignored for rule-call");
			return;
		}
		if (bt_expr != null && expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(bt_expr,"Backtrace expression in boolean rule");
		if (bt_expr != null)
			bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		if (bt_expr != null) {
			base = ((RuleMethod)ctx_method).allocNewBase(1);
			depth = ((RuleMethod)ctx_method).push();
		}
	}

	public void createText(StringBuffer sb) {
		sb.append(
			// No unification need
			"enter$"+idx+":;\n"+
				( expr.getType().equals(Type.tpBoolean) ?
					"if ( ! "+Kiev.reparseExpr(expr,true)+" ) {\n"+
						createTextBacktrack(false)+					// backtrack, bt$ already loaded
					"}\n"+
					createTextMoreCheck(false)
				: bt_expr == null ?
					Kiev.reparseExpr(expr,true)+";\n"+
					createTextMoreCheck(false)
				:
					"$env.bt$"+depth+" = bt$;\n"+					// store a state to backtrack
					"bt$ = "+base+";\n"+							// set new backtrack state to point itself
					Kiev.reparseExpr(expr,true)+";\n"+
					createTextMoreCheck(true)+
			"case "+base+":\n"+
					Kiev.reparseExpr(bt_expr,true)+";\n"+
					createTextBacktrack(true)						// backtrack, bt$ needs to be loaded
				)
		);
	}
}

