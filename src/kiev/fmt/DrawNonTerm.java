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
	
	public String getText() {
		StringBuffer sb = new StringBuffer();
		foreach (Drawable arg; args)
			sb.append(arg.getText());
		return sb.toString();
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
public final class DrawWrapList extends DrawNonTerm {

	public boolean				draw_empty;
	public final AttrSlot		slst_attr;

	public DrawWrapList(ANode node, Draw_SyntaxList syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name && a instanceof SpaceAttrSlot || a instanceof ExtSpaceAttrSlot) {
			slst_attr = a;
			break;
		}
		if (slst_attr instanceof SpaceAttrSlot) {
			if (((SpaceAttrSlot)slst_attr).getArray(node).length != 0)
				draw_empty = true;
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			if (((ExtSpaceAttrSlot)slst_attr).iterate(node).hasMoreElements())
				draw_empty = true;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		ANode node = this.drnode;
		
		boolean empty = true;
		if (slst_attr instanceof SpaceAttrSlot) {
			foreach (ANode n; ((SpaceAttrSlot)slst_attr).getArray(node)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				empty = false;
				break;
			}
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			foreach (ANode n; ((ExtSpaceAttrSlot)slst_attr).iterate(node)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				empty = false;
				break;
			}
		}
		if (empty) {
			if (!draw_empty || args.length == 0) {
				args.delAll();
				if (slst.empty != null) {
					args.append(slst.empty.makeDrawable(cont.fmt, node, text_syntax));
				} else {
					if (slst.prefix != null)
						args.append(slst.prefix.makeDrawable(cont.fmt, node, text_syntax));
					if (slst.sufix != null)
						args.append(slst.sufix.makeDrawable(cont.fmt, node, text_syntax));
				}
				draw_empty = true;
			}
		} else {
			if (draw_empty) {
				args.delAll();
				if (slst.prefix != null)
					args.append(slst.prefix.makeDrawable(cont.fmt, node, text_syntax));
				args.append(new DrawNonTermList(this.drnode,(Draw_SyntaxList)this.syntax,this.text_syntax));
				if (slst.sufix != null)
					args.append(slst.sufix.makeDrawable(cont.fmt, node, text_syntax));
				draw_empty = false;
			}
		}

		int x = 0;
		if (empty) {
			if (args.length > 0) {
				if (slst.empty != null) {
					args[x].preFormat(cont,slst.empty,node);
				} else {
					if (slst.prefix != null)
						args[x++].preFormat(cont,slst.prefix,node);
					if (slst.sufix != null)
						args[x++].preFormat(cont,slst.sufix,node);
				}
			}
		} else {
			if (slst.prefix != null)
				args[x++].preFormat(cont,slst.prefix,node);
			args[x++].preFormat(cont);
			if (slst.sufix != null)
				args[x++].preFormat(cont,slst.sufix,node);
		}
	}
	
	public int getInsertIndex(Drawable dr, boolean next) {
		assert (dr.parent() == this);
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		boolean first = (slst.prefix != null && args[0] == dr);
		if (first && !next)
			return 0;
		if (slst_attr instanceof SpaceAttrSlot) {
			return ((SpaceAttrSlot)slst_attr).getArray(this.drnode).length;
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			int sz = 0;
			foreach (ANode n; ((ExtSpaceAttrSlot)slst_attr).iterate(this.drnode))
				sz++;
			return sz;
		}
		return 0;
	}
}

@ThisIsANode(copyable=false)
public final class DrawNonTermList extends DrawNonTerm {

	public final AttrSlot slst_attr;

	public DrawNonTermList(ANode node, Draw_SyntaxList syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name && a instanceof SpaceAttrSlot || a instanceof ExtSpaceAttrSlot) {
			slst_attr = a;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxList slst = (Draw_SyntaxList)this.syntax;
		ANode node = this.drnode;
		
		if (slst_attr instanceof SpaceAttrSlot) {
			ANode[] narr = ((SpaceAttrSlot)slst_attr).getArray(node);
			int sz = narr.length;
			Drawable[] old_args = args.delToArray();
			boolean need_sep = false;
	next_node:
			for (int i=0; i < sz; i++) {
				ANode n = narr[i];
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				if (need_sep && slst.separator != null)
					args.append(slst.separator.makeDrawable(cont.fmt, null, text_syntax));
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					need_sep = true;
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n, text_syntax));
				need_sep = true;
			}
		}
		else if (slst_attr instanceof ExtSpaceAttrSlot) {
			Drawable[] old_args = args.delToArray();
			boolean need_sep = false;
	next_node:
			foreach (ANode n; ((ExtSpaceAttrSlot)slst_attr).iterate(node)) {
				if (n instanceof ASTNode && n.isAutoGenerated() && !cont.fmt.getShowAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				if (need_sep && slst.separator != null)
					args.append(slst.separator.makeDrawable(cont.fmt, null, text_syntax));
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					need_sep = true;
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n, text_syntax));
				need_sep = true;
			}
		}

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
	}
	
	public int getInsertIndex(Drawable dr, boolean next) {
		assert (dr.parent() == this);
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
		//for (int i=0; i < oarr.length; i++) {
		//	if (oarr[i] == dr.drnode)
		//		return next ? (i+1) : (i) ;
		//}
		//return oarr.length;
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

	public String getText() {
		StringBuffer sb = new StringBuffer();
		if (prefix != null)  sb.append(prefix.getText());
		if (element != null) sb.append(element.getText());
		if (suffix != null)  sb.append(suffix.getText());
		return sb.toString();
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

	public String getText() {
		if (folded != null)
			return folded.getText();
		StringBuffer sb = new StringBuffer();
		foreach (Drawable arg; args)
			sb.append(arg.getText());
		return sb.toString();
	}

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

