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

import syntax kiev.Syntax;

@node(copyable=false)
public class DrawCtrl extends Drawable {
	@att
	public Drawable arg;
	
	public DrawCtrl(ANode node, SyntaxElem syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
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
		return syntax.lout.count;
	}

	public void lnkFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		cont.processSpaceBefore(this);
		if (arg != null)
			arg.lnkFormat(cont);
		cont.processSpaceAfter(this);
	}

	public void postFormat(DrawContext context) {
		if (arg != null) {
			context = context.pushDrawable(this);
			try {
				arg.postFormat(context);
			} finally {
				context.popDrawable(this);
			}
		}
	}

}

@node(copyable=false)
public class DrawSpace extends DrawCtrl {

	public DrawSpace(ANode node, SyntaxElem syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
	}

}

@node(copyable=false)
public class DrawSubAttr extends DrawCtrl {

	public DrawSubAttr(ANode node, SyntaxSubAttr syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxSubAttr sn = (SyntaxSubAttr)syntax;
		ANode node = this.drnode;
		ATextSyntax text_syntax = this.text_syntax;
		if (sn.in_syntax.dnode != null)
			text_syntax = sn.in_syntax.dnode;
		Object obj;
		try {
			obj = node.getVal(sn.name);
		} catch (RuntimeException e) {
			obj = "<?error:"+sn.name+"?>";
		}
		if (arg == null) {
			if (obj instanceof ANode)
				arg = cont.fmt.getDrawable((ANode)obj, null, text_syntax);
			else if (obj == null && sn.empty != null)
				arg = sn.empty.makeDrawable(cont.fmt, node, text_syntax);
			else
				arg = new DrawNodeTerm(node, sn, text_syntax, sn.name);
		}
		if (arg != null) {
			if (obj instanceof ANode)
				arg.preFormat(cont);
			else if (obj == null && sn.empty != null)
				arg.preFormat(cont, sn.empty, text_syntax);
			else
				arg.preFormat(cont);
		}
	}
}

@node(copyable=false)
public class DrawNode extends DrawCtrl {

	public DrawNode(ANode node, SyntaxNode syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxNode sn = (SyntaxNode)syntax;
		ANode node = this.drnode;
		ATextSyntax text_syntax = this.text_syntax;
		if (sn.in_syntax.dnode != null)
			text_syntax = sn.in_syntax.dnode;
		if (arg == null) {
			if (node != null) {
				arg = cont.fmt.getDrawable(node, null, text_syntax);
				if (arg != null)
					arg.preFormat(cont);
			}
			else if (sn.empty != null) {
				arg = sn.empty.makeDrawable(cont.fmt, node, text_syntax);
				if (arg != null)
					arg.preFormat(cont, sn.empty, text_syntax);
			}
		}
		if (arg != null) {
			if (node != null)
				arg.preFormat(cont);
			else if (sn.empty != null)
				arg.preFormat(cont, sn.empty, text_syntax);
		}
	}
}

@node(copyable=false)
public class DrawOptional extends DrawCtrl {

	private	boolean drawed_as_true;
	
	public DrawOptional(ANode node, SyntaxOptional syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxOptional sc = (SyntaxOptional)syntax;
		ANode node = this.drnode;
		if (sc.calculator == null || sc.calculator.calc(node)) {
			if (!drawed_as_true || arg == null) {
				drawed_as_true = true;
				if (sc.opt_true != null) {
					arg = sc.opt_true.makeDrawable(cont.fmt, node, text_syntax);
				} else {
					arg = null;
				}
			}
		} else {
			if (drawed_as_true || arg == null) {
				drawed_as_true = false;
				if (sc.opt_false != null) {
					arg = sc.opt_false.makeDrawable(cont.fmt, node, text_syntax);
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
public class DrawEnumChoice extends DrawCtrl {

	private Object drawed_en;
	private AttrSlot attr;

	public DrawEnumChoice(ANode node, SyntaxEnumChoice syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		foreach (AttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxEnumChoice se = (SyntaxEnumChoice)syntax;
		ANode node = this.drnode;
		Object en = attr.get(node);
		int ord = -1;
		if (en instanceof Boolean)
			ord = en.booleanValue() ? 1 : 0;
		else if (en instanceof Enum)
			ord = en.ordinal();
		else
			arg = null;
		if (arg == null || drawed_en != en) {
			if (ord < 0 || ord >= se.elements.length)
				arg = null;
			else
				arg = se.elements[ord].makeDrawable(cont.fmt, node, text_syntax);
			drawed_en = en;
		}
		if (arg != null)
			arg.preFormat(cont,se.elements[ord],node);
	}
}

@node(copyable=false)
public final class DrawFolded extends DrawCtrl {

	@att
	public	boolean draw_folded;
	private	boolean drawed_as_folded;
	
	public DrawFolded(ANode node, SyntaxFolder syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.draw_folded = syntax.folded_by_default;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxFolder sc = (SyntaxFolder)syntax;
		ANode node = this.drnode;
		if (draw_folded) {
			if (!drawed_as_folded || arg == null) {
				drawed_as_folded = true;
				arg = sc.folded.makeDrawable(cont.fmt, node, text_syntax);
			}
		} else {
			if (drawed_as_folded || arg == null) {
				drawed_as_folded = false;
				arg = sc.unfolded.makeDrawable(cont.fmt, node, text_syntax);
			}
		}
		if (drawed_as_folded)
			arg.preFormat(cont,sc.folded,node);
		else
			arg.preFormat(cont,sc.unfolded,node);
	}
}

