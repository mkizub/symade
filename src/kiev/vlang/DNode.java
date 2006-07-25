package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.ir.java15.RDNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RLvalDNode;
import kiev.be.java15.JLvalDNode;
import kiev.be.java15.JTypeDecl;
import kiev.ir.java15.RTypeDecl;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
import syntax kiev.Syntax;

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@node
public abstract class DNode extends ASTNode {

	@virtual typedef This  ≤ DNode;
	@virtual typedef JView ≤ JDNode;
	@virtual typedef RView ≤ RDNode;
	
	public static final DNode[] emptyArray = new DNode[0];
	
	public static final int MASK_ACC_DEFAULT   = 0;
	public static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
	public static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
	public static final int MASK_ACC_PROTECTED = ACC_PROTECTED;
	public static final int MASK_ACC_NAMESPACE = ACC_PACKAGE;
	public static final int MASK_ACC_SYNTAX    = ACC_SYNTAX;
	
		 public		int			flags;
	@att public		MetaSet		meta;
	@att public		Symbol		id; // short and unique names

//	public @packed:1,flags, 0 boolean is_acc_public;
//	public @packed:1,flags, 1 boolean is_acc_private;
//	public @packed:1,flags, 2 boolean is_acc_protected;
	public @packed:3,flags, 0 int     is_access;

	public @packed:1,flags, 3 boolean is_static;
	public @packed:1,flags, 4 boolean is_final;
	public @packed:1,flags, 5 boolean is_mth_synchronized;	// method
	public @packed:1,flags, 5 boolean is_struct_super;		// struct
	public @packed:1,flags, 6 boolean is_fld_volatile;		// field
	public @packed:1,flags, 6 boolean is_mth_bridge;		// method
	public @packed:1,flags, 7 boolean is_fld_transient;	// field
	public @packed:1,flags, 7 boolean is_mth_varargs;		// method
	public @packed:1,flags, 8 boolean is_native;			// native method, backend operation/field/struct
	public @packed:1,flags, 9 boolean is_struct_interface;
	public @packed:1,flags,10 boolean is_abstract;
	public @packed:1,flags,11 boolean is_math_strict;		// strict math
	public @packed:1,flags,12 boolean is_synthetic;		// any decl that was generated (not in sources)
	public @packed:1,flags,13 boolean is_struct_annotation;
	public @packed:1,flags,14 boolean is_struct_enum;		// struct
	public @packed:1,flags,14 boolean is_fld_enum;			// field
		
	// Flags temporary used with java flags
	public @packed:1,flags,16 boolean is_forward;			// var/field/method, type is wrapper
	public @packed:1,flags,17 boolean is_virtual;			// var/field, method is 'static virtual', struct is 'view'
	public @packed:1,flags,18 boolean is_type_unerasable;	// typedecl, method/struct as parent of typedef

	public @packed:1,flags,19 boolean is_macro;			// macro-declarations for fields, methods, etc
	public @packed:1,flags,20 boolean is_struct_singleton;
	
	public @packed:1,flags,21 boolean is_tdecl_loaded;		// TypeDecl was fully loaded (from src or bytecode) 

	public @packed:1,flags,22 boolean is_has_aliases;
	public @packed:1,flags,23 boolean is_has_throws;

	
	public final boolean isPublic()				{ return this.is_access == MASK_ACC_PUBLIC; }
	public final boolean isPrivate()			{ return this.is_access == MASK_ACC_PRIVATE; }
	public final boolean isProtected()			{ return this.is_access == MASK_ACC_PROTECTED; }
	public final boolean isPkgPrivate()		{ return this.is_access == MASK_ACC_DEFAULT; }
	public final boolean isStatic()				{ return this.is_static; }
	public final boolean isFinal()				{ return this.is_final; }
	public final boolean isSynchronized()		{ return this.is_mth_synchronized; }
	public final boolean isVolatile()			{ return this.is_fld_volatile; }
	public final boolean isFieldVolatile()		{ return this.is_fld_volatile; }
	public final boolean isMethodBridge()		{ return this.is_mth_bridge; }
	public final boolean isFieldTransient()	{ return this.is_fld_transient; }
	public final boolean isMethodVarargs()		{ return this.is_mth_varargs; }
	public final boolean isStructBcLoaded()	{ return this.is_struct_bytecode; }
	public final boolean isNative()				{ return this.is_native; }
	public final boolean isInterface()			{ return this.is_struct_interface; }
	public final boolean isAbstract()			{ return this.is_abstract; }
	public final boolean isMathStrict()		{ return this.is_math_strict; }
	public final boolean isSynthetic()			{ return this.is_synthetic; }

	public final boolean isMacro()				{ return this.is_macro; }
	
	public final boolean isStructView()		{ return this.is_virtual; }
	public final boolean isTypeUnerasable()	{ return this.is_type_unerasable; }
	public final boolean isPackage()			{ return this.is_access == MASK_ACC_NAMESPACE; }
	public final boolean isSyntax()				{ return this.is_access == MASK_ACC_SYNTAX; }

	public void setPublic() {
		MetaAccess m = (MetaAccess)this.meta.getF("kiev.stdlib.meta.access");
		if (m == null || m.value != "public")
			this.meta.setF(new MetaAccess(MetaAccess.AccessValue.Public));
	}
	public void setPrivate() {
		MetaAccess m = (MetaAccess)this.meta.getF("kiev.stdlib.meta.access");
		if (m == null || m.value != "private")
			this.meta.setF(new MetaAccess(MetaAccess.AccessValue.Private));
	}
	public void setProtected() {
		MetaAccess m = (MetaAccess)this.meta.getF("kiev.stdlib.meta.access");
		if (m == null || m.value != "protected")
			this.meta.setF(new MetaAccess(MetaAccess.AccessValue.Protected));
	}
	public void setPkgPrivate() {
		MetaAccess m = (MetaAccess)this.meta.getF("kiev.stdlib.meta.access");
		if (m != null)
			m.detach();
	}
	public final void setPackage() {
		if (this.is_access != MASK_ACC_NAMESPACE) {
			assert(!locked);
			this.is_access = MASK_ACC_NAMESPACE;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public final void setSyntax() {
		if (this.is_access != MASK_ACC_SYNTAX) {
			assert(!locked);
			this.is_access = MASK_ACC_SYNTAX;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public void setStatic(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.static");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaStatic());
		}
	}
	public void setFinal(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.final");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaFinal());
		}
	}
	public void setSynchronized(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.synchronized");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaSynchronized());
		}
	}
	public void setFieldVolatile(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.volatile");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaVolatile());
		}
	}
	public void setMethodBridge(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.bridge");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaBridge());
		}
	}
	public void setFieldTransient(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.transient");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaTransient());
		}
	}
	public void setMethodVarargs(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.varargs");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaVarArgs());
		}
	}
	public void setNative(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.native");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaNative());
		}
	}
	public void setInterface(boolean on) {
		if (this.is_struct_interface != on) {
			assert(!locked);
			this.is_struct_interface = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setAbstract(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.abstract");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaAbstract());
		}
	}
	public void setSynthetic(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.synthetic");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaSynthetic());
		}
	}

	public void setMacro(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.macro");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaMacro());
		}
	}

	public void setStructView() {
		if (!this.is_virtual) {
			assert(!locked);
			this.is_virtual = true;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setTypeUnerasable(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.unerasable");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaUnerasable());
		}
	}

	public final boolean isVirtual() {
		return this.is_virtual;
	}
	public final void setVirtual(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.virtual");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaVirtual());
		}
	}

	@getter public final boolean isForward() {
		return this.is_forward;
	}
	@setter public final void setForward(boolean on) {
		MetaFlag m = this.meta.getF("kiev.stdlib.meta.forward");
		if (m != null) {
			if!(on) m.detach();
		} else {
			if (on) this.meta.setF(new MetaForward());
		}
	}

	public DNode() {
		this.meta = new MetaSet();
		this.id = new Symbol();
	}

	public ASTNode getDummyNode() {
		return DummyDNode.dummyNode;
	}
	
	public final void resolveDecl() { ((RView)this).resolveDecl(); }

	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }

	public boolean hasName(String nm, boolean by_equals) {
		if (id == null) return false;
		if (by_equals) {
			if (id.sname == nm) return true;
			if (id.uname == nm) return true;
			if (id.aliases != null) {
				foreach(String n; id.aliases; n == nm)
					return true;
			}
		} else {
			if (id.sname != null && id.sname.startsWith(nm)) return true;
			if (id.uname != null && id.uname.startsWith(nm)) return true;
			if (id.aliases != null) {
				foreach(String n; id.aliases; n.startsWith(nm))
					return true;
			}
		}
		return false;
	}

}

@node
public final class DummyDNode extends DNode {
	public static final DummyDNode dummyNode = new DummyDNode();

	@virtual typedef This  = DummyDNode;

	private DummyDNode() {}
}



/**
 * An lvalue dnode (var or field)
 */
@node
public abstract class LvalDNode extends DNode {

	@virtual typedef This  ≤ LvalDNode;
	@virtual typedef JView ≤ JLvalDNode;
	@virtual typedef RView ≤ RLvalDNode;

	// init wrapper
	@getter public final boolean isInitWrapper() {
		return this.is_init_wrapper;
	}
	@setter public final void setInitWrapper(boolean on) {
		if (this.is_init_wrapper != on) {
			assert(!locked);
			this.is_init_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// need a proxy access 
	@getter public final boolean isNeedProxy() {
		return this.is_need_proxy;
	}
	@setter public final void setNeedProxy(boolean on) {
		if (this.is_need_proxy != on) {
			this.is_need_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public LvalDNode() {}

}


@node
public class TypeDecl extends DNode implements ScopeOfNames, ScopeOfMethods {

	@dflow(in="root()") private static class DFI {
	@dflow(in="this:in", seq="false")	DNode[]		members;
	}

	@virtual typedef This  ≤ TypeDecl;
	@virtual typedef JView ≤ JTypeDecl;
	@virtual typedef RView ≤ RTypeDecl;

	public static final TypeDecl[] emptyArray = new TypeDecl[0];
	
	@ref public Struct						package_clazz;
	@att public TypeConstr[]				args;
	@att public TypeRef[]					super_types;
	@att public ASTNode[]					members;
		 private MetaType[]					super_meta_types;
	@ref private TypeDecl[]					direct_extenders;
		 public int							type_decl_version;
		 public String						q_name;	// qualified name
		 public MetaType					xmeta_type;
		 public Type						xtype;

	@getter public TypeDecl get$child_ctx_tdecl()	{ return this; }

	public boolean isClazz() {
		return false;
	}
	// resolved
	@getter public final boolean isTypeDeclLoaded() {
		return this.is_tdecl_loaded;
	}
	@setter public final void setTypeDeclLoaded(boolean on) {
		if (this.is_tdecl_loaded != on) {
			this.is_tdecl_loaded = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a structure with the only one instance (singleton)	
	public final boolean isSingleton() {
		return this.is_struct_singleton;
	}
	public final void setSingleton(boolean on) {
		if (this.is_struct_singleton != on) {
			assert(!locked);
			this.is_struct_singleton = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local (in method) class	
	public final boolean isLocal() {
		return this.is_struct_local;
	}
	public final void setLocal(boolean on) {
		if (this.is_struct_local != on) {
			assert(!locked);
			this.is_struct_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// an anonymouse (unnamed) class	
	public final boolean isAnonymouse() {
		return this.is_struct_anomymouse;
	}
	public final void setAnonymouse(boolean on) {
		if (this.is_struct_anomymouse != on) {
			assert(!locked);
			this.is_struct_anomymouse = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev annotation
	public final boolean isAnnotation() {
		return this.is_struct_annotation;
	}
	public final void setAnnotation(boolean on) {
		assert(!on || (!isPackage() && !isSyntax()));
		if (this.is_struct_annotation != on) {
			this.is_struct_annotation = on;
			if (on) this.setInterface(true);
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// java enum
	public final boolean isEnum() {
		return this.is_struct_enum;
	}
	public final void setEnum(boolean on) {
		if (this.is_struct_enum != on) {
			assert(!locked);
			this.is_struct_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// structure was loaded from bytecode
	public final boolean isLoadedFromBytecode() {
		return this.is_struct_bytecode;
	}
	public final void setLoadedFromBytecode(boolean on) {
		assert(!locked);
		this.is_struct_bytecode = on;
	}

	// indicates that type of the structure was attached
	public final boolean isTypeResolved() {
		return this.is_struct_fe_passed || this.is_struct_type_resolved;
	}
	public final void setTypeResolved(boolean on) {
		assert (!this.is_struct_fe_passed);
		if (this.is_struct_type_resolved != on) {
			assert(!locked);
			this.is_struct_type_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that type arguments of the structure were resolved
	public final boolean isArgsResolved() {
		return this.is_struct_fe_passed || this.is_struct_args_resolved;
	}
	public final void setArgsResolved(boolean on) {
		assert (!this.is_struct_fe_passed);
		if (this.is_struct_args_resolved != on) {
			assert(!locked);
			this.is_struct_args_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
	
	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "args" || attr.name == "super_types") {
			this.callbackSuperTypeChanged(this);
		}
	}

	public TypeDecl() {}

	public Type getType() { return this.xtype == null ? Type.tpVoid : this.xtype; }
	public boolean checkResolved() { return true; }
	public Struct getStruct() { return null; }

	public final boolean isTypeAbstract()		{ return this.isAbstract(); }
	public final boolean isTypeVirtual()		{ return this.isVirtual(); }
	public final boolean isTypeFinal()			{ return this.isFinal(); }
	public final boolean isTypeStatic()		{ return this.isStatic(); }
	public final boolean isTypeForward()		{ return this.isForward(); }

	public String qname() {
		if (q_name != null)
			return q_name;
		Struct pkg = package_clazz;
		if (pkg == null)
			return null;
		q_name = (pkg.qname()+"."+id.uname).intern();
		return q_name;
	}

	public String toString() { return package_clazz==null ? id.uname : qname(); }

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
		foreach (Field f; this.members; f.id.equals(name))
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

	public Method resolveMethod(String name, Type ret, ...) {
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
		trace(Kiev.debugResolve,"TypeDecl: Resolving name "+info.getName()+" in "+this),
		checkResolved(),
		{
			trace(Kiev.debugResolve,"TypeDecl: resolving in "+this),
			resolveNameR_1(node,info), // resolve in this class
			$cut
		;	info.isSuperAllowed(),
			info.space_prev == null || (info.space_prev.pslot().name != "super_types"),
			trace(Kiev.debugResolve,"TypeDecl: resolving in super-class of "+this),
			resolveNameR_3(node,info), // resolve in super-classes
			$cut
		}
	}
	protected rule resolveNameR_1(ASTNode@ node, ResInfo info)
	{
			info.checkNodeName(this),
			node ?= this
		;	node @= args,
			info.checkNodeName(node)
		;	node @= members,
			info.checkNodeName(node),
			info.check(node)
	}
	protected rule resolveNameR_3(ASTNode@ node, ResInfo info)
		TypeRef@ sup_ref;
	{
		sup_ref @= super_types,
		sup_ref.getTypeDecl() != null,
		info.enterSuper() : info.leaveSuper(),
		sup_ref.getTypeDecl().resolveNameR(node,info)
	}

	final public rule resolveMethodR(Method@ node, ResInfo info, CallType mt)
		ASTNode@ member;
		TypeRef@ supref;
	{
		info.isStaticAllowed(),
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info.check(member),
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;	info.isImportsAllowed() && isPackage(),
			member @= members, member instanceof Method,
			node ?= ((Method)member),
			((Method)node).equalsByCast(info.getName(),mt,Type.tpVoid,info)
		;	info.isSuperAllowed(),
			supref @= super_types,
			info.enterSuper() : info.leaveSuper(),
			supref.getType().meta_type.tdecl.resolveMethodR(node,info,mt)
		}
	}

}


