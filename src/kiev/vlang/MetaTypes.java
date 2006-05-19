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

@node
class Test extends ASTNode {
	public static SpaceRefAttrSlot<ASTNode> nodeattr$rarr;
	RefArray<ASTNode> rarr;
	@ref ASTNode astn;
	void foo() {
		int i;
		i = rarr.length;
		i = rarr.size();
		i = rarr.size_length();
		i = RefArray.static_size(rarr);
		i = RefArray.static_size_length(rarr);
		ASTNode[] x;
		x = rarr.delToArray();
		rarr.delAll();
		rarr.addAll(x);
		rarr.copyFrom(x);
		i = rarr.indexOf(astn);
		rarr[0] = rarr[1];
		//rarr += astn;
		rarr.add(astn);
		rarr.del(0);
		rarr.detach(astn);
		rarr.insert(0, astn);
	}
}

