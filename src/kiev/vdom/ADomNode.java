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
package kiev.vdom;
import syntax kiev.Syntax;

import org.w3c.dom.DOMException;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode
public abstract class ADomNode extends ASTNode implements ADomNodeMixin, org.w3c.dom.Node {
	
	public static final ADomNode[] emptyArray = new ADomNode[0];
	
	//
	//
	// DOM level 1
	//
	//
	
	public abstract String getNodeName();
	
	public abstract short getNodeType();
	
	public org.w3c.dom.Node getParentNode() {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return (org.w3c.dom.Node)p;
		return null;
	}

	
	public org.w3c.dom.Node getPreviousSibling() { (org.w3c.dom.Node)ANode.getPrevNode(this) }
	
	public org.w3c.dom.Node getNextSibling() { (org.w3c.dom.Node)ANode.getNextNode(this) }
	
	//
	//
	// DOM level 2
	//
	//
	
	public org.w3c.dom.Document getOwnerDocument() {
		if (this instanceof org.w3c.dom.Document)
			return null;
		ANode p = parent();
		while (p != null) {
			if !(p instanceof org.w3c.dom.Node)
				return null;
			if (p instanceof org.w3c.dom.Document)
				return (org.w3c.dom.Document)p;
			p = p.parent();
		}
		return null;
	}

	//
	//
	// DOM level 3
	//
	//
	
	public boolean isSameNode(org.w3c.dom.Node other) {
		return this == other;
	}
										 
	public String lookupPrefix(String namespaceURI) {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return p.lookupPrefix(namespaceURI);
		return null;
	}
 
	public boolean isDefaultNamespace(String namespaceURI) {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return p.isDefaultNamespace(namespaceURI);
		return false;
	}
										 
	public String lookupNamespaceURI(String prefix) {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return p.lookupNamespaceURI(prefix);
		return null;
	}

	public boolean isEqualNode(org.w3c.dom.Node other) {
		return this == other;
	}
}

