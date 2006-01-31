package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.NArr.JArr;

import static kiev.stdlib.Debug.*;

import kiev.vlang.Field.FieldImpl;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JField of FieldImpl extends JLvalDNode {

	public:ro	Access				acc;
	public:ro	KString				name;
	public:ro	Type				ftype;
	public:ro	JENode			init;
	public:ro	JConstExpr		const_value;
	public				Attr[]				attrs;
	public:ro	JArr<JMethod>	invs;
	
	public final Field getField() { return (Field)this.getNode(); }
	
	@getter public final Type	get$type()	{ return this.$view.ftype.getType(); }
	
	public final boolean isVirtual()		{ return this.$view.is_virtual; }
	public final boolean isEnumField()		{ return this.$view.is_fld_enum; }
	public final boolean isPackerField()	{ return this.$view.is_fld_packer; }
	public final boolean isPackedField()	{ return this.$view.is_fld_packed; }

	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		for(int i=0; i < attrs.length; i++) {
			if(attrs[i].name == a.name) {
				attrs[i] = a;
				return a;
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		Attr[] attrs = this.attrs;
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
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

}

