package kiev.tree;

import kiev.vlang.BinaryOperator;

public class BinaryExpr extends NodeImpl {
	public BinaryOperator		op;
	public VNode				expr1;
	public VNode				expr2;

	public BinaryExpr(CreateInfo src) {
		super(src);
	}

	void set$expr1(VNode e)	{
		expr1 = e;
		if (e != null) e.pnode = this;
	}
	void set$expr2(VNode e)	{
		expr2 = e;
		if (e != null) e.pnode = this;
	}
}

public class Closure extends NodeImpl {
    public VNode[]	params = VNode.emptyArray;
    public VNode	rtype;
    public VNode	body;

	public Closure(CreateInfo src) {
		super(src);
	}

	void set$rtype(VNode t)	{
		rtype = t;
		if (t != null) t.pnode = this;
	}
	void set$body(VNode b)	{
		body = b;
		if (b != null) b.pnode = this;
	}
}

public class CallExpr extends NodeImpl {
	public VNode    expr;
	public VNode    func;
    public VNode[]	args = VNode.emptyArray;

	public CallExpr(CreateInfo src) {
		super(src);
	}
	
	void set$expr(VNode e)	{
		expr = e;
		if (e != null) e.pnode = this;
	}
	void set$func(VNode f)	{
		func = f;
		if (f != null) f.pnode = this;
	}
}

public class CastExpr extends NodeImpl {
	public VNode	type;
	public VNode	expr;

	public CastExpr(CreateInfo src) {
		super(src);
	}
	
	void set$type(VNode t)	{
		type = t;
		if (t != null) t.pnode = this;
	}
	void set$expr(VNode e)	{
		expr = e;
		if (e != null) e.pnode = this;
	}
}

