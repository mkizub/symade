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
 *
 */

@ThisIsANode(lang=CoreLang)
public abstract class BoolExpr extends ENode {

	public BoolExpr() {}

	public Type getType() { return Type.tpBoolean; }

	public void mainResolveOut() {
		resolveMethodAndNormalize();
	}

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

@ThisIsANode(name="Or", lang=CoreLang)
public class BinaryBooleanOrExpr extends BoolExpr {

	@DataFlowDefinition(tru="join expr1:true expr2:true", fls="expr2:false") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode			expr1;
	@DataFlowDefinition(in="expr1:false")		ENode			expr2;
	}
	
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public BinaryBooleanOrExpr() {}

	public BinaryBooleanOrExpr(int pos, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanOr);
		this.symbol = cm.getSymbol(op.name);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return Operator.BooleanOr; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public boolean	isConstantExpr() {
		if (expr1.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			if (b1 instanceof Boolean && b1.booleanValue())
				return true;
		}
		if (expr2.isConstantExpr()) {
			Object b2 = expr2.getConstValue();
			if (b2 instanceof Boolean && b2.booleanValue())
				return true;
		}
		if (expr1.isConstantExpr() && expr2.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			Object b2 = expr2.getConstValue();
			if (b1 instanceof Boolean && b2 instanceof Boolean)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		if (expr1.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			if (b1 instanceof Boolean && b1.booleanValue())
				return Boolean.TRUE;
		}
		if (expr2.isConstantExpr()) {
			Object b2 = expr2.getConstValue();
			if (b2 instanceof Boolean && b2.booleanValue())
				return Boolean.TRUE;
		}
		Boolean b1 = (Boolean)expr1.getConstValue();
		Boolean b2 = (Boolean)expr2.getConstValue();
		return Boolean.valueOf(b1.booleanValue() || b2.booleanValue());
	}
}


@ThisIsANode(name="And", lang=CoreLang)
public class BinaryBooleanAndExpr extends BoolExpr {

	@DataFlowDefinition(fls="join expr1:false expr2:false", tru="expr2:true") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr1;
	@DataFlowDefinition(in="expr1:true")		ENode			expr2;
	}
	
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public BinaryBooleanAndExpr() {}

	public BinaryBooleanAndExpr(int pos, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanAnd);
		this.symbol = cm.getSymbol(op.name);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return Operator.BooleanAnd; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public boolean	isConstantExpr() {
		if (expr1.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			if (b1 instanceof Boolean && !b1.booleanValue())
				return true;
		}
		if (expr2.isConstantExpr()) {
			Object b2 = expr2.getConstValue();
			if (b2 instanceof Boolean && !b2.booleanValue())
				return true;
		}
		if (expr1.isConstantExpr() && expr2.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			Object b2 = expr2.getConstValue();
			if (b1 instanceof Boolean && b2 instanceof Boolean)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		if (expr1.isConstantExpr()) {
			Object b1 = expr1.getConstValue();
			if (b1 instanceof Boolean && !b1.booleanValue())
				return Boolean.FALSE;
		}
		if (expr2.isConstantExpr()) {
			Object b2 = expr2.getConstValue();
			if (b2 instanceof Boolean && !b2.booleanValue())
				return Boolean.FALSE;
		}
		Boolean b1 = (Boolean)expr1.getConstValue();
		Boolean b2 = (Boolean)expr2.getConstValue();
		return Boolean.valueOf(b1.booleanValue() && b2.booleanValue());
	}
}

@ThisIsANode(name="Cmp", lang=CoreLang)
public class BinaryBoolExpr extends BoolExpr {
	
	@DataFlowDefinition(out="expr2") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr1;
	@DataFlowDefinition(in="expr1")			ENode			expr2;
	}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public Operator		op;
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public BinaryBoolExpr() {}

	public BinaryBoolExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		this.op = op;
		this.symbol = cm.getSymbol(op.name);
		this.expr1 = args[0];
		this.expr2 = args[1];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public void mainResolveOut() {
		resolveMethodAndNormalize();
	}

	public boolean	isConstantExpr() {
		if (!expr1.isConstantExpr())
			return false;
		if (!expr2.isConstantExpr())
			return false;
		DNode m = this.dnode;
		if (m == null) {
			Symbol sym = getOp().resolveMethod(this);
			if (sym != null)
				m = sym.dnode;
		}
		if (!(m instanceof Method) || !(m.body instanceof CoreExpr))
			return false;
		return true;
	}
	public Object	getConstValue() {
		Method m = (Method)this.dnode;
		if (m == null) {
			Symbol sym = getOp().resolveMethod(this);
			if (sym != null)
				m = (Method)sym.dnode;
		}
		ConstExpr ce = ((CoreExpr)m.body).calc(this);
		return ce.getConstValue();
	}
}

@ThisIsANode(name="InstanceOf", lang=CoreLang)
public class InstanceofExpr extends BoolExpr {

	@DataFlowDefinition(tru="this:tru()", fls="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr;
	}
	
	@nodeAttr public ENode			expr;
	@nodeAttr public TypeRef		itype;

	public InstanceofExpr() {}

	public InstanceofExpr(int pos, ENode expr, TypeRef itype) {
		this.pos = pos;
		this.expr = expr;
		this.itype = itype;
	}

	public InstanceofExpr(int pos, ENode expr, Type itype) {
		this.pos = pos;
		this.expr = expr;
		this.itype = new TypeRef(itype);
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.InstanceOf);
		this.symbol = cm.getSymbol(op.name);
		this.expr = args[0];
		this.itype = (TypeRef)args[1];
	}
	
	public Operator getOp() { return Operator.InstanceOf; }

	public ENode[] getArgs() { return new ENode[]{expr,itype}; }

	public String toString() { return getOp().toString(this); }

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
		Var[] path = null;
		switch(expr) {
		case LVarExpr:
			path = new Var[]{((LVarExpr)expr).getVar()};
			break;
		case IFldExpr:
			path = ((IFldExpr)expr).getAccessPath();
			break;
		case SFldExpr:
			path = new Var[]{((SFldExpr)expr).var};
			break;
		}
		if (path != null) {
			Type et = expr.getType();
			Type tp = itype.getType();
			if (et instanceof CTimeType && !(tp instanceof CTimeType)) {
				tp = et.applay(new TVarBld(et.getArg(0), tp));
			}
			return dfs.addNodeType(path,tp);
		}
		return dfs;
	}
}

@ThisIsANode(name="Not", lang=CoreLang)
public class BooleanNotExpr extends BoolExpr {
	
	@DataFlowDefinition(fls="expr:true", tru="expr:false") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode			expr;
	}
	
	@nodeAttr public ENode		expr;

	public BooleanNotExpr() {}

	public BooleanNotExpr(int pos, ENode expr) {
		this.pos = pos;
		this.expr = expr;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.BooleanNot);
		this.symbol = cm.getSymbol(op.name);
		this.expr = args[0];
	}
	
	public Operator getOp() { return Operator.BooleanNot; }

	public ENode[] getArgs() { return new ENode[]{expr}; }

	public String toString() { return getOp().toString(this); }

	public boolean	isConstantExpr() {
		if (expr.isConstantExpr()) {
			Object b1 = expr.getConstValue();
			if (b1 instanceof Boolean)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		Boolean b = (Boolean)expr.getConstValue();
		return b.booleanValue() ? Boolean.FALSE : Boolean.TRUE;
	}
}

