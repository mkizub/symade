package kiev.dump.xml;

import javax.xml.namespace.QName;

import kiev.dump.Convertor;
import kiev.dump.Marshaller;


public abstract class ExportFieldAlias {
	public QName getQName() { return null; }
	public boolean asAttribute() { return false; }
	public Object getData(Object node) { return null; }
	public Marshaller getMarshaller() { return null; }
	public Convertor getConvertor() { return null; }
}

