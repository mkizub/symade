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

import kiev.gui.ActionPoint;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

/**
 * New element here UI Action.
 */
public final class NewElemHere extends NewElemEditor {

	/**
	 * The constructor.
	 * @param editor the editor
	 */
	public NewElemHere(Editor editor, ActionPoint ap, int idx) {
		super(editor, ap, idx);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		if (ap.curr_node != null && ap.curr_slot != null) {
			boolean ok = makeMenu("Insert new item", ap.curr_node, ap.curr_syntax, ap.curr_slot.name, null/*ap.dr.text_syntax*/);
			if (ok)
				return;
		}
		if (ap.space_node != null) {
			boolean ok = makeMenu("Insert new item", ap.space_node, ap.space_syntax, ap.space_slot.name, null/*ap.dr.text_syntax*/);
			if (ok)
				return;
		}
	}
	
	/**
	 * New Element Here UI Action Factory.
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Create a new element at this position"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			ActionPoint ap = context.ap;
			if (ap.curr_node != null && ap.curr_slot != null) {
				if (checkNewFuncAvailable(ap.curr_syntax))
					return new NewElemHere(editor, ap, -1);
			}
			if (ap.space_node != null) {
				if (checkNewFuncAvailable(ap.space_syntax))
					return new NewElemHere(editor, ap, ap.prev_index);
			}
			return null;
		}
	}
}
