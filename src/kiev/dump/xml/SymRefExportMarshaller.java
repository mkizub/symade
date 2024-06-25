package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vtree.SymbolRef;

public class SymRefExportMarshaller implements Marshaller {
	
	private final QName qname;
	private final QName aname;
	
	public SymRefExportMarshaller(String elname, String sign_name) {
		this.qname = new QName(elname);
		this.aname = new QName(sign_name);
	}
	
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof SymbolRef;
	}

    public void marshal(Object data, DumpWriter _out, MarshallingContext context) throws Exception {
    	XMLDumpWriter out = (XMLDumpWriter)_out; 
		out.startElement(qname);
		out.addAttribute(aname, ((SymbolRef)data).makeSignature(context.getEnv()));
		out.endElement(qname);
	}
}
