package kiev.fmt.common;

import kiev.fmt.DrawSubAttr;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxSubAttr extends Draw_SyntaxAttr {
	private static final long serialVersionUID = -5596546913410820085L;
	public Draw_SyntaxSubAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawSubAttr(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

