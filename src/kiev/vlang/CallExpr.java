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
package kiev.vlang;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@ThisIsANode(name="Call", lang=CoreLang)
public class CallExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode		obj;
	@DataFlowDefinition(in="obj", seq="true")		ENode[]		args;
	}
	
	public static final ExtSpaceAttrSlot TI_EXT_ARG = new ExtSpaceAttrSlot<ENode>("ti-call-args",ANode.nodeattr$parent,TypeInfo.newTypeInfo(ENode.class,null));	

	@nodeAttr				public ENode				obj;
	@nodeAttr				public TypeRef∅			targs;
	@nodeAttr				public ENode∅				args;

	@getter public Method get$func() {
		return (Method)this.dnode;
	}
	@setter public void set$func(Method m) {
		this.symbol = m;
	}

	public CallExpr() {}

	public CallExpr(int pos, ENode obj, SymbolRef<Method> ident, TypeRef[] targs, ENode[] args) {
		this.pos = pos;
		if (ident.symbol != null)
			this.symbol = ident.symbol;
		else
			this.ident = ident.name;
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
		Type ret = m.mtype.ret();
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
			if (func.ctx_root == this.ctx_root)
				return;
			this.ident = func.sname;
		}
		
		// constructor call "this(args)" or "super(args)"
		if (ident == nameThis || ident == nameSuper) {
			CtorCallExpr cce;
			if (ident == nameThis)
				cce = new CtorCallExpr(pos, new ThisExpr(), args.delToArray());
			else
				cce = new CtorCallExpr(pos, new SuperExpr(), args.delToArray());
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
			ResInfo info = new ResInfo(this,this.ident);
			info.enterForward(obj);
			info.enterSuper();
			mt = new CallType(tp,ata,ta,null,false);
			try {
				if( !PassInfo.resolveBestMethodR(tp,m,info,mt) )
					throw new CompilerException(obj,"Unresolved method "+Method.toString(this.ident,args,null));
			} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
			info.leaveSuper();
			info.leaveForward(obj);
			if( info.isEmpty() ) {
				this.symbol = m;
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
				ResInfo info = new ResInfo(this, this.ident, ResInfo.noStatic | ResInfo.noImports);
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
				ResInfo info = new ResInfo(this, this.ident, ResInfo.noStatic | ResInfo.noImports);
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
				ResInfo info = new ResInfo(this, this.ident);
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
				ResInfo info = new ResInfo(this, this.ident, ResInfo.noImports);
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
			if (ctx_method != null && ctx_method.isMacro())
				return;
			StringBuffer msg = new StringBuffer("Unresolved method '"+Method.toString(this.ident,args)+"' in:\n");
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
					this.symbol = e.func.symbol;
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
		Method func = this.func;
		if (func == null && ctx_method != null && ctx_method.isMacro())
			return true;
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
	public ANode doRewrite(RewriteContext ctx) {
		if (func == null || func.body == null || !func.isMacro())
			return super.doRewrite(ctx);
		int idx = 0;
		Hashtable<String,Object> args = new Hashtable<String,Object>();
		foreach (Var fp; func.params; fp.kind == Var.PARAM_NORMAL) {
			if (fp.getType().getErasedType() instanceof ASTNodeType)
				args.put(fp.sname, this.args[idx++].doRewrite(ctx));
			else
				args.put(fp.sname, this.args[idx++]);
		}
		ASTNode rewriter = func.body;
		if (rewriter instanceof RewriteMatch)
			rewriter = rewriter.matchCase(this);
		//rewriter = rewriter.ncopy();
		return rewriter.doRewrite(new RewriteContext(this, args));
	}
}

@ThisIsANode(name="CtorCall", lang=CoreLang)
public class CtorCallExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode		obj;
	@DataFlowDefinition(in="obj")					ENode		tpinfo;
	@DataFlowDefinition(in="tpinfo", seq="true")	ENode[]		args;
	}
	
	public static final ExtSpaceAttrSlot TI_EXT_ARG = new ExtSpaceAttrSlot<ENode>("ti-ctor-args",ANode.nodeattr$parent,TypeInfo.newTypeInfo(ENode.class,null));	
	public static final ExtSpaceAttrSlot ENUM_EXT_ARG = new ExtSpaceAttrSlot<ENode>("enum-args",ANode.nodeattr$parent,TypeInfo.newTypeInfo(ENode.class,null));	

	@nodeAttr					public ENode				obj;
	@nodeAttr(ext_data=true)	public ENode				tpinfo;
	@nodeAttr					public ENode∅				args;

	@getter public Method get$func() {
		return (Method)this.dnode;
	}
	@setter public void set$func(Method m) {
		this.symbol = m;
	}

	public CtorCallExpr() {}

	public CtorCallExpr(int pos, ENode obj, ENode[] args) {
		this.pos = pos;
		this.obj = obj;
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
			if (func.ctx_root == this.ctx_root)
				return;
		}
		
		Constructor@ m;
		Type tp = ctx_tdecl.xtype;

		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
		CallType mt = new CallType(tp,null,ta,Type.tpVoid,false);

		// constructor call "this(args)"
		if (obj instanceof ThisExpr) {
			ResInfo info = new ResInfo(this,null,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if (!PassInfo.resolveBestMethodR(tp,m,info,mt))
				throw new CompilerException(this,"Constructor "+Method.toString("<constructor>",args)+" unresolved");
			this.symbol = (Constructor)m;
			return;
		}
		// constructor call "super(args)"
		if (obj instanceof SuperExpr) {
			mt = new CallType(tp,null,ta,Type.tpVoid,false);
			ResInfo info = new ResInfo(this,null,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if (!PassInfo.resolveBestMethodR(ctx_tdecl.super_types[0].getType(),m,info,mt))
				throw new CompilerException(this,"Constructor "+Method.toString("<constructor>",args)+" unresolved");
			this.symbol = (Constructor)m;
			return;
		}
		throw new CompilerException(this, "Constructor call may only be 'super' or 'this'");
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(obj).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		return sb.toString();
	}
}

@ThisIsANode(name="CallClosure", lang=CoreLang)
public class ClosureCallExpr extends ENode {
	
	@DataFlowDefinition(out="args") private static class DFI {
	@DataFlowDefinition(in="this:in")				ENode		expr;
	@DataFlowDefinition(in="expr", seq="true")		ENode[]		args;
	}
	
	@nodeAttr public ENode					expr;
	@nodeAttr public ENode∅				args;
	@nodeAttr public Boolean				is_a_call;

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
