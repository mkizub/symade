package kiev.be.java15;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JVar of Var extends JLvalDNode {
	
	public:ro	Symbol			id;
	public:ro	Type			vtype;
	public:ro	JType			jtype;
	public:ro	JENode			init;
	public		int				bcpos;

	@getter public final Type get$type() {
		if (((Var)this).vtype == null)
			return Type.tpVoid;
		return ((Var)this).vtype.getType();
	}
	@getter public final JType get$jtype() {
		return this.get$type().getJType();
	}
	
	public final boolean isLocalRuleVar();
	public final boolean isClosureProxy();
	public final boolean isVarThis();
	public final boolean isVarSuper();

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Var declaration");
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

