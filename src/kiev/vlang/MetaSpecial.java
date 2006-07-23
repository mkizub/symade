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
public abstract class MetaSpecial extends Meta {
	
	@virtual typedef This  ≤ MetaSpecial;

	public TypeDecl getTypeDecl() {
		return Env.loadStruct(qname(),true);
	}

	public MetaValue get(String name) {
		throw new RuntimeException("Value "+name+" not found in "+qname()+" annotation");
	}

	public void verify() {}

	public void resolve(Type reqType) {
		getTypeDecl().checkResolved();
	}
}

@node
public final class MetaPacked extends MetaSpecial {
	@virtual typedef This  = MetaPacked;

	@att public ENode				size;
	@att public ENode				offset;
	@att public SymbolRef<Field>	fld = new SymbolRef<Field>();

	public String qname() { return "kiev.stdlib.meta.packed"; }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packed = on;
	}

	public MetaValue get(String name) {
		if (name == "size")
			return new MetaValueScalar(new SymbolRef("size"),size.ncopy());
		if (name == "offset")
			return new MetaValueScalar(new SymbolRef("offset"),offset.ncopy());
		if (name == "in")
			return new MetaValueScalar(new SymbolRef("in"),new ConstStringExpr(fld.symbol.id.uname));
		return super.get(name);
	}
	
	public int getSize() {
		ENode size = this.size;
		if (size instanceof ConstIntExpr)
			return size.value;
		return 0;
	}
	public void setSize(int val) {
		size = new ConstIntExpr(val);
	}
	
	public int getOffset() {
		ENode offset = this.offset;
		if (offset instanceof ConstIntExpr)
			return offset.value;
		return 0;
	}
	public void setOffset(int val) {
		offset = new ConstIntExpr(val);
	}
	
	public String getFld() {
		if (fld.name != null)
			return fld.name;
		return "";
	}
	public void setFld(String val) {
		fld.name = val;
	}
}

@node
public final class MetaPacker extends MetaSpecial {
	@virtual typedef This  = MetaPacker;

	@att public ENode			 size;

	public String qname() { return "kiev.stdlib.meta.packer"; }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_packer = on;
	}

	public MetaValue get(String name) {
		if (name == "size")
			return new MetaValueScalar(new SymbolRef("size"),size.ncopy());
		return super.get(name);
	}
	
	public int getSize() {
		ENode size = this.size;
		if (size instanceof ConstIntExpr)
			return size.value;
		return 0;
	}
	public void setSize(int val) {
		size = new ConstIntExpr(val);
	}
}

@node
public final class MetaAlias extends MetaSpecial {
	@virtual typedef This  = MetaAlias;

	@att public ENode[]			 aliases;

	public String qname() { return "kiev.stdlib.meta.alias"; }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_has_aliases = on;
	}

	public MetaValue get(String name) {
		if (name == "value") {
			ENode[] values = new ENode[aliases.length];
			for (int i=0; i < values.length; i++)
				values[i] = aliases[i].ncopy();
			return new MetaValueArray(new SymbolRef("value"),values);
		}
		return super.get(name);
	}

	public ENode[] getAliases() {
		return aliases;
	}
}

@node
public final class MetaThrows extends MetaSpecial {
	@virtual typedef This  = MetaThrows;

	@att public TypeRef[]		 exceptions;

	public String qname() { return "kiev.stdlib.meta.throws"; }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.is_has_throws = on;
	}

	public MetaValue get(String name) {
		if (name == "value") {
			ENode[] values = new ENode[exceptions.length];
			for (int i=0; i < values.length; i++)
				values[i] = exceptions[i].ncopy();
			return new MetaValueArray(new SymbolRef("value"),values);
		}
		return super.get(name);
	}
	
	public void add(TypeRef thr) {
		exceptions += thr;
	}
	
	public TypeRef[] getThrowns() {
		return exceptions;
	}
}

@node
public final class MetaPizzaCase extends MetaSpecial {
	@virtual typedef This  = MetaPizzaCase;

	@ref public Field[]			 fields;
	@att public int				 tag;

	public String qname() { return "kiev.stdlib.meta.pcase"; }

	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof TypeDecl) p.is_struct_pizza_case = on;
	}

	public MetaValue get(String name) {
		if (name == "fields") {
			ENode[] values = new ENode[fields.length];
			for (int i=0; i < values.length; i++)
				values[i] = new ConstStringExpr(fields[i].id.uname);
			return new MetaValueArray(new SymbolRef("fields"),values);
		}
		if (name == "tag")
			return new MetaValueScalar(new SymbolRef("tag"),new ConstIntExpr(tag));
		return super.get(name);
	}
	
	public void add(Field f) {
		fields += f;
	}
	
	public Field[] getFields() {
		return fields;
	}
	
	public int getTag() { return this.tag; }
	public void setTag(int tag) { this.tag = tag; }
}

@node
public final class MetaAccess extends MetaSpecial {
	@virtual typedef This  = MetaAccess;

	public static enum AccessValue {
		Public					: "public",
		Protected				: "protected",
		Default					: "default",
		Private					: "private"
	}
	
	@att public AccessValue			 value;
	
	public MetaAccess() { value = AccessValue.Default; }
	public MetaAccess(AccessValue av) { value = av; }
	public MetaAccess(int pos, AccessValue av) { this.pos = pos; value = av; }

	public String qname() { return "kiev.stdlib.meta.access"; }

	public void callbackAttached() { setAccess(this.value); super.callbackAttached(); }
	public void callbackDetached() { setAccess(AccessValue.Default); super.callbackDetached(); }

	public void callbackChildChanged(AttrSlot attr) {
		if (attr.name == "value")
			setAccess(this.value);
	}

	public MetaValue get(String name) {
		if (name == "value")
			return new MetaValueScalar(new SymbolRef("value"),new ConstStringExpr(value.toString()));
		return super.get(name);
	}
	
	private void setAccess(AccessValue value) {
		ANode p = parent();
		if (p instanceof MetaSet) {
			p = p.parent();
			if (p instanceof DNode) {
				switch (value) {
				case AccessValue.Public:	p.is_access = DNode.MASK_ACC_PUBLIC; break;
				case AccessValue.Protected:	p.is_access = DNode.MASK_ACC_PROTECTED; break;
				case AccessValue.Private:	p.is_access = DNode.MASK_ACC_PRIVATE; break;
				default:
				case AccessValue.Default:	p.is_access = DNode.MASK_ACC_DEFAULT; break;
				}
			}
		}
	}
}

@node
public abstract class MetaFlag extends MetaSpecial {
	@virtual typedef This  ≤ MetaFlag;
}

@node
public final class MetaUnerasable extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.unerasable"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_type_unerasable = on;
	}
}

@node
public final class MetaSingleton extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.singleton"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof TypeDecl) p.is_struct_singleton = on;
	}
}

@node
public final class MetaForward extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.forward"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_forward = on;
	}
}

@node
public final class MetaVirtual extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.virtual"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_virtual = on;
	}
}

@node
public final class MetaMacro extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.macro"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_macro = on;
	}
}

@node
public final class MetaStatic extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.static"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_static = on;
	}
}

@node
public final class MetaAbstract extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.abstract"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_abstract = on;
	}
}

@node
public final class MetaFinal extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.final"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_final = on;
	}
}

@node
public final class MetaNative extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.native"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_native = on;
	}
}

@node
public final class MetaSynchronized extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.synchronized"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.is_mth_synchronized = on;
	}
}

@node
public final class MetaTransient extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.transient"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_transient = on;
	}
}

@node
public final class MetaVolatile extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.volatile"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Field) p.is_fld_volatile = on;
	}
}

@node
public final class MetaBridge extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.bridge"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.is_mth_bridge = on;
	}
}

@node
public final class MetaVarArgs extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.varargs"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof Method) p.is_mth_varargs = on;
	}
}

@node
public final class MetaSynthetic extends MetaFlag {
	public String qname() { return "kiev.stdlib.meta.synthetic"; }
	public void callbackAttached() { setFlag(true); super.callbackAttached(); }
	public void callbackDetached() { setFlag(false); super.callbackDetached(); }
	private void setFlag(boolean on) {
		ANode p = ((MetaSet)parent()).parent();
		if (p instanceof DNode) p.is_synthetic = on;
	}
}

