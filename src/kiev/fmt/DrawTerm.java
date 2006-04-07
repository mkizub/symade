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
	public DrawTerm() {}
	public DrawTerm(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
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
public class DrawKeyword extends DrawTerm {

	public DrawKeyword() {}
	public DrawKeyword(ASTNode node, SyntaxKeyword syntax) {
		super(node, syntax);
	}

	public String getText() { return ((SyntaxKeyword)this.syntax).text; }
}

@node
public class DrawOperator extends DrawTerm {
	public DrawOperator() {}
	public DrawOperator(ASTNode node, SyntaxOperator syntax) {
		super(node, syntax);
	}

	public String getText() { return ((SyntaxOperator)this.syntax).text; }
}

@node
public class DrawSeparator extends DrawTerm {
	public DrawSeparator() {}
	public DrawSeparator(ASTNode node, SyntaxSeparator syntax) {
		super(node, syntax);
	}

	public String getText() { return ((SyntaxSeparator)this.syntax).text; }
}

@node
public class DrawNodeTerm extends DrawTerm {

	String[] attrs;

	public DrawNodeTerm() {}
	public DrawNodeTerm(ASTNode node, SyntaxElem syntax, String attr) {
		super(node, syntax);
		this.attrs = attr.split("\\.");
		for (int i=0; i < this.attrs.length; i++)
			this.attrs[i] = this.attrs[i].intern();
	}

	public String getText() {
		Object o = node;
		for (int i=0; i < attrs.length; i++) {
			if (o instanceof ASTNode) {
				if (attrs[i] == "")
					break;
				if (attrs[i] == "parent")
					o = o.parent;
				else
					o = o.getVal(attrs[i]);
			}
			else
				return null;
		}
		return String.valueOf(o);
	}
}

