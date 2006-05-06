package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public final class ProcessVNode extends TransfProcessor implements Constants {

	public static final String mnNode				= "kiev.vlang.node"; 
	public static final String mnNodeView			= "kiev.vlang.nodeview"; 
	public static final String mnNodeImpl			= "kiev.vlang.nodeimpl"; 
	public static final String mnNodeSet			= "kiev.vlang.nodeset"; 
	public static final String mnAtt				= "kiev.vlang.att"; 
	public static final String mnRef				= "kiev.vlang.ref"; 
	public static final String nameANode			= "kiev.vlang.ANode"; 
	public static final String nameNode			= "kiev.vlang.ASTNode"; 
	public static final String nameNArr			= "kiev.vlang.NArr"; 
	public static final String nameAttrSlot		= "kiev.vlang.AttrSlot"; 
	public static final String nameRefAttrSlot		= "kiev.vlang.RefAttrSlot"; 
	public static final String nameAttAttrSlot		= "kiev.vlang.AttAttrSlot"; 
	public static final String nameSpaceAttrSlot			= "kiev.vlang.SpaceAttrSlot"; 
	public static final String nameSpaceRefAttrSlot		= "kiev.vlang.SpaceRefAttrSlot"; 
	public static final String nameSpaceAttAttrSlot		= "kiev.vlang.SpaceAttAttrSlot"; 
	private static final String nameCopyable		= "copyable"; 
	
	private static final String sigValues			= "()[Lkiev/vlang/AttrSlot;";
	private static final String sigGetVal			= "(Ljava/lang/String;)Ljava/lang/Object;";
	private static final String sigSetVal			= "(Ljava/lang/String;Ljava/lang/Object;)V";
	private static final String sigCopy			= "()Ljava/lang/Object;";
	private static final String sigCopyTo			= "(Ljava/lang/Object;)Ljava/lang/Object;";
	

	static Type tpANode;
	static Type tpNode;
	static Type tpNArr;
	static Type tpAttrSlot;
	static Type tpRefAttrSlot;
	static Type tpAttAttrSlot;
	static Type tpSpaceAttrSlot;
	static Type tpSpaceRefAttrSlot;
	static Type tpSpaceAttAttrSlot;

	private ProcessVNode() {
		super(Kiev.Ext.VNode);
	}
	
	private boolean isNodeImpl(Struct s) {
		return s.meta.get(mnNode) != null || s.meta.get(mnNodeImpl) != null;
	}
	private boolean isNodeKind(Struct s) {
		return s.meta.get(mnNode) != null || s.meta.get(mnNodeImpl) != null || s.meta.get(mnNodeSet) != null;
	}
	private boolean isNodeKind(Type t) {
		if (t.getStruct() != null)
			return isNodeKind(t.getStruct());
		return false;
	}

	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

	public void pass3(ASTNode:ASTNode node) {
	}
	
	public void pass3(FileUnit:ASTNode fu) {
		if (tpNArr == null)
			tpNArr = Env.loadStruct(nameNArr).ctype;
		foreach (Struct n; fu.members)
			pass3(n);
	}
	
	public void pass3(Struct:ASTNode s) {
		foreach (Struct sub; s.sub_clazz)
			pass3(sub);
		if (isNodeKind(s)) {
			// Check fields of the @node
			foreach (Field n; s.members)
				pass3(n);
			return;
		}
		
		if (s.super_bound.getType() != null && isNodeKind(s.super_type)) {
			if (s.meta.get(mnNodeView) == null)
				Kiev.reportError(s,"Class "+s+" must be marked with @node: it extends @node "+s.super_type);
			return;
		}
		// Check fields to not have @att and @ref
		foreach (Field f; s.members) {
			Meta fmatt = f.meta.get(mnAtt);
			Meta fmref = f.meta.get(mnRef);
			if (fmatt != null || fmref != null) {
				Kiev.reportError(f,"Field "+f+" of non-@node class "+f.parent()+" may not be @att or @ref");
			}
		}
	}
	
	public void pass3(Field:ASTNode f) {
		Meta fmatt = f.meta.get(mnAtt);
		Meta fmref = f.meta.get(mnRef);
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
				if (ft.isInstanceOf(tpNArr)) {
					f.setFinal(true);
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
		}
		else if !(f.isStatic()) {
			if (f.type.isInstanceOf(tpNArr))
				Kiev.reportWarning(f,"Field "+f.parent()+"."+f+" must be marked with @att or @ref");
			else if (isNodeKind(f.type))
				Kiev.reportWarning(f,"Field "+f.parent()+"."+f+" must be marked with @att or @ref");
		}
	}
	
	//////////////////////////////////////////////////////
	//   Generate class members (enumerate sub-nodes)   //
    //////////////////////////////////////////////////////

	private boolean hasField(Struct s, String name) {
		s.checkResolved();
		foreach (Field f; s.members; f.id.equals(name)) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, String name) {
		s.checkResolved();
		foreach (Method m; s.members; m.id.equals(name)) return true;
		return false;
	}
	
	private Type makeNodeAttrClass(Struct snode, Field f) {
		boolean isAtt = (f.meta.get(mnAtt) != null);
		boolean isArr = f.getType().isInstanceOf(tpNArr);
		Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
		Struct s = Env.newStruct(("NodeAttr_"+f.id.sname).intern(),true,snode,ACC_FINAL|ACC_STATIC|ACC_SYNTHETIC,true);
		s.setResolved(true);
		snode.members.add(s);
		if (isArr) {
			if (isAtt) {
				TVarBld set = new TVarBld();
				set.append(tpSpaceAttAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_type = tpSpaceAttAttrSlot.applay(set);
			} else {
				TVarBld set = new TVarBld();
				set.append(tpSpaceRefAttrSlot.getStruct().args[0].getAType(), clz_tp);
				s.super_type = tpSpaceRefAttrSlot.applay(set);
			}
		} else {
			if (isAtt)
				s.super_type = tpAttAttrSlot;
			else
				s.super_type = tpRefAttrSlot;
		}
		// make constructor
		{
			Constructor ctor = new Constructor(0);
			ctor.params.add(new FormPar(0, "name", Type.tpString, FormPar.PARAM_NORMAL, ACC_FINAL));
			ctor.params.add(new FormPar(0, "clazz", Type.tpClass, FormPar.PARAM_NORMAL, ACC_FINAL));
			s.members.add(ctor);
			ctor.body = new Block(0);
			ctor.body.stats.add(new ExprStat(
				new CallExpr(f.pos,
					null,
					s.super_type.getStruct().resolveMethod(nameInit,Type.tpVoid,Type.tpString,Type.tpClass),
					null,
					new ENode[]{
						new LVarExpr(f.pos, ctor.params[0]),
						new LVarExpr(f.pos, ctor.params[1])
					},
					true
				)
			));
		}
		if (isArr) {
			// add public N[] get(ASTNode parent)
			{
				Method getArr = new Method("get",new ArrayType(tpNode),ACC_PUBLIC | ACC_SYNTHETIC);
				getArr.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(getArr);
				getArr.body = new Block(0);
				ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.ctype, new LVarExpr(0, getArr.params[0]) ), f);
				val = new IFldExpr(f.pos,val,tpNArr.getStruct().resolveField("$nodes"));
				getArr.body.stats.add(new ReturnStat(f.pos,val));
			}
			// add public void set(ASTNode parent, N[]:Object narr)
			{
				Method setArr = new Method("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
				setArr.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				setArr.params.add(new FormPar(0, "narr", Type.tpObject /*new ArrayType(tpNode)*/, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(setArr);
				setArr.body = new Block(0);
				ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.ctype, new LVarExpr(0, setArr.params[0]) ), f);
				lval = new IFldExpr(f.pos,lval,tpNArr.getStruct().resolveField("$nodes"));
				setArr.body.stats.add(new ExprStat(
					new AssignExpr(f.pos,AssignOperator.Assign2,lval,new LVarExpr(0, setArr.params[1]))
				));
			}
		} else {
			// add public N[] get(ASTNode parent)
			{
				Method getVal = new Method("get",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
				getVal.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(getVal);
				getVal.body = new Block(0);
				ENode val = new IFldExpr(f.pos, new CastExpr(f.pos, snode.ctype, new LVarExpr(0, getVal.params[0]) ), f);
				getVal.body.stats.add(new ReturnStat(f.pos,val));
				if!(val.getType().isReference())
					CastExpr.autoCastToReference(val);
			}
			// add public void set(ASTNode parent, N[]:Object narr)
			{
				Method setVal = new Method("set",Type.tpVoid,ACC_PUBLIC | ACC_SYNTHETIC);
				setVal.params.add(new FormPar(0, "parent", tpANode, FormPar.PARAM_NORMAL, ACC_FINAL));
				setVal.params.add(new FormPar(0, "val", Type.tpObject, FormPar.PARAM_NORMAL, ACC_FINAL));
				s.addMethod(setVal);
				setVal.body = new Block(0);
				ENode lval = new IFldExpr(f.pos, new CastExpr(f.pos, snode.ctype, new LVarExpr(0, setVal.params[0]) ), f);
				ENode val = new LVarExpr(0, setVal.params[1]);
				Type ftp = f.getType();
				if (ftp.isReference())
					val = new CastExpr(0,ftp,val);
				else
					val = new CastExpr(0,((CoreType)ftp).getRefTypeForPrimitive(),val);
				setVal.body.stats.add(new ExprStat(
					new AssignExpr(f.pos,AssignOperator.Assign2,lval,val)
				));
				if!(ftp.isReference())
					CastExpr.autoCastToPrimitive(val);
			}
		}

		Kiev.runProcessorsOn(s);
		return s.ctype;
	}
	
	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.autoGenerateMembers(dn);
	}
	
	private void autoGenerateMembers(Struct:ASTNode s) {
		if (tpANode == null) {
			tpANode = Env.loadStruct(nameANode, true).ctype;
			tpNode = Env.loadStruct(nameNode, true).ctype;
			tpNArr = Env.loadStruct(nameNArr, true).ctype;
			tpAttrSlot = Env.loadStruct(nameAttrSlot, true).ctype;
			tpRefAttrSlot = Env.loadStruct(nameRefAttrSlot, true).ctype;
			tpAttAttrSlot = Env.loadStruct(nameAttAttrSlot, true).ctype;
			tpSpaceAttrSlot = Env.loadStruct(nameSpaceAttrSlot, true).ctype;
			tpSpaceRefAttrSlot = Env.loadStruct(nameSpaceRefAttrSlot, true).ctype;
			tpSpaceAttAttrSlot = Env.loadStruct(nameSpaceAttAttrSlot, true).ctype;
		}
		foreach (Struct dn; s.members)
			this.autoGenerateMembers(dn);
		if (!s.isClazz())
			return;
		if (!isNodeImpl(s))
			return;
		if (hasField(s, nameEnumValuesFld)) {
			// already generated
			return;
		}
		if (s.super_type != null && isNodeKind(s.super_type)) {
			this.autoGenerateMembers(s.super_type.getStruct());
		}
		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		if (isNodeImpl(s)) {
			Struct ss = s;
			while (ss != null && isNodeImpl(ss)) {
				foreach (Field f; ss.members; !f.isStatic() && (f.meta.get(mnAtt) != null || f.meta.get(mnRef) != null)) {
					aflds.append(f);
				}
				ss = ss.super_bound.getStruct();
			}
		}
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.meta.get(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNArr);
			String fname = "nodeattr$"+f.id.sname;
			if (f.parent() != s) {
				vals_init[i] = new SFldExpr(f.pos, s.resolveField(fname.intern(), true));
				continue;
			}
			Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
			Type tpa = makeNodeAttrClass(s,f);
			TypeClassExpr clz_expr = new TypeClassExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, tpa, new ENode[]{
					new ConstStringExpr(f.id.sname),
					clz_expr
				});
			Field af = s.addField(new Field(fname, e.getType(), ACC_PUBLIC|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
			af.init = e;
			vals_init[i] = new SFldExpr(af.pos, af);
			if (isArr && !f.isAbstract()) {
				f.init = new NewExpr(f.pos, f.getType(), new ENode[]{
					new ThisExpr(),
					new SFldExpr(f.pos, af)
				});
				f.init.setHidden(true);
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
			elems.body.stats.add(
				new ReturnStat(0,
					new SFldExpr(0,vals) ) );
			// Object getVal(String)
			Method getV = new Method("getVal",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			getV.params.add(new FormPar(0, "name", Type.tpString, FormPar.PARAM_NORMAL, 0));
			s.addMethod(getV);
			getV.body = new Block(0);
			foreach (Field f; aflds; f.parent() == s) {
				ENode ee = new IFldExpr(0,new ThisExpr(),f);
				getV.body.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
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
			Type sup = s.super_type;
			if (sup != null && isNodeKind(sup)) {
				getV.body.stats.add(
					new ReturnStat(0,
						new CallExpr(0,
							new ThisExpr(true),
							sup.getStruct().resolveMethod("getVal",Type.tpObject,Type.tpString),
							null,
							new ENode[]{new LVarExpr(0, getV.params[0])},
							true)
					)
				);
			} else {
				StringConcatExpr msg = new StringConcatExpr();
				msg.appendArg(new ConstStringExpr("No @att value \""));
				msg.appendArg(new LVarExpr(0, getV.params[0]));
				msg.appendArg(new ConstStringExpr("\" in "+s.id));
				getV.body.stats.add(
					new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
				);
			}
		}
		// copy()
		if (s.meta.get(mnNode) != null && !s.meta.get(mnNode).getZ(nameCopyable) || s.isAbstract()) {
			// node is not copyable
		}
		else if (hasMethod(s, "copy")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copy"+sigCopy+" already exists, @node member is not generated");
		}
		else {
			Method copyV = new Method("copy",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			s.addMethod(copyV);
			copyV.body = new Block(0);
			Var v = new Var(0, "node",s.ctype,0);
			copyV.body.stats.append(new ReturnStat(0,new ASTCallExpression(0,
				"copyTo",	new ENode[]{new NewExpr(0,s.ctype,ENode.emptyArray)})));
		}
		// copyTo(Object)
		if (hasMethod(s, "copyTo")) {
			Kiev.reportWarning(s,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			Method copyV = new Method("copyTo",Type.tpObject,ACC_PUBLIC | ACC_SYNTHETIC);
			copyV.params.append(new FormPar(0,"to$node", Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(copyV);
			copyV.body = new Block();
			Var v = new Var(0,"node",s.ctype,0);
			if (s.super_bound.getType() != null && isNodeKind(s.super_type)) {
				ASTCallAccessExpression cae = new ASTCallAccessExpression();
				cae.obj = new ASTIdentifier(0,"super");
				cae.ident = new SymbolRef(0,"copyTo");
				cae.args.append(new ASTIdentifier(0,"to$node"));
				v.init = new CastExpr(0,s.ctype,cae);
				copyV.body.addSymbol(v);
			} else {
				v.init = new CastExpr(0,s.ctype,new ASTIdentifier(0,"to$node"));
				copyV.body.addSymbol(v);
			}
			foreach (Field f; s.members) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
					continue;
				{	// check if we may not copy the field
					Meta fmeta = f.meta.get(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				boolean isNode = (isNodeKind(f.getType()));
				boolean isArr = f.getType().isInstanceOf(tpNArr);
				if (f.meta.get(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new IFldExpr(0,new LVarExpr(0,v),f);
						cae.ident = new SymbolRef(0, "copyFrom");
						cae.args.append(new IFldExpr(0,new IFldExpr(0,new ThisExpr(),f),tpNArr.getStruct().resolveField("$nodes")));
						copyV.body.stats.append(new ExprStat(0,cae));
					} else {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new IFldExpr(0, new ThisExpr(),f);
						cae.ident = new SymbolRef(0, "copy");
						copyV.body.stats.append( 
							new IfElseStat(0,
								new BinaryBoolExpr(0, BinaryOperator.NotEquals,
									new IFldExpr(0,new ThisExpr(),f),
									new ConstNullExpr()
									),
								new ExprStat(0,
									new AssignExpr(0,AssignOperator.Assign,
										new IFldExpr(0,new LVarExpr(0,v),f),
										new CastExpr(0,f.getType(),cae)
									)
								),
								null
							)
						);
					}
				} else {
					copyV.body.stats.append( 
						new ExprStat(0,
							new AssignExpr(0,AssignOperator.Assign,
								new IFldExpr(0,new LVarExpr(0,v),f),
								new IFldExpr(0,new ThisExpr(),f)
							)
						)
					);
				}
			}
			copyV.body.stats.append(new ReturnStat(0,new LVarExpr(0,v)));
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
			foreach (Field f; aflds; f.parent() == s) {
				boolean isArr = f.getType().isInstanceOf(tpNArr);
				if (isArr || f.isFinal() || !Access.writeable(f))
					continue;
				{	// check if we may not copy the field
					Meta fmeta = f.meta.get(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				Type atp = f.getType();
				ENode ee;
				if (atp.isReference())
					ee = new CastExpr(0,atp,new LVarExpr(0, setV.params[1]));
				else
					ee = new CastExpr(0,((CoreType)atp).getRefTypeForPrimitive(),new LVarExpr(0, setV.params[1]));
				setV.body.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, setV.params[0]),
							new ConstStringExpr(f.id.sname)
							),
						new Block(0, new ENode[]{
							new ExprStat(0,
								new AssignExpr(0,AssignOperator.Assign,
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
			Type sup = s.super_type;
			if (sup != null && isNodeKind(sup)) {
				setV.body.stats.add(
					new CallExpr(0,
						new ThisExpr(true),
						sup.getStruct().resolveMethod("setVal",Type.tpVoid,Type.tpString,Type.tpObject),
						null,
						new ENode[]{new LVarExpr(0, setV.params[0]),new LVarExpr(0, setV.params[1])},
						true)
				);
			} else {
				StringConcatExpr msg = new StringConcatExpr();
				msg.appendArg(new ConstStringExpr("No @att value \""));
				msg.appendArg(new LVarExpr(0, setV.params[0]));
				msg.appendArg(new ConstStringExpr("\" in "+s.id));
				setV.body.stats.add(
					new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
				);
			}
		}
	}
	
	////////////////////////////////////////////////////
	//	   PASS - verification                        //
	////////////////////////////////////////////////////

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
		if !(dn.getType().isInstanceOf(tpNArr))
			return;
		if (dn instanceof Var)
			Kiev.reportWarning(dn,"Var cannot be NArr");
		return;
	}

	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return JavaVNodeBackend;
		return null;
	}
	
}

@singleton
class JavaVNodeBackend extends BackendProcessor implements Constants {
	public static final String nameMetaGetter = ProcessVirtFld.nameMetaGetter; 
	public static final String nameMetaSetter = ProcessVirtFld.nameMetaSetter; 

	Type tpANode = ProcessVNode.tpANode;
	Type tpNode = ProcessVNode.tpNode;
	Type tpNArr = ProcessVNode.tpNArr;
	Type tpSpaceAttrSlot = ProcessVNode.tpSpaceAttrSlot;
	
	Method treeDelToArray;
	Method attrDelToArray;
	Method treeDelAll;
	Method attrDelAll;
	Method treeAddAll;
	Method attrAddAll;
	Method treeCopyFrom;
	Method attrCopyFrom;
	Method treeIndexOf;
	Method attrIndexOf;
	Method treeSet;
	Method attrSet;
	Method treeAdd;
	Method attrAdd;
	Method treeDel;
	Method attrDel;
	Method treeDetach;
	Method attrDetach;
	Method treeInsert;
	Method attrInsert;

	private JavaVNodeBackend() {
		super(Kiev.Backend.Java15);
	}
	

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode s) {
		foreach(Field f; s.members; !f.isStatic() && f.isVirtual() && f.meta.get(ProcessVNode.mnAtt) != null)
			fixSetterMethod(s, f);
		foreach(Struct sub; s.sub_clazz)
			preGenerate(sub);
	}
	
	private static void fixSetterMethod(Struct s, Field f) {
		assert(f.meta.get(ProcessVNode.mnAtt) != null);

		MetaVirtual mv = f.getMetaVirtual();
		if (mv == null)
			return;

		Method set_var = mv.set;
		if (set_var == null || set_var.isAbstract() || set_var.isStatic())
			return;
		if (set_var.meta.get(ProcessVNode.mnAtt) != null)
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
		Field fatt = f.ctx_clazz.resolveField(fname);
		if (f.type.isInstanceOf(ProcessVNode.tpANode)) {
			ENode p_st = new IfElseStat(0,
					new BinaryBoolExpr(0, BinaryOperator.NotEquals,
						new IFldExpr(0,new ThisExpr(),f,true),
						new ConstNullExpr()
					),
					new Block(0,new ENode[]{
						new ExprStat(0,
							new ASTCallAccessExpression(0,
								new IFldExpr(0,new ThisExpr(),f,true),
								"callbackDetached",
								ENode.emptyArray
							)
						)
					}),
					null
				);
			body.stats.insert(0,p_st);
			ENode p_st = new IfElseStat(0,
					new BinaryBoolExpr(0, BinaryOperator.NotEquals,
						new LVarExpr(0, value),
						new ConstNullExpr()
					),
					new ExprStat(0,
						new ASTCallAccessExpression(0,
							new LVarExpr(0, value),
							"callbackAttached",
							new ENode[] {
								new ThisExpr(),
								new SFldExpr(f.pos, fatt)
							}
						)
					),
					null
				);
			body.stats.append(p_st);
		} else {
			Var old = new Var(body.pos,"$old",f.type,ACC_FINAL);
			old.init = new IFldExpr(0,new ThisExpr(),f,true);
			body.stats.insert(0,old);
			ENode p_st = new IfElseStat(0,
					new BinaryBoolExpr(0, BinaryOperator.NotEquals,
						new LVarExpr(0, value),
						new LVarExpr(0, old)
					),
					new ExprStat(0,
						new ASTCallAccessExpression(0,
							new ThisExpr(),
							"callbackChildChanged",
							new ENode[] {
								new SFldExpr(f.pos, fatt)
							}
						)
					),
					null
				);
			body.stats.append(p_st);
		}
		set_var.meta.set(new Meta(ProcessVNode.mnAtt)).resolve();
	}

	////////////////////////////////////////////////////
	//	   PASS - resolve (actually rewrite)          //
	////////////////////////////////////////////////////

	public void resolve(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) return rewrite((ASTNode)n); return false; }
		});
	}
	
	boolean rewrite(ASTNode:ASTNode o) {
		return true;
	}

	boolean rewrite(CallExpr:ASTNode ce) {
		if (treeDelToArray == null) {
			treeDelToArray = tpNArr.getStruct().resolveMethod("delToArray",new ArrayType(tpNode));
			attrDelToArray = tpSpaceAttrSlot.getStruct().resolveMethod("delToArray",new ArrayType(tpNode),tpANode);
			treeDelAll = tpNArr.getStruct().resolveMethod("delAll",Type.tpVoid);
			attrDelAll = tpSpaceAttrSlot.getStruct().resolveMethod("delAll",Type.tpVoid,tpANode);
			treeAddAll = tpNArr.getStruct().resolveMethod("addAll",Type.tpVoid,new ArrayType(tpNode));
			attrAddAll = tpSpaceAttrSlot.getStruct().resolveMethod("addAll",Type.tpVoid,tpANode,new ArrayType(tpNode));
			treeCopyFrom = tpNArr.getStruct().resolveMethod("copyFrom",Type.tpVoid,new ArrayType(tpNode));
			attrCopyFrom = tpSpaceAttrSlot.getStruct().resolveMethod("copyFrom",Type.tpVoid,tpANode,new ArrayType(tpNode));
			treeIndexOf = tpNArr.getStruct().resolveMethod("indexOf",Type.tpVoid,tpNode);
			attrIndexOf = tpSpaceAttrSlot.getStruct().resolveMethod("indexOf",Type.tpVoid,tpANode,tpNode);
			treeSet = tpNArr.getStruct().resolveMethod("set",tpNode,Type.tpInt,tpNode);
			attrSet = tpSpaceAttrSlot.getStruct().resolveMethod("set",tpNode,tpANode,Type.tpInt,tpNode);
			treeAdd = tpNArr.getStruct().resolveMethod("add",tpNode,tpNode);
			attrAdd = tpSpaceAttrSlot.getStruct().resolveMethod("add",tpANode,tpNode,tpNode);
			treeDel = tpNArr.getStruct().resolveMethod("del",Type.tpVoid,Type.tpInt);
			attrDel = tpSpaceAttrSlot.getStruct().resolveMethod("del",Type.tpVoid,tpANode,Type.tpInt);
			treeDetach = tpNArr.getStruct().resolveMethod("detach",Type.tpVoid,tpNode);
			attrDetach = tpSpaceAttrSlot.getStruct().resolveMethod("detach",Type.tpVoid,tpANode,tpNode);
			treeInsert = tpNArr.getStruct().resolveMethod("insert",Type.tpVoid,Type.tpInt,tpNode);
			attrInsert = tpSpaceAttrSlot.getStruct().resolveMethod("insert",Type.tpVoid,tpANode,Type.tpInt,tpNode);
		}

		Method m_attr;
		
		if      (ce.func == treeDelToArray)	m_attr = attrDelToArray;
		else if (ce.func == treeDelAll)			m_attr = attrDelAll;
		else if (ce.func == treeAddAll)			m_attr = attrAddAll;
		else if (ce.func == treeCopyFrom)		m_attr = attrCopyFrom;
		else if (ce.func == treeIndexOf)		m_attr = attrIndexOf;
		else if (ce.func == treeSet)			m_attr = attrSet;
		else if (ce.func == treeAdd)			m_attr = attrAdd;
		else if (ce.func == treeDel)			m_attr = attrDel;
		else if (ce.func == treeDetach)			m_attr = attrDetach;
		else if (ce.func == treeInsert)			m_attr = attrInsert;
		else									return true;

		if !(ce.obj instanceof IFldExpr) {
			Kiev.reportWarning(ce,"Cannot rewrite");
			return true;
		}
		IFldExpr fa = ce.obj;
		Field f = fa.var;
		Struct st = (Struct)f.parent();
		if (st.package_clazz.isStructView())
			st = st.package_clazz.view_of.getStruct();
		Field fattr = st.resolveField(("nodeattr$"+f.id.sname).intern());
		SFldExpr sfe = new SFldExpr(ce.pos,fattr);
		ENode[] args = ce.args.delToArray();
		args = (ENode[])Arrays.insert(args,~fa.obj,0);
		CallExpr nce = new CallExpr(ce.pos,sfe,m_attr,args);
		ce.replaceWithNodeResolve(null,nce);
		rewriteNode(nce);
		return false;
	}
	
}

