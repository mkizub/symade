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

import syntax kiev.Syntax;

typedef kiev.stdlib.List<kiev.vlang.ENode>		ListAN;
typedef kiev.stdlib.List.Cons<kiev.vlang.ENode>	ConsAN;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTExpression.java,v 1.3.4.2 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.4.2 $
 *
 */

@node
@cfnode
public class ASTExpression extends Expr {
	@att public final NArr<ENode>		nodes;

	public void preResolve() {
		PassInfo.push(this);
		try {
			foreach (ENode n; nodes) n.preResolve();
			if (nodes.length == 1 && nodes[0] instanceof Expr)
				this.replaceWith(nodes[0]);
		} finally { PassInfo.pop(this); }
	}
	
	public void resolve(Type reqType) {
		PassInfo.push(this);
		try {
			List<ENode> lst = List.Nil;
			for (int i=nodes.length-1; i >=0; i--)
				lst = new List.Cons<ENode>(nodes[i], lst);
			List<ENode> results = List.Nil;
			ENode@ result;
			List<ENode>@ rest;
			trace( Kiev.debugOperators, "Expression: "+lst);
			NodeInfoPass.pushState();
			try {
				foreach( resolveExpr(result,rest,lst,0) ) {
					trace( Kiev.debugOperators, "May be resolved as: "+result+" and rest is "+rest);
					trace( Kiev.debugOperators, "Add possible resolved expression: "+result);
					results = new List.Cons<ENode>(result,results);
				}
			} finally { NodeInfoPass.popState(); }
			if (results.length() == 0) {
				StringBuffer msg = new StringBuffer("Expression: '"+this+"' may not be resolved using defined operators");
				foreach(ENode n; results)
					msg.append(n).append("\n");
				throw new CompilerException(pos, msg.toString());
			}
			if (results.length() > 1) {
				StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be reolved as:\n");
				foreach(ENode n; results)
					msg.append(n).append("\n");
				throw new CompilerException(pos, msg.toString());
			}
			
			ENode h = results.head();
			if( h instanceof UnresExpr )
				h = ((UnresExpr)h).toResolvedExpr();
			else if( h instanceof Expr )
				h = (Expr)h;
			
			this.replaceWithResolve(h, reqType);
		} finally { PassInfo.pop(this); }
	}
/*	
	public void resolve(Type reqType) {
		return tryResolve(reqType);
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
					res.parent = this;
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
*/
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
			resolveCastExpr		(result1, rest1, expr, priority)
		;	resolvePrefixExpr	(result1, rest1, expr, priority)
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
		trace( Kiev.debugOperators, "check that "+expr.head()+" is an expression ("+(expr.head() instanceof Expr)+") and has priority >= "+priority),
		(expr.head() instanceof Expr || expr.head() instanceof TypeRef) && getPriority(expr.head()) >= priority,
		result ?= getExpr(expr.head()),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	;
		expr.length() == 1,
		result ?= getExpr(expr.head()),
		rest ?= expr.tail(),
		trace( Kiev.debugOperators, "return expr "+result+" and rest "+rest)
	}

	rule resolveCastExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
	{
		Constants.opCastPriority >= priority,
		expr.length() > 1,
		expr.head() instanceof ASTCastOperator,
		op ?= ((ASTCastOperator)expr.head()).resolveOperator(),
		trace( Kiev.debugOperators, "trying cast "+op),
		resolveExpr(result1,rest1,expr.tail(),Constants.opCastPriority),
		//result ?= new CastExpr(expr.head().pos,((CastOperator)op).type,getExpr(result1),false,((CastOperator)op).reinterp),
		result ?= new PrefixExpr(expr.head().pos,op,getExpr(result1)),
		trace( Kiev.debugOperators, "found cast "+result),
		rest ?= rest1.$var
	}

	rule resolvePrefixExpr(ENode@ result, List<ENode>@ rest, List<ENode> expr, int priority)
		Operator@		op;
		ENode@			result1;
		List<ENode>@	rest1;
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
		//result ?= new UnaryExpr(expr.head().pos,op,getExpr(result1)),
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
			op.isStandard() || PassInfo.resolveOperatorR(op)
		;	expr.tail().head() instanceof ASTIdentifier,
			op ?= PostfixOperator.getOperator(((ASTIdentifier)expr.tail().head()).name),
			op.isStandard() || PassInfo.resolveOperatorR(op),
			trace( Kiev.debugOperators,"identifier as operator: "+op)
		},
		op.priority >= priority,
		getPriority(expr.head()) >= op.getArgPriority(0),
		trace( Kiev.debugOperators, "trying postfix "+op),
		//result ?= new UnaryExpr(expr.tail().head().pos,op,getExpr(expr.head())),
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
			op ?= BinaryOperator.InstanceOf, $cut,	expr.at(2) instanceof TypeRef,
			result ?= new InstanceofExpr(expr.at(1).getPos(),(Expr)expr.head(),((TypeRef)expr.at(2)).getType()),
			rest1 ?= expr.tail().tail().tail()
		;	resolveExpr(result1,rest1,expr.tail().tail(),op.getArgPriority(1)),
//			{
//				((BinaryOperator)op).is_boolean_op, $cut,
//				result ?= new BinaryBoolExpr(expr.tail().head().pos,(BinaryOperator)op,getExpr(expr.head()),getExpr(result1))
//			;	!((BinaryOperator)op).is_boolean_op, $cut,
//				result ?= new BinaryExpr(expr.tail().head().pos,(BinaryOperator)op,getExpr(expr.head()),getExpr(result1))
//			}
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
		//result ?= new AssignExpr(expr.tail().head().pos,(AssignOperator)op,getExpr(expr.head()),getExpr(result1)),
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

	public int getPriority(Object:Object expr) {
		return 256;
	}

	public int getPriority(ENode:Object expr) {
		return expr.getPriority();
	}

	public ENode getExpr(ENode:Object expr) {
		return expr;
	}

	public ENode getExpr(Object@:Object expr) {
		return getExpr(expr.$var);
	}

	public ENode getExpr(Object:Object expr) {
		throw new CompilerException(pos,"Node of type "+expr.getClass()+" cannot be an expression");
	}
/*
	ENode makeAssignExpr(Type tp, int pos, Operator op, OpTypes opt, ENode expr1, ENode expr2) {
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
				e = (Expr)new BinaryBooleanOrExpr(pos,(Expr)expr1,(Expr)expr2)
					.resolve(Type.tpBoolean);
			}
			else if( bop == BinaryOperator.BooleanAnd ) {
				e = (Expr)new BinaryBooleanAndExpr(pos,(Expr)expr1,(Expr)expr2)
					.resolve(Type.tpBoolean);
			}
			else {
				if( opt.method != null )
					if( opt.method.isStatic() )
						e = (Expr)new CallExpr(pos,opt.method,new Expr[]{(Expr)expr1,(Expr)expr2}).resolve(tp);
					else
						e = (Expr)new CallAccessExpr(pos,(Expr)expr1,opt.method,new Expr[]{(Expr)expr2}).resolve(tp);
				else
					e = (Expr)new BinaryBoolExpr(pos,(BinaryOperator)op,getExpr(expr1),getExpr(expr2)).resolve(Type.tpBoolean);
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
*/
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


