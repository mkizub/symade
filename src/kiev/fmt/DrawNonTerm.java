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
public abstract class DrawNonTerm extends Drawable {
	@nodeAttr public Drawable∅		args;

	public DrawNonTerm(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}
	
	public Drawable getNextChild(Drawable dr) {
		assert (dr.parent() == this && dr.pslot().name == "args");
		return (Drawable)ANode.getNextNode(dr);
	}
	public Drawable getPrevChild(Drawable dr) {
		assert (dr.parent() == this && dr.pslot().name == "args");
		return (Drawable)ANode.getPrevNode(dr);
	}
	public Drawable[] getChildren() {
		return args;
	}
}

@ThisIsANode(copyable=false)
public final class DrawElemWrapper extends Drawable {

	public boolean					draw_empty;

	@nodeAttr public Drawable		empty;
	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable		sufix;
	@nodeAttr public Drawable		element;
	
	public DrawElemWrapper(ANode node, Draw_SyntaxElemWrapper syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxElemWrapper stx = (Draw_SyntaxElemWrapper)this.syntax;
		ANode node = this.drnode;
		
		if (empty == null && stx.empty != null)
			empty = stx.empty.makeDrawable(cont.fmt, node, text_syntax);
		if (prefix == null && stx.prefix != null)
			prefix = stx.prefix.makeDrawable(cont.fmt, node, text_syntax);
		if (sufix == null && stx.sufix != null)
			sufix = stx.sufix.makeDrawable(cont.fmt, node, text_syntax);
		if (element == null && stx.element != null)
			element = stx.element.makeDrawable(cont.fmt, node, text_syntax);

		if (element != null)
			element.preFormat(cont, stx.element, node);

		if (empty != null)
			empty.preFormat(cont,stx.empty,node);
		if (prefix != null)
			prefix.preFormat(cont,stx.prefix,node);
		if (sufix != null)
			sufix.preFormat(cont,stx.sufix,node);

		draw_empty = true;
		if (element == null)
			return;
		if (stx.element instanceof Draw_SyntaxAttr) {
			Draw_SyntaxAttr sa = (Draw_SyntaxAttr)stx.element;
			AttrSlot slot = sa.attr_slot;
			if (slot == null) {
				foreach (AttrSlot a; node.values(); a.name == sa.name) {
					slot = a;
					break;
				}
			}
			if (slot instanceof ScalarAttrSlot) {
				if (slot.get(node) == null)
					return;
			}
			if (slot instanceof SpaceAttrSlot) {
				if (slot.getArray(node).length == 0)
					return;
			}
			if (slot instanceof ExtSpaceAttrSlot) {
				if (!slot.iterate(node).hasMoreElements())
					return;
			}
		}
		foreach (Drawable d; element.getChildren(); d != null && !d.isUnvisible()) {
			draw_empty = false;
			break;
		}
	}
	
	public Drawable getNextChild(Drawable dr) {
		if (dr == empty || dr == sufix)
			return null;
		if (dr == element)
			return sufix;
		if (dr == prefix)
			return draw_empty ? sufix : element;
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		if (dr == empty || dr == prefix)
			return null;
		if (dr == element)
			return prefix;
		if (dr == sufix)
			return draw_empty ? prefix : element;
		return null;
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

	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable∅		args;
	@nodeAttr public Drawable		sufix;

	public final AttrSlot slst_attr;

	public DrawNonTermList(ANode node, Draw_SyntaxList syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name && (a instanceof SpaceAttrSlot || a instanceof ExtSpaceAttrSlot)) {
			slst_attr = a;
			break;
		}
	}
	
	public Drawable getNextChild(Drawable dr) {
		if (dr == prefix) {
			if (args.length > 0)
				return args[0];
			return sufix;
		}
		if (dr.pslot().name == "args") {
			if (dr != args[args.length-1])
				return (Drawable)ANode.getNextNode(dr);
			return sufix;
		}
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		if (dr == sufix) {
			if (args.length > 0)
				return args[args.length-1];
			return prefix;
		}
		if (dr.pslot().name == "args") {
			if (dr != args[0])
				return (Drawable)ANode.getPrevNode(dr);
			return prefix;
		}
		return null;
	}
	public Drawable[] getChildren() {
		if (prefix == null && sufix == null)
			return args;
		Drawable[] ret = new Drawable[args.length+2];
		int i = 0;
		ret[i++] = prefix;
		foreach (Drawable dr; args)
			ret[i++] = dr;
		ret[i++] = sufix;
		return ret;
	}

	static ANode[] getDrawableElements(Draw_SyntaxList slst, AttrSlot slst_attr, ANode parent, boolean show_auto_gen) {
		Vector<ANode> nodes = new Vector<ANode>();
		if (slst_attr instanceof SpaceAttrSlot) {
			foreach (ANode n; slst_attr.getArray(parent)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !show_auto_gen)
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				nodes.append(n);
			}
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			foreach (ANode n; slst_attr.iterate(parent)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !show_auto_gen)
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				nodes.append(n);
			}
		}
		return nodes.toArray();
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		ANode node = this.drnode;

		if (prefix == null && slst.prefix != null)
			prefix = slst.prefix.makeDrawable(cont.fmt, node, text_syntax);
		if (sufix == null && slst.sufix != null)
			sufix = slst.sufix.makeDrawable(cont.fmt, node, text_syntax);

		ANode[] narr = getDrawableElements(slst,slst_attr,node,cont.fmt.getShowAutoGenerated());
		if (narr == null || narr.length == 0) {
			args.delAll();
			if (slst.empty != null) {
				prefix = null;
				sufix = null;
				args.append(slst.empty.makeDrawable(cont.fmt, node, text_syntax));
				args[0].preFormat(cont,slst.empty,node);
			} else {
				if (prefix == null && slst.prefix != null)
					prefix = slst.prefix.makeDrawable(cont.fmt, node, text_syntax);
				if (sufix == null && slst.sufix != null)
					sufix = slst.sufix.makeDrawable(cont.fmt, node, text_syntax);
				if (prefix != null)
					prefix.preFormat(cont,slst.prefix,node);
				if (sufix != null) 
					sufix.preFormat(cont,slst.sufix,node);
			}
		} else {
			if (prefix == null && slst.prefix != null)
				prefix = slst.prefix.makeDrawable(cont.fmt, node, text_syntax);
			if (sufix == null && slst.sufix != null)
				sufix = slst.sufix.makeDrawable(cont.fmt, node, text_syntax);

			Drawable[] old_args = args.delToArray();
			boolean need_sep = false;
		next_node:
			foreach (ANode n; getDrawableElements(slst,slst_attr,node,cont.fmt.getShowAutoGenerated())) {
				if (need_sep && slst.separator != null)
					args.append(slst.separator.makeDrawable(cont.fmt, node, text_syntax));
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					need_sep = true;
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n, text_syntax));
				need_sep = true;
			}

			if (prefix != null)
				prefix.preFormat(cont,slst.prefix,node);
			int x = 0;
			int n = args.length;
			if (slst.separator != null) {
				for (; x < n; x++) {
					if ((x & 1) == 0)
						args[x].preFormat(cont,slst.element,args[x].drnode);
					else
						args[x].preFormat(cont,slst.separator,node);
				}
			} else {
				for (; x < n; x++)
					args[x].preFormat(cont,slst.element,args[x].drnode);
			}
			if (sufix != null) 
				sufix.preFormat(cont,slst.sufix,node);
		}
	}
	
	public int getInsertIndex(Drawable dr, boolean next) {
		assert (dr.parent() == this);
		if (dr == prefix)
			return 0;
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		if (slst.separator != null) {
			for (int i=0; i < args.length; i++) {
				if (args[i] == dr)
					return next ? (i+2)/2 : (i+1)/2;
			}
			return next ? (args.length+2)/2 : (args.length+1)/2;
		} else {
			for (int i=0; i < args.length; i++) {
				if (args[i] == dr)
					return next ? (i+1) : (i) ;
			}
			return next ? args.length + 1 : args.length;
		}
	}
}

@ThisIsANode(copyable=false)
public final class DrawNonTermSet extends DrawNonTerm {

	public DrawNonTermSet(ANode node, Draw_SyntaxSet syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSet sset = (Draw_SyntaxSet)this.syntax;
		ANode node = this.drnode;

		if (args.length != sset.elements.length) {
			args.delAll();
			foreach (Draw_SyntaxElem se; sset.elements)
				args.append(se.makeDrawable(cont.fmt, node, text_syntax));
		}
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(cont,sset.elements[i],node);
		}
	}
}


@ThisIsANode(copyable=false)
public class DrawSyntaxSwitch extends Drawable {
	
	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable		element;
	@nodeAttr public Drawable		suffix;
	
	public DrawSyntaxSwitch(ANode node, Draw_SyntaxSwitch syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public Drawable getNextChild(Drawable dr) {
		if (dr == prefix)
			return element;
		if (dr == element)
			return suffix;
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		if (dr == suffix)
			return element;
		if (dr == element)
			return prefix;
		return null;
	}
	public Drawable[] getChildren() {
		return new Drawable[]{prefix,element,suffix};
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSwitch ssw = (Draw_SyntaxSwitch)this.syntax;
		ANode node = this.drnode;
		
		if (prefix == null && ssw.prefix != null)
			prefix = ssw.prefix.makeDrawable(cont.fmt, node, text_syntax);
		if (element == null)
			element = cont.fmt.getDrawable(node, null, ssw.target_syntax);
		if (suffix == null && ssw.suffix != null)
			suffix = ssw.suffix.makeDrawable(cont.fmt, node, text_syntax);

		if (prefix != null)  prefix.preFormat(cont,ssw.prefix,node);
		if (element != null) element.preFormat(cont,element.syntax,node);
		if (suffix != null)  suffix.preFormat(cont,ssw.suffix,node);
	}
}

@ThisIsANode(copyable=false)
public final class DrawTreeBranch extends Drawable {

	@nodeAttr public Drawable		folded;
	@nodeAttr public Drawable∅		args;
	          public boolean		draw_folded;
	          public AttrSlot		slst_attr;

	public DrawTreeBranch(ANode node, Draw_SyntaxTreeBranch syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.draw_folded = true;
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			slst_attr = a;
			break;
		}
	}

	// for GUI
	public Drawable getFolded() { this.folded }
	// for GUI
	public Drawable[] getSubNodes() { this.args }
	// for GUI
	public boolean getDrawFolded() { this.draw_folded }
	// for GUI
	public void setDrawFolded(boolean val) { this.draw_folded = val; }

	public Drawable getNextChild(Drawable dr) {
		if (dr == folded) {
			Drawable[] args = this.args;
			if (args.length > 0)
				return args[0];
			return null;
		}
		else if (dr.pslot().name == "args")
			return (Drawable)ANode.getNextNode(dr);
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		if (dr.pslot().name == "args") {
			Drawable p = (Drawable)ANode.getPrevNode(dr);
			if (p != null)
				return p;
			return folded;
		}
		return null;
	}
	public Drawable[] getChildren() {
		Drawable[] args = this.args;
		Drawable[] ret = new Drawable[args.length+1];
		ret[0] = folded;
		for (int i=0; i < args.length; i++)
			ret[i+1] = args[i];
		return ret;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxTreeBranch slst = (Draw_SyntaxTreeBranch)this.syntax;
		ANode node = this.drnode;
		
		if (folded == null && slst.folded != null)
			folded = slst.folded.makeDrawable(cont.fmt, node, text_syntax);

		folded.preFormat(cont,slst.folded,node);
		if (draw_folded) {
			this.args.delAll();
			return;
		}
		
		if (slst_attr instanceof SpaceAttrSlot) {
			ANode[] narr = ((SpaceAttrSlot)slst_attr).getArray(node);
			int sz = narr.length;
			Drawable[] old_args = args.delToArray();
	next_node:
			for (int i=0; i < sz; i++) {
				ANode n = narr[i];
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n, text_syntax));
			}
			foreach (Drawable arg; this.args)
				arg.preFormat(cont,slst.element,arg.drnode);
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			Drawable[] old_args = args.delToArray();
	next_node:
			foreach (ANode n; ((ExtSpaceAttrSlot)slst_attr).iterate(node)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n, text_syntax));
			}
			foreach (Drawable arg; this.args)
				arg.preFormat(cont,slst.element,arg.drnode);
		}
		else {
			Draw_SyntaxSet sset = (Draw_SyntaxSet)slst.element;
			ANode node = this.drnode;
			if (args.length != sset.elements.length) {
				args.delAll();
				foreach (Draw_SyntaxElem se; sset.elements)
					args.append(se.makeDrawable(cont.fmt, node, text_syntax));
			}
			for (int i=0; i < args.length; i++)
				args[i].preFormat(cont,sset.elements[i],node);
		}
	}
}

