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
package kiev.gui.swing;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * Input Event Info.
 */
public final class InputEventInfo implements kiev.gui.event.InputEvent {
	
	/**
	 * Event types.
	 */
	public enum EventType {UNKNOWN_EVENT, KEYBOARD_EVENT, MOUSE_EVENT};
	
	/**
	 * The kind.
	 */
	private final int kind;
	
	/**
	 * The mask.
	 */
	private final int mask;
	
	/**
	 * The code.
	 */
	private final int code;
	
	/**
	 * The native event.
	 */
	private final EventObject evt;	 
	
	/**
	 * Keyboard event, modifiers + key-code.
	 * @param mask the mask
	 * @param code the code
	 */
	public InputEventInfo(int mask, int code) {
		this.kind = EventType.KEYBOARD_EVENT.ordinal();
		this.mask = mask;
		this.code = code;
		this.evt = null;
	}
	
	/**
	 * Mouse event, modifiers + buttons + x + y.
	 * @param mask the mask
	 * @param count the count click
	 * @param button the button code
	 */
	public InputEventInfo(int mask, int count, int button) {
		this.kind = EventType.MOUSE_EVENT.ordinal();
		this.mask = mask;
		this.code = (count << 30) | button;
		this.evt = null;
	}
	
	/**
	 * NativeEvent, keyboard or mouse.
	 * @param evt the event object
	 */
	public InputEventInfo(EventObject evt) {
		this.evt = evt;
		if (evt instanceof KeyEvent) {
			KeyEvent k = (KeyEvent)evt;
			this.kind = EventType.KEYBOARD_EVENT.ordinal();
			this.mask = k.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK);
			this.code = k.getKeyCode();
		}
		else if (evt instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)evt;
			this.kind = EventType.MOUSE_EVENT.ordinal();
			this.mask = m.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK);
			this.code = (m.getClickCount() << 30) | m.getButton();
		}
		else {
			this.kind = EventType.UNKNOWN_EVENT.ordinal();
			this.mask = 0;
			this.code = 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() { return kind ^ mask ^ code; }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof InputEventInfo) {
			InputEventInfo ie = (InputEventInfo)obj;
			return this.kind == ie.kind && this.mask == ie.mask && ie.code == code;
		}
		return false;
	}
	
	/**
	 * Return the event object.
	 * @return EventObject
	 */
	public EventObject getNativeEvent() {
		return this.evt;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String mod = InputEvent.getModifiersExText(mask);
		if (kind == EventType.KEYBOARD_EVENT.ordinal()) {
			if (mod == null || mod.length() == 0)
				return "keybr: "+KeyEvent.getKeyText(code);
			return "keybr: "+mod+"+"+KeyEvent.getKeyText(code);
		}
		else if (kind == EventType.MOUSE_EVENT.ordinal()) {
			return "mouse: "+(code >> 30)+" "+MouseEvent.getMouseModifiersText(code);
		}
		else {
			return "unknowns event:";
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.event.InputEvent#getX()
	 */
	public int getX() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).getX();
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.event.InputEvent#getY()
	 */
	public int getY() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).getY();
		return -1;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.event.InputEvent#isKeyboardEvent()
	 */
	public boolean isKeyboardEvent() {
		return this.kind == EventType.KEYBOARD_EVENT.ordinal();
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.event.InputEvent#isMouseEvent()
	 */
	public boolean isMouseEvent() {
		return this.kind == EventType.MOUSE_EVENT.ordinal();
	}

	public boolean isKeyboardTyping() {
		if (!isKeyboardEvent())
			return false;
		KeyEvent evt = (KeyEvent)this.evt;
		if ((evt.getModifiersEx() & ~InputEvent.SHIFT_DOWN_MASK) != 0) 
			return false;
		int code = evt.getKeyCode();
		if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE)
			return true;
		int ch = evt.getKeyChar();
		if (ch == KeyEvent.CHAR_UNDEFINED) return false;
		if (ch < 32 || ch == 127) return false;
		return true;
	}
}
