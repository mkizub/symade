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
public final class ProcessDFlow extends TransfProcessor implements Constants {

	public static final KString mnNode				= KString.from("kiev.vlang.dflow"); 
	public static final KString nameNArr			= KString.from("kiev.vlang.NArr"); 
	public static final KString nameNode			= KString.from("kiev.vlang.ASTNode"); 
	public static final KString nameDFState		= KString.from("kiev.vlang.DFState"); 

	public static final KString nameGetDFlowIn		= KString.from("getDFlowIn"); 
	public static final KString signGetDFlowIn		= KString.from("(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"); 
	public static final KString signGetDFlowInFld	= KString.from("()Lkiev/vlang/DFState;"); 
	public static final KString signGetDFlowInSeq	= KString.from("(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"); 
	
	private static Type tpNArr;
	private static Type tpNode;
	private static Type tpDFState;
	
	private ProcessDFlow() {
		super(Kiev.Ext.DFlow);
	}

	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

	//////////////////////////////////////////////////////
	//   Generate class members (enumerate sub-nodes)   //
    //////////////////////////////////////////////////////

	private boolean hasField(Struct s, KString name) {
		s.checkResolved();
		foreach (Field f; s.members; f.id.equals(name)) return true;
		return false;
	}
	
	private boolean hasMethod(Struct s, KString name) {
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
	
	public void autoGenerateMembers(Struct:ASTNode s) {
		if (tpNArr == null)
			tpNArr = Env.getStruct(nameNArr).ctype;
		if (tpNArr == null) {
			Kiev.reportError("Cannot find class "+nameNArr);
			return;
		}
		if (tpNode == null)
			tpNode = Env.getStruct(nameNode).ctype;
		if (tpNode == null) {
			Kiev.reportError("Cannot find class "+nameNode);
			return;
		}
		if (tpDFState == null)
			tpDFState = Env.getStruct(nameDFState).ctype;
		if (tpDFState == null) {
			Kiev.reportError("Cannot find class "+nameDFState);
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
				foreach (Field f; ss.members; !f.isStatic() && f.meta.get(mnNode) != null) {
					aflds.insert(p, f);
					p++;
				}
				ss = ss.super_type.getStruct();
			}
		}
		// DFState getDFlowIn(ASTNode $child)
		if (hasMethod(s, nameGetDFlowIn)) {
			Kiev.reportWarning(s,"Method "+s+"."+nameGetDFlowIn+" already exists, @dflow member is not generated");
		} else {
			Method dfIn = new Method(nameGetDFlowIn,tpDFState,ACC_PUBLIC | ACC_SYNTHETIC);
			dfIn.params.add(new FormPar(0, KString.from("child"), tpNode, FormPar.PARAM_NORMAL, 0));
			dfIn.body = new Block(0);
			Var var = new Var(0, KString.from("name"),Type.tpString,ACC_FINAL);
			dfIn.body.stats.add(var);
			{
				AccessExpr ae0 = new AccessExpr(0, new LVarExpr(0,dfIn.params[0]), new SymbolRef(KString.from("pslot")));
				AccessExpr ae1 = new AccessExpr(0, ae0, new SymbolRef(KString.from("name")));
				var.init = ae1;
			}
			for(int i=0; i < aflds.length; i++) {
				Field fld = aflds[i];
				boolean isArr = fld.getType().isInstanceOf(tpNArr);
				boolean seq = isArr && fld.meta.get(mnNode).getZ(KString.from("seq"));
				KString fldnm = fld.id.sname;
				KString fname = KString.from(nameGetDFlowIn+"$"+fldnm);
				ASTCallExpression ce = new ASTCallExpression();
				ce.func = new SymbolRef(fname);
				if (seq)
					ce.args.add(new LVarExpr(0, dfIn.params[0]));
				dfIn.body.stats.add(
					new IfElseStat(0,
						new BinaryBoolExpr(0, BinaryOperator.Equals,
							new LVarExpr(0, var),
							new ConstStringExpr(fldnm)
						),
						new ReturnStat(0, ce),
						null
					)
				);
			}
			StringConcatExpr msg = new StringConcatExpr();
			msg.appendArg(new ConstStringExpr(KString.from("No @dflow value \"")));
			msg.appendArg(new LVarExpr(0, var));
			msg.appendArg(new ConstStringExpr(KString.from("\" in "+s.id)));
			dfIn.body.stats.add(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
			s.addMethod(dfIn);
		}
		// DFState getDFlowIn$xxx()
		for(int i=0; i < aflds.length; i++) {
			Field fld = aflds[i];
			boolean isArr = fld.getType().isInstanceOf(tpNArr);
			KString fldnm = fld.id.sname;
			KString fname = KString.from(nameGetDFlowIn+"$"+fldnm);
			Meta meta = fld.meta.get(mnNode);
			KString src = meta.getS(KString.from("in"));
			boolean seq = isArr && meta.getZ(KString.from("seq"));
			if (hasMethod(s, fname)) {
				Kiev.reportWarning(s,"Method "+s+"."+fname+" already exists, @dflow member is not generated");
			} else {
				AccessExpr acc_fld = null;
				AccessExpr acc_prev = null;
				ASTCallAccessExpression cae_prev = null;
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
						Kiev.reportError(fld,"Bad @dflow in(): "+src);
						continue;
					}
					cae_tru = new ASTCallAccessExpression();
					cae_tru.obj = new ASTIdentifier(acc_nm);
					cae_tru.func = new SymbolRef(fun_nm);
					
					if (seq) {
						acc_prev = new AccessExpr();
						acc_prev.obj = new ASTIdentifier(KString.from("$child"));
						acc_prev.ident = new SymbolRef(KString.from("pprev"));
						cae_prev = new ASTCallAccessExpression();
						cae_prev.obj = acc_prev.ncopy();
						cae_prev.func = new SymbolRef(KString.from("getDFlowOut"));
					}
					
					if (acc_nm != nameThis) {
						acc_fld = new AccessExpr();
						acc_fld.obj = new ThisExpr();
						acc_fld.ident = new SymbolRef(acc_nm);
						cae_fls = new ASTCallAccessExpression();
						cae_fls.obj = new ThisExpr();
						cae_fls.func = new SymbolRef(KString.from("getDFlowIn$"+acc_nm));
					}
				}
				Method dfIn;
				if (seq) {
					dfIn = new Method(fname,tpDFState,ACC_PRIVATE | ACC_SYNTHETIC);
					dfIn.params.add(new FormPar(0, KString.from("$child"), tpNode, FormPar.PARAM_NORMAL, 0));
				} else {
					dfIn = new Method(fname,tpDFState,ACC_PRIVATE | ACC_SYNTHETIC);
				}
				dfIn.body = new Block(0);
				if (isArr && seq) {
					dfIn.body.stats.add(
						new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								acc_prev,
								new ConstNullExpr()
							),
							new ReturnStat(0,cae_prev),
							null
						)
					);
				}
				if (cae_fls != null) {
					dfIn.body.stats.add(
						new IfElseStat(0,
							new BinaryBoolExpr(0, BinaryOperator.NotEquals,
								acc_fld,
								new ConstNullExpr()
							),
							new ReturnStat(0,cae_tru),
							new ReturnStat(0,cae_fls)
						)
					);
				} else {
					dfIn.body.stats.add(
						new ReturnStat(0,cae_tru)
					);
				}
				s.addMethod(dfIn);
			}
		}
	}

}
