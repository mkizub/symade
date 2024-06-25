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

/**
 * Listener of the pop-up menu.
 */
public interface IPopupMenuListener {

	/**
	 * Called when pop-up menu hidden.
	 */
	public void popupMenuCanceled();
	
	/**
	 * Called when pop-up menu selected.
	 * @param item the menu item
	 */
	public void popupMenuExecuted(IMenuItem item);
	
}
