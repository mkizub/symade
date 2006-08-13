package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

public class DrawContext implements Cloneable {
	
	public Formatter				fmt;
	public Graphics2D				gfx;
	public int						width;
	public int						x, y, cur_attempt;
	public boolean					line_started;
	public Vector<LayoutSpace>		space_infos;
	public Stack<DrawParagraph>		paragraphs;
	private DrawTerm				last_term;
	private DrawContext				prev_ctx;
	private Font					default_font;
	
	public DrawContext(Formatter fmt, Graphics2D gfx) {
		this.fmt = fmt;
		this.gfx = gfx;
		line_started = true;
		space_infos = new Vector<LayoutSpace>();
		paragraphs = new Stack<DrawParagraph>();
		if (gfx != null)
			default_font = new Font("Dialog", Font.PLAIN, 12);
	}
	
	public Object clone() {
		DrawContext dc = (DrawContext)super.clone();
		dc.space_infos = (Vector<LayoutSpace>)this.space_infos.clone();
		dc.paragraphs = (Stack<DrawParagraph>)this.paragraphs.clone();
		return dc;
	}

	public DrawContext pushState(int cur_attempt) {
		DrawContext ctx = (DrawContext)this.clone();
		ctx.prev_ctx = this;
		ctx.cur_attempt = cur_attempt;
		return ctx;
	}
	
	public DrawContext popState(boolean save) {
		DrawContext ctx = prev_ctx;
		if (save) {
			ctx.x = this.x;
			ctx.y = this.y;
			ctx.line_started = this.line_started;
			ctx.space_infos = this.space_infos;
			ctx.paragraphs = this.paragraphs;
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

	public void pushDrawable(Drawable dr) {
		if (dr instanceof DrawParagraph) {
			DrawParagraph dp = (DrawParagraph)dr;
			dp.is_multiline = false;
			paragraphs.push(dp);
		}
		processSpaceBeforeRequest(dr);
	}
	public void popDrawable(Drawable dr) {
		if (dr instanceof DrawParagraph)
			paragraphs.pop();
		processSpaceAfterRequest(dr);
	}
	
	public boolean addLeaf(DrawTerm leaf) {
		processSpaceBeforeRequest(leaf);
		flushSpaceRequests();
		leaf.x = x;
		x += leaf.w;
		line_started = false;
		last_term = leaf;
		processSpaceAfterRequest(leaf);
		return (x < width);
	}
	
	private void flushSpaceRequests() {
		int max_space = 0;
		int max_nl = 0;
		foreach (LayoutSpace csi; space_infos; !csi.eat) {
			if (csi.new_line)
				max_nl = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_nl);
			else
				max_space = Math.max(gfx==null ? csi.text_size : csi.pixel_size, max_space);
		}
		if (this.line_started)
			this.x = getIndent();
		else
			this.x += max_space;

		if (max_nl > 0) {
			if (last_term != null)
				last_term.do_newline = max_nl;
			if (!this.line_started) {
				this.line_started = true;
				this.x = getIndent();
			}
		}
		space_infos.removeAllElements();
	}
	
	private void addSpaceInfo(LayoutSpace sc) {
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
	
	private void processSpaceBeforeRequest(Drawable dr) {
		foreach (LayoutSpace si; dr.syntax.lout.spaces_before; si.from_attempt <= cur_attempt)
			addSpaceInfo(si);
		if (dr.attr_syntax == null)
			return;
		foreach (LayoutSpace si; dr.attr_syntax.lout.spaces_before; si.from_attempt <= cur_attempt)
			addSpaceInfo(si);
	}
	
	private void processSpaceAfterRequest(Drawable dr) {
		foreach (LayoutSpace si; dr.syntax.lout.spaces_after; si.from_attempt <= cur_attempt)
			addSpaceInfo(si);
		if (dr.attr_syntax == null)
			return;
		foreach (LayoutSpace si; dr.attr_syntax.lout.spaces_after; si.from_attempt <= cur_attempt)
			addSpaceInfo(si);
	}
	
	private int getIndent() {
		int indent = 0;
		foreach (DrawParagraph dp; paragraphs; dp.getParLayout().enabled(dp)) {
			ParagraphLayout pl = dp.getParLayout();
			if (!pl.enabled(dp))
				continue;
			indent += gfx==null ? pl.indent_text_size : pl.indent_pixel_size;
			if (!dp.is_multiline) {
				indent += gfx==null ? pl.indent_first_line_text_size : pl.indent_first_line_pixel_size;
				dp.is_multiline = true;
			}
		}
		return indent;
	}
}


