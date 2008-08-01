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
public abstract class VNode_Base extends TransfProcessor {
	private static final String PROP_BASE				= "symade.transf.vnode";
	public static final boolean GEN_VERSIONED			= getPropS(PROP_BASE,"genUnversioned","false") != "true";
	public static final String mnNode					= getPropS(PROP_BASE,"mnNode","kiev\u001fvtree\u001fThisIsANode"); 
	public static final String mnAtt					= getPropS(PROP_BASE,"mnAtt","kiev\u001fvtree\u001fnodeAttr"); 
	public static final String mnRef					= getPropS(PROP_BASE,"mnRef","kiev\u001fvtree\u001fnodeData"); 
	public static final String mnUnVersioned			= getPropS(PROP_BASE,"mnUnVersioned","kiev\u001fvtree\u001fUnVersioned"); 
	public static final String nameINode				= getPropS(PROP_BASE,"nameINode","kiev\u001fvtree\u001fINode");
	public static final String nameANode				= getPropS(PROP_BASE,"nameANode","kiev\u001fvtree\u001fANode");
	public static final String nameNode				= getPropS(PROP_BASE,"nameNode","kiev\u001fvtree\u001fASTNode"); 
	public static final String nameNodeSpace			= getPropS(PROP_BASE,"nameNodeSpace","kiev\u001fvtree\u001fNodeSpace"); 
	public static final String nameNodeExtSpace		= getPropS(PROP_BASE,"nameNodeExtSpace","kiev\u001fvtree\u001fNodeExtSpace");
	public static final String nameNodeSymbolRef		= getPropS(PROP_BASE,"nameNodeSymbolRef","kiev\u001fvtree\u001fNodeSymbolRef");
	public static final String nameTreeWalker			= getPropS(PROP_BASE,"TreeWalker","kiev\u001fvtree\u001fTreeWalker"); 
	public static final String nameAttrSlot			= getPropS(PROP_BASE,"nameAttrSlot","kiev\u001fvtree\u001fAttrSlot"); 
	public static final String nameRefAttrSlot			= getPropS(PROP_BASE,"nameRefAttrSlot","kiev\u001fvtree\u001fRefAttrSlot"); 
	public static final String nameAttAttrSlot			= getPropS(PROP_BASE,"nameAttAttrSlot","kiev\u001fvtree\u001fAttAttrSlot"); 
	public static final String nameExtRefAttrSlot		= getPropS(PROP_BASE,"nameExtRefAttrSlot","kiev\u001fvtree\u001fExtRefAttrSlot"); 
	public static final String nameExtAttAttrSlot		= getPropS(PROP_BASE,"nameExtAttAttrSlot","kiev\u001fvtree\u001fExtAttAttrSlot"); 
	public static final String nameSpaceAttrSlot		= getPropS(PROP_BASE,"nameSpaceAttrSlot","kiev\u001fvtree\u001fSpaceAttrSlot"); 
	public static final String nameSpaceRefAttrSlot	= getPropS(PROP_BASE,"nameSpaceRefAttrSlot","kiev\u001fvtree\u001fSpaceRefAttrSlot"); 
	public static final String nameSpaceAttAttrSlot	= getPropS(PROP_BASE,"nameSpaceAttAttrSlot","kiev\u001fvtree\u001fSpaceAttAttrSlot"); 
	public static final String nameExtSpaceAttrSlot	= getPropS(PROP_BASE,"nameExtSpaceAttrSlot","kiev\u001fvtree\u001fExtSpaceAttrSlot"); 
	public static final String nameLanguageIface		= getPropS(PROP_BASE,"nameLanguageIface","kiev\u001fvlang\u001fLanguage"); 
	public static final String nameCopyContext			= getPropS(PROP_BASE,"nameCopyContext","kiev\u001fvtree\u001fANode\u001fCopyContext"); 
	public static final String nameCopyable			= getPropS(PROP_BASE,"nameCopyable","copyable");
	public static final String nameExtData				= getPropS(PROP_BASE,"nameExtData","ext_data");
	public static final String nameNodeName			= getPropS(PROP_BASE,"nameNodeName","name");
	public static final String nameLangName			= getPropS(PROP_BASE,"nameLangName","lang");
	public static final String nameWalkerVisitor		= getPropS(PROP_BASE,"nameWalkerVisitor","walkTreeFastVisit");
	public static final String nameWalkerVisitNode		= getPropS(PROP_BASE,"nameWalkerVisitNode","visitANode");
	public static final String nameWalkerVisitSpace	= getPropS(PROP_BASE,"nameWalkerVisitSpace","visitANodeSpace");
	
	public static final String operatorPostTypeSpace		= getPropS(PROP_BASE,"operatorPostTypeSpace","T ∅");		// \u2205
	public static final String operatorPostTypeExtSpace		= getPropS(PROP_BASE,"operatorPostTypeExtSpace","T ⋈");		// \u22c8
	public static final String operatorPostTypeSymbolRef	= getPropS(PROP_BASE,"operatorPostTypeSymbolRef","T ⇑");	// \u21d1

	static Type tpINode;
	static Type tpANode;
	static Type tpNode;
	static Type tpNArray;
	static Type tpNodeSpace;
	static Type tpNodeExtSpace;
	static Type tpNodeSymbolRef;
	static Type tpTreeWalker;
	static Type tpAttrSlot;
	static Type tpRefAttrSlot;
	static Type tpAttAttrSlot;
	static Type tpExtRefAttrSlot;
	static Type tpExtAttAttrSlot;
	static Type tpSpaceAttrSlot;
	static Type tpSpaceRefAttrSlot;
	static Type tpSpaceAttAttrSlot;
	static Type tpExtSpaceAttrSlot;
	static Type tpLanguageIface;
	static Type tpCopyContext;
	
	static Operator PostTypeSpace;
	static Operator PostTypeExtSpace;
	static Operator PostTypeSymbolRef;

	VNode_Base() { super(KievExt.VNode); }

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

}

/////////////////////////////////////////////
//      Verify the VNode tree structure    //
/////////////////////////////////////////////

@singleton
public final class VNodeFE_Pass3 extends VNode_Base {

	public String getDescr() { "VNode members creation" }

	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		if (tpINode == null) {
			tpINode = Env.getRoot().loadTypeDecl(nameINode, true).xtype;
			tpANode = Env.getRoot().loadTypeDecl(nameANode, true).xtype;
			tpNode = Env.getRoot().loadTypeDecl(nameNode, true).xtype;
			tpNArray = new ArrayType(tpANode);
			tpNodeSpace = Env.getRoot().loadTypeDecl(nameNodeSpace, true).xtype;
			tpNodeExtSpace = Env.getRoot().loadTypeDecl(nameNodeExtSpace, true).xtype;
			tpNodeSymbolRef = Env.getRoot().loadTypeDecl(nameNodeSymbolRef, true).xtype;
			tpTreeWalker = Env.getRoot().loadTypeDecl(nameTreeWalker, true).xtype;
			tpAttrSlot = Env.getRoot().loadTypeDecl(nameAttrSlot, true).xtype;
			tpRefAttrSlot = Env.getRoot().loadTypeDecl(nameRefAttrSlot, true).xtype;
			tpAttAttrSlot = Env.getRoot().loadTypeDecl(nameAttAttrSlot, true).xtype;
			tpExtRefAttrSlot = Env.getRoot().loadTypeDecl(nameExtRefAttrSlot, true).xtype;
			tpExtAttAttrSlot = Env.getRoot().loadTypeDecl(nameExtAttAttrSlot, true).xtype;
			tpSpaceAttrSlot = Env.getRoot().loadTypeDecl(nameSpaceAttrSlot, true).xtype;
			tpSpaceRefAttrSlot = Env.getRoot().loadTypeDecl(nameSpaceRefAttrSlot, true).xtype;
			tpSpaceAttAttrSlot = Env.getRoot().loadTypeDecl(nameSpaceAttAttrSlot, true).xtype;
			tpExtSpaceAttrSlot = Env.getRoot().loadTypeDecl(nameExtSpaceAttrSlot, true).xtype;
			tpLanguageIface = Env.getRoot().loadTypeDecl(nameLanguageIface, true).xtype;
			tpCopyContext = Env.getRoot().loadTypeDecl(nameCopyContext, true).xtype;
		}
		if (PostTypeSpace == null) {
			PostTypeSpace = Operator.getOperatorByDecl(operatorPostTypeSpace);
			PostTypeExtSpace = Operator.getOperatorByDecl(operatorPostTypeExtSpace);
			PostTypeSymbolRef = Operator.getOperatorByDecl(operatorPostTypeSymbolRef);
		}
		foreach (ASTNode n; fu.members)
			doProcess(n);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach (Struct sub; s.members)
			doProcess(sub);
		if (isNodeKind(s)) {
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
				Kiev.reportWarning(f,"Field "+f+" of non-@node class "+f.ctx_tdecl+" may not be @nodeAttr or @nodeData");
			}
		}
	}
	
	public void doProcess(Field:ASTNode f) {
		UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
		UserMeta fmref = (UserMeta)f.getMeta(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @nodeAttr="+fmatt+" and @nodeData="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" marked both @nodeAttr and @nodeData");
		}
		if (fmatt != null || fmref != null) {
			if (f.isStatic())
				Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" is static and cannot have @nodeAttr or @nodeData");
			boolean isArr = false;
			{
				Type ft = f.getType();
				if (ft.isInstanceOf(tpNArray)) {
					boolean isExtData = fmatt != null ? fmatt.getZ(nameExtData) : fmref.getZ(nameExtData);
					if (!isExtData) {
						if (!ft.isInstanceOf(tpNodeSpace)) {
							Kiev.reportWarning(f,"Use node space operator '"+operatorPostTypeSpace+"' instead of 'T []'");
							TypeExpr te = (TypeExpr)f.vtype;
							te.op = VNode_Base.PostTypeSpace;
							te.ident = VNode_Base.PostTypeSpace.name;
							te.type_lnk = null;
							te.getType();
							ft = f.getType();
						}
					} else {
						if (!ft.isInstanceOf(tpNodeExtSpace)) {
							Kiev.reportWarning(f,"Use node extended space operator '"+operatorPostTypeExtSpace+"' instead of 'T []'"); // ⋈
							TypeExpr te = (TypeExpr)f.vtype;
							te.op = VNode_Base.PostTypeExtSpace;
							te.ident = VNode_Base.PostTypeExtSpace.name;
							te.type_lnk = null;
							te.getType();
							ft = f.getType();
						}
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
				Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" may not have initializer");
			}
		}
		else if (!f.isStatic() && !f.isInterfaceOnly() && !f.isAbstract()) {
			if (f.getType().isInstanceOf(tpNArray))
				Kiev.reportWarning(f,"Field "+f.ctx_tdecl+"."+f+" must be marked with @nodeAttr or @nodeData");
			else if (isNodeKind(f.getType()))
				Kiev.reportWarning(f,"Field "+f.ctx_tdecl+"."+f+" must be marked with @nodeAttr or @nodeData");
		}
		if (!f.isStatic() && !f.isAbstract() && !f.isFinal() && f.getMeta(mnUnVersioned) == null)
			f.setVirtual(true);
	}
}

//////////////////////////////////////////////////////
//   Generate class members (enumerate sub-nodes)   //
//////////////////////////////////////////////////////

@singleton
public final class VNodeFE_GenMembers extends VNode_Base {
	public String getDescr() { "VNode members generation" }

	private Vector<Struct> processed;
	
	private Type makeNodeAttrClass(Struct snode, Field f) {
		UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
		UserMeta fmref = (UserMeta)f.getMeta(mnRef);
		boolean isAtt = (fmatt != null);
		boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
		boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
		boolean isExtData = isAtt ? fmatt.getZ(nameExtData) : fmref.getZ(nameExtData);
		Type ft = f.getType();
		Type clz_tp = (isArr || isSet) ? ft.resolveArg(0) : ft;
		String sname = ("NodeAttr_"+f.sname).intern();
		foreach (TypeDecl td; snode.members; td.sname == sname) {
			if (!f.isInterfaceOnly())
				Kiev.reportWarning(td,"Class "+snode+"."+sname+" already exists and will not be generated");
			return td.xtype;
		}
		Struct s = Env.getRoot().newStruct(sname,true,snode,ACC_FINAL|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),true,null);
		if (!s.isAttached())
			snode.members.add(s);
		{
			String nameTreePkg = nameANode.substring(0,nameANode.lastIndexOf('\u001f'));
			foreach (UserMeta m; f.metas; m.qname.startsWith(nameTreePkg))
				s.setMeta(m.ncopy());
		}
		if (isSet) {
			TVarBld set = new TVarBld();
			set.append(tpExtSpaceAttrSlot.getStruct().args[0].getAType(), clz_tp);
			s.super_types.insert(0, new TypeRef(tpExtSpaceAttrSlot.applay(set)));
		}
		else if (isArr) {
			if (isAtt) {
				TVarBld set = new TVarBld();
				set.append(tpSpaceAttAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_types.insert(0, new TypeRef(tpSpaceAttAttrSlot.applay(set)));
			} else {
				TVarBld set = new TVarBld();
				set.append(tpSpaceRefAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_types.insert(0, new TypeRef(tpSpaceRefAttrSlot.applay(set)));
			}
		}
		else {
			if (isAtt) {
				if (isExtData)
					s.super_types.insert(0, new TypeRef(tpExtAttAttrSlot));
				else
					s.super_types.insert(0, new TypeRef(tpAttAttrSlot));
			} else {
				if (isExtData)
					s.super_types.insert(0, new TypeRef(tpExtRefAttrSlot));
				else
					s.super_types.insert(0, new TypeRef(tpRefAttrSlot));
			}
		}
		// make constructor
		{
			Constructor ctor = new Constructor(0);
			ctor.params.add(new LVar(0, "name", Type.tpString, Var.PARAM_NORMAL, ACC_FINAL));
			ctor.params.add(new LVar(0, "typeinfo", Type.tpTypeInfo, Var.PARAM_NORMAL, ACC_FINAL));
			s.members.add(ctor);
			Constructor sctor = (Constructor)s.super_types[0].getStruct().resolveMethod(null,Type.tpVoid,Type.tpString,Type.tpTypeInfo);
			CtorCallExpr ce = new CtorCallExpr(f.pos,
					new SuperExpr(),
					new ENode[]{
						new LVarExpr(f.pos, ctor.params[0]),
						new LVarExpr(f.pos, ctor.params[1])
					}
				);
			ce.symbol = sctor;
			ctor.body = new Block(0);
			ctor.block.stats.add(new ExprStat(ce));
		}
		if (!isExtData) {
			if (isArr) {
				// add public N[] get(ASTNode parent)
				{
					Method getArr = new MethodImpl("getArray",new ArrayType(tpANode),ACC_PUBLIC | ACC_SYNTHETIC);
					getArr.params.add(new LVar(0, "parent", tpANode, Var.PARAM_NORMAL, ACC_FINAL));
					s.addMethod(getArr);
					getArr.body = new Block(0);
					ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, getArr.params[0]) ), f);
					getArr.block.stats.add(new ReturnStat(f.pos,val));
				}
				// add public void set(ASTNode parent, N[]:Object narr)
				{
					Method setArr = new MethodImpl("setArray",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
					setArr.params.add(new LVar(0, "parent", tpANode, Var.PARAM_NORMAL, ACC_FINAL));
					setArr.params.add(new LVar(0, "narr", Type.tpObject /*new ArrayType(tpANode)*/, Var.PARAM_NORMAL, ACC_FINAL));
					s.addMethod(setArr);
					setArr.body = new Block(0);
					ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, setArr.params[0]) ), f);
					setArr.block.stats.add(new ExprStat(
						new AssignExpr(f.pos,Operator.Assign,lval,new CastExpr(f.pos, f.getType(), new LVarExpr(0, setArr.params[1])))
					));
				}
			} else {
				// add public N[] get(ASTNode parent)
				{
					Method getVal = new MethodImpl("get",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
					getVal.params.add(new LVar(0, "parent", tpANode, Var.PARAM_NORMAL, ACC_FINAL));
					s.addMethod(getVal);
					getVal.body = new Block(0);
					ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, getVal.params[0]) ), f);
					getVal.block.stats.add(new ReturnStat(f.pos,val));
					if!(val.getType().isReference())
						CastExpr.autoCastToReference(val);
				}
				// add public void set(ASTNode parent, Object narr)
				{
					Method setVal = new MethodImpl("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
					setVal.params.add(new LVar(0, "parent", tpANode, Var.PARAM_NORMAL, ACC_FINAL));
					setVal.params.add(new LVar(0, "val", Type.tpObject, Var.PARAM_NORMAL, ACC_FINAL));
					s.addMethod(setVal);
					setVal.body = new Block(0);
					if (!f.isFinal() && MetaAccess.writeable(f)) {
						ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, setVal.params[0]) ), f);
						ENode val = new LVarExpr(0, setVal.params[1]);
						Type ftp = f.getType();
						if (ftp.isReference())
							val = new CastExpr(0,ftp,val);
						else
							val = new CastExpr(0,((CoreType)ftp).getRefTypeForPrimitive(),val);
						setVal.block.stats.add(new ExprStat(
							new AssignExpr(f.pos,Operator.Assign,lval,val)
						));
						if!(ftp.isReference())
							CastExpr.autoCastToPrimitive(val);
					} else {
						ConstStringExpr msg = new ConstStringExpr((isAtt ? "@nodeAttr " : "@nodeData ")+f.sname+" is not writeable");
						setVal.block.stats.add(
							new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
						);
					}
				}
				// add public boolean isWrittable()
				if (f.isFinal() || !MetaAccess.writeable(f)) {
					Method isWrittable = new MethodImpl("isWrittable",Type.tpBoolean,ACC_PUBLIC | ACC_SYNTHETIC);
					s.addMethod(isWrittable);
					isWrittable.body = new Block(0);
					isWrittable.block.stats.add(new ReturnStat(f.pos,new ConstBoolExpr(false)));
				}
			}
		}

		Kiev.runProcessorsOn(s);
		return s.xtype;
	}
	
	public void process(ASTNode node, Transaction tr) {
		processed = null;
		doProcess(node);
		processed = null;
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	private void doProcess(Struct:ASTNode s) {
		if (processed == null)
			processed = new Vector<Struct>();
		processed.append(s);
		foreach (Struct dn; s.members)
			this.doProcess(dn);
		if (!isNodeImpl(s) || (s.isInterface() && !s.isMixin()))
			return;
		foreach (Field f; s.members; f.sname == nameEnumValuesFld)
			return; // already generated

		foreach (TypeRef st; s.super_types; isNodeKind(st.getStruct()))
			this.doProcess(st.getStruct());
		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		foreach (Field f; s.members; !f.isStatic() && (f.getMeta(mnAtt) != null || f.getMeta(mnRef) != null))
			aflds.append(f);
		foreach (Field f; aflds) {
			boolean isAtt = (f.getMeta(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
			boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
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
			} else {
				s.addField(new Field(fname, tpa, ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
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
	}
}

////////////////////////////////////////////////////
//	   PASS - verification                        //
////////////////////////////////////////////////////

@singleton
public final class VNodeFE_Verify extends VerifyProcessor {

	public VNodeFE_Verify() { super(KievExt.VNode); }

	public void verify(ASTNode node) {
		if (node instanceof Struct && VNode_Base.isNodeKind((Struct)node))
			verifyStruct((Struct)node);
	}
	
	private void verifyStruct(Struct s) {
		UserMeta m = (UserMeta)s.getMeta(VNode_Base.mnNode);
		MetaValueScalar langValue = (MetaValueScalar)m.get(VNode_Base.nameLangName);
		TypeDecl td;
		if (langValue.value instanceof TypeClassExpr)
			td = ((TypeClassExpr)langValue.value).ttype.getTypeDecl();
		else
			td = ((TypeRef)langValue.value).getTypeDecl();
		if (td.xtype ≡ StdTypes.tpVoid)
			return;
		if!(td instanceof Struct) {
			Kiev.reportError(m,"Language '"+td+"' is not a class");
			return;
		}
		if (td.isInterfaceOnly())
			return;
		Field nodeClasses = td.resolveField("nodeClasses", false);
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
			if (tce.ttype.getStruct() == s) {
				found = true;
				break;
			}
		}
		if (!found) {
			Kiev.reportError(m,"Language '"+td+"' has no reference to @node '"+s+"' in field 'nodeClasses'");
			return;
		}
	}
	
}

@singleton
public class VNodeME_PreGenerate extends BackendProcessor {
	public static final String nameMetaGetter	= VirtFldFE_GenMembers.nameMetaGetter; 
	public static final String nameMetaSetter	= VirtFldFE_GenMembers.nameMetaSetter; 

	static Type tpINode;
	static Type tpANode;
	static Type tpNode;
	static Type tpNodeSpace;
	static Type tpNodeExtSpace;
	static Type tpNodeSymbolRef;
	static Type tpAttrSlot;
	static Type tpSpaceAttrSlot;
	static Type tpCopyContext;
	static Type tpLanguageIface;
	
	static Method _codeSet;
	static Method _codeGet;
	
	private VNodeME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "VNode pre-generation" }

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"VNodeME_PreGenerate");
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		if (tpINode == null) {
			tpINode = VNode_Base.tpINode;
			tpANode = VNode_Base.tpANode;
			tpNode = VNode_Base.tpNode;
			tpNodeSpace = VNode_Base.tpNodeSpace;
			tpNodeExtSpace = VNode_Base.tpNodeExtSpace;
			tpNodeSymbolRef = VNode_Base.tpNodeSymbolRef;
			tpAttrSlot = VNode_Base.tpAttrSlot;
			tpSpaceAttrSlot = VNode_Base.tpSpaceAttrSlot;
			tpCopyContext = VNode_Base.tpCopyContext;
			tpLanguageIface = VNode_Base.tpLanguageIface;
		}
		foreach (ASTNode dn; fu.members)
			this.doProcess(dn);
	}
	
	public Method getCodeSet() {
		if (_codeSet == null) {
			TypeDecl td = (TypeDecl)Env.getRoot().loadTypeDecl("kiev\u001ftransf\u001fTemplateVNode", true);
			_codeSet = td.resolveMethod("codeSet", StdTypes.tpVoid)
		}
		return _codeSet;
	}
	
	public Method getCodeGet() {
		if (_codeGet == null) {
			TypeDecl td = (TypeDecl)Env.getRoot().loadTypeDecl("kiev\u001ftransf\u001fTemplateVNode", true);
			_codeGet = td.resolveMethod("codeGet", StdTypes.tpVoid)
		}
		return _codeGet;
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
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
		if (s.isClazz() && s.getMeta(VNode_Base.mnNode) != null)
			return true;
		Struct iface = getIFaceOfImpl(s);
		if (iface != null && iface.getMeta(VNode_Base.mnNode) != null)
			return true;
		return false;
	}

	final boolean isNodeIface(Struct s) {
		if (s.isInterface() && s.getMeta(VNode_Base.mnNode) != null)
			return true;
		return false;
	}

	public void doProcess(Struct:ASTNode s) {
		if (s.isInterface() && !s.isMixin() || !isNodeImpl(s)) {
			foreach(Struct sub; s.members)
				doProcess(sub);
			return;
		}
		
		Struct iface = getIFaceOfImpl(s);
		if (iface == null)
			iface = s;
		Struct impl;
		if (s.isClazz())
			impl = s;
		else
			impl = s.iface_impl;

		foreach (TypeRef st; iface.super_types; isNodeKind(st.getStruct()))
			this.doProcess(st.getStruct());

		Field vals = null;
		foreach (Field f; iface.members; f.sname == nameEnumValuesFld) {
			vals = f;
			break;
		}
		if (vals == null)
			throw new CompilerException(s,"Auto-generated field with name "+nameEnumValuesFld+" is not found");
		if (vals.init != null || VNode_Base.nameNode.equals(iface.qname()) || VNode_Base.nameANode.equals(iface.qname()))
			return; // already generated

		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		collectAllAttrFields(aflds, iface);
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.getMeta(VNode_Base.mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
			boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
			if (f.ctx_tdecl != iface) {
				vals_init[i] = new SFldExpr(f.pos, iface.resolveField(("nodeattr$"+f.sname).intern(), true));
				continue;
			}
			Type ft = f.getType();
			Type clz_tp = (isArr || isSet) ? ft.resolveArg(0) : ft;
			Field nodeattr_f = getField(iface,("nodeattr$"+f.sname).intern());
			TypeInfoExpr clz_expr = new TypeInfoExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, nodeattr_f.getType(), new ENode[]{
					new ConstStringExpr(f.sname),
					clz_expr
				});
			nodeattr_f.init = e;
			Kiev.runProcessorsOn(e);
			vals_init[i] = new SFldExpr(nodeattr_f.pos, nodeattr_f);
		}
		vals.init = new NewInitializedArrayExpr(0, new TypeExpr(tpAttrSlot,Operator.PostTypeArray), vals_init);
		Kiev.runProcessorsOn(vals.init);

		// AttrSlot[] values() { return $values; }
		if (hasMethod(iface, nameEnumValues)) {
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

		// Language getCompilerLang()
		if (!iface.xtype.isInstanceOf(tpNode) || hasMethod(impl, "getCompilerLang")) {
			//Kiev.reportWarning(s,"Method "+s+"."+"getCompilerLang already exists, @node member is not generated");
		} else {
			Method lng = new MethodImpl("getCompilerLang",tpLanguageIface,ACC_PUBLIC | ACC_SYNTHETIC);
			impl.addMethod(lng);
			if (!s.isInterfaceOnly()) {
				lng.body = new Block();
				UserMeta m = (UserMeta)iface.getMeta(VNode_Base.mnNode);
				MetaValueScalar langValue = (MetaValueScalar)m.get(VNode_Base.nameLangName);
				TypeDecl td;
				if (langValue.value instanceof TypeClassExpr)
					td = ((TypeClassExpr)langValue.value).ttype.getTypeDecl();
				else
					td = ((TypeRef)langValue.value).getTypeDecl();
				ENode res = null;
				if (td.xtype ≡ StdTypes.tpVoid)
					res = new ConstNullExpr();
				else if (!td.isSingleton())
					res = new ConstNullExpr();
				else
					res = new TypeRef(td.xtype);
				lng.block.stats.append(new ReturnStat(0,res));
				Kiev.runProcessorsOn(lng);
			}
		}

		// String getCompilerNodeName()
		if (!iface.xtype.isInstanceOf(tpNode) || hasMethod(impl, "getCompilerNodeName")) {
			//Kiev.reportWarning(s,"Method "+s+"."+"getCompilerNodeName already exists, @node member is not generated");
		} else {
			Method nname = new MethodImpl("getCompilerNodeName",StdTypes.tpString,ACC_PUBLIC | ACC_SYNTHETIC);
			impl.addMethod(nname);
			if (!s.isInterfaceOnly()) {
				nname.body = new Block();
				UserMeta m = (UserMeta)iface.getMeta(VNode_Base.mnNode);
				String nm = m.getS(VNode_Base.nameNodeName);
				if (nm == null || nm.length() == 0)
					nm = iface.sname;
				nname.block.stats.append(new ReturnStat(0,new ConstStringExpr(nm)));
				Kiev.runProcessorsOn(nname);
			}
		}

		// copyTo(Object)
		if (iface.getMeta(VNode_Base.mnNode) != null && !((UserMeta)iface.getMeta(VNode_Base.mnNode)).getZ(VNode_Base.nameCopyable)) {
			// node is not copyable
		}
		else if (hasMethod(impl, "copyTo")) {
			Kiev.reportWarning(impl,"Method "+impl+"."+"copyTo(...) already exists, @node member is not generated");
		} else {
			Method copyV = new MethodImpl("copyTo",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			Var cc;
			copyV.params.append(new LVar(0,"to$node", Type.tpObject, Var.PARAM_NORMAL, 0));
			copyV.params.append(cc=new LVar(0,"in$context", tpCopyContext, Var.PARAM_NORMAL, 0));
			impl.addMethod(copyV);
			if (!s.isInterfaceOnly()) {
				copyV.body = new Block();
				Var v = new LVar(0,"node",impl.xtype,Var.VAR_LOCAL,0);
				if (VNode_Base.isNodeKind(impl.super_types[0].getStruct())) {
					CallExpr cae = new CallExpr(0,new SuperExpr(),
						new SymbolRef<Method>("copyTo"),null,new ENode[]{new LVarExpr(0,copyV.params[0]), new LVarExpr(0,cc)});
					v.init = new CastExpr(0,impl.xtype,cae);
					copyV.block.addSymbol(v);
				} else {
					v.init = new CastExpr(0,impl.xtype,new EToken(0,"to$node",ETokenKind.IDENTIFIER,true));
					copyV.block.addSymbol(v);
				}
				foreach (Field f; impl.members) {
					if (f.isAbstract() || f.isStatic())
						continue;
					{	// check if we may not copy the field
						UserMeta fmeta = (UserMeta)f.getMeta(VNode_Base.mnAtt);
						if (fmeta == null)
							fmeta = (UserMeta)f.getMeta(VNode_Base.mnRef);
						if (fmeta != null && !fmeta.getZ(VNode_Base.nameCopyable))
							continue; // do not copy the field
					}
					boolean isNode = (VNode_Base.isNodeKind(f.getType()));
					boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
					boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
					if (f.getMeta(VNode_Base.mnAtt) != null && (isNode || isArr)) {
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
									new BinaryBoolExpr(0, Operator.NotEquals,
										new IFldExpr(0,new ThisExpr(),f),
										new ConstNullExpr()
										),
									new ExprStat(0,
										new AssignExpr(0,Operator.Assign,
											new IFldExpr(0,new LVarExpr(0,v),f),
											new CastExpr(0,f.getType(),cae)
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
									new AssignExpr(0,Operator.Assign,
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

		// protected void walkTreeFastVisit(TreeWalker $walker) {
		//  $walker.visitANode(obj);
		//  $walker.visitANodeSpace(space);
		//  super.walkTreeFastVisit($walker);
		// }
		if (hasMethod(iface, VNode_Base.nameWalkerVisitor)) {
			Kiev.reportWarning(iface,"Method "+iface+"."+VNode_Base.nameWalkerVisitor+" already exists, @node member is not generated");
		} else {
			if (!s.isInterfaceOnly()) {
				Method walker = new MethodImpl(VNode_Base.nameWalkerVisitor,StdTypes.tpVoid,ACC_PROTECTED | ACC_SYNTHETIC);
				walker.params.append(new LVar(0,"$walker", VNode_Base.tpTreeWalker, Var.PARAM_NORMAL, 0));
				walker.body = new Block(0);
				foreach (Field f; aflds; f.parent() == iface) {
					UserMeta matt = (UserMeta)f.getMeta(VNode_Base.mnAtt);
					if (matt == null)
						continue;
					if (matt.getZ(VNode_Base.nameExtData))
						continue;
					boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
					if !(isArr || VNode_Base.isNodeKind(f.getType()))
						continue;
					String call_name = VNode_Base.nameWalkerVisitNode;
					if (isArr)
						call_name = VNode_Base.nameWalkerVisitSpace;
					walker.block.stats.add(
						new ExprStat(
							new CallExpr(0,
								new LVarExpr(0,walker.params[0]),
								new SymbolRef<Method>(call_name),
								TypeRef.emptyArray,
								new ENode[]{ new IFldExpr(0, new ThisExpr(), f) }
							)
						)
					);
				}
				if (walker.block.stats.length > 0) {
					walker.block.stats.add(
						new ExprStat(
							new CallExpr(0,
								new SuperExpr(),
								new SymbolRef<Method>(VNode_Base.nameWalkerVisitor),
								TypeRef.emptyArray,
								new ENode[]{ new LVarExpr(0,walker.params[0]) }
							)
						)
					);
					walker.block.stats.add(new ReturnStat());
					impl.addMethod(walker);
					Kiev.runProcessorsOn(walker);
				}
			}
		}

		if (VNode_Base.GEN_VERSIONED) {
			if (iface.xtype.isInstanceOf(tpNode))
				makeNodeValuesClass(iface, impl);
	
			Type tpNodeVVV = ((TypeDecl)Env.getRoot().loadTypeDecl(VNode_Base.nameNode+"\u001fVVV", true)).xtype;
			// nodeRestore(ASTNode.VVV from), a reverted nodeBackup()
			if (!iface.xtype.isInstanceOf(tpNode) || hasMethod(impl, "nodeRestore")) {
				//Kiev.reportWarning(s,"Method "+s+"."+"nodeRestore already exists, @node member is not generated");
			} else {
				Method mkRstr = new MethodImpl("nodeRestore",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
				mkRstr.params.append(new LVar(0,"from", tpNodeVVV, Var.PARAM_NORMAL, 0));
				impl.addMethod(mkRstr);
				if (!s.isInterfaceOnly()) {
					mkRstr.body = new Block();
					Struct sVVV = null;
					foreach (Struct vvv; iface.members; vvv.sname == "VVV") {
						sVVV = vvv;
						break;
					}
					Var v = new LVar(0,"from",sVVV.xtype,Var.VAR_LOCAL,0);
					v.init = new CastExpr(0,sVVV.xtype,new LVarExpr(0,mkRstr.params[0]));
					mkRstr.block.stats += v;
					foreach (Field f; impl.members) {
						if (f.isAbstract() || f.isStatic() || f.isFinal() || f.getMeta(VNode_Base.mnUnVersioned) != null)
							continue;
						Field vf = null;
						foreach (Field vvv; sVVV.members; vvv.sname == f.sname) {
							vf = vvv;
							break;
						}
						mkRstr.block.stats.append( 
							new ExprStat(0,
								new AssignExpr(0,Operator.Assign,
									new IFldExpr(0,new ThisExpr(),f,true),
									new IFldExpr(0,new LVarExpr(0,v),vf,true)
								)
							)
						);
					}
					CallExpr cae = new CallExpr(0,new SuperExpr(),
						new SymbolRef<Method>("nodeRestore"),null,new ENode[]{new LVarExpr(0,v)});
					mkRstr.block.stats += cae;
					Kiev.runProcessorsOn(mkRstr);
				}
			}
			// nodeBackup()
			if (!iface.xtype.isInstanceOf(tpNode) || hasMethod(impl, "nodeBackup")) {
				//Kiev.reportWarning(s,"Method "+s+"."+"nodeBackup already exists, @node member is not generated");
			} else {
				Method mkBkup = new MethodImpl("nodeBackup",tpNodeVVV,ACC_PUBLIC | ACC_SYNTHETIC);
				impl.addMethod(mkBkup);
				if (!s.isInterfaceOnly()) {
					mkBkup.body = new Block();
					Type tpVVV = null;
					foreach (Struct vvv; iface.members; vvv.sname == "VVV") {
						tpVVV = vvv.xtype;
						break;
					}
					mkBkup.block.stats.append(new ReturnStat(0,new NewExpr(0,tpVVV,new ENode[]{new CastExpr(impl.xtype,new ThisExpr())})));
					Kiev.runProcessorsOn(mkBkup);
				}
			}
		}

		if (s.isInterfaceOnly())
			return;

		// fix methods
		foreach(Field f; impl.members; !f.isStatic()) {
			UserMeta fmatt = (UserMeta)f.getMeta(VNode_Base.mnAtt);
			UserMeta fmref = (UserMeta)f.getMeta(VNode_Base.mnRef);
			if (fmatt != null || fmref != null) {
				boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
				boolean isSymRef = f.getType().isInstanceOf(tpNodeSymbolRef);
				if (isArr && !f.isAbstract()) {
					f.init = new CastExpr(f.pos, f.getType(),
						new IFldExpr(f.pos,
							new SFldExpr(f.pos, iface.resolveField(("nodeattr$"+f.sname).intern(), true)),
							tpAttrSlot.resolveField("defaultValue")
						)
					);
				}
				if (isSymRef && !f.isAbstract()) {
					f.init = new ReinterpExpr(f.pos, f.getType(), new NewExpr(f.pos, f.getType().getMetaSupers()[0], ENode.emptyArray));
				}
			}
			if (f.isVirtual() && f.getMetaPacked() == null) {
				fixSetterMethod(impl, f, fmatt, fmref);
				fixGetterMethod(impl, f, fmatt, fmref);
			}
		}
	}
	
	private Struct makeNodeValuesClass(Struct iface, Struct impl) {
		foreach (Struct s; iface.members; s.sname == "VVV") {
			if (!s.isSynthetic())
				return s;
		}
		Struct s = Env.getRoot().newStruct("VVV",true,iface,ACC_PUBLIC|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),true,null);
		if (!s.isAttached())
			iface.members += s;
	super_vvv:
		foreach (TypeRef st; iface.super_types; isNodeKind(st.getStruct()) || VNode_Base.nameNode.equals(st.getStruct().qname()) || VNode_Base.nameANode.equals(st.getStruct().qname())) {
			Struct stv = null;
			foreach (Struct v; st.getStruct().members; v.sname == "VVV") {
				s.super_types += new TypeRef(v.xtype);
				break super_vvv;
			}
		}

		Constructor ctor = new Constructor(ACC_PUBLIC);
		ctor.params += new LVar(0,"node",impl.xtype,Var.PARAM_NORMAL,0);
		ctor.body = new Block();
		ctor.block.stats += new ExprStat(new CtorCallExpr(0,new SuperExpr(),new ENode[]{new LVarExpr(0,ctor.params[0])}));
		s.members += ctor;

		foreach (Field f; impl.members; !f.isStatic() && !f.isAbstract() && !f.isFinal() && f.getMeta(VNode_Base.mnUnVersioned) == null) {
			Field sf = new Field(f.sname,f.getType(),ACC_SYNTHETIC);
			s.members += sf;
			ctor.block.stats += new ExprStat(new AssignExpr(0,Operator.Assign,new IFldExpr(0,new ThisExpr(),sf),new IFldExpr(0,new LVarExpr(0,ctor.params[0]),f,true)));
		}

		Kiev.runProcessorsOn(s);
		return s;
	}
	
	private void collectAllAttrFields(Vector<Field> aflds, Struct iface) {
		if (iface == null)
			return;
		if (iface.qname.equals(VNode_Base.nameANode)) {
			aflds.append(iface.resolveField("parent"));
			return;
		}
		if (!isNodeKind(iface))
			return;
	next_fld:
		foreach (Field f; iface.members; !f.isStatic() && (f.getMeta(VNode_Base.mnAtt) != null || f.getMeta(VNode_Base.mnRef) != null)) {
			foreach (Field af; aflds; f == af || f.sname == "parent")
				continue next_fld;
			aflds.append(f);
		}
		foreach (TypeRef st; iface.super_types)
			collectAllAttrFields(aflds, st.getStruct());
	}

	private Field getField(Struct s, String name) {
		Field f = s.resolveField(name, false);
		if (f != null)
			return f;
		throw new CompilerException(s,"Auto-generated field with name "+name+" is not found");
	}
	
	private boolean hasMethod(Struct s, String name) {
		s.checkResolved();
		foreach (Method m; s.members; m.hasName(name)) return true;
		return false;
	}
	
	private void fixGetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);
		boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
		
		if (isSet)
			return;

		Method get_var = f.getGetterMethod();
		if (get_var == null || get_var.isStatic())
			return;
		if (get_var.getMeta(VNode_Base.mnAtt) != null || get_var.getMeta(VNode_Base.mnRef) != null)
			return; // already generated
		if (get_var.isAbstract()) {
			if (fmatt == null && fmref == null)
				return;
			if (isAtt && !fmatt.getZ(VNode_Base.nameExtData) || !isAtt && !fmref.getZ(VNode_Base.nameExtData))
				return;
			String fname = ("nodeattr$"+f.sname).intern();
			Field fatt = f.ctx_tdecl.resolveField(fname);
			get_var.setAbstract(false);
			if (get_var.isSynthetic())
				get_var.setFinal(true);
			Block body = new Block();
			body.stats += new CastExpr(f.getType(),new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("get"),null,new ENode[]{new ThisExpr()}));
			get_var.body = body;
			Kiev.runProcessorsOn(body);
			return;
		}
		if (get_var.isSynthetic())
			get_var.setFinal(true);
		
		if (s.xtype.isInstanceOf(tpNode) && f.getMeta(VNode_Base.mnUnVersioned) == null && VNode_Base.GEN_VERSIONED) {
			get_var.body.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n instanceof IFldExpr && n.obj instanceof ThisExpr)
						return VNodeME_PreGenerate.this.fixAccessInGetter(f,(IFldExpr)n);
					return true;
				}
			});
		}

		if (isAtt)
			get_var.setMeta(new UserMeta(VNode_Base.mnAtt)).resolve(null);
		else
			get_var.setMeta(new UserMeta(VNode_Base.mnRef)).resolve(null);
	}

	private void fixSetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);
		boolean isArr = f.getType().isInstanceOf(tpNodeSpace);
		boolean isSet = f.getType().isInstanceOf(tpNodeExtSpace);
		
		if (isSet)
			return;

		Method set_var = f.getSetterMethod();
		if (set_var == null || set_var.isStatic())
			return;
		if (set_var.getMeta(VNode_Base.mnAtt) != null || set_var.getMeta(VNode_Base.mnRef) != null)
			return; // already generated

		Var value = null;
		foreach (Var fp; set_var.params; fp.kind == Var.PARAM_NORMAL) {
			value = fp;
			break;
		}
		if (value == null) {
			Kiev.reportError(set_var,"Cannot find a value to assign parameter");
			return;
		}

		Field fatt = null;
		if (fmatt != null || fmref != null)
			fatt = f.ctx_tdecl.resolveField(("nodeattr$"+f.sname).intern());

		if (set_var.isAbstract()) {
			if (fatt == null)
				return;
			if (isAtt && !fmatt.getZ(VNode_Base.nameExtData) || !isAtt && !fmref.getZ(VNode_Base.nameExtData))
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
		if (s.xtype.isInstanceOf(tpNode) && f.getMeta(VNode_Base.mnUnVersioned) == null && VNode_Base.GEN_VERSIONED) {
			body.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n instanceof AssignExpr)
						return VNodeME_PreGenerate.this.fixAssignInSetter(f,(AssignExpr)n);
					return true;
				}
			});
		}
		if (!isArr && isAtt) {
			if (f.getType().isInstanceOf(VNode_Base.tpANode)) {
				ENode p_st = new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.NotEquals,
							new IFldExpr(0,new ThisExpr(),f),
							new ConstNullExpr()
						),
						new Block(0,new ENode[]{
							new ExprStat(0,
								new CallExpr(0,
									new IFldExpr(0,new ThisExpr(),f),
									new SymbolRef<Method>("callbackDetached"),
									null,
									new ENode[] {
										new ThisExpr(),
										new SFldExpr(f.pos, fatt)
									}
								)
							)
						}),
						null
					);
				body.stats.insert(0,p_st);
				Kiev.runProcessorsOn(p_st);
				p_st = new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.NotEquals,
							new LVarExpr(0, value),
							new ConstNullExpr()
						),
						new ExprStat(0,
							new CallExpr(0,
								new LVarExpr(0, value),
								new SymbolRef<Method>("callbackAttached"),
								null,
								new ENode[] {
									new ThisExpr(),
									new SFldExpr(f.pos, fatt)
								}
							)
						),
						null
					);
				body.stats.append(p_st);
				Kiev.runProcessorsOn(p_st);
			} else {
				Var old = new LVar(body.pos,"$old",f.getType(),Var.VAR_LOCAL,ACC_FINAL);
				old.init = new IFldExpr(0,new ThisExpr(),f,!f.isAbstract());
				body.stats.insert(0,old);
				Kiev.runProcessorsOn(old);
				ENode p_st = new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.NotEquals,
							new LVarExpr(0, value),
							new LVarExpr(0, old)
						),
						new ExprStat(0,
							new CallExpr(0,
								new ThisExpr(),
								new SymbolRef<Method>("callbackChildChanged"),
								null,
								new ENode[] {
									new SFldExpr(f.pos, fatt)
								}
							)
						),
						null
					);
				body.stats.append(p_st);
				Kiev.runProcessorsOn(p_st);
			}
		}
		if (isAtt)
			set_var.setMeta(new UserMeta(VNode_Base.mnAtt)).resolve(null);
		else
			set_var.setMeta(new UserMeta(VNode_Base.mnRef)).resolve(null);
	}
	
	boolean fixAccessInGetter(Field f, IFldExpr fe) {
		if!(fe.obj instanceof ThisExpr && fe.var == f && !fe.var.isFinal())
			return true;
		if (fe.parent() instanceof AssignExpr && fe.pslot().name == "lval")
			return true;
		Method csm = getCodeGet();
		ASTNode rewriter = csm.body;
		if (rewriter instanceof RewriteMatch)
			rewriter = rewriter.matchCase(fe);
		Hashtable<String,Object> args = new Hashtable<String,Object>();
		RewriteContext rctx = new RewriteContext(fe, args);
		ASTNode rn = (ASTNode)rewriter.doRewrite(rctx);
		rn = fe.replaceWithNode(~rn);
		Kiev.runProcessorsOn(rn);
		return false;
	}
	boolean fixAssignInSetter(Field f, AssignExpr ae) {
		if!(ae.lval instanceof IFldExpr && ((IFldExpr)ae.lval).obj instanceof ThisExpr && ((IFldExpr)ae.lval).var == f)
			return true;
		Method csm = getCodeSet();
		ASTNode rewriter = csm.body;
		if (rewriter instanceof RewriteMatch)
			rewriter = rewriter.matchCase(ae);
		Hashtable<String,Object> args = new Hashtable<String,Object>();
		RewriteContext rctx = new RewriteContext(ae, args);
		ASTNode rn = (ASTNode)rewriter.doRewrite(rctx);
		rn = ae.replaceWithNode(~rn);
		Kiev.runProcessorsOn(rn);
		return false;
	}

}

@singleton
public class VNodeBE_FixResolve extends BackendProcessor {
	private VNodeBE_FixResolve() { super(KievBackend.Java15); }
	public String getDescr() { "VNode fix resolved" }

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr,"VNodeBE_FixResolve");
		try {
			doProcess(node);
		} finally { tr.leave(); }
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(NameSpace:ASTNode fu) {
		foreach (ASTNode dn; fu.members)
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
		if (s.isClazz() && s.getMeta(VNode_Base.mnNode) != null)
			return true;
		Struct iface = getIFaceOfImpl(s);
		if (iface != null && iface.getMeta(VNode_Base.mnNode) != null)
			return true;
		return false;
	}

	public void doProcess(Struct:ASTNode s) {
		if (s.isInterface() && !s.isMixin() || !isNodeImpl(s)) {
			foreach(Struct sub; s.members)
				doProcess(sub);
			return;
		}
		
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
				if (fe.obj instanceof ThisExpr && fe.var.isFinal() && fe.var.getMeta(VNode_Base.mnAtt) != null) {
					Field f = fe.var;
					fe.setAsField(true);
					Field fatt = f.ctx_tdecl.resolveField(("nodeattr$"+f.sname).intern());
					ENode p_st = new IfElseStat(0,
							new BinaryBoolExpr(0, Operator.NotEquals,
								new IFldExpr(0, new ThisExpr(), f),
								new ConstNullExpr()
							),
							new ExprStat(0,
								new CallExpr(0,
									new IFldExpr(0, new ThisExpr(), f),
									new SymbolRef<Method>("callbackAttached"),
									null,
									new ENode[] {
										new ThisExpr(),
										new SFldExpr(fe.pos, fatt)
									}
								)
							),
							null
						);
					block.stats.insert(i+1, p_st);
					Kiev.runProcessorsOn(p_st);
				}
			}
		}
	}

}
