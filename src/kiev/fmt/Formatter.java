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

public interface Formatter {
	public Drawable   format(ANode node, Drawable dr);
	public Drawable   getDrawable(ANode node, Drawable dr, TextSyntax syntax);
	public TextSyntax getSyntax();
	public void       setSyntax(TextSyntax stx);
}

public abstract class AbstractFormatter implements Formatter {

	private static final int counter;

	public TextSyntax syntax;
	
	protected AbstractFormatter(TextSyntax syntax) {
		this.syntax = syntax;
		String name = "fmt info "+Integer.toHexString(++counter);
		name = name.intern();
	}

	public abstract Drawable format(ANode node, Drawable dr);
	
	public TextSyntax getSyntax() {
		return this.syntax;
	}
	
	public void setSyntax(TextSyntax stx) {
		this.syntax = stx;
	}
	
	public final Drawable getDrawable(ANode node, Drawable dr, TextSyntax syntax) {
		if (node == null) {
			if (dr instanceof DrawSpace)
				return dr;
			SyntaxSpace ssp = new SyntaxSpace();
			ssp.fmt.is_hidden = true;
			dr = new DrawSpace(null, ssp);
			return dr;
		}
		if (dr != null && dr.node == node)
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
	private TextSyntax syntax;
	
	public TextFormatter(TextSyntax syntax) {
		super(syntax);
	}

	public Drawable format(ANode node, Drawable dr) {
		DrawContext ctx = new DrawContext(this,null);
		ctx.width = 1000;
		Drawable root = getDrawable(node, dr, null);
		root.preFormat(ctx, root.syntax, node);
		ctx = new DrawContext(this,null);
		ctx.width = 1000;
		root.postFormat(ctx, true);
		
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
			if (dr.do_newline > 0) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.lineno = lineno;
					l.y = y;
					l.h = 1;
					if (l == dr)
						break;
				}
				y += dr.do_newline;
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
	
	public GfxFormatter(TextSyntax syntax, Graphics2D gfx) {
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
		ctx = new DrawContext(this,gfx);
		ctx.width = this.width;
		root.postFormat(ctx, true);
		
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
			if (dr.do_newline > 0) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.lineno = lineno;
					l.y = y;
					l.h = max_h;
					l.b = max_b;
					if (l == dr)
						break;
				}
				y += max_h + dr.do_newline;
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


