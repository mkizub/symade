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

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

public interface IFmtGfx {
	public Object getNative();
	public void layoutText(String text, String font_name);
	public int  textWidth();
	public int  textHeight();
	public int  textBaseline();
}

public interface Formatter {
	public void       format(ANode node, Drawable dr, Draw_ATextSyntax syntax);
	public Drawable   getDrawable(ANode node, Drawable dr, Draw_ATextSyntax syntax);
	public boolean    getShowAutoGenerated();
	public void       setShowAutoGenerated(boolean show);
	public boolean    getHintEscapes();
	public void       setHintEscapes(boolean show);
	public boolean    getShowPlaceholders();
	public void       setShowPlaceholders(boolean show);
	
	public Drawable         getRootDrawable();
	public DrawLayoutBlock  getRootDrawLayoutBlock();
}

public class ChangeRootException extends RuntimeException {
	public final Drawable dr;
	public ChangeRootException(Drawable dr) {
		this.dr = dr;
	}
}

public abstract class AbstractFormatter implements Formatter {

	private static final int counter;

	/** A flag to show auto-generated nodes */
	public boolean		show_auto_generated;
	/** A hint to show placeholders */
	public boolean		show_placeholders;
	/** A hint to show escaped idents and strings */
	public boolean		show_hint_escapes;
	
	protected Drawable				dr_root;
	protected DrawLayoutBlock		dlb_root;

	protected AbstractFormatter() {
		String name = "fmt info "+Integer.toHexString(++counter);
		name = name.intern();
	}

	public final Drawable         getRootDrawable() { return dr_root; }
	public final DrawLayoutBlock  getRootDrawLayoutBlock() { return dlb_root; }

	public abstract void format(ANode node, Drawable dr, Draw_ATextSyntax syntax);
	
	public boolean    getShowAutoGenerated() {
		return this.show_auto_generated;
	}
	public void setShowAutoGenerated(boolean show) {
		this.show_auto_generated = show;
	}

	public boolean    getHintEscapes() {
		return this.show_hint_escapes;
	}
	public void       setHintEscapes(boolean show) {
		this.show_hint_escapes = show;
	}

	public boolean    getShowPlaceholders() {
		return this.show_placeholders;
	}
	public void       setShowPlaceholders(boolean show) {
		this.show_placeholders = show;
	}

	public final Drawable getDrawable(ANode node, Drawable dr, Draw_ATextSyntax text_syntax) {
		if (dr != null && dr.drnode == node)
			return dr;
		Draw_SyntaxElem stx_elem = text_syntax.getSyntaxElem(node);
		dr = stx_elem.makeDrawable(this,node,text_syntax);
		return dr;
	}
}

public class TextFormatter extends AbstractFormatter {
	private ATextSyntax syntax;
	
	public TextFormatter() {}

	public void format(ANode node, Drawable dr, Draw_ATextSyntax syntax) {
		dr_root = getDrawable(node, dr, syntax);
		{
			TxtDrawContext ctx = new TxtDrawContext(this,1000);
			try {
				dr_root.preFormat(ctx, dr_root.syntax, node);
			} catch (ChangeRootException e) {
				dr_root = e.dr;
				dr_root.preFormat(ctx, dr_root.syntax, node);
			}
		}
		try {
			// link nodes
			dr_root.lnkFormat(new DrawLinkContext(false, false));
			DrawTerm first = dr_root.getFirstLeaf();
			if (first != null)
				DrawTerm.lnkVerify(first);
			else
				assert(dr_root.getLastLeaf() == null);
		} catch (Throwable t) { t.printStackTrace(); }
		{
			dlb_root = new DrawLayoutBlock();
			dr_root.postFormat(dlb_root);
			TxtDrawContext ctx = new TxtDrawContext(this,1000);
			ctx.postFormat(dlb_root);
			//dr_root.postFormat(ctx);
		}
		
		int lineno = 1;
		int line_indent = 0;
		int next_indent = line_indent;
		DrawTerm first = dr_root.getFirstLeaf();
		if (first == null)
			return;
		TxtDrawTermLayoutInfo line_start = (TxtDrawTermLayoutInfo)first.dt_fmt;
		for (TxtDrawTermLayoutInfo dr=line_start; dr != null; dr = dr.getNext()) {
			if (dr.lnk_next != null && dr.lnk_next.do_newline) {
				for (TxtDrawTermLayoutInfo l=line_start; l != null; l=l.getNext()) {
					l.lineno = lineno;
					if (l == dr)
						break;
				}
				lineno += dr.lnk_next.the_size;
				line_start = dr.getNext();
			}
		}
		// fill the rest
		for (TxtDrawTermLayoutInfo l=line_start; l != null; l=l.getNext()) {
			l.lineno = lineno;
		}
	}
}

public class GfxFormatter extends AbstractFormatter {

	private IFmtGfx		gfx;
	private int			width;
	
	public GfxFormatter(IFmtGfx gfx) {
		assert(gfx != null);
		this.gfx = gfx;
		this.width = 100;
	}
	
	public IFmtGfx getGfx() { return gfx; }
	
	public void setWidth(int w) {
		if (w < 100)
			this.width = 100;
		else
			this.width = w;
	}
	
	public void linkNodes() {
		try {
			// link nodes
			dr_root.lnkFormat(new DrawLinkContext(true, false));
			DrawTerm first = dr_root.getFirstLeaf();
			if (first != null)
				DrawTerm.lnkVerify(first);
			else
				assert(dr_root.getLastLeaf() == null);
		} catch (Throwable t) { t.printStackTrace(); }
	}

	public void format(ANode node, Drawable dr, Draw_ATextSyntax syntax) {
		dr_root = getDrawable(node, dr, syntax);
		try {
			dr_root.preFormat(new GfxDrawContext(this,this.width), dr_root.syntax, node);
		} catch (ChangeRootException e) {
			dr_root = e.dr;
			dr_root.preFormat(new GfxDrawContext(this,this.width), dr_root.syntax, node);
		}
		linkNodes();
		{
			dlb_root = new DrawLayoutBlock();
			dr_root.postFormat(dlb_root);
			GfxDrawContext ctx = new GfxDrawContext(this,this.width);
			ctx.postFormat(dlb_root);
			//dr_root.postFormat(ctx);
		}
		
		int lineno = 1;
		int max_h = 10;
		int max_b = 0;
		int line_indent = 0;
		int next_indent = line_indent;
		int y = 0;
		DrawTerm first = dr_root.getFirstLeaf();
		if (first == null)
			return;
		GfxDrawTermLayoutInfo line_start = (GfxDrawTermLayoutInfo)first.dt_fmt;
		for (GfxDrawTermLayoutInfo dr=line_start; dr != null; dr = dr.getNext()) {
			dr.y = y;
			max_h = Math.max(max_h, dr.height);
			max_b = Math.max(max_b, dr.baseline);
			if (dr.lnk_next != null && dr.lnk_next.do_newline) {
				for (GfxDrawTermLayoutInfo l=line_start; l != null; l=l.getNext()) {
					l.lineno = lineno;
					l.y = y;
					l.height = max_h;
					l.baseline = max_b;
					if (l == dr)
						break;
				}
				y += max_h + dr.lnk_next.the_size;
				max_h = 10;
				max_b = 0;
				line_start = dr.getNext();
				lineno++;
			}
		}
		// fill the rest
		for (GfxDrawTermLayoutInfo l=line_start; l != null; l=l.getNext()) {
			l.lineno = lineno;
			l.y = y;
			l.height = max_h;
			l.baseline = max_b;
		}
	}
}

public class GfxTreeFormatter extends GfxFormatter {

	public GfxTreeFormatter(IFmtGfx gfx) {
		super(gfx);
	}

	public void linkNodes() {
		try {
			// link nodes
			dr_root.lnkFormat(new DrawLinkContext(true, true));
			DrawTerm first = dr_root.getFirstLeaf();
			if (first != null)
				DrawTerm.lnkVerify(first);
			else
				assert(dr_root.getLastLeaf() == null);
		} catch (Throwable t) { t.printStackTrace(); }
	}
}



