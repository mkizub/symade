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

public final class DomAttrSlot extends ExtAttrSlot {
	
	private static final Hashtable<String,DomAttrSlot> ALL_DOM_ATTRS;
	public static final DomAttrSlot DEFAULT_NS_DOM_ATTR;
	
	static {
		ALL_DOM_ATTRS = new Hashtable<String,DomAttrSlot>();
		DEFAULT_NS_DOM_ATTR = getAttrSlot(null, "xmlns", null);
	}

	public static DomAttrSlot getAttrSlot(String prefix, String local_name, String uri) {
		String name;
		if ("xmlns".equals(prefix))
			uri = "http://www.w3.org/XML/1998/namespace";
		if (uri == null)
			name = local_name;
		else
			name = "{" + uri + "}" + local_name;
		DomAttrSlot slot = ALL_DOM_ATTRS.get(name);
		if (slot == null) {
			slot = new DomAttrSlot(prefix, local_name, uri);
			ALL_DOM_ATTRS.put(name, slot);
		}
		return slot;
	}

	public final String namespaceURI;
	public final String prefix;
	
	private DomAttrSlot(String prefix, String name, String uri) {
		super(name, ANode.nodeattr$parent, false, true, TypeInfo.newTypeInfo(String.class, null));
		if (uri != null) uri = uri.intern();
		this.namespaceURI = uri;
		if (prefix != null) prefix = prefix.intern();
		this.prefix = prefix;
	}
	
	public String fullName() {
		if (prefix != null && prefix.length() > 0)
			return prefix + ":" + name;
		return name;
	}

	public boolean isXmlIgnore() { return false; }
	public boolean isXmlAttr() { return true; }
	public String getXmlFullName() {
		if (prefix != null && prefix.length() > 0)
			return prefix + ":" + getXmlLocalName();
		return getXmlLocalName();
	}
	public String getXmlNamespaceURI() { return namespaceURI; }
}


public final class GenDomAttr extends ADomAttr {
	public GenDomAttr(String prefix, String name, String uri) {
		super(null, null, DomAttrSlot.getAttrSlot(prefix, name, uri));
	}
	public GenDomAttr(String text, ANode parent, AttrSlot slot) {
		super(text, parent, slot);
	}
}

public class ADomAttr implements ADomNodeMixin, org.w3c.dom.Attr {
	
	AttrSlot 		p_slot;
	ANode			p_parent;
	String			tmp_text;
	
	ADomAttr(String text, ANode parent, AttrSlot slot) {
		if (slot == null)
			throw new NullPointerException();
		this.p_slot = slot;
		this.p_parent = parent;
		this.tmp_text = text;
	}
	
	public final AttrSlot pslot() {
		return p_slot;
	}
	
	public final ANode parent() {
		return p_parent;
	}
	
	private DomTextWrapper makeTextNode() {
		return new DomTextWrapper(getNodeValue(), parent(), pslot());
	}
	
	//
	//
	// DOM level 1 Node interface
	//
	//
	
	public String getNodeName() {
		AttrSlot slot = this.pslot();
		if (slot instanceof DomAttrSlot) {
			String prefix = slot.prefix;
			if (prefix != null && prefix.length() > 0)
				return prefix + ":" + slot.name;
		}
		return slot.name;
	}
	public String getNodeValue() {
		if (pslot() == null || parent() == null)
			return tmp_text;
		return (String)pslot().get(parent());
	}
	public void setNodeValue(String value) {
		if (pslot() == null || parent() == null)
			tmp_text = value;
		else
			pslot().set(parent(), value);
	}
    public final short getNodeType() { org.w3c.dom.Node.ATTRIBUTE_NODE }
	public final org.w3c.dom.Node getParentNode() { null }

	public org.w3c.dom.NodeList getChildNodes() {
		return new org.w3c.dom.NodeList() {
			public int getLength() { 1 }
			public org.w3c.dom.Node item(int idx) {
				return (idx == 0) ? makeTextNode() : null;
			}
		};
	}
	public org.w3c.dom.Node getFirstChild() { makeTextNode() }
	public org.w3c.dom.Node getLastChild() { makeTextNode() }

	public boolean hasChildNodes() { true }

	//
	//
	// DOM level 2 Node interface
	//
	//
	
	public org.w3c.dom.Document getOwnerDocument() {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return ((org.w3c.dom.Node)p).getOwnerDocument();
		return null;
	}

	public final String getNamespaceURI() {
		AttrSlot slot = this.pslot();
		if (slot == null)
			return null;
		if (slot instanceof DomAttrSlot) {
			String namespaceURI = slot.namespaceURI;
			if (namespaceURI != null)
				return namespaceURI;
		}
		return null;
	}

	public String getPrefix() {
		AttrSlot slot = this.pslot();
		if (slot == null)
			return null;
		if (slot instanceof DomAttrSlot) {
			String prefix = slot.prefix;
			if (prefix != null && prefix.length() > 0)
				return prefix;
		}
		return null;
	}
	
	public void setPrefix(String prefix) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "readonly element");
	}

	public String getLocalName() {
		AttrSlot slot = this.pslot();
		if (slot == null)
			return null;
		return slot.name;
	}

	//
	//
	// DOM level 3 Node interface
	//
	//
	
	public boolean isSameNode(org.w3c.dom.Node other) {
		if (other instanceof ADomAttr) {
			return this.parent() == other.parent() && this.pslot() == other.pslot();
		}
		return false;
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

	//
	//
	// DOM level 1 Attr interface
	//
	//
	
	public String getName() { getNodeName() }
	
	public boolean getSpecified() { true }

	public String getValue() { getNodeValue() }
	
	public void setValue(String value) { setNodeValue(value) }
	
	//
	//
	// DOM level 2 Attr interface
	//
	//
	
	public org.w3c.dom.Element getOwnerElement() {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Element)
			return (org.w3c.dom.Element)p;
		return null;
	}
	
	//
	//
	// DOM level 3 Attr interface
	//
	//
	
	public TypeInfo getSchemaTypeInfo() { null }
	
	public boolean isId() { false }
}
