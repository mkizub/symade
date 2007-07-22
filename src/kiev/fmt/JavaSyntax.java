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

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import syntax kiev.Syntax;


@node(lang=SyntaxLang)
public class SyntaxJavaCommentTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxJavaCommentTemplate;

	@att public SyntaxElem		newline;
	@att public SyntaxElem		lin_beg;
	@att public SyntaxElem		doc_beg;
	@att public SyntaxElem		cmt_beg;
	@att public SyntaxElem		cmt_end;

}

@node(lang=SyntaxLang)
public class SyntaxJavaAccessExpr extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccessExpr;

	@att public SyntaxElem			obj_elem;
	@att public SyntaxToken			separator;
	@att public SyntaxElem			fld_elem;

	public SyntaxJavaAccessExpr() {}

	public Drawable makeDrawable(Formatter fmt, ANode node, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaAccessExpr(node, this, attr_syntax, text_syntax);
		return dr;
	}
}

@node(lang=SyntaxLang)
public class SyntaxJavaAccess extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaAccess;

	public SyntaxJavaAccess() {}

	public Drawable makeDrawable(Formatter fmt, ANode node, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaAccess(node, this, attr_syntax, text_syntax);
		return dr;
	}
}

@node(lang=SyntaxLang)
public class SyntaxJavaPackedField extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaPackedField;

	public SyntaxJavaPackedField() {}

	public Drawable makeDrawable(Formatter fmt, ANode node, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaPackedField(node, this, attr_syntax, text_syntax);
		return dr;
	}
}


@node(lang=SyntaxLang)
public class SyntaxJavaComment extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaComment;

	@att public SymbolRef<SyntaxJavaCommentTemplate>	template;

	public SyntaxJavaComment() {
		this.template = new SymbolRef<SyntaxJavaCommentTemplate>();
	}

	public Drawable makeDrawable(Formatter fmt, ANode node, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaComment(node, this, attr_syntax, text_syntax);
		return dr;
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxJavaCommentTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved java expression template "+template);
			else if (template.symbol != d)
				template.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxJavaCommentTemplate> vect = new Vector<SyntaxJavaCommentTemplate>();
			SyntaxJavaCommentTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}
}

@node(lang=SyntaxLang)
public class SyntaxJavaConstructorName extends SyntaxElem {
	@virtual typedef This  = SyntaxJavaConstructorName;

	public SyntaxJavaConstructorName() {}

	public Drawable makeDrawable(Formatter fmt, ANode node, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		Drawable dr = new DrawJavaConstructorName(node, this, attr_syntax, text_syntax);
		return dr;
	}
}

@node(lang=SyntaxLang)
public class KievTextSyntax extends ATextSyntax {
	@virtual typedef This  = KievTextSyntax;

	public KievTextSyntax() {}

	public SyntaxElem getSyntaxElem(ANode node) {
		if (node != null) {
			String cl_name = node.getClass().getName();
			SyntaxElemDecl sed = allSyntax.get(cl_name);
			if (sed != null) {
				SyntaxElem se = sed.elem;
				if (node instanceof ENode && se instanceof SyntaxExpr) {
					ENode e = (ENode)node;
					Operator op = e.getOp();
					if (op == null)
						return se;
					se = allSyntaxExprs.get(new Pair<Operator,Class>(op,node.getClass()));
					if (se == null) {
						se = expr(op, (SyntaxExpr)sed.elem);
						allSyntaxExprs.put(new Pair<Operator,Class>(op,node.getClass()), se);
					}
					return se;
				}
				return se;
			}
		}
		return super.getSyntaxElem(node);
	}
}

