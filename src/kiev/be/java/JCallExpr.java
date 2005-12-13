package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.vlang.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public class JCallExprView extends JENodeView {
	final CallExpr.CallExprImpl impl;
	public JCallExprView(CallExpr.CallExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final JENodeView		get$obj()				{ return this.impl.obj.getJENodeView(); }
	@getter public final JMethodView	get$func()				{ return this.impl.func.getJMethodView(); }
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])this.impl.args.toJViewArray(JENodeView.class); }
	@getter public final JENodeView		get$temp_expr()			{ return this.impl.temp_expr.getJENodeView(); }
	@getter public final boolean		get$super_flag()		{ return this.impl.super_flag; }
	
	@setter public final void	set$temp_expr(JENodeView val)	{ this.impl.temp_expr = val==null? null : val.getENode(); }

	public void generateCheckCastIfNeeded(Code code) {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)func.parent) ) {
			trace( Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			code.addInstr(Instr.op_checkcast,((Struct)func.parent).type);
		}
	}

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		code.setLinePos(this);
		func.acc.verifyReadAccess(this,func);
		CodeLabel ok_label = null;
		if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
			String fname = func.name.name.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
			if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
		}
		if !(obj.getNode() instanceof TypeRef) {
			obj.generate(code,null);
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
		if( func.getNode() instanceof RuleMethod ) {
			// Very special case for rule call from inside of RuleMethod
			if (parent instanceof AssignExpr
				&& ((AssignExpr)parent).op == AssignOperator.Assign
				&& ((AssignExpr)parent).lval.getType() == Type.tpRule
				)
				((AssignExpr)parent).lval.generate(code,null);
			else
				code.addNullConst();
		}
		else if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
			int mode = 0;
			String fname = func.name.name.toString().toLowerCase();
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
			if( func.name.equals(nameInit) && func.getMethod().getOuterThisParam() != null) {
				FormPar fp = code.method.getOuterThisParam();
				if (fp == null) {
					Kiev.reportError(this, "Cannot find outer this parameter");
					code.addNullConst();
				} else {
					code.addInstr(Instr.op_load,fp.getJVarView());
				}
			}
			if( func.name.equals(nameInit) && func.getMethod().getTypeInfoParam() != null) {
				Method mmm = pctx.method;
				Type tp = mmm.pctx.clazz != func.pctx.clazz ? pctx.clazz.super_type : pctx.clazz.type;
				assert(pctx.method.name.equals(nameInit));
				assert(tp.args.length > 0);
				// Insert our-generated typeinfo, or from childs class?
				if (mmm.getTypeInfoParam() != null)
					temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam()).getJENodeView();
				else
					temp_expr = pctx.clazz.accessTypeInfoField(this.getNode(),tp).getJENodeView();
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
			Type type = func.jtype.args[N];
			assert(type.isArray());
			code.addConst(args.length-N);
			code.addInstr(Instr.op_newarray,type.args[0]);
			for(int j=0; i < args.length; i++, j++) {
				code.addInstr(Instr.op_dup);
				code.addConst(j);
				args[i].generate(code,null);
				code.addInstr(Instr.op_arr_store);
			}
		}
		
		// Special meaning of Object.equals and so on
		// for parametriezed with primitive types classes
		Type objt = obj.getType();
		if( !objt.isReference() ) {
			if( func.parent != Type.tpObject.clazz )
				Kiev.reportError(this,"Call to unknown method "+func+" of type "+objt);
			if( func.name.name == nameObjEquals ) {
				CodeLabel label_true = code.newLabel();
				CodeLabel label_false = code.newLabel();
				code.addInstr(Instr.op_ifcmpeq,label_true);
				code.addConst(0);
				code.addInstr(Instr.op_goto,label_false);
				code.addInstr(Instr.set_label,label_true);
				code.addConst(1);
				code.addInstr(Instr.set_label,label_false);
			}
			else if( func.name.name == nameObjGetClass ) {
				BaseType reft = Type.getRefTypeForPrimitive(objt);
				Field f = reft.clazz.resolveField(KString.from("TYPE"));
				code.addInstr(Instr.op_pop);
				code.addInstr(Instr.op_getstatic,f.getJFieldView(),reft);
			}
			else if( func.name.name == nameObjHashCode ) {
				switch(objt.signature.byteAt(0)) {
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
					Method m = Type.tpFloatRef.clazz.resolveMethod(
						KString.from("floatToIntBits"),
						KString.from("(F)I")
						);
					code.addInstr(op_call,m,false);
					}
					break;
				case 'D':
					{
					Method m = Type.tpDoubleRef.clazz.resolveMethod(
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
			else if( func.name.name == nameObjClone ) {
				// Do nothing ;-)
			}
			else if( func.name.name == nameObjToString ) {
				KString sign = null;
				switch(objt.signature.byteAt(0)) {
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
				Method m = Type.tpString.clazz.resolveMethod(
					KString.from("valueOf"),sign);
				code.addInstr(op_call,m,false);
			}
			else
				Kiev.reportError(this,"Call to unknown method "+func+" of type "+objt);
		}
		else
			code.addInstr(op_call,func.getMethod(),super_flag,obj.getType());
		if( func.type.ret != Type.tpVoid ) {
			if( reqType==Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && getType().isReference()
			 && ( !func.jtype.ret.isInstanceOf(getType().getJavaType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
		if( ok_label != null ) {
			code.addInstr(Instr.set_label,ok_label);
		}
	}

}


@nodeview
public class JClosureCallExprView extends JENodeView {
	final ClosureCallExpr.ClosureCallExprImpl impl;
	public JClosureCallExprView(ClosureCallExpr.ClosureCallExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final ENode			get$expr()				{ return this.impl.expr; }
	@getter public final JENodeView[]	get$args()				{ return (JENodeView[])this.impl.args.toJViewArray(JENodeView.class); }
	@getter public final boolean		get$is_a_call()			{ return this.impl.is_a_call; }
	@getter public final ClosureType	get$ctype()				{ return (ClosureType)this.impl.expr.getType(); }
	
	public ClosureCallExpr getClosureCallExpr() { return (ClosureCallExpr)this.getNode(); }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this);
		// Load ref to closure
		expr.generate(code,null);
		ClosureType ctype = this.ctype;
		JENodeView[] args = this.args;
		// Clone it
		if( args.length > 0 ) {
			Method clone_it = ctype.clazz.resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,Type.tpClosureClazz.type);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(ctype.args[i].getJType()),false);
			}
		}
		Method call_it = getClosureCallExpr().getCallIt(ctype);
		// Check if we need to call
		if( is_a_call ) {
			if( call_it.type.ret == Type.tpRule /*env_access != null*/ )
				code.addNullConst(); //env_access.generate(code,null);
			code.addInstr(op_call,call_it,false);
		}
		if( call_it.type.ret != Type.tpVoid ) {
			if( reqType==Type.tpVoid )
				code.addInstr(op_pop);
			else if( Kiev.verify
			 && call_it.type.ret.isReference()
			 && ( !call_it.jtype.ret.isInstanceOf(getType().getJavaType()) || getType().isArray() ) )
				code.addInstr(op_checkcast,getType());
		}
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
	public Method getMethodFor(JType tp) {
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
		Method m = Type.tpClosureClazz.resolveMethod(KString.from("addArg"),sig);
		if( m == null )
			Kiev.reportError(expr,"Unknown method for kiev.vlang.closure");
		return m;
	}

}


