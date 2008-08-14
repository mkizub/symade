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
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.evt.KeyboardEvent;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

public class KeyCodeEditor 
	implements ItemEditor, Runnable {
	private final Editor		editor;
	private final DrawTerm		cur_elem;
	private final KeyboardEvent	node;
	private final JDialog		dialog;
	private final JLabel		labelKey;
	private final JLabel		labelMessage;
	private       boolean		done;
	private       int			keyPressed;
	private       int			keyCode;
	private       int			keyModifiers;
	
	KeyCodeEditor(Editor editor, DrawTerm cur_elem, KeyboardEvent node) {
		this.editor    = editor;
		this.cur_elem  = cur_elem;
		this.node      = node;
		this.keyCode   = node.get$keyCode();
		if (node.get$withCtrl())
			this.keyModifiers |= KeyEvent.CTRL_DOWN_MASK;
		if (node.get$withAlt())
			this.keyModifiers |= KeyEvent.ALT_DOWN_MASK;
		if (node.get$withShift())
			this.keyModifiers |= KeyEvent.SHIFT_DOWN_MASK;
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
	
	String getKeyText() {
		String text = "";
		String mods = KeyEvent.getModifiersExText(keyModifiers);
		if (mods != null && mods.length() > 0)
			text += mods + "+";
		if (keyCode == KeyEvent.VK_UNDEFINED)
			return text;
		if (keyCode == KeyEvent.VK_CONTROL)
			return text;
		if (keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_ALT_GRAPH)
			return text;
		if (keyCode == KeyEvent.VK_SHIFT)
			return text;
		text += KeyEvent.getKeyText(keyCode);
		return text;
	}

	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Edit the key code value"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null)
				return null;
			if (!(dt.drnode instanceof KeyboardEvent))
				return null;
			return new KeyCodeEditor(editor, dt, (KeyboardEvent)dt.drnode);
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
		labelMessage.setText("Type the key...");
		labelKey.setText("Current key is: " + getKeyText());
		dialog.setVisible(true);
	}
	
	private void setKeysFromEvent(KeyEvent evt, boolean released) {
		if (released && keyPressed == 0)
			return;
		keyModifiers = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		keyCode = KeyEvent.VK_UNDEFINED;
		if (released) {
			if (keyPressed == evt.getKeyCode())
				keyCode = evt.getKeyCode();
		} else {
			keyPressed = evt.getKeyCode();
		}
		if (keyCode == KeyEvent.VK_CONTROL)
			keyCode = KeyEvent.VK_UNDEFINED;
		if (keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_ALT_GRAPH)
			keyCode = KeyEvent.VK_UNDEFINED;
		if (keyCode == KeyEvent.VK_SHIFT)
			keyCode = KeyEvent.VK_UNDEFINED;
		labelKey.setText("New key is: " + getKeyText());
	}

	public void keyTyped(KeyEvent evt) {}
	public void keyReleased(KeyEvent evt) {
		if (done)
			return;
		setKeysFromEvent(evt, true);
		if (keyCode != KeyEvent.VK_UNDEFINED && keyCode == keyPressed) {
			done = true;
			labelMessage.setText("ENTER to save, ESC to cansel");
		}
	}
	public void keyPressed(KeyEvent evt) {
		if (done) {
			int code = evt.getKeyCode();
			if (code == KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_UNDEFINED) {
				node.set$keyCode(keyCode);
				node.set$withCtrl((keyModifiers & KeyEvent.CTRL_DOWN_MASK) != 0);
				node.set$withAlt((keyModifiers & KeyEvent.ALT_DOWN_MASK) != 0);
				node.set$withShift((keyModifiers & KeyEvent.SHIFT_DOWN_MASK) != 0);
				node.set$text(getKeyText());
				dialog.setVisible(false);
				editor.stopItemEditor(false);
			}
			else if (code == KeyEvent.VK_ESCAPE) {
				dialog.setVisible(false);
				editor.stopItemEditor(true);
			}
		} else {
			setKeysFromEvent(evt, false);
		}
	}
	
}
