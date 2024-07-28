package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.vtree.INode;

public final class Draw_CalcOptionNotEmpty extends Draw_CalcOption {
	private static final long serialVersionUID = 5228077155972939972L;
	public String							name;

	public boolean calc(INode node) {
		if (node == null)
			return false;
		Object obj = node.getVal(node.getAttrSlot(name));
		if (obj instanceof Object[])
			return ((Object[])obj).length > 0;
		return false;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
