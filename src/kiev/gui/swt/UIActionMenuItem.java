package kiev.gui.swt;

import kiev.gui.IWindow;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class UIActionMenuItem {
	MenuItem item;
	final IWindow wnd;
	final UIActionFactory factory;
	
	/**
	 * Constructor of UIActionMenuItem.
	 * @param wnd
	 * @param text
	 * @param mnemonic
	 * @param factory
	 */
	UIActionMenuItem(Menu menu, int style, IWindow wnd, String text, int accelerator, UIActionFactory factory) {
		item = new MenuItem(menu, style);
		item.setText(text);
		item.setAccelerator(accelerator);
		this.wnd = wnd;
		this.factory = factory;
//		this.getAccessibleContext().setAccessibleDescription(factory.getDescr());		
		item.addSelectionListener((Window)wnd);
		item.setData(this);
	}
	
	public boolean isEnabled() {
		if (factory == null || !item.isEnabled()) return false;
		return factory.getAction(new UIActionViewContext(wnd, null, wnd.getCurrentView())) != null;
	}
	
}
