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
@cfnode
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
	
	public static Expr checkBool(ASTNode e) {
		if( e.getType().isBoolean() )
			return (Expr)e;
		if( e.getType() == Type.tpRule )
			return checkBool(
				new BinaryBoolExpr(e.pos,
					BinaryOperator.NotEquals,
					(Expr)e,
					new ConstNullExpr()
					).resolve(Type.tpBoolean)
				);
		else if( e.getType().args.length == 0
				&& e.getType() instanceof ClosureType
				&& ((CallableType)e.getType()).ret.isAutoCastableTo(Type.tpBoolean)
				)
		{
			((ClosureCallExpr)e).is_a_call = true;
			return (Expr)e;
		}
		throw new RuntimeException("Expression "+e+" must be of boolean type, but found "+e.getType());
	}
	
	public static void gen_iftrue(Expr expr, CodeLabel label) {
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

	public static void gen_iffalse(Expr expr, CodeLabel label) {
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
@cfnode
public class BinaryBooleanOrExpr extends BoolExpr {
	@att public Expr			expr1;
	@att public Expr			expr2;

	public BinaryBooleanOrExpr() {
	}

	public BinaryBooleanOrExpr(int pos, Expr expr1, Expr expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr2 = expr2;
	}

	public void cleanup() {
		parent=null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
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

	public int getPriority() { return opBooleanOrPriority; }

	public ASTNode resolve(Type reqType) {
//		if( isResolved() ) return this;
		PassInfo.push(this);
		ScopeNodeInfoVector result_state = null;
		try {
			NodeInfoPass.pushState();
			expr1 = BoolExpr.checkBool(expr1.resolve(Type.tpBoolean));
			ScopeNodeInfoVector state1 = NodeInfoPass.popState();
			NodeInfoPass.pushState();
			expr2 = BoolExpr.checkBool(expr2.resolve(Type.tpBoolean));
			ScopeNodeInfoVector state2 = NodeInfoPass.popState();
			result_state = NodeInfoPass.joinInfo(state1,state2);
		} finally {
			PassInfo.pop(this);
			if( result_state != null ) NodeInfoPass.addInfo(result_state);
		}
		setResolved(true);
		return this;
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
@cfnode
public class BinaryBooleanAndExpr extends BoolExpr {
	@att public Expr			expr1;
	@att public Expr			expr2;

	public BinaryBooleanAndExpr() {
	}

	public BinaryBooleanAndExpr(int pos, Expr expr1, Expr expr2) {
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

	public int getPriority() { return opBooleanAndPriority; }

	public void cleanup() {
		parent=null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
	}

	public ASTNode resolve(Type reqType) {
//		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			NodeInfoPass.pushState();
			expr1 = BoolExpr.checkBool(expr1.resolve(Type.tpBoolean));
			if( expr1 instanceof InstanceofExpr ) ((InstanceofExpr)expr1).setNodeTypeInfo();
			expr2 = BoolExpr.checkBool(expr2.resolve(Type.tpBoolean));
			NodeInfoPass.popState();
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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
@cfnode
public class BinaryBoolExpr extends BoolExpr {
	@ref public BinaryOperator		op;
	@att public Expr				expr1;
	@att public Expr				expr2;

	public BinaryBoolExpr() {
	}

	public BinaryBoolExpr(int pos, BinaryOperator op, Expr expr1, Expr expr2) {
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

	public int getPriority() { return op.priority; }

	public void cleanup() {
		parent=null;
		expr1.cleanup();
		expr1 = null;
		expr2.cleanup();
		expr2 = null;
	}

	public Expr tryResolve(Type reqType) {
		if( !resolveExprs() ) return null;
		setTryResolved(true);
		Type et1 = expr1.getType();
		Type et2 = expr2.getType();
		if( ( et1.isNumber() && et2.isNumber() ) &&
			(    op==BinaryOperator.LessThen
			||   op==BinaryOperator.LessEquals
			||   op==BinaryOperator.GreaterThen
			||   op==BinaryOperator.GreaterEquals
			)
		) {
			return (Expr)this.resolve(reqType);
		}
		else if( op==BinaryOperator.BooleanOr ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				return (Expr)new BinaryBooleanOrExpr(pos,expr1,expr2).resolve(Type.tpBoolean);
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				return (Expr)new BinaryBooleanAndExpr(pos,expr1,expr2).resolve(Type.tpBoolean);
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
			return (Expr)this.resolve(reqType);
		}
		// Not a standard operator, find out overloaded
		foreach(OpTypes opt; op.types ) {
			Type[] tps = new Type[]{null,et1,et2};
			ASTNode[] argsarr = new ASTNode[]{null,expr1,expr2};
			if( opt.match(tps,argsarr) ) {
				Expr e;
				if( opt.method.isStatic() )
					e = new CallExpr(pos,parent,opt.method,new Expr[]{expr1,expr2}).tryResolve(reqType);
				else
					e = new CallAccessExpr(pos,parent,expr1,opt.method,new Expr[]{expr2}).tryResolve(reqType);
				if( e != null ) return e;
			}
		}
		return null;
	}

	private boolean resolveExprs() {
		Expr ast1 = ((Expr)expr1).tryResolve(null);
		//if (ast1 == null)
		//	throw new RuntimeException("tryResolve for "+expr1+" ("+expr1.getClass()+") returned null");
		if (ast1 == null)
			return false;
		expr1 = ast1;
		if (!expr1.isForWrapper() && expr1.getType().isWrapper())
			expr1 = expr1.getType().makeWrappedAccess(expr1).resolveExpr(null);

		ASTNode ast2 = ((Expr)expr2).tryResolve(null);
		if( ast2 instanceof WrapedExpr )
			ast2 = ((Expr)ast2).resolve(null);
		if( ast2 instanceof TypeRef )
			ast2 = getExprByStruct(((TypeRef)ast2).getType().getStruct());
		if( ast2 instanceof Struct )
			ast2 = getExprByStruct((Struct)ast2);
		if !( ast2 instanceof Expr )
			return false;
		expr2 = (Expr)((Expr)ast2).resolve(null);
		if (!expr2.isForWrapper() && expr2.getType().isWrapper())
			expr2 = expr2.getType().makeWrappedAccess(expr2).resolveExpr(null);
		return true;
	}

	public Expr getExprByStruct(Struct cas) {
		Expr ex = null;
		if( cas.isPizzaCase() ) {
			if( !(op==BinaryOperator.Equals || op==BinaryOperator.NotEquals) )
				throw new CompilerException(pos,"Undefined operation "+op.image+" on cased class");
			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
			ex = (Expr)new ConstIntExpr(ca.caseno).resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp.isWrapper()) {
				expr1 = expr1.getType().makeWrappedAccess(expr1).resolveExpr(null);
				tp = expr1.getType();
			}
			if( !tp.isPizzaCase() && !tp.isHasCases() )
				throw new RuntimeException("Compare non-cased class "+tp+" with class's case "+cas);
			Method m = tp.resolveMethod(nameGetCaseTag,KString.from("()I"));
			expr1 = (Expr)new CallAccessExpr(ex.pos,parent,expr1,m,Expr.emptyArray).resolve(Type.tpInt);
		} else {
			throw new CompilerException(pos,"Class "+cas+" is not a cased class");
		}
		return ex;
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() ) return this;
		if( !isTryResolved() ) {
			Expr e = tryResolve(reqType);
			if( e != null ) return e;
			return this;
		}
		PassInfo.push(this);
		try {
			if( !resolveExprs() )
				throw new CompilerException(pos,"Unresolved expression "+this);
			Type t1 = expr1.getType();
			Type t2 = expr2.getType();
			if( !t1.equals(t2) ) {
				if( t1.isReference() != t2.isReference()) {
					if (t1.isEnum() && !t1.isIntegerInCode()) {
						expr1 = new CastExpr(expr1.pos,Type.tpInt,expr1).resolveExpr(Type.tpInt);
						t1 = expr1.getType();
					}
					if (t2.isEnum() && !t2.isIntegerInCode()) {
						expr2 = new CastExpr(expr2.pos,Type.tpInt,expr2).resolveExpr(Type.tpInt);
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
						expr1 = (Expr)new CastExpr(pos,t,expr1).resolve(t);
					}
					if( !t.equals(t2) && t2.isCastableTo(t) ) {
						expr2 = (Expr)new CastExpr(pos,t,expr2).resolve(t);
					}
				}
			}
		} finally { PassInfo.pop(this); }
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
@cfnode
public class InstanceofExpr extends BoolExpr {
	@att public Expr		expr;
	@ref public Type		type;

	public InstanceofExpr() {
	}

	public InstanceofExpr(int pos, Expr expr, Type type) {
		super(pos);
		this.expr = expr;
		this.type = type;
	}

	public String toString() {
		return expr+" instanceof "+type;
	}

	public int getPriority() { return Constants.opInstanceOfPriority; }

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() && !(isGenResolve() && (Code.generation||Kiev.gen_resolve))) return this;
		PassInfo.push(this);
		try {
			Object e = expr.resolve(null);
			if (e instanceof WrapedExpr)
				e = ((WrapedExpr)e).getType();
			if (e instanceof Struct)
				e = ((Struct)e).type;
			if( e instanceof TypeRef )
				e = ((TypeRef)e).getType();
			if( e instanceof Type ) {
				if( Code.generation||Kiev.gen_resolve ) {
					Type t = (Type)e;
					t = Type.getRealType(Kiev.argtype,t);
					if( t.isArgument() )
						t = t.getSuperType();
					return new ConstBoolExpr(t.isInstanceOf(type));
				} else {
					// Resolve at generate phase
					setGenResolve(true);
					setResolved(true);
					return this;
				}
			} else {
				expr = (Expr)e;
				Type et = expr.getType();
				if (!expr.isForWrapper() && et.isWrapper())
					expr = et.makeWrappedAccess(expr).resolveExpr(null);
			}
			if( !expr.getType().isCastableTo(type) ) {
				throw new CompilerException(pos,"Type "+expr.getType()+" is not castable to "+type);
			}
			if (expr.getType().isInstanceOf(type)) {
				return new BinaryBoolExpr(pos, BinaryOperator.NotEquals,expr,new ConstNullExpr()).resolve(reqType);
			}
			if (!type.isArray() && type.args.length > 0) {
				Expr be = new CallAccessExpr(pos,
						PassInfo.clazz.accessTypeInfoField(pos,PassInfo.clazz,type),
						Type.tpTypeInfo.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;)Z")),
						new Expr[]{expr}
						);
				return be.resolve(reqType);
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void setNodeTypeInfo() {
		ASTNode n;
		switch(expr) {
		case VarAccessExpr:			n = ((VarAccessExpr)expr).var;	break;
		case StaticFieldAccessExpr:	n = ((StaticFieldAccessExpr)expr).var;	break;
		case AccessExpr:
			if !(((AccessExpr)expr).obj instanceof ThisExpr)
				return;
			n = ((AccessExpr)expr).var;
			break;
		default: return;
		}
		NodeInfoPass.setNodeTypes(n,NodeInfoPass.addAccessType(expr.getAccessTypes(),type));
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(Type.tpBoolean);
			Code.addInstr(Instr.op_instanceof,type);
			Code.addInstr(Instr.op_ifne,label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating InstanceofExpr: "+this);
		PassInfo.push(this);
		try {
			expr.generate(Type.tpBoolean);
			Code.addInstr(Instr.op_instanceof,type);
			Code.addInstr(Instr.op_ifeq,label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space();
		dmp.append(expr).append(" instanceof ")
			.append(Type.getRealType(Kiev.argtype,type)).space();
		return dmp;
	}
}

@node
@cfnode
public class BooleanNotExpr extends BoolExpr {
	@att public Expr				expr;

	public BooleanNotExpr() {
	}

	public BooleanNotExpr(int pos, Expr expr) {
		super(pos);
		this.expr = expr;
	}

	public String toString() {
		if( expr.getPriority() < opBooleanNotPriority )
			return "!("+expr+")";
		else
			return "!"+expr;
	}

	public int getPriority() { return opBooleanNotPriority; }

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
	}

	public ASTNode resolve(Type reqType) {
		if( isResolved() && !(isGenResolve() && (Code.generation||Kiev.gen_resolve))) return this;
		PassInfo.push(this);
		try {
			expr = BoolExpr.checkBool(expr.resolve(Type.tpBoolean));
			if( expr.isConstantExpr() ) {
				return new ConstBoolExpr(!((Boolean)expr.getConstValue()).booleanValue());
			}
			if( expr.isGenResolve() )
				setGenResolve(true);
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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

