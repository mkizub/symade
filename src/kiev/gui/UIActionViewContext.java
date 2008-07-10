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
import kiev.fmt.Drawable;
//import kiev.gui.swing.Window;
import kiev.vtree.ANode;

public class UIActionViewContext {
	public final IWindow		wnd;
	public final UIView		ui;
	public final InfoView	uiv;
	public final Editor		editor;
	public final DrawTerm	dt;
	public final ANode		node;
	public Drawable			dr;
	
	/**
	 * Constructor of UIActionViewContext.
	 * @param wnd
	 * @param ui
	 */
	public UIActionViewContext(IWindow wnd, UIView ui) {
		this.wnd = wnd;
		this.ui = ui;
		if (ui instanceof InfoView) {
			this.uiv = (InfoView)ui;
		} else {
			this.uiv = null;
		}
		if (ui instanceof Editor) {
			this.editor = (Editor)ui;
			this.dt = editor.getCur_elem().dr;
			this.node = editor.getCur_elem().node;
			this.dr = dt;
		} else {
			this.editor = null;
			this.dt = null;
			this.node = null;
			this.dr = null;
		}
	}
	public UIActionViewContext(IWindow wnd, Editor editor, Drawable dr) {
		this.wnd = wnd;
		this.ui = editor;
		this.uiv = editor;
		this.editor = editor;
		this.dt = editor.getCur_elem().dr;
		this.node = editor.getCur_elem().node;
		this.dr = dr;
	}
}
