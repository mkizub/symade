package kiev.fmt.common;

import kiev.fmt.DrawErrorTerm;
import kiev.fmt.DrawStrTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

public class Draw_SyntaxStrAttr extends Draw_SyntaxAttr {
	private static final long serialVersionUID = -1689259369964188143L;
	public Draw_SyntaxStrAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			ScalarAttrSlot attr_slot = getScalarAttrSlot(node);
			if (attr_slot == null)
				return new DrawErrorTerm(node, fmt, this, "<?error:"+this.name+"?>");
			return new DrawStrTerm(node, fmt, this, attr_slot);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

