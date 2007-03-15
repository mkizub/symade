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
public abstract class DrawNonTerm extends Drawable {
	@att public Drawable	folded;
	@att public Drawable[]	args;
	@att public boolean		draw_folded;
	     public int			max_layout;

	public DrawNonTerm(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	
	public String getText() {
		if (folded != null)
			return folded.getText();
		StringBuffer sb = new StringBuffer();
		foreach (Drawable arg; args)
			sb.append(arg.getText());
		return sb.toString();
	}

	public DrawTerm getFirstLeaf() {
		if (this.isUnvisible())
			return null;
		for (int i=0; i < args.length; i++) {
			DrawTerm d = args[i].getFirstLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}
	public DrawTerm getLastLeaf()  {
		if (this.isUnvisible())
			return null;
		for (int i=args.length-1; i >= 0 ; i--) {
			DrawTerm d = args[i].getLastLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}

	public final int getMaxLayout() { return max_layout; }

	protected final void calcMaxLayout() {
		max_layout = syntax.lout.count;
		if (attr_syntax != null)
			max_layout = Math.max(max_layout, attr_syntax.lout.count);
		foreach (Drawable dr; args; !dr.isUnvisible())
			max_layout = Math.max(max_layout, dr.getMaxLayout());
	}

	public final boolean postFormat(DrawContext context) {
		AParagraphLayout pl = null;
		if (this.syntax instanceof SyntaxList) {
			if (this instanceof DrawWrapList)
				pl = ((SyntaxList)this.syntax).par.dnode;
			else
				pl = ((SyntaxList)this.syntax).elpar.dnode;
		}
		if (pl != null)
			context = context.pushParagraph(this, pl);
		else
			context.pushDrawable(this);
		boolean fits = true;
		try {
			if (max_layout <= 0 || context.new_lines_first_parent) {
				for (int j=0; j < args.length; j++) {
					Drawable dr = args[j];
					if (dr.isUnvisible())
						continue;
					fits &= dr.postFormat(context);
				}
				return fits;
			} else {
				// for each possible layout. assign it to all sub-components
				// and try to layout them;
			next_layout:
				for (int i=0; i <= this.max_layout; i++) {
					context = context.pushState(i, this.max_layout);
					boolean save = false;
					fits = true;
					try {
						boolean last = (i == this.max_layout);
						fits = (context.x < context.width);
						for (int j=0; j < args.length; j++) {
							Drawable dr = args[j];
							if (dr.isUnvisible())
								continue;
							fits &= dr.postFormat(context);
							if (!fits && !last)
								continue next_layout;
						}
						save = true;
					} finally {
						context = context.popState(save); 
					}
					return fits;
				}
			}
		} finally {
			if (pl != null)
				context.popParagraph(this, fits);
			else
				context.popDrawable(this);
		}
		return true;
	}

}

@node(copyable=false)
public final class DrawWrapList extends DrawNonTerm {

	public boolean		draw_empty;
	public AttrSlot		slst_attr;

	public DrawWrapList(ANode node, SyntaxList syntax) {
		super(node, syntax);
		this.draw_folded = syntax.folded_by_default;
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			slst_attr = a;
			break;
		}
		try {
			if (((ANode[])slst_attr.get(node)).length != 0)
				draw_empty = true;
		} catch (RuntimeException e) {}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxList slst = (SyntaxList)this.syntax;
		ANode node = this.drnode;
		
		if (folded == null && slst.folded != null) {
			folded = slst.folded.makeDrawable(cont.fmt, node);
			if (draw_folded) {
				folded.preFormat(cont,slst.folded,node);
				return;
			}
		}
		
		ANode[] narr = null;
		try {
			narr = (ANode[])slst_attr.get(node);
		} catch (RuntimeException e) {
			narr = null;
		}
		if (narr == null || narr.length == 0) {
			if (!draw_empty || args.length == 0) {
				args.delAll();
				if (slst.empty != null) {
					args.append(slst.empty.makeDrawable(cont.fmt, node));
				} else {
					if (slst.prefix != null)
						args.append(slst.prefix.makeDrawable(cont.fmt, node));
					if (slst.sufix != null)
						args.append(slst.sufix.makeDrawable(cont.fmt, node));
				}
				draw_empty = true;
			}
		} else {
			if (draw_empty) {
				args.delAll();
				if (slst.prefix != null)
					args.append(slst.prefix.makeDrawable(cont.fmt, node));
				args.append(new DrawNonTermList(this.drnode,(SyntaxList)this.syntax));
				if (slst.sufix != null)
					args.append(slst.sufix.makeDrawable(cont.fmt, node));
				draw_empty = false;
			}
		}

		int x = 0;
		if (narr.length == 0) {
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
		calcMaxLayout();
	}
	
	public int getInsertIndex(Drawable dr) {
		assert (dr.parent() == this);
		SyntaxList slst = (SyntaxList)this.syntax;
		if (slst.prefix != null && args[0] == dr)
			return 0;
		try {
			return ((ANode[])slst_attr.get(this.drnode)).length;
		} catch (RuntimeException e) {}
		return 0;
	}
}

@node(copyable=false)
public final class DrawNonTermList extends DrawNonTerm {

	private	ANode[] oarr;
	public AttrSlot slst_attr;

	public DrawNonTermList(ANode node, SyntaxList syntax) {
		super(node, syntax);
		this.draw_folded = syntax.folded_by_default;
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			slst_attr = a;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxList slst = (SyntaxList)this.syntax;
		ANode node = this.drnode;
		
		if (folded == null && slst.folded != null) {
			folded = slst.folded.makeDrawable(cont.fmt, node);
			if (draw_folded) {
				folded.preFormat(cont,slst.folded,node);
				return;
			}
		}
		
		ANode[] narr = (ANode[])oarr;
		try {
			oarr = (ANode[])slst_attr.get(node);
		} catch (RuntimeException e) {
			oarr = new ANode[0];
		}
		if (narr != oarr) {
			narr = (ANode[])oarr;
			int sz = narr.length;
			Drawable[] old_args = args.delToArray();
			boolean need_sep = false;
	next_node:
			for (int i=0; i < sz; i++) {
				ANode n = narr[i];
				if (n instanceof ASTNode && n.isAutoGenerated())
					continue;
				if (slst.filter != null && !slst.filter.calc(n))
					continue;
				if (need_sep && slst.separator != null)
					args.append(slst.separator.makeDrawable(cont.fmt, null));
				foreach (Drawable dr; old_args; dr.drnode == n) {
					args.append(dr);
					need_sep = true;
					continue next_node;
				}
				args.append(slst.element.makeDrawable(cont.fmt, n));
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
		calcMaxLayout();
	}
	
	public int getInsertIndex(Drawable dr) {
		assert (dr.parent() == this);
		if (oarr.length == 0)
			return 0;
		SyntaxList slst = (SyntaxList)this.syntax;
		if (slst.separator != null) {
			for (int i=0; i < args.length; i++) {
				if (args[i] == dr)
					return (1+i)/2;
			}
		} else {
			for (int i=0; i < args.length; i++) {
				if (args[i] == dr)
					return i;
			}
		}
		return oarr.length;
	}
}

@node(copyable=false)
public final class DrawNonTermSet extends DrawNonTerm {

	public DrawNonTermSet(ANode node, SyntaxSet syntax) {
		super(node, syntax);
		this.draw_folded = syntax.folded_by_default;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxSet sset = (SyntaxSet)this.syntax;
		ANode node = this.drnode;

		if (folded == null && sset.folded != null)
			folded = sset.folded.makeDrawable(cont.fmt, node);
		if (folded != null)
			folded.preFormat(cont,sset.folded,node);
			
		if (!draw_folded || folded == null) {
			if (args.length != sset.elements.length) {
				args.delAll();
				foreach (SyntaxElem se; sset.elements)
					args.append(se.makeDrawable(cont.fmt, node));
			}
			for (int i=0; i < args.length; i++) {
				Drawable dr = args[i];
				dr.preFormat(cont,sset.elements[i],node);
			}
		}
		calcMaxLayout();
	}
}


