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
public final class ProcessRewrite extends TransfProcessor implements Constants {
	
	private ProcessRewrite() {
		super(Kiev.Ext.Rewrite);
	}
	
	public BackendProcessor getBackend(Kiev.Backend backend) {
		return RewriteBackend;
	}
	
}

@singleton
class RewriteBackend extends BackendProcessor implements Constants {
	
	private RewriteBackend() {
		super(Kiev.Backend.Generic);
	}
	
	public void preGenerate() {
		foreach (FileUnit fu; Kiev.files) {
			fu.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) { return rewrite(n); }
			});
		}
	}

	public void preGenerate(ASTNode node) {
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { return rewrite(n); }
		});
	}

	boolean rewrite(ANode:ANode node) {
		return true;
	}
	
	boolean rewrite(IFldExpr:ANode fa) {
		Field f = fa.var;
		if (f.getter instanceof RewriteNode)
			((RewriteNode)f.getter).rewrite(fa);
		return true;
	}	
}

