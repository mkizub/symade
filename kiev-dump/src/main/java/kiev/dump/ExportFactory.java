package kiev.dump;

import kiev.vtree.ExportXMLDump;

public interface ExportFactory {

	public void setupXMLExport(ExportXMLDump node, MarshallingContext context);
}
