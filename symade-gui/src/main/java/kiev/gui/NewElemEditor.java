/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui;

import kiev.fmt.common.DrawLayoutInfo;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_FuncNewNode;
import kiev.fmt.common.Draw_SyntaxAttr;
import kiev.fmt.common.Draw_SyntaxFunc;
import kiev.gui.Editor;
import kiev.gui.UIAction;
import kiev.gui.IPopupMenuPeer;
import kiev.vtree.INode;

/**
 * New Element Editor UI Action.
 */
public abstract class NewElemEditor implements UIAction, IPopupMenuListener {

	/** The editor. */
	protected final Editor editor;

	/** The action point. */
	protected final ActionPoint ap;

	/** The index. */
	protected final int idx;

	/**
	 * The constructor.
	 * @param editor the editor
	 */
	public NewElemEditor(Editor editor, ActionPoint ap, int idx) {
		this.editor = editor;
		this.ap = ap;
		this.idx = idx;
	}

	public static boolean checkNewFuncAvailable(Draw_SyntaxAttr satt) {
		if (satt.elem_decl == null || satt.elem_decl.funcs == null)
			return false;
		Draw_FuncNewNode fn = null;
		for (Draw_SyntaxFunc f : satt.elem_decl.funcs) {
			if (f.attr == satt.name && f instanceof Draw_FuncNewNode) {
				fn = (Draw_FuncNewNode)f;
				break;
			}
		}
		if (fn == null)
			return false;
		return fn.checkApplicable(satt.name);
	}
	/**
	 * Make Menu.
	 * @param title the title
	 * @param n the node
	 * @param satt the draw syntax attributes
	 * @param attr the attributes
	 * @param tstx the draw text syntax
	 */
	public boolean makeMenu(String title, INode n, Draw_SyntaxAttr satt, String attr, Draw_ATextSyntax tstx) {
		if (satt.elem_decl == null || satt.elem_decl.funcs == null)
			return false;
		Draw_FuncNewNode fn = null;
		for (Draw_SyntaxFunc f : satt.elem_decl.funcs) {
			if (f.attr == satt.name && f instanceof Draw_FuncNewNode) {
				fn = (Draw_FuncNewNode)f;
				break;
			}
		}
		if (fn == null || !fn.checkApplicable(satt.name))
			return false;
		IMenu m = fn.makeMenu(n, idx, tstx);
		if (m == null)
			return false;
		IPopupMenuPeer menu = editor.getViewPeer().getPopupMenu(this, m);
		DrawLayoutInfo cur_dtli = editor.getDrawTerm().getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.height;
		int y = cur_dtli.getY() + h - editor.getViewPeer().getVertOffset();
		menu.showAt(x, y);
		return true;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuListener#popupMenuCanceled()
	 */
	public void popupMenuCanceled() {
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuListener#popupMenuExecuted(kiev.gui.IMenuItem)
	 */
	public void popupMenuExecuted(final IMenuItem item) {
		editor.getWindow().startTransaction(editor, "Editor.java:NewElem");
		try {
			editor.getWindow().getEditorThreadGroup().runTask(new Runnable() {
				public void run() {
					item.exec();
				}
			});
		} finally {
			editor.getWindow().stopTransaction(false);
		}
		editor.formatAndPaint(true);
	}

}
