package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 0 $
 *
 */

public final class RewriteContext {
	public final ASTNode root;

	public RewriteContext(ASTNode root) {
		this.root = root;
	}
}

@node
public final class RewriteMatch extends ENode {

	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in", seq="false")	RewriteCase[]		cases;
	}

	@virtual typedef This  = RewriteMatch;
	@virtual typedef VView = VRewriteMatch;

	@att public NArr<RewriteCase>		cases;

	@nodeview
	public static final view VRewriteMatch of RewriteMatch extends VENode {
		public:ro NArr<RewriteCase>		cases;
	}

	public RewriteMatch() {}

	public Object doRewrite(RewriteContext ctx) {
		foreach (RewriteCase rc; cases) {
			if (rc.var.match(ctx.root))
				return rc.doRewrite(ctx);
		}
		throw new CompilerException(ctx.root, "Cannot rewrite");
	}

}

@node
public final class RewritePattern extends Var {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = RewritePattern;
	@virtual typedef VView = VRewritePattern;

	@att public NArr<RewritePattern>	vars;

	@nodeview
	public static final view VRewritePattern of RewritePattern extends VVar {
		public:ro NArr<RewritePattern>	vars;
	}

	public RewritePattern() {}
	public RewritePattern(Symbol id, TypeRef tp) {
		super(id, tp, 0);
	}
	public RewritePattern(String id, ASTNodeType tp) {
		super(new Symbol(id), new TypeRef(tp), 0);
	}

	@getter public Type get$type() { return new ASTNodeType(this); }
	public Type	getType() { return new ASTNodeType(this); }

	public boolean match(ASTNode node) {
		if ( ((ASTNodeMetaType)((ASTNodeType)getType()).meta_type).clazz.qname().equals(node.getClass().getName()) )
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

	@virtual typedef This  = RewriteCase;
	@virtual typedef VView = VRewriteCase;

	@att public RewritePattern		var;
	@att public NArr<ASTNode>		stats;

	@nodeview
	public static final view VRewriteCase of RewriteCase extends VENode {
		public:ro RewritePattern	var;
		public:ro NArr<ASTNode>		stats;
	}

	public RewriteCase() {}

	public rule resolveNameR(ASTNode@ node, ResInfo info, String name)
	{
		var.id.equals(name),
		node ?= var
	;
		info.isForwardsAllowed(),
		var.isForward(),
		info.enterForward(var) : info.leaveForward(var),
		var.getType().resolveNameAccessR(node,info,name)
	}

	public Object doRewrite(RewriteContext ctx) {
		ASTNode res = null;
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
	@virtual typedef VView = VRewriteNodeFactory;

	     public Class					node_class;
	@att public NArr<RewriteNodeArg>	args;

	@nodeview
	public static final view VRewriteNodeFactory of RewriteNodeFactory extends VENode {
		public:ro Class					node_class;
		public:ro NArr<RewriteNodeArg>	args;
	}

	public RewriteNodeFactory() {}
	public RewriteNodeFactory(Class node_class) {
		this.node_class = node_class;
	}
	public RewriteNodeFactory(Class node_class, RewriteNodeArg[] args) {
		this.node_class = node_class;
		this.args.addAll(args);
	}
	public RewriteNodeFactory(String class_name) {
		if (class_name.indexOf(".") > 0)
			node_class = Class.forName(class_name);
		else
			node_class = Class.forName("kiev.vlang."+class_name);
	}

	public Object doRewrite(RewriteContext ctx) {
		ASTNode res = (ASTNode)node_class.newInstance();
		foreach (RewriteNodeArg rn; args) {
			ASTNode r = rn.doRewrite(ctx);
			AttrSlot attr = null;
			foreach (AttrSlot a; res.values(); a.name == rn.attr) {
				attr = a;
				break;
			}
			if (attr == null)
				throw new CompilerException(ctx.root, "Cannot find attribute "+rn.attr+" in node "+res.getClass().getName());
			if (r instanceof ConstStringExpr) {
				if (attr.clazz == SymbolRef.class)
					r = new SymbolRef(r.value);
				else if (attr.clazz == Symbol.class)
					r = new Symbol(r.value);
			}
			attr.set(res, ~r);
		}
		return res;
	}

}

@node
public final class RewriteNodeArg extends ENode {
	
	@dflow(out="this:in") private static class DFI {
	@dflow(in="this:in")	ENode				node;
	}
	
	@virtual typedef This  = RewriteNodeArg;
	@virtual typedef VView = VRewriteNodeArg;

	@att public ENode		node;
	@att public String		attr;

	@setter
	public void set$attr(String value) {
		this.attr = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static final view VRewriteNodeArg of RewriteNodeArg extends VENode {
		public:ro ENode		node;
		public:ro String	attr;
	}

	public RewriteNodeArg() {}
	public RewriteNodeArg(String attr, ENode node) {
		this.attr = attr;
		this.node = node;
	}

	public Object doRewrite(RewriteContext ctx) {
		return node.doRewrite(ctx);
	}
}


