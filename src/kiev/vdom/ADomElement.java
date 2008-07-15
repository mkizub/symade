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

import org.w3c.dom.*;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode
public final class GenDomElement extends ADomElement {

	@nodeAttr
	public String			namespaceURI;
	@nodeAttr
	public String			prefixName;
	@nodeAttr
	public String			localName;
	
	public final String getNamespaceURI() { namespaceURI }

	public final String getPrefix() { prefixName }

	public final String getLocalName() { localName }

	public final void setPrefix(String prefix) throws DOMException {
		this.prefixName = prefix;
	}

}

@ThisIsANode
public class ADomElement extends ADomNode implements org.w3c.dom.Element {

	public static final ADomElement[] emptyArray = new ADomElement[0];

	@nodeAttr
	public ADomNode[]		elements;

	//
	//
	// DOM level 1 Node interface
	//
	//
	
	public final String getNodeName() {
		String p = getPrefix();
		if (p == null || p.length() == 0)
			return getLocalName();
		return p + ":" + getLocalName();
	}
	
    public final short getNodeType() { org.w3c.dom.Node.ELEMENT_NODE }
	public org.w3c.dom.NamedNodeMap getAttributes() { new AttrMap() }


	public org.w3c.dom.NodeList getChildNodes() { new DomNodeList() }

	public org.w3c.dom.Node getFirstChild() {
		ADomNode[] els = elements;
		if (els.length > 0)
			return els[0];
		return null;
	}
	public org.w3c.dom.Node getLastChild() {
		ADomNode[] els = elements;
		if (els.length > 0)
			return els[els.length-1];
		return null;
	}
	
	public org.w3c.dom.Node getPreviousSibling() { (org.w3c.dom.Node)ANode.getPrevNode(this) }
	public org.w3c.dom.Node getNextSibling() { (org.w3c.dom.Node)ANode.getNextNode(this) }
	
	public boolean hasChildNodes() { elements.length > 0 }

	//
	//
	// DOM level 2 Node interface
	//
	//
	
	public String getNamespaceURI() { null }

	public String getPrefix() { null }

	public String getLocalName() { getCompilerNodeName() }

	public void setPrefix(String prefix) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setPrefix(String) is not supported for this kind of elements");
	}

	public boolean hasAttributes() {
		foreach (AttrSlot slot; this.values(); slot.isXmlAttr()) {
			if (slot.get(this) != null)
				return true;
		}
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot)
				return true;
		}
		return false;
	}


	//
	//
	// DOM level 3 Node interface
	//
	//
	public String lookupPrefix(String namespaceURI) {
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot) {
				DomAttrSlot slot = (DomAttrSlot)dai.p_slot;
				if (slot.prefix == "xmlns" && dai.p_data.equals(namespaceURI))
					return slot.name;
			}
		}
		return super.lookupPrefix(namespaceURI);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot) {
				DomAttrSlot slot = (DomAttrSlot)dai.p_slot;
				if (slot.name == "xmlns")
					return dai.p_data.equals(namespaceURI);
			}
		}
		return super.isDefaultNamespace(namespaceURI);
	}

	public String lookupNamespaceURI(String prefix) {
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			if (prefix == null || prefix.length() == 0) {
				foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot && ((DomAttrSlot)dai.p_slot).prefix == null && ((DomAttrSlot)dai.p_slot).name == "xmlns")
					return (String)dai.p_data;
			} else {
				foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot && ((DomAttrSlot)dai.p_slot).prefix == "xmlns" && dai.p_slot.name.equals(prefix))
					return (String)dai.p_data;
			}
		}
		return super.lookupNamespaceURI(prefix);
	}

	
	//
	//
	// DOM level 1 Element interface
	//
	//
	
	public String getTagName() { getNodeName() }
	
	public String getAttribute(String name) {
		ADomAttr dattr = lookupAttrNode(name);
		if (dattr == null)
			return null;
		return dattr.getNodeValue();
	}
	
	public void setAttribute(String name, String value) throws DOMException {
		if (name == null || name.length() == 0)
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "name="+name);
		ADomAttr dattr = lookupAttrNode(name);
		if (dattr == null)
			dattr = new GenDomAttr(null, this, DomAttrSlot.getAttrSlot(null,name,null));
		dattr.setNodeValue(value);
	}

	public void removeAttribute(String nm) throws DOMException {
		ADomAttr dattr = lookupAttrNode(nm);
		if (dattr == null)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+nm+" not found");
		this.delExtData(dattr.pslot());
	}

	public org.w3c.dom.Attr getAttributeNode(String nm) {
		return lookupAttrNode(nm);
	}

	public org.w3c.dom.Attr setAttributeNode(org.w3c.dom.Attr _attr) throws DOMException {
		ADomAttr attr = (ADomAttr)_attr;
		this.setExtData(attr.getNodeValue(),attr.pslot());
		return null;
	}

	public org.w3c.dom.Attr removeAttributeNode(org.w3c.dom.Attr _attr) throws DOMException {
		ADomAttr attr = (ADomAttr)_attr;
		this.delExtData(attr.pslot());
		return null;
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
		ADomAttr dattr = lookupAttrNode(uri, nm);
		if (dattr == null)
			return null;
		return dattr.getNodeValue();
	}

	public void setAttributeNS(String uri, String qname, java.lang.String value) throws DOMException {
		ADomAttr dattr = lookupAttrNode(uri, qname);
		if (dattr == null) {
			String prefix = null;
			int p = qname.indexOf(':');
			if (p >= 0) {
				prefix = qname.substring(0,p);
				qname = qname.substring(p+1);
			}
			dattr = new GenDomAttr(value, this, DomAttrSlot.getAttrSlot(prefix,qname,uri));
		}
		dattr.setNodeValue(value);
	}

	public void removeAttributeNS(String uri, String nm) throws DOMException {
		ADomAttr dattr = lookupAttrNode(uri, nm);
		if (dattr == null)
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Attribute "+uri+':'+nm+" not found");
		this.delExtData(dattr.pslot());
	}

	public org.w3c.dom.Attr getAttributeNodeNS(String uri, String nm) throws DOMException {
		return lookupAttrNode(uri, nm);
	}
	
	public org.w3c.dom.Attr setAttributeNodeNS(org.w3c.dom.Attr _attr) throws DOMException {
		ADomAttr attr = (ADomAttr)_attr;
		this.setExtData(attr.getNodeValue(),attr.pslot());
		return null;
	}

	public org.w3c.dom.NodeList getElementsByTagNameNS(String uri, String nm) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementsByTagNameNS(String,String) is not implemented yet");
	}

	public boolean hasAttribute(String qname) {
		return lookupAttrNode(qname) != null;
	}
	
	public boolean hasAttributeNS(String uri, String lname) throws DOMException {
		return lookupAttrNode(uri,lname) != null;
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

	final ADomAttr lookupAttrNode(String name) {
		if (name == null)
			return null;
		foreach (AttrSlot slot; this.values(); slot.isXmlAttr() && slot.getXmlFullName().equals(name))
			return new GenDomAttr(null,this,slot);
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot) {
				DomAttrSlot slot = (DomAttrSlot)dai.p_slot;
				if (slot.fullName().equals(name))
					return new GenDomAttr((String)dai.p_data,this,slot);
			}
		}
		return null;
	}

	final ADomAttr lookupAttrNode(String uri, String name) {
		if (name == null)
			return null;
		foreach (AttrSlot slot; this.values(); slot.isXmlAttr()) {
			String a_uri = slot.getXmlNamespaceURI();
			String a_name = slot.getXmlLocalName();
			if (uri == null) {
				if (a_uri == null && name.equals(a_name))
					return new GenDomAttr(null,this,slot);
			} else {
				if (uri.equals(a_uri) && name.equals(a_name))
					return new GenDomAttr(null,this,slot);
			}
		}
		DataAttachInfo[] data = this.getAllExtData();
		if (data != null) {
			foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot) {
				DomAttrSlot slot = (DomAttrSlot)dai.p_slot;
				String a_uri = slot.namespaceURI;
				String a_name = slot.name;
				if (uri == null) {
					if (a_uri == null && name.equals(a_name))
						return new GenDomAttr((String)dai.p_data,this,slot);
				} else {
					if (uri.equals(a_uri) && name.equals(a_name))
						return new GenDomAttr((String)dai.p_data,this,slot);
				}
			}
		}
		return null;
	}

	final class DomNodeList implements org.w3c.dom.NodeList {
	
		public int getLength() {
			return elements.length;
		}
		
		public Node item(int i) {
			ADomNode[] els = elements;
			if (i < 0 || i > els.length)
				return null;
			return els[i];
		}
	}

	final class AttrMap implements org.w3c.dom.NamedNodeMap {
		
		public int getLength() {
			int count = 0;
			foreach (AttrSlot slot; this.values(); slot.isXmlAttr()) {
				if (slot.get(ADomElement.this) != null)
					count++;
			}
			DataAttachInfo[] data = this.getAllExtData();
			if (data != null) {
				foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot)
					count++;
			}
			return count;
		}
		
		public org.w3c.dom.Node item(int i) {
			int count = 0;
			foreach (AttrSlot slot; this.values(); slot.isXmlAttr()) {
				Object val = slot.get(ADomElement.this);
				if (val != null) {
					if (i == count)
						return new GenDomAttr(String.valueOf(val),ADomElement.this,slot);
					count++;
				}
			}
			DataAttachInfo[] data = this.getAllExtData();
			if (data != null) {
				foreach (DataAttachInfo dai; data; dai.p_slot instanceof DomAttrSlot) {
					if (i == count)
						return new GenDomAttr((String)dai.p_data,ADomElement.this,dai.p_slot);
					count++;
				}
			}
			return null;
		}
		
		public org.w3c.dom.Node getNamedItem(java.lang.String nm) {
			return lookupAttrNode(nm);
		}
		
		public org.w3c.dom.Node setNamedItem(org.w3c.dom.Node _node) throws DOMException {
			ADomAttr attr = (ADomAttr)_node;
			return setAttributeNode(attr);
		}

		public org.w3c.dom.Node removeNamedItem(String nm) throws DOMException {
			removeAttribute(nm);
			return null;
		}
		
		public org.w3c.dom.Node getNamedItemNS(String uri, String nm) throws DOMException {
			return lookupAttrNode(uri,nm);
		}
		
		public org.w3c.dom.Node setNamedItemNS(org.w3c.dom.Node _node) throws DOMException {
			ADomAttr attr = (ADomAttr)_node;
			return setAttributeNodeNS(attr);
		}
		
		public org.w3c.dom.Node removeNamedItemNS(String uri, String nm) throws DOMException {
			removeAttributeNS(uri,nm);
			return null;
		}
		
	}
}

