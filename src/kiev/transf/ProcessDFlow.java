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

	public static final String mnNode				= "kiev.vlang.dflow"; 
	public static final String nameNArr			= "kiev.vlang.NArr"; 
	public static final String nameNode			= "kiev.vlang.ASTNode"; 
	public static final String nameDFState			= "kiev.vlang.DFState"; 

	public static final String nameGetDFlowIn		= "getDFlowIn";
	public static final String signGetDFlowIn		= "(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"; 
	public static final String signGetDFlowInFld	= "()Lkiev/vlang/DFState;"; 
	public static final String signGetDFlowInSeq	= "(Lkiev/vlang/ASTNode;)Lkiev/vlang/DFState;"; 
	
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
	
	public void autoGenerateMembers(Struct:ASTNode s) {
		if (tpNArr == null)
			tpNArr = Env.loadStruct(nameNArr).xtype;
		if (tpNArr == null) {
			Kiev.reportError("Cannot find class "+nameNArr);
			return;
		}
		if (tpNode == null)
			tpNode = Env.loadStruct(nameNode).xtype;
		if (tpNode == null) {
			Kiev.reportError("Cannot find class "+nameNode);
			return;
		}
		if (tpDFState == null)
			tpDFState = Env.loadStruct(nameDFState).xtype;
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
			while (ss.meta.get(mnNode) != null) {
				int p = 0;
				foreach (Field f; ss.members; !f.isStatic() && f.meta.get(mnNode) != null) {
					aflds.insert(p, f);
					p++;
				}
				if (ss.super_types.length > 0)
					ss = ss.super_types[0].getStruct();
				else
					break;
			}
		}
		// DFState getDFlowIn(ASTNode $child)
		if (hasMethod(s, nameGetDFlowIn)) {
			Kiev.reportWarning(s,"Method "+s+"."+nameGetDFlowIn+" already exists, @dflow member is not generated");
		} else {
			Method dfIn = new Method(nameGetDFlowIn,tpDFState,ACC_PUBLIC | ACC_SYNTHETIC);
			dfIn.params.add(new FormPar(0, "child", tpNode, FormPar.PARAM_NORMAL, 0));
			dfIn.body = new Block(0);
			Var var = new Var(0, "name",Type.tpString,ACC_FINAL);
			dfIn.block.stats.add(var);
			{
				AccessExpr ae0 = new AccessExpr(0, new LVarExpr(0,dfIn.params[0]), new SymbolRef("pslot"));
				AccessExpr ae1 = new AccessExpr(0, ae0, new SymbolRef("name"));
				var.init = ae1;
			}
			for(int i=0; i < aflds.length; i++) {
				Field fld = aflds[i];
				boolean isArr = fld.getType().isInstanceOf(tpNArr);
				boolean seq = isArr && fld.meta.get(mnNode).getZ("seq");
				String fldnm = fld.id.sname;
				String fname = (nameGetDFlowIn+"$"+fldnm).intern();
				ASTCallExpression ce = new ASTCallExpression();
				ce.ident = new SymbolRef(fname);
				if (seq)
					ce.args.add(new LVarExpr(0, dfIn.params[0]));
				dfIn.block.stats.add(
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
			msg.appendArg(new ConstStringExpr("No @dflow value \""));
			msg.appendArg(new LVarExpr(0, var));
			msg.appendArg(new ConstStringExpr("\" in "+s.id));
			dfIn.block.stats.add(
				new ThrowStat(0,new NewExpr(0,Type.tpRuntimeException,new ENode[]{msg}))
			);
			s.addMethod(dfIn);
		}
		// DFState getDFlowIn$xxx()
		for(int i=0; i < aflds.length; i++) {
			Field fld = aflds[i];
			boolean isArr = fld.getType().isInstanceOf(tpNArr);
			String fldnm = fld.id.sname;
			String fname = (nameGetDFlowIn+"$"+fldnm).intern();
			Meta meta = fld.meta.get(mnNode);
			String src = meta.getS("in").toString();
			boolean seq = isArr && meta.getZ("seq");
			if (hasMethod(s, fname)) {
				Kiev.reportWarning(s,"Method "+s+"."+fname+" already exists, @dflow member is not generated");
			} else {
				AccessExpr acc_fld = null;
				AccessExpr acc_prev = null;
				ASTCallAccessExpression cae_prev = null;
				ASTCallAccessExpression cae_tru = null;
				ASTCallAccessExpression cae_fls = null;
				{
					String acc_nm;
					String fun_nm;
					int idx = src.indexOf(':');
					if (idx < 0) {
						if (src.length() == 0) {
							acc_nm = nameThis;
							fun_nm = "getDFlowIn";
						} else {
							acc_nm = src;
							fun_nm = "getDFlowOut";
						}
					}
					else if (src.substring(idx+1).equals("true")) {
						acc_nm = src.substring(0,idx);
						fun_nm = "getDFlowTru";
					}
					else if (src.substring(idx+1).equals("false")) {
						acc_nm = src.substring(0,idx);
						fun_nm = "getDFlowFls";
					}
					else {
						Kiev.reportError(fld,"Bad @dflow in(): "+src);
						continue;
					}
					cae_tru = new ASTCallAccessExpression();
					cae_tru.obj = new ASTIdentifier(acc_nm);
					cae_tru.ident = new SymbolRef(fun_nm);
					
					if (seq) {
						acc_prev = new AccessExpr();
						acc_prev.obj = new ASTIdentifier("$child");
						acc_prev.ident = new SymbolRef("pprev");
						cae_prev = new ASTCallAccessExpression();
						cae_prev.obj = acc_prev.ncopy();
						cae_prev.ident = new SymbolRef("getDFlowOut");
					}
					
					if (acc_nm != nameThis) {
						acc_fld = new AccessExpr();
						acc_fld.obj = new ThisExpr();
						acc_fld.ident = new SymbolRef(acc_nm);
						cae_fls = new ASTCallAccessExpression();
						cae_fls.obj = new ThisExpr();
						cae_fls.ident = new SymbolRef("getDFlowIn$"+acc_nm);
					}
				}
				Method dfIn;
				if (seq) {
					dfIn = new Method(fname,tpDFState,ACC_PRIVATE | ACC_SYNTHETIC);
					dfIn.params.add(new FormPar(0, "$child", tpNode, FormPar.PARAM_NORMAL, 0));
				} else {
					dfIn = new Method(fname,tpDFState,ACC_PRIVATE | ACC_SYNTHETIC);
				}
				dfIn.body = new Block(0);
				if (isArr && seq) {
					dfIn.block.stats.add(
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
					dfIn.block.stats.add(
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
					dfIn.block.stats.add(
						new ReturnStat(0,cae_tru)
					);
				}
				s.addMethod(dfIn);
			}
		}
	}

}
