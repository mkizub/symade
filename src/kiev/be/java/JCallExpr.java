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

import kiev.vlang.CallExpr.CallExprImpl;
import kiev.vlang.ClosureCallExpr.ClosureCallExprImpl;

@nodeview
public final view JCallExprView of CallExprImpl extends JENodeView {
	public access:ro JENodeView		obj;
	public access:ro JMethodView	func;
	public           JENodeView		temp_expr;

	@getter public final JENodeView[]		get$args()	{ return (JENodeView[])this.$view.args.toJViewArray(JENodeView.class); }
	
	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf(func.jctx_clazz.getStruct()) ) {
			trace( Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			code.addInstr(Instr.op_checkcast,func.jctx_clazz.type);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		code.setLinePos(this);
		func.acc.verifyReadAccess(this,func);
		CodeLabel ok_label = null;
		CodeLabel null_cast_label = null;
		if( func.jctx_clazz.type.isInstanceOf(Type.tpDebug) ) {
			String fname = func.name.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
			if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
		}
		if !(obj instanceof JTypeRefView) {
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
		JENodeView[] args = this.args;
		int i = 0;
		if( func.isRuleMethod() ) {
			// Very special case for rule call from inside of RuleMethod
			JNodeView p = (JNodeView)this.jparent;
			if (p instanceof JAssignExprView
				&& ((JAssignExprView)p).op == AssignOperator.Assign
				&& ((JAssignExprView)p).lval.getType() ≡ Type.tpRule
				)
				((JAssignExprView)p).lval.generate(code,null);
			else
				code.addNullConst();
		}
		else if( func.jctx_clazz.type.isInstanceOf(Type.tpDebug) ) {
			int mode = 0;
			String fname = func.name.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 ) mode = 1;
			else if( fname.indexOf("trace") >= 0 ) mode = 2;
			if( mode > 0 && args.length > 0 && args[0].getType().isBoolean() ) {
				ok_label = code.newLabel();
				if( args[0] instanceof IBoolExpr ) {
					if( mode == 1 ) ((IBoolExpr)args[0]).generate_iftrue(code,ok_label);
					else ((IBoolExpr)args[0]).generate_iffalse(code,ok_label);
				} else {
					args[0].generate(code,null);
					if( mode == 1 ) code.addInstr(Instr.op_ifne,ok_label);
					else code.addInstr(Instr.op_ifeq,ok_label);
				}
				if( mode == 1 )
					code.addConst(0);
				else
					code.addConst(1);
				i++;
			}
		}
		else {
			if( func.name.equals(nameInit) && func.getOuterThisParam() != null) {
				JVarView fp = code.method.getOuterThisParam();
				if (fp == null) {
					Kiev.reportError(this, "Cannot find outer this parameter");
					code.addNullConst();
				} else {
					code.addInstr(Instr.op_load,fp);
				}
			}
			if( func.name.equals(nameInit) && func.getTypeInfoParam() != null) {
				JMethodView mmm = jctx_method;
				Type tp = !mmm.jctx_clazz.equals(func.jctx_clazz) ? jctx_clazz.super_type : jctx_clazz.type;
				assert(mmm.name.equals(nameInit));
				assert(tp.getStruct().isTypeUnerasable());
				// Insert our-generated typeinfo, or from childs class?
				if (mmm.getTypeInfoParam() != null)
					temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam().getVar()).getJENodeView();
				else
					temp_expr = jctx_clazz.accessTypeInfoField(this,tp);
				temp_expr.generate(code,null);
				temp_expr = null;
			}
		}
		if !(func.isVarArgs()) {
			for(; i < args.length; i++)
				args[i].generate(code,null);
		} else {
			int N = func.params.length-1;
			for(; i < N; i++)
				args[i].generate(code,null);
			if (args.length == func.params.length && args[N].getType().isInstanceOf(func.params[N].type)) {
				// array as va_arg
				args[i].generate(code,null);
			} else {
				ArrayType type = (ArrayType)func.jtype.args[N];
				code.addConst(args.length-N);
				code.addInstr(Instr.op_newarray,type.arg);
				for(int j=0; i < args.length; i++, j++) {
					code.addInstr(Instr.op_dup);
					code.addConst(j);
					args[i].generate(code,null);
					code.addInstr(Instr.op_arr_store);
				}
			}
		}
		
		// Special meaning of Object.equals and so on
		// for parametriezed with primitive types classes
		Type objt = obj.getType();
		if( !objt.isReference() ) {
			if( func.jctx_clazz.type ≢ Type.tpObject )
				Kiev.reportError(this,"Call to unknown method "+func+" of type "+objt);
			if( func.name == nameObjEquals ) {
				CodeLabel label_true = code.newLabel();
				CodeLabel label_false = code.newLabel();
				code.addInstr(Instr.op_ifcmpeq,label_true);
				code.addConst(0);
				code.addInstr(Instr.op_goto,label_false);
				code.addInstr(Instr.set_label,label_true);
				code.addConst(1);
				code.addInstr(Instr.set_label,label_false);
			}
			else if( func.name == nameObjGetClass ) {
				BaseType reft = Type.getRefTypeForPrimitive(objt);
				Field f = reft.clazz.resolveField(KString.from("TYPE"));
				code.addInstr(Instr.op_pop);
				code.addInstr(Instr.op_getstatic,f.getJFieldView(),reft);
			}
			else if( func.name == nameObjHashCode ) {
				switch(objt.getJType().java_signature.byteAt(0)) {
				case 'Z':
					{
					CodeLabel label_true = code.newLabel();
					CodeLabel label_false = code.newLabel();
					code.addInstr(Instr.op_ifne,label_true);
					code.addConst(1237);
					code.addInstr(Instr.op_goto,label_false);
					code.addInstr(Instr.set_label,label_true);
					code.addConst(1231);
					code.addInstr(Instr.set_label,label_false);
					}
					break;
				case 'B':
				case 'S':
				case 'I':
				case 'C':
					// the value is hashcode itself
					break;
				case 'J':
					code.addInstr(Instr.op_dup);
					code.addConst(32);
					code.addInstr(Instr.op_shl);
					code.addInstr(Instr.op_xor);
					code.addInstr(Instr.op_x2y,Type.tpInt);
					break;
				case 'F':
					{
					JMethodView m = Type.tpFloatRef.getJStruct().resolveMethod(
						KString.from("floatToIntBits"),
						KString.from("(F)I")
						);
					code.addInstr(op_call,m,false);
					}
					break;
				case 'D':
					{
					JMethodView m = Type.tpDoubleRef.getJStruct().resolveMethod(
						KString.from("doubleToLongBits"),
						KString.from("(D)J")
						);
					code.addInstr(op_call,m,false);
					code.addInstr(Instr.op_dup);
					code.addConst(32);
					code.addInstr(Instr.op_shl);
					code.addInstr(Instr.op_xor);
					code.addInstr(Instr.op_x2y,Type.tpInt);
					}
					break;
				}
			}
			else if( func.name == nameObjClone ) {
				// Do nothing ;-)
			}
			else if( func.name == nameObjToString ) {
				KString sign = null;
				switch(objt.getJType().java_signature.byteAt(0)) {
				case 'Z':
					sign = KString.from("(Z)Ljava/lang/String;");
					break;
				case 'C':
					sign = KString.from("(C)Ljava/lang/String;");
					break;
				case 'B':
				case 'S':
				case 'I':
					sign = KString.from("(I)Ljava/lang/String;");
					break;
				case 'J':
					sign = KString.from("(J)Ljava/lang/String;");
					break;
				case 'F':
					sign = KString.from("(F)Ljava/lang/String;");
					break;
				case 'D':
					sign = KString.from("(D)Ljava/lang/String;");
					break;
				}
				JMethodView m = Type.tpString.getJStruct().resolveMethod(
					KString.from("valueOf"),sign);
				code.addInstr(op_call,m,false);
			}
			else
				Kiev.reportError(this,"Call to unknown method "+func+" of type "+objt);
		}
		else
			code.addInstr(op_call,func,isSuperExpr(),obj.getType());
		if( null_cast_label != null ) {
			code.stack_pop();
			code.stack_push(JType.tpNull);
			code.addInstr(Instr.set_label,null_cast_label);
		}
		if( func.type.ret ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && getType().isReference()
			 && (!func.jtype.ret.isInstanceOf(getType().getErasedType()) || getType().isArray() || null_cast_label != null) )
				code.addInstr(op_checkcast,getType());
		}
		if( ok_label != null )
			code.addInstr(Instr.set_label,ok_label);
	}

}


@nodeview
public final view JClosureCallExprView of ClosureCallExprImpl extends JENodeView {
	public access:ro JENodeView			expr;
	public access:ro boolean			is_a_call;
	
	@getter public final ClosureType	get$ctype()				{ return (ClosureType)this.$view.expr.getType(); }
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])this.$view.args.toJViewArray(JENodeView.class); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this);
		// Load ref to closure
		expr.generate(code,null);
		ClosureType ctype = this.ctype;
		JENodeView[] args = this.args;
		// Clone it
		if( args.length > 0 ) {
			JMethodView clone_it = Type.tpClosureClazz.getJStructView().resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,Type.tpClosureClazz.type);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(ctype.args[i].getJType()),false);
			}
		}
		JMethodView call_it = getCallIt(ctype);
		// Check if we need to call
		if( is_a_call ) {
			if( call_it.type.ret ≡ Type.tpRule /*env_access != null*/ )
				code.addNullConst(); //env_access.generate(code,null);
			code.addInstr(op_call,call_it,false);
		}
		if( call_it.type.ret ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && call_it.type.ret.isReference()
			 && ( !call_it.jtype.ret.isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public JMethodView getCallIt(ClosureType tp) {
		KString call_it_name;
		KString call_it_sign;
		if( tp.ret.isReference() ) {
			call_it_name = KString.from("call_Object");
			call_it_sign = KString.from("()Ljava/lang/Object;");
		} else {
			call_it_name = KString.from("call_"+tp.ret);
			call_it_sign = KString.from("()"+tp.ret.getJType().java_signature);
		}
		return Type.tpClosureClazz.getJStructView().resolveMethod(call_it_name, call_it_sign);
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
	public JMethodView getMethodFor(JType tp) {
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
		JMethodView m = Type.tpClosureClazz.getJStructView().resolveMethod(KString.from("addArg"),sig);
		if( m == null )
			Kiev.reportError(expr,"Unknown method for kiev.vlang.closure");
		return m;
	}

}

