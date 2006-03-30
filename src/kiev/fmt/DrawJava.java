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
public class DrawJavaPackage extends DrawNonTermSet {
	public DrawJavaPackage() {}
	public DrawJavaPackage(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	public void preFormat(DrawContext cont) {
		FileUnit fu = (FileUnit)node;
		if (fu.pkg == null || fu.pkg.name == null || fu.pkg.getType() ≈ Env.root.ctype)
			this.geometry.is_hidden = true;
		else
			this.geometry.is_hidden = false;
		super.preFormat(cont);
	}
}

@node
public class DrawJavaImport extends DrawNonTermSet {
	@ref Drawable star;
	
	public DrawJavaImport() {}
	public DrawJavaImport(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	public void init(Formatter fmt) {
		super.init(fmt);
		foreach (Drawable dr; args; dr.syntax.ID == ".*") {
			star = dr;
			break;
		}
	}

	public void preFormat(DrawContext cont) {
		Import imp = (Import)node;
		this.star.geometry.is_hidden = !imp.star;
		super.preFormat(cont);
	}
}

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

