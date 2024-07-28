package kiev.dump.xml;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.DNode;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;

public class SymRefExpandExportMarshaller implements Marshaller {
	
	public SymRefExpandExportMarshaller() {}
	
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof SymbolRef;
	}

    public void marshal(Object data, DumpWriter out, MarshallingContext context) throws Exception {
		SymbolRef node = (SymbolRef)data;
		Symbol symbol = node.getTargetSymbol();
		if (symbol != null) {
			DNode dnode =symbol.getTargetDNode();
			if (dnode != null)
				context.marshalData(dnode);
		}
	}
}

