package kiev.fmt.common;

import kiev.fmt.DrawErrorTerm;
import kiev.fmt.DrawIdent;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

public class Draw_SyntaxIdentAttr extends Draw_SyntaxAttr {
	private static final long serialVersionUID = 340634908478187473L;
	public Draw_SyntaxIdentTemplate		template;

	public Draw_SyntaxIdentAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			ScalarAttrSlot attr_slot = getScalarAttrSlot(node);
			if (attr_slot == null)
				return new DrawErrorTerm(node, fmt, this, "<?error:"+this.name+"?>");
			return new DrawIdent(node, fmt, this, attr_slot);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}

	public boolean isOk(String text) {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null) return true;
		if (t.pattern != null && !t.pattern.matcher(text).matches())
			return false;
		for (String cs : t.keywords)
			if (cs == text)
				return false;
		return true;
	}
	
	public String getPrefix() {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null || t.esc_prefix == null) return "";
		return t.esc_prefix;
	}	
	public String getSuffix() {
		Draw_SyntaxIdentTemplate t = template;
		if (t == null || t.esc_suffix == null) return "";
		return t.esc_suffix;
	}	

}

