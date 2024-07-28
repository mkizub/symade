package kiev.fmt.common;

import kiev.fmt.DrawJavaPackedField;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxJavaPackedField extends Draw_SyntaxElem {
	private static final long serialVersionUID = 6639152413918604918L;
	public Draw_SyntaxJavaPackedField(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawJavaPackedField(node, fmt, this);
		return dr;
	}
}

