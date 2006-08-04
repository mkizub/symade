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
public final class Field extends LvalDNode {
	public static Field[]	emptyArray = new Field[0];

	public static final AttrSlot GETTER_ATTR = new ExtAttrSlot("getter method",false,false,TypeInfo.newTypeInfo(Method.class,null));
	public static final AttrSlot SETTER_ATTR = new ExtAttrSlot("setter method",false,false,TypeInfo.newTypeInfo(Method.class,null));
	public static final SpaceRefDataAttrSlot<Method> ATTR_INVARIANT_CHECKERS = new SpaceRefDataAttrSlot<Field>("invariant checkers",false,TypeInfo.newTypeInfo(Method.class,null));	

	private static final Field dummyNode = new Field();
	
	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@virtual typedef This  = Field;
	@virtual typedef JView = JField;
	@virtual typedef RView = RField;

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
			else
				super.callbackChildChanged(attr);
		} else {
			super.callbackChildChanged(attr);
		}
	}

	// is a field of enum
	public final boolean isEnumField() {
		return this.is_fld_enum;
	}
	public final void setEnumField(boolean on) {
		if (this.is_fld_enum != on) {
			this.is_fld_enum = on;
		}
	}
	// packer field (auto-generated for packed fields)
	public final boolean isPackerField() {
		return this.is_fld_packer;
	}
	public final void setPackerField(boolean on) {
		if (this.is_fld_packer != on) {
			assert(!locked);
			this.is_fld_packer = on;
		}
	}
	// packed field
	public final boolean isPackedField() {
		return this.is_fld_packed;
	}
	public final void setPackedField(boolean on) {
		if (this.is_fld_packed != on) {
			assert(!locked);
			this.is_fld_packed = on;
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

	public Field() {}
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(Symbol name, TypeRef ftype, int flags) {
		this.pos = name.pos;
		this.u_name = id.sname;
		this.id = name;
		this.ftype = ftype;
		if (flags != 0) {
			if ((flags & ACC_PUBLIC) == ACC_PUBLIC) meta.setF(new MetaAccess("public"));
			if ((flags & ACC_PROTECTED) == ACC_PROTECTED) meta.setF(new MetaAccess("protected"));
			if ((flags & ACC_PRIVATE) == ACC_PRIVATE) meta.setF(new MetaAccess("private"));
			if ((flags & ACC_STATIC) == ACC_STATIC) meta.setF(new MetaStatic());
			if ((flags & ACC_FINAL) == ACC_FINAL) meta.setF(new MetaFinal());
			if ((flags & ACC_FORWARD) == ACC_FORWARD) meta.setF(new MetaForward());
			if ((flags & ACC_VOLATILE) == ACC_VOLATILE) meta.setF(new MetaVolatile());
			if ((flags & ACC_TRANSIENT) == ACC_TRANSIENT) meta.setF(new MetaTransient());
			if ((flags & ACC_ABSTRACT) == ACC_ABSTRACT) meta.setF(new MetaAbstract());
			if ((flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) meta.setF(new MetaSynthetic());
			if ((flags & ACC_MACRO) == ACC_MACRO) meta.setF(new MetaMacro());
			if ((flags & ACC_NATIVE) == ACC_NATIVE) meta.setF(new MetaNative());
			this.flags = flags;
		}
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
		return (MetaPacked)this.meta.getU("kiev.stdlib.meta.packed");
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.meta.getU("kiev.stdlib.meta.packer");
	}

	public final MetaAlias getMetaAlias() {
		return (MetaAlias)this.meta.getU("kiev.stdlib.meta.alias");
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

