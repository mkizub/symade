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
import kiev.vtree.ScalarAttrSlot;

/**
 * Text Editor UI Action.
 */
public class EnumEditor implements IPopupMenuListener, UIAction {
	
	/**
	 * The editor.
	 */
	protected final Editor editor;
	
	/**
	 * The attributes.
	 */
	public final ActionPoint ap;
	
	/**
	 * The menu.
	 */
	private IPopupMenuPeer menu;
	
	/**
	 * Text Editor UI Action Factory.
	 */
	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the attribute as enum value"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null) return null;
			Editor editor = context.editor;
			ActionPoint ap = context.ap;
			if (ap.curr_node != null && ap.curr_slot instanceof ScalarAttrSlot) {
				Class clazz = ap.curr_slot.typeinfo.clazz;
				if (clazz == Boolean.TYPE || clazz == Boolean.class || Enum.class.isAssignableFrom(clazz))
					return new EnumEditor(editor, ap);
			}
			return null;
		}
	}

	/**
	 * The constructor.
	 * @param editor the editor
	 * @param dr_term the draw term
	 * @param pattr the attributes
	 */
	public EnumEditor(Editor editor, ActionPoint ap) {
		this.editor = editor;
		this.ap = ap;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	@SuppressWarnings("unchecked")
	public void exec() {
		menu = editor.getViewPeer().getPopupMenu(this, null);
		//for (IMenuItem act: actions) menu.addItem(act);
		Class clazz = ap.curr_slot.typeinfo.clazz;
		if (clazz == Boolean.TYPE) {
			menu.addItem(new SetElemAction("false", Boolean.FALSE));
			menu.addItem(new SetElemAction("true", Boolean.TRUE));
		}
		else if (clazz == Boolean.class) {
			menu.addItem(new SetElemAction("False", Boolean.FALSE));
			menu.addItem(new SetElemAction("True", Boolean.TRUE));
			menu.addItem(new SetElemAction("Empty (null)", null));
		}
		else {
			Enum[] vals;
			try {
				vals = (Enum[])clazz.getMethod("values").invoke(null);
			} catch (Exception e) {
				return;
			}
			for (int i=0; i < vals.length; i++) {
				Enum v = vals[i];
				menu.addItem(new SetElemAction(String.valueOf(v), v));
			}
			menu.addItem(new SetElemAction("Empty (null)", null));
		}
		DrawLayoutInfo cur_dtli = editor.getDrawTerm().getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.height;
		int y = cur_dtli.getY() + h - editor.getViewPeer().getVertOffset();
		menu.showAt(x, y);
	}
	
	public void popupMenuCanceled() {
		menu.remove();
	}

	public void popupMenuExecuted(final IMenuItem item) {
		menu.remove();
		editor.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
			public void run() {
				item.exec();
				editor.formatAndPaint(true);
			}
		});
	}

	/**
	 * Set element Function Action.
	 */
	public class SetElemAction implements IMenuItem {
		private final String text;
		private final Object value;
		public SetElemAction(String text, Object value) {
			this.text = text;
			this.value = value;
		}
		public String getText() {
			return text;
		}
		public void exec() {
			editor.getWindow().startTransaction(editor, "Action:EnumEditor");
			try {
				ap.curr_node.setVal(ap.curr_slot, value);
			} finally {
				editor.getWindow().stopTransaction(false);
			}
		}
	}
}

