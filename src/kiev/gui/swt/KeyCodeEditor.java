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
import kiev.fmt.evt.KeyboardEvent;
import kiev.gui.Editor;
import kiev.gui.UIAction;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * This editor used to enter key codes into the editing element 
 * and save its textual representation via description.
 */
public final class KeyCodeEditor implements UIAction, KeyListener {
	
	/**
	 * The editor parameter.
	 */
	private final Editor editor;
	
	/**
	 * The current drawable element parameter.
	 */
	private final DrawTerm cur_elem;
	
	/**
	 * The node for keyboard events in bindings DSL.
	 */
	private final KeyboardEvent	node;
	
	/**
	 * The dialog used to enter/show event codes and their textual representation.
	 */
	private Dialog dialog;
	
	/**
	 * Is done.
	 */
	private boolean done;
	
	/**
	 * The key Pressed.
	 */
	private int keyPressed;
	
	/**
	 * The key Code.
	 */
	private int keyCode;
	
	/**
	 * The key Modifiers.
	 */
	private int keyModifiers;

	/**
	 * The dialog to accept codes.
	 */
	private final class Dialog {
		
		/**
		 * The shell.
		 */
		private final Shell shell;
		
		/**
		 * The label Key.
		 */
		private Label	labelKey;
		
		/**
		 * The label Message.
		 */
		private Label	labelMessage;
		
		/**
		 * The down Button.
		 */
		@SuppressWarnings("unused")
		private Button down;
		
		/**
		 * The constructor.
		 * @param parent the parent
		 */
		public Dialog(Shell parent) {
			shell = new Shell(parent, SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.SYSTEM_MODAL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 1;
			shell.setLayout(layout);
			shell.setText(Window.resources.getString("KeyCodeEditor_title"));
			shell.addShellListener(new ShellAdapter(){
				public void shellClosed(ShellEvent e) {
					e.doit = false;
					shell.setVisible(false);
				}
			});

			labelMessage = new Label(shell, SWT.LEFT);
			labelMessage.setAlignment(SWT.CENTER);

			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 200;
			labelMessage.setLayoutData(gridData);

			labelKey = new Label(shell, SWT.LEFT);
			labelKey.setAlignment(SWT.CENTER);

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint = 200;
			labelKey.setLayoutData(gridData);
			shell.pack();
		}

		/**
		 * Initializes initial control values.  
		 */
		private void init(){
			done = false; 
			keyPressed = 0;			
			labelMessage.setText(Window.resources.getString("KeyCodeEditor_label_message"));
			labelKey.setText(MessageFormat.format(Window.resources.getString("KeyCodeEditor_label_key"), new Object[] {getKeyText()}));		
		}

		/**
		 * Sets the title.
		 * @param title
		 */
		public void setTitle(String title){
			shell.setText(title);
		}

		/**
		 * Sets the bounds.
		 * @param x the X coordinate.
		 * @param y the Y coordinate.
		 * @param w the width.
		 * @param h the height.
		 */
		public void setBounds(int x, int y, int w, int h){
			shell.setBounds(x, y, w, h);
		}

		/**
		 * Opens the dialog.
		 */
		public void open() {
			init();
			if (shell.isVisible()) shell.setFocus();
			else shell.open();			
		}
		
		/**
		 * We just make the dialog invisible when it close.
		 */
		public void close() {
			shell.setVisible(false);
		}
	}

	/**
	 * The constructor.
	 * @param shell the shell
	 */
	KeyCodeEditor(Shell shell){
		this.editor = null;
		this.cur_elem = null;
		this.node = new KeyboardEvent();
		dialog = new Dialog(shell);
		dialog.shell.addKeyListener(this);
		dialog.setTitle(Window.resources.getString("KeyCodeEditor_title"));
		dialog.setBounds(100, 100, 200, 100);
	}

	/**
	 * The constructor.
	 * @param editor the editor
	 * @param cur_elem the drawable
	 * @param node the node
	 */
	KeyCodeEditor(Editor editor, DrawTerm cur_elem, KeyboardEvent node) {
		this.editor = editor;
		this.cur_elem = cur_elem;
		this.node = node;
		this.keyCode = node.getKeyCode();
		if (node.isWithCtrl())
			this.keyModifiers |= SWT.CTRL;
		if (node.isWithAlt())
			this.keyModifiers |= SWT.ALT;
		if (node.isWithShift())
			this.keyModifiers |= SWT.SHIFT;
		dialog = new Dialog(Window.getShell());
	}

	/**
	 * Returns a text representation of the codes.
	 * @param modifiers the modifiers
	 * @return the modifiers text
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
	 * Get Key Text.
	 * @return The string representation of key code.
	 */
	private String getKeyText() {
		String text = "";
		String mods = getModifiersText(keyModifiers);
		if (mods != null && mods.length() > 0)
			text += mods + "+";
		if (keyCode == SWT.NONE)
			return text;
		if (keyCode == SWT.CTRL)
			return text;
		if (keyCode == SWT.ALT)
			return text;
		if (keyCode == SWT.SHIFT)
			return text;
		text += (char)keyCode;
		return text;
	}

	/**
	 * Key Code Editor UI Action Factory. 
	 */
	public final static class Factory implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return Window.resources.getString("KeyCodeEditor_Factory_description"); }
		
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
			if (dt == null || context.node == null)	return null;
			if (! (dt.drnode instanceof KeyboardEvent)) return null;
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
		dialog.shell.addKeyListener(this);
		dialog.setTitle(Window.resources.getString("KeyCodeEditor_title"));
		dialog.setBounds(x, y, 200, 100);
		dialog.open();
	}

	/**
	 * Set Keys From Event.
	 * @param e Key Event
	 * @param released released
	 */
	private void setKeysFromEvent(KeyEvent e, boolean released) {
		if (released && keyPressed == 0) return;
		keyModifiers = e.stateMask & (SWT.CTRL|SWT.SHIFT|SWT.ALT);
		keyCode = SWT.NONE;
		if (released) {
			if (keyPressed == e.keyCode) keyCode = e.keyCode;
		} else {
			keyPressed = e.keyCode;
		}
		if (keyCode == SWT.CTRL)
			keyCode = SWT.NONE;
		if (keyCode == SWT.ALT)
			keyCode = SWT.NONE;
		if (keyCode == SWT.SHIFT)
			keyCode = SWT.NONE;
		dialog.labelKey.setText(MessageFormat.format(Window.resources.getString("KeyCodeEditor_label_new_key"), new Object[] {getKeyText()}));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		if (done) {
			int code = e.keyCode;
			if (code == SWT.CR && keyCode != SWT.NONE) {
				dialog.close();
				editor.getWindow().startTransaction(editor, "KeyCodeEditor:keyPressed");
				try {
					node.setKeyCode(keyCode);
					node.setWithCtrl((keyModifiers & SWT.CTRL) != 0);
					node.setWithAlt((keyModifiers & SWT.ALT) != 0);
					node.setWithShift((keyModifiers & SWT.SHIFT) != 0);
					node.setText(getKeyText());
				} finally {
					editor.getWindow().stopTransaction(false);
				}
			}
			else if (code == SWT.ESC) {
				dialog.close();
			}
		} else {
			setKeysFromEvent(e, false);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		if (done) return;
		setKeysFromEvent(e, true);
		if (keyCode != SWT.NONE && keyCode == keyPressed) {
			done = true;
			dialog.labelMessage.setText(Window.resources.getString("KeyCodeEditor_label_message_enter"));
		}

	}
}
