package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

final class JDNodeInfo extends NodeData {
	public static final KString ID = KString.from("jdnode info");
	final JDNode jdnode;
	JDNodeInfo(JDNode jdnode) {
		super(ID);
		this.jdnode = jdnode;
	}
}

@node(copyable=false)
abstract class JDNode extends DNode {
	/** Vlang node that corresponds to this java dnode */
	@ref public final DNode dnode;

	/** Array of attributes of this structure */
	Attr[] attrs = Attr.emptyArray;
	
	JDNode(DNode dnode) {
		super(dnode.pos);
		this.dnode = dnode;
		assert(dnode.getNodeData(JDNodeInfo.ID) == null);
		dnode.addNodeData(new JDNodeInfo(this));
	}
	
	public static JDNode findJDNode(DNode d) {
		JDNodeInfo jdi = (JDNodeInfo)d.getNodeData(JDNodeInfo.ID);
		if (jdi == null)
			return null;
		return jdi.jdnode;
	}
}
