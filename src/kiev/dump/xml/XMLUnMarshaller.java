package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.AcceptInfo;
import kiev.dump.UnMarshaller;
import kiev.dump.UnMarshallingContext;
import kiev.dump.AttributeSet;

import kiev.vdom.*;

public class XMLUnMarshaller implements UnMarshaller {

	public boolean canUnMarshal(QName qname, AttributeSet attrs, UnMarshallingContext context) {
		return true;
	}

	public AcceptInfo canAccept(Object self, QName qname, AttributeSet attrs, UnMarshallingContext context) {
		return new AcceptInfo(false, qname);
	}

	public Object exit(Object self, UnMarshallingContext context) {
		return self;
	}

	public Object create(QName qname, AttributeSet attributes, UnMarshallingContext context) {
		String prefix = qname.getPrefix();
		if (prefix != null && prefix.length() > 0)
			prefix = qname.getPrefix();
		XMLElement el = new XMLElement(new XMLQName(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix()));
		addAttributes(el, attributes);
		return el;
	}

	public void accept(Object self, QName qname, Object target, UnMarshallingContext context) {
		XMLElement el = (XMLElement)self;
		if (target instanceof String) {
			String text = (String)target;
			if (text.trim().length() == 0)
				return;
			target = new XMLText((String)target);
		}
		XMLNode node = (XMLNode)target;
		el.addVal(el.getAttrSlot("elements"), node);
	}

	private void addAttributes(XMLElement el, AttributeSet attributes) {
		int n = attributes.getCount();
		for (int i = 0; i < n; i++)
			writeAttribute(el, new QName(attributes.getURI(i), attributes.getName(i), attributes.getPrefix(i)), attributes.getValue(i));
	}

	private void writeAttribute(XMLElement el, QName qname, String value) {
		XMLAttribute attr = new XMLAttribute(new XMLQName(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix()), new XMLText(value));
		el.addVal(el.getAttrSlot("attributes"), attr);
	}
}
