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
	private final ANode					_node;
	// syntax kind & draw layout
	public final Draw_SyntaxElem		syntax;
	// syntax, which has produced this drawable, to get
	// sub-nodes in the same syntax
	public final Draw_ATextSyntax		text_syntax;
	
	@getter
	public final ANode get$drnode() {
		return _node;
	}
	
	public Drawable(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		this._node = node;
		this.syntax = syntax;
		this.text_syntax = text_syntax;
	}
	
	public abstract void preFormat(DrawContext cont);
	public abstract Drawable getNextChild(Drawable dr);
	public abstract Drawable getPrevChild(Drawable dr);
	public abstract Drawable[] getChildren();

	public final void preFormat(DrawContext cont, Draw_SyntaxElem expected_stx, ANode expected_node) {
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

	public final DrawTerm getFirstLeaf() {
		if (this.isUnvisible())
			return null;
		if (this instanceof DrawTerm)
			return (DrawTerm)this;
		Drawable[] children = this.getChildren();
		for (int i=0; i < children.length; i++) {
			Drawable ch = children[i];
			if (ch == null || ch.isUnvisible())
				continue;
			DrawTerm d = ch.getFirstLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}
	public final DrawTerm getLastLeaf() {
		if (this.isUnvisible())
			return null;
		if (this instanceof DrawTerm)
			return (DrawTerm)this;
		Drawable[] children = this.getChildren();
		for (int i=children.length-1; i >= 0 ; i--) {
			Drawable ch = children[i];
			if (ch == null || ch.isUnvisible())
				continue;
			DrawTerm d = ch.getLastLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}

	public final DrawTerm getNextLeaf() {
		Drawable p = this;
		for (;;) {
			Drawable parent = (Drawable)p.parent();
			if (parent == null)
				return null;
			p = parent.getNextChild(p);
			if (p == null) {
				p = parent;
				continue;
			}
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
			Drawable parent = (Drawable)p.parent();
			if (parent == null)
				return null;
			p = parent.getPrevChild(p);
			if (p == null) {
				p = parent;
				continue;
			}
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

	public void lnkFormat(DrawLinkContext cont) {
		if (this.isUnvisible())
			return;
		cont.processSpaceBefore(this);
		foreach (Drawable arg; this.getChildren(); arg != null)
			arg.lnkFormat(cont);
		cont.processSpaceAfter(this);
	}

	public final void postFormat(DrawLayoutBlock context) {
		if (this.isUnvisible())
			return;
		if (this instanceof DrawTerm) {
			context.addLeaf(((DrawTerm)this).dt_fmt);
		} else {
			context = context.pushDrawable(this);
			try {
				foreach (Drawable dr; this.getChildren(); dr != null)
					dr.postFormat(context);
			} finally {
				context.popDrawable(this);
			}
		}
	}
}


