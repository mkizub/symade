package kiev.stdlib;

import kiev.vlang.IFldExpr;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype _array_ extends Object {
	@macro
	public:ro final int length
	{
		case IFldExpr# self(): new#ArrayLengthExpr(obj=self.obj, ident=self.ident)
	};
}

