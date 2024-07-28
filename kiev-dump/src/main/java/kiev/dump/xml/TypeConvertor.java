package kiev.dump.xml;

import kiev.dump.Convertor;
import kiev.dump.MarshallingContext;
import kiev.vlang.types.Type;
import kiev.vlang.types.TypeRef;

public class TypeConvertor implements Convertor {
    public boolean canConvert(Object data) {
		return data instanceof Type || data instanceof TypeRef;
	}

    public String convert(Object data, MarshallingContext context) {
		if (data instanceof Type)
			return ((Type)data).makeSignature(context.getEnv());
		return ((TypeRef)data).getType(context.getEnv()).makeSignature(context.getEnv());
	}
}

