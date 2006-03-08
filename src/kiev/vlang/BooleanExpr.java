package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.ir.java.RBoolExpr;
import kiev.be.java.JBoolExpr;
import kiev.ir.java.RBinaryBooleanOrExpr;
import kiev.be.java.JBinaryBooleanOrExpr;
import kiev.ir.java.RBinaryBooleanAndExpr;
import kiev.be.java.JBinaryBooleanAndExpr;
import kiev.ir.java.RBinaryBoolExpr;
import kiev.be.java.JBinaryBoolExpr;
import kiev.ir.java.RInstanceofExpr;
import kiev.be.java.JInstanceofExpr;
import kiev.ir.java.RBooleanNotExpr;
import kiev.be.java.JBooleanNotExpr;

import kiev.be.java.Code;
import kiev.be.java.CodeLabel;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public abstract class BoolExpr extends ENode {

	@virtual typedef This  = BoolExpr;
	@virtual typedef VView = VBoolExpr;
	@virtual typedef JView = JBoolExpr;
	@virtual typedef VView = RBoolExpr;

	@nodeview
	public abstract static view VBoolExpr of BoolExpr extends VENode {
	}

	public BoolExpr() {}

	public Type getType() { return Type.tpBoolean; }

	public static void checkBool(ENode e) {
		Type et = e.getType();
		if (et.isBoolean())
			return;
		if (et ≡ Type.tpRule) {
			e.replaceWithResolve(Type.tpBoolean, fun ()->ENode {
				return new BinaryBoolExpr(e.pos,BinaryOperator.NotEquals,e,new ConstNullExpr());
			});
			return;
		}
		if (et instanceof CallType) {
			CallType ct = (CallType)et;
			if (ct.arity == 0 && ct.ret().isAutoCastableTo(Type.tpBoolean)	) {
				((ClosureCallExpr)e).is_a_call = Boolean.TRUE;
				return;
			}
		}
		throw new RuntimeException("Expression "+e+" must be of boolean type, but found "+e.getType());
	}
	
}

@node
public class BinaryBooleanOrExpr extends BoolExpr {

	@dflow(tru="join expr1:true expr2:true", fls="expr2:false") private static class DFI {
	@dflow(in="this:in")			ENode			expr1;
	@dflow(in="expr1:false")		ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBooleanOrExpr;
	@virtual typedef VView = VBinaryBooleanOrExpr;
	@virtual typedef JView = JBinaryBooleanOrExpr;
	@virtual typedef RView = RBinaryBooleanOrExpr;

	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VBinaryBooleanOrExpr of BinaryBooleanOrExpr extends VBoolExpr {
		public ENode		expr1;
		public ENode		expr2;
	}
	
	public BinaryBooleanOrExpr() {}

	public BinaryBooleanOrExpr(int pos, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return BinaryOperator.BooleanOr; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (expr1 == null)
			sb.append("(?)");
		else if( expr1.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(BinaryOperator.BooleanOr.image);
		if (expr2 == null)
			sb.append("(?)");
		else if( expr2.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < opBooleanOrPriority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(BinaryOperator.BooleanOr.image);
		if( expr2.getPriority() < opBooleanOrPriority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}


@node
public class BinaryBooleanAndExpr extends BoolExpr {

	@dflow(fls="join expr1:false expr2:false", tru="expr2:true") private static class DFI {
	@dflow(in="this:in")		ENode			expr1;
	@dflow(in="expr1:true")		ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBooleanAndExpr;
	@virtual typedef VView = VBinaryBooleanAndExpr;
	@virtual typedef JView = JBinaryBooleanAndExpr;
	@virtual typedef RView = RBinaryBooleanAndExpr;

	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VBinaryBooleanAndExpr of BinaryBooleanAndExpr extends VBoolExpr {
		public ENode		expr1;
		public ENode		expr2;
	}
	
	public BinaryBooleanAndExpr() {}

	public BinaryBooleanAndExpr(int pos, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return BinaryOperator.BooleanAnd; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( expr1.getPriority() < opBooleanAndPriority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(BinaryOperator.BooleanAnd.image);
		if( expr2.getPriority() < opBooleanAndPriority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < opBooleanAndPriority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(BinaryOperator.BooleanAnd.image);
		if( expr2.getPriority() < opBooleanAndPriority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}

@node
public class BinaryBoolExpr extends BoolExpr {
	
	@dflow(out="expr2") private static class DFI {
	@dflow(in="this:in")		ENode			expr1;
	@dflow(in="expr1")			ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBoolExpr;
	@virtual typedef VView = VBinaryBoolExpr;
	@virtual typedef JView = JBinaryBoolExpr;
	@virtual typedef RView = RBinaryBoolExpr;

	@ref public BinaryOperator	op;
	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VBinaryBoolExpr of BinaryBoolExpr extends VBoolExpr {
		public BinaryOperator	op;
		public ENode			expr1;
		public ENode			expr2;

		public void mainResolveOut() {
			Type et1 = expr1.getType();
			Type et2 = expr2.getType();
			if( op==BinaryOperator.BooleanOr ) {
				if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
					replaceWithNode(new BinaryBooleanOrExpr(pos,expr1,expr2));
					return;
				}
			}
			else if( op==BinaryOperator.BooleanAnd ) {
				if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
					replaceWithNode(new BinaryBooleanAndExpr(pos,expr1,expr2));
					return;
				}
			}
		}
	}
	
	public BinaryBoolExpr() {}

	public BinaryBoolExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return op; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(expr1).append(op.image).append(expr2);
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < op.priority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(op.image);
		if( expr2.getPriority() < op.priority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}

@node
public class InstanceofExpr extends BoolExpr {

	@dflow(tru="this:tru()", fls="expr") private static class DFI {
	@dflow(in="this:in")		ENode			expr;
	}
	
	@virtual typedef This  = InstanceofExpr;
	@virtual typedef VView = VInstanceofExpr;
	@virtual typedef JView = JInstanceofExpr;
	@virtual typedef RView = RInstanceofExpr;

	@att public ENode		expr;
	@att public TypeRef		type;

	@nodeview
	public static view VInstanceofExpr of InstanceofExpr extends VBoolExpr {
		public ENode	expr;
		public TypeRef	type;
	}
	
	public InstanceofExpr() {}

	public InstanceofExpr(int pos, ENode expr, TypeRef type) {
		this.pos = pos;
		this.expr = expr;
		this.type = type;
	}

	public InstanceofExpr(int pos, ENode expr, Type type) {
		this.pos = pos;
		this.expr = expr;
		this.type = new TypeRef(type);
	}

	public Operator getOp() { return BinaryOperator.InstanceOf; }

	public String toString() {
		return expr+" instanceof "+type;
	}

	static class InstanceofExprDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		InstanceofExprDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("expr"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = ((InstanceofExpr)dfi.node_impl).addNodeTypeInfo(DFFunc.calc(f, dfi));
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncTru(DataFlowInfo dfi) {
		return new InstanceofExprDFFunc(dfi);
	}

	DFState addNodeTypeInfo(DFState dfs) {
		LvalDNode[] path = null;
		switch(expr) {
		case LVarExpr:
			path = new LvalDNode[]{((LVarExpr)expr).getVar()};
			break;
		case IFldExpr:
			path = ((IFldExpr)expr).getAccessPath();
			break;
		case SFldExpr:
			path = new LvalDNode[]{((SFldExpr)expr).var};
			break;
		}
		if (path != null) {
			Type et = expr.getType();
			Type tp = type.getType();
			if (et instanceof CTimeType && !(tp instanceof CTimeType)) {
				tp = et.applay(new TVarBld(et.bindings().tvars[0].var, tp));
			}
			return dfs.addNodeType(path,tp);
		}
		return dfs;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(expr).append(" instanceof ").append(type).space();
		return dmp;
	}
}

@node
public class BooleanNotExpr extends BoolExpr {
	
	@dflow(fls="expr:true", tru="expr:false") private static class DFI {
	@dflow(in="this:in")		ENode			expr;
	}
	
	@virtual typedef This  = BooleanNotExpr;
	@virtual typedef VView = VBooleanNotExpr;
	@virtual typedef JView = JBooleanNotExpr;
	@virtual typedef RView = RBooleanNotExpr;

	@att public ENode		expr;

	@nodeview
	public static view VBooleanNotExpr of BooleanNotExpr extends VBoolExpr {
		public ENode		expr;
	}
	
	public BooleanNotExpr() {}

	public BooleanNotExpr(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
	}

	public Operator getOp() { return PrefixOperator.BooleanNot; }

	public String toString() {
		if( expr.getPriority() < opBooleanNotPriority )
			return "!("+expr+")";
		else
			return "!"+expr;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('!');
		if( expr.getPriority() < opBooleanNotPriority ) {
			dmp.append('(').append(expr).append(')');
		} else {
			dmp.append(expr);
		}
		return dmp;
	}
}

