package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.Operator.*;
import kiev.vlang.types.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RCallExpr;
import kiev.be.java15.JCallExpr;
import kiev.ir.java15.RClosureCallExpr;
import kiev.be.java15.JClosureCallExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@node
public class CallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = CallExpr;
	@virtual typedef VView = VCallExpr;
	@virtual typedef JView = JCallExpr;
	@virtual typedef RView = RCallExpr;

	@att public ENode				obj;
	@att public SymbolRef			ident;
	@ref public CallType			mt;
	@att public NArr<ENode>			args;

	@getter public Method get$func() {
		if (ident == null) return null;
		Symbol sym = ident.symbol;
		if (sym == null) return null;
		ASTNode res = sym.parent();
		if (res instanceof Method)
			return (Method)res;
		return null;
	}

	@nodeview
	public static final view VCallExpr of CallExpr extends VENode {
		public		ENode			obj;
		public		SymbolRef		ident;
		public:ro	Method			func;
		public		CallType		mt;
		public:ro	NArr<ENode>		args;
	}
	
	public CallExpr() {}

	public CallExpr(int pos, ENode obj, SymbolRef ident, CallType mt, ENode[] args, boolean super_flag) {
		this.pos = pos;
		this.ident = ident;
		if (obj == null) {
			if !(func.isStatic() || func instanceof Constructor)
				throw new RuntimeException("Call to non-static method "+func+" without accessor");
			this.obj = new TypeRef(func.ctx_tdecl.xtype);
		} else {
			this.obj = obj;
		}
		this.mt = mt;
		this.args.addAll(args);
		if (super_flag)
			this.setSuperExpr(true);
	}

	public CallExpr(int pos, ENode obj, Method func, CallType mt, ENode[] args, boolean super_flag) {
		this(pos, obj, new SymbolRef(pos,func.id), mt, args, super_flag);
	}

	public CallExpr(int pos, ENode obj, Method func, CallType mt, ENode[] args) {
		this(pos, obj, new SymbolRef(pos,func.id), mt, args, false);
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, new SymbolRef(pos,func.id), null, args, false);
	}

	public int getPriority() { return Constants.opCallPriority; }

	public Type getType() {
		if (mt == null)
			return Type.getRealType(obj.getType(),func.type.ret());
		else
			return mt.ret();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if( obj.getPriority() > opAccessPriority )
			sb.append('(').append(obj).append(").");
		else
			sb.append(obj).append('.');
		sb.append(func.id).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		if( func.getName().equals(nameInit) ) {
			if( isSuperExpr() )
				dmp.append(nameSuper);
			else
				dmp.append(nameThis);
		} else {
			if( obj != null ) {
				if( obj.getPriority() < opCallPriority ) {
					dmp.append('(').append(obj).append(").");
				} else {
					dmp.append(obj).append('.');
				}
			}
			else if( isSuperExpr() )
				dmp.append("super.");
			else if( func instanceof Method && func.isStatic() )
				dmp.append(func.ctx_tdecl.qname()).append('.');
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
public class ClosureCallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		expr;
	@dflow(in="expr", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = ClosureCallExpr;
	@virtual typedef VView = VClosureCallExpr;
	@virtual typedef JView = JClosureCallExpr;
	@virtual typedef RView = RClosureCallExpr;

	@att public ENode				expr;
	@att public NArr<ENode>			args;
	@att public Boolean				is_a_call;

	@nodeview
	public static final view VClosureCallExpr of ClosureCallExpr extends VENode {
		public		ENode			expr;
		public:ro	NArr<ENode>		args;
		public		Boolean			is_a_call;

		public Method getCallIt(CallType tp);
	}
	
	public ClosureCallExpr() {}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		this.pos = pos;
		this.expr = expr;
		foreach(ENode e; args) this.args.append(e);
	}

	public int getPriority() { return Constants.opCallPriority; }

	public Type getType() {
		CallType t = (CallType)expr.getType();
		if (is_a_call == null)
			is_a_call = Boolean.valueOf(t.arity==args.length);
		if (is_a_call.booleanValue())
			return t.ret();
		Type[] types = new Type[t.arity - args.length];
		for(int i=0; i < types.length; i++) types[i] = t.arg(i+args.length);
		t = new CallType(types,t.ret(),true);
		return t;
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

	public Method getCallIt(CallType tp) {
		String call_it_name;
		Type ret;
		if( tp.ret().isReference() ) {
			call_it_name = "call_Object";
			ret = Type.tpObject;
		} else {
			call_it_name = ("call_"+tp.ret()).intern();
			ret = tp.ret();
		}
		return Type.tpClosureClazz.resolveMethod(call_it_name, ret);
	}
	
	public Dumper toJava(Dumper dmp) {
		expr.toJava(dmp).append(".clone()");
		for(int i=0; i < args.length; i++) {
			dmp.append(".addArg(");
			args[i].toJava(dmp);
			dmp.append(')');
		}
		if (is_a_call == null)
			is_a_call = Boolean.valueOf(((CallType)expr.getType()).arity==args.length);
		if (is_a_call.booleanValue()) {
			Method call_it = getCallIt((CallType)expr.getType());
			dmp.append('.').append(call_it.id).append('(');
			if( call_it.type.ret() ≡ Type.tpRule ) dmp.append("null");
			dmp.append(')');
		}
		return dmp;
	}
}
