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
 *
 */

@ThisIsANode(lang=CoreLang)
public final class MetaUUID extends UserMeta {
	@AttrXMLDumpInfo(attr=true, name="uuid")
	@nodeAttr public String				value;

	public MetaUUID() { super("kiev·stdlib·meta·uuid"); }

	public void callbackChanged(NodeChangeInfo info) {
		if (info.tree_change && info.slot.isSemantic()) {
			if      (info.ct == ChangeType.THIS_ATTACHED)
				setFlag(info.parent.asANode(), true);
			else if (info.ct == ChangeType.THIS_DETACHED)
				setFlag(info.parent.asANode(), false);
		}
		super.callbackChanged(info);
	}

	private void setFlag(ANode parent, boolean on) {
		if (parent instanceof DNode) {
			DNode dn = (DNode)parent;
			if (on) {
				if (dn.symbol.suuid() != null)
					this.value = dn.symbol.suuid().toString();
				else if (this.value != null)
					dn.symbol.setUUID(Env.getEnv(),this.value);
			}
		}
	}

	@setter public void set$value(String val) {
		if (val != null)
			val = val.intern();
		ANode p = parent();
		if (p instanceof DNode) {
			DNode dn = (DNode)p;
			if (dn.symbol.suuid() != null)
				return;
			dn.symbol.setUUID(Env.getEnv(),val);
		}
		this.value = val;
		super.setS("value",val);
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "this")
			return true;
		if (dump == "api" && attr.name == "values")
			return false;
		return super.includeInDump(dump, attr, val);
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaPacked extends UserMeta {
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int					size;
	@nodeAttr public SymbolRef<Field>		fld;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int					offset;

	public MetaPacked() { super("kiev·stdlib·meta·packed"); fld = new SymbolRef<Field>(); }

	@setter public void set$size(int val) {
		size = val;
		super.setI("size",val);
	}
	
	@setter public void set$offset(int val) {
		offset = val;
		super.setI("offset", val);
	}
	
	@setter public void set$fld(SymbolRef<Field> val) {
		if (fld == null) {
			fld = val;
		}
		else if (val == null || val.name == null) {
			fld.name = null;
			super.unset("in");
		}
		else {
			fld = val;
			super.setS("in", fld.name);
		}
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaPacker extends UserMeta {
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int		 size = -1;

	public MetaPacker() { super("kiev·stdlib·meta·packer"); }

	@setter public void set$size(int val) {
		size = val;
		super.setI("size",val);
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaThrows extends UserMeta {
	@AttrBinDumpInfo(ignore=true)
	@nodeData @abstract public ASTNode∅		 exceptions;

	public MetaThrows() { super("kiev·stdlib·meta·throws"); }

	public void add(TypeRef thr) {
		exceptions += thr;
		if (values.length == 0)
			values += new MetaValueArray(new SymbolRef("value"));
		MetaValueArray mva = (MetaValueArray)values[0];
		assert (mva.ident == "value");
		mva.values += thr;
	}
	
	@getter
	public ASTNode[] get$exceptions() {
		return getThrowns();
	}
	@setter
	public void set$exceptions(ASTNode[] val) {
	}
	public ASTNode[] getThrowns() {
		if (values.length == 0)
			return ASTNode.emptyArray;
		MetaValueArray mva = (MetaValueArray)values[0];
		assert (mva.ident == "value");
		return mva.values;
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaGetter extends UserMeta {
	public MetaGetter() { super("kiev·stdlib·meta·getter"); }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSetter extends UserMeta {
	public MetaSetter() { super("kiev·stdlib·meta·setter"); }
}

@ThisIsANode(lang=CoreLang)
public final class MetaAccess extends ASTNode implements MNode {
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String			simple;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int			flags;

	public @packed(1,flags,7) boolean	r_public;
	public @packed(1,flags,6) boolean	w_public;
	public @packed(1,flags,5) boolean	r_protected;
	public @packed(1,flags,4) boolean	w_protected;
	public @packed(1,flags,3) boolean	r_default;
	public @packed(1,flags,2) boolean	w_default;
	public @packed(1,flags,1) boolean	r_private;
	public @packed(1,flags,0) boolean	w_private;

	public MetaAccess() {
		this.simple = "";
		this.flags  = -1;
	}
	public MetaAccess(String simple) {
		this.simple = simple.intern();
		this.flags  = -1;
	}
	public MetaAccess(String simple, int flags) {
		this.simple = simple.intern();
		this.flags = flags;
	}

	public String qname() { return "kiev·stdlib·meta·access"; }

	public JavaAnnotation getAnnotationDecl(Env env) { return (JavaAnnotation)env.resolveGlobalDNode(this.qname()); }

	public void resolve(Env env, Type reqType) {}
	public void verify(INode parent, AttrSlot slot) {}
	public boolean isRuntimeVisible() { return false; }
	public boolean isRuntimeInvisible() { return false; }

	public boolean equals(Object o) {
		if (o instanceof MetaAccess)
			return this.simple == o.simple && this.flags == o.flags;
		return false;
	}

	//public boolean includeInDump(String dump, AttrSlot attr, Object val) {
	//	if (attr.name == "flags")
	//		return this.flags != -1;
	//	return super.includeInDump(dump, attr, val);
	//}

	public void callbackChanged(NodeChangeInfo info) {
		if (info.tree_change && info.slot.isSemantic()) {
			if      (info.ct == ChangeType.THIS_ATTACHED && info.parent instanceof DNode)
				setFlag((DNode)info.parent, true);
			else if (info.ct == ChangeType.THIS_DETACHED && info.parent instanceof DNode)
				setFlag((DNode)info.parent, false);
		}
		super.callbackChanged(info);
	}

	public boolean preVerify(Env env, INode parent, AttrSlot slot) {
		if (this.flags == -1) {
			ANode parent = parent();
			if (parent instanceof DNode) {
				this.detach(parent,slot);
				this.setFlag((DNode)parent, true);
				return false;
			}
		}
		return true;
	}

	void setFlag(DNode dn, boolean on) {
		if (dn == null)
			return;
		if (on) {
			if (simple == "public")		{ dn.mflags_access = DNode.MASK_ACC_PUBLIC; return; }
			if (simple == "protected")	{ dn.mflags_access = DNode.MASK_ACC_PROTECTED; return; }
			if (simple == "private")	{ dn.mflags_access = DNode.MASK_ACC_PRIVATE; return; }
		}
		dn.mflags_access = DNode.MASK_ACC_DEFAULT;
	}
	
	@setter public final void set$simple(String val) {
		this.simple = val.intern();
		ANode p = parent();
		if (p instanceof DNode)
			setFlag((DNode)p, true);
	}

	public static boolean readable(DNode dn) {
		int flags = getFlags(dn);
		return (flags & 0xAA) != 0;
	}
	public static boolean writeable(DNode dn) {
		int flags = getFlags(dn);
		return (flags & 0x55) != 0;
	}
	
	public static final int getFlags(DNode dn) {
		MetaAccess acc = dn.getMetaAccess();
		if (acc == null) {
			if (dn.isPublic())
				return 0xFF;
			if (dn.isProtected())
				return 0x3F;
			if (dn.isPrivate())
				return 0x03;
			return 0x0F;
		}
		if (acc.flags != -1)
			return acc.flags;
		else if (acc.simple == "public")
			return 0xFF;
		else if (acc.simple == "protected")
			return 0x3F;
		else if (acc.simple == "private")
			return 0x03;
		return 0x0F;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(simple.toString().toLowerCase()).append(':');

		if( r_public && w_public ) sb.append("rw,");
		else if( r_public ) sb.append("r,");
		else if( w_public ) sb.append("w,");
		else sb.append("n,");

		if( r_protected && w_protected ) sb.append("rw,");
		else if( r_protected ) sb.append("r,");
		else if( w_protected ) sb.append("w,");
		else sb.append("n,");

		if( r_default && w_default ) sb.append("rw,");
		else if( r_default ) sb.append("r,");
		else if( w_default ) sb.append("w,");
		else sb.append("n,");

		if( r_private && w_private ) sb.append("rw");
		else if( r_private ) sb.append("r");
		else if( w_private ) sb.append("w");
		else sb.append("n");

		return sb.toString();
	}

	public static void verifyDecl(DNode dn) {
		//MetaAccess acc = dn.getMetaAccess();
		//if (acc != null)
		//	acc.verifyAccessDecl(dn);
	}
	
	private void verifyAccessDecl(DNode n) {
		if (flags == -1)
			return;

		if( r_public || w_public ) {
			if (simple != "public") {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared public");
				n.setPublic();
			}
		}
		else if( r_protected || w_protected ) {
			if !(simple == "public" || simple == "protected") {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared protected or public");
				n.setProtected();
			}
		}
		else if( r_default || w_default ) {
			if (simple == "private") {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared with default/protected or public access");
				n.setPkgPrivate();
			}
		}
		if( r_public ) {
			if( !r_protected ) {
				Kiev.reportWarning(n,"Node "+n+" should have protected read access");
				r_protected = true;
			}
			if( !r_default ) {
				Kiev.reportWarning(n,"Node "+n+" should have default (package) read access");
				r_default = true;
			}
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_protected ) {
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_default ) {
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( w_public ) {
			if( !w_protected ) {
				Kiev.reportWarning(n,"Node "+n+" should have protected write access");
				w_protected = true;
			}
			if( !w_default ) {
				Kiev.reportWarning(n,"Node "+n+" should have default (package) write access");
				w_default = true;
			}
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_protected ) {
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_default ) {
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
	}

	public static void verifyRead(ASTNode from, DNode n) { verifyAccess(from,n,2); }
	public static void verifyWrite(ASTNode from, DNode n) { verifyAccess(from,n,1); }
	public static void verifyReadWrite(ASTNode from, DNode n) { verifyAccess(from,n,3); }

	public static boolean accessedFromInner(ASTNode from, DNode n) {
		if (!n.isPrivate())
			return false;
		ComplexTypeDecl outer1 = getStructOf(from);
		ComplexTypeDecl outer2 = getStructOf(n);
		if (outer1 == outer2)
			return false;
		while !(outer1.parent() instanceof KievPackage)
			outer1 = Env.ctxTDecl(outer1);
		while !(outer2.parent() instanceof KievPackage)
			outer2 = Env.ctxTDecl(outer2);
		if (outer1 == outer2)
			return true;
		return false;
	}

	private static ComplexTypeDecl getStructOf(ASTNode n) {
		if (n instanceof ComplexTypeDecl) return (ComplexTypeDecl)n;
		return Env.ctxTDecl(n);
	}

	private static KievPackage getPackageOf(ASTNode n) {
		ANode p = n;
		while (p != null && !(p instanceof KievPackage))
			p = p.parent();
		return (KievPackage)p;
	}

	private static void verifyAccess(ASTNode from, ASTNode n, int acc) {
		int flags = getFlags((DNode)n);

		// Quick check for public access
		if( ((flags>>>6) & acc) == acc ) {
			checkFinalWrite(from,n,acc);
			return;
		}

		// Check for private access
		if( getStructOf(from) == getStructOf(n) ) {
			if( (flags & acc) != acc ) throwAccessError(from,n,acc,"private");
			checkFinalWrite(from,n,acc);
			return;
		}

		// Check for private access from inner class
		if (((DNode)n).isPrivate()) {
			ComplexTypeDecl outer1 = getStructOf(from);
			ComplexTypeDecl outer2 = getStructOf(n);
			while !(outer1.parent() instanceof KievPackage)
				outer1 = Env.ctxTDecl(outer1);
			while !(outer2.parent() instanceof KievPackage)
				outer2 = Env.ctxTDecl(outer2);
			if (outer1 == outer2) {
				if( (flags & acc) == acc ) {
					checkFinalWrite(from,n,acc);
					return;
				}
				throwAccessError(from,n,acc,"private");
			}
		}

		// Check for default access
		if( getPackageOf(from) == getPackageOf(n) ) {
			if( ((flags>>>2) & acc) != acc ) throwAccessError(from,n,acc,"default");
			checkFinalWrite(from,n,acc);
			return;
		}

		// Check for protected access
		if( getStructOf(from).instanceOf(getStructOf(n)) ) {
			if( ((flags>>>4) & acc) != acc ) throwAccessError(from,n,acc,"protected");
			checkFinalWrite(from,n,acc);
			return;
		}

		// Public was already checked, just throw an error
		throwAccessError(from,n,acc,"public");
	}
	
	private static void checkFinalWrite(ASTNode from, ASTNode n, int acc) {
		if ((acc & 1) == 0)
			return; // read access
		if (n instanceof DNode && !n.isFinal())
			return; // not final
		if (n instanceof Field) {
			// final var, may be initialized only in constructor
			Method m = Env.ctxMethod(from);
			if (m instanceof Constructor && Env.ctxTDecl(m) == Env.ctxTDecl(n))
				return;
			if (m == null && Env.ctxTDecl(from) == Env.ctxTDecl(n))
				return;
			throwFinalWriteError(from,n);
		}
		if (n instanceof Var) {
			// final var, may be initialized only by initializer
			throwFinalWriteError(from,n);
		}
	}

	private static void throwAccessError(ASTNode from, ASTNode n, int acc, String astr) {
		StringBuffer sb = new StringBuffer();
		sb.append("Access denied - ").append(astr).append(' ');
		if (acc == 2) sb.append("read");
		else if (acc == 1) sb.append("write");
		else if (acc == 3) sb.append("read/write");
		sb.append("\n\tto ");
		if     (n instanceof Field)  sb.append("field ");
		else if(n instanceof Method) sb.append("method ");
		else if(n instanceof Struct) sb.append("class ");
		if (n instanceof Struct) sb.append(n);
		else sb.append(Env.ctxTDecl(n)).append('.').append(n);
		sb.append("\n\tfrom class ").append(getStructOf(from));
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}

	private static void throwFinalWriteError(ASTNode from, ASTNode n) {
		StringBuffer sb = new StringBuffer();
		sb.append("Write for final value is denied to ");
		if (n instanceof Field)
			sb.append("field ").append(Env.ctxTDecl(n)).append('.').append(n);
		else if (n instanceof Var)
			sb.append("var ").append(n);
		else
			sb.append(n);
		Method m = Env.ctxMethod(from);
		if (m != null)
			sb.append("\n\tin method ").append(Env.ctxTDecl(m)).append('.').append(m);
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class MetaFlag extends ANode implements MNode {

	public MetaFlag() {
		super(new AHandle(), Context.DEFAULT);
	}

	public Language getCompilerLang() { return CoreLang; }
	public String getCompilerNodeName() { return getClass().getSimpleName().intern(); }

	public final JavaAnnotation getAnnotationDecl(Env env) { return (JavaAnnotation)env.resolveGlobalDNode(this.qname()); }

	public void callbackChanged(NodeChangeInfo info) {
		if (info.tree_change && info.slot.isSemantic()) {
			if      (info.ct == ChangeType.THIS_ATTACHED && info.parent instanceof DNode)
				setFlag((DNode)info.parent, true);
			else if (info.ct == ChangeType.THIS_DETACHED && info.parent instanceof DNode)
				setFlag((DNode)info.parent, false);
		}
		super.callbackChanged(info);
	}

	public abstract int getBitPos();
	abstract void setFlag(DNode dn, boolean on);
	
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return (o.getClass() == this.getClass());
	}

	public void resolve(Env env, Type reqType) {}
	public void verify(INode parent, AttrSlot slot) {}
	public boolean isRuntimeVisible() { return false; }
	public boolean isRuntimeInvisible() { return false; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSingleton extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·singleton"; }
	public int getBitPos() { -1 }
	void setFlag(DNode dn, boolean on) {}
}

@ThisIsANode(lang=CoreLang)
public final class MetaMixin extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·mixin"; }
	public int getBitPos() { -1 }
	void setFlag(DNode dn, boolean on) {}
}

@ThisIsANode(lang=CoreLang)
public final class MetaPublic extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·public"; }
	public int getBitPos() { 0 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_access = DNode.MASK_ACC_PUBLIC; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaPrivate extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·private"; }
	public int getBitPos() { 1 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_access = DNode.MASK_ACC_PRIVATE; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaProtected extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·protected"; }
	public int getBitPos() { 2 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_access = DNode.MASK_ACC_PROTECTED; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaStatic extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·static"; }
	public int getBitPos() { 3 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_static = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaFinal extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·final"; }
	public int getBitPos() { 4 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_final = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSynchronized extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·synchronized"; }
	public int getBitPos() { 5 }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_synchronized = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVolatile extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·volatile"; }
	public int getBitPos() { 6 }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.mflags_is_fld_volatile = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaBridge extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·bridge"; }
	public int getBitPos() { 6 }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_bridge = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaTransient extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·transient"; }
	public int getBitPos() { 7 }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.mflags_is_fld_transient = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVarArgs extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·varargs"; }
	public int getBitPos() { 7 }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_varargs = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaNative extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·native"; }
	public int getBitPos() { 8 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_native = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaAbstract extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·abstract"; }
	public int getBitPos() { 10 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_abstract = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSynthetic extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·synthetic"; }
	public int getBitPos() { 12 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_synthetic = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaForward extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·forward"; }
	public int getBitPos() { 16 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_forward = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVirtual extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·virtual"; }
	public int getBitPos() { 17 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_virtual = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaUnerasable extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·unerasable"; }
	public int getBitPos() { 18 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_type_unerasable = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaMacro extends MetaFlag {
	public String qname() { return "kiev·stdlib·meta·macro"; }
	public int getBitPos() { 19 }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_macro = on; }
}

