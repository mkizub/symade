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
package kiev.transf;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class VNodePlugin implements PluginFactory {
	public PluginDescr getPluginDescr(String name) {
		PluginDescr pd = null;
		if (name.equals("vnode")) {
			pd = new PluginDescr("vnode").depends("kiev");
			pd.proc(new ProcessorDescr("pass3", "fe", 100, VNodeFE_Pass3.class).after("kiev:fe:pass3"));
			pd.proc(new ProcessorDescr("gen-members", "fe", 0, VNodeFE_GenMembers.class).after("vnode:fe:pass3").before("kiev:fe:pre-resolve"));
			pd.proc(new ProcessorDescr("verify", "fv", 0, VNodeFE_Verify.class));
			pd.proc(new ProcessorDescr("pre-generate", "me", 0, VNodeME_PreGenerate.class).after("view:me:pre-generate").after("virt-fld:me:pre-generate"));
			pd.proc(new ProcessorDescr("fix-resolve", "be", 0, VNodeBE_FixResolve.class).after("kiev:be:resolve").before("kiev:be:generate").before("virt-fld:be:rewrite"));
		}
		return pd;
	}
}

public final class VNodeUtils {
	private static final String PROP_BASE		= "symade.transf.vnode";
	public static final boolean GEN_VERSIONED	= TransfProcessor.getPropS(PROP_BASE,"genUnversioned","false") != "true";
	public static final String mnNode			= TransfProcessor.getPropS(PROP_BASE,"mnNode","kiev·vtree·ThisIsANode"); 
	public static final String mnAtt			= TransfProcessor.getPropS(PROP_BASE,"mnAtt","kiev·vtree·nodeAttr"); 
	public static final String mnRef			= TransfProcessor.getPropS(PROP_BASE,"mnRef","kiev·vtree·nodeData"); 
	public final String mnUnVersioned			= TransfProcessor.getPropS(PROP_BASE,"mnUnVersioned","kiev·vtree·UnVersioned"); 
	public final String nameINode				= TransfProcessor.getPropS(PROP_BASE,"nameINode","kiev·vtree·INode");
	public final String nameANode				= TransfProcessor.getPropS(PROP_BASE,"nameANode","kiev·vtree·ANode");
	public final String nameNode				= TransfProcessor.getPropS(PROP_BASE,"nameNode","kiev·vtree·ASTNode"); 
	public final String nameNodeTypeInfo		= TransfProcessor.getPropS(PROP_BASE,"nameNodeTypeInfo","kiev·vtree·NodeTypeInfo");
	public final String nameNodeSpace			= TransfProcessor.getPropS(PROP_BASE,"nameNodeSpace","kiev·vtree·NodeSpace"); 
	public final String nameNodeExtSpace		= TransfProcessor.getPropS(PROP_BASE,"nameNodeExtSpace","kiev·vtree·NodeExtSpace");
	public final String nameNodeSymbolRef		= TransfProcessor.getPropS(PROP_BASE,"nameNodeSymbolRef","kiev·vtree·NodeSymbolRef");
	public final String nameTreeWalker			= TransfProcessor.getPropS(PROP_BASE,"TreeWalker","kiev·vtree·TreeWalker"); 
	public final String nameAttrSlot			= TransfProcessor.getPropS(PROP_BASE,"nameAttrSlot","kiev·vtree·AttrSlot"); 
	public final String nameScalarAttrSlot			= TransfProcessor.getPropS(PROP_BASE,"nameScalarAttrSlot","kiev·vtree·ScalarAttrSlot"); 
	public final String nameExtAttrSlot		= TransfProcessor.getPropS(PROP_BASE,"nameExtAttrSlot","kiev·vtree·ExtAttrSlot"); 
	public final String nameSpaceAttrSlot		= TransfProcessor.getPropS(PROP_BASE,"nameSpaceAttrSlot","kiev·vtree·SpaceAttrSlot"); 
	public final String nameExtSpaceAttrSlot	= TransfProcessor.getPropS(PROP_BASE,"nameExtSpaceAttrSlot","kiev·vtree·ExtSpaceAttrSlot"); 
	public final String nameParentAttrSlot			= TransfProcessor.getPropS(PROP_BASE,"nameParentAttrSlot","kiev·vtree·ParentAttrSlot"); 
	public final String nameLanguageIface		= TransfProcessor.getPropS(PROP_BASE,"nameLanguageIface","kiev·vlang·Language"); 
	public final String nameCopyContext			= TransfProcessor.getPropS(PROP_BASE,"nameCopyContext","kiev·vtree·CopyContext"); 
	public final String nameCopyable			= TransfProcessor.getPropS(PROP_BASE,"nameCopyable","copyable");
	public final String nameExtData				= TransfProcessor.getPropS(PROP_BASE,"nameExtData","ext_data");
	public final String nameNodeName			= TransfProcessor.getPropS(PROP_BASE,"nameNodeName","name");
	public final String nameLangName			= TransfProcessor.getPropS(PROP_BASE,"nameLangName","lang");
	public final String nameCallbackDataSet		= TransfProcessor.getPropS(PROP_BASE,"nameCallbackDataSet","callbackDataSet");
	
	public final String operatorPostTypeSpace		= TransfProcessor.getPropS(PROP_BASE,"operatorPostTypeSpace","T ∅");		// \u2205
	public final String operatorPostTypeExtSpace	= TransfProcessor.getPropS(PROP_BASE,"operatorPostTypeExtSpace","T ⋈");		// \u22c8
	public final String operatorPostTypeSymbolRef	= TransfProcessor.getPropS(PROP_BASE,"operatorPostTypeSymbolRef","T ⇑");	// \u21d1

	Type tpINode;
	Type tpANode;
	Type tpNode;
	Type tpNodeTypeInfo;
	Type tpNArray;
	Type tpNodeSpace;
	Type tpNodeExtSpace;
	Type tpNodeSymbolRef;
	Type tpTreeWalker;
	Type tpAttrSlot;
	Type tpScalarAttrSlot;
	Type tpExtAttrSlot;
	Type tpSpaceAttrSlot;
	Type tpExtSpaceAttrSlot;
	Type tpParentAttrSlot;
	Type tpLanguageIface;
	Type tpCopyContext;
	
	Operator PostTypeSpace;
	Operator PostTypeExtSpace;
	Operator PostTypeSymbolRef;

	public static boolean isNodeImpl(Struct s) {
		return s.getMeta(mnNode) != null;
	}
	public static boolean isNodeKind(Struct s) {
		return s.getMeta(mnNode) != null;
	}
	public static boolean isNodeKind(Type t) {
		if (t != null && t.getStruct() != null)
			return isNodeKind(t.getStruct());
		return false;
	}
	
	VNodeUtils(Env env) {
		tpINode = env.loadTypeDecl(nameINode, true).getType(env);
		tpANode = env.loadTypeDecl(nameANode, true).getType(env);
		tpNode = env.loadTypeDecl(nameNode, true).getType(env);
		tpNodeTypeInfo = env.loadTypeDecl(nameNodeTypeInfo, true).getType(env);
		tpNArray = new ArrayType(tpANode);
		tpNodeSpace = env.loadTypeDecl(nameNodeSpace, true).getType(env);
		tpNodeExtSpace = env.loadTypeDecl(nameNodeExtSpace, true).getType(env);
		tpNodeSymbolRef = env.loadTypeDecl(nameNodeSymbolRef, true).getType(env);
		tpTreeWalker = env.loadTypeDecl(nameTreeWalker, true).getType(env);
		tpAttrSlot = env.loadTypeDecl(nameAttrSlot, true).getType(env);
		tpScalarAttrSlot = env.loadTypeDecl(nameScalarAttrSlot, true).getType(env);
		tpExtAttrSlot = env.loadTypeDecl(nameExtAttrSlot, true).getType(env);
		tpSpaceAttrSlot = env.loadTypeDecl(nameSpaceAttrSlot, true).getType(env);
		tpExtSpaceAttrSlot = env.loadTypeDecl(nameExtSpaceAttrSlot, true).getType(env);
		tpParentAttrSlot = env.loadTypeDecl(nameParentAttrSlot, true).getType(env);
		tpLanguageIface = env.loadTypeDecl(nameLanguageIface, true).getType(env);
		tpCopyContext = env.loadTypeDecl(nameCopyContext, true).getType(env);

		PostTypeSpace = Operator.getOperatorByName(operatorPostTypeSpace);
		PostTypeExtSpace = Operator.getOperatorByName(operatorPostTypeExtSpace);
		PostTypeSymbolRef = Operator.getOperatorByName(operatorPostTypeSymbolRef);
	}
}

/////////////////////////////////////////////
//      Verify the VNode tree structure    //
/////////////////////////////////////////////

public final class VNodeFE_Pass3 extends TransfProcessor {

	@forward
	private VNodeUtils utils;
	
	public VNodeFE_Pass3(Env env, int id) { super(env,id,KievExt.VNode); }

	public String getDescr() { "VNode members creation" }

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			if (utils == null)
				utils = new VNodeUtils(this.env);
			tr = Transaction.enter(tr,"VNodeFE_Pass3");
			try {
				doProcess(node);
			} finally { tr.leave(); }
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode n; ss.members)
			doProcess(n);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach (Struct sub; s.members)
			doProcess(sub);
		if (VNodeUtils.isNodeKind(s)) {
			if (nameNode.equals(s.qname()))
				return;
			// Check fields of the @ThisIsANode
			foreach (Field n; s.members)
				doProcess(n);
			return;
		}
		
		// Check fields to not have @nodeAttr and @nodeData
		foreach (Field f; s.members) {
			UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
			UserMeta fmref = (UserMeta)f.getMeta(mnRef);
			if (fmatt != null || fmref != null) {
				Kiev.reportWarning(f,"Field "+f+" of non-@node class "+Env.ctxTDecl(f)+" may not be @nodeAttr or @nodeData");
			}
		}
	}
	
	public void doProcess(Field:ASTNode f) {
		UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
		UserMeta fmref = (UserMeta)f.getMeta(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @nodeAttr="+fmatt+" and @nodeData="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f,"Field "+Env.ctxTDecl(f)+"."+f+" marked both @nodeAttr and @nodeData");
		}
		if (fmatt != null || fmref != null) {
			if (f.isStatic())
				Kiev.reportError(f,"Field "+Env.ctxTDecl(f)+"."+f+" is static and cannot have @nodeAttr or @nodeData");
			boolean isArr = false;
			{
				Type ft = f.getType(env);
				boolean isExtData = fmatt != null ? fmatt.getZ(nameExtData) : fmref.getZ(nameExtData);
				if (ft.isInstanceOf(tpNArray)) {
					if (!isExtData) {
						if (!ft.isInstanceOf(tpNodeSpace))
							Kiev.reportError(f,"Use node space operator '"+operatorPostTypeSpace+"' instead of 'T []'");
					} else {
						if (!ft.isInstanceOf(tpNodeExtSpace))
							Kiev.reportError(f,"Use node extended space operator '"+operatorPostTypeExtSpace+"' instead of 'T []'"); // ⋈
					}
					if (ft.isInstanceOf(tpNodeExtSpace)) {
						if (!f.isAbstract())
							f.setAbstract(true);
						if (f.isVirtual())
							f.setVirtual(false);
					}
					isArr = true;
				}
			}
			//System.out.println("process @node: field "+f+" of type "+fs+" has correct @nodeAttr="+fmatt+" or @nodeData="+fmref);
			if (isArr && f.init != null && !f.init.isAutoGenerated()) {
				Kiev.reportError(f,"Field "+Env.ctxTDecl(f)+"."+f+" may not have initializer");
			}
		}
		else if (!f.isStatic() && !f.isInterfaceOnly() && !f.isAbstract()) {
			if (f.getType(env).isInstanceOf(tpNArray))
				Kiev.reportWarning(f,"Field "+Env.ctxTDecl(f)+"."+f+" must be marked with @nodeAttr or @nodeData");
			else if (VNodeUtils.isNodeKind(f.getType(env)))
				Kiev.reportWarning(f,"Field "+Env.ctxTDecl(f)+"."+f+" must be marked with @nodeAttr or @nodeData");
		}
		if (!f.isStatic() && !f.isAbstract() && !f.isFinal() && f.getMeta(mnUnVersioned) == null)
			f.setVirtual(true);
	}
}

//////////////////////////////////////////////////////
//   Generate class members (enumerate sub-nodes)   //
//////////////////////////////////////////////////////

public final class VNodeFE_GenMembers extends TransfProcessor {
	public VNodeFE_GenMembers(Env env, int id) { super(env,id,KievExt.VNode); }
	public String getDescr() { "VNode members generation" }

	@forward
	private VNodeUtils utils;
	private Vector<Struct> processed;
	
	private Type makeNodeAttrClass(Struct snode, Field f) {
		UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
		UserMeta fmref = (UserMeta)f.getMeta(mnRef);
		boolean isAtt = (fmatt != null);
		boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
		boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
		boolean isExtData = isAtt ? fmatt.getZ(nameExtData) : fmref.getZ(nameExtData);
		Type ft = f.getType(env);
		Type clz_tp = (isArr || isSet) ? ft.resolveArg(0) : ft;
		//if (clz_tp ≡ env.tenv.tpInt);
		//else if (clz_tp ≡ env.tenv.tpBoolean);
		//else if (clz_tp ≡ env.tenv.tpChar);
		//else if (clz_tp ≡ env.tenv.tpByte);
		//else if (clz_tp ≡ env.tenv.tpShort);
		//else if (clz_tp ≡ env.tenv.tpLong);
		//else if (clz_tp ≡ env.tenv.tpFloat);
		//else if (clz_tp ≡ env.tenv.tpDouble);
		//else if (clz_tp ≈ env.tenv.tpString);
		//else if (clz_tp.isInstanceOf(tpANode));
		//else if (clz_tp.isInstanceOf(tpINode));
		//else if (clz_tp.isInstanceOf(env.tenv.tpEnum));
		//else
		//	Kiev.reportWarning(f, "Strange attribute type "+clz_tp);
		String sname = ("NodeAttr_"+f.sname).intern();
		foreach (TypeDecl td; snode.members; td.sname == sname) {
			if (!f.isInterfaceOnly())
				Kiev.reportWarning(td,"Class "+snode+"."+sname+" already exists and will not be generated");
			return td.getType(env);
		}
		Struct s = this.env.newStruct(sname,snode,ACC_FINAL|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),null);
		if (!s.isAttached())
			snode.members.add(s);
		{
			String nameTreePkg = nameANode.substring(0,nameANode.lastIndexOf('·'));
			foreach (UserMeta m; f.metas; m.qname().startsWith(nameTreePkg))
				s.setMeta(new Copier().copyFull(m));
		}
		Field parent_attr_slot = null;
		if (isAtt) {
			String p = fmatt.getS("parent");
			if (p.length() == 0) {
				parent_attr_slot = tpANode.meta_type.tdecl.resolveField(env, "nodeattr$parent");
			} else {
				int s = p.lastIndexOf('.');
				if (s < 0)
					parent_attr_slot = snode.resolveField(env,p.intern());
				else
					parent_attr_slot = ((TypeDecl)env.resolveGlobalDNode(p.substring(0,s))).resolveField(env,p.substring(s+1).intern());
			}
		}
		if (isSet) {
			TVarBld set = new TVarBld();
			set.append(tpExtSpaceAttrSlot.getStruct().args[0].getAType(env), clz_tp);
			s.super_types.insert(0, new TypeRef(tpExtSpaceAttrSlot.applay(set)));
		}
		else if (isArr) {
			TVarBld set = new TVarBld();
			set.append(tpSpaceAttrSlot.getStruct().args[0].getAType(env), clz_tp);
			s.super_types.insert(0, new TypeRef(tpSpaceAttrSlot.applay(set)));
		}
		else {
			if (isExtData)
				s.super_types.insert(0, new TypeRef(tpExtAttrSlot));
			else
				s.super_types.insert(0, new TypeRef(tpScalarAttrSlot));
		}
		StdTypes tenv = this.env.getTypeEnv();
		// make constructor
		{
			Constructor ctor = new Constructor(0);
			s.members.add(ctor);
			Constructor sctor = (Constructor)s.super_types[0].getStruct(env).resolveMethod(env,null,tenv.tpVoid,tenv.tpString,tpParentAttrSlot,tenv.tpTypeInfo);
			ENode parent_fld_expr = (parent_attr_slot == null ? new ConstNullExpr() : new SFldExpr(0,parent_attr_slot));
			CtorCallExpr ce = new CtorCallExpr(f.pos,
					new SuperExpr(),
					new ENode[]{
						new ConstStringExpr(f.sname),
						parent_fld_expr,
						new TypeInfoExpr(0, new TypeRef(clz_tp))
					}
				);
			ce.symbol = sctor.symbol;
			ctor.body = new Block(0);
			ctor.block.stats.add(new ExprStat(ce));
		}
		if (!isExtData) {
			if (isArr) {
				// add public N[] get(ASTNode parent)
				{
					Method getArr = new MethodImpl("getArray",new ArrayType(tpINode),ACC_PUBLIC | ACC_SYNTHETIC);
					getArr.params.add(new LVar(0, "parent", tpINode, Var.VAR_LOCAL, ACC_FINAL));
					s.addMethod(getArr);
					getArr.body = new Block(0);
					ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.getType(env), new LVarExpr(0, getArr.params[0]) ), f);
					getArr.block.stats.add(new ReturnStat(f.pos,val));
				}
				// add public void set(ASTNode parent, N[]:Object narr)
				{
					Method setArr = new MethodImpl("setArray",tenv.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
					setArr.params.add(new LVar(0, "parent", tpINode, Var.VAR_LOCAL, ACC_FINAL));
					setArr.params.add(new LVar(0, "narr", tenv.tpObject /*new ArrayType(tpINode)*/, Var.VAR_LOCAL, ACC_FINAL));
					s.addMethod(setArr);
					setArr.body = new Block(0);
					ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.getType(env), new LVarExpr(0, setArr.params[0]) ), f);
					setArr.block.stats.add(new ExprStat(
						new AssignExpr(f.pos,lval,new CastExpr(f.pos, f.getType(env), new LVarExpr(0, setArr.params[1])))
					));
				}
			} else {
				// add public N[] get(ASTNode parent)
				{
					Method getVal = new MethodImpl("get",tenv.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
					getVal.params.add(new LVar(0, "parent", tpINode, Var.VAR_LOCAL, ACC_FINAL));
					s.addMethod(getVal);
					Block block = new Block(0);
					getVal.body = block;
					ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.getType(env), new LVarExpr(0, getVal.params[0]) ), f);
					ReturnStat rs = new ReturnStat(f.pos,val);
					block.stats.add(rs);
					if!(val.getType(env).isReference())
						CastExpr.autoCastToReference(env, val, rs, ReturnStat.nodeattr$expr);
				}
				// add public void set(ASTNode parent, Object narr)
				{
					Method setVal = new MethodImpl("set",tenv.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
					setVal.params.add(new LVar(0, "parent", tpINode, Var.VAR_LOCAL, ACC_FINAL));
					setVal.params.add(new LVar(0, "val", tenv.tpObject, Var.VAR_LOCAL, ACC_FINAL));
					s.addMethod(setVal);
					setVal.body = new Block(0);
					if (!f.isFinal() && MetaAccess.writeable(f)) {
						ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.getType(env), new LVarExpr(0, setVal.params[0]) ), f);
						ENode val = new LVarExpr(0, setVal.params[1]);
						Type ftp = f.getType(env);
						if (ftp.isReference())
							val = new CastExpr(0,ftp,val);
						else if (ftp ≡ tenv.tpBoolean)
							val = new CastExpr(0,((CoreType)ftp).getRefTypeForPrimitive(),val);
						else if (ftp ≡ tenv.tpChar)
							val = new CastExpr(0,((CoreType)ftp).getRefTypeForPrimitive(),val);
						else
							val = new CastExpr(0,tenv.tpNumberRef,val);
						AssignExpr ae = new AssignExpr(f.pos,lval,val);
						setVal.block.stats.add(new ExprStat(ae));
						if!(ftp.isReference())
							CastExpr.autoCastToPrimitive(env, val, (CoreType)ftp, ae, AssignExpr.nodeattr$value);
					} else {
						ConstStringExpr msg = new ConstStringExpr((isAtt ? "@nodeAttr " : "@nodeData ")+f.sname+" is not writeable");
						setVal.block.stats.add(
							new ThrowStat(0,new NewExpr(0,tenv.tpRuntimeException,new ENode[]{msg}))
						);
					}
				}
				// add public boolean isWrittable()
				if (f.isFinal() || !MetaAccess.writeable(f)) {
					Method isWrittable = new MethodImpl("isWrittable",tenv.tpBoolean,ACC_PUBLIC | ACC_SYNTHETIC);
					s.addMethod(isWrittable);
					isWrittable.body = new Block(0);
					isWrittable.block.stats.add(new ReturnStat(f.pos,new ConstBoolExpr(false)));
				}
			}
		}

		Kiev.runProcessorsOn(s);
		return s.getType(env);
	}
	
	private void makeNodeDecl(SymadeNode sn) {
		if (sn.node_decl != null)
			return;
		
		StdTypes tenv = this.env.getTypeEnv();
		NodeDecl nd = new NodeDecl();
		sn.node_decl = nd;
		
		foreach (TypeRef st; sn.super_types; st.getTypeDecl(env) instanceof SymadeNode) {
			SymadeNode supsn = (SymadeNode)st.getTypeDecl(env);
			makeNodeDecl(supsn);
			NodeDecl⇑ snd = (NodeDecl⇑)new SymbolRef<NodeDecl>(supsn.node_decl);
			snd.qualified = true;
			nd.super_decls += snd;
		}
		foreach (Field f; sn.members; !f.isStatic() && (f.getMeta(mnAtt) != null || f.getMeta(mnRef) != null)) {
			boolean isAtt = (f.getMeta(mnAtt) != null);
			boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
			boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
			NodeAttribute na = new NodeAttribute();
			na.sname = f.sname;
			if (!isAtt)
				na.is_data = true;
			Type ft = f.getType(env);
			if (isArr)
				na.attr_kind = NodeAttrKind.SPACE_OF_NODES;
			else if (isSet)
				na.attr_kind = NodeAttrKind.EXT_SPACE_OF_NODES;
			else if (ft ≡ tenv.tpBoolean)
				na.attr_kind = NodeAttrKind.SCALAR_BOOL;
			else if (ft ≡ tenv.tpChar)
				na.attr_kind = NodeAttrKind.SCALAR_CHAR;
			else if (ft ≡ tenv.tpByte)
				na.attr_kind = NodeAttrKind.SCALAR_BYTE;
			else if (ft ≡ tenv.tpShort)
				na.attr_kind = NodeAttrKind.SCALAR_SHORT;
			else if (ft ≡ tenv.tpInt)
				na.attr_kind = NodeAttrKind.SCALAR_INT;
			else if (ft ≡ tenv.tpLong)
				na.attr_kind = NodeAttrKind.SCALAR_LONG;
			else if (ft ≡ tenv.tpFloat)
				na.attr_kind = NodeAttrKind.SCALAR_FLOAT;
			else if (ft ≡ tenv.tpDouble)
				na.attr_kind = NodeAttrKind.SCALAR_DOUBLE;
			else if (ft.meta_type.tdecl.isEnum())
				na.attr_kind = NodeAttrKind.SCALAR_ENUM;
			else if (ft ≈ tenv.tpString)
				na.attr_kind = NodeAttrKind.SCALAR_STRING;
			else if (ft.meta_type.tdecl instanceof SymadeNode)
				na.attr_kind = NodeAttrKind.SCALAR_NODE;
			else
				na.attr_kind = NodeAttrKind.SCALAR_OBJ;
			nd.attrs += na;
		}
	}
	
	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			if (utils == null)
				utils = new VNodeUtils(this.env);
			processed = null;
			tr = Transaction.enter(tr,"VNodeFE_GenMembers");
			try {
				doProcess(node);
			} finally { tr.leave(); }
			processed = null;
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode dn; ss.members)
			this.doProcess(dn);
	}
	
	private void doProcess(Struct:ASTNode s) {
		if (processed == null)
			processed = new Vector<Struct>();
		processed.append(s);
		foreach (Struct dn; s.members)
			this.doProcess(dn);
		if (s instanceof SymadeNode) {
			SymadeNode sn = (SymadeNode)s;
			if (sn.node_decl == null)
				makeNodeDecl(sn);
		}
		if (!VNodeUtils.isNodeImpl(s) || (s.isInterface() && !s.isMixin()))
			return;
		foreach (Field f; s.members; f.sname == nameEnumValuesFld)
			return; // already generated

		Vector<Struct> sup_struct = new Vector<Struct>();
		foreach (TypeRef st; s.super_types; VNodeUtils.isNodeKind(st.getStruct(env))) {
			Struct sups = st.getStruct(env);
			this.doProcess(sups);
			sup_struct.append(sups);
		}
		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		Vector<Field> dnaflds = new Vector<Field>();
		foreach (Field f; s.members; !f.isStatic() && (f.getMeta(mnAtt) != null || f.getMeta(mnRef) != null))
			aflds.append(f);
		foreach (Field f; aflds) {
			boolean isAtt = (f.getMeta(mnAtt) != null);
			boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
			boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
			Type tpa = makeNodeAttrClass(s,f);
			String fname = ("nodeattr$"+f.sname).intern();
			Field f_attr = null;
			foreach (Field ff; s.members; ff.sname == fname) {
				f_attr = ff;
				break;
			}
			if (f_attr != null) {
				if (!f.isInterfaceOnly())
					Kiev.reportWarning(f_attr,"Field "+s+"."+fname+" already exists and will not be generated");
				dnaflds.append(f_attr);
			} else {
				Field dnaf = new Field(fname, tpa, ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
				s.addField(dnaf);
				dnaflds.append(dnaf);
				f.nodeattr_of_attr = new SymbolRef<Field>(dnaf);
			}
			if (isAtt && !isArr && !isSet)
				f.setVirtual(true);
			UserMeta fmeta = (UserMeta) (isAtt ? f.getMeta(mnAtt) : f.getMeta(mnRef));
			if (fmeta.getZ(nameExtData) && !isSet) {
				f.setVirtual(true);
				f.setAbstract(true);
			}
		}
		s.addField(new Field(nameEnumValuesFld, new ArrayType(tpAttrSlot), ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
		
		// node type info
		ENode node_defined_slots;
		{
			ENode[] vals_init = new ENode[dnaflds.size()];
			for(int i=0; i < vals_init.length; i++) {
				Field dnaf = dnaflds[i];
				vals_init[i] = new SFldExpr(dnaf.pos, dnaf);
			}
			node_defined_slots = new NewInitializedArrayExpr(0, new TypeExpr(tpAttrSlot,Operator.PostTypeArray,new ArrayType(tpAttrSlot)), vals_init)
		}


		Field fld_nti = new Field("$node_type_info", tpNodeTypeInfo, ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC);
		fld_nti.init = new NewExpr(0, tpNodeTypeInfo, new ENode[]{
			new TypeClassExpr(0, new TypeRef(s.getType(env))),
			node_defined_slots
		});
		s.addField(fld_nti);
		

	}
}

////////////////////////////////////////////////////
//	   PASS - verification                        //
////////////////////////////////////////////////////

public final class VNodeFE_Verify extends VerifyProcessor {

	@forward
	private VNodeUtils utils;
	
	private Hashtable<String,LangDecl> allLangs = new Hashtable<String,LangDecl>();
	private Hashtable<String,NodeDecl> allNodeDecls = new Hashtable<String,NodeDecl>();

	public VNodeFE_Verify(Env env, int id) { super(env,id,KievExt.VNode); }
	
	public String getDescr() { "VNode verification" }

	private LangDecl getLangDecl(TypeDecl s) {
		LangDecl ld = allLangs.get(s.qname());
		if (ld != null)
			return ld;
		ld = new LangDecl();
		ld.sname = "lang"+s.sname;
		allLangs.put(s.qname(), ld);
		return ld;
	}

	private NodeDecl getNodeDecl(TypeDecl s) {
		NodeDecl nd = allNodeDecls.get(s.qname());
		if (nd != null)
			return nd;
		nd = new NodeDecl();
		nd.sname = s.sname;
		allNodeDecls.put(s.qname(), nd);
		return nd;
	}

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			if (utils == null)
				utils = new VNodeUtils(this.env);
			if (node instanceof Struct && VNodeUtils.isNodeKind((Struct)node)) {
				tr = Transaction.enter(tr,"VNodeFE_Verify");
				try {
					verifyStruct((Struct)node);
				} finally { tr.leave(); }
			}
			/*
			if (node instanceof Struct && !node.isInterface() && node.getType(env).isInstanceOf(tpLanguageIface)) {
				LangDecl ld = getLangDecl((Struct)node);
				if (!ld.isAttached()) {
					((KievPackage)node.parent()).pkg_members += ld;
					FileUnit fu = FileUnit.makeFile("lang"+node.sname, env.getProject(), false);
					fu.members += ld;
				}
				foreach (TypeRef tr; node.super_types; tr.getType(env).isInstanceOf(tpLanguageIface)) {
					ld.super_langs += (LangDecl⇑)new SymbolRef<LangDecl>(getLangDecl(tr.getStruct(env)));
				}
			}
			*/
		}
	}
	
	private void verifyStruct(Struct s) {
		UserMeta m = (UserMeta)s.getMeta(mnNode);
		MetaValueScalar langValue = (MetaValueScalar)m.get(nameLangName);
		TypeDecl td;
		if (langValue.value instanceof TypeClassExpr)
			td = ((TypeClassExpr)langValue.value).ttype.getTypeDecl(env);
		else
			td = ((TypeRef)langValue.value).getTypeDecl(env);
		if (td.getType(env) ≡ this.env.getTypeEnv().tpVoid)
			return;
		if!(td instanceof Struct) {
			Kiev.reportError(m,"Language '"+td+"' is not a class");
			return;
		}
		if (td.isInterfaceOnly())
			return;
		Field nodeClasses = td.resolveField(env,"nodeClasses", false);
		if (nodeClasses == null) {
			Kiev.reportWarning(m,"Language '"+td+"' has no field 'nodeClasses'");
			return;
		}
		if!(nodeClasses.init instanceof NewInitializedArrayExpr) {
			Kiev.reportError(m,"Language '"+td+"' has field 'nodeClasses' with wrong initializer (NewInitializedArrayExpr expected)");
			return;
		}
		NewInitializedArrayExpr init = (NewInitializedArrayExpr)nodeClasses.init;
		boolean found = false;
		foreach (TypeClassExpr tce; init.args) {
			if (tce.ttype.getStruct(env) == s) {
				found = true;
				break;
			}
		}
		if (!found) {
			Kiev.reportError(m,"Language '"+td+"' has no reference to @node '"+s+"' in field 'nodeClasses'");
			return;
		}
		/*
		LangDecl ld = getLangDecl(td);
		NodeDecl nd = getNodeDecl(s);
		if (nd.isAttached())
			return;
		String ndname = m.getS("name");
		if (ndname == null || ndname.length() == 0)
			ndname = s.sname;
		nd.sname = ndname;
		ld.decls += nd;
		foreach (TypeRef tr; s.super_types; VNodeUtils.isNodeKind(tr.getType(env)))
			nd.super_decls += (NodeDecl⇑)new SymbolRef<NodeDecl>(getNodeDecl(tr.getStruct(env)));
		foreach (Field f; s.members) {
			UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
			UserMeta fmref = (UserMeta)f.getMeta(mnRef);
			if (fmatt != null || fmref != null) {
				NodeAttribute na = new NodeAttribute();
				na.sname = f.sname;
				if (fmref != null)
					na.is_data = true;
				Type ft = f.getType(env);
				if (ft ≈ tenv.tpBoolean)
					na.attr_kind = NodeAttrKind.SCALAR_BOOL;
				else if (ft ≈ tenv.tpString)
					na.attr_kind = NodeAttrKind.SCALAR_STRING;
				else if (ft ≈ tenv.tpInt)
					na.attr_kind = NodeAttrKind.SCALAR_INT;
				else if (ft.isInstanceOf(tpNodeExtSpace))
					na.attr_kind = NodeAttrKind.SPACE;
				else if (ft.isInstanceOf(tpNodeExtSpace))
					na.attr_kind = NodeAttrKind.EXT_SPACE;
				else if (ft.isInstanceOf(tpNodeExtSpace))
					na.attr_kind = NodeAttrKind.EXT_SPACE;
				else if (VNodeUtils.isNodeKind(ft))
					na.attr_kind = NodeAttrKind.SCALAR_NODE;
				else
					na.attr_kind = NodeAttrKind.SCALAR_OBJ;
				nd.attrs += na;
			}
		}
		*/
	}
	
}

public final class VNodeME_PreGenerate extends BackendProcessor {

	@forward
	private VNodeUtils utils;
	private Method _codeSet;
	private Method _codeGet;

	public VNodeME_PreGenerate(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "VNode pre-generation" }

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"VNodeME_PreGenerate");
			try {
				doProcess(node);
			} finally { tr.leave(); }
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode dn; ss.members)
			this.doProcess(dn);
	}
	
	final Struct getIFaceOfImpl(Struct impl) {
		if (impl.parent() instanceof Struct) {
			Struct iface = (Struct)impl.parent();
			if (iface.isInterface() && impl.isClazz() && iface.iface_impl == impl)
				return iface;
		}
		return null;
	}
	
	final boolean isNodeKind(Struct s) {
		return isNodeIface(s) || isNodeImpl(s);
	}

	final boolean isNodeImpl(Struct s) {
		if (s.isClazz() && s.getMeta(mnNode) != null)
			return true;
		Struct iface = getIFaceOfImpl(s);
		if (iface != null && iface.getMeta(mnNode) != null)
			return true;
		return false;
	}

	final boolean isNodeIface(Struct s) {
		if (s.isInterface() && s.getMeta(mnNode) != null)
			return true;
		return false;
	}

	public Method getCodeSet() {
		if (_codeSet == null) {
			TypeDecl td = (TypeDecl)this.env.loadTypeDecl("kiev·transf·TemplateVNode", true);
			_codeSet = td.resolveMethod(env, "codeSet", this.env.getTypeEnv().tpVoid)
		}
		return _codeSet;
	}
	
	public Method getCodeGet() {
		if (_codeGet == null) {
			TypeDecl td = (TypeDecl)this.env.loadTypeDecl("kiev·transf·TemplateVNode", true);
			_codeGet = td.resolveMethod(env, "codeGet", this.env.getTypeEnv().tpVoid)
		}
		return _codeGet;
	}
	
	public void doProcess(Struct:ASTNode s) {
		if (s.isInterface() && !s.isMixin() || !isNodeImpl(s)) {
			foreach(Struct sub; s.members)
				doProcess(sub);
			return;
		}

		if (utils == null)
			utils = new VNodeUtils(this.env);

		Struct iface = getIFaceOfImpl(s);
		if (iface == null)
			iface = s;
		Struct impl;
		if (s.isClazz())
			impl = s;
		else
			impl = s.iface_impl;

		foreach (TypeRef st; iface.super_types; isNodeKind(st.getStruct(env)))
			this.doProcess(st.getStruct(env));

		// attribute names array
		Vector<Field> aflds = null;
		// auto-generated $values field
		Field vals = null;
		if (!s.isInterfaceOnly()) {
			foreach (Field f; iface.members; f.sname == nameEnumValuesFld) {
				vals = f;
				break;
			}
			if (vals == null)
				throw new CompilerException(s,"Auto-generated field with name "+nameEnumValuesFld+" is not found");
			if (vals.init != null || nameNode.equals(iface.qname()) || nameANode.equals(iface.qname()))
				return; // already generated
	
			aflds = new Vector<Field>();
			collectAllAttrFields(aflds, iface);
			ENode[] vals_init = new ENode[aflds.size()];
			for(int i=0; i < vals_init.length; i++) {
				Field f = aflds[i];
				boolean isAtt = (f.getMeta(mnAtt) != null);
				boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
				boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
				if (Env.ctxTDecl(f) != iface) {
					vals_init[i] = new SFldExpr(f.pos, iface.resolveField(env,("nodeattr$"+f.sname).intern(), true));
					continue;
				}
				Field nodeattr_f = getField(iface,("nodeattr$"+f.sname).intern());
				ENode e = new NewExpr(0, nodeattr_f.getType(env), ENode.emptyArray);
				nodeattr_f.init = e;
				Kiev.runProcessorsOn(e);
				vals_init[i] = new SFldExpr(nodeattr_f.pos, nodeattr_f);
			}
			vals.init = new NewInitializedArrayExpr(0, new TypeExpr(tpAttrSlot,Operator.PostTypeArray,new ArrayType(tpAttrSlot)), vals_init);
			Kiev.runProcessorsOn(vals.init);
		}

		// AttrSlot[] values() { return $values; }
		if (hasMethod(iface, nameEnumValues)) {
			if (!s.isInterfaceOnly())
				Kiev.reportWarning(iface,"Method "+iface+"."+nameEnumValues+" already exists, @node member is not generated");
		} else {
			Method elems = new MethodImpl(nameEnumValues,new ArrayType(tpAttrSlot),ACC_PUBLIC | ACC_SYNTHETIC);
			iface.addMethod(elems);
			if (!s.isInterfaceOnly()) {
				elems.body = new Block(0);
				elems.block.stats.add(
					new ReturnStat(0,
						new SFldExpr(0,vals) ) );
				Kiev.runProcessorsOn(elems);
			}
		}

		// NodeTypeInfo getNodeTypeInfo() { return $node_type_info; }
		Field fld_node_type_info = null;
		foreach (Field f; iface.members; f.sname == "$node_type_info") {
			fld_node_type_info = f;
			break;
		}
		if (fld_node_type_info == null)
			throw new CompilerException(s,"Auto-generated field with name "+"$node_type_info"+" is not found");
		if (hasMethod(iface, "getNodeTypeInfo")) {
			if (!s.isInterfaceOnly())
				Kiev.reportWarning(iface,"Method "+iface+"."+"getNodeTypeInfo"+" already exists, @node member is not generated");
		} else {
			Method mnti = new MethodImpl("getNodeTypeInfo",tpNodeTypeInfo,ACC_PUBLIC | ACC_SYNTHETIC);
			iface.addMethod(mnti);
			if (!s.isInterfaceOnly()) {
				mnti.body = new Block(0);
				mnti.block.stats.add(
					new ReturnStat(0,
						new SFldExpr(0,fld_node_type_info) ) );
				Kiev.runProcessorsOn(mnti);
			}
		}

		StdTypes tenv = this.env.getTypeEnv();

		// Language getCompilerLang()
		if (!iface.getType(env).isInstanceOf(tpNode) || hasMethod(impl, "getCompilerLang")) {
			//if (!s.isInterfaceOnly())
			//	Kiev.reportWarning(s,"Method "+s+"."+"getCompilerLang already exists, @node member is not generated");
		} else {
			Method lng = new MethodImpl("getCompilerLang",tpLanguageIface,ACC_PUBLIC | ACC_SYNTHETIC);
			impl.addMethod(lng);
			if (!s.isInterfaceOnly()) {
				lng.body = new Block();
				UserMeta m = (UserMeta)iface.getMeta(mnNode);
				MetaValueScalar langValue = (MetaValueScalar)m.get(nameLangName);
				TypeDecl td;
				if (langValue.value instanceof TypeClassExpr)
					td = ((TypeClassExpr)langValue.value).ttype.getTypeDecl(env);
				else
					td = ((TypeRef)langValue.value).getTypeDecl(env);
				ENode res = null;
				if (td.getType(env) ≡ tenv.tpVoid)
					res = new ConstNullExpr();
				else if (!td.isSingleton())
					res = new ConstNullExpr();
				else
					res = new TypeRef(td.getType(env));
				lng.block.stats.append(new ReturnStat(0,res));
				Kiev.runProcessorsOn(lng);
			}
		}

		// String getCompilerNodeName()
		if (!iface.getType(env).isInstanceOf(tpNode) || hasMethod(impl, "getCompilerNodeName")) {
			//if (!s.isInterfaceOnly())
			//	Kiev.reportWarning(s,"Method "+s+"."+"getCompilerNodeName already exists, @node member is not generated");
		} else {
			Method nname = new MethodImpl("getCompilerNodeName",tenv.tpString,ACC_PUBLIC | ACC_SYNTHETIC);
			impl.addMethod(nname);
			if (!s.isInterfaceOnly()) {
				nname.body = new Block();
				UserMeta m = (UserMeta)iface.getMeta(mnNode);
				String nm = m.getS(nameNodeName);
				if (nm == null || nm.length() == 0)
					nm = iface.sname;
				nname.block.stats.append(new ReturnStat(0,new ConstStringExpr(nm)));
				Kiev.runProcessorsOn(nname);
			}
		}

		// copyTo(Object)
		if (iface.getMeta(mnNode) != null && !((UserMeta)iface.getMeta(mnNode)).getZ(nameCopyable)) {
			// node is not copyable
		}
		else if (hasMethod(impl, "copyTo")) {
			if (!s.isInterfaceOnly())
				Kiev.reportWarning(impl,"Method "+impl+"."+"copyTo(...) already exists, @node member is not generated");
		} else {
			Method copyV = new MethodImpl("copyTo",tenv.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			Var tn;
			Var cc;
			copyV.params.append(tn=new LVar(0,"to$node", tenv.tpObject, Var.VAR_LOCAL, 0));
			copyV.params.append(cc=new LVar(0,"in$context", tpCopyContext, Var.VAR_LOCAL, 0));
			impl.addMethod(copyV);
			if (!s.isInterfaceOnly()) {
				copyV.body = new Block();
				Var v = new LVar(0,"node",impl.getType(env),Var.VAR_LOCAL,0);
				if (isNodeKind(impl.super_types[0].getStruct(env))) {
					CallExpr cae = new CallExpr(0,new SuperExpr(),
						new SymbolRef<Method>("copyTo"),null,new ENode[]{new LVarExpr(0,tn), new LVarExpr(0,cc)});
					v.init = new CastExpr(0,impl.getType(env),cae);
					copyV.block.addSymbol(v);
				} else {
					v.init = new CastExpr(0,impl.getType(env),new LVarExpr(0,tn));
					copyV.block.addSymbol(v);
				}
				foreach (Field f; impl.members) {
					if (f.isAbstract() || f.isStatic())
						continue;
					{	// check if we may not copy the field
						UserMeta fmeta = (UserMeta)f.getMeta(mnAtt);
						if (fmeta == null)
							fmeta = (UserMeta)f.getMeta(mnRef);
						if (fmeta != null && !fmeta.getZ(nameCopyable))
							continue; // do not copy the field
					}
					boolean isNode = (VNodeUtils.isNodeKind(f.getType(env)));
					boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
					boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
					if (f.getMeta(mnAtt) != null && (isNode || isArr)) {
						if (isArr) {
							CallExpr cae = new CallExpr(0,
								new IFldExpr(0,new LVarExpr(0,v),f),
								new SymbolRef<Method>("copyFrom"),
								null,
								new ENode[]{new IFldExpr(0,new ThisExpr(),f),new LVarExpr(0,cc)});
							copyV.block.stats.append(new ExprStat(0,cae));
						}
						else if (f.isFinal()) {
							if (isNode) {
								copyV.block.stats.append( 
									new ExprStat(0,
										new CallExpr(0,
											new IFldExpr(0,new ThisExpr(),f),
											new SymbolRef<Method>("copyTo"),
											null,
											new ENode[]{new IFldExpr(0,new LVarExpr(0,v),f),new LVarExpr(0,cc)}
										)
									)
								);
							}
						} else {
							CallExpr cae = new CallExpr(0,
								new IFldExpr(0, new ThisExpr(),f),
								new SymbolRef<Method>("copy"),
								null,
								new ENode[]{new LVarExpr(0,cc)});
							copyV.block.stats.append( 
								new IfElseStat(0,
									new BinaryBoolExpr(0, env.coreFuncs.fObjectBoolNE,
										new IFldExpr(0,new ThisExpr(),f),
										new ConstNullExpr()
										),
									new ExprStat(0,
										new AssignExpr(0,
											new IFldExpr(0,new LVarExpr(0,v),f),
											new CastExpr(0,f.getType(env),cae)
										)
									),
									null
								)
							);
						}
					} else {
						if (!f.isFinal()) {
							copyV.block.stats.append( 
								new ExprStat(0,
									new AssignExpr(0,
										new IFldExpr(0,new LVarExpr(0,v),f),
										new IFldExpr(0,new ThisExpr(),f)
									)
								)
							);
						}
					}
				}
				copyV.block.stats.append(new ReturnStat(0,new LVarExpr(0,v)));
				Kiev.runProcessorsOn(copyV);
			}
		}

		if (s.isInterfaceOnly())
			return;

		// fix methods
		foreach(Field f; impl.members; !f.isStatic()) {
			UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
			UserMeta fmref = (UserMeta)f.getMeta(mnRef);
			if (fmatt != null || fmref != null) {
				boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
				boolean isSymRef = f.getType(env).isInstanceOf(tpNodeSymbolRef);
				if (isArr && !f.isAbstract()) {
					f.init = new CastExpr(f.pos, f.getType(env),
						new IFldExpr(f.pos,
							new SFldExpr(f.pos, iface.resolveField(env,("nodeattr$"+f.sname).intern(), true)),
							tpAttrSlot.resolveField("defaultValue")
						)
					);
				}
				if (isSymRef && !f.isAbstract()) {
					f.init = new ReinterpExpr(f.pos, f.getType(env), new NewExpr(f.pos, f.getType(env).getMetaSupers()[0], ENode.emptyArray));
				}
			}
			if (f.isVirtual() && f.getMetaPacked() == null) {
				fixSetterMethod(impl, f, fmatt, fmref);
				fixGetterMethod(impl, f, fmatt, fmref);
			}
		}
	}
	
	private Struct makeNodeValuesClass(Struct iface, Struct impl) {
		foreach (Struct s; iface.members; s.sname == "VVV")
			return s; // already generated or manually specified
		Struct s = this.env.newStruct("VVV",iface,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),null);
		if (!s.isAttached())
			iface.members += s;
	super_vvv:
		foreach (TypeRef st; iface.super_types; isNodeKind(st.getStruct(env)) || nameNode.equals(st.getStruct(env).qname()) || nameANode.equals(st.getStruct(env).qname())) {
			Struct stv = null;
			foreach (Struct v; st.getStruct(env).members; v.sname == "VVV") {
				s.super_types += new TypeRef(v.getType(env));
				break super_vvv;
			}
		}

		Constructor ctor = new Constructor(ACC_PUBLIC);
		ctor.params += new LVar(0,"node",impl.getType(env),Var.VAR_LOCAL,0);
		ctor.body = new Block();
		ctor.block.stats += new ExprStat(new CtorCallExpr(0,new SuperExpr(),new ENode[]{new LVarExpr(0,ctor.params[0])}));
		s.members += ctor;

		foreach (Field f; impl.members; !f.isStatic() && !f.isAbstract() && !f.isFinal() && f.getMeta(mnUnVersioned) == null) {
			Field sf = new Field(f.sname,f.getType(env),ACC_SYNTHETIC);
			s.members += sf;
			ctor.block.stats += new ExprStat(new AssignExpr(0,new IFldExpr(0,new ThisExpr(),sf),new IFldExpr(0,new LVarExpr(0,ctor.params[0]),f,true)));
		}

		Kiev.runProcessorsOn(s);
		return s;
	}
	
	private void collectAllAttrFields(Vector<Field> aflds, Struct iface) {
		if (iface == null)
			return;
		if (iface.qname().equals(nameANode)) {
			aflds.append(iface.resolveField(env,"parent"));
			return;
		}
		if (!isNodeKind(iface))
			return;
	next_fld:
		foreach (Field f; iface.members; !f.isStatic() && (f.getMeta(mnAtt) != null || f.getMeta(mnRef) != null)) {
			foreach (Field af; aflds; f == af || f.sname == "parent")
				continue next_fld;
			aflds.append(f);
		}
		foreach (TypeRef st; iface.super_types)
			collectAllAttrFields(aflds, st.getStruct(env));
	}

	private Field getField(Struct s, String name) {
		Field f = s.resolveField(env,name, false);
		if (f != null)
			return f;
		throw new CompilerException(s,"Auto-generated field with name "+name+" is not found");
	}
	
	private boolean hasMethod(Struct s, String name) {
		s = s.checkResolved(env);
		foreach (Method m; s.members; m.hasName(name)) return true;
		return false;
	}
	
	private void fixGetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);
		boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
		
		if (isSet)
			return;

		Method get_var = f.getGetterMethod();
		if (get_var == null || get_var.isStatic())
			return;
		if (get_var.getMeta(mnAtt) != null || get_var.getMeta(mnRef) != null)
			return; // already generated
		if (get_var.isAbstract()) {
			if (fmatt == null && fmref == null)
				return;
			if (isAtt && !fmatt.getZ(nameExtData) || !isAtt && !fmref.getZ(nameExtData))
				return;
			String fname = ("nodeattr$"+f.sname).intern();
			Field fatt = Env.ctxTDecl(f).resolveField(env,fname);
			get_var.setAbstract(false);
			if (get_var.isSynthetic())
				get_var.setFinal(true);
			Block body = new Block();
			body.stats += new CastExpr(f.getType(env),new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("get"),null,new ENode[]{new ThisExpr()}));
			get_var.body = body;
			Kiev.runProcessorsOn(body);
			return;
		}
		if (get_var.isSynthetic())
			get_var.setFinal(true);
		
		if (s.getType(env).isInstanceOf(tpNode) && f.getMeta(mnUnVersioned) == null && GEN_VERSIONED) {
			get_var.body.walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n instanceof IFldExpr && n.obj instanceof ThisExpr)
						return VNodeME_PreGenerate.this.fixAccessInGetter(f,(IFldExpr)n, parent, slot);
					return true;
				}
			});
		}

		if (isAtt)
			get_var.setMeta(new UserMeta(mnAtt)).resolve(env, null);
		else
			get_var.setMeta(new UserMeta(mnRef)).resolve(env, null);
	}

	private void fixSetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);
		boolean isArr = f.getType(env).isInstanceOf(tpNodeSpace);
		boolean isSet = f.getType(env).isInstanceOf(tpNodeExtSpace);
		
		if (isSet)
			return;

		Method set_var = f.getSetterMethod();
		if (set_var == null || set_var.isStatic())
			return;
		if (set_var.getMeta(mnAtt) != null || set_var.getMeta(mnRef) != null)
			return; // already generated

		Var value = null;
		foreach (Var fp; set_var.params; fp.kind == Var.VAR_LOCAL) {
			value = fp;
			break;
		}
		if (value == null) {
			Kiev.reportError(set_var,"Cannot find a value to assign parameter");
			return;
		}

		Field fatt = null;
		if (fmatt != null || fmref != null)
			fatt = Env.ctxTDecl(f).resolveField(env,("nodeattr$"+f.sname).intern());

		if (set_var.isAbstract()) {
			if (fatt == null)
				return;
			if (isAtt && !fmatt.getZ(nameExtData) || !isAtt && !fmref.getZ(nameExtData))
				return;
			set_var.setAbstract(false);
			if (set_var.isSynthetic())
				set_var.setFinal(true);
			Block body = new Block();
			body.stats += new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("set"),null,new ENode[]{new ThisExpr(),new LVarExpr(0,value)});
			set_var.body = body;
			Kiev.runProcessorsOn(body);
			return;
		}

		if (set_var.isSynthetic())
			set_var.setFinal(true);
		Block body = set_var.block;
		//if (s.getType(env).isInstanceOf(tpNode) && f.getMeta(mnUnVersioned) == null && GEN_VERSIONED) {
		//	body.walkTree(null, null, new ITreeWalker() {
		//		public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
		//			if (n instanceof AssignExpr)
		//				return VNodeME_PreGenerate.this.fixAssignInSetter(f,(AssignExpr)n,parent,slot);
		//			return true;
		//		}
		//	});
		//}
		if (!isArr && isAtt) {
			Var old = new LVar(body.pos,"$old",f.getType(env),Var.VAR_LOCAL,ACC_FINAL);
			old.init = new IFldExpr(0,new ThisExpr(),f);
			body.stats.insert(0,old);
			Kiev.runProcessorsOn(old);
			ENode p_st = new ExprStat(0,
						new CallExpr(0,
							new ThisExpr(),
							new SymbolRef<Method>(nameCallbackDataSet),
							null,
							new ENode[] {
								new SFldExpr(f.pos, fatt),
								new LVarExpr(0, old),
								new LVarExpr(0, value),
								new ConstIntExpr(-1)
							}
						)
					);
			body.stats.append(p_st);
			Kiev.runProcessorsOn(p_st);
		}
		if (isAtt)
			set_var.setMeta(new UserMeta(mnAtt)).resolve(env, null);
		else
			set_var.setMeta(new UserMeta(mnRef)).resolve(env, null);
	}
	
	boolean fixAccessInGetter(Field f, IFldExpr fe, INode parent, AttrSlot slot) {
		if!(fe.obj instanceof ThisExpr && fe.var == f && !fe.var.isFinal())
			return true;
		if (parent instanceof AssignExpr && slot.name == "lval")
			return true;
		if (!fe.getType(env).isInstanceOf(tpNode))
			return true;
		Method csm = getCodeGet();
		ASTNode rewriter = csm.body;
		if (rewriter instanceof RewriteMatch)
			rewriter = rewriter.matchCase(fe);
		Hashtable<String,Object> args = new Hashtable<String,Object>();
		RewriteContext rctx = new RewriteContext(env, fe, args);
		ASTNode rn = (ASTNode)rewriter.doRewrite(rctx);
		rn = fe.replaceWithNode(~rn, parent, slot);
		Kiev.runProcessorsOn(rn);
		return false;
	}
	boolean fixAssignInSetter(Field f, AssignExpr ae, INode parent, AttrSlot slot) {
		if!(ae.lval instanceof IFldExpr && ((IFldExpr)ae.lval).obj instanceof ThisExpr && ((IFldExpr)ae.lval).var == f)
			return true;
		Method csm = getCodeSet();
		ASTNode rewriter = csm.body;
		if (rewriter instanceof RewriteMatch)
			rewriter = rewriter.matchCase(ae);
		Hashtable<String,Object> args = new Hashtable<String,Object>();
		RewriteContext rctx = new RewriteContext(env, ae, args);
		ASTNode rn = (ASTNode)rewriter.doRewrite(rctx);
		rn = ae.replaceWithNode(~rn,parent,slot);
		Kiev.runProcessorsOn(rn);
		return false;
	}

}

public final class VNodeBE_FixResolve extends BackendProcessor {

	@forward
	private VNodeUtils utils;

	public VNodeBE_FixResolve(Env env, int id) { super(env,id,KievBackend.Java15); }
	public String getDescr() { "VNode fix resolved" }

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		if (node instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit)node;
			WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
			if (wthg.setProcessorRun(cu,this))
				return;
			tr = Transaction.enter(tr,"VNodeBE_FixResolve");
			try {
				doProcess(node);
			} finally { tr.leave(); }
		}
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(SyntaxScope:ASTNode ss) {
		foreach (ASTNode dn; ss.members)
			this.doProcess(dn);
	}
	
	final Struct getIFaceOfImpl(Struct impl) {
		if (impl.parent() instanceof Struct) {
			Struct iface = (Struct)impl.parent();
			if (iface.isInterface() && impl.isClazz() && iface.iface_impl == impl)
				return iface;
		}
		return null;
	}
	
	final boolean isNodeImpl(Struct s) {
		if (s.isClazz() && s.getMeta(mnNode) != null)
			return true;
		Struct iface = getIFaceOfImpl(s);
		if (iface != null && iface.getMeta(mnNode) != null)
			return true;
		return false;
	}

	public void doProcess(Struct:ASTNode s) {
		if (s.isInterface() && !s.isMixin() || !isNodeImpl(s)) {
			foreach(Struct sub; s.members)
				doProcess(sub);
			return;
		}

		if (utils == null)
			utils = new VNodeUtils(this.env);

		Struct iface = getIFaceOfImpl(s);
		if (iface == null)
			iface = s;
		Struct impl;
		if (s.isClazz())
			impl = s;
		else
			impl = s.iface_impl;

		foreach(Constructor ctor; impl.members; !ctor.isStatic())
			fixFinalFieldsInit(ctor.block);
	}

	private void fixFinalFieldsInit(Block block) {
		for (int i=0; i < block.stats.length; i++) {
			ANode stat = block.stats[i];
			if (stat instanceof Block) {
				fixFinalFieldsInit((Block)stat);
				continue;
			}
			if (stat instanceof ExprStat) stat = stat.expr;
			if (stat instanceof AssignExpr && stat.lval instanceof IFldExpr) {
				IFldExpr fe = (IFldExpr)((AssignExpr)stat).lval;
				if (fe.obj instanceof ThisExpr && fe.var.isFinal() && fe.var.getMeta(mnAtt) != null) {
					Field f = fe.var;
					fe.setAsField(true);
					Field fatt = Env.ctxTDecl(f).resolveField(env,("nodeattr$"+f.sname).intern());
					ENode p_st = new ExprStat(0,
								new CallExpr(0,
									new ThisExpr(),
									new SymbolRef<Method>(nameCallbackDataSet),
									null,
									new ENode[] {
										new SFldExpr(fe.pos, fatt),
										new ConstNullExpr(),
										new IFldExpr(fe.pos, new ThisExpr(), f),
										new ConstIntExpr(-1)
									}
								)
							);
					block.stats.insert(i+1, p_st);
					Kiev.runProcessorsOn(p_st);
				}
			}
		}
	}

}
