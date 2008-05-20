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
import kiev.be.java15.JDNode;
import kiev.be.java15.JTypeDecl;
import kiev.ir.java15.RTypeDecl;

import syntax kiev.Syntax;

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@ThisIsANode(lang=CoreLang)
public abstract class DNode extends ASTNode implements ISymbol {

	@virtual typedef This  ≤ DNode;
	@virtual typedef JView ≤ JDNode;
	@virtual typedef RView ≤ RDNode;
	
	public static final DNode[] emptyArray = new DNode[0];
	
	public static final int MASK_ACC_DEFAULT   = 0;
	public static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
	public static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
	public static final int MASK_ACC_PROTECTED = ACC_PROTECTED;

	@nodeAttr public final			MetaSet			meta;
	@nodeAttr public				String			sname; // source code name, may be null for anonymouse symbols
	@nodeData public				DeclGroup		group;

	@nodeData(ext_data=true, copyable=false)
	public KString								bytecode_name; // used by backend for anonymouse and inner declarations
	@nodeData(ext_data=true)
	public kiev.be.java15.Attr[]				jattrs; // array of java class attributes of this node

	@UnVersioned
	@nodeAttr(copyable=false)
	public	String								uuid;  // UUID of the node, since it's an ISymbol

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
	
	public final boolean isPublic()				{ return this.meta.is_access == MASK_ACC_PUBLIC || group != null && group.meta.is_access == MASK_ACC_PUBLIC; }
	public final boolean isPrivate()			{ return this.meta.is_access == MASK_ACC_PRIVATE || group != null && group.meta.is_access == MASK_ACC_PRIVATE; }
	public final boolean isProtected()			{ return this.meta.is_access == MASK_ACC_PROTECTED || group != null && group.meta.is_access == MASK_ACC_PROTECTED; }
	public final boolean isPkgPrivate()		{ return this.meta.is_access == MASK_ACC_DEFAULT || group != null && group.meta.is_access == MASK_ACC_DEFAULT; }
	public final boolean isStatic()				{ return this.meta.is_static || group != null && group.meta.is_static; }
	public final boolean isFinal()				{ return this.meta.is_final || group != null && group.meta.is_final; }
	public final boolean isSynchronized()		{ return this.meta.is_mth_synchronized || group != null && group.meta.is_final; }
	public final boolean isFieldVolatile()		{ return this.meta.is_fld_volatile || group != null && group.meta.is_fld_volatile; }
	public final boolean isMethodBridge()		{ return this.meta.is_mth_bridge; }
	public final boolean isFieldTransient()	{ return this.meta.is_fld_transient || group != null && group.meta.is_fld_transient; }
	public final boolean isMethodVarargs()		{ return this.meta.is_mth_varargs; }
	public final boolean isStructBcLoaded()	{ return this.meta.is_struct_bytecode; }
	public final boolean isNative()				{ return this.meta.is_native || group != null && group.meta.is_native; }
	public final boolean isInterface()			{ return this.meta.is_struct_interface; }
	public final boolean isAbstract()			{ return this.meta.is_abstract || group != null && group.meta.is_abstract; }
	public final boolean isMathStrict()		{ return this.meta.is_math_strict || group != null && group.meta.is_math_strict; }
	public final boolean isSynthetic()			{ return this.meta.is_synthetic || group != null && group.meta.is_synthetic; }

	public final boolean isMacro()				{ return this.meta.is_macro || group != null && group.meta.is_macro; }
	public final boolean isVirtual()			{ return this.meta.is_virtual || group != null && group.meta.is_virtual; }
	public final boolean isForward()			{ return this.meta.is_forward || group != null && group.meta.is_forward; }
	
	public final boolean isStructView()		{ return this instanceof KievView; }
	public final boolean isTypeUnerasable()	{ return this.meta.is_type_unerasable || group != null && group.meta.is_type_unerasable; }
	public final boolean isPackage()			{ return this instanceof KievPackage; }
	public final boolean isSyntax()				{ return this instanceof KievSyntax; }
	public final boolean isStructInner()		{ return !isPackage() && !isSyntax() && !(this.parent() instanceof NameSpace); }

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
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fstatic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaStatic());
		}
	}
	public void setFinal(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001ffinal");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaFinal());
		}
	}
	public void setSynchronized(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsynchronized");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynchronized());
		}
	}
	public void setFieldVolatile(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvolatile");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVolatile());
		}
	}
	public void setMethodBridge(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fbridge");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaBridge());
		}
	}
	public void setFieldTransient(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001ftransient");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaTransient());
		}
	}
	public void setMethodVarargs(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvarargs");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVarArgs());
		}
	}
	public void setNative(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fnative");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaNative());
		}
	}
	public void setAbstract(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fabstract");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaAbstract());
		}
	}
	public void setSynthetic(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsynthetic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynthetic());
		}
	}

	public void setMacro(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fmacro");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaMacro());
		}
	}

	public void setTypeUnerasable(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001funerasable");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaUnerasable());
		}
	}

	public final void setVirtual(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fvirtual");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVirtual());
		}
	}

	public final void setForward(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fforward");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaForward());
		}
	}

	public DNode() {
		this.meta = new MetaSet();
	}

	public ASTNode getDummyNode() {
		return DummyDNode.dummyNode;
	}
	
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			this.group = null;
		}
		super.callbackDetached(parent, slot);
	}

	public Object copy(CopyContext cc) {
		ANode obj = cc.hasCopyOf(this);
		if (obj != null)
			return obj;
		return super.copy(cc);
	}

	public String toString() { return sname; }

	public final void resolveDecl() { ((RView)this).resolveDecl(); }

	public int getFlags() {
		int mflags = this.meta.mflags;
		if (group != null)
			mflags |= group.meta.mflags;
		return mflags;
	}
	public short getJavaFlags() {
		return (short)(getFlags() & JAVA_ACC_MASK);
	}

	public boolean hasName(String nm, boolean by_equals) {
		if (by_equals) {
			if (this.sname == nm) return true;
		} else {
			if (this.sname != null && this.sname.startsWith(nm)) return true;
		}
		return false;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this" && (isPrivate() || isAutoGenerated() || isSynthetic()))
			return false;
		if (attr.name == "meta" && meta.metas.length == 0)
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public final MNode getMeta(String name) {
		return this.meta.getMeta(name);
	}
	public final MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		return this.meta.setMeta(meta);
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
}

@ThisIsANode(lang=void)
public final class DummyDNode extends DNode {
	public static final DummyDNode dummyNode = new DummyDNode();

	@virtual typedef This  = DummyDNode;

	private DummyDNode() {
		this.sname = "<dummy>";
	}
}

public interface GlobalDNode {
	public String qname();
}


@ThisIsANode(lang=CoreLang)
public abstract class TypeDecl extends DNode implements ScopeOfNames, ScopeOfMethods, GlobalDNode {

	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  ≤ TypeDecl;
	@virtual typedef JView ≤ JTypeDecl;
	@virtual typedef RView ≤ RTypeDecl;

	public static final TypeDecl[] emptyArray = new TypeDecl[0];
	
	@nodeData public SymbolRef<TypeDecl>			package_clazz;
	@nodeAttr public TypeConstr[]				args;
	@nodeAttr public TypeRef[]					super_types;
	@nodeAttr public ASTNode[]					members;
	@nodeData public DNode[]						sub_decls;
	@nodeData public int							prefix_counter;	// for name_prefix auto-generation
		 private MetaType[]					super_meta_types;
	@nodeData private TypeDecl[]					direct_extenders;
		 public int							type_decl_version;
		 public String						q_name;	// qualified name
		 public MetaType					xmeta_type;
		 public Type						xtype;

	@nodeData(ext_data=true, copyable=false) public WrapperMetaType		wmeta_type;
	@nodeData(ext_data=true, copyable=false) public TypeAssign			ometa_tdef;
	@nodeData(ext_data=true, copyable=false) public Integer				inner_counter;

	@getter public TypeDecl get$child_ctx_tdecl()	{ return this; }

	public boolean isClazz() {
		return false;
	}
	// resolved
	public final boolean isTypeDeclNotLoaded() {
		return this.meta.is_tdecl_not_loaded;
	}
	public final void setTypeDeclNotLoaded(boolean on) {
		if (this.meta.is_tdecl_not_loaded != on) {
			this.meta.is_tdecl_not_loaded = on;
		}
	}
	// a structure with the only one instance (singleton)	
	public final boolean isSingleton() {
		return this.meta.is_struct_singleton;
	}
	public final void setSingleton(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fsingleton");
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
		return this.meta.is_struct_annotation;
	}
	// java enum
	public final boolean isEnum() {
		return this.meta.is_enum;
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
		return this.meta.is_struct_mixin;
	}
	public final void setMixin(boolean on) {
		MetaFlag m = this.getMeta("kiev\u001fstdlib\u001fmeta\u001fmixin");
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

	public final MetaType[] getAllSuperTypes() {
		if (super_meta_types != null)
			return super_meta_types;
		Vector<MetaType> types = new Vector<MetaType>();
		foreach (TypeRef it; super_types)
			addSuperTypes(it, types);
		if (types.length == 0)
			super_meta_types = MetaType.emptyArray;
		else
			super_meta_types = types.toArray();
		return super_meta_types;
	}
	
	private void addSuperTypes(TypeRef suptr, Vector<MetaType> types) {
		Type sup = suptr.getType();
		if (sup == null)
			return;
		MetaType tt = sup.meta_type.tdecl.xmeta_type;
		if (tt != null && !types.contains(tt))
			types.append(tt);
		MetaType[] sup_types = sup.meta_type.tdecl.getAllSuperTypes();
		foreach (MetaType t; sup_types) {
			if (!types.contains(t))
				types.append(t);
		}
	}

	public final void callbackSuperTypeChanged(TypeDecl chg) {
		super_meta_types = null;
		type_decl_version++;
		foreach (TypeDecl td; direct_extenders)
			td.callbackSuperTypeChanged(chg);
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "args" || attr.name == "super_types")
			this.callbackSuperTypeChanged(this);
		super.callbackChildChanged(ct, attr, data);
	}
	
	public void updatePackageClazz() {
		Struct pkg = null;
		TypeDecl td = ctx_tdecl;
		NameSpace ns;
		if (td instanceof Struct)
			pkg = (Struct)td;
		else if ((ns=ctx_name_space) != null)
			pkg = ns.getPackage();
		TypeDecl cur = this.package_clazz.dnode;
		if (cur == pkg)
			return;
		if (cur != null) {
			int idx = cur.sub_decls.indexOf(this);
			if (idx >= 0)
				cur.sub_decls.del(idx);
		}
		this.package_clazz.symbol = pkg;
		if (pkg != null) {
			int idx = pkg.sub_decls.indexOf(this);
			if (idx < 0)
				pkg.sub_decls.append(this);
		}
	}

	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			updatePackageClazz();
		}
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			if (package_clazz.dnode != null) {
				int idx = package_clazz.dnode.sub_decls.indexOf(this);
				if (idx >= 0)
					package_clazz.dnode.sub_decls.del(idx);
				package_clazz.symbol = null;
			}
		}
		super.callbackDetached(parent, slot);
	}
	public Object copy(CopyContext cc) {
		TypeDecl obj = (TypeDecl)super.copy(cc);
		if (this == obj)
			return this;
		if (obj.package_clazz.symbol != null)
			obj.package_clazz.symbol = null;
		obj.super_meta_types = null;
		obj.direct_extenders.delAll();
		obj.type_decl_version = 1;
		obj.q_name = null;
		obj.xmeta_type = null;
		obj.xtype = null;
		return obj;
	}


	public TypeDecl(String name) {
		package_clazz = new SymbolRef<TypeDecl>();
		this.sname = name;
	}
	
	public void initForEditor() {
		if (sname == null)
			sname = "<name>";
		super.initForEditor();
	}

	public void cleanupOnReload() {
		this.type_decl_version++;
		if (this.package_clazz.dnode != null) {
			int idx = this.package_clazz.dnode.sub_decls.indexOf(this);
			if (idx >= 0)
				this.package_clazz.dnode.sub_decls.del(idx);
			this.package_clazz.symbol = null;
		}
		this.super_types.delAll();
		this.args.delAll();
		foreach(Method m; this.members; m.isOperatorMethod() )
			Operator.cleanupMethod(m);
		this.members.delAll();
		this.sub_decls.delAll();
		this.meta.metas.delAll();
		this.meta.mflags = 0;
		this.compileflags &= 3;
	}

	public Type getType() { return this.xtype == null ? Type.tpVoid : this.xtype; }
	public Struct getStruct() { return null; }

	public final boolean isTypeAbstract()		{ return this.isAbstract(); }
	public final boolean isTypeVirtual()		{ return this.isVirtual(); }
	public final boolean isTypeFinal()			{ return this.isFinal(); }
	public final boolean isTypeStatic()		{ return !this.isStructInner() || this.isStatic(); }
	public final boolean isTypeForward()		{ return this.isForward(); }

	public String qname() {
		if (q_name != null)
			return q_name;
		if (sname == null || sname == "")
			return null;
		ANode p = parent();
		if (p instanceof NameSpace)
			p = p.getPackage();
		if (p == null)
			p = package_clazz.dnode;
		if (p instanceof Env || !(p instanceof TypeDecl))
			q_name = sname;
		else
			q_name = (p.qname()+"\u001f"+sname).intern();
		return q_name;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('\u001f','.');
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		//if (dump == "api" && attr.name == "package_clazz")
		//	return true;
		return super.includeInDump(dump, attr, val);
	}

	public boolean checkResolved() {
		if( isTypeDeclNotLoaded() ) {
			if (Env.getRoot().loadTypeDecl(this).isTypeDeclNotLoaded()) {
				if (isPackage())
					setTypeDeclNotLoaded(false);
				else
					throw new RuntimeException("TypeDecl "+this+" not found");
			}
			if (isTypeDeclNotLoaded())
				throw new RuntimeException("TypeDecl "+this+" unresolved");
		}
		return true;
	}
	
	public void resolveMetaDefaults() {
		if (isAnnotation()) {
			foreach(Method m; members) {
				try {
					m.resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(m,e);
				}
			}
		}
		if( this instanceof Struct && !isPackage() ) {
			foreach (TypeDecl sub; ((Struct)this).sub_decls) {
				if (!sub.isAnonymouse())
					sub.resolveMetaDefaults();
			}
		}
	}

	public void resolveMetaValues() {
		this.meta.resolve();
		foreach(DNode dn; members) {
			dn.meta.resolve();
			if (dn instanceof Method)
				foreach (Var p; dn.params)
					p.meta.resolve();
		}
		
		if( this instanceof Struct && !isPackage() ) {
			foreach (TypeDecl sub; ((Struct)this).sub_decls) {
				sub.resolveMetaValues();
			}
		}
	}

	public boolean preVerify() {
		setFrontEndPassed();
		return true;
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
		foreach (Field f; this.members; f.sname == name)
			return f;
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
		if (!this.xtype.resolveCallAccessR(m, new ResInfo(this,name,ResInfo.noForwards|ResInfo.noImports|ResInfo.noStatic), mt) &&
			!this.resolveMethodR(m, new ResInfo(this,name,ResInfo.noForwards|ResInfo.noImports), mt))
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
			isSyntax(),
			resolveNameR_Syntax(node,info)
		;
			info.isSuperAllowed(),
			info.space_prev == null || (info.space_prev.pslot().name != "super_types"),
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in super-class of "+this),
			resolveNameR_3(node,info) // resolve in super-classes
		}
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
	protected rule resolveNameR_3(ASTNode@ node, ResInfo info)
		TypeRef@ sup_ref;
	{
		sup_ref @= super_types,
		sup_ref.getTypeDecl() != null,
		info.enterSuper() : info.leaveSuper(),
		sup_ref.getTypeDecl().resolveNameR(node,info)
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
		;	info.isImportsAllowed() && isPackage(),
			member @= members,
			member instanceof Method,
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;	isSyntax(),
			member @= members,
			member instanceof Import,
			((Import)member).resolveMethodR(node,info,mt)
		;	info.isSuperAllowed(),
			supref @= super_types,
			info.enterSuper() : info.leaveSuper(),
			supref.getType().meta_type.tdecl.resolveMethodR(node,info,mt)
		}
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

}

@ThisIsANode(lang=CoreLang)
public final class MetaTypeDecl extends TypeDecl {
	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  = MetaTypeDecl;
	
	public MetaTypeDecl() {
		super(null);
		this.type_decl_version = 1;
		this.xmeta_type = new MetaType(this);
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
		Struct obj = (Struct)super.copy(cc);
		if (this == obj)
			return this;
		obj.xmeta_type = new MetaType(obj);
		obj.xtype = this.xmeta_type.make(TVarBld.emptySet);
		return obj;
	}
}
