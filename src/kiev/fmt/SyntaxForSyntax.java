package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.Operator;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public class SyntaxForSyntax extends TextSyntax {

	private Hashtable<String,SyntaxElemDecl> allSyntax = new Hashtable<String,SyntaxElemDecl>();

	public SyntaxForSyntax() {}
	public void loadFrom(FileUnit fu) {
		if (fu != null) {
			foreach(SyntaxElemDecl sed; fu.members; sed.elem != null) {
				if !(sed.node.symbol instanceof Struct)
					continue;
				if (sed.elem == null)
					continue;
				Struct s = (Struct)sed.node.symbol;
				if !(s.isCompilerNode())
					continue;
				allSyntax.put(s.qname(), sed);
			}
		}
	}

	public SyntaxElem getSyntaxElem(ANode node, FormatInfoHint hint) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null)
				return sed.elem;
		}
		return super.getSyntaxElem(node,hint);
	}
}

