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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(lang=LogicLang)
public class RuleMethod extends Method {
	
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in")	Var[]			localvars;
	@DataFlowDefinition(in="this:in")	Block			body;
	@DataFlowDefinition(in="this:in")	WBCCondition[] 	conditions;
	}

	@nodeAttr public Var∅				localvars;
	          public int				base = 1;
	          public int				max_depth;
	          public int				state_depth;
	          public int				max_vars;
	          public int				index;		// index counter for RuleNode.idx

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "localvars") {
			Var p = (Var)data;
			if (ct == ChildChangeType.ATTACHED && p.kind == Var.VAR_LOCAL)
				p.mflags_var_kind = Var.VAR_RULE;
			else if (ct == ChildChangeType.DETACHED && p.kind == Var.VAR_RULE)
				p.mflags_var_kind = Var.VAR_LOCAL;
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public RuleMethod() {}

	public RuleMethod(String name, int fl) {
		super(name, new TypeRef(Type.tpRule), fl);
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

	public rule resolveNameR(ResInfo path)
		Var@ var;
	{
		isInlinedByDispatcherMethod() || path.getPrevSlotName() == "targs",$cut,false
	;
		path.getPrevSlotName() == "params" ||
		path.getPrevSlotName() == "type_ref" ||
		path.getPrevSlotName() == "dtype_ref",
		$cut,
		path @= targs
	;
		path @= localvars
	;
		path @= params
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_tdecl.xtype.resolveNameAccessR(path)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.getType().resolveNameAccessR(path)
	}

    public void pass3() {
		if !(parent() instanceof Struct)
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = (Struct)this.parent();
		// TODO: check flags for fields
		if( (getFlags() & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic();
			if( body == null ) setAbstract(true);
		}
		if (params.length == 0 || params[0].kind != Var.PARAM_RULE_ENV)
			params.insert(0, new LVar(pos,namePEnv,Type.tpRule,Var.PARAM_RULE_ENV,ACC_FORWARD|ACC_FINAL|ACC_SYNTHETIC));
		// push the method, because formal parameters may refer method's type args
		foreach (Var fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype != null)
				fp.stype.getType(); // resolve
			fp.verifyMetas();
		}
		trace(Kiev.debug && Kiev.debugMultiMethod,"Rule "+this+" has erased type "+this.etype);
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

@ThisIsANode(lang=LogicLang)
public abstract class ASTRuleNode extends ENode {
	public static final ASTRuleNode[]	emptyArray = new ASTRuleNode[0];

	@virtual @final @abstract
	@nodeData public:ro	boolean			more_check;
	@UnVersioned
	@nodeData public		ASTRuleNode		next_check;
	@virtual @final @abstract
	@nodeData public:ro	boolean			more_back;
	@UnVersioned
	@nodeData public		ASTRuleNode		next_back;

	@UnVersioned
	@nodeData public		boolean			jump_to_back;
	@UnVersioned
	@nodeData public		int				depth;
	@UnVersioned
	@virtual
	@nodeData public		int				base;
	@UnVersioned
	@virtual
	@nodeData public		int				idx;

	@getter public final boolean get$more_check() { return this.next_check != null; }
	@getter public final boolean get$more_back() { return this.next_back != null; }
	@getter public int get$idx()  { return this.idx; }
	@getter public int get$base() { return this.base; }

	public ASTRuleNode() {
		depth = -1;
	}

	public abstract void rnResolve();
	public abstract void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back);

	public abstract void testGenerate(SpacePtr space, Struct frame);
}


@ThisIsANode(lang=LogicLang)
public final class RuleBlock extends ENode {
	
	@DataFlowDefinition(out="rnode") private static class DFI {
	@DataFlowDefinition(in="this:in")	ASTRuleNode		rnode;
	}

	@nodeAttr public ASTRuleNode		rnode;

	public RuleBlock() {}

	public RuleBlock(int pos, ASTRuleNode n) {
		this.pos = pos;
		rnode = n;
	}

    public void rnResolve() {
		if (rnode != null)
			rnode.rnResolve();
    }

	public void testGenerate(SpacePtr space, Struct frame) {
		try {
			RuleMethod rule_method = (RuleMethod)ctx_method;
			Block rn = (Block)RewriteContext.rewriteByMacro("kiev·ir·RuleTemplates", "mkRuleBlock", rule_method.max_depth-1, rule_method.localvars);
			SwitchStat sw = null;
			foreach (ASTNode n; rn.stats) {
				if (n instanceof Struct)
					frame = (Struct)n;
				else if (n instanceof SwitchStat)
					sw = (SwitchStat)n;
			}
			rnode.testGenerate(sw.getSpacePtr("stats"), frame);
			//if (Kiev.debug && Kiev.debugRules) {
			//	java.io.File f = new java.io.File("testRuleBlock-"+rule_method.parent()+"-"+rule_method.sname+".txt");
			//	kiev.fmt.Draw_ATextSyntax stx = kiev.fmt.SyntaxManager.getLanguageSyntax("stx-fmt·syntax-for-java", false);
			//	kiev.fmt.SyntaxManager.dumpTextFile(rn, f, stx);
			//}
			this.replaceWithNode(rn);
		} catch (Throwable t) {
			Kiev.reportError(this, t);
		}
	}
}


@ThisIsANode(lang=LogicLang)
public final class RuleOrExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="rules") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	ASTRuleNode[]	rules;
	}

	@nodeAttr public ASTRuleNode∅			rules;

	@getter @nodeData public int get$base() { return rules.length == 0 ? 0 : rules[0].get$base(); }
	@getter @nodeData public int get$idx()  { return rules.length == 0 ? 0 : rules[0].get$idx(); }

	public RuleOrExpr() {}

	public RuleOrExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleOrExpr(int pos, ASTRuleNode[] rules) {
		this.pos = pos;
		this.rules.addAll(rules);
	}

	public void testGenerate(SpacePtr space, Struct frame) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].testGenerate(space, frame);
    	}
	}

    public void rnResolve() {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].rnResolve();
    	}
    }

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		int depth = ((RuleMethod)ctx_method).state_depth;
		int max_depth = depth;
		for(int i=0; i < rules.length; i++ ) {
			if( i < rules.length-1 ) {
				next_check = this.next_check;
				next_back = rules[i+1];
				jump_to_back = true;
			} else {
				next_check = this.next_check;
				next_back = this.next_back;
				jump_to_back = this.jump_to_back;
			}
			((RuleMethod)ctx_method).set_depth(depth);
			rules[i].resolve1(next_check, next_back, jump_to_back);
			max_depth = Math.max(max_depth,((RuleMethod)ctx_method).state_depth);
		}
		((RuleMethod)ctx_method).set_depth(max_depth);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleAndExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="rules") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ASTRuleNode[]	rules;
	}

	@nodeAttr public ASTRuleNode∅			rules;

	@getter @nodeData public int get$base() { return rules.length == 0 ? 0 : rules[0].get$base(); }
	@getter @nodeData public int get$idx()  { return rules.length == 0 ? 0 : rules[0].get$idx(); }

	public RuleAndExpr() {}

	public RuleAndExpr(ASTRuleNode first) {
		this.rules.add(first);
	}

	public RuleAndExpr(int pos, ASTRuleNode[] rules) {
		this.pos = pos;
		this.rules.addAll(rules);
	}

	public void testGenerate(SpacePtr space, Struct frame) {
    	for(int i=0; i < rules.length; i++) {
    		rules[i].testGenerate(space, frame);
    	}
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

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		for(int i=0; i < rules.length-1; i++ ) {
			rules[i].resolve1(rules[i+1], next_back, jump_to_back);
			if (rules[i] instanceof RuleExpr) {
				RuleExpr re = (RuleExpr)rules[i];
				if (re.bt_expr != null) {
					next_back = rules[i];
					jump_to_back = false;
				}
			}
			else if (rules[i] instanceof RuleCutExpr) {
				next_back = null;
				jump_to_back = false;
			}
			else {
				next_back = rules[i];
				jump_to_back = false;
			}
		}
		if (rules.length > 0)
			rules[rules.length-1].resolve1(this.next_check, next_back, jump_to_back);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleIstheExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode	expr;
	}

	@nodeAttr public LVarExpr	var;		// variable of type PVar<...>
	@nodeAttr public ENode		expr;		// expression to check/unify

	public RuleIstheExpr() {}

	public RuleIstheExpr(int pos, LVarExpr var, ENode expr) {
		this.pos = pos;
		this.var = var;
		this.expr = expr;
	}
	
	public Operator getOper() { return Operator.RuleIsThe; }

	public ENode[] getEArgs() { return new ENode[]{var,expr}; }

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.RuleIsThe);
		this.symbol = cm.getSymbol(op.name);
		this.var = (LVarExpr)args[0];
		this.expr = args[1];
	}
	
    public void rnResolve() {
		//var.resolve(null);
		//expr.resolve(((CTimeType)var.var.getType()).getUnboxedType());
    }

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleIstheExpr", this);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleIsoneofExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode	expr;
	}

	public enum IsoneofMode { ARRAY, KENUM, JENUM, ELEMS };

	@nodeAttr public LVarExpr		var;		// variable of type PVar<...>
	@nodeAttr public ENode			expr;		// expression to check/unify
	@nodeAttr public int				iter_var;	// iterator var
	@nodeAttr public IsoneofMode		mode;
	@nodeAttr public TypeRef			itype;

	public RuleIsoneofExpr() {}

	public RuleIsoneofExpr(int pos, LVarExpr var, ENode expr) {
		this.pos = pos;
		this.var = var;
		this.expr = expr;
	}

	public Operator getOper() { return Operator.RuleIsOneOf; }

	public ENode[] getEArgs() { return new ENode[]{var,expr}; }

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.RuleIsOneOf);
		this.symbol = cm.getSymbol(op.name);
		this.var = (LVarExpr)args[0];
		this.expr = args[1];
	}
	
    public void rnResolve() {
		//var.resolve(null);
		//expr.resolve(null);
    }

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(2);
		depth = ((RuleMethod)ctx_method).push();
		expr.resolve(null);
		Type xtype = expr.getType();
		ResInfo<Method> elems;
		if( xtype.isInstanceOf( Type.tpKievEnumeration) ) {
			itype = new TypeRef(xtype);
			mode = IsoneofMode.KENUM;
		} else if( xtype.isInstanceOf( Type.tpJavaEnumeration) ) {
			itype = new TypeRef(xtype);
			mode = IsoneofMode.JENUM;
		} else if( PassInfo.resolveBestMethodR(xtype,
				elems=new ResInfo<Method>(this,nameElements,ResInfo.noStatic|ResInfo.noSyntaxContext),
				new CallType(xtype,null,null,Type.tpAny,false))
		) {
			itype = new TypeRef(Type.getRealType(xtype,elems.resolvedDNode().mtype.ret()));
			mode = IsoneofMode.ELEMS;
		} else if( xtype.isInstanceOf(Type.tpArray) ) {
			TVarBld set = new TVarBld();
			set.append(Type.tpArrayEnumerator.tdecl.args[0].getAType(), xtype.resolve(((ComplexTypeDecl)Type.tpArray.meta_type.tdecl).args[0].getAType()));
			itype = new TypeRef(Type.tpArrayEnumerator.meta_type.make(set));
			mode = IsoneofMode.ARRAY;
		} else {
			throw new CompilerException(expr,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+xtype+" found");
		}
		iter_var = ((RuleMethod)ctx_method).add_iterator_var();
		ANode rb = this.parent();
		while( rb!=null && !(rb instanceof RuleBlock)) {
			Debug.assert(rb.isAttached(), "Parent of "+rb.getClass()+":"+rb+" is null");
			rb = rb.parent();
		}
		Debug.assert(rb != null);
		Debug.assert(rb instanceof RuleBlock);
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleIsoneofExpr", this);
		frame.members += new Field("$iter$"+iter_var,itype.getType(),0);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleCutExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public RuleCutExpr() {}

	public RuleCutExpr(int pos) {
		this.pos = pos;
	}

	public void rnResolve() {}

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		idx = ++((RuleMethod)ctx_method).index;
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleCutExpr", this);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleCallExpr extends ASTRuleNode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode		obj;
	@DataFlowDefinition(in="obj", seq="true")		ENode[]		args;
	}
	
	@nodeAttr public ENode				obj;
	@nodeAttr public ENode∅			args;
	@nodeAttr public int				env_var;

	public RuleCallExpr() {}

	public RuleCallExpr(CallExpr expr) {
		this.pos = expr.pos;
		this.obj = ~expr.obj;
		this.ident = expr.ident;
		this.args.addAll(expr.args.delToArray());
		this.setSuperExpr(expr.isSuperExpr());
	}

	public RuleCallExpr(ClosureCallExpr expr) {
		this.pos = expr.pos;
		this.obj = ~expr.expr;
		if( expr.expr instanceof LVarExpr )
			this.ident = ((LVarExpr)expr.expr).ident;
		else if( expr.expr instanceof SFldExpr )
			this.ident = ((SFldExpr)expr.expr).ident;
		else if( expr.expr instanceof IFldExpr ) {
			this.ident = ((IFldExpr)expr.expr).ident;
			this.obj = ~((IFldExpr)expr.expr).obj;
		}
		this.args.addAll(expr.args.delToArray());
		this.args.insert(0,new ConstNullExpr()/*expr.env_access*/);
	}

	public void rnResolve() {}

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
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
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		Block rn = (Block)RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleCallExpr", this, this.isSuperExpr());
		frame.members += new Field("$rc$frame$"+env_var,StdTypes.tpRule,0);
	}
}

@ThisIsANode(lang=LogicLang)
public abstract class RuleExprBase extends ASTRuleNode {

	@nodeAttr public ENode				expr;
	@nodeAttr public ENode				bt_expr;

	public RuleExprBase() {}
	public RuleExprBase(ENode expr, ENode bt_expr) {
		this.expr = expr;
		this.bt_expr = bt_expr;
	}

	public void rnResolve() {
		//expr.resolve(null);

		if( expr instanceof CallExpr ) {
			CallExpr e = (CallExpr)expr;
			if( e.func.mtype.ret() ≡ Type.tpRule ) {
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

@ThisIsANode(lang=LogicLang)
public final class RuleWhileExpr extends RuleExprBase {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	@DataFlowDefinition(in="this:in")	ENode		bt_expr;
	}
	
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

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		idx = ++((RuleMethod)ctx_method).index;
		base = ((RuleMethod)ctx_method).allocNewBase(1);
		depth = ((RuleMethod)ctx_method).push();
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleWhile", this, bt_expr != null);
	}
}

@ThisIsANode(lang=LogicLang)
public final class RuleExpr extends RuleExprBase {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	@DataFlowDefinition(in="this:in")	ENode		bt_expr;
	}

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

	public void resolve1(ASTRuleNode next_check, ASTRuleNode next_back, boolean jump_to_back) {
		this.next_check = next_check;
		this.next_back = next_back;
		this.jump_to_back = jump_to_back;
		idx = ++((RuleMethod)ctx_method).index;
		if (bt_expr != null) {
			base = ((RuleMethod)ctx_method).allocNewBase(1);
			depth = ((RuleMethod)ctx_method).push();
		}
	}

	public void testGenerate(SpacePtr space, Struct frame) {
		RewriteContext.rewriteByMacro(space, "kiev·ir·RuleTemplates", "mkRuleExpr", this, bt_expr != null, expr.getType() ≡ StdTypes.tpBoolean);
	}
}

