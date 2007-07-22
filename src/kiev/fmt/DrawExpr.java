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


@node(copyable=false)
public class DrawAutoParenth extends DrawNonTerm {
	
	public DrawAutoParenth(ANode node, SyntaxAutoParenth syntax, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		super(node, syntax, attr_syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxAutoParenth sp = (SyntaxAutoParenth)this.syntax;
		SyntaxExprTemplate template = sp.template.dnode;
		ENode node;
		if (sp.attr instanceof SyntaxNode)
			node = this.drnode;
		else
			node = (ENode)this.drnode.getVal(sp.attr.name);
		if (args.length == 0) {
			args.append(template.l_paren.makeDrawable(cont.fmt, node, null, text_syntax));
			args.append(sp.attr.makeDrawable(cont.fmt, this.drnode, null, text_syntax));
			args.append(template.r_paren.makeDrawable(cont.fmt, node, null, text_syntax));
		}
		assert(args.length == 3);
		args[0].preFormat(cont,template.l_paren,node);
		args[1].preFormat(cont,sp.attr,this.drnode);
		args[2].preFormat(cont,template.r_paren,node);
		calcMaxLayout();
	}
}

@node(copyable=false)
public class DrawLispExpr extends DrawNonTerm {
	
	public DrawLispExpr(ANode node, SyntaxExpr syntax, SyntaxElem attr_syntax, ATextSyntax text_syntax) {
		super(node, syntax, attr_syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		SyntaxExpr se = (SyntaxExpr)this.syntax;
		SyntaxExprTemplate st = (SyntaxExprTemplate)se.template.dnode;
		ENode node = (ENode)this.drnode;
		ENode[] eargs = node.getArgs();
		if (args.length != eargs.length + 3) {
			args.delAll();
			args.append(st.l_paren.makeDrawable(cont.fmt, node, null, text_syntax));
			args.append(st.bad_op.makeDrawable(cont.fmt, node, null, text_syntax));
			foreach (ENode ea; eargs)
				args.append(st.elem.makeDrawable(cont.fmt, ea, null, text_syntax));
			args.append(st.l_paren.makeDrawable(cont.fmt, node, null, text_syntax));
		}
		int n = 0;
		args[n++].preFormat(cont,st.l_paren,node);
		args[n++].preFormat(cont,st.bad_op,node);
		foreach (ENode ea; eargs)
			args[n++].preFormat(cont,st.elem,ea);
		args[n++].preFormat(cont,st.r_paren,node);
		calcMaxLayout();
	}
}
