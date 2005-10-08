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
	private static final KString nameParent  = KString.from("parent"); 
	private static final KString nameCopyable  = KString.from("copyable"); 
	
	private static final KString sigValues = KString.from("()[Lkiev/vlang/AttrSlot;");
	private static final KString sigGetVal = KString.from("(Ljava/lang/String;)Ljava/lang/Object;");
	private static final KString sigSetVal = KString.from("(Ljava/lang/String;Ljava/lang/Object;)V");
	private static final KString sigCopy   = KString.from("()Ljava/lang/Object;");
	private static final KString sigCopyTo = KString.from("(Ljava/lang/Object;)Ljava/lang/Object;");
	
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
			foreach (Field f; s.fields) {
				if (f.parent == null) {
					Kiev.reportWarning(f.pos,"Field "+f+" has no parent");
					f.parent = s;
				}
				if (f.parent != s) {
					Kiev.reportError(f.pos,"Field "+f+" has wrong parent "+f.parent);
					return;
				}
				verify(f);
			}
		}
		else if (s.super_clazz != null && s.super_clazz.clazz.meta.get(mnNode) != null) {
			Kiev.reportError(s.pos,"Class "+s+" must be marked with @node: it extends @node "+s.super_clazz);
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
			Struct fs = (Struct)f.type.clazz;
			boolean isArr = false;
			if (fs.name.name == nameNArr) {
				if (!f.isFinal()) {
					Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be final");
					f.setFinal(true);
				}
				fs = f.type.args[0].clazz;
				isArr = true;
			}
			Meta fsm = fs.meta.get(mnNode);
//			if (fsm == null) {
//				Kiev.reportWarning(f.pos,"Type "+fs+" of a field "+f.parent+"."+f+" is not a @node");
//				fs.meta.unset(mnAtt);
//				fs.meta.unset(mnRef);
//				return;
//			}
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
					f.init.parent = f;
				} else {
					f.setVirtual(true);
					((Struct)f.parent).addMethodsForVirtualField(f);
				}
			}
			else if (fmref != null) {
				if (isArr) {
					if (f.init != null)
						Kiev.reportError(f.pos,"Field "+f.parent+"."+f+" may not have initializer");
					f.init = new NewExpr(f.pos, f.getType(), new Expr[]{new ThisExpr(), new ConstExpr(f.pos, null)});
					f.init.parent = f;
				}
			}
		} else {
			Struct fs = (Struct)f.type.clazz;
			if (fs.name.name == nameNArr)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
			else if (fs.type.clazz.meta.get(mnNode) != null)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
		}
	}
	
	//////////////////////////////////////////////////////
	//   Generate class members (enumerate sub-nodes)   //
    //////////////////////////////////////////////////////

	private boolean hasField(Struct s, KString name) {
		s.checkResolved();
		foreach(Field f; s.fields; f.name.equals(name) ) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, KString name) {
		s.checkResolved();
		foreach(Method m; s.methods; m.name.equals(name) ) return true;
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
				foreach (Field f; ss.fields; !f.isStatic() && f.meta.get(mnAtt) != null || f.meta.get(mnRef) != null) {
					aflds.insert(p, f);
					p++;
				}
				ss = ss.super_clazz.clazz;
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
			boolean isArr = (aflds[i].getType().clazz.name.name == nameNArr);
			ASTType clz_tp = new ASTType(0, isArr ? aflds[i].getType().args[0] : aflds[i].getType());
			ASTTypeClassExpression clz_expr = new ASTTypeClassExpression();
			clz_expr.type = clz_tp;
			Expr e = new NewExpr(0, atp, new Expr[]{
				new ConstExpr(0, aflds[i].name.name),
				new ConstExpr(0, isAtt ? Boolean.TRUE : Boolean.FALSE),
				new ConstExpr(0, isArr ? Boolean.TRUE : Boolean.FALSE),
				clz_expr
			});
			KString fname = new KStringBuffer().append("nodeattr$").append(aflds[i].name.name).toKString();
			Field f = s.addField(new Field(s, fname, atp, ACC_PUBLIC|ACC_STATIC|ACC_FINAL));
			f.init = e;
			vals_init[i] = new StaticFieldAccessExpr(f.pos, s, f);
		}
		Field vals = s.addField(new Field(s, nameEnumValuesFld, Type.newArrayType(atp), ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
		vals.init = new NewInitializedArrayExpr(0, atp, 1, vals_init);
		vals.init.parent = vals;
		// AttrSlot[] values() { return $values; }
		if (hasMethod(s, nameEnumValues)) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+nameEnumValues+sigValues+" already exists, @node member is not generated");
		} else {
			MethodType et = (MethodType)Type.fromSignature(sigValues);
			Method elems = new Method(s,nameEnumValues,et,ACC_PUBLIC);
			elems.body = new BlockStat(0,elems);
			((BlockStat)elems.body).addStatement(
				new ReturnStat(0,elems.body,
					new StaticFieldAccessExpr(0,s,vals) ) );
			s.addMethod(elems);
			// Object getVal(String)
			MethodType getVt = (MethodType)Type.fromSignature(sigGetVal);
			Method getV = new Method(s,KString.from("getVal"),getVt,ACC_PUBLIC);
			getV.params = new Var[]{
				new Var(0, getV, nameThis, s.type, 0),
				new Var(0, getV, KString.from("name"), Type.tpString, 0),
			};
			getV.body = new BlockStat(0,getV);
			for(int i=0; i < aflds.length; i++) {
				((BlockStat)getV.body).addStatement(
					new IfElseStat(0,
						new BinaryBooleanExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, getV.params[1]),
							new ConstExpr(0, aflds[i].name.name)
						),
						new ReturnStat(0,null, new AccessExpr(0,new ThisExpr(0),aflds[i])),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstExpr(0, KString.from("No @att value \"")));
			msg.appendArg(new VarAccessExpr(0, getV.params[1]));
			msg.appendArg(new ConstExpr(0, KString.from("\" in "+s.name.short_name)));
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
			Method copyV = new Method(s,KString.from("copy"),copyVt,ACC_PUBLIC);
			copyV.params = new Var[]{
				new Var(0, copyV, nameThis, s.type, 0)
			};
			copyV.body = new BlockStat(0,copyV);
			NArr<ASTNode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0,null,KString.from("node"),s.type,0);
			stats.append(new ReturnStat(0,null,new ASTCallExpression(0,
				KString.from("copyTo"),	new Expr[]{new NewExpr(0,s.type,Expr.emptyArray)})));
			s.addMethod(copyV);
		}
		// copyTo(Object)
		if (hasMethod(s, KString.from("copyTo"))) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+"copyTo"+sigCopyTo+" already exists, @node member is not generated");
		} else {
			MethodType copyVt = (MethodType)Type.fromSignature(sigCopyTo);
			Method copyV = new Method(s,KString.from("copyTo"),copyVt,ACC_PUBLIC);
			copyV.params = new Var[]{
				new Var(0, copyV, nameThis, s.type, 0),
				new Var(0, copyV, KString.from("to$node"), Type.tpObject, 0),
			};
			copyV.body = new BlockStat(0,copyV);
			NArr<ASTNode> stats = ((BlockStat)copyV.body).stats;
			Var v = new Var(0,null,KString.from("node"),s.type,0);
			if (s.super_clazz != null && s.super_clazz.clazz.meta.get(mnNode) != null) {
				ASTCallAccessExpression cae = new ASTCallAccessExpression();
				cae.obj = new ASTIdentifier(0,KString.from("super"));
				cae.func = new ASTIdentifier(0,KString.from("copyTo"));
				cae.args.append(new ASTIdentifier(0,KString.from("to$node")));
				stats.append(new DeclStat(0,null,v,new CastExpr(0,s.type,cae)));
			} else {
				stats.append(new DeclStat(0,null,v,new CastExpr(0,s.type,new ASTIdentifier(0,KString.from("to$node")))));
			}
			foreach (Field f; s.fields) {
				if (f.isPackedField() || f.isAbstract() || f.isStatic())
					continue;
				{	// check if we may not copy the field
					Meta fmeta = f.meta.get(mnAtt);
					if (fmeta == null)
						fmeta = f.meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				if (f.name.equals(nameParent))
					continue;
				boolean isNode = (f.getType().clazz.meta.get(mnNode) != null);
				boolean isArr = (f.getType().clazz.name.name == nameNArr);
				if (f.meta.get(mnAtt) != null && (isNode || isArr)) {
					if (isArr) {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new AccessExpr(0,new VarAccessExpr(0,v),f);
						cae.obj.parent = cae;
						cae.func = new ASTIdentifier(0, KString.from("copyFrom"));
						cae.func.parent = cae;
						cae.args.append(new AccessExpr(0,new ThisExpr(),f));
						stats.append(new ExprStat(0,null,cae));
					} else {
						ASTCallAccessExpression cae = new ASTCallAccessExpression();
						cae.obj = new AccessExpr(0, new ThisExpr(),f);
						cae.obj.parent = cae;
						cae.func = new ASTIdentifier(0, KString.from("copy"));
						cae.func.parent = cae;
						stats.append( 
							new IfElseStat(0,null,
								new BinaryBooleanExpr(0, BinaryOperator.NotEquals,
									new AccessExpr(0,new ThisExpr(),f),
									new ConstExpr(0, null)
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
			Method setV = new Method(s,KString.from("setVal"),setVt,ACC_PUBLIC);
			setV.params = new Var[]{
				new Var(0, setV, nameThis, s.type, 0),
				new Var(0, setV, KString.from("name"), Type.tpString, 0),
				new Var(0, setV, KString.from("val"), Type.tpObject, 0),
			};
			setV.body = new BlockStat(0,setV);
			for(int i=0; i < aflds.length; i++) {
				boolean isArr = (aflds[i].getType().clazz.name.name == nameNArr);
				if (isArr || aflds[i].isFinal())
					continue;
				{	// check if we may not set the field
					Meta fmeta = aflds[i].meta.get(mnAtt);
					if (fmeta == null)
						fmeta = aflds[i].meta.get(mnRef);
					if (fmeta != null && !fmeta.getZ(nameCopyable))
						continue; // do not copy the field
				}
				((BlockStat)setV.body).addStatement(
					new IfElseStat(0,
						new BinaryBooleanExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, setV.params[1]),
							new ConstExpr(0, aflds[i].name.name)
							),
						new BlockStat(0,null, new Statement[]{
							new ExprStat(0,null,
								new AssignExpr(0,AssignOperator.Assign,
									new AccessExpr(0,new ThisExpr(0),aflds[i]),
									new CastExpr(0,aflds[i].getType(),new VarAccessExpr(0, setV.params[2]))
								)
							),
							new ReturnStat(0,null)
						}),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstExpr(0, KString.from("No @att value \"")));
			msg.appendArg(new VarAccessExpr(0, setV.params[1]));
			msg.appendArg(new ConstExpr(0, KString.from("\" in "+s.name.short_name)));
			((BlockStat)setV.body).addStatement(
				new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg}))
			);
			s.addMethod(setV);
		}
	}

}
