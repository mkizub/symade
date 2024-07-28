package kiev.fmt.common;

import kiev.fmt.DrawTreeBranch;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxTreeBranch extends Draw_SyntaxAttr {
	private static final long serialVersionUID = 1993172189372203968L;
	public Draw_SyntaxElem					folded;
	public Draw_SyntaxElem					element;
	public Draw_CalcOption					filter;

	public Draw_SyntaxTreeBranch(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawTreeBranch(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}
