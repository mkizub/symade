package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.Kiev;
import kiev.dump.AcceptInfo;
import kiev.dump.UnMarshaller;
import kiev.dump.UnMarshallingContext;
import kiev.dump.AttributeSet;
import kiev.stdlib.Debug;
import kiev.vlang.Env;
import static kiev.stdlib.Asserts.*;

public abstract class AUnMarshallingContext implements XMLDumpReader, UnMarshallingContext {

	private static final boolean TRACE = false;
	
	public Object result;
	
	final Stack<UnMarshaller>   unmarshallers;
	//final Stack<Decoder>        decoders;
	final Stack<StateInfo>      states;

	static class StateInfo {
		Object node;
		UnMarshaller unmarshaller;
		QName attr;
		boolean implicit;
		StateInfo(Object node, UnMarshaller unmarshaller) {
			this.node = node;
			this.unmarshaller = unmarshaller;
		}
	}
	
	boolean expect_attr;
	Object attr_value;
	int ignore_count;

	AUnMarshallingContext() {
		this.unmarshallers = new Stack<UnMarshaller>();
		//this.decoders      = new Stack<Decoder>();
		this.states        = new Stack<StateInfo>();
	}
	
	public Object getResult() {
		return result;
	}

	public Object peekNode() {
		if (states.isEmpty())
			return null;
		return states.peek().node;
	}
	public QName  peekAttr() {
		if (states.isEmpty())
			return null;
		return states.peek().attr;
	}
	
	public void startDocument() {
		assert (result == null);
		assert (states.isEmpty());
		assert (attr_value == null);
	}
	
	public void endDocument() {
		// do nothing
		assert (result != null);
		assert (states.isEmpty());
	}

	public void startElement(QName qn, AttributeSet attrs) {
		if (ignore_count > 0) {
			if (TRACE) Debug.trace_force("startElement("+qn+") ignore count "+ignore_count);
			ignore_count++;
			return;
		}
		if (expect_attr) {
			// inside node, expecting attribute/field name 
			assert (!states.isEmpty()); // : "Expecting attribute entry, but stack of nodes is empty";
			assert (states.peek().attr == null); // : "Expecting attribute entry, but have unsaved attribute "+states.peek().attr;
			// at node scope, expect attribute(s)
			if (attr_value instanceof String) {
				// probably ignorable whitespace
				if (((String)attr_value).trim().length() != 0)
					Kiev.reportWarning("Mixing text '"+attr_value+"' and data: start of element "+qn);
				attr_value = null;
			}
			assert (attr_value == null); // : "Expecting attribute entry, but have unsaved data "+attr_value;
			StateInfo si = states.peek();
			AcceptInfo ai = si.unmarshaller.canAccept(si.node, qn, attrs, this);
			if (ai != null) {
				if (TRACE) Debug.trace_force("startElement("+qn+") expecting attr as field="+ai.as_field+" to field="+ai.to_field);
				if (ai.as_field) {
					si.attr = qn;
					expect_attr = false;
					return;
				} else {
					for (UnMarshaller unmarshaller : unmarshallers) {
						if (!unmarshaller.canUnMarshal(qn, attrs, this))
							continue;
						si.attr = ai.to_field;
						si.implicit = true;
						Object obj = unmarshaller.create(qn, attrs, this);
						states.push(new StateInfo(obj, unmarshaller));
						expect_attr = true;
						return;
					}
				}
			}
			Kiev.reportWarning("Cannot accept attribute '"+qn+"'");
			ignore_count = 1;
		} else {
			// at the document start or inside an attribute, expecting node data or text, got text element 
			if (attr_value instanceof String) {
				// probably ignorable whitespace
				if (((String)attr_value).trim().length() != 0)
					Kiev.reportWarning("Mixing text '"+attr_value+"' and data: start of element "+qn);
				attr_value = null;
			}
			if (attr_value != null) {
				if (TRACE) Debug.trace_force("startElement("+qn+") expecting value, found="+(attr_value==null?null:attr_value.getClass()));
				// multiple values in one attribute (probably a container attribute)
				assert (!states.isEmpty());
				assert (states.peek().attr != null); // : "Data in attribute entry, but no attribute name in stack";
				StateInfo si = states.peek();
				si.unmarshaller.accept(si.node, si.attr, attr_value, this);
				attr_value = null;
			}
			for (UnMarshaller unmarshaller : unmarshallers) {
				if (!unmarshaller.canUnMarshal(qn, attrs, this))
					continue;
				Object obj = unmarshaller.create(qn, attrs, this);
				if (TRACE) Debug.trace_force("startElement("+qn+") expecting value, pushing="+(obj==null?null:obj.getClass()));
				states.push(new StateInfo(obj, unmarshaller));
				expect_attr = true;
				return;
			}
			Kiev.reportWarning("Cannot unmarshal '"+qn+"'");
			ignore_count = 1;
		}
	}

	public void endElement(QName qn) {
		if (ignore_count > 0) {
			if (TRACE) Debug.trace_force("endElement("+qn+") ignore count "+ignore_count);
			ignore_count--;
			return;
		}
		if (expect_attr) {
			// inside node, expecting attribute/field name
			// thus this shell be the node end, save it to attr_value
			StateInfo si = states.pop();
			attr_value = si.unmarshaller.exit(si.node, this);
			if (TRACE) Debug.trace_force("endElement("+qn+") expecting attr, value="+(attr_value==null?null:attr_value.getClass()));
			if (states.isEmpty()) {
				result = attr_value;
				expect_attr = false;
			} else {
				assert (states.peek().attr != null);
				si = states.peek();
				if (si.implicit) {
					if (attr_value != null) {
						if (TRACE) Debug.trace_force("endElement("+qn+") implicit set");
						si.unmarshaller.accept(si.node, si.attr, attr_value, this);
						attr_value = null;
					}
					si.attr = null;
					si.implicit = false;
					expect_attr = true;
				} else {
					if (TRACE) Debug.trace_force("endElement("+qn+") explicit wait");
					expect_attr = false;
				}
			}
		} else {
			// inside attribute
			assert (!states.isEmpty());
			StateInfo si = states.peek();
			assert (qn.equals(si.attr));
			if (TRACE) Debug.trace_force("endElement("+qn+") expecting value, having="+(attr_value==null?null:attr_value.getClass()));
			if (attr_value != null) {
				si.unmarshaller.accept(si.node, si.attr, attr_value, this);
				attr_value = null;
			}
			si.attr = null;
			expect_attr = true;
		}
	}

	public void addText(String str) {
		if (ignore_count > 0 || expect_attr)
			return;
		if (attr_value == null)
			attr_value = str;
		else if (attr_value instanceof String)
			attr_value = ((String)attr_value) + str;
		else if (str.trim().length() > 0) {
			Kiev.reportWarning("Mixing text '"+str+"' and data: in element "+states.peek().attr);
		}
	}

}

