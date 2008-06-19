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

import syntax kiev.Syntax;

typedef kiev.stdlib.List<kiev.vlang.ENode>			ListAN;
typedef kiev.stdlib.List.Cons<kiev.vlang.ENode>	ConsAN;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(name="Expr", lang=CoreLang)
public class ASTExpression extends ENode {

	@DataFlowDefinition(out="nodes") private static class DFI {
	@DataFlowDefinition(in="this:in", seq="true")		ENode[]		nodes;
	}
	
	@virtual typedef This  = ASTExpression;

	@nodeAttr public ENode∅	nodes;

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
			this.replaceWithNodeReWalk(~e);
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
			StringBuffer msg = new StringBuffer("Umbigous expression: '"+this+"'\nmay be resolved as:\n");
			foreach(ENode n; results)
				msg.append(n).append("\n");
			Kiev.reportError(this, msg.toString());
			return null;
		}
		return (ENode)results.head();
	}

	private rule resolveExpr(ENode@ ret, int priority)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 0 restLength "+restLength()+" priority "+priority),

		restLength() > 1,
		op @= Operator.allOperatorNamesHash,
		matchOpStart(op, priority, false),
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 2 for "+op),
		opArgs = makeOpArgs(op) : popRes(),
		resolveOpArg(expr, op, 1, opArgs),
		resolveExprNext(ret, expr, priority)
	;
		restLength() > 0,
		!isOper(nodes[cur_pos]),
		nodes[cur_pos].getPriority() >= priority,
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 3 res "+nodes[cur_pos]),
		ret ?= nodes[cur_pos],
		++cur_pos : --cur_pos
	}
	
	private rule resolveExprNext(ENode@ ret, ENode prev, int priority)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 0 restLength "+restLength()+" priority "+priority+" prev: "+prev),
		restLength() >= 1,
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
	
	private rule resolveType(TypeRef@ ret)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveType: 0 restLength "+restLength()),

		restLength() > 1,
		op @= Operator.allOperatorNamesHash,
		matchOpStart(op,0,true),
		trace( Kiev.debug && Kiev.debugOperators, "resolveType: 2 for "+op),
		opArgs = makeOpArgs(op) : popRes(),
		resolveOpArg(expr, op, 1, opArgs),
		resolveTypeNext(ret, expr)
	;
		restLength() > 0,
		ret ?= asType(nodes[cur_pos]),
		trace( Kiev.debug && Kiev.debugOperators, "resolveType: 3 res "+nodes[cur_pos]),
		++cur_pos : --cur_pos
	}
	
	private rule resolveTypeNext(TypeRef@ ret, ENode prev)
		ENode@			expr;
		Operator@		op;
		ENode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 0 restLength "+restLength()+" prev: "+prev),
		restLength() >= 1,
		op @= Operator.allOperatorNamesHash,
		matchTypeOpStart(op,prev),
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 1 for "+prev+" "+op),
		opArgs = makeOpArgs(op,prev) : popRes(),
		resolveOpArg(expr, op, 2, opArgs),
		resolveTypeNext(ret, expr)
	;
		ret ?= asType(prev),
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 2 res "+prev)
	}
	
	private rule resolveOpArg(ENode@ ret, Operator op, int pos, ENode[] result)
		ENode@ expr;
		TypeRef@ tref;
		SeqRes seq_res;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 0 for pos "+pos+" as "+(op.args.length == pos? "END" : op.args[pos])),
		op.args.length == pos,
		$cut,
		ret ?= makeExpr(op,result),
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 1 ret "+ret)
	;
		restLength() > 0,
		{
			op.args[pos] instanceof OpArg.EXPR,
			resolveExpr(expr,((OpArg.EXPR)op.args[pos]).priority),
			result[pos] = expr,
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 2 added "+expr),
			resolveOpArg(ret, op, pos+1, result)
		;
			op.args[pos] instanceof OpArg.TYPE,
			resolveType(tref),
			result[pos] = tref,
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 3 added "+tref),
			resolveOpArg(ret, op, pos+1, result)
		;
			op.args[pos] instanceof OpArg.IDNT,
			isIdent(nodes[cur_pos]),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 4 added "+nodes[cur_pos]),
			pushRes(result,pos) : popRes(),
			resolveOpArg(ret, op, pos+1, result)
		;
			op.args[pos] instanceof OpArg.OPER,
			isOperMatch(nodes[cur_pos], ((OpArg.OPER)op.args[pos]).text),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 5 added "+nodes[cur_pos]),
			pushRes(result,pos) : popRes(),
			resolveOpArg(ret, op, pos+1, result)
		}
	;
		op.args[pos] instanceof OpArg.SEQS,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: seq "+op.args[pos]),
		seq_res = parseSeq((OpArg.SEQS)op.args[pos]) : cur_pos = seq_res.save_cur_pos,
		seq_res.exprs != null,
		((OpArg.SEQS)op.args[pos]).min <= seq_res.exprs.length,
		result[pos] = new UnresSeqs(((OpArg.SEQS)op.args[pos]).sep.text,seq_res.exprs),
		resolveOpArg(ret, op, pos+1, result)
	}
	
	class SeqRes {
		ENode[]	exprs;
		int save_cur_pos;
	}
	private SeqRes parseSeq(OpArg.SEQS seq) {
		SeqRes res = new SeqRes();
		res.save_cur_pos = cur_pos;
		Stack<ENode> seq_stk = new Stack<ENode>();
		if (resolveLongestSeqEl(seq.el, seq_stk)) {
			trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 0 add "+seq_stk.peek());
			while (nodes.length > cur_pos && isOperMatch(nodes[cur_pos], seq.sep.text)) {
				trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 1 add "+seq.sep.text);
				++cur_pos;
				if (resolveLongestSeqEl(seq.el, seq_stk)) {
					trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 2 add "+seq_stk.peek());
					continue;
				}
				trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 3 failed ");
				cur_pos = res.save_cur_pos;
				return res; // if there was the separator - there must be the next expression
			}
		}
		res.exprs = seq_stk.toArray();
		trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 4 success found "+res.exprs.length);
		return res;
	}

	private boolean resolveLongestSeqEl(OpArg seq_el, Stack<ENode> seq_stk) {
		int pos = this.cur_pos;
		ENode res = null;
		ENode@ en;
		foreach (resolveSeqEl(seq_el, en)) {
			if (this.cur_pos > pos) {
				res = (ENode)en;
				pos = this.cur_pos;
			}
		}
		if (res == null)
			return false;
		seq_stk.push(res);
		this.cur_pos = pos;
		return true;
	}

	private rule resolveSeqEl(OpArg seq_el, ENode@ res)
		ENode@ expr;
		TypeRef@ tref;
	{
		seq_el instanceof OpArg.EXPR,
		resolveExpr(expr,((OpArg.EXPR)seq_el).priority),
		res ?= expr
	;
		seq_el instanceof OpArg.TYPE,
		resolveType(tref),
		res ?= tref
	;
		seq_el instanceof OpArg.IDNT,
		isIdent(nodes[cur_pos]),
		res ?= nodes[cur_pos],
		++cur_pos : --cur_pos
	}

	private boolean matchOpStart(Operator op, int priority, boolean tp_op) {
		if (tp_op) {
			if (!op.is_type_operator) return false;
		} else {
			if (op.priority < priority) return false;
		}
		if (restLength() < op.min_args) return false;
		ENode en = nodes[cur_pos];
		OpArg arg0 = op.args[0];
		if (arg0 instanceof OpArg.OPER) {
			return isOperMatch(en, arg0.text);
		}
		if (arg0 instanceof OpArg.TYPE) {
			if (asType(en) != null)
				goto check_second;
			return false;
		}
		if (arg0 instanceof OpArg.IDNT) {
			if (isIdent(en))
				goto check_second;
			return false;
		}
		if (arg0 instanceof OpArg.EXPR) {
			if (isOper(en)) return false;
			if (en.getPriority() < arg0.priority) return false;
			goto check_second;
		}
		return false;
	check_second:
		if (op.args.length > 1 && op.args[1] instanceof OpArg.OPER) {
			en = nodes[cur_pos+1];
			if (!isOperMatch(en, ((OpArg.OPER)op.args[1]).text))
				return false;
		}
		return true;
	}
	private boolean matchOpStart(Operator op, ENode prev, int priority) {
		if (op.priority < priority) return false;
		if (restLength() < op.min_args-1) return false;
		ENode en = nodes[cur_pos];
		OpArg arg0 = op.args[0];
		if (arg0 instanceof OpArg.EXPR) {
			if (arg0.priority > prev.getPriority()) return false;
		}
		else if (arg0 instanceof OpArg.TYPE) {
			if (255 > prev.getPriority()) return false;
			if (asType(prev) == null)
				return false;
		}
		else
			return false;
	check_second:;
		OpArg arg1 = op.args[1];
		if (arg1 instanceof OpArg.OPER) return isOperMatch(en, arg1.text);
		if (arg1 instanceof OpArg.TYPE) return (asType(en) != null);
		if (arg1 instanceof OpArg.EXPR) return !isOper(en) && en.getPriority() >= arg1.priority;
		return false;
	}
	private boolean matchTypeOpStart(Operator op, ENode prev) {
		if (!op.is_type_operator) return false;
		if (restLength() < op.min_args-1) return false;
		ENode en = nodes[cur_pos];
		OpArg arg1 = op.args[1];
		if (arg1 instanceof OpArg.OPER) return isOperMatch(en, arg1.text);
		if (arg1 instanceof OpArg.TYPE) return (asType(en) != null);
		if (arg1 instanceof OpArg.IDNT) return isIdent(en);
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
	private boolean isIdent(ENode e) {
		return (e instanceof EToken && e.isIdentifier());
	}
	private TypeRef asType(ENode e) {
		if (e == null)
			return null;
		if (e instanceof TypeRef)
			return (TypeRef)e;
		if (e instanceof EToken)
			return e.asType();
		return null;
	}
	private boolean isOper(ENode e) {
		return (e instanceof EToken && (e.isOperator() || e.asOperator() != null));
	}
	private EToken asOper(ENode e) {
		if (e instanceof EToken)
			return e.asOperator();
		return null;
	}
	private boolean isOperMatch(ENode e, String text) {
		if (isOper(e))
			return e.ident == text;
		return false;
	}
	private ENode makeExpr(Operator op, ENode[] result) {
		int pos = this.pos;
		foreach (ENode e; result; e.pos != 0) {
			pos = e.pos;
			if (pos != 0 && isOper(e))
				break;
		}
		if (op.is_type_operator) {
			TypeRef tr;
			if (result[0] instanceof EToken)
				tr = ((EToken)result[0]).asType();
			else
				tr = (TypeRef)result[0];
			if (op == Operator.TypeAccess) {
				EToken id = (EToken)result[2];
				ComplexTypeDecl@ td;
				if (!tr.getTypeDecl().resolveNameR(td,new ResInfo(id,id.ident,ResInfo.noImports|ResInfo.noForwards)))
					return null;
				if (td.package_clazz.dnode != tr.getTypeDecl())
					return null;
				TypeRef ret;
				if (tr instanceof TypeNameRef) {
					ret = new TypeNameRef(tr.ident+"\u001f"+id.ident);
				} else {
					ret = new TypeInnerNameRef(tr.ncopy(),id.ident);
				}
				ret.symbol = (TypeDecl)td;
				ret.pos = id.pos;
				return ret;
			}
			if (op == Operator.PostTypeArgs || op == Operator.PostTypeArgs2) {
				if (tr instanceof TypeNameArgsRef) {
					if (tr.args.length > 0)
						return null;
					TypeNameArgsRef ret = new TypeNameArgsRef(tr.pos,tr.ident,tr.getTypeDecl());
					foreach (ENode e; ((UnresSeqs)result[2]).exprs) {
						if (e instanceof EToken)
							ret.args += e.asType();
						else
							ret.args += ((TypeRef)e).ncopy();
					}
					return ret;
				}
				if (tr instanceof TypeInnerNameRef) {
					if (tr.args.length > 0)
						return null;
					TypeInnerNameRef ret = new TypeInnerNameRef(tr.outer, tr.ident, tr.getTypeDecl());
					foreach (ENode e; ((UnresSeqs)result[2]).exprs) {
						if (e instanceof EToken)
							ret.args += e.asType();
						else
							ret.args += ((TypeRef)e).ncopy();
					}
					return ret;
				}
				if (tr instanceof TypeNameRef) {
					TypeNameArgsRef ret = new TypeNameArgsRef(tr.pos,tr.ident,tr.getTypeDecl());
					foreach (ENode e; ((UnresSeqs)result[2]).exprs) {
						if (e instanceof EToken)
							ret.args += e.asType();
						else
							ret.args += ((TypeRef)e).ncopy();
					}
					return ret;
				}
				return null;
			}
			TypeExpr ret = new TypeExpr(tr.ncopy(), op);
			ret.pos = pos;
			return ret;
		}
		if (op == Operator.Access) {
			TypeRef tr = null;
			if (result[0] instanceof EToken)
				tr = ((EToken)result[0]).asType();
			else if (result[0] instanceof TypeRef)
				tr = (TypeRef)result[0];
			if (tr != null) {
				EToken id = (EToken)result[2];
				ComplexTypeDecl@ td;
				if (tr.getTypeDecl().resolveNameR(td,new ResInfo(id,id.ident,ResInfo.noImports|ResInfo.noForwards))) {
					if (td.package_clazz.dnode == tr.getTypeDecl())
						return null;
				}
			}
		}
		// filter the result[] to convert EToken to TypeRef-s
		OpArg[] op_args = op.args;
		for (int i=0; i < result.length; i++) {
			ENode e = result[i];
			OpArg a = op_args[i];
			if (a instanceof OpArg.TYPE)
				result[i] = asType(e);
		}
		return new UnresOpExpr(pos, op, result);
	}
}


