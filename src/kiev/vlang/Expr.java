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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.be.java15.JLvalueExpr;
import kiev.ir.java15.RShadow;
import kiev.be.java15.JShadow;
import kiev.ir.java15.RTypeClassExpr;
import kiev.be.java15.JTypeClassExpr;
import kiev.ir.java15.RTypeInfoExpr;
import kiev.be.java15.JTypeInfoExpr;
import kiev.ir.java15.RAssertEnabledExpr;
import kiev.ir.java15.RAssignExpr;
import kiev.be.java15.JAssignExpr;
import kiev.ir.java15.RBinaryExpr;
import kiev.be.java15.JBinaryExpr;
import kiev.ir.java15.RStringConcatExpr;
import kiev.be.java15.JStringConcatExpr;
import kiev.ir.java15.RCommaExpr;
import kiev.be.java15.JCommaExpr;
import kiev.ir.java15.RBlock;
import kiev.be.java15.JBlock;
import kiev.ir.java15.RUnaryExpr;
import kiev.be.java15.JUnaryExpr;
import kiev.ir.java15.RIncrementExpr;
import kiev.be.java15.JIncrementExpr;
import kiev.ir.java15.RConditionalExpr;
import kiev.be.java15.JConditionalExpr;
import kiev.ir.java15.RCastExpr;
import kiev.be.java15.JCastExpr;

import kiev.be.java15.CodeLabel;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */


@node(name="Shadow")
public class Shadow extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = Shadow;
	@virtual typedef JView = JShadow;
	@virtual typedef RView = RShadow;

	@ref public ASTNode	rnode;

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

@node
public class TypeClassExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = TypeClassExpr;
	@virtual typedef JView = JTypeClassExpr;
	@virtual typedef RView = RTypeClassExpr;

	@att public TypeRef		type;

	public TypeClassExpr() {}

	public TypeClassExpr(int pos, TypeRef type) {
		this.pos = pos;
		this.type = type;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() { return Type.tpClass; }

	public String toString() {
		return type.toString()+".class";
	}
}

@node
public class TypeInfoExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = TypeInfoExpr;
	@virtual typedef JView = JTypeInfoExpr;
	@virtual typedef RView = RTypeInfoExpr;

	@att public TypeRef				type;
	@att public ENode				cl_expr;
	@att public ENode[]				cl_args;

	public TypeInfoExpr() {}

	public TypeInfoExpr(int pos, TypeRef type) {
		this.pos = pos;
		this.type = type;
	}

	public Operator getOp() { return Operator.Access; }

	public Type getType() {
		Type t = type.getType().getErasedType();
		if (t.isUnerasable())
			return t.getStruct().typeinfo_clazz.xtype;
		return Type.tpTypeInfo;
	}

	public String toString() {
		return type+".type";
	}
}

@node(name="AssertEnabled")
public class AssertEnabledExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = AssertEnabledExpr;
	@virtual typedef RView = RAssertEnabledExpr;

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

@node(name="Set")
public class AssignExpr extends ENode {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			lval;
	@dflow(in="lval")		ENode			value;
	}
	
	@virtual typedef This  = AssignExpr;
	@virtual typedef JView = JAssignExpr;
	@virtual typedef RView = RAssignExpr;

	@att public Operator		op;
	@att public ENode			lval;
	@att public ENode			value;

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
					if (!ctx_method.isMacro())
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


@node(name="BinOp")
public class BinaryExpr extends ENode {
	
	@dflow(out="expr2") private static class DFI {
	@dflow(in="this:in")	ENode				expr1;
	@dflow(in="expr1")		ENode				expr2;
	}
	
	@virtual typedef This  = BinaryExpr;
	@virtual typedef JView = JBinaryExpr;
	@virtual typedef RView = RBinaryExpr;

	@att public Operator		op;
	@att public ENode			expr1;
	@att public ENode			expr2;

	public BinaryExpr() {}

	public BinaryExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public BinaryExpr(CoreMethod cm, Operator op, ENode[] args) {
		this.ident = new SymbolRef<Method>(cm.getSymbol(op.name));
		this.op = op;
		this.expr1 = args[0];
		this.expr2 = args[1];
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

	public Type getType() {
		Method m;
		if (this.dnode != null) {
			m = (Method)this.dnode;
		} else {
			m = op.resolveMethod(this);
			if (m == null)
				return Type.tpVoid;
			this.open();
			this.symbol = m.id;
		}
		Type ret = m.type.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(null,getArgs()).ret();
	}

	public void mainResolveOut() {
		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (!ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
	}
}

@node(name="UnaryOp")
public class UnaryExpr extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(out="this:in")			ENode		expr;
	}

	@virtual typedef This  = UnaryExpr;
	@virtual typedef JView = JUnaryExpr;
	@virtual typedef RView = RUnaryExpr;

	@att public Operator		op;
	@att public ENode			expr;

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
			this.open();
			this.symbol = m.id;
		}
		Type ret = m.type.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(null,getArgs()).ret();
	}

	public void mainResolveOut() {
		Method m;
		if (this.dnode == null) {
			m = getOp().resolveMethod(this);
			if (m == null) {
				if (!ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return;
			}
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
		// Check if expression is a constant
		if (m instanceof CoreMethod && expr.isConstantExpr()) {
			ConstExpr ce = ((CoreMethod)m).calc(this);
			replaceWithNodeReWalk(ce);
			return;
		}
	}
}

@node(name="StrConcat")
public class StringConcatExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]	args;
	}

	@virtual typedef This  = StringConcatExpr;
	@virtual typedef JView = JStringConcatExpr;
	@virtual typedef RView = RStringConcatExpr;

	@att public ENode[]				args;

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

@node(name="Comma")
public class CommaExpr extends ENode {
	
	@dflow(out="exprs") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]	exprs;
	}

	@virtual typedef This  = CommaExpr;
	@virtual typedef JView = JCommaExpr;
	@virtual typedef RView = RCommaExpr;

	@att public ENode[]			exprs;

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

@node(name="Block")
public class Block extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		stats;
	}

	@virtual typedef This  = Block;
	@virtual typedef JView = JBlock;
	@virtual typedef RView = RBlock;

	@att public ASTNode[]			stats;
	     public CodeLabel			break_label;

	public Block() {}

	public Block(int pos) {
		this.pos = pos;
	}

	public Block(int pos, ASTNode[] sts) {
		this.pos = pos;
		this.stats.addAll(sts);
	}

	public void addSymbol(DNode sym) {
		foreach(ASTNode n; stats; n.hasName(sym.id.sname,true))
			Kiev.reportError((ASTNode)sym,"Symbol "+sym.id+" already declared in this scope");
		stats.append((ASTNode)sym);
	}

	public void insertSymbol(DNode sym, int idx) {
		foreach(ASTNode n; stats; n.hasName(sym.id.sname,true))
			Kiev.reportError((ASTNode)sym,"Symbol "+sym.id+" already declared in this scope");
		stats.insert(idx,(ASTNode)sym);
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
		DNode@ dn;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof DeclGroup,
			dn @= ((DeclGroup)n).decls,
			info.checkNodeName(dn),
			info.check(dn),
			node ?= dn
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
			n instanceof DeclGroup,
			((DeclGroup)n).resolveMethodR(node, info, mt)
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
				if (dn instanceof DeclGroup) {
					foreach (Var v; ((DeclGroup)dn).decls)
						vars.append(v);
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

	public ANode doRewrite(RewriteContext ctx) {
		ANode res = null;
		foreach (ASTNode stat; stats)
			res = stat.doRewrite(ctx);
		return res;
	}
}

@node(name="IncrOp")
public class IncrementExpr extends ENode {
	
	@dflow(out="lval") private static class DFI {
	@dflow(in="this:in")	ENode			lval;
	}

	@virtual typedef This  = IncrementExpr;
	@virtual typedef JView = JIncrementExpr;
	@virtual typedef RView = RIncrementExpr;

	@att public Operator			op;
	@att public ENode				lval;

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
			if (!ctx_method.isMacro())
				Kiev.reportWarning(this, "Unresolved method for operator "+op);
			return;
		}
	}
}

@node(name="IfOp")
public class ConditionalExpr extends ENode {
	
	@dflow(out="join expr1 expr2") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		expr1;
	@dflow(in="cond:false")	ENode		expr2;
	}

	@virtual typedef This  = ConditionalExpr;
	@virtual typedef JView = JConditionalExpr;
	@virtual typedef RView = RConditionalExpr;

	@att public ENode			cond;
	@att public ENode			expr1;
	@att public ENode			expr2;

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

@node(name="Cast")
public class CastExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = CastExpr;
	@virtual typedef JView = JCastExpr;
	@virtual typedef RView = RCastExpr;

	@att public TypeRef		type;
	@att public ENode		expr;

	public CastExpr() {}

	public CastExpr(int pos, Type type, ENode expr) {
		this.pos = pos;
		this.type = new TypeRef(type);
		this.expr = expr;
	}

	public CastExpr(int pos, TypeRef type, ENode expr) {
		this.pos = pos;
		this.type = type;
		this.expr = expr;
	}

	public CastExpr(Type type, ENode expr) {
		this.type = new TypeRef(type);
		this.expr = expr;
	}

	public Operator getOp() { return Operator.CastForce; }

	public int getPriority() { return opCastPriority; }

	public ENode[] getArgs() { return new ENode[]{type, expr}; }

	public String toString() { return getOp().toString(this); }

	public Type getType() {
		return type.getType();
	}

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
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

	public static void autoCastToReference(ENode ex) {
		assert(ex.isAttached());
		Type tp = ex.getType();
		if( tp.isReference() ) return;
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
		ex.replaceWith(fun ()->ENode {return new NewExpr(ex.pos,ref,new ENode[]{~ex});});
	}

	public static void autoCastToPrimitive(ENode ex) {
		assert(ex.isAttached());
		Type tp = ex.getType();
		if( !tp.isReference() ) return;
		if( tp ≈ Type.tpBooleanRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpBooleanRef.clazz.resolveMethod("booleanValue",Type.tpBoolean),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpByteRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpByteRef.clazz.resolveMethod("byteValue",Type.tpByte),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpShortRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpShortRef.clazz.resolveMethod("shortValue",Type.tpShort),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpIntRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpIntRef.clazz.resolveMethod("intValue",Type.tpInt),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpLongRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpLongRef.clazz.resolveMethod("longValue",Type.tpLong),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpFloatRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpFloatRef.clazz.resolveMethod("floatValue",Type.tpFloat),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpDoubleRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpDoubleRef.clazz.resolveMethod("doubleValue",Type.tpDouble),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpCharRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,~ex,
				Type.tpCharRef.clazz.resolveMethod("charValue",Type.tpChar),ENode.emptyArray
			);});
		else
			throw new RuntimeException("Type "+tp+" is not a reflection of primitive type");
	}
}

