package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.TypeNameRef;
import kiev.parser.ASTIdentifier;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public abstract class MetaSpecial extends ASTNode {
	
	public static final MetaSpecial[] emptyArray = new MetaSpecial[0];
	
	public final MetaAttrSlot attr;
	
	@virtual typedef This  = MetaSpecial;
	@virtual typedef VView = VMetaSpecial;

	@nodeview
	public static view VMetaSpecial of MetaSpecial extends NodeView {}

	public MetaSpecial(MetaAttrSlot attr) {
		this.attr = attr;
	}
	
	public ANode nodeCopiedTo(ANode node) {
		return this.ncopy();
	}
	
	public void attachTo(ASTNode node) { node.addNodeData(this, attr); }
	public void detachFrom(ASTNode node) { node.delNodeData(attr); }
}

@node
public class MetaPacked extends MetaSpecial {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.packed", MetaPacked.class);

	@virtual typedef This  = MetaPacked;
	@virtual typedef VView = VMetaPacked;

	@att public ENode			 size;
	@att public ENode			 offset;
	@att public SymbolRef		 fld;
	@ref public Field			 packer;

	@nodeview
	public static view VMetaPacked of MetaPacked extends VMetaSpecial {
		public ENode			 size;
		public ENode			 offset;
		public SymbolRef		 fld;
		public Field			 packer;
	}

	public MetaPacked() { super(ATTR); }

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
		SymbolRef fld = this.fld;
		if (fld != null)
			return fld.name;
		return "";
	}
	public void setFld(String val) {
		fld = new SymbolRef(val);
	}
}

@node
public class MetaPacker extends MetaSpecial {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.packer", MetaPacker.class);

	@virtual typedef This  = MetaPacker;
	@virtual typedef VView = VMetaPacker;

	@att public ENode			 size;

	@nodeview
	public static view VMetaPacker of MetaPacker extends VMetaSpecial {
		public ENode			 size;
	}

	public MetaPacker() { super(ATTR); }

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
public class MetaAlias extends MetaSpecial {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.alias", MetaAlias.class);

	@virtual typedef This  = MetaAlias;
	@virtual typedef VView = VMetaAlias;

	@att public NArr<ENode>		 aliases;

	@nodeview
	public static view VMetaAlias of MetaAlias extends VMetaSpecial {
		public:ro NArr<ENode>		 aliases;
	}

	public MetaAlias() {
		super(ATTR);
	}

	public MetaAlias(ConstStringExpr name) {
		super(ATTR);
		this.aliases.append(name);
	}

	public ENode[] getAliases() {
		return aliases.getArray();
	}
}

@node
public class MetaThrows extends MetaSpecial {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.throws", MetaThrows.class);

	@virtual typedef This  = MetaThrows;
	@virtual typedef VView = VMetaThrows;

	@att public NArr<TypeNameRef>		 exceptions;

	@nodeview
	public static view VMetaThrows of MetaThrows extends VMetaSpecial {
		public:ro NArr<TypeNameRef>		 exceptions;
	}

	public MetaThrows() {
		super(ATTR);
	}

	public void add(TypeNameRef thr) {
		exceptions += thr;
	}
	
	public TypeNameRef[] getThrowns() {
		return exceptions.getArray();
	}
}

@node
public class MetaPizzaCase extends MetaSpecial {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.pcase", MetaPizzaCase.class);

	@virtual typedef This  = MetaPizzaCase;
	@virtual typedef VView = VMetaPizzaCase;

	@ref public NArr<Field>		 fields;
	@att public int				 tag;

	@nodeview
	public static view VMetaPizzaCase of MetaPizzaCase extends VMetaSpecial {
		public:ro NArr<Field>	fields;
		public int				tag;
	}

	public MetaPizzaCase() {
		super(ATTR);
	}

	public void add(Field f) {
		fields += f;
	}
	
	public Field[] getFields() {
		return fields.getArray();
	}
	
	public int getTag() { return this.tag; }
	public void setTag(int tag) { this.tag = tag; }
}

@node
public abstract class MetaFlag extends MetaSpecial {

	public final void attachTo(ASTNode node) { this.set(node, Boolean.TRUE); }
	public final void detachFrom(ASTNode node) { this.set(node, Boolean.FALSE); }
	public abstract void setZ(ASTNode node, boolean val);
	public abstract boolean getZ(ASTNode node);
	
	public MetaFlag(MetaAttrSlot attr) { super(attr); }
	public Object copy() { return this; }
	
	public ANode nodeCopiedTo(ANode node) {
		return this; // attach the same instance to the copied node
	}
	public final void set(ASTNode node, Object value) {
		try {
			this.setZ(node, ((Boolean)value).booleanValue());
		} catch (ClassCastException e) {
			if (((Boolean)value).booleanValue())
				node.addNodeData(this, this.attr);
			else
				node.delNodeData(this.attr);
		}
	}
	public final Object get(ASTNode node) {
		try {
			return Boolean.valueOf(this.getZ(node));
		} catch (ClassCastException e) {
			return Boolean.valueOf(node.getNodeData(this.attr) != null);
		}
	}
}

@singleton
@node
public class MetaUnerasable extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.unerasable", MetaUnerasable.class);

	private MetaUnerasable() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setTypeUnerasable(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isTypeUnerasable(); }
}

@singleton
@node
public final class MetaSingleton extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.singleton", MetaSingleton.class);

	private MetaSingleton() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Struct)node).setSingleton(val); }
	public boolean getZ(ASTNode node)					{ return ((Struct)node).isSingleton(); }
}

@singleton
@node
public final class MetaForward extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.forward", MetaForward.class);

	private MetaForward() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setForward(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isForward(); }
}

@singleton
@node
public final class MetaVirtual extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.virtual", MetaVirtual.class);

	public MetaVirtual() { super(ATTR); }

	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setVirtual(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isVirtual(); }
}

@singleton
@node
public final class MetaMacro extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.macro", MetaMacro.class);

	public MetaMacro() { super(ATTR); }

	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setMacro(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isMacro(); }
}

@singleton
@node
public final class MetaPublic extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.public", MetaPublic.class);

	private MetaPublic() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setPublic(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isPublic(); }
}

@singleton
@node
public final class MetaProtected extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.protected", MetaProtected.class);

	private MetaProtected() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setProtected(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isProtected(); }
}

@singleton
@node
public final class MetaPrivate extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.private", MetaPrivate.class);

	private MetaPrivate() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setPrivate(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isPrivate(); }
}

@singleton
@node
public final class MetaStatic extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.static", MetaStatic.class);

	private MetaStatic() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setStatic(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isStatic(); }
}

@singleton
@node
public final class MetaAbstract extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.abstract", MetaAbstract.class);

	private MetaAbstract() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setAbstract(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isAbstract(); }
}

@singleton
@node
public final class MetaFinal extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.final", MetaFinal.class);

	private MetaFinal() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setFinal(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isFinal(); }
}

@singleton
@node
public final class MetaNative extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.native", MetaNative.class);

	private MetaNative() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Method)node).setMethodNative(val); }
	public boolean getZ(ASTNode node)					{ return ((Method)node).isMethodNative(); }
}

@singleton
@node
public final class MetaSynchronized extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.synchronized", MetaSynchronized.class);

	private MetaSynchronized() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Method)node).setSynchronized(val); }
	public boolean getZ(ASTNode node)					{ return ((Method)node).isSynchronized(); }
}

@singleton
@node
public final class MetaTransient extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.transient", MetaTransient.class);

	private MetaTransient() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Field)node).setFieldTransient(val); }
	public boolean getZ(ASTNode node)					{ return ((Field)node).isFieldTransient(); }
}

@singleton
@node
public final class MetaVolatile extends MetaFlag {
	public static final MetaAttrSlot ATTR = new MetaAttrSlot("kiev.stdlib.meta.volatile", MetaVolatile.class);

	private MetaVolatile() { super(ATTR); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setVolatile(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isVolatile(); }
}

