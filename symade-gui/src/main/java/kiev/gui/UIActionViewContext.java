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

import kiev.fmt.DrawTerm;
import kiev.gui.event.InputEvent;
import kiev.vtree.INode;

/**
 * UI Action View Context.
 */
public class UIActionViewContext {
	
	/** The Window */
	public final IWindow	wnd;
	
	/** The Input Event. */
	public final InputEvent	evt;
	
	/** The UI View. */
	public final IUIView ui;
	
	/** The Editor (null or same as ui). */
	public final Editor editor;
	
	/** The current DrawTerm (in the editor or ui). */
	public final DrawTerm	dt;
	
	/** The node for this action. */
	public final INode node;
	
	/** The action point for the editor action. */
	public final ActionPoint ap;
	
	/**
	 * The Constructor.
	 * @param wnd the Window
	 * @param ui the UI View
	 */
	public UIActionViewContext(IWindow wnd, InputEvent evt, IUIView ui) {
		this.wnd = wnd;
		this.evt = evt;
		this.ui = ui;
		if (ui instanceof Editor) {
			this.editor = (Editor)ui;
			this.dt = editor.getDrawTerm();
			this.node = editor.getSelectedNode();
			this.ap = editor.getActionPoint();
		}
		else if (ui instanceof ProjectView){
			this.editor = null;			
			ProjectView pv = (ProjectView)ui;
			this.dt = pv.getViewPeer().getCurrent();
			this.node = dt == null ? null : dt.drnode;
			this.ap = null;
		}
		else {
			this.editor = null;
			this.dt = null;
			this.node = null;
			this.ap = null;
		}
	}
}
