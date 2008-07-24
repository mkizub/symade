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

import javax.swing.JMenuItem;

import kiev.gui.IWindow;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

public class UIActionMenuItem extends JMenuItem {
	private static final long serialVersionUID = 3759102136127901645L;
	final IWindow wnd;
	final UIActionFactory factory;
	
	/**
	 * Constructor of UIActionMenuItem.
	 * @param wnd
	 * @param text
	 * @param mnemonic
	 * @param factory
	 */
	UIActionMenuItem(IWindow wnd, String text, int mnemonic, UIActionFactory factory) {
		super(text, mnemonic);
		setModel(new UIActionButtonModel());
		this.wnd = wnd;
		this.factory = factory;
		this.getAccessibleContext().setAccessibleDescription(factory.getDescr());
		this.addActionListener((Window)wnd);
	}
	
	public boolean isEnabled() {
		if (factory == null || !super.isEnabled()) return false;
		return factory.getAction(new UIActionViewContext(wnd, null, wnd.getCurrentView())) != null;
	}
	
	class UIActionButtonModel extends javax.swing.DefaultButtonModel {
		private static final long serialVersionUID = 1322172964914939491L;

		public boolean isEnabled() {
			try {
				if (UIActionMenuItem.this == null || factory == null || !super.isEnabled()) return false;
				return factory.getAction(new UIActionViewContext(wnd, null, wnd.getCurrentView())) != null;
			} catch (NullPointerException e) { return false; }
		}
	}
}
