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

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ScalarPtr;

public final class IntEditor extends TextEditor {
	
	public IntEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr) {
		super(editor, dr_term, pattr);
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the attribute as an integer"; }
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
			return new IntEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		editor.getView_canvas().setCursor_offset(edit_offset);
		String text = this.getText();
		if (text != null) {
			edit_offset = text.length();
			editor.getView_canvas().setCursor_offset(edit_offset + dr_term.getPrefix().length());
		}
	}

	String getText() {
		Object o = pattr.get();
		if (o == null)
			return null;
		return String.valueOf(o);
	}
	void setText(String text) {
		pattr.set(Integer.valueOf(text));
	}
}

