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
public class DrawOptional extends DrawNonTerm {

	@ref Drawable dr_true;
	@ref Drawable dr_false;
	
	public DrawOptional() {}
	public DrawOptional(ASTNode node, SyntaxOptional syntax) {
		super(node, syntax);
	}

	public void init(Formatter fmt) {
		SyntaxOptional sc = (SyntaxOptional)syntax;
		if (sc.opt_true != null) {
			dr_true = sc.opt_true.makeDrawable(fmt, node);
			args.add(dr_true);
		}
		if (sc.opt_false != null) {
			dr_false = sc.opt_false.makeDrawable(fmt, node);
			args.add(dr_false);
		}
	}

	public void preFormat(DrawContext cont) {
		this.geometry.is_hidden = true;
		SyntaxOptional sc = (SyntaxOptional)syntax;
		if (sc.calculator.calc(node)) {
			if (dr_true != null) {
				this.geometry.is_hidden = false;
				dr_true.geometry.is_hidden = false;
			} else {
				this.geometry.is_hidden = true;
			}
			if (dr_false != null)
				dr_false.geometry.is_hidden = true;
		} else {
			if (dr_false != null) {
				this.geometry.is_hidden = false;
				dr_false.geometry.is_hidden = false;
			} else {
				this.geometry.is_hidden = true;
			}
			if (dr_true != null)
				dr_true.geometry.is_hidden = true;
		}
		super.preFormat(cont);
	}
}

@node
public class DrawIntChoice extends DrawNonTermSet {

	public DrawIntChoice() {}
	public DrawIntChoice(ASTNode node, SyntaxIntChoice syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		foreach (Drawable dr; args)
			dr.geometry.is_hidden = true;
		SyntaxIntChoice sc = (SyntaxIntChoice)syntax;
		int idx = ((Integer)node.getVal(sc.name)).intValue();
		if (idx >= 0 && idx < args.size())
			args[idx].geometry.is_hidden = false;
		super.preFormat(cont);
	}
}

@node
public class DrawMultipleChoice extends DrawNonTermSet {

	public DrawMultipleChoice() {}
	public DrawMultipleChoice(ASTNode node, SyntaxMultipleChoice syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont) {
		int mask = ((Integer)node.getVal(((SyntaxMultipleChoice)syntax).name)).intValue();
		for (int i=0; i < args.size(); i++)
			args[i].geometry.is_hidden = ((mask & (1<<i)) == 0);
		super.preFormat(cont);
	}
}


