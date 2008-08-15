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
public class SyntaxJavaCommentTemplate extends ASyntaxElemDecl {
	@nodeAttr public SyntaxElem		newline;
	@nodeAttr public SyntaxElem		lin_beg;
	@nodeAttr public SyntaxElem		doc_beg;
	@nodeAttr public SyntaxElem		cmt_beg;
	@nodeAttr public SyntaxElem		cmt_end;

	public Draw_SyntaxJavaCommentTemplate getCompiled() {
		Draw_SyntaxJavaCommentTemplate dr_decl = new Draw_SyntaxJavaCommentTemplate();
		dr_decl.elem = this.elem.getCompiled(null);
		dr_decl.newline = this.newline.getCompiled(null);
		dr_decl.lin_beg = this.lin_beg.getCompiled(null);
		dr_decl.doc_beg = this.doc_beg.getCompiled(null);
		dr_decl.cmt_beg = this.cmt_beg.getCompiled(null);
		dr_decl.cmt_end = this.cmt_end.getCompiled(null);
		return dr_decl;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaAccessExpr extends SyntaxElem {
	@nodeAttr public SyntaxElem			obj_elem;
	@nodeAttr public SyntaxToken		separator;
	@nodeAttr public SyntaxElem			fld_elem;

	public SyntaxJavaAccessExpr() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaAccessExpr dr_elem = new Draw_SyntaxJavaAccessExpr(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxJavaAccessExpr dr_elem = (Draw_SyntaxJavaAccessExpr)_dr_elem;
		dr_elem.obj_elem = this.obj_elem.getCompiled(dr_elem.elem_decl);
		dr_elem.separator = (Draw_SyntaxToken)this.separator.getCompiled(dr_elem.elem_decl);
		dr_elem.fld_elem = this.fld_elem.getCompiled(dr_elem.elem_decl);
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaAccess extends SyntaxElem {
	public SyntaxJavaAccess() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaAccess dr_elem = new Draw_SyntaxJavaAccess(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}

@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaPackedField extends SyntaxElem {
	public SyntaxJavaPackedField() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaPackedField dr_elem = new Draw_SyntaxJavaPackedField(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}
}


@ThisIsANode(lang=SyntaxLang)
public class SyntaxJavaComment extends SyntaxElem {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve
	final
	public SyntaxJavaCommentTemplate⇑	template;

	public SyntaxJavaComment() {}

	public Draw_SyntaxElem getCompiled(Draw_SyntaxElemDecl elem_decl) {
		Draw_SyntaxJavaComment dr_elem = new Draw_SyntaxJavaComment(elem_decl);
		fillCompiled(dr_elem);
		return dr_elem;
	}

	public void fillCompiled(Draw_SyntaxElem _dr_elem) {
		Draw_SyntaxJavaComment dr_elem = (Draw_SyntaxJavaComment)_dr_elem;
		super.fillCompiled(dr_elem);
		dr_elem.template = this.template.dnode.getCompiled();
	}
}

