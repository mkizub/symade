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
	public static final String nameNArr			= "kiev.vlang.NArr"; 
	public static final String nameAttrSlot		= "kiev.vlang.AttrSlot"; 
	private static final String nameCopyable		= "copyable"; 
	
	private static final String sigValues			= "()[Lkiev/vlang/AttrSlot;";
	private static final String sigGetVal			= "(Ljava/lang/String;)Ljava/lang/Object;";
	private static final String sigSetVal			= "(Ljava/lang/String;Ljava/lang/Object;)V";
	private static final String sigCopy			= "()Ljava/lang/Object;";
	private static final String sigCopyTo			= "(Ljava/lang/Object;)Ljava/lang/Object;";
	
	private static Type tpNArr;
	private static Type tpAttrSlot;

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

	
	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.autoGenerateMembers(dn);
	}
	
	private void autoGenerateMembers(Struct:ASTNode s) {
		if (tpNArr == null)
			tpNArr = Env.loadStruct(nameNArr).ctype;
		if (tpNArr == null) {
			Kiev.reportError(s,"Cannot find class "+nameNArr);
			return;
		}
		if (tpAttrSlot == null)
			tpAttrSlot = Env.loadStruct(nameAttrSlot).ctype;
		if (tpAttrSlot == null) {
			Kiev.reportError(s,"Cannot find class "+nameAttrSlot);
			return;
		}
		foreach (Struct dn; s.members)
			this.autoGenerateMembers(dn);
		if (!s.isClazz())
			return;
		if (!isNodeImpl(s))
			return;
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
		if (hasField(s, nameEnumValuesFld)) {
			Kiev.reportWarning(s,"Field "+s+"."+nameEnumValuesFld+" already exists, @node members are not generated");
			return;
		}
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.meta.get(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNArr);
			Type clz_tp = isArr ? f.getType().bindings().tvars[0].unalias().result() : f.getType();
			TypeClassExpr clz_expr = new TypeClassExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, tpAttrSlot, new ENode[]{
				new ConstStringExpr(f.id.sname),
				new ConstBoolExpr(isAtt),
				new ConstBoolExpr(isArr),
				clz_expr
			});
			String fname = "nodeattr$"+f.id.sname;
			Field af = s.addField(new Field(fname, tpAttrSlot, ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC));
			af.init = e;
			vals_init[i] = new SFldExpr(af.pos, af);
			if (f.parent() != s)
				continue;
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
			for(int i=0; i < aflds.length; i++) {
				ENode ee = new IFldExpr(0,new ThisExpr(0),aflds[i]);
				getV.body.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, getV.params[0]),
							new ConstStringExpr(aflds[i].id.sname)
						),
						new ReturnStat(0, ee),
						null
					)
				);
				if!(ee.getType().isReference())
					CastExpr.autoCastToReference(ee);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr("No @att value \""));
			msg.appendArg(new LVarExpr(0, getV.params[0]));
			msg.appendArg(new ConstStringExpr("\" in "+s.id));
			getV.body.stats.add(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
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
			NArr<ENode> stats = copyV.body.stats;
			Var v = new Var(0, "node",s.ctype,0);
			stats.append(new ReturnStat(0,new ASTCallExpression(0,
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
			NArr<ENode> stats = copyV.body.stats;
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
						cae.args.append(new IFldExpr(0,new ThisExpr(),f));
						stats.append(new ExprStat(0,cae));
					} else {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new IFldExpr(0, new ThisExpr(),f);
						cae.ident = new SymbolRef(0, "copy");
						stats.append( 
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
					stats.append( 
						new ExprStat(0,
							new AssignExpr(0,AssignOperator.Assign,
								new IFldExpr(0,new LVarExpr(0,v),f),
								new IFldExpr(0,new ThisExpr(),f)
							)
						)
					);
				}
			}
			stats.append(new ReturnStat(0,new LVarExpr(0,v)));
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
			for(int i=0; i < aflds.length; i++) {
				boolean isArr = aflds[i].getType().isInstanceOf(tpNArr);
				if (isArr || aflds[i].isFinal() || !Access.writeable(aflds[i]))
					continue;
				{	// check if we may not copy the field
					Meta fmeta = aflds[i].meta.get(mnAtt);
					if (fmeta == null)
						fmeta = aflds[i].meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				Type atp = aflds[i].getType();
				ENode ee;
				if (atp.isReference())
					ee = new CastExpr(0,atp,new LVarExpr(0, setV.params[1]));
				else
					ee = new CastExpr(0,((CoreType)atp).getRefTypeForPrimitive(),new LVarExpr(0, setV.params[1]));
				setV.body.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, setV.params[0]),
							new ConstStringExpr(aflds[i].id.sname)
							),
						new Block(0, new ENode[]{
							new ExprStat(0,
								new AssignExpr(0,AssignOperator.Assign,
									new IFldExpr(0,new ThisExpr(0),aflds[i]),
									ee
								)
							),
							new ReturnStat(0,null)
						}),
						null
					)
				);
				if!(aflds[i].getType().isReference())
					CastExpr.autoCastToPrimitive(ee);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr("No @att value \""));
			msg.appendArg(new LVarExpr(0, setV.params[0]));
			msg.appendArg(new ConstStringExpr("\" in "+s.id));
			setV.body.stats.add(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
		}
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
	
	public static final String nameNode			= "kiev.vlang.ASTNode"; 

	private static Type tpNode;

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
		if (tpNode == null)
			tpNode = Env.loadStruct(nameNode).ctype;
		if (tpNode == null) {
			Kiev.reportError("Cannot find class "+nameNode);
			return;
		}

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
		if (f.type.isInstanceOf(tpNode)) {
			ENode p_st = new IfElseStat(0,
					new BinaryBoolExpr(0, BinaryOperator.NotEquals,
						new IFldExpr(0,new ThisExpr(0),f,true),
						new ConstNullExpr()
					),
					new Block(0,new ENode[]{
						new ExprStat(0,
							new ASTCallAccessExpression(0,
								new IFldExpr(0,new ThisExpr(0),f,true),
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
			old.init = new IFldExpr(0,new ThisExpr(0),f,true);
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
}

