package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.Kiev;
import kiev.dump.AcceptInfo;
import kiev.dump.AttributeSet;
import kiev.dump.UnMarshaller;
import kiev.dump.UnMarshallingContext;
import kiev.dump.xml.AUnMarshallingContext.StateInfo;
import kiev.stdlib.Debug;
import kiev.vdom.XMLElement;
import kiev.vlang.Env;

import static kiev.stdlib.Asserts.*;

public class XMLUnMarshallingContext implements XMLDumpReader, UnMarshallingContext {
	
	final Env env;
	final UnMarshaller unmarshaller;
	
	public Object result;
	Stack<XMLElement> elements;
	Object attr_value;
	
	public XMLUnMarshallingContext(Env env) {
		this.env = env;
		this.unmarshaller = new XMLUnMarshaller();
		this.elements = new Stack<XMLElement>();
	}
	
	public Env getEnv() {
		return env;
	}
	public Object getResult() {
		return result;
	}
	
	public Object peekNode() {
		if (elements.isEmpty())
			return null;
		return elements.peek();
	}
	public QName  peekAttr() {
		return null;
	}

	public void startDocument() {
		assert (result == null);
		assert (elements.isEmpty());
		assert (attr_value == null);
	}
	
	public void endDocument() {
		// do nothing
		assert (result != null);
		assert (elements.isEmpty());
	}

	public void startElement(QName qn, AttributeSet attrs) {
		// at the document start or inside an attribute, expecting node data or text, got text element 
		if (attr_value != null) {
			unmarshaller.accept(elements.peek(), null, attr_value, this);
			attr_value = null;
		}
		XMLElement el = (XMLElement)unmarshaller.create(qn, attrs, this);
		elements.push(el);
	}

	public void endElement(QName qn) {
		if (attr_value != null) {
			unmarshaller.accept(elements.peek(), null, attr_value, this);
			attr_value = null;
		}
		attr_value = unmarshaller.exit(elements.pop(), this);
		if (elements.isEmpty())
			result = attr_value;
		else
			unmarshaller.accept(elements.peek(), null, attr_value, this);
		attr_value = null;
	}

	public void addText(String str) {
		if (attr_value == null)
			attr_value = str;
		else if (attr_value instanceof String)
			attr_value = ((String)attr_value) + str;
		else {
			if (!elements.isEmpty())
				unmarshaller.accept(elements.peek(), null, attr_value, this);
			attr_value = str;
		}
	}

}
