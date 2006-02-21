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

@nodeset
public abstract class BoolExpr extends ENode {

	@virtual typedef This  = BoolExpr;
	@virtual typedef NImpl = BoolExprImpl;
	@virtual typedef VView = VBoolExpr;
	@virtual typedef JView = JBoolExpr;
	@virtual typedef VView = RBoolExpr;

	@nodeimpl
	public abstract static class BoolExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = BoolExpr;
	}
	@nodeview
	public abstract static view BoolExprView of BoolExprImpl extends ENodeView {
		public Type getType() { return Type.tpBoolean; }
	}
	@nodeview
	public final static view VBoolExpr of BoolExprImpl extends BoolExprView {
	}

	public BoolExpr(BoolExprImpl impl) { super(impl); }

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

@nodeset
public class BinaryBooleanOrExpr extends BoolExpr {

	@dflow(tru="join expr1:true expr2:true", fls="expr2:false") private static class DFI {
	@dflow(in="this:in")			ENode			expr1;
	@dflow(in="expr1:false")		ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBooleanOrExpr;
	@virtual typedef NImpl = BinaryBooleanOrExprImpl;
	@virtual typedef VView = VBinaryBooleanOrExpr;
	@virtual typedef JView = JBinaryBooleanOrExpr;
	@virtual typedef RView = RBinaryBooleanOrExpr;

	@nodeimpl
	public static class BinaryBooleanOrExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BinaryBooleanOrExpr;
		@att public ENode			expr1;
		@att public ENode			expr2;
	}
	@nodeview
	public static abstract view BinaryBooleanOrExprView of BinaryBooleanOrExprImpl extends BoolExprView {
		public ENode		expr1;
		public ENode		expr2;

		public Operator getOp() { return BinaryOperator.BooleanOr; }
	}
	@nodeview
	public static final view VBinaryBooleanOrExpr of BinaryBooleanOrExprImpl extends BinaryBooleanOrExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public BinaryBooleanOrExpr() {
		super(new BinaryBooleanOrExprImpl());
	}

	public BinaryBooleanOrExpr(int pos, ENode expr1, ENode expr2) {
		this();
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

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

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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


@nodeset
public class BinaryBooleanAndExpr extends BoolExpr {

	@dflow(fls="join expr1:false expr2:false", tru="expr2:true") private static class DFI {
	@dflow(in="this:in")		ENode			expr1;
	@dflow(in="expr1:true")		ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBooleanAndExpr;
	@virtual typedef NImpl = BinaryBooleanAndExprImpl;
	@virtual typedef VView = VBinaryBooleanAndExpr;
	@virtual typedef JView = JBinaryBooleanAndExpr;
	@virtual typedef RView = RBinaryBooleanAndExpr;

	@nodeimpl
	public static class BinaryBooleanAndExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BinaryBooleanAndExpr;
		@att public ENode			expr1;
		@att public ENode			expr2;
	}
	@nodeview
	public static abstract view BinaryBooleanAndExprView of BinaryBooleanAndExprImpl extends BoolExprView {
		public ENode		expr1;
		public ENode		expr2;

		public Operator getOp() { return BinaryOperator.BooleanAnd; }
	}
	@nodeview
	public static view VBinaryBooleanAndExpr of BinaryBooleanAndExprImpl extends BinaryBooleanAndExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public BinaryBooleanAndExpr() {
		super(new BinaryBooleanAndExprImpl());
	}

	public BinaryBooleanAndExpr(int pos, ENode expr1, ENode expr2) {
		this();
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

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

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

@nodeset
public class BinaryBoolExpr extends BoolExpr {
	
	@dflow(out="expr2") private static class DFI {
	@dflow(in="this:in")		ENode			expr1;
	@dflow(in="expr1")			ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBoolExpr;
	@virtual typedef NImpl = BinaryBoolExprImpl;
	@virtual typedef VView = VBinaryBoolExpr;
	@virtual typedef JView = JBinaryBoolExpr;
	@virtual typedef RView = RBinaryBoolExpr;

	@nodeimpl
	public static class BinaryBoolExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BinaryBoolExpr;
		@ref public BinaryOperator	op;
		@att public ENode			expr1;
		@att public ENode			expr2;
	}
	@nodeview
	public static view BinaryBoolExprView of BinaryBoolExprImpl extends BoolExprView {
		public BinaryOperator	op;
		public ENode			expr1;
		public ENode			expr2;

		public Operator getOp() { return op; }
	}
	@nodeview
	public static view VBinaryBoolExpr of BinaryBoolExprImpl extends BinaryBoolExprView {

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
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public BinaryBoolExpr() {
		super(new BinaryBoolExprImpl());
	}

	public BinaryBoolExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
		this();
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(expr1).append(op.image).append(expr2);
		return sb.toString();
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

@nodeset
public class InstanceofExpr extends BoolExpr {

	@dflow(tru="this:tru()", fls="expr") private static class DFI {
	@dflow(in="this:in")		ENode			expr;
	}
	
	@virtual typedef This  = InstanceofExpr;
	@virtual typedef NImpl = InstanceofExprImpl;
	@virtual typedef VView = VInstanceofExpr;
	@virtual typedef JView = JInstanceofExpr;
	@virtual typedef RView = RInstanceofExpr;

	@nodeimpl
	public static class InstanceofExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = InstanceofExpr;
		@att public ENode		expr;
		@att public TypeRef		type;
	}
	@nodeview
	public static abstract view InstanceofExprView of InstanceofExprImpl extends BoolExprView {
		public ENode	expr;
		public TypeRef	type;

		public Operator getOp() { return BinaryOperator.InstanceOf; }
	}
	@nodeview
	public static view VInstanceofExpr of InstanceofExprImpl extends InstanceofExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public InstanceofExpr() {
		super(new InstanceofExprImpl());
	}

	public InstanceofExpr(int pos, ENode expr, TypeRef type) {
		this();
		this.pos = pos;
		this.expr = expr;
		this.type = type;
	}

	public InstanceofExpr(int pos, ENode expr, Type type) {
		this();
		this.pos = pos;
		this.expr = expr;
		this.type = new TypeRef(type);
	}

	public String toString() {
		return expr+" instanceof "+type;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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
			res = ((InstanceofExpr)dfi.node_impl.getNode()).addNodeTypeInfo(DFFunc.calc(f, dfi));
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

@nodeset
public class BooleanNotExpr extends BoolExpr {
	
	@dflow(fls="expr:true", tru="expr:false") private static class DFI {
	@dflow(in="this:in")		ENode			expr;
	}
	
	@virtual typedef This  = BooleanNotExpr;
	@virtual typedef NImpl = BooleanNotExprImpl;
	@virtual typedef VView = VBooleanNotExpr;
	@virtual typedef JView = JBooleanNotExpr;
	@virtual typedef RView = RBooleanNotExpr;

	@nodeimpl
	public static class BooleanNotExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BooleanNotExpr;
		@att public ENode		expr;
	}
	@nodeview
	public static abstract view BooleanNotExprView of BooleanNotExprImpl extends BoolExprView {
		public ENode		expr;

		public Operator getOp() { return PrefixOperator.BooleanNot; }
	}
	@nodeview
	public static view VBooleanNotExpr of BooleanNotExprImpl extends BooleanNotExprView {
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	public BooleanNotExpr() {
		super(new BooleanNotExprImpl());
	}

	public BooleanNotExpr(int pos, ENode expr) {
		this();
		this.pos = pos;
		this.expr = expr;
	}

	public String toString() {
		if( expr.getPriority() < opBooleanNotPriority )
			return "!("+expr+")";
		else
			return "!"+expr;
	}

	public void resolve(Type reqType) {
		getRView().resolve(reqType);
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

