package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;
import kiev.be.java15.JNode;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public final class MetaPacked extends UserMeta {
	@virtual typedef This  = MetaPacked;

	@att public int					size;
	@att public SymbolRef<Field>	fld;
	@att public int					offset;

	public MetaPacked() { super("kiev.stdlib.meta.packed"); fld = new SymbolRef<Field>(); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packed = on;
	}

	@setter public void set$size(int val) {
		this.open();
		size = val;
		super.setI("size",val);
	}
	
	@setter public void set$offset(int val) {
		this.open();
		offset = val;
		super.setI("offset", val);
	}
	
	@setter public void set$fld(SymbolRef<Field> val) {
		if (fld == null) {
			fld = val;
		}
		else if (val == null || val.name == null) {
			this.open();
			this.fld.open();
			fld.name = null;
			super.unset("in");
		}
		else {
			this.open();
			fld = val;
			super.setS("in", fld.name);
		}
	}
}

@node
public final class MetaPacker extends UserMeta {
	@virtual typedef This  = MetaPacker;

	@att public int		 size = -1;

	public MetaPacker() { super("kiev.stdlib.meta.packer"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packer = on;
	}

	@setter public void set$size(int val) {
		this.open();
		size = val;
		super.setI("size",val);
	}
}

@node
public final class MetaThrows extends UserMeta {
	@virtual typedef This  = MetaThrows;

	@ref public TypeRef[]		 exceptions;

	public MetaThrows() { super("kiev.stdlib.meta.throws"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.is_has_throws = on;
	}

	public void add(TypeRef thr) {
		this.open();
		exceptions += thr;
		if (values.length == 0)
			values += new MetaValueArray(new SymbolRef("value"));
		MetaValueArray mva = (MetaValueArray)values[0];
		assert (mva.ident.name == "value");
		mva.values += thr;
	}
	
	public TypeRef[] getThrowns() {
		return exceptions;
	}
}

@node
public abstract class MetaFlag extends ANode {
	@virtual typedef This  ≤ MetaFlag;

	public abstract String qname();

	public final void callbackAttached() { setFlag(getDNode(), true); super.callbackAttached(); }
	public final void callbackDetached() { setFlag(getDNode(), false); super.callbackDetached(); }
	private DNode getDNode() {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) return (DNode)p;
		return null;
	}
	abstract void setFlag(DNode dn, boolean on);
}

@node
public final class MetaAccess extends MetaFlag {
	@virtual typedef This  = MetaAccess;

	public String		simple;
	public int			flags;

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

	public String qname() { return "kiev.stdlib.meta.access"; }

	void setFlag(DNode dn, boolean on) {
		if (dn == null)
			return;
		if (on) {
			if (simple == "public")		{ dn.is_access = DNode.MASK_ACC_PUBLIC; return; }
			if (simple == "protected")	{ dn.is_access = DNode.MASK_ACC_PROTECTED; return; }
			if (simple == "private")	{ dn.is_access = DNode.MASK_ACC_PRIVATE; return; }
		}
		dn.is_access = DNode.MASK_ACC_DEFAULT;
	}
	
	@setter public void setSimple(String val) {
		this.simple = val.intern();
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode)
			setFlag((DNode)p, true);
	}

	@setter public void setFlags(int val) {
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
				return getFlags((DeclGroup)dn.parent());
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
		while( !pkg.isPackage() ) pkg = pkg.package_clazz;
		return (Struct)pkg;
	}

	private static void verifyAccess(ASTNode from, ASTNode n, int acc) {
		int flags = getFlags((DNode)n);

		// Quick check for public access
		if( ((flags>>>6) & acc) == acc ) return;

		// Check for private access
		if( getStructOf(from) == getStructOf(n) ) {
			if( (flags & acc) != acc ) throwAccessError(from,n,acc,"private");
			return;
		}

		// Check for private access from inner class
		if (((DNode)n).isPrivate()) {
			TypeDecl outer1 = getStructOf(from);
			TypeDecl outer2 = getStructOf(n);
			while (!outer1.package_clazz.isPackage())
				outer1 = outer1.package_clazz;
			while (!outer2.package_clazz.isPackage())
				outer2 = outer2.package_clazz;
			if (outer1 == outer2) {
				if( (flags & acc) == acc ) {
					n.setAccessedFromInner(true);
					return;
				}
				throwAccessError(from,n,acc,"private");
			}
		}

		// Check for default access
		if( getPackageOf(from) == getPackageOf(n) ) {
			if( ((flags>>>2) & acc) != acc ) throwAccessError(from,n,acc,"default");
			return;
		}

		// Check for protected access
		if( getStructOf(from).instanceOf(getStructOf(n)) ) {
			if( ((flags>>>4) & acc) != acc ) throwAccessError(from,n,acc,"protected");
			return;
		}

		// Public was already checked, just throw an error
		throwAccessError(from,n,acc,"public");
	}

	private static void throwAccessError(ASTNode from, ASTNode n, int acc, String astr) {
		StringBuffer sb = new StringBuffer();
		sb.append("Access denied - ").append(astr).append(' ');
		if( acc == 2 ) sb.append("read");
		else if( acc == 1 ) sb.append("write");
		else if( acc == 3 ) sb.append("read/write");
		sb.append("\n\tto ");
		if( n instanceof Field ) sb.append("field ");
		else if( n instanceof Method ) sb.append("method ");
		else if( n instanceof Struct ) sb.append("class ");
		if( n instanceof Struct ) sb.append(n);
		else sb.append(n.parent()).append('.').append(n);
		sb.append("\n\tfrom class ").append(getStructOf(from));
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}
}

@node
public final class MetaUnerasable extends MetaFlag {
	@virtual typedef This  = MetaUnerasable;
	public String qname() { return "kiev.stdlib.meta.unerasable"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_type_unerasable = on; }
}

@node
public final class MetaSingleton extends MetaFlag {
	@virtual typedef This  = MetaSingleton;
	public String qname() { return "kiev.stdlib.meta.singleton"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof TypeDecl) dn.is_struct_singleton = on; }
}

@node
public final class MetaForward extends MetaFlag {
	@virtual typedef This  = MetaForward;
	public String qname() { return "kiev.stdlib.meta.forward"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_forward = on; }
}

@node
public final class MetaVirtual extends MetaFlag {
	@virtual typedef This  = MetaVirtual;
	public String qname() { return "kiev.stdlib.meta.virtual"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_virtual = on; }
}

@node
public final class MetaMacro extends MetaFlag {
	@virtual typedef This  = MetaMacro;
	public String qname() { return "kiev.stdlib.meta.macro"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_macro = on; }
}

@node
public final class MetaStatic extends MetaFlag {
	@virtual typedef This  = MetaStatic;
	public String qname() { return "kiev.stdlib.meta.static"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_static = on; }
}

@node
public final class MetaAbstract extends MetaFlag {
	@virtual typedef This  = MetaAbstract;
	public String qname() { return "kiev.stdlib.meta.abstract"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_abstract = on; }
}

@node
public final class MetaFinal extends MetaFlag {
	@virtual typedef This  = MetaFinal;
	public String qname() { return "kiev.stdlib.meta.final"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_final = on; }
}

@node
public final class MetaNative extends MetaFlag {
	@virtual typedef This  = MetaNative;
	public String qname() { return "kiev.stdlib.meta.native"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_native = on; }
}

@node
public final class MetaSynchronized extends MetaFlag {
	@virtual typedef This  = MetaSynchronized;
	public String qname() { return "kiev.stdlib.meta.synchronized"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.is_mth_synchronized = on; }
}

@node
public final class MetaTransient extends MetaFlag {
	@virtual typedef This  = MetaTransient;
	public String qname() { return "kiev.stdlib.meta.transient"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.is_fld_transient = on; }
}

@node
public final class MetaVolatile extends MetaFlag {
	@virtual typedef This  = MetaVolatile;
	public String qname() { return "kiev.stdlib.meta.volatile"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Field) dn.is_fld_volatile = on; }
}

@node
public final class MetaBridge extends MetaFlag {
	@virtual typedef This  = MetaBridge;
	public String qname() { return "kiev.stdlib.meta.bridge"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.is_mth_bridge = on; }
}

@node
public final class MetaVarArgs extends MetaFlag {
	@virtual typedef This  = MetaVarArgs;
	public String qname() { return "kiev.stdlib.meta.varargs"; }
	void setFlag(DNode dn, boolean on) { if (dn instanceof Method) dn.is_mth_varargs = on; }
}

@node
public final class MetaSynthetic extends MetaFlag {
	@virtual typedef This  = MetaSynthetic;
	public String qname() { return "kiev.stdlib.meta.synthetic"; }
	void setFlag(DNode dn, boolean on) { if (dn != null) dn.is_synthetic = on; }
}

