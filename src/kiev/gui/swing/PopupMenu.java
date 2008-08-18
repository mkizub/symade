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

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.TextAction;

import kiev.gui.IMenuItem;
import kiev.gui.IPopupMenuListener;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.ISubMenuPeer;
import kiev.gui.IUIViewPeer;

public class PopupMenu extends JPopupMenu implements IPopupMenuPeer, PopupMenuListener {
	class SubMenu extends JMenu implements ISubMenuPeer {
		SubMenu(String text) {
			super(text);
		}
		public void addItem(IMenuItem item) {
			this.add(new PopupMenuItem(item));
		}
		public ISubMenuPeer newSubMenu(String text) {
			SubMenu m = new SubMenu(text);
			this.add(m);
			return m;
		}
	}

	final JComponent component;
	final IPopupMenuListener listener;
	
	public PopupMenu(IUIViewPeer peer, IPopupMenuListener listener) {
		this.component = (JComponent)peer;
		this.listener = listener;
	}
	
	public ISubMenuPeer newSubMenu(String text) {
		return new SubMenu(text);
	}

	public void addItem(IMenuItem item) {
		this.add(new PopupMenuItem(item));
	}
	
	public void showAt(int x, int y) {
		this.show(component, x, y);
	}
	
	public void remove() {
		component.remove(this);
	}
	
	class PopupMenuItem extends TextAction {
		final IMenuItem item;
		PopupMenuItem(IMenuItem item) {
			super(item.getText());
			this.item = item;
		}

		public void actionPerformed(ActionEvent e) {
			listener.popupMenuExecuted(item);
		}
	}

	public void popupMenuCanceled(PopupMenuEvent e) {
		listener.popupMenuCanceled();
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
}
