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

import javax.xml.stream.*;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import kiev.dump.DumpWriter;
import kiev.stdlib.Arrays;

public interface XMLDumpWriter extends DumpWriter {
	public void startDocument() throws Exception ;
	public void endDocument() throws Exception ;
	public void startElement(QName qn) throws Exception ;
	public void endElement(QName qn) throws Exception ;
	public void addAttribute(QName qn, String value) throws Exception ;
	public void addText(String value) throws Exception ;
	public void addComment(String value, boolean nl) throws Exception ;
}

// final class PullXMLDumpWriter implements XMLDumpWriter {
//
// 	//private org.xmlpull.mxp1_serializer.MXSerializer out;
// 	private XMLNamespaceMap nsmap;
// 	private boolean atStart;
//
//
// 	PullXMLDumpWriter(OutputStream out_stream, XMLNamespaceMap nsmap) throws IOException {
// 		this.out = new org.xmlpull.mxp1_serializer.MXSerializer();
// 		this.nsmap = nsmap;
// 		this.out.setFeature("http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe", true);
// 		this.out.setFeature("http://xmlpull.org/v1/doc/features.html#names-interned", false);
// 		this.out.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", " ");
// 		this.out.setOutput(out_stream, "UTF-8");
// 	}
//
// 	PullXMLDumpWriter(Writer out_writer, XMLNamespaceMap nsmap) {
// 		this.out = new org.xmlpull.mxp1_serializer.MXSerializer();
// 		this.nsmap = nsmap;
// 		this.out.setFeature("http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe", true);
// 		this.out.setFeature("http://xmlpull.org/v1/doc/features.html#names-interned", false);
// 		this.out.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", " ");
// 		this.out.setOutput(out_writer);
// 	}
//
// 	public void startDocument() throws IOException {
// 		out.startDocument("1.0", "UTF-8", Boolean.TRUE);
// 		atStart = true;
// 	}
//
// 	public void endDocument() throws IOException {
// 		atStart = false;
// 		out.ignorableWhitespace("\n");
// 		out.endDocument();
// 		out.flush();
// 		out.getWriter().close();
// 	}
//
// 	public void startElement(QName qn) throws IOException {
// 		if (atStart) {
// 			atStart = false;
// 			for (String prefix : nsmap.getAllPrefixes())
// 				out.setPrefix(prefix, nsmap.prefix2uri(prefix));
// 		}
// 		out.startTag(qn.getNamespaceURI(), qn.getLocalPart());
// 	}
//
// 	public void endElement(QName qn) throws IOException {
// 		out.endTag(qn.getNamespaceURI(), qn.getLocalPart());
// 	}
//
// 	public void addAttribute(QName qn, String value) throws IOException {
// 		out.attribute(qn.getNamespaceURI(), qn.getLocalPart(), value);
// 	}
//
// 	public void addText(String value) throws IOException {
// 		out.text(value);
// 	}
//
// 	public void addComment(String comment, boolean nl) throws IOException {
// 		if (nl)
// 			out.ignorableWhitespace("\n");
// 		out.comment(comment);
// 		if (nl)
// 			out.ignorableWhitespace("\n");
// 	}
// }

final class StreamXMLDumpWriter implements XMLDumpWriter {

	static class PendingElement {
		QName qn;
		QName[] attrs;
		String[] values;
		PendingElement(QName qn) {
			this.qn = qn;
		}
		void addAttr(QName attr, String val) {
			if (attrs == null) {
				attrs = new QName[]{attr};
				values = new String[]{val};
			} else {
				attrs = (QName[])Arrays.append(attrs,attr);
				values = (String[])Arrays.append(values,val);
			}
		}
	}

	private XMLStreamWriter out;
	private XMLNamespaceMap nsmap;
	private boolean atStart;
	private boolean hasText;
	private int depth;
	private PendingElement pendingElement;

	StreamXMLDumpWriter(OutputStream out_stream, XMLNamespaceMap nsmap) throws XMLStreamException {
		XMLOutputFactory outf = XMLOutputFactory.newInstance();
		this.out = outf.createXMLStreamWriter(out_stream, "UTF-8");
		this.nsmap = nsmap;
	}

	StreamXMLDumpWriter(Writer out_writer, XMLNamespaceMap nsmap) throws XMLStreamException {
		XMLOutputFactory outf = XMLOutputFactory.newInstance();
		this.out = outf.createXMLStreamWriter(out_writer);
		this.nsmap = nsmap;
	}

	private void writePendingElement(boolean empty) throws XMLStreamException {
		if (pendingElement == null)
			return;
		QName qn = pendingElement.qn;
		if (empty) {
			if (qn.getNamespaceURI() != null && qn.getPrefix() != null)
				out.writeEmptyElement(qn.getPrefix(), qn.getLocalPart(), qn.getNamespaceURI());
			else if (qn.getNamespaceURI() != null)
				out.writeEmptyElement(qn.getNamespaceURI(), qn.getLocalPart());
			else
				out.writeEmptyElement(qn.getLocalPart());
		} else {
			if (qn.getNamespaceURI() != null && qn.getPrefix() != null)
				out.writeStartElement(qn.getPrefix(), qn.getLocalPart(), qn.getNamespaceURI());
			else if (qn.getNamespaceURI() != null)
				out.writeStartElement(qn.getNamespaceURI(), qn.getLocalPart());
			else
				out.writeStartElement(qn.getLocalPart());
		}
		if (atStart) {
			atStart = false;
			for (String prefix : nsmap.getAllPrefixes())
				out.writeNamespace(prefix, nsmap.prefix2uri(prefix));
		}
		if (pendingElement.attrs != null) {
			for (int i=0; i < pendingElement.attrs.length; i++) {
				qn = pendingElement.attrs[i];
				String value = pendingElement.values[i];
				if (qn.getNamespaceURI() != null && qn.getPrefix() != null)
					out.writeAttribute(qn.getPrefix(), qn.getNamespaceURI(), qn.getLocalPart(), value);
				else if (qn.getNamespaceURI() != null)
					out.writeAttribute(qn.getNamespaceURI(), qn.getLocalPart(), value);
				else
					out.writeAttribute(qn.getLocalPart(), value);
			}
		}
		pendingElement = null;
	}

	private void writeXMLIndent(int indent) throws XMLStreamException {
		if (hasText)
			return;
		out.writeCharacters("\n");
		for (int i=0; i < indent; i++)
			out.writeCharacters(" ");
	}

	public void startDocument() throws XMLStreamException {
		//out.writeStartDocument("UTF-8", "1.0");
		out.writeProcessingInstruction("xml", "version='1.0' encoding='UTF-8' standalone='yes'");
		out.writeCharacters("\n");
		atStart = true;
		for (String prefix : nsmap.getAllPrefixes())
			out.setPrefix(prefix, nsmap.prefix2uri(prefix));
	}

	public void endDocument() throws XMLStreamException {
		atStart = false;
		out.writeCharacters("\n");
		out.writeEndDocument();
		out.flush();
		out.close();
	}

	public void startElement(QName qn) throws XMLStreamException {
		writePendingElement(false);
		writeXMLIndent(depth);
		pendingElement = new PendingElement(qn);
		hasText = false;
		depth++;
	}

	public void endElement(QName qn) throws XMLStreamException {
		depth--;
		if (pendingElement != null) {
			writePendingElement(true);
		} else {
			if (!hasText)
				writeXMLIndent(depth);
			out.writeEndElement();
		}
		hasText = false;
	}

	public void addAttribute(QName qn, String value) throws XMLStreamException {
		pendingElement.addAttr(qn,value);
	}

	public void addText(String value) throws XMLStreamException {
		writePendingElement(false);
		hasText = true;
		out.writeCharacters(value);
	}

	public void addComment(String comment, boolean nl) throws XMLStreamException {
		writePendingElement(false);
		if (nl)
			out.writeCharacters("\n");
		out.writeComment(comment);
		if (nl)
			out.writeCharacters("\n");
	}
}
