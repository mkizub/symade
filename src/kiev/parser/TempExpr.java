package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

/**
 * Base class to represent unresolved, temporary created expressions.
 */
@node
public abstract class UnresExpr extends ENode {

	@node
	public static class UnresExprImpl extends ENodeImpl {
		@ref public Operator				op;
		public UnresExprImpl() {}
		public UnresExprImpl(int pos, Operator op) { super(pos); this.op = op; }
	}
	@nodeview
	public static view UnresExprView of UnresExprImpl extends ENodeView {
		public				Operator			op;
	}
	
	@ref public abstract virtual			Operator				op;
	
	@getter public Operator			get$op()				{ return this.getUnresExprView().op; }
	@setter public void		set$op(Operator val)			{ this.getUnresExprView().op = val; }

	public NodeView					getNodeView()			{ return new UnresExprView((UnresExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()			{ return new UnresExprView((UnresExprImpl)this.$v_impl); }
	public UnresExprView			getUnresExprView()		{ return new UnresExprView((UnresExprImpl)this.$v_impl); }
	

	public UnresExpr(UnresExprImpl $view) {
		super($view);
	}

	public Operator getOp() { return op; }
	
	public abstract ENode toResolvedExpr();
	
	public void resolve(Type reqType) {
		replaceWithResolve(reqType, fun ()->ENode {return toResolvedExpr();});
	}
	
}

/**
 * Represents unresolved, temporary created prefix expression.
 *
 * 'expr' field is @ref to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@node
public class PrefixExpr extends UnresExpr {
	
	@node
	public static class PrefixExprImpl extends UnresExprImpl {
		@ref public ENode				expr;
		public PrefixExprImpl() {}
		public PrefixExprImpl(int pos, Operator op) { super(pos, op); }
	}
	@nodeview
	public static view PrefixExprView of PrefixExprImpl extends UnresExprView {
		public				ENode			expr;
	}
	
	@ref public abstract virtual			ENode				expr;
	
	@getter public ENode			get$expr()				{ return this.getPrefixExprView().expr; }
	
	@setter public void		set$expr(ENode val)				{ this.getPrefixExprView().expr = val; }

	public NodeView				getNodeView()			{ return new PrefixExprView((PrefixExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new PrefixExprView((PrefixExprImpl)this.$v_impl); }
	public UnresExprView		getUnresExprView()		{ return new PrefixExprView((PrefixExprImpl)this.$v_impl); }
	public PrefixExprView		getPrefixExprView()		{ return new PrefixExprView((PrefixExprImpl)this.$v_impl); }
	
	public PrefixExpr() {
		super(new PrefixExprImpl());
	}

	public PrefixExpr(int pos, Operator op, ENode expr) {
		super(new PrefixExprImpl(pos, op));
		this.expr = expr;
	}
	
	public String toString() {
		return op + " ( " +expr+ " )"; 
	}

	public ENode toResolvedExpr() {
		ENode e = expr;
		if (e instanceof UnresExpr)
			e = ((UnresExpr)e).toResolvedExpr();
		else
			e.detach();
		if (op instanceof CastOperator)
			return new CastExpr(pos,((CastOperator)op).type,e,((CastOperator)op).reinterp);
		return new UnaryExpr(0,op,e);
	}
	
}

/**
 * Represents unresolved, temporary created postfix expression.
 *
 * 'expr' field is @ref to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@node
public class PostfixExpr extends UnresExpr {
	
	@node
	public static class PostfixExprImpl extends UnresExprImpl {
		@ref public ENode				expr;
		public PostfixExprImpl() {}
		public PostfixExprImpl(int pos, Operator op) { super(pos, op); }
	}
	@nodeview
	public static view PostfixExprView of PostfixExprImpl extends UnresExprView {
		public				ENode			expr;
	}
	
	@ref public abstract virtual			ENode				expr;
	
	@getter public ENode			get$expr()				{ return this.getPostfixExprView().expr; }
	
	@setter public void		set$expr(ENode val)				{ this.getPostfixExprView().expr = val; }

	public NodeView				getNodeView()			{ return new PostfixExprView((PostfixExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new PostfixExprView((PostfixExprImpl)this.$v_impl); }
	public UnresExprView		getUnresExprView()		{ return new PostfixExprView((PostfixExprImpl)this.$v_impl); }
	public PostfixExprView		getPostfixExprView()	{ return new PostfixExprView((PostfixExprImpl)this.$v_impl); }
	
	public PostfixExpr() {
		super(new PostfixExprImpl());
	}

	public PostfixExpr(int pos, Operator op, ENode expr) {
		super(new PostfixExprImpl(pos, op));
		this.expr = expr;
	}
	
	public String toString() {
		return "( " +expr+ " ) "+op; 
	}

	public ENode toResolvedExpr() {
		ENode e = expr;
		if (e instanceof UnresExpr)
			e = ((UnresExpr)e).toResolvedExpr();
		else
			e.detach();
		return new UnaryExpr(0,op,e);
	}
	
}

/**
 * Represents unresolved, temporary created infix expression.
 *
 * 'expr1' and 'expr2' fields are @ref to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved binary expression is created.
 */
@node
public class InfixExpr extends UnresExpr {

	@node
	public static class InfixExprImpl extends UnresExprImpl {
		@ref public ENode				expr1;
		@ref public ENode				expr2;
		public InfixExprImpl() {}
		public InfixExprImpl(int pos, Operator op) { super(pos, op); }
	}
	@nodeview
	public static view InfixExprView of InfixExprImpl extends UnresExprView {
		public				ENode			expr1;
		public				ENode			expr2;
	}
	
	@ref public abstract virtual			ENode				expr1;
	@ref public abstract virtual			ENode				expr2;
	
	@getter public ENode			get$expr1()				{ return this.getInfixExprView().expr1; }
	@getter public ENode			get$expr2()				{ return this.getInfixExprView().expr2; }
	
	@setter public void		set$expr1(ENode val)			{ this.getInfixExprView().expr1 = val; }
	@setter public void		set$expr2(ENode val)			{ this.getInfixExprView().expr2 = val; }

	public NodeView				getNodeView()			{ return new InfixExprView((InfixExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new InfixExprView((InfixExprImpl)this.$v_impl); }
	public UnresExprView		getUnresExprView()		{ return new InfixExprView((InfixExprImpl)this.$v_impl); }
	public InfixExprView		getInfixExprView()		{ return new InfixExprView((InfixExprImpl)this.$v_impl); }
	
	public InfixExpr() {
		super(new InfixExprImpl());
	}

	public InfixExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		super(new InfixExprImpl(pos, op));
		this.expr1 = expr1;
		this.expr2 = expr2;
	}
	
	public String toString() {
		return "( " +expr1+ " ) "+op+" ( "+expr2+" )"; 
	}
	
	public ENode toResolvedExpr() {
		ENode e1 = expr1;
		if (e1 instanceof UnresExpr)
			e1 = ((UnresExpr)e1).toResolvedExpr();
		else
			e1.detach();
		ENode e2 = expr2;
		if (e2 instanceof UnresExpr)
			e2 = ((UnresExpr)e2).toResolvedExpr();
		else
			e2.detach();
		if (op instanceof AssignOperator)
			return new AssignExpr(pos,(AssignOperator)op,e1,e2);
		if (((BinaryOperator)op).is_boolean_op) {
			if (op==BinaryOperator.BooleanOr)
				return new BinaryBooleanOrExpr(pos,e1,e2);
			if (op==BinaryOperator.BooleanAnd)
				return new BinaryBooleanAndExpr(pos,e1,e2);
			return new BinaryBoolExpr(pos,(BinaryOperator)op,e1,e2);
		}
		return new BinaryExpr(pos,(BinaryOperator)op,e1,e2);
	}
	

}

/**
 * Represents unresolved, temporary created multi-operand expression.
 *
 * 'exprs' field is @ref to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved multi-expression is created.
 */
@node
public class MultiExpr extends UnresExpr {

	@node
	public static final class MultiExprImpl extends UnresExprImpl {
		@ref public NArr<ENode>			exprs;
		public MultiExprImpl() {}
		public MultiExprImpl(int pos, MultiOperator op) { super(pos, op); }
	}
	@nodeview
	public static final view MultiExprView of MultiExprImpl extends UnresExprView {
		public access:ro	NArr<ENode>			exprs;
	}

	@ref public abstract virtual access:ro NArr<ENode>			exprs;
	
	public NodeView				getNodeView()			{ return new MultiExprView((MultiExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new MultiExprView((MultiExprImpl)this.$v_impl); }
	public UnresExprView		getUnresExprView()		{ return new MultiExprView((MultiExprImpl)this.$v_impl); }
	public MultiExprView		getMultiExprView()		{ return new MultiExprView((MultiExprImpl)this.$v_impl); }

	@getter public NArr<ENode>		get$exprs()		{ return this.getMultiExprView().exprs; }

	public MultiExpr() {
		super(new MultiExprImpl());
	}

	public MultiExpr(int pos, MultiOperator op, List<ENode> exprs) {
		super(new MultiExprImpl(pos, op));
		foreach (ENode n; exprs)
			this.exprs.add((ENode)~n);
	}

	public String toString() {
		MultiOperator op = (MultiOperator)this.op;
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < exprs.length; i++) {
			sb.append("(").append(exprs[i]).append(')');
			if (op.images.length > i)
				sb.append(((MultiOperator)op).images[i]);
		}
		return sb.toString(); 
	}

	public ENode toResolvedExpr() {
		if (op == MultiOperator.Conditional) {
			ENode e1 = exprs[0];
			if (e1 instanceof UnresExpr)
				e1 = ((UnresExpr)e1).toResolvedExpr();
			else
				e1.detach();
			ENode e2 = exprs[1];
			if (e2 instanceof UnresExpr)
				e2 = ((UnresExpr)e2).toResolvedExpr();
			else
				e2.detach();
			ENode e3 = exprs[2];
			if (e3 instanceof UnresExpr)
				e3 = ((UnresExpr)e3).toResolvedExpr();
			else
				e3.detach();
			return new ConditionalExpr(pos,e1,e2,e3);
		}
		throw new CompilerException(this,"Multi-operators are not implemented");
	}
}

/**
 * Represents unresolved, temporary created call expression.
 *
 * 'exprs' field is @ref to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved multi-expression is created.
 */
@node
public class UnresCallExpr extends UnresExpr {

	@node
	public static class UnresCallExprImpl extends UnresExprImpl {
		@ref public ENode				obj;
		@ref public Named				func;
		@ref public CallType			mt;
		@ref public NArr<ENode>			args;
		public UnresCallExprImpl() {}
		public UnresCallExprImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static view UnresCallExprView of UnresCallExprImpl extends UnresExprView {
		public				ENode			obj;
		public				Named			func;
		public				CallType		mt;
		public access:ro	NArr<ENode>		args;
	}
	
	@ref public abstract virtual			ENode				obj;
	@ref public abstract virtual			Named				func;
	@ref public abstract virtual			CallType			mt;
	@ref public abstract virtual access:ro	NArr<ENode>			args;
	
	@getter public ENode			get$obj()				{ return this.getUnresCallExprView().obj; }
	@getter public Named			get$func()				{ return this.getUnresCallExprView().func; }
	@getter public CallType			get$mt()				{ return this.getUnresCallExprView().mt; }
	@getter public NArr<ENode>		get$args()				{ return this.getUnresCallExprView().args; }
	
	@setter public void		set$obj(ENode val)				{ this.getUnresCallExprView().obj = val; }
	@setter public void		set$func(Named val)				{ this.getUnresCallExprView().func = val; }
	@setter public void		set$mt(CallType val)			{ this.getUnresCallExprView().mt = val; }

	public NodeView					getNodeView()			{ return new UnresCallExprView((UnresCallExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()			{ return new UnresCallExprView((UnresCallExprImpl)this.$v_impl); }
	public UnresExprView			getUnresExprView()		{ return new UnresCallExprView((UnresCallExprImpl)this.$v_impl); }
	public UnresCallExprView		getUnresCallExprView()	{ return new UnresCallExprView((UnresCallExprImpl)this.$v_impl); }
	

	public UnresCallExpr() {
		super(new UnresCallExprImpl());
	}

	public UnresCallExpr(int pos, ENode obj, Named func, CallType mt, ENode[] args, boolean super_flag) {
		super(new UnresCallExprImpl(pos));
		this.obj = obj;
		this.func = func;
		this.mt = mt;
		this.args.addAll(args);
		this.setSuperExpr(super_flag);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(obj).append('.').append(func);
		sb.append('(');
		for (int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if (i < args.length)
				sb.append(',');
		}
		sb.append(')');
		return sb.toString(); 
	}

	public ENode toResolvedExpr() {
		for (int i=0; i < args.length; i++) {
			if (args[i] instanceof UnresExpr)
				args[i] = ((UnresExpr)args[i]).toResolvedExpr();
			else
				args[i].detach();
		}
		if (obj instanceof TypeRef) {
			if (func instanceof Method) {
				Method m = (Method)func;
				CallExpr ce = new CallExpr(pos, (ENode)~obj, m, mt, args.toArray());
				return ce;
			} else {
				Field f = (Field)func;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args.toArray());
			}
		} else {
			if (func instanceof Method) {
				Method m = (Method)func;
				CallExpr ce = new CallExpr(pos, (ENode)~obj, m, mt, args.toArray(), isSuperExpr());
				ce.setCastCall(this.isCastCall());
				return ce;
			} else {
				return new ClosureCallExpr(pos, (ENode)~obj, args.toArray());
			}
		}
	}
}




