package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.Convertor;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vlang.Env;

public abstract class AMarshallingContext implements MarshallingContext {
	final Env                           env;
	final XMLDumpWriter       out;
	final Stack<Marshaller>   marshallers;
	final Stack<Convertor>    convertors;
	
	AMarshallingContext(Env env, XMLDumpWriter out) {
		this.env         = env;
		this.out         = out;
		this.marshallers = new Stack<Marshaller>();
		this.convertors  = new Stack<Convertor>();
	}
	
	public Env getEnv() {
		return env;
	}
	
	public MarshallingContext add(Marshaller m) {
		marshallers.push(m);
		return this;
	}

	public MarshallingContext add(Convertor c) {
		convertors.push(c);
		return this;
	}

    public void marshalData(Object data) {
    	try {
			for (Marshaller marshaller : marshallers) {
				if (marshaller.canMarshal(data, this)) {
					marshaller.marshal(data, out, this);
					return;
				}
			}
    	} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
    		throw new RuntimeException("Marshalling exception", e);
    	}
		throw new RuntimeException("Marshalling exception");
	}
	
	public void marshalData(Object data, Marshaller marshaller) {
		if (marshaller == null) {
			this.marshalData(data);
		} else {
	    	try {
				if (marshaller.canMarshal(data, this)) {
					marshaller.marshal(data, out, this);
					return;
				}
	    	} catch (Exception e) {
				if (e instanceof RuntimeException) throw (RuntimeException)e;
	    		throw new RuntimeException("Marshalling exception", e);
	    	}
			throw new RuntimeException("Marshalling exception");
		}
	}
	
	public String convertData(Object data) {
		try {
			for (Convertor convertor : convertors) {
				if (convertor.canConvert(data))
					return convertor.convert(data, this);
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException("Conversion exception", e);
		}
		throw new RuntimeException("Conversion exception");
	}
	
	public void attributeData(QName attr, Object data) {
		try {
			for (Convertor convertor : convertors) {
				if (convertor.canConvert(data)) {
					String text = convertor.convert(data, this);
					if (text != null)
						out.addAttribute(attr, text);
					return;
				}
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException("Conversion exception", e);
		}
		throw new RuntimeException("Conversion exception");
	}
	
	public void attributeData(QName attr, Object data, Convertor convertor) {
		if (convertor == null) {
			this.attributeData(attr, data);
		} else {
			try {
				if (convertor.canConvert(data)) {
					String text = convertor.convert(data, this);
					if (text != null)
						out.addAttribute(attr, text);
					return;
				}
			} catch (Exception e) {
				if (e instanceof RuntimeException) throw (RuntimeException)e;
				throw new RuntimeException("Conversion exception", e);
			}
			throw new RuntimeException("Conversion exception");
		}
	}
}

