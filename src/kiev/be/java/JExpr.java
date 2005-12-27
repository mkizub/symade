package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.Shadow.ShadowImpl;
import kiev.vlang.ArrayLengthExpr.ArrayLengthExprImpl;
import kiev.vlang.TypeClassExpr.TypeClassExprImpl;
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
public final view JShadowView of ShadowImpl extends JENodeView {
	public access:ro	JNodeView		node;

	public void generate(Code code, Type reqType) {
		if (node instanceof JENodeView) {
			((JENodeView)node).generate(code,reqType);
		} else {
			((JInitializerView)node).generate(code,reqType);
		}
	}
	
}

@nodeview
public final view JArrayLengthExprView of ArrayLengthExprImpl extends JAccessExprView {
	public JArrayLengthExprView(ArrayLengthExprImpl $view) { super($view); }

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating ContainerLengthExpr: "+this);
		code.setLinePos(this);
		obj.generate(code,null);
		code.addInstr(Instr.op_arrlength);
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JTypeClassExprView of TypeClassExprImpl extends JENodeView {
	public access:ro	Type			type;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		code.setLinePos(this);
		code.addConst(type.getErasedType().getJType());
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JAssignExprView of AssignExprImpl extends JLvalueExprView {
	public access:ro	AssignOperator		op;
	public access:ro	JENodeView			lval;
	public access:ro	JENodeView			value;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		code.setLinePos(this);
		JLvalueExprView lval = (JLvalueExprView)this.lval;
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
		JLvalueExprView lval = (JLvalueExprView)this.lval;
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
		JLvalueExprView lval = (JLvalueExprView)this.lval;
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStore(code);
	}

	/** Stores value using previously duped info, and put stored value in stack */
	public void generateStoreDupValue(Code code) {
		code.setLinePos(this);
		JLvalueExprView lval = (JLvalueExprView)this.lval;
		lval.generateLoadDup(code);
		value.generate(code,null);
		if( !(op == AssignOperator.Assign || op == AssignOperator.Assign2) )
			code.addInstr(op.instr);
		lval.generateStoreDupValue(code);
	}

}

@nodeview
public view JBinaryExprView of BinaryExprImpl extends JENodeView {
	public access:ro	BinaryOperator	op;
	public access:ro	JENodeView		expr1;
	public access:ro	JENodeView		expr2;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BinaryExpr: "+this);
		code.setLinePos(this);
		expr1.generate(code,null);
		expr2.generate(code,null);
		code.addInstr(op.instr);
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JStringConcatExprView of StringConcatExprImpl extends JENodeView {
	@getter public final JENodeView[]	get$args()	{ return (JENodeView[])this.$view.args.toJViewArray(JENodeView.class); }

	public static Struct clazzStringBuffer;
	public static Method clazzStringBufferToString;
	public static Method clazzStringBufferInit;

	static {
		try {
		clazzStringBuffer = Env.getStruct(ClazzName.fromToplevelName(KString.from("java.lang.StringBuffer"),false) );
		if( clazzStringBuffer == null )
			throw new RuntimeException("Core class java.lang.StringBuffer not found");
		clazzStringBufferToString = (Method)clazzStringBuffer.resolveMethod(
			KString.from("toString"),KString.from("()Ljava/lang/String;"));
		clazzStringBufferInit = (Method)clazzStringBuffer.resolveMethod(
			KString.from("<init>"),KString.from("()V"));
		} catch(Exception e ) {
			throw new RuntimeException("Can't initialize: "+e.getMessage());
		}
	}

	static final KString sigI = KString.from("(I)Ljava/lang/StringBuffer;");
	static final KString sigJ = KString.from("(J)Ljava/lang/StringBuffer;");
	static final KString sigZ = KString.from("(Z)Ljava/lang/StringBuffer;");
	static final KString sigC = KString.from("(C)Ljava/lang/StringBuffer;");
	static final KString sigF = KString.from("(F)Ljava/lang/StringBuffer;");
	static final KString sigD = KString.from("(D)Ljava/lang/StringBuffer;");
	static final KString sigObj = KString.from("(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
	static final KString sigStr = KString.from("(Ljava/lang/String;)Ljava/lang/StringBuffer;");
	static final KString sigArrC = KString.from("([C)Ljava/lang/StringBuffer;");
	public JMethodView getMethodFor(JENodeView expr) {
		JType t = expr.getType().getJType();
		KString sig = null;
		switch(t.java_signature.byteAt(0)) {
		case 'B':
		case 'S':
		case 'I': sig = sigI; break;
		case 'J': sig = sigJ; break;
		case 'Z': sig = sigZ; break;
		case 'C': sig = sigC; break;
		case 'F': sig = sigF; break;
		case 'D': sig = sigD; break;
		case 'L':
		case 'A':
		case '&':
		case 'R':
			if(t == Type.tpString)
				sig = sigStr;
			else
				sig = sigObj;
			break;
		case '[':
			if(t.java_signature.byteAt(1)=='C')
				sig = sigArrC;
			else
				sig = sigObj;
			break;
		}
		Method m = clazzStringBuffer.resolveMethod(KString.from("append"),sig);
		if( m == null )
			Kiev.reportError(expr,"Unknown method for StringBuffer");
		return m.getJMethodView();
	}


	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		code.setLinePos(this);
		JENodeView[] args = this.args;
		code.addInstr(op_new,clazzStringBuffer.type);
		code.addInstr(op_dup);
		code.addInstr(op_call,clazzStringBufferInit.getJMethodView(),false);
		for(int i=0; i < args.length; i++) {
			args[i].generate(code,null);
			code.addInstr(op_call,getMethodFor(args[i]),false);
		}
		code.addInstr(op_call,clazzStringBufferToString.getJMethodView(),false);
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public view JCommaExprView of CommaExprImpl extends JENodeView {
	@getter public final JENodeView[]	get$exprs()	{ return (JENodeView[])this.$view.exprs.toJViewArray(JENodeView.class); }

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		JENodeView[] exprs = this.exprs;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1 )
				exprs[i].generate(code,Type.tpVoid);
			else
				exprs[i].generate(code,reqType);
		}
	}
}

@nodeview
public view JBlockExprView of BlockExprImpl extends JENodeView {
	@getter public final JENodeView[]	get$stats()	{ return (JENodeView[])this.$view.stats.toJViewArray(JENodeView.class); }
	public access:ro	JENodeView			res;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\tgenerating BlockExpr");
		code.setLinePos(this);
		JENodeView[] stats = this.stats;
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
		Vector<JVarView> vars = new Vector<JVarView>();
		foreach (JENodeView n; stats) {
			if (n instanceof JVarDeclView)
				vars.append(n.var);
		}
		code.removeVars(vars.toArray());
	}

}

@nodeview
public view JUnaryExprView of UnaryExprImpl extends JENodeView {
	public access:ro	Operator			op;
	public access:ro	JENodeView		expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating UnaryExpr: "+this);
		code.setLinePos(this);
		expr.generate(code,null);
		if( op == PrefixOperator.BitNot ) {
			if( expr.getType() == Type.tpLong )
				code.addConst(-1L);
			else
				code.addConst(-1);
			code.addInstr(op_xor);
		} else {
			code.addInstr(op.instr);
		}
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

@nodeview
public final view JIncrementExprView of IncrementExprImpl extends JENodeView {
	public access:ro	Operator			op;
	public access:ro	JENodeView			lval;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		code.setLinePos(this);
		JLvalueExprView lval = (JLvalueExprView)this.lval;
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
		JLvalueExprView lval = (JLvalueExprView)this.lval;
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

@nodeview
public view JConditionalExprView of ConditionalExprImpl extends JENodeView {
	public access:ro	JENodeView		cond;
	public access:ro	JENodeView		expr1;
	public access:ro	JENodeView		expr2;

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
			JBoolExprView.gen_iffalse(code, cond, elseLabel);
			expr1.generate(code,null);
			code.addInstr(Instr.op_goto,endLabel);
			code.addInstr(Instr.set_label,elseLabel);
			expr2.generate(code,null);
			if( reqType == Type.tpVoid ) code.addInstr(op_pop);
			code.addInstr(Instr.set_label,endLabel);
		}
	}
}

@nodeview
public view JCastExprView of CastExprImpl extends JENodeView {
	public access:ro	JENodeView		expr;
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
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

