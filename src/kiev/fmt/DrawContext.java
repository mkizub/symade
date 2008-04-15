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
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public final class DrawContext implements Cloneable {
	
	public Formatter				fmt;
	public Graphics2D				gfx;
	public int						width;
	public int						x, y;
	public int						cur_attempt, max_attempt;
	public boolean					new_lines_first_parent;
	public boolean					line_started;
	public boolean					update_spaces;
	public Vector<LayoutSpace>		space_infos;
	public Vector<LayoutSpace>		space_infos_1;
	public int						indent;
	private DrawTerm				last_term;
	private DrawContext				prev_ctx;
	private Font					default_font;
	
	public DrawContext(Formatter fmt, Graphics2D gfx) {
		this.fmt = fmt;
		this.gfx = gfx;
		line_started = true;
		if (gfx != null)
			default_font = new Font("Dialog", Font.PLAIN, 12);
	}
	
	public Object clone() {
		return super.clone();
	}

	public DrawContext pushState(int cur_attempt, int max_attempt) {
		DrawContext ctx = (DrawContext)this.clone();
		ctx.prev_ctx = this;
		ctx.cur_attempt = cur_attempt;
		ctx.max_attempt = max_attempt;
		return ctx;
	}
	
	public DrawContext popState(boolean save) {
		DrawContext ctx = prev_ctx;
		if (save) {
			ctx.x = this.x;
			ctx.y = this.y;
			ctx.line_started = this.line_started;
			ctx.last_term = this.last_term;
		}
		return ctx;
	}

	public void formatAsText(DrawTerm dr) {
		dr.x = 0;
		dr.y = 0;
		dr.h = 0;
		String text = dr.getText();
		if (gfx != null) {
			if (text == null) text = "\u25d8"; // ◘
			if (text != null && text.length() != 0) {
				Font  font  = dr.syntax.lout.font;
				TextLayout tl = new TextLayout(text, font, gfx.getFontRenderContext());
				Rectangle2D rect = tl.getBounds();
				dr.w = (int)Math.ceil(tl.getAdvance());
				dr.h = (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
				dr.b = (int)Math.ceil(tl.getAscent()+tl.getLeading());
			} else {
				dr.w = 0;
				dr.h = 10;
				dr.b = 0;
			}
		} else {
			if (text == null) text = "";
			dr.w = text.length();
			dr.h = 1;
			dr.b = 0;
		}
		this.x += dr.w;
	}

	public DrawContext pushDrawable(Drawable dr) {
		SymbolRef<AParagraphLayout> pl = null;
		if (dr.syntax != null) {
			if (dr.syntax instanceof SyntaxList) {
				if (dr instanceof DrawWrapList)
					pl = dr.syntax.par;
				else
					pl = ((SyntaxList)dr.syntax).elpar;
			} else {
				pl = dr.syntax.par;
			}
			if (pl != null && pl.dnode != null)
				this = pushParagraph(dr, pl.dnode);
		}
		return this;
	}
	public DrawContext popDrawable(Drawable dr, boolean save) {
		SymbolRef<AParagraphLayout> pl = null;
		if (dr.syntax != null) {
			if (dr.syntax instanceof SyntaxList) {
				if (dr instanceof DrawWrapList)
					pl = dr.syntax.par;
				else
					pl = ((SyntaxList)dr.syntax).elpar;
			} else {
				pl = dr.syntax.par;
			}
			if (pl != null && pl.dnode != null)
				this = popParagraph(dr, pl.dnode, save);
		}
		return this;
	}
	
	private DrawContext pushParagraph(Drawable dp, AParagraphLayout pl) {
		DrawContext ctx;
		if (pl.new_lines_first_parent)
			ctx = pushState(0,0);
		else
			ctx = pushState(cur_attempt,max_attempt);
		if (pl.enabled(dp)) {
			int indent = gfx==null ? pl.indent_text_size : pl.indent_pixel_size;
			if (pl.indent_from_current_position && last_term != null)
				indent += last_term.x + last_term.w;
			else
				indent += this.indent;
			ctx.indent = indent;
			ctx.new_lines_first_parent = pl.new_lines_first_parent;
		}
		return ctx;
	}
	private DrawContext popParagraph(Drawable dp, AParagraphLayout pl, boolean save) {
		return popState(save);
	}
	
	public boolean addLeaf(DrawTerm leaf) {
		flushSpaceRequests(leaf);
		leaf.x = x;
		x += leaf.w;
		line_started = false;
		last_term = leaf;
		return (x < width);
	}
	
	private void flushSpaceRequests(DrawTerm leaf) {
		DrawTermLink lnk = leaf.lnk_prev;
		if (lnk == null) {
			this.x = indent;
			return;
		}
		
		int max_space;
		int max_nl;
		if (cur_attempt == 0) {
			max_space = lnk.space_size_0;
			max_nl = lnk.newline_size_0;
		} else {
			max_space = lnk.space_size_1;
			max_nl = lnk.newline_size_1;
		}
		if (this.line_started)
			this.x = indent;
		else
			this.x += max_space;
		if (max_nl > 0) {
			lnk.size = max_nl;
			if (last_term != null) {
				last_term.do_newline = true;
			}
			if (!this.line_started) {
				this.line_started = true;
				this.x = indent;
			}
		} else {
			lnk.size = max_space;
		}
	}
	
	// 
	// calculate the space for DrawTermLink
	//
	
	public void flushSpace(DrawTermLink lnk) {
		update_spaces = false;
		if (lnk == null) {
			space_infos.removeAllElements();
			space_infos_1.removeAllElements();
			return;
		}
		int max_space = 0;
		int max_nl = 0;
		foreach (LayoutSpace csi; space_infos; !csi.eat) {
			if (csi.new_line)
				max_nl = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_nl);
			else
				max_space = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_space);
		}
		lnk.space_size_0 = max_space;
		lnk.newline_size_0 = max_nl;
		space_infos.removeAllElements();

		max_space = 0;
		max_nl = 0;
		foreach (LayoutSpace csi; space_infos_1; !csi.eat) {
			if (csi.new_line)
				max_nl = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_nl);
			else
				max_space = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_space);
		}
		lnk.space_size_1 = max_space;
		lnk.newline_size_1 = max_nl;
		space_infos_1.removeAllElements();
	}

	private void collectSpaceInfo(LayoutSpace sc, Vector<LayoutSpace> space_infos) {
		String name = sc.name;
		for (int i=0; i < space_infos.size(); i++) {
			LayoutSpace csi = space_infos[i];
			if (csi.name == name) {
				if (csi.eat)
					return;
				if (sc.eat) {
					space_infos.removeElementAt(i);
					i--;
				}
			}
		}
		space_infos.append(sc);
	}
	
	public void processSpaceBefore(Drawable dr) {
		if (space_infos == null ) space_infos = new Vector<LayoutSpace>();
		if (space_infos_1 == null ) space_infos_1 = new Vector<LayoutSpace>();
		if (!update_spaces)
			return;
		foreach (LayoutSpace si; dr.syntax.lout.spaces_before) {
			if (si.from_attempt <= 0)
				collectSpaceInfo(si,space_infos);
			if (si.from_attempt <= 1)
				collectSpaceInfo(si,space_infos_1);
		}
	}
	
	public void processSpaceAfter(Drawable dr) {
		if (!update_spaces)
			return;
		foreach (LayoutSpace si; dr.syntax.lout.spaces_after) {
			if (si.from_attempt <= 0)
				collectSpaceInfo(si,space_infos);
			if (si.from_attempt <= 1)
				collectSpaceInfo(si,space_infos_1);
		}
	}
	
}


