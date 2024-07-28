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

import kiev.gui.IMenu;
import kiev.gui.IMenuItem;
import kiev.gui.IPopupMenuListener;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.ISubMenuPeer;
import kiev.gui.ICanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * The Pop-up menu used by the framework.
 */
public class PopupMenu implements IPopupMenuPeer, MenuListener {
	
	/**
	 * The menu.
	 */
	private Menu menu;
	
	/**
	 * The component. 
	 */
	private final ICanvas peer;
	
	/**
	 * The listener.
	 */
	private final IPopupMenuListener listener;

	/**
	 * Sub-menus of the pop-up menu.
	 */
	final class SubMenu implements ISubMenuPeer {
		
		/**
		 * Sub menu of menu.
		 */
		@SuppressWarnings("unused")
		private Menu subMenu;
		
		/**
		 * Text string to show.
		 */
		@SuppressWarnings("unused")
		private final String text;
		
		/**
		 * The constructor.
		 * @param text the text
		 */
		SubMenu(String text) {
			this.text = text;
			subMenu = new Menu(menu);
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.ISubMenuPeer#addItem(kiev.gui.IMenuItem)
		 */
		public void addItem(IMenuItem item) {
			new PopupMenuItem(item);
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.ISubMenuPeer#newSubMenu(java.lang.String)
		 */
		public ISubMenuPeer newSubMenu(String text) {
			SubMenu m = new SubMenu(text);
			return m;
		}
	}
	
	/**
	 * The constructor.
	 * @param peer the view peer
	 * @param listener the pop-up menu listener of events
	 */
	public PopupMenu(ICanvas peer, IPopupMenuListener listener, IMenu menu) {
		this.peer = peer;
		this.listener = listener;
		if (peer instanceof Canvas) { //safe 
			Canvas can = (Canvas)peer;	
			this.menu = new Menu(can.getShell(), SWT.POP_UP);
			this.menu.addMenuListener(this);
			can.setMenu(this.menu);
		}
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
		return new SubMenu(text);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ISubMenuPeer#addItem(kiev.gui.IMenuItem)
	 */
	public void addItem(IMenuItem item) {
		new PopupMenuItem(item);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuPeer#showAt(int, int)
	 */
	public void showAt(int x, int y) {
		if (peer instanceof Canvas) {
			Canvas can = (Canvas)peer;	
			Point loc = menu.getShell().getDisplay().map(can, null, new Point(x, y));
			menu.setLocation(loc.x, loc.y);
			menu.setVisible(true);
		}
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.IPopupMenuPeer#remove()
	 */
	public void remove() {
		if (Helper.okToUse(menu)) menu.dispose();
	}
	
	/**
	 * The Pop-up menu item. It listens for the selection events. 
	 */
	final class PopupMenuItem implements SelectionListener {
		
		/**
		 * The framework item.
		 */
		private final IMenuItem item;
		
		/**
		 * The menu item.
		 */
		private final MenuItem menuItem;
		
		/**
		 * The constructor.
		 * @param item item
		 */
		PopupMenuItem(IMenuItem item) {
			menuItem = new MenuItem(menu, SWT.POP_UP);
			this.item = item;
			menuItem.setText(item.getText());
			menuItem.addSelectionListener(this);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			listener.popupMenuExecuted(item);			
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent e) {
		Window.getDisplay().asyncExec(new Runnable(){

			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				listener.popupMenuCanceled();				
				remove();
			}			
		});		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent e) {}

}
