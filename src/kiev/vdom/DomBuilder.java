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

import java.io.IOException;
import javax.xml.parsers.*;
import org.xml.sax.*;

import org.w3c.dom.DOMException;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class DomBuilderFactory extends DocumentBuilderFactory {
	public DocumentBuilder newDocumentBuilder() { new DomBuilder() }
	public Object getAttribute(String name) { null }
	public void setAttribute(String name, Object value) {}
	public boolean getFeature(String name) { false }
	public void setFeature(String name, boolean value) {}
}

public final class DomBuilder extends DocumentBuilder {
	
	private EntityResolver		entityResolver;
	private ErrorHandler		errorHandler;
    
    public boolean isNamespaceAware() { true }
    
    public boolean isValidating() { false }
    
    public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}
    
    public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}
    
    public org.w3c.dom.Document newDocument() { new GenDomDocument() }
    
    public org.w3c.dom.DOMImplementation getDOMImplementation() {
		return GenDomImplementation;
	}
    
    public org.w3c.dom.Document parse(InputSource inputSource) throws SAXException, IOException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		DomBuilderHandler dbh = new DomBuilderHandler();
		sp.parse(inputSource,dbh);
		return dbh.document;
	}
    
}

final class DomBuilderHandler extends org.xml.sax.helpers.DefaultHandler {
	
	ADomDocument document;
	
	private ADomElement		cur_element;
	private DomText			cur_text;
	private DfltNsDomAttr	cur_dflt_namespace;
	private NsDomAttr[]		cur_namespaces;
	
	DomBuilderHandler() {}
	
	public void startDocument() {
		this.document = new GenDomDocument();
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		cur_text = null;
		//System.out.println("startElement("+uri+","+localName+","+qName+")");
		ADomElement elem;
		elem = (ADomElement)document.createElementNS(uri, qName);
		if (cur_dflt_namespace != null) {
			elem.setAttributeNode(cur_dflt_namespace);
			cur_dflt_namespace = null;
		}
		if (cur_namespaces != null) {
			foreach (NsDomAttr ns; cur_namespaces)
				elem.setAttributeNode(ns);
			cur_namespaces = null;
		}
		int nattrs = attributes.getLength();
		for (int i=0; i < nattrs; i++) {
			//System.out.println("attribute("+attributes.getURI(i)+","+attributes.getLocalName(i)+","+attributes.getQName(i)+")='"+attributes.getValue(i)+"'");
			String aUri = attributes.getURI(i);
			ADomAttr a;
			a = (ADomAttr)document.createAttributeNS(aUri,attributes.getQName(i));
			a.setValue(attributes.getValue(i));
			elem.setAttributeNodeNS(a);
			//System.out.println("val='"+a.getValue()+"'");
		}
		if (cur_element == null)
			document.element = elem;
		else
			cur_element.elements += elem;
		cur_element = elem;
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		cur_text = null;
		ANode p = cur_element.parent();
		if (p instanceof ADomElement)
			cur_element = (ADomElement)p;
		else
			cur_element = null;
	}
	
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		//System.out.println("xmlns:"+prefix+"="+uri);
		if (prefix.length() == 0) {
			cur_dflt_namespace = new DfltNsDomAttr(uri);
		} else {
			NsDomAttr attr = new NsDomAttr(prefix, uri);
			if (cur_namespaces == null)
				cur_namespaces = new NsDomAttr[]{attr};
			else
				cur_namespaces = (NsDomAttr[])Arrays.append(cur_namespaces, attr);
		}
	}
	
	public void endPrefixMapping(String prefix) throws SAXException {
		//System.out.println("end xmlns:"+prefix);
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			String str = new String(ch,start,length);
			if (str.trim().length() == 0)
				return;
			if (cur_element == null)
				throw new SAXException("text before element start: "+str);
			if (cur_text != null) {
				cur_text.appendData(str);
			} else {
				DomText txt = new DomText();
				txt.setData(str);
				//System.out.println("text=\'"+txt.getData()+"'");
				cur_text = txt;
				cur_element.elements += txt;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
