package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.NArr.JArr;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public final view JShadow of Shadow extends JENode {
	public:ro	JNode		node;

	public void generate(Code code, Type reqType) {
		if (node instanceof JENode) {
			((JENode)node).generate(code,reqType);
		} else {
			((JInitializer)node).generate(code,reqType);
		}
	}
	
}

@nodeview
public final view JArrayLengthExpr of ArrayLengthExpr extends JENode {
	public:ro JENode		obj;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerLengthExpr: "+this);
		code.setLinePos(this);
		obj.generate(code,null);
		code.addInstr(Instr.op_arrlength);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JTypeClassExpr of TypeClassExpr extends JENode {
	public:ro	Type			type;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		code.setLinePos(this);
		code.addConst(type.getErasedType().getJType());
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JTypeInfoExpr of TypeInfoExpr extends JENode {
	public:ro	Type					type;
	public:ro	JTypeClassExpr		cl_expr;
	public:ro	JArr<JENode>		cl_args;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating TypeInfoExpr: "+this);
		code.setLinePos(this);
		cl_expr.generate(code,null);
		JENode[] cl_args = this.cl_args.toArray();
		if (cl_args.length > 0) { 
			code.addConst(cl_args.length);
			code.addInstr(Instr.op_newarray,Type.tpTypeInfo);
			int i=0;
			foreach (JENode arg; cl_args) {
				code.addInstr(Instr.op_dup);
				code.addConst(i++);
				arg.generate(code,null);
				code.addInstr(Instr.op_arr_store);
			}
		} else {
			code.addNullConst();
		}
		Struct ti_clazz = type.getStruct().typeinfo_clazz;
		if (ti_clazz == null)
			ti_clazz = Type.tpTypeInfo.clazz;
		Method func = ti_clazz.resolveMethod(KString.from("newTypeInfo"), ti_clazz.ctype, Type.tpClass, new ArrayType(Type.tpTypeInfo));
		code.addInstr(op_call,(JMethod)func,false,ti_clazz.ctype);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JAssignExpr of AssignExpr extends JLvalueExpr {
	public:ro	AssignOperator		op;
	public:ro	JENode			lval;
	public:ro	JENode			value;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		code.setLinePos(this);
		JLvalueExpr lval = (JLvalueExpr)this.lval;
		if( reqType ≢ Type.tpVoid ) {
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
		JLvalueExpr lval = (JLvalueExpr)this.lval;
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
		JLvalueExpr lval = (JLvalueExpr)this.lval;
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStore(code);
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue(Code code) {
		code.setLinePos(this);
		JLvalueExpr lval = (JLvalueExpr)this.lval;
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStoreDupValue(code);
	}

}

@nodeview
public view JBinaryExpr of BinaryExpr extends JENode {
	public:ro	BinaryOperator	op;
	public:ro	JENode		expr1;
	public:ro	JENode		expr2;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BinaryExpr: "+this);
		code.setLinePos(this);
		expr1.generate(code,null);
		expr2.generate(code,null);
		code.addInstr(op.instr);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JStringConcatExpr of StringConcatExpr extends JENode {
	public:ro	JArr<JENode>		args;

	public static Struct clazzStringBuffer;
	public static Method clazzStringBufferToString;
	public static Method clazzStringBufferInit;

	static {
		try {
		clazzStringBuffer = Env.getStruct(ClazzName.fromToplevelName(KString.from("java.lang.StringBuffer"),false) );
		if( clazzStringBuffer == null )
			throw new RuntimeException("Core class java.lang.StringBuffer not found");
		clazzStringBufferToString = clazzStringBuffer.resolveMethod(KString.from("toString"),Type.tpString);
		clazzStringBufferInit = clazzStringBuffer.resolveMethod(KString.from("<init>"),Type.tpVoid);
		} catch(Exception e ) {
			throw new RuntimeException("Can't initialize: "+e.getMessage());
		}
	}

	public JMethod getMethodFor(JENode expr) {
		Method m = clazzStringBuffer.resolveMethod(KString.from("append"),clazzStringBuffer.ctype,expr.getType());
		return (JMethod)m;
	}


	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		code.setLinePos(this);
		JENode[] args = this.args.toArray();
		code.addInstr(op_new,clazzStringBuffer.ctype);
		code.addInstr(op_dup);
		code.addInstr(op_call,(JMethod)clazzStringBufferInit,false);
		for(int i=0; i < args.length; i++) {
			args[i].generate(code,null);
			code.addInstr(op_call,getMethodFor(args[i]),false);
		}
		code.addInstr(op_call,(JMethod)clazzStringBufferToString,false);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JCommaExpr of CommaExpr extends JENode {
	public:ro	JArr<JENode>		exprs;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		JENode[] exprs = this.exprs.toArray();
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1 )
				exprs[i].generate(code,Type.tpVoid);
			else
				exprs[i].generate(code,reqType);
		}
	}
}

@nodeview
public view JBlock of Block extends JENode {
	public:ro	JArr<JENode>	stats;
	public				CodeLabel		break_label;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating Block");
		code.setLinePos(this);
		JENode[] stats = this.stats.toArray();
		break_label = code.newLabel();
		for(int i=0; i < stats.length; i++) {
			try {
				if (i < stats.length-1 || isGenVoidExpr())
					stats[i].generate(code,Type.tpVoid);
				else
					stats[i].generate(code,reqType);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		Vector<JVar> vars = new Vector<JVar>();
		foreach (JENode n; stats) {
			if (n instanceof JVarDecl)
				vars.append(n.var);
		}
		code.removeVars(vars.toArray());
		JNode p = this.jparent;
		if( p instanceof JMethod && Kiev.debugOutputC && code.need_to_gen_post_cond && p.type.ret() ≢ Type.tpVoid)
			code.stack_push(p.etype.ret().getJType());
		code.addInstr(Instr.set_label,break_label);
	}

	public CodeLabel getBreakLabel() throws RuntimeException {
		if( break_label == null )
			throw new RuntimeException("Wrong generation phase for getting 'break' label");
		return break_label;
	}
}

@nodeview
public view JUnaryExpr of UnaryExpr extends JENode {
	public:ro	Operator			op;
	public:ro	JENode			expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating UnaryExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,null);
		if( op == PrefixOperator.BitNot ) {
			if( expr.getType() ≡ Type.tpLong )
				code.addConst(-1L);
			else
				code.addConst(-1);
			code.addInstr(op_xor);
		} else {
			code.addInstr(op.instr);
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JIncrementExpr of IncrementExpr extends JENode {
	public:ro	Operator			op;
	public:ro	JENode			lval;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		code.setLinePos(this);
		JLvalueExpr lval = (JLvalueExpr)this.lval;
		if( reqType ≢ Type.tpVoid ) {
			generateLoad(code);
		} else {
			if( lval instanceof JLVarExpr ) {
				JLVarExpr va = (JLVarExpr)lval;
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
	public:n,n,n,rw void generateLoad(Code code) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: - load "+this);
		code.setLinePos(this);
		JLvalueExpr lval = (JLvalueExpr)this.lval;
		if( lval instanceof JLVarExpr ) {
			JLVarExpr va = (JLVarExpr)lval;
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

	public:n,n,n,rw void pushProperConstant(Code code, int i) {
		Type lt = lval.getType();
		if( i > 0 ) { // 1
			if     ( lt ≡ Type.tpDouble ) code.addConst(1.D);
			else if( lt ≡ Type.tpFloat  ) code.addConst(1.F);
			else if( lt ≡ Type.tpLong   ) code.addConst(1L);
			else code.addConst(1);
		} else { // -1
			if     ( lt ≡ Type.tpDouble ) code.addConst(-1.D);
			else if( lt ≡ Type.tpFloat  ) code.addConst(-1.F);
			else if( lt ≡ Type.tpLong   ) code.addConst(-1L);
			else code.addConst(-1);
		}
	}

}

@nodeview
public view JConditionalExpr of ConditionalExpr extends JENode {
	public:ro	JENode		cond;
	public:ro	JENode		expr1;
	public:ro	JENode		expr2;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		if( cond.isConstantExpr() ) {
			if( ((Boolean)cond.getConstValue()).booleanValue() ) {
				expr1.generate(code,null);
			} else {
				expr2.generate(code,null);
			}
		} else {
			CodeLabel elseLabel = code.newLabel();
			CodeLabel endLabel = code.newLabel();
			JBoolExpr.gen_iffalse(code, cond, elseLabel);
			expr1.generate(code,null);
			code.addInstr(Instr.op_goto,endLabel);
			code.addInstr(Instr.set_label,elseLabel);
			expr2.generate(code,null);
			if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
			code.addInstr(Instr.set_label,endLabel);
		}
	}
}

@nodeview
public view JCastExpr of CastExpr extends JENode {
	public:ro	JENode			expr;
	public:ro	Type			type;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CastExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,null);
		Type t = expr.getType();
		if( t.isReference() ) {
			if( t.isReference() != type.isReference() )
				throw new CompilerException(this,"Expression "+expr+" of type "+t+" cannot be casted to type "+type);
			if( type.isReference() )
				code.addInstr(Instr.op_checkcast,type);
		} else {
			code.addInstr(Instr.op_x2y,type);
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

