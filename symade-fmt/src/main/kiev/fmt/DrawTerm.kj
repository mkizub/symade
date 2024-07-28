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

import java.math.*;

import kiev.vtree.AutoCompleteOption;
import kiev.vtree.AutoCompleteResult;

@ThisIsANode(copyable=false)
public abstract class DrawTerm extends Drawable {
	public static final Object NULL_NODE = new Object();
	public static final Object NULL_VALUE = new Object();

	@UnVersioned
	public boolean is_hidden;

	public DrawTerm(INode node, Formatter fmt, Draw_SyntaxElem syntax) {
		super(node, fmt, syntax);
	}
	
	public DrawLayoutInfo getGfxFmtInfo() { return dr_dli; }
	
	public boolean isUnvisible() {
		return is_hidden;
	}  
	
	public Drawable[] getChildren() { return Drawable.emptyArray; }
	public abstract boolean isTextual();
	public abstract Object getTermObj();

	public void preFormat(Formatter fmt) {
	}
	public void formatTerm(Formatter fmt) {
	}
}

@ThisIsANode(copyable=false)
public final class DrawToken extends DrawTerm {

	public DrawToken(INode node, Formatter fmt, Draw_SyntaxToken syntax) {
		super(node, fmt, syntax);
	}

	public boolean isTextual() { true }
	public Object getTermObj() { ((Draw_SyntaxToken)this.syntax).text }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, ((Draw_SyntaxToken)this.syntax).text);
	}
}

@ThisIsANode(copyable=false)
public final class DrawIcon extends DrawTerm {

	public DrawIcon(INode node, Formatter fmt, Draw_SyntaxIcon syntax) {
		super(node, fmt, syntax);
	}

	public boolean isTextual() { false }
	public Object getTermObj() { ((Draw_SyntaxIcon)this.syntax).icon }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, ((Draw_SyntaxIcon)this.syntax).icon);
	}
}

@ThisIsANode(copyable=false)
public final class DrawPlaceHolder extends DrawTerm {

	public DrawPlaceHolder(INode node, Formatter fmt, Draw_SyntaxPlaceHolder syntax) {
		super(node, fmt, syntax);
	}

	public boolean isTextual() { false }
	public Object getTermObj() { ((Draw_SyntaxPlaceHolder)this.syntax).text }

	public void preFormat(Formatter fmt) {
		if (fmt instanceof TextFormatter)
			is_hidden = true;
	}
	public void formatTerm(Formatter fmt) {
		if (fmt instanceof TextFormatter)
			is_hidden = true;
		fmt.formatTerm(this, ((Draw_SyntaxPlaceHolder)this.syntax).text);
	}

	public final ScalarPtr getScalarPtr() {
		try {
			String attr_name = null;
			Draw_SyntaxElemDecl elem_decl = this.syntax.elem_decl;
			for (Drawable p = (Drawable)parent(); p != null; p = (Drawable)parent()) {
				if (p.syntax.elem_decl != elem_decl || p.drnode != this.drnode)
					break;
				if (p.syntax instanceof Draw_SyntaxAttr)
					return Env.getScalarPtr(this.drnode, ((Draw_SyntaxAttr)p.syntax).name);
			}
		} catch (Exception e) {}
		return null;
	}

}

@ThisIsANode(copyable=false)
public class DrawErrorTerm extends DrawTerm {

	private final String error;
	
	public DrawErrorTerm(INode node, Formatter fmt, Draw_SyntaxElem syntax, String error) {
		super(node, fmt, syntax);
		this.error = error;
	}

	public boolean isTextual() { false }
	public Object getTermObj() { error }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, error);
	}
}

@ThisIsANode(copyable=false)
public class DrawEmptyNodeTerm extends DrawTerm {

	protected ScalarAttrSlot attr_slot;

	public DrawEmptyNodeTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax);
		this.attr_slot = attr_slot;
	}

	public boolean isTextual() { false }
	public Object getTermObj() { NULL_NODE }

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, NULL_NODE);
	}
}

@ThisIsANode(copyable=false)
public class DrawValueTerm extends DrawTerm {

	protected Object termObject;
	protected Object tmpTermObj;
	final
	public ScalarAttrSlot attr_slot;

	public DrawValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax);
		this.attr_slot = attr_slot;
	}

	public boolean isTextual() { true }

	public final Object getTermObj() {
		if (tmpTermObj != null)
			return tmpTermObj;
		if (termObject == null)
			return NULL_VALUE;
		return termObject;
	}
	protected Object makeTermObj() {
		if (tmpTermObj != null)
			return tmpTermObj;
		INode node = this.drnode;
		termObject = getAttrObject();
		if (termObject == null)
			termObject = NULL_VALUE;
		return termObject;
	}

	public final Object getAttrObject() {
		return attr_slot.get(drnode);
	}
	public final ScalarPtr getScalarPtr() {
		return new ScalarPtr(drnode, attr_slot);
	}

	public void formatTerm(Formatter fmt) {
		fmt.formatTerm(this, makeTermObj());
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof Boolean || value instanceof Enum) {
				attr_slot.set(drnode, value);
				ok = true;
			}
			else if (value instanceof String) {
				String text = (String)value;
				if (attr_slot.typeinfo.clazz == String.class) {
					attr_slot.set(drnode, text);
					ok = true;
				}
				else if (attr_slot.typeinfo.clazz == Boolean.class || attr_slot.typeinfo.clazz == Boolean.TYPE) {
					if (text.equalsIgnoreCase("true")) {
						attr_slot.set(drnode, Boolean.TRUE);
						ok = true;
					}
					if (text.equalsIgnoreCase("false")) {
						attr_slot.set(drnode, Boolean.FALSE);
						ok = true;
					}
				}
				else if (Enum.class.isAssignableFrom(attr_slot.typeinfo.clazz)) {
					Object v = attr_slot.typeinfo.clazz.getMethod("valueOf",String.class).invoke(null,text);
					attr_slot.set(drnode, v);
					ok = true;
				}
			}
			else if (value instanceof AutoCompleteOption) {
				AutoCompleteOption v = (AutoCompleteOption)value;
				attr_slot.set(drnode, v.data);
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawEnumValueTerm extends DrawValueTerm {
	public DrawEnumValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof Boolean || value instanceof Enum) {
				attr_slot.set(drnode, value);
				ok = true;
			}
			if (value instanceof String) {
				String text = (String)value;
				if (attr_slot.typeinfo.clazz == Boolean.class || attr_slot.typeinfo.clazz == Boolean.TYPE) {
					if (text.equalsIgnoreCase("true")) {
						attr_slot.set(drnode, Boolean.TRUE);
						ok = true;
					}
					if (text.equalsIgnoreCase("false")) {
						attr_slot.set(drnode, Boolean.FALSE);
						ok = true;
					}
				}
				if (Enum.class.isAssignableFrom(attr_slot.typeinfo.clazz)) {
					Object v = attr_slot.typeinfo.clazz.getMethod("valueOf",String.class).invoke(null,text);
					attr_slot.set(drnode, v);
					ok = true;
				}
			}
			if (value instanceof AutoCompleteOption) {
				AutoCompleteOption v = (AutoCompleteOption)value;
				attr_slot.set(drnode, v.data);
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawCharValueTerm extends DrawValueTerm {
	public DrawCharValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof Character) {
				attr_slot.set(drnode, value);
				ok = true;
			}
			if (value instanceof String) {
				String text = (String)value;
				if (text.length() > 1)
					text = ConstExpr.source2ascii(text);
				if (text.length() == 1) {
					attr_slot.set(drnode, Character.valueOf(text.charAt(0)));
					ok = true;
				}
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawStrValueTerm extends DrawValueTerm {
	public DrawStrValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof String) {
				String text = (String)value;
				attr_slot.set(drnode, text);
				ok = true;
			}
			if (value instanceof AutoCompleteOption) {
				AutoCompleteOption v = (AutoCompleteOption)value;
				attr_slot.set(drnode, v.text);
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawFloatValueTerm extends DrawValueTerm {
	public DrawFloatValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof Number) {
				attr_slot.set(drnode, value);
				ok = true;
			}
			if (value instanceof String) {
				String text = (String)value;
				if (attr_slot.typeinfo.clazz == Float.class || attr_slot.typeinfo.clazz == Float.TYPE) {
					attr_slot.set(drnode, Float.valueOf(text));
					ok = true;
				}
				if (attr_slot.typeinfo.clazz == Double.class || attr_slot.typeinfo.clazz == Double.TYPE) {
					attr_slot.set(drnode, Double.valueOf(text));
					ok = true;
				}
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawIntValueTerm extends DrawValueTerm {
	public DrawIntValueTerm(INode node, Formatter fmt, Draw_SyntaxAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof Number) {
				attr_slot.set(drnode, value);
				ok = true;
			}
			if (value instanceof String) {
				String text = (String)value;
				int radix = 10;
				boolean neg = false;
				if( text.startsWith("-") ) { text = text.substring(1); neg = true; }
				if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
				else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
				long l = ConstExpr.parseLong(text,radix);
				if (neg)
					l = -l;
				INode node = drnode;
				Class clazz = attr_slot.typeinfo.clazz;
				if (clazz == Byte.class || clazz == Byte.TYPE)
					node.setVal(attr_slot, Byte.valueOf((byte)l));
				else if (clazz == Short.class || clazz == Short.TYPE)
					node.setVal(attr_slot, Short.valueOf((short)l));
				else if (clazz == Integer.class || clazz == Integer.TYPE)
					node.setVal(attr_slot, Integer.valueOf((int)l));
				else if (clazz == Long.class || clazz == Long.TYPE)
					node.setVal(attr_slot, Long.valueOf(l));
				else
					node.setVal(attr_slot, BigInteger.valueOf(l));
				if (node instanceof ConstRadixExpr) {
					if (radix == 8)
						node.radix = IntRadix.RADIX_OCT;
					else if (radix == 16)
						node.radix = IntRadix.RADIX_HEX;
					else
						node.radix = IntRadix.RADIX_DEC;
				}
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawTokenTerm extends DrawValueTerm {
	public DrawTokenTerm(INode node, Formatter fmt, Draw_SyntaxTokenAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}
	
	private UIDrawPath makePathToNext() {
		INode node = this.drnode;
		INode parent = node.parent();
		Object val = parent.getVal(drnode.pslot());
		if (val instanceof INode[]) {
			INode[] arr = (INode[])val;
			for (int i=0; i < arr.length; i++) {
				if (arr[i] == node) {
					if (i+1 < arr.length)
						return makePathToNode(arr[i+1], 0);
					return null;
				}
			}
		}
		return null;
	}

	private UIDrawPath makePathToPrev() {
		INode node = this.drnode;
		INode parent = node.parent();
		Object val = parent.getVal(drnode.pslot());
		if (val instanceof INode[]) {
			INode[] arr = (INode[])val;
			for (int i=0; i < arr.length; i++) {
				if (arr[i] == node) {
					if (i > 0)
						return makePathToNode(arr[i-1], Integer.MAX_VALUE);
					return null;
				}
			}
		}
		return null;
	}

	private UIDrawPath makePathToNode(INode n, int cursor) {
		Vector<INode> path = new Vector<INode>();
		path.append(n);
		while (n.parent() != null) {
			n = n.parent();
			path.append(n);
		}
		return new UIDrawPath(path.toArray(), cursor);
	}

	private INode addEmptyToken(int incr) {
		INode tok = new Copier().copyFull(drnode);
		attr_slot.set(tok, "");
		INode parent = drnode.parent();
		SpaceAttrSlot drslot = (SpaceAttrSlot)drnode.pslot();
		int idx = drslot.indexOf(parent,drnode);
		parent.insVal(drslot,idx+incr,tok);
		return tok;
	}

	public UIDrawPath insChar(int pos, char ch, boolean override) {
		String text;
		Object obj = getTermObj();
		if (obj == null || obj == NULL_VALUE)
			text = "";
		else
			text = (String)obj;
		if (pos >= text.length() && Character.isWhitespace(ch)) {
			if (text.length() == 0)
				return makePathToNode(this, pos);
			INode tok = addEmptyToken(1);
			return makePathToNode(tok,0);
		}
		if (pos <= 0 && Character.isWhitespace(ch)) {
			if (text.length() == 0)
				return makePathToNode(this, pos);
			INode tok = addEmptyToken(0);
			return makePathToNode(tok,0);
		}
		if (pos >= text.length()) {
			text = text+ch;
			attr_slot.set(drnode, text);
		}
		else if (override) {
			text = text.substring(0,pos)+ch+text.substring(pos+1);
			attr_slot.set(drnode, text);
		}
		else {
			text = text.substring(0,pos)+ch+text.substring(pos);
			attr_slot.set(drnode, text);
		}
		return makePathToNode(this, pos+1);
	}
	
	public UIDrawPath delChar(int pos, boolean backspace) {
		String text;
		Object obj = getTermObj();
		if (obj == null || obj == NULL_VALUE)
			text = "";
		else
			text = (String)obj;
		int tidx = 0;
		if (text.length() == 0) {
			UIDrawPath path = null;
			if (!backspace)
				path = makePathToNext();
			if (path == null)
				path = makePathToPrev();
			if (path == null)
				path = makePathToNode(this, pos);
			drnode.detach();
			return path;
		}
		UIDrawPath path = null;
		if (backspace) {
			if (pos <= 0) {
				path = makePathToPrev();
				if (path == null)
					path = makePathToNode(this, pos);
			} else {
				text = text.substring(0, pos-1)+text.substring(pos);
				attr_slot.set(drnode, text);
				path = makePathToNode(this, pos-1);
			}
		} else {
			if (pos >= text.length()) {
				path = makePathToNext();
				if (path == null)
					path = makePathToNode(this, pos);
			} else {
				text = text.substring(0, pos)+text.substring(pos+1);
				attr_slot.set(drnode, text);
				path = makePathToNode(this, pos);
			}
		}
		return path;
	}
	
	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null)
				value = "";
			if (value instanceof String) {
				String text = (String)value;
				attr_slot.set(drnode, text);
				ok = true;
			} else {
				String text = String.valueOf(value);
				attr_slot.set(drnode, text);
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawIdent extends DrawValueTerm {
	
	public DrawIdent(INode node, Formatter fmt, Draw_SyntaxIdentAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	protected Object makeTermObj() {
		if (tmpTermObj != null)
			return tmpTermObj;
		INode node = this.drnode;
		Object obj = getAttrObject();
		if (attr_slot.isSymRef()) {
			if (node instanceof SymbolRef) {
				if (obj instanceof Symbol)
					obj = node.qualified ? obj.qname() : obj.sname;
				else if (obj instanceof NameAndUUID)
					obj = obj.name;
			}
		}
		if (obj == null)
			return termObject=NULL_VALUE;
		if (obj == NULL_NODE || obj == NULL_VALUE)
			return termObject=obj;
		String text = String.valueOf(obj);
		if (text == null)
			return NULL_VALUE;
		Draw_SyntaxIdentAttr si = (Draw_SyntaxIdentAttr)this.syntax;
		return termObject=text;
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof String) {
				Object obj = getAttrObject();
				if (obj instanceof SymbolRef) {
					obj.name = (String)value;
					ok = true;
				}
				else if (obj instanceof Symbol && obj.parent() == drnode) {
					obj.sname = (String)value;
					ok = true;
				}
				else if (attr_slot.typeinfo.clazz == String.class) {
					attr_slot.set(drnode, value);
					ok = true;
				}
				else if (drnode instanceof SymbolRef && attr_slot.name == "ident_or_symbol_or_type") {
					attr_slot.set(drnode, value);
					ok = true;
				}
				else if (drnode instanceof Symbol && attr_slot.name == "sname") {
					attr_slot.set(drnode, value);
					ok = true;
				}
			}
			if (value instanceof AutoCompleteOption) {
				AutoCompleteOption v = (AutoCompleteOption)value;
				Object obj = getAttrObject();
				if (obj instanceof SymbolRef) {
					obj.ident_or_symbol_or_type = v.data;
					ok = true;
				}
				else if (obj instanceof Symbol && obj.parent() == drnode) {
					obj.sname = v.text;
					ok = true;
				}
				else if (attr_slot.typeinfo.clazz == String.class) {
					attr_slot.set(drnode, v.text);
					ok = true;
				}
				else if (drnode instanceof SymbolRef && attr_slot.name == "ident_or_symbol_or_type") {
					attr_slot.set(drnode, v.data);
					ok = true;
				}
				else if (drnode instanceof Symbol && attr_slot.name == "sname") {
					attr_slot.set(drnode, v.text);
					ok = true;
				}
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawCharTerm extends DrawValueTerm {

	public DrawCharTerm(INode node, Formatter fmt, Draw_SyntaxCharAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	protected Object makeTermObj() {
		if (tmpTermObj != null)
			return tmpTermObj;
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return termObject=obj;
		if (obj instanceof String)
			return termObject=obj;
		else if (obj instanceof Character) {
			Character ch = (Character)obj;
			return termObject=ch;
		}
		return termObject="?";
	}
}

@ThisIsANode(copyable=false)
public class DrawStrTerm extends DrawValueTerm {

	public DrawStrTerm(INode node, Formatter fmt, Draw_SyntaxStrAttr syntax, ScalarAttrSlot attr_slot) {
		super(node, fmt, syntax, attr_slot);
	}

	protected Object makeTermObj() {
		if (tmpTermObj != null)
			return tmpTermObj;
		Object obj = getAttrObject();
		if (obj == null || obj == NULL_NODE || obj == NULL_VALUE)
			return termObject=obj;
		String str = String.valueOf(obj);
		return termObject=str;
	}

	public void setValue(Object value) {
		boolean ok = false;
		try {
			if (value == null || value instanceof String) {
				String text = (String)value;
				attr_slot.set(drnode, text);
				ok = true;
			}
			if (value instanceof AutoCompleteOption) {
				AutoCompleteOption v = (AutoCompleteOption)value;
				attr_slot.set(drnode, v.text);
				ok = true;
			}
		} finally {
			tmpTermObj = ok ? null : value;
		}
	}
}

