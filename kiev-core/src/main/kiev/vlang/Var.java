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
 * @version $Revision: 296 $
 *
 */

@ThisIsANode(name="Var", lang=CoreLang)
public abstract class Var extends DNode {

	public static final int VAR_LOCAL          = 0;
	public static final int FIELD_NORMAL       = 1;
	public static final int PARAM_THIS         = 2;
	public static final int PARAM_OUTER_THIS   = 3;
	public static final int PARAM_RULE_ENV     = 4;
	public static final int PARAM_CLASS_TYPEINFO     = 5;
	public static final int PARAM_ENUM_NAME    = 6;
	public static final int PARAM_ENUM_ORD     = 7;
	public static final int PARAM_LVAR_PROXY   = 8;
	public static final int PARAM_METHOD_TYPEINFO   = 16;

	@nodeAttr
	public TypeRef					vtype;
	@nodeAttr(ext_data=true)
	public TypeRef					stype;
	@nodeAttr
	public ENode					init;
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		getter;
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		setter;
	//@AttrBinDumpInfo(ignore=true)
	//@AttrXMLDumpInfo(ignore=true)
	@nodeData(ext_data=true)
	public ConstExpr				const_value;

	@virtual @abstract
	public:ro int					kind;

	@getter public int get$kind() { return this.mflags_var_kind; }

	// init wrapper
	@getter public final boolean isInitWrapper() {
		return this.is_init_wrapper;
	}
	@setter public final void setInitWrapper(boolean on) {
		if (this.is_init_wrapper != on)
			this.is_init_wrapper = on;
	}
	// need a proxy access
	@getter public final boolean isNeedProxy() {
		return this.is_need_proxy;
	}
	@setter public final void setNeedProxy(boolean on) {
		if (this.is_need_proxy != on) {
			this.is_need_proxy = on;
		}
	}
	// is a field of enum
	public final boolean isEnumField() {
		return this.mflags_is_enum;
	}
	// field's initializer was already added to class initializer
	public final boolean isAddedToInit() {
		return this.is_fld_added_to_init;
	}
	public final void setAddedToInit(boolean on) {
		if (this.is_fld_added_to_init != on) {
			this.is_fld_added_to_init = on;
		}
	}

	public final MetaPacked getMetaPacked() {
		return (MetaPacked)this.getMeta("kiev·stdlib·meta·packed");
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.getMeta("kiev·stdlib·meta·packer");
	}

	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change && isAttached()) {
			if (info.slot.name == "vtype" || info.slot.name == "stype" || info.slot.name == "metas")
				notifyParentThatIHaveChanged();
		}
		super.callbackChanged(info);
	}

	public static final Var[]	emptyArray = new Var[0];

	public Var(int kind) {
		this.mflags_var_kind = kind;
	}

	public Var(String name, TypeRef vtype, int kind, int flags)
		require vtype != null;
	{
		this.sname = name;
		this.vtype = vtype;
		this.nodeflags = flags;
		this.mflags_var_kind = kind;
	}

	public Var(String name, TypeRef vtype, int kind)
		require vtype != null;
	{
		this.sname = name;
		this.vtype = vtype;
		this.mflags_var_kind = kind;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "init")
			return false;
		if (attr.name == "const_value")
			return false;
		return super.includeInDump(dump,attr,val);
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		verifyMetas();
		ENode init = this.init;
		if (init != null && init instanceof NewInitializedArrayExpr && init.ntype == null) {
			Type tp = getType(env);
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Scalar var is initialized by array");
			else
				init.setType((ArrayType)tp);
		}
		return true;
	}

	public boolean	isConstantExpr(Env env) {
		if (this.isFinal()) {
			if (this.init != null && this.init.isConstantExpr(env))
				return true;
			else if (this.const_value != null)
				return true;
		}
		return false;
	}

	public Object	getConstValue(Env env) {
		if (this.init != null && this.init.isConstantExpr(env))
			return this.init.getConstValue(env);
		else if (this.const_value != null)
			return this.const_value.getConstValue(env);
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		if (this.init != null) {
			if (isConstantExpr(env)) {
				ConstExpr ce = ConstExpr.fromConst(getConstValue(env));
				if (!ce.valueEquals(this.const_value))
					this.const_value = ce;
			}
		} else {
			if (this.const_value != null)
				this.init = this.const_value;
		}
		return true;
	}

	public String toString() {
		return sname;
	}

	public int hashCode() {
		return sname.hashCode();
	}

	public Type	getType(Env env) {
		TypeRef vtype = this.vtype;
		if (vtype == null)
			return Env.getEnv().tenv.tpVoid;
		return vtype.getType(env);
	}

	public Method getGetterMethod() {
		SymbolRef<Method> g = this.getter;
		if (g != null)
			return g.dnode;
		return null;
	}

	public Method getSetterMethod() {
		SymbolRef<Method> s = this.setter;
		if (s != null)
			return s.dnode;
		return null;
	}

	public INode doRewrite(RewriteContext ctx) {
		if (getMeta("kiev·stdlib·meta·extern") != null)
			return null;
		return super.doRewrite(ctx);
	}

	static class VarDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		VarDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("init"));
			res_idx = dfi.allocResult();
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			Var node = (Var)dfi.node_impl;
			DFState out = DFFunc.calc(f, dfi);
			out = out.declNode(node);
			Env env = Env.getEnv();
			if( node.init != null && node.init.getType(env) ≢ env.tenv.tpVoid )
				out = out.setNodeValue(new Var[]{node},node.init);
			res = out;
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncOut(DataFlowInfo dfi) {
		return new VarDFFunc(dfi);
	}
}

@ThisIsANode(name="LVar", lang=CoreLang)
public class LVar extends Var {
	@DataFlowDefinition(out="this:out()") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			init;
	}

	public LVar() { super(VAR_LOCAL); }

	public LVar(int pos, String name, Type tp, int kind, int flags)
		require tp != null;
	{
		super(name,new TypeRef(tp),kind,flags);
		this.pos = pos;
	}

	public LVar(int pos, String name, TypeRef vtype, int kind, int flags)
		require vtype != null;
	{
		super(name,vtype,kind,flags);
		this.pos = pos;
	}

	public LVar(String name, Type tp)
		require tp != null;
	{
		super(name,new TypeRef(tp),VAR_LOCAL,0);
	}
}

@ThisIsANode(name="Field", lang=CoreLang)
public class Field extends Var implements GlobalDNode {
	public static final Field[]	emptyArray = new Field[0];

	@DataFlowDefinition(out="init") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			init;
	}

	private static final Class[] $meta_flags = new Class[] {
		MetaPublic.class,    MetaPrivate.class,MetaProtected.class, MetaStatic.class,
		MetaFinal.class,     null,             MetaVolatile.class,  MetaTransient.class,
		MetaNative.class,    null,             MetaAbstract.class,  null,
		MetaSynthetic.class, null,             null,                null,
		MetaForward.class,   MetaVirtual.class,MetaUnerasable.class,MetaMacro.class,
		null,                null,              null,               null,
		null,                null,              null,               null,
		null,                null,              null,               null
	};

	//public static final SpaceRefDataAttrSlot<Method> ATTR_INVARIANT_CHECKERS = new SpaceRefDataAttrSlot<Field>("invariant checkers",false,TypeInfo.newTypeInfo(Method.class,null));

	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		getter_from_inner;
	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		setter_from_inner;
	@nodeAttr(ext_data=true)
	public ConstStringExpr			alt_enum_id;
	@nodeAttr(ext_data=true)
	public SymbolRef<Field>			nodeattr_of_attr;

	public Field() { super(FIELD_NORMAL); }

	public Field(int kind) { super(kind); }

	public Field(String name, Type tp, int flags) {
		super(name, new TypeRef(tp), FIELD_NORMAL, flags);
	}

	public Field(String name, TypeRef vtype, int flags) {
		super(name, vtype, FIELD_NORMAL, flags);
	}

	public Class[] getMetaFlags() { return Field.$meta_flags; }

	public String qname() {
		ANode p = parent();
		if (p == null || p instanceof KievRoot)
			return sname;
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+'·'+sname);
		return sname;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "const_value")
			return isFinal() && const_value != null;
		return super.includeInDump(dump,attr,val);
	}

	public Method makeReadAccessor(Env env) {
		assert(isPrivate());
		if (getter_from_inner != null && getter_from_inner.dnode != null)
			return getter_from_inner.dnode;
		MethodImpl m = new MethodImpl(Env.ctxTDecl(this).allocateAccessName(), this.getType(env), ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		if (isStatic()) {
			m.block.stats += new SFldExpr(pos,this);
		} else {
			Var self = new LVar(pos,Constants.nameThis,Env.ctxTDecl(this).getType(env),Var.VAR_LOCAL,0);
			m.params += self;
			m.block.stats += new IFldExpr(pos,new LVarExpr(pos,self),this);
		}
		Env.ctxTDecl(this).members += m;
		Kiev.runProcessorsOn(m);
		this.getter_from_inner = new SymbolRef<Method>(m);
		return m;
	}

	public Method makeWriteAccessor(Env env) {
		assert(isPrivate());
		if (setter_from_inner != null && setter_from_inner.dnode != null)
			return setter_from_inner.dnode;
		MethodImpl m = new MethodImpl(Env.ctxTDecl(this).allocateAccessName(), env.tenv.tpVoid, ACC_STATIC | ACC_SYNTHETIC);
		Var val = new LVar(pos,"value",this.getType(env),Var.VAR_LOCAL,0);
		m.params += val;
		m.body = new Block();
		if (isStatic()) {
			m.block.stats += new ExprStat(pos,new AssignExpr(pos,new SFldExpr(pos,this),new LVarExpr(pos,val)));
		} else {
			Var self = new LVar(pos,Constants.nameThis,Env.ctxTDecl(this).getType(env),Var.VAR_LOCAL,0);
			m.params.insert(0,self);
			m.block.stats += new ExprStat(pos,new AssignExpr(pos,new IFldExpr(pos,new LVarExpr(pos,self),this),new LVarExpr(pos,val)));
		}
		Env.ctxTDecl(this).members += m;
		Kiev.runProcessorsOn(m);
		this.setter_from_inner = new SymbolRef<Method>(m);
		return m;
	}

	public void postVerify(Env env, INode parent, AttrSlot slot) {
		if (!isStatic() && !isPrivate()) {
			Type t = this.getType(env);
			TypeVariance variance = TypeVariance.IN_VARIANT;
			boolean readable = MetaAccess.readable(this);
			boolean writable = MetaAccess.writeable(this) && !this.isFinal();
			if (readable && writable)
				variance = TypeVariance.IN_VARIANT;
			else if (readable)
				variance = TypeVariance.CO_VARIANT;
			else if (writable)
				variance = TypeVariance.CONTRA_VARIANT;
			VarianceCheckError err = t.checkVariance(variance);
			if (err != null)
				Kiev.reportWarning(this.vtype, err.toString());
		}
	}
}
