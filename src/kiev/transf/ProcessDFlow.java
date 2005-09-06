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

public final class ProcessDFlow implements Constants {

	public static final KString mnNode				= KString.from("kiev.vlang.dflow"); 
	public static final KString nameNArr			= KString.from("kiev.vlang.NArr"); 
	public static final KString nameNode			= KString.from("kiev.vlang.ASTNode"); 

	public static final KString nameGetDFlowIn		= KString.from("getDFlowIn"); 
	public static final KString signGetDFlowIn		= KString.from("(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"); 
	public static final KString signGetDFlowInFld	= KString.from("()Lkiev/vlang/DFState;"); 
	
	private static Type tpNArr;
	private static Type tpNode;
	
	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

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
		if (tpNArr == null)
			tpNArr = Env.getStruct(nameNArr).type;
		if (tpNArr == null) {
			Kiev.reportError(0,"Cannot find class "+nameNArr);
			return;
		}
		if (tpNode == null)
			tpNode = Env.getStruct(nameNode).type;
		if (tpNode == null) {
			Kiev.reportError(0,"Cannot find class "+nameNode);
			return;
		}
		if (!s.isClazz())
			return;
		Meta mnMeta = s.meta.get(mnNode);
		if (mnMeta == null) {
			return;
		}
		// dflow fields names array
		Vector<Field> aflds = new Vector<Field>();
		{
			Struct ss = s;
			while (ss != null && ss.meta.get(mnNode) != null) {
				int p = 0;
				foreach (Field f; ss.fields; !f.isStatic() && f.meta.get(mnNode) != null) {
					aflds.insert(p, f);
					p++;
				}
				ss = ss.super_clazz.clazz;
			}
		}
		// DFState getDFlowIn(ASTNode child)
		if (hasMethod(s, nameGetDFlowIn)) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+nameGetDFlowIn+" already exists, @dflow member is not generated");
		} else {
			MethodType mt = (MethodType)Type.fromSignature(signGetDFlowIn);
			Method dfIn = new Method(s,nameGetDFlowIn,mt,ACC_PUBLIC);
			dfIn.params = new Var[]{
				new Var(0, dfIn, nameThis, s.type, 0),
				new Var(0, dfIn, KString.from("child"), tpNode, 0),
			};
			dfIn.body = new BlockStat(0,dfIn);
			Var var = new Var(0, KString.from("name"),Type.tpString,ACC_FINAL);
			DeclStat vdecl = new DeclStat(0,null,var);
			((BlockStat)dfIn.body).addStatement(vdecl);
			{
				ASTAccessExpression ae0 = new ASTAccessExpression();
				ae0.obj = new VarAccessExpr(0,dfIn.params[1]);
				ae0.ident = new ASTIdentifier(0,KString.from("pslot"));
				ASTAccessExpression ae1 = new ASTAccessExpression();
				ae1.obj = ae0;
				ae1.ident = new ASTIdentifier(0,KString.from("name"));
				vdecl.init = ae1;
			}
			for(int i=0; i < aflds.length; i++) {
				KString fldnm = aflds[i].name.name;
				KString fname = KString.from(nameGetDFlowIn+"$"+fldnm);
				ASTCallExpression ce = new ASTCallExpression();
				ce.func = new ASTIdentifier(0,fname);
				((BlockStat)dfIn.body).addStatement(
					new IfElseStat(0,null,
						new BinaryBooleanExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, var),
							new ConstExpr(0,fldnm)
						),
						new ReturnStat(0,null, ce),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstExpr(0,KString.from("No @dflow value \"")));
			msg.appendArg(new VarAccessExpr(0, var));
			msg.appendArg(new ConstExpr(0,KString.from("\" in "+s.name.short_name)));
			((BlockStat)dfIn.body).addStatement(
				new ThrowStat(0,null,new NewExpr(0,Type.tpRuntimeException,new Expr[]{msg}))
			);
			s.addMethod(dfIn);
		}
		// DFState getDFlowIn$xxx()
		for(int i=0; i < aflds.length; i++) {
			Field fld = aflds[i];
			KString fldnm = fld.name.name;
			KString fname = KString.from(nameGetDFlowIn+"$"+fldnm);
			if (hasMethod(s, fname)) {
				Kiev.reportWarning(s.pos,"Method "+s+"."+fname+" already exists, @dflow member is not generated");
			} else {
				Meta meta = fld.meta.get(mnNode);
				KString src = meta.getS(KString.from("in"));
				ASTAccessExpression acc_fld = null;
				ASTCallAccessExpression cae_tru = null;
				ASTCallAccessExpression cae_fls = null;
				{
					KString acc_nm;
					KString fun_nm;
					int idx = src.indexOf((byte)':');
					if (idx < 0) {
						if (src == KString.Empty) {
							acc_nm = nameThis;
							fun_nm = KString.from("getDFlowIn");
						} else {
							acc_nm = src;
							fun_nm = KString.from("getDFlowOut");
						}
					}
					else if (src.substr(idx+1) == KString.from("true")) {
						acc_nm = src.substr(0,idx);
						fun_nm = KString.from("getDFlowTru");
					}
					else if (src.substr(idx+1) == KString.from("false")) {
						acc_nm = src.substr(0,idx);
						fun_nm = KString.from("getDFlowFls");
					}
					else {
						Kiev.reportError(fld.pos,"Bad @dflow in(): "+src);
						continue;
					}
					cae_tru = new ASTCallAccessExpression();
					cae_tru.obj = new ASTIdentifier(0,acc_nm);
					cae_tru.func = new ASTIdentifier(0,fun_nm);
					
					if (acc_nm != nameThis) {
						acc_fld = new ASTAccessExpression();
						acc_fld.obj = new ThisExpr();
						acc_fld.ident = new ASTIdentifier(0,acc_nm);
						cae_fls = new ASTCallAccessExpression();
						cae_fls.obj = new ThisExpr();
						cae_fls.func = new ASTIdentifier(0,KString.from("getDFlowIn$"+acc_nm));
					}
				}
				MethodType mt = (MethodType)Type.fromSignature(signGetDFlowInFld);
				Method dfIn = new Method(s,fname,mt,ACC_PRIVATE);
				dfIn.params = new Var[]{
					new Var(0, dfIn, nameThis, s.type, 0),
				};
				dfIn.body = new BlockStat(0,dfIn);
				if (cae_fls != null) {
					((BlockStat)dfIn.body).addStatement(
						new IfElseStat(0,null,
							new BinaryBooleanExpr(0, BinaryOperator.NotEquals,
								acc_fld,
								new ConstExpr(0,null)
							),
							new ReturnStat(0,null,cae_tru),
							new ReturnStat(0,null,cae_fls)
						)
					);
				} else {
					((BlockStat)dfIn.body).addStatement(
						new ReturnStat(0,null,cae_tru)
					);
				}
				s.addMethod(dfIn);
			}
		}
	}

}
