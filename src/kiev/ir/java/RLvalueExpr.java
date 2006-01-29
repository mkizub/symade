package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.LVarExpr.LVarExprImpl;
import kiev.vlang.LVarExpr.LVarExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 359 $
 *
 */

@nodeview
public final view RLVarExpr of LVarExprImpl extends LVarExprView {

	static final KString namePEnv = KString.from("$env");

	public boolean preGenerate() {
		if (getVar().isLocalRuleVar()) {
			RuleMethod rm = (RuleMethod)ctx_method;
			assert(rm.params[0].type ≡ Type.tpRule);
			Var pEnv = null;
			foreach (ENode n; rm.body.stats; n instanceof VarDecl) {
				VarDecl vd = (VarDecl)n;
				if (vd.var.name.equals(namePEnv)) {
					assert(vd.var.type.isInstanceOf(Type.tpRule));
					pEnv = vd.var;
					break;
				}
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnv);
				return false;
			}
			Struct s = ((LocalStructDecl)((BlockStat)rm.body).stats[0]).clazz;
			Field f = s.resolveField(ident.name);
			replaceWithNode(new IFldExpr(pos, new LVarExpr(pos, pEnv), ~ident, f));
		}
		return true;
	}
	
}

