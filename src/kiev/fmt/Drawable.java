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

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node(copyable=false)
public abstract class Drawable extends ANode {

	public static final Drawable[] emptyArray = new Drawable[0];

	// the node we draw
	private final ANode		_node;
	// syntax kind & draw layout
	@ref
	public SyntaxElem		syntax;
	// definer syntax (SyntaxAttr or SyntaxNode, etc) 
	@ref
	public SyntaxElem		attr_syntax;
	
	@getter
	public ANode get$drnode() {
		return ANode.getVersion(_node);
	}
	
	public Drawable(ANode node, SyntaxElem syntax) {
		this._node = node;
		this.syntax = syntax;
	}
	
	public abstract String getText();

	public abstract void preFormat(DrawContext cont);
	public abstract boolean postFormat(DrawContext cont);
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();
	public abstract int getMaxLayout();

	public final void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.drnode)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
			return;
		}
		this.preFormat(cont);
	}

	public boolean isUnvisible() {
		if (syntax.fmt == null)
			return false;
		return false; //return syntax.fmt.is_hidden;
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


