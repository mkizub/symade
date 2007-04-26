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

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RVar;
import kiev.be.java15.JVar;
import kiev.ir.java15.RField;
import kiev.be.java15.JField;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Var")
public abstract class Var extends DNode {
	
	@virtual typedef This  ≤ Var;
	@virtual typedef JView ≤ JVar;
	@virtual typedef RView ≤ RVar;

	public static final int VAR_LOCAL          = 0;
	public static final int VAR_RULE           = 1;
	public static final int FIELD_NORMAL       = 2;
	public static final int VAL_ENUM           = 3;
	public static final int REWRITE_PATTERN    = 4;
	public static final int PARAM_NORMAL       = 5;
	public static final int PARAM_THIS         = 6;
	public static final int PARAM_OUTER_THIS   = 7;
	public static final int PARAM_RULE_ENV     = 8;
	public static final int PARAM_TYPEINFO     = 9;
	public static final int PARAM_VARARGS      = 10;
	public static final int PARAM_LVAR_PROXY   = 11;
	public static final int PARAM_TYPEINFO_N   = 128;

	public int varflags;

	@att
	public @packed:8,varflags,0  int			kind;
	public @packed:24,varflags,8 int			bcpos;

	@att
	public TypeRef		vtype;
	@att(ext_data=true)
	public TypeRef		stype;
	@att
	public ENode		init;

	@getter public Type get$type() { return this.vtype.getType(); }

	// init wrapper
	@getter public final boolean isInitWrapper() {
		return this.is_init_wrapper;
	}
	@setter public final void setInitWrapper(boolean on) {
		if (this.is_init_wrapper != on) {
			assert(!locked);
			this.is_init_wrapper = on;
		}
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

	public void callbackAttached() {
		ANode p = parent();
		if (p instanceof DeclGroup)
			this.group = (DeclGroup)p;
		super.callbackAttached();
	}
	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "vtype" || attr.name == "stype")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "meta")
				parent().callbackChildChanged(pslot());
			else
				super.callbackChildChanged(attr);
		} else {
			super.callbackChildChanged(attr);
		}
	}	

	@getter public final Type get$type() { return this.vtype.getType(); }
		
	public static final Var[]	emptyArray = new Var[0];

	public Var(int kind) {
		this.kind = kind;
	}

	public Var(String name, TypeRef vtype, int kind, int flags)
		require vtype != null;
	{
		this.sname = name;
		this.kind = kind;
		this.u_name = name;
		this.vtype = vtype;
		if (flags != 0) {
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_FORWARD) == ACC_FORWARD) setMeta(new MetaForward());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			this.meta.mflags = flags;
		}
	}

	public Var(String name, TypeRef vtype, int kind)
		require vtype != null;
	{
		this.sname = name;
		this.kind = kind;
		this.u_name = name;
		this.vtype = vtype;
	}

	public ASTNode getDummyNode() {
		return LVar.dummyNode;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "init")
			return false;
		return super.includeInDump(dump,attr,val);
	}

	public boolean preResolveIn() {
		ENode init = this.init;
		if (init != null && init instanceof NewInitializedArrayExpr && init.type == null) {
			Type tp = getType();
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Scalar var is initialized by array");
			else
				init.setType((ArrayType)tp);
		}
		return true;
	}

	public String toString() {
		return sname;
	}

	public int hashCode() {
		return sname.hashCode();
	}

	public Type	getType() { return type; }

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

@node(name="Var")
public class LVar extends Var {
	@dflow(out="this:out()") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@virtual typedef This  = LVar;

	static final Var dummyNode = new LVar();

	public LVar() { super(VAR_LOCAL); }

	public LVar(int pos, String name, Type type, int kind, int flags)
		require type != null;
	{
		super(name,new TypeRef(type),kind,flags);
		this.pos = pos;
	}

	public LVar(int pos, String name, TypeRef vtype, int kind, int flags)
		require vtype != null;
	{
		super(name,vtype,kind,flags);
		this.pos = pos;
	}

	public LVar(String name, Type type)
		require type != null;
	{
		super(name,new TypeRef(type),VAR_LOCAL,0);
	}
}

@node(name="Field")
public final class Field extends Var implements GlobalDNode {
	public static final Field[]	emptyArray = new Field[0];
	static final Field dummyNode = new Field();

	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	public static final AttrSlot GETTER_ATTR = new ExtAttrSlot("getter method",false,false,TypeInfo.newTypeInfo(Method.class,null));
	public static final AttrSlot SETTER_ATTR = new ExtAttrSlot("setter method",false,false,TypeInfo.newTypeInfo(Method.class,null));
	public static final SpaceRefDataAttrSlot<Method> ATTR_INVARIANT_CHECKERS = new SpaceRefDataAttrSlot<Field>("invariant checkers",false,TypeInfo.newTypeInfo(Method.class,null));	
	public static final AttrSlot ALT_ENUM_ID_ATTR = new ExtAttrSlot("alt enum id",true,false,TypeInfo.newTypeInfo(ConstStringExpr.class,null));

	@virtual typedef This  = Field;
	@virtual typedef JView = JField;
	@virtual typedef RView = RField;

	/** Constant value of this field */
	@ref public ConstExpr			const_value;

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "vtype")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "meta")
				parent().callbackChildChanged(pslot());
			else
				super.callbackChildChanged(attr);
		} else {
			super.callbackChildChanged(attr);
		}
	}
	
	public String qname() {
		ANode p = parent();
		while (p instanceof DeclGroup)
			p = p.parent();
		if (p == null || p == Env.root)
			return sname;
		if (p instanceof GlobalDNode)
			return (((GlobalDNode)p).qname()+'\u001f'+sname);
		return sname;
	}

	// is a field of enum
	public final boolean isEnumField() {
		return this.kind == VAL_ENUM;
	}
	// packer field (auto-generated for packed fields)
	public final boolean isPackerField() {
		return this.is_fld_packer;
	}
	public final void setPackerField(boolean on) {
		if (this.is_fld_packer != on) {
			assert(!locked);
			this.is_fld_packer = on;
		}
	}
	// packed field
	public final boolean isPackedField() {
		return this.is_fld_packed;
	}
	public final void setPackedField(boolean on) {
		if (this.is_fld_packed != on) {
			assert(!locked);
			this.is_fld_packed = on;
		}
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
              
	public Field() { super(FIELD_NORMAL); }
	
	public Field(String name, Type type, int flags) {
		this(name,new TypeRef(type),flags);
	}
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(String name, TypeRef vtype, int flags) {
		super(name, vtype, FIELD_NORMAL);
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) setMeta(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) setMeta(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) setMeta(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) setMeta(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) setMeta(new MetaFinal());
			if ((flags & ACC_FORWARD) == ACC_FORWARD) setMeta(new MetaForward());
			if ((flags & ACC_VOLATILE) == ACC_VOLATILE) setMeta(new MetaVolatile());
			if ((flags & ACC_TRANSIENT) == ACC_TRANSIENT) setMeta(new MetaTransient());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) setMeta(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) setMeta(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) setMeta(new MetaMacro());
			if ((flags & ACC_NATIVE) == ACC_NATIVE) setMeta(new MetaNative());
			this.meta.mflags = flags;
		}
		trace(Kiev.debug && Kiev.debugCreation,"New field created: "+sname+" with type "+vtype);
	}

	public ASTNode getDummyNode() {
		return Field.dummyNode;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "const_value")
			return isFinal() && const_value != null;
		return super.includeInDump(dump,attr,val);
	}

	public boolean	isConstantExpr() {
		if( this.isFinal() ) {
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

	public final MetaPacked getMetaPacked() {
		return (MetaPacked)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fpacked");
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fpacker");
	}

	public boolean preResolveIn() {
		ENode init = this.init;
		if (init != null && init instanceof NewInitializedArrayExpr && init.type == null) {
			Type tp = getType();
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Scalar field is initialized by array");
			else
				init.setType((ArrayType)tp);
		}
		return true;
	}

	public boolean preVerify() {
		if (this.init != null) {
			if (isConstantExpr()) {
				ConstExpr ce = ConstExpr.fromConst(getConstValue());
				if (!ce.valueEquals(this.const_value)) {
					this = this.open();
					this.const_value = ce;
				}
			}
		} else {
			if (this.const_value != null)
				this.init = this.const_value;
		}
		return true;
	}
}
