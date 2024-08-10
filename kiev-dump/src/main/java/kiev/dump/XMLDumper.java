package kiev.dump;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vtree.ANodeContext;
import kiev.vtree.INode;
import kiev.vtree.ExportXMLDump;

public interface XMLDumper {

	public byte[] serializeToXmlData(Env env, XMLDumpFilter filter, INode[] nodes) throws Exception;

	public byte[] exportToXmlData(Env env, ExportXMLDump node) throws Exception;

	public void dumpToXMLFile(Env env, XMLDumpFilter filter, INode[] nodes, File f) throws Exception;

	public void dumpToXMLStream(Env env, XMLDumpFilter filter, String comment, INode[] nodes, Writer writer) throws Exception;

	public void exportToXMLFile(Env env, ExportXMLDump node, File f) throws Exception;

	public void exportToXMLStream(Env env, String comment, ExportXMLDump node, Writer writer) throws Exception;

	public INode[] loadFromXmlFile(Env env, ANodeContext nodeContext, File f, byte[] data) throws Exception;

	public INode[] loadFromXmlStream(Env env, ANodeContext nodeContext, String mode, Reader reader) throws Exception;

	public INode loadProject(Env env, ANodeContext nodeContext, File f) throws Exception;

	public INode[] loadFromXmlData(Env env, ANodeContext nodeContext, byte[] data, String tdname, DNode pkg) throws Exception;

	public INode[] deserializeFromXmlFile(Env env, ANodeContext nodeContext, File f) throws Exception;

	public INode[] deserializeFromXmlData(Env env, ANodeContext nodeContext, byte[] data) throws Exception;

	public Object importFromXmlFile(File f, UnMarshallingContext context) throws Exception;

	public Object importFromXmlStream(InputStream in, UnMarshallingContext context) throws Exception;

}
