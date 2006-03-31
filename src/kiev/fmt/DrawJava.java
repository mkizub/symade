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

