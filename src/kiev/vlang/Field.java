package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JFieldView;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class Field extends LvalDNode implements Named, Typed, Accessable {
	public static Field[]	emptyArray = new Field[0];

	@node
	public static class FieldImpl extends LvalDNodeImpl {
		public FieldImpl() {}
		public FieldImpl(int pos) { super(pos); }
		public FieldImpl(int pos, int fl) { super(pos, fl); }

		/** Field' access */
		     public Access				acc;
		/** Name of the field */
		     public NodeName			name;
		/** Type of the field */
		@att public TypeRef				ftype;
		/** Initial value of this field */
		@att public ENode				init;
		/** Array of attributes of this field */
		     public Attr[]				attrs = Attr.emptyArray;
		/** Array of invariant methods, that check this field */
		@ref public NArr<Method>		invs;

		public void callbackChildChanged(AttrSlot attr) {
			if (parent != null && pslot != null) {
				if      (attr.name == "ftype")
					parent.callbackChildChanged(pslot);
				else if (attr.name == "meta")
					parent.callbackChildChanged(pslot);
			}
		}
	}
	@nodeview
	public static class FieldView extends LvalDNodeView {
		final FieldImpl impl;
		public FieldView(FieldImpl impl) {
			super(impl);
			this.impl = impl;
		}

		@getter public final Access					get$acc()			{ return this.impl.acc; }
		@getter public final NodeName				get$name()			{ return this.impl.name; }
		@getter public final TypeRef				get$ftype()			{ return this.impl.ftype; }
		@getter public final ENode					get$init()			{ return this.impl.init; }
		@getter public final Attr[]					get$attrs()			{ return this.impl.attrs; }
		@getter public final NArr<Method>			get$invs()			{ return this.impl.invs; }
		
		@getter public final Type					get$type()			{ return this.impl.ftype.getType(); }
		
		@setter public final void set$acc(Access val)					{ this.impl.acc = val; this.impl.acc.verifyAccessDecl((Field)this.impl._self); }
		@setter public final void set$name(NodeName val)				{ this.impl.name = val; }
		@setter public final void set$ftype(TypeRef val)				{ this.impl.ftype = val; }
		@setter public final void set$init(ENode val)					{ this.impl.init = val; }
		@setter public final void set$attrs(Attr[] val)				{ this.impl.attrs = val; }
		
		// is a virtual field
		public final boolean isVirtual() {
			return this.impl.is_fld_virtual;
		}
		public final void setVirtual(boolean on) {
			if (this.impl.is_fld_virtual != on) {
				this.impl.is_fld_virtual = on;
				this.impl.callbackChildChanged(nodeattr$flags);
			}
		}
		// is a field of enum
		public final boolean isEnumField() {
			return this.impl.is_fld_enum;
		}
		public final void setEnumField(boolean on) {
			if (this.impl.is_fld_enum != on) {
				this.impl.is_fld_enum = on;
				this.impl.callbackChildChanged(nodeattr$flags);
			}
		}
		// packer field (auto-generated for packed fields)
		public final boolean isPackerField() {
			return this.impl.is_fld_packer;
		}
		public final void setPackerField(boolean on) {
			if (this.impl.is_fld_packer != on) {
				this.impl.is_fld_packer = on;
				this.impl.callbackChildChanged(nodeattr$flags);
			}
		}
		// packed field
		public final boolean isPackedField() {
			return this.impl.is_fld_packed;
		}
		public final void setPackedField(boolean on) {
			if (this.impl.is_fld_packed != on) {
				this.impl.is_fld_packed = on;
				this.impl.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		{ return new FieldView((FieldImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new FieldView((FieldImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	{ return new FieldView((FieldImpl)this.$v_impl); }
	public FieldView		getFieldView()		{ return new FieldView((FieldImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JFieldView((FieldImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		{ return new JFieldView((FieldImpl)this.$v_impl); }
	public JLvalDNodeView	getJLvalDNodeView()	{ return new JFieldView((FieldImpl)this.$v_impl); }
	public JFieldView		getJFieldView()		{ return new JFieldView((FieldImpl)this.$v_impl); }

	@getter public Access				get$acc()			{ return this.getFieldView().acc; }
	@getter public NodeName				get$name()			{ return this.getFieldView().name; }
	@getter public TypeRef				get$ftype()			{ return this.getFieldView().ftype; }
	@getter public ENode				get$init()			{ return this.getFieldView().init; }
	@getter public Attr[]				get$attrs()			{ return this.getFieldView().attrs; }
	@getter public NArr<Method>			get$invs()			{ return this.getFieldView().invs; }
	
	@getter public Type					get$type()			{ return this.getFieldView().type; }
	
	@setter public void set$acc(Access val)				{ this.getFieldView().acc = val; }
	@setter public void set$name(NodeName val)				{ this.getFieldView().name = val; }
	@setter public void set$ftype(TypeRef val)				{ this.getFieldView().ftype = val; }
	@setter public void set$init(ENode val)				{ this.getFieldView().init = val; }
	@setter public void set$attrs(Attr[] val)				{ this.getFieldView().attrs = val; }

	/** Field' access */
	     public virtual abstract				Access			acc;
	/** Name of the field */
	     public virtual abstract				NodeName		name;
	/** Type of the field */
	@att public virtual abstract				TypeRef			ftype;
	/** Initial value of this field */
	@att public virtual abstract				ENode			init;
	/** Array of attributes of this field */
	     public virtual abstract				Attr[]			attrs;
	/** Array of invariant methods, that check this field */
	@ref public virtual abstract access:ro		NArr<Method>	invs;

	@ref public abstract virtual access:ro		Type			type;
	
	/** JField for java backend */
	//@ref public kiev.backend.java15.JField			jfield;

	public Field() { super(new FieldImpl()); }
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(KString name, TypeRef ftype, int acc) {
		super(new FieldImpl(0,acc));
		this.name = new NodeName(name);
		this.ftype = ftype;
		this.acc = new Access(0);
		this.meta = new MetaSet();
		trace(Kiev.debugCreation,"New field created: "+name+" with type "+ftype);
	}

	public Field(KString name, Type type, int acc) {
		this(name,new TypeRef(type),acc);
	}
	
	// is a virtual field
	public boolean isVirtual() { return this.getFieldView().isVirtual(); }
	public void setVirtual(boolean on) { this.getFieldView().setVirtual(on); }
	// is a field of enum
	public boolean isEnumField() { return this.getFieldView().isEnumField(); }
	public void setEnumField(boolean on) { this.getFieldView().setEnumField(on); }
	// packer field (auto-generated for packed fields)
	public boolean isPackerField() { return this.getFieldView().isPackerField(); }
	public void setPackerField(boolean on) { this.getFieldView().setPackerField(on); }
	// packed field
	public boolean isPackedField() { return this.getFieldView().isPackedField(); }
	public void setPackedField(boolean on) { this.getFieldView().setPackedField(on); }

	public MetaVirtual getMetaVirtual() {
		return (MetaVirtual)this.meta.get(MetaVirtual.NAME);
	}

	public MetaPacked getMetaPacked() {
		return (MetaPacked)this.meta.get(MetaPacked.NAME);
	}

	public MetaPacker getMetaPacker() {
		return (MetaPacker)this.meta.get(MetaPacker.NAME);
	}

	public MetaAlias getMetaAlias() {
		return (MetaAlias)this.meta.get(MetaAlias.NAME);
	}

	public String toString() { return name.toString()/*+":="+type*/; }

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
//		if( !(a.name==attrOperator || a.name==attrImport
//			|| a.name==attrRequire || a.name==attrEnsure) ) {
			for(int i=0; i < attrs.length; i++) {
				if(attrs[i].name == a.name) {
					attrs[i] = a;
					return a;
				}
			}
//		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
	}

	public void resolveDecl() throws RuntimeException {
		foreach (Meta m; meta)
			m.resolve();
		if( init != null ) {
			if (init instanceof TypeRef)
				((TypeRef)init).toExpr(type);
			init.resolve(type);
			if (init.getType() != type) {
				init = new CastExpr(init.pos, type, init);
				init.resolve(type);
			}
		}
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(KString.Empty) )
			type.toJava(dmp).forsed_space().append(name);
		if( init != null ) {
			if( !name.equals(KString.Empty) )
				dmp.append(" = ");
			init.toJava(dmp);
		}
		return dmp.append(';');
	}
}

