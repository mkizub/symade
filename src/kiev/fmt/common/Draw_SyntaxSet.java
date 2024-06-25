package kiev.fmt.common;

import kiev.fmt.DrawNonTermSet;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxSet extends Draw_SyntaxElem {
	private static final long serialVersionUID = 4173580350421641113L;
	public Draw_SyntaxElem[]				elements = Draw_SyntaxElem.emptyArray;
	//public boolean							nested_function_lookup;

	public Draw_SyntaxSet(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		Drawable dr = new DrawNonTermSet(node, fmt, this);
		return dr;
	}
}

