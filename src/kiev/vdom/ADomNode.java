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

import org.w3c.dom.DOMException;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode
public abstract class ADomNode extends ANode implements org.w3c.dom.Node {
	
	public static final ADomNode[] emptyArray = new ADomNode[0];


	private int						dom_node_flags;

	public @packed:1,dom_node_flags,0 boolean is_readonly;
	public @packed:1,dom_node_flags,1 boolean is_owned;
	public @packed:1,dom_node_flags,2 boolean is_normalized;
	public @packed:1,dom_node_flags,3 boolean is_id_attr;

	
    public abstract short getNodeType();

	public String getNodeValue() throws DOMException { null }
	public void setNodeValue(String nodeValue) throws DOMException {}
	
	public org.w3c.dom.NamedNodeMap getAttributes() { null }
	public org.w3c.dom.NodeList getChildNodes() { null }
	public org.w3c.dom.Node getFirstChild() { null }
	public org.w3c.dom.Node getLastChild() { null }
	public org.w3c.dom.Node getPreviousSibling() { null }
	public org.w3c.dom.Node getNextSibling() { null }
	public boolean hasChildNodes() { false }
	public boolean hasAttributes() { false }

	public String getNodeName() { null }
	public String getNamespaceURI() { null }
	public String getPrefix() { null }
	public String getLocalName() { null }
	public String getBaseURI() { null }
	
	public void setPrefix(String prefix) throws DOMException {
		throw new DOMException(DOMException.NAMESPACE_ERR, "Cannot set namespace for this type of node");
	}
	
	public final org.w3c.dom.Node getParentNode() {
		if (is_owned)
			return (org.w3c.dom.Node)parent();
		return null;
	}

	public final org.w3c.dom.Document getOwnerDocument() {
		if (this instanceof org.w3c.dom.Document)
			return null;
		ANode p = parent();
		if !(is_owned)
			return (org.w3c.dom.Document)p;
		while (p != null && !(p instanceof org.w3c.dom.Node) && !(p instanceof org.w3c.dom.Document))
			p = p.parent();
		if (p instanceof org.w3c.dom.Document)
			return (org.w3c.dom.Document)p;
		return null;
	}
	
	public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "insertBefore(Node,Node) is not implemented yet");
	}
	
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, org.w3c.dom.Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "replaceChild(Node,Node) is not implemented yet");
	}
	
	public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "removeChild(Node) is not implemented yet");
	}
	
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "appendChild(Node) is not implemented yet");
	}
	
	public org.w3c.dom.Node cloneNode(boolean deep) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "cloneNode(Node) is not implemented yet");
	}
	
	public void normalize() {}
	
	public boolean isSupported(String feature, String version) {
		return false;
	}
	
	public short compareDocumentPosition(org.w3c.dom.Node other) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "compareDocumentPosition(Node) is not implemented yet");
	}
	public String getTextContent() throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getTextContent() is not implemented yet");
	}
	public void setTextContent(String textContent) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setTextContent(String) is not implemented yet");
	}
	
	public boolean isSameNode(org.w3c.dom.Node other) {
		return this == other;
	}
	public boolean isEqualNode(org.w3c.dom.Node other) {
		return this == other;
	}

	public String lookupPrefix(String namespaceURI) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "lookupPrefix(String) is not implemented yet");
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "isDefaultNamespace(String) is not implemented yet");
	}
	
	public String lookupNamespaceURI(String prefix) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "lookupNamespaceURI(String) is not implemented yet");
	}
	
	public Object getFeature(String feature, String version) {
		return null;
	}
	
	public Object setUserData(String key, Object data, org.w3c.dom.UserDataHandler handler) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setUserData(String,Object,UserDataHandler) is not implemented yet");
	}
	
	public Object getUserData(String key) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getUserData(String) is not implemented yet");
	}

}
