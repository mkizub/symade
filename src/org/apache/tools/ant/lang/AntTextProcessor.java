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
package org.apache.tools.ant.lang;
import syntax kiev.Syntax;

import kiev.fmt.SyntaxManager;
import kiev.fmt.common.TextDrawSyntax;
import kiev.fmt.common.TextPrinter;
import kiev.fmt.common.TextParser;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.dump.*;
import kiev.dump.xml.*;
import java.io.*;
import java.util.Properties;
import kiev.vdom.*;
import javax.xml.namespace.QName;

import org.apache.tools.ant.MagicNames;

public class AntTextProcessor implements TextDrawSyntax, TextPrinter, TextParser, ExportFactory {

	private boolean current;
	private String comment;
	private AntContainer proj;
	
	private static final String[][] STD_TYPE_DEFS = {
		{"and","org.apache.tools.ant.taskdefs.condition.And"},
		{"antversion","org.apache.tools.ant.taskdefs.condition.AntVersion"},
		{"contains","org.apache.tools.ant.taskdefs.condition.Contains"},
		{"equals","org.apache.tools.ant.taskdefs.condition.Equals"},
		{"filesmatch","org.apache.tools.ant.taskdefs.condition.FilesMatch"},
		{"hasfreespace","org.apache.tools.ant.taskdefs.condition.HasFreeSpace"},
		{"http","org.apache.tools.ant.taskdefs.condition.Http"},
		{"isfailure","org.apache.tools.ant.taskdefs.condition.IsFailure"},
		{"isfalse","org.apache.tools.ant.taskdefs.condition.IsFalse"},
		{"isfileselected","org.apache.tools.ant.taskdefs.condition.IsFileSelected"},
		{"isreachable","org.apache.tools.ant.taskdefs.condition.IsReachable"},
		{"isreference","org.apache.tools.ant.taskdefs.condition.IsReference"},
		{"isset","org.apache.tools.ant.taskdefs.condition.IsSet"},
		{"issigned","org.apache.tools.ant.taskdefs.condition.IsSigned"},
		{"istrue","org.apache.tools.ant.taskdefs.condition.IsTrue"},
		{"not","org.apache.tools.ant.taskdefs.condition.Not"},
		{"matches","org.apache.tools.ant.taskdefs.condition.Matches"},
		{"or","org.apache.tools.ant.taskdefs.condition.Or"},
		{"os","org.apache.tools.ant.taskdefs.condition.Os"},
		{"parsersupports","org.apache.tools.ant.taskdefs.condition.ParserSupports"},
		{"resourcesmatch","org.apache.tools.ant.taskdefs.condition.ResourcesMatch"},
		{"resourcecontains","org.apache.tools.ant.taskdefs.condition.ResourceContains"},
		{"scriptcondition","org.apache.tools.ant.types.optional.ScriptCondition"},
		{"socket","org.apache.tools.ant.taskdefs.condition.Socket"},
		{"typefound","org.apache.tools.ant.taskdefs.condition.TypeFound"},
		{"xor","org.apache.tools.ant.taskdefs.condition.Xor"}
	};

	public void setProperty(String name, String value) {
		if (name.equals("current"))
			current = Boolean.parseBoolean(value);
		else if (name.equals("comment"))
			comment = value;
		else
			throw new IllegalArgumentException(name);
	}

	public Draw_ATextSyntax lookup(Env env) {
		if (current)
			return SyntaxManager.getLanguageSyntax("org·apache·tools·ant·lang·syntax-for-ant", env);
		return SyntaxManager.getLanguageSyntax("org·apache·tools·ant·lang·syntax-for-ant", null);
	}

	public void print(ANode[] nodes, File file, Env env) {
		try {
			make_output_dir(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		print(nodes, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")), env);
	}

	public void print(ANode[] nodes, OutputStream outp, Env env) {
		print(nodes, new BufferedWriter(new OutputStreamWriter(outp, "UTF-8")), env);
	}

	public void print(ANode[] nodes, Writer writer, Env env) {
		try {
			for (ANode node : nodes) {
				if (node instanceof FileUnit) {
					for (ANode n : (ANode[])node.getVal("members")) {
						if (n instanceof AntContainer)
							exportXml((AntContainer)n, writer, env);
					}
				} else {
					exportXml((AntContainer)node, writer, env);
				}
			}
			writer.close();
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}
	
	private void exportXml(AntContainer node, Writer writer, Env env) throws Exception {
		if (comment == null)
			comment =	"\n"+
						" Copyright (c) 2005-2008 UAB \"MAKSINETA\".\n"+
						" All rights reserved. This program and the accompanying materials\n"+
						" are made available under the terms of the Common Public License Version 1.0\n"+
						" which accompanies this distribution, and is available at\n"+
						" http://www.eclipse.org/legal/cpl-v10.html\n"+
						" \n"+
						" Contributors:\n"+
						"     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n"
						;
		DumpFactory.getXMLDumper().exportToXMLStream(env, comment, node, writer);
	}

	public ANode[] parse(File file, Env env) {
		return parse(new BufferedInputStream(new FileInputStream(file)), env);
	}

	public ANode[] parse(InputStream inp, Env env) {
		try {
			XMLElement el = (XMLElement)DumpFactory.getXMLDumper().importFromXmlStream(new BufferedInputStream(inp), new XMLUnMarshallingContext(env));
			return new ANode[]{convertXML(el, env)};
		} catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	public ANode[] parse(Reader reader, Env env) {
		throw new RuntimeException("Reading XML from Reader is not supported");
	}

	public void setupXMLExport(ExportXMLDump node, MarshallingContext context) {
		context.add(new DataMarshaller());
		context.add(new TypeMarshaller());
		context.add(new ANodeExportMarshaller().
				add(new ExportTypeAlias("project", AntProject.class).
						add(new ExportAttrAlias("sname", "name")).
						add(new ExportAttrAlias("dflt", "default")).
						add(new ExportAttrAlias("basedir", "basedir")).
						add(new ExportInlineAlias("members"))).
				add(new ExportTypeAlias("antlib", AntLib.class).
						add(new ExportInlineAlias("members"))).
				add(new ExportTypeAlias("target", AntTarget.class).
						add(new ExportAttrAlias("sname", "name")).
						add(new ExportAttrAlias("depends", "depends", new ArrayConvertor(null,",",null,null))).
						add(new ExportAttrAlias("onlyif", "if")).
						add(new ExportAttrAlias("unless", "unless")).
						add(new ExportAttrAlias("description", "description")).
						add(new ExportInlineAlias("members")))
		);
		context.add(new XMLNodeExportMarshaller());
		context.add(new AntNodeExportMarshaller());
		context.add(new DataConvertor());
	}
	
	private ASTNode convertXML(XMLElement el, Env env) {
		if (el.name.eq("project")) {
			this.proj = new AntProject();
			addStandardAndTypes();
			foreach (XMLAttribute attr; el.attributes)
				proj.attributes += new AntAttribute(attr.name, attr.text.text);
			proj.members.addAll(el.elements.delToArray());
			return proj;
		}
		else if (el.name.eq("antlib")) {
			this.proj = new AntLib();
			addStandardAndTypes();
			foreach (XMLAttribute attr; el.attributes)
				proj.attributes += new AntAttribute(attr.name, attr.text.text);
			proj.members.addAll(el.elements.delToArray());
			return proj;
		}
		return el.detach();
	}
	
	private boolean hasAttr(XMLAttribute[] attrs, String name) {
			foreach (XMLAttribute attr; attrs; attr.name.eq(name))
				return true;
			return false;
	}
	
	private void addStandardAndTypes() {
		Properties props = System.getProperties();
		foreach (String p; (java.util.Enumeration<String>)props.propertyNames()) {
			AntValueProperty avp = new AntValueProperty();
			avp.sname = p;
			avp.value = new AntText(props.getProperty(p));
			proj.predefines += avp;
		}
		
		InputStream in = this.getClass().getResourceAsStream(MagicNames.TYPEDEFS_PROPERTIES_RESOURCE);
		Properties p = new Properties();
		p.load(in);
		foreach (String name; (java.util.Enumeration<String>)p.propertyNames()) {
			AntTypeDef td = new AntTypeDef();
			td.sname = name;
			td.classname = p.getProperty(name);
			proj.predefines += td;
		}
		in.close();

		in = this.getClass().getResourceAsStream(MagicNames.TASKDEF_PROPERTIES_RESOURCE);
		p = new Properties();
		p.load(in);
		foreach (String name; (java.util.Enumeration<String>)p.propertyNames()) {
			AntTaskDef td = new AntTaskDef();
			td.sname = name;
			td.classname = p.getProperty(name);
			proj.predefines += td;
		}
		in.close();

		foreach (String[] p; STD_TYPE_DEFS) {
			AntTypeDef td = new AntTypeDef();
			td.sname = p[0];
			td.classname = p[1];
			proj.predefines += td;
		}
	}
	
	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}

}

public class AntNodeExportMarshaller implements Marshaller {
    public boolean canMarshal(Object data, MarshallingContext context) {
		return data instanceof AntNode;
	}
	
	private static QName makeQName(String name, String uri, String prefix) {
		if (uri == null || uri == "")
			return new QName(name);
		if (prefix == null)
			prefix = "";
		return new QName(uri, name, prefix);
	}

    public void marshal(Object data, DumpWriter writer, MarshallingContext context) throws Exception {
		AntNode n = (AntNode)data;
    	XMLDumpWriter out = (XMLDumpWriter)writer;
		
		// start element
		QName qname = makeQName(n.getAntXMLName(), n.getAntXMLNameSpace(), n.getAntXMLPrefix());
		out.startElement(qname);

		// add attributes converted into node's @nodeAttr
		AntXMLAttributes xattrs = n.getClass().getAnnotation(AntXMLAttributes.class);
		if (xattrs != null) {
			foreach (AntXMLAttribute xa; xattrs.value()) {
				String aname = xa.attr();
				if (aname.length() == 0) aname = xa.value();
				Object val = n.getVal(aname.intern());
				if (val == null)
					continue;
				context.attributeData(makeQName(xa.value(), xa.uri(), xa.prefix()), val);
			}
		}

		// add rest of attributes
		foreach (AntAttribute a; n.attributes)
			context.attributeData(makeQName(a.name, a.uri, a.prefix), a.text.toText());
		// add members
		foreach (ASTNode m; n.members)
			context.marshalData(m);
		// end of the element
		out.endElement(qname);
	}

}

