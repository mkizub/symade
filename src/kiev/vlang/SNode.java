package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * A node that is a syntax modifier: import, operator decl, separators, comments, etc.
 */
@node
public class SNode extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	public static final SNode dummyNode = new SNode();

	public SNode() {}

	public ASTNode getDummyNode() {
		return SNode.dummyNode;
	}
	
	public final void resolveDecl() {}
	public abstract Dumper toJavaDecl(Dumper dmp);

}

