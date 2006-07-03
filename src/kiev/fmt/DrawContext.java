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
	public int						x, y;
	public boolean					line_started;
	public Vector<SpaceCmd>			space_infos;
	public Stack<DrawParagraph>		paragraphs;
	private DrawTerm				last_term;
	private DrawContext				prev_ctx;
	
	public DrawContext(Formatter fmt, Graphics2D gfx) {
		this.fmt = fmt;
		this.gfx = gfx;
		line_started = true;
		space_infos = new Vector<SpaceCmd>();
		paragraphs = new Stack<DrawParagraph>();
	}
	
	public Object clone() {
		DrawContext dc = (DrawContext)super.clone();
		dc.space_infos = (Vector<SpaceCmd>)this.space_infos.clone();
		dc.paragraphs = (Stack<DrawParagraph>)this.paragraphs.clone();
		return dc;
	}

	public DrawContext pushState() {
		DrawContext ctx = (DrawContext)this.clone();
		ctx.prev_ctx = this;
		return ctx;
	}
	
	public DrawContext popState() {
		return prev_ctx;
	}

	public void formatAsText(DrawTerm dr) {
		DrawGeometry dg = dr.geometry;
		dg.x = 0;
		dg.y = 0;
		dg.h = 0;
		if (dr instanceof DrawJavaComment) {
			String[] lines = ((DrawJavaComment)dr).lines;
			foreach(String text; lines) {
				if (gfx != null) {
					if (text.length() != 0) {
						DrawFormat fmt = dr.syntax.fmt;
						Font  font  = fmt.font.native_font;
						if (font == null) font = new Font("Dialog", Font.PLAIN, 12);
						TextLayout tl = new TextLayout(text, font, gfx.getFontRenderContext());
						Rectangle2D rect = tl.getBounds();
						dg.w = (int)Math.max(dg.w, Math.ceil(tl.getAdvance()));
						dg.h+= (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
					} else {
						dg.w = 0;
						dg.h+= 10;
					}
					dg.b = 0;
				} else {
					dg.w = text.length();
					dg.h = 1;
					dg.b = 0;
				}
			}
		} else {
			String text = dr.getText();
			if (gfx != null) {
				if (text == null) text = "<?>";
				if (text != null && text.length() != 0) {
					DrawFormat fmt = dr.syntax.fmt;
					Font  font  = fmt.font.native_font;
					if (font == null) font = new Font("Dialog", Font.PLAIN, 12);
					TextLayout tl = new TextLayout(text, font, gfx.getFontRenderContext());
					Rectangle2D rect = tl.getBounds();
					dg.w = (int)Math.ceil(tl.getAdvance());
					dg.h = (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
					dg.b = (int)Math.ceil(tl.getAscent()+tl.getLeading());
				} else {
					dg.w = 0;
					dg.h = 10;
					dg.b = 0;
				}
			} else {
				dg.w = text.length();
				dg.h = 1;
				dg.b = 0;
			}
			this.x += dg.w;
		}
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
		leaf.geometry.x = x;
		x += leaf.geometry.w;
		line_started = false;
		last_term = leaf;
		processSpaceAfterRequest(leaf);
		return (x < width);
	}
	
	private void flushSpaceRequests() {
		int max_space = 0;
		foreach (SpaceCmd csi; space_infos; csi.si.kind == SP_SPACE) {
			if (!csi.eat)
				max_space = Math.max(gfx==null ? csi.si.text_size : csi.si.pixel_size, max_space);
		}
		if (line_started)
			this.x = getIndent();
		else
			this.x += max_space;

		int max_nl = 0;
		foreach (SpaceCmd csi; space_infos; csi.si.kind == SP_NEW_LINE) {
			if (!csi.eat)
				max_nl = Math.max(gfx==null ? csi.si.text_size : csi.si.pixel_size, max_nl);
		}

		if (max_nl > 0) {
			if (last_term != null)
				last_term.geometry.do_newline = max_nl;
			this.line_started = true;
			this.x = getIndent();
		}
		space_infos.removeAllElements();
	}
	
	private void addSpaceInfo(SpaceCmd sc) {
		String name = sc.si.id.uname;
		SpaceKind kind = sc.si.kind;
		for (int i=0; i < space_infos.size(); i++) {
			SpaceCmd csi = space_infos[i];
			if (csi.si.id.uname == name && csi.si.kind == kind) {
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
		foreach (SpaceCmd si; dr.syntax.layout.spaces; si.before)
			addSpaceInfo(si);
	}
	
	private void processSpaceAfterRequest(Drawable dr) {
		foreach (SpaceCmd si; dr.syntax.layout.spaces; !si.before)
			addSpaceInfo(si);
	}
	
	private int getIndent() {
		int indent = 0;
		foreach (DrawParagraph dp; paragraphs; dp.getParLayout().enabled(dp)) {
			ParagraphLayout pl = dp.getParLayout();
			if (!pl.enabled(dp))
				continue;
			indent += gfx==null ? pl.indent_text_size : pl.indent_pixel_size;
			if (dp.is_multiline)
				indent += gfx==null ? pl.indent_first_line_text_size : pl.indent_first_line_pixel_size;
		}
		return indent;
	}
}


