package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JENode;
import kiev.be.java.JCallExpr;
import kiev.be.java.JClosureCallExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@nodeset
public class CallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = CallExprImpl;
	@virtual typedef VView = CallExprView;
	@virtual typedef JView = JCallExpr;

	@nodeimpl
	public static class CallExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = CallExpr;
		@att public ENode				obj;
		@ref public Method				func;
		@ref public CallType			mt;
		@att public NArr<ENode>			args;
		@att public ENode				temp_expr;
		public CallExprImpl() {}
		public CallExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CallExprView of CallExprImpl extends ENodeView {
		public				ENode			obj;
		public				Method			func;
		public				CallType		mt;
		public access:ro	NArr<ENode>		args;
		public				ENode			temp_expr;

		public int		getPriority() { return Constants.opCallPriority; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public CallExpr() {
		super(new CallExprImpl());
	}

	public CallExpr(int pos, ENode obj, Method func, CallType mt, ENode[] args, boolean super_flag) {
		super(new CallExprImpl(pos));
		if (obj == null) {
			if !(func.isStatic() || func instanceof Constructor) {
				throw new RuntimeException("Call to non-static method "+func+" without accessor");
			}
			this.obj = new TypeRef(((Struct)func.parent).ctype);
		} else {
			this.obj = obj;
		}
		this.func = func;
		this.mt = mt;
		this.args.addAll(args);
		if (super_flag)
			this.setSuperExpr(true);
	}

	public CallExpr(int pos, ENode obj, Method func, CallType mt, ENode[] args) {
		this(pos, obj, func, mt, args, false);
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, func, null, args, false);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( obj.getPriority() > opAccessPriority )
			sb.append('(').append(obj).append(").");
		else
			sb.append(obj).append('.');
		sb.append(func.name).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		if (mt == null)
			return Type.getRealType(obj.getType(),func.type.ret());
		else
			return mt.ret();
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if (func.isStatic() && !(obj instanceof TypeRef))
			this.obj = new TypeRef(obj.getType());
		obj.resolve(null);
		func.makeArgs(args, reqType);
		if( func.name.equals(nameInit) && func.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null) {
			Method mmm = ctx_method;
			Type tp = mmm.ctx_clazz != func.ctx_clazz ? ctx_clazz.super_type : ctx_clazz.ctype;
			assert(ctx_method.name.equals(nameInit));
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null)
				temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO));
			else
				temp_expr = ctx_clazz.accessTypeInfoField(this,tp,false);
			temp_expr.resolve(null);
			temp_expr = null;
		}
		if (func.isVarArgs()) {
			int i=0;
			for(; i < func.type.arity; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
			if (args.length == i+1 && args[i].getType().isInstanceOf(func.getVarArgParam().type)) {
				// array as va_arg
				args[i].resolve(func.getVarArgParam().type);
			} else {
				ArrayType varg_tp = (ArrayType)Type.getRealType(obj.getType(),func.getVarArgParam().type);
				for(; i < args.length; i++)
					args[i].resolve(varg_tp.arg);
			}
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.arg(i)));
		}
		if (func.isTypeUnerasable()) {
			TypeDef[] targs = func.targs.toArray();
			for (int i=0; i < targs.length; i++) {
				Type tp = mt.resolve(targs[i].getAType());
				temp_expr = ctx_clazz.accessTypeInfoField(this,tp,false);
				temp_expr.resolve(null);
			}
			temp_expr = null;
		}
		if !(func.parent instanceof Struct) {
			ASTNode n = func.parent;
			while !(n instanceof Method) n = n.parent;
			assert (n.parent instanceof Struct);
			func = (Method)n;
		}
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		if( func.getName().equals(nameInit) ) {
			if( isSuperExpr() )
				dmp.append(nameSuper);
			else
				dmp.append(nameThis);
		} else {
			if( obj != null ) {
				if( obj.getPriority() < opCallPriority ) {
					dmp.append('(').append(obj).append(").");
				} else {
					dmp.append(obj).append('.');
				}
			}
			else if( isSuperExpr() )
				dmp.append("super.");
			else if( func instanceof Method && ((Method)func).isStatic() )
				dmp.append(((Struct)((Method)func).parent).name).append('.');
			dmp.append(func.getName());
		}
		dmp.append('(');
		for(int i=0; i < args.length; i++) {
			dmp.append(args[i]);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		return dmp;
	}
}

@nodeset
public class ClosureCallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		expr;
	@dflow(in="expr", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = ClosureCallExprImpl;
	@virtual typedef VView = ClosureCallExprView;
	@virtual typedef JView = JClosureCallExpr;

	@nodeimpl
	public static class ClosureCallExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = ClosureCallExpr;
		@att public ENode				expr;
		@att public NArr<ENode>			args;
		@att public boolean				is_a_call;
		public ClosureCallExprImpl() {}
		public ClosureCallExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ClosureCallExprView of ClosureCallExprImpl extends ENodeView {
		public				ENode			expr;
		public access:ro	NArr<ENode>		args;
		public				boolean			is_a_call;

		public int		getPriority() { return Constants.opCallPriority; }
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ClosureCallExpr() {
		super(new ClosureCallExprImpl());
	}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		super(new ClosureCallExprImpl(pos));
		this.expr = expr;
		foreach(ENode e; args) this.args.append(e);
		Type tp = expr.getType();
		if (tp instanceof CallType)
			is_a_call = tp.arity==args.length;
		else
			is_a_call = true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(expr).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		CallType t = (CallType)expr.getType();
		if( is_a_call )
			return t.ret();
		Type[] types = new Type[t.arity - args.length];
		for(int i=0; i < types.length; i++) types[i] = t.arg(i+args.length);
		t = new CallType(types,t.ret(),true);
		return t;
	}

	public Method getCallIt(CallType tp) {
		KString call_it_name;
		Type ret;
		if( tp.ret().isReference() ) {
			call_it_name = KString.from("call_Object");
			ret = Type.tpObject;
		} else {
			call_it_name = KString.from("call_"+tp.ret());
			ret = tp.ret();
		}
		return Type.tpClosureClazz.resolveMethod(call_it_name, ret);
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		expr.resolve(null);
		Type extp = expr.getType();
		if !(extp instanceof CallType)
			throw new CompilerException(expr,"Expression "+expr+" is not a closure");
		CallType tp = (CallType)extp;
		if( reqType != null && reqType instanceof CallType )
			is_a_call = false;
		else if( (reqType == null || !(reqType instanceof CallType)) && tp.arity==args.length )
			is_a_call = true;
		else
			is_a_call = false;
		for(int i=0; i < args.length; i++)
			args[i].resolve(tp.arg(i));
		//clone_it = tp.clazz.resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
		Method call_it = getCallIt(tp);
		//if( call_it.type.ret == Type.tpRule ) {
		//	env_access = new ConstNullExpr();
		//} else {
		//	trace(Kiev.debugResolve,"ClosureCallExpr "+this+" is not a rule call");
		//}
		setResolved(true);
	}

	public Dumper toJava(Dumper dmp) {
		expr.toJava(dmp).append(".clone()");
		for(int i=0; i < args.length; i++) {
			dmp.append(".addArg(");
			args[i].toJava(dmp);
			dmp.append(')');
		}
		if( is_a_call ) {
			Method call_it = getCallIt((CallType)expr.getType());
			dmp.append('.').append(call_it.name).append('(');
			if( call_it.type.ret() ≡ Type.tpRule ) dmp.append("null");
			dmp.append(')');
		}
		return dmp;
	}
}
