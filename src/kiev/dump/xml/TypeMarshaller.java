package kiev.dump.xml;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.types.Type;

public class TypeMarshaller implements Marshaller {
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof Type;
	}

    public void marshal(Object data, DumpWriter _out, MarshallingContext context) throws Exception {
    	XMLDumpWriter out = (XMLDumpWriter)_out; 
		out.addText(((Type)data).makeSignature(context.getEnv()));
	}
}
