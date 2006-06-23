package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.be.java15.JLvalDNode;
import kiev.ir.java15.RField;
import kiev.be.java15.JField;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Field")
public final class Field extends LvalDNode implements Accessable {
	public static Field[]	emptyArray = new Field[0];

	public static final AttrSlot GETTER_ATTR = new ExtAttrSlot("getter method",false,false,Method.class);
	public static final AttrSlot SETTER_ATTR = new ExtAttrSlot("setter method",false,false,Method.class);
	public static final SpaceRefDataAttrSlot<Method> ATTR_INVARIANT_CHECKERS = new SpaceRefDataAttrSlot<Field>("invariant checkers",false,Method.class);	

	private static final Field dummyNode = new Field();
	
	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@virtual typedef This  = Field;
	@virtual typedef JView = JField;
	@virtual typedef RView = RField;

	/** Field' access */
		 public Access				acc;
	/** Type of the field */
	@att public TypeRef				ftype;
	/** Initial value of this field */
	@att public ENode				init;
	/** Constant value of this field */
	@ref public ConstExpr			const_value;
	/** Array of attributes of this field */
	public kiev.be.java15.Attr[]		attrs = kiev.be.java15.Attr.emptyArray;

	@getter public final Type	get$type() { return this.ftype.getType(); }

	public void callbackChildChanged(AttrSlot attr) {
		if (isAttached()) {
			if      (attr.name == "ftype")
				parent().callbackChildChanged(pslot());
			else if (attr.name == "meta")
				parent().callbackChildChanged(pslot());
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

	@setter public final void		set$acc(Access val)	{ this.acc = val; Access.verifyDecl(this); }
	@getter public final Access		get$acc()			{ return this.acc; }

	public Field() {}
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(Symbol name, TypeRef ftype, int flags) {
		this.flags = flags;
		this.id = name;
		this.ftype = ftype;
		this.meta = new MetaSet();
		trace(Kiev.debugCreation,"New field created: "+name+" with type "+ftype);
	}

	public Field(String name, TypeRef ftype, int flags) {
		this(new Symbol(name),ftype,flags);
	}
	
	public Field(String name, Type type, int flags) {
		this(new Symbol(name),new TypeRef(type),flags);
	}
	
	public ASTNode getDummyNode() {
		return Field.dummyNode;
	}

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

	public final MetaPacked getMetaPacked() {
		return (MetaPacked)MetaPacked.ATTR.get(this);
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)MetaPacker.ATTR.get(this);
	}

	public final MetaAlias getMetaAlias() {
		return (MetaAlias)MetaAlias.ATTR.get(this);
	}

	public boolean preResolveIn() {
		ENode init = this.init;
		if (init != null && init instanceof NewInitializedArrayExpr && init.type == null) {
			Type tp = getType();
			if!(tp instanceof ArrayType)
				Kiev.reportError(this,"Scalar field is initialized by array");
			else
				init.setType((ArrayType)tp);
		}
		return true;
	}

	public String toString() { return id.toString(); }
}

