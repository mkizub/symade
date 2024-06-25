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

import kiev.gui.event.InputEvent;
import kiev.gui.swt.InputEventInfo;
import kiev.vlang.Env;

/**
 * Configuration is made by using owner implemented event bindings. That
 * means we have a separate DSL reside in <code>kiev.gui.event<code> package 
 * that we use to load events configuration from dump (or something similar). 
 * @see kiev.gui.Configuration 
 */
public final class Configuration extends kiev.gui.Configuration {

	/**
	 * The constructor.
	 */
	public Configuration(Env env) {
		super(env);
		editorBindingsDefault = loadBindings("kiev/gui/swt/bindings-editor.xml");
		infoBindingsDefault = loadBindings("kiev/gui/swt/bindings-info.xml");
		projectBindingsDefault = loadBindings("kiev/gui/swt/bindings-project.xml");
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#getModifierMask(kiev.gui.Configuration.Modifiers)
	 */
	@Override
	public int getModifierMask(Modifiers mods) {
		switch (mods){
		case SHIFT:
			return org.eclipse.swt.SWT.SHIFT;
		case CTRL:
			return org.eclipse.swt.SWT.CTRL;
		case ALT:
			return org.eclipse.swt.SWT.ALT;
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
	 * @see kiev.gui.Configuration#newInputEvent(int, int, int)
	 */
	@Override
	public InputEvent makeKeyboardInputEvent(int mask, int code) {
		return new InputEventInfo(mask, code);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.Configuration#newInputEvent(int, int)
	 */
	@Override
	public InputEvent makeMouseInputEvent(int mask, int count, int button) {
		return new InputEventInfo(mask, count, button);
	}
}



