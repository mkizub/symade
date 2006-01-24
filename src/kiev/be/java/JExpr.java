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

import kiev.vlang.Shadow.ShadowImpl;
import kiev.vlang.ArrayLengthExpr.ArrayLengthExprImpl;
import kiev.vlang.TypeClassExpr.TypeClassExprImpl;
import kiev.vlang.TypeInfoExpr.TypeInfoExprImpl;
import kiev.vlang.AssignExpr.AssignExprImpl;
import kiev.vlang.BinaryExpr.BinaryExprImpl;
import kiev.vlang.StringConcatExpr.StringConcatExprImpl;
import kiev.vlang.CommaExpr.CommaExprImpl;
import kiev.vlang.BlockExpr.BlockExprImpl;
import kiev.vlang.UnaryExpr.UnaryExprImpl;
import kiev.vlang.IncrementExpr.IncrementExprImpl;
import kiev.vlang.ConditionalExpr.ConditionalExprImpl;
import kiev.vlang.CastExpr.CastExprImpl;

@nodeview
public final view JShadow of ShadowImpl extends JENode {
	public access:ro	JNode		node;

	public void generate(Code code, Type reqType) {
		if (node instanceof JENode) {
			((JENode)node).generate(code,reqType);
		} else {
			((JInitializer)node).generate(code,reqType);
		}
	}
	
}

@nodeview
public final view JArrayLengthExpr of ArrayLengthExprImpl extends JAccessExpr {
	public JArrayLengthExpr(ArrayLengthExprImpl $view) { super($view); }

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerLengthExpr: "+this);
		code.setLinePos(this);
		obj.generate(code,null);
		code.addInstr(Instr.op_arrlength);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JTypeClassExpr of TypeClassExprImpl extends JENode {
	public access:ro	Type			type;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		code.setLinePos(this);
		code.addConst(type.getErasedType().getJType());
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JTypeInfoExpr of TypeInfoExprImpl extends JENode {
	public access:ro	Type					type;
	public access:ro	JTypeClassExpr		cl_expr;
	public access:ro	JArr<JENode>		cl_args;

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
		code.addInstr(op_call,func.getJView(),false,ti_clazz.ctype);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JAssignExpr of AssignExprImpl extends JLvalueExpr {
	public access:ro	AssignOperator		op;
	public access:ro	JENode			lval;
	public access:ro	JENode			value;

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
public view JBinaryExpr of BinaryExprImpl extends JENode {
	public access:ro	BinaryOperator	op;
	public access:ro	JENode		expr1;
	public access:ro	JENode		expr2;

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
public view JStringConcatExpr of StringConcatExprImpl extends JENode {
	public access:ro	JArr<JENode>		args;

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
		return m.getJView();
	}


	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		code.setLinePos(this);
		JENode[] args = this.args.toArray();
		code.addInstr(op_new,clazzStringBuffer.ctype);
		code.addInstr(op_dup);
		code.addInstr(op_call,clazzStringBufferInit.getJView(),false);
		for(int i=0; i < args.length; i++) {
			args[i].generate(code,null);
			code.addInstr(op_call,getMethodFor(args[i]),false);
		}
		code.addInstr(op_call,clazzStringBufferToString.getJView(),false);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JCommaExpr of CommaExprImpl extends JENode {
	public access:ro	JArr<JENode>		exprs;

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
public view JBlockExpr of BlockExprImpl extends JENode {
	public access:ro	JArr<JENode>	stats;
	public access:ro	JENode			res;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockExpr");
		code.setLinePos(this);
		JENode[] stats = this.stats.toArray();
		for(int i=0; i < stats.length; i++) {
			try {
				stats[i].generate(code,Type.tpVoid);
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		if (res != null) {
			try {
				res.generate(code,reqType);
			} catch(Exception e ) {
				Kiev.reportError(res,e);
			}
		}
		Vector<JVar> vars = new Vector<JVar>();
		foreach (JENode n; stats) {
			if (n instanceof JVarDecl)
				vars.append(n.var);
		}
		code.removeVars(vars.toArray());
	}

}

@nodeview
public view JUnaryExpr of UnaryExprImpl extends JENode {
	public access:ro	Operator			op;
	public access:ro	JENode			expr;

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
public final view JIncrementExpr of IncrementExprImpl extends JENode {
	public access:ro	Operator			op;
	public access:ro	JENode			lval;

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
	private void generateLoad(Code code) {
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

	private void pushProperConstant(Code code, int i) {
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
public view JConditionalExpr of ConditionalExprImpl extends JENode {
	public access:ro	JENode		cond;
	public access:ro	JENode		expr1;
	public access:ro	JENode		expr2;

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
public view JCastExpr of CastExprImpl extends JENode {
	public access:ro	JENode		expr;
	public access:ro	Type			type;
	public access:ro	boolean			reinterp;

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
			if (reinterp) {
				if (t.isIntegerInCode() && type.isIntegerInCode())
					; //generate nothing, both values are int-s
				else
					throw new CompilerException(this,"Expression "+expr+" of type "+t+" cannot be reinterpreted to type "+type);
			} else {
				code.addInstr(Instr.op_x2y,type);
			}
		}
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

