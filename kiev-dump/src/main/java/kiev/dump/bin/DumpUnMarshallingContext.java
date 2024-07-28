package kiev.dump.bin;

import javax.xml.namespace.QName;

import kiev.dump.UnMarshallingContext;
import kiev.vlang.Env;
import kiev.vtree.INode;

public class DumpUnMarshallingContext implements UnMarshallingContext {

	private final Env env;
	
	public INode result;
	
	public DumpUnMarshallingContext(Env env) {
		this.env = env;
	}
	
	public Env getEnv() {
		return env;
	}

	public Object getResult() {
		return result;
	}

	public QName peekAttr() {
		throw new UnsupportedOperationException();
	}

	public Object peekNode() {
		throw new UnsupportedOperationException();
	}

}
