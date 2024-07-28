package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.fmt.Drawable;
import kiev.vtree.INode;

public class Draw_SyntaxElem implements Serializable {
	private static final long serialVersionUID = -3384347945904357661L;
	public static final Draw_SyntaxElem[] emptyArray = new Draw_SyntaxElem[0];
	
	final
	public Draw_SyntaxElemDecl				elem_decl;
	public Draw_Paragraph					par;
	public Draw_Layout						lout = new Draw_Layout();
	public String[]							style_names;
	
	public Draw_SyntaxElem(Draw_SyntaxElemDecl elem_decl) {
		this.elem_decl = elem_decl;
	}

	public Drawable makeDrawable(Formatter fmt, INode node) {
		throw new AbstractMethodError(this.getClass()+".makeDrawable");
	}

	public boolean check(Formatter fmt, Drawable curr_dr, INode expected_node) {
		if (curr_dr.syntax != this || expected_node != curr_dr.drnode)
			return false;
		return true;
	}
	
	Object readResolve() throws ObjectStreamException {
		if (this.style_names != null) {
			for (int i=0; i < this.style_names.length; i++)
				this.style_names[i] = this.style_names[i].intern();
		}
		return this;
	}
}

