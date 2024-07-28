package kiev.fmt.common;

import kiev.fmt.DrawJavaAccess;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxJavaAccess extends Draw_SyntaxElem {
	private static final long serialVersionUID = -1658011156725604975L;
	public Draw_SyntaxJavaAccess(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawJavaAccess(node, fmt, this);
		return dr;
	}
}

