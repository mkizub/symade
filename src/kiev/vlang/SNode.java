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

	@virtual typedef This  ≤ SNode;

	@dflow(out="this:in") private static class DFI {}

	public static final SNode dummyNode = new SNode();

	public SNode() {}

	public ASTNode getDummyNode() {
		return SNode.dummyNode;
	}
	
	public final void resolveDecl() {}

}

@node(name="Comment")
public final class Comment extends SNode {

	@virtual typedef This  = Comment;

    public static final AttrSlot ATTR_BEFORE = new ExtAttrSlot("comment before", true, false, TypeInfo.newTypeInfo(Comment.class,null));
    public static final AttrSlot ATTR_AFTER  = new ExtAttrSlot("comment after",  true, false, TypeInfo.newTypeInfo(Comment.class,null));

	@dflow(out="this:in") private static class DFI {}

	@att public boolean eol_form;
	@att public boolean multiline;
	@att public boolean doc_form;
	@att public boolean nl_before;
	@att public boolean nl_after;
	@att public String  text;
	
	public Comment() {}
}

