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


@node
public abstract class RewriteNode extends SNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNode;
	@virtual typedef VView = VRewriteNode;

	@att public NArr<RewriteNode>			args;

	@nodeview
	public static view VRewriteNode of RewriteNode extends VSNode {
		public:ro NArr<RewriteNode>			args;
	}

	public RewriteNode() {}
	public RewriteNode(RewriteNode[] args) {
		if (args != null)
			this.args.addAll(args);
	}

	public void rewrite(ASTNode self) {
		ASTNode rn = getNode(self);
		self.replaceWithNode(rn);
		throw ReWalkNodeException.instance;
	}

	public abstract ASTNode getNode(ASTNode self);
}

@node
public final class RewriteNodeSelf extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeSelf;
	@virtual typedef VView = VRewriteNodeSelf;

	@nodeview
	public static final view VRewriteNodeSelf of RewriteNodeSelf extends VRewriteNode {
	}

	public RewriteNodeSelf() {}

	public ASTNode getNode(ASTNode self) {
		return self;
	}
}

@node
public final class RewriteNodeCall extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeCall;
	@virtual typedef VView = VRewriteNodeCall;

	@nodeview
	public static final view VRewriteNodeCall of RewriteNodeCall extends VRewriteNode {
	}

	public RewriteNodeCall() {}
	public RewriteNodeCall(RewriteNode[] args) {
		super(args);
	}

	public ASTNode getNode(ASTNode self) {
		throw new CompilerException(self, "Cannot rewrite #Call");
	}
}

@node
public final class RewriteNodeArrayLength extends RewriteNode {
	
	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  = RewriteNodeArrayLength;
	@virtual typedef VView = VRewriteNodeArrayLength;

	@nodeview
	public static final view VRewriteNodeArrayLength of RewriteNodeArrayLength extends VRewriteNode {
	}

	public RewriteNodeArrayLength() {}
	public RewriteNodeArrayLength(RewriteNode[] args) {
		super(args);
	}

	public ASTNode getNode(ASTNode self) {
		ASTNode[] args = new ASTNode[this.args.length];
		for (int i=0; i < args.length; i++)
			args[i] = this.args[i].getNode(self);
		ASTNode res = null;
		if (args.length == 1 && args[0] instanceof IFldExpr) {
			IFldExpr arg = (IFldExpr)args[0];
			res = new ArrayLengthExpr(arg.pos, ~arg.obj, ~arg.ident);
		}
		if (res == null)
			throw new CompilerException(self, "Cannot rewrite #ArrayLength");
		return res;
	}
}


