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

import kiev.be.java15.CodeAttr;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public abstract class Method extends DNode implements ScopeOfNames,ScopeOfMethods,GlobalDNode {
	@virtual typedef This  ≤ Method;
	
	//public static final SpaceRefDataAttrSlot<Field> ATTR_VIOLATED_FIELDS = new SpaceRefDataAttrSlot<Field>("violated fields",false,TypeInfo.newTypeInfo(Field.class,null));	

	@nodeAttr public TypeConstr[]			targs;
	@nodeAttr public TypeRef				type_ret;
	@nodeAttr public TypeRef				dtype_ret;
	@nodeAttr public Var[]					params;
	@nodeAttr public Symbol[]				aliases;
	@nodeAttr public ENode					body;
	@nodeAttr public WBCCondition[]	 	conditions;
	@nodeAttr(ext_data=true) public Var	ret_var;

	@nodeData(ext_data=true)
	public Method		caller_from_inner;

	private				CallType		_type;
	private				CallType		_dtype;
	@abstract @virtual
	public:ro			CallType		type;
	@abstract @virtual
	public:ro			CallType		dtype;
	@abstract @virtual
	public:ro			CallType		etype;

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "params") {
			Var p = (Var)data;
			if (ct == ChildChangeType.ATTACHED && p.kind == Var.VAR_LOCAL)
				p.meta.var_kind = Var.PARAM_NORMAL;
			else if (ct == ChildChangeType.DETACHED && p.kind == Var.PARAM_NORMAL)
				p.meta.var_kind = Var.VAR_LOCAL;
		}
		if (isAttached()) {
			if      (attr.name == "params")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
			else if (attr.name == "conditions")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		if (attr.name == "params" || attr.name == "type_ret" || attr.name == "dtype_ret" || attr.name == "meta") {
			_type = null;
			_dtype = null;
		}
		super.callbackChildChanged(ct, attr, data);
	}

	@getter public final CallType				get$type()	{ if (this._type == null) rebuildTypes(); return this._type; }
	@getter public final CallType				get$dtype()	{ if (this._dtype == null) rebuildTypes(); return this._dtype; }
	@getter public final CallType				get$etype()	{ if (this._dtype == null) rebuildTypes(); return (CallType)this._dtype.getErasedType(); }

	@getter public final Block					get$block()	{ return (Block)this.body; }

	public String qname() {
		ANode p = parent();
		if (p == null || p instanceof Env)
			return sname;
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+'\u001f'+sname);
		return sname;
	}

	public Var getRetVar() {
		Var retvar = this.ret_var;
		if( retvar == null ) {
			retvar = new LVar(pos,nameResultVar,type_ret.getType(),Var.VAR_LOCAL,ACC_FINAL);
			this.ret_var = retvar;
		}
		return retvar;
	}

	public MetaThrows getMetaThrows() {
		return (MetaThrows)getMeta("kiev\u001fstdlib\u001fmeta\u001fthrows");
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
		return this.meta.is_mth_varargs;
	}
	public final void setVarArgs(boolean on) {
		if (this.meta.is_mth_varargs != on) {
			this.meta.is_mth_varargs = on;
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
		}
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
		Vector<Type> args = new Vector<Type>();
		Vector<Type> dargs = new Vector<Type>();
		boolean is_varargs = false;
		foreach (Var fp; params) {
			TypeRef fpdtype = fp.stype;
			if (fpdtype == null)
				fpdtype = fp.vtype;
			switch (fp.kind) {
			case Var.PARAM_NORMAL:
				args.append(fp.type);
				dargs.append(fpdtype.getType());
				break;
			case Var.PARAM_OUTER_THIS:
				assert(this instanceof Constructor);
				assert(!this.isStatic());
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.sname == nameThisDollar);
				//assert(fp.type ≈ this.ctx_tdecl.package_clazz.dnode.xtype); // not true for View-s
				dargs.append(fp.type);
				break;
			case Var.PARAM_RULE_ENV:
				assert(this instanceof RuleMethod);
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.type ≡ Type.tpRule);
				assert(fp.sname == namePEnv);
				dargs.append(Type.tpRule);
				break;
			case Var.PARAM_ENUM_NAME:
				assert(this instanceof Constructor && !this.isStatic());
				assert(fp.type ≈ Type.tpString);
				dargs.append(Type.tpString);
				break;
			case Var.PARAM_ENUM_ORD:
				assert(this instanceof Constructor && !this.isStatic());
				assert(fp.type ≈ Type.tpInt);
				dargs.append(Type.tpInt);
				break;
			case Var.PARAM_TYPEINFO:
				assert(this instanceof Constructor || (this.isStatic() && this.hasName(nameNewOp)));
				assert(fp.isFinal());
				assert(fpdtype == null || fpdtype.getType() ≈ fp.getType());
				dargs.append(fp.type);
				break;
			case Var.PARAM_VARARGS:
				//assert(fp.isFinal());
				assert(fp.type.isInstanceOf(Type.tpArray));
				args.append(fp.type);
				dargs.append(fp.type);
				is_varargs = true;
				break;
			case Var.PARAM_LVAR_PROXY:
				assert(this instanceof Constructor);
				assert(fp.isFinal());
				dargs.append(fp.type);
				break;
			default:
				if (fp.kind >= Var.PARAM_TYPEINFO_N && fp.kind < Var.PARAM_TYPEINFO_N+128) {
					assert(this.meta.is_type_unerasable);
					assert(fp.isFinal());
					assert(fp.type ≈ Type.tpTypeInfo);
					dargs.append(fp.type);
					break;
				}
				throw new CompilerException(fp, "Unknown kind of the formal parameter "+fp);
			}
		}
		if (is_varargs != this.isVarArgs())
			this.setVarArgs(is_varargs);
		Type tp_ret, dtp_ret;
		if (type_ret == null)
			tp_ret = Type.tpVoid;
		else
			tp_ret = type_ret.getType();
		if (dtype_ret == null)
			dtp_ret = tp_ret;
		else
			dtp_ret = dtype_ret.getType();
		
		this._type = new CallType(this, args.toArray(), tp_ret);
		this._dtype = new CallType(this, dargs.toArray(), dtp_ret);
	}

	@getter public Method get$child_ctx_method() { return this; }

	public static final Method[]	emptyArray = new Method[0];

	public Method() {}
	public Method(String name, TypeRef type_ret, int flags) {
		this.sname = name;
		this.type_ret = type_ret;
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) setMeta(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) setMeta(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) setMeta(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) setMeta(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) setMeta(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			if ((flags & ACC_NATIVE) == ACC_NATIVE) setMeta(new MetaNative());
			if ((flags & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED) setMeta(new MetaSynchronized());
			if ((flags & ACC_BRIDGE) == ACC_BRIDGE) setMeta(new MetaBridge());
			if ((flags & ACC_VARARGS) == ACC_VARARGS) setMeta(new MetaVarArgs());
			if ((flags & ACC_TYPE_UNERASABLE) == ACC_TYPE_UNERASABLE) setMeta(new MetaUnerasable());
			this.meta.mflags = flags;
		}
	}
	
	public void initForEditor() {
		if (sname == null)
			sname = "<name>";
		if (type_ret == null)
			type_ret = new TypeRef(Type.tpVoid);
		super.initForEditor();
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "body") {
			if (this.isMacro() || this.ctx_tdecl.isMixin()) // save for macroses and trait/mixin
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
		foreach(Symbol s; aliases; s.sname == nm)
			return true;
		return false;
	}
	public boolean hasNameStart(String nm) {
		String sname = this.sname;
		if (sname == null)
			return false;
		if (sname.startsWith(nm)) return true;
		foreach(Symbol s; aliases; s.sname.startsWith(nm))
			return true;
		return false;
	}

	public ISymbol getSymbol(String nm) {
		if (sname == nm) return this;
		foreach(Symbol s; aliases; s.sname == nm)
			return s;
		assert (false, "Symbol "+nm+" not found in "+this);
		return null;
	}

	public Type	getType() { return type; }

	public Var getOuterThisParam() {
		Type t = this.type; // rebuildTypes()
		foreach (Var fp; params; fp.kind == Var.PARAM_OUTER_THIS)
			return fp;
		return null;
	}
	
	public Var getTypeInfoParam(int kind) {
		Type t = this.type; // rebuildTypes()
		foreach (Var fp; params; fp.kind == kind)
			return fp;
		return null;
	}
	
	public Var getVarArgParam() {
		Type t = this.type; // rebuildTypes()
		foreach (Var fp; params; fp.kind == Var.PARAM_VARARGS)
			return fp;
		return null;
	}
	
	public void addViolatedField(Field f) {
/*		if (!this.ctx_tdecl.instanceOf(f.ctx_tdecl))
			return;
		Field[] violated_fields = Method.ATTR_VIOLATED_FIELDS.get((Method)this);
		if( isInvariantMethod() ) {
			if (Field.ATTR_INVARIANT_CHECKERS.indexOf(f,this) < 0)
				Field.ATTR_INVARIANT_CHECKERS.add(f,this);
			if( this.ctx_tdecl.instanceOf(f.ctx_tdecl) ) {
				if (Method.ATTR_VIOLATED_FIELDS.indexOf(this, f) < 0)
					Method.ATTR_VIOLATED_FIELDS.add(this,f);
			}
		} else {
			if (Method.ATTR_VIOLATED_FIELDS.indexOf(this,f) < 0)
				Method.ATTR_VIOLATED_FIELDS.add(this,f);
		}
*/	}

	public boolean preResolveIn() {
		//foreach (Var fp; params; fp.kind == Var.VAR_LOCAL)
		//	fp.meta.var_kind = Var.PARAM_NORMAL;
		Type t = this.type; // rebuildTypes()
		return true;
	}

	public boolean mainResolveIn() {
		Type t = this.type; // rebuildTypes()
		return true;
	}

	public boolean preVerify() {
		TypeDecl ctx_tdecl = this.ctx_tdecl;
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
		return true;
	}

	public boolean backendCleanup() {
		//if (Method.ATTR_VIOLATED_FIELDS.get(this) != null)
		//	Method.ATTR_VIOLATED_FIELDS.clear(this);
		return super.backendCleanup();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(sname+"(");
		int n = params.length;
		boolean comma = false;
		foreach (Var fp; params; fp.kind == Var.PARAM_NORMAL || fp.kind == Var.PARAM_VARARGS) {
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

	public void normilizeExpr(ENode expr) {
		if (body instanceof CoreExpr && ((CoreExpr)body).core_func != null) {
			((CoreExpr)body).core_func.normilizeExpr(this,expr);
			return;
		}
		if (expr.ident == null) {
			Operator op = expr.getOp();
			if (op != null)
				expr.ident = op.name;
			else
				expr.ident = this.sname;
		}
		expr.symbol = this;
		if (!isMacro())
			return;
		UserMeta m = (UserMeta)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fCompilerNode");
		if (m == null)
			return;
		Class cls = ASTNodeMetaType.allNodes.get(m.getS("value"));
		if (cls == null) {
			Kiev.reportWarning(expr,"Compiler node '"+m.getS("value")+"' does not exists");
			return;
		}
		if (expr.getClass() == cls)
			return;
		ENode[] args = expr.getArgs();
		if (args == null) {
			Kiev.reportError(expr, "Don't know how to normalize "+expr.getClass()+" into "+cls);
			return;
		}
		ENode en = (ENode)cls.newInstance();
		foreach (ENode e; args)
			e.detach();
		Operator op = expr.getOp();
		if (op == null && this.isOperatorMethod())
			op = Operator.lookupOperatorForMethod(this);
		en.initFrom(expr, op, this, args);
		expr.replaceWithNodeReWalk(en);
		
	}

	public void makeArgs(ENode[] args, Type t) {
		CallType mt = this.type;
		//assert(args.getPSlot().is_attr);
		if( isVarArgs() ) {
			int i=0;
			for(; i < mt.arity-1; i++) {
				Type ptp = Type.getRealType(t,mt.arg(i));
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
			Type tn = Type.getRealType(t,getVarArgParam().type);
			Type varg_tp = tn.resolveArg(0);
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
			} else {
				for(; i < args.length; i++) {
					if !(args[i].getType().isInstanceOf(varg_tp))
						CastExpr.autoCast(CastExpr.autoCastToReference(args[i]),varg_tp);
				}
			}
		} else {
			for(int i=0; i < mt.arity; i++) {
				Type ptp = Type.getRealType(t,mt.arg(i));
				if !(args[i].getType().isInstanceOf(ptp))
					CastExpr.autoCast(args[i],ptp);
			}
		}
	}

	public boolean equalsByCast(String name, CallType mt, Type tp, ResInfo info) {
		if (!this.hasName(name)) return false;
		int type_len = this.type.arity;
		int args_len = mt.arity;
		if( type_len != args_len ) {
			if( !isVarArgs() ) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" differ in number of params: "+type_len+" != "+args_len);
				return false;
			} else if( type_len-1 > args_len ) {
				trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					+" not match in number of params: "+type_len+" != "+args_len);
				return false;
			}
		}
		trace(Kiev.debug && Kiev.debugResolve,"Compare method "+this+" and "+Method.toString(name,mt));
		CallType rt = (CallType)this.type.applay(tp);
		if (!this.isStatic() && tp != null && tp != Type.tpVoid)
			rt = (CallType)rt.rebind(new TVarBld(StdTypes.tpCallThisArg, tp));
		
		if ((mt.getArgsLength() - mt.arity - 1) > 0) {
			TVarBld set = new TVarBld();
			foreach (TypeConstr tc; this.targs) {
				ArgType arg = tc.getAType();
				Type bound = mt.resolve(arg);
				if!(bound.isInstanceOf(arg)) {
					trace(Kiev.debug && Kiev.debugResolve,"Type "+bound+" is not applayable to "+arg);
					return false;
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
				return false;
			}
		}

		foreach (ArgType at; rt.getTArgs()) {
			Vector<Type> bindings = new Vector<Type>();
			// bind from mt
			for (int i=0; i < rt.arity && i < mt.arity; i++)
				addBindingsFor(at, mt.arg(i), rt.arg(i), bindings);
			addBindingsFor(at, mt.ret(), rt.ret(), bindings);
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
			if (b ≡ Type.tpAny)
				return false;
			rt = (CallType)rt.applay(new TVarBld(at, b));
		}
		// check bindings are correct
		int n = rt.getArgsLength();
		for (int i=0; i < n; i++) {
			ArgType var = rt.getArg(i);
			Type val = rt.resolveArg(i);
			if (!var.checkBindings(rt, val)) {
				trace(Kiev.debug && Kiev.debugResolve,"Incorrect bindings for var "+var+" with value "+val+" in type "+rt);
				return false; 
			}
		}
		
		if (mt.ret() ≢ Type.tpAny && rt.ret().getAutoCastTo(mt.ret()) == null) {
			trace(Kiev.debug && Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				+" differ in return type : "+rt.ret()+" not auto-castable to "+mt.ret());
			return false;
		}
		
		trace(Kiev.debug && Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+" match as "+rt);
		info.resolved_type = rt;
		return true;
	}
	
	public final CallType makeType(TypeRef[] targs, ENode[] args) {
		Type[] mt;
		CallType rt;
		if (this.isStatic()) {
			mt = new Type[args.length];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i].getType();
			rt = (CallType)this.type;
		} else {
			mt = new Type[args.length-1];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i+1].getType();
			rt = (CallType)this.type.applay(args[0].getType());
		}
		if (targs != null && targs.length > 0) {
			TVarBld set = new TVarBld();
			for (int i=0; i < targs.length && i < this.targs.length; i++) {
				Type bound = targs[i].getType();
				ArgType arg = this.targs[i].getAType();
				set.append(arg, bound);
			}
			rt = (CallType)rt.applay(set);
		}
		foreach (ArgType at; rt.getTArgs()) {
			Vector<Type> bindings = new Vector<Type>();
			// bind from mt
			for (int i=0; i < rt.arity && i < mt.length; i++)
				addBindingsFor(at, mt[i], rt.arg(i), bindings);
			if (bindings.length == 0)
				continue;
			Type b = bindings.at(0);
			for (int i=1; i < bindings.length; i++)
				b = Type.leastCommonType(b, bindings.at(i));
			if (b ≡ Type.tpAny)
				continue;
			rt = (CallType)rt.applay(new TVarBld(at, b));
		}
		return rt;
	}

	// compares pattern type (pt) with query type (qt) to find bindings for argument type (at),
	// and adds found bindings to the set of bindings
	private static void addBindingsFor(ArgType at, Type pt, Type qt, Vector<Type> bindings) {
		if (pt ≡ null || pt ≡ Type.tpAny || pt ≡ at)
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
			if (qt.isAliasArg(i))
				continue;
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
				if (bnd ≢ qtvar)
					addBindingsFor(at, bnd, qtval, bindings);
			}
		}
		return;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		LVar@ var;
	{
		isInlinedByDispatcherMethod() , $cut, false
	;
		path.space_prev.pslot().name == "targs" ||
		path.space_prev.pslot().name == "params" ||
		path.space_prev.pslot().name == "type_ref" ||
		path.space_prev.pslot().name == "dtype_ref",
		$cut,
		node @= targs,
		path.checkNodeName(node)
	;
		var @= params,
		path.checkNodeName(var),
		node ?= var
	;
		path.getName() == nameResultVar,
		node ?= ret_var
	;
		node @= targs,
		path.checkNodeName(node)
	;
		!this.isStatic() && path.isForwardsAllowed(),
		path.enterForward(ThisExpr.thisPar) : path.leaveForward(ThisExpr.thisPar),
		this.ctx_tdecl.xtype.resolveNameAccessR(node,path)
	;
		path.isForwardsAllowed(),
		var @= params,
		var.isForward(),
		path.enterForward(var) : path.leaveForward(var),
		var.getType().resolveNameAccessR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		Var@ n;
	{
		info.isForwardsAllowed(),
	{
		!this.isStatic(),
		info.enterForward(ThisExpr.thisPar) : info.leaveForward(ThisExpr.thisPar),
		this.ctx_tdecl.xtype.resolveCallAccessR(node,info,mt)
	;
		n @= params,
		n.isForward(),
		info.enterForward(n) : info.leaveForward(n),
		n.getType().resolveCallAccessR(node,info,mt)
	}
	}

    public void pass3() {
		if !( this.parent() instanceof TypeDecl )
			throw new CompilerException(this,"Method must be declared on class level only");
		TypeDecl clazz = this.ctx_tdecl;
		// TODO: check flags for methods
		if( clazz.isPackage() && !isStatic() ) { setStatic(true); }
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
			foreach (Var fp; params; fp.kind >= Var.PARAM_TYPEINFO_N) {
				fp.detach();
			}
			int i = 0;
			foreach (TypeDef td; targs) {
				td.setTypeUnerasable(true);
				LVar v = new LVar(td.pos,nameTypeInfo+"$"+td.sname, Type.tpTypeInfo, Var.PARAM_TYPEINFO_N+i, ACC_FINAL|ACC_SYNTHETIC);
				params.add(v);
			}
		}

		// push the method, because formal parameters may refer method's type args
		foreach (Var fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype != null)
				fp.stype.getType(); // resolve
			if (fp.meta != null)
				fp.meta.verify();
			//if (fp.kind == Var.VAR_LOCAL)
			//	fp.meta.var_kind = Var.PARAM_NORMAL;
		}

		Type t = this.type; // rebuildTypes()
		trace(Kiev.debug && Kiev.debugMultiMethod,"Method "+this+" has dispatcher type "+this.dtype);
		meta.verify();
		if (body instanceof MetaValue)
			((MetaValue)body).verify();
		foreach(ASTOperatorAlias al; aliases) al.pass3();

		foreach(WBCCondition cond; conditions) {
			if (cond.definer != this)
				cond.definer = this;
		}

		if (isMacro() && isNative() && body == null) {
			String name = clazz.qname().replace('\u001f','.')+":"+sname;
			body = CoreExpr.makeInstance(pos,name);
		}
	}

	public void resolveMetaDefaults() {
		if (body instanceof MetaValue) {
			Type tp = this.type_ret.getType();
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
				if (t.getStruct() == null || !(t ≈ Type.tpString || t ≈ Type.tpClass || t.getStruct().isAnnotation() || t.getStruct().isEnum()))
					throw new CompilerException(body, "Bad annotation value type "+tp);
			}
			((MetaValue)body).resolve(t);
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

	public Method makeAccessor() {
		assert(isPrivate());
		if (caller_from_inner != null)
			return caller_from_inner;
		MethodImpl m = new MethodImpl(ctx_tdecl.allocateAccessName(), type_ret.getType(), ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		CallExpr ce;
		if (isStatic()) {
			ce = new CallExpr(pos, new TypeRef(ctx_tdecl.xtype), this, ENode.emptyArray);
		} else {
			Var self = new LVar(pos,Constants.nameThis,ctx_tdecl.xtype,Var.PARAM_NORMAL,0);
			m.params += self;
			ce = new CallExpr(pos, new LVarExpr(pos,self), this, ENode.emptyArray);
		}
		foreach (Var v; this.params) {
			v = v.ncopy();
			m.params += v;
			ce.args += new LVarExpr(pos,v);
		}
		m.block.stats += ce;
		ctx_tdecl.members += m;
		Kiev.runProcessorsOn(m);
		this.caller_from_inner = m;
		return m;
	}

	public void postVerify() {
		if (!isStatic() && !isPrivate()) {
			CallType ct = this.type;
			if (ct.ret() != StdTypes.tpVoid) {
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
	
	@virtual typedef This  = MethodImpl;

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

@ThisIsANode(name="Ctor", lang=CoreLang)
public final class Constructor extends Method {
	
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")	ENode[]			addstats;
	@DataFlowDefinition(in="this:in")				Block			body;
	@DataFlowDefinition(in="this:in")				WBCCondition[] 	conditions;
	}

	@virtual typedef This  = Constructor;

	@nodeAttr public ENode[]				addstats;

	public Constructor() {}

	public Constructor(int fl) {
		super(null, new TypeRef(Type.tpVoid), fl);
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "sname") {
			assert(this.sname == null);
			if (this.sname != null)
				this.sname = null; // constructors are anonymouse
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public Method makeAccessor() {
		assert(isPrivate());
		if (caller_from_inner != null)
			return caller_from_inner;
		MethodImpl m = new MethodImpl(ctx_tdecl.allocateAccessName(), ctx_tdecl.xtype, ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		NewExpr ne = new NewExpr(pos, new TypeRef(ctx_tdecl.xtype), ENode.emptyArray);
		foreach (Var v; this.params) {
			v = v.ncopy();
			m.params += v;
			ne.args += new LVarExpr(pos,v);
		}
		m.block.stats += ne;
		ctx_tdecl.members += m;
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

	@virtual typedef This  = Initializer;

	@nodeAttr public ENode				body;

	@getter public final Block get$block()	{ return (Block)this.body; }

	public void initForEditor() {
		if (body == null)
			body = new Block();
		super.initForEditor();
	}

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

	@virtual typedef This  = WBCCondition;

	@nodeAttr public WBCType				cond;
	@nodeAttr public ENode				body;
	@nodeData public Method				definer;
	     public CodeAttr			code_attr;

	public WBCCondition() {}

	public WBCCondition(int pos, WBCType cond, String name, ENode body) {
		this.pos = pos;
		this.sname = name;
		this.cond = cond;
		this.body = body;
	}
}

