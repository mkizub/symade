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

	@virtual typedef This  = UnresExpr;
	@virtual typedef VView = VUnresExpr;

	@ref public Operator				op;

	@nodeview
	public static view VUnresExpr of UnresExpr extends VENode {
		public				Operator			op;
	}
	
	public UnresExpr() {}
	
	public Operator getOp() { return op; }
	
	public final void callbackAttached() {
		throw new Error("Internal error: "+this.getClass()+" attached to "+parent().getClass()+" to slot "+pslot().name);
	}
	
	public void resolve(Type reqType) {
		replaceWithResolve(reqType, fun ()->ENode {return closeBuild();});
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
	
	@virtual typedef This  = PrefixExpr;
	@virtual typedef VView = VPrefixExpr;

	@ref public ENode				expr;

	@nodeview
	public static view VPrefixExpr of PrefixExpr extends VUnresExpr {
		public				ENode			expr;
	}
	
	public PrefixExpr() {}

	public PrefixExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
		this.expr = expr;
		assert (op != null && expr != null);
	}
	
	public String toString() {
		return op + " ( " +expr+ " )"; 
	}

	public ENode closeBuild() {
		ENode e = expr.closeBuild().detach();
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
@node
public class PostfixExpr extends UnresExpr {
	
	@virtual typedef This  = PostfixExpr;
	@virtual typedef VView = VPostfixExpr;

	@ref public ENode				expr;

	@nodeview
	public static view VPostfixExpr of PostfixExpr extends VUnresExpr {
		public				ENode			expr;
	}
	
	public PostfixExpr() {}

	public PostfixExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
		this.expr = expr;
		assert (op != null && expr != null);
	}
	
	public String toString() {
		return "( " +expr+ " ) "+op; 
	}

	public ENode closeBuild() {
		ENode e = expr.closeBuild().detach();
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

	@virtual typedef This  = InfixExpr;
	@virtual typedef VView = VInfixExpr;

	@ref public ENode				expr1;
	@ref public ENode				expr2;

	@nodeview
	public static view VInfixExpr of InfixExpr extends VUnresExpr {
		public				ENode			expr1;
		public				ENode			expr2;
	}
	
	public InfixExpr() {}

	public InfixExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
		assert (expr1 != null && expr2 != null);
	}
	
	public String toString() {
		return "( " +expr1+ " ) "+op+" ( "+expr2+" )"; 
	}
	
	public ENode closeBuild() {
		ENode e1 = expr1.closeBuild().detach();
		ENode e2 = expr2.closeBuild().detach();
		if (op == null && e1 instanceof ReinterpExpr) {
			e1.expr = e2;
			return e1;
		}
		if (op instanceof AssignOperator)
			return new AssignExpr(pos,(AssignOperator)op,e1,e2);
		if (((BinaryOperator)op).is_boolean_op) {
			if (op==BinaryOperator.InstanceOf)
				return new InstanceofExpr(pos,e1,((TypeRef)e2).getType());
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

	@virtual typedef This  = MultiExpr;
	@virtual typedef VView = VMultiExpr;

	@ref public ENode[]			exprs;

	@nodeview
	public static final view VMultiExpr of MultiExpr extends VUnresExpr {
		public:ro	ENode[]			exprs;
	}

	public MultiExpr() {}

	public MultiExpr(int pos, MultiOperator op, ENode[] exprs) {
		this.pos = pos;
		this.op = op;
		this.exprs.addAll(exprs);
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

	public ENode closeBuild() {
		if (op == MultiOperator.Conditional) {
			ENode e1 = exprs[0].closeBuild().detach();
			ENode e2 = exprs[1].closeBuild().detach();
			ENode e3 = exprs[2].closeBuild().detach();
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

	@virtual typedef This  = UnresCallExpr;
	@virtual typedef VView = VUnresCallExpr;

	@ref public ENode				obj;
	@ref public SymbolRef			func;
	@ref public CallType			mt;
	@ref public ENode[]				args;

	@nodeview
	public static view VUnresCallExpr of UnresCallExpr extends VUnresExpr {
		public		ENode			obj;
		public		SymbolRef		func;
		public		CallType		mt;
		public:ro	ENode[]			args;
	}
	
	public UnresCallExpr() {}

	public UnresCallExpr(int pos, ENode obj, DNode func, CallType mt, ENode[] args, boolean super_flag) {
		this(pos, obj, new SymbolRef(pos, func), mt, args, super_flag);
	}
	public UnresCallExpr(int pos, ENode obj, SymbolRef func, CallType mt, ENode[] args, boolean super_flag) {
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

	public ENode closeBuild() {
		ENode obj = this.obj.closeBuild().detach();
		for (int i=0; i < args.length; i++)
			args[i].closeBuild().detach();
		if (obj instanceof TypeRef) {
			if (func.symbol instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, ~func, mt, args, false);
				return ce;
			} else {
				Field f = (Field)func.symbol;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args);
			}
		} else {
			if (func.symbol instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, ~func, mt, args, isSuperExpr());
				ce.setCastCall(this.isCastCall());
				return ce;
			} else {
				return new ClosureCallExpr(pos, obj, args);
			}
		}
	}
}


/**
 * Represents unresolved, temporary created access expression.
 */
@node
public class AccFldExpr extends UnresExpr {

	@virtual typedef This  = AccFldExpr;
	@virtual typedef VView = VAccFldExpr;

	@ref public ENode				obj;
	@ref public Field				fld;

	@nodeview
	public static view VAccFldExpr of AccFldExpr extends VUnresExpr {
		public				ENode			obj;
		public				Field			fld;
	}
	
	public AccFldExpr() {}

	public AccFldExpr(int pos, ENode obj, Field fld) {
		this.pos = pos;
		this.op = op;
		this.obj = obj;
		this.fld = fld;
	}
	
	public AccFldExpr(int pos, Field fld) {
		this.pos = pos;
		this.op = op;
		this.fld = fld;
		assert (fld.isStatic());
	}
	
	public String toString() {
		if (fld.isStatic())
			return fld.parent()+ "."+fld.id;
		else
			return obj+ "."+fld.id;
	}
	
	public ENode closeBuild() {
		if (fld.isStatic()) {
			return new SFldExpr(pos,fld);
		} else {
			return new IFldExpr(pos,obj.closeBuild().detach(),fld);
		}
	}
}



