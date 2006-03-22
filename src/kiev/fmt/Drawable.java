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
	ASTNode			node;
	AttrSlot		dslot;
	// can be text (line/pos) or graphics (pixel x,y,w,h,baseline info) and so on,
	// filled/modified during preFormat/postFormat
	DrawGeometry	geometry;
	// syntax kind & draw layout
	SyntaxElem		layout;
	// current (selected) layout
	int				curr_layout;
	
	public Drawable() {
		this.geometry = new DrawGeometry();
	}
	public Drawable(ASTNode node, SyntaxElem layout) {
		this.node = node;
		this.geometry = new DrawGeometry();
		this.layout = layout;
	}
	public void callbackAttached(ASTNode parent, AttrSlot pslot) {
		super.callbackAttached(parent, pslot);
		if (parent == this.node)
			dslot = pslot;
	}

	public void init(Formatter fmt) {}

	public final int getNewlineKind() { return layout.getNewlineKind(curr_layout); }
	public final int getExtraSpaceRight() { return layout.getExtraSpaceRight(curr_layout); }
	public final int getExtraSpaceLeft() { return layout.getExtraSpaceLeft(curr_layout); }
	public final boolean isRightAssociated() { return layout.isRightAssociated(curr_layout); }
	public final int getIndentKind() { return layout.getIndentKind(curr_layout); }

	public abstract void preFormat(DrawContext cont);
	public abstract boolean postFormat(DrawContext cont, boolean last_layout);
	public abstract Drawable getFirst();
	public abstract Drawable getLast();
	public abstract DrawTerm getFirstLeaf();
	public abstract DrawTerm getLastLeaf();

	public final boolean isUnvisible() {
		if (geometry != null)
			return geometry.is_hidden;
		if (layout != null)
			return layout.isHidden(curr_layout);
		return false;
	}  

	public final DrawTerm getNextLeaf() {
		Drawable p = this;
		for (;;) {
			while (p.pnext == null && (p=p.parent) != null);
			if (p == null)
				return null;
			p = p.pnext;
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
			while (p.pprev == null && (p=p.parent) != null);
			if (p == null)
				return null;
			p = p.pprev;
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


