package kiev.dump.xml;

import kiev.dump.Marshaller;
import kiev.vtree.INode;

public class ExportInlineAlias extends ExportFieldAlias {
	private final String slot;
	private final Marshaller marshaller;
	public ExportInlineAlias(String slot) {
		this.slot = slot;
		this.marshaller = null;
	}
	public ExportInlineAlias(String slot, Marshaller marshaller) {
		this.slot = slot;
		this.marshaller = marshaller;
	}
	public Object getData(Object data) {
		return ((INode)data).getVal(((INode)data).getAttrSlot(slot));
	}
	public Marshaller getMarshaller() { return marshaller; }
}

