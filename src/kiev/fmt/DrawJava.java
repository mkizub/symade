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
		args.append(fmt.getDrawable(node, null));
		args.append(se.r_paren.makeDrawable(fmt, node));
	}


	public void preFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		int old_priority = cont.expr_priority;
		SyntaxJavaExpr se = (SyntaxJavaExpr)this.syntax;
		int priority = se.priority;
		args[0].geometry.is_hidden = true;
		args[2].geometry.is_hidden = true;
		if ((node instanceof ENode) && (((ENode)node).isPrimaryExpr() || ((ENode)node).getPriority() < cont.expr_priority)) {
			args[0].geometry.is_hidden = false;
			args[2].geometry.is_hidden = false;
			priority = 0;
		}
		cont.expr_priority = priority;
		try {
			for (int i=0; i < args.length; i++) {
				Drawable dr = args[i];
				dr.preFormat(cont);
			}
		} finally { cont.expr_priority = old_priority; }
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


