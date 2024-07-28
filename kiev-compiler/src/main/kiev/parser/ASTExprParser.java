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

/**
 * @author Maxim Kizub
 *
 */

public interface ASTToken extends ANode {
	public boolean isIdentifier();
	public boolean isOperator();
	public boolean isMaybeOper();
	public String  getTokenText();
	public TypeRef asType(Env env);
	public DNode   asScope(Env env);
	public ENode   asExpr(Env env);
}

public final class ASTExprParser {

	final static class OpdefEnumerator implements Enumeration<COpdef> {
		private final COpdef[] opdefs;
		private final int priority;
		private final boolean type_operator;
		private final int min_args;
		private final boolean not_left_recursive;
		private int pos;
		private COpdef curr;
		OpdefEnumerator(COpdef[] opdefs, int priority, boolean type_operator, int min_args, boolean not_left_recursive) {
			this.opdefs = opdefs;
			this.priority = priority;
			this.type_operator = type_operator;
			this.min_args = min_args;
			this.not_left_recursive = not_left_recursive;
		}
		public boolean hasMoreElements() {
			if (curr != null)
				return true;
			scanToNext();
			return curr != null;
		}
		public COpdef nextElement() {
			if (curr != null) {
				COpdef ret = curr;
				curr = null;
				return ret;
			}
			scanToNext();
			if (curr == null)
				throw new NoSuchElementException();
			return curr;
		}
		private void scanToNext() {
			while(pos < opdefs.length) {
				COpdef opd = opdefs[pos++];
				if (opd.prior >= priority && opd.type_operator == type_operator && min_args >= opd.min_args) {
					if (not_left_recursive && opd.left_recursive)
						continue;
					curr = opd;
					return;
				}
			}
		}
	}
	
	final Env env;
	final COpdef[] opdefs;
	final ANode[]  nodes;
	int cur_pos;
	
	public ASTExprParser(Env env, COpdef[] opdefs, ANode[] nodes) {
		this.env = env;
		this.opdefs = opdefs;
		this.nodes = nodes;
	}

	int restLength() {
		return this.nodes.length - this.cur_pos;
	}
	ANode[] makeOpArgs(COpdef opd) {
		ANode[] result = new ANode[opd.args.length];
		return result;
	}
	ANode[] makeOpArgs(COpdef opd, ANode prev) {
		ANode[] result = new ANode[opd.args.length];
		result[0] = prev;
		return result;
	}
	ANode currNode() { this.nodes[this.cur_pos] }
	ANode nextNode() { this.nodes[this.cur_pos+1] }
	ANode push() { this.nodes[this.cur_pos++] }
	void pop() { this.cur_pos -= 1; }
	
	Enumeration<COpdef> iterateFirst(int priority, boolean tp_op) {
		return new OpdefEnumerator(opdefs, priority, tp_op, restLength(), true);
	}
	Enumeration<COpdef> iterateNext(int priority, boolean tp_op) {
		return new OpdefEnumerator(opdefs, priority, tp_op, restLength()+1, false);
	}

	public List<ENode> parseExpr() {
		List<ENode> results = List.Nil;
		ENode@ result;
		trace( Kiev.debug && Kiev.debugOperators, "\n\n\nParsing expr: "+this);
		foreach( resolveExpr(result,0) ) {
			ENode res = result;
			if (this.cur_pos == nodes.length) {
				trace( Kiev.debug && Kiev.debugOperators, "Add possible resolved expression: "+result);
				results = new List.Cons<ENode>(res,results);
			} else {
				trace( Kiev.debug && Kiev.debugOperators, "Incomplete possible resolved expression: "+res);
			}
		}
		return results;
	}

	public List<ENode> parseType() {
		List<ENode> results = List.Nil;
		ENode@ result;
		trace( Kiev.debug && Kiev.debugOperators, "\n\n\nParsing type: "+this);
		foreach( resolveType(result) ) {
			ENode res = result;
			if (this.cur_pos == nodes.length) {
				trace( Kiev.debug && Kiev.debugOperators, "Add possible resolved expression: "+result);
				results = new List.Cons<ENode>(res,results);
			} else {
				trace( Kiev.debug && Kiev.debugOperators, "Incomplete possible resolved expression: "+res);
			}
		}
		return results;
	}

	private rule resolveExpr(ANode@ ret, int priority)
		ANode@			expr;
		COpdef@			opd;
		ANode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 0 restLength "+this.restLength()+" priority "+priority),
		this.restLength() > 0,
		{
			// left recursion ( E <= E ... )
			!isDefinitelyOper(this.currNode()),
			getPriority(this.currNode()) >= priority,
			expr ?= asExpr(this.currNode()),
			trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 1 left "+expr),
			this.push() : this.pop()
		;	// E <= ... E
			this.restLength() > 0,
			opd @= this.iterateFirst(priority,false),
			matchOpStart(opd),
			trace( Kiev.debug && Kiev.debugOperators, "resolveExpr: 2 for "+opd),
			opArgs = this.makeOpArgs(opd),
			resolveOpArg(opd.args, 0, opArgs),
			expr ?= makeExpr(opd,opArgs)
		},
		resolveExprNext(ret, expr, priority)
	}
	
	private rule resolveExprNext(ANode@ ret, ANode prev, int priority)
		ANode@			expr;
		COpdef@			opd;
		ANode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 0 restLength "+this.restLength()+" priority "+priority+" prev: "+prev),
		this.restLength() > 0,
		opd @= this.iterateNext(priority,false),
		matchOpStart(opd,prev),
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 1 for "+prev+" "+opd),
		opArgs = this.makeOpArgs(opd,prev),
		resolveOpArg(opd.args, 1, opArgs),
		expr ?= makeExpr(opd,opArgs),
		resolveExprNext(ret, expr, priority)
	;
		trace( Kiev.debug && Kiev.debugOperators, "resolveExprNext: 2 res "+prev),
		ret ?= prev
	}
	
	private rule resolveType(ANode@ ret)
		ANode@			expr;
		COpdef@			opd;
		ANode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveType: 0 restLength "+this.restLength()),
		this.restLength() > 0,
		{
			// left-recursion ( T <= T ... )
			expr ?= asType(this.currNode()),
			$cut,
			trace( Kiev.debug && Kiev.debugOperators, "resolveType: 1 left "+expr),
			this.push() : this.pop()
		;	// T <= ... T
			this.restLength() > 0,
			opd @= this.iterateFirst(255,true),
			matchOpStart(opd),
			trace( Kiev.debug && Kiev.debugOperators, "resolveType: 2 for "+opd),
			opArgs = this.makeOpArgs(opd),
			resolveOpArg(opd.args, 0, opArgs),
			expr ?= makeExpr(opd,opArgs)
		},
		resolveTypeNext(ret, expr)
	}
	
	private rule resolveTypeNext(ANode@ ret, ANode prev)
		ANode@			expr;
		COpdef@			opd;
		ANode[]			opArgs;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 0 restLength "+this.restLength()+" prev: "+prev),
		this.restLength() > 0,
		opd @= this.iterateNext(0,true),
		matchTypeOpStart(opd,prev),
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 1 for "+prev+" "+opd),
		opArgs = this.makeOpArgs(opd,prev),
		resolveOpArg(opd.args, 1, opArgs),
		expr ?= makeExpr(opd,opArgs),
		resolveTypeNext(ret, expr)
	;
		ret ?= asType(prev),
		trace( Kiev.debug && Kiev.debugOperators, "resolveTypeNext: 2 res "+prev)
	}
	
	private rule resolveOpArg(COpArgument[] args, int pos, ANode[] result)
		ANode@ expr;
		ANode[] opArgs;
		SeqRes seq_res;
	{
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 0 for pos "+pos+" as "+(args.length == pos? "END" : args[pos])),
		pos > 0 && args.length == pos,
		$cut,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 1 return OK")
	;
		this.restLength() > 0,
		{
			args[pos] instanceof COpArgEXPR,
			resolveExpr(expr, ((COpArgEXPR)args[pos]).prior),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 3 added "+expr),
			result[pos] = expr
		;
			args[pos] instanceof COpArgTYPE,
			resolveType(expr),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 5 added "+expr),
			result[pos] = expr
		;
			args[pos] instanceof COpArgIDNT,
			isIdent(this.currNode()),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 6 added "+this.currNode()),
			result[pos] = this.push() : this.pop()
		;
			args[pos] instanceof COpArgOPER,
			isOperMatch(this.currNode(), (COpArgOPER)args[pos]),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 7 added "+this.currNode()),
			result[pos] = makeExpr(this.push(),(COpArgOPER)args[pos]) : this.pop()
		;
			args[pos] instanceof COpArgNODE,
			((COpArgNODE)args[pos]).clazz.isInstance(this.currNode()),
			trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: 8 added "+this.currNode()),
			result[pos] = this.push() : this.pop()
		},
		resolveOpArg(args, pos+1, result)
	;
		args[pos] instanceof COpArgLIST,
		trace( Kiev.debug && Kiev.debugOperators, "resolveOpArg: seq "+args[pos]),
		seq_res = parseSeq((COpArgLIST)args[pos]) : this.cur_pos = seq_res.save_cur_pos,
		seq_res.exprs != null,
		((COpArgLIST)args[pos]).min_count <= seq_res.exprs.length,
		result[pos] = new UnresSeqs(args[pos],seq_res.exprs),
		resolveOpArg(args, pos+1, result)
	}
	
	class SeqRes {
		ANode[]	exprs;
		int save_cur_pos;
	}
	private SeqRes parseSeq(COpArgLIST seq) {
		SeqRes res = new SeqRes();
		res.save_cur_pos = this.cur_pos;
		Stack<ANode> seq_stk = new Stack<ANode>();
		if (resolveLongestSeqEl(seq.el, seq_stk)) {
			trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 0 add "+seq_stk.peek());
			if (seq.sep == null) {
				while (this.nodes.length > this.cur_pos && resolveLongestSeqEl(seq.el, seq_stk)) {
					trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 2 add "+seq_stk.peek());
				}
			} else {
				while (this.nodes.length > this.cur_pos && isOperMatch(this.currNode(), seq.sep)) {
					trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 1 add "+seq.sep);
					this.cur_pos += 1; // each separator
					if (resolveLongestSeqEl(seq.el, seq_stk)) {
						trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 2 add "+seq_stk.peek());
						continue;
					}
					trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 3 failed ");
					this.cur_pos = res.save_cur_pos;
					return res; // if there was the separator - there must be the next expression
				}
			}
		}
		res.exprs = seq_stk.toArray();
		trace( Kiev.debug && Kiev.debugOperators, "parseSeq: 4 success found "+res.exprs.length);
		return res;
	}

	private boolean resolveLongestSeqEl(COpArgument seq_el, Stack<ANode> seq_stk) {
		if (this.restLength() == 0)
			return false;
		int pos = this.cur_pos;
		ANode res = null;
		ANode@ en;
		foreach (resolveSeqEl(seq_el, en)) {
			if (this.cur_pos > pos) {
				res = (ANode)en;
				pos = this.cur_pos;
			}
		}
		if (res == null)
			return false;
		seq_stk.push(res);
		this.cur_pos = pos;
		return true;
	}

	private rule resolveSeqEl(COpArgument seq_el, ANode@ res)
		ANode[]			opArgs;
	{
		seq_el instanceof COpArgEXPR,
		resolveExpr(res,((COpArgEXPR)seq_el).prior)
	;
		seq_el instanceof COpArgTYPE,
		resolveType(res)
	;
		seq_el instanceof COpArgIDNT,
		isIdent(this.currNode()),
		res = this.push() : this.pop()
	;
		seq_el instanceof COpArgOPER,
		isOperMatch(this.currNode(), (COpArgOPER)seq_el),
		res = makeExpr(this.push(),(COpArgOPER)seq_el) : this.pop()
	;
		seq_el instanceof COpArgSEQS,
		opArgs = new ANode[((COpArgSEQS)seq_el).args.length],
		resolveOpArg(((COpArgSEQS)seq_el).args, 0, opArgs),
		res ?= new UnresSeqs(seq_el, opArgs)
	;
		seq_el instanceof COpArgNODE,
		((COpArgNODE)seq_el).clazz.isInstance(this.currNode()),
		res = this.push() : this.pop()
	}

	private boolean matchOpStart(COpdef opd) {
		ANode en = this.currNode();
		COpArgument arg0 = opd.args[0];
		if (arg0 instanceof COpArgOPER) {
			return isOperMatch(en, (COpArgOPER)arg0);
		}
		if (arg0 instanceof COpArgTYPE) {
			if (asType(en) != null)
				return true;
			return false;
		}
		if (arg0 instanceof COpArgIDNT) {
			if (!isIdent(en))
				return false;
		}
		else if (arg0 instanceof COpArgEXPR) {
			if (isDefinitelyOper(en)) return false;
			if (getPriority(en) < arg0.prior) return false;
		}
		else
			return false;
		if (opd.args.length > 1 && this.restLength() > 1 && opd.args[1] instanceof COpArgOPER) {
			en = this.nextNode();
			if (!isOperMatch(en, (COpArgOPER)opd.args[1]))
				return false;
		}
		return true;
	}
	private boolean matchOpStart(COpdef opd, ANode prev) {
		ANode en = this.currNode();
		COpArgument arg0 = opd.args[0];
		if (arg0 instanceof COpArgEXPR) {
			if (arg0.prior > getPriority(prev)) return false;
		}
		else if (arg0 instanceof COpArgTYPE) {
			if !(prev instanceof TypeRef) return false;
		}
		else
			return false;
		COpArgument arg1 = opd.args[1];
		if (arg1 instanceof COpArgOPER) return isOperMatch(en, (COpArgOPER)arg1);
		if (arg1 instanceof COpArgTYPE) return (asType(en) != null);
		if (arg1 instanceof COpArgEXPR) return !isDefinitelyOper(en) && getPriority(en) >= arg1.prior;
		return false;
	}
	private boolean matchTypeOpStart(COpdef opd, ANode prev) {
		assert (opd.type_operator);
		assert (this.restLength() >= opd.min_args-1);
		assert (prev instanceof TypeRef);
		ANode en = this.currNode();
		COpArgument arg1 = opd.args[1];
		if (arg1 instanceof COpArgOPER) return isOperMatch(en, (COpArgOPER)arg1);
		if (arg1 instanceof COpArgTYPE) return (asType(en) != null);
		if (arg1 instanceof COpArgIDNT) return isIdent(en);
		return false;
	}
	private boolean isIdent(ANode e) {
		return (e instanceof ASTToken && e.isIdentifier());
	}
	private TypeRef asType(ANode e) {
		if (e == null)
			return null;
		if (e instanceof TypeRef)
			return (TypeRef)e;
		if (e instanceof ASTToken)
			return e.asType(env);
		return null;
	}
	private ENode asExpr(ANode e) {
		if (e == null)
			return null;
		if (e instanceof ENode)
			return (ENode)e;
		if (e instanceof ASTToken)
			return e.asExpr(env);
		return null;
	}
	private boolean isDefinitelyOper(ANode e) {
		return (e instanceof ASTToken && e.isOperator());
	}
	private boolean isMaybeOper(ANode e) {
		return (e instanceof ASTToken && e.isMaybeOper());
	}
	private boolean isOperMatch(ANode e, COpArgOPER oper) {
		return isMaybeOper(e) && ((ASTToken)e).getTokenText() == oper.text;
	}
	private int getPriority(ANode n) {
		if (n instanceof ENode)
			return n.getPriority(env);
		return 255;
	}
	private ANode makeExpr(ANode e, COpArgOPER oper) {
		if (oper.clazz != null)
			return (ANode) oper.clazz.newInstance();
		return e;	
	}
	private ENode makeExpr(COpdef opd, ANode[] result) {
		int pos = 0;
		foreach (ASTNode e; result; e.pos != 0) {
			pos = e.pos;
			if (pos != 0 && isDefinitelyOper(e))
				break;
		}
		if (opd.args.length == 1 && opd.args[0] instanceof COpArgOPER) {
			ENode ret = (ENode)Class.forName(opd.source.as_node).newInstance();
			ret.pos = pos;
			COpArgOPER arg = (COpArgOPER)opd.args[0];
			if (arg.source.attr_name != null) {
				ScalarPtr pattr = Env.getScalarPtr(ret,arg.source.attr_name);
				Class clazz = pattr.slot.typeinfo.clazz;
				if (clazz == Boolean.TYPE || clazz == Boolean.class)
					pattr.set(Boolean.valueOf(arg.text));
				else
					pattr.set(arg.text);
			}
			return ret;
		}
		if (opd.type_operator)
			return new UnresOpTypeRef(pos, opd, result);
		return new UnresOpExpr(pos, opd, result);
	}
}