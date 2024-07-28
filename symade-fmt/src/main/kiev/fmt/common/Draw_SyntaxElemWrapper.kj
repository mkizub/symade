package kiev.fmt.common;

import kiev.fmt.DrawElemWrapper;
import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxElemWrapper extends Draw_SyntaxAttr {
	private static final long serialVersionUID = -7622927917061590545L;
	public Draw_SyntaxAttr					element;

	public Draw_SyntaxElemWrapper(Draw_SyntaxAttr element) {
		this(element, null);
	}
	
	public Draw_SyntaxElemWrapper(Draw_SyntaxAttr element, Draw_Paragraph par) {
		super(element.elem_decl);
		this.element = element;
		this.name = element.name;
		this.in_syntax = element.in_syntax;
		this.par = element.par;
		this.lout = element.lout;
		element.par = par;
		element.lout = new Draw_Layout();
	}
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		fmt.pushSyntax(in_syntax);
		try {
			return new DrawElemWrapper(node, fmt, this);
		} finally {
			fmt.popSyntax(in_syntax);
		}
	}
}

