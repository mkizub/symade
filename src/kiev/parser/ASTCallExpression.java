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

@nodeset
public class ASTCallExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}

	@virtual typedef NImpl = ASTCallExpressionImpl;
	@virtual typedef VView = ASTCallExpressionView;

	@nodeimpl
	public static class ASTCallExpressionImpl extends ENodeImpl {
		@virtual typedef ImplOf = ASTCallExpression;
		@ref public NameRef				func;
		@att public NArr<TypeRef>		targs;
		@att public NArr<ENode>			args;
		public ASTCallExpressionImpl() {}
		public ASTCallExpressionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTCallExpressionView of ASTCallExpressionImpl extends ENodeView {
		public				NameRef			func;
		public access:ro	NArr<TypeRef>	targs;
		public access:ro	NArr<ENode>		args;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTCallExpression() {
		super(new ASTCallExpressionImpl());
	}

	public ASTCallExpression(int pos, KString func, ENode[] args) {
		super(new ASTCallExpressionImpl(pos));
		this.func = new NameRef(pos, func);
		foreach (ENode e; args) {
			this.args.append(e);
		}
	}

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
				if( !PassInfo.resolveMethodR(this,m,info,func.name,mt) ) {
					// May be a closure
					DNode@ closure;
					ResInfo info = new ResInfo(this);
					try {
						if( !PassInfo.resolveNameR(this,closure,info,func.name) )
							throw new CompilerException(this,"Unresolved method "+Method.toString(func.name,args,null));
					} catch (RuntimeException e) { throw new CompilerException(this,e.getMessage()); }
					try {
						if( closure instanceof Var && Type.getRealType(tp,((Var)closure).type) instanceof CallType
						||  closure instanceof Field && Type.getRealType(tp,((Field)closure).type) instanceof CallType
						) {
							replaceWithNode(new ClosureCallExpr(pos,info.buildAccess(this,closure),args.delToArray()));
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
			ENode e = info.buildCall(this,null,m,info.mt,args.toArray());
			if (e instanceof UnresExpr)
				e = ((UnresExpr)e).toResolvedExpr();
			this.replaceWithNode(e);
		}
	}
	
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
				ASTAnonymouseClosure ac = new ASTAnonymouseClosure();
				ac.pos = pos;
				ac.rettype = new TypeRef(pos, ((CallType)reqType).ret());
				for (int i=0; i < ac.params.length; i++)
					ac.params.append(new FormPar(pos,KString.from("arg"+(i+1)),((Method)m).type.arg(i),FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
				BlockStat bs = new BlockStat(pos,ENode.emptyArray);
				ENode[] oldargs = args.toArray();
				ENode[] cargs = new ENode[ac.params.length];
				for(int i=0; i < cargs.length; i++)
					cargs[i] = new LVarExpr(pos,(Var)ac.params[i]);
				args.delAll();
				foreach (ENode e; cargs)
					args.add(e);
				if( ac.rettype.getType() ≡ Type.tpVoid ) {
					bs.addStatement(new ExprStat(pos,this));
					bs.addStatement(new ReturnStat(pos,null));
				} else {
					bs.addStatement(new ReturnStat(pos,this));
				}
				ac.body = bs;
				if( oldargs.length > 0 ) {
					replaceWithNodeResolve(reqType, new ClosureCallExpr(pos,ac,oldargs));
				} else {
					replaceWithNodeResolve(reqType, ac);
				}
				return;
			} else {
				if( m.isStatic() )
					assert (info.isEmpty());
				//((Method)m).makeArgs(args,tp);
				ENode e = info.buildCall(this,null,m,info.mt,args.toArray());
				this.replaceWithNodeResolve( reqType, e );
			}
		}
	}

	public int		getPriority() { return Constants.opCallPriority; }

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
