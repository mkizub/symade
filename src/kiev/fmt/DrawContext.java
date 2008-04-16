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
	
	public final Formatter				fmt;
	public final Graphics2D				gfx;
	public final Font					default_font;
	private int						width;
	private int						x, y, max_x;
	private boolean					parent_has_more_attempts;
	private boolean					line_started;
	private DrawContext				prev_ctx;
	
	public DrawContext(Formatter fmt, Graphics2D gfx, int width) {
		this.fmt = fmt;
		this.gfx = gfx;
		this.width = width;
		line_started = true;
		if (gfx != null)
			default_font = new Font("Dialog", Font.PLAIN, 12);
	}
	
	public Object clone() {
		return super.clone();
	}

	private DrawContext pushState() {
		DrawContext ctx = (DrawContext)this.clone();
		ctx.prev_ctx = this;
		ctx.max_x = this.x;
		return ctx;
	}
	
	private DrawContext popState(boolean save) {
		DrawContext ctx = prev_ctx;
		if (save) {
			ctx.x = this.x;
			ctx.y = this.y;
			ctx.max_x = this.max_x;
			ctx.line_started = this.line_started;
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

	public void postFormat(DrawLayoutBlock dlb, int indent) {
		AParagraphLayout pl = dlb.par;
		if (pl != null) {
			int pl_indent = gfx==null ? pl.indent_text_size : pl.indent_pixel_size;
			if (pl.indent_from_current_position) {
				indent = pl_indent + this.x;
			} else {
				indent += pl_indent;
			}
		}
		DrawContext ctx = null;
	next_layot:
		for (int i=0; i <= dlb.max_layout; i++) {
			ctx = this.pushState();
			boolean last = (i >= dlb.max_layout);
			if (!last)
				ctx.parent_has_more_attempts = true;
			foreach (DrawLayoutBlock b; dlb.blocks) {
				if (b.dr instanceof DrawTerm)
					ctx.addLeaf((DrawTerm)b.dr, i, indent);
				else
					ctx.postFormat(b, indent);
				if (ctx.max_x >= ctx.width) {
					if (this.parent_has_more_attempts) {
						// overflow, try parent's next layout
						break next_layot;
					}
					else if (!last) {
						// overflow, try our's next layout
						continue next_layot;
					}
					else if (last) {
						// overflow, there are no more layouts in the parent and in this block
						ctx.popState(true);
						this.max_x = ctx.width - 1; // erase the overflow
						continue;
					}
				}
			}
			break;
		}
		ctx.popState(true);
		return;
	}

	public void addLeaf(DrawTerm leaf, int cur_attempt, int indent) {
		flushSpaceRequests(leaf, cur_attempt, indent);
		leaf.x = x;
		x += leaf.w;
		max_x = Math.max(max_x, x);
		line_started = false;
	}
	
	private void flushSpaceRequests(DrawTerm leaf, int cur_attempt, int indent) {
		DrawTermLink lnk = leaf.lnk_prev;
		if (lnk == null) {
			this.x = indent;
			return;
		}
		
		int max_space;
		int max_nl;
		if (cur_attempt == 0) {
			max_space = (lnk.size_0 & 0xFFFF);
			max_nl = (lnk.size_0 >>> 16);
		} else {
			max_space = (lnk.size_1 & 0xFFFF);
			max_nl = (lnk.size_1 >>> 16);
		}
		if (this.line_started)
			this.x = indent;
		else
			this.x += max_space;
		if (max_nl > 0) {
			lnk.the_size = max_nl;
			lnk.do_newline = true;
			if (!this.line_started) {
				this.line_started = true;
				this.x = indent;
			}
		} else {
			lnk.sp_nl_size = max_space;
			lnk.do_newline = false;
		}
	}
}

public final class DrawLinkContext {
	
	private final boolean					gfx;
	private final Vector<LayoutSpace>		space_infos = new Vector<LayoutSpace>();
	private final Vector<LayoutSpace>		space_infos_1 = new Vector<LayoutSpace>();
	private boolean							update_spaces;
	
	public DrawLinkContext(boolean gfx) {
		this.gfx = gfx;
	}
	
	public void requestSpacesUpdate() {
		update_spaces = true;
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
				max_nl = Math.max(gfx ? csi.text_size : csi.pixel_size, max_nl);
			else
				max_space = Math.max(gfx ? csi.text_size : csi.pixel_size, max_space);
		}
		lnk.size_0 = (max_nl << 16) | (max_space & 0xFFFF);
		space_infos.removeAllElements();

		max_space = 0;
		max_nl = 0;
		foreach (LayoutSpace csi; space_infos_1; !csi.eat) {
			if (csi.new_line)
				max_nl = Math.max(gfx ? csi.text_size : csi.pixel_size, max_nl);
			else
				max_space = Math.max(gfx ? csi.text_size : csi.pixel_size, max_space);
		}
		lnk.size_1 = (max_nl << 16) | (max_space & 0xFFFF);
		space_infos_1.removeAllElements();
		
		lnk.sp_nl_size = 0;
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

@node(copyable=false)
public final class DrawLayoutBlock extends ANode {

	public static final DrawLayoutBlock[] emptyArray = new DrawLayoutBlock[0];

	@att
	DrawLayoutBlock[]	blocks;
	@ref
	AParagraphLayout	par;
	@ref
	Drawable			dr;
	int					max_layout;
	
	public DrawLayoutBlock pushDrawable(Drawable dr) {
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
	public DrawLayoutBlock popDrawable(Drawable dr) {
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
				this = popParagraph(dr, pl.dnode);
		}
		return this;
	}
	
	private DrawLayoutBlock pushParagraph(Drawable dp, AParagraphLayout pl) {
		if (pl.enabled(dp)) {
			DrawLayoutBlock dlb = new DrawLayoutBlock();
			this.blocks += dlb;
			dlb.par = pl;
			dlb.dr = dp;
			return dlb;
		}
		return this;
	}
	private DrawLayoutBlock popParagraph(Drawable dp, AParagraphLayout pl) {
		if (this.dr == dp) {
			assert (pl.enabled(dp));
			int max_layout = 0;
			foreach (DrawLayoutBlock b; blocks)
				max_layout = Math.max(max_layout, b.max_layout);
			return (DrawLayoutBlock)parent();
		}
		assert (!pl.enabled(dp));
		return this;
	}
	
	public void addLeaf(DrawTerm leaf) {
		DrawLayoutBlock dlb = new DrawLayoutBlock();
		this.blocks += dlb;
		dlb.dr = leaf;
		if (leaf.syntax != null && leaf.syntax.par != null && leaf.syntax.par.dnode != null)
			dlb.par = leaf.syntax.par.dnode;
		dlb.max_layout = leaf.syntax.lout.count;
	}
	
}

