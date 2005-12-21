/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

public final class ProcessVNode extends TransfProcessor implements Constants {

	public static final KString mnNode				= KString.from("kiev.vlang.node"); 
	public static final KString mnNodeView			= KString.from("kiev.vlang.nodeview"); 
	public static final KString mnAtt				= KString.from("kiev.vlang.att"); 
	public static final KString mnRef				= KString.from("kiev.vlang.ref"); 
	public static final KString nameNArr			= KString.from("kiev.vlang.NArr"); 
	private static final KString nameParent		= KString.from("parent"); 
	private static final KString nameCopyable		= KString.from("copyable"); 
	
	private static final KString sigValues			= KString.from("()[Lkiev/vlang/AttrSlot;");
	private static final KString sigGetVal			= KString.from("(Ljava/lang/String;)Ljava/lang/Object;");
	private static final KString sigSetVal			= KString.from("(Ljava/lang/String;Ljava/lang/Object;)V");
	private static final KString sigCopy			= KString.from("()Ljava/lang/Object;");
	private static final KString sigCopyTo			= KString.from("(Ljava/lang/Object;)Ljava/lang/Object;");
	
	private static Type tpNArr;
	
	public ProcessVNode(Kiev.Ext ext) {
		super(ext);
	}

	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

	public void pass3(ASTNode:ASTNode node) {
	}
	
	public void pass3(FileUnit:ASTNode fu) {
		if (tpNArr == null)
			tpNArr = Env.getStruct(nameNArr).type;
		foreach (ASTNode n; fu.members; n instanceof Struct)
			pass3(n);
	}
	
	public void pass3(Struct:ASTNode s) {
		foreach (Struct sub; s.sub_clazz)
			pass3(sub);
		Meta m = s.meta.get(mnNode);
		if (m != null) {
			// Check fields of the @node
			foreach (ASTNode n; s.members; n instanceof Field)
				pass3(n);
		}
		else if (s.super_bound.isBound() && s.super_type.getStruct() != null && s.super_type.getStruct().meta.get(mnNode) != null) {
			if (s.meta.get(mnNodeView) == null)
				Kiev.reportError(s,"Class "+s+" must be marked with @node: it extends @node "+s.super_type);
			return;
		}
		else {
			// Check fields to not have @att and @ref
			foreach (ASTNode n; s.members; n instanceof Field) {
				Field f = (Field)n;
				Meta fmatt = f.meta.get(mnAtt);
				Meta fmref = f.meta.get(mnRef);
				if (fmatt != null || fmref != null) {
					Kiev.reportError(f,"Field "+f+" of non-@node class "+f.parent+" may not be @att or @ref");
				}
			}
		}
	}
	
	public void pass3(Field:ASTNode f) {
		Meta fmatt = f.meta.get(mnAtt);
		Meta fmref = f.meta.get(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @att="+fmatt+" and @ref="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f,"Field "+f.parent+"."+f+" marked both @att and @ref");
		}
		if (fmatt != null || fmref != null) {
			if (f.isStatic())
				Kiev.reportError(f,"Field "+f.parent+"."+f+" is static and cannot have @att or @ref");
			boolean isArr = false;
			Meta fsm;
			{
				Type ft = f.type;
				if (ft.isInstanceOf(tpNArr)) {
					//if (!f.isFinal()) {
					//	Kiev.reportWarning(f,"Field "+f.parent+"."+f+" must be final");
						f.setFinal(true);
					//}
					isArr = true;
				}
				fsm = ft.getStruct().meta.get(mnNode);
			}
			//System.out.println("process @node: field "+f+" of type "+fs+" has correct @att="+fmatt+" or @ref="+fmref);
			if (fmatt != null) {
				if (isArr && f.init != null) {
					Kiev.reportError(f,"Field "+f.parent+"."+f+" may not have initializer");
				}
				if (!isArr)
					f.setVirtual(true);
			}
			else if (fmref != null) {
				if (isArr && f.init != null)
					Kiev.reportError(f,"Field "+f.parent+"."+f+" may not have initializer");
			}
		}
		else if !(f.isStatic()) {
			if (f.type.isInstanceOf(tpNArr))
				Kiev.reportWarning(f,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
			else if (f.type.getStruct().meta.get(mnNode) != null)
				Kiev.reportWarning(f,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
		}
	}
	
	//////////////////////////////////////////////////////
	//   Generate class members (enumerate sub-nodes)   //
    //////////////////////////////////////////////////////

	private boolean hasField(Struct s, KString name) {
		s.checkResolved();
		foreach (ASTNode n; s.members; n instanceof Field && ((Field)n).name.equals(name)) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, KString name) {
		s.checkResolved();
		foreach (ASTNode n; s.members; n instanceof Method && ((Method)n).name.equals(name)) return true;
		return false;
	}

	
	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.autoGenerateMembers(dn);
	}
	
	private void autoGenerateMembers(Struct:ASTNode s) {
		if (tpNArr == null)
			tpNArr = Env.getStruct(nameNArr).type;
		if (tpNArr == null) {
			Kiev.reportError(s,"Cannot find class "+nameNArr);
			return;
		}
		foreach (DNode dn; s.members; dn instanceof Struct)
			this.autoGenerateMembers(dn);
		if (!s.isClazz())
			return;
		Meta mnMeta = s.meta.get(mnNode);
		if (mnMeta == null)
			return;
		// attribute names array
		Vector<Field> aflds = new Vector<Field>();
		{
			Struct ss = s;
			while (ss != null && ss.meta.get(mnNode) != null) {
				foreach (DNode n; ss.members; n instanceof Field && !n.isStatic() && (n.meta.get(mnAtt) != null || n.meta.get(mnRef) != null)) {
					Field f = (Field)n;
					aflds.append(f);
				}
				ss = ss.super_type.getStruct();
			}
		}
		if (hasField(s, nameEnumValuesFld)) {
			Kiev.reportWarning(s,"Field "+s+"."+nameEnumValuesFld+" already exists, @node members are not generated");
			return;
		}
		Type atp = Type.fromSignature(KString.from("Lkiev/vlang/AttrSlot;"));
		ENode[] vals_init = new ENode[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			Field f = aflds[i];
			boolean isAtt = (f.meta.get(mnAtt) != null);
			boolean isArr = f.getType().isInstanceOf(tpNArr);
			Type clz_tp = isArr ? f.getType().bindings[0] : f.getType();
			TypeClassExpr clz_expr = new TypeClassExpr(0, new TypeRef(clz_tp));
			ENode e = new NewExpr(0, atp, new ENode[]{
				new ConstStringExpr(f.name.name),
				new ConstBoolExpr(isAtt),
				new ConstBoolExpr(isArr),
				clz_expr
			});
			KString fname = new KStringBuffer().append("nodeattr$").append(f.name.name).toKString();
			Field af = s.addField(new Field(fname, atp, ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
			af.init = e;
			vals_init[i] = new SFldExpr(af.pos, af);
			if (f.parent != s)
				continue;
			if (isArr && !f.isAbstract()) {
				f.init = new NewExpr(f.pos, f.getType(), new ENode[]{
					new ThisExpr(),
					new SFldExpr(f.pos, af)
				});
			}
			if (isAtt && !isArr)
				f.setVirtual(true);
		}
		Field vals = s.addField(new Field(nameEnumValuesFld, Type.newArrayType(atp), ACC_PUBLIC|ACC_STATIC|ACC_FINAL));
		vals.init = new NewInitializedArrayExpr(0, new TypeRef(atp), 1, vals_init);
		// AttrSlot[] values() { return $values; }
		if (hasMethod(s, nameEnumValues)) {
			Kiev.reportWarning(s,"Method "+s+"."+nameEnumValues+sigValues+" already exists, @node member is not generated");
		} else {
			MethodType et = (MethodType)Type.fromSignature(sigValues);
			Method elems = new Method(nameEnumValues,et,ACC_PUBLIC);
			s.addMethod(elems);
			elems.body = new BlockStat(0);
			((BlockStat)elems.body).addStatement(
				new ReturnStat(0,
					new SFldExpr(0,vals) ) );
			// Object getVal(String)
			MethodType getVt = (MethodType)Type.fromSignature(sigGetVal);
			Method getV = new Method(KString.from("getVal"),getVt,ACC_PUBLIC);
			getV.params.add(new FormPar(0, KString.from("name"), Type.tpString, FormPar.PARAM_NORMAL, 0));
			s.addMethod(getV);
			getV.body = new BlockStat(0);
			for(int i=0; i < aflds.length; i++) {
				ENode ee = new IFldExpr(0,new ThisExpr(0),aflds[i]);
				((BlockStat)getV.body).addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, getV.params[0]),
							new ConstStringExpr(aflds[i].name.name)
						),
						new ReturnStat(0, ee),
						null
					)
				);
				if!(ee.getType().isReference())
					CastExpr.autoCastToReference(ee);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @att value \"")));
			msg.appendArg(new LVarExpr(0, getV.params[0]));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			((BlockStat)getV.body).addStatement(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
		}
		// copy()
		if (!mnMeta.getZ(nameCopyable) || s.isAbstract()) {
			// node is not copyable
		}
		else if (hasMethod(s, KString.from("copy"))) {
			Kiev.reportWarning(s,"Method "+s+"."+"copy"+sigCopy+" already exists, @node member is not generated");
		}
		else {
			MethodType copyVt = (MethodType)Type.fromSignature(sigCopy);
			Method copyV = new Method(KString.from("copy"),copyVt,ACC_PUBLIC);
			s.addMethod(copyV);
			copyV.body = new BlockStat(0);
			NArr<ASTNode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0, KString.from("node"),s.type,0);
			stats.append(new ReturnStat(0,new ASTCallExpression(0,
				KString.from("copyTo"),	new ENode[]{new NewExpr(0,s.type,ENode.emptyArray)})));
		}
		// copyTo(Object)
		if (hasMethod(s, KString.from("copyTo"))) {
			Kiev.reportWarning(s,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			MethodType copyVt = (MethodType)Type.fromSignature(sigCopyTo);
			Method copyV = new Method(KString.from("copyTo"),copyVt,ACC_PUBLIC);
			copyV.params.append(new FormPar(0,KString.from("to$node"), Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(copyV);
			copyV.body = new BlockStat();
			NArr<ENode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0,KString.from("node"),s.type,0);
			if (s.super_bound.isBound() && s.super_type.getStruct() != null && s.super_type.getStruct().meta.get(mnNode) != null) {
				ASTCallAccessExpression cae = new ASTCallAccessExpression();
				cae.obj = new ASTIdentifier(0,KString.from("super"));
				cae.func = new NameRef(0,KString.from("copyTo"));
				cae.args.append(new ASTIdentifier(0,KString.from("to$node")));
				v.init = new CastExpr(0,s.type,cae);
				((BlockStat)copyV.body).addSymbol(v);
			} else {
				v.init = new CastExpr(0,s.type,new ASTIdentifier(0,KString.from("to$node")));
				((BlockStat)copyV.body).addSymbol(v);
			}
			foreach (ASTNode n; s.members; n instanceof Field) {
				Field f = (Field)n;
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
					continue;
				{	// check if we may not copy the field
					Meta fmeta = f.meta.get(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				boolean isNode = (f.getType().getStruct().meta.get(mnNode) != null);
				boolean isArr = f.getType().isInstanceOf(tpNArr);
				if (f.meta.get(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new IFldExpr(0,new LVarExpr(0,v),f);
						cae.func = new NameRef(0, KString.from("copyFrom"));
						cae.args.append(new IFldExpr(0,new ThisExpr(),f));
						stats.append(new ExprStat(0,cae));
					} else {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new IFldExpr(0, new ThisExpr(),f);
						cae.func = new NameRef(0, KString.from("copy"));
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
		if (hasMethod(s, KString.from("setVal"))) {
			Kiev.reportWarning(s,"Method "+s+"."+"setVal"+sigSetVal+" already exists, @node member is not generated");
		} else {
			MethodType setVt = (MethodType)Type.fromSignature(sigSetVal);
			Method setV = new Method(KString.from("setVal"),setVt,ACC_PUBLIC);
			setV.params.append(new FormPar(0, KString.from("name"), Type.tpString, FormPar.PARAM_NORMAL, 0));
			setV.params.append(new FormPar(0, KString.from("val"), Type.tpObject, FormPar.PARAM_NORMAL, 0));
			s.addMethod(setV);
			setV.body = new BlockStat(0);
			for(int i=0; i < aflds.length; i++) {
				boolean isArr = aflds[i].getType().isInstanceOf(tpNArr);
				if (isArr || aflds[i].isFinal() || !aflds[i].acc.writeable())
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
					ee = new CastExpr(0,Type.getRefTypeForPrimitive(atp),new LVarExpr(0, setV.params[1]));
				((BlockStat)setV.body).addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, setV.params[0]),
							new ConstStringExpr(aflds[i].name.name)
							),
						new BlockStat(0, new ENode[]{
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
			msg.appendArg(new ConstStringExpr(KString.from("No @att value \"")));
			msg.appendArg(new LVarExpr(0, setV.params[0]));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			((BlockStat)setV.body).addStatement(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
		}
	}

}
