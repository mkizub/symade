/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.dump.xml;

import java.io.*;
import java.util.Vector;

import kiev.WorkerThread;
import kiev.dump.DumpFilter;
import kiev.dump.MarshallingContext;
import kiev.dump.UnMarshallingContext;
import kiev.dump.XMLDumpFilter;
import kiev.dump.XMLDumper;
import kiev.vlang.DNode;
import kiev.vlang.Env;
import kiev.vlang.Language;
import kiev.vtree.ExportXMLDump;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ITreeWalker;

import static kiev.stdlib.Asserts.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 133 $
 *
 */

public final class XMLDumperImpl implements XMLDumper {
	
	public static final String SOP_URI = "sop://sop/";
	
	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}

	public XMLNamespaceMap collectNamespaces(XMLDumpFilter filter, INode[] nodes) {
		final Vector<Language> langs = new Vector<Language>();
		for (INode node : nodes) {
			node.walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					//if (!checkIncludeNodeInDump(String dump, INode node))
					//	return false;
					//if (!checkIncludeAttrInDump(String dump, INode node, AttrSlot attr)
					//	return false;
					Language lng = n.asANode().getCompilerLang();
					if (lng != null && !langs.contains(lng))
						langs.add(lng);
					return true;
				}
			});
		}
		XMLNamespaceMap nsmap = XMLDumpFactory.getNamespaceMap();
		nsmap.add("sop", SOP_URI);
		for (Language lng : langs)
			nsmap.add(lng.getName(), lng.getURI());
		return nsmap;
	}

	public byte[] serializeToXmlData(Env env, XMLDumpFilter filter, INode[] nodes) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		XMLDumpWriter out = XMLDumpFactory.getWriter(bout, collectNamespaces(filter,nodes));
		out.startDocument();
		for (INode node : nodes)
			new DumpMarshallingContext(env,out,filter).marshalData(node);
		out.endDocument();
		return bout.toByteArray();
	}
	
	public byte[] exportToXmlData(Env env, ExportXMLDump node) throws Exception {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		XMLDumpWriter out = XMLDumpFactory.getWriter(bout, XMLDumpFactory.getNamespaceMap());
		out.startDocument();
		new ExportMarshallingContext(env, out).exportXMLDump(node);
		out.endDocument();
		return bout.toByteArray();
	}
	
	public void dumpToXMLFile(Env env, XMLDumpFilter filter, INode[] nodes, File f) throws Exception
	{
		make_output_dir(f);
		String comment = 
				"\n"+
				" Copyright (c) 2005-2007 UAB \"MAKSINETA\".\n"+
				" All rights reserved. This program and the accompanying materials\n"+
				" are made available under the terms of the Common Public License Version 1.0\n"+
				" which accompanies this distribution, and is available at\n"+
				" http://www.eclipse.org/legal/cpl-v10.html\n"+
				" \n"+
				" Contributors:\n"+
				"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
				;
		XMLDumpWriter out = XMLDumpFactory.getWriter(new BufferedOutputStream(new FileOutputStream(f)), collectNamespaces(filter,nodes));
		out.startDocument();
		out.addComment(comment,true);
		for (INode node : nodes)
			new DumpMarshallingContext(env,out,filter).marshalData(node);
		out.endDocument();
	}

	public void dumpToXMLStream(Env env, XMLDumpFilter filter, String comment, INode[] nodes, Writer writer) throws Exception
	{
		XMLDumpWriter out = XMLDumpFactory.getWriter(writer, collectNamespaces(filter,nodes));
		out.startDocument();
		if (comment != null)
			out.addComment(comment,true);
		for (INode node : nodes)
			new DumpMarshallingContext(env,out,filter).marshalData(node);
		out.endDocument();
	}

	public void exportToXMLFile(Env env, ExportXMLDump node, File f) throws Exception
	{
		make_output_dir(f);
		XMLDumpWriter out = XMLDumpFactory.getWriter(new BufferedOutputStream(new FileOutputStream(f)), XMLDumpFactory.getNamespaceMap());
		out.startDocument();
		out.addComment("Exported",true);
		new ExportMarshallingContext(env,out).exportXMLDump(node);
		out.endDocument();
	}
	
	public void exportToXMLStream(Env env, String comment, ExportXMLDump node, Writer writer) throws Exception
	{
		XMLDumpWriter out = XMLDumpFactory.getWriter(writer, XMLDumpFactory.getNamespaceMap());
		out.startDocument();
		if (comment != null)
			out.addComment(comment,true);
		new ExportMarshallingContext(env,out).exportXMLDump(node);
		out.endDocument();
	}

	public INode[] loadFromXmlFile(Env env, File f, byte[] data) throws Exception {
		assert (Thread.currentThread() instanceof WorkerThread);
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		deserializer.file = f;
		if (data != null)
			XMLDumpFactory.parse(new ByteArrayInputStream(data), deserializer);
		else
			XMLDumpFactory.parse(new BufferedInputStream(new FileInputStream(f)), deserializer);
		for (DelayedTypeInfo dti : deserializer.delayed_types)
			dti.applay(env);
		return new INode[]{(INode)deserializer.result};
	}
	
	public INode[] loadFromXmlStream(Env env, String mode, Reader reader) throws Exception
	{
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		if (mode != null && mode.equals("import"))
			deserializer.is_import = true;
		XMLDumpFactory.parse(reader, deserializer);
		for (DelayedTypeInfo dti : deserializer.delayed_types)
			dti.applay(env);
		return new INode[]{(INode)deserializer.result};
	}
	
	public INode loadProject(Env env, File f) throws Exception {
		assert (Thread.currentThread() instanceof WorkerThread);
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		XMLDumpFactory.parse(new BufferedInputStream(new FileInputStream(f)), deserializer);
		return (INode)deserializer.result;
	}

	public INode[] loadFromXmlData(Env env, byte[] data, String tdname, DNode pkg) throws Exception {
		assert (Thread.currentThread() instanceof WorkerThread);
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		deserializer.tdname = tdname;
		deserializer.pkg = pkg;
		deserializer.is_interface_only = true;
		XMLDumpFactory.parse(new ByteArrayInputStream(data), deserializer);
		for (DelayedTypeInfo dti : deserializer.delayed_types)
			dti.applay(env);
		return new INode[]{(INode)deserializer.result};
	}
	
	public INode[] deserializeFromXmlFile(Env env, File f) throws Exception {
		assert (Thread.currentThread() instanceof WorkerThread);
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		deserializer.is_import = true;
		XMLDumpFactory.parse(new BufferedInputStream(new FileInputStream(f)), deserializer);
		for (DelayedTypeInfo dti : deserializer.delayed_types)
			dti.applay(env);
		return new INode[]{(INode)deserializer.result};
	}
	public INode[] deserializeFromXmlData(Env env, byte[] data) throws Exception {
		assert (Thread.currentThread() instanceof WorkerThread);
		DumpUnMarshallingContext deserializer = new DumpUnMarshallingContext(env);
		deserializer.is_import = true;
		XMLDumpFactory.parse(new ByteArrayInputStream(data), deserializer);
		for (DelayedTypeInfo dti : deserializer.delayed_types)
			dti.applay(env);
		return new INode[]{(INode)deserializer.result};
	}
	
	public Object importFromXmlFile(File f, UnMarshallingContext _context) throws Exception {
		ImportMarshallingContext context = (ImportMarshallingContext)_context;
		XMLDumpFactory.parse(new BufferedInputStream(new FileInputStream(f)), context);
		return context.result;
	}

	public Object importFromXmlStream(InputStream in, UnMarshallingContext context) throws Exception {
		XMLDumpFactory.parse(in, (XMLDumpReader)context);
		return context.getResult();
	}

}

