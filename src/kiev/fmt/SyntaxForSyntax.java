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
	final SyntaxElem seImport;
	final SyntaxElem seSymbolRef;
	final SyntaxElem seSpaceInfo;
	final SyntaxElem seSpaceCmd;
	final SyntaxElem seDrawColor;
	final SyntaxElem seDrawFont;
	final SyntaxElem seSyntaxElemDecl;
	final SyntaxElem seSyntaxToken;
	final SyntaxElem seSyntaxAttr;
	final SyntaxElem seSyntaxIdent;
	final SyntaxElem seSyntaxList;
	final SyntaxElem seSyntaxSet;
	final SyntaxElem seSyntaxNode;
	final SyntaxElem seSyntaxSpace;

	public SyntaxForSyntax() {
		SpaceCmd[] lout_empty = new SpaceCmd[0];
		SpaceCmd[] lout_nl = new SpaceCmd[] {
				new SpaceCmd(siNl, SP_NOP, SP_ADD, 0)
			};
		SpaceCmd[] lout_pkg = new SpaceCmd[] {
				new SpaceCmd(siNlGrp, SP_NOP, SP_ADD, 0)
			};
		SpaceCmd[] lout_nl_x = new SpaceCmd[] {
				new SpaceCmd(siNl, SP_ADD, SP_ADD, 1)
			};
		{
			// file unit
			SyntaxList fu_members = lst("members", lout_nl);
			fu_members.expected_types += new SymbolRef(0, Env.newStruct("Import",Env.newPackage("kiev.vlang"),0));
			fu_members.expected_types += new SymbolRef(0, Env.newStruct("SpaceInfo",Env.newPackage("kiev.fmt"),0));
			fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawColor",Env.newPackage("kiev.fmt"),0));
			fu_members.expected_types += new SymbolRef(0, Env.newStruct("DrawFont",Env.newPackage("kiev.fmt"),0));
			fu_members.expected_types += new SymbolRef(0, Env.newStruct("SyntaxElemDecl",Env.newPackage("kiev.fmt"),0));
			seFileUnit = setl(lout_nl,
					opt("pkg", setl(lout_pkg, kw("namespace"), ident("pkg"))),
					fu_members
				);
			seImport = setl(lout_nl,
				kw("import"),
				ident("name"),
				opt("star",new CalcOptionTrue("star"), sep(".*"), null, lout_empty)
				);
			seSymbolRef = ident("name");
		}
		{
			// space info, cmd, color, font
			SyntaxIdentAttr id = ident("id");
			id.expected_types += new SymbolRef(0, Env.newStruct("Symbol",Env.newPackage("kiev.vlang"),0));
			seSpaceInfo = setl(lout_nl,kw("def-space"),id.ncopy(),attr("kind"),attr("text_size"),attr("pixel_size"));
			seDrawColor = setl(lout_nl,kw("def-color"),id.ncopy(),attr("rgb_color"));
			seDrawFont  = setl(lout_nl,kw("def-font"), id.ncopy(),attr("font_name"));
			SyntaxAttr scmd_si = ident("si");
			scmd_si.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
			seSpaceCmd = set(sep("["),
				alt_enum("action_before", oper("·"), oper("+"), oper("×")),
				scmd_si,
				alt_enum("action_after", oper("·"), oper("+"), oper("×")),
				attr("from_attempt"),
				sep("]"));
		}
		{
			// syntax element
			SyntaxIdentAttr id = ident("id");
			id.expected_types += new SymbolRef(0, Env.newStruct("Symbol",Env.newPackage("kiev.vlang"),0));
			SyntaxAttr node_attr = ident("node");
			node_attr.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
			SyntaxAttr elem_attr = attr("elem");
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxToken",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxAttr",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxIdentAttr",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxList",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSet",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxNode",Env.newPackage("kiev.fmt"),0));
			elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSpace",Env.newPackage("kiev.fmt"),0));
			seSyntaxElemDecl = setl(lout_nl,kw("def-syntax"), id.ncopy(), node_attr, elem_attr);
		}
		{
			// token
			SyntaxList slst = lst("spaces",node(),sep(","),new SpaceCmd[0]);
			slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
			seSyntaxToken = folder(
				set(oper("'"), attr("text"), oper("'")),
				set(
					sep("<"),
					kw("token:"),
					set(oper("'"), attr("text"), oper("'")),
					ident("font"),
					ident("color"),
					attr("is_hidden"),
					sep("{"),slst,sep("}"),
					sep(">")
					),
				new SpaceCmd[0]
				);
		}
		{
			// attr
			SyntaxElem expected = lst("expected_types", lout_empty);
			expected.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
			SyntaxList slst = lst("spaces",node(),sep(","),new SpaceCmd[0]);
			slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
			seSyntaxAttr = folder(
				set(oper("\""), attr("name"), oper("\"")),
				set(
					sep("<"),
					kw("attr:"),
					set(oper("\""), attr("name"), oper("\"")),
					kw("types:"),
					sep("["),expected,sep("]"),
					sep("{"),slst,sep("}"),
					sep(">")
					),
				new SpaceCmd[0]
				);
		}
		{
			// ident
			SyntaxElem expected = lst("expected_types", lout_empty);
			expected.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
			SyntaxList slst = lst("spaces",node(),sep(","),new SpaceCmd[0]);
			slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
			seSyntaxIdent = folder(
				set(oper("\""), attr("name"), oper("\"")),
				set(
					sep("<"),
					kw("ident:"),
					set(oper("\""), attr("name"), oper("\"")),
					kw("types:"),
					sep("["),expected,sep("]"),
					sep("{"),slst,sep("}"),
					sep(">")
					),
				new SpaceCmd[0]
				);
		}
		{
			// list
			SyntaxAttr lst_elem_attr = attr("element", lout_nl_x);
			lst_elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxToken",Env.newPackage("kiev.fmt"),0));
			lst_elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSet",Env.newPackage("kiev.fmt"),0));
			lst_elem_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxNode",Env.newPackage("kiev.fmt"),0));
			SyntaxAttr lst_sep_attr = attr("separator", lout_nl_x);
			lst_sep_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxToken",Env.newPackage("kiev.fmt"),0));
			lst_sep_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSet",Env.newPackage("kiev.fmt"),0));
			lst_sep_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSpace",Env.newPackage("kiev.fmt"),0));
			SyntaxAttr lst_empty_attr = attr("empty", lout_nl_x);
			lst_empty_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxToken",Env.newPackage("kiev.fmt"),0));
			SyntaxElem expected = lst("expected_types", node(lout_nl_x), null, lout_nl_x);
			expected.expected_types += new SymbolRef(0, Env.newStruct("SymbolRef",Env.newPackage("kiev.vlang"),0));
			SyntaxList slst = lst("spaces", node(lout_nl_x), null, lout_nl_x);
			slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
			seSyntaxList = folder(
				set(oper("["), attr("name"), oper("]")),
				set(
					sep("<"),
					kw("list:"),
					set(oper("["), attr("name"), oper("]")),
					kw("elem="),
					lst_elem_attr,
					kw("sep="),
					lst_sep_attr,
					kw("empty="),
					lst_empty_attr,
					kw("types:"),
					sep("["),expected,sep("]"),
					sep("{"),slst,sep("}"),
					sep(">")
					),
				new SpaceCmd[0]
				);
		}
		{
			// syntax set
			SyntaxList set_elems_attr = lst("elements", node(lout_nl_x), null, lout_nl_x);
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxToken",Env.newPackage("kiev.fmt"),0));
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxAttr",Env.newPackage("kiev.fmt"),0));
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxIdentAttr",Env.newPackage("kiev.fmt"),0));
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxList",Env.newPackage("kiev.fmt"),0));
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSet",Env.newPackage("kiev.fmt"),0));
			set_elems_attr.expected_types += new SymbolRef(0, Env.newStruct("SyntaxSpace",Env.newPackage("kiev.fmt"),0));
			SyntaxList slst = lst("spaces", node(lout_nl_x), null, lout_nl_x);
			slst.expected_types += new SymbolRef(0, Env.newStruct("SpaceCmd",Env.newPackage("kiev.fmt"),0));
			seSyntaxSet = folder(
				set(oper("{"), set_elems_attr.ncopy(), oper("}")),
				set(
					sep("<"),
					kw("set:"),
					set(oper("{"), set_elems_attr.ncopy(), oper("}")),
					sep("{"),slst,sep("}"),
					sep(">")
					),
				new SpaceCmd[0]
				);
		}
		{
			// syntax node, space
			seSyntaxNode = kw("<node>");
			seSyntaxSpace = kw("<sp>");
		}
	}

	public SyntaxElem getSyntaxElem(ANode node, FormatInfoHint hint) {
		switch (node) {
		case FileUnit:       return seFileUnit;
		case Import:         return seImport;
		case SymbolRef:      return seSymbolRef;
		case SpaceInfo:      return seSpaceInfo;
		case SpaceCmd:       return seSpaceCmd;
		case DrawColor:      return seDrawColor;
		case DrawFont:       return seDrawFont;
		case SyntaxElemDecl: return seSyntaxElemDecl;
		case SyntaxToken:    return seSyntaxToken;
		case SyntaxAttr:     return seSyntaxAttr;
		case SyntaxIdentAttr:return seSyntaxIdent;
		case SyntaxList:     return seSyntaxList;
		case SyntaxSet:      return seSyntaxSet;
		case SyntaxNode:     return seSyntaxNode;
		case SyntaxSpace:    return seSyntaxSpace;
		}
		return super.getSyntaxElem(node,hint);
	}
}

