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

/**
 * @author Maxim Kizub
 *
 */

/**
 * Base class to represent unresolved, temporary created expressions.
 */
@node
@cfnode
public abstract class UnresExpr extends ENode {

	public Operator			op;
	
	public UnresExpr() {}

	public UnresExpr(int pos, Operator op) {
		super(pos);
		this.op = op;
	}
	
	public int getPriority() { return op.priority; }
	
	public abstract ENode toResolvedExpr();
	
}

/**
 * Represents unresolved, temporary created prefix expression.
 *
 * 'expr' field is @ref to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@node
@cfnode
public class PrefixExpr extends UnresExpr {
	@ref public ENode		expr;
	
	public PrefixExpr() {}

	public PrefixExpr(int pos, Operator op, ENode expr) {
		super(pos, op);
		this.expr = expr;
	}
	
	public String toString() {
		return op + " ( " +expr+ " )"; 
	}

	public ENode toResolvedExpr() {
		ENode e = expr;
		if (e instanceof UnresExpr) e = ((UnresExpr)e).toResolvedExpr();
		if (op instanceof CastOperator)
			return new CastExpr(pos,((CastOperator)op).type,e,((CastOperator)op).reinterp);
		return new UnaryExpr(0,op,e);
	}
	
}

/**
 * Represents unresolved, temporary created postfix expression.
 *
 * 'expr' field is @ref to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@node
@cfnode
public class PostfixExpr extends UnresExpr {
	@ref public ENode		expr;
	
	public PostfixExpr() {}

	public PostfixExpr(int pos, Operator op, ENode expr) {
		super(pos, op);
		this.expr = expr;
	}
	
	public String toString() {
		return "( " +expr+ " ) "+op; 
	}

	public ENode toResolvedExpr() {
		ENode e = expr;
		if (e instanceof UnresExpr) e = ((UnresExpr)e).toResolvedExpr();
		return new UnaryExpr(0,op,e);
	}
	
}

/**
 * Represents unresolved, temporary created infix expression.
 *
 * 'expr1' and 'expr2' fields are @ref to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved binary expression is created.
 */
@node
@cfnode
public class InfixExpr extends UnresExpr {

	@ref public ENode		expr1;
	@ref public ENode		expr2;
	
	public InfixExpr() {}

	public InfixExpr(int pos, Operator op, ENode expr1, ENode expr2) {
		super(pos, op);
		this.expr1 = expr1;
		this.expr2 = expr2;
	}
	
	public String toString() {
		return "( " +expr1+ " ) "+op+" ( "+expr2+" )"; 
	}
	
	public ENode toResolvedExpr() {
		ENode e1 = expr1;
		if (e1 instanceof UnresExpr) e1 = ((UnresExpr)e1).toResolvedExpr();
		ENode e2 = expr2;
		if (e2 instanceof UnresExpr) e2 = ((UnresExpr)e2).toResolvedExpr();
		if (op instanceof AssignOperator)
			return new AssignExpr(pos,(AssignOperator)op,e1,e2);
		if (((BinaryOperator)op).is_boolean_op)
			return new BinaryBoolExpr(pos,(BinaryOperator)op,e1,e2);
		return new BinaryExpr(pos,(BinaryOperator)op,e1,e2);
	}
	

}

/**
 * Represents unresolved, temporary created multi-operand expression.
 *
 * 'exprs' field is @ref to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved multi-expression is created.
 */
@node
@cfnode
public class MultiExpr extends UnresExpr {
	@ref public final NArr<ENode>		exprs;

	public MultiExpr() {}

	public MultiExpr(int pos, MultiOperator op, List<ENode> exprs) {
		super(pos, op);
		this.exprs.addAll(exprs.toArray());
	}

	public String toString() {
		MultiOperator op = (MultiOperator)this.op;
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < exprs.length; i++) {
			sb.append("(").append(exprs[i]).append(')');
			if (op.images.length > i)
				sb.append(((MultiOperator)op).images[i]);
		}
		return sb.toString(); 
	}

	public ENode toResolvedExpr() {
		if (op == MultiOperator.Conditional) {
			ENode e1 = exprs[0];
			if (e1 instanceof UnresExpr) e1 = ((UnresExpr)e1).toResolvedExpr();
			ENode e2 = exprs[1];
			if (e2 instanceof UnresExpr) e2 = ((UnresExpr)e2).toResolvedExpr();
			ENode e3 = exprs[2];
			if (e3 instanceof UnresExpr) e3 = ((UnresExpr)e3).toResolvedExpr();
			return new ConditionalExpr(pos,e1,e2,e3);
		}
		throw new CompilerException(pos,"Multi-operators are not implemented");
	}
//	public Expr tryResolve(Type reqType) {
//		if( op == MultiOperator.Conditional ) {
//			Expr cond = ((Expr)exprs[0]).tryResolve(Type.tpBoolean);
//			if( cond == null )
//				return null;
//			Expr expr1 = ((Expr)exprs[1]).tryResolve(reqType);
//			if( expr1 == null )
//				return null;
//			Expr expr2 = ((Expr)exprs[2]).tryResolve(reqType);
//			if( expr2 == null )
//				return null;
//			return (Expr)new ConditionalExpr(pos,(Expr)cond.copy(),(Expr)expr1.copy(),(Expr)expr2.copy()).resolve(reqType);
//		}
//		throw new CompilerException(pos,"Multi-operators are not implemented");
//	}
}



