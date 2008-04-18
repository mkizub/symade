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

import kiev.fmt.*;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import kiev.be.java15.Bytecoder;
import kiev.be.java15.Attr;
import kiev.be.java15.JStruct;
import kiev.be.java15.JEnv;

import static kiev.vlang.ProjectFileType.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public enum ProjectFileType {
	CLASS,
	INTERFACE,
	ENUM,
	SYNTAX,
	PACKAGE,
	METATYPE,
	FORMAT
}

@node(lang=void)
public final class ProjectFile extends ASTNode {

	@virtual typedef This  = ProjectFile;

	public ProjectFileType		type;
	public String				qname;
	public KString				bname;
	public File					file;
	public boolean				bad;

	public ProjectFile() {}
}


/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

@node(lang=CoreLang)
public class Env extends KievPackage {

	/** Hashtable of all defined and loaded classes */
	public static Hash<String>						classHashOfFails	= new Hash<String>();

	/** Hashtable for project file (class name + file name) */
	public static Hashtable<String,ProjectFile>	projectHash = new Hashtable<String,ProjectFile>();

	/** Root of package hierarchy */
	private static Env								root = new Env();

	/** Compiler properties */
	public static Properties						props = System.getProperties();
	
	/** Class/library path */
	public static kiev.bytecode.Classpath			classpath;

	/** Backend environment */
	public static JEnv								jenv;

	@att public DirUnit								rdir;
	
	public static Env getRoot() { return root; }
	
	/** Private class constructor -
		really there may be no instances of this class
	 */
	private Env() {
		root = this;
		new CompaundMetaType(this);
	}

	public static void setProperty(String prop, String value) {
		props.put(prop,value);
	}

	public static void removeProperty(String prop) {
		props.remove(prop);
	}

	public static String getProperty(String prop) {
		return props.getProperty(prop);
	}
	
	public String toString() {
		return "<root>";
	}

	public static DNode resolveGlobalDNode(String qname) {
		//assert(qname.indexOf('.') < 0);
		Struct pkg = Env.getRoot();
		int start = 0;
		int end = qname.indexOf('\u001f', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			Struct ss = null;
			foreach (Struct s; pkg.sub_decls; s.sname == nm) {
				ss = s;
				break;
			}
			if (ss == null)
				return null;
			pkg = ss;
			start = end+1;
			end = qname.indexOf('\u001f', start);
		}
		String nm = qname.substring(start).intern();
		foreach (DNode dn; pkg.sub_decls; dn.sname == nm)
			return dn;
		return null;
	}
	
	public static Struct newStruct(String sname, Struct outer, int acces, Struct variant) {
		return newStruct(sname,true,outer,acces,variant,false,null);
	}

	public static Struct newStruct(String sname, boolean direct, Struct outer, int acces, Struct cl, boolean cleanup, String uuid)
	{
		assert(outer != null);
		Struct bcl = null;
		if (direct && sname != null) {
			foreach (Struct s; outer.sub_decls; s.sname == sname) {
				bcl = s;
				break;
			}
		}
		if( bcl != null ) {
			assert (bcl.getClass() == cl.getClass());
			cl = (Struct)bcl;
			if( cleanup ) {
				if (cl.hasUUID() && cl.getUUID() != uuid)
					Kiev.reportWarning(cl,"Replacing class "+sname+" with different UUID: "+cl.getUUID()+" != "+uuid);
				cl.cleanupOnReload();
				cl.meta.mflags = acces;
				cl.package_clazz.symbol = outer;
				outer.sub_decls += cl;
			}
			outer.addSubStruct((Struct)cl);
			return cl;
		}
		cl.initStruct(sname,outer,acces);
		outer.addSubStruct(cl);
		return cl;
	}

	public static Struct newPackage(String qname) {
		if (qname == "")
			return Env.getRoot();
		assert(qname.indexOf('.') < 0);
		int end = qname.lastIndexOf('\u001f');
		if (end < 0)
			return newPackage(qname,Env.getRoot());
		else
			return newPackage(qname.substring(end+1).intern(),newPackage(qname.substring(0,end).intern()));
	}

	public static Struct newPackage(String sname, Struct outer) {
		assert( outer instanceof KievPackage );
		Struct cl = null;
		foreach (Struct s; outer.sub_decls; s.sname == sname) {
			cl = s;
			break;
		}
		if (cl == null) {
			cl = newStruct(sname,outer,0,new KievPackage());
			outer.members += cl;
			cl.setTypeDeclNotLoaded(false);
		}
		return cl;
	}

	public static MetaTypeDecl newMetaType(Symbol<MetaTypeDecl> id, Struct pkg, boolean cleanup, String uuid) {
		if (pkg == null)
			pkg = Env.getRoot();
		assert (pkg.isPackage());
		MetaTypeDecl tdecl = null;
		foreach (MetaTypeDecl pmt; pkg.sub_decls; pmt.sname == id.sname) {
			tdecl = pmt;
			break;
		}
		if (tdecl == null) {
			tdecl = new MetaTypeDecl();
			tdecl.pos = id.pos;
			tdecl.sname = id.sname;
			tdecl.package_clazz.symbol = pkg;
			tdecl.meta.mflags = ACC_MACRO;
			pkg.sub_decls.add(tdecl);
		}
		else if( cleanup ) {
			if (tdecl.hasUUID() && tdecl.getUUID() != uuid)
				Kiev.reportWarning(id,"Replacing class "+id+" with different UUID: "+tdecl.getUUID()+" != "+uuid);
			tdecl.cleanupOnReload();
			tdecl.meta.mflags = ACC_MACRO;
			tdecl.package_clazz.symbol = pkg;
			pkg.sub_decls.add(tdecl);
		}

		return tdecl;
	}

	/** Default environment initialization */
	public static void InitializeEnv() {
		if (Env.getRoot().rdir == null)
			Env.getRoot().rdir = DirUnit.makeRootDir();
	    InitializeEnv(System.getProperty("java.class.path"));
	}

	/** Environment initialization with specified CLASSPATH
		for the compiling classes
	 */
	public static void InitializeEnv(String path) {
		if (Env.getRoot().rdir == null)
			Env.getRoot().rdir = DirUnit.makeRootDir();
		if (path == null) path = System.getProperty("java.class.path");
		classpath = new kiev.bytecode.Classpath(path);
		jenv = new JEnv();
		if( Kiev.project_file != null && Kiev.project_file.exists() ) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(Kiev.project_file));
				while(in.ready()) {
					String line = in.readLine();
					if( line==null ) continue;
					String[] args = line.trim().split("\\s+");
					if (args.length == 0)
						continue;
					int idx = 0;
					ProjectFile pf = new ProjectFile();
					pf.type = ProjectFileType.fromString(args[idx++]);
					switch (pf.type) {
					case CLASS:
					case INTERFACE:
					case ENUM:
					case SYNTAX:
					case PACKAGE:
						pf.qname = args[idx++].intern();
						pf.bname = KString.from(args[idx++]);
						pf.file = new File(args[idx++].replace('/',File.separatorChar));
						break;
					case METATYPE:
					case FORMAT:
						pf.qname = args[idx++].intern();
						pf.file = new File(args[idx++].replace('/',File.separatorChar));
						break;
					}
					for (int i=idx; i < args.length; i++) {
						if (args[i].equals("bad"))
							pf.bad = true;
					}
					projectHash.put(pf.qname, pf);
				}
				in.close();
			} catch (EOFException e) {
				// OK
			} catch (IOException e) {
				Kiev.reportWarning("Error while project file reading: "+e);
			}
		}

		//root.setPackage();
		root.addSpecialField("$GenAsserts", Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputA));
		root.addSpecialField("$GenTraces",  Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputT));
	}
	
	private void addSpecialField(String name, Type tp, ENode init) {
		foreach (Field f; getAllFields(); f.hasName(name,true)) {
			f.init = init;
			return;
		}
		Field f = new Field(name,tp,ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		f.init = init;
		members.add(f);
	}

	public static void dumpProjectFile() {
		if( Kiev.project_file == null ) return;
		try {
			PrintStream out = new PrintStream(new FileOutputStream(Kiev.project_file));
			Vector<String> strs = new Vector<String>();
			for(Enumeration<String> e=projectHash.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				ProjectFile pf = projectHash.get(key);
				DNode cl = resolveGlobalDNode(pf.qname);
				if (cl != null && cl.isBad()) pf.bad = true;
				if (cl instanceof Struct) {
					if      (cl.isSyntax())		pf.type = SYNTAX;
					else if (cl.isPackage())	pf.type = PACKAGE;
					else if (cl.isInterface())	pf.type = INTERFACE;
					else if (cl.isEnum())		pf.type = ENUM;
					else						pf.type = CLASS;
					strs.append(pf.type+" "+pf.qname+" "+pf.bname+" "+pf.file.getPath().replace(File.separatorChar,'/')+(pf.bad?" bad":""));
				}
				else if (cl instanceof TypeDecl) {
					pf.type = METATYPE;
					strs.append(pf.type+" "+pf.qname+" "+pf.file.getPath().replace(File.separatorChar,'/')+(pf.bad?" bad":""));
				}
				else if (cl instanceof ATextSyntax) {
					pf.type = FORMAT;
					strs.append(pf.type+" "+pf.qname+" "+pf.file.getPath().replace(File.separatorChar,'/')+(pf.bad?" bad":""));
				}
			}
			String[] sarr = (String[])strs;
			java.util.Arrays.sort(sarr);
			foreach(String s; sarr) out.println(s);
		} catch (IOException e) {
			Kiev.reportWarning("Error while project file writing: "+e);
		}
	}

	public static void createProjectInfo(GlobalDNode dn, String f) {
		String qname = dn.qname();
		if (qname == null || qname == "")
			return;
		ProjectFile pf = projectHash.get(qname);
		if( pf == null ) {
			ProjectFile pf = new ProjectFile();
			pf.qname = qname;
			pf.file = new File(f);
			projectHash.put(qname,pf);
		}
		else {
			if( !pf.file.getName().equals(f) )
				pf.file = new File(f);
		}
		setProjectInfo(dn, false);
	}

	public static void setProjectInfo(GlobalDNode dn, boolean good) {
		String qname = dn.qname();
		if (qname == null)
			return;
		ProjectFile pf = projectHash.get(qname);
		if (pf != null) {
			if (dn instanceof Struct)
				pf.bname = dn.bytecode_name;
			pf.bad = !good;
			if (dn instanceof Struct) {
				if      (dn.isSyntax())		pf.type = SYNTAX;
				else if (dn.isPackage())	pf.type = PACKAGE;
				else if (dn.isInterface())	pf.type = INTERFACE;
				else if (dn.isEnum())		pf.type = ENUM;
				else						pf.type = CLASS;
			}
			else if (dn instanceof TypeDecl) {
				pf.type = METATYPE;
			}
			else if (dn instanceof ATextSyntax) {
				pf.type = FORMAT;
			}
		}
	}

	public static boolean existsStruct(String qname) {
		if (qname == "") return true;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return false;
		Struct cl = (Struct)resolveGlobalDNode(qname);
		if (cl != null)
			return true;
		// check class in the project
		if (Env.projectHash.get(qname) != null)
			return true;
		// Check if not loaded
		return Env.classpath.exists(qname.replace('\u001f','/'));

	}

	public static TypeDecl loadTypeDecl(String qname, boolean fatal) {
		TypeDecl s = loadTypeDecl(qname);
		if (fatal && s == null)
			throw new RuntimeException("Cannot find TypeDecl "+qname);
		return s;
	}

	public static TypeDecl loadTypeDecl(String qname) {
		if (qname == "") return Env.getRoot();
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return null;
		TypeDecl cl = (TypeDecl)resolveGlobalDNode(qname);
		// Try to load from project file (scan sources) or from .xml API dump
		if (cl == null && !Compiler.makeall_project && Env.projectHash.get(qname) != null)
			cl = loadTypeDeclFromProject(qname);
		//if (cl == null)
		//	cl = loadTypeDeclFromAPIDump(qname);
		// Load if not loaded or not resolved
		if (cl == null)
			cl = jenv.loadClazz(qname);
		else if (cl.isTypeDeclNotLoaded() && !cl.isAnonymouse()) {
			if (cl instanceof Struct)
				cl = jenv.loadClazz((Struct)cl);
			else
				cl = jenv.loadClazz(cl.qname());
		}
		if (cl == null)
			classHashOfFails.put(qname);
		return cl;
	}

	public static TypeDecl loadTypeDecl(TypeDecl cl) {
		if (!cl.isTypeDeclNotLoaded())
			return cl;
		if (cl instanceof Env)
			return Env.getRoot();
		// Try to load from project file (scan sources) or from .xml API dump
		if (!Compiler.makeall_project && Env.projectHash.get(cl.qname()) != null)
			loadTypeDeclFromProject(cl.qname());
		// Load if not loaded or not resolved
		if (cl.isTypeDeclNotLoaded() && !cl.isAnonymouse()) {
			if (cl instanceof Struct)
				jenv.loadClazz((Struct)cl);
			else
				jenv.loadClazz(cl.qname());
		}
		return cl;
	}
	
	private static TypeDecl loadTypeDeclFromProject(String qname) {
		ProjectFile pf = Env.projectHash.get(qname);
		if (pf == null || pf.file == null)
			return null;
		File file = pf.file;
		if (!file.exists() || !file.canRead())
			return null;
		String filename = file.toString();
		String cur_file = Kiev.getCurFile();
		Kiev.setCurFile(filename);
		Parser k = Kiev.k;
		long curr_time = 0L, diff_time = 0L;
		try {
			diff_time = curr_time = System.currentTimeMillis();
			java.io.InputStreamReader file_reader = null;
			char[] file_chars = new char[8196];
			int file_sz = 0;
			try {
				file_reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
				for (;;) {
					int r = file_reader.read(file_chars, file_sz, file_chars.length-file_sz);
					if (r < 0)
						break;
					file_sz += r;
					if (file_sz >= file_chars.length) {
						char[] tmp = new char[file_chars.length + 8196];
						System.arraycopy(file_chars, 0, tmp, 0, file_chars.length);
						file_chars = tmp;
					}
				}
			} finally {
				if (file_reader != null) file_reader.close();
			}
			java.io.CharArrayReader bis = new java.io.CharArrayReader(file_chars, 0, file_sz);
			Kiev.k.interface_only = true;
			diff_time = curr_time = System.currentTimeMillis();
			Kiev.k.ReInit(bis);
			FileUnit fu = Kiev.k.FileUnit(filename);
			fu.current_syntax = "stx-fmt\u001fsyntax-for-java";
			fu.scanned_for_interface_only = true;
			diff_time = System.currentTimeMillis() - curr_time;
			bis.close();
			if( Kiev.verbose )
				Kiev.reportInfo("Scanned file   "+filename,diff_time);
			try {
				Kiev.runProcessorsOn(fu);
				//Kiev.lockNodeTree(fu);
			} catch(Exception e ) {
				Kiev.reportError(e);
			}
		} catch ( ParseException e ) {
			Kiev.reportError(e);
		} catch ( ParseError e ) {
			System.out.println("Error while scanning input file:"+filename+":"+e);
		} finally {
			k.interface_only = Kiev.interface_only;
			Kiev.setCurFile(cur_file);
		}
		TypeDecl td = (TypeDecl)Env.resolveGlobalDNode(qname);
		return td;
	}

	public static void dumpTextFile(ASTNode node, File f, ATextSyntax stx)
		throws IOException
	{
		StringBuilder sb = new StringBuilder(1024);
		TextFormatter tf = new TextFormatter();
		tf.setHintEscapes(true);
		if (stx instanceof XmlDumpSyntax)
			tf.setShowAutoGenerated(true);
		tf.format(node, null, stx);
		Drawable dr = tf.getRootDrawable();
		TextPrinter pr = new TextPrinter(sb);
		pr.draw(dr);
		make_output_dir(f);
		FileOutputStream out = new FileOutputStream(f);
		if (stx instanceof XmlDumpSyntax) {
			out.write("<?xml version='1.1' encoding='UTF-8' standalone='yes'?>\n".getBytes("UTF-8"));
			out.write("<!--\n".getBytes("UTF-8"));
			out.write(" Copyright (c) 2005-2007 UAB \"MAKSINETA\".\n".getBytes("UTF-8"));
			out.write(" All rights reserved. This program and the accompanying materials\n".getBytes("UTF-8"));
			out.write(" are made available under the terms of the Common Public License Version 1.0\n".getBytes("UTF-8"));
			out.write(" which accompanies this distribution, and is available at\n".getBytes("UTF-8"));
			out.write(" http://www.eclipse.org/legal/cpl-v10.html\n".getBytes("UTF-8"));
			out.write(" \n".getBytes("UTF-8"));
			out.write(" Contributors:\n".getBytes("UTF-8"));
			out.write("     \"Maxim Kizub\" mkizub@symade.com - initial design and implementation\n".getBytes("UTF-8"));
			out.write("-->\n".getBytes("UTF-8"));
		}
		out.write(sb.toString().getBytes("UTF-8"));
		out.close();
	}

	private static void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}
	
	public static FileUnit loadFromXmlFile(File f) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SAXHandler handler = new SAXHandler();
		handler.file = f;
		saxParser.parse(f, handler);
		ANode root = handler.root;
		if!(root instanceof FileUnit) {
			root = FileUnit.makeFile(getRelativePath(f));
			root.current_syntax = "stx-fmt\u001fsyntax-dump-full";
			root.members += handler.root;
		}
		return (FileUnit)root;
	}
	
	public static FileUnit loadFromXmlData(byte[] data, String tdname, TypeDecl pkg) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SAXHandler handler = new SAXHandler();
		handler.tdname = tdname;
		handler.pkg = pkg;
		saxParser.parse(new ByteArrayInputStream(data), handler);
		FileUnit root = (FileUnit)handler.root;
		Kiev.runProcessorsOn(root);
		return root;
	}
	
	final static class SAXHandler extends DefaultHandler {
		ASTNode root;
		File file;
		TypeDecl pkg;
		String tdname;
		boolean expect_attr;
		int ignore_count;
		Stack<ANode> nodes = new Stack<ANode>();
		Stack<AttrSlot> attrs = new Stack<AttrSlot>();
		String text;
		public void startElement(String uri, String sName, String qName, Attributes attributes)
			throws SAXException
		{
			if (ignore_count > 0) {
				ignore_count++;
				return;
			}
			if (root == null) {
				assert (!expect_attr);
				assert (qName.equals("a-node"));
				String cl_name = attributes.getValue("class");
				if (pkg != null) {
					String qname;
					if (pkg instanceof Env)
						qname = tdname;
					else
						qname = pkg.qname() + '\u001f' + tdname;
					FileUnit fu = FileUnit.makeFile(qname.replace('\u001f','/')+".xml");
					fu.scanned_for_interface_only = true;
					fu.srpkg.symbol = pkg;
					root = fu;
					TypeDecl td = (TypeDecl)Env.resolveGlobalDNode(qname);
					if (td != null) {
						assert(td.getClass().getName().equals(cl_name));
						td.cleanupOnReload();
					} else {
						td = (TypeDecl)Class.forName(cl_name).newInstance();
					}
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
					nodes.push(td);
				}
				else if (cl_name.equals("kiev.vlang.FileUnit")) {
					FileUnit fu = FileUnit.makeFile(getRelativePath(file));
					root = fu;
					fu.current_syntax = "stx-fmt\u001fsyntax-dump-full";
					nodes.push(root);
				}
				else {
					root = (ASTNode)Class.forName(cl_name).newInstance();
					nodes.push(root);
				}
				expect_attr = true;
				//System.out.println("push root");
				return;
			}
			if (qName.equals("a-node")) {
				assert (!expect_attr);
				String cl_name = attributes.getValue("class");
				ANode n;
				if (cl_name.equals("kiev.vlang.SymbolRef") || cl_name.equals("kiev.vlang.Symbol") || cl_name.equals("kiev.vlang.MetaSet")) {
					AttrSlot attr = attrs.peek();
					if (attr.is_space) {
						n = (ANode)attr.typeinfo.newInstance();
					} else {
						n = attr.get(nodes.peek());
						if (n == null)
							n = (ANode)attr.typeinfo.newInstance();
					}
				}
				else if (cl_name.equals("kiev.parser.ASTOperatorAlias")) {
					n = new kiev.parser.ASTOperatorAlias();
				}
				else {
					try {
						n = (ANode)Class.forName(cl_name).newInstance();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						n = null;
					}
				}
				//System.out.println("push node "+nodes.length);
				nodes.push(n);
				expect_attr = true;
				return;
			}
			assert (expect_attr);
			ANode n = nodes.peek();
			foreach (AttrSlot attr; n.values(); attr.name.equals(qName)) {
				//System.out.println("push attr "+attr.name);
				attrs.push(attr);
				expect_attr = false;
				return;
			}
			//throw new SAXException("Attribute '"+qName+"' not found in "+n.getClass());
			System.out.println("Attribute '"+qName+"' not found in "+n.getClass());
			ignore_count = 1;
		}
		public void endElement(String uri, String sName, String qName)
			throws SAXException
		{
			if (ignore_count > 0) {
				ignore_count--;
				return;
			}
			if (expect_attr) {
				assert(qName.equals("a-node"));
				ANode n = nodes.pop();
				if (n instanceof TypeDecl) {
					n.setTypeDeclNotLoaded(false);
					if (n instanceof Struct) {
						Struct s = (Struct)n;
						s.xmeta_type = new CompaundMetaType(s);
						s.xtype = new CompaundType((CompaundMetaType)s.xmeta_type, TVarBld.emptySet);
						//s.package_clazz.symbol = outer;
					}
				}
				if (nodes.isEmpty()) {
					//System.out.println("pop  root");
					expect_attr = false;
					return;
				}
				//System.out.println("pop  node "+nodes.length);
				AttrSlot attr = attrs.peek();
				if (attr.is_space) {
					SpaceAttrSlot<ANode> sa = (SpaceAttrSlot<ANode>)attr;
					//System.out.println("add node to "+attr.name);
					sa.add(nodes.peek(),n);
				} else {
					//System.out.println("set node to "+attr.name);
					if (!n.isAttached())
						attr.set(nodes.peek(),n);
				}
				expect_attr = false;
			} else {
				AttrSlot attr = attrs.pop();
				//System.out.println("pop  attr "+attr.name);
				if (text != null) {
					//System.out.println("set text: "+text);
					if (attr.clazz == String.class)
						attr.set(nodes.peek(),text);
					else if (attr.clazz == Boolean.TYPE)
						attr.set(nodes.peek(),Boolean.valueOf(text.trim()));
					else if (attr.clazz == Integer.TYPE)
						attr.set(nodes.peek(),Integer.valueOf((int)parseLong(text)));
					else if (attr.clazz == Byte.TYPE)
						attr.set(nodes.peek(),Byte.valueOf((byte)parseLong(text)));
					else if (attr.clazz == Short.TYPE)
						attr.set(nodes.peek(),Short.valueOf((short)parseLong(text)));
					else if (attr.clazz == Long.TYPE)
						attr.set(nodes.peek(),Long.valueOf(parseLong(text)));
					else if (attr.clazz == Float.TYPE)
						attr.set(nodes.peek(),Float.valueOf(text.trim()));
					else if (attr.clazz == Double.TYPE)
						attr.set(nodes.peek(),Double.valueOf(text.trim()));
					else if (attr.clazz == Character.TYPE)
						attr.set(nodes.peek(),Character.valueOf(text.trim().charAt(0)));
					else if (Enum.class.isAssignableFrom(attr.clazz))
						attr.set(nodes.peek(),Enum.valueOf(attr.clazz,text.trim()));
					else if (attr.clazz == Operator.class)
						attr.set(nodes.peek(),Operator.getOperatorByName(text.trim()));
					else if (Type.class.isAssignableFrom(attr.clazz))
						attr.set(nodes.peek(),AType.fromSignature(text.trim()));
					else
						//throw new SAXException("Attribute '"+attr.name+"' of "+nodes.peek().getClass()+" uses unsupported "+attr.clazz);
						System.out.println("Attribute '"+attr.name+"' of "+nodes.peek().getClass()+" uses unsupported "+attr.clazz);
					text = null;
				}
				expect_attr = true;
			}
		}
		private long parseLong(String text) {
			text = text.trim();
			int radix;
			if( text.startsWith("0x") || text.startsWith("0X") ) { text = text.substring(2); radix = 16; }
			else if( text.startsWith("0") && text.length() > 1 ) { text = text.substring(1); radix = 8; }
			else { radix = 10; }
			long l = ConstExpr.parseLong(text,radix);
			return l;
		}
		public void characters(char[] ch, int start, int length) {
			if (ignore_count > 0 || expect_attr || attrs.length <= 0)
				return;
			AttrSlot attr = attrs.peek();
			if (ANode.class.isAssignableFrom(attr.clazz))
				return;
			if (text == null)
				text = new String(ch, start, length);
			else
				text += new String(ch, start, length);
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

	public static String getRelativePath(File f, File home)
		throws IOException
	{
		Vector<String> homelist = getPathList(home);
		Vector<String> filelist = getPathList(f);
		String s = matchPathLists(homelist,filelist);
		return s;
	}

	public static String getRelativePath(File f)
		throws IOException
	{
		return getRelativePath(f, new File("."));
	}
}

