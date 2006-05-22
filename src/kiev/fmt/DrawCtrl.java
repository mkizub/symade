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
public class DrawCtrl extends Drawable {
	@att
	public Drawable arg;
	
	public DrawCtrl() {}
	public DrawCtrl(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}

	public DrawTerm getFirstLeaf() {
		if (arg == null || this.isUnvisible())
			return null;
		return arg.getFirstLeaf();
	}
	public DrawTerm getLastLeaf()  {
		if (arg == null || this.isUnvisible())
			return null;
		return arg.getLastLeaf();
	}

	public void preFormat(DrawContext cont) {
		if (arg == null || this.isUnvisible())
			return;
		arg.preFormat(cont);
	}

	public boolean postFormat(DrawContext context, boolean parent_last_layout) {
		context.pushDrawable(this);
		try {
			if (arg != null)
				return arg.postFormat(context, parent_last_layout);
			return true;
		} finally {
			context.popDrawable(this);
		}
	}

}

@node
public class DrawSpace extends DrawCtrl {
	public DrawSpace() {}
	public DrawSpace(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}
}

@node
public class DrawOptional extends DrawCtrl {

	@ref Drawable dr_true;
	@ref Drawable dr_false;
	
	public DrawOptional() {}
	public DrawOptional(ASTNode node, SyntaxOptional syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxOptional sc = (SyntaxOptional)syntax;
		if (sc.opt_true != null)
			dr_true = sc.opt_true.makeDrawable(fmt, node);
		if (sc.opt_false != null)
			dr_false = sc.opt_false.makeDrawable(fmt, node);
	}

	public void preFormat(DrawContext cont) {
		SyntaxOptional sc = (SyntaxOptional)syntax;
		if (sc.calculator.calc(node))
			arg = dr_true;
		else
			arg = dr_false;
		super.preFormat(cont);
	}
}

@node
public class DrawIntChoice extends DrawCtrl {

	@ref Drawable[] args;

	public DrawIntChoice() {}
	public DrawIntChoice(ASTNode node, SyntaxIntChoice syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxIntChoice sset = (SyntaxIntChoice)this.syntax;
		foreach (SyntaxElem se; sset.elements)
			args.append(se.makeDrawable(fmt, node));
	}

	public void preFormat(DrawContext cont) {
		SyntaxIntChoice sc = (SyntaxIntChoice)syntax;
		int idx = ((Integer)node.getVal(sc.name)).intValue();
		if (idx >= 0 && idx < args.length)
			arg = args[idx];
		else
			arg = null;
		super.preFormat(cont);
	}
}

@node
public class DrawEnumChoice extends DrawCtrl {

	@ref Drawable[] args;

	public DrawEnumChoice() {}
	public DrawEnumChoice(ASTNode node, SyntaxEnumChoice syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxEnumChoice sset = (SyntaxEnumChoice)this.syntax;
		foreach (SyntaxElem se; sset.elements)
			args.append(se.makeDrawable(fmt, node));
	}

	public void preFormat(DrawContext cont) {
		SyntaxEnumChoice sc = (SyntaxEnumChoice)syntax;
		java.lang.Enum en = (java.lang.Enum)node.getVal(sc.name);
		if (en == null || en.ordinal() >= args.length)
			arg = null;
		else
			arg = args[en.ordinal()];
		super.preFormat(cont);
	}
}

@node
public class DrawParagraph extends DrawCtrl {

	boolean is_multiline;
	
	public DrawParagraph() {}
	public DrawParagraph(ASTNode node, SyntaxParagraphLayout syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxParagraphLayout sc = (SyntaxParagraphLayout)syntax;
		arg = sc.elem.makeDrawable(fmt, node);
	}

	public ParagraphLayout getParLayout() {
		return ((SyntaxParagraphLayout)syntax).par;
	}
}


