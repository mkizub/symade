package kiev.dump.bin;

import kiev.dump.DumpException;
import kiev.vtree.ANodeContext;
import kiev.vtree.INode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ScalarAttrSlot;

final class NodeRef {
	final int id;
	INode parent;
	AttrSlot slot;
	NodeRef(int id) {
		this.id = id;
	}
	public void applay(ANodeContext context, BinDumpReader reader) throws DumpException {
		NodeElem ne = reader.nodeTable.get(Integer.valueOf(id));
		if (ne == null) {
			context.theEnv.reportError().log("Cannot find delayed node ref #"+Integer.toHexString(id)+" for node '{}' slot '{}'", parent,slot);
			return;
		}
		if (!ne.isRead())
			ne = (NodeElem)reader.dfactory.makeDecoder(Signature.TAG_NODE_SIGN, reader).readElem(ne.id, ne.saddr);
		((ScalarAttrSlot)slot).set(parent, ne.node);
	}
}

