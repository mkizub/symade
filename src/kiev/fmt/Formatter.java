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
	public Drawable format(ASTNode node);
	public Drawable getDrawable(ASTNode node, FormatInfoHint hint);
	public AttrSlot getAttr();
}

@node
public class FormatInfoHint extends ASTNode {
	@virtual typedef This  = FormatInfoHint;

	@att public String text;
	public FormatInfoHint() {}
	public FormatInfoHint(String text) { this.text = text; }
}

public abstract class AbstractFormatter implements Formatter {

	private static final int counter;

	public final TextSyntax syntax;
	private AttrSlot ATTR;	
	
	protected AbstractFormatter(TextSyntax syntax) {
		this.syntax = syntax;
		String name = "fmt info "+Integer.toHexString(++counter);
		name = name.intern();
		this.ATTR = new DataAttrSlot(name,false,false,Drawable.class);
	}

	public abstract Drawable format(ASTNode node);
	
	public Drawable getDrawable(ASTNode node, FormatInfoHint hint) {
		if (node == null) {
			DrawLayout lout = new DrawLayout();
			lout.is_hidden = true;
			SyntaxSpace ssp = new SyntaxSpace(lout);
			return new DrawSpace(null, ssp);
		}
		Drawable dr = (Drawable)node.getNodeData(ATTR);
		if (dr != null)
			return dr;
		SyntaxElem stx_elem = syntax.getSyntaxElem(node, hint);
		dr = stx_elem.makeDrawable(this,node);
		node.addNodeData(dr, ATTR);
		return dr;
	}

	public final AttrSlot getAttr() {
		return ATTR;
	}

}

public class TextFormatter extends AbstractFormatter {
	private TextSyntax syntax;
	
	public TextFormatter(TextSyntax syntax) {
		super(syntax);
	}

	public Drawable format(ASTNode node) {
		DrawContext ctx = new DrawContext(null);
		ctx.width = 100;
		Drawable root = getDrawable(node, null);
		root.preFormat(ctx);
		ctx = new DrawContext(null);
		ctx.width = 100;
		root.postFormat(ctx, true);
		
		int lineno = 1;
		int line_indent = 0;
		int next_indent = line_indent;
		int y = 0;
		DrawTerm first = root.getFirstLeaf();
		DrawTerm line_start = first;
		for (DrawTerm dr=first; dr != null; dr = dr.getNextLeaf()) {
			dr.geometry.y = y;
			if (dr.isUnvisible()) {
				dr.geometry.w = 0;
				dr.geometry.h = 1;
				continue;
			}
			if (dr.geometry.do_newline > 0) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.geometry.lineno = lineno;
					l.geometry.y = y;
					l.geometry.h = 1;
					if (l == dr)
						break;
				}
				y += dr.geometry.do_newline;
				line_start = dr.getNextLeaf();
				lineno++;
			}
		}
		// fill the rest
		for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
			l.geometry.lineno = lineno;
			l.geometry.y = y;
			l.geometry.h = 1;
		}
		
		return root;
	}
}

public class GfxFormatter extends AbstractFormatter {

	private Graphics2D gfx;
	
	public GfxFormatter(TextSyntax syntax, Graphics2D gfx) {
		super(syntax);
		this.gfx = gfx;
	}

	public Drawable format(ASTNode node) {
		DrawContext ctx = new DrawContext(gfx);
		ctx.width = 100;
		Drawable root = getDrawable(node, null);
		root.preFormat(ctx);
		ctx = new DrawContext(gfx);
		ctx.width = 100;
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
			dr.geometry.y = y;
			if (dr.isUnvisible()) {
				dr.geometry.w = 0;
				dr.geometry.h = max_h;
				continue;
			}
			max_h = Math.max(max_h, dr.geometry.h);
			max_b = Math.max(max_b, dr.geometry.b);
			if (dr.geometry.do_newline > 0) {
				for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
					l.geometry.lineno = lineno;
					l.geometry.y = y;
					l.geometry.h = max_h;
					l.geometry.b = max_b;
					if (l == dr)
						break;
				}
				y += max_h + dr.geometry.do_newline;
				max_h = 10;
				max_b = 0;
				line_start = dr.getNextLeaf();
				lineno++;
			}
		}
		// fill the rest
		for (DrawTerm l=line_start; l != null; l=l.getNextLeaf()) {
			l.geometry.lineno = lineno;
			l.geometry.y = y;
			l.geometry.h = max_h;
			l.geometry.b = max_b;
		}
		
		return root;
	}
}


