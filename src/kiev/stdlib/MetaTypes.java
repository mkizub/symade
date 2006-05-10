package kiev.stdlib;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype _array_ extends Object {
	public:ro final int length
	{
		get #ArrayLengthExpr(obj=obj, ident=ident)
	};
}

