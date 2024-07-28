package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ScalarAttrSlot;

public abstract class Draw_SyntaxAttr extends Draw_SyntaxElem {
	private static final long serialVersionUID = -2302590344239937477L;
	public static final Draw_SyntaxAttr[] emptyArray = new Draw_SyntaxAttr[0];

	public String							name;
	public Draw_ATextSyntax					in_syntax;
	public Draw_SyntaxElem					prefix;
	public Draw_SyntaxElem					sufix;
	public Draw_SyntaxElem					empty;

	public transient AttrSlot				attr_slot;

	public Draw_SyntaxAttr(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
	
	protected ScalarAttrSlot getScalarAttrSlot(INode node) {
		if (this.attr_slot instanceof ScalarAttrSlot)
			return (ScalarAttrSlot)this.attr_slot;
		for (AttrSlot a : node.values()) {
			if (a.name == this.name && a instanceof ScalarAttrSlot)
				return (ScalarAttrSlot)a;
		}
		return null;
	}
}

