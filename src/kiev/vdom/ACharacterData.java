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
public final class DomText extends ACharacterData implements org.w3c.dom.Text {
	
	public DomText() {}
	public DomText(String value) {
		this.value = value;
	}

	public final String getNodeName() { "#text" }
    public final short getNodeType() { org.w3c.dom.Node.TEXT_NODE }

	public String getWholeText() { this.getData() }
	public boolean isElementContentWhitespace() { getData().trim().length() == 0 }
	public org.w3c.dom.Text replaceWholeText(String content) {
		setData(content);
		return this;
	}
	public org.w3c.dom.Text splitText(int offset) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "splitText(int) is not implemented yet");
	}
}

@ThisIsANode
public abstract class ACharacterData extends ADomNode implements org.w3c.dom.CharacterData {
	
	@nodeAttr
	public String value;
	
	public abstract String getNodeName();
	public String getNodeValue() throws DOMException { value }
	public void setNodeValue(String value) throws DOMException { this.value = value; }
    public abstract short getNodeType();

	public int getLength() { value.length() }
	public String getData() throws DOMException { value }
	public void setData(String value) throws DOMException { this.value = value; }
	public void appendData(String value) throws DOMException { this.value += value; }
	public void deleteData(int offset, int count) {
		String val = this.value;
		this.value = val.substring(0,offset)+val.substring(offset+count);
	}
	public void insertData(int offset, String arg) {
		String val = this.value;
		this.value = val.substring(0,offset)+arg+val.substring(offset);
	}
	public void replaceData(int offset, int count, String arg) {
		String val = this.value;
		this.value = val.substring(0,offset)+arg+val.substring(offset+count);
	}
	public String substringData(int offset, int count) {
		this.value.substring(offset, count)
	}
}


public final class DomTextWrapper extends ACharacterDataWrapper implements org.w3c.dom.Text {
	
	public DomTextWrapper(String value, ANode parent, AttrSlot slot) {
		super(value, parent, slot);
	}

	public final String getNodeName() { "#text" }
    public final short getNodeType() { org.w3c.dom.Node.TEXT_NODE }

	public String getWholeText() { this.getData() }
	public boolean isElementContentWhitespace() { getData().trim().length() == 0 }
	public org.w3c.dom.Text replaceWholeText(String content) {
		setData(content);
		return this;
	}
	public org.w3c.dom.Text splitText(int offset) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "splitText(int) is not implemented yet");
	}

	public boolean isSameNode(org.w3c.dom.Node other) {
		if (other instanceof DomTextWrapper) {
			return this.parent() == other.parent() && this.pslot() == other.pslot();
		}
		return false;
	}
										 
}

public abstract class ACharacterDataWrapper implements ADomNodeMixin, org.w3c.dom.CharacterData {
	
	private AttrSlot 		p_slot;
	private ANode			p_parent;
	private String			tmp_text;
	
	@virtual
	public String value;
	
	public ACharacterDataWrapper(String value, ANode parent, AttrSlot slot) {
		this.p_slot = slot;
		this.p_parent = parent;
		this.tmp_text = value;
	}

	public final AttrSlot pslot() { return p_slot; }
	public final ANode parent() { return p_parent; }
	
	@getter public String get$value() {
		if (pslot() == null || parent() == null)
			return tmp_text;
		return (String)pslot().get(parent());
	}
	
	@setter public void set$value(String val) {
		if (pslot() == null || parent() == null)
			tmp_text = val;
		else
			pslot().set(parent(), val);
	}
	
	public abstract String getNodeName();
	public String getNodeValue() throws DOMException { value }
	public void setNodeValue(String value) throws DOMException { this.value = value; }
    public abstract short getNodeType();

	public org.w3c.dom.Document getOwnerDocument() {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return ((org.w3c.dom.Node)p).getOwnerDocument();
		return null;
	}

	public final org.w3c.dom.Node getParentNode() {
		ANode p = parent();
		if (p instanceof org.w3c.dom.Node)
			return (org.w3c.dom.Node)p;
		return null;
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

	public int getLength() { value.length() }
	public String getData() throws DOMException { value }
	public void setData(String value) throws DOMException { this.value = value; }
	public void appendData(String value) throws DOMException { this.value += value; }
	public void deleteData(int offset, int count) {
		String val = this.value;
		this.value = val.substring(0,offset)+val.substring(offset+count);
	}
	public void insertData(int offset, String arg) {
		String val = this.value;
		this.value = val.substring(0,offset)+arg+val.substring(offset);
	}
	public void replaceData(int offset, int count, String arg) {
		String val = this.value;
		this.value = val.substring(0,offset)+arg+val.substring(offset+count);
	}
	public String substringData(int offset, int count) {
		this.value.substring(offset, count)
	}
}
