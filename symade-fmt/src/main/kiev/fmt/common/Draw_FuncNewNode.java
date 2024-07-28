package kiev.fmt.common;

import kiev.gui.IMenu;
import kiev.vtree.INode;

public interface Draw_FuncNewNode {
	public IMenu makeMenu(INode node, int idx, Draw_ATextSyntax tstx);
	public boolean checkApplicable(String attr);
}
