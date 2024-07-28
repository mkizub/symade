package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.fmt.DrawToken;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxToken extends Draw_SyntaxElem implements Cloneable {
	private static final long serialVersionUID = 8757692557080305967L;
	public String							text;

	public Draw_SyntaxToken(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	public Draw_SyntaxToken(Draw_SyntaxElemDecl elem_decl, String text) {
		super(elem_decl);
		this.text = text;
	}
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawToken(node, fmt, this);
		return dr;
	}
	
	public Draw_SyntaxToken copyWithText(String text) {
		Draw_SyntaxToken st = null;
		try {
			st = (Draw_SyntaxToken)this.clone();
		} catch (CloneNotSupportedException e) {}
		st.text = text;
		return st;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.text != null) this.text = this.text.intern();
		return this;
	}
}

