package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JFieldView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class Field extends LvalDNode implements Named, Typed, Accessable {
	public static Field[]	emptyArray = new Field[0];

	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

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
		/** Constant value of this field */
		@ref public ConstExpr			const_value;
		/** Array of attributes of this field */
		public kiev.be.java.Attr[]		attrs = kiev.be.java.Attr.emptyArray;
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
	public static view FieldView of FieldImpl extends LvalDNodeView {
		public				Access			acc;
		public				NodeName		name;
		public				TypeRef			ftype;
		public				ENode			init;
		public				ConstExpr		const_value;
		public access:ro	NArr<Method>	invs;
		
		@setter public final void set$acc(Access val)	{ this.$view.acc = val; Access.verifyDecl((Field)getDNode()); }
		@getter public final Type	get$type()			{ return this.$view.ftype.getType(); }
		
		// is a field of enum
		public final boolean isEnumField() {
			return this.$view.is_fld_enum;
		}
		public final void setEnumField(boolean on) {
			if (this.$view.is_fld_enum != on) {
				this.$view.is_fld_enum = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// packer field (auto-generated for packed fields)
		public final boolean isPackerField() {
			return this.$view.is_fld_packer;
		}
		public final void setPackerField(boolean on) {
			if (this.$view.is_fld_packer != on) {
				this.$view.is_fld_packer = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// packed field
		public final boolean isPackedField() {
			return this.$view.is_fld_packed;
		}
		public final void setPackedField(boolean on) {
			if (this.$view.is_fld_packed != on) {
				this.$view.is_fld_packed = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new FieldView((FieldImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new FieldView((FieldImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	alias operator(210,fy,$cast) { return new FieldView((FieldImpl)this.$v_impl); }
	public FieldView		getFieldView()		alias operator(210,fy,$cast) { return new FieldView((FieldImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JFieldView((FieldImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JFieldView((FieldImpl)this.$v_impl); }
	public JLvalDNodeView	getJLvalDNodeView()	alias operator(210,fy,$cast) { return new JFieldView((FieldImpl)this.$v_impl); }
	public JFieldView		getJFieldView()		alias operator(210,fy,$cast) { return new JFieldView((FieldImpl)this.$v_impl); }

	@getter public Access				get$acc()			{ return this.getFieldView().acc; }
	@getter public NodeName				get$name()			{ return this.getFieldView().name; }
	@getter public TypeRef				get$ftype()			{ return this.getFieldView().ftype; }
	@getter public ENode				get$init()			{ return this.getFieldView().init; }
	@getter public ConstExpr			get$const_value()	{ return this.getFieldView().const_value; }
	@getter public NArr<Method>			get$invs()			{ return this.getFieldView().invs; }
	
	@getter public Type					get$type()			{ return this.getFieldView().type; }
	
	@setter public void set$acc(Access val)				{ this.getFieldView().acc = val; }
	@setter public void set$name(NodeName val)				{ this.getFieldView().name = val; }
	@setter public void set$ftype(TypeRef val)				{ this.getFieldView().ftype = val; }
	@setter public void set$init(ENode val)				{ this.getFieldView().init = val; }
	@setter public void set$const_value(ConstExpr val)		{ this.getFieldView().const_value = val; }

	/** Field' access */
	     public virtual abstract				Access			acc;
	/** Name of the field */
	     public virtual abstract				NodeName		name;
	/** Type of the field */
	@att public virtual abstract				TypeRef			ftype;
	/** Initial value of this field */
	@att public virtual abstract				ENode			init;
	@ref public virtual abstract				ConstExpr		const_value;
	/** Array of invariant methods, that check this field */
	@ref public virtual abstract access:ro		NArr<Method>	invs;

	@ref public abstract virtual access:ro		Type			type;
	
	public Field() { super(new FieldImpl()); }
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(KString name, TypeRef ftype, int acc) {
		super(new FieldImpl(0,acc));
		this.name = new NodeName(name);
		this.ftype = ftype;
		this.meta = new MetaSet();
		trace(Kiev.debugCreation,"New field created: "+name+" with type "+ftype);
	}

	public Field(KString name, Type type, int acc) {
		this(name,new TypeRef(type),acc);
	}
	
	// is a field of enum
	public boolean isEnumField() { return this.getFieldView().isEnumField(); }
	public void setEnumField(boolean on) { this.getFieldView().setEnumField(on); }
	// packer field (auto-generated for packed fields)
	public boolean isPackerField() { return this.getFieldView().isPackerField(); }
	public void setPackerField(boolean on) { this.getFieldView().setPackerField(on); }
	// packed field
	public boolean isPackedField() { return this.getFieldView().isPackedField(); }
	public void setPackedField(boolean on) { this.getFieldView().setPackedField(on); }

	public final MetaVirtual getMetaVirtual() {
		return (MetaVirtual)this.getNodeData(MetaVirtual.ID);
	}

	public final MetaPacked getMetaPacked() {
		return (MetaPacked)this.getNodeData(MetaPacked.ID);
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.getNodeData(MetaPacker.ID);
	}

	public final MetaAlias getMetaAlias() {
		return (MetaAlias)this.getNodeData(MetaAlias.ID);
	}

	public String toString() { return name.toString()/*+":="+type*/; }

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}

	public boolean	isConstantExpr() {
		if( this.isFinal() ) {
			if (this.init != null && this.init.isConstantExpr())
				return true;
			else if (this.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		if (this.init != null && this.init.isConstantExpr())
			return this.init.getConstValue();
		else if (this.const_value != null)
			return this.const_value.getConstValue();
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}

	public void resolveDecl() throws RuntimeException {
		foreach (Meta m; meta)
			m.resolve();
		if( init != null ) {
			if (init instanceof TypeRef)
				((TypeRef)init).toExpr(type);
			init.resolve(type);
			if (init.getType() ≉ type) {
				init = new CastExpr(init.pos, type, init);
				init.resolve(type);
			}
		}
		setResolved(true);
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

