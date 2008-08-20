package kiev.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import kiev.gui.IMenuItem;
import kiev.gui.IPopupMenuListener;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.ISubMenuPeer;
import kiev.gui.IUIViewPeer;

public class PopupMenu implements IPopupMenuPeer {
	Menu menu;
	
	class SubMenu implements ISubMenuPeer {
		Menu subMenu;
		String text;
		SubMenu(String text) {
			this.text = text;
			subMenu = new Menu(menu);
		}
		public void addItem(IMenuItem item) {
			new PopupMenuItem(item);
		}
		public ISubMenuPeer newSubMenu(String text) {
			SubMenu m = new SubMenu(text);
			return m;
		}
	}

	final Object component;
	final IPopupMenuListener listener;
	
	public PopupMenu(IUIViewPeer peer, IPopupMenuListener listener) {
		this.component = peer;
		this.listener = listener;
		menu = new Menu(Window.getShell(), SWT.POP_UP);
	}
	
	public ISubMenuPeer newSubMenu(String text) {
		return new SubMenu(text);
	}

	public void addItem(IMenuItem item) {
		new PopupMenuItem(item);
	}
	
	public void showAt(int x, int y) {
		menu.setLocation(x, y);
		menu.setVisible(true);
	}
	
	public void remove() {
		menu.dispose();
	}
	
	class PopupMenuItem implements SelectionListener {
		final IMenuItem item;
		final MenuItem menuItem;
		PopupMenuItem(IMenuItem item) {
			menuItem = new MenuItem(menu, SWT.POP_UP);
			this.item = item;
			menuItem.setText(item.getText());
			menuItem.addSelectionListener(this);
		}

		public void widgetDefaultSelected(SelectionEvent e) {		
		}

		public void widgetSelected(SelectionEvent e) {
			listener.popupMenuExecuted(item);			
		}
	}

	public void popupMenuCanceled(MenuEvent e) {
		listener.popupMenuCanceled();
	}

}
