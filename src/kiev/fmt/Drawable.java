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

import syntax kiev.Syntax;


@ThisIsANode(copyable=false)
public abstract class Drawable extends ANode {

	public static final Drawable[] emptyArray = new Drawable[0];

	// the node we draw
	private final ANode			_node;
	// syntax kind & draw layout
	public final SyntaxElem		syntax;
	// syntax, which has produced this drawable, to get
	// sub-nodes in the same syntax
	public final ATextSyntax	text_syntax;
	
	@getter
	public final ANode get$drnode() {
		return _node;
	}
	
	public Drawable(ANode node, SyntaxElem syntax, ATextSyntax text_syntax) {
		this._node = node;
		this.syntax = syntax;
		this.text_syntax = text_syntax;
	}
	
	public abstract String getText();

	public abstract void preFormat(DrawContext cont);
	public abstract void lnkFormat(DrawLinkContext cont);
	public abstract void postFormat(DrawLayoutBlock cont);
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();
	public abstract int getMaxLayout();

	public final void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, this, expected_node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node, text_syntax);
			if (!this.isAttached())
				throw new ChangeRootException(dr);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
			return;
		}
		this.preFormat(cont);
	}

	public boolean isUnvisible() {
		return false;
	}  

	public final DrawTerm getNextLeaf() {
		Drawable p = this;
		for (;;) {
			while (ANode.getNextNode(p) == null && (p=(Drawable)p.parent()) != null);
			if (p == null)
				return null;
			p = (Drawable)ANode.getNextNode(p);
			if (p.isUnvisible())
				continue;
			DrawTerm d = p.getFirstLeaf();
			if (d == null)
				continue;
			if (!d.isUnvisible())
				return d;
			p = d;
		}
	}
	public final DrawTerm getPrevLeaf() {
		Drawable p = this;
		for (;;) {
			while (ANode.getPrevNode(p) == null && (p=(Drawable)p.parent()) != null);
			if (p == null)
				return null;
			p = (Drawable)ANode.getPrevNode(p);
			if (p.isUnvisible())
				continue;
			DrawTerm d = p.getLastLeaf();
			if (d == null)
				continue;
			if (!d.isUnvisible())
				return d;
			p = d;
		}
	}
}


