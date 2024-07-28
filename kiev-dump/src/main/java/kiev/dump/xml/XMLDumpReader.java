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
package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.AttributeSet;

// import org.xmlpull.v1.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public interface XMLDumpReader {
	public void startDocument();
	public void endDocument();
	public void startElement(QName qn, AttributeSet attrs);
	public void endElement(QName qn);
	public void addText(String value);
}

// final class PullHandler {
// 	XMLDumpReader deserializer;
//
// 	PullHandler(XMLDumpReader deserializer) {
// 		this.deserializer = deserializer;
// 	}
//
// 	public void processDocument(final XmlPullParser xpp) throws Exception
// 	{
// 		AttributeSet attrs = new AttributeSet() {
// 			public int getCount() { return xpp.getAttributeCount(); }
// 			public String getURI(int i) { return xpp.getAttributeNamespace(i); }
// 			public String getPrefix(int i) { return xpp.getAttributePrefix(i); }
// 			public String getName(int i) { return xpp.getAttributeName(i); }
// 			public String getValue(int i) { return xpp.getAttributeValue(i); }
// 		};
// 		int eventType = xpp.getEventType();
// 		for (;;) {
// 			if(eventType == XmlPullParser.START_DOCUMENT)
// 				deserializer.startDocument();
// 			else if(eventType == XmlPullParser.END_DOCUMENT) {
// 				deserializer.endDocument();
// 				return;
// 			}
// 			else if(eventType == XmlPullParser.START_TAG) {
// 				String uri = xpp.getNamespace();
// 				String name = xpp.getName();
// 				String prefix = xpp.getPrefix();
// 				if (prefix == null)
// 					deserializer.startElement(new QName(uri, name), attrs);
// 				else
// 					deserializer.startElement(new QName(uri, name, prefix), attrs);
// 			}
// 			else if(eventType == XmlPullParser.END_TAG) {
// 				String uri = xpp.getNamespace();
// 				String name = xpp.getName();
// 				String prefix = xpp.getPrefix();
// 				if (prefix == null)
// 					deserializer.endElement(new QName(uri, name));
// 				else
// 					deserializer.endElement(new QName(uri, name, prefix));
// 			}
// 			else if(eventType == XmlPullParser.TEXT)
// 				deserializer.addText(xpp.getText());
// 			eventType = xpp.next();
// 		}
// 	}
// }

final class SAXHandler extends DefaultHandler {

	final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
	final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
	static class NsMap {
		final String prefix;
		final String uri;
		public NsMap(String prefix, String uri) {
			this.prefix = prefix;
			this.uri = uri;
		}
	}

	XMLDumpReader deserializer;
	private String[] ns_prefix;
	private String[] ns_uri;
	private int ns_size;

	SAXHandler(XMLDumpReader deserializer) {
		this.deserializer = deserializer;
		this.ns_prefix = new String[64];
		this.ns_uri = new String[64];
	}

	public void startDocument()
		throws SAXException
	{
		deserializer.startDocument();
	}

	public void endDocument()
		throws SAXException
	{
		deserializer.endDocument();
	}

	public void startElement(String uri, String sName, String qName, final Attributes attributes)
		throws SAXException
	{
		QName qn;
		if (uri == null || uri == "") {
			qn = new QName(sName);
		} else {
			int p = qName.indexOf(':');
			if (p >= 0)
				qn = new QName(uri, sName, qName.substring(0,p));
			else
				qn = new QName(uri, sName, "");
		}
		deserializer.startElement(qn, new AttributeSet() {
			public int getCount() { return ns_size + attributes.getLength(); }
			public String getURI(int i) { return i < ns_size ? XMLNS_URI : attributes.getURI(i-ns_size); }
			public String getName(int i) { return i < ns_size ? ns_prefix[i] : attributes.getLocalName(i-ns_size); }
			public String getValue(int i) { return i < ns_size ? ns_uri[i] : attributes.getValue(i-ns_size); }
			public String getPrefix(int i) {
				if (i < ns_size)
					return "xmlns";
				String qname = attributes.getQName(i-ns_size);
				int p = qname.indexOf(':');
				if (p < 0)
					return "";
				return qname.substring(0,p);
			}
		});
		ns_size = 0;
	}
	public void endElement(String uri, String sName, String qName)
		throws SAXException
	{
		QName qn;
		if (uri == null || uri == "")
			qn = new QName(sName);
		else
			qn = new QName(uri, sName, qName.substring(qName.indexOf(':')+1));
		deserializer.endElement(qn);
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		ns_prefix[ns_size] = prefix;
		ns_uri[ns_size] = uri;
		ns_size += 1;
	}

	public void characters(char[] ch, int start, int length) {
		deserializer.addText(new String(ch, start, length));
	}

}


