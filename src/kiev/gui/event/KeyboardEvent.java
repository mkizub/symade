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
 * The Keyboard Event.
 */
public final class KeyboardEvent extends Event {
	private static final long serialVersionUID = -2314950913928631315L;

	/**
	 * The key code.
	 */
	public int keyCode;

	/**
	 * Resolve.
	 * @return Object
	 * @throws ObjectStreamException
	 */
	Object readResolve() throws ObjectStreamException {
		return this;
	}
}
