package kiev.ir.java15;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@nodeview
public static abstract view RLoopStat of LoopStat extends RENode {
	public:ro	Label					lblcnt;
	public:ro	Label					lblbrk;
}

@nodeview
public final static view RLabel of Label extends RDNode {
	public List<ASTNode>		links;
	public void addLink(ASTNode lnk);
	public void delLink(ASTNode lnk);
}

@nodeview
public static final view RWhileStat of WhileStat extends RLoopStat {
	public ENode		cond;
	public ENode		body;

	public void resolve(Type reqType) {
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) { Kiev.reportError(cond,e); }
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) { Kiev.reportError(body,e); }
		if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}
}

@nodeview
public static view RDoWhileStat of DoWhileStat extends RLoopStat {
	public ENode		cond;
	public ENode		body;

	public void resolve(Type reqType) {
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		try {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		} catch(Exception e ) {
			Kiev.reportError(cond,e);
		}
		if( cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue() && !isBreaked() ) {
			setMethodAbrupted(true);
		}
	}
}

@nodeview
public static final view RForInit of ForInit extends RENode {
	public:ro	NArr<Var>		decls;

	public void resolve(Type reqType) {
		foreach (Var v; decls)
			v.resolveDecl();
	}
}

@nodeview
public static final view RForStat of ForStat extends RLoopStat {
	public ENode		init;
	public ENode		cond;
	public ENode		body;
	public ENode		iter;

	public void resolve(Type reqType) {
		if( init != null ) {
			try {
				init.resolve(Type.tpVoid);
				init.setGenVoidExpr(true);
			} catch(Exception e ) {
				Kiev.reportError(init,e);
			}
		}
		if( cond != null ) {
			try {
				cond.resolve(Type.tpBoolean);
				BoolExpr.checkBool(cond);
			} catch(Exception e ) {
				Kiev.reportError(cond,e);
			}
		}
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		if( iter != null ) {
			try {
				iter.resolve(Type.tpVoid);
				iter.setGenVoidExpr(true);
			} catch(Exception e ) {
				Kiev.reportError(iter,e);
			}
		}
		if( ( cond==null
			|| (cond.isConstantExpr() && ((Boolean)cond.getConstValue()).booleanValue())
			)
			&& !isBreaked()
		) {
			setMethodAbrupted(true);
		}
	}
}

@nodeview
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

	public void resolve(Type reqType) {
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

		container.resolve(null);

		Type itype;
		Type xtype = container.getType();
		Method@ elems;
		Method@ nextelem;
		Method@ moreelem;
		if (xtype instanceof CTimeType) {
			container = xtype.makeUnboxedExpr(container);
			container.resolve(null);
			xtype = container.getType();
		}
		if( xtype.isInstanceOf(Type.tpArray) ) {
			itype = Type.tpInt;
			mode = ForEachStat.ARRAY;
		} else if( xtype.isInstanceOf( Type.tpKievEnumeration) ) {
			itype = xtype;
			mode = ForEachStat.KENUM;
		} else if( xtype.isInstanceOf( Type.tpJavaEnumeration) ) {
			itype = xtype;
			mode = ForEachStat.JENUM;
		} else if( PassInfo.resolveBestMethodR(xtype,elems,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
				nameElements,new CallType(Type.emptyArray,Type.tpAny))
		) {
			itype = Type.getRealType(xtype,elems.type.ret());
			mode = ForEachStat.ELEMS;
		} else if( xtype ≡ Type.tpRule &&
			(
			   ( container instanceof CallExpr && ((CallExpr)container).func.type.ret() ≡ Type.tpRule )
			|| ( container instanceof ClosureCallExpr && ((ClosureCallExpr)container).getType() ≡ Type.tpRule )
			)
		  ) {
			itype = Type.tpRule;
			mode = ForEachStat.RULE;
		} else {
			throw new CompilerException(container,"Container must be an array or an Enumeration "+
				"or a class that implements 'Enumeration elements()' method, but "+xtype+" found");
		}
		if( itype ≡ Type.tpRule ) {
			iter = new Var(pos,"$env",itype,0);
		}
		else if( var != null ) {
			iter = new Var(var.pos,var.id.uname+"$iter",itype,0);
			if (mode == ForEachStat.ARRAY) {
				iter_array = new Var(container.pos,var.id.uname+"$arr",container.getType(),0);
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
				new AssignExpr(iter.pos,AssignOperator.Assign,
					new LVarExpr(container.pos,iter_array),
					container.ncopy()
				));
			((CommaExpr)iter_init).exprs.add(
				new AssignExpr(iter.pos,AssignOperator.Assign,
					new LVarExpr(iter.pos,iter),
					new ConstIntExpr(0)
				));
			iter_init.resolve(Type.tpInt);
			break;
		case ForEachStat.KENUM:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), container.ncopy()
				);
			iter_init.resolve(iter.type);
			break;
		case ForEachStat.JENUM:
			/* iter = container; */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), container.ncopy()
				);
			iter_init.resolve(iter.type);
			break;
		case ForEachStat.ELEMS:
			/* iter = container.elements(); */
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter),
				new CallExpr(container.pos,container.ncopy(),elems,ENode.emptyArray)
				);
			iter_init.resolve(iter.type);
			break;
		case ForEachStat.RULE:
			/* iter = rule(iter/hidden,...); */
			{
			iter_init = new AssignExpr(iter.pos, AssignOperator.Assign,
				new LVarExpr(iter.pos,iter), new ConstNullExpr()
				);
			iter_init.resolve(Type.tpVoid);
//			// now is hidden // Also, patch the rule argument
//			NArr<ENode> args = null;
//			if( container instanceof CallExpr ) {
//				args = ((CallExpr)container).args;
//			}
//			else if( container instanceof ClosureCallExpr ) {
//				args = ((ClosureCallExpr)container).args;
//			}
//			else
//				Debug.assert("Unknown type of rule - "+container.getClass());
//			args[0] = new LVarExpr(container.pos,iter);
//			args[0].resolve(Type.tpRule);
			}
			break;
		}
		iter_init.setGenVoidExpr(true);

		// Check iterator condition

		switch( mode ) {
		case ForEachStat.ARRAY:
			/* iter < container.length */
			iter_cond = new BinaryBoolExpr(iter.pos,BinaryOperator.LessThen,
				new LVarExpr(iter.pos,iter),
				new IFldExpr(iter.pos,new LVarExpr(0,iter_array),Type.tpArray.resolveField("length"))
				);
			break;
		case ForEachStat.KENUM:
		case ForEachStat.JENUM:
		case ForEachStat.ELEMS:
			/* iter.hasMoreElements() */
			if( !PassInfo.resolveBestMethodR(itype,moreelem,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
				nameHasMoreElements,new CallType(Type.emptyArray,Type.tpAny)) )
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			iter_cond = new CallExpr(	iter.pos,
					new LVarExpr(iter.pos,iter),
					moreelem,
					ENode.emptyArray
				);
			break;
		case ForEachStat.RULE:
			/* (iter = rule(iter, ...)) != null */
			iter_cond = new BinaryBoolExpr(
				container.pos,
				BinaryOperator.NotEquals,
				new AssignExpr(container.pos,AssignOperator.Assign,
					new LVarExpr(container.pos,iter),
					container.ncopy()),
				new ConstNullExpr()
				);
			break;
		}
		if( iter_cond != null ) {
			iter_cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(iter_cond);
		}

		// Initialize value
		ENode ce = null;
		var_init = null;
		switch( mode ) {
		case ForEachStat.ARRAY:
			/* var = container[iter] */
			ce = new ContainerAccessExpr(container.pos,new LVarExpr(0,iter_array),new LVarExpr(iter.pos,iter));
			break;
		case ForEachStat.KENUM:
		case ForEachStat.JENUM:
		case ForEachStat.ELEMS:
			/* var = iter.nextElement() */
			if( !PassInfo.resolveBestMethodR(itype,nextelem,new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),
					nameNextElement,new CallType(Type.emptyArray,Type.tpAny)) )
				throw new CompilerException(this,"Can't find method "+nameHasMoreElements);
			ce = new CallExpr(iter.pos,
					new LVarExpr(iter.pos,iter),
					nextelem,
					ENode.emptyArray
				);
			break;
		case ForEachStat.RULE:
			/* iter = rule(...); */
			break;
		}
		if (ce != null) {
			var_init = ce; // to allow ce.getType()
			if (ce.getType().isInstanceOf(var.getType())) {
				var_init = new AssignExpr(var.pos,AssignOperator.Assign2,new LVarExpr(var.pos,var),~ce);
			} else {
				Var tmp = new Var(var.pos, "tmp", ce.getType(), ACC_FINAL);
				tmp.init = ~ce;
				Block b = new Block();
				b.addSymbol(tmp);
				b.stats.add(new IfElseStat(tmp.pos,
					new BooleanNotExpr(tmp.pos, new InstanceofExpr(tmp.pos, new LVarExpr(tmp.pos,tmp),var.getType())),
					new ContinueStat(),
					null
				));
				b.stats.add(
					new AssignExpr(var.pos,AssignOperator.Assign2,new LVarExpr(var.pos,var),new LVarExpr(tmp.pos,tmp))
				);
				var_init = b;
			}
			var_init.resolve(var.getType());
			var_init.setGenVoidExpr(true);
		}

		// Check condition, if any
		if( cond != null ) {
			cond.resolve(Type.tpBoolean);
			BoolExpr.checkBool(cond);
		}

		// Process body
		try {
			body.resolve(Type.tpVoid);
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}

		// Increment iterator
		if( mode == ForEachStat.ARRAY ) {
			/* iter++ */
			iter_incr = new IncrementExpr(iter.pos,PostfixOperator.PostIncr,
				new LVarExpr(iter.pos,iter)
				);
			iter_incr.resolve(Type.tpVoid);
			iter_incr.setGenVoidExpr(true);
		} else {
			iter_incr = null;
		}
	}
}

