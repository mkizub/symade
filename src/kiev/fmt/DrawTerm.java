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
	protected String text;

	public DrawTerm() {}
	public DrawTerm(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	
	public DrawTerm getFirstLeaf() { return isUnvisible() ? null : this; }
	public DrawTerm getLastLeaf()  { return isUnvisible() ? null : this; }

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
		}
		if (this.isUnvisible())
			return;
		this.geometry.x = 0;
		this.geometry.y = 0;
		this.text = makeText(cont.fmt);
		cont.formatAsText(this);
	}

	public final boolean postFormat(DrawContext cont, boolean last_layout) {
		this.geometry.do_newline = 0;
		return cont.addLeaf(this);
	}
	
	abstract String makeText(Formatter fmt);
	public final String getText() { return text; }
}

@node
public final class DrawToken extends DrawTerm {

	public DrawToken() {}
	public DrawToken(ANode node, SyntaxToken syntax) {
		super(node, syntax);
	}

	String makeText(Formatter fmt) { return ((SyntaxToken)this.syntax).text; } 
}

@node
public class DrawNodeTerm extends DrawTerm {

	String attr;

	public DrawNodeTerm() {}
	public DrawNodeTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax);
		this.attr = attr.intern();
	}

	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
		if (o == null)
			return null;
		return String.valueOf(o);
	}
	
	public final AttrPtr getAttrPtr() {
		return node.getAttrPtr(attr);
	}
}

@node
public class DrawCharTerm extends DrawNodeTerm {
	public DrawCharTerm() {}
	public DrawCharTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	String makeText(Formatter fmt) {
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

	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
		if (o == null)
			return null;
		String str = String.valueOf(o);
		return fmt.escapeString(str);
	}
}

