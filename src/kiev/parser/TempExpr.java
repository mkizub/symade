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
package kiev.parser;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

/**
 * Base class to represent unresolved, temporary created expressions.
 */
@ThisIsANode(lang=void)
public abstract class UnresExpr extends ENode {

	@virtual typedef This  ≤ UnresExpr;

	@nodeData public Operator				op;

	public UnresExpr() {}
	
	public Operator getOp() { return op; }
	
	public final void callbackAttached(ParentInfo pi) {
		throw new Error("Internal error: "+this.getClass()+" attached to "+parent().getClass()+" to slot "+pslot().name);
	}
	
	public void resolve(Type reqType) {
		replaceWithResolve(reqType, fun ()->ENode {return closeBuild();});
	}
	
}

/**
 * Represents unresolved, temporary created prefix/postfix/infix expression.
 *
 * 'expr' field is @nodeData to not change the owner of the expression.
 * The owner will be changed when concrete, resolved unary expression is created.
 */
@ThisIsANode(lang=void)
public final class UnresOpExpr extends UnresExpr {
	
	@virtual typedef This  = UnresOpExpr;

	@nodeData public ENode∅				exprs;

	public UnresOpExpr() {}

	public UnresOpExpr(int pos, Operator op, ENode[] exprs) {
		this.pos = pos;
		this.op = op;
		foreach (ENode e; exprs; !(e instanceof EToken && e.isOperator()))
			this.exprs.add(e);
	}
	
	public ENode[] getArgs() { return exprs; }

	public String toString() {
		return op.toString(this);
	}

	public ENode closeBuild() {
		ENode ret;
		for (int i=0; i < exprs.length; i++)
			exprs[i] = exprs[i].closeBuild().detach();
		Operator op = this.op;
		if (op == Operator.CallApplay) {
			EToken id = (EToken)exprs[0];
			ASTNode[] stats = ((BlockRewr)exprs[1]).stats.delToArray();
			ENode[] args = new ENode[stats.length];
			for (int i=0; i < stats.length; i++)
				args[i] = (ENode)stats[i];
			ret = new CallExpr(id.pos,null, new SymbolRef<Method>(id.ident),null,args);
		}
		else if (op == Operator.CallAccess) {
			ENode obj = exprs[0];
			EToken id = (EToken)exprs[1];
			ASTNode[] stats = ((BlockRewr)exprs[2]).stats.delToArray();
			ENode[] args = new ENode[stats.length];
			for (int i=0; i < stats.length; i++)
				args[i] = (ENode)stats[i];
			ret = new CallExpr(id.pos,obj, new SymbolRef<Method>(id.ident),null,args);
		}
		else if (op == Operator.CallTypesAccess) {
			ENode obj = exprs[0];
			EToken id = (EToken)exprs[1];
			ASTNode[] params = ((BlockRewr)exprs[2]).stats.delToArray();
			ASTNode[] stats = ((BlockRewr)exprs[3]).stats.delToArray();
			TypeRef[] types = new TypeRef[params.length];
			for (int i=0; i < params.length; i++)
				types[i] = (TypeRef)params[i];
			ENode[] args = new ENode[stats.length];
			for (int i=0; i < stats.length; i++)
				args[i] = (ENode)stats[i];
			ret = new CallExpr(id.pos,obj, new SymbolRef<Method>(id.ident),types,args);
		}
		else if (op == Operator.NewAccess) {
			ENode obj = exprs[0];
			TypeRef tr = (TypeRef)exprs[1];
			ASTNode[] stats = ((BlockRewr)exprs[2]).stats.delToArray();
			ENode[] args = new ENode[stats.length];
			for (int i=0; i < stats.length; i++)
				args[i] = (ENode)stats[i];
			ret = new NewExpr(pos, tr, args, obj);
		}
		else if (op == Operator.ClassAccess) {
			ENode e = exprs[0];
			TypeRef tr;
			if (e instanceof EToken)
				tr = e.asType();
			else
				tr = (TypeRef)e;
			ret = new TypeClassExpr(pos, tr);
		}
		else if (op == Operator.ElemAccess) {
			ret = new ContainerAccessExpr(pos,exprs[0],exprs[1]);
		}
		else if (op == Operator.Conditional) {
			ret = new ConditionalExpr(pos,exprs[0],exprs[1],exprs[2]);
		}
		else if (exprs.length == 1) {
			if (op == Operator.Parenth) {
				ret = exprs[0];
				ret.setPrimaryExpr(true);
			}
			else
				ret = new UnaryExpr(0,op,exprs[0]);
		}
		else if (exprs.length == 2) {
			if (op == Operator.Access)
				ret = new AccessExpr(pos, exprs[0], exprs[1].ident);
			else if (op == Operator.Cast || op == Operator.CastForce)
				ret = new CastExpr(pos, (TypeRef)exprs[0], exprs[1]);
			else if (op == Operator.Reinterp)
				ret = new ReinterpExpr(pos, (TypeRef)exprs[0], exprs[1]);
			else if (op==Operator.InstanceOf)
				ret = new InstanceofExpr(pos,exprs[0],((TypeRef)exprs[1]).getType());
			else if (op==Operator.BooleanOr)
				ret = new BinaryBooleanOrExpr(pos,exprs[0],exprs[1]);
			else if (op==Operator.BooleanAnd)
				ret = new BinaryBooleanAndExpr(pos,exprs[0],exprs[1]);
			else
				ret = new BinaryExpr(pos,op,exprs[0],exprs[1]);
		}
		else
			throw new CompilerException(this,"Cannot build expression "+this);
		if (isAutoGenerated())
			ret.setAutoGenerated(true);
		return ret;
	}
	
}

/**
 * Represents unresolved, temporary created list of expression.
 */
@ThisIsANode(lang=void)
public final class UnresSeqs extends UnresExpr {
	
	@virtual typedef This  = UnresSeqs;

	@nodeData public String				sep;
	@nodeData public ENode∅				exprs;

	public UnresSeqs() {}

	public UnresSeqs(String sep, ENode[] exprs) {
		this.sep = sep;
		foreach (ENode e; exprs; !(e instanceof EToken && e.isOperator()))
			this.exprs.add(e);
	}
	
	public ENode[] getArgs() { return exprs; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		boolean add_sep = false;
		foreach (ENode e; exprs) {
			if (add_sep)
				sb.append(' ').append(sep).append(' ');
			sb.append(e);
			add_sep = true;
		}
		return sb.toString();
	}

	public ENode closeBuild() {
		for (int i=0; i < exprs.length; i++)
			exprs[i] = exprs[i].closeBuild().detach();
		return new BlockRewr(exprs);
	}
	
}

/**
 * Represents unresolved, temporary created call expression.
 *
 * 'exprs' field is @nodeData to not change the owner of the expressions.
 * The owner will be changed when concrete, resolved multi-expression is created.
 */
@ThisIsANode(lang=void)
public final class UnresCallExpr extends UnresExpr {

	@virtual typedef This  = UnresCallExpr;

	@nodeData public ENode				obj;
	@nodeData public SymbolRef<DNode>	func;
	@nodeData public TypeRef∅			targs;
	@nodeData public ENode∅			args;

	public UnresCallExpr() {}

	public UnresCallExpr(int pos, ENode obj, DNode func, TypeRef[] targs, ENode[] args, boolean super_flag) {
		this(pos, obj, new SymbolRef<DNode>(pos, func), targs, args, super_flag);
	}
	public UnresCallExpr(int pos, ENode obj, SymbolRef<DNode> func, TypeRef[] targs, ENode[] args, boolean super_flag) {
		this.pos = pos;
		this.obj = obj;
		this.func = func;
		this.targs.addAll(targs);
		this.args.addAll(args);
		this.setSuperExpr(super_flag);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(obj).append('.').append(func);
		sb.append('(');
		for (int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if (i < args.length)
				sb.append(',');
		}
		sb.append(')');
		return sb.toString(); 
	}

	public ENode closeBuild() {
		ENode obj = this.obj.closeBuild().detach();
		for (int i=0; i < targs.length; i++)
			targs[i].detach();
		for (int i=0; i < args.length; i++)
			args[i] = args[i].closeBuild().detach();
		if (obj instanceof TypeRef) {
			if (func.dnode instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, (SymbolRef<Method>)~func, targs, args);
				return ce;
			} else {
				Field f = (Field)func.dnode;
				return new ClosureCallExpr(pos, new SFldExpr(pos, f), args);
			}
		} else {
			if (func.dnode instanceof Method) {
				CallExpr ce = new CallExpr(pos, obj, (SymbolRef<Method>)~func, targs, args);
				if (isSuperExpr())
					ce.setSuperExpr(true);
				ce.setCastCall(this.isCastCall());
				return ce;
			} else {
				return new ClosureCallExpr(pos, obj, args);
			}
		}
	}
}


/**
 * Represents unresolved, temporary created access expression.
 */
@ThisIsANode(lang=void)
public final class AccFldExpr extends UnresExpr {

	@virtual typedef This  = AccFldExpr;

	@nodeData public ENode				obj;
	@nodeData public Field				fld;

	public AccFldExpr() {}

	public AccFldExpr(int pos, ENode obj, Field fld) {
		this.pos = pos;
		this.op = op;
		this.obj = obj;
		this.fld = fld;
		assert (obj != null || fld.isStatic());
	}
	
	public String toString() {
		if (fld.isStatic())
			return fld.parent()+ "."+fld.sname;
		else
			return obj+ "."+fld.sname;
	}
	
	public ENode closeBuild() {
		ENode ret;
		if (fld.isStatic()) {
			if (obj instanceof TypeRef)
				ret = new SFldExpr(pos,(TypeRef)obj.detach(),fld);
			else
				ret = new SFldExpr(pos,null,fld);
		} else {
			ret = new IFldExpr(pos,obj.closeBuild().detach(),fld);
		}
		if (isAutoGenerated())
			ret.setAutoGenerated(true);
		return ret;
	}
}



