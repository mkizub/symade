package kiev.fmt.common;

import kiev.fmt.DrawFolded;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxFolder extends Draw_SyntaxElem {
	private static final long serialVersionUID = 7621558209145701674L;
	public boolean							folded_by_default;
	public Draw_SyntaxElem					folded;
	public Draw_SyntaxElem					unfolded;

	public Draw_SyntaxFolder(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawFolded(node, fmt, this);
		return dr;
	}
}

