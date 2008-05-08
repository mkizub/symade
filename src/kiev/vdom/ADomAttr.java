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
public abstract class ADomAttr extends ADomNode implements org.w3c.dom.Attr {
	
	public static final ADomAttr[] emptyArray = new ADomAttr[0];

	public String		attrNamespaceURI;
	public String		attrName;

	private String value = "";
	
    public final short getNodeType() { org.w3c.dom.Node.ATTRIBUTE_NODE }

	public String getValue() throws DOMException { value }
	public void setValue(String value) throws DOMException { this.value = value; }
	
	public String getNodeValue() throws DOMException { value }
	public void setNodeValue(String value) throws DOMException { this.value = value; }
	
	public String getNodeName() { attrName }
	public String getNamespaceURI() { attrNamespaceURI }
	public String getPrefix() {
		int p = attrName.indexOf(':');
        return p < 0 ? null : attrName.substring(0, p);
	}
	public String getLocalName() {
		int p = attrName.indexOf(':');
        return p < 0 ? attrName : attrName.substring(p+1);
	}

}
