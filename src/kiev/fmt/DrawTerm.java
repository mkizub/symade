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
	public final DrawTermFormatInfo prev;
	public final DrawTermFormatInfo next;
	public int size_0;
	public int size_1;

	public int sp_nl_size; // active newline or spacesize
	@packed:30,sp_nl_size,0  public int the_size;
	@packed: 1,sp_nl_size,30 public boolean do_newline;

	DrawTermLink(DrawTermFormatInfo prev, DrawTermFormatInfo next) {
		this.prev = prev;
		this.next = next;
		this.sp_nl_size = -1;
	}
	
}

public abstract class DrawTermFormatInfo implements DrawLayoutInfo {
	public final       DrawTerm        dterm;
	public:r,r,rw,rw   DrawTermLink    lnk_prev;
	public:r,r,rw,rw   DrawTermLink    lnk_next;

	public		int     x;
	public		int     lineno; // line number for text-kind draw/print formatters

	DrawTermFormatInfo(DrawTerm dterm) {
		this.dterm = dterm;
	}
	public DrawTermFormatInfo getNext() {
		if (lnk_next == null)
			return null;
		return lnk_next.next;
	}
	public DrawTermFormatInfo getPrev() {
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
	public Drawable getDrawable() { dterm }
	public DrawLayoutInfo[] getBlocks() { return DrawLayoutBlock.emptyArray; }
	public Draw_Paragraph getParagraph() { dterm.syntax.par }
	public int getMaxLayout() { dterm.syntax.lout.count }
	public boolean isFlow() { false }
}

public final class TxtDrawTermFormatInfo extends DrawTermFormatInfo {
	TxtDrawTermFormatInfo(DrawTerm dterm) {
		super(dterm);
	}
	public TxtDrawTermFormatInfo getNext() {
		if (lnk_next == null)
			return null;
		return (TxtDrawTermFormatInfo)lnk_next.next;
	}
	public TxtDrawTermFormatInfo getPrev() {
		if (lnk_prev == null)
			return null;
		return (TxtDrawTermFormatInfo)lnk_prev.prev;
	}
}

public final class GfxDrawTermFormatInfo extends DrawTermFormatInfo {
	public		int     y;
	public		int		width;
	public		int		height;
	public		int		baseline;

	GfxDrawTermFormatInfo(DrawTerm dterm) {
		super(dterm);
	}
	public GfxDrawTermFormatInfo getNext() {
		if (lnk_next == null)
			return null;
		return (GfxDrawTermFormatInfo)lnk_next.next;
	}
	public GfxDrawTermFormatInfo getPrev() {
		if (lnk_prev == null)
			return null;
		return (GfxDrawTermFormatInfo)lnk_prev.prev;
	}
}

@ThisIsANode(copyable=false)
public abstract class DrawTerm extends Drawable {
	private static String _uninitialized_ = "uninitialized yet";

	public DrawTermFormatInfo dt_fmt;
	
	public boolean hidden_as_auto_generated;
	
	private String text;

	public DrawTerm(ANode node, Draw_SyntaxElem syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.text = _uninitialized_;
	}
	
	public GfxDrawTermFormatInfo getGxfFmtInfo() { return (GfxDrawTermFormatInfo)dt_fmt; }
	
	@getter public final boolean get$do_newline() {
		if (dt_fmt.lnk_next != null)
			return dt_fmt.lnk_next.do_newline;
		return false;
	}
	
	public boolean isUnvisible() {
		return hidden_as_auto_generated;
	}  

	public Drawable getNextChild(Drawable dr) { assert ("DrawToken has no children"); return null; }
	public Drawable getPrevChild(Drawable dr) { assert ("DrawToken has no children"); return null; }
	public Drawable[] getChildren() { return Drawable.emptyArray; }

	public final int getX() { return ((GfxDrawTermFormatInfo)this.dt_fmt).x; }
	public final int getY() { return ((GfxDrawTermFormatInfo)this.dt_fmt).y; }
	public final int getWidth() { return ((GfxDrawTermFormatInfo)this.dt_fmt).width; }
	public final int getHeight() { return ((GfxDrawTermFormatInfo)this.dt_fmt).height; }
	public final int getBaseline() { return ((GfxDrawTermFormatInfo)this.dt_fmt).baseline; }
	public final int getLineNo() { return ((GfxDrawTermFormatInfo)this.dt_fmt).lineno; }
	
	private boolean textIsUpToDate(String txt) {
		if (text == _uninitialized_)
			return false;
		if (text == null)
			return txt == null;
		return text.equals(txt);
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
			this.dt_fmt = cont.makeDrawTermFormatInfo(this);
		dt_fmt.x = 0;
		String tmp = "???";
		try {
			tmp = makeText(cont.fmt);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		if (!textIsUpToDate(tmp)) {
			this.text = tmp;
			cont.formatAsText(this);
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

	public String getPrefix() { return ""; }	
	public String getSuffix() { return ""; }	
	abstract String makeText(Formatter fmt);
	public final String getText() { return text; }
}

@ThisIsANode(copyable=false)
public final class DrawToken extends DrawTerm {

	public DrawToken(ANode node, Draw_SyntaxToken syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	String makeText(Formatter fmt) { return ((Draw_SyntaxToken)this.syntax).text; } 
}

@ThisIsANode(copyable=false)
public final class DrawPlaceHolder extends DrawTerm {

	public DrawPlaceHolder(ANode node, Draw_SyntaxPlaceHolder syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	String makeText(Formatter fmt) {
		if (fmt instanceof GfxFormatter)
			return ((Draw_SyntaxPlaceHolder)this.syntax).text;
		return "";
	} 

}

@ThisIsANode(copyable=false)
public class DrawNodeTerm extends DrawTerm {

	String attr;
	ScalarAttrSlot attr_slot;

	public DrawNodeTerm(ANode node, Draw_SyntaxAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		this.attr = syntax.name.intern();
		this.attr_slot = (ScalarAttrSlot)syntax.attr_slot;
	}

	String makeText(Formatter fmt) {
		ANode node = this.drnode;
		if (node instanceof ConstExpr && attr == "value") {
			return String.valueOf(node);
		} else {
			Object o = getAttrObject();
			if (o == null)
				return null;
			return String.valueOf(o);
		}
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

	private String prefix = "";
	private String suffix = "";

	public DrawIdent(ANode node, Draw_SyntaxIdentAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		//prefix = syntax.getPrefix();
		//suffix = syntax.getSuffix();
	}

	String makeText(Formatter fmt) {
		String text = super.makeText(fmt);
		// set unescaped
		prefix = "";
		suffix = "";
		if (text == null)
			return null;
		//text = text.intern();
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
			prefix = si.getPrefix();
			suffix = si.getSuffix();
			return getPrefix()+text+getSuffix();
		}
	}
	
	public String getPrefix() { prefix }
	public String getSuffix() { suffix }
}

@ThisIsANode(copyable=false)
public class DrawCharTerm extends DrawNodeTerm {

	public DrawCharTerm(ANode node, Draw_SyntaxCharAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public String getPrefix() { return "'"; }	
	public String getSuffix() { return "'"; }	
	String makeText(Formatter fmt) {
		Object o = getAttrObject();
		if (o instanceof String) {
			return "'"+o+"'";
		}
		else if (o instanceof Character) {
			Character ch = (Character)o;
			if (!fmt.getHintEscapes())
				return "'"+ch+"'";
			return "'"+Convert.escape(ch.charValue())+"'";
		}
		return "'?'";
	}
}

@ThisIsANode(copyable=false)
public class DrawStrTerm extends DrawNodeTerm {

	public DrawStrTerm(ANode node, Draw_SyntaxStrAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public String getPrefix() { return "\""; }	
	public String getSuffix() { return "\""; }	
	String makeText(Formatter fmt) {
		Object o = getAttrObject();
		if (o == null)
			return null;
		String str = String.valueOf(o);
		if (!fmt.getHintEscapes())
			return '\"'+str+'\"';
		return '\"'+new String(Convert.string2source(str), 0)+'\"';
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

	public String getPrefix() { ((Draw_SyntaxXmlStrAttr)this.syntax).getPrefix() }
	public String getSuffix() { ((Draw_SyntaxXmlStrAttr)this.syntax).getSuffix() }	
	String makeText(Formatter fmt) {
		Object o = getAttrObject();
		if (o == null)
			return "";
		String str = String.valueOf(o);
		return escapeString(str);
	}
}

@ThisIsANode(copyable=false)
public class DrawXmlTypeTerm extends DrawXmlStrTerm {

	public DrawXmlTypeTerm(ANode node, Draw_SyntaxXmlTypeAttr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	String makeText(Formatter fmt) {
		Type t = (Type)getAttrObject();
		if (t == null)
			return "";
		String str = t.makeSignature();
		return escapeString(str);
	}
}
