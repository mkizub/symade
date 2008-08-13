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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JLabel;

import kiev.fmt.DrawTerm;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ScalarPtr;

public class MouseButtonEditor 
	implements ItemEditor, MouseListener, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final ScalarPtr		pattr;
	private final JDialog	dialog;
	private  JLabel label;
	
	public MouseButtonEditor(Editor editor, DrawTerm cur_elem, ScalarPtr pattr) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.pattr = pattr;
		this.dialog = new JDialog();
		dialog.setLayout(new BorderLayout());
		label = new JLabel();
		dialog.add(label, BorderLayout.CENTER);
	}
	
	int getMouseCode() {
		Integer code = (Integer)pattr.get();
		return code == null?0:code.intValue();
	}
	
	void setMouseCode(int mouseCode ) {
		int oldCode = getMouseCode();
		if (mouseCode != oldCode) {
			pattr.set(new Integer(mouseCode));
		}
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the mouse code value"; }
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
			return new MouseButtonEditor(editor, dt, pattr);
		}
	}

	public void run() {
		editor.startItemEditor(this);
		GfxDrawTermLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.getHeight();
		int y = cur_dtli.getY() + h - editor.getView_canvas().getTranslated_y();
		dialog.addKeyListener(this);
		dialog.addMouseListener(this);
		dialog.setTitle("Press the button...");
		dialog.setModal(true);
		dialog.setBounds(x, y, 200, 100);
		label.setText("Current mouse code is "+getMouseCode());
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
	}

	public void mouseClicked(MouseEvent e) {		
	}

	public void mouseEntered(MouseEvent e) {		
	}

	public void mouseExited(MouseEvent e) {		
	}

	public void mousePressed(MouseEvent e) {
		int code = e.getButton();
		dialog.setVisible(false);
		setMouseCode(code);
		editor.stopItemEditor(false);		
	}

	public void mouseReleased(MouseEvent e) {		
	}
	
}
