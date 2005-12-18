package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

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
		@att public TypeCallRef			type_ref;
		@att public TypeCallRef			dtype_ref;
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
	}
	@nodeview
	public static view MethodView of MethodImpl extends DNodeView {

		public final void checkRebuildTypes() {
			if (invalid_types) ((Method)this.$view._self).rebuildTypes();
		}
	
		public				Access				acc;
		public				NodeName			name;
		public				TypeCallRef			type_ref;
		public				TypeCallRef			dtype_ref;
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

		@getter public final MethodType				get$type()	{ checkRebuildTypes(); return type_ref.getMType(); }
		@getter public final MethodType				get$dtype()	{ checkRebuildTypes(); return dtype_ref.getMType(); }
		@getter public final MethodType				get$jtype()	{ return (MethodType)dtype.getJavaType(); }

		@setter public final void set$acc(Access val)	{ this.$view.acc = val; this.$view.acc.verifyAccessDecl(getDNode()); }

		// multimethod	
		public final boolean isMultiMethod() {
			return this.$view.is_mth_multimethod;
		}
		public final void setMultiMethod(boolean on) {
			if (this.$view.is_mth_multimethod != on) {
				this.$view.is_mth_multimethod = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// virtual static method	
		public final boolean isVirtualStatic() {
			return this.$view.is_mth_virtual_static;
		}
		public final void setVirtualStatic(boolean on) {
			if (this.$view.is_mth_virtual_static != on) {
				this.$view.is_mth_virtual_static = on;
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
			return this.$view.is_mth_rule;
		}
		public final void setRuleMethod(boolean on) {
			if (this.$view.is_mth_rule != on) {
				this.$view.is_mth_rule = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
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
	@getter public TypeCallRef			get$type_ref()				{ return this.getMethodView().type_ref; }
	@getter public TypeCallRef			get$dtype_ref()				{ return this.getMethodView().dtype_ref; }
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

	@getter public MethodType			get$type()	{ return this.getMethodView().type; }
	@getter public MethodType			get$dtype()	{ return this.getMethodView().dtype; }
	@getter public MethodType			get$jtype()	{ return this.getMethodView().jtype; }

	@setter public void set$acc(Access val)						{ this.getMethodView().acc = val; }
	@setter public void set$name(NodeName val)						{ this.getMethodView().name = val; }
	@setter public void set$type_ref(TypeCallRef val)				{ this.getMethodView().type_ref = val; }
	@setter public void set$dtype_ref(TypeCallRef val)				{ this.getMethodView().dtype_ref = val; }
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
	@att public abstract virtual			TypeCallRef			type_ref;
	/** The type of the dispatcher method (if method is a multimethod) */
	@att public abstract virtual			TypeCallRef			dtype_ref;
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
	
	     public virtual abstract access:ro	MethodType			type; 
	     public virtual abstract access:ro	MethodType			jtype; 
		 public virtual abstract access:ro	MethodType			dtype; 
	
	public Method() {
		super(new MethodImpl());
	}

	public Method(KString name, MethodType mt, int fl) {
		this(name,new TypeCallRef(mt),new TypeCallRef(mt),fl);
	}

	public Method(KString name, MethodType mt, MethodType dmt, int fl) {
		this(name,new TypeCallRef(mt),new TypeCallRef(dmt),fl);
		invalid_types = true;
	}
	public Method(KString name, TypeCallRef type_ref, TypeCallRef dtype_ref, int fl) {
		super(new MethodImpl(0,fl));
		assert ((name != nameInit && name != nameClassInit) || this instanceof Constructor);
		this.name = new NodeName(name);
		this.type_ref = type_ref;
		if (dtype_ref != null) {
			this.dtype_ref = dtype_ref;
		} else {
			this.dtype_ref = (TypeCallRef)type_ref.copy();
		}
		this.acc = new Access(0);
		this.meta = new MetaSet();
		invalid_types = true;
	}

	@getter public Method get$child_ctx_method() { return this; }
	
	// multimethod	
	public boolean isMultiMethod() { return this.getMethodView().isMultiMethod(); }
	public void setMultiMethod(boolean on) { this.getMethodView().setMultiMethod(on); }
	// virtual static method	
	public boolean isVirtualStatic() { return this.getMethodView().isVirtualStatic(); }
	public void setVirtualStatic(boolean on) { this.getMethodView().setVirtualStatic(on); }
	// method with variable number of arguments	
	public boolean isVarArgs() { return this.getMethodView().isVarArgs(); }
	public void setVarArgs(boolean on) { this.getMethodView().setVarArgs(on); }
	// logic rule method	
	public boolean isRuleMethod() { return this.getMethodView().isRuleMethod(); }
	public void setRuleMethod(boolean on) { this.getMethodView().setRuleMethod(on); }
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

	public MetaThrows getMetaThrows() {
		return (MetaThrows)this.meta.get(MetaThrows.NAME);
	}

	public void checkRebuildTypes() {
		if (invalid_types) rebuildTypes();
	}
	
	final void rebuildTypes() {
		type_ref.args.delAll();
		dtype_ref.args.delAll();
		foreach (FormPar fp; params) {
			switch (fp.kind) {
			case FormPar.PARAM_NORMAL:
				type_ref.args.add((TypeRef)fp.vtype.copy());
				if (fp.stype != null)
					dtype_ref.args.add((TypeRef)fp.stype.copy());
				else
					dtype_ref.args.add((TypeRef)fp.vtype.copy());
				break;
			case FormPar.PARAM_OUTER_THIS:
				assert(this instanceof Constructor);
				assert(!this.isStatic());
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.name.name == nameThisDollar);
				assert(fp.type == this.ctx_clazz.package_clazz.type);
				dtype_ref.args.add(new TypeRef(this.ctx_clazz.package_clazz.type));
				break;
			case FormPar.PARAM_RULE_ENV:
				assert(this instanceof RuleMethod);
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.type == Type.tpRule);
				assert(fp.name.name == namePEnv);
				dtype_ref.args.add(new TypeRef(Type.tpRule));
				break;
			case FormPar.PARAM_TYPEINFO:
				assert(this instanceof Constructor || (this.isStatic() && this.name.equals(nameNewOp)));
				assert(fp.isFinal());
				assert(fp.stype == null || fp.stype.getType() == fp.vtype.getType());
				dtype_ref.args.add((TypeRef)fp.vtype.copy());
				break;
			case FormPar.PARAM_VARARGS:
				assert(fp.isFinal());
				assert(fp.type.isArray());
				dtype_ref.args.add((TypeRef)fp.vtype.copy());
				break;
			case FormPar.PARAM_LVAR_PROXY:
				assert(this instanceof Constructor);
				assert(fp.isFinal());
				dtype_ref.args.add((TypeRef)fp.vtype.copy());
				break;
			default:
				throw new CompilerException(fp, "Unknown kind of the formal parameter "+fp);
			}
		}
		invalid_types = false;
	}
	
	public FormPar getOuterThisParam() {
		checkRebuildTypes();
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_OUTER_THIS)
			return fp;
		return null;
	}
	
	public FormPar getTypeInfoParam() {
		checkRebuildTypes();
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_TYPEINFO)
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
		int n = type_ref.args.length;
		for(int i=0; i < n; i++) {
			sb.append(type_ref.args[i].toString());
			if( i < (n-1) )
				sb.append(",");
		}
		sb.append(")->").append(type_ref.ret);
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

	public static String toString(KString nm, MethodType mt) {
		Type[] args = mt.args;
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; i < args.length; i++) {
			sb.append(args[i].toString());
			if( i < (args.length-1) ) sb.append(",");
		}
		sb.append(")->").append(mt.ret);
		return sb.toString();
	}

	public NodeName getName() { return name; }

	public Type	getType() { return type_ref.getMType(); }

	public Var	getRetVar() {
		if( retvar == null )
			retvar = new Var(pos,nameResultVar,type_ref.ret.getType(),ACC_FINAL);
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
			for(; i < type.args.length-1; i++) {
				Type ptp = Type.getRealType(t,type.args[i]);
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
			Type varg_tp = Type.getRealType(t,params[params.length-1].type);
			assert(varg_tp.isArray());
			for(; i < args.length; i++) {
				if !(args[i].getType().isInstanceOf(varg_tp.args[0])) {
					CastExpr.autoCastToReference(args[i]);
					CastExpr.autoCast(args[i],varg_tp.args[0]);
				}
			}
//			int j;
//			for(j=0; j < type.args.length-1; j++)
//				CastExpr.autoCast(args[j],Type.getRealType(t,type.args[j]));
//			NArr<ENode> varargs = new NArr<ENode>();
//			while(j < args.length) {
//				CastExpr.autoCastToReference(args[j]);
//				varargs.append(args[j]);
//				args.del(j);
//			}
//			NewInitializedArrayExpr nae =
//				new NewInitializedArrayExpr(getPos(),new TypeRef(Type.tpObject),1,varargs.toArray());
//			args.append(nae);
		} else {
			for(int i=0; i < type.args.length; i++) {
				Type ptp = Type.getRealType(t,type.args[i]);
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
		}
	}

	public boolean equalsByCast(KString name, MethodType mt, Type tp, ResInfo info) {
		if( this.name.equals(name) )
			return compare(name,mt,tp,info,false);
		return false;
	}
	
	public boolean compare(KString name, MethodType mt, Type tp, ResInfo info, boolean exact) {
		if( !this.name.equals(name) ) return false;
		int type_len = this.type.args.length;
		int args_len = mt.args.length;
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
		MethodType rt = (MethodType)Type.getRealType(tp,this.type);
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if( exact && !mt.args[i].equals(rt.args[i]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+rt.args[i]+" != "+mt.args[i]);
				return false;
			}
			else if( !exact && !mt.args[i].isAutoCastableTo(rt.args[i]) ) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.args[i]+" not auto-castable to "+rt.args[i]);
				return false;
			}
		}
		boolean match = false;
		if( mt.ret == Type.tpAny )
			match = true;
		else if( exact &&  rt.ret.equals(mt.ret) )
			match = true;
		else if( !exact && rt.ret.isAutoCastableTo(mt.ret) )
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
			dmp.space().append(type.ret).forsed_space().append(name);
		else
			dmp.space().append(((Struct)parent).name.short_name);
		dmp.append('(');
		for(int i=0; i < params.length; i++) {
			if (params[i].isFinal()) dmp.append("final").forsed_space();
			if (params[i].isForward()) dmp.append("forward").forsed_space();
			params[i].toJavaDecl(dmp,dtype_ref.args[i].getType());
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
		Type@ t;
	{
		checkRebuildTypes(),
		inlined_by_dispatcher,$cut,false
	;
		var @= params,
		var.name.equals(name),
		node ?= var
	;
		node ?= retvar, ((Var)node).name.equals(name)
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_clazz.type.resolveNameAccessR(node,path,name)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.type.resolveNameAccessR(node,path,name)
	}

	public rule resolveMethodR(DNode@ node, ResInfo info, KString name, MethodType mt)
		Var@ n;
	{
		checkRebuildTypes(),
		info.isForwardsAllowed(),
	{
		!this.isStatic(),
		info.enterForward(ThisExpr.thisPar) : info.leaveForward(ThisExpr.thisPar),
		this.ctx_clazz.type.resolveCallAccessR(node,info,name,mt)
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
			setPublic(true);
			if( pbody == null ) setAbstract(true);
		}

//		if (argtypes.length > 0) {
//			ftypes = new Type[argtypes.length];
//			for (int i=0; i < argtypes.length; i++)
//				ftypes[i] = argtypes[i].getType();
//		}

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

		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype == null)
				fp.stype = new TypeRef(fp.vtype.pos,fp.vtype.getType().getJavaType());
			if (fp.meta != null)
				fp.meta.verify();
		}
		if( isVarArgs() ) {
			FormPar va = new FormPar(pos,nameVarArgs,Type.newArrayType(Type.tpObject),FormPar.PARAM_VARARGS,ACC_FINAL);
			params.append(va);
		}
		checkRebuildTypes();
		type_ref.getMType(); // resolve
		dtype_ref.getMType(); // resolve
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
			Type tp = this.type_ref.getMType().ret;
			Type t = tp;
			if (t.isArray()) {
				if (annotation_default instanceof MetaValueScalar) {
					MetaValueArray mva = new MetaValueArray(annotation_default.type);
					mva.values.add(((MetaValueScalar)annotation_default).value);
					annotation_default = mva;
				}
				t = t.args[0];
			}
			if (t.isReference()) {
				t.checkResolved();
				if (!(t == Type.tpString || t == Type.tpClass || t.isAnnotation() || t.isEnum()))
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
			Method m = (Method)dfi.node;
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
	
	public void resolveDecl() {
		if( isResolved() ) return;
		trace(Kiev.debugResolve,"Resolving method "+this);
		assert( ctx_clazz == parent || inlined_by_dispatcher );
		try {
			foreach(WBCCondition cond; conditions; cond.cond == WBCType.CondRequire ) {
				cond.body.resolve(Type.tpVoid);
			}
			if( body != null ) {
				if (type.ret == Type.tpVoid)
					body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret == Type.tpVoid ) {
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
				if( type.ret != Type.tpVoid ) getRetVar();
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

	@att public final NArr<ENode>	addstats;

	public Constructor() {
	}

	public Constructor(MethodType mt, int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, mt, fl);
	}

	public Constructor(TypeCallRef type_ref, int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, type_ref, (TypeCallRef)type_ref.copy(), fl);
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
	public CondUnknown,
	public CondRequire,
	public CondEnsure,
	public CondInvariant;
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

