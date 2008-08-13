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

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ScalarPtr;

public class KeyCodeEditor 
	implements ItemEditor, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final ScalarPtr		pattr;
	private final JDialog	dialog;
	private  JLabel label;
	
	public KeyCodeEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.pattr = pattr;
		this.dialog = new JDialog();
		dialog.setLayout(new BorderLayout());
		label = new JLabel();
		dialog.add(label, BorderLayout.CENTER);
	}
	
	int getKeyCode() {
		Integer code = (Integer)pattr.get();
		return code == null?0:code.intValue();
	}
	
	void setKeyCode(int keyCode ) {
		int oldCode = getKeyCode();
		if (keyCode != oldCode) {
			pattr.set(new Integer(keyCode));
		}
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the key code value"; }
		public boolean isForPopupMenu() { return false; }
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
			return new KeyCodeEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		dialog.addKeyListener(this);
		dialog.setTitle("Type the key...");
		dialog.setModal(true);
		dialog.setBounds(x, y, 200, 100);
		label.setText("Current key code is "+getKeyCode());
		dialog.setVisible(true);
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		switch (code) {
		case KeyEvent.VK_ESCAPE:
			dialog.setVisible(false);
			editor.stopItemEditor(true);
			return;
		}
		dialog.setVisible(false);
		setKeyCode(code);
		editor.stopItemEditor(false);
	}
	
}
