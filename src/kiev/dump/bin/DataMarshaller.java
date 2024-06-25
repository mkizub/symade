package kiev.dump.bin;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.types.Type;

public class DataMarshaller implements Marshaller {
    public boolean canMarshal(Object data, MarshallingContext context) {
		return true;
	}

    public void marshal(Object data, DumpWriter _out, MarshallingContext context) throws Exception {
    	BinDumpWriter out = (BinDumpWriter)_out;
    	if (data == null) {
    		out.writeNull();
    		return;
    	}
    	if (data instanceof String) {
    		String val = (String)data;
    		out.writeString(val);
    		return;
    	}
    	if (data instanceof Boolean) {
    		boolean val = ((Boolean)data).booleanValue();
    		if (val)
    			out.writeValueTag(Signature.TAG_TRUE);
    		else
    			out.writeValueTag(Signature.TAG_FALSE);
    		return;
    	}
    	if (data instanceof Character) {
    		char val = ((Character)data).charValue();
			out.writeValueTag(Signature.TAG_CHAR_8);
            if (val <= 0x7F) {
    			out.writeInt8(val);
            } else if (val <= 0x3FF) {
    			out.writeInt8(0xC0 | (val >> 6));
    			out.writeInt8(0x80 | (val & 0x3F));
            } else {
    			out.writeInt8(0xE0 | (val >> 12));
    			out.writeInt8(0x80 | ((val >> 6) & 0x3F));
    			out.writeInt8(0x80 | (val & 0x3F));
            }
    		return;
    	}
    	if (data instanceof Byte || data instanceof Short || data instanceof Integer || data instanceof Long) {
    		long val = ((Number)data).longValue();
    		if (val == 0L) {
    			out.writeValueTag(Signature.TAG_FALSE);
    			return;
    		}
    		if (val == 1L) {
    			out.writeValueTag(Signature.TAG_TRUE);
    			return;
    		}
    		if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE) {
    			out.writeValueTag(Signature.TAG_INT8);
    			out.writeInt8((int)val);
    			return;
    		}
    		if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE) {
    			out.writeValueTag(Signature.TAG_INT16);
    			out.writeInt16((int)val);
    			return;
    		}
    		if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
    			out.writeValueTag(Signature.TAG_INT32);
    			out.writeInt32((int)val);
    			return;
    		}
			out.writeValueTag(Signature.TAG_INT64);
			out.writeInt64(val);
    		return;
    	}
    	if (data instanceof Float || data instanceof Double) {
    		double val = ((Number)data).doubleValue();
    		if (val == ((int)val)) {
    			marshal(new Integer((int)val), out, context);
    			return;
    		}
			out.writeValueTag(Signature.TAG_FLOAT);
			out.writeInt64(Double.doubleToLongBits(val));
    		return;
    	}
    	if (data instanceof Enum) {
    		Enum val = (Enum)data;
    		ConstElem ce = ((DumpMarshallingContext)context).getConstElem(val);
    		out.writeConstRef(ce);
    		return;
    	}
    	if (data instanceof Type) {
    		String sig = ((Type)data).makeSignature(context.getEnv());
    		out.writeString(sig);
    		return;
    	}
		String val = String.valueOf(data);
		out.writeString(val);
		return;
	}
}

