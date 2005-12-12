package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.vlang.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public class JAssignExprView extends JLvalueExprView {
	final AssignExpr.AssignExprImpl impl;
	public JAssignExprView(AssignExpr.AssignExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final AssignOperator	get$op()					{ return this.impl.op; }
	@getter public final JLvalueExprView	get$lval()					{ return ((LvalueExpr)this.impl.lval).getJLvalueExprView(); }
	@getter public final JENodeView			get$value()					{ return this.impl.value.getJENodeView(); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		code.setLinePos(this);
		if( reqType != Type.tpVoid ) {
			if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) ) {
				lval.generateLoadDup(code);
				value.generate(code,null);
				code.addInstr(op.instr);
				lval.generateStoreDupValue(code);
			} else {
				lval.generateAccess(code);
				value.generate(code,null);
				lval.generateStoreDupValue(code);
			}
		} else {
			if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) ) {
				lval.generateLoadDup(code);
				value.generate(code,null);
				code.addInstr(op.instr);
				lval.generateStore(code);
			} else {
				lval.generateAccess(code);
				value.generate(code,null);
				lval.generateStore(code);
			}
		}
	}

	/** Just load value referenced by lvalue */
	public void generateLoad(Code code) {
		code.setLinePos(this);
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStoreDupValue(code);
	}

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while)
	*/
	public void generateLoadDup(Code code) {
		throw new RuntimeException("Too complex lvalue expression "+this);
	}

	public void generateAccess(Code code) {
		throw new RuntimeException("Too complex lvalue expression "+this);
	}

	/** Stores value using previously duped info */
	public void generateStore(Code code) {
		code.setLinePos(this);
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStore(code);
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue(Code code) {
		code.setLinePos(this);
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStoreDupValue(code);
	}

}

@nodeview
public class JIncrementExprView extends JENodeView {
	final IncrementExpr.IncrementExprImpl impl;
	public JIncrementExprView(IncrementExpr.IncrementExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final Operator			get$op()					{ return this.impl.op; }
	@getter public final JLvalueExprView	get$lval()					{ return ((LvalueExpr)this.impl.lval).getJLvalueExprView(); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		code.setLinePos(this);
		JLvalueExprView lval = this.lval;
		if( reqType != Type.tpVoid ) {
			generateLoad(code);
		} else {
			if( lval instanceof JLVarExprView ) {
				JLVarExprView va = (JLVarExprView)lval;
				if( va.getType().isIntegerInCode() && !va.var.isNeedProxy() || va.isUseNoProxy() ) {
					if( op==PrefixOperator.PreIncr || op==PostfixOperator.PostIncr ) {
						code.addInstrIncr(va.var,1);
						return;
					}
					else if( op==PrefixOperator.PreDecr || op==PostfixOperator.PostDecr ) {
						code.addInstrIncr(va.var,-1);
						return;
					}
				}
			}
			lval.generateLoadDup(code);

			if( op == PrefixOperator.PreIncr ) {
				pushProperConstant(code,1);
				code.addInstr(op_add);
				lval.generateStore(code);
			}
			else if( op == PrefixOperator.PreDecr ) {
				pushProperConstant(code,-1);
				code.addInstr(op_add);
				lval.generateStore(code);
			}
			else if( op == PostfixOperator.PostIncr ) {
				pushProperConstant(code,1);
				code.addInstr(op_add);
				lval.generateStore(code);
			}
			else if( op == PostfixOperator.PostDecr ) {
				pushProperConstant(code,-1);
				code.addInstr(op_add);
				lval.generateStore(code);
			}
		}
	}

	/** Just load value referenced by lvalue */
	private void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: - load "+this);
		code.setLinePos(this);
		if( lval instanceof JLVarExprView ) {
			JLVarExprView va = (JLVarExprView)lval;
			if( va.getType().isIntegerInCode() && !va.var.isNeedProxy() || va.isUseNoProxy() ) {
				if( op == PrefixOperator.PreIncr ) {
					code.addInstrIncr(va.var,1);
					code.addInstr(op_load,va.var);
					return;
				}
				else if( op == PostfixOperator.PostIncr ) {
					code.addInstr(op_load,va.var);
					code.addInstrIncr(va.var,1);
					return;
				}
				else if( op == PrefixOperator.PreDecr ) {
					code.addInstrIncr(va.var,-1);
					code.addInstr(op_load,va.var);
					return;
				}
				else if( op == PostfixOperator.PostDecr ) {
					code.addInstr(op_load,va.var);
					code.addInstrIncr(va.var,-1);
					return;
				}
			}
		}
		lval.generateLoadDup(code);
		if( op == PrefixOperator.PreIncr ) {
			pushProperConstant(code,1);
			code.addInstr(op_add);
			lval.generateStoreDupValue(code);
		}
		else if( op == PrefixOperator.PreDecr ) {
			pushProperConstant(code,-1);
			code.addInstr(op_add);
			lval.generateStoreDupValue(code);
		}
		else if( op == PostfixOperator.PostIncr ) {
			pushProperConstant(code,1);
			code.addInstr(op_add);
			lval.generateStoreDupValue(code);
			pushProperConstant(code,-1);
			code.addInstr(op_add);
		}
		else if( op == PostfixOperator.PostDecr ) {
			pushProperConstant(code,-1);
			code.addInstr(op_add);
			lval.generateStoreDupValue(code);
			pushProperConstant(code,1);
			code.addInstr(op_add);
		}
	}

	private void pushProperConstant(Code code, int i) {
		Type lt = lval.getType();
		if( i > 0 ) { // 1
			if( lt == Type.tpDouble ) code.addConst(1.D);
			else if( lt == Type.tpFloat ) code.addConst(1.F);
			else if( lt == Type.tpLong ) code.addConst(1L);
			else code.addConst(1);
		} else { // -1
			if( lt == Type.tpDouble ) code.addConst(-1.D);
			else if( lt == Type.tpFloat ) code.addConst(-1.F);
			else if( lt == Type.tpLong ) code.addConst(-1L);
			else code.addConst(-1);
		}
	}

}

