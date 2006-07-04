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
	final SyntaxElem seDrawColor;
	final SyntaxElem seDrawFont;

	public SyntaxForSyntax() {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SpaceCmd[] lout_nl = new SpaceCmd[] {
				new SpaceCmd(siNl,SP_ADD_AFTER,0)
			};
		SpaceCmd[] lout_pkg = new SpaceCmd[] {
				new SpaceCmd(siNlGrp, SP_ADD_AFTER, 0)
			};
		// file unit
		SyntaxList fu_members = lst("members", lout_nl);
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("SpaceInfo",Env.newPackage("kiev.fmt"),0));
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawColor",Env.newPackage("kiev.fmt"),0));
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawFont",Env.newPackage("kiev.fmt"),0));
		seFileUnit = setl(lout_nl,
				opt("pkg", setl(lout_pkg, kw("namespace"), ident("pkg"))),
				fu_members
			);
		SyntaxIdentAttr id = ident("id");
		id.expected_types += new SymbolRef(0, Env.newStruct("Symbol",Env.newPackage("kiev.vlang"),0));
		seSpaceInfo = setl(lout_nl,kw("def-space"),id.ncopy(),attr("kind"),attr("text_size"),attr("pixel_size"));
		seDrawColor = setl(lout_nl,kw("def-color"),id.ncopy(),attr("rgb_color"));
		seDrawFont  = setl(lout_nl,kw("def-font"), id.ncopy(),attr("font_name"));
	}

	public SyntaxElem getSyntaxElem(ASTNode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit:  return seFileUnit;
		case SpaceInfo: return seSpaceInfo;
		case DrawColor: return seDrawColor;
		case DrawFont:  return seDrawFont;
		}
		return super.getSyntaxElem(node,hint);
	}
}

