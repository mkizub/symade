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

import kiev.*;
import kiev.parser.AccFldExpr;
import kiev.parser.UnresOpExpr;
import kiev.parser.UnresCallExpr;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class ResInfo {
	
	public static final int noStatic     = 0x0001;
	public static final int noImports    = 0x0002;
	public static final int noForwards   = 0x0004;
	public static final int noSuper      = 0x0008;
	public static final int noEquals     = 0x0010; // to compare as startsWith
	public static final int doImportStar = 0x0020; // to lookup in specific imports, then in the package, then in import with star

	private String		name;
	private int			flags;
	private int[]		flags_stack;
	private int			flags_p;
	private Object[]	forwards_stack;
	private int			forwards_p;
	
	private int			transforms;
	private TypeDecl	from_scope;
	
	// a real type of the method in Method.compare() call
	public CallType	mt;
	
	ASTNode				space_prev;
	
	public boolean isStaticAllowed()   { return (flags & noStatic)     == 0; }
	public boolean isImportsAllowed()  { return (flags & noImports)    == 0; }
	public boolean isForwardsAllowed() { return (flags & noForwards)   == 0; }
	public boolean isSuperAllowed()    { return (flags & noSuper)      == 0; }
	public boolean isCmpByEquals()     { return (flags & noEquals)     == 0; }
	public boolean doImportStar()      { return (flags & doImportStar) != 0; }

	private ResInfo() {}
	
	public ResInfo(ASTNode from, String nm) {
		this(from, nm, 0);
	}
	public ResInfo(ASTNode from, String nm, int fl) {
		name = nm.intern();
		flags = fl;
		flags_stack = new int[16];
		forwards_stack = new Object[16];
		try {
			if (from instanceof TypeDecl)
				from_scope = (TypeDecl)from;
			else
				from_scope = from.ctx_tdecl;
		} catch (Throwable t) {}
	}
	
	public String getName() { return name; }
	
	public void enterMode(int fl) {
		flags_stack[flags_p++] = flags;
		flags |= fl;
		trace(Kiev.debug && Kiev.debugResolve,"Entering mode "+fl+", now "+this);
	}
	public void leaveMode() {
		flags = flags_stack[--flags_p];
		trace(Kiev.debug && Kiev.debugResolve,"Leaving mode, now "+this);
	}
	
	public void enterReinterp(Type tp) {
		forwards_stack[forwards_p++] = tp;
		flags_stack[flags_p++] = flags;
		flags |= noStatic | noImports;
		trace(Kiev.debug && Kiev.debugResolve,"Entering dewrap, now "+this);
	}
	public void leaveReinterp() {
		forwards_stack[--forwards_p] = null;
		flags = flags_stack[--flags_p];
		trace(Kiev.debug && Kiev.debugResolve,"Leaving dewarp, now "+this);
	}

	public void enterForward(ASTNode node) {
		enterForward(node, 1);
	}
	public void enterForward(ASTNode node, int incr) {
		assert ((flags & noForwards) == 0);
		forwards_stack[forwards_p++] = node;
		flags_stack[flags_p++] = flags;
		flags |= noStatic | noImports;
		transforms += incr;
		trace(Kiev.debug && Kiev.debugResolve,"Entering forward of "+node+", now "+this);
	}

	public void leaveForward(ASTNode node) {
		leaveForward(node, 1);
	}
	public void leaveForward(ASTNode node, int incr) {
		forwards_stack[--forwards_p] = null;
		flags = flags_stack[--flags_p];
		transforms -= incr;
		trace(Kiev.debug && Kiev.debugResolve,"Leaving forward of "+node+", now "+this);
	}

	public void enterSuper() {
		enterSuper(1,0);
	}
	public void enterSuper(int incr) {
		enterSuper(incr,0);
	}
	public void enterSuper(int incr, int mode) {
		assert ((flags & noSuper) == 0);
		flags_stack[flags_p++] = flags;
		flags |= noImports | mode;
		transforms += incr;
		trace(Kiev.debug && Kiev.debugResolve,"Entering super, now "+this);
	}

	public void leaveSuper() {
		leaveSuper(1);
	}
	public void leaveSuper(int incr) {
		flags = flags_stack[--flags_p];
		transforms -= incr;
		trace(Kiev.debug && Kiev.debugResolve,"Leaving super, now "+this);
	}
	
	public boolean checkName(String nm) {
		if (!isCmpByEquals())
			return nm.startsWith(name);
		return nm == name;
	}
	public boolean checkNodeName(ASTNode dn) {
		return dn.hasName(name, isCmpByEquals());
	}
	public boolean check(ASTNode n) {
		if (n instanceof Var || n instanceof SNode) {
			return true;
		}
		else if (n instanceof Field || n instanceof Method) {
			DNode d = (DNode)n;
			if (!isStaticAllowed()) {
				if (d.isStatic())
					return false;
			}
			if (isStaticAllowed()) {
				if (!d.isStatic())
					return false;
			}
			if (d.isPrivate()) {
				// check visibility of this or inner classes
				TypeDecl s = from_scope;
				TypeDecl p = n.ctx_tdecl;
				while (s != null && s != p && s.package_clazz.dnode != null && s.package_clazz.dnode.isClazz())
					s = s.package_clazz.dnode;
				if (s == null || s != p)
					return false;
			}
			return true;
		}
		else {
			// struct/type
			if (!isStaticAllowed())
				return false;
		}
		return true;
	}
	
	public ENode buildVarAccess(ASTNode at, Var var, boolean generated) {
		ENode expr;
		if (var.u_name == Constants.nameThis)
			expr = new ThisExpr(at.pos);
		else if (var.u_name == Constants.nameSuper)
			expr = new SuperExpr(at.pos);
		else
			expr = new LVarExpr(at.pos, var);
		if (generated)
			expr.setAutoGenerated(true);
		return expr;
	}

	private ENode buildAccess(ASTNode at, ASTNode from, Object node, boolean node_is_generated) {
		trace(Kiev.debug && Kiev.debugResolve,"Building access from "+from+" to "+node+" via "+this);
		if (from == null && isEmpty()) {
			// var or static field
			if (node instanceof Field) {
				if (node.isStatic()) {
					ENode ret = new AccFldExpr(at.pos,null,(Field)node);
					if (node_is_generated)
						ret.setAutoGenerated(true);
					return ret;
				}
				throw new CompilerException(at, "Static access to an instance field "+node);
			}
			if (node instanceof Var)
				return buildVarAccess(at, (Var)node, node_is_generated);
		}
		int n = 0;
		ENode e = null;
		if (from != null) {
			assert (!(from instanceof TypeDecl));
			if (from instanceof TypeRef) {
				// static field access
				if (isEmpty() && node instanceof Field) {
					if (node.isStatic()) {
						ENode ret = new AccFldExpr(at.pos,(ENode)from,(Field)node);
						if (node_is_generated)
							ret.setAutoGenerated(true);
						return ret;
					}
					throw new CompilerException(at, "Static access to an instance field "+node);
				}
				else if (forwards_stack[0] instanceof Field) {
					Field ff = (Field)forwards_stack[0];
					if (!ff.isStatic())
						throw new CompilerException(at, "Static access to an instance field "+ff);
					e = new AccFldExpr(at.pos,(ENode)from,ff);
					e.setAutoGenerated(true);
					n++;
				}
			} else {
				// access from something
				e = (ENode)from;
			}
		} else {
			// first node must be a var, and we are not empty
			if !(forwards_stack[n] instanceof Var)
				throw new CompilerException(at, "Access must be done through a var, but for "+node+" is dove via "+this);
			e = buildVarAccess(at, (Var)forwards_stack[n], true);
			n++;
		}
		if (e != null && (node instanceof Field || node instanceof Type)) {
			for (; n < forwards_p; n++) {
				Object fwn = forwards_stack[n];
				if (fwn instanceof Type) {
					TypeRef tr = new TypeRef((Type)fwn);
					tr.setAutoGenerated(true);
					e = new UnresOpExpr(e.pos, Operator.Reinterp, new ENode[]{tr, e});
				}
				else if (fwn instanceof Field) {
					if (fwn.isStatic())
						throw new CompilerException(at, "Non-static access to static field "+fwn+" via "+this);
					e = new AccFldExpr(at.pos, e, (Field)fwn);
				}
				else
					throw new CompilerException(at, "Don't know how to build access to field "+node+" through "+e+" via "+this+" because of "+fwn);
				e.setAutoGenerated(true);
			}
			if (node instanceof Field) {
				e = new AccFldExpr(at.pos, e, (Field)node);
			} else {
				TypeRef tr = new TypeRef((Type)node);
				if (node_is_generated)
					tr.setAutoGenerated(true);
				e = new UnresOpExpr(e.pos, Operator.Reinterp, new ENode[]{tr, e});
			}
			if (node_is_generated)
				e.setAutoGenerated(true);
			return e;
		}
		throw new CompilerException(at, "Don't know how to build access to "+node+" from "+from+" via "+this);
	}
	
	public ENode buildAccess(ASTNode at, ASTNode from, Object node) {
		return buildAccess(at, from, node, false);
	}

	public ENode buildCall(ASTNode at, ENode from, ASTNode node, TypeRef[] targs, ENode[] args) {
		if (node instanceof Method) {
			Method meth = (Method)node;
			if (from == null && forwards_p == 0) {
				if !(meth.isStatic())
					throw new CompilerException(at, "Don't know how to build call of "+meth+" via "+this);
				TypeRef tr = new TypeRef(meth.ctx_tdecl.xtype);
				tr.setAutoGenerated(true);
				return new UnresCallExpr(at.pos, tr, meth, targs, args, false);
			}
			ENode expr = from;
			if (forwards_p > 0)
				expr = buildAccess(at, from, forwards_stack[--forwards_p], true);
			return new UnresCallExpr(at.pos,expr,meth,targs,args,false);
		}
		else if (node instanceof Field) {
			Field f = (Field)node;
			if (from == null && forwards_p == 0) {
				if !(node.isStatic())
					throw new CompilerException(at, "Don't know how to build closure for "+node+" via "+this);
				return new UnresCallExpr(at.pos, new TypeRef(f.ctx_tdecl.xtype), f, targs, args, false);
			}
			ENode expr = buildAccess(at, from, f, false);
			return new UnresCallExpr(at.pos,expr,f,targs,args,false);
		}
		else if (node instanceof Var) {
			Var var = (Var)node;
			ENode expr = buildAccess(at, from, var, false);
			return new UnresCallExpr(at.pos,expr,var,targs,args,false);
		}
		throw new CompilerException(at, "Don't know how to call "+node+" via "+this);
	}
	
	public boolean isEmpty() {
		return forwards_p == 0;
	}

	public int getTransforms() {
		return transforms;
	}

	public ResInfo copy() {
		ResInfo ri = new ResInfo();
		ri.name           = this.name;
		ri.flags          = this.flags;
		ri.flags_stack    = (int[])this.flags_stack.clone();
		ri.flags_p        = this.flags_p;
		ri.forwards_stack = (Object[])this.forwards_stack.clone();
		ri.forwards_p     = this.forwards_p;
		ri.transforms     = this.transforms;
		ri.mt             = this.mt;
		ri.from_scope     = this.from_scope;
		return ri;
	}

	public void set(ResInfo ri)
	{
		if (this == ri) return;
		this.name           = ri.name;
		this.flags          = ri.flags;
		this.flags_stack    = ri.flags_stack;
		this.flags_p        = ri.flags_p;
		this.forwards_stack = ri.forwards_stack;
		this.forwards_p     = ri.forwards_p;
		this.transforms     = ri.transforms;
		this.mt             = ri.mt;
		this.from_scope     = ri.from_scope;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < forwards_p; i++) {
			sb.append(forwards_stack[i]);
			sb.append(':');
		}
		sb.append(transforms);
		sb.append('[');
		for (int i=0; i < flags_p; i++) {
			sb.append(flags_stack[i]);
			sb.append(',');
		}
		sb.append(flags);
		sb.append(']');
		return sb.toString();
	}

};

public interface Scope {
}

public interface ScopeOfNames extends Scope {
	public rule resolveNameR(ASTNode@ node, ResInfo path);
}

public interface ScopeOfMethods extends Scope {
	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt);
}

