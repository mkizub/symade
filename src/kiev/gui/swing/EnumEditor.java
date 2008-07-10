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
package kiev.gui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.EnumSet;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.TextAction;

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ScalarPtr;

public class EnumEditor 
	implements ItemEditor, PopupMenuListener, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final ScalarPtr		pattr;
	private final JPopupMenu	menu;
	
	public EnumEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.pattr = pattr;
		this.menu = new JPopupMenu();
	}
	
	final static class Factory implements UIActionFactory {
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
			if (dt.get$drnode() != context.node)
				return null;
			ScalarPtr pattr = dt.get$drnode().getScalarPtr(((Draw_SyntaxAttr)dt.syntax).name);
			return new EnumEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		if (pattr.slot.typeinfo.clazz == Boolean.class || pattr.slot.typeinfo.clazz == boolean.class) {
			menu.add(new JMenuItem(new SetSyntaxAction(Boolean.FALSE)));
			menu.add(new JMenuItem(new SetSyntaxAction(Boolean.TRUE)));
		} else {
			for (Object e: EnumSet.allOf(pattr.slot.typeinfo.clazz))
				menu.add(new JMenuItem(new SetSyntaxAction(e)));
		}
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		menu.addPopupMenuListener(this);
		menu.show((Component)editor.getView_canvas(), x, y);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {}
	
	public void popupMenuCanceled(PopupMenuEvent e) {
		((Canvas)editor.getView_canvas()).remove(menu);
		editor.stopItemEditor(true);
	}
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

	class SetSyntaxAction extends TextAction {
		private static final long serialVersionUID = -5941342241938414111L;
		private Object val; // Enum or Boolean
		SetSyntaxAction(Object val) {
			super(String.valueOf(val));
			this.val = val;
		}
		public void actionPerformed(ActionEvent e) {
			((Canvas)editor.getView_canvas()).remove(menu);
			try {
				pattr.set(val);
			} catch (Throwable t) {
				editor.stopItemEditor(true);
				e = null;
			}
			if (e != null)
				editor.stopItemEditor(false);
		}
	}
}
