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

import java.io.ObjectStreamException;

/**
 * The Mouse Event.
 */
public final class MouseEvent extends Event {
	private static final long serialVersionUID = 2906572108356310743L;

	/**
	 * The button.
	 */
	public int button;
	
	/**
	 * Count clicks.
	 */
	public int count;

	/**
	 * Resolve.
	 * @return Object
	 * @throws ObjectStreamException
	 */
	Object readResolve() throws ObjectStreamException {
		return this;
	}
}
