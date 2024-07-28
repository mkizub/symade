package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.vtree.INode;
import kiev.vtree.SymbolRef;

public final class Draw_CalcOptionNotNull extends Draw_CalcOption {
	private static final long serialVersionUID = 9111122713846315273L;
	public String							name;

	public boolean calc(INode node) {
		if (node == null)
			return false;
		Object obj = null;
		try { obj = node.getVal(node.getAttrSlot(name)); } catch (RuntimeException e) {}
		if (obj == null)
			return false;
		if (obj instanceof SymbolRef && ((SymbolRef)obj).getVal(((SymbolRef)obj).getAttrSlot("name")) == null)
			return false;
		return true;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
