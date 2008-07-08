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
package kiev.gui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class InputEventInfo {
	private final int mask;
	private final int code;
	public InputEventInfo(int mask, int code) {
		this.mask = mask;
		this.code = code;
	}
	public int hashCode() { return mask ^ code; }
	public boolean equals(Object ie) {
		if (ie instanceof InputEventInfo)
			return this.mask == ((InputEventInfo)ie).mask && ((InputEventInfo)ie).code == code;
		return false;
	}
	public String toString() {
		String mod = InputEvent.getModifiersExText(mask);
		if (mod == null || mod.length() == 0)
			return KeyEvent.getKeyText(code);
		return mod+"+"+KeyEvent.getKeyText(code);
	}
}
