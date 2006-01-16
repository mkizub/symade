/* Generated By:JJTree: Do not edit this line. ASTExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;

import syntax kiev.Syntax;

typedef kiev.stdlib.List<kiev.vlang.ENode>		ListAN;
typedef kiev.stdlib.List.Cons<kiev.vlang.ENode>	ConsAN;

/**
 * @author Maxim Kizub
 *
 */

@node
public class ASTExpression extends ENode {

	@dflow(out="nodes") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		nodes;
	}
	
	@node
	public static final class ASTExpressionImpl extends ENodeImpl {
		@att public NArr<ENode>			nodes;
		public ASTExpressionImpl() {}
	}
	@nodeview
	public static final view ASTExpressionView of ASTExpressionImpl extends ENodeView {
		public access:ro	NArr<ENode>			nodes;
	}

	@att public abstract virtual access:ro NArr<ENode>			nodes;
	
	public NodeView				getNodeView()			{ return new ASTExpressionView((ASTExpressionImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new ASTExpressionView((ASTExpressionImpl)this.$v_impl); }
	public ASTExpressionView	getASTExpressionView()	{ return new ASTExpressionView((ASTExpressionImpl)this.$v_impl); }

	@getter public NArr<ENode>		get$nodes()		{ return this.getASTExpressionView().nodes; }

	public ASTExpression() {
		super(new ASTExpressionImpl());
	}
	
	public void preResolveOut() {
		if (nodes.length == 1) {
			ENode n = nodes[0];
			this.replaceWithNode((ENode)~n);
			return;
		}
		List<ENode> lst = List.Nil;
		for (int i=nodes.length-1; i >=0; i--)
			lst = new List.Cons<ENode>(nodes[i], lst);
		List<ENode> results = List.Nil;
		ENode@ result;
		List<ENode>@ rest;
		trace( Kiev.debugOperators, "Expression: "+lst);
		foreach( resolveExpr(result,rest,lst,0) ) {
			trace( Kiev.debugOperators, "May be resolved as: "+result+" and rest is "+rest);
			trace( Kiev.debugOperators, "Add possible resolved expression: "+result);
			results = new List.Cons<ENode>(result,results);
		}
		if (results.length() == 0) {
			StringBuffer msg = new StringBuffer("Expression: '"+this+"' may not be resolved using defined operators");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			Kiev.reportError(this, msg.toString());
			return;
		}
		if (results.length() > 1) {
			StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be reolved as:\n");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			Kiev.reportError(this, msg.toString());
			return;
		}
		
		ENode e = results.head();
		if (e instanceof UnresExpr)
			e = ((UnresExpr)e).toResolvedExpr();
		if (isPrimaryExpr())
			e.setPrimaryExpr(true);
		this.replaceWithNode((ENode)~e);
	}
	
	public void resolve(Type reqType) {
		List<ENode> lst = List.Nil;
		for (int i=nodes.length-1; i >=0; i--)
			lst = new List.Cons<ENode>(nodes[i], lst);
		List<ENode> results = List.Nil;
		ENode@ result;
		List<ENode>@ rest;
		trace( Kiev.debugOperators, "Expression: "+lst);
		foreach( resolveExpr(result,rest,lst,0) ) {
			trace( Kiev.debugOperators, "May be resolved as: "+result+" and rest is "+rest);
			trace( Kiev.debugOperators, "Add possible resolved expression: "+result);
			results = new List.Cons<ENode>(result,results);
		}
		if (results.length() == 0) {
			StringBuffer msg = new StringBuffer("Expression: '"+this+"' may not be resolved using defined operators");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			throw new CompilerException(this, msg.toString());
		}
		if (results.length() > 1) {
			StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be reolved as:\n");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			throw new CompilerException(this, msg.toString());
		}
		
		ENode e = results.head();
		if (e instanceof UnresExpr)
			e = ((UnresExpr)e).toResolvedExpr();
		if (isPrimaryExpr())
			e.setPrimaryExpr(true);
		this.replaceWithNodeResolve(reqType, (ENode)~e);
	}

	/**
	 *  @param result	- output result of parsing
	 *  @param expr		- input list of subexpressions and operators
	 *  @param rest		- output rest of list (uprased yet part)
	 */

	public rule resolveExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		ENode@			result1;
		List<ENode>@	rest1;
	{
		trace( Kiev.debugOperators, "resolving "+expr+" with priority "+priority),
		expr.length() > 1,
		{
			resolvePrefixExpr	(result1, rest1, expr, priority)
		;	resolvePostfixExpr	(result1, rest1, expr, priority)
		;	resolveMultiExpr	(result1, rest1, expr, priority)
		;	resolveAssignExpr	(result1, rest1, expr, priority)
		;	resolveBinaryExpr	(result1, rest1, expr, priority)
		},
		trace( Kiev.debugOperators, "partially resolved as ("+result1+")("+rest1+")"),
		resolveExpr(result, rest, new List.Cons<ENode>(result1,rest1), priority),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	;
		expr.length() > 1 && priority > 0,
		trace( Kiev.debugOperators, "check that "+expr.head()+" is an expression and has priority >= "+priority),
		!(expr.head() instanceof ASTOperator) && expr.head().getPriority() >= priority,
		result ?= getExpr(expr.head()),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	;
		expr.length() == 1,
		result ?= getExpr(expr.head()),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	}

	rule resolvePrefixExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
	{
		expr.length() > 1,
		{
			expr.head() instanceof ASTCastOperator,
			$cut,
			op ?= ((ASTCastOperator)expr.head()).resolveOperator(),
			trace( Kiev.debugOperators, "trying cast "+op)
		;	expr.head() instanceof ASTOperator,
			op ?= PrefixOperator.getOperator(((ASTOperator)expr.head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(this,op)
		;	expr.head() instanceof ASTIdentifier,
			op ?= PrefixOperator.getOperator(((ASTIdentifier)expr.head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(this,op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying prefix "+op),
		resolveExpr(result1,rest1,expr.tail(),op.getArgPriority(0)),
		result ?= new PrefixExpr(expr.head().pos,op,getExpr(result1)),
		trace( Kiev.debugOperators, "found prefix "+result),
		rest ?= rest1.$var
	}

	rule resolvePostfixExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
	{
		expr.length() > 1,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= PostfixOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(this,op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= PostfixOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(this,op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		expr.head().getPriority() >= op.getArgPriority(0),
		trace( Kiev.debugOperators, "trying postfix "+op),
		result ?= new PostfixExpr(expr.tail().head().pos,op,getExpr(expr.head())),
		trace( Kiev.debugOperators, "found postfix "+result),
		rest ?= expr.tail().tail()
	}

	rule resolveBinaryExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
	{
		expr.length() > 2,
		!(expr.head() instanceof ASTOperator),
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= BinaryOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(this,op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= BinaryOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(this,op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying binary "+op),
		expr.head().getPriority() >= op.getArgPriority(0),
		{
			op ?= BinaryOperator.InstanceOf, $cut,	expr.at(2) instanceof TypeRef,
			result ?= new InstanceofExpr(expr.at(1).getPos(),(ENode)~expr.head(),((TypeRef)expr.at(2)).getType()),
			rest1 ?= expr.tail().tail().tail()
		;	resolveExpr(result1,rest1,expr.tail().tail(),op.getArgPriority(1)),
			result ?= new InfixExpr(expr.tail().head().pos,(BinaryOperator)op,getExpr(expr.head()),getExpr(result1))
		},
		trace( Kiev.debugOperators, "found binary "+result+" and rest is "+rest1),
		rest ?= rest1
	}

	rule resolveAssignExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
	{
		expr.length() > 2,
		!(expr.head() instanceof ASTOperator),
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= AssignOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(this,op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= AssignOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(this,op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying assign "+op),
		expr.head().getPriority() >= op.getArgPriority(0),
		resolveExpr(result1,rest1,expr.tail().tail(),op.getArgPriority(1)),
		result ?= new InfixExpr(expr.tail().head().pos,(AssignOperator)op,getExpr(expr.head()),getExpr(result1)),
		trace( Kiev.debugOperators, "found assign "+result+" and rest is "+rest1),
		rest ?= rest1
	}

	rule resolveMultiExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		List<ENode>@	result1;
		List<ENode>@	rest1;
	{
		expr.length() > 2,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= MultiOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(this,op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= MultiOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(this,op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		expr.head().getPriority() >= op.getArgPriority(0),
		resolveMultiExpr((MultiOperator)op,1,result1,expr.tail().tail(),rest1),
		result ?= new MultiExpr(expr.tail().head().pos,(MultiOperator)op,new ConsAN(expr.head(),result1)),
		rest ?= rest1
	}

	rule resolveMultiExpr(MultiOperator op, int n, List<ENode>@ result, List<ENode> expr, List<ENode>@ rest)
		ENode@			result1;
		List<ENode>@	result2;
		List<ENode>@	rest1;
		List<ENode>@	rest2;
	{
		resolveExpr(result1,rest1,expr,op.getArgPriority(n)),
		{
			n == op.images.length,
			result ?= new ConsAN(result1,List.Nil),
			rest ?= rest1
		;
			n < op.images.length,
			rest1.length() > 0,
			{
				rest1.head() instanceof ASTOperator,
				op.images[n].equals(((ASTOperator)rest1.head()).image)
			;	rest1.head() instanceof ASTIdentifier,
				op.images[n].equals(((ASTIdentifier)rest1.head()).name)
			},
			resolveMultiExpr(op,n+1,result2,rest1.tail(),rest2),
			result ?= new ConsAN(result1,result2),
			rest ?= rest2
		}
	}

	public ENode getExpr(ENode:Object expr) {
		return expr;
	}

	public ENode getExpr(PVar<ENode>:Object expr) {
		return getExpr(expr.$var);
	}

	public ENode getExpr(Object:Object expr) {
		throw new CompilerException(this,"Node of type "+expr.getClass()+" cannot be an expression");
	}

	public int		getPriority() { return 256; }

    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	foreach(ENode n; nodes)
	    	sb.append(' ').append(n);
        return sb.toString();
    }

    public Dumper toJava(Dumper dmp) {
    	foreach(ENode n; nodes)
	    	dmp.space().append(n).space();
        return dmp;
    }
}


