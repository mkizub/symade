package kiev.tree;

import kiev.vlang.BinaryOperator;

public class BinaryExpr extends NodeImpl {
	public BinaryOperator		op;
	public VNode				expr1;
	public VNode				expr2;

	public BinaryExpr(CreateInfo src, BinaryOperator op, VNode expr1, VNode expr2) {
		super(src);
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}
	
}

public class Closure extends NodeImpl {
    public VNode[]	params = VNode.emptyArray;
    public VNode	rtype;
    public VNode	body;

	public Closure(CreateInfo src, VNode[] params, VNode rtype, VNode body) {
		super(src);
		this.params = params;
		this.rtype  = rtype;
		this.body   = body;
	}
	
}

public class CallExpr extends NodeImpl {
	public VNode    expr;
	public VNode    func;
    public VNode[]	args = VNode.emptyArray;

	public CallExpr(CreateInfo src, VNode expr, VNode func, VNode[] args) {
		super(src);
		this.expr = expr;
		this.func = func;
		this.args = args;
	}
	
}

public class CastExpr extends NodeImpl {
	public VNode	type;
	public VNode	expr;

	public CastExpr(CreateInfo src, VNode type, VNode expr) {
		super(src);
		this.type = type;
		this.expr = expr;
	}
	
}

