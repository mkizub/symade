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

public final class ProcessVNode implements Constants {

	public static final KString mnNode = KString.from("kiev.vlang.node"); 
	public static final KString mnAtt  = KString.from("kiev.vlang.att"); 
	public static final KString mnRef  = KString.from("kiev.vlang.ref"); 
	public static final KString nameNArr  = KString.from("kiev.vlang.NArr"); 
	private static final KString nameNArrReplace  = KString.from("replace"); 
	private static final KString signNArrReplace  = KString.from("(Ljava/lang/Object;Akiev/vlang/NArr$N;)V"); 
	private static final KString nameParent  = KString.from("parent"); 
	private static final KString nameCopyable  = KString.from("copyable"); 
	
	private static final KString sigValues = KString.from("()[Lkiev/vlang/AttrSlot;");
	private static final KString sigGetVal = KString.from("(Ljava/lang/String;)Ljava/lang/Object;");
	private static final KString sigSetVal = KString.from("(Ljava/lang/String;Ljava/lang/Object;)V");
	private static final KString sigReplaceVal = KString.from("(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V");
	private static final KString sigCopy   = KString.from("()Ljava/lang/Object;");
	private static final KString sigCopyTo   = KString.from("(Ljava/lang/Object;)Ljava/lang/Object;");
	
	private Type tpNArr = Env.getStruct(nameNArr).type;
	
	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

	public boolean verify() {
		boolean failed = false;
		for (int i=0; i < Kiev.files.length; i++) {
			FileUnit fu = Kiev.files[i]; 
			if( fu == null ) continue;
			try {
				verify(fu);
			} catch (Exception e) {
				Kiev.reportError(0,e); failed = true;
			}
		}
		return failed;
	}
	
	private void verify(ASTNode:ASTNode node) {
	}
	
	private void verify(FileUnit:ASTNode fu) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = fu.filename;
		PassInfo.push(fu);
		try {
			foreach (ASTNode n; fu.members; n instanceof Struct) {
				verify(n);
			}
		} finally { PassInfo.pop(fu); Kiev.curFile = oldfn; }
	}
	
	private void verify(Struct:ASTNode s) {
		Meta m = s.meta.get(mnNode);
		if (m != null) {
			// Check fields of the @node
			foreach (ASTNode n; s.members; n instanceof Field) {
				verify(n);
			}
		}
		else if (s.super_bound.isBound() && s.super_type.getStructMeta().get(mnNode) != null) {
			Kiev.reportError(s.pos,"Class "+s+" must be marked with @node: it extends @node "+s.super_type);
			return;
		}
	}
	
	private void verify(Field:ASTNode f) {
		Meta fmatt = f.meta.get(mnAtt);
		Meta fmref = f.meta.get(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @att="+fmatt+" and @ref="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f.pos,"Field "+f.parent+"."+f+" marked both @att and @ref");
		}
		if (fmatt != null || fmref != null) {
			boolean isArr = false;
			Meta fsm;
			{
				Type ft = f.type;
				if (ft.isInstanceOf(tpNArr)) {
					if (!f.isFinal()) {
						Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be final");
						f.setFinal(true);
					}
					isArr = true;
				}
				fsm = ft.getStructMeta().get(mnNode);
			}
			//System.out.println("process @node: field "+f+" of type "+fs+" has correct @att="+fmatt+" or @ref="+fmref);
			if (fmatt != null) {
				if (isArr) {
					if (f.init != null)
						Kiev.reportError(f.pos,"Field "+f.parent+"."+f+" may not have initializer");
					KString fname = new KStringBuffer().append("nodeattr$").append(f.name.name).toKString();
					Struct fs = (Struct)f.parent;
					Field fatt = fs.resolveField(fname);
					f.init = new NewExpr(f.pos, f.getType(), new Expr[]{
						new ThisExpr(),
						new StaticFieldAccessExpr(f.pos, fs, fatt)
					});
				} else {
					f.setVirtual(true);
					ProcessVirtFld.addMethodsForVirtualField((Struct)f.parent, f);
				}
			}
			else if (fmref != null) {
				if (isArr) {
					if (f.init != null)
						Kiev.reportError(f.pos,"Field "+f.parent+"."+f+" may not have initializer");
					f.init = new NewExpr(f.pos, f.getType(), new Expr[]{new ThisExpr(), new ConstNullExpr()});
				}
			}
		} else {
			if (f.type.isInstanceOf(tpNArr))
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
			else if (f.type.getStructMeta().get(mnNode) != null)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
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
	
	public void autoGenerateMembers(Struct s) {
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
				int p = 0;
				foreach (ASTNode n; ss.members; n instanceof Field && !n.isStatic() && ((Field)n).meta.get(mnAtt) != null) {
					Field f = (Field)n;
					aflds.insert(p, f);
					p++;
				}
				ss = ss.super_type.getStruct();
			}
		}
		if (hasField(s, nameEnumValuesFld)) {
			Kiev.reportWarning(s.pos,"Field "+s+"."+nameEnumValuesFld+" already exists, @node members are not generated");
			return;
		}
		Type atp = Type.fromSignature(KString.from("Lkiev/vlang/AttrSlot;"));
		Expr[] vals_init = new Expr[aflds.size()];
		for(int i=0; i < vals_init.length; i++) {
			boolean isAtt = (aflds[i].meta.get(mnAtt) != null);
			boolean isArr = aflds[i].getType().isInstanceOf(tpNArr);
			Expr e = new NewExpr(0, atp, new Expr[]{
				new ConstStringExpr(aflds[i].name.name),
				new ConstBoolExpr(isAtt),
				new ConstBoolExpr(isArr)
			});
			KString fname = new KStringBuffer().append("nodeattr$").append(aflds[i].name.name).toKString();
			Field f = s.addField(new Field(fname, atp, ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
			f.init = e;
			vals_init[i] = new StaticFieldAccessExpr(f.pos, s, f);
		}
		Field vals = s.addField(new Field(nameEnumValuesFld, Type.newArrayType(atp), ACC_PUBLIC|ACC_STATIC|ACC_FINAL));
		vals.init = new NewInitializedArrayExpr(0, new TypeRef(atp), 1, vals_init);
		vals.init.parent = vals;
		// AttrSlot[] values() { return $values; }
		if (hasMethod(s, nameEnumValues)) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+nameEnumValues+sigValues+" already exists, @node member is not generated");
		} else {
			MethodType et = (MethodType)Type.fromSignature(sigValues);
			Method elems = new Method(nameEnumValues,et,ACC_PUBLIC);
			elems.body = new BlockStat(0,elems);
			((BlockStat)elems.body).addStatement(
				new ReturnStat(0,elems.body,
					new StaticFieldAccessExpr(0,s,vals) ) );
			s.addMethod(elems);
			// Object getVal(String)
			MethodType getVt = (MethodType)Type.fromSignature(sigGetVal);
			Method getV = new Method(KString.from("getVal"),getVt,ACC_PUBLIC);
			getV.params.add(new FormPar(0, KString.from("name"), Type.tpString, 0));
			getV.body = new BlockStat(0,getV);
			for(int i=0; i < aflds.length; i++) {
				((BlockStat)getV.body).addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, getV.params[0]),
							new ConstStringExpr(aflds[i].name.name)
						),
						new ReturnStat(0,null, new AccessExpr(0,new ThisExpr(0),aflds[i])),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @att value \"")));
			msg.appendArg(new VarAccessExpr(0, getV.params[0]));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			((BlockStat)getV.body).addStatement(
				new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg}))
			);
			s.addMethod(getV);
		}
		// copy()
		if (!mnMeta.getZ(nameCopyable) || s.isAbstract()) {
			// node is not copyable
		}
		else if (hasMethod(s, KString.from("copy"))) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+"copy"+sigCopy+" already exists, @node member is not generated");
		}
		else {
			MethodType copyVt = (MethodType)Type.fromSignature(sigCopy);
			Method copyV = new Method(KString.from("copy"),copyVt,ACC_PUBLIC);
			copyV.body = new BlockStat(0,copyV);
			NArr<ASTNode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0, KString.from("node"),s.type,0);
			stats.append(new ReturnStat(0,null,new ASTCallExpression(0,
				KString.from("copyTo"),	new Expr[]{new NewExpr(0,s.type,Expr.emptyArray)})));
			s.addMethod(copyV);
		}
		// copyTo(Object)
		if (hasMethod(s, KString.from("copyTo"))) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			MethodType copyVt = (MethodType)Type.fromSignature(sigCopyTo);
			Method copyV = new Method(KString.from("copyTo"),copyVt,ACC_PUBLIC);
			copyV.params.append(new FormPar(0,KString.from("to$node"), Type.tpObject, 0));
			copyV.body = new BlockStat();
			NArr<ENode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0,KString.from("node"),s.type,0);
			if (s.super_bound.isBound() && s.super_type.getStructMeta().get(mnNode) != null) {
				ASTCallAccessExpression cae = new ASTCallAccessExpression();
				cae.obj = new ASTIdentifier(0,KString.from("super"));
				cae.func = new ASTIdentifier(0,KString.from("copyTo"));
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
				boolean isNode = (f.getType().getStructMeta().get(mnNode) != null);
				boolean isArr = f.getType().isInstanceOf(tpNArr);
				if (f.meta.get(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new AccessExpr(0,new VarAccessExpr(0,v),f);
						cae.func = new ASTIdentifier(0, KString.from("copyFrom"));
						cae.args.append(new AccessExpr(0,new ThisExpr(),f));
						stats.append(new ExprStat(0,null,cae));
					} else {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new AccessExpr(0, new ThisExpr(),f);
						cae.func = new ASTIdentifier(0, KString.from("copy"));
						stats.append( 
							new IfElseStat(0,null,
								new BinaryBoolExpr(0, BinaryOperator.NotEquals,
									new AccessExpr(0,new ThisExpr(),f),
									new ConstNullExpr()
									),
								new ExprStat(0,null,
									new AssignExpr(0,AssignOperator.Assign,
										new AccessExpr(0,new VarAccessExpr(0,v),f),
										new CastExpr(0,f.getType(),cae)
									)
								),
								null
							)
						);
					}
				} else {
					stats.append( 
						new ExprStat(0,null,
							new AssignExpr(0,AssignOperator.Assign,
								new AccessExpr(0,new VarAccessExpr(0,v),f),
								new AccessExpr(0,new ThisExpr(),f)
							)
						)
					);
				}
			}
			stats.append(new ReturnStat(0,null,new VarAccessExpr(0,null,v)));
			s.addMethod(copyV);
		}
		// setVal(String, Object)
		if (hasMethod(s, KString.from("setVal"))) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+"setVal"+sigSetVal+" already exists, @node member is not generated");
		} else {
			MethodType setVt = (MethodType)Type.fromSignature(sigSetVal);
			Method setV = new Method(KString.from("setVal"),setVt,ACC_PUBLIC);
			setV.params.append(new FormPar(0, KString.from("name"), Type.tpString, 0));
			setV.params.append(new FormPar(0, KString.from("val"), Type.tpObject, 0));
			setV.body = new BlockStat(0,setV);
			for(int i=0; i < aflds.length; i++) {
				boolean isArr = aflds[i].getType().isInstanceOf(tpNArr);
				if (isArr || aflds[i].isFinal())
					continue;
				((BlockStat)setV.body).addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, setV.params[0]),
							new ConstStringExpr(aflds[i].name.name)
							),
						new BlockStat(0,null, new Statement[]{
							new ExprStat(0,null,
								new AssignExpr(0,AssignOperator.Assign,
									new AccessExpr(0,new ThisExpr(0),aflds[i]),
									new CastExpr(0,aflds[i].getType(),new VarAccessExpr(0, setV.params[1]))
								)
							),
							new ReturnStat(0,null)
						}),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @att value \"")));
			msg.appendArg(new VarAccessExpr(0, setV.params[0]));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			((BlockStat)setV.body).addStatement(
				new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg}))
			);
			s.addMethod(setV);
		}
		// replaceVal(String name, Object old, Object val)
		if (hasMethod(s, KString.from("replaceVal"))) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+"replaceVal"+sigReplaceVal+" already exists, @node member is not generated");
		} else {
			MethodType setVt = (MethodType)Type.fromSignature(sigReplaceVal);
			Method setV = new Method(KString.from("replaceVal"),setVt,ACC_PUBLIC);
			setV.params.append(new FormPar(0, KString.from("name"), Type.tpString, 0));
			setV.params.append(new FormPar(0, KString.from("old"), Type.tpObject, 0));
			setV.params.append(new FormPar(0, KString.from("val"), Type.tpObject, 0));
			setV.body = new BlockStat(0,setV);
			for(int i=0; i < aflds.length; i++) {
				boolean isArr = aflds[i].getType().isInstanceOf(tpNArr);
				if (!isArr && aflds[i].isFinal())
					continue;
				BlockStat bs;
				((BlockStat)setV.body).addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, setV.params[0]),
							new ConstStringExpr(aflds[i].name.name)
							),
						bs = new BlockStat(0,null),
						null
					)
				);
				if (!isArr) {
					StringConcatExpr msg = new StringConcatExpr();
					msg.appendArg(new ConstStringExpr(KString.from("Missmatch node for \"")));
					msg.appendArg(new VarAccessExpr(0, setV.params[0]));
					msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
					bs.addStatement(
						new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new VarAccessExpr(0, setV.params[1]),
								new AccessExpr(0,new ThisExpr(0),aflds[i])
								),
							new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg})),
							null
						)
					);
					bs.addStatement(
						new ExprStat(0,null,
							new AssignExpr(0,AssignOperator.Assign,
								new AccessExpr(0,new ThisExpr(0),aflds[i]),
								new CastExpr(0,aflds[i].getType(),new VarAccessExpr(0, setV.params[2]))
							)
						)
					);
				} else {
					bs.addStatement(
						new ExprStat(0,null,
							new CallAccessExpr(0,null,
								new AccessExpr(0,new ThisExpr(0),aflds[i]),
								Env.getStruct(nameNArr).resolveMethod(nameNArrReplace, signNArrReplace),
								new Expr[]{
									new VarAccessExpr(0, setV.params[1]),
									new CastExpr(0,aflds[i].getType().args[0],new VarAccessExpr(0, setV.params[2]))
								}
							)
						)
					);
				}
				bs.addStatement(new ReturnStat(0,null));
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @att value \"")));
			msg.appendArg(new VarAccessExpr(0, setV.params[0]));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			((BlockStat)setV.body).addStatement(
				new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg}))
			);
			s.addMethod(setV);
		}
	}

}
