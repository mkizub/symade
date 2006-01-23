package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JMethodView;
import kiev.be.java.JInitializerView;
import kiev.be.java.JWBCConditionView;

import kiev.be.java.CodeAttr;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class Method extends DNode implements Named,Typed,ScopeOfNames,ScopeOfMethods,SetBody,Accessable,PreScanneable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in")	BlockStat		body;
	@dflow(in="this:in")	WBCCondition[] 	conditions;
	}

	@node
	public static class MethodImpl extends DNodeImpl {
		public MethodImpl() {}
		public MethodImpl(int pos) { super(pos); }
		public MethodImpl(int pos, int fl) { super(pos, fl); }

		public final Method getMethod() { return (Method)this._self; }
		
		     public Access				acc;
		     public NodeName			name;
		     CallTypeProvider			meta_type;
		@att public NArr<TypeDef>		targs;
		@att public TypeRef				type_ret;
		@att public TypeRef				dtype_ret;
		@att public NArr<FormPar>		params;
		@att public NArr<ASTAlias>		aliases;
		@att public Var					retvar;
		@att public BlockStat			body;
		@att public PrescannedBody 		pbody;
		public kiev.be.java.Attr[]		attrs = kiev.be.java.Attr.emptyArray;
		@att public NArr<WBCCondition> 	conditions;
		@ref public NArr<Field>			violated_fields;
		@att public MetaValue			annotation_default;
		     public boolean				inlined_by_dispatcher;
		     public boolean				invalid_types;

		public virtual						CallType		type;
		public virtual						CallType		dtype;
		public virtual abstract access:ro	CallType		etype;

		public void callbackChildChanged(AttrSlot attr) {
			if (parent != null && pslot != null) {
				if      (attr.name == "params") {
					parent.callbackChildChanged(pslot);
				}
				else if (attr.name == "conditions")
					parent.callbackChildChanged(pslot);
				else if (attr.name == "annotation_default")
					parent.callbackChildChanged(pslot);
			}
			if (attr.name == "params" || attr.name == "flags")
				invalid_types = true;
		}

		@getter public final CallType				get$type()	{ checkRebuildTypes(); return this.type; }
		@getter public final CallType				get$dtype()	{ checkRebuildTypes(); return this.dtype; }
		@getter public final CallType				get$etype()	{ checkRebuildTypes(); return (CallType)this.dtype.getErasedType(); }

		public final void checkRebuildTypes() {
			if (invalid_types) rebuildTypes();
		}
	
		final void rebuildTypes() {
			TVarBld type_set = new TVarBld();
			TVarBld dtype_set = new TVarBld();
			if (targs.length > 0) {
				foreach (TypeDef td; targs) {
					type_set.append(td.getAType(), null);
					dtype_set.append(td.getAType(), null);
				}
			}
			if (!is_static && !is_mth_virtual_static) {
				type_set.append(getMethod().ctx_clazz.ctype.bindings());
				dtype_set.append(getMethod().ctx_clazz.ctype.bindings());
			}
			Vector<Type> args = new Vector<Type>();
			Vector<Type> dargs = new Vector<Type>();
			foreach (FormPar fp; params) {
				switch (fp.kind) {
				case FormPar.PARAM_NORMAL:
					args.append(fp.type);
					dargs.append(fp.dtype);
					break;
				case FormPar.PARAM_OUTER_THIS:
					assert(this instanceof Constructor.ConstructorImpl);
					assert(!this.getMethod().isStatic());
					assert(fp.isForward());
					assert(fp.isFinal());
					assert(fp.name.name == nameThisDollar);
					assert(fp.type ≈ this.getMethod().ctx_clazz.package_clazz.ctype);
					dargs.append(this.getMethod().ctx_clazz.package_clazz.ctype);
					break;
				case FormPar.PARAM_RULE_ENV:
					assert(this instanceof RuleMethod.RuleMethodImpl);
					assert(fp.isForward());
					assert(fp.isFinal());
					assert(fp.type ≡ Type.tpRule);
					assert(fp.name.name == namePEnv);
					dargs.append(Type.tpRule);
					break;
				case FormPar.PARAM_TYPEINFO:
					assert(this instanceof Constructor.ConstructorImpl || (this.getMethod().isStatic() && this.name.equals(nameNewOp)));
					assert(fp.isFinal());
					assert(fp.stype == null || fp.stype.getType() ≈ fp.vtype.getType());
					dargs.append(fp.type);
					break;
				case FormPar.PARAM_VARARGS:
					//assert(fp.isFinal());
					assert(fp.type.isArray());
					dargs.append(fp.type);
					break;
				case FormPar.PARAM_LVAR_PROXY:
					assert(this instanceof Constructor.ConstructorImpl);
					assert(fp.isFinal());
					dargs.append(fp.type);
					break;
				default:
					if (fp.kind >= FormPar.PARAM_TYPEINFO_N && fp.kind < FormPar.PARAM_TYPEINFO_N+128) {
						assert(this.is_type_unerasable);
						assert(fp.isFinal());
						assert(fp.type ≈ Type.tpTypeInfo);
						dargs.append(fp.type);
						break;
					}
					throw new CompilerException(fp, "Unknown kind of the formal parameter "+fp);
				}
			}
			this.type = new CallType(type_set, args.toArray(), type_ret.getType(), false);
			this.dtype = new CallType(dtype_set, dargs.toArray(), dtype_ret.getType(), false);
			invalid_types = false;
		}
		
	}
	@nodeview
	public static view MethodView of MethodImpl extends DNodeView {

		public final void checkRebuildTypes() {
			this.$view.checkRebuildTypes();
		}
	
		public				Access				acc;
		public				NodeName			name;
		public				CallTypeProvider	meta_type;
		public access:ro	NArr<TypeDef>		targs;
		public				TypeRef				type_ret;
		public				TypeRef				dtype_ret;
		public access:ro	CallType			type;
		public access:ro	CallType			dtype;
		public access:ro	CallType			etype;
		public access:ro	NArr<FormPar>		params;
		public access:ro	NArr<ASTAlias>		aliases;
		public				Var					retvar;
		public				BlockStat			body;
		public				PrescannedBody		pbody;
		public access:ro	NArr<WBCCondition>	conditions;
		public access:ro	NArr<Field>			violated_fields;
		public				MetaValue			annotation_default;
		public				boolean				inlined_by_dispatcher;
		public				boolean				invalid_types;

		@setter public final void set$acc(Access val)	{ this.$view.acc = val; Access.verifyDecl((Method)getDNode()); }

		// virtual static method	
		public final boolean isVirtualStatic() {
			return this.$view.is_mth_virtual_static;
		}
		public final void setVirtualStatic(boolean on) {
			if (this.$view.is_mth_virtual_static != on) {
				this.$view.is_mth_virtual_static = on;
				if (!isStatic()) this.setStatic(true);
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// method with variable number of arguments	
		public final boolean isVarArgs() {
			return this.$view.is_mth_varargs;
		}
		public final void setVarArgs(boolean on) {
			if (this.$view.is_mth_varargs != on) {
				this.$view.is_mth_varargs = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// logic rule method
		public final boolean isRuleMethod() {
			return this.$view instanceof RuleMethod.RuleMethodImpl;
		}
		// method with attached operator	
		public final boolean isOperatorMethod() {
			return this.$view.is_mth_operator;
		}
		public final void setOperatorMethod(boolean on) {
			if (this.$view.is_mth_operator != on) {
				this.$view.is_mth_operator = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// need fields initialization	
		public final boolean isNeedFieldInits() {
			return this.$view.is_mth_need_fields_init;
		}
		public final void setNeedFieldInits(boolean on) {
			if (this.$view.is_mth_need_fields_init != on) {
				this.$view.is_mth_need_fields_init = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a method generated as invariant	
		public final boolean isInvariantMethod() {
			return this.$view.is_mth_invariant;
		}
		public final void setInvariantMethod(boolean on) {
			if (this.$view.is_mth_invariant != on) {
				this.$view.is_mth_invariant = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a local method (closure code or inner method)	
		public final boolean isLocalMethod() {
			return this.$view.is_mth_local;
		}
		public final void setLocalMethod(boolean on) {
			if (this.$view.is_mth_local != on) {
				this.$view.is_mth_local = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// a dispatcher (for multimethods)	
		public final boolean isDispatcherMethod() {
			return this.$view.is_mth_dispatcher;
		}
		public final void setDispatcherMethod(boolean on) {
			if (this.$view.is_mth_dispatcher != on) {
				this.$view.is_mth_dispatcher = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new MethodView((MethodImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new MethodView((MethodImpl)this.$v_impl); }
	public MethodView		getMethodView()		alias operator(210,fy,$cast) { return new MethodView((MethodImpl)this.$v_impl); }

	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JMethodView((MethodImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JMethodView((MethodImpl)this.$v_impl); }
	public JMethodView		getJMethodView()	alias operator(210,fy,$cast) { return new JMethodView((MethodImpl)this.$v_impl); }

	public static Method[]	emptyArray = new Method[0];

	@getter public Access				get$acc()					{ return this.getMethodView().acc; }
	@getter public NodeName				get$name()					{ return this.getMethodView().name; }
	@getter public CallTypeProvider		get$meta_type()				{ return this.getMethodView().meta_type; }
	@getter public NArr<TypeDef>		get$targs()					{ return this.getMethodView().targs; }
	@getter public TypeRef				get$type_ret()				{ return this.getMethodView().type_ret; }
	@getter public TypeRef				get$dtype_ret()				{ return this.getMethodView().dtype_ret; }
	@getter public NArr<FormPar>		get$params()				{ return this.getMethodView().params; }
	@getter public NArr<ASTAlias>		get$aliases()				{ return this.getMethodView().aliases; }
	@getter public Var					get$retvar()				{ return this.getMethodView().retvar; }
	@getter public BlockStat			get$body()					{ return this.getMethodView().body; }
	@getter public PrescannedBody		get$pbody()					{ return this.getMethodView().pbody; }
	@getter public NArr<WBCCondition>	get$conditions()			{ return this.getMethodView().conditions; }
	@getter public NArr<Field>			get$violated_fields()		{ return this.getMethodView().violated_fields; }
	@getter public MetaValue			get$annotation_default()	{ return this.getMethodView().annotation_default; }
	@getter public boolean				get$inlined_by_dispatcher()	{ return this.getMethodView().inlined_by_dispatcher; }
	@getter        boolean				get$invalid_types()			{ return this.getMethodView().invalid_types; }

	@getter public CallType				get$type()	{ return this.getMethodView().type; }
	@getter public CallType				get$dtype()	{ return this.getMethodView().dtype; }
	@getter public CallType				get$etype()	{ return this.getMethodView().etype; }

	@setter public void set$acc(Access val)						{ this.getMethodView().acc = val; }
	@setter public void set$name(NodeName val)						{ this.getMethodView().name = val; }
	@setter public void set$meta_type(CallTypeProvider val)		{ this.getMethodView().meta_type = val; }
	@setter public void set$type_ret(TypeRef val)					{ this.getMethodView().type_ret = val; }
	@setter public void set$dtype_ret(TypeRef val)					{ this.getMethodView().dtype_ret = val; }
	@setter public void set$retvar(Var val)						{ this.getMethodView().retvar = val; }
	@setter public void set$body(BlockStat val)					{ this.getMethodView().body = val; }
	@setter public void set$pbody(PrescannedBody val)				{ this.getMethodView().pbody = val; }
	@setter public void set$annotation_default(MetaValue val)		{ this.getMethodView().annotation_default = val; }
	@setter public void set$inlined_by_dispatcher(boolean val)		{ this.getMethodView().inlined_by_dispatcher = val; }
	@setter        void set$invalid_types(boolean val)				{ this.getMethodView().invalid_types = val; }

	/** Method's access */
	     public abstract virtual			Access				acc;
	/** Name of the method */
	     public abstract virtual			NodeName			name;
	/** Return type of the method and signature (argument's types) */
	     public abstract virtual			CallTypeProvider	meta_type;
	@att public abstract virtual access:ro	NArr<TypeDef>		targs;
	@att public abstract virtual			TypeRef				type_ret;
	/** The type of the dispatcher method (if method is a multimethod) */
	@att public abstract virtual			TypeRef				dtype_ret;
	/** Parameters of this method */
	@att public abstract virtual access:ro	NArr<FormPar>		params;
	/** Name/operator aliases of this method */
    @att public abstract virtual access:ro	NArr<ASTAlias>		aliases;
	/** Return value of this method */
	@att public abstract virtual			Var					retvar;
	/** Body of the method */
	@att public abstract virtual			BlockStat			body;
	@att public abstract virtual			PrescannedBody 		pbody;
	/** Require & ensure clauses */
	@att public abstract virtual access:ro	NArr<WBCCondition> 	conditions;
	/** Violated by method fields for normal methods, and checked fields
	 *  for invariant method */
	@ref public abstract virtual access:ro	NArr<Field>			violated_fields;
	/** Default meta-value for annotation methods */
	@att public abstract virtual			MetaValue			annotation_default;
	/** Indicates that this method is inlined by dispatcher method */
	     public abstract virtual			boolean				inlined_by_dispatcher;
	            abstract virtual			boolean				invalid_types;
	
	     public virtual abstract access:ro	CallType			type; 
		 public virtual abstract access:ro	CallType			dtype; 
	     public virtual abstract access:ro	CallType			etype; 
	
	public Method() {
		super(new MethodImpl());
	}

	public Method(MethodImpl $view) {
		super($view);
	}

	public Method(MethodImpl $view, KString name, Type ret) {
		this($view,name,new TypeRef(ret));
	}

	public Method(KString name, Type ret, int fl) {
		this(name,new TypeRef(ret),fl);
		invalid_types = true;
	}
	public Method(KString name, TypeRef type_ret, int fl) {
		this(new MethodImpl(0,fl), name, type_ret);
	}
	public Method(MethodImpl $view, KString name, TypeRef type_ret) {
		super($view);
		assert ((name != nameInit && name != nameClassInit) || this instanceof Constructor);
		this.name = new NodeName(name);
		this.type_ret = type_ret;
		this.dtype_ret = (TypeRef)type_ret.copy();
		this.meta = new MetaSet();
		invalid_types = true;
	}

	@getter public Method get$child_ctx_method() { return this; }
	
	// virtual static method	
	public boolean isVirtualStatic() { return this.getMethodView().isVirtualStatic(); }
	public void setVirtualStatic(boolean on) { this.getMethodView().setVirtualStatic(on); }
	// method with variable number of arguments	
	public boolean isVarArgs() { return this.getMethodView().isVarArgs(); }
	public void setVarArgs(boolean on) { this.getMethodView().setVarArgs(on); }
	// logic rule method	
	public boolean isRuleMethod() { return this.getMethodView().isRuleMethod(); }
	// method with attached operator	
	public boolean isOperatorMethod() { return this.getMethodView().isOperatorMethod(); }
	public void setOperatorMethod(boolean on) { this.getMethodView().setOperatorMethod(on); }
	// need fields initialization	
	public boolean isNeedFieldInits() { return this.getMethodView().isNeedFieldInits(); }
	public void setNeedFieldInits(boolean on) { this.getMethodView().setNeedFieldInits(on); }
	// a method generated as invariant	
	public boolean isInvariantMethod() { return this.getMethodView().isInvariantMethod(); }
	public void setInvariantMethod(boolean on) { this.getMethodView().setInvariantMethod(on); }
	// a local method (closure code or inner method)	
	public boolean isLocalMethod() { return this.getMethodView().isLocalMethod(); }
	public void setLocalMethod(boolean on) { this.getMethodView().setLocalMethod(on); }
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod() { return this.getMethodView().isDispatcherMethod(); }
	public final void setDispatcherMethod(boolean on) { this.getMethodView().setDispatcherMethod(on); }

	public MetaThrows getMetaThrows() {
		return (MetaThrows)this.getNodeData(MetaThrows.ID);
	}

	public void checkRebuildTypes() {
		this.getMethodView().checkRebuildTypes();
	}
	
	public FormPar getOuterThisParam() {
		checkRebuildTypes();
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_OUTER_THIS)
			return fp;
		return null;
	}
	
	public FormPar getTypeInfoParam(int kind) {
		checkRebuildTypes();
		foreach (FormPar fp; params; fp.kind == kind)
			return fp;
		return null;
	}
	
	public FormPar getVarArgParam() {
		checkRebuildTypes();
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_VARARGS)
			return fp;
		return null;
	}
	
	public void addViolatedField(Field f) {
		if( isInvariantMethod() ) {
			f.invs.addUniq(this);
			if( ((Struct)parent).instanceOf((Struct)f.parent) )
				violated_fields.addUniq(f);
		} else {
			violated_fields.addUniq(f);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(name+"(");
		int n = params.length;
		boolean comma = false;
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_NORMAL) {
			if (comma) sb.append(",");
			sb.append(fp.vtype.toString());
			comma = true;
		}
		sb.append(")->").append(type_ret);
		return sb.toString();
	}

	public static String toString(KString nm, NArr<ENode> args) {
		return toString(nm,args.toArray(),null);
	}

	public static String toString(KString nm, ENode[] args) {
		return toString(nm,args,null);
	}

	public static String toString(KString nm, NArr<ENode> args, Type ret) {
		return toString(nm,args.toArray(),ret);
	}
	
	public static String toString(KString nm, ENode[] args, Type ret) {
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; args!=null && i < args.length; i++) {
			sb.append(args[i].getType().toString());
			if( i < (args.length-1) ) sb.append(",");
		}
		if( ret != null )
			sb.append(")->").append(ret);
		else
			sb.append(")->???");
		return sb.toString();
	}

	public static String toString(KString nm, CallType mt) {
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; i < mt.arity; i++) {
			sb.append(mt.arg(i).toString());
			if( i < (mt.arity-1) ) sb.append(",");
		}
		sb.append(")->").append(mt.ret());
		return sb.toString();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public Var	getRetVar() {
		if( retvar == null )
			retvar = new Var(pos,nameResultVar,type_ret.getType(),ACC_FINAL);
		return retvar;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public void makeArgs(NArr<ENode> args, Type t) {
		checkRebuildTypes();
		assert(args.getPSlot().is_attr);
		if( isVarArgs() ) {
			int i=0;
			for(; i < type.arity; i++) {
				Type ptp = Type.getRealType(t,type.arg(i));
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
			if (args.length == i+1 && args[i].getType().isInstanceOf(getVarArgParam().type)) {
				// array as va_arg
			} else {
				ArrayType varg_tp = (ArrayType)Type.getRealType(t,getVarArgParam().type);
				for(; i < args.length; i++) {
					if !(args[i].getType().isInstanceOf(varg_tp.arg)) {
						CastExpr.autoCastToReference(args[i]);
						CastExpr.autoCast(args[i],varg_tp.arg);
					}
				}
			}
		} else {
			for(int i=0; i < type.arity; i++) {
				Type ptp = Type.getRealType(t,type.arg(i));
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
		}
	}

	public boolean equalsByCast(KString name, CallType mt, Type tp, ResInfo info) {
		if (!this.name.equals(name)) return false;
		int type_len = this.type.arity;
		int args_len = mt.arity;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
		CallType rt = (CallType)this.type.bind(tp.bindings());
		
		if ((mt.bindings().tvars.length - mt.arity - 1) > 0) {
			TVarBld set = new TVarBld();
			int a = 0;
			foreach (TVar tv; mt.bindings().tvars) {
				if (tv.var.isHidden())
					continue;
				Type bound = tv.unalias().result();
				ArgType arg = targs[a].getAType();
				if!(bound.isInstanceOf(arg)) {
					trace(Kiev.debugResolve,"Type "+bound+" is not applayable to "+arg	+" for type arg "+a);
					return false;
				}
				set.append(arg, bound);
				a++;
			}
			rt = rt.rebind(set);
		}
		
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if (!mt.arg(i).isAutoCastableTo(rt.arg(i))) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.arg(i)+" not auto-castable to "+rt.arg(i));
				return false;
			}
		}
		boolean match = false;
		if (mt.ret() ≡ Type.tpAny)
			match = true;
		else if (rt.ret().isAutoCastableTo(mt.ret()))
			match = true;
		else
			match = false;
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+(match?" match":" do not match"));
		if (info != null && match)
			info.mt = rt;
		return match;
	}

	// TODO
	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(nameInit) )
			dmp.space().append(type.ret()).forsed_space().append(name);
		else
			dmp.space().append(((Struct)parent).name.short_name);
		dmp.append('(');
		for(int i=0; i < params.length; i++) {
			params[i].toJavaDecl(dmp,params[i].dtype);
			if( i < (params.length-1) ) dmp.append(",");
		}
		dmp.append(')').space();
		foreach(WBCCondition cond; conditions) 
			cond.toJava(dmp);
		if( isAbstract() || body == null ) {
			dmp.append(';').newLine();
		} else {
			dmp.append(body).newLine();
		}
		return dmp;
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		FormPar@ var;
	{
		inlined_by_dispatcher || path.space_prev.pslot.name == "targs",$cut,false
	;
		path.space_prev.pslot.name == "params" ||
		path.space_prev.pslot.name == "type_ref" ||
		path.space_prev.pslot.name == "dtype_ref",$cut,
		node @= targs,
		((TypeDef)node).name.name == name
	;
		var @= params,
		var.name.equals(name),
		node ?= var
	;
		node ?= retvar, ((Var)node).name.equals(name)
	;
		node @= targs,
		((TypeDef)node).name.name == name
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_clazz.ctype.resolveNameAccessR(node,path,name)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, CallType mt)
		Var@ n;
	{
		info.isForwardsAllowed(),
	{
		!this.isStatic(),
		info.enterForward(ThisExpr.thisPar) : info.leaveForward(ThisExpr.thisPar),
		this.ctx_clazz.ctype.resolveCallAccessR(node,info,name,mt)
	;
		n @= params,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,name,mt)
	}
	}

    public ASTNode pass3() {
		if !( parent instanceof Struct )
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = (Struct)parent;
		// TODO: check flags for methods
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic();
			if( pbody == null ) setAbstract(true);
		}

		if (clazz.isAnnotation() && params.length != 0) {
			Kiev.reportError(this, "Annotation methods may not have arguments");
			params.delAll();
			setVarArgs(false);
		}

		if (clazz.isAnnotation() && (body != null || pbody != null)) {
			Kiev.reportError(this, "Annotation methods may not have bodies");
			body = null;
			pbody = null;
		}

		if (isTypeUnerasable()) {
			int i = 0;
			foreach (TypeDef td; targs) {
				td.setTypeUnerasable(true);
				FormPar v = new FormPar(td.pos,KString.from(nameTypeInfo+"$"+td.name), Type.tpTypeInfo, FormPar.PARAM_TYPEINFO_N+i, ACC_FINAL);
				params.add(v);
			}
		}

		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype == null)
				fp.stype = new TypeRef(fp.vtype.pos,fp.vtype.getType());
			if (fp.meta != null)
				fp.meta.verify();
		}

		checkRebuildTypes();
		trace(Kiev.debugMultiMethod,"Method "+this+" has dispatcher type "+this.dtype);
		meta.verify();
		if (annotation_default != null)
			annotation_default.verify();
		foreach(ASTAlias al; aliases) al.attach(this);

		foreach(WBCCondition cond; conditions)
			cond.definer = this;

        return this;
    }

	public void resolveMetaDefaults() {
		if (annotation_default != null) {
			Type tp = this.type_ret.getType();
			Type t = tp;
			if (t.isArray()) {
				if (annotation_default instanceof MetaValueScalar) {
					MetaValueArray mva = new MetaValueArray(annotation_default.type);
					mva.values.add(((MetaValueScalar)annotation_default).value);
					annotation_default = mva;
				}
				t = ((ArrayType)t).arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (!(t ≈ Type.tpString || t ≈ Type.tpClass || t.isAnnotation() || t.isEnum()))
					throw new CompilerException(annotation_default, "Bad annotation value type "+tp);
			}
			annotation_default.resolve(t);
		}
	}
	
	static class MethodDFFunc extends DFFunc {
		final int res_idx;
		MethodDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Method m = (Method)dfi.node_impl.getNode();
			DFState in = DFState.makeNewState();
			for(int i=0; i < m.params.length; i++) {
				Var p = m.params[i];
				in = in.declNode(p);
			}
			res = in;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new MethodDFFunc(dfi);
	}

	public boolean preResolveIn(TransfProcessor proc) {
		checkRebuildTypes();
		return true;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		checkRebuildTypes();
		return true;
	}
	
	public boolean preVerify() {
		if (isAbstract() && isStatic()) {
			setBad(true);
			ctx_clazz.setBad(true);
			Kiev.reportError(this,"Static method cannot be declared abstract");
		}
		return true;
	}

	public void resolveDecl() {
		if( isResolved() ) return;
		trace(Kiev.debugResolve,"Resolving method "+this);
		assert( ctx_clazz == parent || inlined_by_dispatcher );
		try {
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire ) {
				cond.body.resolve(Type.tpVoid);
			}
			if( body != null ) {
				if (type.ret() ≡ Type.tpVoid)
					body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret() ≡ Type.tpVoid ) {
					if( body instanceof BlockStat ) {
						((BlockStat)body).stats.append(new ReturnStat(pos,null));
						body.setAbrupted(true);
					}
					else if !(isInvariantMethod())
						Kiev.reportError(this,"Return requared");
				} else {
					Kiev.reportError(this,"Return requared");
				}
			}
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondEnsure ) {
				if( type.ret() ≢ Type.tpVoid ) getRetVar();
				cond.resolve(Type.tpVoid);
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
		this.cleanDFlow();

		// Append invariants by list of violated/used fields
		if( !isInvariantMethod() ) {
			foreach(Field f; violated_fields; ctx_clazz.instanceOf((Struct)f.parent) ) {
				foreach(Method inv; f.invs; ctx_clazz.instanceOf((Struct)inv.parent) ) {
					assert(inv.isInvariantMethod(),"Non-invariant method in list of field's invariants");
					// check, that this is not set$/get$ method
					if( !(name.name.startsWith(nameSet) || name.name.startsWith(nameGet)) )
						conditions.addUniq(inv.conditions[0]);
				}
			}
		}
		
		setResolved(true);
	}

	public boolean setBody(ENode body) {
		trace(Kiev.debugMultiMethod,"Setting body of methods "+this);
		if (this.body == null) {
			this.body = (BlockStat)body;
		} else {
			throw new RuntimeException("Added body to method "+this+" which already have body");
		}

		return true;
	}

}

@node
public class Constructor extends Method {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]			addstats;
	@dflow(in="this:in")				BlockStat		body;
	@dflow(in="this:in")				WBCCondition[] 	conditions;
	}

	@node
	public static final class ConstructorImpl extends MethodImpl {
		@att public NArr<ENode>			addstats;
		public ConstructorImpl() {}
		public ConstructorImpl(int pos, int flags) { super(pos, flags); }
	}
	@nodeview
	public static final view ConstructorView of ConstructorImpl extends MethodView {
		public access:ro	NArr<ENode>			addstats;
	}

	@att public abstract virtual access:ro NArr<ENode>			addstats;
	
	public NodeView				getNodeView()			{ return new ConstructorView((ConstructorImpl)this.$v_impl); }
	public DNodeView			getDNodeView()			{ return new ConstructorView((ConstructorImpl)this.$v_impl); }
	public MethodView			getMethodView()			{ return new ConstructorView((ConstructorImpl)this.$v_impl); }
	public ConstructorView		getConstructorView()	{ return new ConstructorView((ConstructorImpl)this.$v_impl); }

	@getter public NArr<ENode>		get$addstats()		{ return this.getConstructorView().addstats; }

	public Constructor() {
		super(new ConstructorImpl());
	}

	public Constructor(int fl) {
		super(new ConstructorImpl(0, fl), (fl&ACC_STATIC)==0 ? nameInit:nameClassInit, Type.tpVoid);
	}

	public void resolveDecl() {
		super.resolveDecl();
		ENode[] addstats = this.addstats.delToArray();
		for(int i=0; i < addstats.length; i++) {
			body.stats.insert(addstats[i],i);
			trace(Kiev.debugResolve,"ENode added to constructor: "+addstats[i]);
		}
	}
}

@node
public class Initializer extends DNode implements SetBody, PreScanneable {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")				BlockStat		body;
	}

	@node
	public static final class InitializerImpl extends DNodeImpl {
		@att public BlockStat				body;
		@att public PrescannedBody			pbody;
		public InitializerImpl() {}
		public InitializerImpl(int pos, int flags) { super(pos, flags); }
	}
	@nodeview
	public static final view InitializerView of InitializerImpl extends DNodeView {
		public BlockStat				body;
		public PrescannedBody			pbody;
	}

	@att public abstract virtual BlockStat			body;
	@att public abstract virtual PrescannedBody	pbody;
	
	@getter public BlockStat		get$body()			{ return this.getInitializerView().body; }
	@getter public PrescannedBody	get$pbody()			{ return this.getInitializerView().pbody; }
	
	@setter public void		set$body(BlockStat val)				{ this.getInitializerView().body = val; }
	@setter public void		set$pbody(PrescannedBody val)		{ this.getInitializerView().pbody = val; }
	
	public NodeView				getNodeView()			{ return new InitializerView((InitializerImpl)this.$v_impl); }
	public DNodeView			getDNodeView()			{ return new InitializerView((InitializerImpl)this.$v_impl); }
	public InitializerView		getInitializerView()	{ return new InitializerView((InitializerImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JInitializerView((InitializerImpl)this.$v_impl); }
	public JDNodeView			getJDNodeView()			{ return new JInitializerView((InitializerImpl)this.$v_impl); }
	public JInitializerView		getJInitializerView()	{ return new JInitializerView((InitializerImpl)this.$v_impl); }

	public Initializer() {
		super(new InitializerImpl());
	}

	public Initializer(int pos, int flags) {
		super(new InitializerImpl(pos, flags));
	}

	public void resolveDecl() {
		if( isResolved() ) return;
		
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}

		setResolved(true);
	}

	public boolean setBody(ENode body) {
		trace(Kiev.debugMultiMethod,"Setting body of initializer "+this);
		if (this.body == null) {
			this.body = (BlockStat)body;
		}
		else {
			throw new RuntimeException("Added body to initializer "+this+" which already has body");
		}
		return true;
	}

}

public enum WBCType {
	CondUnknown,
	CondRequire,
	CondEnsure,
	CondInvariant;
}

@node
public class WBCCondition extends DNode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")			ENode		body;
	}
	
	@node
	public static final class WBCConditionImpl extends DNodeImpl {
		@att public WBCType				cond;
		@att public NameRef				name;
		@att public ENode				body;
		@ref public Method				definer;
		@att public CodeAttr			code_attr;
		public WBCConditionImpl() {}
		public WBCConditionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view WBCConditionView of WBCConditionImpl extends DNodeView {
		public WBCType				cond;
		public NameRef				name;
		public ENode				body;
		public Method				definer;
		public CodeAttr				code_attr;
	}

	@att public abstract virtual WBCType			cond;
	@att public abstract virtual NameRef			name;
	@att public abstract virtual ENode				body;
	@ref public abstract virtual Method			definer;
	@att public abstract virtual CodeAttr			code_attr;
	
	@getter public WBCType			get$cond()			{ return this.getWBCConditionView().cond; }
	@getter public NameRef			get$name()			{ return this.getWBCConditionView().name; }
	@getter public ENode			get$body()			{ return this.getWBCConditionView().body; }
	@getter public Method			get$definer()		{ return this.getWBCConditionView().definer; }
	@getter public CodeAttr			get$code_attr()		{ return this.getWBCConditionView().code_attr; }
	
	@setter public void		set$cond(WBCType val)				{ this.getWBCConditionView().cond = val; }
	@setter public void		set$name(NameRef val)				{ this.getWBCConditionView().name = val; }
	@setter public void		set$body(ENode val)					{ this.getWBCConditionView().body = val; }
	@setter public void		set$definer(Method val)				{ this.getWBCConditionView().definer = val; }
	@setter public void		set$code_attr(CodeAttr val)			{ this.getWBCConditionView().code_attr = val; }
	
	public NodeView				getNodeView()			{ return new WBCConditionView((WBCConditionImpl)this.$v_impl); }
	public DNodeView			getDNodeView()			{ return new WBCConditionView((WBCConditionImpl)this.$v_impl); }
	public WBCConditionView		getWBCConditionView()	{ return new WBCConditionView((WBCConditionImpl)this.$v_impl); }
	public JNodeView			getJNodeView()			{ return new JWBCConditionView((WBCConditionImpl)this.$v_impl); }
	public JDNodeView			getJDNodeView()			{ return new JWBCConditionView((WBCConditionImpl)this.$v_impl); }
	public JWBCConditionView	getJWBCConditionView()	{ return new JWBCConditionView((WBCConditionImpl)this.$v_impl); }

	public WBCCondition() {
		super(new WBCConditionImpl());
	}

	public WBCCondition(int pos, WBCType cond, KString name, ENode body) {
		super(new WBCConditionImpl(pos));
		if (name != null)
			this.name = new NameRef(pos, name);
		this.cond = cond;
		this.body = body;
	}

	public void resolve(Type reqType) {
		if( code_attr != null ) return;
		body.resolve(Type.tpVoid);
	}

	public boolean setBody(ENode body) {
		this.body = body;
		return true;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(body);
	}
}

