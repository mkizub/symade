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
import kiev.ir.java15.RArrayLengthExpr;
import kiev.be.java15.JArrayLengthExpr;
import kiev.ir.java15.RTypeClassExpr;
import kiev.be.java15.JTypeClassExpr;
import kiev.ir.java15.RTypeInfoExpr;
import kiev.be.java15.JTypeInfoExpr;
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


@node
public class Shadow extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = Shadow;
	@virtual typedef VView = VShadow;
	@virtual typedef JView = JShadow;
	@virtual typedef RView = RShadow;

	@ref public ASTNode	node;

	@nodeview
	public static final view VShadow of Shadow extends VENode {
		public ASTNode		node;
	}

	public Shadow() {}
	public Shadow(ASTNode node) {
		this.node = node;
	}
	
	public int getPriority() {
		if (node instanceof ENode)
			return ((ENode)node).getPriority();
		return 255;
	}

	public Type getType() { return node.getType(); }
	
	public String toString() {
		return "(shadow of) "+node;
	}

	public Dumper toJava(Dumper dmp) {
		return node.toJava(dmp);
	}

}

@node
public class ArrayLengthExpr extends ENode {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}
	
	@virtual typedef This  = ArrayLengthExpr;
	@virtual typedef VView = VArrayLengthExpr;
	@virtual typedef JView = JArrayLengthExpr;
	@virtual typedef RView = RArrayLengthExpr;

	@att public ENode			obj;
	@att public SymbolRef		ident;

	@nodeview
	public static final view VArrayLengthExpr of ArrayLengthExpr extends VENode {
		public ENode			obj;
		public SymbolRef		ident;
	}

	public ArrayLengthExpr() {}

	public ArrayLengthExpr(int pos, ENode obj) {
		this.pos = pos;
		this.ident = new SymbolRef(pos,nameLength);
		this.obj = obj;
	}
	public ArrayLengthExpr(int pos, ENode obj, SymbolRef length) {
		this.pos = pos;
		assert(length.name == nameLength);
		this.ident = ~length;
		this.obj = obj;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Type getType() { return Type.tpInt; }

	public String toString() {
		if( obj.getPriority() < opAccessPriority )
			return "("+obj.toString()+").length";
		else
			return obj.toString()+".length";
	}

	public Dumper toJava(Dumper dmp) {
		if( obj.getPriority() < opAccessPriority ) {
			dmp.append('(');
			obj.toJava(dmp).append(").length").space();
		} else {
			obj.toJava(dmp).append(".length").space();
		}
		return dmp;
	}
}

@node
public class TypeClassExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = TypeClassExpr;
	@virtual typedef VView = VTypeClassExpr;
	@virtual typedef JView = JTypeClassExpr;
	@virtual typedef RView = RTypeClassExpr;

	@att public TypeRef		type;

	@nodeview
	public static final view VTypeClassExpr of TypeClassExpr extends VENode {
		public TypeRef		type;
	}

	public TypeClassExpr() {}

	public TypeClassExpr(int pos, TypeRef type) {
		this.pos = pos;
		this.type = type;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Type getType() { return Type.tpClass; }

	public String toString() {
		return type.toString()+".class";
	}

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp).append(".class").space();
		return dmp;
	}
}

@node
public class TypeInfoExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = TypeInfoExpr;
	@virtual typedef VView = VTypeInfoExpr;
	@virtual typedef JView = JTypeInfoExpr;
	@virtual typedef RView = RTypeInfoExpr;

	@att public TypeRef				type;
	@att public TypeClassExpr		cl_expr;
	@att public NArr<ENode>			cl_args;

	@nodeview
	public static final view VTypeInfoExpr of TypeInfoExpr extends VENode {
		public		TypeRef				type;
		public		TypeClassExpr		cl_expr;
		public:ro	NArr<ENode>			cl_args;
	}

	public TypeInfoExpr() {}

	public TypeInfoExpr(int pos, TypeRef type) {
		this.pos = pos;
		this.type = type;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public Type getType() {
		Type t = type.getType().getErasedType();
		if (t.isUnerasable())
			return t.getStruct().typeinfo_clazz.xtype;
		return Type.tpTypeInfo;
	}

	public String toString() {
		return type+".type";
	}

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp).append(".type").space();
		return dmp;
	}
}

@node
public class AssignExpr extends LvalueExpr {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			lval;
	@dflow(in="lval")		ENode			value;
	}
	
	@virtual typedef This  = AssignExpr;
	@virtual typedef VView = VAssignExpr;
	@virtual typedef JView = JAssignExpr;
	@virtual typedef RView = RAssignExpr;

	@ref public AssignOperator	op;
	@att public ENode			lval;
	@att public ENode			value;

	@nodeview
	public static final view VAssignExpr of AssignExpr extends VLvalueExpr {
		public AssignOperator	op;
		public ENode			lval;
		public ENode			value;
	}
	
	public AssignExpr() {}

	public AssignExpr(int pos, AssignOperator op, ENode lval, ENode value) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
		this.value = value;
	}

	public Operator getOp() { return op; }

	public Type getType() { return lval.getType(); }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( lval.getPriority() < opAssignPriority )
			sb.append('(').append(lval).append(')');
		else
			sb.append(lval);
		sb.append(op.image);
		if( value.getPriority() < opAssignPriority )
			sb.append('(').append(value).append(')');
		else
			sb.append(value);
		return sb.toString();
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
		LvalDNode[] path = null;
		switch(lval) {
		case LVarExpr:
			path = new LvalDNode[]{((LVarExpr)lval).getVar()};
			break;
		case IFldExpr:
			path = ((IFldExpr)lval).getAccessPath();
			break;
		case SFldExpr:
			path = new LvalDNode[]{((SFldExpr)lval).var};
			break;
		}
		if (path != null)
			return dfs.setNodeValue(path,value);
		return dfs;
	}

	public Dumper toJava(Dumper dmp) {
		if( lval.getPriority() < opAssignPriority ) {
			dmp.append('(').append(lval).append(')');
		} else {
			dmp.append(lval);
		}
		if (op != AssignOperator.Assign2)
			dmp.space().append(op.image).space();
		else
			dmp.space().append(AssignOperator.Assign.image).space();
		if( value.getPriority() < opAssignPriority ) {
			dmp.append('(').append(value).append(')');
		} else {
			dmp.append(value);
		}
		return dmp;
	}
}


@node
public class BinaryExpr extends ENode {
	
	@dflow(out="expr2") private static class DFI {
	@dflow(in="this:in")	ENode				expr1;
	@dflow(in="expr1")		ENode				expr2;
	}
	
	@virtual typedef This  = BinaryExpr;
	@virtual typedef VView = VBinaryExpr;
	@virtual typedef JView = JBinaryExpr;
	@virtual typedef RView = RBinaryExpr;

	@ref public BinaryOperator	op;
	@att public ENode			expr1;
	@att public ENode			expr2;
	@ref public Method			func;

	@nodeview
	public static final view VBinaryExpr of BinaryExpr extends VENode {
		public BinaryOperator	op;
		public ENode			expr1;
		public ENode			expr2;
		public Method			func;

		public void mainResolveOut() {
			Type et1 = expr1.getType();
			Type et2 = expr2.getType();
			if( op == BinaryOperator.Add
				&& ( et1 ≈ Type.tpString || et2 ≈ Type.tpString ||
					(et1 instanceof CTimeType && et1.getUnboxedType() ≈ Type.tpString) ||
					(et2 instanceof CTimeType && et2.getUnboxedType() ≈ Type.tpString)
				   )
			) {
				if( expr1 instanceof StringConcatExpr ) {
					StringConcatExpr sce = (StringConcatExpr)expr1;
					if (et2 instanceof CTimeType) expr2 = et2.makeUnboxedExpr(expr2);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
					replaceWithNode(~sce);
				} else {
					StringConcatExpr sce = new StringConcatExpr(pos);
					if (et1 instanceof CTimeType) expr1 = et1.makeUnboxedExpr(expr1);
					sce.appendArg(expr1);
					if (et2 instanceof CTimeType) expr2 = et2.makeUnboxedExpr(expr2);
					sce.appendArg(expr2);
					trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
					replaceWithNode(sce);
				}
				return;
			}
			else if( ( et1.isNumber() && et2.isNumber() ) &&
				(    op==BinaryOperator.Add
				||   op==BinaryOperator.Sub
				||   op==BinaryOperator.Mul
				||   op==BinaryOperator.Div
				||   op==BinaryOperator.Mod
				)
			) {
				return;
			}
			else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
				(    op==BinaryOperator.LeftShift
				||   op==BinaryOperator.RightShift
				||   op==BinaryOperator.UnsignedRightShift
				)
			) {
				return;
			}
			else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
				(    op==BinaryOperator.BitOr
				||   op==BinaryOperator.BitXor
				||   op==BinaryOperator.BitAnd
				)
			) {
				return;
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				Type[] tps = new Type[]{null,et1,et2};
				ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
				if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
					func = opt.method;
//					ENode e;
//					if( opt.method.isStatic() )
//						replaceWithNode(new CallExpr(pos,null,opt.method,new ENode[]{~expr1,~expr2}));
//					else
//						replaceWithNode(new CallExpr(pos,~expr1,opt.method,new ENode[]{~expr2}));
					return;
				}
			}
			// Not a standard and not overloaded, try wrapped classes
			if (et1 instanceof CTimeType && et2 instanceof CTimeType) {
				expr1 = et1.makeUnboxedExpr(expr1);
				expr2 = et1.makeUnboxedExpr(expr2);
				mainResolveOut();
				return;
			}
			if (et1 instanceof CTimeType) {
				expr1 = et1.makeUnboxedExpr(expr1);
				mainResolveOut();
				return;
			}
			if (et2 instanceof CTimeType) {
				expr2 = et1.makeUnboxedExpr(expr2);
				mainResolveOut();
				return;
			}
		}
	}
	
	public BinaryExpr() {}

	public BinaryExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return op; }

	public Type getType() {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( op==BinaryOperator.BitOr || op==BinaryOperator.BitXor || op==BinaryOperator.BitAnd ) {
			if( (t1.isInteger() && t2.isInteger()) || (t1.isBoolean() && t2.isBoolean()) ) {
				if( t1 ≡ Type.tpLong || t2 ≡ Type.tpLong ) return Type.tpLong;
				if( t1.isAutoCastableTo(Type.tpBoolean) && t2.isAutoCastableTo(Type.tpBoolean) ) return Type.tpBoolean;
				return Type.tpInt;
			}
		}
		else if( op==BinaryOperator.LeftShift || op==BinaryOperator.RightShift || op==BinaryOperator.UnsignedRightShift ) {
			if( t2.isInteger() ) {
				if( t1 ≡ Type.tpLong ) return Type.tpLong;
				if( t1.isInteger() )	return Type.tpInt;
			}
		}
		else if( op==BinaryOperator.Add || op==BinaryOperator.Sub || op==BinaryOperator.Mul || op==BinaryOperator.Div || op==BinaryOperator.Mod ) {
			// Special case for '+' operator if one arg is a String
			if( op==BinaryOperator.Add && t1.equals(Type.tpString) || t2.equals(Type.tpString) ) return Type.tpString;

			if( t1.isNumber() && t2.isNumber() ) {
				if( t1 ≡ Type.tpDouble || t2 ≡ Type.tpDouble ) return Type.tpDouble;
				if( t1 ≡ Type.tpFloat  || t2 ≡ Type.tpFloat )  return Type.tpFloat;
				if( t1 ≡ Type.tpLong   || t2 ≡ Type.tpLong )   return Type.tpLong;
				return Type.tpInt;
			}
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,t1,t2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null )
				return opt.method.type.ret();
		}
		return Type.tpVoid;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( expr1.getPriority() < op.priority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(op.image);
		if( expr2.getPriority() < op.priority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
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
public class StringConcatExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]	args;
	}

	@virtual typedef This  = StringConcatExpr;
	@virtual typedef VView = VStringConcatExpr;
	@virtual typedef JView = JStringConcatExpr;
	@virtual typedef RView = RStringConcatExpr;

	@att public NArr<ENode>			args;

	@nodeview
	public static final view VStringConcatExpr of StringConcatExpr extends VENode {
		public:ro	NArr<ENode>		args;
	}
	
	public StringConcatExpr() {}

	public StringConcatExpr(int pos) {
		this.pos = pos;
	}

	public Operator getOp() { return BinaryOperator.Add; }

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

	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append('+');
		}
		return dmp;
	}

	public Object doRewrite(RewriteContext ctx) {
		StringBuffer sb = new StringBuffer();
		foreach (ENode e; args) {
			if (e instanceof ConstCharExpr)
				sb.append(e.value);
			else if (e instanceof ConstStringExpr)
				sb.append(e.value);
			else
				sb.append(e);
		}
		return new ConstStringExpr(sb.toString());
	}
}

@node
public class CommaExpr extends ENode {
	
	@dflow(out="exprs") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]	exprs;
	}

	@virtual typedef This  = CommaExpr;
	@virtual typedef VView = VCommaExpr;
	@virtual typedef JView = JCommaExpr;
	@virtual typedef RView = RCommaExpr;

	@att public NArr<ENode>		exprs;

	@nodeview
	public static final view VCommaExpr of CommaExpr extends VENode {
		public:ro	NArr<ENode>		exprs;
	}
	
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

	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < exprs.length; i++) {
			exprs[i].toJava(dmp);
			if( i < exprs.length-1 )
				dmp.append(',');
		}
		return dmp;
	}
}

@node
public class Block extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		stats;
	}

	@virtual typedef This  = Block;
	@virtual typedef VView = VBlock;
	@virtual typedef JView = JBlock;
	@virtual typedef RView = RBlock;

	@att public NArr<ASTNode>		stats;
	@ref public CodeLabel			break_label;

	@nodeview
	public static view VBlock of Block extends VENode {
		public:ro	NArr<ASTNode>		stats;
	}
	
	public Block() {}

	public Block(int pos) {
		this.pos = pos;
	}

	public Block(int pos, ASTNode[] sts) {
		this.pos = pos;
		this.stats.addAll(sts);
	}

	public void addSymbol(DNode sym) {
		foreach(DNode n; stats; n.getName() != null) {
			if (n.getName().equals(sym.getName()) ) {
				Kiev.reportError((ASTNode)sym,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.append((ASTNode)sym);
	}

	public void insertSymbol(DNode sym, int idx) {
		foreach(DNode n; stats; n.getName() != null) {
			if (n.getName().equals(sym.getName()) ) {
				Kiev.reportError((ASTNode)sym,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(idx,(ASTNode)sym);
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info, String name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this.stats.getArray(), info.space_prev),
		{
			n instanceof Var,
			((Var)n).id.equals(name),
			node ?= ((Var)n)
		;	n instanceof Struct,
			((Struct)n).id.equals(name),
			node ?= ((Struct)n)
		;	n instanceof TypeDecl,
			((TypeDecl)n).getName().equals(name),
			node ?= ((TypeDecl)n)
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats.getArray(), info.space_prev),
		n instanceof Var && ((Var)n).isForward(),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, String name, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats.getArray(), info.space_prev),
		n instanceof Var && ((Var)n).isForward(),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		((Var)n).getType().resolveCallAccessR(node,info,name,mt)
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
			foreach (Var n; node.stats) vars.append(n);
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

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('{').newLine(1);
		foreach (ENode s; stats)
			s.toJava(dmp);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}

	public Object doRewrite(RewriteContext ctx) {
		ASTNode res = null;
		foreach (ASTNode stat; stats)
			res = stat.doRewrite(ctx);
		return res;
	}
}

@node
public class UnaryExpr extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(out="this:in")			ENode		expr;
	}

	@virtual typedef This  = UnaryExpr;
	@virtual typedef VView = VUnaryExpr;
	@virtual typedef JView = JUnaryExpr;
	@virtual typedef RView = RUnaryExpr;

	@ref public Operator		op;
	@att public ENode			expr;

	@nodeview
	public static view VUnaryExpr of UnaryExpr extends VENode {
		public Operator			op;
		public ENode			expr;
	}
	
	public UnaryExpr() {}

	public UnaryExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
		this.expr = expr;
	}

	public Operator getOp() { return op; }

	public Type getType() {
		return expr.getType();
	}

	public String toString() {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr )
			if( expr.getPriority() < op.priority )
				return "("+expr.toString()+")"+op.image;
			else
				return expr.toString()+op.image;
		else
			if( expr.getPriority() < op.priority )
				return op.image+"("+expr.toString()+")";
			else
				return op.image+expr.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr ) {
			if( expr.getPriority() < op.priority ) {
				dmp.append('(').append(expr).append(')');
			} else {
				dmp.append(expr);
			}
			dmp.append(op.image);
		} else {
			dmp.append(op.image);
			if( expr.getPriority() < op.priority ) {
				dmp.append('(').append(expr).append(')');
			} else {
				dmp.append(expr);
			}
		}
		return dmp;
	}
}

@node
public class IncrementExpr extends ENode {
	
	@dflow(out="lval") private static class DFI {
	@dflow(in="this:in")	ENode			lval;
	}

	@virtual typedef This  = IncrementExpr;
	@virtual typedef VView = VIncrementExpr;
	@virtual typedef JView = JIncrementExpr;
	@virtual typedef RView = RIncrementExpr;

	@ref public Operator			op;
	@att public ENode				lval;

	@nodeview
	public static final view VIncrementExpr of IncrementExpr extends VENode {
		public Operator		op;
		public ENode		lval;
	}
	
	public IncrementExpr() {}

	public IncrementExpr(int pos, Operator op, ENode lval) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
	}

	public Operator getOp() { return op; }

	public Type getType() {
		return lval.getType();
	}

	public String toString() {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr )
			return lval.toString()+op.image;
		else
			return op.image+lval.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr ) {
			if( lval.getPriority() < op.priority ) {
				dmp.append('(').append(lval).append(')');
			} else {
				dmp.append(lval);
			}
			dmp.append(op.image);
		} else {
			dmp.append(op.image);
			if( lval.getPriority() < op.priority ) {
				dmp.append('(').append(lval).append(')');
			} else {
				dmp.append(lval);
			}
		}
		return dmp;
	}
}

@node
public class ConditionalExpr extends ENode {
	
	@dflow(out="join expr1 expr2") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		expr1;
	@dflow(in="cond:false")	ENode		expr2;
	}

	@virtual typedef This  = ConditionalExpr;
	@virtual typedef VView = VConditionalExpr;
	@virtual typedef JView = JConditionalExpr;
	@virtual typedef RView = RConditionalExpr;

	@att public ENode			cond;
	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VConditionalExpr of ConditionalExpr extends VENode {
		public ENode		cond;
		public ENode		expr1;
		public ENode		expr2;
	}
	
	public ConditionalExpr() {}

	public ConditionalExpr(int pos, ENode cond, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.cond = cond;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public Operator getOp() { return MultiOperator.Conditional; }

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

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(').append(cond).append(") ? ");
		sb.append('(').append(expr1).append(") : ");
		sb.append('(').append(expr2).append(") ");
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("((");
		dmp.append(cond).append(") ? (");
		dmp.append(expr1).append(") : (");
		dmp.append(expr2).append("))");
		return dmp;
	}
}

@node
public class CastExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = CastExpr;
	@virtual typedef VView = VCastExpr;
	@virtual typedef JView = JCastExpr;
	@virtual typedef RView = RCastExpr;

	@att public ENode		expr;
	@att public TypeRef		type;

	@nodeview
	public static final view VCastExpr of CastExpr extends VENode {
		public ENode	expr;
		public TypeRef	type;
	}
	
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

	public int getPriority() { return opCastPriority; }

	public Type getType() {
		return type.getType();
	}

	public String toString() {
		return "(("+type+")"+expr+")";
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("((").append(type).append(")(");
		dmp.append(expr).append("))");
		return dmp;
	}
}

