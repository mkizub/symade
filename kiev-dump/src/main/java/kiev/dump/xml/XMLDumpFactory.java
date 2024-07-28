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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.xml.parsers.*;
// import org.xmlpull.v1.*;

public final class XMLDumpFactory {

	private XMLDumpFactory() {}

	public static XMLNamespaceMap getNamespaceMap() {
		return new NamespaceMapImpl();
	}

	public static XMLDumpWriter getWriter(OutputStream out_stream, XMLNamespaceMap nsmap) {
// 		try {
// 			return new PullXMLDumpWriter(out_stream, nsmap);
// 		} catch (Throwable e) {}
		try {
			return new StreamXMLDumpWriter(out_stream, nsmap);
		} catch (Throwable e) {}
		throw new Error("No XML writer found");
	}

	public static XMLDumpWriter getWriter(Writer out_writer, XMLNamespaceMap nsmap) {
// 		try {
// 			return new PullXMLDumpWriter(out_writer, nsmap);
// 		} catch (Throwable e) {}
		try {
			return new StreamXMLDumpWriter(out_writer, nsmap);
		} catch (Throwable e) {}
		throw new Error("No XML writer found");
	}

	public static void parse(InputStream in_stream, XMLDumpReader reader) throws Exception {
// 		try {
// 			XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
// 			factory.setNamespaceAware(true);
// 			XmlPullParser xpp = factory.newPullParser();
// 			xpp.setInput(in_stream, "UTF-8");
// 			xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
// 			xpp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, true);
// 			new PullHandler(reader).processDocument(xpp);
// 			return;
// 		} catch (Throwable e) {}
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(in_stream, new SAXHandler(reader));
			return;
		} catch (NoClassDefFoundError e) {}
		throw new Error("No XML parser found");
	}

	public static void parse(Reader in_reader, XMLDumpReader reader) throws Exception {
// 		try {
// 			XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
// 			factory.setNamespaceAware(true);
// 			XmlPullParser xpp = factory.newPullParser();
// 			xpp.setInput(in_reader);
// 			xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
// 			xpp.setFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES, true);
// 			new PullHandler(reader).processDocument(xpp);
// 			return;
// 		} catch (Throwable e) {}
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(true);
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new org.xml.sax.InputSource(in_reader), new SAXHandler(reader));
			return;
		} catch (NoClassDefFoundError e) {}
		throw new Error("No XML parser found");
	}
}

class NamespaceMapImpl implements XMLNamespaceMap {
	private String[] prfs = new String[16];
	private String[] uris = new String[16];

	public void add(String prefix, String uri) {
		uri = uri.intern();
		prefix = prefix.intern();
		String[] prfs = this.prfs;
		String[] uris = this.uris;
		int len = prfs.length;
		int ins = -1;
		for (int i=0; i < len; i++) {
			if (uris[i] == uri && prfs[i] == prefix)
				return;
			if (ins < 0 && uris[i] == null)
				ins = i;
		}
		if (ins < 0) {
			String[] tmp = new String[prfs.length+16];
			for (int i=0; i < len; i++)
				tmp[i] = prfs[i];
			this.prfs = prfs = tmp;
			tmp = new String[prfs.length+16];
			for (int i=0; i < len; i++)
				tmp[i] = uris[i];
			this.uris = uris = tmp;
			ins = len;
		}
		prfs[ins] = prefix;
		uris[ins] = uri;
	}

	public void remove(String prefix, String uri) {
	}

	public String uri2prefix(String uri) {
		uri = uri.intern();
		String[] prfs = this.prfs;
		String[] uris = this.uris;
		int len = prfs.length;
		for (int i=0; i < len; i++) {
			if (uris[i] == uri)
				return prfs[i];
		}
		return null;
	}
	public String prefix2uri(String prefix) {
		prefix = prefix.intern();
		String[] prfs = this.prfs;
		String[] uris = this.uris;
		int len = prfs.length;
		for (int i=0; i < len; i++) {
			if (prfs[i] == prefix)
				return uris[i];
		}
		return null;
	}
	public String[] getAllPrefixes() {
		String[] prfs = this.prfs;
		int len = prfs.length;
		int cnt = 0;
		for (int i=0; i < len; i++) {
			if (prfs[i] != null)
				cnt++;
		}
		String[] res = new String[cnt];
		cnt = 0;
		for (int i=0; i < len; i++) {
			if (prfs[i] != null)
				res[cnt++] = prfs[i];
		}
		return res;
	}
}

