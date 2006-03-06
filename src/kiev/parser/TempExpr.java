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
@nodeset
public abstract class UnresExpr extends ENode {

	@virtual typedef This  = UnresExpr;
	@virtual typedef VView = UnresExprView;

	@ref public Operator				op;

	@nodeview
	public static view UnresExprView of UnresExpr extends ENodeView {
		public				Operator			op;

		public Operator getOp() { return op; }
	}
	
	public UnresExpr() {}
	
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
@nodeset
public class PrefixExpr extends UnresExpr {
	
	@virtual typedef This  = PrefixExpr;
	@virtual typedef VView = PrefixExprView;

	@ref public ENode				expr;

	@nodeview
	public static view PrefixExprView of PrefixExpr extends UnresExprView {
		public				ENode			expr;
	}
	
	public PrefixExpr() {}

	public PrefixExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
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
		Operator op = this.op;
		if (op instanceof CastOperator) {
			if (op.reinterp)
				return new ReinterpExpr(pos,op.type,e);
			else
				return new CastExpr(pos,op.type,e);
		}
		return new UnaryExpr(0,op,e);
	}
	
}

/**
 * Represents unresolved, temporary created postfix expression.
 *
 * 'expr' field is @ref to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@nodeset
public class PostfixExpr extends UnresExpr {
	
	@virtual typedef This  = PostfixExpr;
	@virtual typedef VView = PostfixExprView;

	@ref public ENode				expr;

	@nodeview
	public static view PostfixExprView of PostfixExpr extends UnresExprView {
		public				ENode			expr;
	}
	
	public PostfixExpr() {}

	public PostfixExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
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
@nodeset
public class InfixExpr extends UnresExpr {

	@virtual typedef This  = InfixExpr;
	@virtual typedef VView = InfixExprView;

	@ref public ENode				expr1;
	@ref public ENode				expr2;

	@nodeview
	public static view InfixExprView of InfixExpr extends UnresExprView {
		public				ENode			expr1;
		public				ENode			expr2;
	}
	
	public InfixExpr() {}

	public InfixExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
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
@nodeset
public class MultiExpr extends UnresExpr {

	@virtual typedef This  = MultiExpr;
	@virtual typedef VView = MultiExprView;

	@ref public NArr<ENode>			exprs;

	@nodeview
	public static final view MultiExprView of MultiExpr extends UnresExprView {
		public:ro	NArr<ENode>			exprs;
	}

	public MultiExpr() {}

	public MultiExpr(int pos, MultiOperator op, List<ENode> exprs) {
		this.pos = pos;
		this.op = op;
		foreach (ENode n; exprs)
			this.exprs.add(~n);
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
@nodeset
public class UnresCallExpr extends UnresExpr {

	@virtual typedef This  = UnresCallExpr;
	@virtual typedef VView = UnresCallExprView;

	@ref public ENode				obj;
	@ref public Named				func;
	@ref public CallType			mt;
	@ref public NArr<ENode>			args;

	@nodeview
	public static view UnresCallExprView of UnresCallExpr extends UnresExprView {
		public		ENode			obj;
		public		Named			func;
		public		CallType		mt;
		public:ro	NArr<ENode>		args;
	}
	
	public UnresCallExpr() {}

	public UnresCallExpr(int pos, ENode obj, Named func, CallType mt, ENode[] args, boolean super_flag) {
		this.pos = pos;
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
				CallExpr ce = new CallExpr(pos, ~obj, m, mt, args.toArray());
				return ce;
			} else {
				Field f = (Field)func;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args.toArray());
			}
		} else {
			if (func instanceof Method) {
				Method m = (Method)func;
				CallExpr ce = new CallExpr(pos, ~obj, m, mt, args.toArray(), isSuperExpr());
				ce.setCastCall(this.isCastCall());
				return ce;
			} else {
				return new ClosureCallExpr(pos, ~obj, args.toArray());
			}
		}
	}
}




