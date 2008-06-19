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

import kiev.be.java15.CodeLabel;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */


@ThisIsANode(name="Shadow", lang=CoreLang)
public class Shadow extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@nodeData public ASTNode	rnode;

	public Shadow() {}
	public Shadow(ASTNode node) {
		this.rnode = node;
	}
	
	public int getPriority() {
		if (rnode instanceof ENode)
			return ((ENode)rnode).getPriority();
		return 255;
	}

	public Type getType() { return rnode.getType(); }
	
	public String toString() {
		return "(shadow of) "+rnode;
	}
}

@ThisIsANode(lang=CoreLang)
public class TypeClassExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@nodeAttr public TypeRef		ttype;

	public TypeClassExpr() {}

	public TypeClassExpr(int pos, TypeRef ttype) {
		this.pos = pos;
		this.ttype = ttype;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		if (this.ttype == null || StdTypes.tpClass.getArgsLength() == 0)
			return StdTypes.tpClass;
		return Type.tpClass.make(new TVarBld(StdTypes.tpClass.getArg(0), this.ttype.getType()));
	}

	public String toString() {
		return ttype.toString()+".class";
	}
}

@ThisIsANode(lang=CoreLang)
public class TypeInfoExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	@nodeAttr public TypeRef				ttype;
	@nodeAttr public ENode					cl_expr;
	@nodeAttr public ENode∅				cl_args;

	public TypeInfoExpr() {}

	public TypeInfoExpr(int pos, TypeRef ttype) {
		this.pos = pos;
		this.ttype = ttype;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		Type t = ttype.getType().getErasedType();
		if (t.isUnerasable())
			return t.getStruct().typeinfo_clazz.xtype;
		return Type.tpTypeInfo;
	}

	public String toString() {
		return ttype+".type";
	}
}

@ThisIsANode(name="AssertEnabled", lang=CoreLang)
public class AssertEnabledExpr extends ENode {
	
	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	public AssertEnabledExpr() {}

	public Type getType() {
		return Type.tpBoolean;
	}

	public boolean	isConstantExpr() { return !Kiev.debugOutputA; }
	public Object	getConstValue() { return Boolean.FALSE; }

	public String toString() {
		return "$assertionsEnabled";
	}
}

@ThisIsANode(name="Set", lang=CoreLang)
public class AssignExpr extends ENode {
	
	@DataFlowDefinition(out="this:out()") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			lval;
	@DataFlowDefinition(in="lval")		ENode			value;
	}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public Operator		op;
	@nodeAttr public ENode			lval;
	@nodeAttr public ENode			value;

	public AssignExpr() {}

	public AssignExpr(int pos, Operator op, ENode lval, ENode value) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
		this.value = value;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		this.op = op;
		this.symbol = cm.getSymbol(op.name);
		this.lval = args[0];
		this.value = args[1];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{lval,value}; }

	public Type getType() { return lval.getType(); }

	public String toString() { return getOp().toString(this); }

	public void mainResolveOut() {
		Type et1 = lval.getType();
		Type et2 = value.getType();
		// Find out overloaded operator
		if (op == Operator.Assign && lval instanceof ContainerAccessExpr) {
			ContainerAccessExpr cae = (ContainerAccessExpr)lval;
			Type ect1 = cae.obj.getType();
			Type ect2 = cae.index.getType();
			Method@ m;
			ResInfo info = new ResInfo(this,nameArraySetOp,ResInfo.noStatic | ResInfo.noImports);
			CallType mt = new CallType(null,null,new Type[]{ect2,et2},et2,false);
			if (PassInfo.resolveBestMethodR(ect1,m,info,mt)) {
				Method rm = (Method)m;
				if !(rm.isMacro() && rm.isNative()) {
					ENode res = info.buildCall((ASTNode)this, cae.obj, m, null, new ENode[]{~cae.index,~value});
					res = res.closeBuild();
					this.replaceWithNodeReWalk(res);
				}
			}
		} else {
			Method m;
			if (this.dnode == null) {
				m = getOp().resolveMethod(this);
				if (m == null) {
					if (ctx_method == null || !ctx_method.isMacro())
						Kiev.reportError(this, "Unresolved method for operator "+getOp());
					return;
				}
			} else {
				m = (Method)this.dnode;
			}
			m.normilizeExpr(this);
		}
	}

	static class AssignExprDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		AssignExprDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("value"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = ((AssignExpr)dfi.node_impl).addNodeTypeInfo(f.calc(dfi));
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new AssignExprDFFunc(dfi);
	}
	
	DFState addNodeTypeInfo(DFState dfs) {
		if (value instanceof TypeRef)
			return dfs;
		Var[] path = null;
		switch(lval) {
		case LVarExpr:
			path = new Var[]{((LVarExpr)lval).getVar()};
			break;
		case IFldExpr:
			path = ((IFldExpr)lval).getAccessPath();
			break;
		case SFldExpr:
			path = new Var[]{((SFldExpr)lval).var};
			break;
		}
		if (path != null)
			return dfs.setNodeValue(path,value);
		return dfs;
	}

}


@ThisIsANode(name="BinOp", lang=CoreLang)
public class BinaryExpr extends ENode {
	
	@DataFlowDefinition(out="expr2") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode				expr1;
	@DataFlowDefinition(in="expr1")		ENode				expr2;
	}
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public Operator		op;
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public BinaryExpr() {}

	public BinaryExpr(int pos, Operator op, ENode expr1, ENode expr2) {
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
	public void setOp(Operator op) {
		this.symbol = null;
		this.op = op;
	}

	public ENode[] getArgs() { return new ENode[]{expr1,expr2}; }

	public String toString() { return getOp().toString(this); }

	public Type getType() {
		Method m;
		if (this.dnode != null) {
			m = (Method)this.dnode;
		} else {
			m = op.resolveMethod(this);
			if (m == null)
				return Type.tpVoid;
			this.symbol = m;
		}
		Type ret = m.mtype.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(null,getArgs()).ret();
	}

	public void mainResolveOut() {
		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (ctx_method == null || !ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
	}

	public boolean	isConstantExpr() {
		if (!expr1.isConstantExpr())
			return false;
		if (!expr2.isConstantExpr())
			return false;
		DNode m = this.dnode;
		if (m == null)
			m = getOp().resolveMethod(this);
		if (!(m instanceof Method) || !(m.body instanceof CoreExpr))
			return false;
		return true;
	}
	public Object	getConstValue() {
		Method m = (Method)this.dnode;
		if (m == null)
			m = getOp().resolveMethod(this);
		ConstExpr ce = ((CoreExpr)m.body).calc(this);
		return ce.getConstValue();
	}
}

@ThisIsANode(name="UnaryOp", lang=CoreLang)
public class UnaryExpr extends ENode {
	
	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(out="this:in")			ENode		expr;
	}

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public Operator		op;
	@nodeAttr public ENode			expr;

	public UnaryExpr() {}

	public UnaryExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
		this.expr = expr;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		this.op = (Operator)op;
		this.symbol = cm.getSymbol(op.name);
		this.expr = args[0];
	}
	
	public Operator getOp() { return op; }
	public void setOp(Operator op) {
		this.symbol = null;
		this.op = op;
	}

	public ENode[] getArgs() { return new ENode[]{expr}; }

	public String toString() { return getOp().toString(this); }

	public Type getType() {
		Method m;
		if (this.dnode != null) {
			m = (Method)this.dnode;
		} else {
			m = op.resolveMethod(this);
			if (m == null)
				return Type.tpVoid;
			this.symbol = m;
		}
		Type ret = m.mtype.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(null,getArgs()).ret();
	}

	public void mainResolveOut() {
		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (ctx_method == null || !ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
		// Check if expression is a constant
		if (m.body instanceof CoreExpr && expr.isConstantExpr()) {
			ConstExpr ce = ((CoreExpr)m.body).calc(this);
			replaceWithNodeReWalk(ce);
			return;
		}
	}
	public boolean	isConstantExpr() {
		if (!expr.isConstantExpr())
			return false;
		DNode m = this.dnode;
		if (m == null)
			m = getOp().resolveMethod(this);
		if (!(m instanceof Method) || !(m.body instanceof CoreExpr))
			return false;
		return true;
	}
	public Object	getConstValue() {
		Method m = (Method)this.dnode;
		if (m == null)
			m = getOp().resolveMethod(this);
		ConstExpr ce = ((CoreExpr)m.body).calc(this);
		return ce.getConstValue();
	}
}

@ThisIsANode(name="StrConcat", lang=CoreLang)
public class StringConcatExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]	args;
	}

	@nodeAttr public ENode∅				args;

	public StringConcatExpr() {}

	public StringConcatExpr(int pos) {
		this.pos = pos;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.Add);
		this.symbol = cm.getSymbol(op.name);
		ENode arg1 = args[0];
		ENode arg2 = args[1];
		if (arg1 instanceof StringConcatExpr)
			this.args.addAll(arg1.args.delToArray());
		else
			this.args.add(arg1);
		if (arg2 instanceof StringConcatExpr)
			this.args.addAll(arg2.args.delToArray());
		else
			this.args.add(arg2);
	}
	
	public Operator getOp() { return Operator.Add; }

	public Type getType() { return Type.tpString; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			if( i < args.length-1 )
				sb.append('+');
		}
		return sb.toString();
	}

	public void appendArg(ENode expr) {
		args.append(~expr);
	}
}

@ThisIsANode(name="Comma", lang=CoreLang)
public class CommaExpr extends ENode {
	
	@DataFlowDefinition(out="exprs") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]	exprs;
	}

	@nodeAttr public ENode∅			exprs;

	public CommaExpr() {}

	public CommaExpr(ENode expr) {
		this.pos = pos;
		this.exprs.add(expr);
	}

	public int getPriority() { return 0; }

	public Type getType() { return exprs[exprs.length-1].getType(); }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < exprs.length; i++) {
			sb.append(exprs[i]);
			if( i < exprs.length-1 )
				sb.append(',');
		}
		return sb.toString();
	}
}

@ThisIsANode(name="Block", lang=CoreLang)
public class Block extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@DataFlowDefinition(out="this:out()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]		stats;
	}

	@nodeAttr public ASTNode∅			stats;
	@nodeAttr(copyable=false, ext_data=true)
	          public Label				lblbrk;

	public Block() {}

	public Block(int pos) {
		this.pos = pos;
	}

	public Block(int pos, ASTNode[] sts) {
		this.pos = pos;
		this.stats.addAll(sts);
	}

	public void addSymbol(DNode sym) {
		foreach(ASTNode n; stats; n.hasName(sym.sname))
			Kiev.reportError((ASTNode)sym,"Symbol "+sym.sname+" already declared in this scope");
		stats.append((ASTNode)sym);
	}

	public void insertSymbol(DNode sym, int idx) {
		foreach(ASTNode n; stats; n.hasName(sym.sname))
			Kiev.reportError((ASTNode)sym,"Symbol "+sym.sname+" already declared in this scope");
		stats.insert(idx,(ASTNode)sym);
	}

	public boolean backendCleanup() {
		this.lblbrk = null;
		return true;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
		DNode@ dn;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof CaseLabel,
			((CaseLabel)n).resolveNameR(node,info)
		;
			info.checkNodeName(n),
			node ?= n
		;
			info.isForwardsAllowed(),
			n instanceof Var && ((Var)n).isForward(),
			info.enterForward((Var)n) : info.leaveForward((Var)n),
			n.getType().resolveNameAccessR(node,info)
		}
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof CaseLabel,
			((CaseLabel)n).resolveMethodR(node, info, mt)
		;
			n instanceof Var && ((Var)n).isForward(),
			info.enterForward((Var)n) : info.leaveForward((Var)n),
			((Var)n).getType().resolveCallAccessR(node,info,mt)
		}
	}

	public int		getPriority() { return 255; }

	public Type getType() {
		if (isGenVoidExpr()) return Type.tpVoid;
		if (stats.length == 0) return Type.tpVoid;
		return stats[stats.length-1].getType();
	}

	static class BlockDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		BlockDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("stats"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Block node = (Block)dfi.node_impl;
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode dn; node.stats) {
				if (dn instanceof Var) {
					vars.append((Var)dn);
					continue;
				}
			}
			if (vars.length > 0)
				res = DFFunc.calc(f, dfi).cleanInfoForVars(vars.toArray());
			else
				res = DFFunc.calc(f, dfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new BlockDFFunc(dfi);
	}
	
	public String toString() {
		return "{...}";
	}

}

@ThisIsANode(name="IncrOp", lang=CoreLang)
public class IncrementExpr extends ENode {
	
	@DataFlowDefinition(out="lval") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			lval;
	}

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public Operator			op;
	@nodeAttr public ENode				lval;

	public IncrementExpr() {}

	public IncrementExpr(int pos, Operator op, ENode lval) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
	}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		this.pos = node.pos;
		this.op = (Operator)op;
		this.symbol = cm.getSymbol(op.name);
		this.lval = args[0];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{lval}; }

	public Type getType() {
		return lval.getType();
	}

	public String toString() { return getOp().toString(this); }

	public void mainResolveOut() {
		Method m = op.resolveMethod(this);
		if (m == null) {
			if (ctx_method == null || !ctx_method.isMacro())
				Kiev.reportWarning(this, "Unresolved method for operator "+op);
			return;
		}
	}
}

@ThisIsANode(name="IfOp", lang=CoreLang)
public class ConditionalExpr extends ENode {
	
	@DataFlowDefinition(out="join expr1 expr2") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		cond;
	@DataFlowDefinition(in="cond:true")	ENode		expr1;
	@DataFlowDefinition(in="cond:false")	ENode		expr2;
	}

	@nodeAttr public ENode			cond;
	@nodeAttr public ENode			expr1;
	@nodeAttr public ENode			expr2;

	public ConditionalExpr() {}

	public ConditionalExpr(int pos, ENode cond, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.cond = cond;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return Operator.Conditional; }

	public ENode[] getArgs() { return new ENode[]{cond, expr1, expr2}; }

	public String toString() { return getOp().toString(this); }

	public Type getType() {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( t1.isReference() && t2.isReference() ) {
			if( t1 ≡ t2 ) return t1;
			if( t1 ≡ Type.tpNull ) return t2;
			if( t2 ≡ Type.tpNull ) return t1;
			return Type.leastCommonType(t1,t2);
		}
		if( t1.isNumber() && t2.isNumber() ) {
			if( t1 ≡ t2 ) return t1;
			return CoreType.upperCastNumbers(t1,t2);
		}
		return expr1.getType();
	}
}

@ThisIsANode(name="Cast", lang=CoreLang)
public class CastExpr extends ENode {

	@DataFlowDefinition(out="expr") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode		expr;
	}

	@nodeAttr public TypeRef		ctype;
	@nodeAttr public ENode			expr;

	public CastExpr() {}

	public CastExpr(int pos, Type ctype, ENode expr) {
		this.pos = pos;
		this.ctype = new TypeRef(ctype);
		this.expr = expr;
	}

	public CastExpr(int pos, TypeRef ctype, ENode expr) {
		this.pos = pos;
		this.ctype = ctype;
		this.expr = expr;
	}

	public CastExpr(Type ctype, ENode expr) {
		this.ctype = new TypeRef(ctype);
		this.expr = expr;
	}

	public Operator getOp() { return Operator.CastForce; }

	public int getPriority() { return opCastPriority; }

	public ENode[] getArgs() { return new ENode[]{ctype, expr}; }

	public String toString() { return getOp().toString(this); }

	public Type getType() {
		return ctype.getType();
	}

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void mainResolveOut() {
		Type ctype = this.ctype.getType();
		Type extp = Type.getRealType(ctype,expr.getType());
		if (extp.getAutoCastTo(ctype) == null) {
			resolveOverloadedCast(extp);
		}
		else if (extp instanceof CTimeType && extp.getUnboxedType().getAutoCastTo(ctype) != null) {
			resolveOverloadedCast(extp);
		}
		else if (!extp.isInstanceOf(ctype) && extp.getStruct() != null && extp.getStruct().isStructView()
				&& ((KievView)extp.getStruct()).view_of.getType().getAutoCastTo(ctype) != null)
		{
			resolveOverloadedCast(extp);
		}
	}

	private boolean resolveOverloadedCast(Type et) {
		Method@ v;
		ResInfo info = new ResInfo(this,nameCastOp,ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
		CallType mt = new CallType(et,null,null,this.ctype.getType(),false);
		if( PassInfo.resolveBestMethodR(et,v,info,mt) ) {
			this.symbol = (Method)v;
			return true;
		}
		v.$unbind();
		info = new ResInfo(this,nameCastOp,ResInfo.noForwards|ResInfo.noImports);
		mt = new CallType(null,null,new Type[]{expr.getType()},this.ctype.getType(),false);
		if( PassInfo.resolveMethodR(this,v,info,mt) ) {
			this.symbol = (Method)v;
			return true;
		}
		return false;
	}

	public static void autoCast(ENode ex, TypeRef tp) {
		autoCast(ex, tp.getType());
	}
	public static void autoCast(ENode ex, Type tp) {
		assert(ex.isAttached());
		Type at = ex.getType();
		if( !at.equals(tp) ) {
			if( at.isReference() && !tp.isReference() && ((CoreType)tp).getRefTypeForPrimitive() ≈ at )
				autoCastToPrimitive(ex);
			else if( !at.isReference() && tp.isReference() && ((CoreType)at).getRefTypeForPrimitive() ≈ tp )
				autoCastToReference(ex);
			else if( at.isReference() && tp.isReference() && at.isInstanceOf(tp) )
				;
			else
				ex.replaceWith(fun ()->ENode {return new CastExpr(ex.pos,tp,~ex);});
		}
	}

	public static ENode autoCastToReference(ENode ex) {
		assert(ex.isAttached());
		Type tp = ex.getType();
		if( tp.isReference() ) return ex;
		Type ref;
		if     ( tp ≡ Type.tpBoolean )	ref = Type.tpBooleanRef;
		else if( tp ≡ Type.tpByte    )	ref = Type.tpByteRef;
		else if( tp ≡ Type.tpShort   )	ref = Type.tpShortRef;
		else if( tp ≡ Type.tpInt     )	ref = Type.tpIntRef;
		else if( tp ≡ Type.tpLong    )	ref = Type.tpLongRef;
		else if( tp ≡ Type.tpFloat   )	ref = Type.tpFloatRef;
		else if( tp ≡ Type.tpDouble  )	ref = Type.tpDoubleRef;
		else if( tp ≡ Type.tpChar    )	ref = Type.tpCharRef;
		else
			throw new RuntimeException("Unknown primitive type "+tp);
		return (ENode)ex.replaceWith(fun ()->ENode {return new NewExpr(ex.pos,ref,new ENode[]{~ex});});
	}

	public static ENode autoCastToPrimitive(ENode ex) {
		assert(ex.isAttached());
		Type tp = ex.getType();
		if( !tp.isReference() ) return ex;
		if( tp ≈ Type.tpBooleanRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpBooleanRef.tdecl.resolveMethod("booleanValue",Type.tpBoolean),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpByteRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpByteRef.tdecl.resolveMethod("byteValue",Type.tpByte),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpShortRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpShortRef.tdecl.resolveMethod("shortValue",Type.tpShort),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpIntRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpIntRef.tdecl.resolveMethod("intValue",Type.tpInt),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpLongRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpLongRef.tdecl.resolveMethod("longValue",Type.tpLong),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpFloatRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpFloatRef.tdecl.resolveMethod("floatValue",Type.tpFloat),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpDoubleRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpDoubleRef.tdecl.resolveMethod("doubleValue",Type.tpDouble),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpCharRef )
			return (ENode)ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpCharRef.tdecl.resolveMethod("charValue",Type.tpChar),ENode.emptyArray
			);});
		else
			throw new RuntimeException("Type "+tp+" is not a reflection of primitive type");
	}
}

