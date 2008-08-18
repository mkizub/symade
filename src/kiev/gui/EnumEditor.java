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

import java.util.EnumSet;

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ScalarPtr;

public class EnumEditor implements ItemEditor, IPopupMenuListener {
	private final Editor			editor;
	private final DrawTerm			cur_elem;
	private final ScalarPtr			pattr;
	private IPopupMenuPeer			menu;
	
	public EnumEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.pattr = pattr;
	}
	
	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the attribute as an enumerated value"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null)
				return null;
			if (!(dt.syntax instanceof Draw_SyntaxAttr))
				return null;
			if (dt.drnode != context.node)
				return null;
			ScalarPtr pattr = dt.drnode.getScalarPtr(((Draw_SyntaxAttr)dt.syntax).name);
			return new EnumEditor(editor, dt, pattr);
		}
	}

	public void run() {
		this.menu = UIManager.newPopupMenu(editor, this);
		editor.startItemEditor(this);
		if (pattr.slot.typeinfo.clazz == Boolean.class || pattr.slot.typeinfo.clazz == boolean.class) {
			menu.addItem(new SetSyntaxAction(Boolean.FALSE));
			menu.addItem(new SetSyntaxAction(Boolean.TRUE));
		} else {
			for (Object e: EnumSet.allOf(pattr.slot.typeinfo.clazz))
				menu.addItem(new SetSyntaxAction(e));
		}
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.showAt(x, y);
	}

	public void popupMenuCanceled() {
		menu.remove();
		editor.stopItemEditor(true);
	}
	public void popupMenuExecuted(IMenuItem item) {
		SetSyntaxAction sa = (SetSyntaxAction)item;
		menu.remove();
		try {
			pattr.set(sa.val);
			sa = null;
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			editor.stopItemEditor(sa != null);
		}
	}

	class SetSyntaxAction implements IMenuItem {
		final Object val; // Enum or Boolean
		SetSyntaxAction(Object val) {
			this.val = val;
		}
		public String getText() {
			return String.valueOf(val);
		}
	}
}
