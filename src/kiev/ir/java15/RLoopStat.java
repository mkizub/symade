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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@ViewOf(vcast=true)
public static abstract view RLoopStat of LoopStat extends RENode {
	public:ro	Label					lblcnt;
	public:ro	Label					lblbrk;
}

@ViewOf(vcast=true)
public final static view RLabel of Label extends RDNode {
	public List<ASTNode>		links;
	public void addLink(ASTNode lnk);
	public void delLink(ASTNode lnk);
}

@ViewOf(vcast=true)
public static final view RWhileStat of WhileStat extends RLoopStat {
	public ENode		cond;
	public ENode		body;

	public void resolveENode(Type reqType, Env env) {
		try {
			resolveENode(cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(cond, env);
		} catch(Exception e ) { Kiev.reportError(cond,e); }
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) { Kiev.reportError(body,e); }
		if( cond.isConstantExpr(env) && ((Boolean)cond.getConstValue(env)).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}
}

@ViewOf(vcast=true)
public static view RDoWhileStat of DoWhileStat extends RLoopStat {
	public ENode		cond;
	public ENode		body;

	public void resolveENode(Type reqType, Env env) {
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		try {
			resolveENode(cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(cond, env);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		if( cond.isConstantExpr(env) && ((Boolean)cond.getConstValue(env)).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}
}

@ViewOf(vcast=true)
public static final view RForStat of ForStat extends RLoopStat {
	public:ro ASTNode[]		inits;
	public    ENode			cond;
	public    ENode			body;
	public    ENode			iter;

	public void resolveENode(Type reqType, Env env) {
		foreach (ASTNode n; inits) {
			try {
				if (n instanceof DNode) {
					resolveDNode(n,env);
				}
				else if (n instanceof SNode) {
					resolveSNode(n,env);
				}
				else if (n instanceof ENode) {
					resolveENode(n,env.tenv.tpVoid,env);
					((ENode)n).setGenVoidExpr(true);
				}
			} catch(Exception e ) {
				Kiev.reportError(n,e);
			}
		}
		if( cond != null ) {
			try {
				resolveENode(cond,env.tenv.tpBoolean,env);
				RBoolExpr.checkBool(cond, env);
			} catch(Exception e ) {
				Kiev.reportError(cond,e);
			}
		}
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		if( iter != null ) {
			try {
				resolveENode(iter,env.tenv.tpVoid,env);
				iter.setGenVoidExpr(true);
			} catch(Exception e ) {
				Kiev.reportError(iter,e);
			}
		}
		if( ( cond==null
			|| (cond.isConstantExpr(env) && ((Boolean)cond.getConstValue(env)).booleanValue())
			)
			&& !isBreaked()
		) {
			setMethodAbrupted(true);
		}
	}
}

@ViewOf(vcast=true)
public static final view RForEachStat of ForEachStat extends RLoopStat {
	public int			mode;
	public ENode		container;
	public Var			var;
	public Var			iter;
	public Var			iter_array;
	public ENode		iter_init;
	public ENode		iter_cond;
	public ENode		var_init;
	public ENode		cond;
	public ENode		body;
	public ENode		iter_incr;

	public void resolveENode(Type reqType, Env env) {
		// foreach( type x; container; cond) statement
		// is equivalent to
		// for(iter-type x$iter = container.elements(); x$iter.hasMoreElements(); ) {
		//		type x = container.nextElement();
		//		if( !cond ) continue;
		//		...
		//	}
		//	or if container is an array:
		//	for(int x$iter=0, x$arr=container; x$iter < x$arr.length; x$iter++) {
		//		type x = x$arr[ x$iter ];
		//		if( !cond ) continue;
		//		...
		//	}
		//	or if container is a rule:
		//	for(rule $env=null; ($env=rule($env,...)) != null; ) {
		//		if( !cond ) continue;
		//		...
		//	}
		//

		resolveENode(container,null,env);

		Type itype;
		Type xtype = container.getType(env);
		ResInfo<Method> elems = null;
		ResInfo<Method> nextelem = null;
		ResInfo<Method> moreelem = null;
		if (xtype instanceof CTimeType) {
			container = xtype.makeUnboxedExpr(container);
			resolveENode(container,null,env);
			xtype = container.getType(env);
		}
		if( xtype.isInstanceOf(env.tenv.tpArrayOfAny) ) {
			itype = env.tenv.tpInt;
			mode = ForEachStat.ARRAY;
		} else if( xtype.isInstanceOf( env.tenv.tpJavaEnumeration) ) {
			itype = xtype;
			mode = ForEachStat.JENUM;
		} else if( xtype.isInstanceOf( env.tenv.tpJavaIterator) ) {
			itype = xtype;
			mode = ForEachStat.JITERATOR;
		} else if( xtype.isInstanceOf( env.tenv.tpJavaIterable) ) {
			PassInfo.resolveBestMethodR(xtype,
				elems=new ResInfo<Method>(env,this,"iterator",ResInfo.noStatic|ResInfo.noSyntaxContext),
				new CallType(xtype,null,null,env.tenv.tpAny,false));
			itype = Type.getRealType(xtype,elems.resolvedDNode().mtype.ret());
			mode = ForEachStat.JITERABLE;
		} else if( PassInfo.resolveBestMethodR(xtype,
				elems=new ResInfo<Method>(env,this,nameElements,ResInfo.noStatic|ResInfo.noSyntaxContext),
				new CallType(xtype,null,null,env.tenv.tpAny,false))
		) {
			itype = Type.getRealType(xtype,elems.resolvedDNode().mtype.ret());
			mode = ForEachStat.ELEMS;
		} else if( xtype ≡ env.tenv.tpRule &&
			(
			   ( container instanceof CallExpr && ((CallExpr)container).func.mtype.ret() ≡ env.tenv.tpRule )
			|| ( container instanceof ClosureCallExpr && ((ClosureCallExpr)container).getType(env) ≡ env.tenv.tpRule )
			)
		  ) {
			itype = env.tenv.tpRule;
			mode = ForEachStat.RULE;
		} else {
			throw new CompilerException(container,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+xtype+" found");
		}
		if( itype ≡ env.tenv.tpRule ) {
			iter = new LVar(pos,"$env",itype,Var.VAR_LOCAL,0);
		}
		else if( var != null ) {
			iter = new LVar(var.pos,var.sname+"$iter",itype,Var.VAR_LOCAL,0);
			if (mode == ForEachStat.ARRAY) {
				iter_array = new LVar(container.pos,var.sname+"$arr",container.getType(env),Var.VAR_LOCAL,0);
			}
		}
		else {
			iter = null;
		}

		// Initialize iterator
		switch( mode ) {
		case ForEachStat.ARRAY:
			/* iter = 0; arr = container;*/
			iter_init = new CommaExpr();
			((CommaExpr)iter_init).exprs.add(
				new AssignExpr(iter.pos,
					new LVarExpr(container.pos,iter_array),
					new Copier().copyFull(container)
				));
			((CommaExpr)iter_init).exprs.add(
				new AssignExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					new ConstIntExpr(0)
				));
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,env.tenv.tpInt,env);
			break;
		case ForEachStat.JENUM:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos,
				new LVarExpr(iter.pos,iter), new Copier().copyFull(container)
				);
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,iter.getType(env),env);
			break;
		case ForEachStat.JITERATOR:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos,
				new LVarExpr(iter.pos,iter), new Copier().copyFull(container)
				);
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,iter.getType(env),env);
			break;
		case ForEachStat.JITERABLE:
			/* iter = container.iterate(); */
			iter_init = new AssignExpr(iter.pos,
				new LVarExpr(iter.pos,iter),
				new CallExpr(container.pos,new Copier().copyFull(container),elems.resolvedSymbol(),ENode.emptyArray)
				);
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,iter.getType(env),env);
			break;
		case ForEachStat.ELEMS:
			/* iter = container.elements(); */
			iter_init = new AssignExpr(iter.pos,
				new LVarExpr(iter.pos,iter),
				new CallExpr(container.pos,new Copier().copyFull(container),elems.resolvedSymbol(),ENode.emptyArray)
				);
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,iter.getType(env),env);
			break;
		case ForEachStat.RULE:
			/* iter = rule(iter/hidden,...); */
			{
			iter_init = new AssignExpr(iter.pos,
				new LVarExpr(iter.pos,iter), new ConstNullExpr()
				);
			Kiev.runProcessorsOn(iter_init);
			resolveENode(iter_init,env.tenv.tpVoid,env);
			}
			break;
		}
		iter_init.setGenVoidExpr(true);

		// Check iterator condition

		switch( mode ) {
		case ForEachStat.ARRAY:
			/* iter < container.length */
			iter_cond = new BinaryBoolExpr(iter.pos, env.coreFuncs.fIntBoolLT,
				new LVarExpr(iter.pos,iter),
				new IFldExpr(iter.pos,new LVarExpr(0,iter_array),env.tenv.tpArrayOfAny.resolveField("length"))
				);
			break;
		case ForEachStat.JENUM:
		case ForEachStat.ELEMS:
			/* iter.hasMoreElements() */
			if( !PassInfo.resolveBestMethodR(itype,
					moreelem=new ResInfo<Method>(env,this,nameHasMoreElements,ResInfo.noStatic|ResInfo.noSyntaxContext),
					new CallType(itype,null,null,env.tenv.tpAny,false))
				)
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			iter_cond = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					moreelem.resolvedSymbol(),
					ENode.emptyArray
				);
			break;
		case ForEachStat.JITERATOR:
		case ForEachStat.JITERABLE:
			/* iter.hasNext() */
			if( !PassInfo.resolveBestMethodR(itype,
					moreelem=new ResInfo<Method>(env,this,"hasNext",ResInfo.noStatic|ResInfo.noSyntaxContext),
					new CallType(itype,null,null,env.tenv.tpAny,false))
				)
				throw new CompilerException(this,"Can't find method "+"hasNext");
			iter_cond = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					moreelem.resolvedSymbol(),
					ENode.emptyArray
				);
			break;
		case ForEachStat.RULE:
			/* (iter = rule(iter, ...)) != null */
			iter_cond = new BinaryBoolExpr(
				container.pos,
				env.coreFuncs.fObjectBoolNE,
				new AssignExpr(container.pos,
					new LVarExpr(container.pos,iter),
					new Copier().copyFull(container)),
				new ConstNullExpr()
				);
			break;
		}
		if( iter_cond != null ) {
			Kiev.runProcessorsOn(iter_cond);
			resolveENode(iter_cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(iter_cond, env);
		}

		// Initialize value
		ENode ce = null;
		var_init = null;
		switch( mode ) {
		case ForEachStat.ARRAY:
			/* var = container[iter] */
			ce = new ContainerAccessExpr(container.pos,new LVarExpr(0,iter_array),new LVarExpr(iter.pos,iter));
			break;
		case ForEachStat.JENUM:
		case ForEachStat.ELEMS:
			/* var = iter.nextElement() */
			if( !PassInfo.resolveBestMethodR(itype,
					nextelem=new ResInfo<Method>(env,this,nameNextElement,ResInfo.noStatic|ResInfo.noSyntaxContext),
					new CallType(itype,null,null,env.tenv.tpAny,false))
				)
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			ce = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					nextelem.resolvedSymbol(),
					ENode.emptyArray
				);
			break;
		case ForEachStat.JITERATOR:
		case ForEachStat.JITERABLE:
			/* var = iter.nextElement() */
			if( !PassInfo.resolveBestMethodR(itype,
					nextelem=new ResInfo<Method>(env,this,"next",ResInfo.noStatic|ResInfo.noSyntaxContext),
					new CallType(itype,null,null,env.tenv.tpAny,false))
				)
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			ce = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					nextelem.resolvedSymbol(),
					ENode.emptyArray
				);
			break;
		case ForEachStat.RULE:
			/* iter = rule(...); */
			break;
		}
		if (ce != null) {
			var_init = ce; // to allow ce.getType(env)
			if (ce.getType(env).isInstanceOf(var.getType(env))) {
				var_init = new AssignExpr(var.pos,new LVarExpr(var.pos,var),~ce);
			} else {
				Var tmp = new LVar(var.pos, "tmp", ce.getType(env), Var.VAR_LOCAL, ACC_FINAL);
				tmp.init = ~ce;
				Block b = new Block();
				b.addSymbol(tmp);
				b.stats.add(new IfElseStat(tmp.pos,
					new BooleanNotExpr(tmp.pos, new InstanceofExpr(tmp.pos, new LVarExpr(tmp.pos,tmp),var.getType(env))),
					new ContinueStat(),
					null
				));
				b.stats.add(
					new AssignExpr(var.pos,
						new LVarExpr(var.pos,var),
						new CastExpr(var.pos, var.getType(env), new LVarExpr(tmp.pos,tmp))
						)
				);
				var_init = b;
			}
			Kiev.runProcessorsOn(var_init);
			resolveENode(var_init,var.getType(env),env);
			var_init.setGenVoidExpr(true);
		}

		// Check condition, if any
		if( cond != null ) {
			resolveENode(cond,env.tenv.tpBoolean,env);
			RBoolExpr.checkBool(cond, env);
		}

		// Process body
		try {
			resolveENode(body,env.tenv.tpVoid,env);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}

		// Increment iterator
		if( mode == ForEachStat.ARRAY ) {
			/* iter++ */
			iter_incr = new IncrementExpr(iter.pos,env.coreFuncs.fIntPostINCR,new LVarExpr(iter.pos,iter));
			resolveENode(iter_incr,env.tenv.tpVoid,env);
			iter_incr.setGenVoidExpr(true);
		} else {
			iter_incr = null;
		}
	}
}

