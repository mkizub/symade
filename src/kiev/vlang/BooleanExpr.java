package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JBoolExpr;
import kiev.be.java.JBinaryBooleanOrExpr;
import kiev.be.java.JBinaryBooleanAndExpr;
import kiev.be.java.JBinaryBoolExpr;
import kiev.be.java.JInstanceofExpr;
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
	@virtual typedef VView = BoolExprView;
	@virtual typedef JView = JBoolExpr;

	@nodeimpl
	public abstract static class BoolExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = BoolExpr;
	}
	@nodeview
	public abstract static view BoolExprView of BoolExprImpl extends ENodeView {
	}

	public BoolExpr(BoolExprImpl impl) { super(impl); }

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

@nodeset
public class BinaryBooleanOrExpr extends BoolExpr {

	@dflow(tru="join expr1:true expr2:true", fls="expr2:false") private static class DFI {
	@dflow(in="this:in")			ENode			expr1;
	@dflow(in="expr1:false")		ENode			expr2;
	}
	
	@virtual typedef This  = BinaryBooleanOrExpr;
	@virtual typedef NImpl = BinaryBooleanOrExprImpl;
	@virtual typedef VView = BinaryBooleanOrExprView;
	@virtual typedef JView = JBinaryBooleanOrExpr;

	@nodeimpl
	public static class BinaryBooleanOrExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BinaryBooleanOrExpr;
		@att public ENode			expr1;
		@att public ENode			expr2;
	}
	@nodeview
	public static view BinaryBooleanOrExprView of BinaryBooleanOrExprImpl extends BoolExprView {
		public ENode		expr1;
		public ENode		expr2;

		public Operator getOp() { return BinaryOperator.BooleanOr; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	
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
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		getDFlow().out();
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
	@virtual typedef VView = BinaryBooleanAndExprView;
	@virtual typedef JView = JBinaryBooleanAndExpr;

	@nodeimpl
	public static class BinaryBooleanAndExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BinaryBooleanAndExpr;
		@att public ENode			expr1;
		@att public ENode			expr2;
	}
	@nodeview
	public static view BinaryBooleanAndExprView of BinaryBooleanAndExprImpl extends BoolExprView {
		public ENode		expr1;
		public ENode		expr2;

		public Operator getOp() { return BinaryOperator.BooleanAnd; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	
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
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
	@virtual typedef VView = BinaryBoolExprView;
	@virtual typedef JView = JBinaryBoolExpr;

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

	private boolean resolveExprs() {
		expr1.resolve(null);
		if (!expr1.isForWrapper() && expr1.getType() instanceof CTimeType) {
			expr1 = expr1.getType().makeWrappedAccess(expr1);
			expr1.resolve(null);
		}

		expr2.resolve(null);
		if( expr2 instanceof TypeRef )
			getExprByStruct(((TypeRef)expr2).getType().getStruct());
		expr2.resolve(null);
		if (!expr2.isForWrapper() && expr2.getType() instanceof CTimeType) {
			expr2 = expr2.getType().makeWrappedAccess(expr2);
			expr2.resolve(null);
		}
		return true;
	}

	public void getExprByStruct(Struct cas) {
		if( cas.isPizzaCase() ) {
			if( !(op==BinaryOperator.Equals || op==BinaryOperator.NotEquals) )
				throw new CompilerException(this,"Undefined operation "+op.image+" on cased class");
//			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
//			expr2 = new ConstIntExpr(ca.caseno);
			MetaPizzaCase meta = cas.getMetaPizzaCase();
			expr2 = new ConstIntExpr(meta.getTag());
			expr2.resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp instanceof CTimeType) {
				tp.makeWrappedAccess(expr1);
				expr1.resolve(null);
				tp = expr1.getType();
			}
			if( !tp.isPizzaCase() && !tp.isHasCases() )
				throw new RuntimeException("Compare non-cased class "+tp+" with class's case "+cas);
			Method m = tp.getStruct().resolveMethod(nameGetCaseTag,Type.tpInt);
			expr1 = new CallExpr(expr1.pos,~expr1,m,ENode.emptyArray);
			expr1.resolve(Type.tpInt);
		} else {
			throw new CompilerException(this,"Class "+cas+" is not a cased class");
		}
	}

	private static KString clazzType = KString.from("kiev.vlang.Type");
	
	public void resolve(Type reqType) {
		if( isResolved() ) return;
		resolveExprs();
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==BinaryOperator.LessThen
			||   op==BinaryOperator.LessEquals
			||   op==BinaryOperator.GreaterThen
			||   op==BinaryOperator.GreaterEquals
			)
		) {
			this.resolve2(reqType);
			return;
		}
		else if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanOrExpr(pos,~expr1,~expr2));
				return;
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanAndExpr(pos,~expr1,~expr2));
				return;
			}
		}
		else if(
			(	(et1.isNumber() && et2.isNumber())
			 || (et1.isReference() && et2.isReference())
			 || (et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean))
			 || (et1.isEnum() && et2.isIntegerInCode())
			 || (et1.isIntegerInCode() && et2.isEnum())
			 || (et1.isEnum() && et2.isEnum() && et1 ≡ et2)
			) &&
			(   op==BinaryOperator.Equals
			||  op==BinaryOperator.NotEquals
			)
		) {
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) ) {
				if( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{~expr1,~expr2}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,expr1,opt.method,new ENode[]{~expr2}));
				return;
			}
		}
		throw new CompilerException(this,"Unresolved expression "+this);
	}
	
	private void resolve2(Type reqType) {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( t1 ≉ t2 ) {
			if( t1.isReference() != t2.isReference()) {
				if (t1.isEnum() && !t1.isIntegerInCode()) {
					expr1 = new CastExpr(expr1.pos,Type.tpInt,~expr1);
					expr1.resolve(Type.tpInt);
					t1 = expr1.getType();
				}
				if (t2.isEnum() && !t2.isIntegerInCode()) {
					expr2 = new CastExpr(expr2.pos,Type.tpInt,~expr2);
					expr2.resolve(Type.tpInt);
					t2 = expr2.getType();
				}
				if( t1.isReference() != t2.isReference() && t1.isIntegerInCode() != t2.isIntegerInCode())
					throw new CompilerException(this,"Boolean operator on reference and non-reference types");
			}
			if( !t1.isReference() && !t2.isReference()) {
				Type t;
				if      (t1 ≡ Type.tpDouble || t2 ≡ Type.tpDouble ) t = Type.tpDouble;
				else if (t1 ≡ Type.tpFloat  || t2 ≡ Type.tpFloat  ) t = Type.tpFloat;
				else if (t1 ≡ Type.tpLong   || t2 ≡ Type.tpLong   ) t = Type.tpLong;
				else t = Type.tpInt;

				if( t ≢ t1 && t1.isCastableTo(t) ) {
					expr1 = new CastExpr(pos,t,~expr1);
					expr1.resolve(t);
				}
				if( t ≢ t2 && t2.isCastableTo(t) ) {
					expr2 = new CastExpr(pos,t,~expr2);
					expr2.resolve(t);
				}
			}
			if( t1.isReference() && t2.isReference()) {
				if (t1 ≢ Type.tpNull && t2 ≢ Type.tpNull) {
					if (!t1.isInstanceOf(t2) && !t2.isInstanceOf(t1))
						Kiev.reportWarning(this, "Operation "+op+" on uncomparable types "+t1+" and "+t2);
					if (t1.getStruct() != null && t1.getStruct().isStructView())
						Kiev.reportWarning(this, "Operation "+op+" on a view type "+t1);
					if (t2.getStruct() != null && t2.getStruct().isStructView())
						Kiev.reportWarning(this, "Operation "+op+" on a view type "+t2);
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
	@virtual typedef VView = InstanceofExprView;
	@virtual typedef JView = JInstanceofExpr;

	@nodeimpl
	public static class InstanceofExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = InstanceofExpr;
		@att public ENode		expr;
		@att public TypeRef		type;
	}
	@nodeview
	public static view InstanceofExprView of InstanceofExprImpl extends BoolExprView {
		public ENode	expr;
		public TypeRef	type;

		public Operator getOp() { return BinaryOperator.InstanceOf; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	
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
		if( isResolved() ) return;
		expr.resolve(null);
		Type tp = null;
		if( expr instanceof TypeRef )
			tp = ((TypeRef)expr).getType();
		if( tp != null ) {
			replaceWithNode(new ConstBoolExpr(tp.isInstanceOf(type.getType())));
			return;
		} else {
			Type et = expr.getType();
			if (!expr.isForWrapper() && et instanceof CTimeType) {
				expr = et.makeWrappedAccess(expr);
				expr.setForWrapper(true);
				expr.resolve(null);
			}
		}
		tp = type.getType();
		if( !expr.getType().isCastableTo(tp) ) {
			throw new CompilerException(this,"Type "+expr.getType()+" is not castable to "+type);
		}
		if (expr.getType().isInstanceOf(tp)) {
			replaceWithNodeResolve(reqType,
				new BinaryBoolExpr(pos, BinaryOperator.NotEquals,~expr,new ConstNullExpr()));
			return;
		}
		if (tp instanceof WrapperType)
			tp = tp.getUnwrappedType();
		if (tp instanceof CompaundType) {
			CompaundType bt = (CompaundType)tp;
			if (tp.clazz.isTypeUnerasable()) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,
						ctx_clazz.getRView().accessTypeInfoField(this,type.getType(), false),
						Type.tpTypeInfo.clazz.resolveMethod(KString.from("$instanceof"),Type.tpBoolean,Type.tpObject),
						new ENode[]{~expr}
						)
					);
				return;
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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
	@virtual typedef VView = BooleanNotExprView;
	@virtual typedef JView = JBooleanNotExpr;

	@nodeimpl
	public static class BooleanNotExprImpl extends BoolExprImpl {
		@virtual typedef ImplOf = BooleanNotExpr;
		@att public ENode		expr;
	}
	@nodeview
	public static view BooleanNotExprView of BooleanNotExprImpl extends BoolExprView {
		public ENode		expr;

		public Operator getOp() { return PrefixOperator.BooleanNot; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	
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
		if( isResolved() ) return;
		expr.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr);
		if( expr.isConstantExpr() ) {
			replaceWithNode(new ConstBoolExpr(!((Boolean)expr.getConstValue()).booleanValue()));
			return;
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
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

