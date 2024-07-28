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
import syntax kiev.Syntax;

import static kiev.be.java15.Instr.*;

interface IBoolExpr {
	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);
}

public class JBoolExpr extends JENode implements IBoolExpr {

	@virtual typedef VT  ≤ BoolExpr;

	public static JBoolExpr attach(BoolExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBoolExpr)jn;
		if (impl instanceof BinaryBooleanOrExpr)
			return JBinaryBooleanOrExpr.attach((BinaryBooleanOrExpr)impl);
		if (impl instanceof BinaryBooleanAndExpr)
			return JBinaryBooleanAndExpr.attach((BinaryBooleanAndExpr)impl);
		if (impl instanceof BinaryBoolExpr)
			return JBinaryBoolExpr.attach((BinaryBoolExpr)impl);
		if (impl instanceof InstanceofExpr)
			return JInstanceofExpr.attach((InstanceofExpr)impl);
		if (impl instanceof BooleanNotExpr)
			return JBooleanNotExpr.attach((BooleanNotExpr)impl);
		return new JBoolExpr(impl);
	}
	
	protected JBoolExpr(BoolExpr impl) {
		super(impl);
	}
	
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
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(Instr.op_pop);
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		throw new RuntimeException("JBoolExpr generate_iftrue:"+this);
	}
	public void generate_iffalse(Code code, CodeLabel label) {
		throw new RuntimeException("JBoolExpr generate_iffalse:"+this);
	}

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
				BinaryExpr vbe = be.vn();
				ENode vbe_expr2 = vbe.expr2;
				if( vbe_expr2.getType(code.env).isIntegerInCode() && vbe_expr2.isConstantExpr(code.env) ) {
					Object ce = vbe_expr2.getConstValue(code.env);
					if( ((Number)ce).intValue() == 0 ) {
						optimized = true;
						JENode be_expr1 = (JENode)vbe.expr1;
						Operator beop = be.getOper();
						if( beop == Operator.LessThen ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_ifge,label);
						}
						else if( beop == Operator.LessEquals ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_ifgt,label);
						}
						else if( beop == Operator.GreaterThen ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_ifle,label);
						}
						else if( beop == Operator.GreaterEquals ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_iflt,label);
						}
						else if( beop == Operator.Equals ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_ifne,label);
						}
						else if( beop == Operator.NotEquals ) {
							be_expr1.generate(code,null);
							code.addInstr(Instr.op_ifeq,label);
						}
						else {
							optimized = false;
						}
					}
				}
			}
			if( !optimized ) {
				expr.generate(code,code.tenv.tpBoolean);
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
			expr.generate(code,code.tenv.tpBoolean);
			code.addInstr(Instr.op_ifeq,label);
		}
		else
			throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
	}
}

public final class JBinaryBooleanOrExpr extends JBoolExpr {

	@virtual typedef VT  ≤ BinaryBooleanOrExpr;

	public static JBinaryBooleanOrExpr attach(BinaryBooleanOrExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBinaryBooleanOrExpr)jn;
		return new JBinaryBooleanOrExpr(impl);
	}
	
	protected JBinaryBooleanOrExpr(BinaryBooleanOrExpr impl) {
		super(impl);
	}
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		BinaryBooleanOrExpr vn = vn();
		JBoolExpr.gen_iftrue(code, (JENode)vn.expr1, label);
		JBoolExpr.gen_iftrue(code, (JENode)vn.expr2, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		BinaryBooleanOrExpr vn = vn();
		JBoolExpr.gen_iftrue(code, (JENode)vn.expr1, label1);
		JBoolExpr.gen_iffalse(code, (JENode)vn.expr2, label);
		code.addInstr(Instr.set_label,label1);
	}
}

public final class JBinaryBooleanAndExpr extends JBoolExpr {

	@virtual typedef VT  ≤ BinaryBooleanAndExpr;

	public static JBinaryBooleanAndExpr attach(BinaryBooleanAndExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBinaryBooleanAndExpr)jn;
		return new JBinaryBooleanAndExpr(impl);
	}
	
	protected JBinaryBooleanAndExpr(BinaryBooleanAndExpr impl) {
		super(impl);
	}
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this);
		CodeLabel label1 = code.newLabel();
		BinaryBooleanAndExpr vn = vn();
		JBoolExpr.gen_iffalse(code, (JENode)vn.expr1, label1);
		JBoolExpr.gen_iftrue(code, (JENode)vn.expr2, label);
		code.addInstr(Instr.set_label,label1);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this);
		BinaryBooleanAndExpr vn = vn();
		JBoolExpr.gen_iffalse(code, (JENode)vn.expr1, label);
		JBoolExpr.gen_iffalse(code, (JENode)vn.expr2, label);
	}
}

public final class JBinaryBoolExpr extends JBoolExpr {

	@virtual typedef VT  ≤ BinaryBoolExpr;

	public static JBinaryBoolExpr attach(BinaryBoolExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBinaryBoolExpr)jn;
		return new JBinaryBoolExpr(impl);
	}
	
	protected JBinaryBoolExpr(BinaryBoolExpr impl) {
		super(impl);
	}
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BoolExpr (if true): "+this);
		code.setLinePos(this);
		BinaryBoolExpr vn = vn();
		JENode expr1 = (JENode)vn.expr1;
		JENode expr2 = (JENode)vn.expr2;
		Operator op = getOper();
		if( expr2 instanceof JConstExpr ) {
			JConstExpr ce = (JConstExpr)expr2;
			Object cv = ce.getConstValue(code.env);
			if( cv == null ) {
				expr1.generate(code,code.tenv.tpBoolean);
				if( op == Operator.Equals) code.addInstr(Instr.op_ifnull,label);
				else if( op == Operator.NotEquals ) code.addInstr(Instr.op_ifnonnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,code.tenv.tpBoolean);
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
		expr1.generate(code,code.tenv.tpBoolean);
		expr2.generate(code,code.tenv.tpBoolean);
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
		BinaryBoolExpr vn = vn();
		JENode expr1 = (JENode)vn.expr1;
		JENode expr2 = (JENode)vn.expr2;
		Operator op = getOper();
		if( expr2 instanceof JConstExpr ) {
			JConstExpr ce = (JConstExpr)expr2;
			Object cv = ce.getConstValue(code.env);
			if( cv == null ) {
				expr1.generate(code,code.tenv.tpBoolean);
				if( op == Operator.Equals) code.addInstr(Instr.op_ifnonnull,label);
				else if( op == Operator.NotEquals ) code.addInstr(Instr.op_ifnull,label);
				else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
				return;
			}
			else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
				expr1.generate(code,code.tenv.tpBoolean);
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
		expr1.generate(code,code.tenv.tpBoolean);
		expr2.generate(code,code.tenv.tpBoolean);
		if     ( op == Operator.Equals )			code.addInstr(Instr.op_ifcmpne,label);
		else if( op == Operator.NotEquals )		code.addInstr(Instr.op_ifcmpeq,label);
		else if( op == Operator.LessThen )			code.addInstr(Instr.op_ifcmpge,label);
		else if( op == Operator.LessEquals )		code.addInstr(Instr.op_ifcmpgt,label);
		else if( op == Operator.GreaterThen )		code.addInstr(Instr.op_ifcmple,label);
		else if( op == Operator.GreaterEquals )	code.addInstr(Instr.op_ifcmplt,label);
	}
}

public final class JInstanceofExpr extends JBoolExpr {

	@virtual typedef VT  ≤ InstanceofExpr;

	public static JInstanceofExpr attach(InstanceofExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JInstanceofExpr)jn;
		return new JInstanceofExpr(impl);
	}
	
	protected JInstanceofExpr(InstanceofExpr impl) {
		super(impl);
	}
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		InstanceofExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		Type itype = vn.itype.getType(code.env);
		expr.generate(code,code.tenv.tpBoolean);
		code.addInstr(Instr.op_instanceof,itype);
		code.addInstr(Instr.op_ifne,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this);
		InstanceofExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		Type itype = vn.itype.getType(code.env);
		expr.generate(code,code.tenv.tpBoolean);
		code.addInstr(Instr.op_instanceof,itype);
		code.addInstr(Instr.op_ifeq,label);
	}
}

public final class JBooleanNotExpr extends JBoolExpr {

	@virtual typedef VT  ≤ BooleanNotExpr;

	public static JBooleanNotExpr attach(BooleanNotExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBooleanNotExpr)jn;
		return new JBooleanNotExpr(impl);
	}
	
	protected JBooleanNotExpr(BooleanNotExpr impl) {
		super(impl);
	}
	
	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iffalse(code, (JENode)vn().expr, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		code.setLinePos(this);
		JBoolExpr.gen_iftrue(code, (JENode)vn().expr, label);
	}

}

