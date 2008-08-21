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
package kiev.gui;

import kiev.fmt.DrawTerm;
import kiev.gui.event.BindingSet;
import kiev.gui.event.EventActionMap;
import kiev.gui.IBgFormatter;
import kiev.vtree.ScalarPtr;

public class UIManager {
	
	public static final boolean SWT = kiev.Kiev.run_gui_swt;
	public static final boolean SWING = kiev.Kiev.run_gui_swing;

	public static IWindow newWindow(){
		if (SWT)
			return new kiev.gui.swt.Window();
		else
			return new kiev.gui.swing.Window();
	}
	
	public static IBgFormatter getBgFormatter(UIView view) {
		IBgFormatter f;
		if (SWT) {
			f = new kiev.gui.swt.BgFormatter(view);
		} else {
			f = new kiev.gui.swing.BgFormatter(view);
		}
		return f;
	}

	public static void attachEventBindings(BindingSet bs) {
		kiev.gui.swing.Configuration.attachBindings(bs);
	}
	public static void resetEventBindings() {
		kiev.gui.swing.Configuration.resetBindings();
	}
	public static EventActionMap getUIActions(UIView uiv) {
		if (SWT) {
			if (uiv instanceof Editor)
				return kiev.gui.swt.Configuration.getEditorActionMap();
			if (uiv instanceof ProjectView)
				return kiev.gui.swt.Configuration.getProjectViewActionMap();
			if (uiv instanceof InfoView)
				return kiev.gui.swt.Configuration.getInfoViewActionMap();
		} else {
			if (uiv instanceof Editor)
				return kiev.gui.swing.Configuration.getEditorActionMap();
			if (uiv instanceof ProjectView)
				return kiev.gui.swing.Configuration.getProjectViewActionMap();
			if (uiv instanceof InfoView)
				return kiev.gui.swing.Configuration.getInfoViewActionMap();
		}
		return new EventActionMap();
	}
	
	public static Runnable newIntEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
		return new kiev.gui.swing.IntEditor(editor, dr_term, pattr);
	}

	public static Runnable newTextEditor(Editor editor, DrawTerm dr_term, ScalarPtr pattr){
		return new kiev.gui.swing.TextEditor(editor, dr_term, pattr);
	}
	
	public static Object getClipboardContent() {
		if (SWT) return kiev.gui.swt.Clipboard.getClipboardContent();
		return kiev.gui.swing.Clipboard.getClipboardContent();
	}

	public static void setClipboardContent(Object obj) {
		if (SWT) kiev.gui.swt.Clipboard.setClipboardContent(obj);
		kiev.gui.swing.Clipboard.setClipboardContent(obj);
	}

	public static void doGUIBeep() {
		kiev.gui.swing.Configuration.doGUIBeep();
	}

	public static IPopupMenuPeer newPopupMenu(IUIView view, IPopupMenuListener listener) {
		if (SWT)
			return new kiev.gui.swt.PopupMenu(view.getViewPeer(), listener);
		return new kiev.gui.swing.PopupMenu(view.getViewPeer(), listener);
	}
}
