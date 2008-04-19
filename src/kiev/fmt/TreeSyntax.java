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
public class TreeSyntax extends ATextSyntax {
	@virtual typedef This  = TreeSyntax;

	public TreeSyntax() {}

	public SyntaxElem getSyntaxElem(ANode node) {
		if (node == null)
			return super.getSyntaxElem(node);
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
			}
			return se;
		}
		SyntaxSet ss = new SyntaxSet();
		ss.folded_by_default = true;
		{
			String name = node.getClass().getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0)
				name = name.substring(idx+1);
			ss.folded = new SyntaxToken(name);
		}
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr.is_space) {
				SyntaxList lst = new SyntaxList(attr.name);
				lst.folded_by_default = true;
				ss.elements += lst;
			} else {
				ss.elements += new SyntaxSubAttr(attr.name);
			}
		}
		SyntaxElemDecl sed = new SyntaxElemDecl();
		sed.elem = ss;
		allSyntax.put(cl_name,sed);
		return ss;
	}
}

