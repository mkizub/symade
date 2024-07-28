package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.vtree.INode;

public final class Draw_CalcOptionTrue extends Draw_CalcOption {
	private static final long serialVersionUID = 4681718280186599247L;
	public String							name;

	public boolean calc(INode node) {
		if (node == null) return false;
		Object val = node.getVal(node.getAttrSlot(name));
		if (val == null || !(val instanceof Boolean)) return false;
		return ((Boolean)val).booleanValue();
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
