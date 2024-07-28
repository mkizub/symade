package kiev.dump.xml;

import kiev.dump.UnMarshaller;

public class ImportMarshallingContext extends AUnMarshallingContext {
	
	public ImportMarshallingContext(UnMarshaller um) {
		this.unmarshallers.push(um);
	}
	public ImportMarshallingContext(UnMarshaller... unmarshallers) {
		for (UnMarshaller um : unmarshallers)
			this.unmarshallers.push(um);
	}
}
