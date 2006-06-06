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
public class UnresOpExpr extends UnresExpr {
	
	@virtual typedef This  = UnresOpExpr;
	@virtual typedef VView = VUnresOpExpr;

	@ref public ENode[]				exprs;

	@nodeview
	public static view VUnresOpExpr of UnresOpExpr extends VUnresExpr {
		public:ro	ENode[]			exprs;
	}
	
	public UnresOpExpr() {}

	public UnresOpExpr(int pos, Operator op, ENode[] exprs) {
		this.pos = pos;
		this.op = op;
		foreach (ENode e; exprs; !(e instanceof ASTOperator))
			this.exprs.add(e);
	}
	
	public ENode[] getArgs() { return exprs; }

	public String toString() {
		return op.toString(this);
	}

	public ENode closeBuild() {
		for (int i=0; i < exprs.length; i++)
			exprs[i] = exprs[i].closeBuild().detach();
		Operator op = this.op;
		if (exprs.length == 1)
			return new UnaryExpr(0,op,exprs[0]);
		if (exprs.length == 2) {
//			if (op == null && e1 instanceof ReinterpExpr) {
//				exprs[0].expr = exprs[1];
//				return exprs[0];
//			}
			if (op == Operator.Cast || op == Operator.CastForce)
				return new CastExpr(pos, (TypeRef)exprs[0], exprs[1]);
			if (op == Operator.Reinterp)
				return new ReinterpExpr(pos, (TypeRef)exprs[0], exprs[1]);
//			if (op instanceof AssignOperator)
//				return new AssignExpr(pos,op,exprs[0],exprs[1]);
//			if (((BinaryOperator)op).is_boolean_op) {
				if (op==Operator.InstanceOf)
					return new InstanceofExpr(pos,exprs[0],((TypeRef)exprs[1]).getType());
				if (op==Operator.BooleanOr)
					return new BinaryBooleanOrExpr(pos,exprs[0],exprs[1]);
				if (op==Operator.BooleanAnd)
					return new BinaryBooleanAndExpr(pos,exprs[0],exprs[1]);
//				return new BinaryBoolExpr(pos,op,exprs[0],exprs[1]);
//			}
			return new BinaryExpr(pos,op,exprs[0],exprs[1]);
		}
		if (op == Operator.Conditional)
			return new ConditionalExpr(pos,exprs[0],exprs[1],exprs[2]);
		throw new CompilerException(this,"Cannot build expression "+this);
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
	@ref public TypeRef[]			targs;
	@ref public ENode[]				args;

	@nodeview
	public static view VUnresCallExpr of UnresCallExpr extends VUnresExpr {
		public		ENode			obj;
		public		SymbolRef		func;
		public:ro	TypeRef[]		targs;
		public:ro	ENode[]			args;
	}
	
	public UnresCallExpr() {}

	public UnresCallExpr(int pos, ENode obj, DNode func, TypeRef[] targs, ENode[] args, boolean super_flag) {
		this(pos, obj, new SymbolRef(pos, func), targs, args, super_flag);
	}
	public UnresCallExpr(int pos, ENode obj, SymbolRef func, TypeRef[] targs, ENode[] args, boolean super_flag) {
		this.pos = pos;
		this.obj = obj;
		this.func = func;
		this.targs.addAll(targs);
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
		for (int i=0; i < targs.length; i++)
			targs[i].detach();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].closeBuild().detach();
		if (obj instanceof TypeRef) {
			if (func.symbol instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, ~func, targs, args);
				return ce;
			} else {
				Field f = (Field)func.symbol;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args);
			}
		} else {
			if (func.symbol instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, ~func, targs, args);
				if (isSuperExpr())
					ce.setSuperExpr(true);
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



