package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.fmt.IndentKind.*;
import static kiev.fmt.NewLineAction.*;
import static kiev.fmt.SpaceAction.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public interface Formatter {
	public Drawable format(ASTNode node);
	public Drawable getDrawable(ASTNode node);
	public AttrSlot getAttr();
}

public class TextFormatter implements Formatter {
	public static final AttrSlot ATTR = new DataAttrSlot("text fmt info",false,Drawable.class);	
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
		Drawable root = getDrawable(node);
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
	
	public Drawable getDrawable(ASTNode node) {
		if (node == null) {
			DrawLayout lout = new DrawLayout(1, INDENT_KIND_NONE,
				new NewLineInfo[]{},
				new SpaceInfo[]{}
			);
			lout.is_hidden = true;
			SyntaxSpace ssp = new SyntaxSpace(syntax,"",lout);
			return new DrawSpace(null, ssp);
		}
		Drawable dr = node.getNodeData(ATTR);
		if (dr != null)
			return dr;
		SyntaxElem stx_elem = syntax.getSyntaxElem(node);
		Drawable dr = stx_elem.makeDrawable(this,node);
		node.addNodeData(dr, ATTR);
		return dr;
	}
}


