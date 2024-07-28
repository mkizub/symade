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

import kiev.fmt.common.*;

public interface StyleProvider {
}

@ThisIsANode(copyable=false)
public abstract class Drawable extends ANode {

	public static final Drawable[] emptyArray = new Drawable[0];

	// the node we draw
	public final INode					drnode;
	// syntax kind & draw layout
	public final Draw_SyntaxElem		syntax;
	// block layout info
	@UnVersioned
	public       DrawLayoutInfo         dr_dli;
	
	public Drawable(INode node, Formatter fmt, Draw_SyntaxElem syntax) {
		super(node.handle(), fmt.formatter_context);
		this.drnode = node;
		this.syntax = syntax;
	}
	
	public void callbackChanged(NodeChangeInfo info) {
		if (info.ct == ChangeType.THIS_DETACHED) {
			handle().delData(this);
		}
		else if (info.ct == ChangeType.THIS_ATTACHED) {
			foreach (AHandleData nh; handle().getHandleData(); nh == this)
				goto call_super;
			handle().addData(this);
		}
	call_super:
		super.callbackChanged(info);
	}

	public abstract void preFormat(Formatter fmt);
	public abstract Drawable[] getChildren();

	public final Drawable getNextChild(Drawable dr) {
		Drawable[] children = this.getChildren();
		if (dr == null || children == null)
			return null;
		for (int i=0; i < children.length; i++) {
			if (children[i] == dr) {
				for (i++; i < children.length; i++) {
					if (children[i] != null)
						return children[i];
				}
				return null;
			}
		}
		return null;
	}
	public final Drawable getPrevChild(Drawable dr) {
		Drawable[] children = this.getChildren();
		if (dr == null || children == null)
			return null;
		for (int i=children.length-1; i >= 0; i--) {
			if (children[i] == dr) {
				for (i--; i >= 0; i--) {
					if (children[i] != null)
						return children[i];
				}
				return null;
			}
		}
		return null;
	}

	public final void preFormat(Formatter fmt, Draw_SyntaxElem expected_stx, INode expected_node) {
		if (!expected_stx.check(fmt, this, expected_node)) {
			Drawable dr = expected_stx.makeDrawable(fmt, expected_node);
			if (!this.isAttached())
				throw new ChangeRootException(dr);
			replaceWithNode(dr, this.parent(), this.pslot());
			dr.preFormat(fmt, expected_stx, expected_node);
			return;
		}
		this.preFormat(fmt);
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

	public final void postFormat(DrawLayoutInfo context, Formatter fmt) {
		if (this.isUnvisible())
			return;
		if (this instanceof DrawTerm) {
			context.addLeaf((DrawTerm)this);
			((DrawTerm)this).formatTerm(fmt);
		} else {
			context = context.pushDrawable(this);
			try {
				foreach (Drawable dr; this.getChildren(); dr != null)
					dr.postFormat(context, fmt);
			} finally {
				context.popDrawable(this);
			}
		}
	}
}


