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
package kiev.gui.swt;

import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;

/**
 * The event representation of the GUI input. The events from SWT 
 * implementation are captured and transformed here.
 * @see kiev.gui.event.InputEvent
 */
public final class InputEventInfo implements kiev.gui.event.InputEvent {
	
	/**
	 * Event types.
	 */
	public enum EventType {UNKNOWN_EVENT, KEYBOARD_EVENT, MOUSE_EVENT};
	
	/**
	 * This is kind an event (keyboard or mouse or ...).
	 */
	private final int kind;
	
	/**
	 * The mask used.
	 */
	private final int mask;
	
	/**
	 * The code is taken from native widgets event.
	 */
	private final int code;
	
	/**
	 * The native event.
	 */
	private final TypedEvent  evt;	 
	
	/** 
	 * The constructor.
	 * Keyboard event, modifiers + key-code 
	 */
	public InputEventInfo(int mask, int code) {
		this.kind = EventType.KEYBOARD_EVENT.ordinal();
		this.mask = mask;
		this.code = code;
		this.evt = null;
	}
	
	/** 
	 * The constructor.
	 * Mouse event, modifiers + buttons + x + y 
	 */
	public InputEventInfo(int mask, int count, int button) {
		this.kind = EventType.MOUSE_EVENT.ordinal();
		this.mask = mask;
		this.code = (count << 30) | button;
		this.evt = null;
	}
	
	/** 
	 * The constructor.
	 * NativeEvent, keyboard or mouse 
	 */
	public InputEventInfo(TypedEvent evt) {
		this.evt = evt;
		if (evt instanceof KeyEvent) {
			KeyEvent k = (KeyEvent)evt;
			this.kind = EventType.KEYBOARD_EVENT.ordinal();
			this.mask = k.stateMask;
			this.code = k.keyCode;
		}
		else if (evt instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)evt;
			this.kind = EventType.MOUSE_EVENT.ordinal();
			this.mask = m.stateMask;
			int buttons = m.button;
			this.code = (m.count << 30) | buttons;
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
	@Override
	public int hashCode() { return kind ^ mask ^ code; }
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InputEventInfo) {
			InputEventInfo ie = (InputEventInfo)obj;
			return this.kind == ie.kind && this.mask == ie.mask && ie.code == code;
		}
		return false;
	}
	
	/**
	 * Returns the event that is native to the framework.
	 * @return the event
	 */
	public TypedEvent getNativeEvent() {
		return this.evt;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String mod = Integer.toString(mask);
		if (kind == EventType.KEYBOARD_EVENT.ordinal()) {
			if (mod == null || mod.length() == 0)
				return "keybr: "+code;
			return "keybr: "+mod+"+"+code;
		}
		else if (kind == EventType.MOUSE_EVENT.ordinal()) {
			return "mouse: "+(code >> 30)+" "+code;
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
			return ((MouseEvent)this.evt).x;
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.event.InputEvent#getY()
	 */
	public int getY() {
		if (this.evt instanceof MouseEvent)
			return ((MouseEvent)this.evt).y;
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
		return false;
	}
}
