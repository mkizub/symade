package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import kiev.ir.java15.RRuleMethod;
import kiev.ir.java15.RRuleBlock;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class RuleMethod extends Method {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in")	Var[]			localvars;
	@dflow(in="this:in")	Block			body;
	@dflow(in="this:in")	WBCCondition[] 	conditions;
	}

	@virtual typedef This  = RuleMethod;
	@virtual typedef RView = RRuleMethod;

	@att public LocalRuleVar[]		localvars;
	     public int					base = 1;
	     public int					max_depth;
	     public int					state_depth;
	     public int					max_vars;
	     public int					index;		// index counter for RuleNode.idx

	public RuleMethod() {}

	public RuleMethod(Symbol id, int fl) {
		super(id, new TypeRef(Type.tpRule), fl);
		this.pos = id.pos;
	}
	public RuleMethod(String name, int fl) {
		super(new Symbol(name), new TypeRef(Type.tpRule), fl);
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

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		Var@ var;
	{
		isInlinedByDispatcherMethod() || path.space_prev.pslot().name == "targs",$cut,false
	;
		path.space_prev.pslot().name == "params" ||
		path.space_prev.pslot().name == "type_ref" ||
		path.space_prev.pslot().name == "dtype_ref",$cut,
		node @= targs,
		path.checkNodeName(node)
	;
		var @= localvars,
		path.checkNodeName(var),
		node ?= var
	;
		isInlinedByDispatcherMethod(),$cut,false
	;
		var @= params,
		path.checkNodeName(var),
		node ?= var
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_tdecl.xtype.resolveNameAccessR(node,path)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path)
	}

    public void pass3() {
		if !( parent() instanceof Struct )
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = this.ctx_tdecl;
		// TODO: check flags for fields
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic();
			if( body == null ) setAbstract(true);
		}
		if (params.length == 0 || params[0].kind != FormPar.PARAM_RULE_ENV)
			params.insert(0, new FormPar(pos,namePEnv,Type.tpRule,FormPar.PARAM_RULE_ENV,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype != null)
				fp.stype.getType(); // resolve
			if (fp.meta != null)
				fp.meta.verify();
		}
		trace(Kiev.debugMultiMethod,"Rule "+this+" has erased type "+this.etype);
		foreach(ASTOperatorAlias al; aliases) al.pass3();

		foreach(WBCCondition cond; conditions)
			cond.definer = this;
    }

	static class RuleMethodDFFunc extends DFFunc {
		final int res_idx;
		RuleMethodDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			RuleMethod m = (RuleMethod)dfi.node_impl;
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

	@virtual typedef This  ≤ ASTRuleNode;

	public JumpNodes			jn;
	public int					base;
	public int					idx;
	public int					depth = -1;

	@getter public int get$base() {	return ((ASTRuleNode)this).base; }
	@setter public void set$base(int b) { ((ASTRuleNode)this).base = b; }

	@getter public int get$idx() {	return ((ASTRuleNode)this).idx; }
	@setter public void set$idx(int i) { ((ASTRuleNode)this).idx = i; }

	public ASTRuleNode() {}

	public abstract 		void	createText(StringBuffer sb);
	public abstract 		void	resolve1(JumpNodes jn);
	public abstract void rnResolve();

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
		if !(v.getVar() instanceof LocalRuleVar) return v.ident.toString();
		return "$env."+v;
	}
}


@node
public final class RuleBlock extends ENode {
	
	@dflow(out="node") private static class DFI {
	@dflow(in="this:in")	ASTRuleNode		node;
	}

	@virtual typedef This  = RuleBlock;
	@virtual typedef RView = RRuleBlock;

	@att public ASTRuleNode		node;
	     public StringBuffer	fields_buf;

	public RuleBlock() {}

	public RuleBlock(int pos, ASTRuleNode n) {
		this.pos = pos;
		node = n;
	}

    public void rnResolve() {
		if (node != null)
			node.rnResolve();
    }

}


@node
public final class RuleOrExpr extends ASTRuleNode {
	
	@dflow(out="rules") private static class DFI {
	@dflow(in="this:in", seq="false")	ASTRuleNode[]	rules;
	}

	@virtual typedef This  = RuleOrExpr;

	@att public ASTRuleNode[]			rules;

	public int get$base() {	return rules.length == 0 ? 0 : rules[0].get$base(); }
	public void set$base(int b) {}

	public int get$idx() {	return rules.length == 0 ? 0 : rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleOrExpr() {}

	public RuleOrExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		this.pos = pos;
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void rnResolve() {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].rnResolve();
    	}
    }

	public void resolve1(JumpNodes jn) {
		this.open();
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

	@virtual typedef This  = RuleAndExpr;

	@att public ASTRuleNode[]			rules;

	public int get$base() {	return rules.length == 0 ? 0 : rules[0].get$base();	}
	public void set$base(int b) {}

	public int get$idx() {	return rules.length == 0 ? 0 : rules[0].get$idx(); }
	public void set$idx(int i) {}

	public RuleAndExpr() {}

	public RuleAndExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		this.pos = pos;
		this.rules.addAll(rules);
	}

	public void createText(StringBuffer sb) {
		foreach( ASTRuleNode n; rules )
			n.createText(sb);
	}

    public void rnResolve() {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].rnResolve();
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
    		RuleExpr e = new RuleExpr(new BinaryBooleanAndExpr(e1.pos,~e1.expr,~e2.expr));
    		rules[i] = e;
			rules.del(i+1);
    		i--;
    	}
    	if (rules.length == 1)
    		replaceWithNode(~rules[0]);
    }

	public void resolve1(JumpNodes jn) {
		this.open();
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

	@virtual typedef This  = RuleIstheExpr;

	@att public LVarExpr	var;		// variable of type PVar<...>
	@att public ENode		expr;		// expression to check/unify

	public RuleIstheExpr() {}

	public RuleIstheExpr(int pos, LVarExpr var, ENode expr) {
		this.pos = pos;
		this.var = var;
		this.expr = expr;
	}
	
	public Operator getOp() { return Operator.RuleIsThe; }

	public ENode[] getArgs() { return new ENode[]{var,expr}; }

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.RuleIsThe);
		this.ident = new SymbolRef<DNode>(op.name, cm);
		this.var = (LVarExpr)args[0];
		this.expr = args[1];
	}
	
    public void rnResolve() {
		//var.resolve(null);
		//expr.resolve(((CTimeType)var.var.type).getUnboxedType());
    }

	public void resolve1(JumpNodes jn) {
		this.open();
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
				"if !( "+createTextVarAccess(var)+".$is_bound ) {\n"+
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
				"if !( "+createTextVarAccess(var)+".equals("+Kiev.reparseExpr(expr,true)+") ) {\n"+	// check
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

	@virtual typedef This  = RuleIsoneofExpr;

	@att public LVarExpr	var;		// variable of type PVar<...>
	@att public ENode		expr;		// expression to check/unify
	@att public int			iter_var;	// iterator var
	     public Type		itype;
	     public int			mode;

	public RuleIsoneofExpr() {}

	public RuleIsoneofExpr(int pos, LVarExpr var, ENode expr) {
		this.pos = pos;
		this.var = var;
		this.expr = expr;
	}

	public Operator getOp() { return Operator.RuleIsOneOf; }

	public ENode[] getArgs() { return new ENode[]{var,expr}; }

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.RuleIsOneOf);
		this.ident = new SymbolRef<DNode>(op.name, cm);
		this.var = (LVarExpr)args[0];
		this.expr = args[1];
	}
	
    public void rnResolve() {
		//var.resolve(null);
		//expr.resolve(null);
    }

	public void resolve1(JumpNodes jn) {
		this.open();
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(2);
		depth = ((RuleMethod)ctx_method).push();
		expr.resolve(null);
		Type xtype = expr.getType();
		Method@ elems;
		if( xtype.isInstanceOf(Type.tpArray) ) {
			TVarBld set = new TVarBld();
			set.append(Type.tpArrayEnumerator.clazz.args[0].getAType(), xtype.resolve(Type.tpArray.meta_type.tdecl.args[0].getAType()));
			itype = Type.tpArrayEnumerator.meta_type.make(set);
			mode = ARRAY;
		} else if( xtype.isInstanceOf( Type.tpKievEnumeration) ) {
			itype = xtype;
			mode = KENUM;
		} else if( xtype.isInstanceOf( Type.tpJavaEnumeration) ) {
			itype = xtype;
			mode = JENUM;
		} else if( PassInfo.resolveBestMethodR(xtype,elems,
				new ResInfo(this,nameElements,ResInfo.noStatic|ResInfo.noImports),
				new CallType(xtype,null,null,Type.tpAny,false))
		) {
			itype = Type.getRealType(xtype,elems.type.ret());
			mode = ELEMS;
		} else {
			throw new CompilerException(expr,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+xtype+" found");
		}
		this.open();
		iter_var = ((RuleMethod)ctx_method).add_iterator_var();
		ANode rb = this.parent();
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.isAttached(), "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent();
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

	@virtual typedef This  = RuleCutExpr;

	public RuleCutExpr() {}

	public RuleCutExpr(int pos) {
		this.pos = pos;
	}

	public void rnResolve() {}

	public void resolve1(JumpNodes jn) {
		this.open();
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
	
	@virtual typedef This  = RuleCallExpr;

	@att public ENode				obj;
	@att public ENode[]				args;
	@att public int					env_var;

	public RuleCallExpr() {}

	public RuleCallExpr(CallExpr expr) {
		this.pos = expr.pos;
		this.obj = ~expr.obj;
		this.ident = ~expr.ident;
		this.args.addAll(expr.args.delToArray());
		this.setSuperExpr(expr.isSuperExpr());
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		this.pos = expr.pos;
		this.obj = ~expr.expr;
		if( expr.expr instanceof LVarExpr )
			this.ident = ~((LVarExpr)expr.expr).ident;
		else if( expr.expr instanceof SFldExpr )
			this.ident = ~((SFldExpr)expr.expr).ident;
		else if( expr.expr instanceof IFldExpr ) {
			this.ident = ~((IFldExpr)expr.expr).ident;
			this.obj = ~((IFldExpr)expr.expr).obj;
		}
		this.args.addAll(expr.args.delToArray());
		this.args.insert(0,new ConstNullExpr()/*expr.env_access*/);
	}

	public void rnResolve() {}

	public void resolve1(JumpNodes jn) {
		this.open();
		this.jn = jn;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
		env_var = ((RuleMethod)ctx_method).add_iterator_var();
		ANode rb = this.parent();
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.isAttached(), "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent();
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
				assert (obj instanceof SuperExpr);
				sb.append("super.");
			} else {
				sb.append(Kiev.reparseExpr(obj,true)).append('.');
			}
		}
		else if (this.isSuperExpr()) {
			sb.append("super.");
		}
		sb.append(ident).append('(');
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
				"if !( "+createTextCall()+" ) {\n"+
					createTextBacktrack(true)+					// backtrack, bt$ may needs to be loaded
				"}\n"+
				createTextMoreCheck(false)
		);
	}
}

@node
public abstract class RuleExprBase extends ASTRuleNode {

	@virtual typedef This  ≤ RuleExprBase;

	@att public ENode				expr;
	@att public ENode				bt_expr;

	public RuleExprBase() {}
	public RuleExprBase(ENode expr, ENode bt_expr) {
		this.expr = expr;
		this.bt_expr = bt_expr;
	}

	public void rnResolve() {
		//expr.resolve(null);

		if( expr instanceof CallExpr ) {
			CallExpr e = (CallExpr)expr;
			if( e.func.type.ret() ≡ Type.tpRule ) {
				replaceWithNode(new RuleCallExpr(~e));
				return;
			}
		}
		else if( expr instanceof ClosureCallExpr ) {
			ClosureCallExpr e = (ClosureCallExpr)expr;
			Type tp = e.getType();
			if( tp ≡ Type.tpRule || (tp instanceof CallType && ((CallType)tp).ret() ≡ Type.tpRule && tp.arity == 0) ) {
				replaceWithNode(new RuleCallExpr(~e));
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
	
	@virtual typedef This  = RuleWhileExpr;

	public RuleWhileExpr() {}

	public RuleWhileExpr(ENode expr) {
		super(expr, null);
	}

	public RuleWhileExpr(ENode expr, ENode bt_expr) {
		super(expr, bt_expr);
	}

	public void rnResolve() {
		super.rnResolve();
		if (!isAttached()) return; // check we were replaced
		if (!expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(expr,"Boolean expression is requared");
		//if (bt_expr != null)
		//	bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.open();
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
				"if !( "+Kiev.reparseExpr(expr,true)+" ) {\n"+
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

	@virtual typedef This  = RuleExpr;
	
	public RuleExpr() {}

	public RuleExpr(ENode expr) {
		super(expr, null);
	}

	public RuleExpr(ENode expr, ENode bt_expr) {
		super(expr, bt_expr);
	}

	public void rnResolve() {
		super.rnResolve();
		if (!isAttached()) {
			if (bt_expr != null)
				throw new CompilerException(bt_expr,"Backtrace expression ignored for rule-call");
			return;
		}
		if (bt_expr != null && expr.getType().equals(Type.tpBoolean))
			throw new CompilerException(bt_expr,"Backtrace expression in boolean rule");
		//if (bt_expr != null)
		//	bt_expr.resolve(null);
	}

	public void resolve1(JumpNodes jn) {
		this.open();
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
					"if !( "+Kiev.reparseExpr(expr,true)+" ) {\n"+
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

