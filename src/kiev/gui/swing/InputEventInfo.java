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
package kiev.gui.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public final class InputEventInfo implements kiev.gui.event.InputEvent {
	
	public static final int UNKNOWN_EVENT  = 0;
	public static final int KEYBOARD_EVENT = 1;
	public static final int MOUSE_EVENT    = 2; 
	
	private final int         kind;
	private final int         mask;
	private final int         code;
	private final EventObject evt;	// native event
	
	/** Keyboard event, modifiers + key-code */
	InputEventInfo(int mask, int code) {
		this.kind = KEYBOARD_EVENT;
		this.mask = mask;
		this.code = code;
		this.evt = null;
	}
	/** Mouse event, modifiers + buttons + x + y */
	InputEventInfo(int mask, int count, int button) {
		this.kind = MOUSE_EVENT;
		this.mask = mask;
		this.code = (count << 30) | button;
		this.evt = null;
	}
	/** NativeEvent, keyboard or mouse */
	InputEventInfo(EventObject evt) {
		this.evt = evt;
		if (evt instanceof KeyEvent) {
			KeyEvent k = (KeyEvent)evt;
			this.kind = KEYBOARD_EVENT;
			this.mask = k.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
			this.code = k.getKeyCode();
		}
		else if (evt instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)evt;
			this.kind = MOUSE_EVENT;
			this.mask = m.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
			int buttons = m.getModifiers() & (MouseEvent.BUTTON1_MASK | MouseEvent.BUTTON2_MASK | MouseEvent.BUTTON3_MASK);
			this.code = (m.getClickCount() << 30) | buttons;
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
	
	public EventObject getNativeEvent() {
		return this.evt;
	}
	
	public String toString() {
		String mod = InputEvent.getModifiersExText(mask);
		if (kind == KEYBOARD_EVENT) {
			if (mod == null || mod.length() == 0)
				return "keybr: "+KeyEvent.getKeyText(code);
			return "keybr: "+mod+"+"+KeyEvent.getKeyText(code);
		}
		else if (kind == MOUSE_EVENT) {
			return "mouse: "+(code >> 30)+" "+MouseEvent.getMouseModifiersText(code);
		}
		else {
			return "unknowns event:";
		}
	}

	public int getX() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).getX();
		return -1;
	}
	public int getY() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).getY();
		return -1;
	}

	public boolean isKeyboardEvent() {
		return this.kind == KEYBOARD_EVENT;
	}
	public boolean isMouseEvent() {
		return this.kind == MOUSE_EVENT;
	}
}
