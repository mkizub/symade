package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JLvalueExprView;
import kiev.be.java.JShadowView;
import kiev.be.java.JArrayLengthExprView;
import kiev.be.java.JTypeClassExprView;
import kiev.be.java.JTypeInfoExprView;
import kiev.be.java.JAssignExprView;
import kiev.be.java.JBinaryExprView;
import kiev.be.java.JStringConcatExprView;
import kiev.be.java.JCommaExprView;
import kiev.be.java.JBlockExprView;
import kiev.be.java.JUnaryExprView;
import kiev.be.java.JIncrementExprView;
import kiev.be.java.JConditionalExprView;
import kiev.be.java.JCastExprView;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */


@node
public class Shadow extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@node
	public static final class ShadowImpl extends ENodeImpl {
		public ShadowImpl() {}
		@ref public ASTNode	node;
	}
	@nodeview
	public static final view ShadowView of ShadowImpl extends ENodeView {
		public ASTNode		node;
	}

	@ref public abstract virtual ASTNode node;
	
	public NodeView			getNodeView()		{ return new ShadowView((ShadowImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new ShadowView((ShadowImpl)this.$v_impl); }
	public ShadowView		getShadowView()		{ return new ShadowView((ShadowImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JShadowView((ShadowImpl)this.$v_impl); }
	public JENodeView		getJENodeView()		{ return new JShadowView((ShadowImpl)this.$v_impl); }
	public JShadowView		getJShadowView()	{ return new JShadowView((ShadowImpl)this.$v_impl); }

	@getter public ASTNode	get$node()				{ return this.getShadowView().node; }
	@setter public void		set$node(ASTNode val)	{ this.getShadowView().node = val; }
	
	public Shadow() {
		super(new ShadowImpl());
	}
	public Shadow(ENode node) {
		super(new ShadowImpl());
		this.node = node;
	}
	public Shadow(Initializer node) {
		super(new ShadowImpl());
		this.node = node;
	}
	public Type getType() { return node.getType(); }
	
	public int getPriority() {
		if (node instanceof ENode)
			return ((ENode)node).getPriority();
		return 255;
	}
	
	public void resolve(Type reqType) {
		if (node instanceof ENode)
			((ENode)node).resolve(reqType);
		else
			((Initializer)node).resolveDecl();
		setResolved(true);
	}

	public String toString() {
		return "(shadow of) "+node;
	}

	public Dumper toJava(Dumper dmp) {
		return node.toJava(dmp);
	}

}

@node
public class ArrayLengthExpr extends AccessExpr {
	
	@dflow(out="obj") private static class DFI {
	@dflow(in="this:in")	ENode			obj;
	}
	
	@node
	public static final class ArrayLengthExprImpl extends AccessExprImpl {
		public ArrayLengthExprImpl() {}
		public ArrayLengthExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view ArrayLengthExprView of ArrayLengthExprImpl extends AccessExprView {
		ArrayLengthExprView(ArrayLengthExprImpl $view) { super($view); }
	}

	public NodeView					getNodeView()				{ return new ArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new ArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }
	public ArrayLengthExprView		getArrayLengthExprView()	{ return new ArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }
	public JArrayLengthExprView		getJArrayLengthExprView()	{ return new JArrayLengthExprView((ArrayLengthExprImpl)this.$v_impl); }

	public ArrayLengthExpr() {
		super(new ArrayLengthExprImpl());
	}

	public ArrayLengthExpr(int pos, ENode obj) {
		super(new ArrayLengthExprImpl(pos));
		this.ident = new NameRef(pos,nameLength);
		this.obj = obj;
	}
	public ArrayLengthExpr(int pos, ENode obj, NameRef length) {
		super(new ArrayLengthExprImpl(pos));
		assert(length.name == nameLength);
		this.ident = new NameRef(pos,nameLength);
		this.obj = obj;
	}

	public String toString() {
		if( obj.getPriority() < opAccessPriority )
			return "("+obj.toString()+").length";
		else
			return obj.toString()+".length";
	}

	public Type getType() {
		return Type.tpInt;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public void resolve(Type reqType) {
		obj.resolve(null);
		if !(obj.getType().isArray())
			throw new CompilerException(this, "Access to array length for non-array type "+obj.getType());
		setResolved(true);
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
	
	@node
	public static final class TypeClassExprImpl extends ENodeImpl {
		public TypeClassExprImpl() {}
		public TypeClassExprImpl(int pos) { super(pos); }
		@att public TypeRef		type;
	}
	@nodeview
	public static final view TypeClassExprView of TypeClassExprImpl extends ENodeView {
		public TypeRef		type;
	}

	@att public abstract virtual TypeRef type;
	
	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new TypeClassExprView((TypeClassExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new TypeClassExprView((TypeClassExprImpl)this.$v_impl); }
	public TypeClassExprView	getTypeClassExprView()	alias operator(210,fy,$cast) { return new TypeClassExprView((TypeClassExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JTypeClassExprView((TypeClassExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JTypeClassExprView((TypeClassExprImpl)this.$v_impl); }
	public JTypeClassExprView	getJTypeClassExprView()	alias operator(210,fy,$cast) { return new JTypeClassExprView((TypeClassExprImpl)this.$v_impl); }

	@getter public TypeRef	get$type()				{ return this.getTypeClassExprView().type; }
	@setter public void		set$type(TypeRef val)	{ this.getTypeClassExprView().type = val; }
	
	public TypeClassExpr() {
		super(new TypeClassExprImpl());
	}

	public TypeClassExpr(int pos, TypeRef type) {
		super(new TypeClassExprImpl(pos));
		this.type = type;
	}

	public String toString() {
		return type.toString()+".class";
	}

	public Type getType() {
		return Type.tpClass;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public void resolve(Type reqType) {
		Type tp = type.getType();
		if (!tp.isReference()) {
			Type rt = ((CoreType)tp).getRefTypeForPrimitive();
			Field f = rt.clazz.resolveField(KString.from("TYPE"));
			replaceWithNodeResolve(reqType,new SFldExpr(pos,f));
			return;
		}
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		type.toJava(dmp).append(".class").space();
		return dmp;
	}
}

@node
public class TypeInfoExpr extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@node
	public static final class TypeInfoExprImpl extends ENodeImpl {
		public TypeInfoExprImpl() {}
		public TypeInfoExprImpl(int pos) { super(pos); }
		@att public TypeRef				type;
		@att public TypeClassExpr		cl_expr;
		@att public NArr<ENode>			cl_args;
	}
	@nodeview
	public static final view TypeInfoExprView of TypeInfoExprImpl extends ENodeView {
		public				TypeRef				type;
		public				TypeClassExpr		cl_expr;
		public access:ro	NArr<ENode>			cl_args;
	}

	@ref public abstract virtual			TypeRef				type;
	@att public abstract virtual			TypeClassExpr		cl_expr;
	@att public abstract virtual access:ro	NArr<ENode>			cl_args;
	
	public NodeView				getNodeView()			alias operator(210,fy,$cast) { return new TypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			alias operator(210,fy,$cast) { return new TypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }
	public TypeInfoExprView		getTypeInfoExprView()	alias operator(210,fy,$cast) { return new TypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			alias operator(210,fy,$cast) { return new JTypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			alias operator(210,fy,$cast) { return new JTypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }
	public JTypeInfoExprView	getJTypeInfoExprView()	alias operator(210,fy,$cast) { return new JTypeInfoExprView((TypeInfoExprImpl)this.$v_impl); }

	@getter public TypeRef				get$type()		{ return this.getTypeInfoExprView().type; }
	@getter public TypeClassExpr		get$cl_expr()	{ return this.getTypeInfoExprView().cl_expr; }
	@getter public NArr<ENode>			get$cl_args()	{ return this.getTypeInfoExprView().cl_args; }
	@setter public void		set$type(TypeRef val)			{ this.getTypeInfoExprView().type = val; }
	@setter public void		set$cl_expr(TypeClassExpr val)	{ this.getTypeInfoExprView().cl_expr = val; }
	
	public TypeInfoExpr() {
		super(new TypeInfoExprImpl());
	}

	public TypeInfoExpr(int pos, TypeRef type) {
		super(new TypeInfoExprImpl(pos));
		this.type = type;
	}

	public String toString() {
		return type+".type";
	}

	public Type getType() {
		Type t = type.getType().getErasedType();
		if (t.isUnerasable())
			return t.getStruct().typeinfo_clazz.concr_type;
		return Type.tpTypeInfo;
	}

	public Operator getOp() { return BinaryOperator.Access; }

	public void resolve(Type reqType) {
		if (isResolved())
			return;
		Type type = this.type.getType();
		ConcreteType ftype = Type.tpTypeInfo;
		Struct clazz = type.getStruct();
		if (clazz.isTypeUnerasable()) {
			if (clazz.typeinfo_clazz == null)
				clazz.autoGenerateTypeinfoClazz();
			ftype = clazz.typeinfo_clazz.concr_type;
		}
		cl_expr = new TypeClassExpr(pos,new TypeRef(clazz.concr_type));
		cl_expr.resolve(Type.tpClass);
		foreach (ArgType at; clazz.getTypeInfoArgs())
			cl_args.add(ctx_clazz.accessTypeInfoField(this, type.resolve(at),false));
		foreach (ENode tie; cl_args)
			tie.resolve(null);
//		CallExpr ce = new CallExpr(from.pos,null,
//			ftype.clazz.resolveMethod(KString.from("newTypeInfo"),ftype,Type.tpClass,new ArrayType(Type.tpTypeInfo)),
//			new ENode[]{new TypeClassExpr(new TypeRef(t.getErasedType()))});
//		TVar[] templ = ftype.imeta_type.templ_type.bindings().tvars;
//		foreach (TVar tv; templ; !tv.isBound() && !tv.isAlias())
//			ce.args.append(accessTypeInfoField(from,tv.var,false));
//		return ce;
//
//		Type tp = type.getType();
//		if (!tp.isReference()) {
//			Type rt = ((CoreType)tp).getRefTypeForPrimitive();
//			Field f = rt.clazz.resolveField(KString.from("TYPE"));
//			replaceWithNodeResolve(reqType,new SFldExpr(pos,f));
//			return;
//		}
		setResolved(true);
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
	
	@node
	public static class AssignExprImpl extends LvalueExprImpl {		
		@ref public AssignOperator	op;
		@att public ENode			lval;
		@att public ENode			value;

		public AssignExprImpl() {}
		public AssignExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static view AssignExprView of AssignExprImpl extends LvalueExprView {
		public AssignOperator	op;
		public ENode			lval;
		public ENode			value;
	}
	
	@att public abstract virtual AssignOperator	op;
	@att public abstract virtual ENode				lval;
	@att public abstract virtual ENode				value;
	
	
	public NodeView			getNodeView()			{ return new AssignExprView((AssignExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new AssignExprView((AssignExprImpl)this.$v_impl); }
	public LvalueExprView	getLvalueExprView()		{ return new AssignExprView((AssignExprImpl)this.$v_impl); }
	public AssignExprView	getAssignExprView()		{ return new AssignExprView((AssignExprImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			{ return new JAssignExprView((AssignExprImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			{ return new JAssignExprView((AssignExprImpl)this.$v_impl); }
	public JLvalueExprView	getJLvalueExprView()	{ return new JAssignExprView((AssignExprImpl)this.$v_impl); }
	public JAssignExprView	getJAssignExprView()	{ return new JAssignExprView((AssignExprImpl)this.$v_impl); }

	@getter public AssignOperator	get$op()			{ return this.getAssignExprView().op; }
	@getter public ENode			get$lval()			{ return this.getAssignExprView().lval; }
	@getter public ENode			get$value()			{ return this.getAssignExprView().value; }
	
	@setter public void set$op(AssignOperator val)		{ this.getAssignExprView().op = val; }
	@setter public void set$lval(ENode val)			{ this.getAssignExprView().lval = val; }
	@setter public void set$value(ENode val)			{ this.getAssignExprView().value = val; }

	public AssignExpr() {
		super(new AssignExprImpl());
	}

	public AssignExpr(int pos, AssignOperator op, ENode lval, ENode value) {
		super(new AssignExprImpl(pos));
		this.op = op;
		this.lval = lval;
		this.value = value;
	}

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

	public Type getType() { return lval.getType(); }

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() )
			return;
		lval.resolve(reqType);
		Type et1 = lval.getType();
		if (op == AssignOperator.Assign && et1.isWrapper())
			value.resolve(et1.getWrappedType());
		else if (op == AssignOperator.Assign2 && et1.isWrapper())
			value.resolve(((WrapperType)et1).getUnwrappedType());
		else
			value.resolve(et1);
		if (value instanceof TypeRef)
			((TypeRef)value).toExpr(et1);
		Type et2 = value.getType();
		if( op == AssignOperator.Assign && et2.isAutoCastableTo(et1) && !et1.isWrapper() && !et2.isWrapper()) {
			this.resolve2(reqType);
			return;
		}
		else if( op == AssignOperator.Assign2 && et1.isWrapper() && et2.isInstanceOf(et1)) {
			this.resolve2(reqType);
			return;
		}
		else if( op == AssignOperator.AssignAdd && et1 ≈ Type.tpString ) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==AssignOperator.AssignAdd
			||   op==AssignOperator.AssignSub
			||   op==AssignOperator.AssignMul
			||   op==AssignOperator.AssignDiv
			||   op==AssignOperator.AssignMod
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==AssignOperator.AssignLeftShift
			||   op==AssignOperator.AssignRightShift
			||   op==AssignOperator.AssignUnsignedRightShift
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isInteger() && et2.isInteger() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( ( et1.isBoolean() && et2.isBoolean() ) &&
			(    op==AssignOperator.AssignBitOr
			||   op==AssignOperator.AssignBitXor
			||   op==AssignOperator.AssignBitAnd
			)
		) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,lval,value};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,(ENode)~lval,opt.method,new ENode[]{(ENode)~value}));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (op != AssignOperator.Assign2) {
			if (et1.isWrapper() && et2.isWrapper()) {
				lval = et1.makeWrappedAccess(lval);
				value = et2.makeWrappedAccess(value);
				resolve(reqType);
				return;
			}
			else if (et1.isWrapper()) {
				lval = et1.makeWrappedAccess(lval);
				resolve(reqType);
				return;
			}
			else if (et2.isWrapper()) {
				value = et2.makeWrappedAccess(value);
				resolve(reqType);
				return;
			}
		}
		this.resolve2(reqType); //throw new CompilerException(pos,"Unresolved expression "+this);
	}

	private ENode resolve2(Type reqType) {
		lval.resolve(null);
		if( !(lval instanceof LvalueExpr) )
			throw new RuntimeException("Can't assign to "+lval+": lvalue requared");
		Type t1 = lval.getType();
		if( op==AssignOperator.AssignAdd && t1 ≈ Type.tpString ) {
			op = AssignOperator.Assign;
			value = new BinaryExpr(pos,BinaryOperator.Add,new Shadow(lval),(ENode)~value);
		}
		if (value instanceof TypeRef)
			((TypeRef)value).toExpr(t1);
		else if (value instanceof ENode)
			value.resolve(t1);
		else
			throw new CompilerException(value, "Can't opeerate on "+value);
		Type t2 = value.getType();
		if( op==AssignOperator.AssignLeftShift || op==AssignOperator.AssignRightShift || op==AssignOperator.AssignUnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				value = new CastExpr(pos,Type.tpInt,(ENode)~value);
				value.resolve(Type.tpInt);
			}
		}
		else if( !t2.isInstanceOf(t1) ) {
			if( t2.isCastableTo(t1) ) {
				value = new CastExpr(pos,t1,(ENode)~value);
				value.resolve(t1);
			} else {
				throw new RuntimeException("Value of type "+t2+" can't be assigned to "+lval);
			}
		}
		getDFlow().out();

		// Set violation of the field
		if( lval instanceof SFldExpr
		 || (
				lval instanceof IFldExpr
			 && ((IFldExpr)lval).obj instanceof LVarExpr
			 &&	((LVarExpr)((IFldExpr)lval).obj).ident.equals(nameThis)
			)
		) {
			if( ctx_method != null && ctx_method.isInvariantMethod() )
				Kiev.reportError(this,"Side-effect in invariant condition");
			if( ctx_method != null && !ctx_method.isInvariantMethod() ) {
				if( lval instanceof SFldExpr )
					ctx_method.addViolatedField( ((SFldExpr)lval).var );
				else
					ctx_method.addViolatedField( ((IFldExpr)lval).var );
			}
		}
		setResolved(true);
		return this;
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
			res = ((AssignExpr)dfi.node_impl.getNode()).addNodeTypeInfo(f.calc(dfi));
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
	
	@node
	public static class BinaryExprImpl extends ENodeImpl {
		@ref public BinaryOperator	op;
		@att public ENode			expr1;
		@att public ENode			expr2;
		public BinaryExprImpl() {}
		public BinaryExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view BinaryExprView of BinaryExprImpl extends ENodeView {
		public BinaryOperator	op;
		public ENode			expr1;
		public ENode			expr2;
	}
	
	@att public abstract virtual BinaryOperator	op;
	@att public abstract virtual ENode				expr1;
	@att public abstract virtual ENode				expr2;
	
	@getter public BinaryOperator	get$op()					{ return this.getBinaryExprView().op; }
	@getter public ENode			get$expr1()					{ return this.getBinaryExprView().expr1; }
	@getter public ENode			get$expr2()					{ return this.getBinaryExprView().expr2; }
	@setter public void				set$op(BinaryOperator val)	{ this.getBinaryExprView().op = val; }
	@setter public void				set$expr1(ENode val)		{ this.getBinaryExprView().expr1 = val; }
	@setter public void				set$expr2(ENode val)		{ this.getBinaryExprView().expr2 = val; }

	public NodeView					getNodeView()				{ return new BinaryExprView((BinaryExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new BinaryExprView((BinaryExprImpl)this.$v_impl); }
	public BinaryExprView			getBinaryExprView()			{ return new BinaryExprView((BinaryExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JBinaryExprView((BinaryExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JBinaryExprView((BinaryExprImpl)this.$v_impl); }
	public JBinaryExprView			getJBinaryExprView()		{ return new JBinaryExprView((BinaryExprImpl)this.$v_impl); }
	
	public BinaryExpr() {
		super(new BinaryExprImpl());
	}

	public BinaryExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
		super(new BinaryExprImpl(pos));
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
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
		resolve(null);
		return getType();
	}

	public void mainResolveOut() {
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( op == BinaryOperator.Add
			&& ( et1 ≈ Type.tpString || et2 ≈ Type.tpString ||
				(et1.isWrapper() && et1.getWrappedType() ≈ Type.tpString) ||
				(et2.isWrapper() && et2.getWrappedType() ≈ Type.tpString)
			   )
		) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
				replaceWithNode((ENode)~sce);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				if (et1.isWrapper()) expr1 = et1.makeWrappedAccess(expr1);
				sce.appendArg(expr1);
				if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
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
				ENode e;
				if( opt.method.isStatic() )
					replaceWithNode(new CallExpr(pos,null,opt.method,new ENode[]{(ENode)~expr1,(ENode)~expr2}));
				else
					replaceWithNode(new CallExpr(pos,(ENode)~expr1,opt.method,new ENode[]{(ENode)~expr2}));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et1.isWrapper() && et2.isWrapper()) {
			expr1 = et1.makeWrappedAccess(expr1);
			expr2 = et1.makeWrappedAccess(expr2);
			mainResolveOut();
			return;
		}
		if (et1.isWrapper()) {
			expr1 = et1.makeWrappedAccess(expr1);
			mainResolveOut();
			return;
		}
		if (et2.isWrapper()) {
			expr2 = et1.makeWrappedAccess(expr2);
			mainResolveOut();
			return;
		}
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr1.resolve(null);
		expr2.resolve(null);
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( op == BinaryOperator.Add
			&& ( et1 ≈ Type.tpString || et2 ≈ Type.tpString ||
				(et1.isWrapper() && et1.getWrappedType() ≈ Type.tpString) ||
				(et2.isWrapper() && et2.getWrappedType() ≈ Type.tpString)
			   )
		) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
				replaceWithNodeResolve(Type.tpString, (ENode)~sce);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				if (et1.isWrapper()) expr1 = et1.makeWrappedAccess(expr1);
				sce.appendArg(expr1);
				if (et2.isWrapper()) expr2 = et2.makeWrappedAccess(expr2);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
				replaceWithNodeResolve(Type.tpString, sce);
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
			this.resolve2(null);
			return;
		}
		else if( ( et1.isInteger() && et2.isIntegerInCode() ) &&
			(    op==BinaryOperator.LeftShift
			||   op==BinaryOperator.RightShift
			||   op==BinaryOperator.UnsignedRightShift
			)
		) {
			this.resolve2(null);
			return;
		}
		else if( ( (et1.isInteger() && et2.isInteger()) || (et1.isBoolean() && et2.isBoolean()) ) &&
			(    op==BinaryOperator.BitOr
			||   op==BinaryOperator.BitXor
			||   op==BinaryOperator.BitAnd
			)
		) {
			this.resolve2(null);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				ENode e;
				if( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{(ENode)~expr1,(ENode)~expr2}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,(ENode)~expr1,opt.method,new ENode[]{(ENode)~expr2}));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et1.isWrapper() && et2.isWrapper()) {
			expr1 = et1.makeWrappedAccess(expr1);
			expr2 = et1.makeWrappedAccess(expr2);
			resolve(reqType);
			return;
		}
		if (et1.isWrapper()) {
			expr1 = et1.makeWrappedAccess(expr1);
			resolve(reqType);
			return;
		}
		if (et2.isWrapper()) {
			expr2 = et1.makeWrappedAccess(expr2);
			resolve(reqType);
			return;
		}
		resolve2(reqType);
	}

	private void resolve2(Type reqType) {
		expr1.resolve(null);
		expr2.resolve(null);

		Type rt = getType();
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();

		// Special case for '+' operator if one arg is a String
		if( op==BinaryOperator.Add && expr1.getType().equals(Type.tpString) || expr2.getType().equals(Type.tpString) ) {
			if( expr1 instanceof StringConcatExpr ) {
				StringConcatExpr sce = (StringConcatExpr)expr1;
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Adding "+expr2+" to StringConcatExpr, now ="+sce);
				replaceWithNodeResolve(Type.tpString, sce);
			} else {
				StringConcatExpr sce = new StringConcatExpr(pos);
				sce.appendArg(expr1);
				sce.appendArg(expr2);
				trace(Kiev.debugStatGen,"Rewriting "+expr1+"+"+expr2+" as StringConcatExpr");
				replaceWithNodeResolve(Type.tpString, sce);
			}
			return;
		}

		if( op==BinaryOperator.LeftShift || op==BinaryOperator.RightShift || op==BinaryOperator.UnsignedRightShift ) {
			if( !t2.isIntegerInCode() ) {
				expr2 = new CastExpr(pos,Type.tpInt,expr2);
				expr2.resolve(Type.tpInt);
			}
		} else {
			if( !rt.equals(t1) && t1.isCastableTo(rt) ) {
				expr1 = new CastExpr(pos,rt,(ENode)~expr1);
				expr1.resolve(null);
			}
			if( !rt.equals(t2) && t2.isCastableTo(rt) ) {
				expr2 = new CastExpr(pos,rt,(ENode)~expr2);
				expr2.resolve(null);
			}
		}

		// Check if both expressions are constant
		if( expr1.isConstantExpr() && expr2.isConstantExpr() ) {
			Number val1 = (Number)expr1.getConstValue();
			Number val2 = (Number)expr2.getConstValue();
			if( op == BinaryOperator.BitOr ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() | val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() | val2.intValue()));
			}
			else if( op == BinaryOperator.BitXor ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() ^ val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() ^ val2.intValue()));
			}
			else if( op == BinaryOperator.BitAnd ) {
				if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() & val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() & val2.intValue()));
			}
			else if( op == BinaryOperator.LeftShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() << val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() << val2.intValue()));
			}
			else if( op == BinaryOperator.RightShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() >> val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() >> val2.intValue()));
			}
			else if( op == BinaryOperator.UnsignedRightShift ) {
				if( val1 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() >>> val2.intValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() >>> val2.intValue()));
			}
			else if( op == BinaryOperator.Add ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() + val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() + val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() + val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() + val2.intValue()));
			}
			else if( op == BinaryOperator.Sub ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() - val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() - val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() - val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() - val2.intValue()));
			}
			else if( op == BinaryOperator.Mul ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() * val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() * val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() * val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() * val2.intValue()));
			}
			else if( op == BinaryOperator.Div ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() / val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() / val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() / val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() / val2.intValue()));
			}
			else if( op == BinaryOperator.Mod ) {
				if( val1 instanceof Double || val2 instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val1.doubleValue() % val2.doubleValue()));
				else if( val1 instanceof Float || val2 instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val1.floatValue() % val2.floatValue()));
				else if( val1 instanceof Long || val2 instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val1.longValue() % val2.longValue()));
				else
					replaceWithNodeResolve(new ConstIntExpr(val1.intValue() % val2.intValue()));
			}
		}
		setResolved(true);
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

	@node
	public static class StringConcatExprImpl extends ENodeImpl {
		@att public NArr<ENode>			args;
		public StringConcatExprImpl() {}
		public StringConcatExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view StringConcatExprView of StringConcatExprImpl extends ENodeView {
		public access:ro	NArr<ENode>		args;
	}
	
	@att public abstract virtual access:ro	NArr<ENode>			args;
	
	@getter public NArr<ENode>		get$args()				{ return this.getStringConcatExprView().args; }

	public NodeView					getNodeView()				{ return new StringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new StringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	public StringConcatExprView		getStringConcatExprView()	{ return new StringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JStringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JStringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	public JStringConcatExprView	getJStringConcatExprView()	{ return new JStringConcatExprView((StringConcatExprImpl)this.$v_impl); }
	

	public StringConcatExpr() {
		super(new StringConcatExprImpl());
	}

	public StringConcatExpr(int pos) {
		super(new StringConcatExprImpl(pos));
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			if( i < args.length-1 )
				sb.append('+');
		}
		return sb.toString();
	}

	public Type getType() {
		return Type.tpString;
	}

	public Operator getOp() { return BinaryOperator.Add; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		foreach (ENode e; args)
			e.resolve(null);
		setResolved(true);
	}

	public void appendArg(ENode expr) {
		args.append((ENode)~expr);
	}

	public Dumper toJava(Dumper dmp) {
//		dmp.append("((new java.lang.StringBuffer())");
//		for(int i=0; i < args.length; i++) {
//			dmp.append(".append(");
//			args[i].toJava(dmp);
//			dmp.append(')');
//		}
//		dmp.append(".toString())");
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append('+');
		}
		return dmp;
	}
}

@node
public class CommaExpr extends ENode {
	
	@dflow(out="exprs") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]	exprs;
	}

	@node
	public static class CommaExprImpl extends ENodeImpl {
		@att public NArr<ENode>		exprs;
		public CommaExprImpl() {}
		public CommaExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CommaExprView of CommaExprImpl extends ENodeView {
		public access:ro	NArr<ENode>		exprs;
	}
	
	@att public abstract virtual access:ro	NArr<ENode>			exprs;
	
	@getter public NArr<ENode>		get$exprs()				{ return this.getCommaExprView().exprs; }

	public NodeView					getNodeView()			{ return new CommaExprView((CommaExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()			{ return new CommaExprView((CommaExprImpl)this.$v_impl); }
	public CommaExprView			getCommaExprView()		{ return new CommaExprView((CommaExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()			{ return new JCommaExprView((CommaExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()			{ return new JCommaExprView((CommaExprImpl)this.$v_impl); }
	public JCommaExprView			getJCommaExprView()		{ return new JCommaExprView((CommaExprImpl)this.$v_impl); }
	
	
	public CommaExpr() {
		super(new CommaExprImpl());
	}

	public CommaExpr(ENode expr) {
		super(new CommaExprImpl(expr.pos));
		this.exprs.add(expr);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < exprs.length; i++) {
			sb.append(exprs[i]);
			if( i < exprs.length-1 )
				sb.append(',');
		}
		return sb.toString();
	}

	public KString getName() { return KString.Empty; };

	public Type getType() { return exprs[exprs.length-1].getType(); }

	public int getPriority() { return 0; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1) {
				exprs[i].resolve(Type.tpVoid);
				exprs[i].setGenVoidExpr(true);
			} else {
				exprs[i].resolve(reqType);
			}
		}
		setResolved(true);
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
public class BlockExpr extends ENode implements ScopeOfNames, ScopeOfMethods {
	
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		stats;
	@dflow(in="stats")					ENode		res;
	}

	@node
	public static class BlockExprImpl extends ENodeImpl {
		@att public NArr<ENode>			stats;
		@att public ENode				res;
		public BlockExprImpl() {}
		public BlockExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view BlockExprView of BlockExprImpl extends ENodeView {
		public access:ro	NArr<ENode>		stats;
		public				ENode			res;
	}
	
	@att public abstract virtual			ENode				res;
	@att public abstract virtual access:ro	NArr<ENode>			stats;
	
	@getter public ENode			get$res()				{ return this.getBlockExprView().res; }
	@getter public NArr<ENode>		get$stats()				{ return this.getBlockExprView().stats; }
	
	@setter public void				set$res(ENode val)		{ this.getBlockExprView().res = val; }

	public NodeView				getNodeView()			{ return new BlockExprView((BlockExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new BlockExprView((BlockExprImpl)this.$v_impl); }
	public BlockExprView		getBlockExprView()		{ return new BlockExprView((BlockExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JBlockExprView((BlockExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JBlockExprView((BlockExprImpl)this.$v_impl); }
	public JBlockExprView		getJBlockExprView()		{ return new JBlockExprView((BlockExprImpl)this.$v_impl); }
	
	
	public BlockExpr() {
		super(new BlockExprImpl());
	}

	public BlockExpr(int pos) {
		super(new BlockExprImpl(pos));
	}

	public void setExpr(ENode res) {
		this.res = res;
	}

	public ENode addStatement(ENode st) {
		stats.append(st);
		return st;
	}

	public void addSymbol(Named sym) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ENode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.append(decl);
	}

	public void insertSymbol(Named sym, int idx) {
		ENode decl;
		if (sym instanceof Var)
			decl = new VarDecl((Var)sym);
		else if (sym instanceof Struct)
			decl = new LocalStructDecl((Struct)sym);
		else
			throw new RuntimeException("Expected e-node declaration, but got "+sym+" ("+sym.getClass()+")");
		foreach(ASTNode n; stats) {
			if (n instanceof Named && ((Named)n).getName().equals(sym.getName()) ) {
				Kiev.reportError(decl,"Symbol "+sym.getName()+" already declared in this scope");
			}
		}
		stats.insert(decl,idx);
	}
	
	public Type getType() {
		if (res == null) return Type.tpVoid;
		return res.getType();
	}

	public int		getPriority() { return 255; }

	public rule resolveNameR(DNode@ node, ResInfo info, KString name)
		ASTNode@ n;
	{
		n @= new SymbolIterator(this.stats, info.space_prev),
		{
			n instanceof VarDecl,
			((VarDecl)n).var.name.equals(name),
			node ?= ((VarDecl)n).var
		;	n instanceof LocalStructDecl,
			name.equals(((LocalStructDecl)n).clazz.name.short_name),
			node ?= ((LocalStructDecl)n).clazz
		;	n instanceof TypeDecl,
			name.equals(((TypeDecl)n).getName()),
			node ?= ((TypeDecl)n)
		}
	;
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward() && ((VarDecl)n).var.name.equals(name),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		n.getType().resolveNameAccessR(node,info,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
		ASTNode@ n;
	{
		info.isForwardsAllowed(),
		n @= new SymbolIterator(this.stats, info.space_prev),
		n instanceof VarDecl && ((VarDecl)n).var.isForward(),
		info.enterForward(((VarDecl)n).var) : info.leaveForward(((VarDecl)n).var),
		((VarDecl)n).var.getType().resolveCallAccessR(node,info,name,mt)
	}

	public void resolve(Type reqType) {
		BlockStat.resolveBlockStats(this, stats);
		if (res != null) {
			res.resolve(reqType);
		}
	}

	static class BlockExprDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		BlockExprDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("res"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			BlockExpr node = (BlockExpr)dfi.node_impl.getNode();
			Vector<Var> vars = new Vector<Var>();
			foreach (ASTNode n; node.stats; n instanceof VarDecl) vars.append(((VarDecl)n).var);
			if (vars.length > 0)
				res = DFFunc.calc(f, dfi).cleanInfoForVars(vars.toArray());
			else
				res = DFFunc.calc(f, dfi);
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new BlockExprDFFunc(dfi);
	}
	
	public String toString() {
		Dumper dmp = new Dumper();
		dmp.append("({").space();
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).space();
		if (res != null)
			res.toJava(dmp);
		dmp.space().append("})");
		return dmp.toString();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append("({").newLine(1);
		for(int i=0; i < stats.length; i++)
			stats[i].toJava(dmp).newLine();
		if (res != null)
			res.toJava(dmp);
		dmp.newLine(-1).append("})");
		return dmp;
	}

}

@node
public class UnaryExpr extends ENode {
	
	@dflow(out="expr") private static class DFI {
	@dflow(out="this:in")			ENode		expr;
	}

	@node
	public static class UnaryExprImpl extends ENodeImpl {
		@ref public Operator		op;
		@att public ENode			expr;
		public UnaryExprImpl() {}
		public UnaryExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view UnaryExprView of UnaryExprImpl extends ENodeView {
		public Operator			op;
		public ENode			expr;
	}
	
	@att public abstract virtual Operator			op;
	@att public abstract virtual ENode				expr;
	
	@getter public Operator			get$op()					{ return this.getUnaryExprView().op; }
	@getter public ENode			get$expr()					{ return this.getUnaryExprView().expr; }
	@setter public void				set$op(Operator val)		{ this.getUnaryExprView().op = val; }
	@setter public void				set$expr(ENode val)			{ this.getUnaryExprView().expr = val; }

	public NodeView					getNodeView()				{ return new UnaryExprView((UnaryExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new UnaryExprView((UnaryExprImpl)this.$v_impl); }
	public UnaryExprView			getUnaryExprView()			{ return new UnaryExprView((UnaryExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JUnaryExprView((UnaryExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JUnaryExprView((UnaryExprImpl)this.$v_impl); }
	public JUnaryExprView			getJUnaryExprView()			{ return new JUnaryExprView((UnaryExprImpl)this.$v_impl); }
	
	public UnaryExpr() {
		super(new UnaryExprImpl());
	}

	public UnaryExpr(int pos, Operator op, ENode expr) {
		super(new UnaryExprImpl(pos));
		this.op = op;
		this.expr = expr;
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

	public Type getType() {
		return expr.getType();
	}

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		expr.resolve(reqType);
		Type et = expr.getType();
		if( et.isNumber() &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.PreDecr
			|| op==PostfixOperator.PostIncr
			|| op==PostfixOperator.PostDecr
			)
		) {
			replaceWithNodeResolve(reqType, new IncrementExpr(pos,op,(ENode)~expr));
			return;
		}
		if( et.isAutoCastableTo(Type.tpBoolean) &&
			(  op==PrefixOperator.PreIncr
			|| op==PrefixOperator.BooleanNot
			)
		) {
			replaceWithNodeResolve(Type.tpBoolean, new BooleanNotExpr(pos,(ENode)~expr));
			return;
		}
		if( et.isNumber() &&
			(  op==PrefixOperator.Pos
			|| op==PrefixOperator.Neg
			)
		) {
			this.resolve2(reqType);
			return;
		}
		if( et.isInteger() && op==PrefixOperator.BitNot ) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			if (ctx_clazz != null && opt.method != null && opt.method.type.arity == 1) {
				if ( !ctx_clazz.concr_type.isInstanceOf(opt.method.ctx_clazz.concr_type) )
					continue;
			}
			Type[] tps = new Type[]{null,et};
			ASTNode[] argsarr = new ASTNode[]{null,expr};
			if( opt.match(tps,argsarr) && tps[0] != null && opt.method != null ) {
				ENode e;
				if ( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{(ENode)~expr}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,(ENode)~expr,opt.method,ENode.emptyArray));
				return;
			}
		}
		// Not a standard and not overloaded, try wrapped classes
		if (et.isWrapper()) {
			replaceWithNodeResolve(reqType, new UnaryExpr(pos,op,et.makeWrappedAccess(expr)));
			return;
		}
		resolve2(reqType);
	}

	private void resolve2(Type reqType) {
		expr.resolve(null);
		if( op==PrefixOperator.PreIncr
		||  op==PrefixOperator.PreDecr
		||  op==PostfixOperator.PostIncr
		||  op==PostfixOperator.PostDecr
		) {
			replaceWithNodeResolve(reqType, new IncrementExpr(pos,op,expr));
			return;
		} else if( op==PrefixOperator.BooleanNot ) {
			replaceWithNodeResolve(reqType, new BooleanNotExpr(pos,expr));
			return;
		}
		// Check if expression is constant
		if( expr.isConstantExpr() ) {
			Number val = (Number)expr.getConstValue();
			if( op == PrefixOperator.Pos ) {
				if( val instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(val.doubleValue()));
				else if( val instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(val.floatValue()));
				else if( val instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(val.longValue()));
				else if( val instanceof Integer )
					replaceWithNodeResolve(new ConstIntExpr(val.intValue()));
				else if( val instanceof Short )
					replaceWithNodeResolve(new ConstShortExpr(val.shortValue()));
				else if( val instanceof Byte )
					replaceWithNodeResolve(new ConstByteExpr(val.byteValue()));
			}
			else if( op == PrefixOperator.Neg ) {
				if( val instanceof Double )
					replaceWithNodeResolve(new ConstDoubleExpr(-val.doubleValue()));
				else if( val instanceof Float )
					replaceWithNodeResolve(new ConstFloatExpr(-val.floatValue()));
				else if( val instanceof Long )
					replaceWithNodeResolve(new ConstLongExpr(-val.longValue()));
				else if( val instanceof Integer )
					replaceWithNodeResolve(new ConstIntExpr(-val.intValue()));
				else if( val instanceof Short )
					replaceWithNodeResolve(new ConstShortExpr(-val.shortValue()));
				else if( val instanceof Byte )
					replaceWithNodeResolve(new ConstByteExpr(-val.byteValue()));
			}
		}
		setResolved(true);
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

	@node
	public static class IncrementExprImpl extends ENodeImpl {		
		@ref public Operator			op;
		@att public ENode				lval;

		public IncrementExprImpl() {}
		public IncrementExprImpl(int pos) {
			super(pos);
		}
	}
	@nodeview
	public static view IncrementExprView of IncrementExprImpl extends ENodeView {
		public Operator		op;
		public ENode		lval;
	}
	
	@att public abstract virtual Operator			op;
	@att public abstract virtual ENode				lval;
	
	
	public NodeView				getNodeView()			{ return new IncrementExprView((IncrementExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new IncrementExprView((IncrementExprImpl)this.$v_impl); }
	public IncrementExprView	getIncrementExprView()	{ return new IncrementExprView((IncrementExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JIncrementExprView((IncrementExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()			{ return new JIncrementExprView((IncrementExprImpl)this.$v_impl); }
	public JIncrementExprView	getJIncrementExprView()	{ return new JIncrementExprView((IncrementExprImpl)this.$v_impl); }

	@getter public Operator			get$op()			{ return this.getIncrementExprView().op; }
	@getter public ENode			get$lval()			{ return this.getIncrementExprView().lval; }
	
	@setter public void set$op(Operator val)			{ this.getIncrementExprView().op = val; }
	@setter public void set$lval(ENode val)			{ this.getIncrementExprView().lval = val; }

	public IncrementExpr() {
		super(new IncrementExprImpl());
	}

	public IncrementExpr(int pos, Operator op, ENode lval) {
		super(new IncrementExprImpl(pos));
		this.op = op;
		this.lval = lval;
	}

	public String toString() {
		if( op == PostfixOperator.PostIncr || op == PostfixOperator.PostDecr )
			return lval.toString()+op.image;
		else
			return op.image+lval.toString();
	}

	public Type getType() {
		return lval.getType();
	}

	public Operator getOp() { return op; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		setResolved(true);
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

	@node
	public static class ConditionalExprImpl extends ENodeImpl {
		@att public ENode			cond;
		@att public ENode			expr1;
		@att public ENode			expr2;
		public ConditionalExprImpl() {}
		public ConditionalExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ConditionalExprView of ConditionalExprImpl extends ENodeView {
		public ENode		cond;
		public ENode		expr1;
		public ENode		expr2;
	}
	
	@att public abstract virtual ENode			cond;
	@att public abstract virtual ENode			expr1;
	@att public abstract virtual ENode			expr2;
	
	@getter public ENode		get$cond()				{ return this.getConditionalExprView().cond; }
	@getter public ENode		get$expr1()				{ return this.getConditionalExprView().expr1; }
	@getter public ENode		get$expr2()				{ return this.getConditionalExprView().expr2; }
	@setter public void			set$cond(ENode val)		{ this.getConditionalExprView().cond = val; }
	@setter public void			set$expr1(ENode val)	{ this.getConditionalExprView().expr1 = val; }
	@setter public void			set$expr2(ENode val)	{ this.getConditionalExprView().expr2 = val; }

	public NodeView					getNodeView()				{ return new ConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new ConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	public ConditionalExprView		getConditionalExprView()	{ return new ConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	public JConditionalExprView		getJConditionalExprView()	{ return new JConditionalExprView((ConditionalExprImpl)this.$v_impl); }
	
	public ConditionalExpr() {
		super(new ConditionalExprImpl());
	}

	public ConditionalExpr(int pos, ENode cond, ENode expr1, ENode expr2) {
		super(new ConditionalExprImpl(pos));
		this.cond = cond;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(').append(cond).append(") ? ");
		sb.append('(').append(expr1).append(") : ");
		sb.append('(').append(expr2).append(") ");
		return sb.toString();
	}

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

	public Operator getOp() { return MultiOperator.Conditional; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		cond.resolve(Type.tpBoolean);
		expr1.resolve(reqType);
		expr2.resolve(reqType);

		if( expr1.getType() ≉ getType() ) {
			expr1 = new CastExpr(expr1.pos,getType(),(ENode)~expr1);
			expr1.resolve(getType());
		}
		if( expr2.getType() ≉ getType() ) {
			expr2 = new CastExpr(expr2.pos,getType(),(ENode)~expr2);
			expr2.resolve(getType());
		}
		setResolved(true);
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

	@node
	public static class CastExprImpl extends ENodeImpl {
		@att public ENode		expr;
		@att public TypeRef		type;
		@att public boolean		reinterp;
		public CastExprImpl() {}
		public CastExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CastExprView of CastExprImpl extends ENodeView {
		public ENode	expr;
		public TypeRef	type;
		public boolean	reinterp;
	}
	
	@att public abstract virtual ENode			expr;
	@att public abstract virtual TypeRef		type;
	@att public abstract virtual boolean		reinterp;
	
	@getter public ENode		get$expr()					{ return this.getCastExprView().expr; }
	@getter public TypeRef		get$type()					{ return this.getCastExprView().type; }
	@getter public boolean		get$reinterp()				{ return this.getCastExprView().reinterp; }
	@setter public void			set$expr(ENode val)			{ this.getCastExprView().expr = val; }
	@setter public void			set$type(TypeRef val)		{ this.getCastExprView().type = val; }
	@setter public void			set$reinterp(boolean val)	{ this.getCastExprView().reinterp = val; }

	public NodeView					getNodeView()			{ return new CastExprView((CastExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()			{ return new CastExprView((CastExprImpl)this.$v_impl); }
	public CastExprView				getCastExprView()		{ return new CastExprView((CastExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()			{ return new JCastExprView((CastExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()			{ return new JCastExprView((CastExprImpl)this.$v_impl); }
	public JCastExprView			getJCastExprView()		{ return new JCastExprView((CastExprImpl)this.$v_impl); }
	
	public CastExpr() {
		super(new CastExprImpl());
	}

	public CastExpr(int pos, Type type, ENode expr) {
		super(new CastExprImpl(pos));
		this.type = new TypeRef(type);
		this.expr = expr;
	}

	public CastExpr(int pos, TypeRef type, ENode expr) {
		super(new CastExprImpl(pos));
		this.type = type;
		this.expr = expr;
	}

	public CastExpr(int pos, Type type, ENode expr, boolean reint) {
		this(pos, type, expr);
		reinterp = reint;
	}

	public CastExpr(int pos, TypeRef type, ENode expr, boolean reint) {
		this(pos, type, expr);
		reinterp = reint;
	}

	public String toString() {
		return "(("+type+")"+expr+")";
	}

	public Type getType() {
		return type.getType();
	}

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public int getPriority() { return opCastPriority; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		Type type = this.type.getType();
		expr.resolve(type);
		if (expr instanceof TypeRef)
			((TypeRef)expr).toExpr(type);
		Type extp = Type.getRealType(type,expr.getType());
		if( type ≡ Type.tpBoolean && extp ≡ Type.tpRule ) {
			replaceWithNode(expr);
			return;
		}
		// Try to find $cast method
		if( !extp.isAutoCastableTo(type) ) {
			if( tryOverloadedCast(extp) )
				return;
			if (extp.isWrapper()) {
				expr = extp.makeWrappedAccess(expr);
				resolve(reqType);
				return;
			}
		}
		else if (extp.isWrapper() && extp.getWrappedType().isAutoCastableTo(type)) {
			if( tryOverloadedCast(extp) )
				return;
			expr = extp.makeWrappedAccess(expr);
			resolve(reqType);
			return;
		}
		else {
			this.resolve2(type);
			return;
		}
		if( extp.isCastableTo(type) ) {
			this.resolve2(type);
			return;
		}
		if( type ≡ Type.tpInt && extp ≡ Type.tpBoolean && reinterp ) {	
			this.resolve2(type);
			return;
		}
		throw new CompilerException(this,"Expression "+expr+" of type "+extp+" is not castable to "+type);
	}

	public boolean tryOverloadedCast(Type et) {
		Method@ v;
		ResInfo info = new ResInfo(this,ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
		v.$unbind();
		CallType mt = new CallType(Type.emptyArray,this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			ENode call = info.buildCall(this,(ENode)~expr,(Method)v,info.mt,ENode.emptyArray);
			if (this.type.getType().isReference())
				call.setCastCall(true);
			replaceWithNodeResolve(type.getType(),call);
			return true;
		}
		v.$unbind();
		info = new ResInfo(this,ResInfo.noForwards|ResInfo.noImports);
		mt = new CallType(new Type[]{expr.getType()},this.type.getType());
		if( PassInfo.resolveBestMethodR(et,v,info,nameCastOp,mt) ) {
			assert(v.isStatic());
			ENode call = new CallExpr(pos,null,(Method)v,new ENode[]{(ENode)~expr});
			replaceWithNodeResolve(type.getType(),call);
			return true;
		}
		return false;
	}

	private void resolve2(Type reqType) {
		Type type = this.type.getType();
		expr.resolve(type);
//		if( e instanceof Struct )
//			expr = Expr.toExpr((Struct)e,reqType,pos,parent);
//		else
//			expr = (Expr)e;
		if (reqType ≡ Type.tpVoid) {
			setResolved(true);
		}
		Type et = Type.getRealType(type,expr.getType());
		// Try wrapped field
		if (et.isWrapper() && et.getWrappedType().equals(type)) {
			expr = et.makeWrappedAccess(expr);
			resolve(reqType);
			return;
		}
		// try null to something...
		if (et ≡ Type.tpNull && reqType.isReference())
			return;
		if( type ≡ Type.tpBoolean && et ≡ Type.tpRule ) {
			replaceWithNodeResolve(type, new BinaryBoolExpr(pos,BinaryOperator.NotEquals,expr,new ConstNullExpr()));
			return;
		}
		if( type.isBoolean() && et.isBoolean() )
			return;
		if( !Kiev.javaMode && type.isInstanceOf(Type.tpEnum) && et.isIntegerInCode() ) {
			if (type.isIntegerInCode())
				return;
			Method cm = ((CompaundType)type).clazz.resolveMethod(nameCastOp,type,Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,null,cm,new ENode[]{(ENode)~expr}));
			return;
		}
		if( !Kiev.javaMode && type.isIntegerInCode() && et.isInstanceOf(Type.tpEnum) ) {
			if (et.isIntegerInCode())
				return;
			Method cf = Type.tpEnum.clazz.resolveMethod(nameEnumOrdinal, Type.tpInt);
			replaceWithNodeResolve(reqType, new CallExpr(pos,(ENode)~expr,cf,ENode.emptyArray));
			return;
		}
		// Try to find $cast method
		if( !et.isAutoCastableTo(type) && tryOverloadedCast(et))
			return;

		if( et.isReference() != type.isReference() && !(expr instanceof ClosureCallExpr) )
			if( !et.isReference() && type.isArgument() )
				Kiev.reportWarning(this,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			else if (!et.isEnum())
				throw new CompilerException(this,"Expression "+expr+" of type "+et+" cannot be casted to type "+type);
		if( !et.isCastableTo((Type)type) && !(reinterp && et.isIntegerInCode() && type.isIntegerInCode() )) {
			throw new RuntimeException("Expression "+expr+" cannot be casted to type "+type);
		}
		if( Kiev.verify && expr.getType() ≉ et ) {
			setResolved(true);
			return;
		}
		if( et.isReference() && et.isInstanceOf((Type)type) ) {
			setResolved(true);
			return;
		}
		if( et.isReference() && type.isReference() && et.getStruct() != null
		 && et.getStruct().package_clazz.isClazz()
		 && !et.isArgument()
		 && !et.isStaticClazz() && et.getStruct().package_clazz.concr_type.isAutoCastableTo(type)
		) {
			replaceWithNodeResolve(reqType,
				new CastExpr(pos,type,
					new IFldExpr(pos,(ENode)~expr,OuterThisAccessExpr.outerOf((Struct)et.getStruct()))
				));
			return;
		}
		if( expr.isConstantExpr() ) {
			Object val = expr.getConstValue();
			Type t = type;
			if( val instanceof Number ) {
				Number num = (Number)val;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num.doubleValue())); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num.floatValue())); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num.longValue())); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num.intValue())); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num.intValue())); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num.intValue())); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num.intValue())); return; }
			}
			else if( val instanceof Character ) {
				char num = ((Character)val).charValue();
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)(int)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) (int)num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  (int)num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   (int)num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) (int)num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  (int)num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
			else if( val instanceof Boolean ) {
				int num = ((Boolean)val).booleanValue() ? 1 : 0;
				if     ( t ≡ Type.tpDouble ) { replaceWithNodeResolve(new ConstDoubleExpr ((double)num)); return; }
				else if( t ≡ Type.tpFloat )  { replaceWithNodeResolve(new ConstFloatExpr  ((float) num)); return; }
				else if( t ≡ Type.tpLong )   { replaceWithNodeResolve(new ConstLongExpr   ((long)  num)); return; }
				else if( t ≡ Type.tpInt )    { replaceWithNodeResolve(new ConstIntExpr    ((int)   num)); return; }
				else if( t ≡ Type.tpShort )  { replaceWithNodeResolve(new ConstShortExpr  ((short) num)); return; }
				else if( t ≡ Type.tpByte )   { replaceWithNodeResolve(new ConstByteExpr   ((byte)  num)); return; }
				else if( t ≡ Type.tpChar )   { replaceWithNodeResolve(new ConstCharExpr   ((char)  num)); return; }
			}
		}
		if( et.equals(type) ) {
			setResolved(true);
			return;
		}
		if( expr instanceof ClosureCallExpr && et instanceof CallType ) {
			if( et.isAutoCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = true;
				return;
			}
			else if( et.isCastableTo(type) ) {
				((ClosureCallExpr)expr).is_a_call = true;
			}
		}
		setResolved(true);
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
				ex.replaceWith(fun ()->ENode {return new CastExpr(ex.pos,tp,(ENode)~ex);});
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
		ex.replaceWith(fun ()->ENode {return new NewExpr(ex.pos,ref,new ENode[]{(ENode)~ex});});
	}

	public static void autoCastToPrimitive(ENode ex) {
		assert(ex.isAttached());
		Type tp = ex.getType();
		if( !tp.isReference() ) return;
		if( tp ≈ Type.tpBooleanRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpBooleanRef.clazz.resolveMethod(KString.from("booleanValue"),Type.tpBoolean),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpByteRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpByteRef.clazz.resolveMethod(KString.from("byteValue"),Type.tpByte),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpShortRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpShortRef.clazz.resolveMethod(KString.from("shortValue"),Type.tpShort),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpIntRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpIntRef.clazz.resolveMethod(KString.from("intValue"),Type.tpInt),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpLongRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpLongRef.clazz.resolveMethod(KString.from("longValue"),Type.tpLong),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpFloatRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpFloatRef.clazz.resolveMethod(KString.from("floatValue"),Type.tpFloat),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpDoubleRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpDoubleRef.clazz.resolveMethod(KString.from("doubleValue"),Type.tpDouble),ENode.emptyArray
			);});
		else if( tp ≈ Type.tpCharRef )
			ex.replaceWith(fun ()->ENode {return new CallExpr(ex.pos,(ENode)~ex,
				Type.tpCharRef.clazz.resolveMethod(KString.from("charValue"),Type.tpChar),ENode.emptyArray
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

