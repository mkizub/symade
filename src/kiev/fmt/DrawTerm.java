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

	static int check_stack_overflow;
	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			check_stack_overflow++;
			if (check_stack_overflow > 1000)
				throw new Error();
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
			check_stack_overflow--;
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

	String[] attrs;

	public DrawNodeTerm() {}
	public DrawNodeTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax);
		this.attrs = attr.split("\\.");
		for (int i=0; i < this.attrs.length; i++)
			this.attrs[i] = this.attrs[i].intern();
	}

	String makeText(Formatter fmt) {
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
		String str = String.valueOf(getAttrPtr().get());
		return fmt.escapeString(str);
	}
}

