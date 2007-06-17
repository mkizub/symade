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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public final class MetaUUID extends UserMeta {
	@virtual typedef This  = MetaUUID;

	private static final Hashtable<String,DNode> registeredNodes = new Hashtable<String,DNode>();
	
	@att public String				value;

	public MetaUUID() { super("kiev\u001fstdlib\u001fmeta\u001fuuid"); }

	public MetaUUID(boolean autogen) {
		this();
		if (autogen)
			value = java.util.UUID.randomUUID().toString();
	}

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		MetaSet ms = (MetaSet)parent();
		ANode p = null;
		if (ms != null)
			p = ms.parent();
		if (p instanceof DNode) {
			if (on) {
				p.meta.is_has_uuid = true;
				if (value != null)
					registeredNodes.put(value,(DNode)p);
			} else {
				p.meta.is_has_uuid = false;
				if (value != null)
					registeredNodes.remove(value);
			}
		}
	}

	@setter public void set$value(String val) {
		MetaSet ms = (MetaSet)parent();
		ANode p = null;
		if (ms != null)
			p = ms.parent();
		if (p instanceof DNode) {
			if (value != null)
				registeredNodes.remove(value);
		}
		this = this.open();
		if (val != null)
			val = val.intern();
		value = val;
		super.setS("value",val);
		if (p instanceof DNode) {
			if (value != null)
				registeredNodes.put(value, (DNode)p);
		}
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "this")
			return true;
		if (dump == "api" && attr.name == "values")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public static DNode getRegisteredNode(String uuid) {
		return registeredNodes.get(uuid);
	}
}

@node
public final class MetaPacked extends UserMeta {
	@virtual typedef This  = MetaPacked;

	@att public int					size;
	@att public SymbolRef<Field>	fld;
	@att public int					offset;

	public MetaPacked() { super("kiev\u001fstdlib\u001fmeta\u001fpacked"); fld = new SymbolRef<Field>(); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packed = on;
	}

	@setter public void set$size(int val) {
		this = this.open();
		size = val;
		super.setI("size",val);
	}
	
	@setter public void set$offset(int val) {
		this = this.open();
		offset = val;
		super.setI("offset", val);
	}
	
	@setter public void set$fld(SymbolRef<Field> val) {
		if (fld == null) {
			fld = val;
		}
		else if (val == null || val.name == null) {
			this = this.open();
			this.fld.open();
			fld.name = null;
			super.unset("in");
		}
		else {
			this = this.open();
			fld = val;
			super.setS("in", fld.name);
		}
	}
}

@node
public final class MetaPacker extends UserMeta {
	@virtual typedef This  = MetaPacker;

	@att public int		 size = -1;

	public MetaPacker() { super("kiev\u001fstdlib\u001fmeta\u001fpacker"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packer = on;
	}

	@setter public void set$size(int val) {
		this = this.open();
		size = val;
		super.setI("size",val);
	}
}

@node
public final class MetaThrows extends UserMeta {
	@virtual typedef This  = MetaThrows;

	@ref @abstract public ASTNode[]		 exceptions;

	public MetaThrows() { super("kiev\u001fstdlib\u001fmeta\u001fthrows"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.meta.is_has_throws = on;
	}

	public void add(TypeRef thr) {
		this = this.open();
		exceptions += thr;
		if (values.length == 0)
			values += new MetaValueArray(new SymbolRef("value"));
		MetaValueArray mva = (MetaValueArray)values[0];
		assert (mva.ident == "value");
		mva.values += thr;
	}
	
	@getter @ref
	public ASTNode[] get$exceptions() {
		return getThrowns();
	}
	@setter @ref
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

@node
public abstract class MetaFlag extends MNode {
	@virtual typedef This  ≤ MetaFlag;

	@getter public abstract String get$qname();

	public final TypeDecl getTypeDecl() { return (TypeDecl)Env.resolveGlobalDNode(this.qname); }

	public final void callbackAttached() { setFlag(getMetaSet(), true); super.callbackAttached(); }
	public final void callbackDetached() { setFlag(getMetaSet(), false); super.callbackDetached(); }
	private MetaSet getMetaSet() {
		ANode p = parent();
		if (p instanceof MetaSet) return (MetaSet)p;
		return null;
	}
	abstract void setFlag(MetaSet dn, boolean on);
	
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return (o.getClass() == this.getClass());
	}
}

@node
public final class MetaAccess extends MetaFlag {
	@virtual typedef This  = MetaAccess;

	@att public String		simple;
	@att public int			flags;

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

	void setFlag(MetaSet dn, boolean on) {
		if (dn == null)
			return;
		if (on) {
			if (simple == "public")		{ dn.is_access = DNode.MASK_ACC_PUBLIC; return; }
			if (simple == "protected")	{ dn.is_access = DNode.MASK_ACC_PROTECTED; return; }
			if (simple == "private")	{ dn.is_access = DNode.MASK_ACC_PRIVATE; return; }
		}
		dn.is_access = DNode.MASK_ACC_DEFAULT;
	}
	
	public final void setSimple(String val) {
		this = this.open();
		this.simple = val;
	}
	@setter public final void set$simple(String val) {
		this.simple = val.intern();
		ANode p = parent();
		if (p instanceof MetaSet)
			setFlag((MetaSet)p, true);
	}

	public final void setFlags(int val) {
		this = this.open();
		this.flags = val;
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
		if (acc == null) {
			if (dn.parent() instanceof DeclGroup)
				acc = ((DeclGroup)dn.parent()).getMetaAccess();
			if (acc == null)
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
		MetaAccess acc = dn.getMetaAccess();
		if (acc != null)
			acc.verifyAccessDecl(dn);
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
	public static void verifyRead(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,2); }
	public static void verifyWrite(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,1); }
	public static void verifyReadWrite(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,3); }

	private static TypeDecl getStructOf(ASTNode n) {
		if( n instanceof TypeDecl ) return (TypeDecl)n;
		return n.ctx_tdecl;
	}

	private static Struct getPackageOf(ASTNode n) {
		TypeDecl pkg = getStructOf(n);
		while( !pkg.isPackage() ) pkg = pkg.package_clazz.dnode;
		return (Struct)pkg;
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
			TypeDecl outer1 = getStructOf(from);
			TypeDecl outer2 = getStructOf(n);
			while (!outer1.package_clazz.dnode.isPackage())
				outer1 = outer1.package_clazz.dnode;
			while (!outer2.package_clazz.dnode.isPackage())
				outer2 = outer2.package_clazz.dnode;
			if (outer1 == outer2) {
				if( (flags & acc) == acc ) {
					n.setAccessedFromInner(true);
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

@node
public final class MetaUnerasable extends MetaFlag {
	@virtual typedef This  = MetaUnerasable;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001funerasable"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_type_unerasable = on; }
}

@node
public final class MetaSingleton extends MetaFlag {
	@virtual typedef This  = MetaSingleton;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsingleton"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_struct_singleton = on; }
}

@node
public final class MetaForward extends MetaFlag {
	@virtual typedef This  = MetaForward;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fforward"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_forward = on; }
}

@node
public final class MetaVirtual extends MetaFlag {
	@virtual typedef This  = MetaVirtual;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvirtual"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_virtual = on; }
}

@node
public final class MetaMacro extends MetaFlag {
	@virtual typedef This  = MetaMacro;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fmacro"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_macro = on; }
}

@node
public final class MetaStatic extends MetaFlag {
	@virtual typedef This  = MetaStatic;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fstatic"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_static = on; }
}

@node
public final class MetaAbstract extends MetaFlag {
	@virtual typedef This  = MetaAbstract;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fabstract"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_abstract = on; }
}

@node
public final class MetaFinal extends MetaFlag {
	@virtual typedef This  = MetaFinal;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001ffinal"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_final = on; }
}

@node
public final class MetaNative extends MetaFlag {
	@virtual typedef This  = MetaNative;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fnative"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_native = on; }
}

@node
public final class MetaSynchronized extends MetaFlag {
	@virtual typedef This  = MetaSynchronized;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsynchronized"; }
	void setFlag(MetaSet dn, boolean on) { if (dn.parent() instanceof Method) dn.is_mth_synchronized = on; }
}

@node
public final class MetaTransient extends MetaFlag {
	@virtual typedef This  = MetaTransient;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001ftransient"; }
	void setFlag(MetaSet dn, boolean on) { if (dn.parent() instanceof Field) dn.is_fld_transient = on; }
}

@node
public final class MetaVolatile extends MetaFlag {
	@virtual typedef This  = MetaVolatile;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvolatile"; }
	void setFlag(MetaSet dn, boolean on) { if (dn.parent() instanceof Field) dn.is_fld_volatile = on; }
}

@node
public final class MetaBridge extends MetaFlag {
	@virtual typedef This  = MetaBridge;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fbridge"; }
	void setFlag(MetaSet dn, boolean on) { if (dn.parent() instanceof Method) dn.is_mth_bridge = on; }
}

@node
public final class MetaVarArgs extends MetaFlag {
	@virtual typedef This  = MetaVarArgs;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fvarargs"; }
	void setFlag(MetaSet dn, boolean on) { if (dn.parent() instanceof Method) dn.is_mth_varargs = on; }
}

@node
public final class MetaSynthetic extends MetaFlag {
	@virtual typedef This  = MetaSynthetic;
	@getter public String get$qname() { return "kiev\u001fstdlib\u001fmeta\u001fsynthetic"; }
	void setFlag(MetaSet dn, boolean on) { if (dn != null) dn.is_synthetic = on; }
}

