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

public final class JShadow extends JENode {

	@virtual typedef VT  ≤ Shadow;

	public static JShadow attach(Shadow impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JShadow)jn;
		return new JShadow(impl);
	}
	
	protected JShadow(Shadow impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		JNode rnode = JNode.attachJNode(vn().rnode);
		if (rnode instanceof JENode) {
			((JENode)rnode).generate(code,reqType);
		} else {
			((JInitializer)rnode).generate(code,reqType);
		}
	}
	
}

public final class JTypeClassExpr extends JENode {

	@virtual typedef VT  ≤ TypeClassExpr;

	public static JTypeClassExpr attach(TypeClassExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JTypeClassExpr)jn;
		return new JTypeClassExpr(impl);
	}
	
	protected JTypeClassExpr(TypeClassExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating TypeClassExpr: "+this);
		code.setLinePos(this);
		Type ttype = vn().ttype.getType(code.env);
		code.addConst(code.jtenv.getJType(ttype.getErasedType()));
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
	}

}

public final class JTypeInfoExpr extends JENode {

	@virtual typedef VT  ≤ TypeInfoExpr;

	public static JTypeInfoExpr attach(TypeInfoExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JTypeInfoExpr)jn;
		return new JTypeInfoExpr(impl);
	}
	
	protected JTypeInfoExpr(TypeInfoExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType ) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating TypeInfoExpr: "+this);
		TypeInfoExpr vn = vn();
		JENode cl_expr = (JENode)vn.cl_expr;
		JENode[] cl_args = JNode.toJArray<JENode>(vn.cl_args);
		code.setLinePos(this);
		cl_expr.generate(code,null);
		if (cl_args.length > 0) { 
			code.addConst(cl_args.length);
			code.addInstr(Instr.op_newarray,code.tenv.tpTypeInfo);
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
		Type ttype = vn.ttype.getType(code.env);
		Struct ti_clazz = ttype.getStruct();
		if (ti_clazz == null || ti_clazz.typeinfo_clazz == null)
			ti_clazz = (Struct)code.tenv.tpTypeInfo.tdecl;
		else
			ti_clazz = ti_clazz.typeinfo_clazz;
		Method func = ti_clazz.resolveMethod(code.env, "newTypeInfo", ti_clazz.getType(code.env), code.tenv.tpClass, new ArrayType(code.tenv.tpTypeInfo));
		code.addInstr(op_call,(JMethod)func,false,ti_clazz.getType(code.env));
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
	}

}

public final class JAssignExpr extends JENode {

	@virtual typedef VT  ≤ AssignExpr;

	public static JAssignExpr attach(AssignExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JAssignExpr)jn;
		return new JAssignExpr(impl);
	}
	
	protected JAssignExpr(AssignExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating AssignExpr: "+this);
		code.setLinePos(this);
		DNode d = vn().dnode;
		if (d instanceof CoreOperation) {
			((BEndFunc)d.bend_func).generate(code,reqType,this);
		} else {
			Kiev.reportError(vn(), "Unresolved core operation "+d+" at generatioin phase");
		}
	}
}

public final class JModifyExpr extends JENode {

	@virtual typedef VT  ≤ ModifyExpr;

	public static JModifyExpr attach(ModifyExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JModifyExpr)jn;
		return new JModifyExpr(impl);
	}
	
	protected JModifyExpr(ModifyExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ModifyExpr: "+this);
		code.setLinePos(this);
		DNode d = vn().dnode;
		if (d instanceof CoreOperation) {
			((BEndFunc)d.bend_func).generate(code,reqType,this);
		} else {
			Kiev.reportError(vn(), "Unresolved core operation "+d+" at generatioin phase");
		}
	}
}

public final class JBinaryExpr extends JENode {

	@virtual typedef VT  ≤ BinaryExpr;

	public static JBinaryExpr attach(BinaryExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBinaryExpr)jn;
		return new JBinaryExpr(impl);
	}
	
	protected JBinaryExpr(BinaryExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating BinaryExpr: "+this);
		code.setLinePos(this);
		DNode d = vn().dnode;
		if (d instanceof CoreOperation) {
			((BEndFunc)d.bend_func).generate(code,reqType,this);
		} else {
			Kiev.reportError(vn(), "Unresolved core operation "+d+" at generatioin phase");
		}
	}

}

public final class JUnaryExpr extends JENode {

	@virtual typedef VT  ≤ UnaryExpr;

	public static JUnaryExpr attach(UnaryExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JUnaryExpr)jn;
		return new JUnaryExpr(impl);
	}
	
	protected JUnaryExpr(UnaryExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating UnaryExpr: "+this);
		code.setLinePos(this);
		DNode d = vn().dnode;
		if (d instanceof CoreOperation) {
			((BEndFunc)d.bend_func).generate(code,reqType,this);
		} else {
			Kiev.reportError(vn(), "Unresolved core operation "+d+" at generatioin phase");
		}
	}

}

public final class JStringConcatExpr extends JENode {

	@virtual typedef VT  ≤ StringConcatExpr;

	public static JStringConcatExpr attach(StringConcatExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JStringConcatExpr)jn;
		return new JStringConcatExpr(impl);
	}
	
	protected JStringConcatExpr(StringConcatExpr impl) {
		super(impl);
	}

	private JMethod getMethodFor(JEnv jenv, JENode expr) {
		Method m = jenv.getClsStringBuffer().resolveMethod(jenv.env,"append",jenv.getClsStringBuffer().getType(jenv.env),expr.getType());
		return (JMethod)m;
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating StringConcatExpr: "+this);
		code.setLinePos(this);
		StringConcatExpr vn = vn();
		JENode[] args = JNode.toJArray<JENode>(vn.args);
		code.addInstr(op_new,code.jenv.getClsStringBuffer().getType(code.env));
		code.addInstr(op_dup);
		code.addInstr(op_call,(JMethod)code.jenv.getMthStringBufferInit(),true);
		for(int i=0; i < args.length; i++) {
			args[i].generate(code,null);
			code.addInstr(op_call,getMethodFor(code.jenv,args[i]),false);
		}
		code.addInstr(op_call,(JMethod)code.jenv.getMthStringBufferToString(),false);
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
	}

}

public final class JCommaExpr extends JENode {

	@virtual typedef VT  ≤ CommaExpr;

	public static JCommaExpr attach(CommaExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCommaExpr)jn;
		return new JCommaExpr(impl);
	}
	
	protected JCommaExpr(CommaExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		JENode[] exprs = JNode.toJArray<JENode>(vn().exprs);
		for(int i=0; i < exprs.length; i++) {
			if( i < exprs.length-1 )
				exprs[i].generate(code,code.tenv.tpVoid);
			else
				exprs[i].generate(code,reqType);
		}
	}
}

public class JBlock extends JENode {

	@virtual typedef VT  ≤ Block;

	public static JBlock attach(Block impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JBlock)jn;
		if (impl instanceof SwitchStat)
			return JSwitchStat.attach((SwitchStat)impl);
		return new JBlock(impl);
	}
	
	protected JBlock(Block impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\tgenerating Block");
		code.setLinePos(this);
		generateStats(code, reqType);
	}
	public void generateStats(Code code, Type reqType) {
		Block vn = vn();
		JNode[] stats = JNode.toJArray<JNode>(vn.stats);
		JCaseLabel jcase = null;
		for(int i=0; i < stats.length; i++) {
			try {
				JNode st = stats[i];
				if (st instanceof JCaseLabel) {
					if (jcase != null)
						jcase.removeVars(code);
					jcase = (JCaseLabel)st;
					jcase.generate(code,code.tenv.tpVoid);
				}
				else if (st instanceof JENode) {
					if (i < stats.length-1 || isGenVoidExpr())
						st.generate(code,code.tenv.tpVoid);
					else
						st.generate(code,reqType);
				}
				else if (st instanceof JVar) {
					st.generate(code,code.tenv.tpVoid);
				}
			} catch(Exception e ) {
				Kiev.reportError(stats[i].vn(),e);
			}
		}
		if (jcase != null)
			jcase.removeVars(code);
		for(int j=stats.length-1; j >= 0; j--) {
			JNode n = stats[j];
			if (n instanceof JVar)
				((JVar)n).removeVar(code);
		}
		JNode p = this.jparent;
		if( p instanceof JMethod && Kiev.debugOutputC && code.need_to_gen_post_cond && p.mtype.ret() ≢ code.tenv.tpVoid)
			code.stack_push(code.jtenv.getJType(p.etype.ret()));
		JLabel lblbrk = (JLabel)vn.lblbrk;
		if (lblbrk != null)
			lblbrk.generate(code,null);
	}

	public JLabel getBrkLabel() {
		return (JLabel)vn().lblbrk;
	}

}

public final class JIncrementExpr extends JENode {

	@virtual typedef VT  ≤ IncrementExpr;

	public static JIncrementExpr attach(IncrementExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JIncrementExpr)jn;
		return new JIncrementExpr(impl);
	}
	
	protected JIncrementExpr(IncrementExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating IncrementExpr: "+this);
		code.setLinePos(this);
		DNode d = vn().dnode;
		if (d instanceof CoreOperation) {
			((BEndFunc)d.bend_func).generate(code,reqType,this);
		} else {
			Kiev.reportError(vn(), "Unresolved core operation "+d+" at generatioin phase");
		}
	}
}

public final class JConditionalExpr extends JENode {

	@virtual typedef VT  ≤ ConditionalExpr;

	public static JConditionalExpr attach(ConditionalExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JConditionalExpr)jn;
		return new JConditionalExpr(impl);
	}
	
	protected JConditionalExpr(ConditionalExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		code.setLinePos(this);
		ConditionalExpr vn = vn();
		JENode cond = (JENode)vn.cond;
		JENode expr1 = (JENode)vn.expr1;
		JENode expr2 = (JENode)vn.expr2;
		if( cond.isConstantExpr(code.env) ) {
			if( ((Boolean)cond.getConstValue(code.env)).booleanValue() ) {
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
			if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
		}
	}
}

public final class JCastExpr extends JENode {

	@virtual typedef VT  ≤ CastExpr;

	public static JCastExpr attach(CastExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCastExpr)jn;
		return new JCastExpr(impl);
	}
	
	protected JCastExpr(CastExpr impl) {
		super(impl);
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CastExpr: "+this);
		code.setLinePos(this);
		CastExpr vn = vn();
		ENode expr = vn.expr;
		((JENode)expr).generate(code,null);
		Type ctype = vn.ctype.getType(code.env);
		Type t = expr.getType(code.env);
		if( t.isReference() ) {
			if( !ctype.isReference() )
				Kiev.reportError(vn,"Expression "+expr+" of type "+t+" cannot be casted to type "+ctype);
			code.addInstr(Instr.op_checkcast,ctype);
		} else {
			code.addInstr(Instr.op_x2y,ctype);
		}
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
	}

}

