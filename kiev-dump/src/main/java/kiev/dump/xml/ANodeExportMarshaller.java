package kiev.dump.xml;

import java.util.Enumeration;
import java.util.Vector;

import kiev.dump.DumpWriter;
import kiev.dump.Marshaller;
import kiev.dump.MarshallingContext;
import kiev.vtree.INode;

public class ANodeExportMarshaller implements Marshaller {
	
	final Vector<ExportTypeAlias> type_aliases;
	
	public ANodeExportMarshaller() {
		this.type_aliases = new Vector<ExportTypeAlias>();
	}
	
	public ANodeExportMarshaller add(ExportTypeAlias ta) {
		this.type_aliases.add(ta);
		return this;
	}
	
    public boolean canMarshal(Object data, MarshallingContext context) {
		if (data == null)
			return false;
		Class clazz = data.getClass();
		for (ExportTypeAlias eta : type_aliases) {
			if (eta.clazz == clazz)
				return true;
		}
		return false;
	}

    public void marshal(Object _data, DumpWriter _out, MarshallingContext context) throws Exception {
		INode node = (INode)_data;
    	XMLDumpWriter out = (XMLDumpWriter)_out; 
		ExportTypeAlias eta = null;
		for (ExportTypeAlias ta : type_aliases) {
			if (ta.clazz == node.getClass()) {
				eta = ta;
				break;
			}
		}
		
		out.startElement(eta.qname);
		
		for (ExportFieldAlias efa : eta.field_aliases) {
			if (!efa.asAttribute())
				continue;
			Object val = efa.getData(node);
			if (val == null)
				continue;
			context.attributeData(efa.getQName(), val, efa.getConvertor());
		}
		for (ExportFieldAlias efa : eta.field_aliases) {
			if (efa.asAttribute())
				continue;
			Object data = efa.getData(node);
			if (data == null)
				continue;
			if (data instanceof Object[] && ((Object[])data).length == 0)
				continue;
			if (efa.getQName() != null)
				out.startElement(efa.getQName());
			if (data instanceof Object[]) {
				for (Object obj : (Object[])data)
					context.marshalData(obj, efa.getMarshaller());
			}
			else {
				context.marshalData(data, efa.getMarshaller());
			}
			if (efa.getQName() != null)
				out.endElement(efa.getQName());
		}

		out.endElement(eta.qname);
	}
}

