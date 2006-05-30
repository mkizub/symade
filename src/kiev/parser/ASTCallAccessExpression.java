/* Generated By:JJTree: Do not edit this line. ASTCallAccessExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class ASTCallAccessExpression extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@virtual typedef This  = ASTCallAccessExpression;
	@virtual typedef VView = VASTCallAccessExpression;

	@att public ENode				obj;
	@att public TypeRef[]			targs;
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
	public static view VASTCallAccessExpression of ASTCallAccessExpression extends VENode {
		public		ENode			obj;
		public:ro	TypeRef[]		targs;
		public:ro	ENode[]			args;

		public void mainResolveOut() {
			if( obj instanceof ASTIdentifier
			&& ((ASTIdentifier)obj).name.equals(Constants.nameSuper)
			&& !ctx_method.isStatic() )
			{
				ThisExpr te = new ThisExpr(obj.pos);
				te.setSuperExpr(true);
				obj = te;
			}
			
			Type[] ata = new Type[targs.length];
			for (int i=0; i < ata.length; i++)
				ata[i] = targs[i].getType();
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();

			if (obj instanceof ThisExpr && obj.isSuperExpr()) {
				Method@ m;
				Type tp = ctx_tdecl.super_types[0].getType();
				ResInfo info = new ResInfo(this);
				info.enterForward(obj);
				info.enterSuper();
				CallType mt = new CallType(tp,ata,ta,null,false);
				try {
					if( !PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt) )
						throw new CompilerException(obj,"Unresolved method "+Method.toString(ident.name,args,null));
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				info.leaveSuper();
				info.leaveForward(obj);
				if( info.isEmpty() ) {
					Method meth = (Method)m;
					CallExpr cae = new CallExpr(pos,~obj,meth,((ASTCallAccessExpression)this).args.delToArray());
					cae.setSuperExpr(true);
					replaceWithNode(cae);
					return;
				}
				throw new CompilerException(obj,"Super-call via forwarding is not allowed");
			}
			
			CallType mt = null;
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
				mt = new CallType(res_flags==0?null:tp,ata,ta,null,false);
				try {
					if (PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt)) {
						if (tps.length == 1 && res_flags == 0)
							res[si] = info.buildCall((ASTNode)this, obj, m, info.mt, args);
						else if (res_flags == 0)
							res[si] = info.buildCall((ASTNode)this, new TypeRef(tps[si]), m, info.mt, args);
						else
							res[si] = info.buildCall((ASTNode)this, obj.ncopy(), m, info.mt, args);
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
			if (e instanceof UnresExpr)
				e = ((UnresExpr)e).toResolvedExpr();
			if (isPrimaryExpr())
				e.setPrimaryExpr(true);
			this.replaceWithNode( e );
		}
	}
	
	public ASTCallAccessExpression() {}
	
	public ASTCallAccessExpression(int pos, ENode obj, String func, ENode[] args) {
		this.pos = pos;
		this.obj = obj;
		this.ident = new SymbolRef(pos, func);
		this.args.addAll(args);
	}

	public int		getPriority() { return Constants.opCallPriority; }

	public void resolve(Type reqType) {
		for(int i=0; i < args.length; i++) {
			args[i].resolve(null);
		}
		
		if( obj instanceof ASTIdentifier
		&& ((ASTIdentifier)obj).name.equals(Constants.nameSuper)
		&& !ctx_method.isStatic() )
		{
			ThisExpr te = new ThisExpr(obj.pos);
			te.setSuperExpr(true);
			obj = te;
		}
		
		Type[] ata = new Type[targs.length];
		for (int i=0; i < ata.length; i++)
			ata[i] = targs[i].getType();
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();

		if (obj instanceof ThisExpr && obj.isSuperExpr()) {
			Type ret = reqType;
			Method@ m;
			Type tp = ctx_tdecl.super_types[0].getType();
	retry_with_null_ret:;
			ResInfo info = new ResInfo(this);
			info.enterForward(obj);
			info.enterSuper();
			CallType mt = new CallType(tp,ata,ta,ret,false);
			try {
				if( !PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt) ) {
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(obj,"Unresolved method "+Method.toString(ident.name,args,ret));
				}
			} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
			info.leaveSuper();
			info.leaveForward(obj);
			if( info.isEmpty() ) {
				Method meth = (Method)m;
				CallExpr cae = new CallExpr(pos,~obj,meth,args.delToArray());
				cae.setSuperExpr(true);
				replaceWithNode(cae);
				cae.resolve(ret);
				return;
			}
			throw new CompilerException(obj,"Super-call via forwarding is not allowed");
		}
		
		obj.resolve(null);
		
		CallType mt = null;
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
			mt = new CallType(res_flags==0?null:tp,ata,ta,null,false);
			try {
				if (PassInfo.resolveBestMethodR(tp,m,info,ident.name,mt)) {
					if (tps.length == 1 && res_flags == 0)
						res[si] = info.buildCall(this, obj, m, info.mt, args.delToArray());
					else if (res_flags == 0)
						res[si] = info.buildCall(this, new TypeRef(tps[si]), m, info.mt, args.delToArray());
					else
						res[si] = info.buildCall(this, obj.ncopy(), m, info.mt, args.delToArray());
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
		if (e instanceof UnresExpr)
			e = ((UnresExpr)e).toResolvedExpr();
		if (isPrimaryExpr())
			e.setPrimaryExpr(true);
		this.replaceWithNodeResolve( reqType, e );
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(obj).append('.').append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		return sb.append(')').toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
