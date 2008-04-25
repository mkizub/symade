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
	private boolean						parent_has_more_attempts;
	private int							width;
	private int							x;
	private int							max_x;
	private boolean						line_started;
	private boolean						force_new_line;
	private boolean						at_flow_break_point;
	
	static class Indents implements Cloneable {
		int cur_indent;
		int next_indent;
		
		Indents makeIndents(Draw_Paragraph pl, int cur_x, boolean is_text) {
			if (pl != null) {
				Indents i = new Indents();
				int pl_indent = is_text ? pl.indent_text_size : pl.indent_pixel_size;
				int pl_next_indent = is_text ? pl.next_indent_text_size : pl.next_indent_pixel_size;
				if (pl.indent_from_current_position) {
					i.cur_indent = pl_indent + cur_x;
					i.next_indent = pl_indent + pl_next_indent + cur_x;
				} else {
					i.cur_indent = this.cur_indent + pl_indent;
					i.next_indent = this.cur_indent + pl_next_indent + pl_indent;
				}
				return i;
			}
			return this;
		}
		public Object clone() {
			return super.clone();
		}
	}
	
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
		ctx.max_x = this.x;
		return ctx;
	}
	
	private void popState(DrawContext prev) {
		if (prev != null) {
			prev.x = this.x;
			prev.max_x = this.max_x;
			prev.line_started = this.line_started;
			prev.force_new_line = this.force_new_line;
			prev.at_flow_break_point = this.at_flow_break_point;
		}
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
	}

	public void postFormat(DrawLayoutBlock dlb) {
		this.postFormat(dlb, new Indents());
	}
	
	private void postFormat(DrawLayoutBlock dlb, Indents indents) {
		if (!dlb.is_flow) {
		next_layot:
			for (int i=0; i <= dlb.max_layout; i++) {
				DrawContext ctx = this.pushState();
				Indents ctx_indents = indents.makeIndents(dlb.par, this.x, gfx==null);
				boolean last = (i >= dlb.max_layout);
				if (!last)
					ctx.parent_has_more_attempts = true;
				foreach (DrawLayoutBlock b; dlb.blocks) {
					if (b.dr instanceof DrawTerm)
						ctx.addLeaf(b, i, ctx_indents);
					else
						ctx.postFormat(b, ctx_indents);
					if (ctx.max_x >= ctx.width) {
						if (this.parent_has_more_attempts) {
							// overflow, try parent's next layout
							ctx.popState(this);
							return;
						}
						else if (!last) {
							// overflow, try our's next layout
							continue next_layot;
						}
						else if (last) {
							// overflow, there are no more layouts in the parent and in this block
							ctx.popState(this);
							this.max_x = ctx.width - 1; // erase the overflow
							continue;
						}
					}
				}
				ctx.popState(this);
				break;
			}
		} else {
			// savepoint data
			int save_idx = -1;
			DrawContext ctx = this.pushState();
			Indents ctx_indents = indents.makeIndents(dlb.par, this.x, gfx==null);
			// work data between savepoints
			DrawContext tmp = ctx.pushState();
			Indents tmp_indents = (Indents)ctx_indents.clone();
			// check if the start is a safe point
			if (!line_started && this.at_flow_break_point) {
				save_idx = 0;
				tmp.parent_has_more_attempts = true;
			}
			int idx = 0;
			do {
				if (idx >= dlb.blocks.length) {
					// end of scan, save result and return
					tmp.popState(this);
					return;
				}
				DrawLayoutBlock b = (DrawLayoutBlock)dlb.blocks[idx];
				if (b.dr instanceof DrawTerm)
					tmp.addLeaf(b, 0, tmp_indents);
				else
					tmp.postFormat(b, tmp_indents);
				if (tmp.max_x >= tmp.width) {
					if (this.parent_has_more_attempts) {
						// overflow, try parent's next layout
						tmp.popState(this);
						return;
					}
					// if we have a save point behind - use it
					if (save_idx >= 0) {
						tmp = ctx.pushState();
						tmp_indents = (Indents)ctx_indents.clone();
						tmp.force_new_line = true;
						idx = save_idx;
						save_idx = -1; // we've used this savepoint, can't use it again
						continue;
					}
					// erase overflow info and force newline to appear as soon as possible
					tmp.max_x = tmp.width - 1;
					tmp.force_new_line = true;
					// if there was no safe point - continue with overflowed layout
					idx += 1;
					continue;
				} else {
					// not overflowed, check if this can be a new safe point
					if (!line_started && tmp.at_flow_break_point) {
						save_idx = idx + 1;
						tmp.popState(ctx);
						ctx_indents = (Indents)tmp_indents.clone();
						tmp.parent_has_more_attempts = true;
					}
					idx += 1;
					continue;
				}
			} while (true);
		}
	}

	private void addLeaf(DrawLayoutBlock dlb, int cur_attempt, Indents indents) {
		indents = indents.makeIndents(dlb.par, this.x, gfx==null);
		DrawTerm leaf = (DrawTerm)dlb.dr;
		flushSpaceRequests(leaf, cur_attempt, indents);
		leaf.x = x;
		x += leaf.w;
		max_x = Math.max(max_x, x);
		line_started = false;
		indents.cur_indent = indents.next_indent;
		// check flow break point
		DrawTermLink lnk = leaf.lnk_next;
		if (lnk != null) {
			int max_space = (lnk.size_0 & 0xFFFF);
			int max_nl = (lnk.size_0 >>> 16);
			this.at_flow_break_point = (max_space > 0 && max_nl == 0);
		}
	}
	
	private void flushSpaceRequests(DrawTerm leaf, int cur_attempt, Indents indents) {
		DrawTermLink lnk = leaf.lnk_prev;
		if (lnk == null) {
			this.x = indents.cur_indent;
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
			this.x = indents.cur_indent;
		else
			this.x += max_space;
		if (max_nl > 0 || (this.force_new_line && max_space > 0)) {
			lnk.do_newline = true;
			this.x = indents.cur_indent;
			this.force_new_line = false;
			if (max_nl > 0)
				lnk.the_size = max_nl;
			else
				lnk.the_size = 0;
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

@ThisIsANode(copyable=false)
public final class DrawLayoutBlock extends ANode {

	public static final DrawLayoutBlock[] emptyArray = new DrawLayoutBlock[0];

	@nodeAttr
	public DrawLayoutBlock[]	blocks;
	@nodeData
	public Draw_Paragraph		par;
	@nodeData
	public Drawable				dr;
	public int					max_layout;		// for block (alternative) layouts
	public boolean				is_flow;		// for flow blocks
	
	public DrawLayoutBlock pushDrawable(Drawable dr) {
		Draw_Paragraph pl = null;
		if (dr.syntax != null) {
			if (dr.syntax instanceof Draw_SyntaxList) {
				if (dr instanceof DrawWrapList)
					pl = dr.syntax.par;
				else
					pl = ((Draw_SyntaxList)dr.syntax).elpar;
			} else {
				pl = dr.syntax.par;
			}
			if (pl != null)
				this = pushParagraph(dr, pl);
		}
		return this;
	}
	public DrawLayoutBlock popDrawable(Drawable dr) {
		Draw_Paragraph pl = null;
		if (dr.syntax != null) {
			if (dr.syntax instanceof Draw_SyntaxList) {
				if (dr instanceof DrawWrapList)
					pl = dr.syntax.par;
				else
					pl = ((Draw_SyntaxList)dr.syntax).elpar;
			} else {
				pl = dr.syntax.par;
			}
			if (pl != null)
				this = popParagraph(dr, pl);
		}
		return this;
	}
	
	private DrawLayoutBlock pushParagraph(Drawable dp, Draw_Paragraph pl) {
		if (pl.enabled(dp)) {
			DrawLayoutBlock dlb = new DrawLayoutBlock();
			this.blocks += dlb;
			dlb.dr = dp;
			dlb.par = pl;
			dlb.is_flow = pl.flow;
			return dlb;
		}
		return this;
	}
	private DrawLayoutBlock popParagraph(Drawable dp, Draw_Paragraph pl) {
		if (this.dr == dp) {
			assert (pl.enabled(dp));
			if (!this.is_flow) {
				int max_layout = 0;
				foreach (DrawLayoutBlock b; blocks)
					max_layout = Math.max(max_layout, b.max_layout);
				this.max_layout = max_layout;
			}
			return (DrawLayoutBlock)parent();
		}
		assert (!pl.enabled(dp));
		return this;
	}
	
	public void addLeaf(DrawTerm leaf) {
		DrawLayoutBlock dlb = new DrawLayoutBlock();
		this.blocks += dlb;
		dlb.dr = leaf;
		if (leaf.syntax != null && leaf.syntax.par != null)
			dlb.par = leaf.syntax.par;
		dlb.max_layout = leaf.syntax.lout.count;
	}
	
}

