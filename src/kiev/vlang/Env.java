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
				cl.mflags = acces;
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

	public MetaTypeDecl newMetaType(Symbol<MetaTypeDecl> id, ComplexTypeDecl pkg, boolean cleanup, String uuid) {
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
			tdecl.mflags = ACC_MACRO;
			pkg.sub_decls.add(tdecl);
		}
		else if( cleanup ) {
			if (tdecl.uuid != uuid)
				Kiev.reportWarning(id,"Replacing class "+id+" with different UUID: "+tdecl.uuid+" != "+uuid);
			tdecl.cleanupOnReload();
			tdecl.mflags = ACC_MACRO;
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
			this.project = DumpUtils.loadProject(Kiev.project_file);
		if (this.project == null)
			this.project = new Project();

		//root.setPackage();
		root.addSpecialField("$GenAsserts", Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputA));
		root.addSpecialField("$GenTraces",  Type.tpBoolean, new ConstBoolExpr(Kiev.debugOutputT));
	}
	
	private void addSpecialField(String name, Type tp, ENode init) {
		foreach (Field f; this.members; f.hasName(name)) {
			f.init = init;
			return;
		}
		Field f = new Field(name,tp,ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		f.init = init;
		members.add(f);
	}

	public void dumpProjectFile() {
		if( Kiev.project_file == null ) return;
		DumpUtils.dumpToXMLFile("proj", getProject(), Kiev.project_file);
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
		if (qname.length() == 0) return Env.getRoot();
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
	
	public byte[] loadClazzFromClasspath(String name) {
		trace(kiev.bytecode.Clazz.traceRules,"Loading data for clazz "+name);

		byte[] data = this.classpath.read(name);
		trace(kiev.bytecode.Clazz.traceRules && data != null ,"Data for clazz "+name+" loaded");

		if (data == null || data.length == 0)
			trace(kiev.bytecode.Clazz.traceRules,"Data for clazz "+name+" not found");

		return data;
	}

}

