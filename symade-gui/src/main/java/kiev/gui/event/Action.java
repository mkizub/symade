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
 * The Action.
 */
public final class Action extends Item {
	/**
	 * The serial.
	 */
	private static final long serialVersionUID = -3447830218698768972L;

	/**
	 * The name.
	 */
	public String name;
	
	/**
	 * The description.
	 */
	public String description;
	
	/**
	 * Is for pop-up menu.
	 */
	public boolean isForPopupMenu;
	
	/**
	 * The action class.
	 */
	public String actionClass;

	/**
	 * Resolve.
	 * @return Object
	 * @throws ObjectStreamException
	 */
	Object readResolve() throws ObjectStreamException {
		if (this.description != null) this.description = this.description.intern();
		if (this.actionClass != null) this.actionClass = this.actionClass.intern();
		return this;
	}
}
