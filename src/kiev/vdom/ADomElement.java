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

import org.w3c.dom.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode
public final class GenDomElement extends ADomElement {
}

@ThisIsANode
public abstract class ADomElement extends ADomContainer implements org.w3c.dom.Element {

	public static final ADomElement[] emptyArray = new ADomElement[0];

	@nodeAttr
	public String			nodeNamespaceURI;
	@nodeAttr
	public String			nodeName;
	@nodeAttr
	public ADomAttr[]		attributes;
	
	//
	//
	// DOM level 1 Node interface
	//
	//
	
	public final String getNodeName() { nodeName }
    public final short getNodeType() { org.w3c.dom.Node.ELEMENT_NODE }
	public org.w3c.dom.NamedNodeMap getAttributes() { new AttrMap() }


	//
	//
	// DOM level 2 Node interface
	//
	//
	
	public final String getNamespaceURI() { nodeNamespaceURI }
	
	public String getPrefix() {
		int p = nodeName.indexOf(':');
        return p < 0 ? null : nodeName.substring(0, p);
	}
	public void setPrefix(String prefix) throws DOMException {
		String nm = this.nodeName;
		int p = nm.indexOf(':');
		if (p < 0)
			nodeName = prefix + ":" + nm;
		else
			nodeName = prefix + ":" + nm.substring(p+1);
	}
	public String getLocalName() {
		int p = nodeName.indexOf(':');
        return p < 0 ? nodeName : nodeName.substring(p+1);
	}

	public boolean hasAttributes() { attributes.length > 0 }


	//
	//
	// DOM level 3 Node interface
	//
	//
	public String lookupPrefix(String namespaceURI) {
		foreach (NsDomAttr ns; attributes; ns.getNodeValue().equals(namespaceURI)) {
			return ns.getLocalName();
		}
		return super.lookupPrefix(namespaceURI);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		foreach (DfltNsDomAttr ns; attributes; ns.getNodeValue().equals(namespaceURI)) {
			return true;
		}
		return super.isDefaultNamespace(namespaceURI);
	}

	public String lookupNamespaceURI(String prefix) {
		if (prefix == null || prefix.length() == 0) {
			foreach (DfltNsDomAttr ns; attributes)
				return ns.getNodeValue();
		} else {
			foreach (NsDomAttr ns; attributes; ns.getLocalName().equals(prefix))
				return ns.getNodeValue();
		}
		return super.lookupNamespaceURI(prefix);
	}

	
	//
	//
	// DOM level 1 Element interface
	//
	//
	
	public String getTagName() { nodeName }
	
	public String getAttribute(String name) {
		int i = lookupAttrIndex(name);
		if (i < 0 || i >= attributes.length)
			return null;
		return attributes[i].getValue();
	}
	
	public void setAttribute(String name, String value) throws DOMException {
		if (name == null || name.length() == 0)
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "name="+name);
		ADomAttr attr = new GenDomAttr();
		attr.attrName = name;
		attr.setValue(value);
		setAttributeNodeNS(attr);
	}

	public void removeAttribute(String nm) throws DOMException {
		if (is_readonly)
			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
		int i = lookupAttrIndex(nm);
		if (i < 0)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+nm+" not found");
		attributes.del(i);
	}

	public org.w3c.dom.Attr getAttributeNode(String name) {
		int i = lookupAttrIndex(name);
		if (i >= 0 && i < attributes.length)
			return attributes[i];
		return null;
	}

	public org.w3c.dom.Attr setAttributeNode(org.w3c.dom.Attr _attr) throws DOMException {
		ADomAttr attr = (ADomAttr)_attr;
		if (is_readonly)
			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
		int i = lookupAttrIndex(attr.getNodeName());
		if (i < 0) {
			attributes.append(attr);
			return null;
		} else {
			ADomAttr old = attributes[i];
			attributes[i] = attr;
			return old;
		}
	}

	public org.w3c.dom.Attr removeAttributeNode(org.w3c.dom.Attr attr) throws DOMException {
		if (is_readonly)
			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
		int i = lookupAttrIndex(attr.getNodeName());
		if (i < 0)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+attr.getNodeName()+" not found");
		org.w3c.dom.Attr old = attributes[i];
		attributes.del(i);
		return old;
	}

    public org.w3c.dom.NodeList getElementsByTagName(String name) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementsByTagName(String) is not implemented yet");
	}
	
	
	//
	//
	// DOM level 2 Element interface
	//
	//
	
	public String getAttributeNS(String uri, String nm) throws DOMException {
		int i = lookupAttrIndex(uri, nm);
		if (i < 0 || i >= attributes.length)
			return null;
		return attributes[i].getValue();
	}

	public void setAttributeNS(String uri, String nm, java.lang.String value) throws DOMException {
		int i = lookupAttrIndex(uri, nm);
		if (i < 0) {
			if (nm == null || nm.length() == 0)
				throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "name="+nm);
			ADomAttr attr = new GenDomAttr();
			attr.attrName = nm;
			attr.attrNamespaceURI = uri;
			attr.setValue(value);
			setAttributeNodeNS(attr);
		} else {
			ADomAttr attr = attributes[i];
			attr.setValue(value);
		}
	}

	public void removeAttributeNS(String uri, String nm) throws DOMException {
		if (is_readonly)
			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
		int i = lookupAttrIndex(uri,nm);
		if (i < 0)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+uri+':'+nm+" not found");
		attributes.del(i);
	}

	public org.w3c.dom.Attr getAttributeNodeNS(String uri, String nm) throws DOMException {
		int i = lookupAttrIndex(uri, nm);
		if (i >= 0)
			return attributes[i];
		return null;
	}
	
	public org.w3c.dom.Attr setAttributeNodeNS(org.w3c.dom.Attr _attr) throws DOMException {
		ADomAttr attr = (ADomAttr)_attr;
		if (is_readonly)
			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
		int i = lookupAttrIndex(attr.getNamespaceURI(), attr.getNodeName());
		if (i < 0) {
			attributes.append(attr);
			return null;
		} else {
			ADomAttr old = attributes[i];
			attributes[i] = attr;
			return old;
		}
	}

	public org.w3c.dom.NodeList getElementsByTagNameNS(String uri, String nm) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementsByTagNameNS(String,String) is not implemented yet");
	}

	public boolean hasAttribute(String nm) {
		return lookupAttrIndex(nm) >= 0;
	}
	
	public boolean hasAttributeNS(String uri, String nm) throws DOMException {
		return lookupAttrIndex(uri,nm) >= 0;
	}
	
	
	//
	//
	// DOM level 3 Element interface
	//
	//
	
	public org.w3c.dom.TypeInfo getSchemaTypeInfo() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getSchemaTypeInfo() is not implemented yet");
	}
	
	public void setIdAttribute(String nm, boolean b) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setIdAttribute(String,bool) is not implemented yet");
	}
	
	public void setIdAttributeNS(String uri, String nm, boolean b) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setIdAttributeNS(String,String,bool) is not implemented yet");
	}
	
	public void setIdAttributeNode(org.w3c.dom.Attr attr, boolean b) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setIdAttributeNS(String,String,bool) is not implemented yet");
	}
	
	
	//
	//
	// Support code
	//
	//

	final int lookupAttrIndex(String name) {
		if (name == null)
			return -1;
		ADomAttr[] attrs = attributes;
		for (int i=0; i < attrs.length; i++) {
			if (name.equals(attrs[i].getNodeName()))
				return i;
		}
		return -1;
	}

	final int lookupAttrIndex(String uri, String name) {
		if (name == null)
			return -1;
		ADomAttr[] attrs = attributes;
		for (int i = 0; i < attrs.length; i++) {
			ADomAttr a = attrs[i];
			String a_uri = a.getNamespaceURI();
			String a_name = a.getLocalName();
			if (uri == null) {
				if (a_uri == null && (name.equals(a_name) ||
					(a_name == null && name.equals(a.getNodeName()))))
					return i;
			} else {
				if (uri.equals(a_uri) && name.equals(a_name))
					return i;
			}
		}
		return -1;
	}

	final class AttrMap implements org.w3c.dom.NamedNodeMap {
		
		public int getLength() { attributes.length }
		
		public org.w3c.dom.Node item(int i) {
			if (i < 0 || i >= attributes.length)
				return null;
			return attributes[i];
		}
		
		public org.w3c.dom.Node getNamedItem(java.lang.String nm) {
			return item(lookupAttrIndex(nm));
		}
		
		public org.w3c.dom.Node setNamedItem(org.w3c.dom.Node _node) throws DOMException {
			ADomAttr attr = (ADomAttr)_node;
			return setAttributeNode(attr);
		}

		public org.w3c.dom.Node removeNamedItem(String nm) throws DOMException {
			if (is_readonly)
				throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
			int i = lookupAttrIndex(nm);
			if (i < 0)
				throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+nm+" not found");
			ADomAttr old = attributes[i];
			attributes.del(i);
			return old;
		}
		
		public org.w3c.dom.Node getNamedItemNS(String uri, String nm) throws DOMException {
			return item(lookupAttrIndex(uri,nm));
		}
		
		public org.w3c.dom.Node setNamedItemNS(org.w3c.dom.Node _node) throws DOMException {
			ADomAttr attr = (ADomAttr)_node;
			if (is_readonly)
				throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
			int i = lookupAttrIndex(attr.getNamespaceURI(), attr.getLocalName());
			if (i < 0) {
				attributes.append(attr);
				return null;
			} else {
				ADomAttr old = attributes[i];
				attributes[i] = attr;
				return old;
			}
		}
		
		public org.w3c.dom.Node removeNamedItemNS(String uri, String nm) throws DOMException {
			if (is_readonly)
				throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
			int i = lookupAttrIndex(uri,nm);
			if (i < 0)
				throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+uri+':'+nm+" not found");
			ADomAttr old = attributes[i];
			attributes.del(i);
			return old;
		}
		
	}
}

