package kiev.transf;

import kiev.Kiev;
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

@singleton
public class RewriteME_PreGenerate extends BackendProcessor {
	private RewriteME_PreGenerate() { super(Kiev.Backend.Generic); }
	public String getDescr() { "Rewrite rules" }

	public void process(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { return rewrite(n); }
		});
	}

	boolean rewrite(ANode:ANode node) {
		return true;
	}
	
	boolean rewrite(DNode:ANode dn) {
		if (dn.isMacro())
			return false;
		return true;
	}	

	boolean rewrite(IFldExpr:ANode fa) {
		Field f = fa.var;
		if (f.isMacro() && !f.isNative()) {
			if (f.init != null)
				doRewrite(f.init, fa, ASTNode.emptyArray);
		}
		return true;
	}

	boolean rewrite(CallExpr:ANode ce) {
		Method m = ce.func;
		if (m.isMacro() && !m.isNative()) {
			if (m.body != null) {
				doRewrite(ce, ce, ASTNode.emptyArray);
			}
		}
		return true;
	}

	private void doRewrite(ENode rewriter, ASTNode self, ASTNode[] args) {
		ASTNode rn = (ASTNode)rewriter.doRewrite(new RewriteContext(self, args));
		self.replaceWithNode(rn);
		Kiev.runProcessorsOn(rn);
		throw new ReWalkNodeException(rn);
	}
}

