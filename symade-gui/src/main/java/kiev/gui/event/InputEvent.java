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
package kiev.gui.event;

/**
 * Input Event.
 */
public interface InputEvent {

	/**
	 * Is Keyboard Event.
	 * @return boolean
	 */
	public boolean isKeyboardEvent();
	
	/**
	 * Is Mouse Event.
	 * @return boolean
	 */
	public boolean isMouseEvent();
	
	/**
	 * Get X coordinate.
	 * @return int
	 */
	public int getX();
	
	/**
	 * Get Y cooordinate.
	 * @return int
	 */
	public int getY();

	/**
	 * Is Keyboard Typing (non-control).
	 * @return boolean
	 */
	public boolean isKeyboardTyping();
	
}
