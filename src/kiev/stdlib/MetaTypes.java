package kiev.stdlib;

//import kiev.vlang.CallExpr;
//import kiev.vlang.ContainerAccessExpr;
//import kiev.vlang.IFldExpr;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public metatype _array_<_elem_ extends any> extends Object {
	@macro
	public:ro final native int length;

	@macro
	public native _elem_ get(int idx)
		alias operator(210,xfy,[])
;//	{
//		case @forward CallExpr# self():
//			new#ContainerAccessExpr(obj=self.obj, index=idx)
//	}

//	@macro
//	public _elem_ set(int idx, _elem_ val)
//		alias operator(210,lfy,[])
//	{
//		case @forward CallExpr# self():
//			new#AssignExpr(lval=new#ContainerAccessExpr(obj=self.obj, index=idx), value=val) 
//	}
}

