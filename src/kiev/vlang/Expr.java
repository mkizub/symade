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

	public Operator getOp() { return Operator.Access; }

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
	@att public ENode[]				cl_args;

	@nodeview
	public static final view VTypeInfoExpr of TypeInfoExpr extends VENode {
		public		TypeRef				type;
		public		TypeClassExpr		cl_expr;
		public:ro	ENode[]				cl_args;
	}

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

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp).append(".type").space();
		return dmp;
	}
}

@node
public class AssignExpr extends ENode {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			lval;
	@dflow(in="lval")		ENode			value;
	}
	
	@virtual typedef This  = AssignExpr;
	@virtual typedef VView = VAssignExpr;
	@virtual typedef JView = JAssignExpr;
	@virtual typedef RView = RAssignExpr;

	@ref public Operator		op;
	@att public ENode			lval;
	@att public ENode			value;

	@nodeview
	public static final view VAssignExpr of AssignExpr extends VENode {
		public Operator			op;
		public ENode			lval;
		public ENode			value;

		public void mainResolveOut() {
			Type et1 = lval.getType();
			Type et2 = value.getType();
			// Find out overloaded operator
			if (op == Operator.Assign && lval instanceof ContainerAccessExpr) {
				ContainerAccessExpr cae = (ContainerAccessExpr)lval;
				Type ect1 = cae.obj.getType();
				Type ect2 = cae.index.getType();
				Method@ m;
				ResInfo info = new ResInfo(this,ResInfo.noStatic | ResInfo.noImports);
				CallType mt = new CallType(null,null,new Type[]{ect2,et2},et2,false);
				if (PassInfo.resolveBestMethodR(ect1,m,info,nameArraySetOp,mt)) {
					Method rm = (Method)m;
					if !(rm.isMacro() && rm.isNative()) {
						ENode res = info.buildCall((ASTNode)this, cae.obj, m, info.mt, new ENode[]{~cae.index,~value});
						res = res.closeBuild();
						this.replaceWithNodeReWalk(res);
					}
				}
			} else {
				Method m = op.resolveMethod(this);
				if (m == null) {
					Kiev.reportWarning(this, "Unresolved method for operator "+op);
				} else {
					if (ident == null)
						ident = new SymbolRef(pos, op.name);
					if (m instanceof CoreMethod && m.core_func != null) {
						m.normilizeExpr(this);
						return;
					} else {
						ident.symbol = m;
					}
				}
			}
		}
	}
	
	public AssignExpr() {}

	public AssignExpr(int pos, Operator op, ENode lval, ENode value) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
		this.value = value;
	}

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		this.op = op;
		this.ident = new SymbolRef(op.name, cm);
		this.lval = args[0];
		this.value = args[1];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{lval,value}; }

	public Type getType() { return lval.getType(); }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	@ref public Operator		op;
	@att public ENode			expr1;
	@att public ENode			expr2;

	@nodeview
	public static final view VBinaryExpr of BinaryExpr extends VENode {
		public Operator			op;
		public ENode			expr1;
		public ENode			expr2;

		public void mainResolveOut() {
			Method m = op.resolveMethod(this);
			if (m == null) {
				Kiev.reportError(this, "Unresolved method for operator "+op);
				return;
			}
			if (ident == null)
				ident = new SymbolRef(pos, op.name);
			if (m instanceof CoreMethod && m.core_func != null) {
				m.normilizeExpr(this);
				return;
			} else {
				ident.symbol = m;
			}
		}
	}
	
	public BinaryExpr() {}

	public BinaryExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		this.pos = pos;
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public BinaryExpr(CoreMethod cm, Operator op, ENode[] args) {
		this.ident = new SymbolRef(op.name,cm);
		this.op = op;
		this.expr1 = args[0];
		this.expr2 = args[1];
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

	public Type getType() {
		Method m;
		if (ident != null && ident.symbol != null) {
			m = (Method)ident.symbol;
		} else {
			m = op.resolveMethod(this);
			if (m == null)
				return Type.tpVoid;
			if (ident == null) ident = new SymbolRef(pos, op.name);
			ident.symbol = m;
		}
		Type ret = m.type.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(getArgs()).ret();
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

		public void mainResolveOut() {
			Method m = op.resolveMethod(this);
			if (m == null) {
				Kiev.reportError(this, "Unresolved method for operator "+op);
				return;
			}
			if (ident == null)
				ident = new SymbolRef(pos, op.name);
			if (m instanceof CoreMethod && m.core_func != null) {
				m.normilizeExpr(this);
				return;
			} else {
				ident.symbol = m;
			}
		}
	}
	
	public UnaryExpr() {}

	public UnaryExpr(int pos, Operator op, ENode expr) {
		this.pos = pos;
		this.op = op;
		this.expr = expr;
	}

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		this.op = (Operator)op;
		this.ident = new SymbolRef(op.name, cm);
		this.expr = args[0];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{expr}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

	public Type getType() {
		Method m;
		if (ident != null && ident.symbol != null) {
			m = (Method)ident.symbol;
		} else {
			m = op.resolveMethod(this);
			if (m == null)
				return Type.tpVoid;
			if (ident == null) ident = new SymbolRef(pos, op.name);
			ident.symbol = m;
		}
		Type ret = m.type.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return m.makeType(getArgs()).ret();
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

	@att public ENode[]				args;

	@nodeview
	public static final view VStringConcatExpr of StringConcatExpr extends VENode {
		public:ro	ENode[]			args;
	}
	
	public StringConcatExpr() {}

	public StringConcatExpr(int pos) {
		this.pos = pos;
	}

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		assert (op == Operator.Add);
		this.ident = new SymbolRef(op.name, cm);
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
			Object o = e.doRewrite(ctx);
			if (o instanceof ConstCharExpr)
				sb.append(o.value);
			else if (o instanceof ConstStringExpr)
				sb.append(o.value);
			else
				sb.append(o);
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

	@att public ENode[]			exprs;

	@nodeview
	public static final view VCommaExpr of CommaExpr extends VENode {
		public:ro	ENode[]			exprs;
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

	@att public ASTNode[]			stats;
	@ref public CodeLabel			break_label;

	@nodeview
	public static view VBlock of Block extends VENode {
		public:ro	ASTNode[]		stats;
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
		n @= new SymbolIterator(this.stats, info.space_prev),
		n.hasName(name),
		node ?= n
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof Var && ((Var)n).isForward(),
		info.enterForward((Var)n) : info.leaveForward((Var)n),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, String name, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
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
		Object res = null;
		foreach (ASTNode stat; stats)
			res = stat.doRewrite(ctx);
		return res;
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

		public void mainResolveOut() {
			Method m = op.resolveMethod(this);
			if (m == null)
				Kiev.reportWarning(this, "Unresolved method for operator "+op);
		}
	}
	
	public IncrementExpr() {}

	public IncrementExpr(int pos, Operator op, ENode lval) {
		this.pos = pos;
		this.op = op;
		this.lval = lval;
	}

	public void initFrom(ENode node, Operator op, CoreMethod cm, ENode[] args) {
		this.pos = node.pos;
		this.op = (Operator)op;
		this.ident = new SymbolRef(op.name, cm);
		this.lval = args[0];
	}
	
	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{lval}; }

	public Type getType() {
		return lval.getType();
	}

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

	public Operator getOp() { return Operator.Conditional; }

	public ENode[] getArgs() { return new ENode[]{cond, expr1, expr2}; }

	public String toString() { return getOp().toString(this); }

	public Dumper toJava(Dumper dmp) { return getOp().toJava(dmp, this); }

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

@node
public class CastExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode		expr;
	}

	@virtual typedef This  = CastExpr;
	@virtual typedef VView = VCastExpr;
	@virtual typedef JView = JCastExpr;
	@virtual typedef RView = RCastExpr;

	@att public TypeRef		type;
	@att public ENode		expr;

	@nodeview
	public static final view VCastExpr of CastExpr extends VENode {
		public TypeRef	type;
		public ENode	expr;
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

	public Dumper toJava(Dumper dmp) {
		dmp.append("((").append(type).append(")(");
		dmp.append(expr).append("))");
		return dmp;
	}
}

