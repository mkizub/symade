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

import kiev.ir.java15.RDNode;

import syntax kiev.Syntax;

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@ThisIsANode(lang=CoreLang)
public abstract class DNode extends ASTNode implements ISymbol {

	public static final DNode[] emptyArray = new DNode[0];
	
	public static final int MASK_ACC_DEFAULT   = 0;
	public static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
	public static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
	public static final int MASK_ACC_PROTECTED = ACC_PROTECTED;

	@abstract
	@nodeAttr(ext_data=true)
	public MNode⋈		metas;

	// TODO: make private
	public int			mflags;

	public @packed:3,mflags, 0 int     is_access;

	public @packed:1,mflags, 3 boolean is_static;
	public @packed:1,mflags, 4 boolean is_final;
	public @packed:1,mflags, 5 boolean is_mth_synchronized;	// method
	public @packed:1,mflags, 5 boolean is_struct_super;		// struct
	public @packed:1,mflags, 6 boolean is_fld_volatile;		// field
	public @packed:1,mflags, 6 boolean is_mth_bridge;			// method
	public @packed:1,mflags, 7 boolean is_fld_transient;		// field
	public @packed:1,mflags, 7 boolean is_mth_varargs;			// method
	public @packed:1,mflags, 8 boolean is_native;				// native method, backend operation/field/struct
	public @packed:1,mflags, 9 boolean is_struct_interface;
	public @packed:1,mflags,10 boolean is_abstract;
	public @packed:1,mflags,11 boolean is_math_strict;			// strict math
	public @packed:1,mflags,12 boolean is_synthetic;			// any decl that was generated (not in sources)
	public @packed:1,mflags,13 boolean is_struct_annotation;
	public @packed:1,mflags,14 boolean is_enum;				// struct/decl group/fields
		
	// Flags temporary used with java flags
	public @packed:1,mflags,16 boolean is_forward;				// var/field/method, type is wrapper
	public @packed:1,mflags,17 boolean is_virtual;				// var/field, method is 'static virtual', struct is 'view'
	public @packed:1,mflags,18 boolean is_type_unerasable;		// typedecl, method/struct as parent of typedef
	public @packed:1,mflags,19 boolean is_macro;				// macro-declarations for fields, methods, etc
	
	public @packed:1,mflags,20 boolean is_has_throws;			// methods

	public @packed:1,mflags,20 boolean is_struct_singleton;	// struct
	public @packed:1,mflags,21 boolean is_struct_mixin;		// struct
	public @packed:1,mflags,22 boolean is_tdecl_not_loaded;	// TypeDecl was fully loaded (from src or bytecode) 

	public @packed:10,mflags,20 int    var_kind;				// var/field kind
	
	public @packed:1,mflags,31 boolean is_interface_only;		// only node's interface was scanned/loded; no implementation

	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr
	public							String			sname; // source code name, may be null for anonymouse symbols

	@nodeData(ext_data=true)
	public kiev.be.java15.Attr[]					jattrs; // array of java class attributes of this node

	@AttrXMLDumpInfo(attr=true)
	@UnVersioned
	@nodeAttr(copyable=false)
	public							String			uuid;  // UUID of the node, since it's an ISymbol

	public final MetaAccess getMetaAccess() {
		return (MetaAccess)this.getMeta("kiev\u001fstdlib\u001fmeta\u001faccess");
	}

	@getter final public DNode get$dnode() { return this; }

	@getter final public String get$sname() {
		return this.sname;
	}
	@setter final public void set$sname(String value) {
		this.sname = (value == null) ? null : value.intern();
	}
	@getter final public String get$qname() {
		if (this instanceof GlobalDNode)
			return ((GlobalDNode)this).qname();
		return this.sname;
	}
	@getter final public String get$UUID() {
		String u = this.uuid;
		if (u == null) {
			u = java.util.UUID.randomUUID().toString();
			this.uuid = u;
		}
		return u;
	}
	@setter final public void set$uuid(String value) {
		value = value.intern();
		assert (this.uuid == null || this.uuid == value);
		if (this.uuid == null) {
			Env.getRoot().registerISymbol(value,this);
			this.uuid = value;
		}
	}
	
	public final boolean isPublic()			{ return this.is_access == MASK_ACC_PUBLIC; }
	public final boolean isPrivate()			{ return this.is_access == MASK_ACC_PRIVATE; }
	public final boolean isProtected()			{ return this.is_access == MASK_ACC_PROTECTED; }
	public final boolean isPkgPrivate()		{ return this.is_access == MASK_ACC_DEFAULT; }
	public final boolean isStatic()			{ return this.is_static; }
	public final boolean isFinal()				{ return this.is_final; }
	public final boolean isSynchronized()		{ return this.is_mth_synchronized; }
	public final boolean isFieldVolatile()		{ return this.is_fld_volatile; }
	public final boolean isMethodBridge()		{ return this.is_mth_bridge; }
	public final boolean isFieldTransient()	{ return this.is_fld_transient; }
	public final boolean isMethodVarargs()		{ return this.is_mth_varargs; }
	public final boolean isStructBcLoaded()	{ return this.is_struct_bytecode; }
	public final boolean isNative()			{ return this.is_native; }
	public final boolean isInterface()			{ return this.is_struct_interface; }
	public final boolean isAbstract()			{ return this.is_abstract; }
	public final boolean isMathStrict()		{ return this.is_math_strict; }
	public final boolean isSynthetic()			{ return this.is_synthetic; }

	public final boolean isMacro()				{ return this.is_macro; }
	public final boolean isVirtual()			{ return this.is_virtual; }
	public final boolean isForward()			{ return this.is_forward; }
	
	public final boolean isStructView()		{ return this instanceof KievView; }
	public final boolean isTypeUnerasable()	{ return this.is_type_unerasable; }
	public final boolean isStructInner()		{ return !(this.parent() instanceof KievPackage); }

	public final boolean isInterfaceOnly()		{ return this.is_interface_only; }

	public void setPublic() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("public"));
		else if (m.simple != "public")
			m.setSimple("public");
	}
	public void setPrivate() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("private"));
		else if (m.simple != "private")
			m.setSimple("private");
	}
	public void setProtected() {
		MetaAccess m = (MetaAccess)this.getMeta("kiev\u001fstdlib\u001fmeta\u001faccess");
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("protected"));
		else if (m.simple != "protected")
			m.setSimple("protected");
	}
	public void setPkgPrivate() {
		MetaAccess m = (MetaAccess)this.getMeta("kiev\u001fstdlib\u001fmeta\u001faccess");
		MetaAccess m = getMetaAccess();
		if (m != null) {
			if (m.flags != -1 || m.flags != 0xF)
				m.setSimple("");
			else
				m.detach();
		}
	}

	public void setStatic(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fstatic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaStatic());
		}
	}
	public void setFinal(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001ffinal");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaFinal());
		}
	}
	public void setSynchronized(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsynchronized");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynchronized());
		}
	}
	public void setFieldVolatile(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvolatile");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVolatile());
		}
	}
	public void setMethodBridge(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fbridge");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaBridge());
		}
	}
	public void setFieldTransient(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001ftransient");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaTransient());
		}
	}
	public void setMethodVarargs(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvarargs");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVarArgs());
		}
	}
	public void setNative(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fnative");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaNative());
		}
	}
	public void setAbstract(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fabstract");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaAbstract());
		}
	}
	public void setSynthetic(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsynthetic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynthetic());
		}
	}

	public void setMacro(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fmacro");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaMacro());
		}
	}

	public void setTypeUnerasable(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001funerasable");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaUnerasable());
		}
	}

	public final void setVirtual(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvirtual");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVirtual());
		}
	}

	public final void setForward(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fforward");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaForward());
		}
	}

	// resolved
	public final boolean isTypeDeclNotLoaded() {
		return this.is_tdecl_not_loaded;
	}
	public final void setTypeDeclNotLoaded(boolean on) {
		if (this.is_tdecl_not_loaded != on) {
			this.is_tdecl_not_loaded = on;
		}
	}

	public DNode() {}

	public Object copy(CopyContext cc) {
		ANode obj = cc.hasCopyOf(this);
		if (obj != null)
			return obj;
		return super.copy(cc);
	}

	public String toString() { return sname; }

	public final void resolveDecl() { ((RDNode)this).resolveDecl(); }

	public int getFlags() {
		return this.mflags;
	}
	public short getJavaFlags() {
		return (short)(getFlags() & JAVA_ACC_MASK);
	}

	public boolean hasName(String nm) {
		return (this.sname == nm);
	}
	public boolean hasNameStart(String nm) {
		return (this.sname != null && this.sname.startsWith(nm));
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this" && ((isPrivate() && !isMacro()) || isAutoGenerated() || isSynthetic()))
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public boolean backendCleanup() {
		this.jattrs = null;
		return true;
	}

	public ANode doRewrite(RewriteContext ctx) {
		DNode dn = (DNode)super.doRewrite(ctx);
		String id = this.sname;
		String rw = ctx.replace(id);
		if (id != rw)
			dn.sname = rw;
		return dn;
	}

	public boolean hasRuntimeVisibleMetas() {
		foreach (MNode m; metas; m.isRuntimeVisible())
			return true;
		return false;
	}
	public boolean hasRuntimeInvisibleMetas() {
		foreach (MNode m; metas; m.isRuntimeInvisible())
			return true;
		return false;
	}

	public final MNode getMeta(String name) {
		foreach (MNode m; metas; m.qname == name)
			return m;
		return null;
	}
	
	public final MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		String qname = meta.qname;
		foreach (MNode m; metas; m.qname == qname) {
			if (meta != m)
				m.replaceWithNode(meta);
			return meta;
		}
		metas.append(meta);
		return meta;
	}

	public void verifyMetas() {
		foreach (MNode m; metas) {
			try {
				m.verify();
			} catch (CompilerException e) {
				Kiev.reportError(m, e);
				continue;
			}
		}
	}
	
}

public interface GlobalDNode {
	public String qname();
}


@ThisIsANode(lang=CoreLang)
public abstract class TypeDecl extends DNode implements ScopeOfNames, ScopeOfMethods {

	public static final TypeDecl[] emptyArray = new TypeDecl[0];
	
	@nodeAttr
	public TypeRef∅					super_types;
	@AttrXMLDumpInfo(ignore=true)
	@nodeData(copyable=false)
	public MetaType						xmeta_type;
	@AttrXMLDumpInfo(ignore=true)
	@nodeData(copyable=false)
	public Type							xtype;

	@nodeData(ext_data=true, copyable=false) public WrapperMetaType	wmeta_type;

	@getter public ComplexTypeDecl get$child_ctx_tdecl()	{ null }

	public ASTNode[] getMembers() { ASTNode.emptyArray }

	@setter public final void set$xmeta_type(MetaType mt) {
		assert (this.xmeta_type == null || this.xmeta_type == mt);
		this.xmeta_type = mt;
	}
	@setter public final void set$xtype(Type tp) {
		assert (this.xtype == null);
		this.xtype = tp;
	}
	
	public boolean isClazz() {
		return false;
	}
	// a structure with the only one instance (singleton)	
	public final boolean isSingleton() {
		return this.is_struct_singleton;
	}
	public final void setSingleton(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsingleton");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSingleton());
		}
	}
	// a local (in method) class	
	public final boolean isLocal() {
		return this.is_struct_local;
	}
	public final void setLocal(boolean on) {
		if (this.is_struct_local != on) {
			this.is_struct_local = on;
		}
	}
	// an anonymouse (unnamed) class	
	public final boolean isAnonymouse() {
		return this.is_struct_anomymouse;
	}
	public final void setAnonymouse(boolean on) {
		if (this.is_struct_anomymouse != on) {
			this.is_struct_anomymouse = on;
		}
	}
	// kiev annotation
	public final boolean isAnnotation() {
		return this.is_struct_annotation;
	}
	// java enum
	public final boolean isEnum() {
		return this.is_enum;
	}
	// structure was loaded from bytecode
	public final boolean isLoadedFromBytecode() {
		return this.is_struct_bytecode;
	}
	public final void setLoadedFromBytecode(boolean on) {
		this.is_struct_bytecode = on;
	}

	// a pizza case	
	public final boolean isPizzaCase() {
		return this instanceof PizzaCase;
	}
	// has pizza cases
	public final boolean isHasCases() {
		return this.is_struct_has_pizza_cases;
	}
	public final void setHasCases(boolean on) {
		if (this.is_struct_has_pizza_cases != on) {
			this.is_struct_has_pizza_cases = on;
		}
	}

	// an interface with methdos and fields (mixin)	
	public final boolean isMixin() {
		return this.is_struct_mixin;
	}
	public final void setMixin(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev\u001fstdlib\u001fmeta\u001fmixin");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaMixin());
		}
	}

	// indicates that type of the structure was attached
	public final boolean isTypeResolved() {
		return this.is_struct_fe_passed || this.is_struct_type_resolved;
	}
	public final void setTypeResolved(boolean on) {
		assert (!this.is_struct_fe_passed);
		if (this.is_struct_type_resolved != on) {
			this.is_struct_type_resolved = on;
		}
	}
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved() {
		return this.is_struct_fe_passed || this.is_struct_args_resolved;
	}
	public final void setArgsResolved(boolean on) {
		assert (!this.is_struct_fe_passed);
		if (this.is_struct_args_resolved != on) {
			this.is_struct_args_resolved = on;
		}
	}
	public final boolean isFrontEndPassed() {
		return this.is_struct_fe_passed;
	}
	public final void setFrontEndPassed() {
		this.is_struct_fe_passed = true;
	}

	public Object copy(CopyContext cc) {
		TypeDecl obj = (TypeDecl)super.copy(cc);
		if (this == obj)
			return this;
		return obj;
	}


	public TypeDecl(String name) {
		this.sname = name;
	}
	
	public String qname() { sname }

	public void initForEditor() {
		if (sname == null)
			sname = "<name>";
		super.initForEditor();
	}

	public void cleanupOnReload() {
		this.super_types.delAll();
		this.metas.delAll();
		this.mflags = 0;
		this.compileflags &= 3;
	}

	public Type getType() { return this.xtype == null ? Type.tpVoid : this.xtype; }
	public Struct getStruct() { return null; }

	public final boolean isTypeAbstract()		{ return this.isAbstract(); }
	public final boolean isTypeVirtual()		{ return this.isVirtual(); }
	public final boolean isTypeFinal()			{ return this.isFinal(); }
	public final boolean isTypeStatic()		{ return !this.isStructInner() || this.isStatic(); }
	public final boolean isTypeForward()		{ return this.isForward(); }

	public String toString() { return sname; }

	public void checkResolved() {
		if( isTypeDeclNotLoaded() ) {
			if (Env.getRoot().loadTypeDecl(this).isTypeDeclNotLoaded())
				throw new RuntimeException("TypeDecl "+this+" unresolved");
		}
	}
	
	public boolean preVerify() {
		setFrontEndPassed();
		return true;
	}

	public final boolean instanceOf(TypeDecl tdecl) {
		if (tdecl == null) return false;
		if (this == tdecl) return true;
		foreach (TypeRef st; super_types; st.getTypeDecl() != null) {
			if (st.getTypeDecl().instanceOf(tdecl))
				return true;
		}
		return false;
	}

	public Field resolveField(String name) {
		return resolveField(name,true);
	}

	public Field resolveField(String name, boolean fatal) {
		checkResolved();
		if (this instanceof ComplexTypeDecl) {
			foreach (Field f; ((ComplexTypeDecl)this).members; f.sname == name)
				return f;
		}
		foreach (TypeRef tr; this.super_types; tr.getTypeDecl() != null) {
			Field f = tr.getTypeDecl().resolveField(name, false);
			if (f != null)
				return f;
		}
		if (fatal)
			throw new RuntimeException("Unresolved field "+name+" in class "+this);
		return null;
	}

	public Method resolveMethod(String name, Type ret, Type... va_args) {
		Type[] args = new Type[va_args.length];
		for (int i=0; i < va_args.length; i++)
			args[i] = (Type)va_args[i];
		CallType mt = new CallType(null,null,args,ret,false);
		Method@ m;
		if (!this.xtype.resolveCallAccessR(m, new ResInfo(this,name,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic), mt) &&
			!this.resolveMethodR(m, new ResInfo(this,name,ResInfo.noForwards|ResInfo.noSyntaxContext), mt))
			throw new CompilerException(this,"Unresolved method "+name+mt+" in class "+this);
		return (Method)m;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: Resolving name "+info.getName()+" in "+this),
		checkResolved(),
		{
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in "+this),
			resolveNameR_1(node,info) // resolve in this class
		;
			info.isSuperAllowed(),
			info.getPrevSlotName() != "super_types",
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in super-class of "+this),
			resolveNameR_3(node,info) // resolve in super-classes
		}
	}
	protected rule resolveNameR_1(ASTNode@ node, ResInfo info)
		ASTNode@ n;
	{
			info.checkNodeName(this),
			node ?= this
	}
	protected rule resolveNameR_3(ASTNode@ node, ResInfo info)
		TypeRef@ sup_ref;
	{
		sup_ref @= super_types,
		sup_ref.getTypeDecl() != null,
		info.enterSuper() : info.leaveSuper(),
		sup_ref.getTypeDecl().resolveNameR(node,info)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		info.isSuperAllowed(),
		checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		supref @= super_types,
		info.enterSuper() : info.leaveSuper(),
		supref.getType().meta_type.tdecl.resolveMethodR(node,info,mt)
	}
	
}

@ThisIsANode(lang=CoreLang)
public abstract class ComplexTypeDecl extends TypeDecl implements GlobalDNode {

	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	@nodeAttr public TypeConstr∅					args;
	@nodeAttr public ASTNode∅						members;
	          public String							q_name;	// qualified name

	@nodeData(ext_data=true, copyable=false) public KString			bytecode_name; // used by backend for anonymouse and inner declarations
	@nodeData(ext_data=true, copyable=false) public TypeAssign			ometa_tdef;

	@getter public ComplexTypeDecl get$child_ctx_tdecl()	{ return this; }

	public final ASTNode[] getMembers() { this.members }
	
	public ComplexTypeDecl(String name) {
		super(name);
	}
	
	public void callbackTypeVersionChanged() {
		if (xmeta_type != null)
			xmeta_type.callbackTypeVersionChanged();
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "sname")
			resetNames();
		if (attr.name == "super_types" || attr.name == "args")
			callbackTypeVersionChanged();
		super.callbackChildChanged(ct, attr, data);
	}
	
	private void resetNames() {
		q_name = null;
		foreach (ComplexTypeDecl s; members)
			s.resetNames();
	}
	
	public Object copy(CopyContext cc) {
		ComplexTypeDecl obj = (ComplexTypeDecl)super.copy(cc);
		if (this == obj)
			return this;
		obj.q_name = null;
		return obj;
	}

	public void cleanupOnReload() {
		super.cleanupOnReload();
		this.args.delAll();
		foreach(Method m; this.members; m.isOperatorMethod() )
			Operator.cleanupMethod(m);
		this.members.delAll();
		this.callbackTypeVersionChanged();
	}

	public String qname() {
		if (q_name != null)
			return q_name;
		if (sname == null || sname == "")
			return null;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			q_name = (p.qname()+"\u001f"+sname).intern();
		else
			q_name = sname;
		return q_name;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('\u001f','.');
	}

	static class TypeDeclDFFunc extends DFFunc {
		final int res_idx;
		TypeDeclDFFunc(DataFlowInfo dfi) {
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = DFState.makeNewState();
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncIn(DataFlowInfo dfi) {
		return new TypeDeclDFFunc(dfi);
	}

	public String allocateAccessName() {
		int x = 0;
		foreach (Method m; members; m.sname != null && m.sname.startsWith("access$")) {
			int v = Integer.parseInt(m.sname.substring(7),10);
			if (x <= v)
				x = v+1;
		}
		String name = String.valueOf(x);
		while (name.length() < 3)
			name = "0"+name;
		return "access$"+name;
	}

	protected rule resolveNameR_1(ASTNode@ node, ResInfo info)
		ASTNode@ n;
	{
			info.checkNodeName(this),
			node ?= this
		;	node @= args,
			info.checkNodeName(node)
		;	n @= members,
			info.checkNodeName(n),
			info.check(n),
			node ?= n
	}

	protected rule resolveNameR_Syntax(ASTNode@ node, ResInfo info)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import ("+(info.doImportStar() ? "with star" : "no star" )+"): "+syn),
		((Import)syn).resolveNameR(node,info)
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ member;
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info.check(member),
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;	info.isSuperAllowed(),
			supref @= super_types,
			info.enterSuper() : info.leaveSuper(),
			supref.getType().meta_type.tdecl.resolveMethodR(node,info,mt)
		}
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaTypeDecl extends ComplexTypeDecl {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	public MetaTypeDecl() {
		super(null);
		this.callbackTypeVersionChanged();
		this.xmeta_type = new XMetaType(this, 0);
		this.xtype = this.xmeta_type.make(TVarBld.emptySet);
	}
	public MetaTypeDecl(MetaType meta_type) {
		super(null);
		if (meta_type != null) {
			this.xmeta_type = meta_type;
			this.xtype = meta_type.make(TVarBld.emptySet);
		}
	}
	public Object copy(CopyContext cc) {
		MetaTypeDecl obj = (MetaTypeDecl)super.copy(cc);
		if (this == obj)
			return this;
		if (this.xmeta_type != null)
			obj.xmeta_type = new XMetaType(obj, this.xmeta_type.flags);
		else
			obj.xmeta_type = new XMetaType(obj, 0);
		obj.xtype = this.xmeta_type.make(TVarBld.emptySet);
		obj.callbackTypeVersionChanged();
		return obj;
	}
}

@ThisIsANode(lang=CoreLang)
public final class KievSyntax extends DNode implements GlobalDNode, ScopeOfNames, ScopeOfMethods {
	@nodeAttr public SymbolRef∅		super_syntax;
	@nodeAttr public ASTNode∅			members;

	public KievSyntax() {}
	
	public String qname() {
		if (sname == null || sname == "")
			return null;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			return (p.qname()+"\u001f"+sname).intern();
		return sname;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('\u001f','.');
	}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
		ASTNode@ n;
		SymbolRef@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: Resolving name "+info.getName()+" in "+this),
		{
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in "+this),
			info.checkNodeName(this),
			node ?= this
		;	// resolve in this syntax
			n @= members,
			info.checkNodeName(n),
			info.check(n),
			node ?= n
		;	// resolve in imports
			n @= members,
			n instanceof Import,
			trace( Kiev.debug && Kiev.debugResolve, "In import ("+(info.doImportStar() ? "with star" : "no star" )+"): "+n),
			((Import)n).resolveNameR(node,info)
		;
			info.getPrevSlotName() != "super_syntax",
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in super-syntax of "+this),
			super_stx @= super_syntax,
			super_stx.dnode instanceof KievSyntax,
			((KievSyntax)super_stx.dnode).resolveNameR(node,info)
		}
	}

	public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ member;
		SymbolRef<KievSyntax>@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info.check(member),
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;
			member @= members,
			member instanceof Import,
			((Import)member).resolveMethodR(node,info,mt)
		;
			super_stx @= super_syntax,
			super_stx.dnode != null,
			super_stx.dnode.resolveMethodR(node,info,mt)
		}
	}
}

@ThisIsANode(lang=CoreLang)
public class KievPackage extends DNode implements GlobalDNode, ScopeOfNames {

	@nodeAttr public DNode∅						pkg_members;

	public String qname() {
		ANode p = parent();
		if ((p instanceof KievPackage) && !(p instanceof Env))
			return (p.qname()+"\u001f"+sname).intern();
		return sname;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('\u001f','.');
	}

	public final rule resolveNameR(ASTNode@ node, ResInfo info)
		DNode@ dn;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"KievPackage: Resolving name "+info.getName()+" in "+this),
		{
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in "+this),
			info.checkNodeName(this),
			node ?= this
		;
			dn @= pkg_members,
			info.checkNodeName(dn),
			info.check(dn),
			node ?= dn
		;
			info.isCmpByEquals(),
			node ?= tryLoad(info.getName())
		}
	}

	public DNode tryLoad(String name) {
		trace(Kiev.debug && Kiev.debugResolve,"Package: trying to load in package "+this);
		DNode dn;
		String qn = name;
		if (this instanceof Env)
			dn = Env.getRoot().loadAnyDecl(qn);
		else
			dn = Env.getRoot().loadAnyDecl(qn=(this.qname()+"\u001f"+name));
		trace(Kiev.debug && Kiev.debugResolve,"DNode "+(dn != null ? dn+" found " : qn+" not found")+" in "+this);
		return dn;
	}
	
}

