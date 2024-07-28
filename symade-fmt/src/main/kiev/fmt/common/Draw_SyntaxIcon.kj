package kiev.fmt.common;

import kiev.fmt.DrawIcon;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxIcon extends Draw_SyntaxElem implements Cloneable {
	private static final long serialVersionUID = 1040843537786842347L;
	public Draw_Icon					icon;

	public Draw_SyntaxIcon(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	public Draw_SyntaxIcon(Draw_SyntaxElemDecl elem_decl, String icon_name) {
		super(elem_decl);
		this.icon = new Draw_Icon(icon_name);
	}
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawIcon(node, fmt, this);
		return dr;
	}
}

