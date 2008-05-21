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
import kiev.be.java15.JSNode;

import syntax kiev.Syntax;

/**
 * A node that is a syntax modifier: import, operator decl, separators, comments, etc.
 */
@ThisIsANode(lang=CoreLang)
public class SNode extends ASTNode {

	@virtual typedef This  ≤ SNode;
	@virtual typedef JView ≤ JSNode;
	@virtual typedef RView ≤ RSNode;

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final SNode dummySNode = new SNode();

	public SNode() {}

	public ASTNode getDummyNode() { SNode.dummySNode }
	
	public final void resolveDecl() { ((RView)this).resolveDecl(); }

}

@ThisIsANode(name="Comment", lang=CoreLang)
public final class Comment extends SNode {

	@virtual typedef This  = Comment;

    public static final AttrSlot ATTR_BEFORE = new ExtAttrSlot("comment before", ANode.nodeattr$parent, false, true, TypeInfo.newTypeInfo(Comment.class,null));
    public static final AttrSlot ATTR_AFTER  = new ExtAttrSlot("comment after",  ANode.nodeattr$parent, false, true, TypeInfo.newTypeInfo(Comment.class,null));

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public boolean eol_form;
	@nodeAttr public boolean multiline;
	@nodeAttr public boolean doc_form;
	@nodeAttr public boolean nl_before;
	@nodeAttr public boolean nl_after;
	@nodeAttr public String  text;
	
	public Comment() {}
}

