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
package kiev.gui.swing;

import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import kiev.fmt.DrawTerm;
import kiev.fmt.common.DrawLayoutInfo;
import kiev.fmt.evt.KeyboardEvent;
import kiev.gui.Editor;
import kiev.gui.UIAction;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

/**
 * Key Code Editor UI Action.
 */
public class KeyCodeEditor implements UIAction, KeyListener {
	
	/**
	 * The editor.
	 */
	private final Editor editor;
	
	/**
	 * The current draw element.
	 */
	private final DrawTerm cur_elem;
	
	/**
	 * The keyboard event node.
	 */
	private final KeyboardEvent node;
	
	/**
	 * The dialog.
	 */
	private final JDialog dialog;
	
	/**
	 * The label key.
	 */
	private final JLabel labelKey;
	
	/**
	 * The label message.
	 */
	private final JLabel labelMessage;
	
	/**
	 * Is done.
	 */
	private boolean done;
	
	/**
	 * The key pressed.
	 */
	private int keyPressed;
	
	/**
	 * The key code.
	 */
	private int keyCode;
	
	/**
	 * The key modifiers.
	 */
	private int keyModifiers;
	
	/**
	 * The constructor.
	 * @param editor the editor
	 * @param cur_elem the current element
	 * @param node the node
	 */
	KeyCodeEditor(Editor editor, DrawTerm cur_elem, KeyboardEvent node) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.node = node;
		this.keyCode = node.getKeyCode();
		if (node.isWithCtrl())
			this.keyModifiers |= InputEvent.CTRL_DOWN_MASK;
		if (node.isWithAlt())
			this.keyModifiers |= InputEvent.ALT_DOWN_MASK;
		if (node.isWithShift())
			this.keyModifiers |= InputEvent.SHIFT_DOWN_MASK;
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
	
	/**
	 * Returns the key text.
	 * @return String
	 */
	String getKeyText() {
		String text = "";
		String mods = InputEvent.getModifiersExText(keyModifiers);
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

	/**
	 * The Key Code Editor UI Action Factory.
	 */
	final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Edit the key code value"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null) return null;
			Editor editor = context.editor;
			DrawTerm dt = context.dt;
			if (dt == null || context.node == null) return null;
			if (!(dt.drnode instanceof KeyboardEvent)) return null;
			return new KeyCodeEditor(editor, dt, (KeyboardEvent)dt.drnode);
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		DrawLayoutInfo cur_dtli = cur_elem.getGfxFmtInfo();
		int x = cur_dtli.getX();
		int h = cur_dtli.height;
		int y = cur_dtli.getY() + h - editor.getViewPeer().getVertOffset();
		dialog.addKeyListener(this);
		dialog.setTitle("Type the key...");
		dialog.setModal(true);
		dialog.setBounds(x, y, 200, 100);
		labelMessage.setText("Type the key...");
		labelKey.setText("Current key is: " + getKeyText());
		dialog.setVisible(true);
	}
	
	/**
	 * Set Keys From Event.
	 * @param evt the event
	 * @param released is released
	 */
	private void setKeysFromEvent(KeyEvent evt, boolean released) {
		if (released && keyPressed == 0) return;
		keyModifiers = evt.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK);
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

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent evt) {}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent evt) {
		if (done) return;
		setKeysFromEvent(evt, true);
		if (keyCode != KeyEvent.VK_UNDEFINED && keyCode == keyPressed) {
			done = true;
			labelMessage.setText("ENTER to save, ESC to cansel");
		}
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent evt) {
		if (done) {
			int code = evt.getKeyCode();
			if (code == KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_UNDEFINED) {
				editor.getWindow().startTransaction(editor, "KeyCodeEditor:keyPressed");
				try {
					node.setKeyCode(keyCode);
					node.setWithCtrl((keyModifiers & InputEvent.CTRL_DOWN_MASK) != 0);
					node.setWithAlt((keyModifiers & InputEvent.ALT_DOWN_MASK) != 0);
					node.setWithShift((keyModifiers & InputEvent.SHIFT_DOWN_MASK) != 0);
					node.setText(getKeyText());
					dialog.setVisible(false);
				} finally {
					editor.getWindow().stopTransaction(false);
				}
			}
			else if (code == KeyEvent.VK_ESCAPE) {
				dialog.setVisible(false);
			}
		} else {
			setKeysFromEvent(evt, false);
		}
	}
	
}
