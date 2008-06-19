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

import kiev.ir.java15.RSNode;

import syntax kiev.Syntax;

/**
 * A node that is a syntax modifier: import, operator decl, separators, comments, etc.
 */
@ThisIsANode(lang=CoreLang)
public abstract class SNode extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public SNode() {}

	public final void resolveDecl() { ((RSNode)this).resolveDecl(); }

}

@ThisIsANode(name="Comment", lang=CoreLang)
public final class Comment extends SNode {

	@AttrXMLDumpInfo
	static final class ExtSpaceAttrSlot_comment extends ExtSpaceAttrSlot {
		ExtSpaceAttrSlot_comment() { super("comment", ANode.nodeattr$parent, TypeInfo.newTypeInfo(Comment.class,null)); }
		public Language getCompilerLang() { return CoreLang; }
		public String getXmlNamespaceURI() { return getCompilerLang().getURI(); }
	}

    public static final ExtSpaceAttrSlot_comment ATTR_COMMENT = new ExtSpaceAttrSlot_comment();

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean eol_form;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean multiline;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean doc_form;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean nl_before;
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr public boolean nl_after;
	@nodeAttr public String  text;
	
	public Comment() {}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "eol_form") return eol_form;
		if (attr.name == "multiline") return multiline;
		if (attr.name == "doc_form") return doc_form;
		if (attr.name == "nl_before") return nl_before;
		if (attr.name == "nl_after") return nl_after;
		return super.includeInDump(dump, attr, val);
	}
}

