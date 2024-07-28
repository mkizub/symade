package kiev.fmt.common;

import kiev.fmt.DrawOptional;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxOptional extends Draw_SyntaxElem {
	private static final long serialVersionUID = 7584341725551810893L;
	public Draw_CalcOption					calculator;
	public Draw_SyntaxElem					opt_true;
	public Draw_SyntaxElem					opt_false;

	public Draw_SyntaxOptional(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawOptional(node, fmt, this);
		return dr;
	}
}
