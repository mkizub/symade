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

import kiev.fmt.common.Draw_ATextSyntax;
import kiev.gui.ICanvas;

/**
 * Project view is on the right side and shows as a project tree.
 */
public class ProjectView extends kiev.gui.ProjectView {

	/**
	 * The constructor.
	 * @param window the window
	 * @param view_canvas the canvas
	 * @param syntax the syntax
	 */
	public ProjectView(Window window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
	}

}
