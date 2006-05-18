package kiev.vlang.test;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

public metatype RefArray<N extends ANode> extends N[] {
	
	@macro
	private static ENode# getAttr(Field# f) {
		case CallExpr# self():
			new #SFldExpr(obj=f.parent, ident="nodeattr$"+f.id)
	}
	
	@macro
	public N[] delToArray()
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new #CallExpr(obj=getAttr(obj.var),ident="delToArray",args={obj.obj})
	}

	@macro
	public void delAll()
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="delAll",args={obj.obj})
	}

	@macro
	public void addAll(N[] arr)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="addAll",args={obj.obj, arr})
	}

	@macro
	public void copyFrom(N[] arr)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="copyFrom",args={obj.obj, arr})
	}

	@macro
	public void indexOf(N node)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="indexOf",args={obj.obj, node})
	}

	@macro
	public N set(int idx, N node)
		alias operator(210,lfy,[])
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="set",args={obj.obj, idx, node})
	}

	@macro
	public N add(N node)
		alias append
		alias operator(5, lfy, +=)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="add",args={obj.obj, node})
	}

	@macro
	public void del(int idx)
	{
		case @forward @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="del",args={obj.obj, idx})
	}

	@macro
	public void detach(N node)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="detach",args={obj.obj, node})
	}

	@macro
	public void insert(int idx, N node)
	{
		case @forward CallExpr# self(IFldExpr# obj):
			new#CallExpr(obj=getAttr(obj.var),ident="insert",args={obj.obj, idx, node})
	}

	@macro
	public int size()
	{
		case @forward CallExpr# self():
			new#ArrayLengthExpr(obj=self.obj, ident=self.ident)
	}

	@macro
	public int size_length()
	{
		case @forward CallExpr# self():
			new#CallExpr(obj=self.obj, ident="size")
	}

	@macro
	public static int static_size(RefArray<N> rarr)
	{
		case @forward CallExpr# self():
			new#ArrayLengthExpr(obj=rarr, ident=self.ident)
	}

	@macro
	public static int static_size_length(RefArray<N> rarr)
	{
		case @forward CallExpr# self():
			new#CallExpr(obj=self.obj, ident="static_size", args={rarr})
	}

}

class Test {
	RefArray<ASTNode> rarr;
	int foo() {
		return rarr.length;
	}
	int bar_size() {
		return rarr.size();
	}
	int bar_length() {
		return rarr.size_length();
	}
	int static_bar_size() {
		return RefArray.static_size(rarr);
	}
	int static_bar_length() {
		return RefArray.static_size_length(rarr);
	}
}

