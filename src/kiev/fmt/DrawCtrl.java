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

@node(copyable=false)
public class DrawCtrl extends Drawable {
	@att
	public Drawable arg;
	
	public DrawCtrl(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}

	public String getText() {
		if (arg != null)
			return arg.getText();
		return "???";
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

@node(copyable=false)
public class DrawSpace extends DrawCtrl {

	public DrawSpace(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
	}

}

@node(copyable=false)
public class DrawOptional extends DrawCtrl {

	@att
	public	boolean draw_optional;
	private	boolean drawed_as_true;
	
	public DrawOptional(ANode node, SyntaxOptional syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxOptional sc = (SyntaxOptional)syntax;
		if (sc.calculator.calc(node)) {
			if (!drawed_as_true || arg == null) {
				drawed_as_true = true;
				if (sc.opt_true != null) {
					arg = sc.opt_true.makeDrawable(cont.fmt, node);
				} else {
					arg = null;
				}
			}
		} else {
			if (drawed_as_true || arg == null) {
				drawed_as_true = false;
				if (sc.opt_false != null) {
					arg = sc.opt_false.makeDrawable(cont.fmt, node);
				} else {
					arg = null;
				}
			}
		}
		if (arg != null) {
			if (drawed_as_true) {
				arg.preFormat(cont,sc.opt_true,node);
			} else {
				if (sc.opt_false.fmt.is_hidden && !draw_optional)
					arg = null;
				else
					arg.preFormat(cont,sc.opt_false,node);
			}
		}
	}
}

@node(copyable=false)
public final class DrawFolded extends DrawCtrl {

	@att
	public	boolean draw_folded;
	private	boolean drawed_as_folded;
	
	public DrawFolded(ANode node, SyntaxFolder syntax) {
		super(node, syntax);
		this.draw_folded = syntax.folded_by_default;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxFolder sc = (SyntaxFolder)syntax;
		if (draw_folded) {
			if (!drawed_as_folded || arg == null) {
				drawed_as_folded = true;
				arg = sc.folded.makeDrawable(cont.fmt, node);
			}
		} else {
			if (drawed_as_folded || arg == null) {
				drawed_as_folded = false;
				arg = sc.unfolded.makeDrawable(cont.fmt, node);
			}
		}
		if (drawed_as_folded)
			arg.preFormat(cont,sc.folded,node);
		else
			arg.preFormat(cont,sc.unfolded,node);
	}
}

@node(copyable=false)
public class DrawIntChoice extends DrawCtrl {

	private int drawed_idx;

	public DrawIntChoice(ANode node, SyntaxIntChoice syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxIntChoice sc = (SyntaxIntChoice)syntax;
		int idx = ((Integer)node.getVal(sc.name)).intValue();
		if (arg == null || drawed_idx != idx) {
			if (idx < 0 || idx >= sc.elements.length)
				arg = null;
			else
				arg = sc.elements[idx].makeDrawable(cont.fmt, node);
			drawed_idx = idx;
		}
		if (arg != null)
			arg.preFormat(cont,sc.elements[idx],node);
		
	}
}

@node(copyable=false)
public class DrawEnumChoice extends DrawCtrl {

	private Enum drawed_en;

	public DrawEnumChoice(ANode node, SyntaxEnumChoice syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxEnumChoice se = (SyntaxEnumChoice)syntax;
		java.lang.Enum en = (java.lang.Enum)node.getVal(se.name);
		if (arg == null || drawed_en != en) {
			if (en.ordinal() >= se.elements.length)
				arg = null;
			else
				arg = se.elements[en.ordinal()].makeDrawable(cont.fmt, node);
			drawed_en = en;
		}
		if (arg != null)
			arg.preFormat(cont,se.elements[en.ordinal()],node);
	}
}

@node(copyable=false)
public class DrawParagraph extends DrawCtrl {

	boolean is_multiline;
	
	public DrawParagraph(ANode node, SyntaxParagraphLayout syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxParagraphLayout spl = (SyntaxParagraphLayout)syntax;
		if (arg == null)
			arg = spl.elem.makeDrawable(cont.fmt, node);
		if (arg != null)
			arg.preFormat(cont,spl.elem,node);
	}

	public ParagraphLayout getParLayout() {
		return (ParagraphLayout)((SyntaxParagraphLayout)syntax).par.dnode;
	}
}

