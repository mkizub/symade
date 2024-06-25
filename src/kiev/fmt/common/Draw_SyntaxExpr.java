package kiev.fmt.common;

import kiev.fmt.DrawLispExpr;
import kiev.fmt.Drawable;
import kiev.vlang.ENode;
import kiev.vtree.INode;

public class Draw_SyntaxExpr extends Draw_SyntaxElem {
	private static final long serialVersionUID = 4816928333044654372L;
	public Draw_SyntaxExprTemplate			template;
	public Draw_SyntaxAttr[]				attrs = Draw_SyntaxAttr.emptyArray;

	public Draw_SyntaxExpr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		if (node instanceof ENode && ((ENode)node).getOper() != null)
			return fmt.getDrawable(node, null);
		return new DrawLispExpr(node, fmt, this);
	}

	public boolean check(Formatter fmt, Drawable curr_dr, INode expected_node) {
		if (expected_node != curr_dr.drnode)
			return false;
		if (expected_node instanceof ENode && ((ENode)expected_node).getOper() == null)
			return true;
		return false;
	}
}

