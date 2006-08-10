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

@singleton
public class EnumFE_GenMembers extends TransfProcessor {
	private EnumFE_GenMembers() { super(Kiev.Ext.Enum); }
	public String getDescr() { "Enum members generation" }

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
	
	public void doProcess(Struct:ASTNode clazz) {
		if !( clazz.isEnum() ) {
			foreach (Struct dn; clazz.members)
				this.doProcess(dn);
			return;
		}
		
		Field[] eflds = clazz.getEnumFields();
		int pos = clazz.pos;
		
		{
			if (!clazz.instanceOf(Type.tpEnum.clazz))
				clazz.super_types.insert(0, new TypeRef(Type.tpEnum));
			Field vals = clazz.addField(new Field(nameEnumValuesFld,
				new ArrayType(clazz.xtype), ACC_SYNTHETIC|ACC_PRIVATE|ACC_STATIC|ACC_FINAL));
			vals.init = new NewInitializedArrayExpr(pos, new TypeRef(clazz.xtype), 1, ENode.emptyArray);
			for(int i=0; i < eflds.length; i++) {
				ENode e = new SFldExpr(eflds[i].pos,eflds[i]);
				((NewInitializedArrayExpr)vals.init).args.append(e);
			}
		}

		// Generate enum's methods
		
		// values()[]
		{
			Method mvals = new Method(nameEnumValues,new ArrayType(clazz.xtype),ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC);
			mvals.pos = pos;
			mvals.body = new Block(pos);
			mvals.block.stats.add(
				new ReturnStat(pos,
					new SFldExpr(pos,clazz.resolveField(nameEnumValuesFld)) ) );
			clazz.addMethod(mvals);
		}
		
		// Cast from int
		{
			Method tome = new Method("fromInt",clazz.xtype,ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC);
			tome.aliases += new ASTOperatorAlias(nameCastOp);
			tome.pos = pos;
			tome.params.append(new FormPar(pos,nameEnumOrdinal,Type.tpInt, FormPar.PARAM_NORMAL,0));
			tome.body = new Block(pos);
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
			tome.block.stats.add(sw);
			clazz.addMethod(tome);
		}

		// toString
		{
			Method tostr = new Method("toString",Type.tpString,ACC_PUBLIC | ACC_SYNTHETIC);
			tostr.aliases += new ASTOperatorAlias(nameCastOp);
			tostr.pos = pos;
			tostr.body = new Block(pos);
			SwitchStat sw = new SwitchStat(pos,
				new CallExpr(pos,	new ThisExpr(),
					Type.tpEnum.clazz.resolveMethod(nameEnumOrdinal, Type.tpInt),
					ENode.emptyArray),
				CaseLabel.emptyArray);
			CaseLabel[] cases = new CaseLabel[eflds.length+1];
			for(int i=0; i < eflds.length; i++) {
				Field f = eflds[i];
				String str = f.id.sname;
				MetaAlias al = f.getMetaAlias();
				if (al != null) {
					foreach (ConstStringExpr n; al.getAliases()) {
						str = n.value;
						break;
					}
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
			tostr.block.stats.add(sw);
			clazz.addMethod(tostr);
		}

		// fromString
		{
			Method fromstr = new Method("valueOf",clazz.xtype,ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC);
			fromstr.aliases += new ASTOperatorAlias(nameCastOp);
			fromstr.aliases += new Symbol("fromString");
			fromstr.pos = pos;
			fromstr.params.add(new FormPar(pos,"val",Type.tpString, FormPar.PARAM_NORMAL,0));
			fromstr.body = new Block(pos);
			AssignExpr ae = new AssignExpr(pos,Operator.Assign,
				new LVarExpr(pos,fromstr.params[0]),
				new CallExpr(pos,
					new LVarExpr(pos,fromstr.params[0]),
					Type.tpString.clazz.resolveMethod("intern",Type.tpString),
					ENode.emptyArray
				));
			fromstr.block.stats.add(new ExprStat(pos,ae));
			for(int i=0; i < eflds.length; i++) {
				Field f = eflds[i];
				String str = f.id.sname;
				IfElseStat ifst = new IfElseStat(pos,
					new BinaryBoolExpr(pos,Operator.Equals,
						new LVarExpr(pos,fromstr.params[0]),
						new ConstStringExpr(str)),
					new ReturnStat(pos,new SFldExpr(pos,f)),
					null
					);
				fromstr.block.stats.add(ifst);
				MetaAlias al = f.getMetaAlias();
				if (al != null) {
					foreach (ConstStringExpr n; al.getAliases()) {
						str = n.value;
						if (str != f.id.sname) {
							ifst = new IfElseStat(pos,
								new BinaryBoolExpr(pos,Operator.Equals,
									new LVarExpr(pos,fromstr.params[0]),
									new ConstStringExpr(str)),
									new ReturnStat(pos,new SFldExpr(pos,f)),
									null
									);
							fromstr.block.stats.add(ifst);
						}
					}
				}
			}
			fromstr.block.stats.add(
				new ThrowStat(pos,new NewExpr(pos,Type.tpRuntimeException,ENode.emptyArray))
				);
			clazz.addMethod(fromstr);
		}
	}

}

