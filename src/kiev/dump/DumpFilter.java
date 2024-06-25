package kiev.dump;

import kiev.vtree.ANode;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;

public interface DumpFilter {
	public boolean ignoreAttr(INode parent, AttrSlot attr);
	public boolean ignoreNode(INode parent, AttrSlot attr, INode node);
}
