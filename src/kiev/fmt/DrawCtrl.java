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

import kiev.fmt.common.*;

@ThisIsANode(copyable=false)
public abstract class DrawCtrl extends Drawable {
	@nodeAttr
	public Drawable arg;
	
	public DrawCtrl(INode node, Formatter fmt, Draw_SyntaxElem syntax) {
		super(node, fmt, syntax);
	}

	public Drawable[] getChildren() {
		if (arg == null)
			return Drawable.emptyArray;
		return new Drawable[]{arg};
	}
}

@ThisIsANode(copyable=false)
public class DrawSpace extends DrawCtrl {

	public DrawSpace(INode node, Formatter fmt, Draw_SyntaxElem syntax) {
		super(node, fmt, syntax);
	}

	public void preFormat(Formatter fmt) {
	}

}

@ThisIsANode(copyable=false)
public class DrawSubAttr extends DrawCtrl implements StyleProvider {

	protected ScalarAttrSlot attr_slot;

	public DrawSubAttr(INode node, Formatter fmt, Draw_SyntaxSubAttr syntax) {
		super(node, fmt, syntax);
		if (syntax.attr_slot != null) {
			this.attr_slot = (ScalarAttrSlot)syntax.attr_slot;
		} else {
			foreach (ScalarAttrSlot a; node.values(); a.name == syntax.name) {
				this.attr_slot = a;
				break;
			}
		}
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxSubAttr sn = (Draw_SyntaxSubAttr)syntax;
		INode node = this.drnode;
		fmt.pushSyntax(sn.in_syntax);
		try {
			if (attr_slot == null) {
				if!(arg instanceof DrawErrorTerm)
					arg = new DrawErrorTerm(node, fmt, sn, "<?error:"+sn.name+"?>");
				arg.preFormat(fmt, sn, node);
				return;
			}
			Object obj = attr_slot.get(node);
			if (obj != null && arg instanceof DrawEmptyNodeTerm)
				arg = null;
			if (arg == null) {
				if (obj instanceof INode)
					arg = fmt.getDrawable((INode)obj, null);
				else if (obj == null) {
					if (sn.empty != null)
						arg = sn.empty.makeDrawable(fmt, node);
					else if (attr_slot.isChild())
						arg = new DrawEmptyNodeTerm(node, fmt, sn, attr_slot);
				}
				if (arg == null) {
					Class clazz = attr_slot.typeinfo.clazz;
					if (clazz == Boolean.class || clazz == Boolean.TYPE)
						arg = new DrawEnumValueTerm(node, fmt, sn, attr_slot);
					else if (Enum.class.isAssignableFrom(clazz))
						arg = new DrawEnumValueTerm(node, fmt, sn, attr_slot);
					else if (clazz == Character.class || clazz == Character.TYPE)
						arg = new DrawCharValueTerm(node, fmt, sn, attr_slot);
					else if (clazz == String.class)
						arg = new DrawStrValueTerm(node, fmt, sn, attr_slot);
					else if (clazz == Float.class || clazz == Float.TYPE
							|| clazz == Double.class || clazz == Double.TYPE)
						arg = new DrawFloatValueTerm(node, fmt, sn, attr_slot);
					else if (clazz == Byte.class || clazz == Byte.TYPE
							|| clazz == Short.class || clazz == Short.TYPE
							|| clazz == Integer.class || clazz == Integer.TYPE
							|| clazz == Long.class || clazz == Long.TYPE
						)
						arg = new DrawIntValueTerm(node, fmt, sn, attr_slot);
					else
						arg = new DrawValueTerm(node, fmt, sn, attr_slot);
				}
			}
			if (arg != null) {
				if (obj instanceof INode)
					arg.preFormat(fmt, fmt.getSyntax().getSyntaxElem((INode)obj, fmt.getSyntaxList(), fmt.env), (INode)obj);
				else if (obj == null && sn.empty != null)
					arg.preFormat(fmt, sn.empty, node);
				else
					arg.preFormat(fmt, sn, node);
			}
		} finally {
			fmt.popSyntax(sn.in_syntax);
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawNode extends DrawCtrl implements StyleProvider {

	@nodeAttr public Drawable		prefix;
	@nodeAttr public Drawable		sufix;

	public DrawNode(INode node, Formatter fmt, Draw_SyntaxNode syntax) {
		super(node, fmt, syntax);
	}

	public Drawable[] getChildren() {
		Drawable p = (Drawable)parent();
		Drawable[] children = p.getChildren();
		if (children != null && children.length > 0) {
			Drawable prefix = this.prefix;
			Drawable sufix = this.sufix;
			Drawable f = children[0];
			Drawable l = children[children.length-1];
			if (this == f && this == l) {
				return new Drawable[]{arg};
			}
			else if (this == f) {
				if (sufix != null)
					return new Drawable[]{arg, sufix};
			}
			else if (this == l) {
				if (prefix != null)
					return new Drawable[]{prefix, arg};
			}
			else {
				if (prefix != null && sufix != null)
					return new Drawable[]{prefix, arg, sufix};
				else if (sufix != null)
					return new Drawable[]{arg, sufix};
				else if (prefix != null)
					return new Drawable[]{prefix, arg};
			}
		}
		return new Drawable[]{arg};
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxNode sn = (Draw_SyntaxNode)syntax;
		INode node = this.drnode;
		fmt.pushSyntax(sn.in_syntax);
		try {
			if (prefix == null && sn.prefix != null)
				prefix = sn.prefix.makeDrawable(fmt, node);
			if (sufix == null && sn.sufix != null)
				sufix = sn.sufix.makeDrawable(fmt, node);
	
			if (arg == null) {
				if (node != null) {
					arg = fmt.getDrawable(node, null);
					if (arg != null)
						arg.preFormat(fmt);
				}
				else if (sn.empty != null) {
					arg = sn.empty.makeDrawable(fmt, null);
					if (arg != null)
						arg.preFormat(fmt, sn.empty, null);
				}
			}
			if (prefix != null)
				prefix.preFormat(fmt,sn.prefix,node);
			if (arg != null) {
				if (node != null)
					arg.preFormat(fmt, fmt.getSyntax().getSyntaxElem(node, fmt.getSyntaxList(), fmt.env), node);
				else if (sn.empty != null)
					arg.preFormat(fmt, sn.empty, null);
			}
			if (sufix != null)
				sufix.preFormat(fmt,sn.sufix,node);
		} finally {
			fmt.popSyntax(sn.in_syntax);
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawOptional extends DrawCtrl {

	private	boolean drawed_as_true;
	
	public DrawOptional(INode node, Formatter fmt, Draw_SyntaxOptional syntax) {
		super(node, fmt, syntax);
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxOptional sc = (Draw_SyntaxOptional)syntax;
		INode node = this.drnode;
		if (sc.calculator == null || sc.calculator.calc(node)) {
			if (!drawed_as_true || arg == null) {
				drawed_as_true = true;
				if (sc.opt_true != null) {
					arg = sc.opt_true.makeDrawable(fmt, node);
				} else {
					arg = null;
				}
			}
		} else {
			if (drawed_as_true || arg == null) {
				drawed_as_true = false;
				if (sc.opt_false != null) {
					arg = sc.opt_false.makeDrawable(fmt, node);
				} else {
					arg = null;
				}
			}
		}
		if (arg != null) {
			if (drawed_as_true)
				arg.preFormat(fmt,sc.opt_true,node);
			else
				arg.preFormat(fmt,sc.opt_false,node);
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawEnumChoice extends DrawCtrl implements StyleProvider {

	private Object drawed_en;
	private ScalarAttrSlot attr;

	public DrawEnumChoice(INode node, Formatter fmt, Draw_SyntaxEnumChoice syntax) {
		super(node, fmt, syntax);
		foreach (ScalarAttrSlot a; node.values(); a.name == syntax.name) {
			attr = a;
			break;
		}
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxEnumChoice se = (Draw_SyntaxEnumChoice)syntax;
		INode node = this.drnode;
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
				arg = se.elements[ord].makeDrawable(fmt, node);
			drawed_en = en;
		}
		if (arg != null)
			arg.preFormat(fmt,se.elements[ord],node);
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
	
	public DrawFolded(INode node, Formatter fmt, Draw_SyntaxFolder syntax) {
		super(node, fmt, syntax);
		this.draw_folded = syntax.folded_by_default;
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxFolder sc = (Draw_SyntaxFolder)syntax;
		INode node = this.drnode;
		if (draw_folded) {
			if (!drawed_as_folded || arg == null) {
				drawed_as_folded = true;
				arg = sc.folded.makeDrawable(fmt, node);
			}
		} else {
			if (drawed_as_folded || arg == null) {
				drawed_as_folded = false;
				arg = sc.unfolded.makeDrawable(fmt, node);
			}
		}
		if (drawed_as_folded)
			arg.preFormat(fmt,sc.folded,node);
		else
			arg.preFormat(fmt,sc.unfolded,node);
	}
}

