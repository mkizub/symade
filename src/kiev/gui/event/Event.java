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

import java.io.Serializable;

/**
 * The Event.
 */
@SuppressWarnings({"serial"})
public abstract class Event implements Serializable {
	
	/**
	 * Zero initializer.
	 */
	public static final Event[] emptyArray = new Event[0];

	/**
	 * Is with Ctrl.
	 */
	public boolean withCtrl;
	
	/**
	 * Is with Alt.
	 */
	public boolean withAlt;
	
	/**
	 * Is with Shift.
	 */
	public boolean withShift;
}
