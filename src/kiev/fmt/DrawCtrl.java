package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node
public abstract class DrawCtrlTerm extends Drawable {

	public DrawCtrlTerm() {}
	public DrawCtrlTerm(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}

}

@node
public class DrawOptional extends DrawCtrlTerm {

	@ref Drawable arg;
	@att Drawable elem;
	@att Drawable altern;
	boolean expr_not;
	String[] attrs;

	public DrawOptional() {}
	public DrawOptional(ASTNode node, SyntaxOptional syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxOptional so = (SyntaxOptional)syntax;
		this.elem = so.element.makeDrawable(fmt, node);
		if (so.altern != null)
			this.altern = so.altern.makeDrawable(fmt, node);
		String prop = so.prop;
		if (prop.charAt(0) == '!') {
			expr_not = true;
			prop = prop.substring(1);
		}
		this.attrs = prop.split("\\.");
		for (int i=0; i < this.attrs.length; i++)
			this.attrs[i] = this.attrs[i].intern();
	}

	public DrawTerm getFirstLeaf() { return isUnvisible() || arg == null ? null : arg.getFirstLeaf(); }
	public DrawTerm getLastLeaf()  { return isUnvisible() || arg == null ? null : arg.getLastLeaf();  }

	public void preFormat(DrawContext cont) {
		boolean alt = false;
		Object obj = node;
		for (int i=0; i < attrs.length; i++) {
			if (obj instanceof ASTNode) {
				if (attrs[i] == "parent")
					obj = obj.parent;
				else
					obj = obj.getVal(attrs[i]);
			} else {
				alt = true;
				break;
			}
		}
		if (obj == null)
			alt = true;
		else if (obj instanceof Boolean)
			alt = !obj.booleanValue();
		else if (obj instanceof String)
			alt = obj.length() == 0;
		else if (obj instanceof KString)
			alt = obj.len == 0;
		if (expr_not)
			alt = !alt;
		
		if (alt) {
			if (altern != null) {
				arg = altern;
				geometry.is_hidden = false;
			} else {
				arg = null;
				geometry.is_hidden = true;
			}
		} else {
			arg = elem;
			geometry.is_hidden = false;
		}
		if (this.isUnvisible())
			return;
		this.geometry.x = 0;
		this.geometry.y = 0;
		arg.preFormat(cont);
	}

	public final boolean postFormat(DrawContext context, boolean parent_last_layout) {
		this.geometry.do_newline = 0;
		if (arg == null)
			return true;
		return arg.postFormat(context, parent_last_layout);
	}
}


