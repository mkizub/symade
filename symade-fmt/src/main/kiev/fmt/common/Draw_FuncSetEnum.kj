package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.gui.IMenu;
import kiev.gui.IMenuItem;
import kiev.vtree.AttrSlot;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

public final class Draw_FuncSetEnum extends Draw_SyntaxFunc {
	private static final long serialVersionUID = 5924171910757439709L;
	
	public String[]				names;

	Object readResolve() throws ObjectStreamException {
		super.readResolve();
		if (names == null)
			names = new String[0];
		return this;
	}

	final static class SetEnumAction implements IMenuItem {
		private final String title;
		private final INode node;
		private final ScalarAttrSlot attr;
		private final Object value;
		SetEnumAction(String title, INode node, ScalarAttrSlot attr, Object value) {
			this.title = title;
			this.node = node;
			this.attr = attr;
			this.value = value;
		}
		public String getText() { return title; }
		public void exec() {
			node.setVal(attr, value);
		}
	}

	/** Make menu for setting new enum or boolean value the specified node
	 */
	public IMenu makeMenu(INode node) {
		ScalarAttrSlot attr = null;
		for (AttrSlot a : node.values()) {
			if (a.name == this.attr && a instanceof ScalarAttrSlot) {
				attr = (ScalarAttrSlot)a;
				break;
			}
		}
		if (attr == null)
			return null;
		Menu m = new Menu(title);
		if (attr.typeinfo.clazz == Boolean.TYPE) {
			m.append(new SetEnumAction(names.length > 0 ? names[0] : "false", node, attr, Boolean.FALSE));
			m.append(new SetEnumAction(names.length > 1 ? names[1] : "true", node, attr, Boolean.TRUE));
			return m;
		}
		else if (attr.typeinfo.clazz == Boolean.class) {
			m.append(new SetEnumAction(names.length > 0 ? names[0] : "False", node, attr, Boolean.FALSE));
			m.append(new SetEnumAction(names.length > 1 ? names[1] : "True", node, attr, Boolean.TRUE));
			m.append(new SetEnumAction(names.length > 2 ? names[2] : "Empty (null)", node, attr, null));
			return m;
		}
		else if (Enum.class.isAssignableFrom(attr.typeinfo.clazz)) {
			Enum[] vals;
			try {
				vals = (Enum[])attr.typeinfo.clazz.getMethod("values").invoke(null);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			for (int i=0; i < vals.length; i++) {
				Enum v = vals[i];
				m.append(new SetEnumAction(names.length >= vals.length ? names[i] : String.valueOf(v), node, attr, v));
			}
			m.append(new SetEnumAction(names.length > vals.length ? names[vals.length] : "Empty (null)", node, attr, null));
			return m;
		}
		return null;
	}

	public boolean checkApplicable(String attr) {
		if (this.attr != attr)
			return false;
		return true;
	}
		
}

