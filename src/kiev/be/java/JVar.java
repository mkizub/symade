package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public class JVarView extends JLvalDNodeView {
	final Var.VarImpl impl;
	public JVarView(Var.VarImpl impl) {
		super(impl);
		this.impl = impl;
	}

	final Var getVVar() { return this.impl.getVar(); }
		
	@getter public final KString				get$name()			{ return this.impl.name.name; }
	@getter public final TypeRef				get$vtype()			{ return this.impl.vtype; }
	@getter public final ENode					get$init()			{ return this.impl.init; }
	@getter public final int					get$bcpos()			{ return this.impl.bcpos; }

	@setter public final void set$bcpos(int val)					{ this.impl.bcpos = val; }

	@getter public final Type get$type() {
		if (this.impl.vtype == null)
			return Type.tpVoid;
		return this.impl.vtype.getType();
	}
	
	public final boolean isLocalRuleVar() { return this.impl.is_var_local_rule_var; }
	public final boolean isClosureProxy() { return this.impl.is_var_closure_proxy; }
	public final boolean isVarThis() { return this.impl.is_var_this; }
	public final boolean isVarSuper() { return this.impl.is_var_super; }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Var declaration");
		//assert (parent instanceof BlockStat || parent instanceof ExprStat || parent instanceof ForInit);
		code.setLinePos(this);
		try {
			if( init != null ) {
				init.generate(code,this.type);
				code.addVar(getVVar());
				code.addInstr(Instr.op_store,this);
			} else {
				code.addVar(getVVar());
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

}

