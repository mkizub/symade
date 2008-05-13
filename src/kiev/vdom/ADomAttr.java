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
public final class GenDomAttr extends ADomAttr {
}

@ThisIsANode
public final class DfltNsDomAttr extends ADomAttr {
	
	public DfltNsDomAttr() {}
	public DfltNsDomAttr(String uri) {
		this.attrName = "xmlns";
		this.attrNamespaceURI = null;
		this.setNodeValue(uri);
	}
}

@ThisIsANode
public final class NsDomAttr extends ADomAttr {
	
	public NsDomAttr() {}
	public NsDomAttr(String name, String uri) {
		this.attrName = "xmlns:"+name;
		this.attrNamespaceURI = "http://www.w3.org/XML/1998/namespace";
		this.setNodeValue(uri);
	}
}

@ThisIsANode
public abstract class ADomAttr extends ADomNode implements org.w3c.dom.Attr {
	
	public static final ADomAttr[] emptyArray = new ADomAttr[0];

	@nodeAttr
	public String		attrNamespaceURI;
	@nodeAttr
	public String		attrName;
	@nodeAttr
	public DomText		text;
	
	//
	//
	// DOM level 1 Node interface
	//
	//
	
	public String getNodeName() { attrName }
	public String getNodeValue() {
		if (text == null)
			return null;
		return text.getData();
	}
	public void setNodeValue(String value) {
		if (text == null)
			text = new DomText(value);
		else
			text.setData(value);
	}
    public final short getNodeType() { org.w3c.dom.Node.ATTRIBUTE_NODE }
	public final org.w3c.dom.Node getParentNode() { null }

	public org.w3c.dom.NodeList getChildNodes() {
		return new org.w3c.dom.NodeList() {
			public int getLength() {
				DomText txt = ADomAttr.this.text;
				return (txt == null) ? 0 : 1;
			}
			public org.w3c.dom.Node item(int idx) {
				DomText txt = ADomAttr.this.text;
				return (idx == 0) ? txt : null;
			}
		};
	}
	public org.w3c.dom.Node getFirstChild() { text }
	public org.w3c.dom.Node getLastChild() { text }

	public org.w3c.dom.Node getPreviousSibling() { null }
	public org.w3c.dom.Node getNextSibling() { null }

	public boolean hasChildNodes() { text != null }

	//
	//
	// DOM level 2 Node interface
	//
	//
	
	public final String getNamespaceURI() { attrNamespaceURI }

	public String getPrefix() {
		int p = attrName.indexOf(':');
        return p < 0 ? null : attrName.substring(0, p);
	}
	public void setPrefix(String prefix) throws DOMException {
		String nm = this.attrName;
		int p = nm.indexOf(':');
		if (p < 0)
			attrName = prefix + ":" + nm;
		else
			attrName = prefix + ":" + nm.substring(p+1);
	}
	public String getLocalName() {
		int p = attrName.indexOf(':');
        return p < 0 ? attrName : attrName.substring(p+1);
	}

	
	//
	//
	// DOM level 1 Attr interface
	//
	//
	
	public String getName() { attrName }
	
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
