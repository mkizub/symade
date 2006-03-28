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
public abstract class DrawChoice extends DrawNonTerm {

	@ref Drawable			arg;
	@att NArr<Drawable>		options;

	public DrawChoice() {}
	public DrawChoice(ASTNode node, SyntaxChoice syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxChoice sc = (SyntaxChoice)syntax;
		foreach (SyntaxElem se; sc.elements)
			options.append(se.makeDrawable(fmt, node));
	}

	public DrawTerm getFirstLeaf() { return isUnvisible() || arg == null ? null : arg.getFirstLeaf(); }
	public DrawTerm getLastLeaf()  { return isUnvisible() || arg == null ? null : arg.getLastLeaf();  }

	public void preFormat(DrawContext cont) {
		if (arg == null)
			this.geometry.is_hidden = true;
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


