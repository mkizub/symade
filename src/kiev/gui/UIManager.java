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

import kiev.gui.event.EventActionMap;
import kiev.WorkerThreadGroup;
import kiev.vlang.Env;

/**
 * UImanager used to manage different implementations of the GUI.
 */
public class UIManager {
	
	/**
	 * The SWT switch.
	 */
	public static final boolean SWT = kiev.Kiev.run_gui_swt;
	
	/**
	 * The Swing switch.
	 */
	public static final boolean SWING = kiev.Kiev.run_gui_swing;
	
	/**
	 * The configuration.
	 */
	private static Configuration cfg;
	
	/**
	 * Returns the new instance of <code>IWindow</code>.
	 * @param env the environment
	 * @return <code>IWindow</code>
	 * @see kiev.gui.IWindow
	 */
	public static IWindow newWindow(WorkerThreadGroup thrg){
		if (SWT) {
			cfg = new kiev.gui.swt.Configuration(thrg.getEnv());
			return new kiev.gui.swt.Window(thrg);
		} else {
			cfg = new kiev.gui.swing.Configuration(thrg.getEnv());
			return new kiev.gui.swing.Window(thrg);
		}
	}
	
	/**
	 * Initializes configuration with code bindings.
	 * @param bs the binding set
	 */
	public static void attachEventBindings(kiev.fmt.evt.BindingSet bs) {
		cfg.attachBindings(bs);
	}
	
	/**
	 * Reset configuration bindings.
	 */
	public static void resetEventBindings() {
		cfg.resetBindings();
	}
	
	/**
	 * Returns <code>EventActionMap</code> of UI actions.
	 * @param uiv the view
	 * @return <code>EventActionMap</code> or <code>null</code> if no instances
	 * of <code>UIView</code> found.
	 */
	public static EventActionMap getUIActions(IUIView uiv) {
		if (SWT) {
			if (uiv instanceof Editor)
				return cfg.getEditorActionMap();
			if (uiv instanceof ProjectView)
				return cfg.getProjectViewActionMap();
			return cfg.getInfoViewActionMap();
		} else {
			if (uiv instanceof Editor)
				return cfg.getEditorActionMap();
			if (uiv instanceof ProjectView)
				return cfg.getProjectViewActionMap();
			if (uiv instanceof ErrorsView)
				return cfg.getErrorsViewActionMap();
			return cfg.getInfoViewActionMap();
		}
	}
	
	/**
	 * Returns the object put into clipboard.
	 * @return <code>Object</code>
	 * @see kiev.gui.swt.Clipboard#getClipboardContent
	 * @see kiev.gui.swing.Clipboard#getClipboardContent
	 */
	public static Object getClipboardContent() {
		if (SWT) return kiev.gui.swt.Clipboard.getClipboardContent();
		return kiev.gui.swing.Clipboard.getClipboardContent();
	}

	/**
	 * Put object into clipboard.
	 * @param obj the object
	 * @see kiev.gui.swt.Clipboard#setClipboardContent
	 * @see kiev.gui.swing.Clipboard#setClipboardContent
	 */
	public static void setClipboardContent(Object obj) {
		if (SWT) kiev.gui.swt.Clipboard.setClipboardContent(obj);
		else kiev.gui.swing.Clipboard.setClipboardContent(obj);
	}

	/**
	 * Returns new instance of <code>IFileDialog</code>.
	 * @param window the window
	 * @param type the type
	 * @return IFileDialog
	 */
	public static IFileDialog newFileDialog(IWindow window, int type){
		if (SWT) return new kiev.gui.swt.FileDialog(window, type);
		return new kiev.gui.swing.FileDialog(window, type);
	}
	
}
