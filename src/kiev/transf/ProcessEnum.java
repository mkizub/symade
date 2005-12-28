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
		
		Field[] eflds = clazz.getEnumFields();
		int pos = clazz.pos;
		
		{
			clazz.super_type = Type.tpEnum;
			Field vals = clazz.addField(new Field(nameEnumValuesFld,
				new ArrayType(clazz.type), ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
			vals.init = new NewInitializedArrayExpr(pos, new TypeRef(clazz.type), 1, ENode.emptyArray);
			for(int i=0; i < eflds.length; i++) {
				ENode e = new SFldExpr(eflds[i].pos,eflds[i]);
				((NewInitializedArrayExpr)vals.init).args.append(e);
			}
		}

		// Generate enum's methods
		
		// values()[]
		{
			MethodType valuestp;
		 	valuestp = new MethodType(Type.emptyArray,new ArrayType(clazz.type));
			Method mvals = new Method(nameEnumValues,valuestp,ACC_PUBLIC | ACC_STATIC);
			mvals.pos = pos;
			mvals.body = new BlockStat(pos);
			((BlockStat)mvals.body).addStatement(
				new ReturnStat(pos,
					new SFldExpr(pos,clazz.resolveField(nameEnumValuesFld)) ) );
			clazz.addMethod(mvals);
		}
		
		// Cast from int
		{
			MethodType tomet;
		 	tomet = new MethodType(new Type[]{Type.tpInt},clazz.type);
			Method tome = new Method(nameCastOp,tomet,ACC_PUBLIC | ACC_STATIC);
			tome.pos = pos;
			tome.params.append(new FormPar(pos,nameEnumOrdinal,Type.tpInt, FormPar.PARAM_NORMAL,0));
			tome.body = new BlockStat(pos);
			SwitchStat sw = new SwitchStat(pos,new LVarExpr(pos,tome.params[0]),CaseLabel.emptyArray);
			//EnumAttr ea = (EnumAttr)clazz.getAttr(attrEnum);
			//if( ea == null )
			//	throw new RuntimeException("enum structure "+clazz+" without "+attrEnum+" attribute");
			CaseLabel[] cases = new CaseLabel[eflds.length+1];
			for(int i=0; i < eflds.length; i++) {
				cases[i] = new CaseLabel(pos,
					new ConstIntExpr(i),
					new ENode[]{
						new ReturnStat(pos,new SFldExpr(pos,eflds[i]))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,null,
					new ENode[]{
						new ThrowStat(pos,new NewExpr(pos,Type.tpCastException,ENode.emptyArray))
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
			tostrt = new MethodType(Type.emptyArray,Type.tpString);
			acc_flags = ACC_PUBLIC;
			Method tostr = new Method(KString.from("toString"),tostrt,acc_flags);
			tostr.name.addAlias(nameCastOp);
			tostr.pos = pos;
			tostr.body = new BlockStat(pos);
			SwitchStat sw = new SwitchStat(pos,
				new CallExpr(pos,	new ThisExpr(),
					Type.tpEnum.clazz.resolveMethod(nameEnumOrdinal, Type.tpInt),
					ENode.emptyArray),
				CaseLabel.emptyArray);
			CaseLabel[] cases = new CaseLabel[eflds.length+1];
			for(int i=0; i < eflds.length; i++) {
				Field f = eflds[i];
				KString str = f.name.name;
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					str = str.substr(1,str.length()-1);
				}
				cases[i] = new CaseLabel(pos,new ConstIntExpr(i)	,
					new ENode[]{
						new ReturnStat(pos,new ConstStringExpr(str))
					});
			}
			cases[cases.length-1] = new CaseLabel(pos,null,
					new ENode[]{
						new ThrowStat(pos,new NewExpr(pos,Type.tpRuntimeException,ENode.emptyArray))
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
			fromstrt = new MethodType(new Type[]{Type.tpString},clazz.type);
			jfromstrt= fromstrt;
			acc_flags = ACC_PUBLIC | ACC_STATIC;
			Method fromstr = new Method(KString.from("valueOf"),fromstrt,acc_flags);
			fromstr.name.addAlias(nameCastOp);
			fromstr.name.addAlias(KString.from("fromString"));
			fromstr.pos = pos;
			fromstr.params.add(new FormPar(pos,KString.from("val"),Type.tpString, FormPar.PARAM_NORMAL,0));
			fromstr.body = new BlockStat(pos);
			AssignExpr ae = new AssignExpr(pos,AssignOperator.Assign,
				new LVarExpr(pos,fromstr.params[0]),
				new CallExpr(pos,
					new LVarExpr(pos,fromstr.params[0]),
					Type.tpString.clazz.resolveMethod(KString.from("intern"),Type.tpString),
					ENode.emptyArray
				));
			((BlockStat)fromstr.body).addStatement(new ExprStat(pos,ae));
			for(int i=0; i < eflds.length; i++) {
				Field f = eflds[i];
				KString str = f.name.name;
				IfElseStat ifst = new IfElseStat(pos,
					new BinaryBoolExpr(pos,BinaryOperator.Equals,
						new LVarExpr(pos,fromstr.params[0]),
						new ConstStringExpr(str)),
					new ReturnStat(pos,new SFldExpr(pos,f)),
					null
					);
				((BlockStat)fromstr.body).addStatement(ifst);
				if (f.name.aliases != List.Nil) {
					str = f.name.aliases.head();
					if (str.byteAt(0) == (byte)'\"') {
						str = str.substr(1,str.length()-1);
						if (str != f.name.name) {
							ifst = new IfElseStat(pos,
								new BinaryBoolExpr(pos,BinaryOperator.Equals,
									new LVarExpr(pos,fromstr.params[0]),
									new ConstStringExpr(str)),
									new ReturnStat(pos,new SFldExpr(pos,f)),
									null
									);
							((BlockStat)fromstr.body).addStatement(ifst);
						}
					}
				}
			}
			((BlockStat)fromstr.body).addStatement(
				new ThrowStat(pos,new NewExpr(pos,Type.tpRuntimeException,ENode.emptyArray))
				);
			clazz.addMethod(fromstr);
		}
	}

}

