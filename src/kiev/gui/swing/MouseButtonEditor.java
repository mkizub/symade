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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import kiev.fmt.DrawTerm;
import kiev.fmt.common.DrawLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.UIAction;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

/**
 * Mouse Button Editor UI Action.
 */
public class MouseButtonEditor implements UIAction, MouseListener, KeyListener {
	
	/**
	 * The editor.
	 */
	private final Editor editor;
	
	/**
	 * The current draw element.
	 */
	private final DrawTerm cur_elem;
	
	/**
	 * The mouse event node.
	 */
	private final kiev.fmt.evt.MouseEvent	node;
	
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
	 * The mouse button.
	 */
	private int mouseButton;
	
	/**
	 * The click count.
	 */
	private int mouseCount;
	
	/**
	 * Keyboard modifiers.
	 */
	private int mouseModifiers;
	
	/**
	 * The constructor.
	 * @param editor the editor.
	 * @param cur_elem the current element
	 * @param node the node
	 */
	MouseButtonEditor(Editor editor, DrawTerm cur_elem, kiev.fmt.evt.MouseEvent node) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.node = node;
		this.mouseButton = node.getButton();
		this.mouseCount = node.getCount();
		if (node.isWithCtrl())
			this.mouseModifiers |= InputEvent.CTRL_DOWN_MASK;
		if (node.isWithAlt())
			this.mouseModifiers |= InputEvent.ALT_DOWN_MASK;
		if (node.isWithShift())
			this.mouseModifiers |= InputEvent.SHIFT_DOWN_MASK;
		this.dialog = new JDialog();
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
	 * Gets mouse text.
	 * @return String
	 */
	String getMouseText() {
		String text = InputEvent.getModifiersExText(mouseModifiers);
		text += " # "+mouseCount;
		return text;
	}

	/**
	 * The Mouse Button Editor UI Action Factory.
	 */
	final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Edit the mouse code value"; }
		
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
			if (!(dt.drnode instanceof kiev.fmt.evt.MouseEvent)) return null;
			return new MouseButtonEditor(editor, dt, (kiev.fmt.evt.MouseEvent)dt.drnode);
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
		dialog.addMouseListener(this);
		dialog.setTitle("Press the button...");
		dialog.setModal(true);
		dialog.setBounds(x, y, 200, 100);
		labelMessage.setText("Press the button...");
		labelKey.setText("Current mouse code is "+getMouseText());
		dialog.setVisible(true);
	}

	/**
	 * set Keys From Event.
	 * @param evt the event
	 * @param released is released
	 */
	private void setKeysFromEvent(InputEvent evt, boolean released) {
		mouseModifiers = evt.getModifiersEx();
		if (evt instanceof MouseEvent) {
			mouseButton = ((MouseEvent)evt).getButton();
			mouseCount = ((MouseEvent)evt).getClickCount();
		}
		labelKey.setText("New button is: " + getMouseText());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent evt) {}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent evt) {}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
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
				editor.getWindow().startTransaction(editor, "MouseButtonEditor:keyPressed");
				try {
					node.setButton(mouseButton);
					node.setCount(mouseCount);
					node.setWithCtrl((mouseModifiers & InputEvent.CTRL_DOWN_MASK) != 0);
					node.setWithAlt((mouseModifiers & InputEvent.ALT_DOWN_MASK) != 0);
					node.setWithShift((mouseModifiers & InputEvent.SHIFT_DOWN_MASK) != 0);
					node.setText(getMouseText());
				} finally {
					editor.getWindow().stopTransaction(false);
				}
			}
			return;
		case KeyEvent.VK_ESCAPE:
			dialog.setVisible(false);
			return;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
		if (! done) {
			setKeysFromEvent(e, false);
			return;
		}
		if (e.getModifiersEx() == mouseModifiers && e.getClickCount() > mouseCount) {
			setKeysFromEvent(e, false);
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		if (! done && e.getClickCount() > 0) {
			done = true;
			labelMessage.setText("ENTER to save, ESC to cansel");
		}
	}
	
}
