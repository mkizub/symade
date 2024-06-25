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

import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.TextAction;

import kiev.gui.IMenu;
import kiev.gui.IMenuItem;
import kiev.gui.IPopupMenuListener;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.ISubMenuPeer;

@SuppressWarnings("serial")
public class PopupMenu extends JPopupMenu implements IPopupMenuPeer, PopupMenuListener {

	/**
	 * The component.
	 */
	private final Canvas component;
	
	/**
	 * The listener.
	 */
	private IPopupMenuListener listener;

	/**
	 * Sub menu.
	 */
	class SubMenu extends JMenu implements ISubMenuPeer {
		
		/**
		 * The constructor.
		 * @param text the text
		 */
		SubMenu(String text) {
			super(text);
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.ISubMenuPeer#addItem(kiev.gui.IMenuItem)
		 */
		public void addItem(IMenuItem item) {
			this.add(new PopupMenuItem(item));
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.ISubMenuPeer#newSubMenu(java.lang.String)
		 */
		public ISubMenuPeer newSubMenu(String text) {
			SubMenu m = new SubMenu(text);
			this.add(m);
			return m;
		}
	}
	
	/**
	 * The constructor.
	 * @param peer the UI view peer
	 */
	public PopupMenu(Canvas peer) {
		this.component = peer;
		this.addPopupMenuListener(this);
	}
	
	/**
	 * Reset the menu.
	 */
	private void reset() {
		setVisible(false);
		this.removeAll();
	}
	
	/**
	 * Init & popup the menu.
	 * @param listener the pop-up menu listener
	 * @param menu the menu data
	 */
	public void init(IPopupMenuListener listener, IMenu menu) {
		reset();
		this.listener = listener;
		this.addPopupMenuListener(this);
		if (menu != null) {
			while (menu.getSubItems().length == 1 && menu.getSubItems()[0] instanceof IMenu)
				menu = (IMenu)menu.getSubItems()[0];
			makeSubMenu(this, menu);
		}
	}
	
	/**
	 * Make Sub Menu.
	 * @param jm the sub-menu
	 * @param m the menu
	 */
	private void makeSubMenu(ISubMenuPeer jm, IMenu m) {
		for (IMenuItem item: m.getSubItems()) {
			if (item instanceof IMenu) {
				IMenu sub = (IMenu)item;
				while (sub.getSubItems().length == 1 && sub.getSubItems()[0] instanceof IMenu)
					sub = (IMenu)sub.getSubItems()[0];
				if (sub.getSubItems().length == 0)
					continue;
				makeSubMenu(jm.newSubMenu(sub.getText()), sub);
			} else {
				jm.addItem(item);
			}
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ISubMenuPeer#newSubMenu(java.lang.String)
	 */
	public ISubMenuPeer newSubMenu(String text) {
		SubMenu sub = new SubMenu(text);
		this.add(sub);
		return sub;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ISubMenuPeer#addItem(kiev.gui.IMenuItem)
	 */
	public void addItem(IMenuItem item) {
		if (item instanceof IMenu) {
			IMenu m = (IMenu)item;
			SubMenu sub = new SubMenu(m.getText());
			for (IMenuItem it : m.getSubItems())
				sub.addItem(it);
			this.add(sub);
		} else {
			this.add(new PopupMenuItem(item));
		}
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuPeer#showAt(int, int)
	 */
	public void showAt(int x, int y) {
		this.show(component, x, y);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuPeer#remove()
	 */
	public void remove() {
		component.remove(this);
	}
	
	/**
	 * Popup Menu Item.
	 */
	class PopupMenuItem extends TextAction {
		
		/**
		 * The item.
		 */
		private final IMenuItem item;
		
		/**
		 * The constructor.
		 * @param item the item
		 */
		PopupMenuItem(IMenuItem item) {
			super(item.getText());
			this.item = item;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			listener.popupMenuExecuted(item);
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.PopupMenuListener#popupMenuCanceled(javax.swing.event.PopupMenuEvent)
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
		listener.popupMenuCanceled();
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.PopupMenuListener#popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent)
	 */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

	/* (non-Javadoc)
	 * @see javax.swing.event.PopupMenuListener#popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent)
	 */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
	
}
