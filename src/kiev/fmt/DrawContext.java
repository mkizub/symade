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

public abstract class DrawContext implements Cloneable {
	
	public final Formatter				fmt;
	private boolean						parent_has_more_attempts;
	private int							width;
	private int							x;
	private int							max_x;
	private boolean						line_started;
	private boolean						force_new_line;
	private boolean						at_flow_break_point;
	
	static class Indents implements Cloneable {
		final Indents         prev;
		final Draw_Paragraph  par;
		private int indent;
		private boolean initialized;
		private boolean added_next;
		Indents(Indents prev, Draw_Paragraph par) {
			this.prev = prev;
			this.par = par;
		}
		public Object clone() { return super.clone(); }
		
		private Indents getPrev() {
			Indents prev = this.prev;
		prev_prev:
			while (prev != null) {
				if (par.no_indent_if_prev != null) {
					String prev_par_name = prev.par.name;
					String prev_ind_name = prev.par.indent == null ? null : prev.par.indent.name;
					foreach (String ign; par.no_indent_if_prev; ign == prev_par_name || ign == prev_ind_name) {
						prev = prev.prev;
						continue prev_prev;
					}
				}
				if (prev.par.no_indent_if_next != null) {
					String curr_par_name = par.name;
					String curr_ind_name = par.indent == null ? null : par.indent.name;
					foreach (String ign; prev.par.no_indent_if_next; ign == curr_par_name || ign == curr_ind_name) {
						prev = prev.prev;
						continue prev_prev;
					}
				}
				return prev;
			}
			return null;
		}
		
		public void init(int x) {
			if (!initialized) {
				if (prev != null)
					prev.init(x);
				Indents prev = getPrev();
				if (par.indent != null && par.indent.from_current_position)
					indent = x;
				else if (prev != null && par.indent != null)
					indent = par.indent.pixel_size + prev.getIndent();
				else if (prev != null)
					indent = prev.getIndent();
				else if (par.indent != null)
					indent = par.indent.pixel_size;
				initialized = true;
			}
		}
		
		public void addNext() {
			if (!added_next) {
				if (par.indent != null)
					indent += par.indent.next_pixel_size;
				added_next = true;
			}
		}
		
		public int getIndent() {
			return indent;
		}
	}
	
	public DrawContext(Formatter fmt, int width) {
		this.fmt = fmt;
		this.width = width;
		line_started = true;
	}
	
	public abstract DrawTermLayoutInfo makeDrawTermLayoutInfo(DrawTerm dt);
	public abstract void formatAsText(DrawTerm dr);
	public abstract int setXgetWidth(DrawTermLayoutInfo dlb, int x);
	public abstract Indents makeIndents(Indents indents, Draw_Paragraph pl, int cur_x);

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

	public void postFormat(DrawLayoutBlock dlb) {
		this.postFormat(dlb, new Indents(null,new Draw_Paragraph()));
	}
	
	private void postFormat(DrawLayoutInfo dlb, Indents indents) {
		if (!dlb.isFlow()) {
		next_layot:
			for (int i=0; i <= dlb.getMaxLayout(); i++) {
				DrawContext ctx = this.pushState();
				Indents ctx_indents = makeIndents(indents, dlb.getParagraph(), this.x);
				boolean last = (i >= dlb.getMaxLayout());
				if (!last)
					ctx.parent_has_more_attempts = true;
				DrawLayoutInfo[] blocks = dlb.getBlocks();
				for (int j=0; j < blocks.length; j++) {
					DrawLayoutInfo b = blocks[j];
					if (dlb.isVertical() && j > 0 && j < blocks.length-1)
						ctx.force_new_line = true;
					if (b instanceof DrawTermLayoutInfo)
						ctx.addLeaf((DrawTermLayoutInfo)b, i, ctx_indents);
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
			Indents ctx_indents = makeIndents(indents, dlb.getParagraph(), this.x);
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
				if (idx >= dlb.getBlocks().length) {
					// end of scan, save result and return
					tmp.popState(this);
					return;
				}
				DrawLayoutInfo b = dlb.getBlocks()[idx];
				if (b instanceof DrawTermLayoutInfo)
					tmp.addLeaf((DrawTermLayoutInfo)b, 0, tmp_indents);
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

	private void addLeaf(DrawTermLayoutInfo dlb, int cur_attempt, Indents indents) {
		indents = makeIndents(indents, dlb.getParagraph(), this.x);
		flushSpaceRequests(dlb, cur_attempt, indents);
		x += setXgetWidth(dlb, x);
		max_x = Math.max(max_x, x);
		line_started = false;
		indents.addNext();
		//indents.cur_indent = indents.next_indent;
		// check flow break point
		DrawTermLink lnk = dlb.lnk_next;
		if (lnk != null) {
			int max_space = (lnk.size_0 & 0xFFFF);
			int max_nl = (lnk.size_0 >>> 16);
			this.at_flow_break_point = (max_space > 0 && max_nl == 0);
		}
	}
	
	private void flushSpaceRequests(DrawTermLayoutInfo dlb, int cur_attempt, Indents indents) {
		DrawTermLink lnk = dlb.lnk_prev;
		if (lnk == null) {
			indents.init(0);
			this.x = indents.getIndent();
			indents.addNext();
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
		if (this.line_started) {
			indents.init(this.x);
			this.x = indents.getIndent();
		} else {
			this.x += max_space;
			indents.init(this.x);
		}
		if (max_nl > 0 || (this.force_new_line && max_space > 0)) {
			lnk.do_newline = true;
			this.x = indents.getIndent();
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

public final class GfxDrawContext extends DrawContext {
	public final IFmtGfx				gfx;
	public GfxDrawContext(GfxFormatter fmt, int width) {
		super(fmt,width);
		this.gfx = fmt.getGfx();
	}
	public DrawTermLayoutInfo makeDrawTermLayoutInfo(DrawTerm dt) {
		return new GfxDrawTermLayoutInfo(dt);
	}
	public void formatAsText(DrawTerm dr) {
		GfxDrawTermLayoutInfo gfx_fmt = (GfxDrawTermLayoutInfo)dr.dt_fmt;
		gfx_fmt.x = 0;
		gfx_fmt.y = 0;
		gfx_fmt.height = 0;
		String text = dr.getText();
		if (text == null) text = "\u25d8"; // ◘
		if (text != null && text.length() != 0) {
			gfx.layoutText(text, dr.syntax.lout.font_name);
			gfx_fmt.width = gfx.textWidth();
			gfx_fmt.height = gfx.textHeight();
			gfx_fmt.baseline = gfx.textBaseline();
		} else {
			gfx_fmt.width = 0;
			gfx_fmt.height = 10;
			gfx_fmt.baseline = 0;
		}
	}
	public int setXgetWidth(DrawTermLayoutInfo dlb, int x) {
		GfxDrawTermLayoutInfo gfx_fmt = (GfxDrawTermLayoutInfo)dlb;
		gfx_fmt.x = x;
		return gfx_fmt.width;
	}
	public Indents makeIndents(Indents from, Draw_Paragraph pl, int cur_x) {
		if (pl != null && pl.indent != null) {
			Indents ind;
			int pl_indent = pl.indent.pixel_size;
			int pl_next_indent = pl.indent.next_pixel_size;
			//if (pl.indent.from_current_position)
			//	ind = new Indents(pl.indent.name, pl_indent + cur_x, pl_indent + pl_next_indent + cur_x);
			//else
			//	ind = new Indents(pl.indent.name, from.cur_indent + pl_indent, from.cur_indent + pl_next_indent + pl_indent);
			ind = new Indents(from, pl);
			return ind;
		}
		return from;
	}
}

public final class TxtDrawContext extends DrawContext {
	public TxtDrawContext(TextFormatter fmt, int width) {
		super(fmt,width);
	}
	public DrawTermLayoutInfo makeDrawTermLayoutInfo(DrawTerm dt) {
		return new TxtDrawTermLayoutInfo(dt);
	}
	public void formatAsText(DrawTerm dr) {
		TxtDrawTermLayoutInfo txt_fmt = (TxtDrawTermLayoutInfo)dr.dt_fmt;
		txt_fmt.x = 0;
		txt_fmt.lineno = 0;
	}
	public int setXgetWidth(DrawTermLayoutInfo dlb, int x) {
		TxtDrawTermLayoutInfo txt_fmt = (TxtDrawTermLayoutInfo)dlb;
		txt_fmt.x = x;
		String txt = dlb.getDrawable().getText();
		if (txt == null)
			return 0;
		return txt.length();
	}
	public Indents makeIndents(Indents from, Draw_Paragraph pl, int cur_x) {
		if (pl != null && pl.indent != null) {
			Indents ind;
			int pl_indent = pl.indent.text_size;
			int pl_next_indent = pl.indent.next_text_size;
			//if (pl.indent.from_current_position)
			//	ind = new Indents(pl.indent.name, pl_indent + cur_x, pl_indent + pl_next_indent + cur_x);
			//else
			//	ind = new Indents(pl.indent.name, from.cur_indent + pl_indent, from.cur_indent + pl_next_indent + pl_indent);
			ind = new Indents(from, pl);
			return ind;
		}
		return from;
	}
}


public final class DrawLinkContext {
	
	public final boolean					is_gfx;
	public final boolean					is_tree;
	private final Vector<LayoutSpace>		space_infos = new Vector<LayoutSpace>();
	private final Vector<LayoutSpace>		space_infos_1 = new Vector<LayoutSpace>();
	private boolean							update_spaces;
	
	public DrawLinkContext(boolean is_gfx, boolean is_tree) {
		this.is_gfx = is_gfx;
		this.is_tree = is_tree;
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
				max_nl = Math.max(is_gfx ? csi.pixel_size : csi.text_size, max_nl);
			else
				max_space = Math.max(is_gfx ? csi.pixel_size : csi.text_size, max_space);
		}
		lnk.size_0 = (max_nl << 16) | (max_space & 0xFFFF);
		space_infos.removeAllElements();

		max_space = 0;
		max_nl = 0;
		foreach (LayoutSpace csi; space_infos_1; !csi.eat) {
			if (csi.new_line)
				max_nl = Math.max(is_gfx ? csi.pixel_size : csi.text_size, max_nl);
			else
				max_space = Math.max(is_gfx ? csi.pixel_size : csi.text_size, max_space);
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

public abstract class DrawLayoutInfo {
	public abstract Drawable getDrawable();
	public abstract DrawLayoutInfo[] getBlocks();
	public abstract Draw_Paragraph getParagraph();
	public abstract int getMaxLayout();
	public abstract boolean isFlow();
	public abstract boolean isVertical();
}

public final class DrawLayoutBlock extends DrawLayoutInfo {

	public static final DrawLayoutInfo[] emptyArray = new DrawLayoutInfo[0];

	public final DrawLayoutBlock parent;

	public DrawLayoutInfo[]		blocks;
	public Draw_Paragraph		par;
	public Drawable				dr;
	public int					max_layout;		// for block (alternative) layouts
	
	public DrawLayoutBlock() {
		this.parent = null;
		this.blocks = DrawLayoutBlock.emptyArray;
	}
	
	private DrawLayoutBlock(DrawLayoutBlock parent) {
		this.parent = parent;
		this.blocks = DrawLayoutBlock.emptyArray;
	}
	
	// for GUI
	public Drawable getDrawable() { dr }
	// for GUI
	public DrawLayoutInfo[] getBlocks() { blocks }
	public Draw_Paragraph getParagraph() { par }
	public int getMaxLayout() { max_layout }
	public boolean isFlow() { par != null && par.flow == ParagraphFlow.FLOW }
	public boolean isVertical() { par != null && par.flow == ParagraphFlow.VERTICAL }
	
	public DrawLayoutBlock pushDrawable(Drawable dr) {
		if (dr.syntax != null) {
			Draw_Paragraph pl = dr.syntax.par;
			if (pl != null)
				this = pushParagraph(dr, pl);
		}
		return this;
	}
	public DrawLayoutBlock popDrawable(Drawable dr) {
		if (dr.syntax != null) {
			Draw_Paragraph pl = dr.syntax.par;
			if (pl != null)
				this = popParagraph(dr, pl);
		}
		return this;
	}
	
	private DrawLayoutBlock pushParagraph(Drawable dp, Draw_Paragraph pl) {
		DrawLayoutBlock dlb = new DrawLayoutBlock(this);
		this.blocks = (DrawLayoutInfo[])Arrays.append(this.blocks, dlb);
		dlb.dr = dp;
		dlb.par = pl;
		return dlb;
	}
	private DrawLayoutBlock popParagraph(Drawable dp, Draw_Paragraph pl) {
		assert (this.dr == dp);
		if (pl.flow == ParagraphFlow.HORIZONTAL) {
			int max_layout = 0;
			foreach (DrawLayoutInfo b; blocks)
				max_layout = Math.max(max_layout, b.getMaxLayout());
			this.max_layout = max_layout;
		}
		return this.parent;
	}
	
	public void addLeaf(DrawTermLayoutInfo dlb) {
		this.blocks = (DrawLayoutInfo[])Arrays.append(this.blocks, dlb);
	}
	
	public DrawLayoutBlock enterBlock(Draw_Paragraph pl) {
		DrawLayoutBlock dlb = new DrawLayoutBlock(this);
		this.blocks = (DrawLayoutInfo[])Arrays.append(this.blocks, dlb);
		dlb.par = pl;
		return dlb;
	}
	public DrawLayoutBlock leaveBlock() {
		return this.parent;
	}
}

