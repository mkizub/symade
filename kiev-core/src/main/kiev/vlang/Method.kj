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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(name="AMethod", lang=CoreLang)
public abstract class Method extends DNode implements ScopeOfNames,ScopeOfMethods {
	private static final Class[] $meta_flags = new Class[] {
		MetaPublic.class,    MetaPrivate.class,        MetaProtected.class, MetaStatic.class,
		MetaFinal.class,     MetaSynchronized.class,   MetaBridge.class,    MetaVarArgs.class,
		MetaNative.class,    null,                     MetaAbstract.class,  null,
		MetaSynthetic.class, null,                     null,                null,
		MetaForward.class,   MetaVirtual.class,        MetaUnerasable.class,MetaMacro.class,
		null,                null,                     null,               null,
		null,                null,                     null,               null,
		null,                null,                     null,               null
	};
	
	//public static final SpaceRefDataAttrSlot<Field> ATTR_VIOLATED_FIELDS = new SpaceRefDataAttrSlot<Field>("violated fields",false,TypeInfo.newTypeInfo(Field.class,null));	

	@nodeAttr public TypeConstr∅			targs;
	@nodeAttr public TypeRef				type_ret;
	@nodeAttr public Var∅					params;
	@nodeAttr public ENode					body;

	@abstract
	@nodeAttr(ext_data=true)
	public Alias⋈							aliases;
	@abstract
	@nodeAttr(ext_data=true)
	public WBCCondition⋈				 	conditions;
	@nodeAttr(ext_data=true)
	public Var								ret_var;

	@AttrBinDumpInfo(ignore=true)
	@nodeData(ext_data=true)
	public Method		caller_from_inner;

	private				CallType		_mtype;
	private				CallType		_dtype;
	@abstract @virtual
	public:ro			CallType		mtype;
	@abstract @virtual
	public:ro			CallType		dtype;
	@abstract @virtual
	public:ro			CallType		etype;
	@virtual @abstract
	public:ro			Block			block;

	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change) {
			if (info.slot.name == "params") {
				notifyParentThatIHaveChanged();
			}
			if (info.slot.name == "params" || info.slot.name == "type_ret" || info.slot.name == "metas") {
				_mtype = null;
				_dtype = null;
			}
		}
		super.callbackChanged(info);
	}

	@getter public final CallType				get$mtype()	{ if (this._mtype == null) rebuildTypes(); return this._mtype; }
	@getter public final CallType				get$dtype()	{ if (this._dtype == null) rebuildTypes(); return this._dtype; }
	@getter public final CallType				get$etype()	{
		if (this._dtype == null) rebuildTypes();
		if (isStatic())
			(CallType)this._dtype.getErasedType();
		TypeDecl tdecl = Env.ctxTDecl(this);
		if (tdecl == null)
			(CallType)this._dtype.getErasedType();
		return (CallType)this._dtype.applay(tdecl.getType(Env.getEnv()).bindings()).getErasedType();
	}

	@getter public final Block					get$block()	{ return (Block)this.body; }

	public String qname() {
		ANode p = parent();
		if (p == null || p instanceof KievRoot)
			return sname;
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+'·'+sname);
		return sname;
	}

	public Var getRetVar() {
		Var retvar = this.ret_var;
		if( retvar == null ) {
			retvar = new LVar(pos,nameResultVar,type_ret.getType(Env.getEnv()),Var.VAR_LOCAL,ACC_FINAL);
			this.ret_var = retvar;
		}
		return retvar;
	}

	public Class[] getMetaFlags() { return Method.$meta_flags; }
	
	public MetaThrows getMetaThrows() {
		return (MetaThrows)getMeta("kiev·stdlib·meta·throws");
	}

	// virtual static method
	public final boolean isVirtualStatic() {
		return this.is_mth_virtual_static;
	}
	public final void setVirtualStatic(boolean on) {
		if (this.is_mth_virtual_static != on) {
			this.is_mth_virtual_static = on;
			if (!isStatic()) this.setStatic(true);
		}
	}
	// method with variable number of arguments	
	public final boolean isVarArgs() {
		return this.mflags_is_mth_varargs;
	}
	public final void setVarArgs(boolean on) {
		if (this.mflags_is_mth_varargs != on) {
			this.mflags_is_mth_varargs = on;
		}
	}
	// logic rule method
	public final boolean isRuleMethod() {
		return this instanceof RuleMethod;
	}
	// need fields initialization	
	public final boolean isNeedFieldInits() {
		return this.is_mth_need_fields_init;
	}
	public final void setNeedFieldInits(boolean on) {
		if (this.is_mth_need_fields_init != on) {
			this.is_mth_need_fields_init = on;
		}
	}
	// a method generated as invariant	
	public final boolean isInvariantMethod() {
		return this.is_mth_invariant;
	}
	public final void setInvariantMethod(boolean on) {
		if (this.is_mth_invariant != on) {
			this.is_mth_invariant = on;
		}
	}
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod() {
		return this.is_mth_dispatcher;
	}
	public final void setDispatcherMethod(boolean on) {
		if (this.is_mth_dispatcher != on) {
			this.is_mth_dispatcher = on;
		}
	}
	// a methood inlined bt dispatcher (for multimethods)	
	public final boolean isInlinedByDispatcherMethod() {
		return this.parent() instanceof InlineMethodStat;
	}

	final void rebuildTypes() {
		Env env = Env.getEnv();
		Vector<Type> args = new Vector<Type>();
		Vector<Type> dargs = new Vector<Type>();
		boolean is_varargs = false;
		foreach (Var fp; params) {
			TypeRef fpdtype = fp.stype;
			if (fpdtype == null)
				fpdtype = fp.vtype;
			switch (fp.kind) {
			case Var.VAR_LOCAL:
				args.append(fp.getType(env));
				dargs.append(fpdtype.getType(env));
				if (fp.getType(env) instanceof VarargType)
					is_varargs = true;
				break;
			case Var.PARAM_OUTER_THIS:
				assert(this instanceof Constructor);
				assert(!this.isStatic());
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.sname == nameThisDollar);
				dargs.append(fp.getType(env));
				break;
			case Var.PARAM_RULE_ENV:
				assert(this instanceof RuleMethod);
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.getType(env) ≡ env.tenv.tpRule);
				assert(fp.sname == namePEnvParam);
				dargs.append(env.tenv.tpRule);
				break;
			case Var.PARAM_ENUM_NAME:
				assert(this instanceof Constructor && !this.isStatic());
				assert(fp.getType(env) ≈ env.tenv.tpString);
				dargs.append(env.tenv.tpString);
				break;
			case Var.PARAM_ENUM_ORD:
				assert(this instanceof Constructor && !this.isStatic());
				assert(fp.getType(env) ≈ env.tenv.tpInt);
				dargs.append(env.tenv.tpInt);
				break;
			case Var.PARAM_CLASS_TYPEINFO:
				assert(this instanceof Constructor || (this.isStatic() && this.hasName(nameNewOp)));
				assert(fp.isFinal());
				assert(fpdtype == null || fpdtype.getType(env) ≈ fp.getType(env));
				dargs.append(fp.getType(env));
				break;
			case Var.PARAM_LVAR_PROXY:
				assert(this instanceof Constructor);
				assert(fp.isFinal());
				dargs.append(fp.getType(env));
				break;
			default:
				if (fp.kind == Var.PARAM_METHOD_TYPEINFO) {
					assert(this.mflags_is_type_unerasable);
					assert(fp.isFinal());
					assert(fp.getType(env) ≈ env.tenv.tpTypeInfo);
					dargs.append(fp.getType(env));
					break;
				}
				throw new CompilerException(fp, "Unknown kind of the formal parameter "+fp);
			}
		}
		if (is_varargs != this.isVarArgs())
			this.setVarArgs(is_varargs);
		Type tp_ret, dtp_ret;
		if (type_ret == null)
			tp_ret = env.tenv.tpVoid;
		else
			tp_ret = type_ret.getType(env);
		if (tp_ret == null)
			tp_ret = env.tenv.tpVoid;
		
		this._mtype = new CallType(this, args.toArray(), tp_ret);
		this._dtype = new CallType(this, dargs.toArray(), tp_ret);
	}

	public static final Method[]	emptyArray = new Method[0];

	public Method() {}
	public Method(String name, TypeRef type_ret, int flags) {
		this.sname = name;
		this.type_ret = type_ret;
		this.nodeflags |= flags;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "body") {
			if (this.isMacro() || Env.ctxTDecl(this).isMixin()) // save for macroses and trait/mixin
				return true;
			if (this.body instanceof MetaValue)
				return true;
			return false;
		}
		return super.includeInDump(dump, attr, val);
	}

	public boolean hasName(String nm) {
		String sname = this.sname;
		if (sname == nm) return true;
		foreach(Alias a; aliases; a.symbol.sname == nm)
			return true;
		return false;
	}

	public Symbol getSymbol(String nm) {
		if (sname == nm) return this.symbol;
		foreach(Alias a; aliases; a.symbol.sname == nm)
			return a.symbol;
		assert (false, "Symbol "+nm+" not found in "+this);
		return null;
	}

	public Type	getType(Env env) { return mtype; }

	public Var getHiddenParam(int kind) {
		foreach (Var fp; params; fp.kind == kind)
			return fp;
		return null;
	}
	
	public Var getOuterThisParam() {
		Type t = this.mtype; // rebuildTypes()
		foreach (Var fp; params; fp.kind == Var.PARAM_OUTER_THIS)
			return fp;
		return null;
	}
	
	public Var getClassTypeInfoParam() {
		foreach (Var fp; params; fp.kind == Var.PARAM_CLASS_TYPEINFO)
			return fp;
		return null;
	}
	
	public Var getMethodTypeInfoParam(String name) {
		name = (nameTypeInfo+"$"+name).intern();
		foreach (Var fp; params; fp.kind == Var.PARAM_METHOD_TYPEINFO && fp.sname == name)
			return fp;
		return null;
	}
	
	public Var getVarArgParam() {
		Type t = this.mtype; // rebuildTypes()
		foreach (Var fp; params; fp.getType(Env.getEnv()) instanceof VarargType)
			return fp;
		return null;
	}
	
	public void addViolatedField(Field f) {
/*		if (!Env.ctxTDecl(this).instanceOf(Env.ctxTDecl(f)))
			return;
		Field[] violated_fields = Method.ATTR_VIOLATED_FIELDS.get((Method)this);
		if( isInvariantMethod() ) {
			if (Field.ATTR_INVARIANT_CHECKERS.indexOf(f,this) < 0)
				Field.ATTR_INVARIANT_CHECKERS.add(f,this);
			if( Env.ctxTDecl(this).instanceOf(Env.ctxTDecl(f)) ) {
				if (Method.ATTR_VIOLATED_FIELDS.indexOf(this, f) < 0)
					Method.ATTR_VIOLATED_FIELDS.add(this,f);
			}
		} else {
			if (Method.ATTR_VIOLATED_FIELDS.indexOf(this,f) < 0)
				Method.ATTR_VIOLATED_FIELDS.add(this,f);
		}
*/	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		Type t = this.mtype; // rebuildTypes()
		return true;
	}

	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
		Type t = this.mtype; // rebuildTypes()
		return true;
	}

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		TypeDecl ctx_tdecl = Env.ctxTDecl(this);
		if (isAbstract() && isStatic()) {
			setBad(true);
			ctx_tdecl.setBad(true);
			Kiev.reportWarning(this,"Static method cannot be declared abstract");
		}
		if (ctx_tdecl.isInterface() && !ctx_tdecl.isStructView()) {
			if (isFinal()) {
				Kiev.reportWarning(this,"Interface methods cannot be final");
				setFinal(false);
			}
		}
		if (this.isMacro() && this.isInterfaceOnly())
			return false;
		return true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(sname+"(");
		int n = params.length;
		boolean comma = false;
		foreach (Var fp; params; fp.kind == Var.VAR_LOCAL) {
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
			sb.append(args[i].getType(Env.getEnv()).toString());
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

	public void normilizeExpr(Env env, ENode expr, Symbol sym, INode parent, AttrSlot slot) {
		assert (sym.dnode == this);
		if (body instanceof CoreExpr) {
			CoreOperation cop = ((CoreExpr)body).getCoreOperation(env);
			if (cop != null)
				cop.core_func.normilizeExpr(expr,parent,slot);
		}
	}

	public void makeArgs(ENode[] args, Type t) {
		Env env = Env.getEnv();
		CallType mt = this.mtype;
		//assert(args.getPSlot().is_attr);
		if( isVarArgs() ) {
			int i=0;
			for(; i < mt.arity-1; i++) {
				Type ptp = Type.getRealType(t,mt.arg(i));
				if !(args[i].getType(env).isInstanceOf(ptp))
					CastExpr.autoCast(env, args[i], ptp, args[i].parent(), args[i].pslot());
			}
			Type tn = Type.getRealType(t,getVarArgParam().getType(env));
			Type varg_tp = tn.resolveArg(0);
			if (args.length == i+1 && args[i].getType(env).isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
			} else {
				for(; i < args.length; i++) {
					if !(args[i].getType(env).isInstanceOf(varg_tp))
						CastExpr.autoCast(env, CastExpr.autoCastToReference(env, args[i], args[i].parent(), args[i].pslot()), varg_tp, args[i].parent(), args[i].pslot());
				}
			}
		} else {
			for(int i=0; i < mt.arity; i++) {
				Type ptp = Type.getRealType(t,mt.arg(i));
				if !(args[i].getType(env).isInstanceOf(ptp))
					CastExpr.autoCast(env, args[i], ptp, args[i].parent(), args[i].pslot());
			}
		}
	}

	public Symbol equalsByCast(String name, CallType mt, Type tp, ResInfo info) {
		Symbol sym = null;
		{
			String sname = this.sname;
			if (sname == name) {
				sym = this.symbol;
			} else {
				foreach(Alias a; aliases; a.symbol.sname == name) {
					sym = a.symbol;
					break;
				}
				if (sym == null)
					return null;
			}
		}
		int type_len = this.mtype.arity;
		int args_len = mt.arity;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in number of params: "+type_len+" != "+args_len);
				return null;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" not match in number of params: "+type_len+" != "+args_len);
				return null;
			}
		}
		trace(Kiev.debug && Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
		CallType rt = (CallType)this.mtype.applay(tp);
		
		Env env = info.env;
		
		if ((mt.getArgsLength() - mt.arity - 1) > 0) {
			TVarBld set = new TVarBld();
			for (int i=0; i < this.targs.length; i++) {
				ArgType arg = this.targs[i].getAType(info.env);
				Type bound = mt.resolve(env.tenv.tpUnattachedArgs[i]);
				if (bound == env.tenv.tpUnattachedArgs[i])
					continue;
				if!(bound.isInstanceOf(arg)) {
					trace(Kiev.debug && Kiev.debugResolve,"Type "+bound+" is not applayable to "+arg);
					return null;
				}
				set.append(arg, bound);
			}
			if (set.getArgsLength() > 0)
				rt = (CallType)rt.applay(set);
		}
		
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if (mt.arg(i).getAutoCastTo(rt.arg(i)) == null) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in param # "+i+": "+mt.arg(i)+" not auto-castable to "+rt.arg(i));
				return null;
			}
		}

		foreach (ArgType at; rt.getTArgs()) {
			Vector<Type> bindings = new Vector<Type>();
			// bind from mt
			for (int i=0; i < rt.arity && i < mt.arity; i++)
				addBindingsFor(env, at, mt.arg(i), rt.arg(i), bindings);
			addBindingsFor(env, at, mt.ret(), rt.ret(), bindings);
			if (bindings.length == 0) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" do not allow to infer type: "+at);
				continue;
			}
			Type b = bindings.at(0);
			for (int i=1; i < bindings.length; i++)
				b = Type.leastCommonType(b, bindings.at(i));
			trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				+" infer argument: "+at+" to "+b);
			if (b ≡ env.tenv.tpAny)
				return null;
			rt = (CallType)rt.applay(new TVarBld(at, b));
		}
		// check bindings are correct
		int n = rt.getArgsLength();
		for (int i=0; i < n; i++) {
			ArgType var = rt.getArg(i);
			Type val = rt.resolveArg(i);
			if (!var.checkBindings(rt, val)) {
				trace(Kiev.debug && Kiev.debugResolve,"Incorrect bindings for var "+var+" with value "+val+" in type "+rt);
				return null; 
			}
		}
		
		if (mt.ret() ≢ env.tenv.tpAny && rt.ret().getAutoCastTo(mt.ret()) == null) {
			trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				+" differ in return type : "+rt.ret()+" not auto-castable to "+mt.ret());
			return null;
		}
		
		trace(Kiev.debug && Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+" match as "+rt);
		info.resolved_type = rt;
		return sym;
	}
	
	public CallType makeType(ENode expr) {
		return makeType(null, expr.getEArgs());
	}
	public CallType makeType(TypeRef[] targs, ENode[] args) {
		Env env = Env.getEnv();
		if (this.isStatic()) {
			Type[] mt = new Type[args.length];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i].getType(env);
			return makeType(targs, null, mt);
		} else {
			Type[] mt = new Type[args.length-1];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i+1].getType(env);
			return makeType(targs, args[0].getType(env), mt);
		}
	}
	public CallType makeType(TypeRef[] targs, Type ttp, Type[] mt) {
		Env env = Env.getEnv();
		CallType rt;
		if (this.isStatic()) {
			rt = (CallType)this.mtype;
		} else {
			rt = (CallType)this.mtype.applay(ttp);
		}
		if (targs != null && targs.length > 0) {
			TVarBld set = new TVarBld();
			for (int i=0; i < targs.length && i < this.targs.length; i++) {
				Type bound = targs[i].getType(env);
				ArgType arg = this.targs[i].getAType(env);
				set.append(arg, bound);
			}
			rt = (CallType)rt.applay(set);
		}
		foreach (ArgType at; rt.getTArgs()) {
			Vector<Type> bindings = new Vector<Type>();
			// bind from mt
			for (int i=0; i < rt.arity && i < mt.length; i++)
				addBindingsFor(env, at, mt[i], rt.arg(i), bindings);
			if (bindings.length == 0)
				continue;
			Type b = bindings.at(0);
			for (int i=1; i < bindings.length; i++)
				b = Type.leastCommonType(b, bindings.at(i));
			if (b ≡ env.tenv.tpAny)
				continue;
			rt = (CallType)rt.applay(new TVarBld(at, b));
		}
		return rt;
	}

	// compares pattern type (pt) with query type (qt) to find bindings for argument type (at),
	// and adds found bindings to the set of bindings
	private static void addBindingsFor(Env env, ArgType at, Type pt, Type qt, Vector<Type> bindings) {
		if (qt instanceof VarargType) {
			qt = qt.arg;
			if (pt instanceof ArrayType)
				pt = pt.arg;
		}
		if (pt ≡ null || pt ≡ env.tenv.tpAny || pt ≡ at)
			return;
		qt.checkResolved();
		pt.checkResolved();
		if (qt ≡ at) {
			Type t = pt.getAutoCastTo(at);
			if (t != null)
				bindings.append(t);
			return;
		}
		if (!qt.hasApplayable(at))
			return;
		final int qt_size = qt.getArgsLength();
		for (int i=0; i < qt_size; i++) {
			//if (qt.isAliasArg(i))
			//	continue;
			ArgType qtvar = qt.getArg(i);
			Type qtval = qt.resolveArg(i);
			if (qtval ≡ at) {
				Type bnd = pt.resolve(qtvar);
				if (bnd ≢ qtvar && !bindings.contains(bnd)) {
					Type t = bnd.getAutoCastTo(at);
					if (t != null)
						bindings.append(t);
				}
			}
			else if (qtval.hasApplayable(at)) {
				Type bnd = pt.resolve(qtvar);
				if (bnd ≢ qtvar && !bindings.contains(bnd)) {
					if (qtval instanceof WildcardCoType && qtval.getUnboxedType() ≡ at) {
						Type t = bnd.getAutoCastTo(at);
						if (t != null) {
							bindings.append(t);
							continue;
						}
					}
					if (qtval instanceof WildcardContraType && qtval.getUnboxedType() ≡ at) {
						Type t = bnd.getAutoCastTo(at);
						if (t != null) {
							bindings.append(t);
							continue;
						}
					}
					addBindingsFor(env, at, bnd, qtval, bindings);
				}
			}
		}
		return;
	}

	public rule resolveNameR(ResInfo path)
		LVar@ var;
	{
		isInlinedByDispatcherMethod() , $cut, false
	;
		path.getPrevSlotName() == "targs" ||
		path.getPrevSlotName() == "params" ||
		path.getPrevSlotName() == "type_ref" ||
		path.getPrevSlotName() == "dtype_ref",
		$cut,
		path @= targs
	;
		path @= params
	//;
	//	path.getName() == nameResultVar,
	//	path ?= ret_var
	;
		path @= targs
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(path.env.proj.thisPar) : path.leaveForward(path.env.proj.thisPar),
		Env.ctxTDecl(this).getType(path.env).resolveNameAccessR(path)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.getType(path.env).resolveNameAccessR(path)
	}

	public rule resolveMethodR(ResInfo info, CallType mt)
		Var@ n;
	{
		info.isForwardsAllowed(),
	{
		!this.isStatic(),
		info.enterForward(info.env.proj.thisPar) : info.leaveForward(info.env.proj.thisPar),
		Env.ctxTDecl(this).getType(info.env).resolveCallAccessR(info,mt)
	;
		n @= params,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType(info.env).resolveCallAccessR(info,mt)
	}
	}

    public void pass3(Env env) {
		if !( this.parent() instanceof TypeDecl )
			throw new CompilerException(this,"Method must be declared on class level only");
		TypeDecl clazz = Env.ctxTDecl(this);
		// TODO: check flags for methods
		if( isPrivate() && isFinal() ) { setFinal(false); }
		else if( clazz.isClazz() && clazz.isFinal() && !isFinal() ) { setFinal(true); }
		else if( clazz.isInterface() && !isPublic() ) {
			setPublic();
			if (body == null && !isAbstract()) setAbstract(true);
		}

		if (clazz.isAnnotation() && params.length != 0) {
			Kiev.reportError(this, "Annotation methods may not have arguments");
			params.delAll();
			setVarArgs(false);
		}

		if (clazz.isAnnotation() && body != null && !(body instanceof MetaValue)) {
			Kiev.reportError(this, "Annotation methods may not have bodies");
			body = null;
		}

		if (isTypeUnerasable()) {
			foreach (Var fp; params; fp.kind >= Var.PARAM_METHOD_TYPEINFO || (fp.sname != null && fp.sname.startsWith(nameTypeInfo+"$"))) {
				fp.detach();
			}
			foreach (TypeDef td; targs) {
				td.setTypeUnerasable(true);
				LVar v = new LVar(td.pos,nameTypeInfo+"$"+td.sname, env.tenv.tpTypeInfo, Var.PARAM_METHOD_TYPEINFO, ACC_FINAL|ACC_SYNTHETIC);
				params.add(v);
			}
		}

		// push the method, because formal parameters may refer method's type args
		foreach (Var fp; params) {
			fp.vtype.getType(env); // resolve
			if (fp.stype != null)
				fp.stype.getType(env); // resolve
			fp.verifyMetas();
		}

		Type t = this.mtype; // rebuildTypes()
		trace(Kiev.debug && Kiev.debugMultiMethod,"Method "+this+" has dispatcher type "+this.dtype);
		verifyMetas();
		if (body instanceof MetaValue)
			((MetaValue)body).verify(this,nodeattr$body);

		foreach(WBCCondition cond; conditions) {
			if (cond.definer != this)
				cond.definer = this;
		}

		if (isMacro() && isNative()) {
			if !(body instanceof CoreExpr)
				Kiev.reportError(this, "@native @macro method must have CoreExpr body");
		}
	}

	public void resolveMetaDefaults(Env env) {
		if (body instanceof MetaValue) {
			Type tp = this.type_ret.getType(env);
			Type t = tp;
			if (t instanceof ArrayType) {
				if (body instanceof MetaValueScalar) {
					MetaValueArray mva = new MetaValueArray(new SymbolRef<DNode>(body.pos, this));
					mva.values.add(~((MetaValueScalar)body).value);
					body = mva;
				}
				t = t.arg;
			}
			if (t.isReference()) {
				t.checkResolved();
				if (t.getStruct() == null || !(t ≈ env.tenv.tpString || t ≈ env.tenv.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
					throw new CompilerException(body, "Bad annotation value type "+tp);
			}
			((MetaValue)body).resolveFrontEnd(t);
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

	public Method makeAccessor(Env env) {
		assert(isPrivate());
		if (caller_from_inner != null)
			return caller_from_inner;
		MethodImpl m = new MethodImpl(Env.ctxTDecl(this).allocateAccessName(), type_ret.getType(env), ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		CallExpr ce;
		if (isStatic()) {
			ce = new CallExpr(pos, new TypeRef(Env.ctxTDecl(this).getType(env)), this, ENode.emptyArray);
		} else {
			Var self = new LVar(pos,Constants.nameThis,Env.ctxTDecl(this).getType(env),Var.VAR_LOCAL,0);
			m.params += self;
			ce = new CallExpr(pos, new LVarExpr(pos,self), this, ENode.emptyArray);
		}
		foreach (Var v; this.params) {
			v = new Copier().copyFull(v);
			m.params += v;
			ce.args += new LVarExpr(pos,v);
		}
		m.block.stats += ce;
		Env.ctxTDecl(this).members += m;
		Kiev.runProcessorsOn(m);
		this.caller_from_inner = m;
		return m;
	}

	public void postVerify(Env env, INode parent, AttrSlot slot) {
		if (!isStatic() && !isPrivate()) {
			CallType ct = this.mtype;
			if (ct.ret() != env.tenv.tpVoid) {
				// check return to be co-variant
				VarianceCheckError err = ct.ret().checkVariance(ct,TypeVariance.CO_VARIANT);
				if (err != null)
					Kiev.reportWarning(this, err.toString());
			}
			for (int i=0; i < ct.arity; i++) {
				// check argument to be contra-variant
				VarianceCheckError err = ct.arg(i).checkVariance(ct,TypeVariance.CONTRA_VARIANT);
				if (err != null)
					Kiev.reportWarning(this, err.toString());
			}
		}
	}
}

@ThisIsANode(name="Method", lang=CoreLang)
public final class MethodImpl extends Method {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in")	Block		body;
	@DataFlowDefinition(in="this:in")	WBCCondition[] 	conditions;
	}
	
	public MethodImpl() {}

	public MethodImpl(String name, Type ret) {
		this(name, new TypeRef(ret),0);
	}
	public MethodImpl(String name, Type ret, int fl) {
		this(name, new TypeRef(ret),fl);
	}
	public MethodImpl(String name, TypeRef type_ret, int fl) {
		super(name, type_ret, fl);
	}
}

@ThisIsANode(name="Getter", lang=CoreLang)
public final class MethodGetter extends Method {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in")	Block		body;
	@DataFlowDefinition(in="this:in")	WBCCondition[] 	conditions;
	}
	
	public MethodGetter() {}

	public MethodGetter(Field f) {
		super(nameGet+f.sname, new TypeRef(f.getType(Env.getEnv())), f.getJavaFlags());
		this.pos = f.pos;
	}
}

@ThisIsANode(name="Setter", lang=CoreLang)
public final class MethodSetter extends Method {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in")	Block		body;
	@DataFlowDefinition(in="this:in")	WBCCondition[] 	conditions;
	}
	
	public MethodSetter() {}

	public MethodSetter(Field f) {
		super(nameSet+f.sname, new TypeRef(Env.getEnv().tenv.tpVoid), f.getJavaFlags());
		this.pos = f.pos;
		this.params += new LVar(f.pos,"value",f.getType(Env.getEnv()),Var.VAR_LOCAL,0);
	}
}

@ThisIsANode(name="Ctor", lang=CoreLang)
public final class Constructor extends Method {
	
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]			addstats;
	@DataFlowDefinition(in="this:in")				Block			body;
	@DataFlowDefinition(in="this:in")				WBCCondition[] 	conditions;
	}

	@nodeAttr public ENode∅				addstats;

	public Constructor() {}

	public Constructor(int fl) {
		super(null, new TypeRef(Env.getEnv().tenv.tpVoid), fl);
	}
	
	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change && info.slot.name == "sname") {
			assert(this.sname == null);
			if (this.sname != null)
				this.sname = null; // constructors are anonymouse
		}
		super.callbackChanged(info);
	}

	public Method makeAccessor(Env env) {
		assert(isPrivate());
		if (caller_from_inner != null)
			return caller_from_inner;
		MethodImpl m = new MethodImpl(Env.ctxTDecl(this).allocateAccessName(), Env.ctxTDecl(this).getType(env), ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		NewExpr ne = new NewExpr(pos, new TypeRef(Env.ctxTDecl(this).getType(env)), ENode.emptyArray);
		foreach (Var v; this.params) {
			v = new Copier().copyFull(v);
			m.params += v;
			ne.args += new LVarExpr(pos,v);
		}
		m.block.stats += ne;
		Env.ctxTDecl(this).members += m;
		Kiev.runProcessorsOn(m);
		this.caller_from_inner = m;
		return m;
	}

}

@ThisIsANode(name="InitBlock", lang=CoreLang)
public final class Initializer extends DNode {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")		ENode		body;
	}

	@nodeAttr public ENode				body;
	@virtual @abstract
	public:ro			Block			block;

	@getter public final Block get$block()	{ return (Block)this.body; }

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this")
			return false;
		return super.includeInDump(dump, attr, val);
	}

}

public enum WBCType {
	CondUnknown,
	CondRequire,
	CondEnsure,
	CondInvariant;
}

@ThisIsANode(lang=CoreLang)
public final class WBCCondition extends DNode {
	
	@DataFlowDefinition(out="body") private static class DFI {
	@DataFlowDefinition(in="this:in")			ENode		body;
	}
	
	public static final WBCCondition[]	emptyArray = new WBCCondition[0];

	@nodeAttr public WBCType				cond;
	@nodeAttr public ENode				body;
	@nodeData public Method				definer;

	public WBCCondition() {}

	public WBCCondition(int pos, WBCType cond, String name, ENode body) {
		this.pos = pos;
		this.sname = name;
		this.cond = cond;
		this.body = body;
	}
}

