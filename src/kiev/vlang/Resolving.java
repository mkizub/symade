package kiev.vlang;

import kiev.*;
import kiev.parser.UnresCallExpr;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
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
	public CallType	mt;
	
	ASTNode				space_prev;
	
	public boolean isStaticAllowed()   { return (flags & noStatic)   == 0; }
	public boolean isImportsAllowed()  { return (flags & noImports)  == 0; }
	public boolean isForwardsAllowed() { return (flags & noForwards) == 0; }
	public boolean isSuperAllowed()    { return (flags & noSuper)    == 0; }

	private ResInfo() {}
	
	public ResInfo(ASTNode.NodeView from) {
		this(from.getNode(), 0);
	}
	public ResInfo(ASTNode.NodeView from, int fl) {
		this(from.getNode(), fl);
	}
	public ResInfo(ASTNode from) {
		this(from, 0);
	}
	public ResInfo(ASTNode from, int fl) {
		flags = fl;
		flags_stack = new int[16];
		forwards_stack = new ASTNode[16];
		if (from instanceof Struct)
			from_scope = (Struct)from;
		else
			from_scope = from.ctx_clazz;
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
	
	public void enterReinterp(Type tp) {
		forwards_stack[forwards_p++] = new ReinterpExpr(tp);
		flags_stack[flags_p++] = flags;
		flags |= noStatic | noImports;
		trace(Kiev.debugResolve,"Entering dewrap, now "+this);
	}
	public void leaveReinterp() {
		forwards_stack[--forwards_p] = null;
		flags = flags_stack[--flags_p];
		trace(Kiev.debugResolve,"Leaving dewarp, now "+this);
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
	
	public ENode buildVarAccess(ASTNode at, Var var) {
		if (var.name.name == Constants.nameThis)
			return new ThisExpr(at.pos);
		return new LVarExpr(at.pos, var);
	}

	public ENode buildAccess(ASTNode at, ASTNode from) {
		if (isEmpty())
			return buildAccess(at, null, from);
			//throw new CompilerException(pos, "Empty access build requested");
		else
			return buildAccess(at, from, forwards_stack[--forwards_p]);
	}
	
	public ENode buildAccess(ASTNode at, ASTNode from, ASTNode node) {
		trace(Kiev.debugResolve,"Building access from "+from+" to "+node+" via "+this);
		if (from == null && isEmpty()) {
			// var or static field
			if (node instanceof Field) {
				if (node.isStatic())
					return new SFldExpr(at.pos,(Field)node);
				throw new CompilerException(at, "Static access to an instance field "+node);
			}
			if (node instanceof Var) {
				return buildVarAccess(at, (Var)node);
			}
		}
		int n = 0;
		ENode e = null;
		if (from != null) {
			if (from instanceof TypeRef)
				from = ((TypeRef)from).getType().getStruct();
			if (from instanceof Struct) {
				// static field access
				if (isEmpty() && node instanceof Field) {
					if (node.isStatic())
						return new SFldExpr(at.pos,(Field)node);
					throw new CompilerException(at, "Static access to an instance field "+node);
				}
				else if (forwards_stack[0] instanceof Field) {
					Field ff = (Field)forwards_stack[0];
					if (!ff.isStatic())
						throw new CompilerException(at, "Static access to an instance field "+ff);
					e = new SFldExpr(at.pos,ff);
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
			e = buildVarAccess(at, (Var)forwards_stack[n]);
			n++;
		}
		if (e != null && (node instanceof Field || node instanceof ReinterpExpr)) {
			for (; n < forwards_p; n++) {
				ASTNode fwn = forwards_stack[n];
				if (fwn instanceof ReinterpExpr) {
					fwn.expr = e;
					e = fwn;
				}
				else if (fwn instanceof Field) {
					if (fwn.isStatic())
						throw new CompilerException(at, "Non-static access to static field "+fwn+" via "+this);
					e = new IFldExpr(at.pos, e, (Field)fwn);
				}
				else
					throw new CompilerException(at, "Don't know how to build access to field "+node+" through "+e+" via "+this+" because of "+fwn);
			}
			if (node instanceof Field) {
				e = new IFldExpr(at.pos, e, (Field)node);
			} else {
				((ReinterpExpr)node).expr = e;
				e = (ReinterpExpr)node;
			}
			return e;
		}
		throw new CompilerException(at, "Don't know how to build access to "+node+" from "+from+" via "+this);
	}
	
	public ENode buildCall(ASTNode at, ENode from, ASTNode node, CallType mt, ENode[] args) {
		if (node instanceof Method) {
			Method meth = (Method)node;
			if (from == null && forwards_p == 0) {
				if !(meth.isStatic())
					throw new CompilerException(at, "Don't know how to build call of "+meth+" via "+this);
				//return new CallExpr(pos,meth,args);
				return new UnresCallExpr(at.pos, new TypeRef(meth.ctx_clazz.ctype), meth, mt, args, false);
			}
			ENode expr = from;
			if (forwards_p > 0)
				expr = buildAccess(at, from, forwards_stack[--forwards_p]);
			return new UnresCallExpr(at.pos,expr,meth,mt,args,false);
		}
		else if (node instanceof Field) {
			Field f = (Field)node;
			if (from == null && forwards_p == 0) {
				if !(node.isStatic())
					throw new CompilerException(at, "Don't know how to build closure for "+node+" via "+this);
				return new UnresCallExpr(at.pos, new TypeRef(f.ctx_clazz.ctype), f, mt, args, false);
			}
			ENode expr = buildAccess(at, from, f);
			return new UnresCallExpr(at.pos,expr,f,mt,args,false);
		}
		else if (node instanceof Var) {
			Var var = (Var)node;
			ENode expr = buildAccess(at, from, var);
			return new UnresCallExpr(at.pos,expr,var,mt,args,false);
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
		ri.flags          = this.flags;
		ri.flags_stack    = (int[])this.flags_stack.clone();
		ri.flags_p        = this.flags_p;
		ri.forwards_stack = (ASTNode[])this.forwards_stack.clone();
		ri.forwards_p     = this.forwards_p;
		ri.transforms     = this.transforms;
		ri.mt             = this.mt;
		ri.from_scope     = this.from_scope;
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
	public rule resolveNameR(DNode@ node, ResInfo path, KString name);
}

public interface ScopeOfMethods extends Scope {
	public rule resolveMethodR(DNode@ node, ResInfo path, KString name, CallType mt);
}

public interface ScopeOfOperators extends ScopeOfNames {
	public rule resolveOperatorR(Operator@ op);
}



