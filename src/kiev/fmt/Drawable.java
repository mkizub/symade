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


@node
public abstract class Drawable extends ASTNode {
	//static final AttrSlot ATTR = new DataAttrSlot("draw info",false,Drawable.class);

	// the node we draw
	@ref
	public ASTNode			node;
	// can be text (line/pos) or graphics (pixel x,y,w,h,baseline info) and so on,
	// filled/modified during preFormat/postFormat
	public DrawGeometry		geometry;
	// syntax kind & draw layout
	public SyntaxElem		syntax;
	// current (selected) layout
	int						curr_layout;
	
	public Drawable() {
		this.geometry = new DrawGeometry();
	}
	public Drawable(ASTNode node, SyntaxElem syntax) {
		this.node = node;
		this.geometry = new DrawGeometry();
		this.syntax = syntax;
	}

	public void init(Formatter fmt) {}

	public abstract void preFormat(DrawContext cont);
	public abstract boolean postFormat(DrawContext cont, boolean last_layout);
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();

	public final boolean isUnvisible() {
		if (geometry != null)
			return geometry.is_hidden;
		if (syntax != null)
			return syntax.is_hidden;
		return false;
	}  

	public final DrawTerm getNextLeaf() {
		Drawable p = this;
		for (;;) {
			while (p.pnext() == null && (p=p.parent()) != null);
			if (p == null)
				return null;
			p = p.pnext();
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
			while (p.pprev() == null && (p=p.parent()) != null);
			if (p == null)
				return null;
			p = p.pprev();
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


