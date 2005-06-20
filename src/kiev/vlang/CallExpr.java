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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/CallExpr.java,v 1.6.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.2 $
 *
 */

public class CallExpr extends Expr {
	public Method	func;
	public Expr[]	args;
	public Type		type_of_static;
	public boolean	super_flag = false;

	public CallExpr(int pos, Method func, Expr[] args) {
		super(pos);
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public CallExpr(int pos, ASTNode par, Method func, Expr[] args) {
		super(pos,par);
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public CallExpr(int pos, Method func, Expr[] args, boolean sf) {
		super(pos);
		this.func = func;
		this.args = args;
		super_flag = sf;
	}

	public CallExpr(int pos, ASTNode par, Method func, Expr[] args, boolean sf) {
		super(pos,par);
		this.func = func;
		this.args = args;
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

	public static Expr[] insertPEnvForRuleCall(Expr[] args,ASTNode me) {
		trace(Kiev.debugResolve,"CallExpr "+me+" is rule call");
		Var env;
		if( PassInfo.method.type.ret == Type.tpRule  ) {
			args = (Expr[])Arrays.insert(args,new ConstExpr(me.pos,null),0);
		} else {
//			ASTNode par = me.parent;
//			if( par != null && par instanceof ForEachStat ) {
//				trace(Kiev.debugResolve,"CallExpr parent stat is "+par.getClass()+" - adding VarAccessExpr($env)");
//				args = (Expr[])Arrays.insert(args,
//					new VarAccessExpr(me.pos,me,((ForEachStat)par).iter),0);
//			} else {
//				trace(Kiev.debugResolve,"CallExpr parent stat is "+par.getClass()+" - adding $env.init()");
				args = (Expr[])Arrays.insert(args,new ConstExpr(me.pos,null),0);
//			}
		}
		return args;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( func.type.ret == Type.tpRule ) {
			if( args.length == 0 || args[0].getType() != Type.tpRule )
				args = insertPEnvForRuleCall(args,this);
		} else {
			trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
		}
		if (args != null) {
			for (int i=0; i < args.length; i++)
				args[i] = args[i].resolveExpr(Type.getRealType(PassInfo.clazz.type,func.type.args[i]));
		}
		setResolved(true);
		return this;
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		PassInfo.push(this);
		try {
			func.acc.verifyReadAccess(func);
			CodeLabel ok_label = null;
			if( ((Struct)func.parent).instanceOf(Type.tpDebug.clazz) ) {
				String fname = func.name.name.toString().toLowerCase();
				if( fname.indexOf("assert") >= 0 && !Kiev.debugOutputA ) return;
				if( fname.indexOf("trace") >= 0 && !Kiev.debugOutputT ) return;
			}
			if( !func.isStatic() ) {
				if( !PassInfo.method.isStatic() )
					Code.addInstr(Instr.op_load,PassInfo.method.params[0]);
				else
					throw new RuntimeException("Non-static method "+func+" is called from static method "+PassInfo.method);
			}
			if( ((Struct)func.parent).instanceOf(Type.tpDebug.clazz) ) {
				int i = 0;
				int mode = 0;
				String fname = func.name.name.toString().toLowerCase();
				if( fname.indexOf("assert") >= 0 ) mode = 1;
				else if( fname.indexOf("trace") >= 0 ) mode = 2;
				if( mode > 0 && args.length > 0 && args[0].getType().isBoolean() ) {
					ok_label = Code.newLabel();
					if( args[0] instanceof BooleanExpr ) {
						if( mode == 1 ) ((BooleanExpr)args[0]).generate_iftrue(ok_label);
						else ((BooleanExpr)args[0]).generate_iffalse(ok_label);
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
				Code.addInstr(op_call,func,super_flag, PassInfo.method.params[0].type);
			else
				Code.addInstr(op_call,func,super_flag, PassInfo.clazz.type);
			if( func.type.ret != Type.tpVoid ) {
				if( reqType==Type.tpVoid )
					Code.addInstr(op_pop);
				else if( Kiev.verify
				 && getType().isReference()
//				 && func.jtype!= null
				 && ( !getType().clazz.equals(func.type.ret.clazz) || getType().isArray()) )
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

public class CallAccessExpr extends Expr {
	public Expr		obj;
	public Method	func;
	public Expr[]	args;
	public boolean	super_flag = false;

	public CallAccessExpr(int pos, Expr obj, Method func, Expr[] args) {
		super(pos);
		this.obj = obj;
		this.obj.parent = this;
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public CallAccessExpr(int pos, ASTNode par, Expr obj, Method func, Expr[] args) {
		super(pos,par);
		this.obj = obj;
		this.obj.parent = this;
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
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

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		if( func.isStatic() ) return new CallExpr(pos,parent,func,args).resolve(reqType);
		obj = (Expr)obj.resolve(null);
		if( func.type.ret == Type.tpRule ) {
			if( args.length == 0 || args[0].getType() != Type.tpRule )
				args = CallExpr.insertPEnvForRuleCall(args,this);
		} else {
			trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
		}
		if (args != null) {
			for (int i=0; i < args.length; i++)
				args[i] = args[i].resolveExpr(Type.getRealType(obj.getType(),func.type.args[i]));
		}
		setResolved(true);
		return this;
	}

	public void generateCheckCastIfNeeded() {
		if( !Kiev.verify ) return;
		Type ot = obj.getType();
		if( !ot.clazz.instanceOf((Struct)func.parent) ) {
			trace( Kiev.debugNodeTypes, "Need checkcast for method "+ot+"."+func);
			Code.addInstr(Instr.op_checkcast,((Struct)func.parent).type);
		}
	}

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating CallExpr: "+this);
		PassInfo.push(this);
		try {
			func.acc.verifyReadAccess(func);
			if( ((Struct)func.parent).instanceOf(Type.tpDebug.clazz) ) {
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
					Code.addInstr(Instr.op_load,PassInfo.method.params[0]);
				else
					throw new RuntimeException("Non-static method "+func+" is called from static method "+PassInfo.method);
			}
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
			// Special meaning of Object.equals and so on
			// for parametriezed with primitive types classes
			Type objt = Type.getRealType(Kiev.argtype,obj.getType());
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
			// Special meaning of StringBuffer.append
			// for parametriezed with primitive types classes
			else if( objt == StringConcatExpr.clazzStringBuffer.type
				  && func.name.name == nameStrBuffAppend
				  && Kiev.argtype != null
				  && !Type.getRealType(Kiev.argtype,args[0].getType()).isReference()
			) {
				KString sign = null;
				switch(Type.getRealType(Kiev.argtype,args[0].getType()).signature.byteAt(0)) {
				case 'Z':
					sign = StringConcatExpr.sigZ;
					break;
				case 'C':
					sign = StringConcatExpr.sigC;
					break;
				case 'B':
				case 'S':
				case 'I':
					sign = StringConcatExpr.sigI;
					break;
				case 'J':
					sign = StringConcatExpr.sigJ;
					break;
				case 'F':
					sign = StringConcatExpr.sigF;
					break;
				case 'D':
					sign = StringConcatExpr.sigD;
					break;
				}
				Method m = StringConcatExpr.clazzStringBuffer.resolveMethod(
					nameStrBuffAppend,sign);
				Code.addInstr(op_call,m,false);
			}
			else
				Code.addInstr(op_call,func,super_flag,obj.getType());
			if( func.type.ret != Type.tpVoid ) {
				if( reqType==Type.tpVoid )
					Code.addInstr(op_pop);
				else if( Kiev.verify
				 && Type.getRealType(Kiev.argtype,getType()).isReference()
//				 && func.jtype!= null
				 && ( !getType().clazz.equals(func.type.ret.clazz) || getType().isArray() ) )
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

public class ClosureCallExpr extends Expr {
	public Expr		expr;
	public ASTNode	func;	// Var or Field
	public Expr[]	args;
	public Expr		env_access;		// $env for rule closures
	public boolean	is_a_call = false;

	public Method	clone_it;
	public Method	call_it;
	public Method[]	addArg;
	public Type		func_tp;

	public ClosureCallExpr(int pos, ASTNode func, Expr[] args) {
		super(pos);
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public ClosureCallExpr(int pos, ASTNode par, ASTNode func, Expr[] args) {
		super(pos,par);
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public ClosureCallExpr(int pos, Expr expr, ASTNode func, Expr[] args) {
		super(pos);
		this.expr = expr;
		this.expr.parent = this;
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public ClosureCallExpr(int pos, ASTNode par, Expr expr, ASTNode func, Expr[] args) {
		super(pos,par);
		this.expr = expr;
		this.expr.parent = this;
		this.func = func;
		this.args = args;
		foreach(Expr e; args; e!=null) e.parent = this;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(func).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
	public Type getType() {
		Type tp1 = expr==null?null:expr.getType();
		MethodType t = (MethodType)Type.getRealType(tp1,((Expr)func).getType());
		if( is_a_call )
			return t.ret;
		Type[] types = new Type[t.args.length - args.length];
		for(int i=0; i < types.length; i++) types[i] = t.args[i+args.length];
		t = MethodType.newMethodType(t.clazz,null,types,t.ret);
		return t;
	}

	public void cleanup() {
		parent=null;
		if( expr != null ) {
			expr.cleanup();
			expr = null;
		}
		func = null;
		foreach(ASTNode n; args; args!=null) n.cleanup();
		args = null;
		if( env_access != null ) {
			env_access.cleanup();
			env_access = null;
		}
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return this;
		PassInfo.push(this);
		try {
			if( expr != null )
				expr = (Expr)expr.resolve(null);
			ASTNode v = func;
			Type tp1 = expr==null?null:expr.getType();
			Type tp;
			if( v instanceof Expr && (tp=Type.getRealType(tp1,((Expr)v).getType())) instanceof MethodType )
				func = (Expr)v;
			else if( v instanceof Var && (tp=Type.getRealType(tp1,((Var)v).getType())) instanceof MethodType )
				func = new VarAccessExpr(pos,this,(Var)v).resolve(null);
			else if( v instanceof Field && (tp=Type.getRealType(tp1,((Field)v).getType())) instanceof MethodType )
				if( ((Field)v).isStatic() )
					func = (Expr)new StaticFieldAccessExpr(pos,PassInfo.clazz,(Field)v).resolve(null);
				else if( expr == null )
					func = (Expr)new FieldAccessExpr(pos,(Field)v).resolve(null);
				else {
					func = (Expr)new AccessExpr(pos,parent,expr,(Field)v).resolve(null);
					expr = null;
				}
			else
				throw new RuntimeException("Resolved item "+v+" is not a closure");
			if( reqType != null && reqType instanceof MethodType )
				is_a_call = false;
			else if( (reqType == null || !(reqType instanceof MethodType)) && tp.args.length==args.length )
				is_a_call = true;
			else
				is_a_call = false;
			func_tp = tp;
			for(int i=0; i < args.length; i++)
				args[i] = args[i].resolveExpr(tp.args[i]);
//			addArg = new Method[args.length];
//			for(int i=0; i < args.length; i++) {
//				if( !args[i].getType().equals(tp.args[i]) ) {
//					if( !args[i].getType().isAutoCastableTo(tp.args[i]) )
//						throw new RuntimeException("Closure arg "+i+": "+args[i].getType()+" is not auto-castable to "+((Expr)func).getType().args[i]);
//					else
//						args[i] = (Expr)new CastExpr(args[i].getPos(),tp.args[i],args[i]);
//				}
//				PVar<ASTNode> addArgM = new PVar<ASTNode>();
//				if( !PassInfo.resolveBestMethodR(tp.clazz,addArgM,new PVar<List<ASTNode>>(List.Nil),KString.from("addArg"),new Expr[]{args[i]},null,reqType,0) ) {
//					throw new RuntimeException("Can't resolve method "+Method.toString(KString.from("addArg"),new Expr[]{args[i]})+" in class "+tp.clazz);
//				} else {
//					addArg[i] = (Method)addArgM;
//				}
//			}
			clone_it = tp.clazz.resolveMethod(nameClone,KString.from("()Ljava/lang/Object;"));
			KString call_it_name;
			if( ((MethodType)tp).ret.isReference() )
				call_it_name = KString.from("call_Object");
			else
				call_it_name = KString.from("call_"+((MethodType)tp).ret);
			PVar<ASTNode> callIt = new PVar<ASTNode>();
			if( !PassInfo.resolveBestMethodR(tp.clazz,callIt,new ResInfo(),call_it_name,Expr.emptyArray,null,reqType,ResolveFlags.NoForwards) ) {
				throw new RuntimeException("Can't resolve method "+Method.toString(call_it_name,new Expr[0])+" in class "+tp.clazz);
			} else {
				call_it = (Method)callIt;
				if( call_it.type.ret == Type.tpRule ) {
					env_access = CallExpr.insertPEnvForRuleCall(args,this)[0];
				} else {
					trace(Kiev.debugResolve,"CallExpr "+this+" is not a rule call");
				}
			}
		} finally { PassInfo.pop(this); }
		setResolved(true);
		return this;
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
		Type t = Type.getRealType(Kiev.argtype,tp);
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
			if( expr != null )
				expr.generate(null);
			// Load ref to closure
			((Expr)func).generate(null);
			// Clone it
			if( args.length > 0 ) {
				Code.addInstr(op_call,clone_it,false);
				if( Kiev.verify )
					Code.addInstr(op_checkcast,Type.tpClosureClazz.type);
				// Add arguments
				for(int i=0; i < args.length; i++) {
					args[i].generate(null);
//					Code.addInstr(op_call,addArg[i],false);
					Code.addInstr(op_call,getMethodFor(func_tp.args[i]),false);
//					Code.addInstr(op_checkcast,Type.tpClosureClazz.type);
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
//				 && call_it.jtype!= null
				 && ( !getType().clazz.equals(call_it.type.ret.clazz) || getType().isArray() ) )
				 	Code.addInstr(op_checkcast,getType());
			}
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public Dumper toJava(Dumper dmp) {
		func.toJava(dmp).append(".clone()");
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
