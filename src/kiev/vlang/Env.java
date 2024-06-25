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
import syntax kiev.Syntax;

import kiev.dump.DumpFactory;
import kiev.dump.XMLDumpFilter;

/**
 * @author Maxim Kizub
 * @version $Revision: 299 $
 *
 */

public interface BEndEnvFactory {
	public BEndEnv makeBEndEnv(Env env);
}
public interface BEndEnv {
	public DNode actuallyLoadDecl(String qname);
	public Struct actuallyLoadDecl(Struct cl);
	public void generateFile(FileUnit fu);
	public void backendCleanup(ASTNode node);
}

/** Class Env is a static class that implements global
	static methods and data for kiev compiler
 */

public final class Env {

	/** Root of package hierarchy */
	//private static Env[]							allEnvironments = new Env[0];
	
	/** Hashtable of all defined and loaded classes */
	private Hash<String>							classHashOfFails;

	/** Class/library path */
	private kiev.bytecode.Classpath					classpath;

	/** Type environment */
	public StdTypes									tenv;

	/** Backend environment */
	private BEndEnv									benv;

	public Project									proj;
	
	public KievRoot									root;
	
	public CoreFuncs								coreFuncs;
	
	private SymUUIDHash	uuidToSymbolMap	= new SymUUIDHash();
	
	private AbstractProcessor[]	feProcessors;
	private AbstractProcessor[]	vfProcessors;
	private AbstractProcessor[]	meProcessors;
	private AbstractProcessor[]	beProcessors;

	public static Env getEnv() {
		WorkerThread wt = (WorkerThread)Thread.currentThread();
		return wt.theEnv;
		
	}
	public static Project getProject() {
		WorkerThread wt = (WorkerThread)Thread.currentThread();
		return wt.theEnv.proj;
	}
	
	public static synchronized Env createEnv() {
		Env env = new Env();
		return env;
	}

	/** Private class constructor -
		really there may be no instances of this class
	 */
	private Env() {
		this.classHashOfFails = new Hash<String>();
	}

	public StdTypes           getTypeEnv()            { return tenv; }
	public BEndEnv             getBackendEnv()         { return benv; }
	public AbstractProcessor[]  getFEProcessors()       { return feProcessors; }
	public AbstractProcessor[]  getVFProcessors()       { return vfProcessors; }
	public AbstractProcessor[] getMEProcessors()       { return meProcessors; }
	public AbstractProcessor[] getBEProcessors()       { return beProcessors; }
	
	public void cleanupBackendEnv() {
		this.benv = null;
	}

	public void cleanupHashOfFails() {
		classHashOfFails.clear();
	}
	
	public boolean isInHashOfFails(String name) {
		return classHashOfFails.get(name) != null;
	}
	
	public void addToHashOfFails(String name) {
		return classHashOfFails.put(name);
	}
	
	public String toString() {
		return "<root>";
	}
	
	private void addCorePlugin(String name, String clazz) {
		name = "symade.plugin."+name;
		if (System.getProperty(name) != null)
			return;
		System.setProperty(name, clazz);
	}
	
	@unerasable
	private AbstractProcessor[] collectProcessors(String stage, ProcessorDescr[][] processors, int proc_id) {
		for (int i=0; i < PluginDescr.stages.length; i++) {
			if (!PluginDescr.stages[i].equals(stage))
				continue;
			Vector<AbstractProcessor> v = new Vector<AbstractProcessor>();
			foreach (ProcessorDescr pd; processors[i]) {
				AbstractProcessor p = pd.makeProcessor(this, proc_id++);
				if (p != null)
					v.append(p);
			}
			return v.toArray();
		}
		throw new Error("Unknown stage "+stage);
	}

	/** Environment initialization with specified CLASSPATH
		for the compiling classes
	 */
	public void InitializeEnv(String path) {
		if (this.root != null)
			return;
		this.root = new KievRoot();
		this.root.symbol.setUUID(this, "7067fac2-59db-33f6-9094-60b0aff4ad95");
		if (path == null) path = System.getProperty("java.class.path");
		this.classpath = new kiev.bytecode.Classpath(path);
		this.tenv = new StdTypes(this);
		this.coreFuncs = new CoreFuncs(this);
		BEndEnvFactory benv_factory = (BEndEnvFactory)Class.forName("kiev.be.java15.JEnvFactory").newInstance();
		this.benv = benv_factory.makeBEndEnv(this);
		if (Kiev.project_file != null && Kiev.project_file.exists()) {
			Project prj = (Project)DumpFactory.getXMLDumper().loadProject(this, Kiev.project_file);
			this.proj = prj;
			prj.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n instanceof CompilationUnit) {
						CompilationUnit cu = (CompilationUnit)n;
						if (cu instanceof FileUnit)
							cu.is_project_file = true;
							if (prj.compilationUnits.indexOf(cu) < 0)
								prj.compilationUnits += cu;
						return false;
					}
					return true;
				}
			});
		}
		if (this.proj == null)
			this.proj = new Project();

		proj.thisPar = new LVar(0,Constants.nameThis,getTypeEnv().tpVoid,Var.PARAM_THIS,Constants.ACC_FINAL|Constants.ACC_FORWARD|Constants.ACC_SYNTHETIC);
		this.addSpecialField("$GenAsserts", getTypeEnv().tpBoolean, new ConstBoolExpr(Kiev.debugOutputA));
		this.addSpecialField("$GenTraces",  getTypeEnv().tpBoolean, new ConstBoolExpr(Kiev.debugOutputT));

		addCorePlugin("kiev", "kiev.transf.KievPlugin");
		addCorePlugin("inner", "kiev.transf.InnerPlugin");
		addCorePlugin("pizza", "kiev.transf.PizzaPlugin");
		addCorePlugin("vnode", "kiev.transf.VNodePlugin");
		addCorePlugin("virt-fld", "kiev.transf.VirtFldPlugin");
		addCorePlugin("pack-fld", "kiev.transf.PackFldPlugin");
		addCorePlugin("enum", "kiev.transf.EnumPlugin");
		addCorePlugin("view", "kiev.transf.ViewPlugin");
		addCorePlugin("logic", "kiev.transf.LogicPlugin");
		addCorePlugin("macro", "kiev.transf.MacroPlugin");
		ProcessorDescr[][] stages = PluginDescr.loadAllPlugins();
		
		int proc_id = 0;
		{
			//processors.append(new KievFE_Pass1(this,proc_id++));
			//processors.append(new KievFE_Pass2(this,proc_id++));
			//processors.append(new KievFE_MetaDecls(this,proc_id++));
			//processors.append(new KievFE_MetaDefaults(this,proc_id++));
			//processors.append(new KievFE_MetaValues(this,proc_id++));
			//processors.append(new KievFE_Pass3(this,proc_id++));
			//processors.append(new PizzaFE_Pass3(this,proc_id++));
			//processors.append(new VNodeFE_Pass3(this,proc_id++));
			//processors.append(new VirtFldFE_GenMembers(this,proc_id++));
			//processors.append(new EnumFE_GenMembers(this,proc_id++));
			//processors.append(new ViewFE_GenMembers(this,proc_id++));
			//processors.append(new VNodeFE_GenMembers(this,proc_id++));
			//processors.append(new KievFE_PreResolve(this,proc_id++));
			//processors.append(new KievFE_MainResolve(this,proc_id++));
			//this.feProcessors = processors.toArray();
			this.feProcessors = collectProcessors("fe", stages, proc_id);
			proc_id += this.feProcessors.length;
		}
		
		{
			//processors.append(new VNodeFE_Verify(this,proc_id++));
			//processors.append(new PackedFldFE_Verify(this,proc_id++));
			//this.vfProcessors = processors.toArray();
			this.vfProcessors = collectProcessors("fv", stages, proc_id);
			proc_id += this.vfProcessors.length;
		}
		
		{
			//processors.append(new KievME_DumpAPI(this,proc_id++));
			//processors.append(new KievME_RuleGenartion(this,proc_id++));
			//processors.append(new KievME_PreGenartion(this,proc_id++));
			//processors.append(new PackedFldME_PreGenerate(this,proc_id++));
			//processors.append(new VirtFldME_PreGenerate(this,proc_id++));
			//processors.append(new PizzaME_PreGenerate(this,proc_id++));
			//processors.append(new ViewME_PreGenerate(this,proc_id++));
			//processors.append(new VNodeME_PreGenerate(this,proc_id++));
			//processors.append(new InnerBE_PreGenartion(this,proc_id++));
			//this.meProcessors = processors.toArray();
			this.meProcessors = collectProcessors("me", stages, proc_id);
			proc_id += this.meProcessors.length;
		}

		{
			//processors.append(new RewriteME_Rewrite(this,proc_id++));
			//processors.append(new InnerBE_Rewrite(this,proc_id++));
			//processors.append(new KievBE_Resolve(this,proc_id++));
			//processors.append(new VNodeBE_FixResolve(this,proc_id++));
			//processors.append(new VirtFldBE_Rewrite(this,proc_id++));
			//processors.append(new KievBE_Generate(this,proc_id++));
			////processors.append(new ExportBE_Generate(this,proc_id++));
			//processors.append(new KievBE_Cleanup(this,proc_id++));
			//this.beProcessors = processors.toArray();
			this.beProcessors = collectProcessors("be", stages, proc_id);
			proc_id += this.beProcessors.length;
		}
		//this.beCleanup = new KievBE_Cleanup(this,proc_id++);
	}
	
	private void addSpecialField(String name, Type tp, ENode init) {
		foreach (Field f; root.pkg_members; f.sname == name) {
			f.init = init;
			return;
		}
		Field f = new Field(name,tp,Constants.ACC_PUBLIC|Constants.ACC_STATIC|Constants.ACC_FINAL|Constants.ACC_SYNTHETIC);
		f.init = init;
		root.pkg_members.add(f);
	}

	public Symbol makeGlobalSymbol(String qname) {
		Symbol sym = null;
		Symbol psym = root.symbol;
		int s = 0;
		int e = qname.indexOf('·');
		for (;;) {
			String name = e < 0 ? qname.substring(s).intern() : qname.substring(s,e).intern();
			sym = null;
			foreach (Symbol sub; psym.sub_symbols; sub.sname == name) {
				sym = sub;
				break;
			}
			if (sym == null) {
				sym = new Symbol(name);
				psym.sub_symbols += sym;
			}
			psym = sym;
			if (e < 0)
				break;
			s = e+1;
			e = qname.indexOf('·', s);
		}
		return sym;
	}
	
	public DNode resolveGlobalDNode(String qname) {
		//assert(qname.indexOf('.') < 0);
		DNode pkg = (DNode)root;
		int start = 0;
		int end = qname.indexOf('·', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			DNode ss = null;
			if (pkg instanceof KievPackage) {
				foreach (DNode s; pkg.pkg_members; s.sname == nm) {
					ss = s;
					break;
				}
			}
			else if (pkg instanceof ComplexTypeDecl) {
				foreach (DNode s; pkg.members; s.sname == nm) {
					ss = s;
					break;
				}
			}
			else
				return null;
			if (ss == null)
				return null;
			pkg = ss;
			start = end+1;
			end = qname.indexOf('·', start);
		}
		String nm = qname.substring(start).intern();
		if (pkg instanceof KievPackage) {
			foreach (DNode dn; pkg.pkg_members; dn.sname == nm)
				return dn;
		}
		else if (pkg instanceof ComplexTypeDecl) {
			foreach (DNode dn; pkg.members; dn.sname == nm)
				return dn;
		}
		return null;
	}
	
	public synchronized void registerSymbol(SymUUID suuid) {
		Symbol old = this.uuidToSymbolMap.get(suuid);
		if (old != null) {
			Debug.assert("Registering another symbol with the same UUID ("+suuid+"):\n\t"+suuid.symbol);
			return;
		}
		this.uuidToSymbolMap.put(suuid);
	}
	
	public Symbol getSymbolByUUID(long high, long low) {
		return this.uuidToSymbolMap.get(new SymUUID(high, low, null));
	}
	public Symbol getSymbolByUUID(SymUUID suuid) {
		return this.uuidToSymbolMap.get(suuid);
	}
	public Symbol getSymbolByUUID(String uuid) {
		if (uuid == "")
			return null;
		SymUUID suuid = new SymUUID(uuid, null);
		return this.uuidToSymbolMap.get(suuid);
	}
	
	public synchronized Struct newStruct(String sname, DNode outer, int acces, Struct cl, String uuid)
	{
		Symbol sym = null;
		if (outer != null) {
			Symbol psym = outer.symbol;
			if (sname != null && psym.isGlobalSymbol())
				sym = psym.makeGlobalSubSymbol(sname);
		}
		if (sym == null)
			sym = new Symbol(sname);
		if (uuid != null && uuid != "")
			sym.setUUID(this,uuid);
		if (sym.isAttached())
			sym.parent().detach();
		cl.symbol = ~sym;
		cl.nodeflags |= acces;
		if (outer != null) {
			if (outer instanceof ComplexTypeDecl)
				outer.members += cl;
			else if (outer instanceof KievPackage)
				outer.pkg_members += cl;
		}
		cl.nodeflags |= acces;
		tenv.callbackTypeVersionChanged(cl);
		return cl;
	}

	public synchronized KievPackage newPackage(String qname) {
		if (qname == "")
			return this.root;
		assert(qname.indexOf('.') < 0);
		int end = qname.lastIndexOf('·');
		if (end < 0)
			return newPackage(qname,root);
		else
			return newPackage(qname.substring(end+1).intern(),newPackage(qname.substring(0,end).intern()));
	}

	public synchronized KievPackage newPackage(String sname, KievPackage outer) {
		if (sname.indexOf(" ") >= 0)
			Kiev.reportWarning(root,"Creating a package with space in the name: '"+sname+"'");
		foreach (KievPackage pkg; outer.pkg_members; pkg.sname == sname)
			return pkg;
		KievPackage pkg = new KievPackage(outer.symbol.makeGlobalSubSymbol(sname));
		outer.pkg_members += pkg;
		return pkg;
	}

	static class ProjDumpFilter extends XMLDumpFilter {
		ProjDumpFilter() { super("api"); }
		public boolean ignoreAttr(INode parent, AttrSlot attr) {
			if (parent instanceof FileUnit) {
				if (attr.name == "fname" || attr.name == "ftype")
					return false;
				return true;
			}
			return super.ignoreAttr(parent, attr);
		}
		public boolean ignoreNode(INode parent, AttrSlot attr, INode node) {
			if (node instanceof FileUnit)
				return !node.is_project_file;
			if (node instanceof DirUnit)
				return !node.hasProjectFiles();
			return super.ignoreNode(parent, attr, node);
		}
	};
	public void dumpProjectFile() {
		if (Kiev.project_file == null) return;
		try {
			Kiev.project_file.createNewFile();
		} catch (Exception e) {
			return;
		}
		if (!Kiev.project_file.canWrite()) return;
		DumpFactory.getXMLDumper().dumpToXMLFile(this, new ProjDumpFilter(), new ANode[]{this.proj}, Kiev.project_file);
	}

	public boolean existsTypeDecl(String qname) {
		if (qname == "") return true;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return false;
		DNode dn = resolveGlobalDNode(qname);
		if (dn instanceof TypeDecl)
			return true;
		// Check if not loaded
		return this.classpath.exists(qname.replace('·','/'));

	}

	public TypeDecl loadTypeDecl(String qname, boolean fatal) {
		DNode dn = loadAnyDecl(qname);
		if (fatal && !(dn instanceof TypeDecl))
			throw new RuntimeException("Cannot find TypeDecl "+qname.replace('·','.'));
		return (TypeDecl)dn;
	}

	public TypeDecl loadTypeDecl(TypeDecl cl) {
		if (!cl.isTypeDeclNotLoaded())
			return cl;
		// Load if not loaded or not resolved
		if (cl.isTypeDeclNotLoaded()) {
			if (cl instanceof Struct)
				return (TypeDecl)benv.actuallyLoadDecl((Struct)cl);
			else
				return (TypeDecl)benv.actuallyLoadDecl(cl.qname());
		}
		return cl;
	}
	
	public DNode loadAnyDecl(String qname) {
		if (qname.length() == 0) return this.root;
		// Check class is already loaded
		if (classHashOfFails.get(qname) != null) return null;
		DNode dn = resolveGlobalDNode(qname);
		// Load if not loaded or not resolved
		if (dn == null)
			dn = benv.actuallyLoadDecl(qname);
		else if (dn instanceof TypeDecl && dn.isTypeDeclNotLoaded()) {
			if (dn instanceof Struct)
				dn = benv.actuallyLoadDecl((Struct)dn);
			else
				dn = benv.actuallyLoadDecl(dn.qname());
		}
		if (dn == null)
			classHashOfFails.put(qname);
		return dn;
	}

	public byte[] loadClazzFromClasspath(String name) {
		trace(kiev.bytecode.Clazz.traceRules,"Loading data for clazz "+name);

		byte[] data = this.classpath.read(name);
		trace(kiev.bytecode.Clazz.traceRules && data != null ,"Data for clazz "+name+" loaded");

		if (data == null || data.length == 0)
			trace(kiev.bytecode.Clazz.traceRules,"Data for clazz "+name+" not found");

		return data;
	}

	public static INode getPrevNode(ANode node) {
		AttrSlot slot = node.pslot();
		if (slot instanceof ASpaceAttrSlot) {
			INode prev = null;
			foreach (INode n; slot.iterate(node.parent())) {
				if (node == n)
					return prev;
				prev = n;
			}
		}
		return null;
	}
	public static INode getNextNode(ANode node) {
		AttrSlot slot = node.pslot();
		if (slot instanceof ASpaceAttrSlot) {
			Enumeration<INode> iter = slot.iterate(node.parent());
			foreach (INode n; iter; node == n) {
				if (iter.hasMoreElements())
					return iter.nextElement();
				return null;
			}
		}
		return null;
	}
	
	public static ScalarPtr getScalarPtr(INode node, String name) {
		foreach (ScalarAttrSlot attr; node.values(); attr.name == name)
			return new ScalarPtr(node.asANode(), attr);
		throw new RuntimeException("No @nodeAttr/@nodeData attribute '"+name+"' in "+node.getClass());
	}
	
	public static SpacePtr getSpacePtr(INode node, String name) {
		foreach (SpaceAttrSlot attr; node.values(); attr.name == name)
			return new SpacePtr(node.asANode(), attr);
		throw new RuntimeException("No @nodeAttr/@nodeData space '"+name+"' in "+node.getClass());
	}

	public static boolean hasSameRoot(INode n1, INode n2) {
		return ctxRoot(n1) == ctxRoot(n2);
	}

	public static boolean needResolving(SymbolRef sref) {
		Symbol symb = sref.symbol;
		if (symb == null)
			return (sref.name != null && sref.name != "");
		else
			return !hasSameRoot(sref, symb);
	}
	
	public static INode ctxRoot(INode self) {
		while (self.isAttached())
			self = self.parent();
		return self;
	}
	
	public static ANode ctxRoot(ANode self) {
		while (self.isAttached())
			self = self.parent();
		return self;
	}
	
	public static FileUnit ctxFileUnit(INode self) {
		return ctxFileUnit((ANode)self);
	}
	public static FileUnit ctxFileUnit(ANode self) {
		if (self == null)
			return null;
		if (self instanceof FileUnit)
			return (FileUnit)self;
		for (;;) {
			ANode p = ANode.nodeattr$syntax_parent.get(self);
			if (p != null) {
				if (p instanceof FileUnit)
					return (FileUnit)p;
				self = p;
				continue;
			}
			p = self.parent();
			if (p != null) {
				if (p instanceof FileUnit)
					return (FileUnit)p;
				self = p;
				continue;
			}
			return null;
		}
	}
	public static SyntaxScope ctxSyntaxScope(INode self) {
		return ctxSyntaxScope((ANode)self);
	}
	public static SyntaxScope ctxSyntaxScope(ANode self) {
		if (self == null)
			return null;
		if (self instanceof SyntaxScope)
			return (SyntaxScope)self;
		for (;;) {
			ANode p = ANode.nodeattr$syntax_parent.get(self);
			if (p != null) {
				if (p instanceof SyntaxScope)
					return (SyntaxScope)p;
				self = p;
				continue;
			}
			p = self.parent();
			if (p != null) {
				if (p instanceof SyntaxScope)
					return (SyntaxScope)p;
				self = p;
				continue;
			}
			return null;
		}
	}
	
	public static ComplexTypeDecl ctxTDecl(ANode self) {
		if (self == null || self instanceof SyntaxScope)
			return null;
		return ctxChildTDecl(self.parent());
	}
	private static ComplexTypeDecl ctxChildTDecl(ANode self) {
		if (self == null || self instanceof SyntaxScope)
			return null;
		if (self instanceof ComplexTypeDecl)
			return (ComplexTypeDecl)self;
		return ctxChildTDecl(self.parent());
	}
	public static Method ctxMethod(ANode self) {
		if (self == null || self instanceof SyntaxScope)
			return null;
		return ctxChildMethod(self.parent());
	}
	private static Method ctxChildMethod(ANode self) {
		if (self == null || self instanceof SyntaxScope)
			return null;
		if (self instanceof Method)
			return (Method)self;
		return ctxChildMethod(self.parent());
	}
}

