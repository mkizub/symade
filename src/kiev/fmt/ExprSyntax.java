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


@ThisIsANode(lang=SyntaxLang)
public class SyntaxExprTemplate extends ASyntaxElemDecl {
	@virtual typedef This  = SyntaxExprTemplate;

	@nodeAttr public SyntaxToken		l_paren;
	@nodeAttr public SyntaxToken		bad_op;
	@nodeAttr public SyntaxToken		r_paren;
	@nodeAttr public SyntaxToken[]		operators;

	public SyntaxExprTemplate() {
		super(new SyntaxNode());
		this.l_paren = new SyntaxToken("(");
		this.bad_op = new SyntaxToken("\u25d9");
		this.r_paren = new SyntaxToken(")");
	}

	public Draw_SyntaxExprTemplate getCompiled() {
		Draw_SyntaxExprTemplate dr_decl = new Draw_SyntaxExprTemplate();
		dr_decl.elem = this.elem.getCompiled();
		dr_decl.l_paren = (Draw_SyntaxToken)this.l_paren.getCompiled();
		dr_decl.bad_op = (Draw_SyntaxToken)this.bad_op.getCompiled();
		dr_decl.r_paren = (Draw_SyntaxToken)this.r_paren.getCompiled();
		dr_decl.operators = new Draw_SyntaxToken[this.operators.length];
		for (int i=0; i < dr_decl.operators.length; i++)
			dr_decl.operators[i] = (Draw_SyntaxToken)this.operators[i].getCompiled();
		return dr_decl;
	}
}


@ThisIsANode(lang=SyntaxLang)
public class SyntaxExpr extends SyntaxElem {
	@virtual typedef This  = SyntaxExpr;

	@nodeAttr public SyntaxAttr[]					attrs;
	@nodeAttr public SymbolRef<SyntaxExprTemplate>	template;

	public SyntaxExpr() {
		this.template = new SymbolRef<SyntaxExprTemplate>();
	}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxExprTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved expression template "+template);
			else if (template.symbol != d)
				template.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxExprTemplate> vect = new Vector<SyntaxExprTemplate>();
			SyntaxExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxExpr dr_elem = new Draw_SyntaxExpr();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxExpr dr_elem = (Draw_SyntaxExpr)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = (Draw_SyntaxExprTemplate)this.template.dnode.getCompiled();
		dr_elem.attrs = new Draw_SyntaxAttr[this.attrs.length];
		for (int i=0; i < dr_elem.attrs.length; i++)
			dr_elem.attrs[i] = (Draw_SyntaxAttr)this.attrs[i].getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxAutoParenth extends SyntaxElem {
	@virtual typedef This  = SyntaxAutoParenth;

	@nodeAttr public SyntaxAttr						attr;
	@nodeAttr public int							priority;
	@nodeAttr public SymbolRef<SyntaxExprTemplate>	template;

	public SyntaxAutoParenth() {}

	public void preResolveOut() {
		if (template.name != null && template.name != "") {
			SyntaxExprTemplate@ d;
			if (!PassInfo.resolveNameR(this,d,new ResInfo(this,template.name,ResInfo.noForwards)))
				Kiev.reportError(template,"Unresolved expression template "+template);
			else if (template.symbol != d)
				template.symbol = d;
		}
	}
	
	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "template") {
			ResInfo info = new ResInfo(this, name, by_equals ? 0 : ResInfo.noEquals);
			Vector<SyntaxExprTemplate> vect = new Vector<SyntaxExprTemplate>();
			SyntaxExprTemplate@ d;
			foreach (PassInfo.resolveNameR(this,d,info))
				if (!vect.contains(d)) vect.append(d);
			return vect.toArray();
		}
		return super.findForResolve(name,slot,by_equals);
	}


	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxAutoParenth dr_elem = new Draw_SyntaxAutoParenth();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxAutoParenth dr_elem = (Draw_SyntaxAutoParenth)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = (Draw_SyntaxExprTemplate)this.template.dnode.getCompiled();
		dr_elem.attr = (Draw_SyntaxAttr)this.attr.getCompiled();
		dr_elem.priority = this.priority;
	}
}

