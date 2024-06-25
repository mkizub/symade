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
package kiev.gui.swt;

import java.text.MessageFormat;

import kiev.fmt.DrawTerm;
import kiev.fmt.common.DrawLayoutInfo;
import kiev.gui.Editor;
import kiev.gui.UIAction;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Editor used for editing mouse button codes.
 */
public final class MouseButtonEditor implements UIAction, MouseListener, KeyListener {
	
	/**
	 * The editor parameter.
	 */
	private final Editor editor;
	
	/**
	 * Current drawable leaf.
	 */
	private final DrawTerm cur_elem;
	
	/**
	 * Mouse event node in bindings DSL.
	 */
	private final kiev.fmt.evt.MouseEvent	node;
	
	/**
	 * The dialog used for editing codes.
	 */
	private Dialog dialog;
	
	/**
	 * The flag indicates that process of entering code is done.
	 */
	private boolean done;
	
	/**
	 * Mouse pressed code.
	 */
	private int mousePressed;
	
	/**
	 * Mouse button code.
	 */
	private int mouseButton;
	
	/**
	 * Count the mouse clicks.
	 */
	private int mouseCount;
	
	/**
	 * Modifiers such as Ctrl, Alt or Shift.
	 */
	private int mouseModifiers;

	/**
	 * Dialog appears asking for an action to be taken upon the entry.
	 */
	private final class Dialog {
		
		/**
		 * The dialog runs in its own shell.
		 */
		private Shell shell;
		
		/**
		 * The label to show the key.
		 */
		private Label	labelKey;
		
		/**
		 * The label to show the message.
		 */
		private Label	labelMessage;
				
		/**
		 * The constructor.
		 * @param parent the parent shell.
		 */
		public Dialog(Shell parent) {
			shell = new Shell(parent, SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.SYSTEM_MODAL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			shell.setLayout(layout);
			shell.setText(Window.resources.getString("MouseButtonEditor_title"));
			shell.addShellListener(new ShellAdapter(){
				/* (non-Javadoc)
				 * @see org.eclipse.swt.events.ShellAdapter#shellClosed(org.eclipse.swt.events.ShellEvent)
				 */
				public void shellClosed(ShellEvent e) {
					e.doit = false;
					shell.setVisible(false);
				}
			});

			labelMessage = new Label(shell, SWT.LEFT);
			labelMessage.setAlignment(SWT.CENTER);

			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 300;
			labelMessage.setLayoutData(gridData);

			labelKey = new Label(shell, SWT.LEFT);
			labelKey.setAlignment(SWT.CENTER);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 300;
			labelKey.setLayoutData(gridData);
		}

		/**
		 * Initialize the dialog controls with the initial values.
		 */
		private void init(){
			done = false; 
			mousePressed = 0;			
			labelMessage.setText(Window.resources.getString("MouseButtonEditor_label_message"));
			labelKey.setText(MessageFormat.format(Window.resources.getString("MouseButtonEditor_label_key"), new Object[] {getMouseText()}));		
			shell.pack();
		}

		/**
		 * Sets the dialog title.
		 * @param title the title
		 */
		public void setTitle(String title){
			shell.setText(title);
		}

		/**
		 * Sets bounds as for X/Y coordinates combined with width and height
		 * of the dialog shell. 
		 * @param x the X coordinate
		 * @param y the Y coordinate
		 * @param w the width
		 * @param h the height
		 */
		public void setBounds(int x, int y, int w, int h){
			shell.setBounds(x, y, w, h);
		}

		/**
		 * Opens the dialog and captures user input.
		 */
		public void open() {
			init();
			if (shell.isVisible()) {
				shell.setFocus();
			} else {
				shell.open();
			}
			shell.setCapture(true);
		}
		/**
		 * Closes the dialog. We are just hide it.
		 */
		public void close() {
			shell.setCapture(false);
			shell.setVisible(false);
		}
	}

	/**
	 * This a constructor.
	 * @param shell uses the shell
	 */
	MouseButtonEditor(Shell shell){
		this.editor = null;
		this.cur_elem = null;
		this.node = new kiev.fmt.evt.MouseEvent();
		dialog = new Dialog(shell);
		dialog.shell.addKeyListener(this);
		dialog.shell.addMouseListener(this);
		dialog.setTitle(Window.resources.getString("MouseButtonEditor_title"));
		dialog.setBounds(100, 100, 300, 100);
	}

	/**
	 * Another constructor is more usable against its initializers. 
	 * @param editor the editor
	 * @param cur_elem the editing element
	 * @param node the editing node
	 */
	MouseButtonEditor(Editor editor, DrawTerm cur_elem, kiev.fmt.evt.MouseEvent node) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.node = node;
		mouseButton = node.getButton();
		mouseCount = node.getCount();
		if (node.isWithCtrl())
			mouseModifiers |= SWT.CTRL;
		if (node.isWithAlt())
			mouseModifiers |= SWT.ALT;
		if (node.isWithShift())
			mouseModifiers |= SWT.SHIFT;
		dialog = new Dialog(Window.getShell());
	}

	/**
	 * Gets the modifiers text representation of the mask. 
	 * @param modifiers the mask
	 * @return text representation
	 */
	static String getModifiersText(int modifiers) {
		StringBuffer buf = new StringBuffer();
		if ((modifiers & SWT.CTRL) != 0) {
			buf.append("Ctrl");
			buf.append("+");
		}
		if ((modifiers & SWT.ALT) != 0) {
			buf.append("Alt");
			buf.append("+");
		}
		if ((modifiers & SWT.SHIFT) != 0) {
			buf.append("Shift");
			buf.append("+");
		}
		if ((modifiers & SWT.BUTTON1) != 0) {
			buf.append("Button1");
			buf.append("+");
		}
		if ((modifiers & SWT.BUTTON2) != 0) {
			buf.append("Button2");
			buf.append("+");
		}
		if ((modifiers & SWT.BUTTON3) != 0) {
			buf.append("Button3");
			buf.append("+");
		}
		if (buf.length() > 0) {
			buf.setLength(buf.length()-1); 
		}
		return buf.toString();
	}

	/**
	 * Returns the mouse code text representation. 
	 * @return String
	 */
	String getMouseText() {
		String text = "";
		String mods = getModifiersText(mouseModifiers);
		if (mods != null && mods.length() > 0)
			text += mods + "+";
		if (mouseButton == SWT.NONE) return text;
		text += "button #" + mouseButton + "/" + mouseCount;
		return text;
	}

	/**
	 * The factory associates the action to be taken.
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return Window.resources.getString("MouseButtonEditor_Factory_description"); }
		
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
		dialog.shell.addKeyListener(this);
		dialog.shell.addMouseListener(this);
		dialog.setTitle(Window.resources.getString("MouseButtonEditor_title"));
		dialog.setBounds(x, y, 200, 100);
		dialog.open();
	}

	/**
	 * Sets actual keys from event.
	 * @param e the original mouse event
	 * @param released the flag that shows the key is released.
	 */
	private void setKeysFromEvent(MouseEvent e, boolean released) {
		if (released && mousePressed == 0) return;
		mouseModifiers = e.stateMask & (SWT.CTRL|SWT.SHIFT|SWT.ALT);
		mouseButton = SWT.NONE;
		mouseCount = 0;
		if (released) {
			if (mousePressed == e.button){
				mouseButton = e.button;
				mouseCount = e.count;
			}
		} else {
			mousePressed = e.button;
		}
		if (mouseButton == SWT.CTRL)
			mouseButton = SWT.NONE;
		if (mouseButton == SWT.ALT)
			mouseButton = SWT.NONE;
		if (mouseButton == SWT.SHIFT)
			mouseButton = SWT.NONE;
		dialog.labelKey.setText(MessageFormat.format(Window.resources.getString("MouseButtonEditor_label_new_key"), new Object[] {getMouseText()}));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		if (done) {
			int code = e.keyCode;
			if (code == SWT.CR && mouseButton != SWT.NONE) {
				dialog.close();
				editor.getWindow().startTransaction(editor, "MouseButtonEditor:keyPressed");
				try {
					node.setButton(mouseButton);
					node.setCount(mouseCount);
					node.setWithCtrl((mouseModifiers & SWT.CTRL) != 0);
					node.setWithAlt((mouseModifiers & SWT.ALT) != 0);
					node.setWithShift((mouseModifiers & SWT.SHIFT) != 0);
					node.setText(getMouseText());
				} finally {
					editor.getWindow().stopTransaction(false);
				}
			}
			else if (code == SWT.ESC) {
				dialog.close();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {
		setKeysFromEvent(e, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent e) {
		if (! done) {
			setKeysFromEvent(e, false);
			return;
		}
		if (e.stateMask == mouseModifiers && e.count > mouseCount) {
			setKeysFromEvent(e, false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseUp(MouseEvent e) {
		if (done) return;
		setKeysFromEvent(e, true);
		if (mouseButton != SWT.NONE && mouseButton == mousePressed) {
			done = true;
			dialog.labelMessage.setText(Window.resources.getString("MouseButtonEditor_label_message_enter"));
		}
	}
}
