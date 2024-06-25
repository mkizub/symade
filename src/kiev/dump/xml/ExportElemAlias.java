package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.Marshaller;
import kiev.vtree.INode;

public class ExportElemAlias extends ExportFieldAlias {
	private final String slot;
	private final QName  name;
	private final Marshaller marshaller;
	public ExportElemAlias(String slot, String name) {
		this.slot = slot;
		this.name = new QName(name);
		this.marshaller = null;
	}
	public ExportElemAlias(String slot, String name, Marshaller marshaller) {
		this.slot = slot;
		this.name = new QName(name);
		this.marshaller = marshaller;
	}
	public QName getQName() { return name; }
	public boolean asAttribute() { return false; }
	public Object getData(Object data) {
		return ((INode)data).getVal(((INode)data).getAttrSlot(slot));
	}
	public Marshaller getMarshaller() { return marshaller; }
}

