package kiev.be.java;

import kiev.Kiev;
import kiev.CError;

import kiev.vlang.*;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.BoolExpr.BoolExprImpl;
import kiev.vlang.BinaryBooleanOrExpr.BinaryBooleanOrExprImpl;
import kiev.vlang.BinaryBooleanAndExpr.BinaryBooleanAndExprImpl;
import kiev.vlang.BinaryBoolExpr.BinaryBoolExprImpl;
import kiev.vlang.InstanceofExpr.InstanceofExprImpl;
import kiev.vlang.BooleanNotExpr.BooleanNotExprImpl;

public interface IBoolExpr {
	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);
}

@nodeview
public abstract view JBoolExprView of BoolExprImpl extends JENodeView implements IBoolExpr {
	public JBoolExprView(BoolExprImpl $view) {
		super($view);
		this.$view = $view;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr: "+this);
		code.setLinePos(this);
		CodeLabel label_true = code.newLabel();
		CodeLabel label_false = code.newLabel();

		generate_iftrue(code,label_true);
		code.addConst(0);
		code.addInstr(Instr.op_goto,label_false);
		code.addInstr(Instr.set_label,label_true);
		code.addConst(1);
		code.addInstr(Instr.set_label,label_false);
		if( reqType ≡ Type.tpVoid ) code.addInstr(Instr.op_pop);
	}

	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);

	public static void gen_iftrue(Code code, JENodeView expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			expr.generate_iftrue(code,label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if true): "+expr);
		code.setLinePos(expr);
		if( expr.getType().isBoolean() ) {
			boolean optimized = false;
			if( expr instanceof JBinaryExprView ) {
				JBinaryExprView be = (JBinaryExprView)expr;
				if( be.expr2.getType().isIntegerInCode() && be.expr2.isConstantExpr() ) {
					Object ce = be.expr2.getConstValue();
					if( ((Number)ce).intValue() == 0 ) {
						optimized = true;
						if( be.op == BinaryOperator.LessThen ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifge,label);
						}
						else if( be.op == BinaryOperator.LessEquals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifgt,label);
						}
						else if( be.op == BinaryOperator.GreaterThen ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifle,label);
						}
						else if( be.op == BinaryOperator.GreaterEquals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_iflt,label);
						}
						else if( be.op == BinaryOperator.Equals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifne,label);
						}
						else if( be.op == BinaryOperator.NotEquals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifeq,label);
						}
						else {
							optimized = false;
						}
					}
				}
			}
			if( !optimized ) {
				expr.generate(code,Type.tpBoolean);
				code.addInstr(Instr.op_ifne,label);
			}
		}
		else
			throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
	}

	public static void gen_iffalse(Code code, JENodeView expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			expr.generate_iffalse(code, label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if false): "+expr);
		code.setLinePos(expr);
		if( expr.getType().isBoolean() ) {
			expr.generate(code,Type.tpBoolean);
			code.addInstr(Instr.op_ifeq,label);
		}
		else
			throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
	}
}


@nodeview
public final view JBinaryBooleanOrExprView of BinaryBooleanOrExprImpl extends JBoolExprView {
	public access:ro JENodeView		expr1;
	public access:ro JENodeView		expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		JBoolExprView.gen_iftrue(code, expr1, label);
		JBoolExprView.gen_iftrue(code, expr2, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		JBoolExprView.gen_iftrue(code, expr1, label1);
		JBoolExprView.gen_iffalse(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
	}
}

@nodeview
public final view JBinaryBooleanAndExprView of BinaryBooleanAndExprImpl extends JBoolExprView {
	public access:ro JENodeView		expr1;
	public access:ro JENodeView		expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		JBoolExprView.gen_iffalse(code, expr1, label1);
		JBoolExprView.gen_iftrue(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		JBoolExprView.gen_iffalse(code, expr1, label);
		JBoolExprView.gen_iffalse(code, expr2, label);
	}
}

@nodeview
public final view JBinaryBoolExprView of BinaryBoolExprImpl extends JBoolExprView {
	public access:ro BinaryOperator		op;
	public access:ro JENodeView			expr1;
	public access:ro JENodeView			expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr (if true): "+this);
		code.setLinePos(this);
		if( expr2 instanceof JConstExprView ) {
			JConstExprView ce = (JConstExprView)expr2;
			Object cv = ce.getConstValue();
			if( cv == null ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == BinaryOperator.Equals) code.addInstr(Instr.op_ifnull,label);
				else if( op == BinaryOperator.NotEquals ) code.addInstr(Instr.op_ifnonnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == BinaryOperator.Equals ) {
					code.addInstr(Instr.op_ifeq,label);
					return;
				}
				else if( op == BinaryOperator.NotEquals ) {
					code.addInstr(Instr.op_ifne,label);
					return;
				}
				else if( op == BinaryOperator.LessThen ) {
					code.addInstr(Instr.op_iflt,label);
					return;
				}
				else if( op == BinaryOperator.LessEquals ) {
					code.addInstr(Instr.op_ifle,label);
					return;
				}
				else if( op == BinaryOperator.GreaterThen ) {
					code.addInstr(Instr.op_ifgt,label);
					return;
				}
				else if( op == BinaryOperator.GreaterEquals ) {
					code.addInstr(Instr.op_ifge,label);
					return;
				}
			}
		}
		expr1.generate(code,Type.tpBoolean);
		expr2.generate(code,Type.tpBoolean);
		if( op == BinaryOperator.Equals )				code.addInstr(Instr.op_ifcmpeq,label);
		else if( op == BinaryOperator.NotEquals )		code.addInstr(Instr.op_ifcmpne,label);
		else if( op == BinaryOperator.LessThen )		code.addInstr(Instr.op_ifcmplt,label);
		else if( op == BinaryOperator.LessEquals )		code.addInstr(Instr.op_ifcmple,label);
		else if( op == BinaryOperator.GreaterThen )	code.addInstr(Instr.op_ifcmpgt,label);
		else if( op == BinaryOperator.GreaterEquals )	code.addInstr(Instr.op_ifcmpge,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr (if false): "+this);
		code.setLinePos(this);
		if( expr2 instanceof JConstExprView ) {
			JConstExprView ce = (JConstExprView)expr2;
			Object cv = ce.getConstValue();
			if( cv == null ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == BinaryOperator.Equals) code.addInstr(Instr.op_ifnonnull,label);
				else if( op == BinaryOperator.NotEquals ) code.addInstr(Instr.op_ifnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == BinaryOperator.Equals ) {
					code.addInstr(Instr.op_ifne,label);
					return;
				}
				else if( op == BinaryOperator.NotEquals ) {
					code.addInstr(Instr.op_ifeq,label);
					return;
				}
				else if( op == BinaryOperator.LessThen ) {
					code.addInstr(Instr.op_ifge,label);
					return;
				}
				else if( op == BinaryOperator.LessEquals ) {
					code.addInstr(Instr.op_ifgt,label);
					return;
				}
				else if( op == BinaryOperator.GreaterThen ) {
					code.addInstr(Instr.op_ifle,label);
					return;
				}
				else if( op == BinaryOperator.GreaterEquals ) {
					code.addInstr(Instr.op_iflt,label);
					return;
				}
			}
		}
		expr1.generate(code,Type.tpBoolean);
		expr2.generate(code,Type.tpBoolean);
		if( op == BinaryOperator.Equals )				code.addInstr(Instr.op_ifcmpne,label);
		else if( op == BinaryOperator.NotEquals )		code.addInstr(Instr.op_ifcmpeq,label);
		else if( op == BinaryOperator.LessThen )		code.addInstr(Instr.op_ifcmpge,label);
		else if( op == BinaryOperator.LessEquals )		code.addInstr(Instr.op_ifcmpgt,label);
		else if( op == BinaryOperator.GreaterThen )	code.addInstr(Instr.op_ifcmple,label);
		else if( op == BinaryOperator.GreaterEquals )	code.addInstr(Instr.op_ifcmplt,label);
	}
}

@nodeview
public final view JInstanceofExprView of InstanceofExprImpl extends JBoolExprView {
	public access:ro JENodeView		expr;
	public access:ro Type			type;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,type);
		code.addInstr(Instr.op_ifne,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,type);
		code.addInstr(Instr.op_ifeq,label);
	}
}

@nodeview
public final view JBooleanNotExprView of BooleanNotExprImpl extends JBoolExprView {
	public access:ro JENodeView		expr;
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		code.setLinePos(this);
		JBoolExprView.gen_iffalse(code, expr, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		code.setLinePos(this);
		JBoolExprView.gen_iftrue(code, expr, label);
	}

}

