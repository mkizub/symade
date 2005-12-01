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
	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);
}

@node
public abstract class BoolExpr extends Expr implements IBoolExpr {

	public BoolExpr() {}

	public BoolExpr(int pos) { super(pos); }

	public Type getType() { return Type.tpBoolean; }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr: "+this);
		code.setLinePos(this.getPosLine());
		CodeLabel label_true = code.newLabel();
		CodeLabel label_false = code.newLabel();

		generate_iftrue(code,label_true);
		code.addConst(0);
		code.addInstr(Instr.op_goto,label_false);
		code.addInstr(Instr.set_label,label_true);
		code.addConst(1);
		code.addInstr(Instr.set_label,label_false);
		if( reqType == Type.tpVoid ) code.addInstr(Instr.op_pop);
	}

	public abstract void generate_iftrue(Code code, CodeLabel label);
	public abstract void generate_iffalse(Code code, CodeLabel label);
	
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
	
	public static void gen_iftrue(Code code, ENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			((IBoolExpr)expr).generate_iftrue(code,label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if true): "+expr);
		code.setLinePos(expr.getPosLine());
		if( expr.getType().isBoolean() ) {
			boolean optimized = false;
			if( expr instanceof BinaryExpr ) {
				BinaryExpr be = (BinaryExpr)expr;
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

	public static void gen_iffalse(Code code, ENode expr, CodeLabel label) {
		if (expr instanceof IBoolExpr) {
			((IBoolExpr)expr).generate_iffalse(code, label);
			return;
		}
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if false): "+expr);
		code.setLinePos(expr.getPosLine());
		if( expr.getType().isBoolean() ) {
			expr.generate(code,Type.tpBoolean);
			code.addInstr(Instr.op_ifeq,label);
		}
		else
			throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
	}
}

@node
@dflow(tru="join expr1:true expr2:true", fls="expr2:false")
public class BinaryBooleanOrExpr extends BoolExpr {
	@att
	@dflow
	public ENode			expr1;
	
	@att
	@dflow(in="expr1:false")
	public ENode			expr2;

	public BinaryBooleanOrExpr() {
	}

	public BinaryBooleanOrExpr(int pos, ENode expr1, ENode expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (expr1 == null)
			sb.append("(?)");
		else if( expr1.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr1).append(')');
		else
			sb.append(expr1);
		sb.append(BinaryOperator.BooleanOr.image);
		if (expr2 == null)
			sb.append("(?)");
		else if( expr2.getPriority() < opBooleanOrPriority )
			sb.append('(').append(expr2).append(')');
		else
			sb.append(expr2);
		return sb.toString();
	}

	public Operator getOp() { return BinaryOperator.BooleanOr; }

	public void resolve(Type reqType) {
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		getDFlow().out();
		setResolved(true);
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this.getPosLine());
		BoolExpr.gen_iftrue(code, expr1, label);
		BoolExpr.gen_iftrue(code, expr2, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this.getPosLine());
		CodeLabel label1 = code.newLabel();
		BoolExpr.gen_iftrue(code, expr1, label1);
		BoolExpr.gen_iffalse(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
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
@dflow(fls="join expr1:false expr2:false", tru="expr2:true")
public class BinaryBooleanAndExpr extends BoolExpr {
	@dflow
	@att public ENode			expr1;
	
	@dflow(in="expr1:true")
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
		expr1.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr1);
		expr2.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr2);
		setResolved(true);
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		code.setLinePos(this.getPosLine());
		CodeLabel label1 = code.newLabel();
		BoolExpr.gen_iffalse(code, expr1, label1);
		BoolExpr.gen_iftrue(code, expr2, label);
		code.addInstr(Instr.set_label,label1);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		code.setLinePos(this.getPosLine());
		BoolExpr.gen_iffalse(code, expr1, label);
		BoolExpr.gen_iffalse(code, expr2, label);
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
@dflow(out="expr2")
public class BinaryBoolExpr extends BoolExpr {
	
	@ref public BinaryOperator		op;
	
	@dflow(in="")
	@att public ENode				expr1;
	@dflow(in="expr1")
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

	public void mainResolveOut() {
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNode(new BinaryBooleanOrExpr(pos,expr1,expr2));
				return;
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNode(new BinaryBooleanAndExpr(pos,expr1,expr2));
				return;
			}
		}
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
				throw new CompilerException(this,"Undefined operation "+op.image+" on cased class");
//			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
//			expr2 = new ConstIntExpr(ca.caseno);
			MetaPizzaCase meta = cas.getMetaPizzaCase();
			expr2 = new ConstIntExpr(meta.getTag());
			expr2.resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp.isWrapper()) {
				expr1.getType().makeWrappedAccess(expr1);
				expr1.resolve(null);
				tp = expr1.getType();
			}
			if( !tp.isPizzaCase() && !tp.isHasCases() )
				throw new RuntimeException("Compare non-cased class "+tp+" with class's case "+cas);
			Method m = ((BaseType)tp).clazz.resolveMethod(nameGetCaseTag,KString.from("()I"));
			expr1 = new CallExpr(expr1.pos,(ENode)~expr1,m,Expr.emptyArray);
			expr1.resolve(Type.tpInt);
		} else {
			throw new CompilerException(this,"Class "+cas+" is not a cased class");
		}
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
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
			this.resolve2(reqType);
			return;
		}
		else if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanOrExpr(pos,(ENode)~expr1,(ENode)~expr2));
				return;
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				replaceWithNodeResolve(Type.tpBoolean, new BinaryBooleanAndExpr(pos,(ENode)~expr1,(ENode)~expr2));
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
			this.resolve2(reqType);
			return;
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) ) {
				if( opt.method.isStatic() )
					replaceWithNodeResolve(reqType, new CallExpr(pos,null,opt.method,new ENode[]{(ENode)~expr1,(ENode)~expr2}));
				else
					replaceWithNodeResolve(reqType, new CallExpr(pos,expr1,opt.method,new ENode[]{(ENode)~expr2}));
				return;
			}
		}
		throw new CompilerException(this,"Unresolved expression "+this);
	}
	
	private ASTNode resolve2(Type reqType) {
		Type t1 = expr1.getType();
		Type t2 = expr2.getType();
		if( !t1.equals(t2) ) {
			if( t1.isReference() != t2.isReference()) {
				if (t1.isEnum() && !t1.isIntegerInCode()) {
					expr1 = new CastExpr(expr1.pos,Type.tpInt,(ENode)~expr1);
					expr1.resolve(Type.tpInt);
					t1 = expr1.getType();
				}
				if (t2.isEnum() && !t2.isIntegerInCode()) {
					expr2 = new CastExpr(expr2.pos,Type.tpInt,(ENode)~expr2);
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
					expr1 = new CastExpr(pos,t,(ENode)~expr1);
					expr1.resolve(t);
				}
				if( !t.equals(t2) && t2.isCastableTo(t) ) {
					expr2 = new CastExpr(pos,t,(ENode)~expr2);
					expr2.resolve(t);
				}
			}
		}
		setResolved(true);
		return this;
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BoolExpr (if true): "+this);
		code.setLinePos(this.getPosLine());
		if( expr2 instanceof ConstExpr ) {
			ConstExpr ce = (ConstExpr)expr2;
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
		code.setLinePos(this.getPosLine());
		if( expr2 instanceof ConstExpr ) {
			ConstExpr ce = (ConstExpr)expr2;
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
@dflow(tru="this:tru()", fls="expr")
public class InstanceofExpr extends BoolExpr {
	@dflow(in="")
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
				expr.setForWrapper(true);
				expr.resolve(null);
			}
		}
		if( !expr.getType().isCastableTo(type.getType()) ) {
			throw new CompilerException(this,"Type "+expr.getType()+" is not castable to "+type);
		}
		if (expr.getType().isInstanceOf(type.getType())) {
			replaceWithNodeResolve(reqType,
				new BinaryBoolExpr(pos, BinaryOperator.NotEquals,(ENode)~expr,new ConstNullExpr()));
			return;
		}
		if (!type.isArray() && type.args.length > 0) {
			replaceWithNodeResolve(reqType, new CallExpr(pos,
					pctx.clazz.accessTypeInfoField(this,type.getType()),
					Type.tpTypeInfo.clazz.resolveMethod(
						KString.from("$instanceof"),KString.from("(Ljava/lang/Object;)Z")),
					new ENode[]{(ENode)~expr}
					)
				);
			return;
		}
		setResolved(true);
	}

	static class InstanceofExprDFFunc extends DFFunc {
		final DFFunc f;
		final int res_idx;
		InstanceofExprDFFunc(DataFlowInfo dfi) {
			f = new DFFunc.DFFuncChildOut(dfi.getSocket("expr"));
			res_idx = dfi.allocResult(); 
		}
		DFState calc(DataFlowInfo dfi) {
			DFState res = dfi.getResult(res_idx);
			if (res != null) return res;
			res = ((InstanceofExpr)dfi.node).addNodeTypeInfo(DFFunc.calc(f, dfi));
			dfi.setResult(res_idx, res);
			return res;
		}
	}
	public DFFunc newDFFuncTru(DataFlowInfo dfi) {
		return new InstanceofExprDFFunc(dfi);
	}

	DFState addNodeTypeInfo(DFState dfs) {
		DNode[] path = null;
		switch(expr) {
		case LVarExpr:
			path = new DNode[]{((LVarExpr)expr).getVar()};
			break;
		case IFldExpr:
			path = ((IFldExpr)expr).getAccessPath();
			break;
		case SFldExpr:
			path = new DNode[]{((SFldExpr)expr).var};
			break;
		}
		if (path != null) {
			Type et = expr.getType();
			Type tp = type.getType();
			if (et.isWrapper() && !tp.isWrapper()) {
				Type ut = ((WrapperType)et).getUnwrappedType();
				tp = WrapperType.newWrapperType(Type.newRefType(ut,new Type[]{tp}));
			}
			return dfs.addNodeType(path,tp);
		}
		return dfs;
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this.getPosLine());
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,type.getType());
		code.addInstr(Instr.op_ifne,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		code.setLinePos(this.getPosLine());
		expr.generate(code,Type.tpBoolean);
		code.addInstr(Instr.op_instanceof,type.getType());
		code.addInstr(Instr.op_ifeq,label);
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(expr).append(" instanceof ").append(type).space();
		return dmp;
	}
}

@node
@dflow(fls="expr:true", tru="expr:false")
public class BooleanNotExpr extends BoolExpr {
	
	@dflow(in="")
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
		expr.resolve(Type.tpBoolean);
		BoolExpr.checkBool(expr);
		if( expr.isConstantExpr() ) {
			replaceWithNode(new ConstBoolExpr(!((Boolean)expr.getConstValue()).booleanValue()));
			return;
		}
		setResolved(true);
		return;
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		code.setLinePos(this.getPosLine());
		BoolExpr.gen_iffalse(code, expr, label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		code.setLinePos(this.getPosLine());
		BoolExpr.gen_iftrue(code, expr, label);
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

