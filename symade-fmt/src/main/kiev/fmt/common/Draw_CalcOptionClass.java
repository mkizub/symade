package kiev.fmt.common;

import kiev.vtree.INode;

public final class Draw_CalcOptionClass extends Draw_CalcOption {
	private static final long serialVersionUID = -6991565039282353962L;
	public Class							clazz;

	public boolean calc(INode node) {
		if (clazz == null) return true;
		return clazz.isInstance(node);
	}
}
