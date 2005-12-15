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
	public access:ro	NodeName			name;
	public access:ro	TypeRef				ftype;
	public access:ro	ENode				init;
	public access:ro	Attr[]				attrs;
	public access:ro	NArr<Method>		invs;
	
	@getter public final Type	get$type()	{ return this.$view.ftype.getType(); }
	
	public final boolean isVirtual()		{ return this.$view.is_fld_virtual; }
	public final boolean isEnumField()		{ return this.$view.is_fld_enum; }
	public final boolean isPackerField()	{ return this.$view.is_fld_packer; }
	public final boolean isPackedField()	{ return this.$view.is_fld_packed; }
}

