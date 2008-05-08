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

    public final short getNodeType() { org.w3c.dom.Node.TEXT_NODE }
	public final String getNodeName() { "#text" }

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
	public String value = "";
	
	public String getNodeValue() throws DOMException { value }
	public void setNodeValue(String value) throws DOMException { this.value = value; }
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
