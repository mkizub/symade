package kiev.fmt.common;

import kiev.fmt.DrawEnumChoice;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxEnumChoice extends Draw_SyntaxAttr {
	private static final long serialVersionUID = -6110636984918109945L;
	public Draw_SyntaxElem[]				elements = Draw_SyntaxElem.emptyArray;

	public Draw_SyntaxEnumChoice(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawEnumChoice(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

