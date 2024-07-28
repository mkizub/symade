package kiev.fmt.common;

import kiev.fmt.DrawErrorTerm;
import kiev.fmt.DrawTokenTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

public class Draw_SyntaxTokenAttr extends Draw_SyntaxAttr {
	private static final long serialVersionUID = 6840414617142977006L;

	public Draw_SyntaxTokenAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			ScalarAttrSlot attr_slot = getScalarAttrSlot(node);
			if (attr_slot == null)
				return new DrawErrorTerm(node, fmt, this, "<?error:"+this.name+"?>");
			return new DrawTokenTerm(node, fmt, this, attr_slot);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

