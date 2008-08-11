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

@ViewOf(vcast=true, iface=true)
public final view JCallExpr of CallExpr extends JENode {

	public:ro	JMethod			func;
	public:ro	JENode			obj;
	public:ro	JENode[]		args;

	public final CallType getCallType();

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !code.jtenv.getJType(ot).isInstanceOf(func.jctx_tdecl.getJType(code.jtenv)) ) {
			trace( Kiev.debug && Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			code.addInstr(Instr.op_checkcast,func.jctx_tdecl.xtype);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		code.setLinePos(this);
		JMethod func = this.func;
		if (((Method)func).body instanceof CoreExpr) {
			CoreExpr m = (CoreExpr)((Method)func).body;
			m.bend_func.generate(code,reqType,this);
			return;
		}
		MetaAccess.verifyRead(this,func);
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
		JENode[] args = this.args;
		int i = 0;
		if (func.isRuleMethod()) {
			ENode env_arg = (ENode)CallExpr.RULE_ENV_ARG.get((CallExpr)this);
			if (env_arg != null)
				((JENode)env_arg).generate(code,null);
		}
		if !(func.isVarArgs()) {
			for(; i < args.length; i++)
				args[i].generate(code,null);
		} else {
			int N = func.params.length-1;
			for(; i < N; i++)
				args[i].generate(code,null);
			Type tn = func.params[N].vtype;
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
			foreach (ENode earg; CallExpr.TI_EXT_ARG.iterate((CallExpr)this))
				((JENode)earg).generate(code,null);
		}

		// Now, do the call instruction 		
		code.addInstr(op_call,func,isSuperExpr(),obj.getType());
		if( null_cast_label != null ) {
			code.stack_pop();
			code.stack_push(code.jenv.getJTypeEnv().tpNull);
			code.addInstr(Instr.set_label,null_cast_label);
		}
		if( func.mtype.ret() ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && getType().isReference()
			 && (!func.etype.ret().isInstanceOf(getType().getErasedType()) || null_cast_label != null) )
				code.addInstr(op_checkcast,getType());
		}
	}

}


@ViewOf(vcast=true, iface=true)
public final view JCtorCallExpr of CtorCallExpr extends JENode {

	public:ro	JMethod			func;
	public:ro	JENode			obj;
	public:ro	JENode			tpinfo;
	public:ro	JENode[]		args;

	public final CallType getCallType();

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating CtorCallExpr: "+this);
		code.setLinePos(this);
		JMethod func = this.func;
		if (((Method)func).body instanceof CoreExpr) {
			CoreExpr m = (CoreExpr)((Method)func).body;
			m.bend_func.generate(code,reqType,this);
			return;
		}
		MetaAccess.verifyRead(this,func);
		// load this/super
		obj.generate(code,null);
		JENode[] args = this.args;
		int i = 0;
		if (func.getOuterThisParam() != null) {
			JVar fp = code.method.getOuterThisParam();
			if (fp == null) {
				Kiev.reportError(this, "Cannot find outer this parameter");
				code.addNullConst();
			} else {
				code.addInstr(Instr.op_load,fp);
			}
		}
		if (func.getTypeInfoParam(Var.PARAM_TYPEINFO) != null) {
			JMethod jmm = jctx_method;
			Type tp;
			if (!jmm.jctx_tdecl.equals(func.jctx_tdecl))
				tp = ((TypeDecl)jctx_tdecl).super_types[0].getType();
			else
				tp = ((TypeDecl)jctx_tdecl).xtype;
			assert(jmm.isConstructor() && !jmm.isStatic());
			assert(tp.getStruct().isTypeUnerasable());
			// Insert our-generated typeinfo, or from childs class?
			tpinfo.generate(code,null);
		}
		if (func.jparent instanceof JStruct && ((JStruct)func.jparent).isEnum()) {
			assert (!func.isTypeUnerasable());
			// enum field name & ordinal
			foreach (ENode earg; CtorCallExpr.ENUM_EXT_ARG.iterate((CtorCallExpr)this))
				((JENode)earg).generate(code,null);
		}
		if !(func.isVarArgs()) {
			for(; i < args.length; i++)
				args[i].generate(code,null);
		} else {
			int N = func.params.length-1;
			for(; i < N; i++)
				args[i].generate(code,null);
			Type tn = func.params[N].vtype;
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
			foreach (ENode earg; CtorCallExpr.TI_EXT_ARG.iterate((CtorCallExpr)this))
				((JENode)earg).generate(code,null);
		}

		// Now, do the call instruction 		
		code.addInstr(op_call,func,true,Type.tpVoid);
	}

}


@ViewOf(vcast=true, iface=true)
public final view JClosureCallExpr of ClosureCallExpr extends JENode {
	public:ro JENode		expr;
	public:ro JENode[]		args;
	public:ro Boolean		is_a_call;
	
	@virtual @abstract
	public:ro CallType		xtype;
	
	@getter public final CallType	get$xtype() { return (CallType)((ClosureCallExpr)this).expr.getType(); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this);
		// Load ref to closure
		expr.generate(code,null);
		CallType xtype = this.xtype;
		JENode[] args = this.args;
		// Clone it
		if( args.length > 0 ) {
			JMethod clone_it = ((JStruct)Type.tpClosureClazz).resolveMethod(code.jenv,nameClone,KString.from("()Ljava/lang/Object;"));
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,Type.tpClosureClazz.xtype);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(code.jenv,code.jtenv.getJType(xtype.arg(i))),false);
			}
		}
		JMethod call_it = getCallIt(code,xtype);
		// Check if we need to call
		if( is_a_call.booleanValue() ) {
			code.addInstr(op_call,call_it,false);
		}
		if( call_it.mtype.ret() ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && call_it.mtype.ret().isReference()
			 && ( !call_it.etype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public JMethod getCallIt(Code code, CallType tp) {
		String call_it_name;
		KString call_it_sign;
		if( tp.ret().isReference() ) {
			call_it_name = "call_Object";
			call_it_sign = KString.from("()Ljava/lang/Object;");
		} else {
			call_it_name = ("call_"+tp.ret()).intern();
			call_it_sign = KString.from("()"+code.jtenv.getJType(tp.ret()).java_signature);
		}
		return ((JStruct)Type.tpClosureClazz).resolveMethod(code.jenv, call_it_name, call_it_sign);
	}
	
	static final KString sigZ = KString.from("(Z)Lkiev/stdlib/closure;");
	static final KString sigC = KString.from("(C)Lkiev/stdlib/closure;");
	static final KString sigB = KString.from("(B)Lkiev/stdlib/closure;");
	static final KString sigS = KString.from("(S)Lkiev/stdlib/closure;");
	static final KString sigI = KString.from("(I)Lkiev/stdlib/closure;");
	static final KString sigJ = KString.from("(J)Lkiev/stdlib/closure;");
	static final KString sigF = KString.from("(F)Lkiev/stdlib/closure;");
	static final KString sigD = KString.from("(D)Lkiev/stdlib/closure;");
	static final KString sigObj = KString.from("(Ljava/lang/Object;)Lkiev/stdlib/closure;");
	public JMethod getMethodFor(JEnv jenv, JType tp) {
		KString sig = null;
		switch(tp.java_signature.byteAt(0)) {
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
		JMethod m = ((JStruct)Type.tpClosureClazz).resolveMethod(jenv,"addArg",sig);
		if( m == null )
			Kiev.reportError(expr,"Unknown method for kiev.vlang.closure");
		return m;
	}

}


