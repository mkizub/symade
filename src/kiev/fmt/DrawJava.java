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
	public DrawJavaExpr(ASTNode node, SyntaxJavaExpr syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxJavaExpr se = (SyntaxJavaExpr)this.syntax;
		args.append(se.l_paren.makeDrawable(fmt, node));
		args.append(fmt.getDrawable(node, se.hint));
		args.append(se.r_paren.makeDrawable(fmt, node));
	}


	public void preFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		SyntaxJavaExpr se = (SyntaxJavaExpr)this.syntax;
		args[0].geometry.is_hidden = true;
		args[2].geometry.is_hidden = true;
		if ((node instanceof ENode) && (((ENode)node).isPrimaryExpr() || ((ENode)node).getPriority() < se.priority)) {
			args[0].geometry.is_hidden = false;
			args[2].geometry.is_hidden = false;
		}
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(cont);
		}
	}
}

@node
public class DrawJavaAccess extends DrawTerm {

	private String text;
	
	public DrawJavaAccess() {}
	public DrawJavaAccess(ASTNode node, SyntaxJavaAccess syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		if (node instanceof DNode && node instanceof Accessable) {
			if (Access.getFlags((DNode)node) == 0x0F)
				this.geometry.is_hidden = true;
			else
				this.geometry.is_hidden = false;
		}
		else
			this.geometry.is_hidden = true;
		if (this.isUnvisible())
			return;
		DNode dn = (DNode)node;
		if (dn.isPublic())
			text = "public"+mkString(0xFF);
		else if (dn.isProtected())
			text = "protected"+mkString(0x3F);
		else if (dn.isPrivate())
			text = "private"+mkString(0x03);
		else
			text = "@access"+mkString(0x0F);
		super.preFormat(cont);
	}

	private String mkString(int expected) {
		if (Access.getFlags((DNode)node) == expected)
			return "";
		Access acc = ((Accessable)node).acc;
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

	public String getText() { return this.text; }
}

@node
public class DrawJavaType extends DrawTerm {
	
	private String text;
	
	public DrawJavaType() {}
	public DrawJavaType(ASTNode node, SyntaxJavaType syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		SyntaxJavaType stx = (SyntaxJavaType)syntax;
		TypeRef tr = (TypeRef)node;
		if (stx.hint == null || stx.hint.text == null) {
			text = String.valueOf(tr);
		}
		else if ("no-args".equals(stx.hint.text) || "call-accessor".equals(stx.hint.text)) {
			Struct s = tr.getStruct();
			if (s != null)
				text = s.qname.toString();
			else
				text = String.valueOf(tr);
		}
		else {
			text = String.valueOf(tr);
		}

		if (node.parent instanceof FormPar) {
			FormPar fp = (FormPar)node.parent;
			if (fp.kind == FormPar.PARAM_VARARGS && tr.getType().isArray() && text.endsWith("[]")) {
				text = text.substring(0,text.length()-2) + "...";
			}
		}

		super.preFormat(cont);
	}	
	
	public String getText() { return this.text; }
}

@node
public class DrawJavaEnumAlias extends DrawTerm {
	
	private String text;
	
	public DrawJavaEnumAlias() {}
	public DrawJavaEnumAlias(ASTNode node, SyntaxJavaEnumAlias syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		Field f = (Field)node;
		KString str = f.id.sname;
		if (f.id.aliases != null) {
			str = f.id.aliases[0];
			str = str.substr(1,str.length()-1);
		}
		text = "\""+str+"\"";
		super.preFormat(cont);
	}	
	
	public String getText() { return this.text; }
}

@node
public class DrawJavaPackedField extends DrawTerm {

	private String text;
	
	public DrawJavaPackedField() {}
	public DrawJavaPackedField(ASTNode node, SyntaxJavaPackedField syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		MetaPacked mp = ((Field)node).getMetaPacked();
		text = "@packed("+mp.size;
		if (mp.fld != null)
			text += ","+mp.fld+","+mp.offset;
		text += ")";
		super.preFormat(cont);
	}

	public String getText() { return this.text; }
}

@node
public class DrawJavaComment extends DrawTerm {

	public String[] lines;
	
	public DrawJavaComment() {}
	public DrawJavaComment(ASTNode node, SyntaxJavaComment syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		Comment c = (Comment)node;
		String text = c.text;
		int nl = 0;
		if (text == null) text = "";
		if (c.eol_form) {
			if (c.multiline) {
				this.lines = text.split("\n");
				for (int i=0; i < lines.length; i++)
					lines[i] = "// "+lines[i];
			} else {
				this.lines = new String[]{ "// "+text };
			}
		}
		else if (c.doc_form) {
			if (c.multiline) {
				this.lines = text.split("\n");
				for (int i=0; i < lines.length; i++)
					lines[i] = " * "+lines[i];
				this.lines = (String[])Arrays.insert(this.lines, "/**", 0);
				this.lines = (String[])Arrays.append(this.lines, "*/");
			} else {
				this.lines = new String[]{ "/** "+text + " */" };
			}
		}
		else {
			if (c.multiline) {
				this.lines = text.split("\n");
				for (int i=0; i < lines.length; i++) {
					if (i == 0)
						lines[i] = "/* "+lines[i];
					else
						lines[i] = " * "+lines[i];
				}
				lines[lines.length-1] = lines[lines.length-1] + " */";
			} else {
				this.lines = new String[]{ "/* "+text + " */" };
			}
		}
		super.preFormat(cont);
	}

	public String getText() { return ""; }
}


