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
package kiev.gui;

import kiev.fmt.DrawEnumChoice;
import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawTerm;
import kiev.fmt.DrawToken;
import kiev.fmt.Draw_SyntaxEnumChoice;
import kiev.fmt.Draw_SyntaxToken;
import kiev.fmt.Drawable;
import kiev.fmt.SyntaxTokenKind;
import kiev.vlang.ConstIntExpr;
import kiev.vlang.ENode;
import kiev.vlang.SymbolRef;
import kiev.vtree.ScalarPtr;

public final class ChooseItemEditor implements UIActionFactory {
	public String getDescr() { return "Edit current element"; }
	public boolean isForPopupMenu() { return false; }
	public Runnable getAction(UIActionViewContext context) {
		if (context.editor == null)
			return null;
		Editor editor = context.editor;
		if (context.dt == null || context.node == null)
			return null;
		if (context.dt.get$drnode() != context.node)
			return null;
		Drawable dr = context.dr;
		if (dr instanceof DrawNodeTerm) {
			DrawNodeTerm dt = (DrawNodeTerm)dr;
			ScalarPtr pattr = dt.getScalarPtr();
			Object obj = pattr.get();
			if (obj instanceof SymbolRef)
				return UIManager.newTextEditor(editor, dt, ((SymbolRef)obj).getScalarPtr("name"));
			else if (obj instanceof String || obj == null && pattr.slot.typeinfo.clazz == String.class)
				return UIManager.newTextEditor(editor, dt, pattr);
			else if (obj instanceof Integer)
				return UIManager.newIntEditor(editor, dt, pattr);
			else if (obj instanceof ConstIntExpr)
				return UIManager.newIntEditor(editor, dt, ((ConstIntExpr)obj).getScalarPtr("value"));
			else if (obj instanceof Boolean || Enum.class.isAssignableFrom(pattr.slot.typeinfo.clazz))
				return UIManager.newEnumEditor(editor, dt, pattr);
		}
		else if (dr instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr;
			Draw_SyntaxEnumChoice stx = (Draw_SyntaxEnumChoice)dec.syntax;
			DrawTerm dt = dr.getFirstLeaf();
			if (dt == null) {
				dt = editor.getCur_elem().dr.getFirstLeaf();
				if (dt == null)
					dt = editor.getCur_elem().dr.getNextLeaf();
			}
			return UIManager.newEnumEditor(editor, dt, dec.get$drnode().getScalarPtr(stx.name));
		}
		else if (dr.parent() instanceof DrawEnumChoice) {
			DrawEnumChoice dec = (DrawEnumChoice)dr.parent();
			Draw_SyntaxEnumChoice stx = (Draw_SyntaxEnumChoice)dec.syntax;
			return UIManager.newEnumEditor(editor, dr.getFirstLeaf(), dec.get$drnode().getScalarPtr(stx.name));
		}
		else if (dr instanceof DrawToken && dr.get$drnode() instanceof ENode && ((Draw_SyntaxToken)dr.syntax).kind == SyntaxTokenKind.OPERATOR) {
			return UIManager.newOperatorEditor(editor, (DrawToken)dr);
		}
		return null;
	}
}
