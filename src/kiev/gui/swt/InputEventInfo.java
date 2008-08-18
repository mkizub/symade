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
package kiev.gui.swt;

import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;

public final class InputEventInfo implements kiev.gui.event.InputEvent {
	
	public static final int UNKNOWN_EVENT  = 0;
	public static final int KEYBOARD_EVENT = 1;
	public static final int MOUSE_EVENT    = 2; 
	
	private final int         kind;
	private final int         mask;
	private final int         code;
	private final TypedEvent  evt;	// native event
	
	/** Keyboard event, modifiers + key-code */
	public InputEventInfo(int mask, int code) {
		this.kind = KEYBOARD_EVENT;
		this.mask = mask;
		this.code = code;
		this.evt = null;
	}
	/** Mouse event, modifiers + buttons + x + y */
	public InputEventInfo(int mask, int count, int button) {
		this.kind = MOUSE_EVENT;
		this.mask = mask;
		this.code = (count << 30) | button;
		this.evt = null;
	}
	/** NativeEvent, keyboard or mouse */
	public InputEventInfo(TypedEvent evt) {
		this.evt = evt;
		if (evt instanceof KeyEvent) {
			KeyEvent k = (KeyEvent)evt;
			this.kind = KEYBOARD_EVENT;
			this.mask = k.stateMask;
			this.code = k.keyCode;
		}
		else if (evt instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)evt;
			this.kind = MOUSE_EVENT;
			this.mask = m.stateMask;
			int buttons = m.button;
			this.code = (m.count << 30) | buttons;
		}
		else {
			this.kind = UNKNOWN_EVENT;
			this.mask = 0;
			this.code = 0;
		}
	}
	
	public int hashCode() { return kind ^ mask ^ code; }
	
	public boolean equals(Object obj) {
		if (obj instanceof InputEventInfo) {
			InputEventInfo ie = (InputEventInfo)obj;
			return this.kind == ie.kind && this.mask == ie.mask && ie.code == code;
		}
		return false;
	}
	
	public TypedEvent getNativeEvent() {
		return this.evt;
	}
	
	public String toString() {
		String mod = Integer.toString(mask);
		if (kind == KEYBOARD_EVENT) {
			if (mod == null || mod.length() == 0)
				return "keybr: "+code;
			return "keybr: "+mod+"+"+code;
		}
		else if (kind == MOUSE_EVENT) {
			return "mouse: "+(code >> 30)+" "+code;
		}
		else {
			return "unknowns event:";
		}
	}

	public int getX() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).x;
		return -1;
	}
	public int getY() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).y;
		return -1;
	}

	public boolean isKeyboardEvent() {
		return this.kind == KEYBOARD_EVENT;
	}
	public boolean isMouseEvent() {
		return this.kind == MOUSE_EVENT;
	}
}
