package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
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

	public Operator			op;
	
	public UnresExpr() {}

	public UnresExpr(int pos, Operator op) {
		super(pos);
		this.op = op;
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
	@ref public ENode		expr;
	
	public PrefixExpr() {}

	public PrefixExpr(int pos, Operator op, ENode expr) {
		super(pos, op);
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
	@ref public ENode		expr;
	
	public PostfixExpr() {}

	public PostfixExpr(int pos, Operator op, ENode expr) {
		super(pos, op);
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

	@ref public ENode		expr1;
	@ref public ENode		expr2;
	
	public InfixExpr() {}

	public InfixExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		super(pos, op);
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
	@ref public final NArr<ENode>		exprs;

	public MultiExpr() {}

	public MultiExpr(int pos, MultiOperator op, List<ENode> exprs) {
		super(pos, op);
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
	@ref public final ENode				obj;	// access expression or type ref
	@ref public final Named				func;	// function name
	@ref public final NArr<ENode>		args;

	public UnresCallExpr() {}

	public UnresCallExpr(int pos, ENode obj, Named func, ENode[] args, boolean super_flag) {
		super(pos, null);
		this.obj = obj;
		this.func = func;
		this.args.addAll(args);
		this.setSuperExpr(super_flag);
	}

	public UnresCallExpr(int pos, ENode obj, Named func, NArr<ENode> args, boolean super_flag) {
		this(pos, obj, func, args.toArray(), super_flag);
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
				CallExpr ce = new CallExpr(pos, (ENode)~obj, m, args.toArray());
				m.makeArgs(ce.args, null);
				return ce;
			} else {
				Field f = (Field)func;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args.toArray());
			}
		} else {
			if (func instanceof Method) {
				Method m = (Method)func;
				CallExpr ce = new CallExpr(pos, (ENode)~obj, m, args.toArray(), isSuperExpr());
				ce.setCastCall(this.isCastCall());
				m.makeArgs(ce.args, null);
				return ce;
			} else {
				return new ClosureCallExpr(pos, (ENode)~obj, args.toArray());
			}
		}
	}
}




