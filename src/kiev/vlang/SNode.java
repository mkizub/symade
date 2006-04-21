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

	@virtual typedef This  = SNode;
	@virtual typedef VView = VSNode;

	@dflow(out="this:in") private static class DFI {}

	public static final SNode dummyNode = new SNode();

	@nodeview
	public static view VSNode of SNode extends NodeView {
	}
	
	public SNode() {}

	public ASTNode getDummyNode() {
		return SNode.dummyNode;
	}
	
	public final void resolveDecl() {}
	public Dumper toJavaDecl(Dumper dmp) { return dmp; }

}

@node
public class Comment extends SNode {

	@virtual typedef This  = Comment;

	@dflow(out="this:in") private static class DFI {}

	@att public boolean eol_form;
	@att public boolean multiline;
	@att public boolean doc_form;
	@att public String  text;
	
	public Comment() {}

	public Dumper toJavaDecl(Dumper dmp) {
		String text = this.text;
		if (text == null) text = "";
		if (eol_form) {
			if (multiline) {
				String[] lines = text.split("\n");
				foreach (String s; lines)
					dmp.append("// ").append(s).newLine();
			} else {
				dmp.append("// ").append(text).newLine();
			}
		}
		else if (doc_form) {
			if (multiline) {
				dmp.newLine().append("/**").newLine();
				String[] lines = text.split("\n");
				foreach (String s; lines)
					dmp.append(" * ").append(s).newLine();
				dmp.append(" */").newLine();
			} else {
				dmp.append("/** ").append(text).append(" */").newLine();
			}
		}
		else {
			if (multiline) {
				dmp.newLine().append("/*").newLine();
				String[] lines = text.split("\n");
				foreach (String s; lines)
					dmp.append(" * ").append(s).newLine();
				dmp.append(" */").newLine();
			} else {
				dmp.append(" /* ").append(text).append(" */ ");
			}
		}
		return dmp;
	}

}

