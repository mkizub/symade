package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
abstract class VNode_Base extends TransfProcessor {
	public static final String mnNode				= "kiev.vlang.node"; 
	public static final String mnNodeView			= "kiev.vlang.nodeview"; 
	public static final String mnNodeImpl			= "kiev.vlang.nodeimpl"; 
	public static final String mnNodeSet			= "kiev.vlang.nodeset"; 
	public static final String mnAtt				= "kiev.vlang.att"; 
	public static final String mnRef				= "kiev.vlang.ref"; 
	public static final String nameANode			= "kiev.vlang.ANode"; 
	public static final String nameNode			= "kiev.vlang.ASTNode"; 
	public static final String nameNodeSpace		= "kiev.vlang.NodeSpace"; 
	public static final String nameAttrSlot		= "kiev.vlang.AttrSlot"; 
	public static final String nameRefAttrSlot		= "kiev.vlang.RefAttrSlot"; 
	public static final String nameAttAttrSlot		= "kiev.vlang.AttAttrSlot"; 
	public static final String nameSpaceAttrSlot			= "kiev.vlang.SpaceAttrSlot"; 
	public static final String nameSpaceRefAttrSlot		= "kiev.vlang.SpaceRefAttrSlot"; 
	public static final String nameSpaceAttAttrSlot		= "kiev.vlang.SpaceAttAttrSlot"; 
	public static final String nameCopyable		= "copyable"; 
	
	static final String sigValues			= "()[Lkiev/vlang/AttrSlot;";
	static final String sigGetVal			= "(Ljava/lang/String;)Ljava/lang/Object;";
	static final String sigSetVal			= "(Ljava/lang/String;Ljava/lang/Object;)V";
	static final String sigCopy				= "()Ljava/lang/Object;";
	static final String sigCopyTo			= "(Ljava/lang/Object;)Ljava/lang/Object;";
	
	static Type tpANode;
	static Type tpNode;
	static Type tpNArray;
	static Type tpNodeSpace;
	static Type tpAttrSlot;
	static Type tpRefAttrSlot;
	static Type tpAttAttrSlot;
	static Type tpSpaceAttrSlot;
	static Type tpSpaceRefAttrSlot;
	static Type tpSpaceAttAttrSlot;

	VNode_Base() { super(Kiev.Ext.VNode); }

	final boolean isNodeImpl(Struct s) {
		return s.meta.getU(mnNode) != null || s.meta.getU(mnNodeImpl) != null;
	}
	final boolean isNodeKind(Struct s) {
		return s.meta.getU(mnNode) != null || s.meta.getU(mnNodeImpl) != null || s.meta.getU(mnNodeSet) != null;
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
		if (tpANode == null) {
			tpANode = Env.loadStruct(nameANode, true).xtype;
			tpNode = Env.loadStruct(nameNode, true).xtype;
			tpNArray = new ArrayType(tpANode);
			tpNodeSpace = Env.newMetaType(new Symbol("NodeSpace"), Env.newPackage("kiev.vlang"), false).xtype;
			tpAttrSlot = Env.loadStruct(nameAttrSlot, true).xtype;
			tpRefAttrSlot = Env.loadStruct(nameRefAttrSlot, true).xtype;
			tpAttAttrSlot = Env.loadStruct(nameAttAttrSlot, true).xtype;
			tpSpaceAttrSlot = Env.loadStruct(nameSpaceAttrSlot, true).xtype;
			tpSpaceRefAttrSlot = Env.loadStruct(nameSpaceRefAttrSlot, true).xtype;
			tpSpaceAttAttrSlot = Env.loadStruct(nameSpaceAttAttrSlot, true).xtype;
		}
		foreach (Struct n; fu.members)
			doProcess(n);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach (Struct sub; s.sub_decls)
			doProcess(sub);
		if (isNodeKind(s)) {
			s.setCompilerNode(true);
			// add node name to global map of compiler nodes
			if (s.meta.getU(mnNode) != null) {
				UserMeta m = s.meta.getU(mnNode);
				String name = m.getS("name");
				if (name != null && name.length() > 0)
					TypeExpr.AllNodes.put(name,s);
			}

			// Check fields of the @node
			foreach (Field n; s.members)
				doProcess(n);
			return;
		}
		
		if (s.meta.getU(mnNodeView) == null) {
			foreach (TypeRef st; s.super_types; isNodeKind(st.getType()))
				Kiev.reportError(s,"Class "+s+" must be marked with @node: it extends @node");
			return;
		}

		// Check fields to not have @att and @ref
		foreach (Field f; s.members) {
			UserMeta fmatt = f.meta.getU(mnAtt);
			UserMeta fmref = f.meta.getU(mnRef);
			if (fmatt != null || fmref != null) {
				Kiev.reportError(f,"Field "+f+" of non-@node class "+f.parent()+" may not be @att or @ref");
			}
		}
	}
	
	public void doProcess(Field:ASTNode f) {
		UserMeta fmatt = f.meta.getU(mnAtt);
		UserMeta fmref = f.meta.getU(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @att="+fmatt+" and @ref="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f,"Field "+f.parent()+"."+f+" marked both @att and @ref");
		}
		if (fmatt != null || fmref != null) {
			if (f.isStatic())
				Kiev.reportError(f,"Field "+f.parent()+"."+f+" is static and cannot have @att or @ref");
			boolean isArr = false;
			{
				Type ft = f.type;
				if (ft.isInstanceOf(tpNArray)) {
					if !(ft.isInstanceOf(tpNodeSpace)) {
						ArgType arg = tpNodeSpace.bindings().tvars[0].var;
						Type bnd = ft.resolve(StdTypes.tpArrayArg);
						f.ftype = new TypeRef(tpNodeSpace.applay(new TVarBld(arg, bnd)));
					}
					isArr = true;
				}
			}
			//System.out.println("process @node: field "+f+" of type "+fs+" has correct @att="+fmatt+" or @ref="+fmref);
			if (fmatt != null) {
				if (isArr && f.init != null) {
					Kiev.reportError(f,"Field "+f.parent()+"."+f+" may not have initializer");
				}
				if (!isArr)
					f.setVirtual(true);
			}
			else if (fmref != null) {
				if (isArr && f.init != null)
					Kiev.reportError(f,"Field "+f.parent()+"."+f+" may not have initializer");
			}
			Struct fts = f.type.getStruct();
			if (fts != null) {
				TypeDef td = new TypeAssign(
					new Symbol(f.pos,"attr$"+f.id+"$type"),
					new TypeRef(new ASTNodeType(f.type.getStruct())));
				td.setSynthetic(true);
				Struct clazz = (Struct)f.parent();
				clazz.members.append(td);
				if (clazz.ameta_type != null)
					clazz.ameta_type.version++;
			}
		}
		else if !(f.isStatic()) {
			if (f.type.isInstanceOf(tpNArray))
				Kiev.reportWarning(f,"Field "+f.parent()+"."+f+" must be marked with @att or @ref");
			else if (isNodeKind(f.type))
				Kiev.reportWarning(f,"Field "+f.parent()+"."+f+" must be marked with @att or @ref");
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
		foreach (Field f; s.members; f.id.equals(name)) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, String name) {
		s.checkResolved();
		foreach (Method m; s.members; m.hasName(name,true)) return true;
		return false;
	}
	
	private Type makeNodeAttrClass(Struct snode, Field f) {
		boolean isAtt = (f.meta.getU(mnAtt) != null);
		boolean isArr = f.getType().isInstanceOf(tpNArray);
		Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
		Struct s = Env.newStruct(("NodeAttr_"+f.id.sname).intern(),true,snode,ACC_FINAL|ACC_STATIC|ACC_SYNTHETIC,new JavaClass(),true);
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
			if (isAtt)
				s.super_types.insert(0, new TypeRef(tpAttAttrSlot));
			else
				s.super_types.insert(0, new TypeRef(tpRefAttrSlot));
		}
		// make constructor
		{
			Constructor ctor = new Constructor(0);
			ctor.params.add(new FormPar(0, "name", Type.tpString, FormPar.PARAM_NORMAL, ACC_FINAL));
			ctor.params.add(new FormPar(0, "typeinfo", Type.tpTypeInfo, FormPar.PARAM_NORMAL, ACC_FINAL));
			s.members.add(ctor);
			Constructor sctor = (Constructor)s.super_types[0].getStruct().resolveMethod(nameInit,Type.tpVoid,Type.tpString,Type.tpTypeInfo);
			CtorCallExpr ce = new CtorCallExpr(f.pos,
					new SuperExpr(),
					new ENode[]{
						new LVarExpr(f.pos, ctor.params[0]),
						new LVarExpr(f.pos, ctor.params[1])
					}
				);
			ce.ident.symbol = sctor.id;
			ctor.body = new Block(0);
			ctor.block.stats.add(new ExprStat(ce));
		}
		if (isArr) {
			// add public N[] get(ASTNode parent)
			{
				Method getArr = new Method("get",new ArrayType(tpANode),ACC_PUBLIC | ACC_SYNTHETIC);
				getArr.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(getArr);
				getArr.body = new Block(0);
				ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, getArr.params[0]) ), f);
				getArr.block.stats.add(new ReturnStat(f.pos,val));
			}
			// add public void set(ASTNode parent, N[]:Object narr)
			{
				Method setArr = new Method("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
				setArr.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				setArr.params.add(new FormPar(0, "narr", Type.tpObject /*new ArrayType(tpANode)*/, FormPar.PARAM_NORMAL, ACC_FINAL));
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
				Method getVal = new Method("get",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
				getVal.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(getVal);
				getVal.body = new Block(0);
				ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.xtype, new LVarExpr(0, getVal.params[0]) ), f);
				getVal.block.stats.add(new ReturnStat(f.pos,val));
				if!(val.getType().isReference())
					CastExpr.autoCastToReference(val);
			}
			// add public void set(ASTNode parent, Object narr)
			{
				Method setVal = new Method("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
				setVal.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				setVal.params.add(new FormPar(0, "val", Type.tpObject, FormPar.PARAM_NORMAL, ACC_FINAL));
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
					ConstStringExpr msg = new ConstStringExpr((isAtt ? "@att " : "@ref ")+f.id+" is not writeable");
					setVal.block.stats.add(
						new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
					);
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
				foreach (Field f; ss.members; !f.isStatic() && (f.meta.getU(mnAtt) != null || f.meta.getU(mnRef) != null)) {
					aflds.append(f);
				}
				ss = ss.super_types[0].getStruct();
			}
		}
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.meta.getU(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNArray);
			String fname = "nodeattr$"+f.id.sname;
			if (f.parent() != s) {
				vals_init[i] = new SFldExpr(f.pos, s.resolveField(fname.intern(), true));
				continue;
			}
			Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
			Type tpa = makeNodeAttrClass(s,f);
			TypeInfoExpr clz_expr = new TypeInfoExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, tpa, new ENode[]{
					new ConstStringExpr(f.id.sname),
					clz_expr
				});
			Field af = s.addField(new Field(fname, e.getType(), ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
			af.init = e;
			vals_init[i] = new SFldExpr(af.pos, af);
			if (isArr && !f.isAbstract()) {
				TypeDecl N = f.getType().resolve(StdTypes.tpArrayArg).meta_type.tdecl;
				Field emptyArray = N.resolveField("emptyArray", false);
				if (emptyArray == null || emptyArray.parent() != N)
					Kiev.reportError(f, "Cannot find 'emptyArray' field in "+N);
				else
					f.init = new ReinterpExpr(f.pos, f.getType(), new SFldExpr(f.pos, emptyArray));
				if (f.init != null)
					f.init.setAutoGenerated(true);
			}
			if (isAtt && !isArr)
				f.setVirtual(true);
		}
		Field vals = s.addField(new Field(nameEnumValuesFld, new ArrayType(tpAttrSlot), ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
		vals.init = new NewInitializedArrayExpr(0, new TypeRef(tpAttrSlot), 1, vals_init);
		// AttrSlot[] values() { return $values; }
		if (hasMethod(s, nameEnumValues)) {
			Kiev.reportWarning(s,"Method "+s+"."+nameEnumValues+sigValues+" already exists, @node member is not generated");
		} else {
			Method elems = new Method(nameEnumValues,new ArrayType(tpAttrSlot),ACC_PUBLIC | ACC_SYNTHETIC);
			s.addMethod(elems);
			elems.body = new Block(0);
			elems.block.stats.add(
				new ReturnStat(0,
					new SFldExpr(0,vals) ) );
			// Object getVal(String)
			Method getV = new Method("getVal",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			getV.params.add(new FormPar(0, "name", Type.tpString, FormPar.PARAM_NORMAL, 0));
			s.addMethod(getV);
			getV.body = new Block(0);
			foreach (Field f; aflds; f.parent() == s) {
				ENode ee = new IFldExpr(0,new ThisExpr(),f);
				getV.block.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.Equals,
							new LVarExpr(0, getV.params[0]),
							new ConstStringExpr(f.id.sname)
						),
						new ReturnStat(0, ee),
						null
					)
				);
				if!(ee.getType().isReference())
					CastExpr.autoCastToReference(ee);
			}
			Struct sup = s.super_types[0].getStruct();
			if (isNodeKind(sup)) {
				CallExpr ce = new CallExpr(0,
						new SuperExpr(),
						sup.resolveMethod("getVal",Type.tpObject,Type.tpString),
						null,
						new ENode[]{new LVarExpr(0, getV.params[0])}
						);
				ce.setSuperExpr(true);
				getV.block.stats.add(new ReturnStat(0,ce));
			} else {
				StringConcatExpr msg = new StringConcatExpr();
				msg.appendArg(new ConstStringExpr("No @att value \""));
				msg.appendArg(new LVarExpr(0, getV.params[0]));
				msg.appendArg(new ConstStringExpr("\" in "+s.id));
				getV.block.stats.add(
					new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
				);
			}
		}
		// copy()
		if (s.meta.getU(mnNode) != null && !s.meta.getU(mnNode).getZ(nameCopyable) || s.isAbstract()) {
			// node is not copyable
		}
		else if (hasMethod(s, "copy")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copy"+sigCopy+" already exists, @node member is not generated");
		}
		else {
			Method copyV = new Method("copy",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			s.addMethod(copyV);
			copyV.body = new Block(0);
			Var v = new Var(0, "node",s.xtype,0);
			copyV.block.stats.append(new ReturnStat(0,new CallExpr(0,new ThisExpr(),
				new SymbolRef<Method>("copyTo"), null, new ENode[]{new NewExpr(0,s.xtype,ENode.emptyArray)})));
		}
		// copyTo(Object)
		if (hasMethod(s, "copyTo")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			Method copyV = new Method("copyTo",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			copyV.params.append(new FormPar(0,"to$node", Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(copyV);
			copyV.body = new Block();
			Var v = new Var(0,"node",s.xtype,0);
			if (isNodeKind(s.super_types[0].getStruct())) {
				CallExpr cae = new CallExpr(0,new SuperExpr(),
					new SymbolRef<Method>("copyTo"),null,new ENode[]{new LVarExpr(0,copyV.params[0])});
				v.init = new CastExpr(0,s.xtype,cae);
				copyV.block.addSymbol(v);
			} else {
				v.init = new CastExpr(0,s.xtype,new ASTIdentifier(0,"to$node"));
				copyV.block.addSymbol(v);
			}
			foreach (Field f; s.members) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
					continue;
				{	// check if we may not copy the field
					UserMeta fmeta = f.meta.getU(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.getU(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				boolean isNode = (isNodeKind(f.getType()));
				boolean isArr = f.getType().isInstanceOf(tpNArray);
				if (f.meta.getU(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						CallExpr cae = new CallExpr(0,
							new IFldExpr(0,new LVarExpr(0,v),f),
							new SymbolRef<Method>("copyFrom"),
							null,
							new ENode[]{new IFldExpr(0,new ThisExpr(),f)});
						copyV.block.stats.append(new ExprStat(0,cae));
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
			copyV.block.stats.append(new ReturnStat(0,new LVarExpr(0,v)));
		}
		// setFrom(Object), a reverted clone()
		if (hasMethod(s, "setFrom")) {
			Kiev.reportWarning(s,"Method "+s+"."+"setFrom already exists, @node member is not generated");
		} else {
			Method setF = new Method("setFrom",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
			setF.params.append(new FormPar(0,"from$node", Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(setF);
			setF.body = new Block();
			Var v = new Var(0,"node",s.xtype,0);
			v.init = new CastExpr(0,s.xtype,new ASTIdentifier(0,"from$node"));
			setF.block.addSymbol(v);
			foreach (Field f; s.members) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
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
		// setVal(String, Object)
		if (hasMethod(s, "setVal")) {
			Kiev.reportWarning(s,"Method "+s+"."+"setVal"+sigSetVal+" already exists, @node member is not generated");
		} else {
			Method setV = new Method("setVal",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
			setV.params.append(new FormPar(0, "name", Type.tpString, FormPar.PARAM_NORMAL, 0));
			setV.params.append(new FormPar(0, "val", Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(setV);
			setV.body = new Block(0);
			// check the node is not locked
			if (s.xtype.isInstanceOf(tpNode)) {
				setV.block.stats.add(new ExprStat(
					new CallExpr(0,new TypeRef(Type.tpDebug), new SymbolRef<Method>("assert"),null,
						new ENode[]{new BooleanNotExpr(0,new AccessExpr(0,new ThisExpr(),new SymbolRef<DNode>("locked")))})
				));
			}
			foreach (Field f; aflds; f.parent() == s) {
				boolean isArr = f.getType().isInstanceOf(tpNArray);
				if (isArr || f.isFinal() || !MetaAccess.writeable(f))
					continue;
				{	// check if we may not copy the field
					UserMeta fmeta = f.meta.getU(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.getU(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				Type atp = f.getType();
				ENode ee;
				if (atp.isReference())
					ee = new CastExpr(0,atp,new LVarExpr(0, setV.params[1]));
				else
					ee = new CastExpr(0,((CoreType)atp).getRefTypeForPrimitive(),new LVarExpr(0, setV.params[1]));
				setV.block.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, Operator.Equals,
							new LVarExpr(0, setV.params[0]),
							new ConstStringExpr(f.id.sname)
							),
						new Block(0, new ENode[]{
							new ExprStat(0,
								new AssignExpr(0,Operator.Assign,
									new IFldExpr(0,new ThisExpr(),f),
									ee
								)
							),
							new ReturnStat(0,null)
						}),
						null
					)
				);
				if!(f.getType().isReference())
					CastExpr.autoCastToPrimitive(ee);
			}
			Struct sup = s.super_types[0].getStruct();
			if (isNodeKind(sup)) {
				CallExpr ce = new CallExpr(0,
					new SuperExpr(),
					sup.resolveMethod("setVal",Type.tpVoid,Type.tpString,Type.tpObject),
					null,
					new ENode[]{new LVarExpr(0, setV.params[0]),new LVarExpr(0, setV.params[1])}
					);
				ce.setSuperExpr(true);
				setV.block.stats.add(ce);
			} else {
				StringConcatExpr msg = new StringConcatExpr();
				msg.appendArg(new ConstStringExpr("No @att value \""));
				msg.appendArg(new LVarExpr(0, setV.params[0]));
				msg.appendArg(new ConstStringExpr("\" in "+s.id));
				setV.block.stats.add(
					new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
				);
			}
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
	public static final String nameMetaGetter = VirtFldFE_GenMembers.nameMetaGetter; 
	public static final String nameMetaSetter = VirtFldFE_GenMembers.nameMetaSetter; 

	Type tpANode;
	Type tpNode;
	Type tpSpaceAttrSlot;
	
	private VNodeME_PreGenerate() { super(Kiev.Backend.Java15); }
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
		if (tpANode == null) {
			tpANode = VNode_Base.tpANode;
			tpNode = VNode_Base.tpNode;
			tpSpaceAttrSlot = VNode_Base.tpSpaceAttrSlot;
		}
		foreach (Struct dn; fu.members)
			this.doProcess(dn);
	}
	
	public void doProcess(Struct:ASTNode s) {
		foreach(Field f; s.members; !f.isStatic() && f.isVirtual() && f.meta.getU(VNode_Base.mnAtt) != null)
			fixSetterMethod(s, f);
		foreach(Struct sub; s.sub_decls)
			doProcess(sub);
	}
	
	private void fixSetterMethod(Struct s, Field f) {
		assert(f.meta.getU(VNode_Base.mnAtt) != null);

		Method set_var = (Method)Field.SETTER_ATTR.get(f);
		if (set_var == null || set_var.isAbstract() || set_var.isStatic())
			return;
		if (set_var.meta.getU(VNode_Base.mnAtt) != null)
			return; // already generated

		FormPar value = null;
		foreach (FormPar fp; set_var.params; fp.kind == FormPar.PARAM_NORMAL) {
			value = fp;
			break;
		}
		if (value == null) {
			Kiev.reportError(set_var,"Cannot fine a value to assign parameter");
			return;
		}

		Block body = set_var.body;
		String fname = ("nodeattr$"+f.id.sname).intern();
		Field fatt = f.ctx_tdecl.resolveField(fname);
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
			Var old = new Var(body.pos,"$old",f.type,ACC_FINAL);
			old.init = new IFldExpr(0,new ThisExpr(),f,true);
			body.stats.insert(0,old);
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
		// check the node is not locked
		if (s.xtype.isInstanceOf(tpNode)) {
			ENode p_st = new ExprStat(
				new CallExpr(0,new TypeRef(Type.tpDebug), new SymbolRef<Method>("assert"),null,
					new ENode[]{new BooleanNotExpr(0,new AccessExpr(0,new ThisExpr(),new SymbolRef<DNode>("locked")))})
			);
			body.stats.insert(0,p_st);
			Kiev.runProcessorsOn(p_st);
		}
		set_var.meta.setU(new UserMeta(VNode_Base.mnAtt)).resolve(null);
	}

}

