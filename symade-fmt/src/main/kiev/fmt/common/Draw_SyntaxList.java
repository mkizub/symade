package kiev.fmt.common;

import kiev.fmt.DrawNonTermList;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxList extends Draw_SyntaxAttr {
	private static final long serialVersionUID = -5099833920999174978L;
	public Draw_SyntaxElem					element;
	public Draw_CalcOption					filter;

	public Draw_SyntaxList(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawNonTermList(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

