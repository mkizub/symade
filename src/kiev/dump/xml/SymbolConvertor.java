package kiev.dump.xml;

import kiev.dump.Convertor;
import kiev.dump.MarshallingContext;
import kiev.vtree.Symbol;

public class SymbolConvertor implements Convertor {
    public boolean canConvert(Object data) {
		return data != null && data.getClass() == Symbol.class;
	}

    public String convert(Object data, MarshallingContext context) {
		return ((Symbol)data).makeSignature(context.getEnv());
	}
}

