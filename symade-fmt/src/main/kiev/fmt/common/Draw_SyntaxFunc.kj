package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;

import kiev.gui.IMenu;
import kiev.gui.IMenuItem;

public abstract class Draw_SyntaxFunc implements Serializable {
	public static final Draw_SyntaxFunc[] emptyArray = new Draw_SyntaxFunc[0];

	/**
	 * Menu.
	 */
	public final static class Menu implements IMenu {
		private final String title;
		private IMenuItem[] actions;
		public Menu(String title) {
			this.title = title;
			this.actions = new IMenuItem[0];
		}
		public void append(IMenuItem menuItem) {
			actions = (IMenuItem[])kiev.stdlib.Arrays.append(actions, menuItem);
		}
		public IMenuItem[] getSubItems() { return actions; }
		public String getText() { return title; }
		public void exec() {}
	}

	public String				title;
	public String				attr;

	Object readResolve() throws ObjectStreamException {
		if (this.title != null) this.title = this.title.intern();
		if (this.attr != null) this.attr = this.attr.intern();
		return this;
	}
}

