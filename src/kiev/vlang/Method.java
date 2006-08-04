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
import kiev.ir.java15.RCoreMethod;
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

@node(name="Method")
public class Method extends DNode implements ScopeOfNames,ScopeOfMethods,PreScanneable {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in")	Block		body;
	@dflow(in="this:in")	WBCCondition[] 	conditions;
	}

	public static final AttrSlot ATTR_RET_VAR = new TmpAttrSlot("method ret var",true,false,TypeInfo.newTypeInfo(Var.class,null));	
	public static final SpaceRefDataAttrSlot<Field> ATTR_VIOLATED_FIELDS = new SpaceRefDataAttrSlot<Field>("violated fields",false,TypeInfo.newTypeInfo(Field.class,null));	

	@virtual typedef This  ≤ Method;
	@virtual typedef JView = JMethod;
	@virtual typedef RView ≤ RMethod;

	@att public TypeDef[]			targs;
	@att public TypeRef				type_ret;
	@att public TypeRef				dtype_ret;
	@att public FormPar[]			params;
	@att public Symbol[]			aliases;
	@att public ENode				body;
	public kiev.be.java15.Attr[]		attrs = kiev.be.java15.Attr.emptyArray;
	@att public WBCCondition[]	 	conditions;


	@virtual public					CallType		type;
	@virtual public					CallType		dtype;
	@abstract
	@virtual public:ro				CallType		etype;

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "params")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "conditions")
				parent().callbackChildChanged(pslot());
			else
				super.callbackChildChanged(attr);
		} else {
			super.callbackChildChanged(attr);
		}
		if (attr.name == "params" || attr.name == "meta") {
			type = null;
			dtype = null;
		}
	}

	@getter public final CallType				get$type()	{ if (this.type == null) rebuildTypes(); return this.type; }
	@getter public final CallType				get$dtype()	{ if (this.dtype == null) rebuildTypes(); return this.dtype; }
	@getter public final CallType				get$etype()	{ if (this.dtype == null) rebuildTypes(); return (CallType)this.dtype.getErasedType(); }

	@getter public final Block					get$block()	{ return (Block)this.body; }

	public Var getRetVar() {
		Var retvar = (Var)ATTR_RET_VAR.get(this);
		if( retvar == null ) {
			retvar = new Var(pos,nameResultVar,type_ret.getType(),ACC_FINAL);
			ATTR_RET_VAR.set(this, retvar);
		}
		return retvar;
	}

	public MetaThrows getMetaThrows() {
		return (MetaThrows)meta.getU("kiev.stdlib.meta.throws");
	}

	// virtual static method
	public final boolean isVirtualStatic() {
		return this.is_mth_virtual_static;
	}
	public final void setVirtualStatic(boolean on) {
		if (this.is_mth_virtual_static != on) {
			assert(!locked);
			this.is_mth_virtual_static = on;
			if (!isStatic()) this.setStatic(true);
		}
	}
	// method with variable number of arguments	
	public final boolean isVarArgs() {
		return this.is_mth_varargs;
	}
	public final void setVarArgs(boolean on) {
		if (this.is_mth_varargs != on) {
			assert(!locked);
			this.is_mth_varargs = on;
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
			assert(!locked);
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
			assert(!locked);
			this.is_mth_invariant = on;
		}
	}
	// a dispatcher (for multimethods)	
	public final boolean isDispatcherMethod() {
		return this.is_mth_dispatcher;
	}
	public final void setDispatcherMethod(boolean on) {
		if (this.is_mth_dispatcher != on) {
			assert(!locked);
			this.is_mth_dispatcher = on;
		}
	}
	// a methood inlined bt dispatcher (for multimethods)	
	public final boolean isInlinedByDispatcherMethod() {
		return this.is_mth_inlined_by_dispatcher;
	}
	public final void setInlinedByDispatcherMethod(boolean on) {
		if (this.is_mth_inlined_by_dispatcher != on) {
			this.is_mth_inlined_by_dispatcher = on;
		}
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
			type_set.append(StdTypes.tpCallThisArg,null);
			type_set.append(ctx_tdecl.xtype.meta_type.getTemplBindings());
			dtype_set.append(StdTypes.tpCallThisArg,null);
			dtype_set.append(ctx_tdecl.xtype.meta_type.getTemplBindings());
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
				assert(fp.u_name == nameThisDollar);
				assert(fp.type ≈ this.ctx_tdecl.package_clazz.xtype);
				dargs.append(this.ctx_tdecl.package_clazz.xtype);
				break;
			case FormPar.PARAM_RULE_ENV:
				assert(this instanceof RuleMethod);
				assert(fp.isForward());
				assert(fp.isFinal());
				assert(fp.type ≡ Type.tpRule);
				assert(fp.u_name == namePEnv);
				dargs.append(Type.tpRule);
				break;
			case FormPar.PARAM_TYPEINFO:
				assert(this instanceof Constructor || (this.isStatic() && this.hasName(nameNewOp,true)));
				assert(fp.isFinal());
				assert(fp.stype == null || fp.getSType() ≈ fp.getType());
				dargs.append(fp.type);
				break;
			case FormPar.PARAM_VARARGS:
				//assert(fp.isFinal());
				assert(fp.type.isInstanceOf(Type.tpArray));
				args.append(fp.type);
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
	}

	@getter public Method get$child_ctx_method() { return (Method)this; }


	public static Method[]	emptyArray = new Method[0];

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
	public Method(Symbol id, TypeRef type_ret, int flags) {
		assert (!(id.equals(nameInit) || id.equals(nameClassInit)) || this instanceof Constructor);
		this.u_name = id.sname;
		this.id = id;
		this.type_ret = type_ret;
		this.dtype_ret = type_ret.ncopy();
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) meta.setF(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) meta.setF(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) meta.setF(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) meta.setF(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) meta.setF(new MetaFinal());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) meta.setF(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) meta.setF(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) meta.setF(new MetaMacro());
			if ((flags & ACC_NATIVE) == ACC_NATIVE) meta.setF(new MetaNative());
			if ((flags & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED) meta.setF(new MetaSynchronized());
			if ((flags & ACC_BRIDGE) == ACC_BRIDGE) meta.setF(new MetaBridge());
			if ((flags & ACC_VARARGS) == ACC_VARARGS) meta.setF(new MetaVarArgs());
			if ((flags & ACC_TYPE_UNERASABLE) == ACC_TYPE_UNERASABLE) meta.setF(new MetaUnerasable());
			this.flags = flags;
		}
	}

	public boolean hasName(String nm, boolean by_equals) {
		if (by_equals) {
			if (this.u_name == nm) return true;
			if (id.sname == nm) return true;
			foreach(Symbol s; aliases; s.sname == nm)
				return true;
		} else {
			if (this.u_name != null && this.u_name.startsWith(nm)) return true;
			if (id.sname != null && id.sname.startsWith(nm)) return true;
			foreach(Symbol s; aliases; s.sname.startsWith(nm))
				return true;
		}
		return false;
	}

	public Symbol<Method> getSymbol(String nm) {
		if (id.sname == nm) return (Symbol<Method>)id;
		foreach(Symbol s; aliases; s.sname == nm)
			return (Symbol<Method>)s;
		assert (false, "Symbol "+nm+" not found in "+this);
		return null;
	}

	public Type	getType() { return type; }

	public FormPar getOuterThisParam() {
		Type t = this.type; // rebuildTypes()
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_OUTER_THIS)
			return fp;
		return null;
	}
	
	public FormPar getTypeInfoParam(int kind) {
		Type t = this.type; // rebuildTypes()
		foreach (FormPar fp; params; fp.kind == kind)
			return fp;
		return null;
	}
	
	public FormPar getVarArgParam() {
		Type t = this.type; // rebuildTypes()
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_VARARGS)
			return fp;
		return null;
	}
	
	public void addViolatedField(Field f) {
		if (!this.ctx_tdecl.instanceOf(f.ctx_tdecl))
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
	}

	public boolean preResolveIn() {
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

	public String toString() {
		StringBuffer sb = new StringBuffer(id+"(");
		int n = params.length;
		boolean comma = false;
		foreach (FormPar fp; params; fp.kind == FormPar.PARAM_NORMAL || fp.kind == FormPar.PARAM_VARARGS) {
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
		if (expr.ident.name == null) {
			Operator op = expr.getOp();
			if (op != null)
				expr.ident.name = op.name;
			else
				expr.ident.name = this.id.sname;
		}
		expr.ident.symbol = this.id;
		if (!isMacro())
			return;
		UserMeta m = (UserMeta)this.meta.getU("kiev.stdlib.meta.CompilerNode");
		if (m == null)
			return;
		Struct s = TypeExpr.AllNodes.get(m.getS("value"));
		if (s == null) {
			Kiev.reportWarning(expr,"Compiler node '"+m.getS("value")+"' does not exists");
			return;
		}
		Class cls = Class.forName(s.qname());
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
			Type varg_tp = Type.getRealType(t,getVarArgParam().type).tvars[0].unalias().result();
			if (args.length == i+1 && args[i].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
			} else {
				for(; i < args.length; i++) {
					if !(args[i].getType().isInstanceOf(varg_tp)) {
						CastExpr.autoCastToReference(args[i]);
						CastExpr.autoCast(args[i],varg_tp);
					}
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
		if (!this.hasName(name,true)) return false;
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
		if (!this.isStatic() && tp != null && tp != Type.tpVoid) {
			rt = (CallType)rt.rebind(new TVarBld(StdTypes.tpCallThisArg, tp));
		}
		
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
				rt = (CallType)rt.rebind(set);
		}
		
		for(int i=0; i < (isVarArgs()?type_len-1:type_len); i++) {
			if (mt.arg(i).getAutoCastTo(rt.arg(i)) == null) {
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
					addBindingsFor(at, mt.arg(i), rt.arg(i), bindings);
				addBindingsFor(at, mt.ret(), rt.ret(), bindings);
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
				rt = (CallType)rt.rebind(new TVarBld(at, b));
			}
		}
		
		if (mt.ret() ≢ Type.tpAny && rt.ret().getAutoCastTo(mt.ret()) == null) {
			trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				+" differ in return type : "+rt.ret()+" not auto-castable to "+mt.ret());
			return false;
		}
		
		trace(Kiev.debugResolve,"Method "+this+" and "+Method.toString(name,mt)+" match as "+rt);
		info.mt = rt;
		return true;
	}
	
	public final CallType makeType(TypeRef[] targs, ENode[] args) {
		Type[] mt;
		CallType rt;
		if (this.isStatic()) {
			mt = new Type[args.length];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i].getType();
			rt = this.type;
		} else {
			mt = new Type[args.length-1];
			for (int i=0; i < mt.length; i++)
				mt[i] = args[i+1].getType();
			rt = (CallType)this.type.bind(args[0].getType().bindings());
		}
		if (targs != null && targs.length > 0) {
			TVarBld set = new TVarBld();
			int a = 0;
			foreach (TVar tv; rt.bindings().tvars) {
				if (tv.var.isHidden())
					continue;
				Type bound = targs[a].getType();
				ArgType arg = this.targs[a].getAType();
				set.append(arg, bound);
				a++;
			}
			if (a > 0)
				rt = (CallType)rt.rebind(set);
		}
		foreach (TypeDef td; this.targs) {
			ArgType at = td.getAType();
			Type bnd = rt.resolve(at);
			if (bnd ≡ at) {
				Vector<Type> bindings = new Vector<Type>();
				// bind from mt
				for (int i=0; i < rt.arity; i++)
					addBindingsFor(at, mt[i], rt.arg(i), bindings);
				//addBindingsFor(at, mt.ret(), rt.ret(), bindings);
				if (bindings.length == 0) {
					//trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
					//	+" do not allow to infer type: "+at);
					continue;
				}
				Type b = bindings.at(0);
				for (int i=1; i < bindings.length; i++)
					b = Type.leastCommonType(b, bindings.at(i));
				//trace(Kiev.debugResolve,"Methods "+this+" and "+Method.toString(name,mt)
				//	+" infer argument: "+at+" to "+b);
				if (b ≡ Type.tpAny)
					continue;
				rt = (CallType)rt.rebind(new TVarBld(at, b));
			}
		}
		return rt;
	}

	// compares pattern type (pt) with query type (qt) to find bindings for argument type (at),
	// and adds found bindings to the set of bindings
	private static void addBindingsFor(ArgType at, Type pt, Type qt, Vector<Type> bindings) {
		if (pt ≡ null || pt ≡ Type.tpAny || pt ≡ at)
			return;
		if (qt ≡ at) {
			Type t = pt.getAutoCastTo(at);
			if (t != null)
				bindings.append(t);
			return;
		}
		if (!qt.hasApplayable(at))
			return;
		final int qt_size = qt.tvars.length;
		for (int i=0; i < qt_size; i++) {
			TVar qtv = qt.tvars[i];
			if (!qtv.isAlias()) {
				if (qtv.val ≡ at) {
					Type bnd = pt.resolve(qtv.var);
					if (bnd ≢ qtv.var && !bindings.contains(bnd)) {
						Type t = bnd.getAutoCastTo(at);
						if (t != null)
							bindings.append(t);
					}
				}
				else if (qtv.val.hasApplayable(at)) {
					Type bnd = pt.resolve(qtv.var);
					if (bnd ≢ qtv.var)
						addBindingsFor(at, bnd, qtv.val, bindings);
				}
			}
		}
		return;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		FormPar@ var;
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
		node ?= (Var)ATTR_RET_VAR.get(this)
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
		var.type.resolveNameAccessR(node,path)
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
		if( clazz.isPackage() && !isStatic() ) { this.open(); setStatic(true); }
		if( isPrivate() && isFinal() ) { this.open(); setFinal(false); }
		else if( clazz.isClazz() && clazz.isFinal() && !isFinal() ) { this.open(); setFinal(true); }
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
			foreach (FormPar fp; params; fp.kind >= FormPar.PARAM_TYPEINFO_N)
				fp.detach();
			int i = 0;
			foreach (TypeDef td; targs) {
				td.setTypeUnerasable(true);
				FormPar v = new FormPar(td.pos,nameTypeInfo+"$"+td.u_name, Type.tpTypeInfo, FormPar.PARAM_TYPEINFO_N+i, ACC_FINAL|ACC_SYNTHETIC);
				params.add(v);
			}
		}

		// push the method, because formal parameters may refer method's type args
		foreach (FormPar fp; params) {
			fp.vtype.getType(); // resolve
			if (fp.stype != null)
				fp.stype.getType(); // resolve
			if (fp.meta != null)
				fp.meta.verify();
		}

		Type t = this.type; // rebuildTypes()
		trace(Kiev.debugMultiMethod,"Method "+this+" has dispatcher type "+this.dtype);
		meta.verify();
		if (body instanceof MetaValue)
			((MetaValue)body).verify();
		foreach(ASTOperatorAlias al; aliases) al.pass3();

		foreach(WBCCondition cond; conditions)
			cond.definer = this;
	}

	public void resolveMetaDefaults() {
		if (body instanceof MetaValue) {
			Type tp = this.type_ret.getType();
			Type t = tp;
			if (t instanceof ArrayType) {
				if (body instanceof MetaValueScalar) {
					MetaValueArray mva = new MetaValueArray(new SymbolRef<DNode>(body.pos, this.id));
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

	public boolean setBody(ENode body) {
		trace(Kiev.debugMultiMethod,"Setting body of methods "+this);
		this.body = body;
		return true;
	}

}

@node(name="Ctor")
public final class Constructor extends Method {
	
	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]			addstats;
	@dflow(in="this:in")				Block			body;
	@dflow(in="this:in")				WBCCondition[] 	conditions;
	}

	@virtual typedef This  = Constructor;
	@virtual typedef RView = RConstructor;

	@att public ENode[]				addstats;

	public Constructor() {}

	public Constructor(int fl) {
		super((fl&ACC_STATIC)==0 ? nameInit:nameClassInit, Type.tpVoid, fl);
		if (isStatic())
			this.u_name = nameClassInit;
		else
			this.u_name = nameInit;
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "id") {
			if (parent() instanceof TypeDecl) {
				TypeDecl td = (TypeDecl)parent();
				if (id != null && id.sname != td.id.sname) {
					id.open();
					id.sname = td.id.sname;
				}
			}
		} else {
			super.callbackChildChanged(attr);
		}
	}

	public void callbackAttached() {
		if (!isStatic() && parent() instanceof TypeDecl) {
			TypeDecl td = (TypeDecl)parent();
			if (id != null && id.sname != td.id.sname) {
				id.open();
				id.sname = ((TypeDecl)parent()).id.sname;
			}
		}
	}
}

@node(name="InitBlock")
public final class Initializer extends DNode implements PreScanneable {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")		ENode		body;
	}

	@virtual typedef This  = Initializer;
	@virtual typedef JView = JInitializer;
	@virtual typedef RView = RInitializer;

	@att public ENode				body;

	@getter public final Block get$block()	{ return (Block)this.body; }

	public Initializer() {}

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
public final class WBCCondition extends DNode {
	
	@dflow(out="body") private static class DFI {
	@dflow(in="this:in")			ENode		body;
	}
	
	public static WBCCondition[]	emptyArray = new WBCCondition[0];

	@virtual typedef This  = WBCCondition;
	@virtual typedef JView = JWBCCondition;
	@virtual typedef RView = RWBCCondition;

	@att public WBCType				cond;
	@att public ENode				body;
	@ref public Method				definer;
	@att public CodeAttr			code_attr;

	public WBCCondition() {}

	public WBCCondition(int pos, WBCType cond, String name, ENode body) {
		this.pos = pos;
		this.id = new Symbol<WBCCondition>(name);
		this.cond = cond;
		this.body = body;
	}

	public boolean setBody(ENode body) {
		this.body = body;
		return true;
	}
}

