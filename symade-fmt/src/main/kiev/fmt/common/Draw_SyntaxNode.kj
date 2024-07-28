package kiev.fmt.common;

import kiev.fmt.DrawNode;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxNode extends Draw_SyntaxElem {
	private static final long serialVersionUID = -9075422413292203835L;

	public Draw_ATextSyntax					in_syntax;
	public Draw_SyntaxElem					prefix;
	public Draw_SyntaxElem					sufix;
	public Draw_SyntaxElem					empty;

	public Draw_SyntaxNode(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawNode(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

