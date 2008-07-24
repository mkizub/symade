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

	public MetaUUID() { super("kiev\u001fstdlib\u001fmeta\u001fuuid"); }

	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			setFlag(true);
		}
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			setFlag(false);
		}
		super.callbackDetached(parent, slot);
	}
	private void setFlag(boolean on) {
		ANode p = parent();
		if (p instanceof DNode) {
			DNode dn = (DNode)p;
			if (on) {
				if (dn.uuid != null)
					this.value = dn.uuid;
				else if (this.value != null)
					dn.uuid = this.value;
			}
		}
	}

	@setter public void set$value(String val) {
		if (val != null)
			val = val.intern();
		ANode p = parent();
		if (p instanceof DNode) {
			DNode dn = (DNode)p;
			if (dn.uuid != null)
				return;
			dn.uuid = val;
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

	public MetaPacked() { super("kiev\u001fstdlib\u001fmeta\u001fpacked"); fld = new SymbolRef<Field>(); }

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

	public MetaPacker() { super("kiev\u001fstdlib\u001fmeta\u001fpacker"); }

	@setter public void set$size(int val) {
		size = val;
		super.setI("size",val);
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaThrows extends UserMeta {
	@nodeData @abstract public ASTNode∅		 exceptions;

	public MetaThrows() { super("kiev\u001fstdlib\u001fmeta\u001fthrows"); }

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
public abstract class MetaFlag extends MNode {
	@getter public abstract String get$qname();

	public final JavaAnnotation getAnnotationDecl() { return (JavaAnnotation)Env.getRoot().resolveGlobalDNode(this.qname); }

	public final void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic()) {
			setFlag(getDNode(), true);
		}
		super.callbackAttached(pi);
	}
	public final void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic()) {
			setFlag(getDNode(), false);
		}
		super.callbackDetached(parent, slot);
	}
	private DNode getDNode() {
		ANode p = parent();
		if (p instanceof DNode) return (DNode)p;
		return null;
	}
	abstract void setFlag(DNode dn, boolean on);
	
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return (o.getClass() == this.getClass());
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaAccess extends MetaFlag {
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String			simple;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public int			flags;

	public @packed:1,flags,7 boolean	r_public;
	public @packed:1,flags,6 boolean	w_public;
	public @packed:1,flags,5 boolean	r_protected;
	public @packed:1,flags,4 boolean	w_protected;
	public @packed:1,flags,3 boolean	r_default;
	public @packed:1,flags,2 boolean	w_default;
	public @packed:1,flags,1 boolean	r_private;
	public @packed:1,flags,0 boolean	w_private;

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

	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001faccess"; }

	public boolean equals(Object o) {
		if (o instanceof MetaAccess)
			return this.simple == o.simple && this.flags == o.flags;
		return false;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "flags")
			return this.flags != -1;
		return super.includeInDump(dump, attr, val);
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
	
	public final void setSimple(String val) {
		this.simple = val;
	}
	public final String getSimple() {
		this.simple
	}
	@setter public final void set$simple(String val) {
		this.simple = val.intern();
		ANode p = parent();
		if (p instanceof DNode)
			setFlag((DNode)p, true);
	}

	public final void setFlags(int val) {
		this.flags = val;
	}
	public int getFlags() {
		this.flags
	}
	@setter public void set$flags(int val) {
		this.flags = val;
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
		if (acc == null)
			return 0x0F;
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
			outer1 = outer1.ctx_tdecl;
		while !(outer2.parent() instanceof KievPackage)
			outer2 = outer2.ctx_tdecl;
		if (outer1 == outer2)
			return true;
		return false;
	}

	private static ComplexTypeDecl getStructOf(ASTNode n) {
		if (n instanceof ComplexTypeDecl) return (ComplexTypeDecl)n;
		return n.ctx_tdecl;
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
				outer1 = outer1.ctx_tdecl;
			while !(outer2.parent() instanceof KievPackage)
				outer2 = outer2.ctx_tdecl;
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
			Method m = from.ctx_method;
			if (m instanceof Constructor && m.ctx_tdecl == n.ctx_tdecl)
				return;
			if (m == null && from.ctx_tdecl == n.ctx_tdecl)
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
		else sb.append(n.ctx_tdecl).append('.').append(n);
		sb.append("\n\tfrom class ").append(getStructOf(from));
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}

	private static void throwFinalWriteError(ASTNode from, ASTNode n) {
		StringBuffer sb = new StringBuffer();
		sb.append("Write for final value is denied to ");
		if (n instanceof Field)
			sb.append("field ").append(n.ctx_tdecl).append('.').append(n);
		else if (n instanceof Var)
			sb.append("var ").append(n);
		else
			sb.append(n);
		Method m = from.ctx_method;
		if (m != null)
			sb.append("\n\tin method ").append(m.ctx_tdecl).append('.').append(m);
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}
}

@ThisIsANode(lang=CoreLang)
public final class MetaUnerasable extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001funerasable"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_type_unerasable = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSingleton extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsingleton"; }
	void setFlag(DNode dn, boolean on) {}
}

@ThisIsANode(lang=CoreLang)
public final class MetaMixin extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fmixin"; }
	void setFlag(DNode dn, boolean on) {}
}

@ThisIsANode(lang=CoreLang)
public final class MetaForward extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fforward"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_forward = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVirtual extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvirtual"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_virtual = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaMacro extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fmacro"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_macro = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaStatic extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fstatic"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_static = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaAbstract extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fabstract"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_abstract = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaFinal extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001ffinal"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_final = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaNative extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fnative"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_native = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSynchronized extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsynchronized"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_synchronized = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaTransient extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001ftransient"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.mflags_is_fld_transient = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVolatile extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvolatile"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.mflags_is_fld_volatile = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaBridge extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fbridge"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_bridge = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaVarArgs extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvarargs"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.mflags_is_mth_varargs = on; }
}

@ThisIsANode(lang=CoreLang)
public final class MetaSynthetic extends MetaFlag {
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsynthetic"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.mflags_is_synthetic = on; }
}

