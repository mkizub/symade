package kiev.dump.xml;

import kiev.vtree.LoggingBuilderFactory;
import kiev.dump.UnMarshaller;

public class ImportMarshallingContext extends AUnMarshallingContext {

	public ImportMarshallingContext(LoggingBuilderFactory logger, UnMarshaller um) {
		super(logger);
		this.unmarshallers.push(um);
	}
	public ImportMarshallingContext(LoggingBuilderFactory logger, UnMarshaller... unmarshallers) {
		super(logger);
		for (UnMarshaller um : unmarshallers)
			this.unmarshallers.push(um);
	}
}
