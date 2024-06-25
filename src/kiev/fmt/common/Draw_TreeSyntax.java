package kiev.fmt.common;

import static kiev.fmt.SpaceAction.SP_ADD;
import static kiev.fmt.SpaceAction.SP_NOP;
import static kiev.fmt.SpaceKind.SP_NEW_LINE;
import static kiev.fmt.SpaceKind.SP_SPACE;
import kiev.fmt.*;
import kiev.fmt.common.Formatter.LstStx;
import kiev.parser.Opdef;
import kiev.stdlib.Arrays;
import kiev.vlang.*;
import kiev.vtree.*;

public class Draw_TreeSyntax extends Draw_ATextSyntax {
	private static final long serialVersionUID = -6892135597834558696L;

	protected Draw_Layout loutSpNo;
	protected Draw_Layout loutNoNo;
	protected Draw_Layout loutNlNl;
	protected Draw_Layout loutNoNl;
	protected Draw_Paragraph plIndented;

	public Draw_TreeSyntax() {
		SpaceInfo siSp = new SpaceInfo("sp", SP_SPACE, 1,  4);
		SpaceInfo siNl = new SpaceInfo("nl", SP_NEW_LINE, 1,  1);
		SyntaxElemFormatDecl sefdNoNo = new SyntaxElemFormatDecl("fmt-default");
		SyntaxElemFormatDecl sefdSpNo = new SyntaxElemFormatDecl("fmt-sp-no");
		SyntaxElemFormatDecl sefdNlNl = new SyntaxElemFormatDecl("fmt-nl-nl");
		SyntaxElemFormatDecl sefdNoNl = new SyntaxElemFormatDecl("fmt-no-nl");

		sefdSpNo.addVal(sefdSpNo.getAttrSlot("spaces"), new SpaceCmd(siSp, SP_ADD, SP_NOP));
		sefdNlNl.addVal(sefdNlNl.getAttrSlot("spaces"), new SpaceCmd(siNl, SP_ADD, SP_ADD));
		sefdNoNl.addVal(sefdNoNl.getAttrSlot("spaces"), new SpaceCmd(siNl, SP_NOP, SP_ADD));

		loutSpNo = sefdSpNo.compile();
		loutNoNo = sefdNoNo.compile();
		loutNlNl = sefdNlNl.compile();
		loutNoNl = sefdNoNl.compile();

		plIndented = new Draw_Paragraph();
		//plIndented.indent_next_text_size = 2;
		//plIndented.indent_next_pixel_size = 20;
		Draw_ParIndent ind = new Draw_ParIndent();
		ind.indent_next = new Draw_Size();
		ind.indent_next.unit = SyntaxSizeUnit.PIXELS;
		ind.indent_next.gfx_size = 20;
		ind.indent_next.txt_size = 2;
		plIndented.options = new Draw_ParOption[]{ind};
	}

	public Draw_SyntaxElem getSyntaxElem(INode node, LstStx syntax_stack, Env env) {
		if (node == null)
			return super.getSyntaxElem(node, syntax_stack, env);
		String cl_name = node.getNodeTypeInfo().getId();
		Draw_SyntaxElem sed = allSyntax.get(cl_name);
		if (sed != null) {
			Draw_SyntaxElem se = sed;
			if (node instanceof ENode && se instanceof Draw_SyntaxExpr) {
				ENode e = (ENode)node;
				Opdef opd = e.getFakeOpdef(env);
				if (opd == null)
					return se;
				se = allSyntaxExprs.get(new PairOpdefClass(opd,node.getClass()));
				if (se == null) {
					se = expr(opd, (Draw_SyntaxExpr)sed);
					allSyntaxExprs.put(new PairOpdefClass(opd,node.getClass()), se);
				}
			}
			return se;
		}
		Draw_SyntaxElemDecl elem_decl = new Draw_SyntaxElemDecl();
		Draw_SyntaxTreeBranch stb = new Draw_SyntaxTreeBranch(elem_decl);
		stb.par = plIndented;
		elem_decl.elem = stb;
		elem_decl.node_type_id = cl_name;
		{
			String name = node.getClass().getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0)
				name = name.substring(idx+1);
			stb.folded = new Draw_SyntaxToken(elem_decl,name);
			stb.folded.lout = loutNlNl;
		}
		Draw_SyntaxSet ss = new Draw_SyntaxSet(elem_decl);
		stb.element = ss;
		for (AttrSlot attr : node.values()) {
			if (!attr.isAttr()) continue;
			if (attr instanceof ASpaceAttrSlot) {
				Draw_SyntaxTreeBranch lst = new Draw_SyntaxTreeBranch(elem_decl);
				lst.par = plIndented;
				lst.folded =  new Draw_SyntaxToken(elem_decl,attr.name+"∅");
				lst.folded.lout = loutNlNl;
				lst.name = attr.name;
				lst.attr_slot = attr;
				lst.element = new Draw_SyntaxNode(elem_decl);
				lst.element.lout = loutNlNl;
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, lst);
			}
			else if (attr.isChild()) {
				Draw_SyntaxTreeBranch lst = new Draw_SyntaxTreeBranch(elem_decl);
				lst.par = plIndented;
				lst.folded =  new Draw_SyntaxToken(elem_decl,attr.name);
				lst.folded.lout = loutNlNl;
				lst.name = attr.name;
				lst.attr_slot = attr;
				Draw_SyntaxSet ass = new Draw_SyntaxSet(elem_decl);
				lst.element = ass;
				lst.element.lout = loutNlNl;
				Draw_SyntaxSubAttr sa = new Draw_SyntaxSubAttr(elem_decl);
				sa.name = attr.name;
				sa.attr_slot = attr;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, sa);
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, lst);
			}
			else {
				Draw_SyntaxSet ass = new Draw_SyntaxSet(elem_decl);
				ass.lout = loutNlNl;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, new Draw_SyntaxToken(elem_decl,attr.name+": "));
				Draw_SyntaxSubAttr st = new Draw_SyntaxSubAttr(elem_decl);
				st.name = attr.name;
				st.attr_slot = attr;
				ass.elements = (Draw_SyntaxElem[])Arrays.append(ass.elements, st);
				ss.elements = (Draw_SyntaxElem[])Arrays.append(ss.elements, ass);
			}
		}
		allSyntax.put(cl_name,stb);
		return ss;
	}
}
