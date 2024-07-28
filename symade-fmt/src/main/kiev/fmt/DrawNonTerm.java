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

@ThisIsANode(copyable=false)
public abstract class DrawNonTerm extends Drawable {
	@nodeAttr public Drawable∅		args;

	public DrawNonTerm(INode node, Formatter fmt, Draw_SyntaxElem syntax) {
		super(node, fmt, syntax);
	}
	
	public Drawable[] getChildren() {
		return args;
	}
}

@ThisIsANode(copyable=false)
public final class DrawElemWrapper extends Drawable {

	public boolean					draw_empty;
	public final AttrSlot			sub_attr;

	@nodeAttr public Drawable		empty;
	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable		sufix;
	@nodeAttr public Drawable		element;
	
	public DrawElemWrapper(INode node, Formatter fmt, Draw_SyntaxElemWrapper syntax) {
		super(node, fmt, syntax);
		Draw_SyntaxAttr sa = syntax.element;
		if (sa.attr_slot != null) {
			sub_attr = sa.attr_slot;
		} else {
			foreach (AttrSlot a; node.values(); a.name == sa.name) {
				sub_attr = a;
				break;
			}
		}
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxElemWrapper stx = (Draw_SyntaxElemWrapper)this.syntax;
		Draw_SyntaxAttr astx = stx.element;
		INode node = this.drnode;
		
		if (empty == null && astx.empty != null)
			empty = astx.empty.makeDrawable(fmt, node);
		if (prefix == null && astx.prefix != null)
			prefix = astx.prefix.makeDrawable(fmt, node);
		if (sufix == null && astx.sufix != null)
			sufix = astx.sufix.makeDrawable(fmt, node);
		if (element == null)
			element = stx.element.makeDrawable(fmt, node);
		
		if (prefix != null)
			prefix.preFormat(fmt,astx.prefix,node);
		if (element != null)
			element.preFormat(fmt, stx.element, node);
		if (empty != null)
			empty.preFormat(fmt,astx.empty,node);
		if (sufix != null)
			sufix.preFormat(fmt,astx.sufix,node);

		draw_empty = true;
		AttrSlot sub_attr = this.sub_attr;
		if (sub_attr instanceof ScalarAttrSlot) {
			Object val = sub_attr.get(node);
			if (val == null)
				return;
			if (val instanceof Symbol && val.sname == null)
				return;
			if (val instanceof SymbolRef && val.name == null)
				return;
		}
		if (sub_attr instanceof ASpaceAttrSlot) {
			if (sub_attr.isEmpty(node))
				return;
		}
		draw_empty = false;
	}
	
	public Drawable[] getChildren() {
		if (draw_empty) {
			if (empty != null)
				return new Drawable[]{empty};
			return new Drawable[]{prefix, sufix};
		}
		return new Drawable[]{prefix, element, sufix};
	}

	public int getInsertIndex(Drawable dr, boolean next) {
		assert (dr.parent() == this);
		if (dr == prefix || dr == empty)
			return 0;
		return Integer.MAX_VALUE;
	}
}

@ThisIsANode(copyable=false)
public final class DrawNonTermList extends Drawable {

	@nodeAttr public Drawable∅		args;

	public final AttrSlot slst_attr;

	public DrawNonTermList(INode node, Formatter fmt, Draw_SyntaxList syntax) {
		super(node, fmt, syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name && a instanceof ASpaceAttrSlot) {
			slst_attr = a;
			break;
		}
	}
	
	public Drawable[] getChildren() {
		return args;
	}

	static INode[] getDrawableElements(Draw_SyntaxList slst, AttrSlot slst_attr, INode parent) {
		Vector<INode> nodes = new Vector<INode>();
		if (slst_attr instanceof ASpaceAttrSlot) {
			foreach (INode n; slst_attr.iterate(parent)) {
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				nodes.append(n);
			}
		}
		return nodes.toArray();
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		INode node = this.drnode;

		INode[] narr = getDrawableElements(slst,slst_attr,node);
		if (narr == null || narr.length == 0) {
			args.delAll();
		} else {
			Drawable[] old_args = args.delToArray();
		next_node:
			foreach (INode n; narr) {
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					continue next_node;
				}
				args.append(slst.element.makeDrawable(fmt, n));
			}

			int x = 0;
			int n = args.length;
			for (; x < n; x++)
				args[x].preFormat(fmt,slst.element,args[x].drnode);
		}
	}
	
	public int getInsertIndex(Drawable dr, boolean next) {
		assert (dr.parent() == this);
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		for (int i=0; i < args.length; i++) {
			Drawable el = args[i];
			if (el == dr)
				return next ? (i+1) : (i);
			if (el instanceof DrawNode) {
				if (el.prefix == dr)
					return i-1;
				if (el.arg == dr)
					return next ? (i+1) : (i);
				if (el.sufix == dr)
					return i+1;
			}
		}
		return next ? args.length + 1 : 0;
	}
}

@ThisIsANode(copyable=false)
public final class DrawNonTermSet extends DrawNonTerm {

	public DrawNonTermSet(INode node, Formatter fmt, Draw_SyntaxSet syntax) {
		super(node, fmt, syntax);
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSet sset = (Draw_SyntaxSet)this.syntax;
		INode node = this.drnode;

		if (args.length != sset.elements.length) {
			args.delAll();
			foreach (Draw_SyntaxElem se; sset.elements)
				args.append(se.makeDrawable(fmt, node));
		}
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(fmt,sset.elements[i],node);
		}
	}
}


@ThisIsANode(copyable=false)
public class DrawSyntaxSwitch extends Drawable {
	
	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable		element;
	@nodeAttr public Drawable		suffix;
	
	public DrawSyntaxSwitch(INode node, Formatter fmt, Draw_SyntaxSwitch syntax) {
		super(node, fmt, syntax);
	}

	public Drawable[] getChildren() {
		return new Drawable[]{prefix,element,suffix};
	}
	
	public Drawable getElem() {
		return element;
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSwitch ssw = (Draw_SyntaxSwitch)this.syntax;
		INode node = this.drnode;
		
		fmt.pushSyntax(ssw.target_syntax);
		try {
			if (prefix == null && ssw.prefix != null)
				prefix = ssw.prefix.makeDrawable(fmt, node);
			if (element == null)
				element = fmt.getDrawable(node, null);
			if (suffix == null && ssw.suffix != null)
				suffix = ssw.suffix.makeDrawable(fmt, node);

			if (prefix != null)  prefix.preFormat(fmt,ssw.prefix,node);
			if (element != null) element.preFormat(fmt,element.syntax,node);
			if (suffix != null)  suffix.preFormat(fmt,ssw.suffix,node);
		} finally {
			fmt.popSyntax(ssw.target_syntax);
		}

	}
}

@ThisIsANode(copyable=false)
public final class DrawTreeBranch extends Drawable {

	@nodeAttr public Drawable		folded;
	@nodeAttr public Drawable∅		args;
	          public boolean		draw_folded;
	          public AttrSlot		slst_attr;

	public DrawTreeBranch(INode node, Formatter fmt, Draw_SyntaxTreeBranch syntax) {
		super(node, fmt, syntax);
		this.draw_folded = true;
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			slst_attr = a;
			break;
		}
	}

	// for GUI
	public Drawable[] getSubNodes() { this.args }
	// for GUI
	public boolean getDrawFolded() { this.draw_folded }
	// for GUI
	public void setDrawFolded(boolean val) { this.draw_folded = val; }

	public Drawable[] getChildren() {
		Drawable[] args = this.args;
		Drawable[] ret = new Drawable[args.length+1];
		ret[0] = folded;
		for (int i=0; i < args.length; i++)
			ret[i+1] = args[i];
		return ret;
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxTreeBranch slst = (Draw_SyntaxTreeBranch)this.syntax;
		INode node = this.drnode;
		
		if (folded == null && slst.folded != null)
			folded = slst.folded.makeDrawable(fmt, node);

		folded.preFormat(fmt,slst.folded,node);
		if (draw_folded) {
			this.args.delAll();
			return;
		}
		
		if (slst_attr instanceof ASpaceAttrSlot) {
			Drawable[] old_args = args.delToArray();
	next_node:
			foreach (INode n; ((ASpaceAttrSlot)slst_attr).iterate(node)) {
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					continue next_node;
				}
				args.append(slst.element.makeDrawable(fmt, n));
			}
			foreach (Drawable arg; this.args)
				arg.preFormat(fmt,slst.element,arg.drnode);
		} else {
			Draw_SyntaxSet sset = (Draw_SyntaxSet)slst.element;
			INode node = this.drnode;
			if (args.length != sset.elements.length) {
				args.delAll();
				foreach (Draw_SyntaxElem se; sset.elements)
					args.append(se.makeDrawable(fmt, node));
			}
			for (int i=0; i < args.length; i++)
				args[i].preFormat(fmt,sset.elements[i],node);
		}
	}
}

