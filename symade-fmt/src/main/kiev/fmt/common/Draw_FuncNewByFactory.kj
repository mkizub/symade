package kiev.fmt.common;

import kiev.gui.IMenu;
import kiev.vtree.INode;

public final class Draw_FuncNewByFactory extends Draw_SyntaxFunc implements Draw_FuncNewNode {
	private static final long serialVersionUID = 4137650142637389475L;
	
	public String factory;

	/** Make menu for creating new element for the specified node
	 * in the specified attribute at position idx
	 */
	public IMenu makeMenu(INode node, int idx, Draw_ATextSyntax tstx) {
		try {
			Draw_FuncNewNode f = (Draw_FuncNewNode)Class.forName(factory).newInstance();
			return f.makeMenu(node, idx, tstx);
		} catch (Exception e) {}
		return null;
	}

	public boolean checkApplicable(String attr) {
		if (this.attr != attr)
			return false;
		return true;
	}
}

