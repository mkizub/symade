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
	public DrawCtrlTerm(ASTNode node, SyntaxElem layout) {
		super(node, layout);
	}

}

@node
public class DrawOptional extends DrawCtrlTerm {

	@att Drawable arg;

	public DrawOptional() {}
	public DrawOptional(ASTNode node, SyntaxOptional layout) {
		super(node, layout);
	}

	public void init(Formatter fmt) {
		arg = ((SyntaxOptional)layout).element.makeDrawable(fmt, node);
	}

	public Drawable getFirst() { return arg; }
	public Drawable getLast() { return arg; }
	public DrawTerm getFirstLeaf() {
		if (this.isUnvisible())
			return null;
		return arg.getFirstLeaf();
	}
	public DrawTerm getLastLeaf()  {
		if (this.isUnvisible())
			return null;
		return arg.getLastLeaf();
	}

	public void preFormat(DrawContext cont) {
		SyntaxOptional so = (SyntaxOptional)layout;
		Object obj = node.getVal(so.opt.name);
		boolean hidden = false;
		if (obj == null)
			hidden = true;
		else if (obj instanceof Boolean)
			hidden = !obj.booleanValue();
		else if (obj instanceof String)
			hidden = obj.length() == 0;
		else if (obj instanceof Struct)
			hidden = obj == Env.root;
		else if (obj instanceof TypeNameRef)
			hidden = obj.name == KString.Empty;
		geometry.is_hidden = hidden;
		if (this.isUnvisible())
			return;
		this.geometry.x = 0;
		this.geometry.y = 0;
		arg.preFormat(cont);
	}

	public final boolean postFormat(DrawContext context, boolean parent_last_layout) {
		this.geometry.do_newline = 0;
		return arg.postFormat(context, parent_last_layout);
	}
}


