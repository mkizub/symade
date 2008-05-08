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
public abstract class ADomContainer extends ADomNode {

	@nodeAttr
	public ADomNode[]	elements;

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
}

