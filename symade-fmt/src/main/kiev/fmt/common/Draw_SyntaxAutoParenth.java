package kiev.fmt.common;

import kiev.fmt.DrawAutoParenth;
import kiev.fmt.Drawable;
import kiev.vlang.ENode;
import kiev.vtree.INode;

public class Draw_SyntaxAutoParenth extends Draw_SyntaxElem {
	private static final long serialVersionUID = 7990778417174522581L;
	public Draw_SyntaxExprTemplate			template;
	public Draw_SyntaxElem					attr;
	public int								priority;
	
	public Draw_SyntaxAutoParenth(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }

	public Draw_SyntaxAutoParenth(Draw_SyntaxElemDecl elem_decl, Draw_SyntaxElem attr, int priority, Draw_SyntaxExprTemplate template) {
		super(elem_decl);
		this.template = template;
		this.attr = attr;
		this.priority = priority;
	}

	public Draw_SyntaxAutoParenth(Draw_SyntaxElemDecl elem_decl, String attr_name, int priority, Draw_SyntaxExprTemplate template) {
		super(elem_decl);
		this.template = template;
		Draw_SyntaxSubAttr sa = new Draw_SyntaxSubAttr(elem_decl);
		sa.name = attr_name;
		this.attr = sa;
		this.priority = priority;
	}

	public Drawable makeDrawable(Formatter fmt, INode node) {
		Object obj;
		if (attr instanceof Draw_SyntaxNode) {
			obj = node;
		} else try {
			obj = node.getVal(node.getAttrSlot(((Draw_SyntaxAttr)attr).name));
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			ENode en = (ENode)obj;
			if (en.isPrimaryExpr() || en.getPriority(fmt.env) < this.priority)
				return new DrawAutoParenth(node, fmt, this);
		}
		return attr.makeDrawable(fmt, node);
	}

	public boolean check(Formatter fmt, Drawable curr_dr, INode expected_node) {
		Object obj;
		if (attr instanceof Draw_SyntaxNode) {
			obj = expected_node;
		} else try {
			obj = expected_node.getVal(expected_node.getAttrSlot(((Draw_SyntaxAttr)attr).name));
		} catch (RuntimeException e) {
			obj = "";
		}
		if (obj instanceof ENode) {
			ENode en = (ENode)obj;
			if (en.isPrimaryExpr() || en.getPriority(fmt.env) < this.priority)
				return expected_node == curr_dr.drnode;
		}
		return attr.check(fmt, curr_dr, expected_node);
	}
}

