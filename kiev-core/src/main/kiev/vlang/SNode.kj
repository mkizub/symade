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
package kiev.vlang;
import syntax kiev.Syntax;

/**
 * A node that is a syntax modifier: import, operator decl, separators, comments, etc.
 */
@ThisIsANode(lang=CoreLang)
public abstract class SNode extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public SNode() {}

}

// base of text elements
@ThisIsANode(lang=CoreLang)
public abstract class ATextNode extends SNode {
	
	public abstract String toText();
}

// a simple text element, small piece of text, formatted in the same way
@ThisIsANode(name="TextElem", lang=CoreLang)
public class TextElem extends ATextNode {
	@nodeAttr public String text;
	
	public TextElem() {}
	public TextElem(String text) {
		this.text = text;
	}
	
	public String toText() { return text; }
}

// attributed text
@ThisIsANode(name="TextBrk", lang=CoreLang)
public final class TextBrk extends ATextNode {
	public String toText() {
		return "\n";
	}
}

// a line of text or paragraph, list of simple text elements
@ThisIsANode(name="Line", lang=CoreLang)
public class TextLine extends ATextNode {
	@nodeAttr public ATextNode∅  elems;
	
	public TextLine() {}
	public TextLine(String text) {
		elems += new TextElem(text);
	}
	
	public String toText() {
		StringBuffer sb = new StringBuffer();
		foreach (ATextNode t; elems)
			sb.append(t.toText());
		return sb.toString();
	}
}

// text, may have multiple lines/paragraphs, etc
@ThisIsANode(name="Text", lang=CoreLang)
public class Text extends ATextNode {
	@nodeAttr public ATextNode∅  elems;
	public String toText() {
		StringBuffer sb = new StringBuffer();
		boolean add_nl = false;
		foreach (ATextNode t; elems) {
			if (t instanceof TextLine) {
				if (add_nl)
					sb.append('\n');
				add_nl = true;
			}
			sb.append(t.toText());
		}
		return sb.toString();
	}
}

public enum CommentMode {
	LINE, EOLINE, FLOW, INLINE, DOCUMENTATION 
}

@ThisIsANode(name="Comment", lang=CoreLang)
public final class Comment extends Text {

	//@AttrXMLDumpInfo
	//static final class ExtSpaceAttrSlot_comment extends ExtSpaceAttrSlot<Comment> {
	//	ExtSpaceAttrSlot_comment() { super("comment", ANode.nodeattr$parent, true, TypeInfo.newTypeInfo(Comment.class,null)); }
	//	public Language getCompilerLang() { return CoreLang; }
	//	public String getXmlNamespaceURI() { return getCompilerLang().getURI(); }
	//}
	//
    //public static final ExtSpaceAttrSlot_comment ATTR_COMMENT = new ExtSpaceAttrSlot_comment();

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public CommentMode mode;

	public Comment() {
		this.mode = CommentMode.LINE;
	}
}

