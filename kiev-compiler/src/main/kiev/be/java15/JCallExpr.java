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

public final class JCallExpr extends JENode {

	@virtual typedef VT  ≤ CallExpr;

	public static JCallExpr attach(CallExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCallExpr)jn;
		return new JCallExpr(impl);
	}
	
	protected JCallExpr(CallExpr impl) {
		super(impl);
	}
	
	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		CallExpr vn = vn();
		Method func = vn.func;
		JTypeDecl jctx_tdecl = (JTypeDecl)Env.ctxTDecl(func);
		Type ot = vn.obj.getType(code.env);
		if( !code.jtenv.getJType(ot).isInstanceOf(jctx_tdecl.getJType(code.jtenv)) ) {
			trace( Kiev.debug && Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			code.addInstr(Instr.op_checkcast,jctx_tdecl.getType());
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		code.setLinePos(this);
		CallExpr vn = vn();
		Method func = vn.func;
		JENode obj = (JENode)vn.obj;
		JENode[] args = JNode.toJArray<JENode>(vn.args);
		if (func instanceof CoreOperation) {
			CoreOperation cm = (CoreOperation)func;
			((BEndFunc)cm.bend_func).generate(code,reqType,this);
			return;
		}
		MetaAccess.verifyRead(vn,func);
		CodeLabel null_cast_label = null;
		if !(obj instanceof JTypeRef) {
			obj.generate(code,null);
			if (isCastCall()) {
				null_cast_label = code.newLabel();
				code.addInstr(Instr.op_dup);
				code.addInstr(Instr.op_ifnull, null_cast_label);
			}
			generateCheckCastIfNeeded(code);
		}
		else if( !func.isStatic() ) {
			if( !code.method.isStatic() )
				code.addInstrLoadThis();
			else
				throw new RuntimeException("Non-static method "+func+" is called from static method "+code.method);
		}
		int i = 0;
		if (func.isRuleMethod()) {
			ArgExpr env_arg = vn.getHiddenArg(Var.PARAM_RULE_ENV);
			if (env_arg != null)
				((JENode)env_arg.expr).generate(code,null);
		}
		if !(func.isVarArgs()) {
			for(; i < args.length; i++)
				args[i].generate(code,null);
		} else {
			int N = func.params.length-1;
			for(; i < N; i++)
				args[i].generate(code,null);
			Type tn = func.params[N].vtype.getType(code.env);
			Type varg_tp = tn.resolveArg(0);
			if (args.length == func.params.length && args[N].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].generate(code,null);
			} else {
				code.addConst(args.length-N);
				code.addInstr(Instr.op_newarray,varg_tp);
				for(int j=0; i < args.length; i++, j++) {
					code.addInstr(Instr.op_dup);
					code.addConst(j);
					args[i].generate(code,null);
					code.addInstr(Instr.op_arr_store);
				}
			}
		}
		if (func.isTypeUnerasable()) {
			foreach (ArgExpr earg; vn.hargs; earg.var.kind >= Var.PARAM_METHOD_TYPEINFO)
				((JENode)earg.expr).generate(code,null);
		}

		// Now, do the call instruction
		boolean is_super = (obj instanceof JSuperExpr);
		code.addInstr(op_call,(JMethod)func,is_super,obj.getType());
		if( null_cast_label != null ) {
			code.stack_pop();
			code.stack_push(code.jenv.getJTypeEnv().tpNull);
			code.addInstr(Instr.set_label,null_cast_label);
		}
		if( func.mtype.ret() ≢ code.tenv.tpVoid ) {
			if( reqType ≡ code.tenv.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && getType().isReference()
			 && (!func.etype.ret().isInstanceOf(getType().getErasedType()) || null_cast_label != null) )
				code.addInstr(op_checkcast,getType());
		}
	}

}


public final class JCtorCallExpr extends JENode {

	@virtual typedef VT  ≤ CtorCallExpr;

	public static JCtorCallExpr attach(CtorCallExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JCtorCallExpr)jn;
		return new JCtorCallExpr(impl);
	}
	
	protected JCtorCallExpr(CtorCallExpr impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CtorCallExpr: "+this);
		code.setLinePos(this);
		CtorCallExpr vn = vn();
		Method func = vn.func;
		JENode obj = (JENode)vn.obj;
		JENode[] args = JNode.toJArray<JENode>(vn.args);
		if (func instanceof CoreOperation) {
			CoreOperation cm = (CoreOperation)func;
			((BEndFunc)cm.bend_func).generate(code,reqType,this);
			return;
		}
		MetaAccess.verifyRead(vn,func);
		// load this/super
		obj.generate(code,null);
		int i = 0;
		if (func.getOuterThisParam() != null) {
			JVar fp = code.method.getOuterThisParam();
			if (fp == null) {
				Kiev.reportError(vn, "Cannot find outer this parameter");
				code.addNullConst();
			} else {
				code.addInstr(Instr.op_load,fp);
			}
		}
		if (func.getClassTypeInfoParam() != null) {
			JENode tpinfo = (JENode)vn.tpinfo;
			tpinfo.generate(code,null);
		}
		if (func.parent() instanceof Struct && ((Struct)func.parent()).isEnum()) {
			assert (!func.isTypeUnerasable());
			ArgExpr enum_name = vn.getHiddenArg(Var.PARAM_ENUM_NAME);
			((JENode)enum_name.expr).generate(code,null);
			ArgExpr enum_ord = vn.getHiddenArg(Var.PARAM_ENUM_ORD);
			((JENode)enum_ord.expr).generate(code,null);
		}
		if !(func.isVarArgs()) {
			for(; i < args.length; i++)
				args[i].generate(code,null);
		} else {
			int N = func.params.length-1;
			for(; i < N; i++)
				args[i].generate(code,null);
			Type tn = func.params[N].vtype.getType(code.env);
			Type varg_tp = tn.resolveArg(0);
			if (args.length == func.params.length && args[N].getType().isInstanceOf(new ArrayType(varg_tp))) {
				// array as va_arg
				args[i].generate(code,null);
			} else {
				code.addConst(args.length-N);
				code.addInstr(Instr.op_newarray,varg_tp);
				for(int j=0; i < args.length; i++, j++) {
					code.addInstr(Instr.op_dup);
					code.addConst(j);
					args[i].generate(code,null);
					code.addInstr(Instr.op_arr_store);
				}
			}
		}
		if (func.isTypeUnerasable()) {
			foreach (ArgExpr earg; vn.hargs; earg.var.kind >= Var.PARAM_METHOD_TYPEINFO)
				((JENode)earg.expr).generate(code,null);
		}

		// Now, do the call instruction 		
		code.addInstr(op_call,(JMethod)func,true,code.tenv.tpVoid);
	}

}


public final class JClosureCallExpr extends JENode {

	@virtual typedef VT  ≤ ClosureCallExpr;

	public static JClosureCallExpr attach(ClosureCallExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JClosureCallExpr)jn;
		return new JClosureCallExpr(impl);
	}
	
	protected JClosureCallExpr(ClosureCallExpr impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this);
		ClosureCallExpr vn = vn();
		JENode expr = (JENode)vn.expr;
		JENode[] args = JNode.toJArray<JENode>(vn.args);
		// Load ref to closure
		expr.generate(code,null);
		CallType ctype = (CallType)expr.getType();
		// Clone it
		if( args.length > 0 ) {
			JMethod clone_it = ((JStruct)code.tenv.tpClosure.getStruct()).resolveMethod(code.jenv,nameClone,"()Ljava/lang/Object;");
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,code.tenv.tpClosure);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(code.jenv,code.jtenv.getJType(ctype.arg(i))),false);
			}
		}
		JMethod call_it = getCallIt(code,ctype);
		// Check if we need to call
		if( vn.isACall() ) {
			code.addInstr(op_call,call_it,false);
		}
		if( call_it.mtype.ret() ≢ code.tenv.tpVoid ) {
			if( reqType ≡ code.tenv.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && call_it.mtype.ret().isReference()
			 && ( !call_it.etype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public JMethod getCallIt(Code code, CallType tp) {
		String call_it_name;
		String call_it_sign;
		if( tp.ret().isReference() ) {
			call_it_name = "call_Object";
			call_it_sign = "()Ljava/lang/Object;";
		} else {
			call_it_name = ("call_"+tp.ret()).intern();
			call_it_sign = "()"+code.jtenv.getJType(tp.ret()).java_signature;
		}
		return ((JStruct)code.tenv.tpClosure.getStruct()).resolveMethod(code.jenv, call_it_name, call_it_sign);
	}
	
	static final String sigZ = "(Z)Lkiev/stdlib/closure;";
	static final String sigC = "(C)Lkiev/stdlib/closure;";
	static final String sigB = "(B)Lkiev/stdlib/closure;";
	static final String sigS = "(S)Lkiev/stdlib/closure;";
	static final String sigI = "(I)Lkiev/stdlib/closure;";
	static final String sigJ = "(J)Lkiev/stdlib/closure;";
	static final String sigF = "(F)Lkiev/stdlib/closure;";
	static final String sigD = "(D)Lkiev/stdlib/closure;";
	static final String sigObj = "(Ljava/lang/Object;)Lkiev/stdlib/closure;";
	public JMethod getMethodFor(JEnv jenv, JType tp) {
		String sig = null;
		switch(tp.java_signature.charAt(0)) {
		case 'B': sig = sigB; break;
		case 'S': sig = sigS; break;
		case 'I': sig = sigI; break;
		case 'J': sig = sigJ; break;
		case 'Z': sig = sigZ; break;
		case 'C': sig = sigC; break;
		case 'F': sig = sigF; break;
		case 'D': sig = sigD; break;
		case '[':
		case 'L':
		case 'A':
		case '&':
		case 'R': sig = sigObj; break;
		}
		JMethod m = ((JStruct)jenv.vtypes.tpClosure.getStruct()).resolveMethod(jenv,"addArg",sig);
		if( m == null )
			Kiev.reportError(vn().expr,"Unknown method for kiev.vlang.closure");
		return m;
	}

}


