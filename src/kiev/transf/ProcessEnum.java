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
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class ProcessEnum extends TransfProcessor implements Constants {

	public ProcessEnum(Kiev.Ext ext) {
		super(ext);
	}

	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.autoGenerateMembers(dn);
	}
	
	public void autoGenerateMembers(Struct:ASTNode clazz) {
		if !( clazz.isEnum() ) {
			foreach (ASTNode dn; clazz.members; dn instanceof Struct) {
				this.autoGenerateMembers(dn);
			}
			return;
		}
		
		int pos = clazz.pos;
		
		EnumAttr ea = null;
		
		{
			int enum_fields = 0;
			foreach (ASTNode n; clazz.members; n instanceof Field && n.isEnumField()) {
				enum_fields++;
			}
			Field[] eflds = new Field[enum_fields];
			int[] values = new int[enum_fields];
			{
				int idx = 0;
				foreach (ASTNode n; clazz.members; n instanceof Field && n.isEnumField()) {
					Field f = (Field)n;
					eflds[idx] = f;
					values[idx] = idx;
					idx ++;
				}
			}
			ea = new EnumAttr(eflds,values);
			clazz.addAttr(ea);
			clazz.super_type = Type.tpEnum;
			Field vals = clazz.addField(new Field(nameEnumValuesFld,
				Type.newArrayType(clazz.type), ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
			vals.init = new NewInitializedArrayExpr(pos, new TypeRef(clazz.type), 1, Expr.emptyArray);
			for(int i=0; i < eflds.length; i++) {
				Expr e = new StaticFieldAccessExpr(eflds[i].pos,eflds[i]);
				((NewInitializedArrayExpr)vals.init).args.append(e);
			}
		}

		// Generate enum's methods
		
		// values()[]
		{
			MethodType valuestp;
		 	valuestp = MethodType.newMethodType(null,Type.emptyArray,Type.newArrayType(clazz.type));
			Method mvals = new Method(nameEnumValues,valuestp,ACC_PUBLIC | ACC_STATIC);
			mvals.pos = pos;
			mvals.body = new BlockStat(pos,mvals);
			((BlockStat)mvals.body).addStatement(
				new ReturnStat(pos,mvals.body,
					new StaticFieldAccessExpr(pos,clazz.resolveField(nameEnumValuesFld)) ) );
			clazz.addMethod(mvals);
		}
		
		// Cast from int
		{
			MethodType tomet;
		 	tomet = MethodType.newMethodType(null,new Type[]{Type.tpInt},clazz.type);
			Method tome = new Method(nameCastOp,tomet,ACC_PUBLIC | ACC_STATIC);
			tome.pos = pos;
			tome.params.append(new FormPar(pos,nameEnumOrdinal,Type.tpInt,0));
			tome.body = new BlockStat(pos,tome);
			SwitchStat sw = new SwitchStat(pos,tome.body,new VarAccessExpr(pos,tome.params[0]),CaseLabel.emptyArray);
			//EnumAttr ea = (EnumAttr)clazz.getAttr(attrEnum);
			//if( ea == null )
			//	throw new RuntimeException("enum structure "+clazz+" without "+attrEnum+" attribute");
			CaseLabel[] cases = new CaseLabel[ea.fields.length+1];
			for(int i=0; i < ea.fields.length; i++) {
				cases[i] = new CaseLabel(pos,sw,
					new ConstIntExpr(ea.values[i]),
					new ENode[]{
						new ReturnStat(pos,null,new StaticFieldAccessExpr(pos,ea.fields[i]))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,sw,null,
					new ENode[]{
						new ThrowStat(pos,null,new NewExpr(pos,Type.tpCastException,Expr.emptyArray))
					});
			foreach (CaseLabel c; cases)
				sw.cases.add(c);
			((BlockStat)tome.body).addStatement(sw);
			clazz.addMethod(tome);
		}

		// toString
		{
			MethodType tostrt;
			int acc_flags;
			tostrt = MethodType.newMethodType(null,Type.emptyArray,Type.tpString);
			acc_flags = ACC_PUBLIC;
			Method tostr = new Method(KString.from("toString"),tostrt,acc_flags);
			tostr.name.addAlias(nameCastOp);
			tostr.pos = pos;
			tostr.body = new BlockStat(pos,tostr);
			SwitchStat sw = new SwitchStat(pos,tostr.body,
				new CallExpr(pos,
					(Method)Type.tpEnum.clazz.resolveMethod(nameEnumOrdinal, KString.from("()I")),
					Expr.emptyArray),
				CaseLabel.emptyArray);
			CaseLabel[] cases = new CaseLabel[ea.fields.length+1];
			for(int i=0; i < ea.fields.length; i++) {
				Field f = ea.fields[i];
				KString str = f.name.name;
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					str = str.substr(1,str.length()-1);
				}
				cases[i] = new CaseLabel(pos,sw,new ConstIntExpr(ea.values[i])	,
					new ENode[]{
						new ReturnStat(pos,null,new ConstStringExpr(str))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,sw,null,
					new ENode[]{
						new ThrowStat(pos,null,new NewExpr(pos,Type.tpRuntimeException,Expr.emptyArray))
					});
			foreach (CaseLabel c; cases)
				sw.cases.add(c);
			((BlockStat)tostr.body).addStatement(sw);
			clazz.addMethod(tostr);
		}

		// fromString
		{
			MethodType fromstrt, jfromstrt;
			int acc_flags;
			fromstrt = MethodType.newMethodType(null,new Type[]{Type.tpString},clazz.type);
			jfromstrt= fromstrt;
			acc_flags = ACC_PUBLIC | ACC_STATIC;
			Method fromstr = new Method(KString.from("valueOf"),fromstrt,acc_flags);
			fromstr.name.addAlias(nameCastOp);
			fromstr.name.addAlias(KString.from("fromString"));
			fromstr.pos = pos;
			fromstr.params.add(new FormPar(pos,KString.from("val"),Type.tpString,0));
			fromstr.body = new BlockStat(pos,fromstr);
			AssignExpr ae = new AssignExpr(pos,AssignOperator.Assign,
				new VarAccessExpr(pos,fromstr.params[0]),
				new CallAccessExpr(pos,
					new VarAccessExpr(pos,fromstr.params[0]),
					Type.tpString.clazz.resolveMethod(
						KString.from("intern"),KString.from("()Ljava/lang/String;"),true
					),
					Expr.emptyArray
				));
			((BlockStat)fromstr.body).addStatement(new ExprStat(pos,null,ae));
			for(int i=0; i < ea.fields.length; i++) {
				Field f = ea.fields[i];
				KString str = f.name.name;
				IfElseStat ifst = new IfElseStat(pos,null,
					new BinaryBoolExpr(pos,BinaryOperator.Equals,
						new VarAccessExpr(pos,fromstr.params[0]),
						new ConstStringExpr(str)),
					new ReturnStat(pos,null,new StaticFieldAccessExpr(pos,f)),
					null
					);
				((BlockStat)fromstr.body).addStatement(ifst);
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					if (str.byteAt(0) == (byte)'\"') {
						str = str.substr(1,str.length()-1);
						if (str != f.name.name) {
							ifst = new IfElseStat(pos,null,
								new BinaryBoolExpr(pos,BinaryOperator.Equals,
									new VarAccessExpr(pos,fromstr.params[0]),
									new ConstStringExpr(str)),
									new ReturnStat(pos,null,new StaticFieldAccessExpr(pos,f)),
									null
									);
							((BlockStat)fromstr.body).addStatement(ifst);
						}
					}
				}
			}
			((BlockStat)fromstr.body).addStatement(
				new ThrowStat(pos,null,new NewExpr(pos,Type.tpRuntimeException,Expr.emptyArray))
				);
			clazz.addMethod(fromstr);
		}
	}

}

