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
abstract class VNode_Base extends TransfProcessor {
	public static final String mnNode					= "kiev\u001fvlang\u001fnode"; 
	public static final String mnAtt					= "kiev\u001fvlang\u001fatt"; 
	public static final String mnRef					= "kiev\u001fvlang\u001fref"; 
	public static final String nameINode				= "kiev\u001fvlang\u001fINode";
	public static final String nameANode				= "kiev\u001fvlang\u001fANode";
	public static final String nameNode				= "kiev\u001fvlang\u001fASTNode"; 
	public static final String nameNodeSpace			= "kiev\u001fvlang\u001fNodeSpace"; 
	public static final String nameAttrSlot			= "kiev\u001fvlang\u001fAttrSlot"; 
	public static final String nameRefAttrSlot			= "kiev\u001fvlang\u001fRefAttrSlot"; 
	public static final String nameAttAttrSlot			= "kiev\u001fvlang\u001fAttAttrSlot"; 
	public static final String nameExtRefAttrSlot		= "kiev\u001fvlang\u001fExtRefAttrSlot"; 
	public static final String nameExtAttAttrSlot		= "kiev\u001fvlang\u001fExtAttAttrSlot"; 
	public static final String nameSpaceAttrSlot		= "kiev\u001fvlang\u001fSpaceAttrSlot"; 
	public static final String nameSpaceRefAttrSlot	= "kiev\u001fvlang\u001fSpaceRefAttrSlot"; 
	public static final String nameSpaceAttAttrSlot	= "kiev\u001fvlang\u001fSpaceAttAttrSlot"; 
	public static final String nameCopyable			= "copyable";
	public static final String nameExtData				= "ext_data";
	
	static final String sigValues			= "()[Lkiev/vlang/AttrSlot;";
	static final String sigGetVal			= "(Ljava/lang/String;)Ljava/lang/Object;";
	static final String sigSetVal			= "(Ljava/lang/String;Ljava/lang/Object;)V";
	static final String sigCopy				= "()Ljava/lang/Object;";
	static final String sigCopyTo			= "(Ljava/lang/Object;)Ljava/lang/Object;";
	
	static Type tpINode;
	static Type tpANode;
	static Type tpNode;
	static Type tpNArray;
	static Type tpNodeSpace;
	static Type tpAttrSlot;
	static Type tpRefAttrSlot;
	static Type tpAttAttrSlot;
	static Type tpExtRefAttrSlot;
	static Type tpExtAttAttrSlot;
	static Type tpSpaceAttrSlot;
	static Type tpSpaceRefAttrSlot;
	static Type tpSpaceAttAttrSlot;

	VNode_Base() { super(KievExt.VNode); }

	final boolean isNodeImpl(Struct s) {
		return s.getMeta(mnNode) != null;
	}
	final boolean isNodeKind(Struct s) {
		return s.getMeta(mnNode) != null;
	}
	final boolean isNodeKind(Type t) {
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
			tpINode = Env.loadTypeDecl(nameINode, true).xtype;
			tpANode = Env.loadTypeDecl(nameANode, true).xtype;
			tpNode = Env.loadTypeDecl(nameNode, true).xtype;
			tpNArray = new ArrayType(tpANode);
			tpNodeSpace = Env.newMetaType(new Symbol<MetaTypeDecl>("NodeSpace"), Env.newPackage("kiev\u001fvlang"), false, null).xtype;
			tpAttrSlot = Env.loadTypeDecl(nameAttrSlot, true).xtype;
			tpRefAttrSlot = Env.loadTypeDecl(nameRefAttrSlot, true).xtype;
			tpAttAttrSlot = Env.loadTypeDecl(nameAttAttrSlot, true).xtype;
			tpExtRefAttrSlot = Env.loadTypeDecl(nameExtRefAttrSlot, true).xtype;
			tpExtAttAttrSlot = Env.loadTypeDecl(nameExtAttAttrSlot, true).xtype;
			tpSpaceAttrSlot = Env.loadTypeDecl(nameSpaceAttrSlot, true).xtype;
			tpSpaceRefAttrSlot = Env.loadTypeDecl(nameSpaceRefAttrSlot, true).xtype;
			tpSpaceAttAttrSlot = Env.loadTypeDecl(nameSpaceAttAttrSlot, true).xtype;
		}
		foreach (Struct n; fu.members)
			doProcess(n);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach (Struct sub; s.sub_decls)
			doProcess(sub);
		if (isNodeKind(s)) {
			s.setCompilerNode(true);

			// Check fields of the @node
			foreach (Field n; s.getAllFields())
				doProcess(n);
			return;
		}
		
		// Check fields to not have @att and @ref
		foreach (Field f; s.getAllFields()) {
			UserMeta fmatt = f.getMeta(mnAtt);
			UserMeta fmref = f.getMeta(mnRef);
			if (fmatt != null || fmref != null) {
				Kiev.reportError(f,"Field "+f+" of non-@node class "+f.ctx_tdecl+" may not be @att or @ref");
			}
		}
	}
	
	public void doProcess(Field:ASTNode f) {
		UserMeta fmatt = f.getMeta(mnAtt);
		UserMeta fmref = f.getMeta(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @att="+fmatt+" and @ref="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" marked both @att and @ref");
		}
		if (fmatt != null || fmref != null) {
			if (f.isStatic())
				Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" is static and cannot have @att or @ref");
			boolean isArr = false;
			{
				Type ft = f.type;
				if (ft.isInstanceOf(tpNArray)) {
					if !(ft.isInstanceOf(tpNodeSpace)) {
						TypeExpr te = (TypeExpr)f.vtype;
						te.op = Operator.PostTypeSpace;
						te.ident = Operator.PostTypeSpace.name;
						te.lnk = null;
						te.getType();
						//ArgType arg = tpNodeSpace.bindings().tvars[0].var;
						//Type bnd = ft.resolve(StdTypes.tpArrayArg);
						//f.vtype = new TypeRef(tpNodeSpace.applay(new TVarBld(arg, bnd)));
					}
					isArr = true;
				}
			}
			//System.out.println("process @node: field "+f+" of type "+fs+" has correct @att="+fmatt+" or @ref="+fmref);
			if (fmatt != null) {
				if (isArr && f.init != null && !f.init.isAutoGenerated()) {
					Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" may not have initializer");
				}
				if (!isArr)
					f.setVirtual(true);
			}
			else if (fmref != null) {
				if (isArr && f.init != null && !f.init.isAutoGenerated())
					Kiev.reportError(f,"Field "+f.ctx_tdecl+"."+f+" may not have initializer");
				if (!isArr)
					f.setVirtual(true);
			}
		}
		else if !(f.isStatic()) {
			if (f.type.isInstanceOf(tpNArray))
				Kiev.reportWarning(f,"Field "+f.ctx_tdecl+"."+f+" must be marked with @att or @ref");
			else if (isNodeKind(f.type))
				Kiev.reportWarning(f,"Field "+f.ctx_tdecl+"."+f+" must be marked with @att or @ref");
		}
	}
}

//////////////////////////////////////////////////////
//   Generate class members (enumerate sub-nodes)   //
//////////////////////////////////////////////////////

@singleton
public final class VNodeFE_GenMembers extends VNode_Base {
	public String getDescr() { "VNode members generation" }

	private boolean hasField(Struct s, String name) {
		s.checkResolved();
		foreach (Field f; s.getAllFields(); f.sname == name) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, String name) {
		s.checkResolved();
		foreach (Method m; s.members; m.hasName(name,true)) return true;
		return false;
	}
	
	private Type makeNodeAttrClass(Struct snode, Field f) {
		UserMeta fmatt = (UserMeta)f.getMeta(mnAtt);
		UserMeta fmref = (UserMeta)f.getMeta(mnRef);
		boolean isAtt = (fmatt != null);
		boolean isArr = f.getType().isInstanceOf(tpNArray);
		boolean isExtData = isAtt ? fmatt.getZ(nameExtData) : fmref.getZ(nameExtData);
		Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
		Struct s = Env.newStruct(("NodeAttr_"+f.sname).intern(),true,snode,ACC_FINAL|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),true,null);
		s.setTypeDeclLoaded(true);
		snode.members.add(s);
		if (isArr) {
			if (isAtt) {
				TVarBld set = new TVarBld();
				set.append(tpSpaceAttAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_types.insert(0, new TypeRef(tpSpaceAttAttrSlot.applay(set)));
			} else {
				TVarBld set = new TVarBld();
				set.append(tpSpaceRefAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_types.insert(0, new TypeRef(tpSpaceRefAttrSlot.applay(set)));
			}
		} else {
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
			Constructor sctor = (Constructor)s.super_types[0].getStruct().resolveMethod(nameInit,Type.tpVoid,Type.tpString,Type.tpTypeInfo);
			CtorCallExpr ce = new CtorCallExpr(f.pos,
					new SuperExpr(),
					new ENode[]{
						new LVarExpr(f.pos, ctor.params[0]),
						new LVarExpr(f.pos, ctor.params[1])
					}
				);
			ce = ce.open();
			ce.symbol = sctor;
			ctor.body = new Block(0);
			ctor.block.stats.add(new ExprStat(ce));
		}
		if (!isExtData) {
			if (isArr) {
				// add public N[] get(ASTNode parent)
				{
					Method getArr = new MethodImpl("get",new ArrayType(tpANode),ACC_PUBLIC | ACC_SYNTHETIC);
					getArr.params.add(new LVar(0, "parent", tpANode, Var.PARAM_NORMAL, ACC_FINAL));
					s.addMethod(getArr);
					getArr.body = new Block(0);
					ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, getArr.params[0]) ), f);
					getArr.block.stats.add(new ReturnStat(f.pos,val));
				}
				// add public void set(ASTNode parent, N[]:Object narr)
				{
					Method setArr = new MethodImpl("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
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
						ConstStringExpr msg = new ConstStringExpr((isAtt ? "@att " : "@ref ")+f.sname+" is not writeable");
						setVal.block.stats.add(
							new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
						);
					}
				}
			}
		}

		Kiev.runProcessorsOn(s);
		return s.xtype;
	}
	
	public void process(ASTNode node, Transaction tr) {
		doProcess(node);
	}
	
	public void doProcess(ASTNode:ASTNode node) {
		return;
	}
	
	public void doProcess(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	private void doProcess(Struct:ASTNode s) {
		foreach (Struct dn; s.members)
			this.doProcess(dn);
		if (!s.isClazz())
			return;
		if (!isNodeImpl(s))
			return;
		if (hasField(s, nameEnumValuesFld)) {
			// already generated
			return;
		}
		foreach (TypeRef st; s.super_types; isNodeKind(st.getStruct()))
			this.doProcess(st.getStruct());
		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		if (isNodeImpl(s)) {
			Struct ss = s;
			while (ss != null && isNodeImpl(ss)) {
				foreach (Field f; ss.getAllFields(); !f.isStatic() && (f.getMeta(mnAtt) != null || f.getMeta(mnRef) != null)) {
					aflds.append(f);
				}
				ss = ss.super_types[0].getStruct();
			}
		}
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.getMeta(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNArray);
			String fname = "nodeattr$"+f.sname;
			if (f.ctx_tdecl != s) {
				vals_init[i] = new SFldExpr(f.pos, s.resolveField(fname.intern(), true));
				continue;
			}
			Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
			Type tpa = makeNodeAttrClass(s,f);
			TypeInfoExpr clz_expr = new TypeInfoExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, tpa, new ENode[]{
					new ConstStringExpr(f.sname),
					clz_expr
				});
			Field af = s.addField(new Field(fname, e.getType(), ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
			af.init = e;
			vals_init[i] = new SFldExpr(af.pos, af);
			if (isArr && !f.isAbstract()) {
				TypeDecl N = f.getType().resolve(StdTypes.tpArrayArg).meta_type.tdecl;
				Field emptyArray = N.resolveField("emptyArray", false);
				if (emptyArray == null || emptyArray.ctx_tdecl != N)
					Kiev.reportError(f, "Cannot find 'emptyArray' field in "+N);
				else
					f.init = new ReinterpExpr(f.pos, f.getType(), new SFldExpr(f.pos, emptyArray));
				if (f.init != null)
					f.init.setAutoGenerated(true);
			}
			if (isAtt && !isArr)
				f.setVirtual(true);
			UserMeta fmeta = (UserMeta) (isAtt ? f.getMeta(mnAtt) : f.getMeta(mnRef));
			if (fmeta.getZ(nameExtData)) {
				f.setVirtual(true);
				f.setAbstract(true);
			}
		}
		Field vals = s.addField(new Field(nameEnumValuesFld, new ArrayType(tpAttrSlot), ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
		vals.init = new NewInitializedArrayExpr(0, new TypeExpr(tpAttrSlot,Operator.PostTypeArray), 1, vals_init);
		// AttrSlot[] values() { return $values; }
		if (hasMethod(s, nameEnumValues)) {
			Kiev.reportWarning(s,"Method "+s+"."+nameEnumValues+sigValues+" already exists, @node member is not generated");
		} else {
			Method elems = new MethodImpl(nameEnumValues,new ArrayType(tpAttrSlot),ACC_PUBLIC | ACC_SYNTHETIC);
			s.addMethod(elems);
			elems.body = new Block(0);
			elems.block.stats.add(
				new ReturnStat(0,
					new SFldExpr(0,vals) ) );
		}
		// copy()
		if (s.getMeta(mnNode) != null && !((UserMeta)s.getMeta(mnNode)).getZ(nameCopyable) || s.isAbstract()) {
			// node is not copyable
		}
		else if (hasMethod(s, "copy")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copy"+sigCopy+" already exists, @node member is not generated");
		}
		else {
			Method copyV = new MethodImpl("copy",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			s.addMethod(copyV);
			copyV.body = new Block(0);
			Var v = new LVar(0, "node",s.xtype,Var.VAR_LOCAL,0);
			copyV.block.stats.append(new ReturnStat(0,new CallExpr(0,new ThisExpr(),
				new SymbolRef<Method>("copyTo"), null, new ENode[]{new NewExpr(0,s.xtype,ENode.emptyArray)})));
		}
		// copyTo(Object)
		if (s.getMeta(mnNode) != null && !((UserMeta)s.getMeta(mnNode)).getZ(nameCopyable)) {
			// node is not copyable
		}
		else if (hasMethod(s, "copyTo")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			Method copyV = new MethodImpl("copyTo",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			copyV.params.append(new LVar(0,"to$node", Type.tpObject, Var.PARAM_NORMAL, 0));
			s.addMethod(copyV);
			copyV.body = new Block();
			Var v = new LVar(0,"node",s.xtype,Var.VAR_LOCAL,0);
			if (isNodeKind(s.super_types[0].getStruct())) {
				CallExpr cae = new CallExpr(0,new SuperExpr(),
					new SymbolRef<Method>("copyTo"),null,new ENode[]{new LVarExpr(0,copyV.params[0])});
				v.init = new CastExpr(0,s.xtype,cae);
				copyV.block.addSymbol(v);
			} else {
				v.init = new CastExpr(0,s.xtype,new ASTIdentifier(0,"to$node"));
				copyV.block.addSymbol(v);
			}
			foreach (Field f; s.getAllFields()) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
					continue;
				{	// check if we may not copy the field
					UserMeta fmeta = (UserMeta)f.getMeta(mnAtt);
					if (fmeta == null)
						fmeta = (UserMeta)f.getMeta(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				boolean isNode = (isNodeKind(f.getType()));
				boolean isArr = f.getType().isInstanceOf(tpNArray);
				if (f.getMeta(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						CallExpr cae = new CallExpr(0,
							new IFldExpr(0,new LVarExpr(0,v),f),
							new SymbolRef<Method>("copyFrom"),
							null,
							new ENode[]{new IFldExpr(0,new ThisExpr(),f)});
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
										new ENode[]{new IFldExpr(0,new LVarExpr(0,v),f)}
									)
								)
							);
						}
					} else {
						CallExpr cae = new CallExpr(0,
							new IFldExpr(0, new ThisExpr(),f),
							new SymbolRef<Method>("copy"),
							null,
							ENode.emptyArray);
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
		}
		// setFrom(Object), a reverted clone()
		if (hasMethod(s, "setFrom")) {
			Kiev.reportWarning(s,"Method "+s+"."+"setFrom already exists, @node member is not generated");
		} else {
			Method setF = new MethodImpl("setFrom",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
			setF.params.append(new LVar(0,"from$node", Type.tpObject, Var.PARAM_NORMAL, 0));
			s.addMethod(setF);
			setF.body = new Block();
			Var v = new LVar(0,"node",s.xtype,Var.VAR_LOCAL,0);
			v.init = new CastExpr(0,s.xtype,new ASTIdentifier(0,"from$node"));
			setF.block.addSymbol(v);
			foreach (Field f; s.getAllFields()) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic() || f.isFinal())
					continue;
				setF.block.stats.append( 
					new ExprStat(0,
						new AssignExpr(0,Operator.Assign,
							new IFldExpr(0,new ThisExpr(),f,true),
							new IFldExpr(0,new LVarExpr(0,v),f,true)
						)
					)
				);
			}
			CallExpr cae = new CallExpr(0,new SuperExpr(),
				new SymbolRef<Method>("setFrom"),null,new ENode[]{new LVarExpr(0,v)});
			setF.block.stats.append(cae);
		}
	}
}

////////////////////////////////////////////////////
//	   PASS - verification                        //
////////////////////////////////////////////////////

@singleton
public final class VNodeFE_Verify extends VNode_Base {
	public String getDescr() { "VNode verify" }

	public void verify(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof DNode)
					verifyDecl((DNode)n);
				return true;
			}
		});
		return;
	}

	void verifyDecl(DNode dn) {
		return;
	}
}

@singleton
public class VNodeME_PreGenerate extends BackendProcessor {
	public static final String nameMetaGetter	= VirtFldFE_GenMembers.nameMetaGetter; 
	public static final String nameMetaSetter	= VirtFldFE_GenMembers.nameMetaSetter; 

	Type tpINode;
	Type tpANode;
	Type tpNode;
	Type tpSpaceAttrSlot;
	
	private VNodeME_PreGenerate() { super(KievBackend.Java15); }
	public String getDescr() { "VNode pre-generation" }

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void process(ASTNode node, Transaction tr) {
		tr = Transaction.enter(tr);
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
			tpSpaceAttrSlot = VNode_Base.tpSpaceAttrSlot;
		}
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	final boolean isNodeImpl(Struct s) {
		return s.getMeta(VNode_Base.mnNode) != null;
	}

	public void doProcess(Struct:ASTNode s) {
		if (isNodeImpl(s)) {
			foreach(Field f; s.getAllFields(); !f.isStatic() && f.isVirtual()) {
				UserMeta fmatt = f.getMeta(VNode_Base.mnAtt);
				UserMeta fmref = f.getMeta(VNode_Base.mnRef);
				if (fmatt != null || fmref != null) {
					fixSetterMethod(s, f, fmatt, fmref);
					fixGetterMethod(s, f, fmatt, fmref);
				}
			}
			foreach(Constructor ctor; s.members; !ctor.isStatic())
				fixFinalFieldsInit(s, ctor);
		}
		foreach(Struct sub; s.sub_decls)
			doProcess(sub);
	}
	
	private void fixFinalFieldsInit(Struct s, Constructor ctor) {
		for (int i=0; i < ctor.block.stats.length; i++) {
			ANode stat = ctor.block.stats[i];
			if (stat instanceof ExprStat) stat = stat.expr;
			if (stat instanceof AssignExpr && stat.lval instanceof IFldExpr) {
				IFldExpr fe = ((AssignExpr)stat).lval;
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
					ctor.block.stats.insert(i+1, p_st);
					Kiev.runProcessorsOn(p_st);
				}
			}
		}
	}

	private void fixGetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);

		Method get_var = (Method)Field.GETTER_ATTR.get(f);
		if (get_var == null || get_var.isStatic())
			return;
		if (get_var.getMeta(VNode_Base.mnAtt) != null || get_var.getMeta(VNode_Base.mnRef) != null)
			return; // already generated
		if (get_var.isAbstract()) {
			if (isAtt && !fmatt.getZ(VNode_Base.nameExtData) || !isAtt && !fmref.getZ(VNode_Base.nameExtData))
				return;
			String fname = ("nodeattr$"+f.sname).intern();
			Field fatt = f.ctx_tdecl.resolveField(fname);
			get_var.setAbstract(false);
			Block body = new Block();
			body.stats += new CastExpr(f.getType(),new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("get"),null,new ENode[]{new ThisExpr()}));
			get_var.body = body;
			Kiev.runProcessorsOn(body);
			return;
		}
		ANode stat = get_var.block.stats[0];
		if !(stat instanceof ReturnStat) {
			Kiev.reportWarning(get_var,"Cannot fixup getter method with getVersion()");
			return;
		}
		ENode expr = ((ReturnStat)stat).expr;
		if !(expr instanceof IFldExpr) {
			Kiev.reportWarning(get_var,"Cannot fixup getter method with getVersion()");
			return;
		}
		IFldExpr fe = (IFldExpr)expr;
		if (fe.var != f || !(fe.obj instanceof ThisExpr)) {
			Kiev.reportWarning(get_var,"Cannot fixup getter method with getVersion()");
			return;
		}
		
		if !(fe.var.getType().isInstanceOf(tpANode) || fe.var.getType().isInstanceOf(tpINode))
			return;
		((ReturnStat)stat).expr = new CallExpr(0,new TypeRef(tpANode),new SymbolRef<Method>("getVersion"),null,new ENode[]{~fe});
		if (isAtt)
			get_var.setMeta(new UserMeta(VNode_Base.mnAtt)).resolve(null);
		else
			get_var.setMeta(new UserMeta(VNode_Base.mnRef)).resolve(null);
		Kiev.runProcessorsOn(((ReturnStat)stat).expr);
		if (get_var.isSynthetic())
			get_var.setFinal(true);
	}

	private void fixSetterMethod(Struct s, Field f, UserMeta fmatt, UserMeta fmref) {
		boolean isAtt = (fmatt != null);

		Method set_var = (Method)Field.SETTER_ATTR.get(f);
		if (set_var == null || set_var.isStatic())
			return;
		if (set_var.getMeta(VNode_Base.mnAtt) != null || set_var.getMeta(VNode_Base.mnRef) != null)
			return; // already generated

		LVar value = null;
		foreach (Var fp; set_var.params; fp.kind == Var.PARAM_NORMAL) {
			value = fp;
			break;
		}
		if (value == null) {
			Kiev.reportError(set_var,"Cannot fine a value to assign parameter");
			return;
		}

		String fname = ("nodeattr$"+f.sname).intern();
		Field fatt = f.ctx_tdecl.resolveField(fname);

		if (set_var.isAbstract()) {
			if (isAtt && !fmatt.getZ(VNode_Base.nameExtData) || !isAtt && !fmref.getZ(VNode_Base.nameExtData))
				return;
			set_var.setAbstract(false);
			Block body = new Block();
			body.stats += new IfElseStat(0,
								new BinaryBoolExpr(0,Operator.NotEquals,new LVarExpr(0,value),new ConstNullExpr()),
								new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("set"),null,new ENode[]{new ThisExpr(),new LVarExpr(0,value)}),
								new CallExpr(0,new SFldExpr(0,fatt),new SymbolRef<Method>("clear"),null,new ENode[]{new ThisExpr()})
								);
			set_var.body = body;
			Kiev.runProcessorsOn(body);
			return;
		}

		Block body = set_var.body;
		if (isAtt) {
			if (f.type.isInstanceOf(VNode_Base.tpANode)) {
				ENode p_st = new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.NotEquals,
							new IFldExpr(0,new ThisExpr(),f,true),
							new ConstNullExpr()
						),
						new Block(0,new ENode[]{
							new ExprStat(0,
								new CallExpr(0,
									new IFldExpr(0,new ThisExpr(),f,true),
									new SymbolRef<Method>("callbackDetached"),
									null,
									ENode.emptyArray
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
				Var old = new LVar(body.pos,"$old",f.type,Var.VAR_LOCAL,ACC_FINAL);
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
		// check the node is not locked
		if (s.xtype.isInstanceOf(tpNode)) {
			ENode p_st = new ExprStat(
				new CallExpr(0,new TypeRef(Type.tpDebug), new SymbolRef<Method>("assert"),null,
					new ENode[]{new BooleanNotExpr(0,new AccessExpr(0,new ThisExpr(),new SymbolRef<DNode>("locked")))})
			);
			body.stats.insert(0,p_st);
			Kiev.runProcessorsOn(p_st);
		}
		if (isAtt)
			set_var.setMeta(new UserMeta(VNode_Base.mnAtt)).resolve(null);
		else
			set_var.setMeta(new UserMeta(VNode_Base.mnRef)).resolve(null);
		if (set_var.isSynthetic())
			set_var.setFinal(true);
	}

}

