package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public final class MetaPacked extends UserMeta {
	@virtual typedef This  = MetaPacked;

	@att public int		size;
	@att public int		offset;
	@ref public Field	fld;

	public MetaPacked() { super("kiev.stdlib.meta.packed"); }

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
	
	@setter public void set$fld(Field val) {
		this.open();
		fld = val;
		if (val != null)
			super.setS("in", val.id.sname);
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
public final class MetaAlias extends UserMeta {
	@virtual typedef This  = MetaAlias;

	@ref public ENode[]			 aliases;

	public MetaAlias() { super("kiev.stdlib.meta.alias"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_has_aliases = on;
	}

	public void add(ConstStringExpr cse) {
		this.open();
		aliases += cse;
		if (values.length == 0)
			values += new MetaValueArray(new SymbolRef("value"));
		MetaValueArray mva = (MetaValueArray)values[0];
		assert (mva.ident.name == "value");
		mva.values += cse;
	}
	
	public ENode[] getAliases() {
		return aliases;
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
public final class MetaPizzaCase extends UserMeta {
	@virtual typedef This  = MetaPizzaCase;

	@ref public Field[]			 fields;
	@att public int				 tag;

	public MetaPizzaCase() { super("kiev.stdlib.meta.pcase"); }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof TypeDecl) p.is_struct_pizza_case = on;
	}

	public void add(Field f) {
		this.open();
		fields += f;
		MetaValueArray mva;
		if (values.length > 0 && values[0] instanceof MetaValueArray)
			mva = values[0];
		else if (values.length > 1 && values[1] instanceof MetaValueArray)
			mva = values[1];
		else
			values += (mva = new MetaValueArray(new SymbolRef("fields")));
		assert (mva.ident.name == "fields");
		mva.values += new ConstStringExpr(f.id.sname);
	}
	
	public Field[] getFields() {
		return fields;
	}

	@setter public void set$tag(int val) {
		this.open();
		this.tag = val;
		super.setI("tag",val);
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

	public static enum AccessValue {
		Public					: "public",
		Protected				: "protected",
		Default					: "default",
		Private					: "private"
	}
	
	public final String		value;
	
	public MetaAccess() { value = "default"; }
	public MetaAccess(AccessValue av) { value = av.toString(); }

	public String qname() { return "kiev.stdlib.meta.access"; }

	void setFlag(DNode dn, boolean on) {
		if (dn == null)
			return;
		if (on) {
			if (value == "public")		{ dn.is_access = DNode.MASK_ACC_PUBLIC; return; }
			if (value == "protected")	{ dn.is_access = DNode.MASK_ACC_PROTECTED; return; }
			if (value == "private")		{ dn.is_access = DNode.MASK_ACC_PRIVATE; return; }
		}
		dn.is_access = DNode.MASK_ACC_DEFAULT;
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

