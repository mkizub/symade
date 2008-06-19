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
package kiev.vlang;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.stream.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 133 $
 *
 */

public final class DumpUtils {
	
	public static final String SOP_URI = "sop://sop/";
	
	private static final boolean OLD_XML_READ;
	private static final boolean OLD_XML_WRITE;
	private static final boolean XPP_PARSER;
	static {
		String dump = System.getProperty("symade.dump.old",null);
		if (dump != null) {
			OLD_XML_READ = Boolean.valueOf(dump).booleanValue();
			OLD_XML_WRITE = Boolean.valueOf(dump).booleanValue();
		} else {
			OLD_XML_READ = Boolean.valueOf(System.getProperty("symade.dump.read.old","false")).booleanValue();
			OLD_XML_WRITE = Boolean.valueOf(System.getProperty("symade.dump.write.old","false")).booleanValue();
		}
		try {
			Class.forName("org.xmlpull.mxp1.MXParser");
			XPP_PARSER = true;
		} catch (NoClassDefFoundError e) {
			XPP_PARSER = false;
		}
	}
	private DumpUtils() {}

	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}

	private static Vector<Language> collectLanguages(String dump, ANode node) {
		Vector<Language> langs = new Vector<Language>();
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				//if (!checkIncludeNodeInDump(String dump, ANode node))
				//	return false;
				//if (!checkIncludeAttrInDump(String dump, ANode node, AttrSlot attr)
				//	return false;
				Language lng = n.getCompilerLang();
				if (lng != null && !langs.contains(lng))
					langs.append(lng);
				return true;
			}
		});
		return langs;
	}

	public static void dumpToXMLFile(String dump, ASTNode node, File f)
		throws IOException
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
		if (!OLD_XML_WRITE) {
			org.xmlpull.mxp1_serializer.MXSerializer out = new org.xmlpull.mxp1_serializer.MXSerializer();
			out.setFeature("http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe", true);
			out.setFeature("http://xmlpull.org/v1/doc/features.html#names-interned", true);
			out.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", " ");
			out.setOutput(new BufferedOutputStream(new FileOutputStream(f)), "UTF-8");
			out.startDocument("1.1", "UTF-8", Boolean.TRUE);
			out.ignorableWhitespace("\n");
			out.comment(comment);
			out.ignorableWhitespace("\n");
			out.setPrefix("sop", SOP_URI);
			foreach (Language lng; collectLanguages(dump,node))
				out.setPrefix(lng.getName(), lng.getURI());
			out.startTag(SOP_URI,"dump");
			out.attribute(null, "version", "1.0");
			writeNodeToXML(dump,node,out);
			out.endTag(SOP_URI,"dump");
			out.endDocument();
			out.getWriter().close();
		} else {
			XMLOutputFactory outf = XMLOutputFactory.newInstance();
			XMLStreamWriter out = outf.createXMLStreamWriter(new BufferedOutputStream(new FileOutputStream(f)), "UTF-8");
			//out.writeStartDocument("UTF-8", "1.1");
			out.writeProcessingInstruction("xml", "version='1.1' encoding='UTF-8' standalone='yes'");
			out.writeCharacters("\n");
			out.writeComment(comment);
			writeNodeToXML(dump,node,out,0);
			out.writeEndDocument();
			out.close();
		}
	}
	
	private static void writeNodeToXML(String dump, ANode node, org.xmlpull.mxp1_serializer.MXSerializer out) {
		Language lng = node.getCompilerLang();
		if (node.getCompilerNodeName() == "ASTNode" && node.getClass() != ASTNode.class)
			lng = null;
		if (OLD_XML_WRITE) {
			out.startTag(null,"a-node");
			out.attribute(null, "class", node.getClass().getName());
		}
		else if (lng != null) {
			out.startTag(lng.getURI(),node.getCompilerNodeName());
		}
		else {
			out.startTag(SOP_URI,node.getClass().getName());
		}
		if (!OLD_XML_WRITE) {
			foreach (AttrSlot attr; node.values(); attr instanceof ScalarAttrSlot && attr.isXmlAttr()) {
				if (!checkIncludeAttrInDump(dump,node,attr))
					continue;
				Object obj = ((ScalarAttrSlot)attr).get(node);
				if (obj == null)
					continue;
				
				if (obj instanceof Type)
					out.attribute(null, attr.getXmlLocalName(), ((Type)obj).makeSignature());
				else
					out.attribute(null, attr.getXmlLocalName(), String.valueOf(obj));
			}
		}
		foreach (AttrSlot attr; node.values(); (OLD_XML_WRITE || !attr.isXmlAttr()) && !attr.isXmlIgnore()) {
			if (!checkIncludeAttrInDump(dump,node,attr))
				continue;
			if (attr instanceof SpaceAttrSlot) {
				ANode[] elems = attr.getArray(node);
				foreach (ANode n; elems; checkIncludeNodeInDump(dump,n))
					goto write_space;
				continue;
			write_space:
				out.startTag(null, attr.getXmlLocalName());
				foreach (ANode n; elems; checkIncludeNodeInDump(dump,n))
					writeNodeToXML(dump, n, out);
				out.endTag(null, attr.getXmlLocalName());
				continue;
			}
			else if (attr instanceof ScalarAttrSlot) {
				Object obj = attr.get(node);
				if (obj == null)
					continue;
				out.startTag(null, attr.getXmlLocalName());
				if (obj instanceof ANode)
					writeNodeToXML(dump, (ANode)obj, out);
				else if (obj instanceof Type)
					out.text(((Type)obj).makeSignature());
				else
					out.text(String.valueOf(obj));
				out.endTag(null, attr.getXmlLocalName());
			}
		}
		if (!OLD_XML_WRITE) {
			foreach (ANode n; node.getExtChildIterator(null)) {
				AttrSlot attr = n.pslot();
				if (!attr.is_attr || !attr.is_external)
					continue;
				if (attr.getCompilerLang() == null)
					continue;
				if (!checkIncludeAttrInDump(dump,node,attr))
					continue;
				out.startTag(attr.getXmlNamespaceURI(), attr.getXmlLocalName());
				writeNodeToXML(dump, n, out);
				out.endTag(attr.getXmlNamespaceURI(), attr.getXmlLocalName());
			}
		}

		if (OLD_XML_WRITE)
			out.endTag(null,"a-node");
		else if (lng != null)
			out.endTag(lng.getURI(),node.getCompilerNodeName());
		else
			out.endTag(SOP_URI,node.getClass().getName());
	}
	
	private static void writeNodeToXML(String dump, ANode node, XMLStreamWriter out, int indent) {
		writeXMLIndent(out,indent);
		out.writeStartElement("a-node");
		out.writeAttribute("class", node.getClass().getName());

		foreach (AttrSlot attr; node.values(); attr != ASTNode.nodeattr$this && attr != ASTNode.nodeattr$parent) {
			if (!checkIncludeAttrInDump(dump,node,attr))
				continue;
			if (attr instanceof SpaceAttrSlot) {
				ANode[] elems = attr.getArray(node);
				foreach (ANode n; elems; checkIncludeNodeInDump(dump,n))
					goto write_space;
				continue;
			write_space:
				writeXMLIndent(out,indent+1);
				out.writeStartElement(attr.name);
				foreach (ANode n; elems; checkIncludeNodeInDump(dump,n))
					writeNodeToXML(dump, n, out, indent+2);
				writeXMLIndent(out,indent+1);
				out.writeEndElement();
				continue;
			}
			else if (attr instanceof ExtSpaceAttrSlot) {
				foreach (ANode n; attr.iterate(node); checkIncludeNodeInDump(dump,n))
					goto write_list;
				continue;
			write_list:
				writeXMLIndent(out,indent+1);
				out.writeStartElement(attr.name);
				foreach (ANode n; attr.iterate(node); checkIncludeNodeInDump(dump,n))
					writeNodeToXML(dump, n, out, indent+2);
				writeXMLIndent(out,indent+1);
				out.writeEndElement();
				continue;
			}
			else if (attr instanceof ScalarAttrSlot) {
				Object obj = attr.get(node);
				if (obj == null)
					continue;
				writeXMLIndent(out,indent+1);
				out.writeStartElement(attr.name);
				if (obj instanceof ANode) {
					writeNodeToXML(dump, (ANode)obj, out, indent+2);
					writeXMLIndent(out,indent+1);
				}
				else if (obj instanceof Type)
					out.writeCharacters(((Type)obj).makeSignature());
				else
					out.writeCharacters(String.valueOf(obj));
				out.writeEndElement();
			}
		}

		writeXMLIndent(out,indent);
		out.writeEndElement();
	}
	
	private static void writeXMLIndent(XMLStreamWriter out, int indent) {
		out.writeCharacters("\n");
		for (int i=0; i < indent; i++)
			out.writeCharacters(" ");
	}

	private static boolean checkIncludeNodeInDump(String dump, ANode node) {
		if (node == null)
			return false;
		return node.includeInDump(dump, ASTNode.nodeattr$this, node);
	}

	private static boolean checkIncludeAttrInDump(String dump, ANode node, AttrSlot attr) {
		if (node == null || attr == null)
			return false;
		if (attr instanceof ScalarAttrSlot) {
			Object val = attr.get(node);
			if (val == null)
				return false;
			return node.includeInDump(dump, attr, val);
		}
		else if (attr instanceof SpaceAttrSlot) {
			ANode[] vals = attr.getArray(node);
			if (vals.length == 0)
				return false;
			return node.includeInDump(dump, attr, vals);
		}
		else if (attr instanceof ExtSpaceAttrSlot) {
			ExtChildrenIterator iter = attr.iterate(node);
			if (!iter.hasMoreElements())
				return false;
			return node.includeInDump(dump, attr, iter);
		}
		return false;
	}

	public static FileUnit loadFromXmlFile(File f, byte[] data) {
		assert (Thread.currentThread() instanceof WorkerThread);
		XMLDeSerializer deserializer = new XMLDeSerializer();
		deserializer.file = f;
		if (XPP_PARSER) {
			//XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			//factory.setNamespaceAware(false);//factory.setNamespaceAware(true);
			//XmlPullParser xpp = factory.newPullParser();
			/*XmlPullParser*/org.xmlpull.mxp1.MXParserCachingStrings xpp = new org.xmlpull.mxp1.MXParserCachingStrings();
			xpp.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces",true);
			if (data != null)
				xpp.setInput(new ByteArrayInputStream(data), "UTF-8");
			else
				xpp.setInput(new FileInputStream(f), "UTF-8");
			new PullHandler(deserializer).processDocument(xpp);
		} else {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			if (data != null)
				saxParser.parse(new ByteArrayInputStream(data), new SAXHandler(deserializer));
			else
				saxParser.parse(f, new SAXHandler(deserializer));
		}
		foreach (DelayedTypeInfo dti; deserializer.delayed_types)
			dti.applay();
		ANode root = deserializer.root;
		if!(root instanceof FileUnit) {
			root = FileUnit.makeFile(getRelativePath(f), false);
			root.current_syntax = "stx-fmt\u001fsyntax-dump-full";
			root.members += deserializer.root;
		}
		return (FileUnit)root;
	}
	
	public static Project loadProject(File f) {
		assert (Thread.currentThread() instanceof WorkerThread);
		XMLDeSerializer deserializer = new XMLDeSerializer();
		if (XPP_PARSER) {
			//XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			//factory.setNamespaceAware(false);//factory.setNamespaceAware(true);
			//XmlPullParser xpp = factory.newPullParser();
			/*XmlPullParser*/org.xmlpull.mxp1.MXParserCachingStrings xpp = new org.xmlpull.mxp1.MXParserCachingStrings();
			xpp.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces",true);
			xpp.setInput(new FileInputStream(f), "UTF-8");
			new PullHandler(deserializer).processDocument(xpp);
		} else {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(f, new SAXHandler(deserializer));
		}
		Project prj = (Project)deserializer.root;
		prj.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof FileUnit) {
					n.project_file = true;
					return false;
				}
				return true;
			}
		});
		return prj;
	}

	public static FileUnit loadFromXmlData(byte[] data, String tdname, ComplexTypeDecl pkg) {
		assert (Thread.currentThread() instanceof WorkerThread);
		XMLDeSerializer deserializer = new XMLDeSerializer();
		deserializer.tdname = tdname;
		deserializer.pkg = pkg;
		if (XPP_PARSER) {
			//XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
			//factory.setNamespaceAware(false);//factory.setNamespaceAware(true);
			//XmlPullParser xpp = factory.newPullParser();
			/*XmlPullParser*/org.xmlpull.mxp1.MXParserCachingStrings xpp = new org.xmlpull.mxp1.MXParserCachingStrings();
			xpp.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces",true);
			xpp.setInput(new ByteArrayInputStream(data), "UTF-8");
			new PullHandler(deserializer).processDocument(xpp);
		} else {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new ByteArrayInputStream(data), new SAXHandler(deserializer));
		}
		foreach (DelayedTypeInfo dti; deserializer.delayed_types)
			dti.applay();
		FileUnit root = (FileUnit)deserializer.root;
		Kiev.runProcessorsOn(root);
		return root;
	}
	
	final static class DelayedTypeInfo {
		final ANode node;
		final ScalarAttrSlot attr;
		final String signature;
		DelayedTypeInfo(ANode node, ScalarAttrSlot attr, String signature) {
			this.node = node;
			this.attr = attr;
			this.signature = signature;
		}
		void applay() {
			AType tp = AType.fromSignature(signature,false);
			if (tp != null) {
				attr.set(node,tp);
			} else {
				((ENode)node).setTypeSignature(signature);
			}
		}
	}
	
	
	static abstract class XMLAttributeSet {
		public abstract int getCount();
		public abstract String getName(int i);
		public abstract String getValue(int i);
	}
	
	final static class XMLDeSerializer {
		ASTNode root;
		File file;
		ComplexTypeDecl pkg;
		String tdname;
		boolean expect_attr;
		int ignore_count;
		Stack<ANode> nodes = new Stack<ANode>();
		Stack<AttrSlot> attrs = new Stack<AttrSlot>();
		String text;
		Vector<DelayedTypeInfo> delayed_types = new Vector<DelayedTypeInfo>();
		Hashtable<String,Language> languages = new Hashtable<String,Language>();
		
		private Language getLanguage(String uri) {
			Language lng = languages.get(uri);
			if (lng != null)
				return lng;
			String cl_name = uri.substring(uri.indexOf("?class=")+7);
			Class lng_class = Class.forName(cl_name);
			lng = (Language)lng_class.getField(Constants.nameInstance).get(null);
			languages.put(uri, lng);
			return lng;
		}
		
		
		public void startElement(String elUri, String elName, XMLAttributeSet attributes) {
			if (ignore_count > 0) {
				ignore_count++;
				return;
			}
			if (root == null && "dump".equals(elName) && SOP_URI.equals(elUri))
				return;
			if (!expect_attr) {
				if (root == null) {
					Language lng = null;
					String cl_name = null;
					if (elUri.length() > 0) {
						if (elUri.equals(SOP_URI)) {
							cl_name = elName;
						} else {
							lng = getLanguage(elUri);
							cl_name = lng.getClassByNodeName(elName);
						}
					} else {
						assert (elName.equals("a-node"));
						assert (attributes.getCount() >= 1 && attributes.getName(0).equals("class"));
						cl_name = attributes.getValue(0);
					}
					if (pkg != null) {
						String qname;
						if (pkg instanceof Env)
							qname = tdname;
						else
							qname = pkg.qname() + '\u001f' + tdname;
						FileUnit fu = FileUnit.makeFile(qname.replace('\u001f','/')+".xml", false);
						fu.scanned_for_interface_only = true;
						ComplexTypeDecl p = pkg;
						while (p != null && !p.isPackage())
							p = p.package_clazz.dnode;
						fu.srpkg.symbol = p;
						root = fu;
						ComplexTypeDecl td = (ComplexTypeDecl)Env.getRoot().resolveGlobalDNode(qname);
						if (td != null) {
							assert(td.getClass().getName().equals(cl_name));
							td.cleanupOnReload();
						}
						else if (lng != null) {
							td = (ComplexTypeDecl)lng.makeNode(elName);
						}
						else {
							td = (ComplexTypeDecl)Class.forName(cl_name).newInstance();
						}
						addAttributes(td, attributes);
						td.sname = tdname;
						td.package_clazz.symbol = pkg;
						pkg.sub_decls += td;
						if (td instanceof KievPackage) {
							fu.setAutoGenerated(true);
							if (td.parent() == null)
								pkg.members += td;
						} else {
							fu.members += td;
						}
						td.is_interface_only = true;
						nodes.push(td);
					}
					else if (cl_name.equals("kiev.vlang.FileUnit")) {
						if (file == null)
							file = new File(tdname.replace('\u001f','/')+".xml");
						FileUnit fu = FileUnit.makeFile(getRelativePath(file), false);
						root = fu;
						fu.current_syntax = "stx-fmt\u001fsyntax-dump-full";
						addAttributes(fu, attributes);
						nodes.push(root);
					}
					else {
						if (lng != null)
							root = (ASTNode)lng.makeNode(elName);
						else
							root = (ASTNode)Class.forName(cl_name).newInstance();
						addAttributes(root, attributes);
						if (root instanceof DNode)
							((DNode)root).is_interface_only = true;
						nodes.push(root);
					}
					expect_attr = true;
					//System.out.println("push root");
					return;
				}
				if (elUri.length() == 0 && elName.equals("a-node") || elUri.length() > 0) {
					Language lng = null;
					String cl_name = null;
					if (elUri.length() > 0) {
						if (elUri.equals(SOP_URI)) {
							cl_name = elName;
						} else {
							lng = getLanguage(elUri);
							cl_name = lng.getClassByNodeName(elName);
						}
					} else {
						assert (elName.equals("a-node"));
						assert (attributes.getCount() >= 1 && attributes.getName(0).equals("class"));
						cl_name = attributes.getValue(0);
					}
					ANode n;
					AttrSlot attr = attrs.peek();
					if (!attr.isWrittable() || cl_name.equals("kiev.vlang.Symbol")) {
						AttrSlot attr = attrs.peek();
						if (attr instanceof SpaceAttrSlot || attr instanceof ExtSpaceAttrSlot) {
							n = (ANode)attr.typeinfo.newInstance();
						} else {
							n = (ANode)((ScalarAttrSlot)attr).get(nodes.peek());
							if (n == null)
								n = (ANode)attr.typeinfo.newInstance();
						}
						addAttributes(n, attributes);
					}
					else if (lng != null) {
						n = lng.makeNode(elName);
						addAttributes(n, attributes);
					}
					else if (cl_name.equals("kiev.parser.ASTOperatorAlias")) {
						n = new kiev.parser.ASTOperatorAlias();
						addAttributes(n, attributes);
					}
					else {
						try {
							n = (ANode)Class.forName(cl_name).newInstance();
							addAttributes(n, attributes);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							n = null;
						}
					}
					if (n instanceof DNode)
						n.is_interface_only = true;
					//System.out.println("push node "+nodes.length);
					nodes.push(n);
					expect_attr = true;
					return;
				}
			} else {
				Language lng = null;
				AttrSlot attr = null;
				if (!OLD_XML_READ && elUri.length() > 0) {
					lng = getLanguage(elUri);
					if (lng != null)
						attr = lng.getExtAttrByName(elName);
					if (attr != null) {
						//System.out.println("push attr "+attr.name);
						attrs.push(attr);
						expect_attr = false;
						return;
					}
					//throw new SAXException("Attribute '"+qName+"' not found in "+n.getClass());
					System.out.println("Attribute '"+elName+"' not found in language "+(lng!=null?lng.getName():"?")+" URI "+elUri);
					ignore_count = 1;
				} else {
					ANode n = nodes.peek();
					if (!OLD_XML_READ) {
						foreach (AttrSlot a; n.values(); !a.isXmlIgnore() && a.getXmlLocalName().equals(elName)) {
							attr = a;
							break;
						}
					}
					if (attr == null) {
						foreach (AttrSlot a; n.values(); !a.isXmlIgnore() && a.name.equals(elName)) {
							attr = a;
							break;
						}
					}
					if (attr != null) {
						//System.out.println("push attr "+attr.name);
						attrs.push(attr);
						expect_attr = false;
						return;
					}
					//throw new SAXException("Attribute '"+qName+"' not found in "+n.getClass());
					System.out.println("Attribute '"+elName+"' not found in "+n.getClass());
					ignore_count = 1;
				}
			}
		}

		public void endElement(String elUri, String elName) {
			if (ignore_count > 0) {
				ignore_count--;
				return;
			}
			if (nodes.isEmpty() && "dump".equals(elName) && SOP_URI.equals(elUri))
				return;
			if (expect_attr) {
				ANode n = nodes.pop();
				if (elUri.length() > 0) {
					if (elUri.equals(SOP_URI)) {
						assert(n.getCompilerLang() == null);
						assert(n.getClass().getName().equals(elName));
					} else {
						assert(n.getCompilerLang().getURI().equals(elUri));
						assert(n.getCompilerNodeName().equals(elName));
					}
				} else {
					assert(elName.equals("a-node"));
				}

				if (n instanceof TypeDecl) {
					n.setTypeDeclNotLoaded(false);
				}
				if (nodes.isEmpty()) {
					//System.out.println("pop  root");
					expect_attr = false;
					return;
				}
				//System.out.println("pop  node "+nodes.length);
				AttrSlot attr = attrs.peek();
				if (attr instanceof SpaceAttrSlot) {
					//System.out.println("add node to "+attr.name);
					attr.add(nodes.peek(),n);
				}
				else if (attr instanceof ExtSpaceAttrSlot) {
					//System.out.println("add node to "+attr.name);
					attr.add(nodes.peek(),n);
				}
				else if (attr instanceof ScalarAttrSlot){
					//System.out.println("set node to "+attr.name);
					if (!n.isAttached())
						attr.set(nodes.peek(),n);
				}
				expect_attr = false;
			} else {
				AttrSlot attr = attrs.pop();
				//System.out.println("pop  attr "+attr.name);
				if (attr instanceof ScalarAttrSlot && text != null) {
					writeAttribute(nodes.peek(), (ScalarAttrSlot)attr, text);
					text = null;
				}
				expect_attr = true;
			}
		}
		public void addText(String str) {
			if (ignore_count > 0 || expect_attr || attrs.length <= 0)
				return;
			AttrSlot attr = attrs.peek();
			if (ANode.class.isAssignableFrom(attr.clazz))
				return;
			if (text == null)
				text = str;
			else
				text += str;
		}
		private void addAttributes(ANode node, XMLAttributeSet attributes) {
			int n = attributes.getCount();
		next_attr:
			for (int i=0; i < n; i++) {
				String nm = attributes.getName(i);
				if (nm.equals("class"))
					continue;
				foreach (ScalarAttrSlot attr; node.values(); !attr.isXmlIgnore() && attr.getXmlLocalName().equals(nm)) {
					writeAttribute(node, attr, attributes.getValue(i))
					continue next_attr;
				}
				foreach (ScalarAttrSlot attr; node.values(); !attr.isXmlIgnore() && attr.name.equals(nm)) {
					writeAttribute(node, attr, attributes.getValue(i))
					continue next_attr;
				}
				System.out.println("Attribute '"+nm+"' not found in "+node.getClass());
			}
		}
		private void writeAttribute(ANode node, ScalarAttrSlot attr, String value) {
			if (attr.clazz == String.class)
				attr.set(node,value);
			else if (attr.clazz == Boolean.TYPE)
				attr.set(node,Boolean.valueOf(value.trim()));
			else if (attr.clazz == Integer.TYPE)
				attr.set(node,Integer.valueOf((int)parseLong(value)));
			else if (attr.clazz == Byte.TYPE)
				attr.set(node,Byte.valueOf((byte)parseLong(value)));
			else if (attr.clazz == Short.TYPE)
				attr.set(node,Short.valueOf((short)parseLong(value)));
			else if (attr.clazz == Long.TYPE)
				attr.set(node,Long.valueOf(parseLong(value)));
			else if (attr.clazz == Float.TYPE)
				attr.set(node,Float.valueOf(value.trim()));
			else if (attr.clazz == Double.TYPE)
				attr.set(node,Double.valueOf(value.trim()));
			else if (attr.clazz == Character.TYPE)
				attr.set(node,Character.valueOf(value.trim().charAt(0)));
			else if (Enum.class.isAssignableFrom(attr.clazz)) {
				//attr.set(node,Enum.valueOf(attr.clazz,value.trim()));
				attr.set(node,attr.clazz.getMethod("valueOf",String.class).invoke(null,value.trim()));
			}
			else if (attr.clazz == Operator.class)
				attr.set(node,Operator.getOperatorByName(value.trim()));
			else if (Type.class.isAssignableFrom(attr.clazz)) {
				//attr.set(node,AType.fromSignature(value.trim()));
				if (node instanceof ENode && attr.name == "type_lnk")
					((ENode)node).setTypeSignature(value.trim());
				else
					delayed_types.append(new DelayedTypeInfo(node, attr, value.trim()));
			}
			else
				//throw new SAXException("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+attr.clazz);
				System.out.println("Attribute '"+attr.name+"' of "+node.getClass()+" uses unsupported "+attr.clazz);
		}
		private static long parseLong(String text) {
			text = text.trim();
			int radix;
			if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
			else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
			else { radix = 10; }
			if (text.charAt(text.length()-1) == 'L' || text.charAt(text.length()-1) == 'l') {
				text = text.substring(0,text.length()-1);
				if (text.length() == 0)
					return 0L; // 0L 
			}
			long l = ConstExpr.parseLong(text,radix);
			return l;
		}
	}
		
	final static class PullHandler {
		XMLDeSerializer deserializer;
		
		PullHandler(XMLDeSerializer deserializer) {
			this.deserializer = deserializer;
		}

		public void processDocument(/*XmlPullParser*/ org.xmlpull.mxp1.MXParserCachingStrings xpp) // throws XmlPullParserException, IOException
		{
			XMLAttributeSet attrs = new XMLAttributeSet() {
				public int getCount() { return xpp.getAttributeCount(); }
				public String getName(int i) { return xpp.getAttributeName(i); }
				public String getValue(int i) { return xpp.getAttributeValue(i); }
			};
			int eventType = xpp.getEventType();
			for (;;) {
				if(eventType == xpp.START_DOCUMENT)
					;
				else if(eventType == xpp.END_DOCUMENT)
					return;
				else if(eventType == xpp.START_TAG)
					deserializer.startElement(xpp.getNamespace(), xpp.getName(), attrs);
				else if(eventType == xpp.END_TAG)
					deserializer.endElement(xpp.getNamespace(), xpp.getName());
				else if(eventType == xpp.TEXT)
					deserializer.addText(xpp.getText());
				eventType = xpp.next();
			}
		}
	}
		
	final static class SAXHandler extends DefaultHandler {
		XMLDeSerializer deserializer;
		
		SAXHandler(XMLDeSerializer deserializer) {
			this.deserializer = deserializer;
		}

		public void startElement(String uri, String sName, String qName, Attributes attributes)
			throws SAXException
		{
			deserializer.startElement(uri, sName, new XMLAttributeSet() {
				public int getCount() { return attributes.getLength(); }
				public String getName(int i) { return attributes.getLocalName(i); }
				public String getValue(int i) { return attributes.getValue(i); }
			});
		}
		public void endElement(String uri, String sName, String qName)
			throws SAXException
		{
			deserializer.endElement(uri, sName);
		}

		public void characters(char[] ch, int start, int length) {
			deserializer.addText(new String(ch, start, length));
		}
	}



	private static Vector<String> getPathList(File f)
		throws IOException
	{
		Vector<String> l = new Vector<String>();
		File[] roots = File.listRoots();
		File r;
		r = f.getCanonicalFile();
		while(r != null && !Arrays.contains(roots,r)) {
			l.append(r.getName());
			r = r.getParentFile();
		}
		return l;
	}

	private static String matchPathLists(Vector<String> r,Vector<String> f) {
		// start at the beginning of the lists
		// iterate while both lists are equal
		String s = "";
		int i = r.size()-1;
		int j = f.size()-1;

		// first eliminate common root
		while (i >= 0 && j >= 0 && r[i].equals(f[j])) {
			i--;
			j--;
		}
		// for each remaining level in the home path, add a ..
		for(; i >= 0; i--)
			s += ".." + File.separator;
		// for each level in the file path, add the path
		for(; j>=1; j--)
			s += f.get(j) + File.separator;
		// file name
		s += f[j];
		return s;
	}

	private static String getRelativePath(File f, File home)
		throws IOException
	{
		Vector<String> homelist = getPathList(home);
		Vector<String> filelist = getPathList(f);
		String s = matchPathLists(homelist,filelist);
		return s;
	}

	private static String getRelativePath(File f)
		throws IOException
	{
		return getRelativePath(f, new File("."));
	}
}

