package kiev.dump.xml;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vdom.*;
import javax.xml.namespace.QName;

public class XMLNodeExportMarshaller implements Marshaller {
	
	public XMLNodeExportMarshaller add(ExportTypeAlias ta) {
		throw new UnsupportedOperationException();
	}
	
    public boolean canMarshal(Object data, MarshallingContext context) {
		return (data instanceof XMLElement || data instanceof XMLText);
	}

    public void marshal(Object _data, DumpWriter _out, MarshallingContext context) throws Exception {
		XMLNode node = (XMLNode)_data;
    	XMLDumpWriter out = (XMLDumpWriter)_out; 
		
		if (node instanceof XMLText) {
			out.addText((String)node.getVal(node.getAttrSlot("text")));
			return;
		}
		
		if (node instanceof XMLElement) {
			QName qname = makeQName((XMLQName)node.getVal(node.getAttrSlot("name")));
			out.startElement(qname);
			for (XMLAttribute a : (XMLAttribute[])node.getVal(node.getAttrSlot("attributes"))) {
				XMLText xt = (XMLText)a.getVal(a.getAttrSlot("text"));
				out.addAttribute(makeQName((XMLQName)a.getVal(a.getAttrSlot("name"))), (String)xt.getVal(xt.getAttrSlot("text")));
			}
			for (XMLNode m : (XMLNode[])node.getVal(node.getAttrSlot("elements")))
				context.marshalData(m);
			out.endElement(qname);
		}
	}

	private static QName makeQName(XMLQName xqn) {
		String local = (String)xqn.getVal(xqn.getAttrSlot("local"));
		String uri = (String)xqn.getVal(xqn.getAttrSlot("uri"));
		String prefix = (String)xqn.getVal(xqn.getAttrSlot("prefix"));
		if (uri == null || uri == "")
			return new QName(local);
		if (prefix == null)
			prefix = "";
		return new QName(uri, local, prefix);
	}

}

