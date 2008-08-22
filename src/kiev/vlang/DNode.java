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

import kiev.ir.java15.RDNode;

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

	@AttrXMLDumpInfo(ignore=true)
	@nodeAttr
	public Symbol		symbol;

	@abstract
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr
	public							String			sname; // source code name, may be null for anonymouse symbols

	@abstract
	@AttrXMLDumpInfo(attr=true)
	@UnVersioned
	@nodeAttr(copyable=false)
	public							String			uuid;  // UUID of the node, since it's an ISymbol


	@virtual @abstract
	public:ro						DNode			dnode;


	public final MetaAccess getMetaAccess() {
		return (MetaAccess)this.getMeta("kiev·stdlib·meta·access");
	}

	@getter final public DNode get$dnode() { return this; }
	@getter final public Symbol get$symbol() { return this.symbol; }

	@getter final public String get$sname() {
		return this.symbol.sname;
	}
	@setter final public void set$sname(String value) {
		//if (this instanceof MethodImpl && value != null && (value.startsWith(nameGet) || value.startsWith(nameSet)))
		//	Kiev.reportWarning(this, "Setting name "+value+" on MethodImpl");
		this.symbol.sname = value;
	}
	@getter final public String get$uuid() {
		return this.symbol.uuid;
	}
	@setter final public void set$uuid(String value) {
		this.symbol.uuid = value;
	}
	
	public final boolean isPublic()			{ return this.mflags_access == MASK_ACC_PUBLIC; }
	public final boolean isPrivate()			{ return this.mflags_access == MASK_ACC_PRIVATE; }
	public final boolean isProtected()			{ return this.mflags_access == MASK_ACC_PROTECTED; }
	public final boolean isPkgPrivate()		{ return this.mflags_access == MASK_ACC_DEFAULT; }
	public final boolean isStatic()			{ return this.mflags_is_static; }
	public final boolean isFinal()				{ return this.mflags_is_final; }
	public final boolean isSynchronized()		{ return this.mflags_is_mth_synchronized; }
	public final boolean isFieldVolatile()		{ return this.mflags_is_fld_volatile; }
	public final boolean isMethodBridge()		{ return this.mflags_is_mth_bridge; }
	public final boolean isFieldTransient()	{ return this.mflags_is_fld_transient; }
	public final boolean isMethodVarargs()		{ return this.mflags_is_mth_varargs; }
	public final boolean isNative()			{ return this.mflags_is_native; }
	public final boolean isInterface()			{ return this.mflags_is_struct_interface; }
	public final boolean isAbstract()			{ return this.mflags_is_abstract; }
	public final boolean isMathStrict()		{ return this.mflags_is_math_strict; }
	public final boolean isSynthetic()			{ return this.mflags_is_synthetic; }

	public final boolean isMacro()				{ return this.mflags_is_macro; }
	public final boolean isVirtual()			{ return this.mflags_is_virtual; }
	public final boolean isForward()			{ return this.mflags_is_forward; }
	
	public final boolean isStructView()		{ return this instanceof KievView; }
	public final boolean isTypeUnerasable()	{ return this.mflags_is_type_unerasable; }
	public final boolean isStructInner()		{ return !(this.parent() instanceof KievPackage); }

	public final boolean isInterfaceOnly()		{ return this.is_interface_only; }

	public void setPublic() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("public"));
		else if (m.simple != "public")
			m.simple = "public";
	}
	public void setPrivate() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("private"));
		else if (m.simple != "private")
			m.simple = "private";
	}
	public void setProtected() {
		MetaAccess m = getMetaAccess();
		if (m == null)
			this.setMeta(new MetaAccess("protected"));
		else if (m.simple != "protected")
			m.simple = "protected";
	}
	public void setPkgPrivate() {
		MetaAccess m = getMetaAccess();
		if (m != null) {
			if (m.flags != -1 || m.flags != 0xF)
				m.simple = "";
			else
				m.detach();
		}
	}
	public void setPkgPrivateKeepAccess() {
		int flags = MetaAccess.getFlags(this);
		MetaAccess m = getMetaAccess();
		if (m != null) {
			m.simple = "";
			m.flags = flags;
		} else {
			this.setMeta(new MetaAccess("", flags));
		}
	}
	public void setPrivateKeepAccess() {
		int flags = MetaAccess.getFlags(this);
		MetaAccess m = getMetaAccess();
		if (m != null) {
			m.simple = "private";
			m.flags = flags;
		} else {
			this.setMeta(new MetaAccess("private", flags));
		}
	}

	public void setStatic(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·static");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaStatic());
		}
	}
	public void setFinal(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·final");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaFinal());
		}
	}
	public void setSynchronized(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·synchronized");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynchronized());
		}
	}
	public void setFieldVolatile(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·volatile");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVolatile());
		}
	}
	public void setMethodBridge(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·bridge");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaBridge());
		}
	}
	public void setFieldTransient(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·transient");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaTransient());
		}
	}
	public void setMethodVarargs(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·varargs");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVarArgs());
		}
	}
	public void setNative(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·native");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaNative());
		}
	}
	public void setAbstract(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·abstract");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaAbstract());
		}
	}
	public void setSynthetic(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·synthetic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaSynthetic());
		}
	}

	public void setMacro(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·macro");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaMacro());
		}
	}

	public void setTypeUnerasable(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·unerasable");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaUnerasable());
		}
	}

	public final void setVirtual(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·virtual");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.setMeta(new MetaVirtual());
		}
	}

	public final void setForward(boolean on) {
		MetaFlag m = (MetaFlag)this.getMeta("kiev·stdlib·meta·forward");
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

	public DNode() {
		this.symbol = new Symbol();
	}

	public Object copy(CopyContext cc) {
		ANode obj = cc.hasCopyOf(this);
		if (obj != null)
			return obj;
		return super.copy(cc);
	}

	public String toString() { return sname; }

	public final void resolveDecl() { ((RDNode)this).resolveDecl(); }

	public int getFlags() {
		return this.nodeflags & 0xFFFFF;
	}
	public short getJavaFlags() {
		return (short)(getFlags() & JAVA_ACC_MASK);
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this" && ((isPrivate() && !isMacro()) || isAutoGenerated() || isSynthetic()))
			return false;
		return super.includeInDump(dump, attr, val);
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
		foreach (MNode m; metas; m.qname() == name)
			return m;
		return null;
	}
	
	public final MNode setMeta(MNode meta)  alias add alias lfy operator +=
	{
		String qname = meta.qname();
		foreach (MNode m; metas; m.qname() == qname) {
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

public interface GlobalDNodeContainer extends GlobalDNode, ScopeOfNames {
	public ASTNode[] getContainerMembers();
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

	public ComplexTypeDecl get_child_ctx_tdecl()	{ null }

	public ASTNode[] getContainerMembers() { ASTNode.emptyArray }

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
	// kiev annotation
	public final boolean isAnnotation() {
		return this.mflags_is_struct_annotation;
	}
	// java enum
	public final boolean isEnum() {
		return this.mflags_is_enum;
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

	// a structure with the only one instance (singleton)	
	public final boolean isSingleton() {
		return this.getMeta("kiev·stdlib·meta·singleton") != null;
	}
	// an interface with methdos and fields (mixin)	
	public final boolean isMixin() {
		return this.getMeta("kiev·stdlib·meta·mixin") != null;
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

	public void cleanupOnReload() {
		this.super_types.delAll();
		this.metas.delAll();
		this.nodeflags = 0;
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
		ResInfo<Method> info = new ResInfo<Method>(this,name,ResInfo.noForwards|ResInfo.noSyntaxContext|ResInfo.noStatic);
		if (!this.xtype.resolveCallAccessR(info, mt)) {
			info = new ResInfo<Method>(this,name,ResInfo.noForwards|ResInfo.noSyntaxContext);
			if (!this.resolveMethodR(info, mt))
				throw new CompilerException(this,"Unresolved method "+name+mt+" in class "+this);
		}
		return info.resolvedDNode();
	}

	public rule resolveNameR(ResInfo info)
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: Resolving name "+info.getName()+" in "+this),
		checkResolved(),
		{
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in "+this),
			resolveNameR_1(info) // resolve in this class
		;
			info.isSuperAllowed(),
			info.getPrevSlotName() != "super_types",
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in super-class of "+this),
			resolveNameR_3(info) // resolve in super-classes
		}
	}
	protected rule resolveNameR_1(ResInfo info)
	{
		info ?= this
	}
	protected rule resolveNameR_3(ResInfo info)
		TypeRef@ sup_ref;
	{
		sup_ref @= super_types,
		sup_ref.getTypeDecl() != null,
		info.enterSuper() : info.leaveSuper(),
		sup_ref.getTypeDecl().resolveNameR(info)
	}

	public rule resolveMethodR(ResInfo info, CallType mt)
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		info.isSuperAllowed(),
		checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		supref @= super_types,
		info.enterSuper() : info.leaveSuper(),
		supref.getType().meta_type.tdecl.resolveMethodR(info,mt)
	}
	
}

@ThisIsANode(lang=CoreLang)
public abstract class ComplexTypeDecl extends TypeDecl implements GlobalDNodeContainer, CompilationUnit {

	@DataFlowDefinition(in="root()") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="false")	DNode[]		members;
	}

	@nodeAttr public TypeConstr∅					args;
	@nodeAttr public ASTNode∅						members;
	          public String							q_name;	// qualified name

	@nodeData(ext_data=true, copyable=false) public KString			bytecode_name; // used by backend for anonymouse and inner declarations
	@nodeData(ext_data=true, copyable=false) public TypeAssign			ometa_tdef;

	public ComplexTypeDecl get_child_ctx_tdecl()	{ return this; }

	public final ASTNode[] getContainerMembers() { this.members }
	
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
	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic())
			resetNames();
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic())
			resetNames();
		super.callbackDetached(parent, slot);
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
			q_name = (p.qname()+"·"+sname).intern();
		else
			q_name = sname;
		return q_name;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('·','.');
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

	protected rule resolveNameR_1(ResInfo info)
	{
			info ?= this
		;	info @= args
		;	info @= members
	}

	protected rule resolveNameR_Syntax(ResInfo info)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import ("+(info.doImportStar() ? "with star" : "no star" )+"): "+syn),
		((Import)syn).resolveNameR(info)
	}

	public rule resolveMethodR(ResInfo info, CallType mt)
		ASTNode@ member;
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info ?= ((Method)member).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;	info.isSuperAllowed(),
			supref @= super_types,
			info.enterSuper() : info.leaveSuper(),
			supref.getType().meta_type.tdecl.resolveMethodR(info,mt)
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
public final class KievSyntax extends DNode implements GlobalDNodeContainer, ScopeOfMethods, CompilationUnit {
	@SymbolRefAutoComplete(scopes={KievPackage})
	@nodeAttr public SymbolRef<KievSyntax>∅		super_syntax;
	@nodeAttr public ASTNode∅			members;

	public KievSyntax() {}
	
	public final ASTNode[] getContainerMembers() { this.members }
	
	public String qname() {
		if (sname == null || sname == "")
			return null;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			return (p.qname()+"·"+sname).intern();
		return sname;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('·','.');
	}

	public rule resolveNameR(ResInfo info)
		ASTNode@ n;
		SymbolRef@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: Resolving name "+info.getName()+" in "+this),
		{
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in "+this),
			info ?= this
		;	// resolve in this syntax
			info @= members
		;	// resolve in imports and opdefs
			n @= members,
			{
				n instanceof Import,
				trace( Kiev.debug && Kiev.debugResolve, "In import ("+(info.doImportStar() ? "with star" : "no star" )+"): "+n),
				((Import)n).resolveNameR(info)
			;
				n instanceof ImportSyntax,
				trace( Kiev.debug && Kiev.debugResolve, "In syntax ("+(info.doImportStar() ? "with star" : "no star" )+"): "+n),
				((ImportSyntax)n).resolveNameR(info)
			;
				n instanceof Opdef,
				((Opdef)n).resolveNameR(info)
			}
		;
			info.getPrevSlotName() != "super_syntax",
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in super-syntax of "+this),
			super_stx @= super_syntax,
			super_stx.dnode instanceof KievSyntax,
			((KievSyntax)super_stx.dnode).resolveNameR(info)
		}
	}

	public rule resolveMethodR(ResInfo info, CallType mt)
		ASTNode@ member;
		SymbolRef<KievSyntax>@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info ?= ((Method)member).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;
			member @= members,
			{
				member instanceof Import,
				((Import)member).resolveMethodR(info,mt)
			;
				member instanceof ImportSyntax,
				((ImportSyntax)member).resolveMethodR(info,mt)
			}
		;
			super_stx @= super_syntax,
			super_stx.dnode != null,
			super_stx.dnode.resolveMethodR(info,mt)
		}
	}
}

@ThisIsANode(lang=CoreLang)
public class KievPackage extends DNode implements GlobalDNodeContainer {

	@nodeAttr public DNode∅						pkg_members;
	
	public final ASTNode[] getContainerMembers() { this.pkg_members }

	public String qname() {
		ANode p = parent();
		if ((p instanceof KievPackage) && !(p instanceof Env))
			return (p.qname()+"·"+sname).intern();
		return sname;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('·','.');
	}

	public final rule resolveNameR(ResInfo info)
		DNode@ dn;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"KievPackage: Resolving name "+info.getName()+" in "+this),
		{
			trace(Kiev.debug && Kiev.debugResolve,"TypeDecl: resolving in "+this),
			info ?= this
		;
			info @= pkg_members
		;
			info.isCmpByEquals(),
			info ?= tryLoad(info.getName())
		}
	}

	public DNode tryLoad(String name) {
		trace(Kiev.debug && Kiev.debugResolve,"Package: trying to load in package "+this);
		DNode dn;
		String qn = name;
		if (this instanceof Env)
			dn = Env.getRoot().loadAnyDecl(qn);
		else
			dn = Env.getRoot().loadAnyDecl(qn=(this.qname()+"·"+name));
		trace(Kiev.debug && Kiev.debugResolve,"DNode "+(dn != null ? dn+" found " : qn+" not found")+" in "+this);
		return dn;
	}
	
}

