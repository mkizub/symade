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

import javax.swing.DefaultButtonModel;
import javax.swing.JMenuItem;

import kiev.gui.UIActionFactory;

/**
 * UI Action Menu Item.
 */
public class UIActionMenuItem extends kiev.gui.UIActionMenuItem {
	
	/**
	 * The menu item.
	 */
	protected MenuItem item;
	
	/**
	 * Internal implementation of Menu Item. 
	 */
	@SuppressWarnings("serial")
	class MenuItem  extends JMenuItem {
		
		/**
		 * The owner.
		 */
		final UIActionMenuItem owner;
		
		/**
		 * Menu Item.
		 * @param text the text
		 * @param mnemonic the mnemonic
		 */
		MenuItem(UIActionMenuItem owner, String text, int mnemonic) {
			super(text, mnemonic);
			this.owner = owner;
		}
		
		/* (non-Javadoc)
		 * @see java.awt.Component#isEnabled()
		 */
		@Override
		public boolean isEnabled() {
			if (!super.isEnabled()) return false;
			if (!this.isShowing()) return true;
			return checkEnabled();
		}

	}

	/**
	 * UI Action Button Model.
	 */
	@SuppressWarnings("serial")
	class ButtonModel extends DefaultButtonModel {
		
		/* (non-Javadoc)
		 * @see javax.swing.DefaultButtonModel#isEnabled()
		 */
		@Override
		public boolean isEnabled() { return item.isEnabled();	}
	}
	
	/**
	 * The Constructor.
	 * @param wnd the window
	 * @param text the text
	 * @param mnemonic the mnemonic
	 * @param factory the factory
	 */
	UIActionMenuItem(Window wnd, String text, int mnemonic, UIActionFactory factory) {
		super(wnd, factory);
		item = new MenuItem(this, text, mnemonic);
		item.setModel(new ButtonModel());
		item.getAccessibleContext().setAccessibleDescription(factory.getDescr());
		item.addActionListener(wnd);
	}
		
}
