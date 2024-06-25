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
 * Pop-up Menu Peer.
 */
public interface IPopupMenuPeer extends ISubMenuPeer {

	/**
	 * Shows pop-up menu at the specified position. 
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	public void showAt(int x, int y);
	
	/**
	 * Remove/destroy the menu.
	 */
	public void remove();
	
}
