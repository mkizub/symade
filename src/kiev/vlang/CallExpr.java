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
import kiev.ir.java15.RCtorCallExpr;
import kiev.be.java15.JCtorCallExpr;
import kiev.ir.java15.RClosureCallExpr;
import kiev.be.java15.JClosureCallExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@node(name="Call")
public class CallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = CallExpr;
	@virtual typedef JView = JCallExpr;
	@virtual typedef RView = RCallExpr;
	@virtual typedef TypeOfIdent = Method;

	@att public ENode				obj;
	@att public TypeRef[]			targs;
	@att public ENode[]				args;

	@getter public Method get$func() {
		return (Method)ident.symbol;
	}
	@setter public void set$func(Method m) {
		this.ident.symbol = m;
	}

	public CallExpr() {}

	public CallExpr(int pos, ENode obj, SymbolRef<Method> ident, TypeRef[] targs, ENode[] args) {
		this.pos = pos;
		this.ident = ident;
		this.obj = obj;
		this.targs.addAll(targs);
		this.args.addAll(args);
	}

	public CallExpr(int pos, ENode obj, Method func, TypeRef[] targs, ENode[] args) {
		this(pos, obj, new SymbolRef<Method>(pos,func), targs, args);
	}

	public CallExpr(int pos, ENode obj, Method func, ENode[] args) {
		this(pos, obj, new SymbolRef<Method>(pos,func), null, args);
	}

	public ENode[] getArgs() {
		if (func == null || func.isStatic())
			return this.args;
		ENode[] args = new ENode[this.args.length+1];
		args[0] = obj;
		for (int i=0; i < this.args.length; i++)
			args[i+1] = this.args[i];
		return args;
	}

	public int getPriority() { return Constants.opCallPriority; }

	public Type getType() {
		Method m = this.func;
		if (m == null)
			return Type.tpVoid;
		Type ret = m.type.ret();
		if (!(ret instanceof ArgType) && !ret.isAbstract()) return ret;
		return getCallType().ret();
	}

	public CallType getCallType() {
		Method m = this.func;
		if (m == null)
			return new CallType(null,null,null,Type.tpVoid,false);
		return this.func.makeType(this.targs, this.getArgs());
	}

	public void mainResolveOut() {
		if (func != null) {
			if (obj == null) {
				assert (func.isStatic() || func instanceof Constructor);
				obj = new TypeRef(func.ctx_tdecl.xtype);
			}
			return;
		}
		
		// constructor call "this(args)" or "super(args)"
		if (ident.name == nameThis || ident.name == nameSuper) {
			CtorCallExpr cce = new CtorCallExpr(pos, new SymbolRef<Constructor>(ident.pos,ident.name), args.delToArray());
			if (isPrimaryExpr())
				cce.setPrimaryExpr(true);
			this.replaceWithNodeReWalk(cce);
			return;
		}

		Method@ m;
		Type tp = ctx_tdecl.xtype;
		CallType mt = null;

		Type[] ata = new Type[targs.length];
		for (int i=0; i < ata.length; i++)
			ata[i] = targs[i].getType();
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();

		// super-call "super.func(args)"
		if (obj instanceof SuperExpr) {
			Method@ m;
			Type tp = ctx_tdecl.super_types[0].getType();
			ResInfo info = new ResInfo(this,ident.name);
			info.enterForward(obj);
			info.enterSuper();
			mt = new CallType(tp,ata,ta,null,false);
			try {
				if( !PassInfo.resolveBestMethodR(tp,m,info,mt) )
					throw new CompilerException(obj,"Unresolved method "+Method.toString(ident.name,args,null));
			} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
			info.leaveSuper();
			info.leaveForward(obj);
			if( info.isEmpty() ) {
				this.ident.symbol = m;
				this.setSuperExpr(true);
				return;
			}
			throw new CompilerException(obj,"Super-call via forwarding is not allowed");
		}
		
		int cnt = 0;
		ENode[] res;
		Type[] tps;
		if (obj == null) {
			tps = new Type[]{tp};
			res = new ENode[1];
		}
		else if( obj instanceof TypeRef ) {
			Type otp = ((TypeRef)obj).getType();
			tps = new Type[]{otp};
			res = new ENode[1];
			goto try_static;
		} else {
			tps = obj.getAccessTypes();
			res = new ENode[tps.length];
		}

		if (cnt == 0) {
			// try virtual call first
			for (int si=0; si < tps.length; si++) {
				tp = tps[si];
				Method@ m;
				ResInfo info = new ResInfo(this, ident.name, ResInfo.noStatic | ResInfo.noImports);
				mt = new CallType(tp,ata,ta,null,false);
				if (obj == null) {
					if (PassInfo.resolveMethodR((ASTNode)this,m,info,mt)) {
						res[si] = info.buildCall((ASTNode)this, obj, m, targs, args);
						cnt += 1;
					}
				} else {
					if (PassInfo.resolveBestMethodR(tp,m,info,mt)) {
						res[si] = info.buildCall((ASTNode)this, obj, m, targs, args);
						cnt += 1;
					}
				}
			}
		}

		if (cnt == 0) {
			// try closure var or an instance fiels
			for (int si=0; si < tps.length; si++) {
				tp = tps[si];
				DNode@ closure;
				ResInfo info = new ResInfo(this, ident.name, ResInfo.noStatic | ResInfo.noImports);
				if (obj == null) {
					if (PassInfo.resolveNameR((ASTNode)this,closure,info)) { 
						if ((closure instanceof Var || closure instanceof Field)
							&& Type.getRealType(tp,closure.getType()) instanceof CallType
						) {
							res[si] = info.buildCall((ASTNode)this, obj, closure, targs, args);
							cnt += 1;
						}
					}
				} else {
					if (tp.resolveNameAccessR(closure,info)) { 
						if ((closure instanceof Var || closure instanceof Field)
							&& Type.getRealType(tp,closure.getType()) instanceof CallType
						) {
							res[si] = info.buildCall((ASTNode)this, obj, closure, targs, args);
							cnt += 1;
						}
					}
				}
			}
		}

	try_static:;
		if (cnt == 0) {
			// try static call
			for (int si=0; si < tps.length; si++) {
				tp = tps[si];
				Method@ m;
				ResInfo info = new ResInfo(this, ident.name);
				mt = new CallType(null,ata,ta,null,false);
				if (obj == null) {
					if (PassInfo.resolveMethodR((ASTNode)this,m,info,mt)) {
						res[si] = info.buildCall((ASTNode)this, obj, m, targs, args);
						cnt += 1;
					}
				} else {
					if (PassInfo.resolveBestMethodR(tp,m,info,mt)) {
						res[si] = info.buildCall((ASTNode)this, obj, m, targs, args);
						cnt += 1;
					}
				}
			}
		}
		
		if (cnt == 0) {
			// try closure static field
			for (int si=0; si < tps.length; si++) {
				tp = tps[si];
				DNode@ closure;
				ResInfo info = new ResInfo(this, ident.name, ResInfo.noImports);
				if (obj == null) {
					if (PassInfo.resolveNameR((ASTNode)this,closure,info)) { 
						if (closure instanceof Field
							&& Type.getRealType(tp,closure.getType()) instanceof CallType
						) {
							res[si] = info.buildCall((ASTNode)this, obj, closure, targs, args);
							cnt += 1;
						}
					}
				} else {
					if (tp.meta_type.tdecl.resolveNameR(closure,info)) { 
						if (closure instanceof Field
							&& Type.getRealType(tp,closure.getType()) instanceof CallType
						) {
							res[si] = info.buildCall((ASTNode)this, obj, closure, targs, args);
							cnt += 1;
						}
					}
				}
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
		else if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved method '"+Method.toString(ident.name,args)+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		
		for (int si=0; si < res.length; si++) {
			ENode e = res[si];
			if (e != null) {
				if (e instanceof UnresCallExpr && e.obj == this.obj) {
					this.ident.symbol = e.func.symbol;
					return;
				}
				e = e.closeBuild();
				if (isPrimaryExpr())
					e.setPrimaryExpr(true);
				this.replaceWithNodeReWalk(e);
			}
		}
	}

	// verify resolved call
	public boolean preVerify() {
		if (func.isStatic() && !func.isVirtualStatic() && !(obj instanceof TypeRef))
			obj = new TypeRef(func.ctx_tdecl.xtype);
		return true;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (obj != null) {
			if( obj.getPriority() > opAccessPriority )
				sb.append('(').append(obj).append(").");
			else
				sb.append(obj).append('.');
		}
		sb.append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
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

@node(name="CtorCall")
public class CtorCallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = CtorCallExpr;
	@virtual typedef JView = JCtorCallExpr;
	@virtual typedef RView = RCtorCallExpr;
	@virtual typedef TypeOfIdent = Constructor;

	@att public ENode[]				args;

	@getter public Method get$func() {
		return (Method)ident.symbol;
	}
	@setter public void set$func(Method m) {
		this.ident.symbol = m;
	}

	public CtorCallExpr() {}

	public CtorCallExpr(int pos, SymbolRef<Constructor> ident, ENode[] args) {
		this.pos = pos;
		this.ident = ident;
		this.args.addAll(args);
	}

	public ENode[] getArgs() {
		return this.args;
	}

	public int getPriority() { return Constants.opCallPriority; }

	public Type getType() {
		return Type.tpVoid;
	}

	public CallType getCallType() {
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		return new CallType(ctx_tdecl.xtype,null,ta,Type.tpVoid,false);
	}

	public void mainResolveOut() {
		if (func != null) {
			assert (func instanceof Constructor);
			return;
		}
		
		Method@ m;
		Type tp = ctx_tdecl.xtype;

		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = new CallType(tp,null,ta,Type.tpVoid,false);

		// constructor call "this(args)"
		if (ident.name == nameThis) {
			ResInfo info = new ResInfo(this,nameInit,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if (!PassInfo.resolveBestMethodR(tp,m,info,mt))
				throw new CompilerException(this,"Constructor "+Method.toString(ident.name,args)+" unresolved");
			ident.symbol = (Constructor)m;
			return;
		}
		// constructor call "super(args)"
		if (ident.name == nameSuper) {
			mt = new CallType(tp,null,ta,Type.tpVoid,false);
			ResInfo info = new ResInfo(this,nameInit,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if (!PassInfo.resolveBestMethodR(ctx_tdecl.super_types[0].getType(),m,info,mt))
				throw new CompilerException(this,"Constructor "+Method.toString(ident.name,args)+" unresolved");
			ident.symbol = (Constructor)m;
			return;
		}
		throw new CompilerException(this, "Constructor call may only be 'super' or 'this'");
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
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

@node(name="CallClosure")
public class ClosureCallExpr extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		expr;
	@dflow(in="expr", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = ClosureCallExpr;
	@virtual typedef JView = JClosureCallExpr;
	@virtual typedef RView = RClosureCallExpr;

	@att public ENode				expr;
	@att public ENode[]				args;
	@att public Boolean				is_a_call;

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
}
