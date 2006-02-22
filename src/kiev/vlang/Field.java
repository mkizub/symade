package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java.JNode;
import kiev.be.java.JDNode;
import kiev.be.java.JLvalDNode;
import kiev.ir.java.RField;
import kiev.be.java.JField;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public final class Field extends LvalDNode implements Named, Accessable {
	public static Field[]	emptyArray = new Field[0];

	private static final Field dummyNode = new Field();
	
	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@virtual typedef This  = Field;
	@virtual typedef NImpl = FieldImpl;
	@virtual typedef VView = VField;
	@virtual typedef JView = JField;
	@virtual typedef RView = RField;

	@nodeimpl
	public static final class FieldImpl extends LvalDNodeImpl {
		@virtual typedef ImplOf = Field;

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

		// is a field of enum
		public final boolean isEnumField() {
			return this.is_fld_enum;
		}
		public final void setEnumField(boolean on) {
			if (this.is_fld_enum != on) {
				this.is_fld_enum = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// packer field (auto-generated for packed fields)
		public final boolean isPackerField() {
			return this.is_fld_packer;
		}
		public final void setPackerField(boolean on) {
			if (this.is_fld_packer != on) {
				this.is_fld_packer = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// packed field
		public final boolean isPackedField() {
			return this.is_fld_packed;
		}
		public final void setPackedField(boolean on) {
			if (this.is_fld_packed != on) {
				this.is_fld_packed = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// field's initializer was already added to class initializer
		public final boolean isAddedToInit() {
			return this.is_fld_added_to_init;
		}
		public final void setAddedToInit(boolean on) {
			if (this.is_fld_added_to_init != on) {
				this.is_fld_added_to_init = on;
			}
		}
	}
	@nodeview
	public static view FieldView of FieldImpl extends LvalDNodeView {
		public		Access			acc;
		public		NodeName		name;
		public		TypeRef			ftype;
		public		ENode			init;
		public		ConstExpr		const_value;
		public:ro	NArr<Method>	invs;
		
		@setter public final void set$acc(Access val)	{ ((FieldImpl)this).acc = val; Access.verifyDecl((Field)getDNode()); }
		@getter public final Type	get$type()			{ return ((FieldImpl)this).ftype.getType(); }
		
		// is a field of enum
		public final boolean isEnumField();
		public final void setEnumField(boolean on);
		// packer field (auto-generated for packed fields)
		public final boolean isPackerField();
		public final void setPackerField(boolean on);
		// packed field
		public final boolean isPackedField();
		public final void setPackedField(boolean on);
		// field's initializer was already added to class initializer
		public final boolean isAddedToInit();
		public final void setAddedToInit(boolean on);

		public Type	getType() { return type; }

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
	}
	@nodeview
	public static final view VField of FieldImpl extends FieldView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	@getter public Access			get$acc()			{ return this.getVView().acc; }
	@setter public void set$acc(Access val)			{ this.getVView().acc = val; }

	public Field() { super(new FieldImpl()); }
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(KString name, TypeRef ftype, int flags) {
		this();
		this.flags = flags;
		this.name = new NodeName(name);
		this.ftype = ftype;
		this.meta = new MetaSet();
		trace(Kiev.debugCreation,"New field created: "+name+" with type "+ftype);
	}

	public Field(KString name, Type type, int acc) {
		this(name,new TypeRef(type),acc);
	}
	
	public ASTNode getDummyNode() {
		return Field.dummyNode;
	}
	
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

	public String toString() { return name.toString(); }

	public NodeName getName() { return name; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}

	public void resolveDecl() {
		getRView().resolveDecl();
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

