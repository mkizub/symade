/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
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

@node(name="Expr")
public class ASTExpression extends ENode {

	@dflow(out="nodes") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		nodes;
	}
	
	@virtual typedef This  = ASTExpression;

	@att public ENode[]				nodes;

	private int cur_pos;

	public ASTExpression() {}

	public int		getPriority() { return 256; }

	public void preResolveOut() {
		if (nodes.length == 1) {
			ENode n = nodes[0];
			this.replaceWithNode(~n);
			return;
		}
		ENode e = parseExpr();
		if (e != null) {
			e = e.closeBuild();
			if (isPrimaryExpr())
				e.setPrimaryExpr(true);
			this.replaceWithNode(~e);
		}
	}

    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	foreach(ENode n; nodes)
	    	sb.append(' ').append(n);
        return sb.toString();
    }

	public ENode parseExpr() {
		cur_pos = 0;
		List<ENode> results = List.Nil;
		ENode@ result;
		trace( Kiev.debug && Kiev.debugOperators, "\n\n\nParsing: "+this);
		foreach( resolveExpr(result,0) ) {
			if (cur_pos == nodes.length) {
				trace( Kiev.debug && Kiev.debugOperators, "Add possible resolved expression: "+result);
				results = new List.Cons<ENode>(result,results);
			} else {
				trace( Kiev.debug && Kiev.debugOperators, "Incomplete possible resolved expression: "+result);
			}
		}
		if (results.length() == 0) {
			StringBuffer msg = new StringBuffer("Expression: '"+this+"' may not be resolved using defined operators");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			Kiev.reportError(this, msg.toString());
			return null;
		}
		if (results.length() > 1) {
			StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be reolved as:\n");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			Kiev.reportError(this, msg.toString());
			return null;
		}
		return (ENode)results.head();
	}

	public rule resolveExpr(ENode@ ret, int priority)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 0 restLength "+restLength()+" priority "+priority),

		restLength() > 1,
		op @= Operator.allOperatorNamesHash,
		matchOpStart(op, priority),
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 2 for "+op),
		opArgs = makeOpArgs(op) : popRes(),
		resolveOpArg(expr, op, 1, opArgs),
		resolveExprNext(ret, expr, priority)
	;
		restLength() > 0,
		!(nodes[cur_pos] instanceof ASTOperator),
		nodes[cur_pos].getPriority() >= priority,
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 3 res "+nodes[cur_pos]),
		ret ?= nodes[cur_pos],
		++cur_pos : --cur_pos
	}
	
	public rule resolveExprNext(ENode@ ret, ENode prev, int priority)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 0 restLength "+restLength()+" priority "+priority+" prev: "+prev),
		restLength() > 1,
		op @= Operator.allOperatorNamesHash,
		matchOpStart(op,prev,priority),
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 1 for "+prev+" "+op),
		opArgs = makeOpArgs(op,prev) : popRes(),
		resolveOpArg(expr, op, 2, opArgs),
		resolveExprNext(ret, expr, priority)
	;
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 2 res "+prev),
		ret ?= prev
	}
	
	public rule resolveOpArg(ENode@ ret, Operator op, int pos, ENode[] result)
		ENode@ expr;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 0 for pos "+pos+" as "+(op.args.length == pos? "END" : op.args[pos])),
		op.args.length == pos,
		$cut,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 1"),
		ret ?= makeExpr(op,result)
	;
		op.args[pos] instanceof OpArg.EXPR,
		resolveExpr(expr,((OpArg.EXPR)op.args[pos]).priority),
		result[pos] = expr,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 2 added "+expr),
		resolveOpArg(ret, op, pos+1, result)
	;
		op.args[pos] instanceof OpArg.TYPE,
		nodes[cur_pos] instanceof TypeRef,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 3 added "+nodes[cur_pos]),
		pushRes(result,pos) : popRes(),
		resolveOpArg(ret, op, pos+1, result)
	;
		op.args[pos] instanceof OpArg.OPER,
		nodes[cur_pos] instanceof ASTOperator,
		((OpArg.OPER)op.args[pos]).text == ((ASTOperator)nodes[cur_pos]).ident,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 4 added "+nodes[cur_pos]),
		pushRes(result,pos) : popRes(),
		resolveOpArg(ret, op, pos+1, result)
	}
	
	private boolean matchOpStart(Operator op, int priority) {
		if (op.priority < priority) return false;
		if (restLength() < op.args.length) return false;
		ENode en = nodes[cur_pos];
		if (op.args[0] instanceof OpArg.OPER) return (en instanceof ASTOperator && en.ident == ((OpArg.OPER)op.args[0]).text);
		if (op.args[0] instanceof OpArg.TYPE) return (en instanceof TypeRef);
		if (op.args[0] instanceof OpArg.EXPR) {
			if (en instanceof ASTOperator) return false;
			if (en.getPriority() < ((OpArg.EXPR)op.args[0]).priority) return false;
			if (op.args.length > 1 && op.args[1] instanceof OpArg.OPER) {
				en = nodes[cur_pos+1];
				if !(en instanceof ASTOperator && en.ident == ((OpArg.OPER)op.args[1]).text)
					return false;
			}
			return true;
		}
		return false;
	}
	private boolean matchOpStart(Operator op, ENode prev, int priority) {
		if (op.priority < priority) return false;
		if (restLength() < op.args.length-1) return false;
		ENode en = nodes[cur_pos];
		if!(op.args[0] instanceof OpArg.EXPR) return false;
		if (((OpArg.EXPR)op.args[0]).priority > prev.getPriority()) return false;
		if (op.args[1] instanceof OpArg.OPER) return (en instanceof ASTOperator && en.ident == ((OpArg.OPER)op.args[1]).text);
		if (op.args[1] instanceof OpArg.TYPE) return (en instanceof TypeRef);
		if (op.args[1] instanceof OpArg.EXPR) return !(en instanceof ASTOperator) && en.getPriority() >= ((OpArg.EXPR)op.args[1]).priority;
		return false;
	}
	private int restLength() {
		return nodes.length - cur_pos;
	}
	private ENode[] makeOpArgs(Operator op) {
		ENode[] result = new ENode[op.args.length];
		result[0] = nodes[cur_pos++];
		return result;
	}
	private ENode[] makeOpArgs(Operator op, ENode prev) {
		ENode[] result = new ENode[op.args.length];
		result[0] = prev;
		result[1] = nodes[cur_pos++];
		return result;
	}
	private void pushRes(ENode[] result, int pos) {
		result[pos] = nodes[cur_pos];
		++cur_pos;
	}
	private void popRes() {
		--cur_pos;
	}
	private ENode makeExpr(Operator op, ENode[] result) {
		int pos = this.pos;
		foreach (ENode e; result; e.pos > 0) {
			pos = e.pos;
			if (e instanceof ASTOperator)
				break;
		}
		return new UnresOpExpr(pos, op, result);
	}
}


