package kiev.dump.xml;

import kiev.dump.Convertor;
import kiev.dump.MarshallingContext;
import kiev.vlang.types.Type;
import kiev.vlang.types.TypeRef;

public class ArrayConvertor implements Convertor {

	private final String prefix;
	private final String separator;
	private final String sufix;
	private final String empty;

	public ArrayConvertor(String prefix, String separator, String sufix, String empty) {
		this.prefix = prefix;
		this.separator = separator;
		this.sufix = sufix;
		this.empty = empty;
	}

    public boolean canConvert(Object data) {
		return data instanceof Object[];
	}

    public String convert(Object data, MarshallingContext context) {
		Object[] arr = (Object[])data;
		StringBuffer sb = new StringBuffer();
		if (prefix != null) sb.append(prefix);
		boolean emp = true;
		for (int i=0; i < arr.length; i++) {
			Object obj = arr[i];
			if (obj == null)
				continue;
			String text = context.convertData(obj);
			if (text == null)
				continue;
			if (!emp && separator != null)
				sb.append(separator);
			else
				emp = false;
			sb.append(text);
		}
		if (sufix != null) sb.append(sufix);
		if (emp)
			return empty;
		return sb.toString();
	}
}

