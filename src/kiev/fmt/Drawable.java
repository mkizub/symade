package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node(copyable=false)
public abstract class Drawable extends ANode {

	public static final Drawable[] emptyArray = new Drawable[0];

	// the node we draw
	@ref
	public ANode			node;
	// can be text (line/pos) or graphics (pixel x,y,w,h,baseline info) and so on,
	// filled/modified during preFormat/postFormat
	public DrawGeometry		geometry;
	// syntax kind & draw layout
	@ref
	public SyntaxElem		syntax;
	// definer syntax (StntaxAttr or SyntaxNode, etc) 
	@ref
	public SyntaxElem		attr_syntax;
	
	public Drawable(ANode node, SyntaxElem syntax) {
		this.node = node;
		this.geometry = new DrawGeometry();
		this.syntax = syntax;
		this.geometry.is_hidden = this.syntax.fmt.is_hidden;
	}
	
	public abstract String getText();

	public abstract void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node);
	public abstract boolean postFormat(DrawContext cont, boolean last_layout);
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();

	public final boolean isUnvisible() {
		if (geometry != null)
			return geometry.is_hidden;
		if (syntax != null)
			return syntax.fmt.is_hidden;
		return false;
	}  

	public final DrawTerm getNextLeaf() {
		Drawable p = this;
		for (;;) {
			while (p.pnext() == null && (p=(Drawable)p.parent()) != null);
			if (p == null)
				return null;
			p = (Drawable)p.pnext();
			if (p.isUnvisible())
				continue;
			DrawTerm d = p.getFirstLeaf();
			if (d == null)
				continue;
			if (!d.isUnvisible())
				return d;
			p = d;
		}
	}
	public final DrawTerm getPrevLeaf() {
		Drawable p = this;
		for (;;) {
			while (p.pprev() == null && (p=(Drawable)p.parent()) != null);
			if (p == null)
				return null;
			p = (Drawable)p.pprev();
			if (p.isUnvisible())
				continue;
			DrawTerm d = p.getLastLeaf();
			if (d == null)
				continue;
			if (!d.isUnvisible())
				return d;
			p = d;
		}
	}
}


