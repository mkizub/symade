package kiev.stdlib;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype _array_ extends Object {
	@macro
	public:ro final int length
	{
		get { new#ArrayLengthExpr(obj=obj, ident=ident) }
	};
}

