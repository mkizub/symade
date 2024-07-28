package kiev.fmt.common;

import kiev.fmt.DrawSpace;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxSpace extends Draw_SyntaxElem {
	private static final long serialVersionUID = -5927945464857237540L;
	public Draw_SyntaxSpace(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawSpace(node, fmt, this);
		return dr;
	}
}

