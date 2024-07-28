package kiev.fmt.common;

import java.io.ObjectStreamException;

import kiev.vlang.DNode;
import kiev.vtree.INode;

public final class Draw_CalcOptionHasMeta extends Draw_CalcOption {
	private static final long serialVersionUID = -36079655332483986L;
	public String							name;
	
	public boolean calc(INode node) {
		if (node instanceof DNode) {
			DNode dn = (DNode)node;
			return (dn.getMeta(name) != null);
		}
		return false;
	}
	Object readResolve() throws ObjectStreamException {
		if (this.name != null) this.name = this.name.intern();
		return this;
	}
}
