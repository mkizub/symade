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


@ThisIsANode(copyable=false)
public class DrawAutoParenth extends DrawNonTerm {
	
	public DrawAutoParenth(ANode node, Draw_SyntaxAutoParenth syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxAutoParenth sp = (Draw_SyntaxAutoParenth)this.syntax;
		Draw_SyntaxExprTemplate template = sp.template;
		ENode node;
		if (sp.attr instanceof Draw_SyntaxNode)
			node = (ENode)this.drnode;
		else
			node = (ENode)this.drnode.getVal(sp.attr.name);
		if (args.length == 0) {
			args.append(template.l_paren.makeDrawable(cont.fmt, node, text_syntax));
			args.append(sp.attr.makeDrawable(cont.fmt, this.drnode, text_syntax));
			args.append(template.r_paren.makeDrawable(cont.fmt, node, text_syntax));
		}
		assert(args.length == 3);
		args[0].preFormat(cont,template.l_paren,node);
		args[1].preFormat(cont,sp.attr,this.drnode);
		args[2].preFormat(cont,template.r_paren,node);
		calcMaxLayout();
	}
}

@ThisIsANode(copyable=false)
public class DrawLispExpr extends DrawNonTerm {
	
	public DrawLispExpr(ANode node, Draw_SyntaxExpr syntax, Draw_ATextSyntax text_syntax) {
		super(node, syntax, text_syntax);
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible()) return;
		Draw_SyntaxExpr se = (Draw_SyntaxExpr)this.syntax;
		Draw_SyntaxExprTemplate st = (Draw_SyntaxExprTemplate)se.template;
		ENode node = (ENode)this.drnode;
		ENode[] eargs = node.getArgs();
		if (args.length != eargs.length + 3) {
			args.delAll();
			args.append(st.l_paren.makeDrawable(cont.fmt, node, text_syntax));
			args.append(st.bad_op.makeDrawable(cont.fmt, node, text_syntax));
			foreach (ENode ea; eargs)
				args.append(st.elem.makeDrawable(cont.fmt, ea, text_syntax));
			args.append(st.l_paren.makeDrawable(cont.fmt, node, text_syntax));
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
