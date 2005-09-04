/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Instr.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

/**
 * @author Maxim Kizub
 *
 */

public interface IBoolExpr {
	public abstract void generate_iftrue(CodeLabel label);
	public abstract void generate_iffalse(CodeLabel label);
}

@node
public abstract class BoolExpr extends Expr implements IBoolExpr {

	public BoolExpr() {}

	public BoolExpr(int pos) { super(pos); }

	public BoolExpr(int pos, ASTNode parent) { super(pos, parent); }

	public Type getType() { return Type.tpBoolean; }

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr: "+this);
		PassInfo.push(this);
		try {
			CodeLabel label_true = Code.newLabel();
			CodeLabel label_false = Code.newLabel();

			generate_iftrue(label_true);
			Code.addConst(0);
			Code.addInstr(Instr.op_goto,label_false);
			Code.addInstr(Instr.set_label,label_true);
			Code.addConst(1);
			Code.addInstr(Instr.set_label,label_false);
			if( reqType == Type.tpVoid ) Code.addInstr(Instr.op_pop);
		} finally { PassInfo.pop(this); }
	}

	public abstract void generate_iftrue(CodeLabel label);
	public abstract void generate_iffalse(CodeLabel label);
	
	public static void checkBool(ENode e) {
		if( e.getType().isBoolean() ) {
			return;
		}
		if( e.getType() == Type.tpRule ) {
			e.replaceWithResolve(Type.tpBoolean, fun ()->ENode {
				return new BinaryBoolExpr(e.pos,BinaryOperator.NotEquals,e,new ConstNullExpr());
			});
			return;
		}
		else if( e.getType().args.length == 0
				&& e.getType() instanceof ClosureType
				&& ((CallableType)e.getType()).ret.isAutoCastableTo(Type.tpBoolean)
				)
		{
			((ClosureCallExpr)e).is_a_call = true;
			return;
		}
		throw new RuntimeException("Expression "+e+" must be of boolean type, but found "+e.getType());
	}
	
	public static void gen_iftrue(ENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			((IBoolExpr)expr).generate_iftrue(label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if true): "+expr);
		PassInfo.push(expr);
		try {
			if( expr.getType().isBoolean() ) {
				boolean optimized = false;
				if( expr instanceof BinaryExpr ) {
					BinaryExpr be = (BinaryExpr)expr;
					if( be.expr2.getType().isIntegerInCode() && be.expr2.isConstantExpr() ) {
						Object ce = be.expr2.getConstValue();
						if( ((Number)ce).intValue() == 0 ) {
							optimized = true;
							if( be.op == BinaryOperator.LessThen ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_ifge,label);
							}
							else if( be.op == BinaryOperator.LessEquals ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_ifgt,label);
							}
							else if( be.op == BinaryOperator.GreaterThen ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_ifle,label);
							}
							else if( be.op == BinaryOperator.GreaterEquals ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_iflt,label);
							}
							else if( be.op == BinaryOperator.Equals ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_ifne,label);
							}
							else if( be.op == BinaryOperator.NotEquals ) {
								be.expr1.generate(null);
								Code.addInstr(Instr.op_ifeq,label);
							}
							else {
								optimized = false;
							}
						}
					}
				}
				if( !optimized ) {
					expr.generate(Type.tpBoolean);
					Code.addInstr(Instr.op_ifne,label);
				}
			}
			else
				throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
		} finally { PassInfo.pop(expr); }
	}

	public static void gen_iffalse(ENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			((IBoolExpr)expr).generate_iffalse(label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if false): "+expr);
		PassInfo.push(expr);
		try {
			if( expr.getType().isBoolean() ) {
				expr.generate(Type.tpBoolean);
				Code.addInstr(Instr.op_ifeq,label);
			}
			else
				throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
		} finally { PassInfo.pop(expr); }
	}
}

@node
public class BinaryBooleanOrExpr extends BoolExpr {
	@att public ENode			expr1;
	@att public ENode			expr2;

	public BinaryBooleanOrExpr() {
	}

	public BinaryBooleanOrExpr(int pos, ENode expr1, ENode expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( expr1.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(BinaryOperator.BooleanOr.image);
		if( expr2.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public Operator getOp() { return BinaryOperator.BooleanOr; }

	public void resolve(Type reqType) {
		PassInfo.push(this);
		List<ScopeNodeInfo> state_base = NodeInfoPass.states;
		try {
			expr1.resolve(Type.tpBoolean);
			BoolExpr.checkBool(expr1);
			List<ScopeNodeInfo> state1 = NodeInfoPass.states;
			NodeInfoPass.states = state_base;
			expr2.resolve(Type.tpBoolean);
			BoolExpr.checkBool(expr2);
			List<ScopeNodeInfo> state2 = NodeInfoPass.states;
			NodeInfoPass.states = state_base;
			NodeInfoPass.joinInfo(state1,state2,state_base);
			state_base = null;
		} finally {
			if( state_base != null ) NodeInfoPass.states = state_base;
			PassInfo.pop(this);
		}
		setResolved(true);
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		PassInfo.push(this);
		try {
			BoolExpr.gen_iftrue(expr1, label);
			BoolExpr.gen_iftrue(expr2, label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		PassInfo.push(this);
		try {
			CodeLabel label1 = Code.newLabel();
			BoolExpr.gen_iftrue(expr1, label1);
			BoolExpr.gen_iffalse(expr2, label);
			Code.addInstr(Instr.set_label,label1);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < opBooleanOrPriority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(BinaryOperator.BooleanOr.image);
		if( expr2.getPriority() < opBooleanOrPriority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}


@node
public class BinaryBooleanAndExpr extends BoolExpr {
	@att public ENode			expr1;
	@att public ENode			expr2;

	public BinaryBooleanAndExpr() {
	}

	public BinaryBooleanAndExpr(int pos, ENode expr1, ENode expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( expr1.getPriority() < opBooleanAndPriority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(BinaryOperator.BooleanAnd.image);
		if( expr2.getPriority() < opBooleanAndPriority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public Operator getOp() { return BinaryOperator.BooleanAnd; }

	public void resolve(Type reqType) {
		PassInfo.push(this);
		List<ScopeNodeInfo> state_base = NodeInfoPass.states;
		try {
			expr1.resolve(Type.tpBoolean);
			BoolExpr.checkBool(expr1);
			if( expr1 instanceof InstanceofExpr )
				((InstanceofExpr)expr1).setNodeTypeInfo();
			expr2.resolve(Type.tpBoolean);
			BoolExpr.checkBool(expr2);
		} finally {
			if( state_base != null ) NodeInfoPass.states = state_base;
			PassInfo.pop(this);
		}
		setResolved(true);
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		PassInfo.push(this);
		try {
			CodeLabel label1 = Code.newLabel();
			BoolExpr.gen_iffalse(expr1, label1);
			BoolExpr.gen_iftrue(expr2, label);
			Code.addInstr(Instr.set_label,label1);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		PassInfo.push(this);
		try {
			BoolExpr.gen_iffalse(expr1, label);
			BoolExpr.gen_iffalse(expr2, label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < opBooleanAndPriority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(BinaryOperator.BooleanAnd.image);
		if( expr2.getPriority() < opBooleanAndPriority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}

@node
public class BinaryBoolExpr extends BoolExpr {
	@ref public BinaryOperator		op;
	@att public ENode				expr1;
	@att public ENode				expr2;

	public BinaryBoolExpr() {
	}

	public BinaryBoolExpr(int pos, BinaryOperator op, ENode expr1, ENode expr2) {
		super(pos);
		this.op = op;
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(expr1).append(op.image).append(expr2);
		return sb.toString();
	}

	public Operator getOp() { return op; }

	private void initialResolve(Type reqType) {
		setTryResolved(true);
		resolveExprs();
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==BinaryOperator.LessThen
			||   op==BinaryOperator.LessEquals
			||   op==BinaryOperator.GreaterThen
			||   op==BinaryOperator.GreaterEquals
			)
		) {
			this.resolve(reqType);
			return;
		}
		else if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanOrExpr(pos,expr1,expr2));
				return;
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanAndExpr(pos,expr1,expr2));
				return;
			}
		}
		else if(
			(	(et1.isNumber() && et2.isNumber())
			 || (et1.isReference() && et2.isReference())
			 || (et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean))
			 || (et1.isEnum() && et2.isIntegerInCode())
			 || (et1.isIntegerInCode() && et2.isEnum())
			 || (et1.isEnum() && et2.isEnum() && et1 == et2)
			) &&
			(   op==BinaryOperator.Equals
			||  op==BinaryOperator.NotEquals
			)
		) {
			this.resolve(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) ) {
				Expr e;
				if( opt.method.isStatic() )
					e = new CallExpr(pos,null,opt.method,new ENode[]{expr1,expr2});
				else
					e = new CallExpr(pos,expr1,opt.method,new ENode[]{expr2});
				replaceWithNodeResolve(reqType, e);
				return;
			}
		}
		throw new CompilerException(pos,"Unresolved expression "+this);
	}

	private boolean resolveExprs() {
		expr1.resolve(null);
		if (!expr1.isForWrapper() && expr1.getType().isWrapper()) {
			expr1 = expr1.getType().makeWrappedAccess(expr1);
			expr1.resolve(null);
		}

		expr2.resolve(null);
		if( expr2 instanceof TypeRef )
			getExprByStruct(((TypeRef)expr2).getType().getStruct());
		expr2.resolve(null);
		if (!expr2.isForWrapper() && expr2.getType().isWrapper()) {
			expr2 = expr2.getType().makeWrappedAccess(expr2);
			expr2.resolve(null);
		}
		return true;
	}

	public void getExprByStruct(Struct cas) {
		if( cas.isPizzaCase() ) {
			if( !(op==BinaryOperator.Equals || op==BinaryOperator.NotEquals) )
				throw new CompilerException(pos,"Undefined operation "+op.image+" on cased class");
			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
			expr2 = new ConstIntExpr(ca.caseno);
			expr2.resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp.isWrapper()) {
				expr1.getType().makeWrappedAccess(expr1);
				expr1.resolve(null);
				tp = expr1.getType();
			}
			if( !tp.isPizzaCase() && !tp.isHasCases() )
				throw new RuntimeException("Compare non-cased class "+tp+" with class's case "+cas);
			Method m = tp.resolveMethod(nameGetCaseTag,KString.from("()I"));
			expr1 = new CallExpr(expr1.pos,expr1,m,Expr.emptyArray);
			expr1.resolve(Type.tpInt);
		} else {
			throw new CompilerException(pos,"Class "+cas+" is not a cased class");
		}
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			resolveExprs();
			Type et1 = expr1.getType();
			Type et2 = expr2.getType();
			if( ( et1.isNumber() && et2.isNumber() ) &&
				(    op==BinaryOperator.LessThen
				||   op==BinaryOperator.LessEquals
				||   op==BinaryOperator.GreaterThen
				||   op==BinaryOperator.GreaterEquals
				)
			) {
				this.postResolve(reqType);
				return;
			}
			else if( op==BinaryOperator.BooleanOr ) {
				if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
					replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanOrExpr(pos,expr1,expr2));
					return;
				}
			}
			else if( op==BinaryOperator.BooleanAnd ) {
				if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
					replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanAndExpr(pos,expr1,expr2));
					return;
				}
			}
			else if(
				(	(et1.isNumber() && et2.isNumber())
				 || (et1.isReference() && et2.isReference())
				 || (et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean))
				 || (et1.isEnum() && et2.isIntegerInCode())
				 || (et1.isIntegerInCode() && et2.isEnum())
				 || (et1.isEnum() && et2.isEnum() && et1 == et2)
				) &&
				(   op==BinaryOperator.Equals
				||  op==BinaryOperator.NotEquals
				)
			) {
				this.postResolve(reqType);
				return;
			}
			// Not a standard operator, find out overloaded
			foreach(OpTypes opt; op.types ) {
				Type[] tps = new Type[]{null,et1,et2};
				ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
				if( opt.match(tps,argsarr) ) {
					if( opt.method.isStatic() )
						replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{expr1,expr2}));
					else
						replaceWithNodeResolve(reqType, new CallExpr(pos,expr1,opt.method,new ENode[]{expr2}));
					return;
				}
			}
		} finally { PassInfo.pop(this); }
		throw new CompilerException(pos,"Unresolved expression "+this);
	}
	
	private ASTNode postResolve(Type reqType) {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( !t1.equals(t2) ) {
			if( t1.isReference() != t2.isReference()) {
				if (t1.isEnum() && !t1.isIntegerInCode()) {
					expr1 = new CastExpr(expr1.pos,Type.tpInt,expr1);
					expr1.resolve(Type.tpInt);
					t1 = expr1.getType();
				}
				if (t2.isEnum() && !t2.isIntegerInCode()) {
					expr2 = new CastExpr(expr2.pos,Type.tpInt,expr2);
					expr2.resolve(Type.tpInt);
					t2 = expr2.getType();
				}
				if( t1.isReference() != t2.isReference() && t1.isIntegerInCode() != t2.isIntegerInCode())
					throw new RuntimeException("Boolean operator on reference and non-reference types");
			}
			if( !t1.isReference() && !t2.isReference()) {
				Type t;
				if( t1==Type.tpDouble || t2==Type.tpDouble ) t=Type.tpDouble;
				else if( t1==Type.tpFloat || t2==Type.tpFloat ) t=Type.tpFloat;
				else if( t1==Type.tpLong || t2==Type.tpLong ) t=Type.tpLong;
//					else if( t1==tInt || t2==tInt ) t=tInt;
//					else if( t1==tShort || t2==tShort ) t=tShort;
//					else if( t1==tByte || t2==tByte ) t=tByte;
//					else t = tVoid;
				else t = Type.tpInt;

				if( !t.equals(t1) && t1.isCastableTo(t) ) {
					expr1 = new CastExpr(pos,t,expr1);
					expr1.resolve(t);
				}
				if( !t.equals(t2) && t2.isCastableTo(t) ) {
					expr2 = new CastExpr(pos,t,expr2);
					expr2.resolve(t);
				}
			}
		}
		setResolved(true);
		return this;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr (if true): "+this);
		PassInfo.push(this);
		try {
			if( expr2 instanceof ConstExpr ) {
				ConstExpr ce = (ConstExpr)expr2;
				Object cv = ce.getConstValue();
				if( cv == null ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals) Code.addInstr(Instr.op_ifnull,label);
					else if( op == BinaryOperator.NotEquals ) Code.addInstr(Instr.op_ifnonnull,label);
					else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
					return;
				}
				else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals ) {
						Code.addInstr(Instr.op_ifeq,label);
						return;
					}
					else if( op == BinaryOperator.NotEquals ) {
						Code.addInstr(Instr.op_ifne,label);
						return;
					}
					else if( op == BinaryOperator.LessThen ) {
						Code.addInstr(Instr.op_iflt,label);
						return;
					}
					else if( op == BinaryOperator.LessEquals ) {
						Code.addInstr(Instr.op_ifle,label);
						return;
					}
					else if( op == BinaryOperator.GreaterThen ) {
						Code.addInstr(Instr.op_ifgt,label);
						return;
					}
					else if( op == BinaryOperator.GreaterEquals ) {
						Code.addInstr(Instr.op_ifge,label);
						return;
					}
				}
			}
			expr1.generate(Type.tpBoolean);
			expr2.generate(Type.tpBoolean);
			if( op == BinaryOperator.Equals )				Code.addInstr(Instr.op_ifcmpeq,label);
			else if( op == BinaryOperator.NotEquals )		Code.addInstr(Instr.op_ifcmpne,label);
			else if( op == BinaryOperator.LessThen )		Code.addInstr(Instr.op_ifcmplt,label);
			else if( op == BinaryOperator.LessEquals )	Code.addInstr(Instr.op_ifcmple,label);
			else if( op == BinaryOperator.GreaterThen )	Code.addInstr(Instr.op_ifcmpgt,label);
			else if( op == BinaryOperator.GreaterEquals )	Code.addInstr(Instr.op_ifcmpge,label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr (if false): "+this);
		PassInfo.push(this);
		try {
			if( expr2 instanceof ConstExpr ) {
				ConstExpr ce = (ConstExpr)expr2;
				Object cv = ce.getConstValue();
				if( cv == null ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals) Code.addInstr(Instr.op_ifnonnull,label);
					else if( op == BinaryOperator.NotEquals ) Code.addInstr(Instr.op_ifnull,label);
					else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
					return;
				}
				else if( expr2.getType().isIntegerInCode() && cv instanceof Number && ((Number)cv).intValue() == 0 ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals ) {
						Code.addInstr(Instr.op_ifne,label);
						return;
					}
					else if( op == BinaryOperator.NotEquals ) {
						Code.addInstr(Instr.op_ifeq,label);
						return;
					}
					else if( op == BinaryOperator.LessThen ) {
						Code.addInstr(Instr.op_ifge,label);
						return;
					}
					else if( op == BinaryOperator.LessEquals ) {
						Code.addInstr(Instr.op_ifgt,label);
						return;
					}
					else if( op == BinaryOperator.GreaterThen ) {
						Code.addInstr(Instr.op_ifle,label);
						return;
					}
					else if( op == BinaryOperator.GreaterEquals ) {
						Code.addInstr(Instr.op_iflt,label);
						return;
					}
				}
			}
			expr1.generate(Type.tpBoolean);
			expr2.generate(Type.tpBoolean);
			if( op == BinaryOperator.Equals )				Code.addInstr(Instr.op_ifcmpne,label);
			else if( op == BinaryOperator.NotEquals )		Code.addInstr(Instr.op_ifcmpeq,label);
			else if( op == BinaryOperator.LessThen )		Code.addInstr(Instr.op_ifcmpge,label);
			else if( op == BinaryOperator.LessEquals )	Code.addInstr(Instr.op_ifcmpgt,label);
			else if( op == BinaryOperator.GreaterThen )	Code.addInstr(Instr.op_ifcmple,label);
			else if( op == BinaryOperator.GreaterEquals )	Code.addInstr(Instr.op_ifcmplt,label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		if( expr1.getPriority() < op.priority ) {
			dmp.append('(').append(expr1).append(')');
		} else {
			dmp.append(expr1);
		}
		dmp.append(op.image);
		if( expr2.getPriority() < op.priority ) {
			dmp.append('(').append(expr2).append(')');
		} else {
			dmp.append(expr2);
		}
		return dmp;
	}
}

@node
public class InstanceofExpr extends BoolExpr {
	@att public ENode		expr;
	@att public TypeRef		type;

	public InstanceofExpr() {
	}

	public InstanceofExpr(int pos, ENode expr, TypeRef type) {
		super(pos);
		this.expr = expr;
		this.type = type;
	}

	public InstanceofExpr(int pos, ENode expr, Type type) {
		super(pos);
		this.expr = expr;
		this.type = new TypeRef(type);
	}

	public String toString() {
		return expr+" instanceof "+type;
	}

	public Operator getOp() { return BinaryOperator.InstanceOf; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			expr.resolve(null);
			Type tp = null;
			if( expr instanceof TypeRef )
				tp = ((TypeRef)expr).getType();
			if( tp != null ) {
				replaceWithNode(new ConstBoolExpr(tp.isInstanceOf(type.getType())));
				return;
			} else {
				Type et = expr.getType();
				if (!expr.isForWrapper() && et.isWrapper()) {
					expr = et.makeWrappedAccess(expr);
					expr.resolve(null);
				}
			}
			if( !expr.getType().isCastableTo(type.getType()) ) {
				throw new CompilerException(pos,"Type "+expr.getType()+" is not castable to "+type);
			}
			if (expr.getType().isInstanceOf(type.getType())) {
				replaceWithNodeResolve(reqType, new BinaryBoolExpr(pos, BinaryOperator.NotEquals,expr,new ConstNullExpr()));
				return;
			}
			if (!type.isArray() && type.args.length > 0) {
				replaceWithNodeResolve(reqType, new CallExpr(pos,
						PassInfo.clazz.accessTypeInfoField(pos,PassInfo.clazz,type.getType()),
						Type.tpTypeInfo.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;)Z")),
						new ENode[]{expr}
						)
					);
				return;
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
	}

	public void setNodeTypeInfo() {
		DNode[] path = null;
		switch(expr) {
		case VarAccessExpr:
			path = new DNode[]{((VarAccessExpr)expr).var};
			break;
		case AccessExpr:
			path = ((AccessExpr)expr).getAccessPath();
			break;
		case StaticFieldAccessExpr:
			path = new DNode[]{((StaticFieldAccessExpr)expr).var};
			break;
		}
		if (path != null)
			NodeInfoPass.addNodeType(path,type.getType());
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(Type.tpBoolean);
			Code.addInstr(Instr.op_instanceof,type.getType());
			Code.addInstr(Instr.op_ifne,label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(Type.tpBoolean);
			Code.addInstr(Instr.op_instanceof,type.getType());
			Code.addInstr(Instr.op_ifeq,label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(expr).append(" instanceof ").append(type).space();
		return dmp;
	}
}

@node
public class BooleanNotExpr extends BoolExpr {
	@att public ENode				expr;

	public BooleanNotExpr() {
	}

	public BooleanNotExpr(int pos, ENode expr) {
		super(pos);
		this.expr = expr;
	}

	public String toString() {
		if( expr.getPriority() < opBooleanNotPriority )
			return "!("+expr+")";
		else
			return "!"+expr;
	}

	public Operator getOp() { return PrefixOperator.BooleanNot; }

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			expr.resolve(Type.tpBoolean);
			BoolExpr.checkBool(expr);
			if( expr.isConstantExpr() ) {
				replaceWithNode(new ConstBoolExpr(!((Boolean)expr.getConstValue()).booleanValue()));
				return;
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		PassInfo.push(this);
		try {
			BoolExpr.gen_iffalse(expr, label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		PassInfo.push(this);
		try {
			BoolExpr.gen_iftrue(expr, label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append('!');
		if( expr.getPriority() < opBooleanNotPriority ) {
			dmp.append('(').append(expr).append(')');
		} else {
			dmp.append(expr);
		}
		return dmp;
	}
}

