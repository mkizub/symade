/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
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

	public final int getMaxLayout() {
		int max_layout = syntax.lout.count;
		if (attr_syntax != null)
			max_layout = Math.max(max_layout, attr_syntax.lout.count);
		if (arg != null)
			max_layout = Math.max(max_layout, arg.getMaxLayout());
		return max_layout;
	}

	public void lnkFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		cont.processSpaceBefore(this);
		if (arg != null)
			arg.lnkFormat(cont);
		cont.processSpaceAfter(this);
	}

	public boolean postFormat(DrawContext context) {
		context.pushDrawable(this);
		try {
			if (arg != null)
				return arg.postFormat(context);
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
		ANode node = this.drnode;
		if (sc.calculator == null || sc.calculator.calc(node)) {
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
			if (drawed_as_true)
				arg.preFormat(cont,sc.opt_true,node);
			else
				arg.preFormat(cont,sc.opt_false,node);
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
		ANode node = this.drnode;
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
	private AttrSlot attr;

	public DrawIntChoice(ANode node, SyntaxIntChoice syntax) {
		super(node, syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
		int idx = ((Integer)attr.get(node)).intValue();
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxIntChoice sc = (SyntaxIntChoice)syntax;
		ANode node = this.drnode;
		int idx = ((Integer)attr.get(node)).intValue();
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
public class DrawBoolChoice extends DrawCtrl {

	private Boolean drawed_bool;
	private AttrSlot attr;

	public DrawBoolChoice(ANode node, SyntaxBoolChoice syntax) {
		super(node, syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxBoolChoice sb = (SyntaxBoolChoice)syntax;
		ANode node = this.drnode;
		Boolean val = (Boolean)attr.get(node);
		if (val != null) {
			if (val.booleanValue())
				val = Boolean.TRUE;
			else
				val = Boolean.FALSE;
		}
		SyntaxElem se = null;
		if (val == null)
			se = sb.empty;
		else if (val.booleanValue())
			se = sb.elem_true;
		else
			se = sb.elem_false;
		if (arg == null || drawed_bool != val) {
			if (se != null)
				arg = se.makeDrawable(cont.fmt, node);
			else
				arg = null;
			drawed_bool = val;
		}
		if (arg != null)
			arg.preFormat(cont,se,node);
		
	}
}

@node(copyable=false)
public class DrawEnumChoice extends DrawCtrl {

	private Enum drawed_en;
	private AttrSlot attr;

	public DrawEnumChoice(ANode node, SyntaxEnumChoice syntax) {
		super(node, syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
		java.lang.Enum en = (java.lang.Enum)attr.get(node);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxEnumChoice se = (SyntaxEnumChoice)syntax;
		ANode node = this.drnode;
		java.lang.Enum en = (java.lang.Enum)attr.get(node);
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
public final class DrawParagraph extends DrawCtrl {

	public DrawParagraph(ANode node, SyntaxParagraphLayout syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxParagraphLayout spl = (SyntaxParagraphLayout)syntax;
		ANode node = this.drnode;
		if (arg == null)
			arg = spl.elem.makeDrawable(cont.fmt, node);
		if (arg != null)
			arg.preFormat(cont,spl.elem,node);
	}

	public boolean postFormat(DrawContext context) {
		boolean fits = true;
		context = context.pushParagraph(this, this.getParLayout());
		try {
			if (arg != null) {
				fits = arg.postFormat(context);
				if (!context.new_lines_first_parent)
					fits = true;
			}
		} finally {
			context.popParagraph(this, fits);
		}
		return fits;
	}

	public AParagraphLayout getParLayout() {
		return ((SyntaxParagraphLayout)syntax).par.dnode;
	}
}

