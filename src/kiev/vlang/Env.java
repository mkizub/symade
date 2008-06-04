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

import kiev.be.java15.JEnv;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

@ThisIsANode(lang=CoreLang)
public final class Env extends KievPackage {

	/** Hashtable of all defined and loaded classes */
	public static Hash<String>						classHashOfFails	= new Hash<String>();

	/** Root of package hierarchy */
	private static Env								root = new Env();

	/** Class/library path */
	private kiev.bytecode.Classpath				classpath;

	/** Backend environment */
	private JEnv									jenv;

	@nodeAttr public Project						project;
	
	private java.util.WeakHashMap					uuidToSymbolMap	= new java.util.WeakHashMap();
	
	private Hashtable<String,Draw_ATextSyntax>		languageSyntaxMap	= new Hashtable<String,Draw_ATextSyntax>();

	public static Env getRoot() { return root; }
	public static Project getProject() { return root.project; }
	
	/** Private class constructor -
		really there may be no instances of this class
	 */
	private Env() {
		root = this;
		this.setTypeDeclNotLoaded(false);
		new CompaundMetaType(this);
	}

	public JEnv getBackendEnv() {
		return jenv;
	}
	
	public String toString() {
		return "<root>";
	}

	public DNode resolveGlobalDNode(String qname) {
		//assert(qname.indexOf('.') < 0);
		TypeDecl pkg = Env.getRoot();
		int start = 0;
		int end = qname.indexOf('\u001f', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			TypeDecl ss = null;
			foreach (TypeDecl s; pkg.sub_decls; s.sname == nm) {
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
	
	public void registerISymbol(String uuid, ISymbol sym) {
		ISymbol old = (ISymbol)this.uuidToSymbolMap.get(uuid);
		if (old != null) {
			if (old == sym)
				return;
			Debug.assert("Registering another symbol with the same UUID ("+uuid+"):\n\t"+sym);
		}
		this.uuidToSymbolMap.put(uuid,sym);
	}
	
	public ISymbol getISymbolByUUID(String uuid) {
		return (ISymbol)this.uuidToSymbolMap.get(uuid);
	}
	
	public Struct newStruct(String sname, Struct outer, int acces, Struct variant) {
		return newStruct(sname,true,outer,acces,variant,false,null);
	}

	public Struct newStruct(String sname, boolean direct, Struct outer, int acces, Struct cl, boolean cleanup, String uuid)
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
				if (cl.uuid != uuid)
					Kiev.reportWarning(cl,"Replacing class "+sname+" with different UUID: "+cl.uuid+" != "+uuid);
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

	public KievPackage newPackage(String qname) {
		if (qname == "")
			return Env.getRoot();
		assert(qname.indexOf('.') < 0);
		int end = qname.lastIndexOf('\u001f');
		if (end < 0)
			return newPackage(qname,Env.getRoot());
		else
			return newPackage(qname.substring(end+1).intern(),newPackage(qname.substring(0,end).intern()));
	}

	public KievPackage newPackage(String sname, KievPackage outer) {
		KievPackage cl = null;
		foreach (KievPackage s; outer.sub_decls; s.sname == sname) {
			cl = s;
			break;
		}
		if (cl == null) {
			cl = (KievPackage)newStruct(sname,outer,0,new KievPackage());
			outer.members += cl;
			cl.setTypeDeclNotLoaded(false);
		}
		return cl;
	}

	public MetaTypeDecl newMetaType(Symbol<MetaTypeDecl> id, TypeDecl pkg, boolean cleanup, String uuid) {
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
			if (tdecl.uuid != uuid)
				Kiev.reportWarning(id,"Replacing class "+id+" with different UUID: "+tdecl.uuid+" != "+uuid);
			tdecl.cleanupOnReload();
			tdecl.meta.mflags = ACC_MACRO;
			tdecl.package_clazz.symbol = pkg;
			pkg.sub_decls.add(tdecl);
		}

		return tdecl;
	}

	/** Environment initialization with specified CLASSPATH
		for the compiling classes
	 */
	public void InitializeEnv(String path) {
		if (path == null) path = System.getProperty("java.class.path");
		this.classpath = new kiev.bytecode.Classpath(path);
		this.jenv = new JEnv();
		if (Kiev.project_file != null && Kiev.project_file.exists())
			this.project = loadProject(Kiev.project_file);
		if (this.project == null)
			this.project = new Project();

		//root.setPackage();
		root.addSpecialField("$GenAsserts", Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputA));
		root.addSpecialField("$GenTraces",  Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputT));
	}
	
	private void addSpecialField(String name, Type tp, ENode init) {
		foreach (Field f; this.members; f.hasName(name,true)) {
			f.init = init;
			return;
		}
		Field f = new Field(name,tp,ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		f.init = init;
		members.add(f);
	}

	public void dumpProjectFile() {
		if( Kiev.project_file == null ) return;
		dumpTextFile(getProject(), Kiev.project_file, new XmlDumpSyntax("proj").getCompiled().init());
	}

	public boolean existsTypeDecl(String qname) {
		if (qname == "") return true;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return false;
		DNode dn = resolveGlobalDNode(qname);
		if (dn instanceof TypeDecl)
			return true;
		// Check if not loaded
		return this.classpath.exists(qname.replace('\u001f','/'));

	}

	public TypeDecl loadTypeDecl(String qname, boolean fatal) {
		TypeDecl s = loadTypeDecl(qname);
		if (fatal && s == null)
			throw new RuntimeException("Cannot find TypeDecl "+qname);
		return s;
	}

	public TypeDecl loadTypeDecl(String qname) {
		if (qname == "") return Env.getRoot();
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return null;
		TypeDecl cl = (TypeDecl)resolveGlobalDNode(qname);
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

	public Draw_ATextSyntax getLanguageSyntax(String name, boolean in_project) {
		if (in_project) {
			DNode ts = Env.getRoot().resolveGlobalDNode(name);
			if (ts instanceof ATextSyntax)
				return ts.getCompiled().init();
		}
		Draw_ATextSyntax stx = languageSyntaxMap.get(name);
		if (stx != null)
			return stx;
		return loadLanguageSyntax(name);
	}
	
	public Draw_ATextSyntax loadLanguageSyntax(String name) {
		Draw_ATextSyntax dts = null;
		InputStream inp = null;
		try {
			inp = Env.class.getClassLoader().getSystemResourceAsStream(name.replace('\u001f','/')+".ser");
			ObjectInput oi = new ObjectInputStream(inp);
			dts = (Draw_ATextSyntax)oi.readObject();
			dts.init();
		} catch (IOException e) {
			System.out.println("Read error while syntax deserialization: "+e);
		} finally {
			if (inp != null)
				inp.close();
		}
		if (dts != null)
			languageSyntaxMap.put(name, dts);
		return dts;
	}

	public TypeDecl loadTypeDecl(TypeDecl cl) {
		if (!cl.isTypeDeclNotLoaded())
			return cl;
		if (cl instanceof Env)
			return Env.getRoot();
		// Load if not loaded or not resolved
		if (cl.isTypeDeclNotLoaded() && !cl.isAnonymouse()) {
			if (cl instanceof Struct)
				jenv.loadClazz((Struct)cl);
			else
				jenv.loadClazz(cl.qname());
		}
		return cl;
	}
	
	public void dumpTextFile(ASTNode node, File f, Draw_ATextSyntax stx)
		throws IOException
	{
		StringBuffer sb = new StringBuffer(1024);
		TextFormatter tf = new TextFormatter();
		tf.setHintEscapes(true);
		if (stx instanceof Draw_XmlDumpSyntax)
			tf.setShowAutoGenerated(true);
		tf.format(node, null, stx);
		Drawable dr = tf.getRootDrawable();
		TextPrinter pr = new TextPrinter(sb);
		pr.draw(dr);
		make_output_dir(f);
		FileOutputStream out = new FileOutputStream(f);
		if (stx instanceof Draw_XmlDumpSyntax) {
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

	private void make_output_dir(File f) throws IOException {
		File dir = f.getParentFile();
		if (dir != null) {
			dir.mkdirs();
			if( !dir.exists() || !dir.isDirectory() ) throw new IOException("Can't create output dir "+dir);
		}
	}
	

	public byte[] loadClazzFromClasspath(String name) {
		trace(kiev.bytecode.Clazz.traceRules,"Loading data for clazz "+name);

		byte[] data = this.classpath.read(name);
		trace(kiev.bytecode.Clazz.traceRules && data != null ,"Data for clazz "+name+" loaded");

		if (data == null || data.length == 0)
			trace(kiev.bytecode.Clazz.traceRules,"Data for clazz "+name+" not found");

		return data;
	}

	private Project loadProject(File f) {
		assert (Thread.currentThread() instanceof WorkerThread);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SAXHandler handler = new SAXHandler();
		saxParser.parse(f, handler);
		Project prj = (Project)handler.root;
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

	public FileUnit loadFromXmlFile(File f, byte[] data) {
		assert (Thread.currentThread() instanceof WorkerThread);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SAXHandler handler = new SAXHandler();
		handler.file = f;
		if (data != null)
			saxParser.parse(new ByteArrayInputStream(data), handler);
		else
			saxParser.parse(f, handler);
		foreach (DelayedTypeInfo dti; handler.delayed_types)
			dti.applay();
		ANode root = handler.root;
		if!(root instanceof FileUnit) {
			root = FileUnit.makeFile(getRelativePath(f), false);
			root.current_syntax = "stx-fmt\u001fsyntax-dump-full";
			root.members += handler.root;
		}
		//Kiev.runProcessorsOn((ASTNode)root);
		return (FileUnit)root;
	}

	public FileUnit loadFromXmlData(byte[] data, String tdname, TypeDecl pkg) {
		assert (Thread.currentThread() instanceof WorkerThread);
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		SAXHandler handler = new SAXHandler();
		handler.tdname = tdname;
		handler.pkg = pkg;
		saxParser.parse(new ByteArrayInputStream(data), handler);
		foreach (DelayedTypeInfo dti; handler.delayed_types)
			dti.applay();
		FileUnit root = (FileUnit)handler.root;
		Kiev.runProcessorsOn(root);
		return root;
	}
	
	final static class DelayedTypeInfo {
		final ANode node;
		final AttrSlot attr;
		final String signature;
		DelayedTypeInfo(ANode node, AttrSlot attr, String signature) {
			this.node = node;
			this.attr = attr;
			this.signature = signature;
		}
		void applay() {
			AType tp = AType.fromSignature(signature,false);
			if (tp != null) {
				attr.set(node,tp);
			} else {
				((TypeRef)node).signature = signature;
			}
		}
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
		Vector<DelayedTypeInfo> delayed_types = new Vector<DelayedTypeInfo>();
		
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
					FileUnit fu = FileUnit.makeFile(qname.replace('\u001f','/')+".xml", false);
					fu.scanned_for_interface_only = true;
					TypeDecl p = pkg;
					while (p != null && !p.isPackage())
						p = p.package_clazz.dnode;
					fu.srpkg.symbol = p;
					root = fu;
					TypeDecl td = (TypeDecl)Env.getRoot().resolveGlobalDNode(qname);
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
					td.meta.is_interface_only = true;
					nodes.push(td);
				}
				else if (cl_name.equals("kiev.vlang.FileUnit")) {
					if (file == null)
						file = new File(tdname.replace('\u001f','/')+".xml");
					FileUnit fu = FileUnit.makeFile(getRelativePath(file), false);
					root = fu;
					fu.current_syntax = "stx-fmt\u001fsyntax-dump-full";
					nodes.push(root);
				}
				else {
					root = (ASTNode)Class.forName(cl_name).newInstance();
					if (root instanceof DNode)
						((DNode)root).meta.is_interface_only = true;
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
				AttrSlot attr = attrs.peek();
				if (!attr.isWrittable() || cl_name.equals("kiev.vlang.SymbolRef") || cl_name.equals("kiev.vlang.Symbol") || cl_name.equals("kiev.vlang.MetaSet")) {
					AttrSlot attr = attrs.peek();
					if (attr.is_space) {
						n = (ANode)attr.typeinfo.newInstance();
					} else {
						n = (ANode)attr.get(nodes.peek());
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
				if (n instanceof DNode)
					n.meta.is_interface_only = true;
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
					//if (n instanceof Struct) {
					//	Struct s = (Struct)n;
					//	s.xmeta_type = new CompaundMetaType(s);
					//	s.xtype = new CompaundType((CompaundMetaType)s.xmeta_type, TVarBld.emptySet);
					//	//s.package_clazz.symbol = outer;
					//}
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
					else if (Enum.class.isAssignableFrom(attr.clazz)) {
						//attr.set(nodes.peek(),Enum.valueOf(attr.clazz,text.trim()));
						attr.set(nodes.peek(),attr.clazz.getMethod("valueOf",String.class).invoke(null,text.trim()));
					}
					else if (attr.clazz == Operator.class)
						attr.set(nodes.peek(),Operator.getOperatorByName(text.trim()));
					else if (Type.class.isAssignableFrom(attr.clazz)) {
						//attr.set(nodes.peek(),AType.fromSignature(text.trim()));
						ANode node = nodes.peek();
						if (node instanceof TypeRef && attr.name == "type_lnk") {
							((TypeRef)node).signature = text.trim();
						} else {
							delayed_types.append(new DelayedTypeInfo(nodes.peek(), attr, text.trim()));
						}
					}
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
			if (text.charAt(text.length()-1) == 'L' || text.charAt(text.length()-1) == 'l') {
				text = text.substring(0,text.length()-1);
				if (text.length() == 0)
					return 0L; // 0L 
			}
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

