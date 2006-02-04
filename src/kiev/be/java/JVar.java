package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;

import kiev.vlang.Var.VarImpl;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JVarView of VarImpl extends JLvalDNodeView {
	
	public final Var getVar() { return (Var)this.getNode(); }
		
	public access:ro	KString				name;
	public access:ro	Type				vtype;
	public access:ro	JType				jtype;
	public access:ro	JENodeView			init;
	public				int					bcpos;

	@getter public final Type get$type() {
		if (((VarImpl)this.$view).vtype == null)
			return Type.tpVoid;
		return ((VarImpl)this.$view).vtype.getType();
	}
	@getter public final JType get$jtype() {
		return this.get$type().getJType();
	}
	
	public final boolean isLocalRuleVar()		{ return ((VarImpl)this.$view).is_var_local_rule_var; }
	public final boolean isClosureProxy()		{ return ((VarImpl)this.$view).is_var_closure_proxy; }
	public final boolean isVarThis()			{ return ((VarImpl)this.$view).is_var_this; }
	public final boolean isVarSuper()			{ return ((VarImpl)this.$view).is_var_super; }

	public void set$bcpos(int pos) {
		if( pos < 0 || pos > 255)
			throw new RuntimeException("Bad bytecode position specified: "+pos);
		((VarImpl)this.$view).bcpos = pos;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Var declaration");
		//assert (parent instanceof BlockStat || parent instanceof ExprStat || parent instanceof ForInit);
		code.setLinePos(this);
		try {
			if( init != null ) {
				init.generate(code,this.type);
				code.addVar(this);
				code.addInstr(Instr.op_store,this);
			} else {
				code.addVar(this);
			}
		} catch(Exception e ) {
			Kiev.reportError(this,e);
		}
	}

}

