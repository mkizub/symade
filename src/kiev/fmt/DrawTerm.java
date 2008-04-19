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
	public final DrawTerm prev;
	public final DrawTerm next;
	public int size_0;
	public int size_1;

	public int sp_nl_size; // active newline or spacesize
	@packed:30,sp_nl_size,0  public int the_size;
	@packed: 1,sp_nl_size,30 public boolean do_newline;

	DrawTermLink(DrawTerm prev, DrawTerm next) {
		this.prev = prev;
		this.next = next;
		this.sp_nl_size = -1;
	}
}

@ThisIsANode(copyable=false)
public abstract class DrawTerm extends Drawable {
	private static String _uninitialized_ = "uninitialized yet";

	public		int     x;
	public		int     y;
	public		int     lineno; // line number for text-kind draw/print formatters
	public		int		_metric;
	@packed:12,_metric,0
	public		int		w;
	@packed:8,_metric,12
	public		int		h;
	@packed:8,_metric,20
	public		int		b;
	@packed:1,_metric,31
	public		boolean	hidden_as_auto_generated;
	
	private				String			text;
	public:r,r,r,rw		DrawTermLink	lnk_prev;
	public:r,r,r,rw		DrawTermLink	lnk_next;

	public DrawTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
		text = _uninitialized_;
	}
	
	@getter public final boolean get$do_newline() {
		if (lnk_prev != null)
			return lnk_prev.do_newline;
		return false;
	}
	
	public boolean isUnvisible() {
		return hidden_as_auto_generated;
	}  

	public DrawTerm getFirstLeaf() { return isUnvisible() ? null : this; }
	public DrawTerm getLastLeaf()  { return isUnvisible() ? null : this; }

	public final int getMaxLayout() {
		return syntax.lout.count;
	}

	private boolean textIsUpToDate(String txt) {
		if (text == _uninitialized_)
			return false;
		if (text == null)
			return txt == null;
		return text.equals(txt);
	}

	public final void preFormat(DrawContext cont) {
		this.x = 0;
		this.y = 0;
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
		assert(next != null && next.lnk_prev == null);
		assert(this != null && this.lnk_next == null);
		DrawTermLink lnk = new DrawTermLink(this,next);
		this.lnk_next = lnk;
		next.lnk_prev = lnk;
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
		DrawTermLink plnk = this.lnk_prev;
		{
			DrawTerm prev = getPrevLeaf();
			if (prev == null) {
				// first leaf
				if (plnk != null)
					unlink(plnk);
			} else {
				// check we are linked correctly
				assert(plnk != null && plnk.prev == prev && plnk.next == this);
				// fill spaces if it's a new link
				if (plnk.sp_nl_size < 0) {
					cont.processSpaceBefore(this);
					cont.flushSpace(lnk_prev);
				}
			}
		}
		// ensure correct link to the next term
		plnk = this.lnk_next;
		{
			DrawTerm next = getNextLeaf();
			if (next == null) {
				if (plnk != null)
					unlink(plnk);
			}
			else if (plnk != null) {
				assert (plnk.prev == this);
				if (plnk.next != next) {
					unlink(plnk);
					plnk = next.lnk_prev;
					if (plnk != null)
						unlink(plnk);
					link(next);
				}
			} else {
				plnk = next.lnk_prev;
				if (plnk != null)
					unlink(plnk);
				link(next);
			}
		}
		plnk = this.lnk_next;
		// fill spaces if it's a new link
		if (plnk != null && plnk.sp_nl_size < 0) {
			cont.requestSpacesUpdate();
			cont.processSpaceAfter(this);
		}
	}
	public static void lnkVerify(DrawTerm dt) {
		assert(dt.getPrevLeaf() == null);
		assert(dt.lnk_prev == null);
		assert(!dt.isUnvisible());
		while (dt.getNextLeaf()!=null) {
			assert(dt.lnk_next != null);
			assert(dt.lnk_next.next != null);
			assert(dt.lnk_next.next.lnk_prev == dt.lnk_next);
			assert(dt.lnk_next.next.lnk_prev.prev == dt);
			assert(dt.getNextLeaf() == dt.lnk_next.next);
			assert(dt.lnk_next.next.getPrevLeaf() == dt);
			dt = dt.getNextLeaf();
			assert(!dt.isUnvisible());
		}
		assert(dt.lnk_next == null);
	}

	public final void postFormat(DrawLayoutBlock context) {
		if (this.isUnvisible())
			return;
		context.addLeaf(this);
	}

	public String getPrefix() { return ""; }	
	public String getSuffix() { return ""; }	
	abstract String makeText(Formatter fmt);
	public final String getText() { return text; }
}

@ThisIsANode(copyable=false)
public final class DrawToken extends DrawTerm {

	public DrawToken(ANode node, SyntaxToken syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	String makeText(Formatter fmt) { return ((SyntaxToken)this.syntax).text; } 
}

@ThisIsANode(copyable=false)
public final class DrawPlaceHolder extends DrawTerm {

	public DrawPlaceHolder(ANode node, SyntaxPlaceHolder syntax, ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	String makeText(Formatter fmt) {
		if (fmt instanceof GfxFormatter)
			return ((SyntaxPlaceHolder)this.syntax).text;
		return "";
	} 

}

@ThisIsANode(copyable=false)
public class DrawNodeTerm extends DrawTerm {

	String attr;

	public DrawNodeTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax);
		this.attr = attr.intern();
	}

	String makeText(Formatter fmt) {
		ANode node = this.drnode;
		if (node instanceof ConstExpr && attr == "value") {
			return String.valueOf(node);
		} else {
			Object o = getAttrPtr().get();
			if (o == null)
				return null;
			return String.valueOf(o);
		}
	}
	
	public final AttrPtr getAttrPtr() {
		return drnode.getAttrPtr(attr);
	}
}

@ThisIsANode(copyable=false)
public class DrawIdent extends DrawNodeTerm {

	private boolean escaped;

	public DrawIdent(ANode node, SyntaxIdentAttr syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax, attr);
	}

	String makeText(Formatter fmt) {
		String text = super.makeText(fmt);
		escaped = false;
		if (text == null)
			return null;
		//text = text.intern();
		SyntaxIdentAttr si = (SyntaxIdentAttr)this.syntax;
		if (text.indexOf('\u001f') >= 0) {
			String[] idents = text.split("\u001f");
			StringBuilder sb = new StringBuilder(text.length());
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
			escaped = true;
			return getPrefix()+text+getSuffix();
		}
	}
	
	public String getPrefix() { if (escaped) return ((SyntaxIdentAttr)this.syntax).getPrefix(); return ""; }
	public String getSuffix() { if (escaped) return ((SyntaxIdentAttr)this.syntax).getSuffix(); return ""; }
}

@ThisIsANode(copyable=false)
public class DrawCharTerm extends DrawNodeTerm {

	public DrawCharTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax, attr);
	}

	public String getPrefix() { return "'"; }	
	public String getSuffix() { return "'"; }	
	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
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

	public DrawStrTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax, attr);
	}

	public String getPrefix() { return "\""; }	
	public String getSuffix() { return "\""; }	
	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
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

	public DrawXmlStrTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax, attr);
	}

	final String escapeString(String str) {
		StringBuilder sb = new StringBuilder(str);
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

	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
		if (o == null)
			return "";
		String str = String.valueOf(o);
		return escapeString(str);
	}
}

@ThisIsANode(copyable=false)
public class DrawXmlTypeTerm extends DrawXmlStrTerm {

	public DrawXmlTypeTerm(ANode node, SyntaxElem syntax, ATextSyntax text_syntax, String attr) {
		super(node, syntax, text_syntax, attr);
	}

	String makeText(Formatter fmt) {
		Type t = (Type)getAttrPtr().get();
		if (t == null)
			return "";
		String str = t.makeSignature();
		return escapeString(str);
	}
}
