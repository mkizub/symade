package kiev.dump;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vtree.INode;
import kiev.vtree.ExportXMLDump;

public interface XMLDumper {

	public byte[] serializeToXmlData(Env env, XMLDumpFilter filter, INode[] nodes) throws Exception;
	
	public byte[] exportToXmlData(Env env, ExportXMLDump node) throws Exception;
	
	public void dumpToXMLFile(Env env, XMLDumpFilter filter, INode[] nodes, File f) throws Exception;
	
	public void dumpToXMLStream(Env env, XMLDumpFilter filter, String comment, INode[] nodes, Writer writer) throws Exception;
	
	public void exportToXMLFile(Env env, ExportXMLDump node, File f) throws Exception;
	
	public void exportToXMLStream(Env env, String comment, ExportXMLDump node, Writer writer) throws Exception;
	
	public INode[] loadFromXmlFile(Env env, File f, byte[] data) throws Exception;
	
	public INode[] loadFromXmlStream(Env env, String mode, Reader reader) throws Exception;
	
	public INode loadProject(Env env, File f) throws Exception;

	public INode[] loadFromXmlData(Env env, byte[] data, String tdname, DNode pkg) throws Exception;
	
	public INode[] deserializeFromXmlFile(Env env, File f) throws Exception;
	
	public INode[] deserializeFromXmlData(Env env, byte[] data) throws Exception;
	
	public Object importFromXmlFile(File f, UnMarshallingContext context) throws Exception;

	public Object importFromXmlStream(InputStream in, UnMarshallingContext context) throws Exception;

}
