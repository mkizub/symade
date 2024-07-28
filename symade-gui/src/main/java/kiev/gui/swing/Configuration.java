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

import kiev.gui.event.InputEvent;
import kiev.vlang.Env;

/**
 * The Configuration.
 * @see kiev.gui.Configuration
 */
public final class Configuration extends kiev.gui.Configuration {

	/**
	 * The constructor.
	 */
	public Configuration(Env env) {
		super(env);
		editorBindingsDefault = loadBindings("kiev/gui/swing/bindings-editor.xml");
		infoBindingsDefault = loadBindings("kiev/gui/swing/bindings-info.xml");
		projectBindingsDefault = loadBindings("kiev/gui/swing/bindings-project.xml");
		errorsBindingsDefault = loadBindings("kiev/gui/swing/bindings-errors.xml");
	}

	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#getModifierMask(kiev.gui.Configuration.Modifiers)
	 */
	@Override
	public int getModifierMask(Modifiers mods) {
		switch (mods){
		case SHIFT:
			return java.awt.event.InputEvent.SHIFT_DOWN_MASK;
		case CTRL:
			return java.awt.event.InputEvent.CTRL_DOWN_MASK;
		case ALT:
			return java.awt.event.InputEvent.ALT_DOWN_MASK;
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#makeInputEvent()
	 */
	@Override
	public InputEvent makeInputEvent() {
		return new InputEventInfo(null);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#makeKeyboardInputEvent(int, int)
	 */
	@Override
	public InputEvent makeKeyboardInputEvent(int mask, int code) {
		return new InputEventInfo(mask, code);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#makeMouseInputEvent(int, int, int)
	 */
	@Override
	public InputEvent makeMouseInputEvent(int mask, int count, int button) {
		return new InputEventInfo(mask, count, button);
	}
}



