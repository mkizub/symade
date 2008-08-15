/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.fmt;

import syntax kiev.Syntax;


@ThisIsANode(copyable=false)
public class DrawJavaAccessExpr extends Drawable {
	
	@nodeAttr public Drawable		accessor;
	@nodeAttr public DrawTerm		separator;
	@nodeAttr public Drawable		field;
	
	public DrawJavaAccessExpr(ANode node, Draw_SyntaxJavaAccessExpr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public Drawable getNextChild(Drawable dr) {
		if (dr == accessor)
			return separator;
		if (dr == separator)
			return field;
		return null;
	}
	public Drawable getPrevChild(Drawable dr) {
		if (dr == field)
			return separator;
		if (dr == separator)
			return accessor;
		return null;
	}
	public Drawable[] getChildren() {
		return new Drawable[]{accessor,separator,field};
	}

	public void preFormat(DrawContext cont) {
		Draw_SyntaxJavaAccessExpr st = (Draw_SyntaxJavaAccessExpr)this.syntax;
		ANode node = this.drnode;
		if (accessor == null)
			accessor = st.obj_elem.makeDrawable(cont.fmt, node, text_syntax);
		if (separator == null)
			separator = (DrawTerm)st.separator.makeDrawable(cont.fmt, node, text_syntax);
		if (field == null)
			field = st.fld_elem.makeDrawable(cont.fmt, node, text_syntax);
		
		accessor.preFormat(cont,st.obj_elem,node);
		separator.preFormat(cont,st.separator,node);
		field.preFormat(cont,st.fld_elem,node);
		
		if (!cont.fmt.getShowAutoGenerated()) {
			if (node instanceof ASTNode && ((ASTNode)node).isAutoGenerated()) {
				separator.hidden_as_auto_generated = true;
				DrawTerm last = field.getLastLeaf();
				for (DrawTerm dr=field.getFirstLeaf(); dr != null; dr = dr.getNextLeaf()) {
					dr.hidden_as_auto_generated = true;
					if (dr == last)
						break;
				}
			}
			else if (accessor.getFirstLeaf() == null) {
				separator.hidden_as_auto_generated = true;
			}
		} else {
			separator.hidden_as_auto_generated = false;
			DrawTerm last = field.getLastLeaf();
			for (DrawTerm dr=field.getFirstLeaf(); dr != null; dr = dr.getNextLeaf()) {
				dr.hidden_as_auto_generated = false;
				if (dr == last)
					break;
			}
		}
	}
}

@ThisIsANode(copyable=false)
public class DrawJavaAccess extends DrawTerm {

	public DrawJavaAccess(ANode node, Draw_SyntaxJavaAccess syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		return buildText();
	}

	public String buildText() {
		MetaAccess acc = (MetaAccess)drnode;
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
		MetaAccess acc = (MetaAccess)drnode;
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

@ThisIsANode(copyable=false)
public class DrawJavaPackedField extends DrawTerm {

	public DrawJavaPackedField(ANode node, Draw_SyntaxJavaPackedField syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	protected Object makeTermObj(Formatter fmt) {
		MetaPacked mp = (MetaPacked)drnode;
		String text = "@packed("+mp.size;
		if (mp.fld != null)
			text += ","+mp.fld+","+mp.offset;
		text += ")";
		return text;
	}
}

@ThisIsANode(copyable=false)
public final class JavaComment extends ANode {
	@nodeData public String text;

	public JavaComment(String text) {
		this.text = text;
	}
}

@ThisIsANode(copyable=false)
public class DrawJavaComment extends DrawNonTerm {

	public String old_text;
	
	public DrawJavaComment(ANode node, Draw_SyntaxJavaComment syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxJavaComment se = (Draw_SyntaxJavaComment)this.syntax;
		Draw_SyntaxJavaCommentTemplate st = (Draw_SyntaxJavaCommentTemplate)se.template;
		ANode node = this.drnode;
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
	private void append(DrawContext cont, Draw_SyntaxElem se, ANode node) {
		Drawable dr = se.makeDrawable(cont.fmt, node, text_syntax);
		args.append(dr);
		dr.preFormat(cont, se, node);
	}
}

