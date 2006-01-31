package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.TypeNameRef;
import kiev.parser.ASTIdentifier;
import kiev.vlang.ASTNode.NodeImpl;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public abstract class MetaSpecial extends ASTNode implements NodeData {
	
	public static final MetaSpecial[] emptyArray = new MetaSpecial[0];
	
	public final MetaAttrSlot attr;
	
	@virtual typedef This  = MetaSpecial;
	@virtual typedef NImpl = MetaSpecialImpl;
	@virtual typedef VView = MetaSpecialView;

	@nodeimpl
	public static class MetaSpecialImpl extends NodeImpl {
		@virtual typedef ImplOf = MetaSpecial;
	}
	@nodeview
	public static view MetaSpecialView of NodeImpl extends NodeView {}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaSpecial(MetaSpecialImpl impl, MetaAttrSlot attr) {
		super(impl);
		this.attr = attr;
	}
	
	public final KString getNodeDataId() {
		return attr.id;
	}
	
	public NodeData nodeCopiedTo(NodeImpl node) {
		return this.ncopy();
	}
	public void nodeAttached(NodeImpl node) {}
	public void dataAttached(NodeImpl node) { this.callbackAttached(node.getNode(), attr); }
	public void nodeDetached(NodeImpl node) {}
	public void dataDetached(NodeImpl node) { this.callbackDetached(); }
	
	public void attach(ASTNode node) { node.addNodeData(this); }
	public void detach(ASTNode node) { node.addNodeData(this); }

	public final void walkTree(TreeWalker walker) {
		theView.walkTree(walker);
	}
}

@nodeset
public final class MetaVirtual extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.virtual");
	public static final MetaAttrSlot MetaVirtualAttr = new MetaAttrSlot(ID, MetaVirtual.class);

	@virtual typedef This  = MetaVirtual;
	@virtual typedef NImpl = MetaVirtualImpl;
	@virtual typedef VView = MetaVirtualView;

	@nodeimpl
	public static final class MetaVirtualImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaVirtual;
		/** Getter/setter methods for this field */
		@ref public Method		get;
		@ref public Method		set;
	}
	@nodeview
	public static view MetaVirtualView of MetaVirtualImpl extends MetaSpecialView {
		public Method		get;
		public Method		set;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaVirtual() { super(new MetaVirtualImpl(), MetaVirtualAttr); }

	public void dataAttached(NodeImpl node) {
		super.dataAttached(node);
		if (node instanceof DNode.DNodeImpl) {
			((DNode)node.getNode()).setVirtual(true);
		}
	}
	public void dataDetached(NodeImpl node) {
		super.dataDetached(node);
		if (node instanceof DNode.DNodeImpl)
			((DNode)node.getNode()).setVirtual(false);
	}
}

@nodeset
public class MetaPacked extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.packed");
	public static final MetaAttrSlot MetaPackedAttr = new MetaAttrSlot(ID, MetaPacked.class);

	@virtual typedef This  = MetaPacked;
	@virtual typedef NImpl = MetaPackedImpl;
	@virtual typedef VView = MetaPackedView;

	@nodeimpl
	public static final class MetaPackedImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaPacked;
		@att public ENode			 size;
		@att public ENode			 offset;
		@att public NameRef			 fld;
		@ref public Field			 packer;
	}
	@nodeview
	public static view MetaPackedView of MetaPackedImpl extends MetaSpecialView {
		public ENode			 size;
		public ENode			 offset;
		public NameRef			 fld;
		public Field			 packer;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaPacked() { super(new MetaPackedImpl(), MetaPackedAttr); }

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
	
	public KString getFld() {
		NameRef fld = this.fld;
		if (fld != null)
			return fld.name;
		return KString.Empty;
	}
	public void setFld(KString val) {
		fld = new NameRef(val);
	}
}

@nodeset
public class MetaPacker extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.packer");
	public static final MetaAttrSlot MetaPackerAttr = new MetaAttrSlot(ID, MetaPacker.class);

	@virtual typedef This  = MetaPacker;
	@virtual typedef NImpl = MetaPackerImpl;
	@virtual typedef VView = MetaPackerView;

	@nodeimpl
	public static final class MetaPackerImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaPacker;
		@att public ENode			 size;
	}
	@nodeview
	public static view MetaPackerView of MetaPackerImpl extends MetaSpecialView {
		public ENode			 size;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaPacker() { super(new MetaPackerImpl(), MetaPackerAttr); }

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

@nodeset
public class MetaAlias extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.alias");
	public static final MetaAttrSlot MetaAliasAttr = new MetaAttrSlot(ID, MetaAlias.class);

	@virtual typedef This  = MetaAlias;
	@virtual typedef NImpl = MetaAliasImpl;
	@virtual typedef VView = MetaAliasView;

	@nodeimpl
	public static final class MetaAliasImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaAlias;
		@att public NArr<ENode>		 aliases;
	}
	@nodeview
	public static view MetaAliasView of MetaAliasImpl extends MetaSpecialView {
		public:ro NArr<ENode>		 aliases;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaAlias() {
		super(new MetaAliasImpl(), MetaAliasAttr);
	}

	public MetaAlias(ConstStringExpr name) {
		super(new MetaAliasImpl(), MetaAliasAttr);
		this.aliases.append(name);
	}

	public ENode[] getAliases() {
		return aliases.toArray();
	}
}

@nodeset
public class MetaThrows extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.throws");
	public static final MetaAttrSlot MetaThrowsAttr = new MetaAttrSlot(ID, MetaThrows.class);

	@virtual typedef This  = MetaThrows;
	@virtual typedef NImpl = MetaThrowsImpl;
	@virtual typedef VView = MetaThrowsView;

	@nodeimpl
	public static final class MetaThrowsImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaThrows;
		@att public NArr<TypeNameRef>		 exceptions;
	}
	@nodeview
	public static view MetaThrowsView of MetaThrowsImpl extends MetaSpecialView {
		public:ro NArr<TypeNameRef>		 exceptions;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaThrows() {
		super(new MetaThrowsImpl(), MetaThrowsAttr);
	}

	public void add(TypeNameRef thr) {
		exceptions += thr;
	}
	
	public TypeNameRef[] getThrowns() {
		return exceptions.toArray();
	}
}

@nodeset
public class MetaPizzaCase extends MetaSpecial {
	public static final KString ID = KString.from("kiev.stdlib.meta.pcase");
	public static final MetaAttrSlot MetaPizzaCaseAttr = new MetaAttrSlot(ID, MetaPizzaCase.class);

	@virtual typedef This  = MetaPizzaCase;
	@virtual typedef NImpl = MetaPizzaCaseImpl;
	@virtual typedef VView = MetaPizzaCaseView;

	@nodeimpl
	public static final class MetaPizzaCaseImpl extends MetaSpecialImpl {
		@virtual typedef ImplOf = MetaPizzaCase;
		@ref public NArr<Field>		 fields;
		@att public int				 tag;
	}
	@nodeview
	public static view MetaPizzaCaseView of MetaPizzaCaseImpl extends MetaSpecialView {
		public:ro NArr<Field>		 fields;
		public int							 tag;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public MetaPizzaCase() {
		super(new MetaPizzaCaseImpl(), MetaPizzaCaseAttr);
	}

	public void add(Field f) {
		fields += f;
	}
	
	public Field[] getFields() {
		return fields.toArray();
	}
	
	public int getTag() { return this.tag; }
	public void setTag(int tag) { this.tag = tag; }
}

@nodeset
public abstract class MetaFlag extends MetaSpecial {

	public final void attach(ASTNode node) { this.set(node, Boolean.TRUE); }
	public final void detach(ASTNode node) { this.set(node, Boolean.FALSE); }
	public abstract void setZ(ASTNode node, boolean val);
	public abstract boolean getZ(ASTNode node);
	
	public MetaFlag(MetaAttrSlot attr) { super(new MetaSpecialImpl(), attr); }
	public Object copy() { return this; }
	
	public NodeData nodeCopiedTo(NodeImpl node) {
		return this; // attach the same instance to the copied node
	}
	public final void set(ASTNode node, Object value) {
		try {
			this.setZ(node, ((Boolean)value).booleanValue());
		} catch (ClassCastException e) {
			if (((Boolean)value).booleanValue())
				node.addNodeData(this);
			else
				node.delNodeData(this.getNodeDataId());
		}
	}
	public final Object get(ASTNode node) {
		try {
			return Boolean.valueOf(this.getZ(node));
		} catch (ClassCastException e) {
			return Boolean.valueOf(node.getNodeData(this.getNodeDataId()) != null);
		}
	}
}

@singleton
@nodeset
public class MetaUnerasable extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.unerasable");
	public static final MetaAttrSlot MetaUnerasableAttr = new MetaAttrSlot(ID, MetaUnerasable.class);

	private MetaUnerasable() { super(MetaUnerasableAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setTypeUnerasable(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isTypeUnerasable(); }
}

@singleton
@nodeset
public final class MetaSingleton extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.singleton");
	public static final MetaAttrSlot MetaSingletonAttr = new MetaAttrSlot(ID, MetaSingleton.class);

	private MetaSingleton() { super(MetaSingletonAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Struct)node).setSingleton(val); }
	public boolean getZ(ASTNode node)					{ return ((Struct)node).isSingleton(); }
}

@singleton
@nodeset
public final class MetaForward extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.forward");
	public static final MetaAttrSlot MetaForwardAttr = new MetaAttrSlot(ID, MetaForward.class);

	private MetaForward() { super(MetaForwardAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setForward(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isForward(); }
}

@singleton
@nodeset
public final class MetaPublic extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.public");
	public static final MetaAttrSlot MetaPublicAttr = new MetaAttrSlot(ID, MetaPublic.class);

	private MetaPublic() { super(MetaPublicAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setPublic(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isPublic(); }
}

@singleton
@nodeset
public final class MetaProtected extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.protected");
	public static final MetaAttrSlot MetaProtectedAttr = new MetaAttrSlot(ID, MetaProtected.class);

	private MetaProtected() { super(MetaProtectedAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setProtected(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isProtected(); }
}

@singleton
@nodeset
public final class MetaPrivate extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.private");
	public static final MetaAttrSlot MetaPrivateAttr = new MetaAttrSlot(ID, MetaPrivate.class);

	private MetaPrivate() { super(MetaPrivateAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ if (val) ((DNode)node).setPrivate(); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isPrivate(); }
}

@singleton
@nodeset
public final class MetaStatic extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.static");
	public static final MetaAttrSlot MetaStaticAttr = new MetaAttrSlot(ID, MetaStatic.class);

	private MetaStatic() { super(MetaStaticAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setStatic(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isStatic(); }
}

@singleton
@nodeset
public final class MetaAbstract extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.abstract");
	public static final MetaAttrSlot MetaAbstractAttr = new MetaAttrSlot(ID, MetaAbstract.class);

	private MetaAbstract() { super(MetaAbstractAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setAbstract(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isAbstract(); }
}

@singleton
@nodeset
public final class MetaFinal extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.final");
	public static final MetaAttrSlot MetaFinalAttr = new MetaAttrSlot(ID, MetaFinal.class);

	private MetaFinal() { super(MetaFinalAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setFinal(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isFinal(); }
}

@singleton
@nodeset
public final class MetaNative extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.native");
	public static final MetaAttrSlot MetaNativeAttr = new MetaAttrSlot(ID, MetaNative.class);

	private MetaNative() { super(MetaNativeAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Method)node).setMethodNative(val); }
	public boolean getZ(ASTNode node)					{ return ((Method)node).isMethodNative(); }
}

@singleton
@nodeset
public final class MetaSynchronized extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.synchronized");
	public static final MetaAttrSlot MetaSynchronizedAttr = new MetaAttrSlot(ID, MetaSynchronized.class);

	private MetaSynchronized() { super(MetaSynchronizedAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Method)node).setSynchronized(val); }
	public boolean getZ(ASTNode node)					{ return ((Method)node).isSynchronized(); }
}

@singleton
@nodeset
public final class MetaTransient extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.transient");
	public static final MetaAttrSlot MetaTransientAttr = new MetaAttrSlot(ID, MetaTransient.class);

	private MetaTransient() { super(MetaTransientAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((Field)node).setFieldTransient(val); }
	public boolean getZ(ASTNode node)					{ return ((Field)node).isFieldTransient(); }
}

@singleton
@nodeset
public final class MetaVolatile extends MetaFlag {
	public static final KString ID = KString.from("kiev.stdlib.meta.volatile");
	public static final MetaAttrSlot MetaVolatileAttr = new MetaAttrSlot(ID, MetaVolatile.class);

	private MetaVolatile() { super(MetaVolatileAttr); }
	
	public void    setZ(ASTNode node, boolean val)		{ ((DNode)node).setVolatile(val); }
	public boolean getZ(ASTNode node)					{ return ((DNode)node).isVolatile(); }
}

