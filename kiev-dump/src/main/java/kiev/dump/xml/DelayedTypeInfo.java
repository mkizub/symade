package kiev.dump.xml;

import kiev.vlang.Env;
import kiev.vlang.ENode;
import kiev.vlang.types.AType;
import kiev.vtree.ANodeContext;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

final class DelayedTypeInfo {
	final INode node;
	final ScalarAttrSlot attr;
	final String signature;
	DelayedTypeInfo(INode node, ScalarAttrSlot attr, String signature) {
		this.node = node;
		this.attr = attr;
		this.signature = signature;
	}
	public void applay(ANodeContext context) {
		AType tp = AType.fromSignature(context,signature,false);
		if (tp != null) {
			attr.set(node,tp);
		} else {
			((ENode)node).setTypeSignature(signature);
		}
	}
}

