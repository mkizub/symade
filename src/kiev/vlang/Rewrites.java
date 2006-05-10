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
public final class RewriteNodeFactory extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeFactory;
	@virtual typedef VView = VRewriteNodeFactory;

	     public Class					node_class;
	@att public NArr<RewriteNodeArg>	args;

	@nodeview
	public static final view VRewriteNodeFactory of RewriteNodeFactory extends VENode {
		public:ro Class					node_class;
		public:ro NArr<RewriteNodeArg>	args;

		public boolean preResolveIn() { return false; }
		public boolean mainResolveIn() { return false; }
		public boolean preVerify() { return false; }
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
			res.setVal(rn.attr, ~r);
		}
		return res;
	}

}

@node
public final class RewriteNodeArg extends ENode {
	
	@dflow(out="this:in") private static class DFI {}
	
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


