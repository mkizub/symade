/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.be.java15;

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

interface IBoolExpr {
	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);
}

@ViewOf(vcast=true, iface=true)
public abstract view JBoolExpr of BoolExpr extends JENode implements IBoolExpr {

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BoolExpr: "+this);
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

	public static void gen_iftrue(Code code, JENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			expr.generate_iftrue(code,label);
			return;
		}
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if true): "+expr);
		code.setLinePos(expr);
		if( expr.getType().isBoolean() ) {
			boolean optimized = false;
			if( expr instanceof JBinaryExpr ) {
				JBinaryExpr be = (JBinaryExpr)expr;
				if( be.expr2.getType().isIntegerInCode() && be.expr2.isConstantExpr() ) {
					Object ce = be.expr2.getConstValue();
					if( ((Number)ce).intValue() == 0 ) {
						optimized = true;
						if( be.op == Operator.LessThen ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifge,label);
						}
						else if( be.op == Operator.LessEquals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifgt,label);
						}
						else if( be.op == Operator.GreaterThen ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifle,label);
						}
						else if( be.op == Operator.GreaterEquals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_iflt,label);
						}
						else if( be.op == Operator.Equals ) {
							be.expr1.generate(code,null);
							code.addInstr(Instr.op_ifne,label);
						}
						else if( be.op == Operator.NotEquals ) {
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

	public static void gen_iffalse(Code code, JENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			expr.generate_iffalse(code, label);
			return;
		}
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if false): "+expr);
		code.setLinePos(expr);
		if( expr.getType().isBoolean() ) {
			expr.generate(code,Type.tpBoolean);
			code.addInstr(Instr.op_ifeq,label);
		}
		else
			throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JBinaryBooleanOrExpr of BinaryBooleanOrExpr extends JBoolExpr {
	public:ro JENode		expr1;
	public:ro JENode		expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iftrue(code, expr1, label);
		JBoolExpr.gen_iftrue(code, expr2, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		JBoolExpr.gen_iftrue(code, expr1, label1);
		JBoolExpr.gen_iffalse(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JBinaryBooleanAndExpr of BinaryBooleanAndExpr extends JBoolExpr {
	public:ro JENode		expr1;
	public:ro JENode		expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		JBoolExpr.gen_iffalse(code, expr1, label1);
		JBoolExpr.gen_iftrue(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iffalse(code, expr1, label);
		JBoolExpr.gen_iffalse(code, expr2, label);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JBinaryBoolExpr of BinaryBoolExpr extends JBoolExpr {
	public:ro Operator			op;
	public:ro JENode			expr1;
	public:ro JENode			expr2;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BoolExpr (if true): "+this);
		code.setLinePos(this);
		if( expr2 instanceof JConstExpr ) {
			JConstExpr ce = (JConstExpr)expr2;
			Object cv = ce.getConstValue();
			if( cv == null ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == Operator.Equals) code.addInstr(Instr.op_ifnull,label);
				else if( op == Operator.NotEquals ) code.addInstr(Instr.op_ifnonnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == Operator.Equals ) {
					code.addInstr(Instr.op_ifeq,label);
					return;
				}
				else if( op == Operator.NotEquals ) {
					code.addInstr(Instr.op_ifne,label);
					return;
				}
				else if( op == Operator.LessThen ) {
					code.addInstr(Instr.op_iflt,label);
					return;
				}
				else if( op == Operator.LessEquals ) {
					code.addInstr(Instr.op_ifle,label);
					return;
				}
				else if( op == Operator.GreaterThen ) {
					code.addInstr(Instr.op_ifgt,label);
					return;
				}
				else if( op == Operator.GreaterEquals ) {
					code.addInstr(Instr.op_ifge,label);
					return;
				}
			}
		}
		expr1.generate(code,Type.tpBoolean);
		expr2.generate(code,Type.tpBoolean);
		if     ( op == Operator.Equals )			code.addInstr(Instr.op_ifcmpeq,label);
		else if( op == Operator.NotEquals )		code.addInstr(Instr.op_ifcmpne,label);
		else if( op == Operator.LessThen )			code.addInstr(Instr.op_ifcmplt,label);
		else if( op == Operator.LessEquals )		code.addInstr(Instr.op_ifcmple,label);
		else if( op == Operator.GreaterThen )		code.addInstr(Instr.op_ifcmpgt,label);
		else if( op == Operator.GreaterEquals )	code.addInstr(Instr.op_ifcmpge,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BoolExpr (if false): "+this);
		code.setLinePos(this);
		if( expr2 instanceof JConstExpr ) {
			JConstExpr ce = (JConstExpr)expr2;
			Object cv = ce.getConstValue();
			if( cv == null ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == Operator.Equals) code.addInstr(Instr.op_ifnonnull,label);
				else if( op == Operator.NotEquals ) code.addInstr(Instr.op_ifnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,Type.tpBoolean);
				if( op == Operator.Equals ) {
					code.addInstr(Instr.op_ifne,label);
					return;
				}
				else if( op == Operator.NotEquals ) {
					code.addInstr(Instr.op_ifeq,label);
					return;
				}
				else if( op == Operator.LessThen ) {
					code.addInstr(Instr.op_ifge,label);
					return;
				}
				else if( op == Operator.LessEquals ) {
					code.addInstr(Instr.op_ifgt,label);
					return;
				}
				else if( op == Operator.GreaterThen ) {
					code.addInstr(Instr.op_ifle,label);
					return;
				}
				else if( op == Operator.GreaterEquals ) {
					code.addInstr(Instr.op_iflt,label);
					return;
				}
			}
		}
		expr1.generate(code,Type.tpBoolean);
		expr2.generate(code,Type.tpBoolean);
		if     ( op == Operator.Equals )			code.addInstr(Instr.op_ifcmpne,label);
		else if( op == Operator.NotEquals )		code.addInstr(Instr.op_ifcmpeq,label);
		else if( op == Operator.LessThen )			code.addInstr(Instr.op_ifcmpge,label);
		else if( op == Operator.LessEquals )		code.addInstr(Instr.op_ifcmpgt,label);
		else if( op == Operator.GreaterThen )		code.addInstr(Instr.op_ifcmple,label);
		else if( op == Operator.GreaterEquals )	code.addInstr(Instr.op_ifcmplt,label);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JInstanceofExpr of InstanceofExpr extends JBoolExpr {
	public:ro JENode		expr;
	public:ro Type			itype;

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,itype);
		code.addInstr(Instr.op_ifne,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,itype);
		code.addInstr(Instr.op_ifeq,label);
	}
}

@ViewOf(vcast=true, iface=true)
public final view JBooleanNotExpr of BooleanNotExpr extends JBoolExpr {
	public:ro JENode		expr;
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iffalse(code, expr, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iftrue(code, expr, label);
	}

}

