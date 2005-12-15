package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

import kiev.vlang.Var.VarImpl;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JVarView of VarImpl extends JLvalDNodeView {
	final Var getVar() { return this.$view.getVar(); }
		
	public access:ro	KString				name;
	public access:ro	TypeRef				vtype;
	public access:ro	ENode				init;
	public				int					bcpos;

	@getter public final Type get$type() {
		if (this.$view.vtype == null)
			return Type.tpVoid;
		return this.$view.vtype.getType();
	}
	
	public final boolean isLocalRuleVar()		{ return this.$view.is_var_local_rule_var; }
	public final boolean isClosureProxy()		{ return this.$view.is_var_closure_proxy; }
	public final boolean isVarThis()			{ return this.$view.is_var_this; }
	public final boolean isVarSuper()			{ return this.$view.is_var_super; }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Var declaration");
		//assert (parent instanceof BlockStat || parent instanceof ExprStat || parent instanceof ForInit);
		code.setLinePos(this);
		try {
			if( init != null ) {
				init.generate(code,this.type);
				code.addVar(getVar());
				code.addInstr(Instr.op_store,this);
			} else {
				code.addVar(getVar());
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

}

