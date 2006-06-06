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
	public static view VASTCallExpression of ASTCallExpression extends VENode {
		public:ro	TypeRef[]		targs;
		public:ro	ENode[]			args;

		public void mainResolveOut() {
			// method of current class or first-order function
			Method@ m;
			Type tp = ctx_tdecl.xtype;
			
			Type[] ata = new Type[targs.length];
			for (int i=0; i < ata.length; i++)
				ata[i] = targs[i].getType();
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			
			if( ident.name.equals(nameThis) ) {
				CallType mt = new CallType(tp,ata,ta,Type.tpVoid,false);
				ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
				try {
					if( !PassInfo.resolveBestMethodR(tp,m,info,ctx_method.id.uname,mt) )
						throw new CompilerException(this,"Method "+Method.toString(ident.name,args)+" unresolved");
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				if( info.isEmpty() ) {
					ASTCallExpression self = (ASTCallExpression)this;
					CallExpr ce = new CallExpr(pos,null,m,self.targs.delToArray(),self.args.delToArray(),false);
					replaceWithNode(ce);
					return;
				}
				throw new CompilerException(this,"Constructor call via forwarding is not allowed");
			}
			else if( ident.name.equals(nameSuper) ) {
				CallType mt = new CallType(tp,ata,ta,Type.tpVoid,false);
				ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
				try {
					if( !PassInfo.resolveBestMethodR(ctx_tdecl.super_types[0].getType(),m,info,ctx_method.id.uname,mt) )
						throw new CompilerException(this,"Method "+Method.toString(ident.name,args)+" unresolved");
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
				if( info.isEmpty() ) {
					ASTCallExpression self = (ASTCallExpression)this;
					CallExpr ce = new CallExpr(pos,null,m,self.targs.delToArray(),self.args.delToArray(),true);
					replaceWithNode(ce);
					return;
				}
				throw new CompilerException(this,"Super-constructor call via forwarding is not allowed");
			} else {
				CallType mt = new CallType(tp,ata,ta,Type.tpAny,false);
				ResInfo info = new ResInfo(this);
				try {
					if( !PassInfo.resolveMethodR((ASTNode)this,m,info,ident.name,mt) ) {
						// May be a closure
						DNode@ closure;
						ResInfo info = new ResInfo(this);
						try {
							if( !PassInfo.resolveNameR((ASTNode)this,closure,info,ident.name) )
								throw new CompilerException(this,"Unresolved method "+Method.toString(ident.name,args,null));
						} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
						try {
							if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof CallType
							||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof CallType
							) {
								replaceWithNode(new ClosureCallExpr(pos,
									info.buildAccess((ASTNode)this,null,closure).closeBuild(),
									((ASTCallExpression)this).args.delToArray()
								));
								return;
							}
						} catch(Exception eee) {
							Kiev.reportError(this,eee);
						}
						throw new CompilerException(this,"Unresolved method "+Method.toString(ident.name,args));
					}
				} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
	
				if( m.isStatic() )
					assert (info.isEmpty());
				ENode e = info.buildCall((ASTNode)this,null,m,targs,args).closeBuild();
				if (isPrimaryExpr())
					e.setPrimaryExpr(true);
				this.replaceWithNode(e);
			}
		}
	}
	
	public ASTCallExpression() {}

	public ASTCallExpression(int pos, String func, ENode[] args) {
		this.pos = pos;
		this.ident = new SymbolRef(pos, func);
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
		Type tp = ctx_tdecl.xtype;
		Type ret = reqType;
		Type[] ata = new Type[targs.length];
		for (int i=0; i < ata.length; i++)
			ata[i] = targs[i].getType();
		Type[] ta = new Type[args.length];
		for (int i=0; i < ta.length; i++)
			ta[i] = args[i].getType();
	retry_with_null_ret:;
		if( ident.name.equals(nameThis) ) {
			CallType mt = new CallType(tp,ata,ta,Type.tpVoid,false);
			ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(tp,m,info,ctx_method.id.uname,mt) )
				throw new CompilerException(this,"Method "+Method.toString(ident.name,args)+" unresolved");
            if( info.isEmpty() ) {
				CallExpr ce = new CallExpr(pos,null,m,targs.delToArray(),args.delToArray(),false);
				replaceWithNode(ce);
				ce.resolve(ret);
				return;
			}
			throw new CompilerException(this,"Constructor call via forwarding is not allowed");
		}
		else if( ident.name.equals(nameSuper) ) {
			Type[] ta = new Type[args.length];
			for (int i=0; i < ta.length; i++)
				ta[i] = args[i].getType();
			CallType mt = new CallType(tp,ata,ta,Type.tpVoid,false);
			ResInfo info = new ResInfo(this,ResInfo.noSuper|ResInfo.noStatic|ResInfo.noForwards|ResInfo.noImports);
			if( !PassInfo.resolveBestMethodR(ctx_tdecl.super_types[0].getType(),m,info,ctx_method.id.uname,mt) )
				throw new CompilerException(this,"Method "+Method.toString(ident.name,args)+" unresolved");
            if( info.isEmpty() ) {
				CallExpr ce = new CallExpr(pos,null,m,targs.delToArray(),args.delToArray(),true);
				replaceWithNode(ce);
				ce.resolve(ret);
				return;
			}
			throw new CompilerException(this,"Super-constructor call via forwarding is not allowed");
		} else {
			CallType mt;
			if( reqType instanceof CallType && ((CallType)reqType).arity > 0 ) {
				mt = (CallType)reqType;
			} else {
				mt = new CallType(tp,ata,ta,ret,false);
			}
			ResInfo info = new ResInfo(this);
			if( !PassInfo.resolveMethodR(this,m,info,ident.name,mt) ) {
				// May be a closure
				DNode@ closure;
				ResInfo info = new ResInfo(this);
				if( !PassInfo.resolveNameR(this,closure,info,ident.name) ) {
					if( ret != null ) { ret = null; goto retry_with_null_ret; }
					throw new CompilerException(this,"Unresolved method "+Method.toString(ident.name,args,ret));
				}
				try {
					if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof CallType
					||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof CallType
					) {
						replaceWithNodeResolve(ret, new ClosureCallExpr(pos,
							info.buildAccess(this,closure).closeBuild(),
							args.delToArray()
						));
						return;
					}
				} catch(Exception eee) {
					Kiev.reportError(this,eee);
				}
				if( ret != null ) { ret = null; goto retry_with_null_ret; }
				throw new CompilerException(this,"Unresolved method "+Method.toString(ident.name,args));
			}
			if( reqType instanceof CallType ) {
				NewClosure nc = new NewClosure(pos);
				nc.type_ret = new TypeRef(pos, ((CallType)reqType).ret());
				for (int i=0; i < nc.params.length; i++)
					nc.params.append(new FormPar(pos,"arg"+(i+1),((Method)m).type.arg(i),FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
				Block bs = new Block(pos);
				ENode[] oldargs = args;
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
				ENode e = info.buildCall(this,null,m,targs,args).closeBuild();
				if (isPrimaryExpr())
					e.setPrimaryExpr(true);
				this.replaceWithNodeResolve( reqType, e );
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		return sb.append(')').toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append(ident).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
