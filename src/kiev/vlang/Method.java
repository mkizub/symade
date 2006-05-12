package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.vlang.types.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RMethod;
import kiev.be.java15.JMethod;
import kiev.ir.java15.RConstructor;
import kiev.ir.java15.RInitializer;
import kiev.be.java15.JInitializer;
import kiev.ir.java15.RWBCCondition;
import kiev.be.java15.JWBCCondition;

import kiev.be.java15.CodeAttr;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class Method extends DNode implements ScopeOfNames,ScopeOfMethods,Accessable,PreScanneable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in")	Block		body;
	@dflow(in="this:in")	WBCCondition[] 	conditions;
	}

	@virtual typedef This  = Method;
	@virtual typedef VView = VMethod;
	@virtual typedef JView = JMethod;
	@virtual typedef RView = RMethod;

		 public Access				acc;
		 CallMetaType				meta_type;
	@att public NArr<TypeDef>		targs;
	@att public TypeRef				type_ret;
	@att public TypeRef				dtype_ret;
	@att public NArr<FormPar>		params;
	@att public NArr<ASTAlias>		aliases;
	@att public Var					retvar;
	@att public ENode				body;
	public kiev.be.java15.Attr[]		attrs = kiev.be.java15.Attr.emptyArray;
	@att public NArr<WBCCondition> 	conditions;
	@ref public NArr<Field>			violated_fields;
	@att public MetaValue			annotation_default;
		 public boolean				inlined_by_dispatcher;
		 public boolean				invalid_types;

	@virtual public					CallType		type;
	@virtual public					CallType		dtype;
	@abstract
	@virtual public:ro				CallType		etype;

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "params") {
				parent().callbackChildChanged(pslot());
			}
			else if (attr.name == "conditions")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "annotation_default")
				parent().callbackChildChanged(pslot());
		}
		if (attr.name == "params" || attr.name == "flags")
			invalid_types = true;
	}

	@getter public final CallType				get$type()	{ checkRebuildTypes(); return this.type; }
	@getter public final CallType				get$dtype()	{ checkRebuildTypes(); return this.dtype; }
	@getter public final CallType				get$etype()	{ checkRebuildTypes(); return (CallType)this.dtype.getErasedType(); }

	@getter public final Block					get$block()	{ return (Block)this.body; }

	public Var getRetVar() {
		if( retvar == null )
			retvar = new Var(pos,nameResultVar,type_ret.getType(),ACC_FINAL);
		return retvar;
	}

	public MetaThrows getMetaThrows() {
		return (MetaThrows)this.getNodeData(MetaThrows.ATTR);
	}

	// virtual static method
	public final boolean isVirtualStatic() {
		return this.is_mth_virtual_static;
	}
	public final void setVirtualStatic(boolean on) {
		if (this.is_mth_virtual_static != on) {
			this.is_mth_virtual_static = on;
			if (!isStatic()) this.setStatic(true);
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// method with variable number of arguments	
	public final boolean isVarArgs() {
		return this.is_mth_varargs;
	}
	public final void setVarArgs(boolean on) {
		if (this.is_mth_varargs != on) {
			this.is_mth_varargs = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// logic rule method
	public final boolean isRuleMethod() {
		return this instanceof RuleMethod;
	}
	// method with attached operator	
	public final boolean isOperatorMethod() {
		return this.is_mth_operator;
	}
	public final void setOperatorMethod(boolean on) {
		if (this.is_mth_operator != on) {
			this.is_mth_operator = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// need fields initialization	
	public final boolean isNeedFieldInits() {
		return this.is_mth_need_fields_init;
	}
	public final void setNeedFieldInits(boolean on) {
		if (this.is_mth_need_fields_init != on) {
			this.is_mth_need_fields_init = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a method generated as invariant	
	public final boolean isInvariantMethod() {
		return this.is_mth_invariant;
	}
	public final void setInvariantMethod(boolean on) {
		if (this.is_mth_invariant != on) {
			this.is_mth_invariant = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local method (closure code or inner method)	
	public final boolean isLocalMethod() {
		return this.is_mth_local;
	}
	public final void setLocalMethod(boolean on) {
		if (this.is_mth_local != on) {
			this.is_mth_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod() {
		return this.is_mth_dispatcher;
	}
	public final void setDispatcherMethod(boolean on) {
		if (this.is_mth_dispatcher != on) {
			this.is_mth_dispatcher = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

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
			type_set.append(ctx_clazz.ctype.bindings());
			dtype_set.append(ctx_clazz.ctype.bindings());
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
				assert(this instanceof Constructor);
				assert(!this.isStatic());
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.id.uname == nameThisDollar);
				assert(fp.type ≈ this.ctx_clazz.package_clazz.ctype);
				dargs.append(this.ctx_clazz.package_clazz.ctype);
				break;
			case FormPar.PARAM_RULE_ENV:
				assert(this instanceof RuleMethod);
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.type ≡ Type.tpRule);
				assert(fp.id.uname == namePEnv);
				dargs.append(Type.tpRule);
				break;
			case FormPar.PARAM_TYPEINFO:
				assert(this instanceof Constructor || (this.isStatic() && this.id.equals(nameNewOp)));
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
				assert(this instanceof Constructor);
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

	@getter public Method get$child_ctx_method() { return (Method)this; }
		

	@nodeview
	public static view VMethod of Method extends VDNode {

		public final void checkRebuildTypes();
	
		public				Access				acc;
		public				CallMetaType		meta_type;
		public:ro			NArr<TypeDef>		targs;
		public				TypeRef				type_ret;
		public				TypeRef				dtype_ret;
		public:ro			CallType			type;
		public:ro			CallType			dtype;
		public:ro			CallType			etype;
		public:ro			NArr<FormPar>		params;
		public:ro			NArr<ASTAlias>		aliases;
		public				Var					retvar;
		public				ENode				body;
		public:ro			NArr<WBCCondition>	conditions;
		public:ro			NArr<Field>			violated_fields;
		public				MetaValue			annotation_default;
		public				boolean				inlined_by_dispatcher;
		public				boolean				invalid_types;
	
		public Var getRetVar();
		public MetaThrows getMetaThrows();
		
		// virtual static method
		public final boolean isVirtualStatic();
		public final void setVirtualStatic(boolean on);
		// method with variable number of arguments	
		public final boolean isVarArgs();
		public final void setVarArgs(boolean on);
		// logic rule method
		public final boolean isRuleMethod();
		// method with attached operator	
		public final boolean isOperatorMethod();
		public final void setOperatorMethod(boolean on);
		// need fields initialization	
		public final boolean isNeedFieldInits();
		public final void setNeedFieldInits(boolean on);
		// a method generated as invariant	
		public final boolean isInvariantMethod();
		public final void setInvariantMethod(boolean on);
		// a local method (closure code or inner method)	
		public final boolean isLocalMethod();
		public final void setLocalMethod(boolean on);
		// a dispatcher (for multimethods)	
		public final boolean isDispatcherMethod();
		public final void setDispatcherMethod(boolean on);

		public boolean preResolveIn() {
			checkRebuildTypes();
			return true;
		}
	
		public boolean mainResolveIn() {
			checkRebuildTypes();
			return true;
		}
	
		public boolean preVerify() {
			Struct ctx_clazz = this.ctx_clazz;
			if (isAbstract() && isStatic()) {
				setBad(true);
				ctx_clazz.setBad(true);
				Kiev.reportError(this,"Static method cannot be declared abstract");
			}
			if (ctx_clazz.isInterface() && !ctx_clazz.isStructView()) {
				if (isFinal()) {
					Kiev.reportWarning(this,"Interface methods cannot be final");
					setFinal(false);
				}
			}
			return true;
		}
	}

	public static Method[]	emptyArray = new Method[0];

	@getter public Access			get$acc()			{ return this.acc; }
	@setter public void set$acc(Access val)			{ this.acc = val; Access.verifyDecl(this); }
	
	public Method() {}

	public Method(String name, Type ret) {
		this(new Symbol(name),new TypeRef(ret),0);
	}

	public Method(String name, Type ret, int fl) {
		this(name,new TypeRef(ret),fl);
	}
	public Method(String name, TypeRef type_ret, int fl) {
		this(new Symbol(name), type_ret, fl);
	}
	public Method(Symbol id, TypeRef type_ret, int fl) {
		assert (!(id.equals(nameInit) || id.equals(nameClassInit)) || this instanceof Constructor);
		this.flags = fl;
		this.id = id;
		this.type_ret = type_ret;
		this.dtype_ret = type_ret.ncopy();
		this.meta = new MetaSet();
		invalid_types = true;
	}

	public Type	getType() { return type; }

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
			if (f.invs.indexOf(this) < 0)
				f.invs.add(this);
			if( this.ctx_clazz.instanceOf(f.ctx_clazz) ) {
				if (violated_fields.indexOf(f) < 0)
					violated_fields.add(f);
			}
		} else {
			if (violated_fields.indexOf(f) < 0)
				violated_fields.add(f);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(id+"(");
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

	public static String toString(String nm, ENode[] args) {
		return toString(nm,args,null);
	}

	public static String toString(String nm, ENode[] args, Type ret) {
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

	public static String toString(String nm, CallType mt) {
		StringBuffer sb = new StringBuffer(nm+"(");
		for(int i=0; i < mt.arity; i++) {
			sb.append(mt.arg(i).toString());
			if( i < (mt.arity-1) ) sb.append(",");
		}
		sb.append(")->").append(mt.ret());
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(id);
	}

	public void makeArgs(ENode[] args, Type t) {
		checkRebuildTypes();
		//assert(args.getPSlot().is_attr);
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

	public boolean equalsByCast(String name, CallType mt, Type tp, ResInfo info) {
		if (!this.id.equals(name)) return false;
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
			if (a > 0)
				rt = rt.rebind(set);
		}
		
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if (!mt.arg(i).isAutoCastableTo(rt.arg(i))) {
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.arg(i)+" not auto-castable to "+rt.arg(i));
				return false;
			}
		}

		foreach (TypeDef td; this.targs) {
			ArgType at = td.getAType();
			Type bnd = rt.resolve(at);
			if (bnd ≡ at) {
				Vector<Type> bindings = new Vector<Type>();
				// bind from mt
				for (int i=0; i < rt.arity; i++)
					bindings = addBindingsFor(at, mt.arg(i), rt.arg(i), bindings);
				if (mt.ret() ≢ Type.tpAny)
					bindings = addBindingsFor(at, mt.ret(), rt.ret(), bindings);
				if (bindings.length == 0) {
					trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
						+" do not allow to infer type: "+at);
					continue;
				}
				Type b = bindings.at(0);
				for (int i=1; i < bindings.length; i++)
					b = Type.leastCommonType(b, bindings.at(i));
				trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" infer argument: "+at+" to "+b);
				if (b ≡ Type.tpAny)
					return false;
				rt = rt.rebind(new TVarBld(at, b));
			}
		}
		
		if (mt.ret() ≢ Type.tpAny && !rt.ret().isAutoCastableTo(mt.ret())) {
			trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				+" differ in return type : "+rt.ret()+" not auto-castable to "+mt.ret());
			return false;
		}
		
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+" match as "+rt);
		info.mt = rt;
		return true;
	}

	// compares pattern type (pt) with query type (qt) to find bindings for argument type (at),
	// and adds found bindings to the set of bindings
	private static Vector<Type> addBindingsFor(ArgType at, Type pt, Type qt, Vector<Type> bindings) {
		if (!qt.hasApplayable(at))
			return bindings;
		final int qt_size = qt.tvars.length;
		for (int i=0; i < qt_size; i++) {
			TVar qtv = qt.tvars[i];
			if (!qtv.isAlias()) {
				if (qtv.val ≡ at) {
					Type bnd = pt.resolve(qtv.var);
					if (bnd ≢ qtv.var && !bindings.contains(bnd))
						bindings.append(bnd);
				}
				else if (qtv.val.hasApplayable(at)) {
					Type bnd = pt.resolve(qtv.var);
					if (bnd ≢ qtv.var)
						addBindingsFor(at, bnd, qtv.val, bindings);
				}
			}
		}
		return bindings;
	}
	

	// TODO
	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !id.equals(nameInit) )
			dmp.space().append(type.ret()).forsed_space().append(id);
		else
			dmp.space().append(this.ctx_clazz.id);
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

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
		FormPar@ var;
	{
		inlined_by_dispatcher || path.space_prev.pslot().name == "targs",$cut,false
	;
		path.space_prev.pslot().name == "params" ||
		path.space_prev.pslot().name == "type_ref" ||
		path.space_prev.pslot().name == "dtype_ref",$cut,
		node @= targs,
		((TypeDef)node).id.equals(name)
	;
		var @= params,
		var.id.equals(name),
		node ?= var
	;
		node ?= retvar, ((Var)node).id.equals(name)
	;
		node @= targs,
		((TypeDef)node).id.equals(name)
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

	public rule resolveMethodR(Method@ node, ResInfo info, String name, CallType mt)
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
		if !( this.parent() instanceof Struct )
			throw new CompilerException(this,"Method must be declared on class level only");
		Struct clazz = this.ctx_clazz;
		// TODO: check flags for methods
		if( clazz.isPackage() ) setStatic(true);
		if( (flags & ACC_PRIVATE) != 0 ) setFinal(false);
		else if( clazz.isClazz() && clazz.isFinal() ) setFinal(true);
		else if( clazz.isInterface() ) {
			setPublic();
			if (body == null) setAbstract(true);
		}

		if (clazz.isAnnotation() && params.length != 0) {
			Kiev.reportError(this, "Annotation methods may not have arguments");
			params.delAll();
			setVarArgs(false);
		}

		if (clazz.isAnnotation() && (body != null)) {
			Kiev.reportError(this, "Annotation methods may not have bodies");
			body = null;
		}

		if (isTypeUnerasable()) {
			int i = 0;
			foreach (TypeDef td; targs) {
				td.setTypeUnerasable(true);
				FormPar v = new FormPar(td.pos,nameTypeInfo+"$"+td.id.uname, Type.tpTypeInfo, FormPar.PARAM_TYPEINFO_N+i, ACC_FINAL|ACC_SYNTHETIC);
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
				if (t.getStruct() == null || !(t ≈ Type.tpString || t ≈ Type.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
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
			Method m = (Method)dfi.node_impl;
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

	public boolean setBody(ENode body) {
		trace(Kiev.debugMultiMethod,"Setting body of methods "+this);
		this.body = body;
		return true;
	}

}

@node
public class Constructor extends Method {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]			addstats;
	@dflow(in="this:in")				Block			body;
	@dflow(in="this:in")				WBCCondition[] 	conditions;
	}

	@virtual typedef This  = Constructor;
	@virtual typedef VView = VConstructor;
	@virtual typedef RView = RConstructor;

	@att public NArr<ENode>			addstats;

	@nodeview
	public static final view VConstructor of Constructor extends VMethod {
		public:ro	NArr<ENode>			addstats;
	}

	public Constructor() {}

	public Constructor(int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, Type.tpVoid);
		this.flags = fl;
	}
}

@node
public class Initializer extends DNode implements PreScanneable {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")		ENode		body;
	}

	@virtual typedef This  = Initializer;
	@virtual typedef VView = VInitializer;
	@virtual typedef JView = JInitializer;
	@virtual typedef RView = RInitializer;

	@att public ENode				body;

	@getter public final Block get$block()	{ return (Block)this.body; }

	@nodeview
	public static final view VInitializer of Initializer extends VDNode {
		public ENode				body;
	}

	public Initializer() {}

	public Initializer(int pos, int flags) {
		this();
		this.pos = pos;
		this.flags = flags;
	}

	public boolean setBody(ENode body) {
		trace(Kiev.debugMultiMethod,"Setting body of initializer "+this);
		this.body = body;
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
	
	@virtual typedef This  = WBCCondition;
	@virtual typedef VView = VWBCCondition;
	@virtual typedef JView = JWBCCondition;
	@virtual typedef RView = RWBCCondition;

	@att public WBCType				cond;
	@att public ENode				body;
	@ref public Method				definer;
	@att public CodeAttr			code_attr;

	@nodeview
	public static final view VWBCCondition of WBCCondition extends VDNode {
		public WBCType				cond;
		public ENode				body;
		public Method				definer;
		public CodeAttr				code_attr;
	}

	public WBCCondition() {}

	public WBCCondition(int pos, WBCType cond, String name, ENode body) {
		this.pos = pos;
		if (name != null)
			this.id = new Symbol(name);
		this.cond = cond;
		this.body = body;
	}

	public boolean setBody(ENode body) {
		this.body = body;
		return true;
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(body);
	}
}

