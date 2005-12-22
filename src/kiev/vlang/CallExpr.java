package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Operator.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JCallExprView;
import kiev.be.java.JClosureCallExprView;

import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@node
public class CallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@node
	public static class CallExprImpl extends ENodeImpl {
		@att public ENode				obj;
		@ref public Method				func;
		@att public NArr<ENode>			args;
		@att public ENode				temp_expr;
		public CallExprImpl() {}
		public CallExprImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view CallExprView of CallExprImpl extends ENodeView {
		public				ENode			obj;
		public				Method			func;
		public access:ro	NArr<ENode>		args;
		public				ENode			temp_expr;
	}
	
	@att public abstract virtual			ENode				obj;
	@ref public abstract virtual			Method				func;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			ENode				temp_expr;
	
	@getter public ENode			get$obj()				{ return this.getCallExprView().obj; }
	@getter public Method			get$func()				{ return this.getCallExprView().func; }
	@getter public NArr<ENode>		get$args()				{ return this.getCallExprView().args; }
	@getter public ENode			get$temp_expr()			{ return this.getCallExprView().temp_expr; }
	
	@setter public void		set$obj(ENode val)				{ this.getCallExprView().obj = val; }
	@setter public void		set$func(Method val)			{ this.getCallExprView().func = val; }
	@setter public void		set$temp_expr(ENode val)		{ this.getCallExprView().temp_expr = val; }

	public NodeView				getNodeView()		{ return new CallExprView((CallExprImpl)this.$v_impl); }
	public ENodeView			getENodeView()		{ return new CallExprView((CallExprImpl)this.$v_impl); }
	public CallExprView			getCallExprView()	{ return new CallExprView((CallExprImpl)this.$v_impl); }
	public JNodeView			getJNodeView()		{ return new JCallExprView((CallExprImpl)this.$v_impl); }
	public JENodeView			getJENodeView()		{ return new JCallExprView((CallExprImpl)this.$v_impl); }
	public JCallExprView		getJCallExprView()	{ return new JCallExprView((CallExprImpl)this.$v_impl); }
	

	public CallExpr() {
		super(new CallExprImpl());
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args, boolean super_flag) {
		super(new CallExprImpl(pos));
		if (obj == null) {
			if !(func.isStatic() || func instanceof Constructor) {
				throw new RuntimeException("Call to non-static method "+func+" without accessor");
			}
			this.obj = new TypeRef(((Struct)func.parent).type);
		} else {
			this.obj = obj;
		}
		this.func = func;
		this.args.addAll(args);
		if (super_flag)
			this.setSuperExpr(true);
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, func, args, false);
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
		return Type.getRealType(obj.getType(),func.type.ret);
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if (func.isStatic() && !(obj instanceof TypeRef))
			this.obj = new TypeRef(obj.getType());
		obj.resolve(null);
		func.makeArgs(args, reqType);
		if( func.name.equals(nameInit) && func.getTypeInfoParam() != null) {
			Method mmm = ctx_method;
			Type tp = mmm.ctx_clazz != func.ctx_clazz ? ctx_clazz.super_type : ctx_clazz.type;
			assert(ctx_method.name.equals(nameInit));
			assert(tp.args.length > 0);
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam() != null)
				temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam());
			else
				temp_expr = ctx_clazz.accessTypeInfoField(this,tp);
			temp_expr.resolve(null);
			temp_expr = null;
		}
		if (func.isVarArgs()) {
			int i=0;
			for(; i < func.type.args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.args[i]));
			Type varg_tp = Type.getRealType(obj.getType(),func.getVarArgParam().type);
			assert(varg_tp.isArray());
			for(; i < args.length; i++)
				args[i].resolve(varg_tp.args[0]);
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.args[i]));
		}
		if !(func.parent instanceof Struct) {
			ASTNode n = func.parent;
			while !(n instanceof Method) n = n.parent;
			assert (n.parent instanceof Struct);
			func = (Method)n;
		}
		setResolved(true);
	}

	public int		getPriority() { return Constants.opCallPriority; }

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

@node
public class ClosureCallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		expr;
	@dflow(in="expr", seq="true")		ENode[]		args;
	}
	
	@node
	public static class ClosureCallExprImpl extends ENodeImpl {
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
	}
	
	@att public abstract virtual			ENode				expr;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			boolean				is_a_call;
	
	@getter public ENode			get$expr()				{ return this.getClosureCallExprView().expr; }
	@getter public NArr<ENode>		get$args()				{ return this.getClosureCallExprView().args; }
	@getter public boolean			get$is_a_call()			{ return this.getClosureCallExprView().is_a_call; }
	
	@setter public void		set$expr(ENode val)				{ this.getClosureCallExprView().expr = val; }
	@setter public void		set$is_a_call(boolean val)		{ this.getClosureCallExprView().is_a_call = val; }

	public NodeView					getNodeView()				{ return new ClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new ClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	public ClosureCallExprView		getClosureCallExprView()	{ return new ClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	public JClosureCallExprView		getJClosureCallExprView()	{ return new JClosureCallExprView((ClosureCallExprImpl)this.$v_impl); }
	
	public ClosureCallExpr() {
		super(new ClosureCallExprImpl());
	}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		super(new ClosureCallExprImpl(pos));
		this.expr = expr;
		foreach(ENode e; args) this.args.append(e);
		Type tp = expr.getType();
		if (tp instanceof ClosureType)
			is_a_call = tp.args.length==args.length;
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
		ClosureType t = (ClosureType)expr.getType();
		if( is_a_call )
			return t.ret;
		Type[] types = new Type[t.args.length - args.length];
		for(int i=0; i < types.length; i++) types[i] = t.args[i+args.length];
		t = ClosureType.newClosureType(null,types,t.ret);
		return t;
	}

	public Method getCallIt(ClosureType tp) {
		KString call_it_name;
		KString call_it_sign;
		if( tp.ret.isReference() ) {
			call_it_name = KString.from("call_Object");
			call_it_sign = KString.from("()"+Type.tpObject.signature);
		} else {
			call_it_name = KString.from("call_"+tp.ret);
			call_it_sign = KString.from("()"+tp.ret.signature);
		}
		Method call_it = Type.tpClosureClazz.resolveMethod(call_it_name, call_it_sign, false);
		if( call_it == null )
			throw new CompilerException(this,"Can't resolve method "+call_it_name+call_it_sign+" in "+tp);
		return call_it;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		expr.resolve(null);
		Type extp = expr.getType();
		if !(extp instanceof ClosureType)
			throw new CompilerException(expr,"Expression "+expr+" is not a closure");
		ClosureType tp = (ClosureType)extp;
		if( reqType != null && reqType instanceof CallableType )
			is_a_call = false;
		else if( (reqType == null || !(reqType instanceof CallableType)) && tp.args.length==args.length )
			is_a_call = true;
		else
			is_a_call = false;
		for(int i=0; i < args.length; i++)
			args[i].resolve(tp.args[i]);
		//clone_it = tp.clazz.resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
		Method call_it = getCallIt(tp);
		//if( call_it.type.ret == Type.tpRule ) {
		//	env_access = new ConstNullExpr();
		//} else {
		//	trace(Kiev.debugResolve,"ClosureCallExpr "+this+" is not a rule call");
		//}
		setResolved(true);
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public Dumper toJava(Dumper dmp) {
		expr.toJava(dmp).append(".clone()");
		for(int i=0; i < args.length; i++) {
			dmp.append(".addArg(");
			args[i].toJava(dmp);
			dmp.append(')');
		}
		if( is_a_call ) {
			Method call_it = getCallIt((ClosureType)expr.getType());
			dmp.append('.').append(call_it.name).append('(');
			if( call_it.type.ret == Type.tpRule ) dmp.append("null");
			dmp.append(')');
		}
		return dmp;
	}
}
