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

import java.awt.Graphics2D;

public interface Formatter {
	public Drawable   format(ANode node, Drawable dr);
	public Drawable   getDrawable(ANode node, Drawable dr, ATextSyntax syntax);
	public ATextSyntax getSyntax();
	public void       setSyntax(ATextSyntax stx);
	public boolean    getShowAutoGenerated();
	public void       setShowAutoGenerated(boolean show);
}

public abstract class AbstractFormatter implements Formatter {

	private static final int counter;

	/** A default syntax to use for formatting */
	public ATextSyntax syntax;
	/** A flag to show auto-generated nodes */
	public boolean		show_auto_generated;

	protected AbstractFormatter(ATextSyntax syntax) {
		this.syntax = syntax;
		String name = "fmt info "+Integer.toHexString(++counter);
		name = name.intern();
	}

	public abstract Drawable format(ANode node, Drawable dr);
	
	public ATextSyntax getSyntax() {
		return this.syntax;
	}
	
	public void setSyntax(ATextSyntax stx) {
		this.syntax = stx;
	}
	
	public boolean    getShowAutoGenerated() {
		return this.show_auto_generated;
	}

	public void setShowAutoGenerated(boolean show) {
		this.show_auto_generated = show;
	}

	public final Drawable getDrawable(ANode node, Drawable dr, ATextSyntax syntax) {
		if (dr != null && dr.drnode == node)
			return dr;
		SyntaxElem stx_elem;
		if (syntax == null)
			syntax = this.syntax;
		stx_elem = syntax.getSyntaxElem(node);
		dr = stx_elem.makeDrawable(this,node);
		return dr;
	}
}

public class TextFormatter extends AbstractFormatter {
	private ATextSyntax syntax;
	
	public TextFormatter(ATextSyntax syntax) {
		super(syntax);
	}

	public Drawable format(ANode node, Drawable dr) {
		DrawContext ctx = new DrawContext(this,null);
		ctx.width = 1000;
		Drawable root = getDrawable(node, dr, null);
		root.preFormat(ctx, root.syntax, node);
		try {
			// link nodes
			ctx = new DrawContext(this,null);
			ctx.width = 1000;
			root.lnkFormat(ctx);
			DrawTerm first = root.getFirstLeaf();
			if (first != null)
				DrawTerm.lnkVerify(first);
			else
				assert(root.getLastLeaf() == null);
		} catch (Throwable t) { t.printStackTrace(); }
		ctx = new DrawContext(this,null);
		ctx.width = 1000;
		root.postFormat(ctx);
		
		int lineno = 1;
		int line_indent = 0;
		int next_indent = line_indent;
		int y = 0;
		DrawTerm first = root.getFirstLeaf();
		DrawTerm line_start = first;
		for (DrawTerm dr=first; dr != null; dr = dr.getNextLeaf()) {
			dr.y = y;
			if (dr.isUnvisible()) {
				dr.w = 0;
				dr.h = 1;
				continue;
			}
			if (dr.do_newline) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.lineno = lineno;
					l.y = y;
					l.h = 1;
					if (l == dr)
						break;
				}
				y += dr.lnk_next.size;
				line_start = dr.getNextLeaf();
				lineno++;
			}
		}
		// fill the rest
		for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
			l.lineno = lineno;
			l.y = y;
			l.h = 1;
		}
		
		return root;
	}
}

public class GfxFormatter extends AbstractFormatter {

	private Graphics2D	gfx;
	private int			width;
	
	public GfxFormatter(ATextSyntax syntax, Graphics2D gfx) {
		super(syntax);
		this.gfx = gfx;
		this.width = 100;
	}
	
	public void setWidth(int w) {
		if (w < 100)
			this.width = 100;
		else
			this.width = w;
	}

	public Drawable format(ANode node, Drawable dr) {
		DrawContext ctx = new DrawContext(this,gfx);
		ctx.width = this.width;
		Drawable root = getDrawable(node, dr, null);
		root.preFormat(ctx, root.syntax, node);
		try {
			// link nodes
			ctx = new DrawContext(this,gfx);
			ctx.width = this.width;
			root.lnkFormat(ctx);
			DrawTerm first = root.getFirstLeaf();
			if (first != null)
				DrawTerm.lnkVerify(first);
			else
				assert(root.getLastLeaf() == null);
		} catch (Throwable t) { t.printStackTrace(); }
		ctx = new DrawContext(this,gfx);
		ctx.width = this.width;
		root.postFormat(ctx);
		
		int lineno = 1;
		int max_h = 10;
		int max_b = 0;
		int line_indent = 0;
		int next_indent = line_indent;
		int y = 0;
		DrawTerm first = root.getFirstLeaf();
		DrawTerm line_start = first;
		for (DrawTerm dr=first; dr != null; dr = dr.getNextLeaf()) {
			dr.y = y;
			if (dr.isUnvisible()) {
				dr.w = 0;
				dr.h = max_h;
				continue;
			}
			max_h = Math.max(max_h, dr.h);
			max_b = Math.max(max_b, dr.b);
			if (dr.do_newline) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.lineno = lineno;
					l.y = y;
					l.h = max_h;
					l.b = max_b;
					if (l == dr)
						break;
				}
				y += max_h + dr.lnk_next.size;
				max_h = 10;
				max_b = 0;
				line_start = dr.getNextLeaf();
				lineno++;
			}
		}
		// fill the rest
		for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
			l.lineno = lineno;
			l.y = y;
			l.h = max_h;
			l.b = max_b;
		}
		
		return root;
	}
}


