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

// links to DrawTerm-s and holds space information
public class DrawTermLink {
	public final DrawTermLayoutInfo prev;
	public final DrawTermLayoutInfo next;
	public int size_0;
	public int size_1;

	public int sp_nl_size; // active newline or spacesize
	@packed:30,sp_nl_size,0  public int the_size;
	@packed: 1,sp_nl_size,30 public boolean do_newline;

	DrawTermLink(DrawTermLayoutInfo prev, DrawTermLayoutInfo next) {
		this.prev = prev;
		this.next = next;
		this.sp_nl_size = -1;
	}
	
}

public abstract class DrawTermLayoutInfo extends DrawLayoutInfo {
	public final       DrawTerm        dterm;
	public:r,r,rw,rw   DrawTermLink    lnk_prev;
	public:r,r,rw,rw   DrawTermLink    lnk_next;

	public		int     x;
	public		int     lineno; // line number for text-kind draw/print formatters

	DrawTermLayoutInfo(DrawTerm dterm) {
		this.dterm = dterm;
	}
	public DrawTermLayoutInfo getNext() {
		if (lnk_next == null)
			return null;
		return lnk_next.next;
	}
	public DrawTermLayoutInfo getPrev() {
		if (lnk_prev == null)
			return null;
		return lnk_prev.prev;
	}
	@getter public final boolean get$do_newline() {
		if (lnk_next != null)
			return lnk_next.do_newline;
		return false;
	}
	
	// DrawLayoutInfo
	public DrawTerm getDrawable() { dterm }
	public DrawLayoutInfo[] getBlocks() { return DrawLayoutBlock.emptyArray; }
	public Draw_Paragraph getParagraph() { dterm.syntax.par }
	public int getMaxLayout() { dterm.syntax.lout.count }
	public boolean isFlow() { false }
	public boolean isVertical() { false }

	public int getX() { return this.x; }
	public int getY() { return this.lineno-1; }
	public int getWidth() { return 0; }
	public int getHeight() { return 0; }
	public int getBaseline() { return 0; }
	public int getLineNo() { return this.lineno; }
	
}

public final class TxtDrawTermLayoutInfo extends DrawTermLayoutInfo {
	TxtDrawTermLayoutInfo(DrawTerm dterm) {
		super(dterm);
	}
	public TxtDrawTermLayoutInfo getNext() {
		if (lnk_next == null)
			return null;
		return (TxtDrawTermLayoutInfo)lnk_next.next;
	}
	public TxtDrawTermLayoutInfo getPrev() {
		if (lnk_prev == null)
			return null;
		return (TxtDrawTermLayoutInfo)lnk_prev.prev;
	}
}

public final class GfxDrawTermLayoutInfo extends DrawTermLayoutInfo {
	public		int     y;
	public		int		width;
	public		int		height;
	public		int		baseline;

	GfxDrawTermLayoutInfo(DrawTerm dterm) {
		super(dterm);
	}
	public GfxDrawTermLayoutInfo getNext() {
		if (lnk_next == null)
			return null;
		return (GfxDrawTermLayoutInfo)lnk_next.next;
	}
	public GfxDrawTermLayoutInfo getPrev() {
		if (lnk_prev == null)
			return null;
		return (GfxDrawTermLayoutInfo)lnk_prev.prev;
	}

	public final int getX() { return this.x; }
	public final int getY() { return this.y; }
	public final int getWidth() { return this.width; }
	public final int getHeight() { return this.height; }
	public final int getBaseline() { return this.baseline; }
	public final int getLineNo() { return this.lineno; }
	
}

@ThisIsANode(copyable=false)
public abstract class DrawTerm extends Drawable {
	private static Object _uninitialized_ = new Object();
	
	public static final Object NULL_NODE = new Object();
	public static final Object NULL_VALUE = new Object();

	public DrawTermLayoutInfo dt_fmt;
	
	public boolean hidden_as_auto_generated;
	
	private Object term_obj;

	public DrawTerm(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.term_obj = _uninitialized_;
	}
	
	public GfxDrawTermLayoutInfo getGfxFmtInfo() { return (GfxDrawTermLayoutInfo)dt_fmt; }
	
	public boolean isUnvisible() {
		return hidden_as_auto_generated;
	}  

	public Drawable getNextChild(Drawable dr) { assert ("DrawToken has no children"); return null; }
	public Drawable getPrevChild(Drawable dr) { assert ("DrawToken has no children"); return null; }
	public Drawable[] getChildren() { return Drawable.emptyArray; }

	private boolean isUpToDate(Object obj) {
		if (term_obj == _uninitialized_)
			return false;
		if (term_obj == null)
			return obj == null;
		return term_obj.equals(obj);
	}

	public final void preFormat(DrawContext cont) {
		ANode node = this.drnode;
		if (node instanceof ASTNode && ((ASTNode)node).isAutoGenerated()) {
			if (cont.fmt.getShowAutoGenerated())
				this.hidden_as_auto_generated = false;
			else
				this.hidden_as_auto_generated = true;
		}
		if (this instanceof DrawPlaceHolder) {
			if (cont.fmt.getShowPlaceholders())
				this.hidden_as_auto_generated = false;
			else
				this.hidden_as_auto_generated = true;
		}
		if (this.isUnvisible()) return;

		if (this.dt_fmt == null)
			this.dt_fmt = cont.makeDrawTermLayoutInfo(this);
		dt_fmt.x = 0;
		Object tmp = "???";
		try {
			tmp = makeTermObj(cont.fmt);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (!isUpToDate(tmp)) {
			this.term_obj = tmp;
			cont.formatTerm(this);
		}
	}

	private void link(DrawTerm next) {
		assert(next != null && next.dt_fmt.lnk_prev == null);
		assert(this != null && this.dt_fmt.lnk_next == null);
		DrawTermLink lnk = new DrawTermLink(this.dt_fmt,next.dt_fmt);
		this.dt_fmt.lnk_next = lnk;
		next.dt_fmt.lnk_prev = lnk;
	}
	private void unlink(DrawTermLink lnk) {
		assert(lnk.prev != null && lnk.prev.lnk_next == lnk);
		assert(lnk.next != null && lnk.next.lnk_prev == lnk);
		lnk.prev.lnk_next = null;
		lnk.next.lnk_prev = null;
	}
	public final void lnkFormat(DrawLinkContext cont) {
		if (this.isUnvisible())
			return;
		DrawTermLink plnk = this.dt_fmt.lnk_prev;
		{
			DrawTerm prev = getPrevLeaf();
			if (prev == null) {
				// first leaf
				if (plnk != null)
					unlink(plnk);
			} else {
				// check we are linked correctly
				assert(plnk != null && plnk.prev.dterm == prev && plnk.next.dterm == this);
				// fill spaces if it's a new link
				if (plnk.sp_nl_size < 0) {
					cont.processSpaceBefore(this);
					cont.flushSpace(this.dt_fmt.lnk_prev);
				}
			}
		}
		// ensure correct link to the next term
		plnk = this.dt_fmt.lnk_next;
		{
			DrawTerm next = getNextLeaf();
			if (next == null) {
				if (plnk != null)
					unlink(plnk);
			}
			else if (plnk != null) {
				assert (plnk.prev.dterm == this);
				if (plnk.next.dterm != next) {
					unlink(plnk);
					plnk = next.dt_fmt.lnk_prev;
					if (plnk != null)
						unlink(plnk);
					link(next);
				}
			} else {
				plnk = next.dt_fmt.lnk_prev;
				if (plnk != null)
					unlink(plnk);
				link(next);
			}
		}
		plnk = this.dt_fmt.lnk_next;
		// fill spaces if it's a new link
		if (plnk != null && plnk.sp_nl_size < 0) {
			cont.requestSpacesUpdate();
			cont.processSpaceAfter(this);
		}
	}
	public static void lnkVerify(DrawTerm dt) {
		assert(dt.getPrevLeaf() == null);
		assert(dt.dt_fmt.lnk_prev == null);
		assert(!dt.isUnvisible());
		while (dt.getNextLeaf()!=null) {
			assert(dt.dt_fmt.lnk_next != null);
			assert(dt.dt_fmt.lnk_next.next != null);
			assert(dt.dt_fmt.lnk_next.next.lnk_prev == dt.dt_fmt.lnk_next);
			assert(dt.dt_fmt.lnk_next.next.lnk_prev.prev == dt.dt_fmt);
			assert(dt.getNextLeaf() == dt.dt_fmt.lnk_next.next.dterm);
			assert(dt.dt_fmt.lnk_next.next.dterm.getPrevLeaf() == dt);
			dt = dt.getNextLeaf();
			assert(!dt.isUnvisible());
		}
		assert(dt.dt_fmt.lnk_next == null);
	}

	protected abstract Object makeTermObj(Formatter fmt);
	public final Object getTermObj() { return term_obj; }
}

@ThisIsANode(copyable=false)
public final class DrawToken extends DrawTerm {

	public DrawToken(ANode node, Draw_SyntaxToken syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) { return ((Draw_SyntaxToken)this.syntax).text; } 
}

@ThisIsANode(copyable=false)
public final class DrawPlaceHolder extends DrawTerm {

	public DrawPlaceHolder(ANode node, Draw_SyntaxPlaceHolder syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		Draw_SyntaxPlaceHolder stx = (Draw_SyntaxPlaceHolder)this.syntax;
		if (fmt instanceof GfxFormatter)
			return stx.text;
		return "";
	} 

}

@ThisIsANode(copyable=false)
public class DrawNodeTerm extends DrawTerm {

	protected String         attr;
	protected ScalarAttrSlot attr_slot;

	public DrawNodeTerm(ANode node, Draw_SyntaxAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.attr = syntax.name.intern();
		this.attr_slot = (ScalarAttrSlot)syntax.attr_slot;
		if (this.attr_slot == null) {
			foreach (ScalarAttrSlot a; node.values(); a.name == this.attr) {
				this.attr_slot = a;
				break;
			}
		}
	}

	protected Object makeTermObj(Formatter fmt) {
		ANode node = this.drnode;
		if (node instanceof ConstExpr && attr == "value")
			return String.valueOf(node);
		Object obj = getAttrObject();
		if (obj == null) {
			if (attr_slot != null && attr_slot.is_child)
				return NULL_NODE;
			else
				return NULL_VALUE;
		}
		return obj;
	}
	
	public final Object getAttrObject() {
		if (attr_slot != null)
			return attr_slot.get(drnode);
		return drnode.getVal(attr);
	}
	public final ScalarPtr getScalarPtr() {
		if (attr_slot != null)
			return new ScalarPtr(drnode, attr_slot);
		return drnode.getScalarPtr(attr);
	}
}

@ThisIsANode(copyable=false)
public class DrawIdent extends DrawNodeTerm {

	public DrawIdent(ANode node, Draw_SyntaxIdentAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		Object obj = super.makeTermObj(fmt);
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return obj;
		String text = String.valueOf(obj);
		if (text == null)
			return NULL_VALUE;
		Draw_SyntaxIdentAttr si = (Draw_SyntaxIdentAttr)this.syntax;
		if (text.indexOf('\u001f') >= 0) {
			String[] idents = text.split("\u001f");
			StringBuffer sb = new StringBuffer(text.length());
			foreach (String id; idents) {
				if (sb.length() > 0)
					sb.append('.');
				if (!fmt.getHintEscapes() || si.isOk(id)) {
					sb.append(id);
				} else {
					sb.append(si.getPrefix());
					sb.append(id);
					sb.append(si.getSuffix());
				}
			}
			return sb.toString();
		} else {
			if (!fmt.getHintEscapes() || si.isOk(text))
				return text;
			return si.getPrefix()+text+si.getSuffix();
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawCharTerm extends DrawNodeTerm {

	public DrawCharTerm(ANode node, Draw_SyntaxCharAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return obj;
		if (obj instanceof String)
			return obj;
		else if (obj instanceof Character) {
			Character ch = (Character)obj;
			if (!fmt.getHintEscapes())
				return ch;
			return Convert.escape(ch.charValue());
		}
		return "?";
	}
}

@ThisIsANode(copyable=false)
public class DrawStrTerm extends DrawNodeTerm {

	public DrawStrTerm(ANode node, Draw_SyntaxStrAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return obj;
		String str = String.valueOf(obj);
		if (!fmt.getHintEscapes())
			return str;
		return new String(Convert.string2source(str), 0);
	}
}

@ThisIsANode(copyable=false)
public class DrawXmlStrTerm extends DrawNodeTerm {

	public DrawXmlStrTerm(ANode node, Draw_SyntaxXmlStrAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	final String escapeString(String str) {
		StringBuffer sb = new StringBuffer(str);
		boolean changed = false;
		for(int i=0; i < sb.length(); i++) {
			char ch = sb.charAt(i);
			switch (sb.charAt(i)) {
			case '&':  sb.setCharAt(i, '&'); sb.insert(i+1,"amp;");  i += 4; changed = true; continue;
			case '<':  sb.setCharAt(i, '&'); sb.insert(i+1,"lt;");   i += 3; changed = true; continue;
			case '>':  sb.setCharAt(i, '&'); sb.insert(i+1,"gt;");   i += 3; changed = true; continue;
			case '\"': sb.setCharAt(i, '&'); sb.insert(i+1,"quot;"); i += 5; changed = true; continue;
			case '\'': sb.setCharAt(i, '&'); sb.insert(i+1,"apos;"); i += 5; changed = true; continue;
			}
			if (ch < ' ') {
				String s = "#"+Integer.toString((int)ch)+";";
				sb.setCharAt(i, '&');
				sb.insert(i+1,s);
				i += s.length();
				changed = true;
				continue;
			}
		}
		if (changed) return sb.toString();
		return str;
	}

	protected Object makeTermObj(Formatter fmt) {
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return "";
		String str = String.valueOf(obj);
		return escapeString(str);
	}
}

@ThisIsANode(copyable=false)
public class DrawXmlTypeTerm extends DrawXmlStrTerm {

	public DrawXmlTypeTerm(ANode node, Draw_SyntaxXmlTypeAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return "";
		String str = ((Type)obj).makeSignature();
		return escapeString(str);
	}
}
