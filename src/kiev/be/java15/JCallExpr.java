package kiev.be.java15;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public final view JCallExpr of CallExpr extends JENode {

	static final AttrSlot ATTR = new DataAttrSlot("jcall temp expr",true,false,ENode.class);	

	public:ro	JMethod			func;
	public:ro	JENode			obj;
	public:ro	CallType		mt;
	public:ro	JENode[]		args;
	abstract
	public 		JENode			tmp_expr;
	
	@getter public final JENode get$tmp_expr() {
		return (JENode)(ENode)this.getNodeData(ATTR);
	}
	@setter public final void set$tmp_expr(JENode e) {
		if (e != null)
			this.addNodeData((ENode)e, ATTR);
		else
			this.delNodeData(ATTR);
	}

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.getJType().isInstanceOf(func.jctx_tdecl.jtype) ) {
			trace( Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			code.addInstr(Instr.op_checkcast,func.jctx_tdecl.xtype);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		code.setLinePos(this);
		JMethod func = this.func;
		if (((Method)func) instanceof CoreMethod) {
			CoreMethod m = (CoreMethod)(Method)func;
			m.bend_func.generate(code,reqType,this);
			return;
		}
		Access.verifyRead(this,func);
		CodeLabel ok_label = null;
		CodeLabel null_cast_label = null;
		if( func.jctx_tdecl.xtype.isInstanceOf(Type.tpDebug) ) {
			String fname = func.id.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
			if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
		}
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
		if( func.isRuleMethod() ) {
			// Very special case for rule call from inside of RuleMethod
			JNode p = (JNode)this.jparent;
			if (p instanceof JAssignExpr
				&& ((JAssignExpr)p).op == Operator.Assign
				&& ((JAssignExpr)p).lval.getType() ≡ Type.tpRule
				)
				((JAssignExpr)p).lval.generate(code,null);
			else
				code.addNullConst();
		}
		else if( func.jctx_tdecl.xtype.isInstanceOf(Type.tpDebug) ) {
			int mode = 0;
			String fname = func.id.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 ) mode = 1;
			else if( fname.indexOf("trace") >= 0 ) mode = 2;
			if( mode > 0 && args.length > 0 && args[0].getType().isBoolean() ) {
				ok_label = code.newLabel();
				JENode arg0 = args[0];
				if( arg0 instanceof IBoolExpr ) {
					if( mode == 1 ) arg0.generate_iftrue(code,ok_label);
					else arg0.generate_iffalse(code,ok_label);
				} else {
					arg0.generate(code,null);
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
			if( func.id.equals(nameInit) && func.getOuterThisParam() != null) {
				JVar fp = code.method.getOuterThisParam();
				if (fp == null) {
					Kiev.reportError(this, "Cannot find outer this parameter");
					code.addNullConst();
				} else {
					code.addInstr(Instr.op_load,fp);
				}
			}
			if( func.id.equals(nameInit) && func.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null) {
				JMethod jmm = jctx_method;
				Type tp;
				if (!jmm.jctx_tdecl.equals(func.jctx_tdecl))
					tp = ((TypeDecl)jctx_tdecl).super_types[0].getType();
				else
					tp = ((TypeDecl)jctx_tdecl).xtype;
				assert(jmm.id.equals(nameInit));
				assert(tp.getStruct().isTypeUnerasable());
				// Insert our-generated typeinfo, or from childs class?
				if (jmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO) != null)
					tmp_expr = (JENode)new LVarExpr(pos,(Var)jmm.getTypeInfoParam(FormPar.PARAM_TYPEINFO));
				else
					tmp_expr = ((JStruct)jctx_tdecl).accessTypeInfoField(this,tp,true);
				tmp_expr.generate(code,null);
				tmp_expr = null;
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
				ArrayType type = (ArrayType)func.etype.arg(N);
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
		if (func.isTypeUnerasable()) {
			foreach (TypeDef td; ((Method)func).targs) {
				Type tp = mt.resolve(td.getAType());
				tmp_expr = ((JStruct)jctx_tdecl).accessTypeInfoField(this,tp,true);
				tmp_expr.generate(code,null);
				tmp_expr = null;
			}
		}
		
		// Special meaning of Object.equals and so on
		// for parametriezed with primitive types classes
		Type objt = obj.getType();
		if( !objt.isReference() ) {
			if( func.jctx_tdecl.xtype ≉ Type.tpObject )
				Kiev.reportError(this,"Call to unknown method "+func+" of type "+objt);
			if( func.id.uname == nameObjEquals ) {
				CodeLabel label_true = code.newLabel();
				CodeLabel label_false = code.newLabel();
				code.addInstr(Instr.op_ifcmpeq,label_true);
				code.addConst(0);
				code.addInstr(Instr.op_goto,label_false);
				code.addInstr(Instr.set_label,label_true);
				code.addConst(1);
				code.addInstr(Instr.set_label,label_false);
			}
			else if( func.id.uname == nameObjGetClass ) {
				CompaundType reft = ((CoreType)objt).getRefTypeForPrimitive();
				Field f = reft.clazz.resolveField("TYPE");
				code.addInstr(Instr.op_pop);
				code.addInstr(Instr.op_getstatic,(JField)f,reft);
			}
			else if( func.id.uname == nameObjHashCode ) {
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
					code.addInstr(Instr.op_lshl);
					code.addInstr(Instr.op_lxor);
					code.addInstr(Instr.op_x2y,Type.tpInt);
					break;
				case 'F':
					{
					JMethod m = Type.tpFloatRef.getJStruct().resolveMethod(
						"floatToIntBits",
						KString.from("(F)I")
						);
					code.addInstr(op_call,m,false);
					}
					break;
				case 'D':
					{
					JMethod m = Type.tpDoubleRef.getJStruct().resolveMethod(
						"doubleToLongBits",
						KString.from("(D)J")
						);
					code.addInstr(op_call,m,false);
					code.addInstr(Instr.op_dup);
					code.addConst(32);
					code.addInstr(Instr.op_lshl);
					code.addInstr(Instr.op_lxor);
					code.addInstr(Instr.op_x2y,Type.tpInt);
					}
					break;
				}
			}
			else if( func.id.uname == nameObjClone ) {
				// Do nothing ;-)
			}
			else if( func.id.uname == nameObjToString ) {
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
				JMethod m = Type.tpString.getJStruct().resolveMethod("valueOf",sign);
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
		if( func.type.ret() ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && getType().isReference()
			 && (!func.etype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() || null_cast_label != null) )
				code.addInstr(op_checkcast,getType());
		}
		if( ok_label != null )
			code.addInstr(Instr.set_label,ok_label);
	}

}


@nodeview
public final view JClosureCallExpr of ClosureCallExpr extends JENode {
	public:ro JENode		expr;
	public:ro JENode[]		args;
	public:ro Boolean		is_a_call;
	
	@getter public final CallType	get$xtype() { return (CallType)((ClosureCallExpr)this).expr.getType(); }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this);
		// Load ref to closure
		expr.generate(code,null);
		CallType xtype = this.xtype;
		JENode[] args = this.args;
		// Clone it
		if( args.length > 0 ) {
			JMethod clone_it = ((JStruct)Type.tpClosureClazz).resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,Type.tpClosureClazz.xtype);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(xtype.arg(i).getJType()),false);
			}
		}
		JMethod call_it = getCallIt(xtype);
		// Check if we need to call
		if( is_a_call.booleanValue() ) {
			if( call_it.type.ret() ≡ Type.tpRule )
				code.addNullConst(); //env_access.generate(code,null);
			code.addInstr(op_call,call_it,false);
		}
		if( call_it.type.ret() ≢ Type.tpVoid ) {
			if( reqType ≡ Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && call_it.type.ret().isReference()
			 && ( !call_it.etype.ret().isInstanceOf(getType().getErasedType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
	}

	public JMethod getCallIt(CallType tp) {
		String call_it_name;
		KString call_it_sign;
		if( tp.ret().isReference() ) {
			call_it_name = "call_Object";
			call_it_sign = KString.from("()Ljava/lang/Object;");
		} else {
			call_it_name = ("call_"+tp.ret()).intern();
			call_it_sign = KString.from("()"+tp.ret().getJType().java_signature);
		}
		return ((JStruct)Type.tpClosureClazz).resolveMethod(call_it_name, call_it_sign);
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
	public JMethod getMethodFor(JType tp) {
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
		JMethod m = ((JStruct)Type.tpClosureClazz).resolveMethod("addArg",sig);
		if( m == null )
			Kiev.reportError(expr,"Unknown method for kiev.vlang.closure");
		return m;
	}

}


