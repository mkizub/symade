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

import java.util.Enumeration;
import javax.xml.namespace.QName;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.stdlib.TypeInfoInterface;
import kiev.vtree.*;
import kiev.vlang.*;

public class ANodeDumpMarshaller implements Marshaller {
	public static final String SOP_URI = "sop://sop/";
	
	public ANodeDumpMarshaller() {}
	
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof INode;
	}

    public void marshal(Object data, DumpWriter _out, MarshallingContext _context) throws Exception {
    	XMLDumpWriter out = (XMLDumpWriter)_out;
    	DumpMarshallingContext context = (DumpMarshallingContext)_context;
		INode node = (INode)data;
		Language lng = node.asANode().getCompilerLang();

		QName qnElem;
		if (lng != null)
			qnElem = new QName(lng.getURI(),node.asANode().getCompilerNodeName(),lng.getName());
		else
			qnElem = new QName(SOP_URI,node.getClass().getName(),"sop");
		out.startElement(qnElem);
		
		if (node instanceof TypeInfoInterface && ((TypeInfoInterface)node).getTypeInfoField().getTopArgs().length > 0)
			out.addAttribute(new QName(SOP_URI, "ti", "sop"), ((TypeInfoInterface)node).getTypeInfoField().toString());

		if (node instanceof DNode && !((DNode)node).isInterfaceOnly()) {
			SymUUID suuid = ((Symbol)node.getVal(node.getAttrSlot("symbol"))).getUUID(context.getEnv());
			if (suuid != SymUUID.Empty)
				context.attributeData(new QName("uuid"), suuid.toString());
		}
		if (node instanceof Symbol && ((Symbol)node).suuid() != null) {
			SymUUID suuid = ((Symbol)node).suuid();
			if (suuid != SymUUID.Empty)
				context.attributeData(new QName("uuid"), suuid.toString());
		}
		
		boolean popDumpMode = false;
		if (node instanceof DNode && ((DNode)node).isMacro() || node instanceof TypeDecl && ((TypeDecl)node).isMixin()) {
			context.pushDumpMode("full");
			popDumpMode = true;
		}
		
		for (AttrSlot attr : node.values()) {
			if (!(attr instanceof ScalarAttrSlot && attr.isXmlAttr()))
				continue;
			if (context.filter.ignoreAttr(node, attr))
				continue;
			Object obj = ((ScalarAttrSlot)attr).get(node);
			if (obj == null)
				continue;
			if (attr.isSymRef() && node instanceof SymbolRef)
				context.attributeData(new QName(attr.getXmlLocalName()), ((SymbolRef)node).makeSignature(context.getEnv(),"api".equals(context.getDumpMode())));
			else
				context.attributeData(new QName(attr.getXmlLocalName()), obj);
		}
		for (AttrSlot attr : node.values()) {
			if (attr.isXmlAttr() || attr.isXmlIgnore())
				continue;
			if (context.filter.ignoreAttr(node, attr))
				continue;
			QName qnAttr = new QName(attr.getXmlLocalName());
			out.startElement(qnAttr);
			if (attr instanceof ASpaceAttrSlot) {
				Enumeration en = ((ASpaceAttrSlot)attr).iterate(node);
				while (en.hasMoreElements()) {
					INode n = (INode)en.nextElement();
					if (!context.filter.ignoreNode(node, attr, n))
						context.marshalData(n);
				}
			}
			else if (attr instanceof ScalarAttrSlot) {
				Object obj = ((ScalarAttrSlot)attr).get(node);
				if (obj == null)
					continue;
				context.marshalData(obj);
			}
			out.endElement(qnAttr);
		}
		//ExtSpaceIterator en = node.asANode().getExtSpaceIterator(null);
		//while (en.hasMoreElements()) {
		//	AttrSlot attr = en.nextAttrSlot();
		//	INode n = en.nextElement();
		//	if (!attr.isAttr() || !attr.isExternal())
		//		continue;
		//	if (attr.getCompilerLang() == null)
		//		continue;
		//	if (context.filter.ignoreAttr(node, attr))
		//		continue;
		//	QName qnAttr = new QName(attr.getXmlNamespaceURI(), attr.getXmlLocalName());
		//	out.startElement(qnAttr);
		//	context.marshalData(n);
		//	out.endElement(qnAttr);
		//}

		if (popDumpMode)
			context.popDumpMode();

		out.endElement(qnElem);
	}
}

