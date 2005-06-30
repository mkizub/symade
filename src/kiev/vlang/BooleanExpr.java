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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/BooleanExpr.java,v 1.5.2.1.2.1 1999/02/15 21:45:10 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

@node
public class BooleanWrapperExpr extends BooleanExpr {
	public Expr		expr;

	public BooleanWrapperExpr(int pos, Expr expr) {
		super(pos);
		this.expr = expr;
		this.expr.parent = this;
	}

	public String toString() { return expr.toString(); }

	public Type getType() { return Type.tpBoolean; }

	public void cleanup() {
		parent = null;
		expr.cleanup();
		expr = null;
	}

	public ASTNode resolve(Type reqType) {
		this.expr.parent = this;
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			Expr e = (Expr)expr.resolve(Type.tpBoolean);
			if( e instanceof BooleanExpr ) {
				return e;
			}
			if( e.getType().isBoolean() ) {
				expr = e;
			}
			else if( e.getType() == Type.tpRule ) {
				return new BinaryBooleanExpr(pos,
					BinaryOperator.NotEquals,
					e,
					new ConstExpr(e.pos,null)).resolve(reqType);
			}
			else if( e.getType().args.length == 0 && e.getType() instanceof MethodType && ((MethodType)e.getType()).ret.isAutoCastableTo(Type.tpBoolean) ) {
				expr = e;
				((ClosureCallExpr)expr).is_a_call = true;
			}
			else
				throw new RuntimeException("Expression "+e+" resolved from "+expr+" must be of boolean type, but found "+e.getType());
			this.expr.parent = this;
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if true): "+this);
		PassInfo.push(this);
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
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanWarpperExpr (if false): "+this);
		PassInfo.push(this);
		try {
			if( expr.getType().isBoolean() ) {
				expr.generate(Type.tpBoolean);
				Code.addInstr(Instr.op_ifeq,label);
			}
			else
				throw new RuntimeException("BooleanWrapper generation of non-boolean expression "+expr);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(expr).space();
	}
}

@node
public class ConstBooleanExpr extends BooleanExpr {
	public boolean value;

	public ConstBooleanExpr(int pos, boolean val) {
		super(pos);
		value = val;
	}

	public String toString() { return String.valueOf(value); }

	public boolean isConstantExpr() { return true; }
	public Object getConstValue() { return value ? Boolean.TRUE: Boolean.FALSE; }
	public int		getPriority() { return 255; }

	public void cleanup() {
		parent=null;
	}

	public ASTNode resolve(Type reqType) {
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBooleanExpr: "+this);
		PassInfo.push(this);
		try {
			if( reqType != Type.tpVoid ) {
				if( value )
					Code.addConst(1);
				else
					Code.addConst(0);
			}
		} finally { PassInfo.pop(this); }
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBooleanExpr: if_true "+this);
		PassInfo.push(this);
		try {
			if( value ) Code.addInstr(op_goto,label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBooleanExpr: if_false "+this);
		PassInfo.push(this);
		try {
			if( !value ) Code.addInstr(op_goto,label);
		} finally { PassInfo.pop(this); }
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(String.valueOf(value)).space();
	}
}

@node
public class BinaryBooleanOrExpr extends BooleanExpr {
	public BooleanExpr			expr1;
	public BooleanExpr			expr2;

	public BinaryBooleanOrExpr(int pos, BooleanExpr expr1, BooleanExpr expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr1.parent = this;
		this.expr2 = expr2;
		this.expr2.parent = this;
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
		this.expr1.parent = this;
		this.expr2.parent = this;
//		if( isResolved() ) return this;
		PassInfo.push(this);
		ScopeNodeInfoVector result_state = null;
		try {
			this.expr1.parent = this;
			this.expr2.parent = this;
			NodeInfoPass.pushState();
			expr1 = (BooleanExpr)expr1.resolve(Type.tpBoolean);
			ScopeNodeInfoVector state1 = NodeInfoPass.popState();
			NodeInfoPass.pushState();
			expr2 = (BooleanExpr)expr2.resolve(Type.tpBoolean);
			ScopeNodeInfoVector state2 = NodeInfoPass.popState();
			result_state = NodeInfoPass.joinInfo(state1,state2);
			this.expr1.parent = this;
			this.expr2.parent = this;
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
			expr1.generate_iftrue(label);
			expr2.generate_iftrue(label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		PassInfo.push(this);
		try {
			CodeLabel label1 = Code.newLabel();
			expr1.generate_iftrue(label1);
			expr2.generate_iffalse(label);
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
public class BinaryBooleanAndExpr extends BooleanExpr {
	public BooleanExpr			expr1;
	public BooleanExpr			expr2;

	public BinaryBooleanAndExpr(int pos, BooleanExpr expr1, BooleanExpr expr2) {
		super(pos);
		this.expr1 = expr1;
		this.expr1.parent = this;
		this.expr2 = expr2;
		this.expr2.parent = this;
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
		this.expr1.parent = this;
		this.expr2.parent = this;
//		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			this.expr1.parent = this;
			this.expr2.parent = this;
			NodeInfoPass.pushState();
			expr1 = (BooleanExpr)expr1.resolve(Type.tpBoolean);
			if( expr1 instanceof InstanceofExpr ) ((InstanceofExpr)expr1).setNodeTypeInfo();
			expr2 = (BooleanExpr)expr2.resolve(Type.tpBoolean);
			NodeInfoPass.popState();
			this.expr1.parent = this;
			this.expr2.parent = this;
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if true): "+this);
		PassInfo.push(this);
		try {
			CodeLabel label1 = Code.newLabel();
			expr1.generate_iffalse(label1);
			expr2.generate_iftrue(label);
			Code.addInstr(Instr.set_label,label1);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanOrExpr (if false): "+this);
		PassInfo.push(this);
		try {
			expr1.generate_iffalse(label);
			expr2.generate_iffalse(label);
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
public class BinaryBooleanExpr extends BooleanExpr {
	public BinaryOperator		op;
	public Expr					expr1;
	public Expr					expr2;

	public BinaryBooleanExpr(int pos, BinaryOperator op, Expr expr1, Expr expr2) {
		super(pos);
		this.op = op;
		this.expr1 = expr1;
		this.expr1.parent = this;
		this.expr2 = expr2;
		this.expr2.parent = this;
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
				if( !(expr1 instanceof BooleanExpr) ) expr1 = new BooleanWrapperExpr(expr1.getPos(),(Expr)expr1);
				if( !(expr2 instanceof BooleanExpr) ) expr2 = new BooleanWrapperExpr(expr2.getPos(),(Expr)expr2);
				return (Expr)new BinaryBooleanOrExpr(pos,(BooleanExpr)expr1,(BooleanExpr)expr2).resolve(Type.tpBoolean);
			}
		}
		else if( op==BinaryOperator.BooleanAnd ) {
			if( et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean) ) {
				if( !(expr1 instanceof BooleanExpr) ) expr1 = new BooleanWrapperExpr(expr1.getPos(),(Expr)expr1);
				if( !(expr2 instanceof BooleanExpr) ) expr2 = new BooleanWrapperExpr(expr2.getPos(),(Expr)expr2);
				return (Expr)new BinaryBooleanAndExpr(pos,(BooleanExpr)expr1,(BooleanExpr)expr2).resolve(Type.tpBoolean);
			}
		}
		else if(
			(	(et1.isNumber() && et2.isNumber())
			 || (et1.isReference() && et2.isReference())
			 || (et1.isAutoCastableTo(Type.tpBoolean) && et2.isAutoCastableTo(Type.tpBoolean))
			 || (et1.clazz.isEnum() && et2.isIntegerInCode())
			 || (et1.isIntegerInCode() && et2.clazz.isEnum())
			 || (et1.clazz.isEnum() && et2.clazz.isEnum() && et1 == et2)
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

		ASTNode ast2 = ((Expr)expr2).tryResolve(null);
		if( ast2 instanceof WrapedExpr )
			ast2 = ((Expr)ast2).resolve(null);
		if( ast2 instanceof Struct )
			ast2 = getExprByStruct((Struct)ast2);
		if( ast2 instanceof Expr )
			expr2 = (Expr)((Expr)ast2).resolve(null);
		else
			return false;
		return true;
	}

	public Expr getExprByStruct(Struct cas) {
		Expr ex = null;
		if( cas.isPizzaCase() ) {
			if( !(op==BinaryOperator.Equals || op==BinaryOperator.NotEquals) )
				throw new CompilerException(pos,"Undefined operation "+op.image+" on cased class");
			PizzaCaseAttr ca = (PizzaCaseAttr)cas.getAttr(attrPizzaCase);
			ex = (Expr)new ConstExpr(pos,Kiev.newInteger(ca.caseno)).resolve(Type.tpInt);
			Type tp = expr1.getType();
			if (tp.clazz.isWrapper()) {
				expr1 = new AccessExpr(expr1.pos,expr1,tp.clazz.wrapped_field).resolveExpr(null);
				tp = expr1.getType();
			}
			if( !tp.clazz.isPizzaCase() && !tp.clazz.isHasCases() )
				throw new RuntimeException("Compare non-cased class "+tp.clazz+" with class's case "+cas);
			Method m = tp.clazz.resolveMethod(nameGetCaseTag,KString.from("()I"));
			expr1 = (Expr)new CallAccessExpr(ex.pos,parent,expr1,m,Expr.emptyArray).resolve(Type.tpInt);
		} else {
			throw new CompilerException(pos,"Class "+cas+" is not a cased class");
		}
		return ex;
	}

	public ASTNode resolve(Type reqType) {
		this.expr1.parent = this;
		this.expr2.parent = this;
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
					if (t1.clazz.isEnum() && !t1.isIntegerInCode()) {
						expr1 = new CastExpr(expr1.pos,Type.tpInt,expr1).resolveExpr(Type.tpInt);
						t1 = expr1.getType();
					}
					if (t2.clazz.isEnum() && !t2.isIntegerInCode()) {
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
			this.expr1.parent = this;
			this.expr2.parent = this;
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanExpr (if true): "+this);
		PassInfo.push(this);
		try {
			if( expr2 instanceof ConstExpr ) {
				ConstExpr ce = (ConstExpr)expr2;
				if( ce.value == null ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals) Code.addInstr(Instr.op_ifnull,label);
					else if( op == BinaryOperator.NotEquals ) Code.addInstr(Instr.op_ifnonnull,label);
					else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
					return;
				}
				else if( expr2.getType().isIntegerInCode() && ce.value instanceof Number && ((Number)ce.value).intValue() == 0 ) {
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
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanExpr (if false): "+this);
		PassInfo.push(this);
		try {
			if( expr2 instanceof ConstExpr ) {
				ConstExpr ce = (ConstExpr)expr2;
				if( ce.value == null ) {
					expr1.generate(Type.tpBoolean);
					if( op == BinaryOperator.Equals) Code.addInstr(Instr.op_ifnonnull,label);
					else if( op == BinaryOperator.NotEquals ) Code.addInstr(Instr.op_ifnull,label);
					else throw new RuntimeException("Only == and != boolean operations permitted on 'null' constant");
					return;
				}
				else if( expr2.getType().isIntegerInCode() && ce.value instanceof Number && ((Number)ce.value).intValue() == 0 ) {
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
public class InstanceofExpr extends BooleanExpr {
	public Expr		expr;
	public Type		type;

	public InstanceofExpr(int pos, Expr expr, Type type) {
		super(pos);
		this.expr = expr;
		this.expr.parent = this;
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
		this.expr.parent = this;
		if( isResolved() && !(isGenResolve() && (Code.generation||Kiev.gen_resolve))) return this;
		PassInfo.push(this);
		try {
			ASTNode e = expr.resolve(null);
			if( e instanceof Struct ) {
				if( Code.generation||Kiev.gen_resolve ) {
					ASTNode e = e.resolve(null);
					Struct s = (Struct)e;
					if( s.isArgument() ) {
						s = Type.getRealType(Kiev.argtype,s.type).clazz;
						if( s.isArgument() )
							s = s.super_clazz.clazz;
					}
					return new ConstBooleanExpr(pos,s.instanceOf(type.clazz));
				} else {
					// Resolve at generate phase
					setGenResolve(true);
					setResolved(true);
					return this;
				}
			} else {
				expr = (Expr)e;
				if (!expr.isForWrapper() && expr.getType().clazz.isWrapper())
					expr = new AccessExpr(expr.pos,expr,expr.getType().clazz.wrapped_field).resolveExpr(null);
			}
			if( !expr.getType().isCastableTo(type) ) {
				throw new CompilerException(pos,"Type "+expr.getType()+" is not castable to "+type);
			}
			if (!type.isArray() && type.args.length > 0) {
				BooleanExpr be = new BooleanWrapperExpr(pos, new CallAccessExpr(pos,
						PassInfo.clazz.accessTypeInfoField(pos,PassInfo.clazz,type),
						Type.tpTypeInfo.clazz.resolveMethod(
							KString.from("$instanceof"),KString.from("(Ljava/lang/Object;)Z")),
						new Expr[]{expr}
						));
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
		case FieldAccessExpr:		n = ((FieldAccessExpr)expr).var;	break;
		case StaticFieldAccessExpr:	n = ((StaticFieldAccessExpr)expr).var;	break;
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
public class BooleanNotExpr extends BooleanExpr {
	public BooleanExpr				expr;

	public BooleanNotExpr(int pos, BooleanExpr expr) {
		super(pos);
		this.expr = expr;
		this.expr.parent = this;
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
		this.expr.parent = this;
		if( isResolved() && !(isGenResolve() && (Code.generation||Kiev.gen_resolve))) return this;
		PassInfo.push(this);
		try {
			expr = (BooleanExpr)expr.resolve(Type.tpBoolean);
			if( expr.isConstantExpr() ) {
				return new ConstBooleanExpr(pos, !((Boolean)expr.getConstValue()).booleanValue());
			}
			if( expr.isGenResolve() )
				setGenResolve(true);
			this.expr.parent = this;
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
	}

	public void generate_iftrue(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if true): "+this);
		PassInfo.push(this);
		try {
			expr.generate_iffalse(label);
		} finally { PassInfo.pop(this); }
	}

	public void generate_iffalse(CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanNotExpr (if false): "+this);
		PassInfo.push(this);
		try {
			expr.generate_iftrue(label);
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

