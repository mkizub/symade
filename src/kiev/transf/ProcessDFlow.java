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

public final class ProcessDFlow extends TransfProcessor implements Constants {

	public static final KString mnNode				= KString.from("kiev.vlang.dflow"); 
	public static final KString nameNArr			= KString.from("kiev.vlang.NArr"); 
	public static final KString nameNode			= KString.from("kiev.vlang.ASTNode"); 

	public static final KString nameGetDFlowIn		= KString.from("getDFlowIn"); 
	public static final KString signGetDFlowIn		= KString.from("(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"); 
	public static final KString signGetDFlowInFld	= KString.from("()Lkiev/vlang/DFState;"); 
	
	private static Type tpNArr;
	private static Type tpNode;
	
	public ProcessDFlow(Kiev.Ext ext) {
		super(ext);
	}

	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

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
	
	public void autoGenerateMembers(Struct:ASTNode s) {
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
				foreach (ASTNode n; ss.members; n instanceof Field && !n.isStatic() && ((Field)n).meta.get(mnNode) != null) {
					Field f = (Field)n;
					aflds.insert(p, f);
					p++;
				}
				ss = ss.super_type.getStruct();
			}
		}
		// DFState getDFlowIn(ASTNode child)
		if (hasMethod(s, nameGetDFlowIn)) {
			Kiev.reportWarning(s.pos,"Method "+s+"."+nameGetDFlowIn+" already exists, @dflow member is not generated");
		} else {
			MethodType mt = (MethodType)Type.fromSignature(signGetDFlowIn);
			Method dfIn = new Method(nameGetDFlowIn,mt,ACC_PUBLIC);
			dfIn.params.add(new FormPar(0, KString.from("child"), tpNode, 0));
			dfIn.body = new BlockStat(0,dfIn);
			Var var = new Var(0, KString.from("name"),Type.tpString,ACC_FINAL);
			dfIn.body.addStatement(new VarDecl(var));
			{
				ASTAccessExpression ae0 = new ASTAccessExpression();
				ae0.obj = new VarAccessExpr(0,dfIn.params[0]);
				ae0.ident = new ASTIdentifier(KString.from("pslot"));
				ASTAccessExpression ae1 = new ASTAccessExpression();
				ae1.obj = ae0;
				ae1.ident = new ASTIdentifier(KString.from("name"));
				var.init = ae1;
			}
			for(int i=0; i < aflds.length; i++) {
				KString fldnm = aflds[i].name.name;
				KString fname = KString.from(nameGetDFlowIn+"$"+fldnm);
				ASTCallExpression ce = new ASTCallExpression();
				ce.func = new ASTIdentifier(fname);
				dfIn.body.addStatement(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new VarAccessExpr(0, var),
							new ConstStringExpr(fldnm)
						),
						new ReturnStat(0,null, ce),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @dflow value \"")));
			msg.appendArg(new VarAccessExpr(0, var));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.name.short_name)));
			dfIn.body.addStatement(
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
					cae_tru.obj = new ASTIdentifier(acc_nm);
					cae_tru.func = new ASTIdentifier(fun_nm);
					
					if (acc_nm != nameThis) {
						cae_fls = new ASTCallAccessExpression();
						cae_fls.obj = new ThisExpr();
						cae_fls.func = new ASTIdentifier(KString.from("getDFlowIn$"+acc_nm));
					}
				}
				MethodType mt = (MethodType)Type.fromSignature(signGetDFlowInFld);
				Method dfIn = new Method(fname,mt,ACC_PRIVATE);
				dfIn.body = new BlockStat(0,dfIn);
				if (cae_fls != null) {
					dfIn.body.addStatement(
						new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								new AccessExpr(0,new ThisExpr(), fld),
								new ConstNullExpr()
							),
							new ReturnStat(0,null,cae_tru),
							new ReturnStat(0,null,cae_fls)
						)
					);
				} else {
					dfIn.body.addStatement(
						new ReturnStat(0,null,cae_tru)
					);
				}
				s.addMethod(dfIn);
			}
		}
	}

}
