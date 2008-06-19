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
 * @version $Revision$
 *
 */

@ThisIsANode(name="Var", lang=CoreLang)
public abstract class Var extends DNode implements GlobalDNode {
	
	public static final int VAR_LOCAL          = 0;
	public static final int VAR_RULE           = 1;
	public static final int FIELD_NORMAL       = 2;
	public static final int REWRITE_PATTERN    = 3;
	public static final int PARAM_NORMAL       = 4;
	public static final int PARAM_THIS         = 5;
	public static final int PARAM_OUTER_THIS   = 6;
	public static final int PARAM_RULE_ENV     = 7;
	public static final int PARAM_TYPEINFO     = 8;
	public static final int PARAM_ENUM_NAME    = 9;
	public static final int PARAM_ENUM_ORD     = 10;
	public static final int PARAM_VARARGS      = 11;
	public static final int PARAM_LVAR_PROXY   = 12;
	public static final int PARAM_TYPEINFO_N   = 16;

	@nodeAttr
	public TypeRef					vtype;
	@nodeAttr(ext_data=true)
	public TypeRef					stype;
	@nodeAttr
	public ENode					init;
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		getter;
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		setter;
	@nodeData(ext_data=true)
	public ConstExpr				const_value;

	@getter public int get$kind() { return this.meta.var_kind; }

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
		return this.meta.is_enum;
	}
	// packer field (auto-generated for packed fields)
	public final boolean isPackerField() {
		return this.is_fld_packer;
	}
	public final void setPackerField(boolean on) {
		if (this.is_fld_packer != on)
			this.is_fld_packer = on;
	}
	// packed field
	public final boolean isPackedField() {
		return this.is_fld_packed;
	}
	public final void setPackedField(boolean on) {
		if (this.is_fld_packed != on)
			this.is_fld_packed = on;
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
		return (MetaPacked)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fpacked");
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fpacker");
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (isAttached()) {
			if      (attr.name == "vtype" || attr.name == "stype")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
			else if (attr.name == "meta")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public static final Var[]	emptyArray = new Var[0];

	public Var(int kind) {
		this.meta.var_kind = kind;
	}

	public Var(String name, TypeRef vtype, int kind, int flags)
		require vtype != null;
	{
		this.sname = name;
		this.vtype = vtype;
		if (flags != 0) {
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_FORWARD) == ACC_FORWARD) setMeta(new MetaForward());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) setMeta(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) setMeta(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) setMeta(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) setMeta(new MetaStatic());
			if ((flags & ACC_VOLATILE) == ACC_VOLATILE) setMeta(new MetaVolatile());
			if ((flags & ACC_TRANSIENT) == ACC_TRANSIENT) setMeta(new MetaTransient());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) setMeta(new MetaAbstract());
			if ((flags & ACC_NATIVE) == ACC_NATIVE) setMeta(new MetaNative());
			this.meta.mflags = flags;
		}
		this.meta.var_kind = kind;
	}

	public Var(String name, TypeRef vtype, int kind)
		require vtype != null;
	{
		this.sname = name;
		this.vtype = vtype;
		this.meta.var_kind = kind;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "init")
			return false;
		return super.includeInDump(dump,attr,val);
	}

	public String qname() {
		ANode p = parent();
		if (p == null || p instanceof Env)
			return sname;
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+'\u001f'+sname);
		return sname;
	}

	public boolean preResolveIn() {
		if (meta != null)
			meta.verify();
		ENode init = this.init;
		if (init != null && init instanceof NewInitializedArrayExpr && init.ntype == null) {
			Type tp = getType();
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Scalar var is initialized by array");
			else
				init.setType((ArrayType)tp);
		}
		return true;
	}

	public boolean	isConstantExpr() {
		if (this.isFinal()) {
			if (this.init != null && this.init.isConstantExpr())
				return true;
			else if (this.const_value != null)
				return true;
		}
		return false;
	}

	public Object	getConstValue() {
		if (this.init != null && this.init.isConstantExpr())
			return this.init.getConstValue();
		else if (this.const_value != null)
			return this.const_value.getConstValue();
		throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public boolean preVerify() {
		if (this.init != null) {
			if (isConstantExpr()) {
				ConstExpr ce = ConstExpr.fromConst(getConstValue());
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

	public Type	getType() {
		TypeRef vtype = this.vtype;
		if (vtype == null)
			return StdTypes.tpVoid;
		return vtype.getType();
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

	public ANode doRewrite(RewriteContext ctx) {
		if (getMeta("kiev\u001fstdlib\u001fmeta\u001fextern") != null)
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
			if( node.init != null && node.init.getType() ≢ Type.tpVoid )
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
public class Field extends Var {
	public static final Field[]	emptyArray = new Field[0];

	@DataFlowDefinition(out="init") private static class DFI {
	@DataFlowDefinition(in="this:in")	ENode			init;
	}

	//public static final SpaceRefDataAttrSlot<Method> ATTR_INVARIANT_CHECKERS = new SpaceRefDataAttrSlot<Field>("invariant checkers",false,TypeInfo.newTypeInfo(Method.class,null));	

	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		getter_from_inner;
	@nodeAttr(ext_data=true)
	public SymbolRef<Method>		setter_from_inner;
	@nodeAttr(ext_data=true)
	public ConstStringExpr			alt_enum_id;

	public Field() { super(FIELD_NORMAL); }
	
	public Field(int kind) { super(kind); }
	
	public Field(String name, Type tp, int flags) {
		super(name, new TypeRef(tp), FIELD_NORMAL, flags);
	}

	public Field(String name, TypeRef vtype, int flags) {
		super(name, vtype, FIELD_NORMAL, flags);
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "const_value")
			return isFinal() && const_value != null;
		return super.includeInDump(dump,attr,val);
	}

	public Method makeReadAccessor() {
		assert(isPrivate());
		if (getter_from_inner != null && getter_from_inner.dnode != null)
			return getter_from_inner.dnode;
		MethodImpl m = new MethodImpl(ctx_tdecl.allocateAccessName(), this.getType(), ACC_STATIC | ACC_SYNTHETIC);
		m.body = new Block();
		if (isStatic()) {
			m.block.stats += new SFldExpr(pos,this);
		} else {
			Var self = new LVar(pos,Constants.nameThis,ctx_tdecl.xtype,Var.PARAM_NORMAL,0);
			m.params += self;
			m.block.stats += new IFldExpr(pos,new LVarExpr(pos,self),this);
		}
		ctx_tdecl.members += m;
		Kiev.runProcessorsOn(m);
		this.getter_from_inner = new SymbolRef<Method>(m);
		return m;
	}

	public Method makeWriteAccessor() {
		assert(isPrivate());
		if (setter_from_inner != null && setter_from_inner.dnode != null)
			return setter_from_inner.dnode;
		MethodImpl m = new MethodImpl(ctx_tdecl.allocateAccessName(), Type.tpVoid, ACC_STATIC | ACC_SYNTHETIC);
		Var val = new LVar(pos,"value",this.getType(),Var.PARAM_NORMAL,0);
		m.params += val;
		m.body = new Block();
		if (isStatic()) {
			m.block.stats += new ExprStat(pos,new AssignExpr(pos,Operator.Assign,new SFldExpr(pos,this),new LVarExpr(pos,val)));
		} else {
			Var self = new LVar(pos,Constants.nameThis,ctx_tdecl.xtype,Var.PARAM_NORMAL,0);
			m.params.insert(0,self);
			m.block.stats += new ExprStat(pos,new AssignExpr(pos,Operator.Assign,new IFldExpr(pos,new LVarExpr(pos,self),this),new LVarExpr(pos,val)));
		}
		ctx_tdecl.members += m;
		Kiev.runProcessorsOn(m);
		this.setter_from_inner = new SymbolRef<Method>(m);
		return m;
	}
	
	public void postVerify() {
		if (!isStatic() && !isPrivate()) {
			Type t = this.getType();
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
