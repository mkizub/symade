package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


@node
public class DrawJavaExpr extends DrawNonTermSet {
	
	public DrawJavaExpr() {}
	public DrawJavaExpr(ANode node, SyntaxJavaExpr syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
		}
		if (this.isUnvisible())
			return;
		SyntaxJavaExpr se = (SyntaxJavaExpr)this.syntax;
		SyntaxJavaExprTemplate st = (SyntaxJavaExprTemplate)se.template.dnode;
		boolean no_paren = true;
		if (node instanceof ENode) {
			if (((ENode)node).isPrimaryExpr())
				no_paren = false;
			else if (((ENode)node).getPriority() < se.priority)
				no_paren = false;
		}
		if (args.length == 0)
			args.append(st.elem.makeDrawable(cont.fmt, node));
		if (args.length == 1) {
			if (!no_paren) {
				args.insert(0,st.l_paren.makeDrawable(cont.fmt, node));
				args.insert(2,st.r_paren.makeDrawable(cont.fmt, node));
			}
		} else {
			assert(args.length == 3);
			if (no_paren) {
				args.del(2);
				args.del(0);
			}
		}
		if (no_paren) {
			assert(args.length == 1);
			args[0].preFormat(cont,st.elem,node);
		} else {
			assert(args.length == 3);
			args[0].preFormat(cont,st.l_paren,node);
			args[1].preFormat(cont,st.elem,node);
			args[2].preFormat(cont,st.r_paren,node);
		}
	}
}

@node
public class DrawJavaAccess extends DrawTerm {

	public DrawJavaAccess() {}
	public DrawJavaAccess(ANode node, SyntaxJavaAccess syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (this.isUnvisible())
			return;
		super.preFormat(cont, expected_stx, expected_node);
	}
	
	String makeText(Formatter fmt) {
		MetaAccess acc = (MetaAccess)node;
		String text;
		if (acc.simple == "public")
			text = "public"+mkString(0xFF);
		else if (acc.simple == "protected")
			text = "protected"+mkString(0x3F);
		else if (acc.simple == "private")
			text = "private"+mkString(0x03);
		else
			text = "@access"+mkString(0x0F);
		return text;
	}

	private String mkString(int expected) {
		MetaAccess acc = (MetaAccess)node;
		if (acc.flags == -1 || acc.flags == expected)
			return "";
		StringBuffer sb = new StringBuffer(":");

		if( acc.r_public && acc.w_public ) sb.append("rw,");
		else if( acc.r_public ) sb.append("ro,");
		else if( acc.w_public ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_protected && acc.w_protected ) sb.append("rw,");
		else if( acc.r_protected ) sb.append("ro,");
		else if( acc.w_protected ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_default && acc.w_default ) sb.append("rw,");
		else if( acc.r_default ) sb.append("ro,");
		else if( acc.w_default ) sb.append("wo,");
		else sb.append("no,");

		if( acc.r_private && acc.w_private ) sb.append("rw");
		else if( acc.r_private ) sb.append("ro");
		else if( acc.w_private ) sb.append("wo");
		else sb.append("no");

		return sb.toString();
	}

}
/*
@node
public class DrawJavaEnumAlias extends DrawTerm {
	
	private String text;
	
	public DrawJavaEnumAlias() {}
	public DrawJavaEnumAlias(ANode node, SyntaxJavaEnumAlias syntax) {
		super(node, syntax);
	}

	String makeText(Formatter fmt) {
		Field f = (Field)node;
		String text = f.id.sname;
		if (f.id.aliases != null) {
			text = f.id.aliases[0];
			text = text.substring(1,text.length()-1);
		}
		return "\""+text+"\"";
	}	
}
*/
@node
public class DrawJavaPackedField extends DrawTerm {

	private String text;
	
	public DrawJavaPackedField() {}
	public DrawJavaPackedField(ANode node, SyntaxJavaPackedField syntax) {
		super(node, syntax);
	}

	String makeText(Formatter fmt) {
		MetaPacked mp = (MetaPacked)node;
		String text = "@packed("+mp.size;
		if (mp.fld != null)
			text += ","+mp.fld+","+mp.offset;
		text += ")";
		return text;
	}
}

@node
public final class JavaComment extends ANode {
	@ref public String text;
	public JavaComment() {
		this.text = "";
	}
	public JavaComment(String text) {
		this.text = text;
	}
}

@node
public class DrawJavaComment extends DrawNonTermSet {

	public String old_text;
	
	public DrawJavaComment() {}
	public DrawJavaComment(ANode node, SyntaxJavaComment syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
		}
		if (this.isUnvisible())
			return;
		SyntaxJavaComment se = (SyntaxJavaComment)this.syntax;
		SyntaxJavaCommentTemplate st = (SyntaxJavaCommentTemplate)se.template.dnode;
		Comment c = (Comment)node;
		String text = c.text;
		if (text == null) text = "";
		if (text == old_text)
			return;
		old_text = text;
		
		args.delAll();
				
		if (c.eol_form) {
			if (c.multiline) {
				String[] lines = text.split("\n");
				append(cont, st.newline, node);
				for (int i=0; i < lines.length; i++) {
					append(cont, st.lin_beg, node);
					append(cont, st.elem, new JavaComment(lines[i]));
				}
			} else {
				if (c.nl_before)
					append(cont, st.newline, node);
				append(cont, st.lin_beg, node);
				append(cont, st.elem, new JavaComment(text));
			}
		} else {
			if (c.nl_before)
				append(cont, st.newline, node);
			if (c.doc_form)
				append(cont, st.doc_beg, node);
			else
				append(cont, st.cmt_beg, node);
			if (c.multiline) {
				String[] lines = text.split("\n");
				for (int i=0; i < lines.length; i++)
					append(cont, st.elem, new JavaComment(lines[i]));
			} else {
				append(cont, st.elem, new JavaComment(text));
			}
			append(cont, st.cmt_end, node);
			if (c.nl_after)
				append(cont, st.newline, node);
		}
	}
	private void append(DrawContext cont, SyntaxElem se, ANode node) {
		Drawable dr = se.makeDrawable(cont.fmt, node);
		args.append(dr);
		dr.preFormat(cont, se, node);
	}
}

