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

import kiev.gui.UIActionFactory;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * This is a menu bar item.
 */
public final class UIActionMenuItem extends kiev.gui.UIActionMenuItem {
	
	/**
	 * The Menu item.
	 */
	private final MenuItem item;	
	
	/**
	 * The Constructor.
	 * @param menu the menu
	 * @param style the SWT style
	 * @param wnd the window
	 * @param text the text
	 * @param accelerator the accelerator key
	 * @param factory the UI action factory
	 */
	UIActionMenuItem(Menu menu, int style, Window wnd, String text, int accelerator, UIActionFactory factory) {
		super(wnd, factory);
		item = new MenuItem(menu, style);
		item.setText(text);
		item.setAccelerator(accelerator);
		item.addSelectionListener(wnd);
		item.setData(this);
		item.setEnabled(checkEnabled());
	}


	/**
	 * Returns menu item enable state.
	 * @return <code>true</code> if enabled or <code>false</code>
	 */
	boolean isEnabled(){
		return item.isEnabled();
	}
	
	/**
	 * Set enabled.
	 * @param enabled the enabled flag
	 */
	void setEnabled(boolean enabled){
		item.setEnabled(enabled);
	}
	
	
}
