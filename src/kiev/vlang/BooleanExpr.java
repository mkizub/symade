package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RBoolExpr;
import kiev.be.java15.JBoolExpr;
import kiev.ir.java15.RBinaryBooleanOrExpr;
import kiev.be.java15.JBinaryBooleanOrExpr;
import kiev.ir.java15.RBinaryBooleanAndExpr;
import kiev.be.java15.JBinaryBooleanAndExpr;
import kiev.ir.java15.RBinaryBoolExpr;
import kiev.be.java15.JBinaryBoolExpr;
import kiev.ir.java15.RInstanceofExpr;
import kiev.be.java15.JInstanceofExpr;
import kiev.ir.java15.RBooleanNotExpr;
import kiev.be.java15.JBooleanNotExpr;

import kiev.be.java15.Code;
import kiev.be.java15.CodeLabel;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
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
		public void mainResolveOut() {
			Method m = getOp().resolveMethod(this);
			if (m == null) {
				Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
			if (ident == null)
				ident = new SymbolRef(pos, getOp().name);
			if (m instanceof CoreMethod && m.core_func != null) {
				m.normilizeExpr(this);
				return;
			} else {
				ident.symbol = m;
			}
		}
	}

	public BoolExpr() {}

	public Type getType() { return Type.tpBoolean; }

	public static void checkBool(ENode e) {
		Type et = e.getType();
		if (et.isBoolean())
			return;
		if (et ≡ Type.tpRule) {
			e.replaceWithResolve(Type.tpBoolean, fun ()->ENode {
				return new BinaryBoolExpr(e.pos,Operator.NotEquals,e,new ConstNullExpr());
			});
			return;
		}
		if (et instanceof CallType) {
			CallType ct = (CallType)et;
			if (ct.arity == 0 && ct.ret().getAutoCastTo(Type.tpBoolean) != null) {
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

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanOr);
		this.ident = new SymbolRef(op.name, cm);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return Operator.BooleanOr; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanAnd);
		this.ident = new SymbolRef(op.name, cm);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return Operator.BooleanAnd; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	@ref public Operator		op;
	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VBinaryBoolExpr of BinaryBoolExpr extends VBoolExpr {
		public Operator			op;
		public ENode			expr1;
		public ENode			expr2;
	}
	
	public BinaryBoolExpr() {}

	public BinaryBoolExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		this.op = op;
		this.ident = new SymbolRef(op.name, cm);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.InstanceOf);
		this.ident = new SymbolRef(op.name, cm);
		this.expr = args[0];
		this.type = (TypeRef)args[1];
	}
	
	public Operator getOp() { return Operator.InstanceOf; }

	public ENode[] getArgs() { return new ENode[]{expr,type}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanNot);
		this.ident = new SymbolRef(op.name, cm);
		this.expr = args[0];
	}
	
	public Operator getOp() { return Operator.BooleanNot; }

	public ENode[] getArgs() { return new ENode[]{expr}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

}

