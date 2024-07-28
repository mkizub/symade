package kiev.fmt.common;

import kiev.vtree.INode;

public final class Draw_CalcOptionAnd extends Draw_CalcOption {
	private static final long serialVersionUID = -6723637813837648633L;
	public Draw_CalcOption[]				opts = Draw_CalcOption.emptyArray;

	public boolean calc(INode node) {
		for (Draw_CalcOption opt : opts) {
			if (opt != null && !opt.calc(node))
				return false;
		}
		return true;
	}
}
