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
public abstract class DrawTerm extends Drawable {
	Formatter fmt;
	public DrawTerm() {}
	public DrawTerm(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	
	public void init(Formatter fmt) {
		super.init(fmt);
		this.fmt = fmt;
	}

	public DrawTerm getFirstLeaf() { return isUnvisible() ? null : this; }
	public DrawTerm getLastLeaf()  { return isUnvisible() ? null : this; }

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		this.geometry.x = 0;
		this.geometry.y = 0;
		cont.formatAsText(this);
	}

	public final boolean postFormat(DrawContext cont, boolean last_layout) {
		this.geometry.do_newline = 0;
		return cont.addLeaf(this);
	}
	
	public abstract String getText();
}

@node
public final class DrawToken extends DrawTerm {

	public DrawToken() {}
	public DrawToken(ANode node, SyntaxToken syntax) {
		super(node, syntax);
	}

	public String getText() { return ((SyntaxToken)this.syntax).text; }
}

@node
public class DrawNodeTerm extends DrawTerm {

	String[] attrs;

	public DrawNodeTerm() {}
	public DrawNodeTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax);
		this.attrs = attr.split("\\.");
		for (int i=0; i < this.attrs.length; i++)
			this.attrs[i] = this.attrs[i].intern();
	}

	public String getText() {
		return String.valueOf(getAttrPtr().get());
	}
	
	public final AttrPtr getAttrPtr() {
		ANode n = node;
		for (int i=0; i < attrs.length-1; i++) {
			n = (ANode)n.getVal(attrs[i]);
		}
		String attr = attrs[attrs.length-1];
		if (attr == "")
			return new AttrPtr(n.parent(), n.pslot());
		return n.getAttrPtr(attr);
	}
}

@node
public class DrawCharTerm extends DrawNodeTerm {
	public DrawCharTerm() {}
	public DrawCharTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	public String getText() {
		Character ch = (Character)getAttrPtr().get();
		return fmt.escapeChar(ch.charValue());
	}
}

@node
public class DrawStrTerm extends DrawNodeTerm {
	public DrawStrTerm() {}
	public DrawStrTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	public String getText() {
		String str = String.valueOf(getAttrPtr().get());
		return fmt.escapeString(str);
	}
}

