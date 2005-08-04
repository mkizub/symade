/* Generated By:JJTree: Do not edit this line. ASTExpression.java */

/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

typedef kiev.stdlib.List<kiev.vlang.ASTNode>		ListAN;
typedef kiev.stdlib.List.Cons<kiev.vlang.ASTNode>	ConsAN;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTExpression.java,v 1.3.4.2 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.2 $
 *
 */

@node
public class ASTExpression extends Expr {
	@att public final NArr<ASTNode>		nodes;


	public ASTExpression() {
		nodes = new NArr<ASTNode>(this, new AttrSlot("nodes", true, true));
	}

	public ASTExpression(int id) {
		this();
	}

	public void jjtAddChild(ASTNode n, int i) {
		nodes.add(n);
		if( i == 0 || pos == 0 ) setPos(n.getPos());
    }

	public ASTNode resolve(Type reqType) {
		PassInfo.push(this);
		try {
			List<ASTNode> lst = List.Nil;
			for (int i=nodes.length-1; i >=0; i--)
				lst = new List.Cons<ASTNode>(nodes[i], lst);
			List<ASTNode> results = List.Nil;
			ASTNode@ result;
			List<ASTNode>@ rest;
			boolean may_be_resolved = false;
			trace( Kiev.debugOperators, "Expression: "+lst);
			NodeInfoPass.pushState();
			try {
				foreach( resolveExpr(result,rest,lst,0) ) {
					trace( Kiev.debugOperators, "May be resolved as: "+result+" and rest is "+rest);
					Expr res = null;
					if( (res = ((Expr)result).tryResolve(reqType)) == null ) {
						trace( Kiev.debugOperators, "WARNING: full resolve of "+result+" to type "+reqType+" fails");
						continue;
					}
					may_be_resolved = true;
					trace( Kiev.debugOperators, "Add possible resolved expression: "+res);
					results = new List.Cons<ASTNode>(res.resolve(reqType),results);
				}
			} catch(Exception e ) {
				if( ! may_be_resolved )
				Kiev.reportError(pos,e);
			} finally {
				NodeInfoPass.popState();
			}
			if( ! may_be_resolved )
				throw new CompilerException(pos, "unresolved expression: "+this);
			if (results.length() > 1) {
				StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be reolved as:\n");
				foreach(ASTNode n; results)
					msg.append(n).append("\n");
				throw new CompilerException(pos, msg.toString());
			}
			if( results.head() instanceof Expr )
				return ((Expr)results.head()).resolve(reqType);
			else
				return results.head();
		} finally { PassInfo.pop(this); }
	}

	/**
	 *  @param result	- output result of parsing
	 *  @param expr		- input list of subexpressions and operators
	 *  @param rest		- output rest of list (uprased yet part)
	 */

	public rule resolveExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		trace( Kiev.debugOperators, "resolving "+expr+" with priority "+priority),
		expr.length() > 1,
		{
			resolveCastExpr		(result1, rest1, expr, priority)
		;	resolvePrefixExpr	(result1, rest1, expr, priority)
		;	resolvePostfixExpr	(result1, rest1, expr, priority)
		;	resolveMultiExpr	(result1, rest1, expr, priority)
		;	resolveAssignExpr	(result1, rest1, expr, priority)
		;	resolveBinaryExpr	(result1, rest1, expr, priority)
		},
		trace( Kiev.debugOperators, "partially resolved as ("+result1+")("+rest1+")"),
		resolveExpr(result, rest, new List.Cons<ASTNode>(result1,rest1), priority),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	;
		expr.length() > 1 && priority > 0,
		trace( Kiev.debugOperators, "check that "+expr.head()+" is an expression ("+(expr.head() instanceof Expr)+") and has priority >= "+priority),
		expr.head() instanceof Expr && ((Expr)expr.head()).getPriority() >= priority,
		result ?= expr.head(),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	;
		expr.length() == 1,
		result ?= expr.head(),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	}

	rule resolveCastExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		Constants.opCastPriority >= priority,
		expr.length() > 1,
		expr.head() instanceof ASTCastOperator,
		op ?= ((ASTCastOperator)expr.head()).resolveOperator(),
		trace( Kiev.debugOperators, "trying cast "+op),
		resolveExpr(result1,rest1,expr.tail(),Constants.opCastPriority),
		result ?= new CastExpr(expr.head().pos,((CastOperator)op).type,getExpr(result1),false,((CastOperator)op).reinterp),
		trace( Kiev.debugOperators, "found cast "+result),
		rest ?= rest1.$var
	}

	rule resolvePrefixExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		expr.length() > 1,
		{
			expr.head() instanceof ASTOperator,
			op ?= PrefixOperator.getOperator(((ASTOperator)expr.head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.head() instanceof ASTIdentifier,
			op ?= PrefixOperator.getOperator(((ASTIdentifier)expr.head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying prefix "+op),
		resolveExpr(result1,rest1,expr.tail(),op.getArgPriority(0)),
		result ?= new UnaryExpr(expr.head().pos,op,getExpr(result1)),
		trace( Kiev.debugOperators, "found prefix "+result),
		rest ?= rest1.$var
	}

	rule resolvePostfixExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		expr.length() > 1,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= PostfixOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= PostfixOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		getPriority(expr.head()) >= op.getArgPriority(0),
		trace( Kiev.debugOperators, "trying postfix "+op),
		result ?= new UnaryExpr(expr.tail().head().pos,op,getExpr(expr.head())),
		trace( Kiev.debugOperators, "found postfix "+result),
		rest ?= expr.tail().tail()
	}

	rule resolveBinaryExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		expr.length() > 2,
		expr.head() instanceof Expr,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= BinaryOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= BinaryOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying binary "+op),
		getPriority(expr.head()) >= op.getArgPriority(0),
		{
			op ?= BinaryOperator.InstanceOf, $cut,	expr.at(2) instanceof ASTType,
			result ?= new InstanceofExpr(expr.at(1).getPos(),(Expr)expr.head(),((ASTType)expr.at(2)).getType()),
			rest1 ?= expr.tail().tail().tail()
		;	resolveExpr(result1,rest1,expr.tail().tail(),op.getArgPriority(1)),
			{
				((BinaryOperator)op).is_boolean_op, $cut,
				result ?= new BinaryBooleanExpr(expr.tail().head().pos,(BinaryOperator)op,getExpr(expr.head()),getExpr(result1))
			;	!((BinaryOperator)op).is_boolean_op, $cut,
				result ?= new BinaryExpr(expr.tail().head().pos,(BinaryOperator)op,getExpr(expr.head()),getExpr(result1))
			}
		},
		trace( Kiev.debugOperators, "found binary "+result+" and rest is "+rest1),
		rest ?= rest1
	}

	rule resolveAssignExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		ASTNode@		result1;
		List<ASTNode>@	rest1;
	{
		expr.length() > 2,
		expr.head() instanceof Expr,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= AssignOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= AssignOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		trace( Kiev.debugOperators, "trying assign "+op),
		getPriority(expr.head()) >= op.getArgPriority(0),
		resolveExpr(result1,rest1,expr.tail().tail(),op.getArgPriority(1)),
		result ?= new AssignExpr(expr.tail().head().pos,(AssignOperator)op,getExpr(expr.head()),getExpr(result1)),
		trace( Kiev.debugOperators, "found assign "+result+" and rest is "+rest1),
		rest ?= rest1
	}

	rule resolveMultiExpr(ASTNode@ result, List<ASTNode>@ rest, List<ASTNode> expr, int priority)
		Operator@		op;
		List<ASTNode>@	result1;
		List<ASTNode>@	rest1;
	{
		expr.length() > 2,
		{
			expr.tail().head() instanceof ASTOperator,
			op ?= MultiOperator.getOperator(((ASTOperator)expr.tail().head()).image),
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= MultiOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		getPriority(expr.head()) >= op.getArgPriority(0),
		resolveMultiExpr((MultiOperator)op,1,result1,expr.tail().tail(),rest1),
		result ?= new MultiExpr(expr.tail().head().pos,(MultiOperator)op,new ConsAN(expr.head(),result1)),
		rest ?= rest1
	}

	rule resolveMultiExpr(MultiOperator op, int n, List<ASTNode>@ result, List<ASTNode>@ expr, List<ASTNode>@ rest)
		ASTNode@		result1;
		List<ASTNode>@	result2;
		List<ASTNode>@	rest1;
		List<ASTNode>@	rest2;
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

	public int getPriority(Object:Object expr) {
		return 256;
	}

	public int getPriority(ASTExpression:Object expr) {
		return 256;
	}

	public int getPriority(Expr:Object expr) {
		return expr.getPriority();
	}

	public Expr getExpr(Expr:Object expr) {
		return expr;
	}

	public Expr getExpr(PVar<Object>:Object expr) {
		return getExpr(expr.$var);
	}

	public Expr getExpr(Struct:Object expr) {
		return new WrapedExpr(expr.pos,expr);
	}

	public Expr getExpr(Type:Object expr) {
		return new WrapedExpr(expr.pos,expr);
	}

	public Expr getExpr(Object:Object expr) {
		throw new CompilerException(pos,"Node of type "+expr.getClass()+" cannot be an expression");
	}

	ASTNode makeAssignExpr(Type tp, int pos, Operator op, OpTypes opt, ASTNode expr1, ASTNode expr2) {
		Expr e;
		AssignOperator aop = (AssignOperator)op;
		if( expr1 instanceof Struct )
			expr1 = Expr.toExpr((Struct)expr1,null,pos,this);
		else
			expr1 = ((Expr)expr1).resolve(null);
		if( expr2 instanceof Struct )
			expr2 = Expr.toExpr((Struct)expr2,null,pos,this);
		else
			expr2 = ((Expr)expr2).resolveExpr(expr1.getType());
		if( opt.method != null )
			if( opt.method.isStatic() )
				e = new CallExpr(pos,opt.method,new Expr[]{(Expr)expr1,(Expr)expr2});
			else
				e = new CallAccessExpr(pos,(Expr)expr1,opt.method,new Expr[]{(Expr)expr2});
		else
			e = (Expr)new AssignExpr(pos,(AssignOperator)op,(Expr)expr1,(Expr)expr2);
		return e.resolve(tp);
	}

	ASTNode makeBinaryExpr(Type tp, int pos, Operator op, OpTypes opt, ASTNode expr1, ASTNode expr2) {
		Expr e;
		BinaryOperator bop = (BinaryOperator)op;
		if( bop.is_boolean_op ) {
			if( bop == BinaryOperator.BooleanOr ) {
				if( !(expr1 instanceof BooleanExpr) ) expr1 = new BooleanWrapperExpr(expr1.getPos(),(Expr)expr1);
				if( !(expr2 instanceof BooleanExpr) ) expr2 = new BooleanWrapperExpr(expr2.getPos(),(Expr)expr2);
				e = (Expr)new BinaryBooleanOrExpr(pos,(BooleanExpr)expr1,(BooleanExpr)expr2)
					.resolve(Type.tpBoolean);
			}
			else if( bop == BinaryOperator.BooleanAnd ) {
				if( !(expr1 instanceof BooleanExpr) ) expr1 = new BooleanWrapperExpr(expr1.getPos(),(Expr)expr1);
				if( !(expr2 instanceof BooleanExpr) ) expr2 = new BooleanWrapperExpr(expr2.getPos(),(Expr)expr2);
				e = (Expr)new BinaryBooleanAndExpr(pos,(BooleanExpr)expr1,(BooleanExpr)expr2)
					.resolve(Type.tpBoolean);
			}
			else {
				if( opt.method != null )
					if( opt.method.isStatic() )
						e = (Expr)new CallExpr(pos,opt.method,new Expr[]{(Expr)expr1,(Expr)expr2}).resolve(tp);
					else
						e = (Expr)new CallAccessExpr(pos,(Expr)expr1,opt.method,new Expr[]{(Expr)expr2}).resolve(tp);
				else
					e = (Expr)new BinaryBooleanExpr(pos,(BinaryOperator)op,getExpr(expr1),getExpr(expr2)).resolve(Type.tpBoolean);
			}
		} else {
			if( expr1 instanceof Struct )
				expr1 = Expr.toExpr((Struct)expr1,null,pos,this);
			else
				expr1 = (Expr)((Expr)expr1).resolve(null);
			if( expr2 instanceof Struct )
				expr2 = Expr.toExpr((Struct)expr2,null,pos,this);
			else
				expr2 = (Expr)((Expr)expr2).resolve(null);
			if( opt.method != null )
				if( opt.method.isStatic() )
					e = (Expr)new CallExpr(pos,opt.method,new Expr[]{(Expr)expr1,(Expr)expr2}).resolve(tp);
				else
					e = (Expr)new CallAccessExpr(pos,(Expr)expr1,opt.method,new Expr[]{(Expr)expr2}).resolve(tp);
			else
				e = (Expr)new BinaryExpr(pos,(BinaryOperator)op,(Expr)expr1,(Expr)expr2).resolve(tp);
		}
		return e;
	}

	public int		getPriority() { return 256; }

    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	foreach(ASTNode n; nodes)
	    	sb.append(' ').append(n);
        return sb.toString();
    }

    public Dumper toJava(Dumper dmp) {
    	foreach(ASTNode n; nodes)
	    	dmp.space().append(n).space();
        return dmp;
    }
}


