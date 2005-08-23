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
@cfnode
public class CallExpr extends Expr {
	@ref public Method				func;
	@att public final NArr<ENode>	args;
	@ref public Type				type_of_static;
	public boolean					super_flag;

	public CallExpr() {
	}

	public CallExpr(int pos, Method func, ENode[] args) {
		super(pos);
		this.func = func;
		foreach(Expr e; args) this.args.append(e);
	}

	public CallExpr(int pos, Method func, NArr<ENode> args) {
		super(pos);
		this.func = func;
		this.args.addAll(args);
	}

	public CallExpr(int pos, Method func, ENode[] args, boolean sf) {
		super(pos);
		this.func = func;
		foreach(Expr e; args) this.args.append(e);
		super_flag = sf;
	}

	public CallExpr(int pos, Method func, NArr<ENode> args, boolean sf) {
		super(pos);
		this.func = func;
		this.args.addAll(args);
		super_flag = sf;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(func.getName()).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		if( func.isStatic() )
			return Type.getRealType(type_of_static,func.type.ret);
		else
			return Type.getRealType(PassInfo.clazz.type,func.type.ret);
	}

	public void cleanup() {
		parent=null;
		func = null;
		foreach(ASTNode n; args; args!=null) n.cleanup();
		args = null;
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( func.type.ret == Type.tpRule ) {
			if( args.length == 0 || args[0].getType() != Type.tpRule )
				args.insert(0, new ConstNullExpr());
		} else {
			trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
		}
		if (args != null) {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(PassInfo.clazz.type,func.type.args[i]));
		}
		setResolved(true);
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		PassInfo.push(this);
		try {
			func.acc.verifyReadAccess(func);
			CodeLabel ok_label = null;
			if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
				String fname = func.name.name.toString().toLowerCase();
				if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
				if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
			}
			if( !func.isStatic() ) {
				if( !PassInfo.method.isStatic() )
					Code.addInstr(Instr.op_load,PassInfo.method.getThisPar());
				else
					throw new RuntimeException("Non-static method "+func+" is called from static method "+PassInfo.method);
			}
			if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
				int i = 0;
				int mode = 0;
				String fname = func.name.name.toString().toLowerCase();
				if( fname.indexOf("assert") >= 0 ) mode = 1;
				else if( fname.indexOf("trace") >= 0 ) mode = 2;
				if( mode > 0 && args.length > 0 && args[0].getType().isBoolean() ) {
					ok_label = Code.newLabel();
					if( args[0] instanceof IBoolExpr ) {
						if( mode == 1 ) ((IBoolExpr)args[0]).generate_iftrue(ok_label);
						else ((IBoolExpr)args[0]).generate_iffalse(ok_label);
					} else {
						args[0].generate(null);
						if( mode == 1 ) Code.addInstr(Instr.op_ifne,ok_label);
						else Code.addInstr(Instr.op_ifeq,ok_label);
					}
					if( mode == 1 )
						Code.addConst(0);
					else
						Code.addConst(1);
					i++;
				}
				for(; i < args.length; i++)
					args[i].generate(null);
			} else {
				// Very special case for rule call from inside
				// of RuleMethod
				if( func instanceof RuleMethod
				 && parent instanceof AssignExpr
				 && ((AssignExpr)parent).op == AssignOperator.Assign
				 && ((AssignExpr)parent).lval.getType() == Type.tpRule
				) {
					((AssignExpr)parent).lval.generate(null);
					for(int i=1; i < args.length; i++)
						args[i].generate(null);
				} else {
					for(int i=0; i < args.length; i++)
						args[i].generate(null);
				}
			}
			if( !func.isStatic() )
				Code.addInstr(op_call,func,super_flag, PassInfo.method.getThisPar().type);
			else
				Code.addInstr(op_call,func,super_flag, PassInfo.clazz.type);
			if( func.type.ret != Type.tpVoid ) {
				if( reqType==Type.tpVoid )
					Code.addInstr(op_pop);
				else if( Kiev.verify
				 && getType().isReference()
				 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray()) )
				 	Code.addInstr(op_checkcast,getType());
			}
			if( ok_label != null ) {
				Code.addInstr(Instr.set_label,ok_label);
			}
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public Dumper toJava(Dumper dmp) {
		if( func.getName().equals(nameInit) ) {
			if( super_flag ) dmp.append(nameSuper);
			else dmp.append(nameThis);
		} else {
			if( super_flag )
				dmp.append("super.");
			else if( func.isStatic() )
				dmp.append(((Struct)func.parent).name).append('.');
			dmp.append(func.name);
		}
		dmp.append('(');
		for(int i=0; i < args.length; i++) {
			// Very special case for rule call from inside
			// of RuleMethod
			if( i==0
			 && func instanceof RuleMethod
			 && parent instanceof AssignExpr
			 && ((AssignExpr)parent).op == AssignOperator.Assign
			 && ((AssignExpr)parent).lval.getType() == Type.tpRule
			) {
				((AssignExpr)parent).lval.toJava(dmp).append(',');
			} else {
				args[i].toJava(dmp);
				if( i < args.length-1 )
					dmp.append(',');
			}
		}
		dmp.append(')');
		return dmp;
	}

}

@node
@cfnode
public class CallAccessExpr extends Expr {
	@att public ENode				obj;
	@ref public Method				func;
	@att public final NArr<ENode>	args;
	public boolean					super_flag;

	public CallAccessExpr() {
	}

	public CallAccessExpr(int pos, ENode obj, Method func, ENode[] args) {
		super(pos);
		this.obj = obj;
		this.func = func;
		foreach(Expr e; args) this.args.append(e);
	}

	public CallAccessExpr(int pos, ENode obj, Method func, NArr<ENode> args) {
		super(pos);
		this.obj = obj;
		this.func = func;
		this.args.addAll(args);
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

	public void cleanup() {
		parent=null;
		func = null;
		obj.cleanup();
		obj = null;
		foreach(ASTNode n; args; args!=null) n.cleanup();
		args = null;
	}

	public void resolve(Type reqType) {
		if( isResolved() ) return;
		if( func.isStatic() ) {
			replaceWithResolve(new CallExpr(pos,func,args.toArray()), reqType);
			return;
		}
		obj.resolve(null);
		if( func.type.ret == Type.tpRule ) {
			if( args.length == 0 || args[0].getType() != Type.tpRule )
				args.insert(0, new ConstNullExpr());
		} else {
			trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
		}
		if (args != null) {
			for (int i=0; i < args.length; i++)
				args[i].resolve(Type.getRealType(obj.getType(),func.type.args[i]));
		}
		setResolved(true);
	}

	public void generateCheckCastIfNeeded() {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.isStructInstanceOf((Struct)func.parent) ) {
			trace( Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			Code.addInstr(Instr.op_checkcast,((Struct)func.parent).type);
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		PassInfo.push(this);
		try {
			func.acc.verifyReadAccess(func);
			if( ((Struct)func.parent).type.isInstanceOf(Type.tpDebug) ) {
				String fname = func.name.name.toString().toLowerCase();
				if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
				if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
			}
			if( obj != null ) {
				obj.generate(null);
				generateCheckCastIfNeeded();
			}
			else if( !func.isStatic() ) {
				if( !PassInfo.method.isStatic() )
					Code.addInstr(Instr.op_load,PassInfo.method.getThisPar());
				else
					throw new RuntimeException("Non-static method "+func+" is called from static method "+PassInfo.method);
			}
			// Very special case for rule call from inside of RuleMethod
			if( func instanceof RuleMethod
			 && parent instanceof AssignExpr
			 && ((AssignExpr)parent).op == AssignOperator.Assign
			 && ((AssignExpr)parent).lval.getType() == Type.tpRule
			) {
				((AssignExpr)parent).lval.generate(null);
				for(int i=1; i < args.length; i++)
					args[i].generate(null);
			} else {
				for(int i=0; i < args.length; i++)
					args[i].generate(null);
			}
			// Special meaning of Object.equals and so on
			// for parametriezed with primitive types classes
			Type objt = obj.getType();
			if( !objt.isReference() ) {
				if( func.parent != Type.tpObject.clazz )
					Kiev.reportError(pos,"Call to unknown method "+func+" of type "+objt);
				if( func.name.name == nameObjEquals ) {
					CodeLabel label_true = Code.newLabel();
					CodeLabel label_false = Code.newLabel();
					Code.addInstr(Instr.op_ifcmpeq,label_true);
					Code.addConst(0);
					Code.addInstr(Instr.op_goto,label_false);
					Code.addInstr(Instr.set_label,label_true);
					Code.addConst(1);
					Code.addInstr(Instr.set_label,label_false);
				}
				else if( func.name.name == nameObjGetClass ) {
					Type reft = Type.getRefTypeForPrimitive(objt);
					Field f = (Field)reft.clazz.resolveName(KString.from("TYPE"));
					Code.addInstr(Instr.op_pop);
					Code.addInstr(Instr.op_getstatic,f,reft);
				}
				else if( func.name.name == nameObjHashCode ) {
					switch(objt.signature.byteAt(0)) {
					case 'Z':
						{
						CodeLabel label_true = Code.newLabel();
						CodeLabel label_false = Code.newLabel();
						Code.addInstr(Instr.op_ifne,label_true);
						Code.addConst(1237);
						Code.addInstr(Instr.op_goto,label_false);
						Code.addInstr(Instr.set_label,label_true);
						Code.addConst(1231);
						Code.addInstr(Instr.set_label,label_false);
						}
						break;
					case 'B':
					case 'S':
					case 'I':
					case 'C':
						// the value is hashcode itself
						break;
					case 'J':
						Code.addInstr(Instr.op_dup);
						Code.addConst(32);
						Code.addInstr(Instr.op_shl);
						Code.addInstr(Instr.op_xor);
						Code.addInstr(Instr.op_x2y,Type.tpInt);
						break;
					case 'F':
						{
						Method m = Type.tpFloatRef.clazz.resolveMethod(
							KString.from("floatToIntBits"),
							KString.from("(F)I")
							);
						Code.addInstr(op_call,m,false);
						}
						break;
					case 'D':
						{
						Method m = Type.tpDoubleRef.clazz.resolveMethod(
							KString.from("doubleToLongBits"),
							KString.from("(D)J")
							);
						Code.addInstr(op_call,m,false);
						Code.addInstr(Instr.op_dup);
						Code.addConst(32);
						Code.addInstr(Instr.op_shl);
						Code.addInstr(Instr.op_xor);
						Code.addInstr(Instr.op_x2y,Type.tpInt);
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
					Code.addInstr(op_call,m,false);
				}
				else
					Kiev.reportError(pos,"Call to unknown method "+func+" of type "+objt);
			}
			else
				Code.addInstr(op_call,func,super_flag,obj.getType());
			if( func.type.ret != Type.tpVoid ) {
				if( reqType==Type.tpVoid )
					Code.addInstr(op_pop);
				else if( Kiev.verify
				 && getType().isReference()
				 && ( !getType().isStructInstanceOf(func.type.ret.clazz) || getType().isArray() ) )
				 	Code.addInstr(op_checkcast,getType());
			}
		} finally { PassInfo.pop(this); }
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
@cfnode
public class ClosureCallExpr extends Expr {
	@att public ENode					expr;
	@att public final NArr<ENode>		args;
	@att public ENode					env_access;		// $env for rule closures
	public boolean						is_a_call;

	@ref public Method	clone_it;
	@ref public Method	call_it;
	public Method[]	addArg;
	@ref public Type	func_tp;

	public ClosureCallExpr() {
	}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		super(pos);
		this.expr = expr;
		foreach(Expr e; args) this.args.append(e);
	}

	public ClosureCallExpr(int pos, ENode expr, NArr<ENode> args) {
		super(pos);
		this.expr = expr;
		this.args.addAll(args);
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

	public void cleanup() {
		parent=null;
		expr.cleanup();
		expr = null;
		foreach(ASTNode n; args; args!=null) n.cleanup();
		args = null;
		if( env_access != null ) {
			env_access.cleanup();
			env_access = null;
		}
	}

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		PassInfo.push(this);
		try {
			expr.resolve(null);
//			ASTNode v = func;
//			Type tp1 = expr==null?null:expr.getType();
//			Type tp;
//			if( v instanceof Expr && (tp=Type.getRealType(tp1,((Expr)v).getType())) instanceof ClosureType ) {
//				func = (Expr)v;
//			}
//			else if( v instanceof Var && (tp=Type.getRealType(tp1,((Var)v).getType())) instanceof ClosureType ) {
//				func = new VarAccessExpr(pos,this,(Var)v);
//				func.resolve(null);
//			}
//			else if( v instanceof Field && (tp=Type.getRealType(tp1,((Field)v).getType())) instanceof ClosureType ) {
//				if( ((Field)v).isStatic() ) { 
//					func = new StaticFieldAccessExpr(pos,PassInfo.clazz,(Field)v);
//					func.resolve(null);
//				}
//				else if( expr == null ) {
//					func = new AccessExpr(pos,new ThisExpr(pos),(Field)v);
//					func.resolve(null);
//				}
//				else {
//					func = new AccessExpr(pos,parent,expr,(Field)v);
//					func.resolve(null);
//					expr = null;
//				}
//			}
//			else
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
			ASTNode@ callIt;
			MethodType mt = MethodType.newMethodType(Type.emptyArray,Type.tpAny);
			ResInfo info = new ResInfo(ResInfo.noForwards|ResInfo.noStatic|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(tp,callIt,info,call_it_name,mt) ) {
				throw new RuntimeException("Can't resolve method "+Method.toString(call_it_name,mt)+" in class "+tp.clazz);
			} else {
				call_it = (Method)callIt;
				if( call_it.type.ret == Type.tpRule ) {
					env_access = new ConstNullExpr();
				} else {
					trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
				}
			}
		} finally { PassInfo.pop(this); }
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
	public Method getMethodFor(Type tp) {
		Type t = tp;
		KString sig = null;
		switch(t.java_signature.byteAt(0)) {
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
			Kiev.reportError(expr.pos,"Unknown method for kiev.vlang.closure");
		return m;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ClosureCallExpr: "+this);
		PassInfo.push(this);
		try {
			// Load ref to closure
			expr.generate(null);
			// Clone it
			if( args.length > 0 ) {
				Code.addInstr(op_call,clone_it,false);
				if( Kiev.verify )
					Code.addInstr(op_checkcast,Type.tpClosureClazz.type);
				// Add arguments
				for(int i=0; i < args.length; i++) {
					args[i].generate(null);
					Code.addInstr(op_call,getMethodFor(func_tp.args[i]),false);
				}
			}
			// Check if we need to call
			if( is_a_call ) {
				if( env_access != null )
					env_access.generate(null);
				Code.addInstr(op_call,call_it,false);
			}
			if( call_it.type.ret != Type.tpVoid ) {
				if( reqType==Type.tpVoid )
					Code.addInstr(op_pop);
				else if( Kiev.verify
				 && call_it.type.ret.isReference()
				 && ( !getType().isStructInstanceOf(call_it.type.ret.clazz) || getType().isArray() ) )
				 	Code.addInstr(op_checkcast,getType());
			}
		} finally { PassInfo.pop(this); }
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
