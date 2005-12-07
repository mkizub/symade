/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Instr.*;
import kiev.vlang.Operator.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@node
@dflow(out="args")
public class CallExpr extends ENode {
	@att
	@dflow(in="")
	public ENode				obj;
	
	@ref
	public Method				func;
	
	@att
	@dflow(in="obj", seq="true")
	public final NArr<ENode>	args;
	
	@att private ENode			temp_expr;

	public boolean				super_flag;

	public CallExpr() {
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args, boolean super_flag) {
		super(pos);
		if (obj == null) {
			if !(func.isStatic() || func instanceof Constructor) {
				throw new RuntimeException("Call to non-static method "+func+" without accessor");
			}
			this.obj = new TypeRef(((Struct)func.parent).type);
		}
		else if (func.isStatic() && !(obj instanceof TypeRef)) {
			this.obj = new TypeRef(obj.getType());
		}
		else {
			this.obj = obj;
		}
		this.func = func;
		this.args.addAll(args);
		this.super_flag = super_flag;
	}

	public CallExpr(int pos, ENode obj, Method func, NArr<ENode> args, boolean super_flag) {
		this(pos, obj, func, args.toArray(), super_flag);
	}
	
	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, func, args, false);
	}

	public CallExpr(int pos, ENode obj, Method func, NArr<ENode> args) {
		this(pos, obj, func, args.toArray(), false);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( obj.getPriority() > opAccessPriority )
			sb.append('(').append(obj).append(").");
		else
			sb.append(obj).append('.');
		sb.append(func.name).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		return Type.getRealType(obj.getType(),func.type.ret);
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		obj.resolve(null);
//		if (func.type.ret == Type.tpRule) {
//			if( args.length == 0 || args[0].getType() != Type.tpRule )
//				args.insert(0, new ConstNullExpr());
//		} else {
//			trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
//		}
		if( func.name.equals(nameInit) && func.getTypeInfoParam() != null) {
			Method mmm = pctx.method;
			Type tp = mmm.pctx.clazz != func.pctx.clazz ? pctx.clazz.super_type : pctx.clazz.type;
			assert(pctx.method.name.equals(nameInit));
			assert(tp.args.length > 0);
			// Insert our-generated typeinfo, or from childs class?
			if (mmm.getTypeInfoParam() != null)
				temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam());
			else
				temp_expr = pctx.clazz.accessTypeInfoField(this,tp);
			temp_expr.resolve(null);
			temp_expr = null;
		}
		if (func.isVarArgs()) {
			int i=0;
			for(; i < func.type.args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.args[i]));
			Type varg_tp = Type.getRealType(obj.getType(),func.getVarArgParam().type);
			assert(varg_tp.isArray());
			for(; i < args.length; i++)
				args[i].resolve(varg_tp.args[0]);
		} else {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.args[i]));
		}
		if !(func.parent instanceof Struct) {
			ASTNode n = func.parent;
			while !(n instanceof Method) n = n.parent;
			assert (n.parent instanceof Struct);
			func = (Method)n;
		}
		setResolved(true);
	}

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
		code.setLinePos(this.getPosLine());
		func.acc.verifyReadAccess(this,func);
		CodeLabel ok_label = null;
		if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
			String fname = func.name.name.toString().toLowerCase();
			if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
			if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
		}
		if !(obj instanceof TypeRef) {
			obj.generate(code,null);
			generateCheckCastIfNeeded(code);
		}
		else if( !func.isStatic() ) {
			if( !code.method.isStatic() )
				code.addInstrLoadThis();
			else
				throw new RuntimeException("Non-static method "+func+" is called from static method "+code.method);
		}
		int i = 0;
		if( func instanceof RuleMethod ) {
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
			if( func.name.equals(nameInit) && func.getOuterThisParam() != null) {
				FormPar fp = code.method.getOuterThisParam();
				if (fp == null) {
					Kiev.reportError(this, "Cannot find outer this parameter");
					code.addNullConst();
				} else {
					code.addInstr(Instr.op_load,fp);
				}
			}
			if( func.name.equals(nameInit) && func.getTypeInfoParam() != null) {
				Method mmm = pctx.method;
				Type tp = mmm.pctx.clazz != func.pctx.clazz ? pctx.clazz.super_type : pctx.clazz.type;
				assert(pctx.method.name.equals(nameInit));
				assert(tp.args.length > 0);
				// Insert our-generated typeinfo, or from childs class?
				if (mmm.getTypeInfoParam() != null)
					temp_expr = new LVarExpr(pos,mmm.getTypeInfoParam());
				else
					temp_expr = pctx.clazz.accessTypeInfoField(this,tp);
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
				code.addInstr(Instr.op_getstatic,f,reft);
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
			code.addInstr(op_call,func,super_flag,obj.getType());
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

	public int		getPriority() { return Constants.opCallPriority; }

	public Dumper toJava(Dumper dmp) {
		if( func.getName().equals(nameInit) ) {
			if( super_flag ) dmp.append(nameSuper);
			else dmp.append(nameThis);
		} else {
			if( obj != null ) {
				if( obj.getPriority() < opCallPriority ) {
					dmp.append('(').append(obj).append(").");
				} else {
					dmp.append(obj).append('.');
				}
			}
			else if( super_flag )
				dmp.append("super.");
			else if( func instanceof Method && ((Method)func).isStatic() )
				dmp.append(((Struct)((Method)func).parent).name).append('.');
			dmp.append(func.getName());
		}
		dmp.append('(');
		for(int i=0; i < args.length; i++) {
			dmp.append(args[i]);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		return dmp;
	}
}

@node
@dflow(out="args")
public class ClosureCallExpr extends ENode {
	@att
	@dflow(in="this:in")
	public ENode					expr;

	@att
	@dflow(in="expr", seq="true")
	public final NArr<ENode>		args;
	
	@att
	public ENode					env_access;		// $env for rule closures
	
	public boolean					is_a_call;

	@ref public Method	clone_it;
	@ref public Method	call_it;
	public Method[]	addArg;
	@ref public Type	func_tp;

	public ClosureCallExpr() {
	}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		super(pos);
		this.expr = expr;
		foreach(ENode e; args) this.args.append(e);
		Type tp = expr.getType();
		if (tp instanceof ClosureType)
			is_a_call = tp.args.length==args.length;
		else
			is_a_call = true;
	}

	public ClosureCallExpr(int pos, ENode expr, NArr<ENode> args) {
		super(pos);
		this.expr = expr;
		this.args.addAll(args);
		Type tp = expr.getType();
		if (tp instanceof ClosureType)
			is_a_call = tp.args.length==args.length;
		else
			is_a_call = true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(expr).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		ClosureType t = (ClosureType)expr.getType();
		if( is_a_call )
			return t.ret;
		Type[] types = new Type[t.args.length - args.length];
		for(int i=0; i < types.length; i++) types[i] = t.args[i+args.length];
		t = ClosureType.newClosureType(t.clazz,types,t.ret);
		return t;
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		expr.resolve(null);
		if !(expr.getType() instanceof ClosureType)
			throw new RuntimeException("Resolved item "+expr+" is not a closure");
		ClosureType tp = (ClosureType)expr.getType();
		if( reqType != null && reqType instanceof CallableType )
			is_a_call = false;
		else if( (reqType == null || !(reqType instanceof CallableType)) && tp.args.length==args.length )
			is_a_call = true;
		else
			is_a_call = false;
		func_tp = tp;
		for(int i=0; i < args.length; i++)
			args[i].resolve(tp.args[i]);
		clone_it = tp.clazz.resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
		KString call_it_name;
		if( ((CallableType)tp).ret.isReference() )
			call_it_name = KString.from("call_Object");
		else
			call_it_name = KString.from("call_"+((CallableType)tp).ret);
		Method@ callIt;
		MethodType mt = MethodType.newMethodType(Type.emptyArray,Type.tpAny);
		ResInfo info = new ResInfo(this,ResInfo.noForwards|ResInfo.noStatic|ResInfo.noImports);
		if( !PassInfo.resolveBestMethodR(tp,callIt,info,call_it_name,mt) ) {
			throw new RuntimeException("Can't resolve method "+Method.toString(call_it_name,mt)+" in class "+tp.clazz);
		} else {
			call_it = (Method)callIt;
			if( call_it.type.ret == Type.tpRule ) {
				env_access = new ConstNullExpr();
			} else {
				trace(Kiev.debugResolve,"ClosureCallExpr "+this+" is not a rule call");
			}
		}
		setResolved(true);
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

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		code.setLinePos(this.getPosLine());
		// Load ref to closure
		expr.generate(code,null);
		// Clone it
		if( args.length > 0 ) {
			code.addInstr(op_call,clone_it,false);
			if( Kiev.verify )
				code.addInstr(op_checkcast,Type.tpClosureClazz.type);
			// Add arguments
			for(int i=0; i < args.length; i++) {
				args[i].generate(code,null);
				code.addInstr(op_call,getMethodFor(func_tp.args[i].getJType()),false);
			}
		}
		// Check if we need to call
		if( is_a_call ) {
			if( env_access != null )
				env_access.generate(code,null);
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

	public int		getPriority() { return Constants.opCallPriority; }

	public Dumper toJava(Dumper dmp) {
		expr.toJava(dmp).append(".clone()");
		for(int i=0; i < args.length; i++) {
			dmp.append(".addArg(");
			args[i].toJava(dmp);
			dmp.append(')');
		}
		if( is_a_call ) {
			dmp.append('.').append(call_it.name).append('(');
			if( env_access != null ) dmp.append(env_access);
			dmp.append(')');
		}
		return dmp;
	}
}
