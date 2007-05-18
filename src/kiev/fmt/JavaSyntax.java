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
package kiev.fmt;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import syntax kiev.Syntax;


@node
public class SyntaxJavaExprTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxJavaExprTemplate;

	@att public SyntaxToken		l_paren;
	@att public SyntaxToken		bad_op;
	@att public SyntaxToken		r_paren;
	@att public SyntaxToken[]	operators;

	public SyntaxJavaExprTemplate() {
		super(new SyntaxNode());
		this.l_paren = new SyntaxToken("(");
		this.bad_op = new SyntaxToken("\u25d9");
		this.r_paren = new SyntaxToken(")");
	}
}

@node
public class SyntaxJavaCommentTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxJavaCommentTemplate;

	@att public SyntaxElem		newline;
	@att public SyntaxElem		lin_beg;
	@att public SyntaxElem		doc_beg;
	@att public SyntaxElem		cmt_beg;
	@att public SyntaxElem		cmt_end;

}

@node
public class SyntaxJavaExpr extends SyntaxAttr {
	@virtual typedef This  = SyntaxJavaExpr;

	@att public int									idx;
	@att public int									priority;
	@att public SymbolRef<SyntaxJavaExprTemplate>	template;

	public SyntaxJavaExpr() {
		super("");
		this.idx = -1;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(0,"java-expr-template");
	}
	public SyntaxJavaExpr(String name, int priority, SyntaxJavaExprTemplate template) {
		super(name);
		this.idx = -1;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(template);
	}
	public SyntaxJavaExpr(int idx, int priority, SyntaxJavaExprTemplate template) {
		super("");
		this.idx = idx;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxJavaExprTemplate>(template);
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		if (idx >= 0)
			return (((ENode)expected_node).getArgs()[idx] == current_node);
		String name = this.name;
		if (name == "") {
			if (expected_node != current_node)
				return false;
			if (((ENode)current_node).getOp() != null)
				return false;
			return true;
		}
		if (name == "this")
			return (expected_node == current_node);
		return (expected_node.getVal(name) == current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		ANode n;
		if (idx >= 0) {
			n = ((ENode)node).getArgs()[idx];
		}
		else if (name == "") {
			Drawable dr = new DrawJavaLispExpr(node, this);
			return dr;
		}
		else {
			n = (name == "this") ? node : (ANode)node.getVal(name);
		}
		Drawable dr = new DrawJavaExpr(n, this);
		return dr;
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxJavaExprTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved java expression template "+template);
			else if (template.symbol != d) {
				template = template.open();
				template.symbol = d;
			}
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaExprTemplate> vect = new Vector<SyntaxJavaExprTemplate>();
			SyntaxJavaExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public class SyntaxJavaAccessExpr extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccessExpr;

	@att public SyntaxElem			obj_elem;
	@att public SyntaxToken			separator;
	@att public SyntaxElem			fld_elem;

	public SyntaxJavaAccessExpr() {}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaAccessExpr(node, this);
		return dr;
	}
}

@node
public class SyntaxJavaAccess extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccess;

	public SyntaxJavaAccess() {}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaAccess(node, this);
		return dr;
	}
}

@node
public class SyntaxJavaPackedField extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaPackedField;

	public SyntaxJavaPackedField() {}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaPackedField(node, this);
		return dr;
	}
}


@node
public class SyntaxJavaComment extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaComment;

	@att public SymbolRef<SyntaxJavaCommentTemplate>	template;

	public SyntaxJavaComment() {
		this.template = new SymbolRef<SyntaxJavaCommentTemplate>();
	}

	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Drawable dr = new DrawJavaComment(node, this);
		return dr;
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxJavaCommentTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved java expression template "+template);
			else if (template.symbol != d) {
				template.open();
				template.symbol = d;
			}
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaCommentTemplate> vect = new Vector<SyntaxJavaCommentTemplate>();
			SyntaxJavaCommentTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public class KievTextSyntax extends ATextSyntax {
	@virtual typedef This  = KievTextSyntax;

	final Hashtable<Pair<Operator,Class>, SyntaxElem> exprs;
	
	private SyntaxSet expr(Operator op, SyntaxExpr sexpr)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new SyntaxAutoParenth(sexpr.attrs[earg], priority, sexpr.template.dnode);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new SyntaxAutoParenth(sexpr.attrs[earg], 255, sexpr.template.dnode);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (sexpr.template.dnode != null) {
					foreach (SyntaxToken t; sexpr.template.dnode.operators) {
						if (t.text == text) {
							elems[i] = t.ncopy();
							break;
						}
						if (t.text == "DEFAULT") {
							SyntaxToken st = t.ncopy();
							st.text = text;
							elems[i] = st;
						}
					}
				}
				if (elems[i] == null) {
					SyntaxToken st = new SyntaxToken(text);
					st.kind = SyntaxToken.TokenKind.OPERATOR;
					elems[i] = st;
				}
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		return set;
	}

	private SyntaxSet expr(Operator op, SyntaxJavaExprTemplate template)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new SyntaxJavaExpr(earg, priority, template);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new SyntaxJavaExpr(earg, 255, template);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (template != null) {
					foreach (SyntaxToken t; template.operators) {
						if (t.text == text) {
							elems[i] = t.ncopy();
							break;
						}
						if (t.text == "DEFAULT") {
							SyntaxToken st = t.ncopy();
							st.text = text;
							elems[i] = st;
						}
					}
				}
				if (elems[i] == null)
					elems[i] = new SyntaxToken(text);
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		return set;
	}

	public KievTextSyntax() {
		exprs = new Hashtable<Pair<Operator,Class>, SyntaxElem>();
	}

	protected void cleanup() {
		exprs.clear();
		super.cleanup();
	}

	public SyntaxElem getSyntaxElem(ANode node) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null) {
				SyntaxElem se = sed.elem;
				if (node instanceof ENode && se instanceof SyntaxExpr) {
					ENode e = (ENode)node;
					Operator op = e.getOp();
					if (op == null)
						return se;
					se = exprs.get(new Pair<Operator,Class>(op,node.getClass()));
					if (se == null) {
						se = expr(op, (SyntaxExpr)sed.elem);
						exprs.put(new Pair<Operator,Class>(op,node.getClass()), se);
					}
					return se;
				}
				if (node instanceof ENode && se instanceof SyntaxJavaExpr && se.name == "") {
					ENode e = (ENode)node;
					Operator op = e.getOp();
					if (op == null)
						return se;
					se = exprs.get(new Pair<Operator,Class>(op,node.getClass()));
					if (se == null) {
						se = expr(op,(SyntaxJavaExprTemplate)((SyntaxJavaExpr)sed.elem).template.dnode);
						exprs.put(new Pair<Operator,Class>(op,node.getClass()), se);
					}
				}
				return se;
			}
		}
		return super.getSyntaxElem(node);
	}
}

