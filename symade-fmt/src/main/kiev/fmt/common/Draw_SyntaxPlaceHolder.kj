package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxPlaceHolder extends Draw_SyntaxElem {
	private static final long serialVersionUID = 571440015215472617L;
	public String							text;

	public Draw_SyntaxPlaceHolder(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawPlaceHolder(node, fmt, this);
		return dr;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.text != null) this.text = this.text.intern();
		return this;
	}
}

