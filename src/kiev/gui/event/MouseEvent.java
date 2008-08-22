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

public final class MouseEvent extends Event {
	public int button;
	public int count;
	public boolean withCtrl;
	public boolean withAlt;
	public boolean withShift;

	Object readResolve() throws ObjectStreamException {
		return this;
	}
}
