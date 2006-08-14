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
	// syntax kind & draw layout
	@ref
	public SyntaxElem		syntax;
	// definer syntax (StntaxAttr or SyntaxNode, etc) 
	@ref
	public SyntaxElem		attr_syntax;
	
	public Drawable(ANode node, SyntaxElem syntax) {
		this.node = node;
		this.syntax = syntax;
	}
	
	public abstract String getText();

	public abstract void preFormat(DrawContext cont);
	public abstract boolean postFormat(DrawContext cont, boolean last_layout);
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();

	public final void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
			return;
		}
		this.preFormat(cont);
	}

	public boolean isUnvisible() {
		return syntax.fmt.is_hidden;
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


