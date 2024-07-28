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
package kiev.fmt.common;

import kiev.fmt.*;

import java.awt.Rectangle;
import java.util.ArrayList;

public final class DrawContext implements Cloneable {
	
	static class LstDLI {
		final DrawLayoutInfo hd;
		final LstDLI tl;
		LstDLI(DrawLayoutInfo hd, LstDLI tl) {
			this.hd = hd;
			this.tl = tl;
		}
	}
	static class LstLNS {
		final LstDLI hd;
		final LstLNS tl;
		LstLNS(LstDLI hd, LstLNS tl) {
			this.hd = hd;
			this.tl = tl;
		}
	}
	
	public final Formatter				fmt;
	public final DrawLayoutInfo			block;
	private int							width;
	private int							x;
	private int							y;
	private int							indent;
	private int							indent_add;
	private LstLNS						lines;
	
	public DrawContext(Formatter fmt, DrawLayoutInfo block, int width) {
		this.fmt = fmt;
		this.block = block;
		this.width = width;
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) { return null; }
	}

	private DrawContext pushState() {
		DrawContext ctx = (DrawContext)this.clone();
		return ctx;
	}
	
	private void popState(DrawContext prev) {
		prev.x = this.x;
		prev.y = this.y;
		prev.lines = this.lines;
	}
	
	public void postFormat(DrawLayoutInfo dlb) {
		Rectangle r = calcFormat(dlb, false, false);
		r.add(0, 0);
		dlb.x_offs = 0;
		dlb.y_offs = 0;
		dlb.width = r.width;
		dlb.height = r.height;
	}
	
	private Rectangle calcFormat(DrawLayoutInfo dlb, boolean alt_layout, boolean parent_has_more_attempts) {
		DrawContext ctx = this.pushState();
		Rectangle r = ctx.calcLayoutInfo(dlb, alt_layout, parent_has_more_attempts);
		if (r.x+r.width > this.width && parent_has_more_attempts)
			return r;
		ctx.popState(this);
		return r;
	}
	
	private Rectangle calcLayoutInfo(DrawLayoutInfo b, boolean alt_layout, boolean parent_has_more_attempts) {
		Draw_Paragraph par = b.getParagraph();
		{
			Draw_ParIndent pindent = par == null ? null : par.getIndent();
			Draw_Paragraph par_prev = b.getEnclosingParagraph();
			Draw_ParIndent unindent = par_prev == null ? null : par_prev.getIndent();
			boolean indent_par = pindent != null;
			boolean unindent_prev = false;
			if (pindent != null && par_prev != null) {
				Draw_ParNoIndentIfPrev nidp = par.getNoIndentIfPrev();
				if (nidp != null) {
					for (String name : nidp.names) {
						if (par_prev.name == name) {
							indent_par = false;
							break;
						}
					}
				}
			}
			if (par != null && unindent != null) {
				Draw_ParNoIndentIfNext nidn = par_prev.getNoIndentIfNext();
				if (nidn != null) {
					for (String name : nidn.names) {
						if (par.name == name) {
							unindent_prev = true;
							break;
						}
					}
				}
			}
			if (indent_par) {
				this.indent += fmt.getSize(pindent.indent);
				this.indent_add += fmt.getSize(pindent.indent_next);
			}
			if (unindent_prev) {
				this.indent -= fmt.getSize(unindent.indent);
				this.indent_add -= fmt.getSize(unindent.indent_next);
			}
		}
		Draw_ParInset pinset = par == null ? null : par.getInsets();
		
		if (lines == null || lines.hd == null)
			this.x = getIndent();
		int x = this.x;
		int y = this.y;
		Rectangle r;
		if (b.isSubBlock()) {
			if (!(b.getDrawable() instanceof DrawTerm)) {
				int sub_width = this.width-this.x;
				if (sub_width < 100) sub_width = 100;
				DrawContext ctx = new DrawContext(fmt, b, sub_width);
				if (pinset != null) {
					ctx.y = fmt.getSize(pinset.top);
					ctx.indent = fmt.getSize(pinset.left);
					ctx.width -= fmt.getSize(pinset.right);
				}
				r = ctx.calcBlock(b, parent_has_more_attempts);
				r.add(0, 0);
				if (pinset != null) {
					r.height += fmt.getSize(pinset.bottom);
					r.width += fmt.getSize(pinset.right);
				}
				b.width = r.width;
				b.height = r.height;
			}
			b.x_offs = x;
			b.y_offs = y;
			Draw_ParSize psize = par == null ? null : par.getSize();
			if (psize != null) {
				if (psize.min_width != null && b.width < fmt.getSize(psize.min_width))
					b.width = fmt.getSize(psize.min_width);
				if (psize.max_width != null && b.width > fmt.getSize(psize.max_width))
					b.width = fmt.getSize(psize.max_width);
				if (psize.min_height != null && b.height < fmt.getSize(psize.min_height))
					b.height = fmt.getSize(psize.min_height);
				if (psize.max_height != null && b.height > fmt.getSize(psize.max_height))
					b.height = fmt.getSize(psize.max_height);
			}
			r = new Rectangle(x, y, b.width, b.height);
			if (pinset != null && pinset.left != null)
				this.indent -= fmt.getSize(pinset.left);
			this.x += b.width;
			if (lines == null)
				lines = new LstLNS(null,null);
			lines = new LstLNS(new LstDLI(b, lines.hd), lines.tl);
			if (alt_layout && b.space_alt != null) {
				addNewLine();
				this.x = getIndent();
			}
			else if (b.space_std != null && b.space_std.kind == SpaceKind.SP_NEW_LINE) {
				addNewLine();
				this.y += fmt.getSize(b.space_std);
				this.x = getIndent();
			} else {
				b.setDoNewline(false);
				if (b.space_std != null && b.space_std.kind == SpaceKind.SP_SPACE)
					this.x += fmt.getSize(b.space_std);
			}
			r = new Rectangle(x, y, b.width, b.height);
		} else {
			r = calcBlock(b, parent_has_more_attempts);
			b.x_offs = x;
			b.y_offs = y;
		}
		return r;
	}
	
	private Rectangle calcBlock(DrawLayoutInfo dlb, boolean parent_has_more_attempts) {
		Rectangle rb = new Rectangle(0,0,-1,-1);
		if (dlb.isVertical()) {
			DrawLayoutInfo[] blocks = dlb.getBlocks();
			for (int i=0; i < blocks.length; i++) {
				DrawContext ctx = this.pushState();
				Rectangle r = ctx.calcFormat(blocks[i], false, parent_has_more_attempts);
				rb.add(r);
				if (r.x+r.width > this.width && parent_has_more_attempts) // overflow, try parent's next layout
					return rb;
				if (i+1 < blocks.length)
					ctx.addNewLine();
				ctx.popState(this);
			}
		}
		else if (dlb.isFlow()) {
			DrawLayoutInfo[] blocks = dlb.getBlocks();
			for (int i=0; i < blocks.length; i++) {
				DrawContext ctx = this.pushState();
				Rectangle r = ctx.calcFormat(blocks[i], false, parent_has_more_attempts || (lines != null && lines.hd != null));
				if (r.x+r.width > this.width) {
					if (parent_has_more_attempts) {
						// overflow, try parent's next layout
						rb.add(r);
						return rb;
					}
					else if (lines != null && lines.hd != null) {
						// overflow, try our's next layout
						i -= 1;
						this.addNewLine();
						continue;
					}
				}
				rb.add(r);
				ctx.popState(this);
			}
		}
		else {
		next_layot:
			for (boolean alt_layout=false;; alt_layout=true) {
				DrawContext ctx = this.pushState();
				DrawLayoutInfo[] blocks = dlb.getBlocks();
				for (int j=0; j < blocks.length; j++) {
					Rectangle r = ctx.calcFormat(blocks[j], alt_layout, parent_has_more_attempts || (!alt_layout && dlb.hasAltLayout()));
					if (r.x+r.width > this.width) {
						if (parent_has_more_attempts) {
							// overflow, try parent's next layout
							rb.add(r);
							return rb;
						}
						else if (!alt_layout && dlb.hasAltLayout()) {
							// overflow, try our's next layout
							continue next_layot;
						}
					}
					rb.add(r);
				}
				ctx.popState(this);
				return rb;
			}
		}
		return rb;
	}

	private void addNewLine() {
		if (lines == null || lines.hd == null)
			return;
		int top = 0; // above baseline
		int bot = 0; // below baseline
		// iterate all unaligned blocks with baselines, make 'top' & 'bot' values
		for (LstDLI l = lines.hd; l != null; l = l.tl) {
			DrawLayoutInfo dli = l.hd;
			Draw_Paragraph p = dli.getParagraph();
			Draw_ParAlignBlock a = p == null ? null : p.getAlignBlock();
			if (a == null) {
				int bln = dli.baseline;
				if (bln != 0) {
					top = Math.max(top, bln);
					bot = Math.max(bot, dli.height - bln);
				}
			}
		}
		// iterate all unaligned blocks without baseline, increase 'top' by placing block's bottom at 'bot'
		for (LstDLI l = lines.hd; l != null; l = l.tl) {
			DrawLayoutInfo dli = l.hd;
			Draw_Paragraph p = dli.getParagraph();
			Draw_ParAlignBlock a = p == null ? null : p.getAlignBlock();
			if (a == null) {
				int bln = dli.baseline;
				if (bln == 0) {
					top = Math.max(top, dli.height-bot);
				}
			}
		}
		int above = 0;
		// iterate all blocks, setup their y_offs acording to baseline or alignment
		for (LstDLI l = lines.hd; l != null; l = l.tl) {
			DrawLayoutInfo dli = l.hd;
			Draw_Paragraph p = dli.getParagraph();
			Draw_ParAlignBlock a = p == null ? null : p.getAlignBlock();
			if (a == null) {
				int bln = dli.baseline;
				if (bln != 0) {
					dli.y_offs = top - bln;
				} else {
					dli.y_offs = top + bot - dli.height;
				}
			} else {
				if (a.align == SyntaxAlignType.BOTTOM_CENTER || a.align == SyntaxAlignType.BOTTOM_LEFT || a.align == SyntaxAlignType.BOTTOM_RIGHT)
					dli.y_offs = top + bot - dli.height;
				if (a.align == SyntaxAlignType.CENTER_CENTER || a.align == SyntaxAlignType.CENTER_LEFT || a.align == SyntaxAlignType.CENTER_RIGHT)
					dli.y_offs = (top + bot - dli.height) / 2;
				if (a.align == SyntaxAlignType.TOP_CENTER || a.align == SyntaxAlignType.TOP_LEFT || a.align == SyntaxAlignType.TOP_RIGHT)
					dli.y_offs = 0;
			}
			// check all blocks to have top above 'top' (for bottom-aligned blocks)
			if (dli.y_offs < 0)
				above = Math.max(above, -dli.y_offs);
		}
		int height = 0;
		// shift down blocks (add 'above')
		// calculate total line height
		// setup final y_offs
		for (LstDLI l = lines.hd; l != null; l = l.tl) {
			DrawLayoutInfo dli = l.hd;
			dli.y_offs += above;
			height = Math.max(height, dli.y_offs + dli.height);
			dli.y_offs += this.y;
		}
		// increment y
		this.y += height;
		lines.hd.hd.setDoNewline(true);
		lines = new LstLNS(null,lines);
	}
	
	private int getIndent() {
		if (lines == null || lines.tl == null)
			return indent;
		return indent + indent_add;
	}
}

final class DrawLinkContext {
	
	public final boolean					is_gfx;
	private final ArrayList<LayoutSpace>	space_infos = new ArrayList<LayoutSpace>();
	private boolean							update_spaces;
	
	public DrawLinkContext(boolean is_gfx) {
		this.is_gfx = is_gfx;
	}
	
	public void requestSpacesUpdate() {
		update_spaces = true;
	}
	
	// 
	// calculate the space for DrawLayoutInfo
	//
	
	public void flushSpace(DrawLayoutInfo lnk) {
		update_spaces = false;
		if (lnk == null) {
			space_infos.clear();
			return;
		}
		LayoutSpace space_std = null;
		LayoutSpace space_alt = null;
		for (LayoutSpace csi : space_infos) {
			if (csi.eat) continue;
			if (csi.kind == SpaceKind.SP_SPACE) {
				if (space_std == null || (space_std.kind == SpaceKind.SP_SPACE && space_std.gfx_size < csi.gfx_size))
					space_std = csi;
			}
			if (csi.kind == SpaceKind.SP_NEW_LINE) {
				if (space_std == null || space_std.kind != SpaceKind.SP_NEW_LINE)
					space_std = csi;
				else if (space_std.kind == SpaceKind.SP_NEW_LINE && space_std.gfx_size < csi.gfx_size)
					space_std = csi;
			}
			if (csi.kind == SpaceKind.SP_BRK_LINE) {
				if (space_alt == null || (space_alt.kind == SpaceKind.SP_BRK_LINE && space_alt.gfx_size < csi.gfx_size))
					space_alt = csi;
			}
		}
		lnk.space_std = space_std;
		lnk.space_alt = space_alt;
		space_infos.clear();
	}

	private void collectSpaceInfo(LayoutSpace sc, ArrayList<LayoutSpace> space_infos) {
		String name = sc.name;
		for (int i=0; i < space_infos.size(); i++) {
			LayoutSpace csi = space_infos.get(i);
			if (csi.name == name) {
				if (csi.eat)
					return;
				if (sc.eat) {
					space_infos.remove(i);
					i--;
				}
			}
		}
		space_infos.add(sc);
	}
	
	public void processSpaceBefore(Drawable dr) {
		if (!update_spaces)
			return;
		Draw_Style style = null;
		if (dr instanceof DrawTerm)
			style = ((DrawTerm)dr).getGfxFmtInfo().style;
		if (style != null && style.spaces_before != null) {
			for (LayoutSpace si : style.spaces_before)
				collectSpaceInfo(si,space_infos);
		}
		if (dr.syntax != null && dr.syntax.lout != null) {
			for (LayoutSpace si : dr.syntax.lout.spaces_before)
				collectSpaceInfo(si,space_infos);
		}
	}
	
	public void processSpaceAfter(Drawable dr) {
		if (!update_spaces)
			return;
		Draw_Style style = null;
		if (dr instanceof DrawTerm)
			style = ((DrawTerm)dr).getGfxFmtInfo().style;
		if (style != null && style.spaces_before != null) {
			for (LayoutSpace si : style.spaces_after)
				collectSpaceInfo(si,space_infos);
		}
		if (dr.syntax != null && dr.syntax.lout != null) {
			for (LayoutSpace si : dr.syntax.lout.spaces_after)
				collectSpaceInfo(si,space_infos);
		}
	}
}
