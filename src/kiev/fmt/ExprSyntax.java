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
public class SyntaxExprTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxExprTemplate;

	@att public SyntaxToken		l_paren;
	@att public SyntaxToken		bad_op;
	@att public SyntaxToken		r_paren;
	@att public SyntaxToken[]	operators;

	public SyntaxExprTemplate() {
		super(new SyntaxNode());
		this.l_paren = new SyntaxToken("(");
		this.bad_op = new SyntaxToken("\u25d9");
		this.r_paren = new SyntaxToken(")");
	}
}


@node
public class SyntaxExpr extends SyntaxElem {
	@virtual typedef This  = SyntaxExpr;

	@att public SyntaxAttr[]					attrs;
	@att public SymbolRef<SyntaxExprTemplate>	template;

	public SyntaxExpr() {
		this.template = new SymbolRef<SyntaxExprTemplate>();
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		if (expected_node != current_node)
			return false;
		if (expected_node instanceof ENode && expected_node.getOp() == null)
			return true;
		return false;
	}
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		if (node instanceof ENode && node.getOp() != null)
			return fmt.getDrawable(node, null, null);
		return new DrawLispExpr(node, this);
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxExprTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved expression template "+template);
			else if (template.symbol != d) {
				template.open();
				template.symbol = d;
			}
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxExprTemplate> vect = new Vector<SyntaxExprTemplate>();
			SyntaxExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node
public class SyntaxAutoParenth extends SyntaxElem {
	@virtual typedef This  = SyntaxAutoParenth;

	@att public SyntaxAttr						attr;
	@att public int								priority;
	@att public SymbolRef<SyntaxExprTemplate>	template;

	public SyntaxAutoParenth() {}
	public SyntaxAutoParenth(SyntaxAttr attr, int priority, SyntaxExprTemplate template) {
		this.attr = attr;
		this.priority = priority;
		this.template = new SymbolRef<SyntaxExprTemplate>(template);
	}

	public boolean check(DrawContext cont, SyntaxElem current_stx, ANode expected_node, ANode current_node) {
		Object obj;
		if (attr instanceof SyntaxNode) {
			obj = expected_node;
		} else try {
			obj = expected_node.getVal(attr.name);
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			if (obj.isPrimaryExpr() || obj.getPriority() < this.priority)
				return expected_node == current_node;
		}
		return attr.check(cont, current_stx, expected_node, current_node);
	}
	
	public Drawable makeDrawable(Formatter fmt, ANode node) {
		Object obj;
		if (attr instanceof SyntaxNode) {
			obj = node;
		} else try {
			obj = node.getVal(attr.name);
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			if (obj.isPrimaryExpr() || obj.getPriority() < this.priority)
				return new DrawAutoParenth(node, this);
		}
		return attr.makeDrawable(fmt, node);
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxExprTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved expression template "+template);
			else if (template.symbol != d) {
				template.open();
				template.symbol = d;
			}
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxExprTemplate> vect = new Vector<SyntaxExprTemplate>();
			SyntaxExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}
