package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public class JFieldView extends JLvalDNodeView {
	final Field.FieldImpl impl;
	public JFieldView(Field.FieldImpl impl) {
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
	
	public final boolean isVirtual() { return this.impl.is_fld_virtual; }
	public final boolean isEnumField() { return this.impl.is_fld_enum; }
	public final boolean isPackerField() { return this.impl.is_fld_packer; }
	public final boolean isPackedField() { return this.impl.is_fld_packed; }
}

