package kiev.dump.xml;

import kiev.dump.ExportFactory;
import kiev.vlang.Env;
import kiev.vtree.ExportXMLDump;

public class ExportMarshallingContext extends AMarshallingContext {
	
	public ExportMarshallingContext(Env env, XMLDumpWriter out) {
		super(env, out);
	}
	
	public void exportXMLDump(ExportXMLDump node) {
		try {
			ExportFactory factory = (ExportFactory)Class.forName(node.exportFactory()).newInstance();
			factory.setupXMLExport(node, this);
			marshalData(node);
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException("Export error", e);
		}
	}
}
