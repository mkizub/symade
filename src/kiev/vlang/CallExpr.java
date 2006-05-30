package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
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
	@ref public CallType			mt;
	@att public ENode[]				args;

	@getter public Method get$func() {
		if (ident == null) return null;
		DNode sym = ident.symbol;
		if (sym instanceof Method)
			return (Method)sym;
		return null;
	}
	@setter public void set$func(Method m) {
		this.ident.symbol = m;
	}

	@nodeview
	public static final view VCallExpr of CallExpr extends VENode {
		public:ro	Method			func;
		public		ENode			obj;
		public		CallType		mt;
		public:ro	ENode[]			args;

		public void mainResolveOut() {
			if (func != null)
				return;
			if( obj instanceof ASTIdentifier
			&& ((ASTIdentifier)obj).name.equals(Constants.nameSuper)
			&& !ctx_method.isStatic() )
			{
				ThisExpr te = new ThisExpr(obj.pos);
				te.setSuperExpr(true);
				obj = te;
			}
			
			if (obj instanceof ThisExpr && obj.isSuperExpr()) {
				Method@ m;
				Type tp = ctx_tdecl.super_types[0].getType();
				ResInfo info = new ResInfo(this);
				info.enterForward(obj);
				info.enterSuper();
				Type[] ta = new Type[args.length];
				for (int i=0; i < ta.length; i++)
					ta[i] = args[i].getType();
				CallType mt = new CallType(tp,null,ta,null,false);
				try {
					if( !PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt) )
						throw new CompilerException(obj,"Unresolved method "+Method.toString(ident.name,args,null));
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				info.leaveSuper();
				info.leaveForward(obj);
				if( info.isEmpty() ) {
					this.ident.symbol = m;
					this.mt = info.mt;
					this.setSuperExpr(true);
					return;
				}
				throw new CompilerException(obj,"Super-call via forwarding is not allowed");
			}
			
			int res_flags = ResInfo.noStatic | ResInfo.noImports;
			ENode[] res;
			Type[] tps;
		try_static:;
			if( obj instanceof TypeRef ) {
				Type tp = ((TypeRef)obj).getType();
				tps = new Type[]{tp};
				res = new ENode[1];
				res_flags = 0;
			} else {
				tps = obj.getAccessTypes();
				res = new ENode[tps.length];
				// fall down
			}
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				Method@ m;
				ResInfo info = new ResInfo(this,res_flags);
				CallType mt = this.mt;
				if (mt == null) {
					Type[] ta = new Type[args.length];
					for (int i=0; i < ta.length; i++)
						ta[i] = args[i].getType();
					mt = new CallType(res_flags==0?null:tp,null,ta,null,false);
				}
				try {
					if (PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt)) {
						if (tps.length == 1 && res_flags == 0)
							res[si] = info.buildCall((ASTNode)this, obj, m, info.mt, args);
						else if (res_flags == 0)
							res[si] = info.buildCall((ASTNode)this, new TypeRef(tps[si]), m, info.mt, args);
						else
							res[si] = info.buildCall((ASTNode)this, obj, m, info.mt, args);
					}
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
			}
			int cnt = 0;
			int idx = -1;
			for (int si=0; si < res.length; si++) {
				if (res[si] != null) {
					cnt ++;
					if (idx < 0) idx = si;
				}
			}
			if (cnt > 1) {
				StringBuffer msg = new StringBuffer("Umbigous methods:\n");
				for(int si=0; si < res.length; si++) {
					if (res[si] == null)
						continue;
					msg.append("\t").append(res[si]).append('\n');
				}
				msg.append("while resolving ").append(this);
				throw new CompilerException(this, msg.toString());
			}
			if (cnt == 0 && res_flags != 0) {
				res_flags = 0;
				goto try_static;
			}
			if (cnt == 0) {
				StringBuffer msg = new StringBuffer("Unresolved method '"+Method.toString(ident.name,mt)+"' in:\n");
				for(int si=0; si < res.length; si++) {
					if (tps[si] == null)
						continue;
					msg.append("\t").append(tps[si]).append('\n');
				}
				msg.append("while resolving ").append(this);
				throw new CompilerException(this, msg.toString());
			}
			ENode e = res[idx];
			if (e instanceof UnresCallExpr) {
				if (e.obj == this.obj) {
					this.ident.symbol = e.func.symbol;
					this.mt = e.mt;
					return;
				}
			}
			if (e instanceof UnresExpr)
				e = ((UnresExpr)e).toResolvedExpr();
			if (isPrimaryExpr())
				e.setPrimaryExpr(true);
			this.replaceWithNode( e );
		}
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
		this(pos, obj, new SymbolRef(pos,func), mt, args, super_flag);
	}

	public CallExpr(int pos, ENode obj, Method func, CallType mt, ENode[] args) {
		this(pos, obj, new SymbolRef(pos,func), mt, args, false);
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, new SymbolRef(pos,func), null, args, false);
	}

	public ENode[] getArgs() {
		if (func.isStatic())
			return this.args;
		ENode[] args = new ENode[this.args.length+1];
		args[0] = obj;
		for (int i=0; i < this.args.length; i++)
			args[i+1] = this.args[i];
		return args;
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
		sb.append(ident).append('(');
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

	public Object doRewrite(RewriteContext ctx) {
		if (func == null || func.body == null || !func.isMacro())
			super.doRewrite(ctx);
		int idx = -1;
		Object[] args = new Object[this.args.length];
		foreach(FormPar fp; func.params; fp.kind == FormPar.PARAM_NORMAL) {
			idx++;
			if (fp.type instanceof ASTNodeType)
				args[idx] = this.args[idx].doRewrite(ctx);
			else
				args[idx] = this.args[idx];
		}
		return func.body.doRewrite(new RewriteContext(this, args));
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
	@att public ENode[]				args;
	@att public Boolean				is_a_call;

	@nodeview
	public static final view VClosureCallExpr of ClosureCallExpr extends VENode {
		public		ENode			expr;
		public:ro	ENode[]			args;
		public		Boolean			is_a_call;

		public Method getCallIt(CallType tp);
	}
	
	public ClosureCallExpr() {}

	public ClosureCallExpr(int pos, ENode expr, ENode[] args) {
		this.pos = pos;
		this.expr = expr;
		this.args.addAll(args);
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
		t = new CallType(null,null,types,t.ret(),true);
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
