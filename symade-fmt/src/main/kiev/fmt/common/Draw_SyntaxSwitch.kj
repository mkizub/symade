package kiev.fmt.common;

import kiev.fmt.DrawSyntaxSwitch;
import kiev.fmt.Drawable;
import kiev.fmt.SyntaxManager;
import kiev.vlang.Constants;
import kiev.vlang.Language;
import kiev.vtree.INode;
import kiev.vtree.ThisIsANode;

public class Draw_SyntaxSwitch extends Draw_SyntaxElem {
	private static final long serialVersionUID = 6271831434121100981L;
	public Draw_SyntaxToken					prefix;
	public Draw_ATextSyntax					target_syntax;
	public Draw_SyntaxToken					suffix;
	
	public Draw_SyntaxSwitch(Draw_SyntaxElemDecl elem_decl) { super(elem_decl); }
	
	public Draw_SyntaxSwitch(Draw_SyntaxElemDecl elem_decl, Draw_SyntaxToken prefix, Draw_SyntaxToken suffix, Draw_ATextSyntax target_syntax) {
		super(elem_decl);
		this.prefix = prefix;
		this.suffix = suffix;
		this.target_syntax = target_syntax;
	}
	
	public Drawable makeDrawable(Formatter fmt, INode node) {
		return new DrawSyntaxSwitch(node, fmt, this);
	}
	
	private Draw_ATextSyntax getTargetSyntax(INode for_node) {
		ThisIsANode node_data = (ThisIsANode)for_node.getClass().getAnnotation(ThisIsANode.class);
		Class lng_class = node_data.lang();
		Language lng = null;
		try {
			lng = (Language)lng_class.getField(Constants.nameInstance).get(null);
		} catch (Exception e) {}
		return SyntaxManager.getDefaultEditorSyntax(lng);
	}

	public boolean check(Formatter fmt, Drawable curr_dr, INode expected_node) {
		if (expected_node != curr_dr.drnode)
			return false;
		if (!(curr_dr instanceof DrawSyntaxSwitch))
			return false;
		DrawSyntaxSwitch curr_dss = (DrawSyntaxSwitch)curr_dr;
		if (getTargetSyntax(expected_node) != target_syntax)
			return false;
		Drawable sub_dr = curr_dss.getElem();
		if (sub_dr == null)
			return true;
		fmt.pushSyntax(target_syntax);
		try {
			Draw_SyntaxElem expected_stx = target_syntax.getSyntaxElem(expected_node, fmt.getSyntaxList(), fmt.env);
			return expected_stx.check(fmt,sub_dr,expected_node);
		} finally {
			fmt.popSyntax(target_syntax);
		}
	}
}

