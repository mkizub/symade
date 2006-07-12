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
	final SyntaxElem seSpaceCmd;
	final SyntaxElem seDrawColor;
	final SyntaxElem seDrawFont;
	final SyntaxElem seSyntaxElemDecl;
	final SyntaxElem seSyntaxKeyword;

	public SyntaxForSyntax() {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SpaceCmd[] lout_nl = new SpaceCmd[] {
				new SpaceCmd(siNl, SP_NOP, SP_ADD, 0)
			};
		SpaceCmd[] lout_pkg = new SpaceCmd[] {
				new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
			};
		// file unit
		SyntaxList fu_members = lst("members", lout_nl);
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("SpaceInfo",Env.newPackage("kiev.fmt"),0));
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawColor",Env.newPackage("kiev.fmt"),0));
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawFont",Env.newPackage("kiev.fmt"),0));
		fu_members.expected_types += new SymbolRef(0, Env.newStruct("SyntaxElemDecl",Env.newPackage("kiev.fmt"),0));
		seFileUnit = setl(lout_nl,
				opt("pkg", setl(lout_pkg, kw("namespace"), ident("pkg"))),
				fu_members
			);
		SyntaxIdentAttr id = ident("id");
		id.expected_types += new SymbolRef(0, Env.newStruct("Symbol",Env.newPackage("kiev.vlang"),0));
		seSpaceInfo = setl(lout_nl,kw("def-space"),id.ncopy(),attr("kind"),attr("text_size"),attr("pixel_size"));
		seDrawColor = setl(lout_nl,kw("def-color"),id.ncopy(),attr("rgb_color"));
		seDrawFont  = setl(lout_nl,kw("def-font"), id.ncopy(),attr("font_name"));
		SyntaxAttr scmd_si = ident("si");
		scmd_si.expected_types += new SymbolRef(0, Env.newStruct("SpaceInfo",Env.newPackage("kiev.fmt"),0));
		seSpaceCmd = set(sep("["),
			alt_enum("action_before", oper("·"), oper("+"), oper("×")),
			scmd_si,
			alt_enum("action_after", oper("·"), oper("+"), oper("×")),
			attr("from_attempt"),
			sep("]"));
		SyntaxAttr node_attr = ident("node");
		node_attr.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
		SyntaxAttr elem_attr = attr("elem");
		elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxKeyword",Env.newPackage("kiev.fmt"),0));
		seSyntaxElemDecl = setl(lout_nl,kw("def-syntax"), id.ncopy(), node_attr, elem_attr);
		SyntaxList slst = lst("spaces",node(),sep(","),new SpaceCmd[0]);
		slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
		seSyntaxKeyword = folder(
			attr("text"),
			set(
				attr("text"),
				sep("<"),
					ident("font"),
					ident("color"),
					attr("is_hidden"),
					sep("{"),slst,sep("}"),
				sep(">")
				),
			new SpaceCmd[0]
			);
	}

	public SyntaxElem getSyntaxElem(ANode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit:       return seFileUnit;
		case SpaceInfo:      return seSpaceInfo;
		case SpaceCmd:       return seSpaceCmd;
		case DrawColor:      return seDrawColor;
		case DrawFont:       return seDrawFont;
		case SyntaxElemDecl: return seSyntaxElemDecl;
		case SyntaxKeyword:  return seSyntaxKeyword;
		}
		return super.getSyntaxElem(node,hint);
	}
}

