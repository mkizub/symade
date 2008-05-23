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
public final class GenDomDocument extends ADomDocument {
}

@singleton
public class GenDomImplementation implements org.w3c.dom.DOMImplementation {
	public boolean hasFeature(String feature, String version) { false }
	public Object getFeature(String feature, String version) { null }
	public org.w3c.dom.DocumentType createDocumentType(String qualifiedName, String publicId, String systemId) { null }
	public org.w3c.dom.Document createDocument(String namespaceURI, String qualifiedName, org.w3c.dom.DocumentType doctype) {
		GenDomDocument doc = new GenDomDocument();
		doc.element = doc.createElementNS(namespaceURI, qualifiedName);
		return doc;
	}
}

@ThisIsANode
public abstract class ADomDocument extends ADomNode implements org.w3c.dom.Document {
	
	@nodeAttr
	public ADomElement		element;
	
	@nodeAttr
	public String			xml_version;
	
	@nodeAttr
	public String			xml_encoding;
	
	@nodeAttr
	public boolean			is_standalone;
	
	public String getNodeName() { "#document" }
	public boolean hasChildNodes() { return this.element != null; }
	public org.w3c.dom.NodeList getChildNodes() { new DomDocNodeList() }
	public org.w3c.dom.Node getFirstChild() { this.element }
	public org.w3c.dom.Node getLastChild() { this.element }

	public final org.w3c.dom.Node getParentNode() { null }
    public final short getNodeType() { org.w3c.dom.Node.DOCUMENT_NODE }

	public org.w3c.dom.DocumentType getDoctype() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getDoctype() is not implemented yet");
	}
	
	public org.w3c.dom.DOMImplementation getImplementation() {
		return GenDomImplementation;
	}
    
	public org.w3c.dom.Element getDocumentElement() {
		return this.element;
	}
	
	public org.w3c.dom.Element createElement(String name) throws DOMException {
		ADomElement node = new GenDomElement();
		node.nodeName = name;
		return node;
	}
	
	public org.w3c.dom.DocumentFragment createDocumentFragment() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "createDocumentFragment() is not implemented yet");
	}
	
	public org.w3c.dom.Text createTextNode(String s) {
		DomText txt = new DomText();
		txt.setData(s);
		return txt;
	}
	
	public org.w3c.dom.Comment createComment(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "createComment(String) is not implemented yet");
	}
	
	public org.w3c.dom.CDATASection createCDATASection(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "createCDATASection(String) is not implemented yet");
	}
	
	public org.w3c.dom.ProcessingInstruction createProcessingInstruction(String s, String s1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "createProcessingInstruction(String,String) is not implemented yet");
	}
	
	public org.w3c.dom.Attr createAttribute(String name) throws DOMException {
		if (name == null || name.length() == 0)
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "name="+name);
		ADomAttr attr = new GenDomAttr();
		attr.attrName = name;
		return attr;
	}
	
	public org.w3c.dom.EntityReference createEntityReference(String s) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "createEntityReference(String) is not implemented yet");
	}
	
	public org.w3c.dom.NodeList getElementsByTagName(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementsByTagName(String) is not implemented yet");
	}
	
	public org.w3c.dom.Node importNode(org.w3c.dom.Node node, boolean b) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "importNode(Node,bool) is not implemented yet");
	}
	
	public org.w3c.dom.Element createElementNS(String uri, String qname) throws DOMException {
		if (qname == null || qname.length() == 0)
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "qname="+qname);
		ADomElement node;
		if (uri != null && uri.startsWith("map:")) {
			String nm = qname;
			int p = nm.indexOf(':');
			if (p > 0)
				nm = nm.substring(p+1);
			node = (ADomElement)Class.forName(uri.substring(4)+nm).newInstance();
		} else {
			node = new GenDomElement();
		}
		node.nodeName = qname;
		node.nodeNamespaceURI = uri;
		return node;
	}
	
	public org.w3c.dom.Attr createAttributeNS(String uri, String qname) throws DOMException {
		if (qname == null || qname.length() == 0)
			throw new DOMException(DOMException.INVALID_CHARACTER_ERR, "qname="+qname);
		ADomAttr attr = new GenDomAttr();
		attr.attrName = qname;
		attr.attrNamespaceURI = uri;
		return attr;
	}
	
	public org.w3c.dom.NodeList getElementsByTagNameNS(String s, String s1) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementsByTagNameNS(String,String) is not implemented yet");
	}
	
	public org.w3c.dom.Element getElementById(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getElementById(String) is not implemented yet");
	}
	
	public String getInputEncoding() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getInputEncoding() is not implemented yet");
	}
	
	public String getXmlEncoding() {
		return this.xml_encoding;
	}
	
	public boolean getXmlStandalone() {
		return this.is_standalone;
	}
	
	public void setXmlStandalone(boolean b) throws DOMException {
		this.is_standalone = b;
	}
	
	public String getXmlVersion() {
		return (this.xml_version == null)? "1.0" : this.xml_version;
	}
	
	public void setXmlVersion(String value) throws DOMException {
		if(value.equals("1.0") || value.equals("1.1")) {
			this.xml_version = value;
		} else{
			throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Version "+value+" is not supported");
		}
	}
	
	public boolean getStrictErrorChecking() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getStrictErrorChecking() is not implemented yet");
	}
	
	public void setStrictErrorChecking(boolean b) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setStrictErrorChecking(bool) is not implemented yet");
	}
	
	public String getDocumentURI() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getDocumentURI() is not implemented yet");
	}
	
	public void setDocumentURI(String s) {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "setDocumentURI(String) is not implemented yet");
	}
	
	public org.w3c.dom.Node adoptNode(org.w3c.dom.Node node) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "adoptNode(Node) is not implemented yet");
	}
	
	public org.w3c.dom.DOMConfiguration getDomConfig() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "getDomConfig() is not implemented yet");
	}
	
	public void normalizeDocument() {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "normalizeDocument() is not implemented yet");
	}
	
	public org.w3c.dom.Node renameNode(org.w3c.dom.Node node, String s, String s1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "renameNode(node,String,String) is not implemented yet");
	}

	final class DomDocNodeList implements org.w3c.dom.NodeList {
		public int getLength() {
			return (element == null) ? 0 : 1;
		}
		public Node item(int i) {
			if (i != 0)
				return null;
			return element;
		}
	}
}
