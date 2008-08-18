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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.ItemEditor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

public class MouseButtonEditor implements ItemEditor, MouseListener, KeyListener {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final kiev.fmt.evt.MouseEvent	node;
	private final JDialog		dialog;
	private final JLabel		labelKey;
	private final JLabel		labelMessage;
	private       boolean		done;
	private       int			mouseButton;
	private       int			mouseCount;
	private       int			mouseModifiers;
	
	public MouseButtonEditor(Editor editor, DrawTerm cur_elem, kiev.fmt.evt.MouseEvent node) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.node = node;
		this.mouseButton   = node.get$button();
		this.mouseCount    = node.get$count();
		if (node.get$withCtrl())
			this.mouseModifiers |= KeyEvent.CTRL_DOWN_MASK;
		if (node.get$withAlt())
			this.mouseModifiers |= KeyEvent.ALT_DOWN_MASK;
		if (node.get$withShift())
			this.mouseModifiers |= KeyEvent.SHIFT_DOWN_MASK;
		this.dialog    = new JDialog();
		dialog.setLayout(new BorderLayout());
		labelMessage = new JLabel();
		labelMessage.setHorizontalAlignment(SwingConstants.CENTER);
		dialog.add(labelMessage, BorderLayout.NORTH);
		labelKey = new JLabel();
		labelKey.setHorizontalAlignment(SwingConstants.CENTER);
		labelKey.setVerticalAlignment(SwingConstants.CENTER);
		labelKey.setBorder(new BevelBorder(BevelBorder.RAISED));
		dialog.add(labelKey, BorderLayout.CENTER);
	}
	
	String getMouseText() {
		String text = MouseEvent.getModifiersExText(mouseModifiers);
		text += " # "+mouseCount;
		return text;
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
			if (!(dt.drnode instanceof kiev.fmt.evt.MouseEvent))
				return null;
			return new MouseButtonEditor(editor, dt, (kiev.fmt.evt.MouseEvent)dt.drnode);
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
		labelMessage.setText("Press the button...");
		labelKey.setText("Current mouse code is "+getMouseText());
		dialog.setVisible(true);
	}

	private void setKeysFromEvent(InputEvent evt, boolean released) {
		mouseModifiers = evt.getModifiersEx();
		if (evt instanceof MouseEvent) {
			mouseButton = ((MouseEvent)evt).getButton();
			mouseCount = ((MouseEvent)evt).getClickCount();
		}
		labelKey.setText("New button is: " + getMouseText());
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		switch (code) {
		case KeyEvent.VK_CONTROL:
		case KeyEvent.VK_ALT:
		case KeyEvent.VK_ALT_GRAPH:
		case KeyEvent.VK_SHIFT:
			setKeysFromEvent(evt, false);
			return;
		case KeyEvent.VK_ENTER:
			dialog.setVisible(false);
			if (mouseButton != 0 && mouseButton != MouseEvent.NOBUTTON) {
				node.set$button(mouseButton);
				node.set$count(mouseCount);
				node.set$withCtrl((mouseModifiers & KeyEvent.CTRL_DOWN_MASK) != 0);
				node.set$withAlt((mouseModifiers & KeyEvent.ALT_DOWN_MASK) != 0);
				node.set$withShift((mouseModifiers & KeyEvent.SHIFT_DOWN_MASK) != 0);
				node.set$text(getMouseText());
				editor.stopItemEditor(false);
			} else {
				editor.stopItemEditor(true);
			}
			return;
		case KeyEvent.VK_ESCAPE:
			dialog.setVisible(false);
			editor.stopItemEditor(true);
			return;
		}
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (!done) {
			setKeysFromEvent(e, false);
			return;
		}
		if (e.getModifiersEx() == mouseModifiers && e.getClickCount() > mouseCount) {
			setKeysFromEvent(e, false);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (!done && e.getClickCount() > 0) {
			done = true;
			labelMessage.setText("ENTER to save, ESC to cansel");
		}
	}
	
}
