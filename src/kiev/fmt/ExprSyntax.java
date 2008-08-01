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

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;


@ThisIsANode(lang=SyntaxLang)
public class SyntaxExprTemplate extends ASyntaxElemDecl {
	@nodeAttr public SyntaxToken		l_paren;
	@nodeAttr public SyntaxToken		bad_op;
	@nodeAttr public SyntaxToken		r_paren;
	@nodeAttr public SyntaxToken∅		operators;

	public SyntaxExprTemplate() {
		super(new SyntaxNode());
		this.l_paren = new SyntaxToken("(");
		this.bad_op = new SyntaxToken("\u25d9");
		this.r_paren = new SyntaxToken(")");
	}

	public Draw_SyntaxExprTemplate getCompiled() {
		Draw_SyntaxExprTemplate dr_decl = new Draw_SyntaxExprTemplate();
		dr_decl.elem = this.elem.getCompiled(null);
		dr_decl.l_paren = (Draw_SyntaxToken)this.l_paren.getCompiled(null);
		dr_decl.bad_op = (Draw_SyntaxToken)this.bad_op.getCompiled(null);
		dr_decl.r_paren = (Draw_SyntaxToken)this.r_paren.getCompiled(null);
		dr_decl.operators = new Draw_SyntaxToken[this.operators.length];
		for (int i=0; i < dr_decl.operators.length; i++)
			dr_decl.operators[i] = (Draw_SyntaxToken)this.operators[i].getCompiled(null);
		return dr_decl;
	}
}


@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpr extends SyntaxElem {
	@nodeAttr
	public SyntaxAttr∅			attrs;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SyntaxExprTemplate⇑	template;

	public SyntaxExpr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxExpr dr_elem = new Draw_SyntaxExpr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxExpr dr_elem = (Draw_SyntaxExpr)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = (Draw_SyntaxExprTemplate)this.template.dnode.getCompiled();
		dr_elem.attrs = new Draw_SyntaxAttr[this.attrs.length];
		for (int i=0; i < dr_elem.attrs.length; i++)
			dr_elem.attrs[i] = (Draw_SyntaxAttr)this.attrs[i].getCompiled(dr_elem.elem_decl);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxAutoParenth extends SyntaxElem {
	@nodeAttr
	public SyntaxAttr					attr;

	@nodeAttr
	public int							priority;

	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SyntaxExprTemplate⇑			template;

	public SyntaxAutoParenth() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxAutoParenth dr_elem = new Draw_SyntaxAutoParenth(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxAutoParenth dr_elem = (Draw_SyntaxAutoParenth)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = (Draw_SyntaxExprTemplate)this.template.dnode.getCompiled();
		dr_elem.attr = (Draw_SyntaxAttr)this.attr.getCompiled(dr_elem.elem_decl);
		dr_elem.priority = this.priority;
	}
}

