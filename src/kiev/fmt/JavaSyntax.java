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
public class SyntaxJavaCommentTemplate extends ASyntaxElemDecl {
	@nodeAttr public SyntaxElem		newline;
	@nodeAttr public SyntaxElem		lin_beg;
	@nodeAttr public SyntaxElem		doc_beg;
	@nodeAttr public SyntaxElem		cmt_beg;
	@nodeAttr public SyntaxElem		cmt_end;

	public Draw_SyntaxJavaCommentTemplate getCompiled() {
		Draw_SyntaxJavaCommentTemplate dr_decl = new Draw_SyntaxJavaCommentTemplate();
		dr_decl.elem = this.elem.getCompiled();
		dr_decl.newline = this.newline.getCompiled();
		dr_decl.lin_beg = this.lin_beg.getCompiled();
		dr_decl.doc_beg = this.doc_beg.getCompiled();
		dr_decl.cmt_beg = this.cmt_beg.getCompiled();
		dr_decl.cmt_end = this.cmt_end.getCompiled();
		return dr_decl;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaAccessExpr extends SyntaxElem {
	@nodeAttr public SyntaxElem			obj_elem;
	@nodeAttr public SyntaxToken		separator;
	@nodeAttr public SyntaxElem			fld_elem;

	public SyntaxJavaAccessExpr() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxJavaAccessExpr dr_elem = new Draw_SyntaxJavaAccessExpr();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxJavaAccessExpr dr_elem = (Draw_SyntaxJavaAccessExpr)_dr_elem;
		dr_elem.obj_elem = this.obj_elem.getCompiled();
		dr_elem.separator = (Draw_SyntaxToken)this.separator.getCompiled();
		dr_elem.fld_elem = this.fld_elem.getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaAccess extends SyntaxElem {
	public SyntaxJavaAccess() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxJavaAccess dr_elem = new Draw_SyntaxJavaAccess();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaPackedField extends SyntaxElem {
	public SyntaxJavaPackedField() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxJavaPackedField dr_elem = new Draw_SyntaxJavaPackedField();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}


@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaComment extends SyntaxElem {
	@nodeAttr public SymbolRef<SyntaxJavaCommentTemplate>	template;

	public SyntaxJavaComment() {
		this.template = new SymbolRef<SyntaxJavaCommentTemplate>();
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

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxJavaComment dr_elem = new Draw_SyntaxJavaComment();
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxJavaComment dr_elem = (Draw_SyntaxJavaComment)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = this.template.dnode.getCompiled();
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaConstructorName extends SyntaxElem {
	public SyntaxJavaConstructorName() {}

	public Draw_SyntaxElem getCompiled() {
		Draw_SyntaxJavaConstructorName dr_elem = new Draw_SyntaxJavaConstructorName();
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class KievTextSyntax extends ATextSyntax {
	public KievTextSyntax() {}

	public Draw_ATextSyntax getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Draw_KievTextSyntax();
		fillCompiled(compiled);
		return compiled;
	}
	
}

