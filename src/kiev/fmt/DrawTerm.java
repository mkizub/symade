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
public abstract class DrawTerm extends Drawable {
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
	@packed:4,_metric,28
	public		int		do_newline; // used by formatter to mark actual new-lines after a DrawTerm
	
	private		String	text;

	public DrawTerm(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	
	public DrawTerm getFirstLeaf() { return isUnvisible() ? null : this; }
	public DrawTerm getLastLeaf()  { return isUnvisible() ? null : this; }

	public final void preFormat(DrawContext cont) {
		this.x = 0;
		this.y = 0;
		this.text = makeText(cont.fmt);
		cont.formatAsText(this);
	}

	public final boolean postFormat(DrawContext cont, boolean last_layout) {
		this.do_newline = 0;
		return cont.addLeaf(this);
	}

	public String getPrefix() { return ""; }	
	public String getSuffix() { return ""; }	
	abstract String makeText(Formatter fmt);
	public final String getText() { return text; }
}

@node(copyable=false)
public final class DrawToken extends DrawTerm {

	public DrawToken(ANode node, SyntaxToken syntax) {
		super(node, syntax);
	}

	String makeText(Formatter fmt) { return ((SyntaxToken)this.syntax).text; } 
}

@node(copyable=false)
public class DrawNodeTerm extends DrawTerm {

	String attr;

	public DrawNodeTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax);
		this.attr = attr.intern();
	}

	String makeText(Formatter fmt) {
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
		return node.getAttrPtr(attr);
	}
}

@node(copyable=false)
public class DrawCharTerm extends DrawNodeTerm {

	public DrawCharTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	public String getPrefix() { return "'"; }	
	public String getSuffix() { return "'"; }	
	String makeText(Formatter fmt) {
		Character ch = (Character)getAttrPtr().get();
		return "'"+Convert.escape(ch.charValue())+"'";
	}
}

@node(copyable=false)
public class DrawStrTerm extends DrawNodeTerm {

	public DrawStrTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	public String getPrefix() { return "\""; }	
	public String getSuffix() { return "\""; }	
	String makeText(Formatter fmt) {
		Object o = getAttrPtr().get();
		if (o == null)
			return null;
		String str = String.valueOf(o);
		return '\"'+new String(Convert.string2source(str), 0)+'\"';
	}
}

@node(copyable=false)
public class DrawXmlStrTerm extends DrawNodeTerm {

	public DrawXmlStrTerm(ANode node, SyntaxElem syntax, String attr) {
		super(node, syntax, attr);
	}

	private String escapeString(String str) {
		StringBuffer sb = new StringBuffer(str);
		boolean changed = false;
		for(int i=0; i < sb.length(); i++) {
			switch (sb.charAt(i)) {
			case '&':  sb.setCharAt(i, '&'); sb.insert(i+1,"amp;");  i += 4; changed = true; continue;
			case '<':  sb.setCharAt(i, '&'); sb.insert(i+1,"lt;");   i += 3; changed = true; continue;
			case '>':  sb.setCharAt(i, '&'); sb.insert(i+1,"gt;");   i += 3; changed = true; continue;
			case '\"': sb.setCharAt(i, '&'); sb.insert(i+1,"quot;"); i += 5; changed = true; continue;
			case '\'': sb.setCharAt(i, '&'); sb.insert(i+1,"apos;"); i += 5; changed = true; continue;
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

