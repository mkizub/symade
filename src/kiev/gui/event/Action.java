/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.gui.event;

import java.io.ObjectStreamException;

public final class Action extends Item {
	public String description;
	public boolean isForPopupMenu;
	public String actionClass;

	Object readResolve() throws ObjectStreamException {
		if (this.description != null) this.description = this.description.intern();
		if (this.actionClass != null) this.actionClass = this.actionClass.intern();
		return this;
	}
}
