package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.Convertor;
import kiev.vtree.INode;

public class ExportAttrAlias extends ExportFieldAlias {
	private final String slot;
	private final QName  name;
	private final Convertor convertor;
	public ExportAttrAlias(String slot, String name) {
		this.slot = slot;
		this.name = new QName(name);
		this.convertor = null;
	}
	public ExportAttrAlias(String slot, String name, Convertor convertor) {
		this.slot = slot;
		this.name = new QName(name);
		this.convertor = convertor;
	}
	public QName getQName() { return name; }
	public boolean asAttribute() { return true; }
	public Object getData(Object data) {
		return ((INode)data).getVal(((INode)data).getAttrSlot(slot));
	}
	public Convertor getConvertor() { return convertor; }
}

