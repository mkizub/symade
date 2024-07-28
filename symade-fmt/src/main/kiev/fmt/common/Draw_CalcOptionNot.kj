package kiev.fmt.common;

import kiev.vtree.INode;

public final class Draw_CalcOptionNot extends Draw_CalcOption {
	private static final long serialVersionUID = 2907619420920739136L;
	public Draw_CalcOption					opt;

	public boolean calc(INode node) {
		if (opt == null)
			return true;
		return !opt.calc(node);
	}
}
