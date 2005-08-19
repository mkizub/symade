/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Resolving.java,v 1.3 1998/10/26 23:47:22 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */

public class ResInfo {
	
	public static final int noStatic   = 0x0001;
	public static final int noImports  = 0x0002;
	public static final int noForwards = 0x0004;
	public static final int noSuper    = 0x0008;
	
	private int			flags;
	private int[]		flags_stack;
	private int			flags_p;
	private ASTNode[]	forwards_stack;
	private int			forwards_p;
	
	private int			transforms;
	private Struct		from_scope;
	
	// a real type of the method in Method.compare() call
	MethodType			mt;
	
	public boolean isStaticAllowed()   { return (flags & noStatic)   == 0; }
	public boolean isImportsAllowed()  { return (flags & noImports)  == 0; }
	public boolean isForwardsAllowed() { return (flags & noForwards) == 0; }
	public boolean isSuperAllowed()    { return (flags & noSuper)    == 0; }

	public ResInfo() {
		flags_stack = new int[16];
		forwards_stack = new ASTNode[16];
		from_scope = PassInfo.clazz;
	}
	
	public ResInfo(int fl) {
		flags = fl;
		flags_stack = new int[16];
		forwards_stack = new ASTNode[16];
		from_scope = PassInfo.clazz;
	}
	
	public void enterMode(int fl) {
		flags_stack[flags_p++] = flags;
		flags |= fl;
		trace(Kiev.debugResolve,"Entering mode "+fl+", now "+this);
	}
	public void leaveMode() {
		flags = flags_stack[--flags_p];
		trace(Kiev.debugResolve,"Leaving mode, now "+this);
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
		trace(Kiev.debugResolve,"Entering forward of "+node+", now "+this);
	}

	public void leaveForward(ASTNode node) {
		leaveForward(node, 1);
	}
	public void leaveForward(ASTNode node, int incr) {
		forwards_stack[--forwards_p] = null;
		flags = flags_stack[--flags_p];
		transforms -= incr;
		trace(Kiev.debugResolve,"Leaving forward of "+node+", now "+this);
	}

	public void enterSuper() {
		enterSuper(1);
	}
	public void enterSuper(int incr) {
		assert ((flags & noSuper) == 0);
		flags_stack[flags_p++] = flags;
		flags |= noImports;
		transforms += incr;
		trace(Kiev.debugResolve,"Entering super, now "+this);
	}

	public void leaveSuper() {
		leaveSuper(1);
	}
	public void leaveSuper(int incr) {
		flags = flags_stack[--flags_p];
		transforms -= incr;
		trace(Kiev.debugResolve,"Leaving super, now "+this);
	}
	
	public boolean check(ASTNode n) {
		if (n instanceof Var) {
			return true;
		}
		else if (n instanceof Field || n instanceof Method) {
			if (!isStaticAllowed()) {
				if (n.isStatic())
					return false;
			}
			if (isStaticAllowed()) {
				if (!n.isStatic())
					return false;
			}
			if (n.isPrivate()) {
				// check visibility of this or inner classes
				Struct s = from_scope;
				while (s != null && s != n.parent && s.package_clazz.isClazz())
					s = s.package_clazz;
				if (s == null || s != n.parent)
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
	
	public Expr buildVarAccess(int pos, Var var) {
		if (var.isLocalRuleVar()) {
			return new LocalPrologVarAccessExpr(pos, var);
		}
		else if (var.name.name == Constants.nameThis) {
			return new ThisExpr(pos);
		}
		return new VarAccessExpr(pos, var);
	}

	public Expr buildAccess(int pos, ASTNode from) {
		if (isEmpty())
			throw new CompilerException(pos, "Empty access build requested");
		return buildAccess(pos, from, forwards_stack[--forwards_p]);
	}
	
	public Expr buildAccess(int pos, ASTNode from, ASTNode node) {
		trace(Kiev.debugResolve,"Building access from "+from+" to "+node+" via "+this);
		if (from == null && isEmpty()) {
			// var or static field
			if (node instanceof Field) {
				if (node.isStatic())
					return new StaticFieldAccessExpr(pos,(Struct)node.parent,(Field)node);
				throw new CompilerException(pos, "Static access to an instance field "+node);
			}
			if (node instanceof Var) {
				return buildVarAccess(pos, (Var)node);
			}
		}
		int n = 0;
		Expr e = null;
		if (from != null) {
			if (from instanceof TypeRef)
				from = ((TypeRef)from).getType().getStruct();
			if (from instanceof Struct) {
				// static field access
				if (isEmpty() && node instanceof Field) {
					if (node.isStatic())
						return new StaticFieldAccessExpr(pos,(Struct)node.parent,(Field)node);
					throw new CompilerException(pos, "Static access to an instance field "+node);
				}
				else if (forwards_stack[0] instanceof Field) {
					if (!forwards_stack[0].isStatic())
						throw new CompilerException(pos, "Static access to an instance field "+forwards_stack[0]);
					e = new StaticFieldAccessExpr(pos,(Struct)node.parent,(Field)forwards_stack[0]);
					n++;
				}
			}
			else if (from instanceof Expr) {
				// access from something
				e = (Expr)from;
			}
		} else {
			// first node must be a var, and we are not empty
			if !(forwards_stack[n] instanceof Var)
				throw new CompilerException(pos, "Access must be done through a var, but for "+node+" is dove via "+this);
			e = buildVarAccess(pos, (Var)forwards_stack[n]);
			n++;
		}
		if (e != null && node instanceof Field) {
			for (; n < forwards_p; n++) {
				if !(forwards_stack[n] instanceof Field)
					throw new CompilerException(pos, "Don't know how to build access to field "+node+" through "+e+" via "+this+" because of "+forwards_stack[n]);
				Field f = (Field)forwards_stack[n];
				if (f.isStatic())
					throw new CompilerException(pos, "Non-static access to static field "+f+" via "+this);
				e = new AccessExpr(pos, e, f);
			}
			e = new AccessExpr(pos, e, (Field)node);
			return e;
		}
		throw new CompilerException(pos, "Don't know how to build access to "+node+" from "+from+" via "+this);
	}
	
	public Expr buildCall(int pos, Expr from, ASTNode node, Expr[] args) {
		if (node instanceof Method) {
			Method meth = (Method)node;
			if (from == null && forwards_p == 0) {
				if !(meth.isStatic())
					throw new CompilerException(pos, "Don't know how to build call of "+meth+" via "+this);
				return new CallExpr(pos,meth,args);
			}
			Expr expr = from;
			if (forwards_p > 0)
				expr = buildAccess(pos, from, forwards_stack[--forwards_p]);
			return new CallAccessExpr(pos,expr,meth,args);
		}
		else if (node instanceof Field || node instanceof Var) {
			if (from == null && forwards_p == 0) {
				if !(node.isStatic())
					throw new CompilerException(pos, "Don't know how to build closure for "+node+" via "+this);
				return new ClosureCallExpr(pos,null,null,node,args);
			}
			Expr expr = from;
			if (forwards_p > 0)
				expr = buildAccess(pos, from, forwards_stack[--forwards_p]);
			return new ClosureCallExpr(pos,null,expr,node,args);
		}
		throw new CompilerException(pos, "Don't know how to call "+node+" via "+this);
	}
	
	public boolean isEmpty() {
		return forwards_p == 0;
	}

	public int getTransforms() {
		return transforms;
	}

	public ResInfo copy() {
		ResInfo ri = new ResInfo();
		ri.flags          = this.flags;
		ri.flags_stack    = (int[])this.flags_stack.clone();
		ri.flags_p        = this.flags_p;
		ri.forwards_stack = (ASTNode[])this.forwards_stack.clone();
		ri.forwards_p     = this.forwards_p;
		ri.transforms     = this.transforms;
		return ri;
	}

	public void set(ResInfo ri)
	{
		if (this == ri) return;
		this.flags          = ri.flags;
		this.flags_stack    = ri.flags_stack;
		this.flags_p        = ri.flags_p;
		this.forwards_stack = ri.forwards_stack;
		this.forwards_p     = ri.forwards_p;
		this.transforms     = ri.transforms;
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
	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name);
}

public interface ScopeOfMethods extends Scope {
	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, MethodType mt);
}

public interface ScopeOfOperators extends ScopeOfNames {
	public rule resolveOperatorR(Operator@ op);
}



