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

import kiev.Kiev;

/**
 * UI Action Menu Item.
 */
public abstract class UIActionMenuItem {

	/**
	 * The UI Action Factory.
	 */
	protected final UIActionFactory factory;

	/**
	 * The Window.
	 */
	protected final Window wnd;

	/**
	 * The Constructor.
	 * @param wnd the window
	 * @param factory the UI action factory
	 */
	public UIActionMenuItem(Window wnd, UIActionFactory factory) {
		this.wnd = wnd;
		this.factory = factory;
	}
	
	/**
	 * Check if it's enabled.
	 * @return boolean
	 */
	public boolean checkEnabled() {
		Kiev.setSemContext(wnd.currentEditorThreadGroup.semantic_context);
		try {
			return factory != null && factory.getAction(new UIActionViewContext(wnd, null, wnd.getCurrentView())) != null;
		} finally {
			Kiev.setSemContext(null);
		}
	}

	/**
	 * @return the factory
	 */
	public UIActionFactory getFactory() {
		return factory;
	}

}
