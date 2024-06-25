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

import kiev.fmt.common.*;

@ThisIsANode(copyable=false)
public class DrawAutoParenth extends DrawNonTerm {
	
	public DrawAutoParenth(INode node, Formatter fmt, Draw_SyntaxAutoParenth syntax) {
		super(node, fmt, syntax);
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxAutoParenth sp = (Draw_SyntaxAutoParenth)this.syntax;
		Draw_SyntaxExprTemplate template = sp.template;
		ENode node;
		if (sp.attr instanceof Draw_SyntaxNode)
			node = (ENode)this.drnode;
		else
			node = (ENode)this.drnode.getVal(this.drnode.getAttrSlot(((Draw_SyntaxAttr)sp.attr).name));
		if (args.length == 0) {
			args.append(template.l_paren.makeDrawable(fmt, node));
			args.append(sp.attr.makeDrawable(fmt, this.drnode));
			args.append(template.r_paren.makeDrawable(fmt, node));
		}
		assert(args.length == 3);
		args[0].preFormat(fmt,template.l_paren,node);
		args[1].preFormat(fmt,sp.attr,this.drnode);
		args[2].preFormat(fmt,template.r_paren,node);
	}
}

@ThisIsANode(copyable=false)
public class DrawLispExpr extends DrawNonTerm {
	
	public DrawLispExpr(INode node, Formatter fmt, Draw_SyntaxExpr syntax) {
		super(node, fmt, syntax);
	}

	public void preFormat(Formatter fmt) {
		if (this.isUnvisible()) return;
		Draw_SyntaxExpr se = (Draw_SyntaxExpr)this.syntax;
		Draw_SyntaxExprTemplate st = (Draw_SyntaxExprTemplate)se.template;
		ENode node = (ENode)this.drnode;
		ENode[] eargs = node.getEArgs();
		if (args.length != eargs.length + 3) {
			args.delAll();
			args.append(st.l_paren.makeDrawable(fmt, node));
			args.append(st.bad_op.makeDrawable(fmt, node));
			foreach (ENode ea; eargs)
				args.append(st.elem.makeDrawable(fmt, ea));
			args.append(st.l_paren.makeDrawable(fmt, node));
		}
		int n = 0;
		args[n++].preFormat(fmt,st.l_paren,node);
		args[n++].preFormat(fmt,st.bad_op,node);
		foreach (ENode ea; eargs)
			args[n++].preFormat(fmt,st.elem,ea);
		args[n++].preFormat(fmt,st.r_paren,node);
	}
}
