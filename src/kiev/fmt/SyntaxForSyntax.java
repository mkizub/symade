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

	final SyntaxElem seFileUnit;
	final SyntaxElem seSpaceInfo;

	public SyntaxForSyntax() {
		DrawLayout lout_empty = new DrawLayout();
		DrawLayout lout_nl = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siNl,SP_ADD_AFTER,0)
			});
		DrawLayout lout_pkg = new DrawLayout(new SpaceCmd[]{
				new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
			});
		// file unit
		seFileUnit = setl(lout_nl.ncopy(),
				opt("pkg", setl(lout_pkg, kw("namespace"), ident("pkg"))),
				lst("members", lout_nl.ncopy())
			);
		seSpaceInfo = setl(lout_nl.ncopy(),kw("def-space"),ident("id"),attr("kind"),attr("text_size"),attr("pixel_size"));
	}

	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit:  return seFileUnit;
		case SpaceInfo: return seSpaceInfo;
		}
		return super.getSyntaxElem(node,hint);
	}
}

