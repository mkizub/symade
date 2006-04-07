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

public class TextFormatter implements Formatter {
	public static final AttrSlot ATTR = new DataAttrSlot("text fmt info",true,Shadow.class);	
	private Syntax syntax;
	
	public TextFormatter(Syntax syntax) {
		this.syntax = syntax;
	}

	public AttrSlot getAttr() {
		return ATTR;
	}

	public Drawable format(ASTNode node) {
		DrawContext ctx = new DrawContext();
		ctx.width = 100;
		Drawable root = getDrawable(node, null);
		root.preFormat(ctx);
		ctx = new DrawContext();
		ctx.width = 100;
		root.postFormat(ctx, true);
		
		int lineno = 0;
		int line_indent = 0;
		int next_indent = line_indent;
		int y = 0;
		DrawTerm first = root.getFirstLeaf();
		DrawTerm line_start = first;
		for (DrawTerm dr=first; dr != null; dr = dr.getNextLeaf()) {
			dr.geometry.y = y;
			if (dr.isHidden()) {
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
	
	public Drawable getDrawable(ASTNode node, FormatInfoHint hint) {
		if (node == null) {
			DrawLayout lout = new DrawLayout();
			lout.is_hidden = true;
			SyntaxSpace ssp = new SyntaxSpace(lout);
			return new DrawSpace(null, ssp);
		}
		Shadow sdr = node.getNodeData(ATTR);
		if (sdr != null)
			return (Drawable)sdr.node;
		SyntaxElem stx_elem = syntax.getSyntaxElem(node, hint);
		Drawable dr = stx_elem.makeDrawable(this,node);
		node.addNodeData(new Shadow(dr), ATTR);
		return dr;
	}
}


