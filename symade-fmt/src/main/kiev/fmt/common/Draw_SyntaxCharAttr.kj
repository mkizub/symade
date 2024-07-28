package kiev.fmt.common;

import kiev.fmt.DrawCharTerm;
import kiev.fmt.DrawErrorTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

public class Draw_SyntaxCharAttr extends Draw_SyntaxAttr {
	private static final long serialVersionUID = 3628954390935803144L;
	public Draw_SyntaxCharAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			ScalarAttrSlot attr_slot = getScalarAttrSlot(node);
			if (attr_slot == null)
				return new DrawErrorTerm(node, fmt, this, "<?error:"+this.name+"?>");
			return new DrawCharTerm(node, fmt, this, attr_slot);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

