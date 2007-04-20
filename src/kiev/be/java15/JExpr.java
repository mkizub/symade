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

public final view JShadow of Shadow extends JENode {
	public:ro	JNode		rnode;

	public void generate(Code code, Type reqType) {
		if (rnode instanceof JENode) {
			((JENode)rnode).generate(code,reqType);
		} else {
			((JInitializer)rnode).generate(code,reqType);
		}
	}
	
}

public final view JTypeClassExpr of TypeClassExpr extends JENode {
	public:ro	Type			type;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		code.setLinePos(this);
		code.addConst(type.getErasedType().getJType());
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

public final view JTypeInfoExpr of TypeInfoExpr extends JENode {
	public:ro	Type				type;
	public:ro	JENode				cl_expr;
	public:ro	JENode[]			cl_args;

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating TypeInfoExpr: "+this);
		code.setLinePos(this);
		cl_expr.generate(code,null);
		JENode[] cl_args = this.cl_args;
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
		Struct ti_clazz = type.getStruct();
		if (ti_clazz == null || ti_clazz.typeinfo_clazz == null)
			ti_clazz = (Struct)Type.tpTypeInfo.tdecl;
		else
			ti_clazz = ti_clazz.typeinfo_clazz;
		Method func = ti_clazz.resolveMethod("newTypeInfo", ti_clazz.xtype, Type.tpClass, new ArrayType(Type.tpTypeInfo));
		code.addInstr(op_call,(JMethod)func,false,ti_clazz.xtype);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

public view JAssignExpr of AssignExpr extends JENode {
	public:ro	Operator		op;
	public:ro	JENode			lval;
	public:ro	JENode			value;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		DNode d = this.dnode;
		if !(d instanceof Method && d.body instanceof CoreExpr) {
			Kiev.reportError(this, "Unresolved core operation "+op+" at generatioin phase");
			return;
		}
		code.setLinePos(this);
		CoreExpr m = (CoreExpr)((Method)d).body;
		m.bend_func.generate(code,reqType,this);
	}
}

public view JBinaryExpr of BinaryExpr extends JENode {
	public:ro	Operator	op;
	public:ro	JENode		expr1;
	public:ro	JENode		expr2;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BinaryExpr: "+this);
		DNode d = this.dnode;
		if !(d instanceof Method && d.body instanceof CoreExpr) {
			Kiev.reportError(this, "Unresolved core operation "+op+" at generatioin phase");
			return;
		}
		code.setLinePos(this);
		CoreExpr m = (CoreExpr)((Method)d).body;
		m.bend_func.generate(code,reqType,this);
	}

}

public view JUnaryExpr of UnaryExpr extends JENode {
	public:ro	Operator			op;
	public:ro	JENode			expr;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating UnaryExpr: "+this);
		DNode d = this.dnode;
		if !(d instanceof Method && d.body instanceof CoreExpr) {
			Kiev.reportError(this, "Unresolved core operation "+op+" at generatioin phase");
			return;
		}
		code.setLinePos(this);
		CoreExpr m = (CoreExpr)((Method)d).body;
		m.bend_func.generate(code,reqType,this);
	}

}

public view JStringConcatExpr of StringConcatExpr extends JENode {
	public:ro	JENode[]			args;

	public static Struct clazzStringBuffer;
	public static Method clazzStringBufferToString;
	public static Method clazzStringBufferInit;

	static {
		try {
		clazzStringBuffer = (Struct)Env.loadTypeDecl("java.lang.StringBuffer");
		if( clazzStringBuffer == null )
			throw new RuntimeException("Core class java.lang.StringBuffer not found");
		clazzStringBufferToString = clazzStringBuffer.resolveMethod("toString",Type.tpString);
		clazzStringBufferInit = clazzStringBuffer.resolveMethod("<init>",Type.tpVoid);
		} catch(Exception e ) {
			throw new RuntimeException("Can't initialize: "+e.getMessage());
		}
	}

	public JMethod getMethodFor(JENode expr) {
		Method m = clazzStringBuffer.resolveMethod("append",clazzStringBuffer.xtype,expr.getType());
		return (JMethod)m;
	}


	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		code.setLinePos(this);
		JENode[] args = this.args;
		code.addInstr(op_new,clazzStringBuffer.xtype);
		code.addInstr(op_dup);
		code.addInstr(op_call,(JMethod)clazzStringBufferInit,true);
		for(int i=0; i < args.length; i++) {
			args[i].generate(code,null);
			code.addInstr(op_call,getMethodFor(args[i]),false);
		}
		code.addInstr(op_call,(JMethod)clazzStringBufferToString,false);
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

public view JCommaExpr of CommaExpr extends JENode {
	public:ro	JENode[]			exprs;

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		JENode[] exprs = this.exprs;
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1 )
				exprs[i].generate(code,Type.tpVoid);
			else
				exprs[i].generate(code,reqType);
		}
	}
}

public view JBlock of Block extends JENode {
	public:ro	JNode[]			stats;
	public		CodeLabel		break_label;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating Block");
		code.setLinePos(this);
		JNode[] stats = this.stats;
		break_label = code.newLabel();
		for(int i=0; i < stats.length; i++) {
			try {
				JNode st = stats[i];
				if (st instanceof JENode) {
					if (i < stats.length-1 || isGenVoidExpr())
						st.generate(code,Type.tpVoid);
					else
						st.generate(code,reqType);
				}
				else if (st instanceof JVar || st instanceof JDeclGroup) {
					st.generate(code,Type.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(stats[i],e);
			}
		}
		for(int j=stats.length-1; j >= 0; j--) {
			JNode n = stats[j];
			if (n instanceof JVar)
				((JVar)n).removeVar(code);
			else if (n instanceof JDeclGroup)
				((JDeclGroup)n).removeVars(code);
		}
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

public final view JIncrementExpr of IncrementExpr extends JENode {
	public:ro	Operator			op;
	public:ro	JENode			lval;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		DNode d = this.dnode;
		if !(d instanceof Method && d.body instanceof CoreExpr) {
			Kiev.reportError(this, "Unresolved core operation "+op+" at generatioin phase");
			return;
		}
		code.setLinePos(this);
		CoreExpr m = (CoreExpr)((Method)d).body;
		m.bend_func.generate(code,reqType,this);
	}
}

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
			code.addInstr(Instr.set_label,endLabel);
			if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
		}
	}
}

public view JCastExpr of CastExpr extends JENode {
	public:ro	JENode			expr;
	public:ro	Type			type;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CastExpr: "+this);
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

