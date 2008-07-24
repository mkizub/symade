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

@ThisIsANode(copyable=false)
public abstract class DrawCtrl extends Drawable {
	@nodeAttr
	public Drawable arg;
	
	public DrawCtrl(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	// for GUI
	public Drawable getArg() { this.arg }

	public Drawable getNextChild(Drawable dr) {
		assert (dr == arg);
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		assert (dr == arg);
		return null;
	}
	public Drawable[] getChildren() {
		if (arg == null)
			return Drawable.emptyArray;
		return new Drawable[]{arg};
	}
}

@ThisIsANode(copyable=false)
public class DrawSpace extends DrawCtrl {

	public DrawSpace(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
	}

}

@ThisIsANode(copyable=false)
public class DrawSubAttr extends DrawCtrl {

	public DrawSubAttr(ANode node, Draw_SyntaxSubAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSubAttr sn = (Draw_SyntaxSubAttr)syntax;
		ANode node = this.drnode;
		Draw_ATextSyntax text_syntax = this.text_syntax;
		if (sn.in_syntax != null)
			text_syntax = sn.in_syntax;
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
				arg = new DrawNodeTerm(node, sn, text_syntax);
		}
		if (arg != null) {
			if (obj instanceof ANode)
				arg.preFormat(cont, text_syntax.getSyntaxElem((ANode)obj), (ANode)obj);
			else if (obj == null && sn.empty != null)
				arg.preFormat(cont, sn.empty, node);
			else
				arg.preFormat(cont, sn, node);
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawNode extends DrawCtrl {

	public DrawNode(ANode node, Draw_SyntaxNode syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxNode sn = (Draw_SyntaxNode)syntax;
		ANode node = this.drnode;
		Draw_ATextSyntax text_syntax = this.text_syntax;
		if (sn.in_syntax != null)
			text_syntax = sn.in_syntax;
		if (arg == null) {
			if (node != null) {
				arg = cont.fmt.getDrawable(node, null, text_syntax);
				if (arg != null)
					arg.preFormat(cont);
			}
			else if (sn.empty != null) {
				arg = sn.empty.makeDrawable(cont.fmt, null, text_syntax);
				if (arg != null)
					arg.preFormat(cont, sn.empty, null);
			}
		}
		if (arg != null) {
			if (node != null)
				arg.preFormat(cont, text_syntax.getSyntaxElem(node), node);
			else if (sn.empty != null)
				arg.preFormat(cont, sn.empty, null);
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawOptional extends DrawCtrl {

	private	boolean drawed_as_true;
	
	public DrawOptional(ANode node, Draw_SyntaxOptional syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxOptional sc = (Draw_SyntaxOptional)syntax;
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

@ThisIsANode(copyable=false)
public class DrawEnumChoice extends DrawCtrl {

	private Object drawed_en;
	private ScalarAttrSlot attr;

	public DrawEnumChoice(ANode node, Draw_SyntaxEnumChoice syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		foreach (ScalarAttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxEnumChoice se = (Draw_SyntaxEnumChoice)syntax;
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

@ThisIsANode(copyable=false)
public final class DrawFolded extends DrawCtrl {

	public	boolean draw_folded;
	private	boolean drawed_as_folded;
	
	// for GUI
	public boolean getDrawFolded() { draw_folded }
	// for GUI
	public void setDrawFolded(boolean val) { draw_folded = val; }
	
	public DrawFolded(ANode node, Draw_SyntaxFolder syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.draw_folded = syntax.folded_by_default;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxFolder sc = (Draw_SyntaxFolder)syntax;
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

