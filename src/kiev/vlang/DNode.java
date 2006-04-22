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

	@virtual typedef This  = DNode;
	@virtual typedef VView = VDNode;
	@virtual typedef JView = JDNode;
	@virtual typedef RView = RDNode;
	
	public static final DNode[] emptyArray = new DNode[0];
	
	private static final int MASK_ACC_DEFAULT   = 0;
	private static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
	private static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
	private static final int MASK_ACC_PROTECTED = ACC_PROTECTED;
	private static final int MASK_ACC_NAMESPACE = ACC_PACKAGE;
	private static final int MASK_ACC_SYNTAX    = ACC_SYNTAX;
	
		 public		int			flags;
	@att public		MetaSet		meta;

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
	public @packed:1,flags, 8 boolean is_mth_native;
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
	public final boolean isMethodNative()		{ return this.is_mth_native; }
	public final boolean isInterface()			{ return this.is_struct_interface; }
	public final boolean isAbstract()			{ return this.is_abstract; }
	public final boolean isMathStrict()		{ return this.is_math_strict; }
	public final boolean isSynthetic()			{ return this.is_synthetic; }
	
	public final boolean isStructView()		{ return this.is_virtual; }
	public final boolean isTypeUnerasable()	{ return this.is_type_unerasable; }
	public final boolean isPackage()			{ return this.is_access == MASK_ACC_NAMESPACE; }
	public final boolean isSyntax()				{ return this.is_access == MASK_ACC_SYNTAX; }

	public void setPublic() {
		if (this.is_access != MASK_ACC_PUBLIC) {
			this.is_access = MASK_ACC_PUBLIC;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setPrivate() {
		if (this.is_access != MASK_ACC_PRIVATE) {
			this.is_access = MASK_ACC_PRIVATE;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setProtected() {
		if (this.is_access != MASK_ACC_PROTECTED) {
			this.is_access = MASK_ACC_PROTECTED;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setPkgPrivate() {
		if (this.is_access != MASK_ACC_DEFAULT) {
			this.is_access = MASK_ACC_DEFAULT;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public final void setPackage() {
		if (this.is_access != MASK_ACC_NAMESPACE) {
			this.is_access = MASK_ACC_NAMESPACE;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public final void setSyntax() {
		if (this.is_access != MASK_ACC_SYNTAX) {
			this.is_access = MASK_ACC_SYNTAX;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public void setStatic(boolean on) {
		if (this.is_static != on) {
			this.is_static = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setFinal(boolean on) {
		if (this.is_final != on) {
			this.is_final = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setSynchronized(boolean on) {
		if (this.is_mth_synchronized != on) {
			this.is_mth_synchronized = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setVolatile(boolean on) {
		if (this.is_fld_volatile != on) {
			this.is_fld_volatile = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setFieldVolatile(boolean on) {
		if (this.is_fld_volatile != on) {
			this.is_fld_volatile = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setMethodBridge(boolean on) {
		if (this.is_mth_bridge != on) {
			this.is_mth_bridge = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setFieldTransient(boolean on) {
		if (this.is_fld_transient != on) {
			this.is_fld_transient = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setMethodVarargs(boolean on) {
		if (this.is_mth_varargs != on) {
			this.is_mth_varargs = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setMethodNative(boolean on) {
		if (this.is_mth_native != on) {
			this.is_mth_native = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setInterface(boolean on) {
		if (this.is_struct_interface != on) {
			this.is_struct_interface = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setAbstract(boolean on) {
		if (this.is_abstract != on) {
			this.is_abstract = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setSynthetic(boolean on) {
		if (this.is_synthetic != on) {
			this.is_synthetic = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public void setStructView() {
		if (!this.is_virtual) {
			this.is_virtual = true;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	public void setTypeUnerasable(boolean on) {
		if (this.is_type_unerasable != on) {
			this.is_type_unerasable = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public final boolean isVirtual() {
		return this.is_virtual;
	}
	public final void setVirtual(boolean on) {
		if (this.is_virtual != on) {
			this.is_virtual = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	@getter public final boolean isForward() {
		return this.is_forward;
	}
	@setter public final void setForward(boolean on) {
		if (this.is_forward != on) {
			this.is_forward = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	@nodeview
	public static abstract view VDNode of DNode extends NodeView {

		public Dumper toJavaDecl(Dumper dmp);
		
		public int		flags;
		public MetaSet	meta;

		public final boolean isPublic()	;
		public final boolean isPrivate();
		public final boolean isProtected();
		public final boolean isPkgPrivate();
		public final boolean isStatic();
		public final boolean isFinal();
		public final boolean isSynchronized();
		public final boolean isVolatile();
		public final boolean isFieldVolatile();
		public final boolean isMethodBridge();
		public final boolean isFieldTransient();
		public final boolean isMethodVarargs();
		public final boolean isStructBcLoaded();
		public final boolean isMethodNative();
		public final boolean isInterface();
		public final boolean isAbstract();
		public final boolean isMathStrict();
		public final boolean isSynthetic();
		
		public final boolean isStructView();
		public final boolean isTypeUnerasable();
		public final boolean isPackage();
		public final boolean isSyntax();

		public final void setPublic();
		public final void setPrivate();
		public final void setProtected();
		public final void setPkgPrivate();
		public final void setPackage();
		public final void setSyntax();
		public final void setStatic(boolean on);
		public final void setFinal(boolean on);
		public final void setSynchronized(boolean on);
		public final void setVolatile(boolean on);
		public final void setFieldVolatile(boolean on);
		public final void setMethodBridge(boolean on);
		public final void setFieldTransient(boolean on);
		public final void setMethodVarargs(boolean on);
		public final void setMethodNative(boolean on);
		public final void setInterface(boolean on);
		public final void setAbstract(boolean on);
		public final void setStructView();
		public final void setTypeUnerasable(boolean on);
		public final boolean isVirtual();
		public final void setVirtual(boolean on);
		public final boolean isForward();
		public final void setForward(boolean on);
	}

	public DNode() {}

	public ASTNode getDummyNode() {
		return DummyDNode.dummyNode;
	}
	
	public final void resolveDecl() { ((RView)this).resolveDecl(); }
	public abstract Dumper toJavaDecl(Dumper dmp);

	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }
}

@node
public final class DummyDNode extends DNode {
	public static final DummyDNode dummyNode = new DummyDNode();

	@virtual typedef This  = DummyDNode;
	@virtual typedef VView = VDummyDNode;

	@nodeview
	public static final view VDummyDNode of DummyDNode extends VDNode {
	}
	private DummyDNode() {}
}



/**
 * An lvalue dnode (var or field)
 */
@node
public abstract class LvalDNode extends DNode {

	@virtual typedef This  = LvalDNode;
	@virtual typedef VView = VLvalDNode;
	@virtual typedef JView = JLvalDNode;
	@virtual typedef RView = RLvalDNode;

	// init wrapper
	@getter public final boolean isInitWrapper() {
		return this.is_init_wrapper;
	}
	@setter public final void setInitWrapper(boolean on) {
		if (this.is_init_wrapper != on) {
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

	@nodeview
	public static abstract view VLvalDNode of LvalDNode extends VDNode {
		// init wrapper
		public final boolean isInitWrapper();
		public final void setInitWrapper(boolean on);
		// need a proxy access 
		public final boolean isNeedProxy();
		public final void setNeedProxy(boolean on);
	}

	public LvalDNode() {}

}


@node
public abstract class TypeDecl extends DNode implements Named {

	@virtual typedef This  = TypeDecl;
	@virtual typedef VView = VTypeDecl;
	@virtual typedef JView = JTypeDecl;
	@virtual typedef RView = RTypeDecl;

	public void callbackSuperTypeChanged(TypeDecl chg) {}
	public TypeProvider[] getAllSuperTypes() { return TypeProvider.emptyArray; }
	protected final void addSuperTypes(TypeRef suptr, Vector<TypeProvider> types) {
		Type sup = suptr.getType();
		if (sup == null)
			return;
		TypeProvider tt = sup.getStruct().imeta_type;
		if (!types.contains(tt))
			types.append(tt);
		TypeProvider[] sup_types = sup.getStruct().getAllSuperTypes();
		foreach (TypeProvider t; sup_types) {
			if (!types.contains(t))
				types.append(t);
		}
	}

	@nodeview
	public static view VTypeDecl of TypeDecl extends VDNode {
		public TypeProvider[] getAllSuperTypes();
	}

	public TypeDecl() {}

	public abstract NodeName	getName();
	public abstract boolean		checkResolved();
	public abstract Struct		getStruct();

	public final boolean isTypeAbstract()		{ return this.isAbstract(); }
	public final boolean isTypeVirtual()		{ return this.isVirtual(); }
	public final boolean isTypeFinal()			{ return this.isFinal(); }
	public final boolean isTypeStatic()		{ return this.isStatic(); }
	public final boolean isTypeForward()		{ return this.isForward(); }
}


