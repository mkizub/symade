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

final class RewriteContext {
	final ASTNode root;

	RewriteContext(ASTNode root) {
		this.root = root;
	}
}

@node
public abstract class RewriteNode extends SNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNode;
	@virtual typedef VView = VRewriteNode;

	@nodeview
	public static view VRewriteNode of RewriteNode extends VSNode {
	}

	public RewriteNode() {}

	public abstract Object makeValue(RewriteContext ctx);

	public void rewrite(ASTNode self) {
		ASTNode rn = (ASTNode)makeValue(new RewriteContext(self));
		self.replaceWithNode(rn);
		throw ReWalkNodeException.instance;
	}
}

@node
public final class RewriteNodeFactory extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeFactory;
	@virtual typedef VView = VRewriteNodeFactory;

	     public Class					node_class;
	@att public NArr<RewriteNodeArg>	args;

	@nodeview
	public static final view VRewriteNodeFactory of RewriteNodeFactory extends VRewriteNode {
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

	public Object makeValue(RewriteContext ctx) {
		ASTNode res = (ASTNode)node_class.newInstance();
		foreach (RewriteNodeArg rn; args) {
			ASTNode r = rn.makeValue(ctx);
			res.setVal(rn.attr, ~r);
		}
		return res;
	}
}

@node
public final class RewriteNodeArg extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeArg;
	@virtual typedef VView = VRewriteNodeArg;

	@att public RewriteNode		node;
	@att public String			attr;

	@setter
	public void set$attr(String value) {
		this.attr = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static final view VRewriteNodeArg of RewriteNodeArg extends VRewriteNode {
		public:ro RewriteNode		node;
		public:ro String			attr;
	}

	public RewriteNodeArg() {}
	public RewriteNodeArg(String attr, RewriteNode node) {
		this.attr = attr;
		this.node = node;
	}

	public Object makeValue(RewriteContext ctx) {
		return node.makeValue(ctx);
	}
}

@node
public final class RewriteNodeVar extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeVar;
	@virtual typedef VView = VRewriteNodeVar;

	@att public String			name;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static final view VRewriteNodeVar of RewriteNodeVar extends VRewriteNode {
		public:ro String			name;
	}

	public RewriteNodeVar() {}
	public RewriteNodeVar(String name) {
		this.name = name;
	}

	public Object makeValue(RewriteContext ctx) {
		if (name == "self")
			return ctx.root;
		else
			return ctx.root.getVal(name);
	}
}

@node
public final class RewriteNodeAccess extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeAccess;
	@virtual typedef VView = VRewriteNodeAccess;

	@att public RewriteNode		node;
	@att public String			attr;

	@setter
	public void set$attr(String value) {
		this.attr = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static final view VRewriteNodeAccess of RewriteNodeAccess extends VRewriteNode {
		public:ro RewriteNode		node;
		public:ro String			attr;
	}

	public RewriteNodeAccess() {}
	public RewriteNodeAccess(RewriteNode node, String attr) {
		this.node = node;
		this.attr = attr;
	}

	public Object makeValue(RewriteContext ctx) {
		return node.getVal(attr);
	}
}


