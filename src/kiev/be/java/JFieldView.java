package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

import kiev.vlang.Field.FieldImpl;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JFieldView of FieldImpl extends JLvalDNodeView {

	public access:ro	Access				acc;
	public access:ro	KString				name;
	public access:ro	Type				ftype;
	public access:ro	JENodeView			init;
	public				Attr[]				attrs;
	public access:ro	JMethodView[]		invs;
	
	public final Field getField() { return (Field)this.getNode(); }
	
	@getter public final Type	get$type()	{ return this.$view.ftype.getType(); }
	
	@getter public final JMethodView[] get$invs() { return (JMethodView[])this.$view.invs.toJViewArray(JMethodView.class); }

	public final boolean isVirtual()		{ return this.$view.is_fld_virtual; }
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

}

