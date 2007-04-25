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

import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class RewriteContext {
	public final ASTNode root;
	public final Hashtable<String,Object> args;

	public RewriteContext(ASTNode root, Hashtable<String,Object> args) {
		this.root = root;
		this.args = args;
	}

	public String replace(String s) {
		foreach (String k; args.keys())
			s = s.replace('\''+k+'\'', String.valueOf(args.get(k)));
		return s;
	}
	public Object fixup(AttrSlot attr, Object o) {
		if (o instanceof ConstStringExpr) {
			String s = replace(o.value);
			if (attr.clazz == String.class) {
				o = s;
			}
			else if (attr.clazz == SymbolRef.class) {
				o = new SymbolRef<DNode>(s);
			}
			else if (attr.clazz == Symbol.class) {
				o = new Symbol(s);
			}
			else if (attr.clazz == Operator.class) {
				Operator op = Operator.getOperatorByName(s);
				if (op == null)
					op = Operator.getOperatorByDecl(s);
				o = op;
			}
		}
		else if (o instanceof TypeDecl) {
			if (attr.clazz == TypeRef.class)
				o = new TypeRef(o.xtype);
			else if (attr.clazz == ENode.class)
				o = new TypeRef(o.xtype);
		}
		if (o instanceof ANode && attr.is_attr)
			o = o.detach(); //assert(!o.isAttached());
		return o;
	}
}

@node
public final class RewriteMatch extends ENode {

	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in", seq="false")	RewriteCase[]		cases;
	}

	@virtual typedef This  = RewriteMatch;

	@att public RewriteCase[]		cases;

	public RewriteMatch() {}

	public ANode doRewrite(RewriteContext ctx) {
		foreach (RewriteCase rc; cases) {
			if (rc.var.match(ctx.root))
				return rc.doRewrite(ctx);
		}
		throw new CompilerException(ctx.root, "Cannot rewrite");
	}

	public ASTNode matchCase(ASTNode root) {
		foreach (RewriteCase rc; cases; rc.var.match(root))
			return rc;
		throw new CompilerException(root, "Cannot rewrite");
	}

}

@node
public final class RewritePattern extends Var {

	@dflow(out="this:in") private static class DFI {}

	public static final RewritePattern[] emptyArray = new RewritePattern[0];
	
	@virtual typedef This  = RewritePattern;

	@att public RewritePattern[]		vars;

	public RewritePattern() { super(REWRITE_PATTERN); }
	public RewritePattern(String name, TypeRef tp) {
		super(name, tp, REWRITE_PATTERN, 0);
	}
	public RewritePattern(String name, ASTNodeType tp) {
		super(name, new TypeRef(tp), REWRITE_PATTERN, 0);
	}

	@getter public Type get$type() { return new ASTNodeType(this); }
	public Type	getType() { return new ASTNodeType(this); }

	public boolean match(ASTNode node) {
		if ( ((ASTNodeMetaType)((ASTNodeType)getType()).meta_type).clazz.equals(node.getClass()) )
			return true;
		return false;
	}

}

@node
public final class RewriteCase extends ENode implements ScopeOfNames {

	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")			RewritePattern		var;
	@dflow(in="var", seq="true")	ASTNode[]			stats;
	}

	public static final RewriteCase[] emptyArray = new RewriteCase[0];
	
	@virtual typedef This  = RewriteCase;

	@att public RewritePattern		var;
	@att public ASTNode[]			stats;

	public RewriteCase() {}

	public rule resolveNameR(ASTNode@ node, ResInfo info)
	{
		info.checkNodeName(var),
		node ?= var
	;
		info.isForwardsAllowed(),
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveNameAccessR(node,info)
	}

	public ANode doRewrite(RewriteContext ctx) {
		ctx.args.put(var.sname, ctx.root);
		ANode res = null;
		foreach (ASTNode stat; stats)
			res = stat.doRewrite(ctx);
		return res;
	}

}

@node
public final class RewriteNodeFactory extends ENode {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in", seq="true")	RewriteNodeArg[]	args;
	}
	
	@virtual typedef This  = RewriteNodeFactory;

	@att public TypeRef					type;
	@att public RewriteNodeArg[]		args;
	     private Class					node_class;

	public RewriteNodeFactory() {}
	public RewriteNodeFactory(String class_name) {
		if (class_name.indexOf('\u001f') > 0) {
			node_class = Class.forName(class_name.replace('\u001f','.'));
			type = new TypeNameRef(class_name);
		} else {
			node_class = Class.forName("kiev.vlang."+class_name);
			type = new TypeNameRef("kiev\u001fvlang\u001f"+class_name);
		}
	}

	public ANode doRewrite(RewriteContext ctx) {
		ASTNode res = (ASTNode)node_class.newInstance();
		foreach (RewriteNodeArg rn; args) {
			ANode r = rn.doRewrite(ctx);
			AttrSlot attr = null;
			foreach (AttrSlot a; res.values(); a.name == rn.attr) {
				attr = a;
				break;
			}
			if (attr == null)
				throw new CompilerException(ctx.root, "Cannot find attribute "+rn.attr+" in node "+res.getClass().getName());
			if (attr instanceof SpaceAttrSlot) {
				if (r instanceof RewriteNodeArgArray) {
					foreach (ASTNode o; ((RewriteNodeArgArray)r).args)
						attr.add(res, (ASTNode)ctx.fixup(attr,o));
				} else {
					attr.add(res, (ASTNode)ctx.fixup(attr,r));
				}
			} else {
				attr.set(res, ctx.fixup(attr,r));
			}
		}
		return res;
	}
}

@node
public final class RewriteNodeArg extends ENode {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")	ENode				anode;
	}
	
	public static final RewriteNodeArg[] emptyArray = new RewriteNodeArg[0];
	
	@virtual typedef This  = RewriteNodeArg;

	@att public ENode		anode;
	@att public String		attr;

	@setter
	public void set$attr(String value) {
		this.attr = (value != null) ? value.intern() : null;
	}
	
	public RewriteNodeArg() {}
	public RewriteNodeArg(String attr, ENode node) {
		this.attr = attr;
		this.anode = node;
		assert( attr != null && node != null );
	}

	public ANode doRewrite(RewriteContext ctx) {
		return anode.doRewrite(ctx);
	}
}

@node(name="NewArrInitialized")
public final class RewriteNodeArgArray extends ENode {
	
	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")	ENode[]		args;
	}

	@virtual typedef This  = RewriteNodeArgArray;

	@att public ASTNode[]				args;

	public RewriteNodeArgArray() {}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		for(int i=0; i < args.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(args[i]);
		}
		sb.append('}');
		return sb.toString();
	}
}

@node
public class IfElseRewr extends ENode {
	
	@dflow(out="join thenSt elseSt") private static class DFI {
	@dflow(in="this:in")	ENode		cond;
	@dflow(in="cond:true")	ENode		thenSt;
	@dflow(in="cond:false")	ENode		elseSt;
	}

	@virtual typedef This  = IfElseRewr;
	@virtual typedef JView = JENode;
	@virtual typedef RView = RENode;

	@att public ENode			cond;
	@att public ENode			thenSt;
	@att public ENode			elseSt;

	public IfElseRewr() {}
	
	public IfElseRewr(int pos, ENode cond, ENode thenSt, ENode elseSt) {
		this.pos = pos;
		this.cond = cond;
		this.thenSt = thenSt;
		this.elseSt = elseSt;
	}

	public void postVerify() {
		if (!cond.isConstantExpr())
			throw new RuntimeException("Non-const 'if#' condition "+cond);
		if (thenSt == null || elseSt == null)
			throw new RuntimeException("Missed then or else part of 'if#'");
	}

	public ANode doRewrite(RewriteContext ctx) {
		assert (cond.isConstantExpr());
		Boolean b = (Boolean)cond.getConstValue();
		if (b.booleanValue())
			return thenSt.doRewrite(ctx);
		else
			return elseSt.doRewrite(ctx);
	}

}


