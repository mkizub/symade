/* Generated By:JJTree: Do not edit this line. ASTCallExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTCallExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}

	@virtual typedef This  = ASTCallExpression;
	@virtual typedef VView = VASTCallExpression;

	@ref public NameRef				func;
	@att public NArr<TypeRef>		targs;
	@att public NArr<ENode>			args;

	@nodeview
	public static view VASTCallExpression of ASTCallExpression extends VENode {
		public		NameRef			func;
		public:ro	NArr<TypeRef>	targs;
		public:ro	NArr<ENode>		args;

		public void mainResolveOut() {
			// method of current class or first-order function
			DNode@ m;
			Type tp = ctx_clazz.ctype;
			
			Type[] ata = new Type[targs.length];
			for (int i=0; i < ata.length; i++)
				ata[i] = targs[i].getType();
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			
			if( func.name.equals(nameThis) ) {
				CallType mt = new CallType(ta,Type.tpVoid);
				ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
				try {
					if( !PassInfo.resolveBestMethodR(tp,m,info,ctx_method.name.name,mt) )
						throw new CompilerException(this,"Method "+Method.toString(func.name,args)+" unresolved");
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				if( info.isEmpty() ) {
					Type st = ctx_clazz.super_type;
					CallExpr ce = new CallExpr(pos,null,(Method)m,info.mt,args.delToArray(),false);
					replaceWithNode(ce);
					//((Method)m).makeArgs(ce.args,st);
					return;
				}
				throw new CompilerException(this,"Constructor call via forwarding is not allowed");
			}
			else if( func.name.equals(nameSuper) ) {
				CallType mt = new CallType(ta,Type.tpVoid);
				ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
				try {
					if( !PassInfo.resolveBestMethodR(ctx_clazz.super_type,m,info,ctx_method.name.name,mt) )
						throw new CompilerException(this,"Method "+Method.toString(func.name,args)+" unresolved");
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				if( info.isEmpty() ) {
					Type st = ctx_clazz.super_type;
					CallExpr ce = new CallExpr(pos,null,(Method)m,info.mt,args.delToArray(),true);
					replaceWithNode(ce);
					//((Method)m).makeArgs(ce.args,st);
					return;
				}
				throw new CompilerException(this,"Super-constructor call via forwarding is not allowed");
			} else {
				CallType mt = new CallType(ata,ta,null);
				ResInfo info = new ResInfo(this);
				try {
					if( !PassInfo.resolveMethodR((ASTNode)this,m,info,func.name,mt) ) {
						// May be a closure
						DNode@ closure;
						ResInfo info = new ResInfo(this);
						try {
							if( !PassInfo.resolveNameR((ASTNode)this,closure,info,func.name) )
								throw new CompilerException(this,"Unresolved method "+Method.toString(func.name,args,null));
						} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
						try {
							if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof CallType
							||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof CallType
							) {
								replaceWithNode(new ClosureCallExpr(pos,info.buildAccess((ASTNode)this,null,closure),args.delToArray()));
								return;
							}
						} catch(Exception eee) {
							Kiev.reportError(this,eee);
						}
						throw new CompilerException(this,"Unresolved method "+Method.toString(func.name,args));
					}
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
	
				if( m.isStatic() )
					assert (info.isEmpty());
				ENode e = info.buildCall((ASTNode)this,null,m,info.mt,args.toArray());
				if (e instanceof UnresExpr)
					e = ((UnresExpr)e).toResolvedExpr();
				if (isPrimaryExpr())
					e.setPrimaryExpr(true);
				this.replaceWithNode(e);
			}
		}
	}
	
	public ASTCallExpression() {}

	public ASTCallExpression(int pos, KString func, ENode[] args) {
		this.pos = pos;
		this.func = new NameRef(pos, func);
		foreach (ENode e; args) {
			this.args.append(e);
		}
	}

	public int getPriority() { return Constants.opCallPriority; }

	public void resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
			args[i].resolve(null);
        }
		// method of current class or first-order function
		Method@ m;
		Type tp = ctx_clazz.ctype;
		Type ret = reqType;
	retry_with_null_ret:;
		if( func.name.equals(nameThis) ) {
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			CallType mt = new CallType(ta,Type.tpVoid);
			ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(tp,m,info,ctx_method.name.name,mt) )
				throw new CompilerException(this,"Method "+Method.toString(func.name,args)+" unresolved");
            if( info.isEmpty() ) {
				Type st = ctx_clazz.super_type;
				CallExpr ce = new CallExpr(pos,null,m,info.mt,args.delToArray(),false);
				replaceWithNode(ce);
				//m.makeArgs(ce.args,st);
				ce.resolve(ret);
				return;
			}
			throw new CompilerException(this,"Constructor call via forwarding is not allowed");
		}
		else if( func.name.equals(nameSuper) ) {
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			CallType mt = new CallType(ta,Type.tpVoid);
			ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(ctx_clazz.super_type,m,info,ctx_method.name.name,mt) )
				throw new CompilerException(this,"Method "+Method.toString(func.name,args)+" unresolved");
            if( info.isEmpty() ) {
				Type st = ctx_clazz.super_type;
				CallExpr ce = new CallExpr(pos,null,m,info.mt,args.delToArray(),true);
				replaceWithNode(ce);
				//m.makeArgs(ce.args,st);
				ce.resolve(ret);
				return;
			}
			throw new CompilerException(this,"Super-constructor call via forwarding is not allowed");
		} else {
			CallType mt;
			if( reqType instanceof CallType && ((CallType)reqType).arity > 0 ) {
				mt = (CallType)reqType;
			} else {
				Type[] ta = new Type[args.length];
				for(int i=0; i < ta.length; i++)
					ta[i] = args[i].getType();
				mt = new CallType(ta,ret);
			}
			ResInfo info = new ResInfo(this);
			if( !PassInfo.resolveMethodR(this,m,info,func.name,mt) ) {
				// May be a closure
				DNode@ closure;
				ResInfo info = new ResInfo(this);
				if( !PassInfo.resolveNameR(this,closure,info,func.name) ) {
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(this,"Unresolved method "+Method.toString(func.name,args,ret));
				}
				try {
					if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof CallType
					||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof CallType
					) {
						replaceWithNodeResolve(ret, new ClosureCallExpr(pos,info.buildAccess(this,closure),args.delToArray()));
						return;
					}
				} catch(Exception eee) {
					Kiev.reportError(this,eee);
				}
				if( ret != null ) { ret = null; goto retry_with_null_ret; }
				throw new CompilerException(this,"Unresolved method "+Method.toString(func.name,args));
			}
			if( reqType instanceof CallType ) {
				NewClosure nc = new NewClosure(pos);
				nc.type_ret = new TypeRef(pos, ((CallType)reqType).ret());
				for (int i=0; i < nc.params.length; i++)
					nc.params.append(new FormPar(pos,KString.from("arg"+(i+1)),((Method)m).type.arg(i),FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
				Block bs = new Block(pos);
				ENode[] oldargs = args.toArray();
				ENode[] cargs = new ENode[nc.params.length];
				for(int i=0; i < cargs.length; i++)
					cargs[i] = new LVarExpr(pos,(Var)nc.params[i]);
				args.delAll();
				foreach (ENode e; cargs)
					args.add(e);
				if( nc.type_ret.getType() ≡ Type.tpVoid ) {
					bs.stats.add(new ExprStat(pos,this));
					bs.stats.add(new ReturnStat(pos,null));
				} else {
					bs.stats.add(new ReturnStat(pos,this));
				}
				nc.body = bs;
				ENode e = nc;
				if( oldargs.length > 0 )
					e = new ClosureCallExpr(pos,nc,oldargs);
				replaceWithNode(e);
				Kiev.runProcessorsOn(e);
				e.resolve(reqType);
				return;
			} else {
				if( m.isStatic() )
					assert (info.isEmpty());
				//((Method)m).makeArgs(args,tp);
				ENode e = info.buildCall(this,null,m,info.mt,args.toArray());
				if (e instanceof UnresExpr)
					e = ((UnresExpr)e).toResolvedExpr();
				if (isPrimaryExpr())
					e.setPrimaryExpr(true);
				this.replaceWithNodeResolve( reqType, e );
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(func).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		return sb.append(')').toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(func).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
