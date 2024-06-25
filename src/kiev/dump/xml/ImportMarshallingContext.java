package kiev.dump.xml;

import kiev.dump.UnMarshaller;
import kiev.vlang.Env;

public class ImportMarshallingContext extends AUnMarshallingContext {
	
	public ImportMarshallingContext(Env env, UnMarshaller um) {
		super(env);
		this.unmarshallers.push(um);
	}
	public ImportMarshallingContext(Env env, UnMarshaller... unmarshallers) {
		super(env);
		for (UnMarshaller um : unmarshallers)
			this.unmarshallers.push(um);
	}
}
