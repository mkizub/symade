package kiev.dump.xml;

import kiev.dump.Convertor;
import kiev.dump.MarshallingContext;
import kiev.vtree.SymbolRef;

public class SymRefConvertor implements Convertor {
    public boolean canConvert(Object data) {
		return data instanceof SymbolRef;
	}

    public String convert(Object data, MarshallingContext context) {
		return ((SymbolRef)data).makeSignature(context.getEnv());
	}
}
