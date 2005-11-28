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
	
	public static JDNode findJDNode(DNode d) {
		JDNodeInfo jdi = (JDNodeInfo)d.getNodeData(JDNodeInfo.ID);
		if (jdi == null)
			return null;
		return jdi.jdnode;
	}

	JDNode(DNode dnode) {
		super(dnode.pos);
		this.flags = dnode.flags;
		this.dnode = dnode;
		if (dnode.meta != null)
			this.meta = (MetaSet)dnode.meta.copy();
		assert(dnode.getNodeData(JDNodeInfo.ID) == null);
		dnode.addNodeData(new JDNodeInfo(this));
	}
	
	Dumper toJavaModifiers(Dumper dmp) {
		if (meta != null) {
			foreach (Meta m; meta)
				m.toJavaDecl(dmp);
		}
		if( (flags & ACC_PUBLIC		) != 0 ) dmp.append("public ");
		if( (flags & ACC_PRIVATE	) != 0 ) dmp.append("private ");
		if( (flags & ACC_PROTECTED	) != 0 ) dmp.append("protected ");
		if( (flags & ACC_FINAL		) != 0 ) dmp.append("final ");
		if( (flags & ACC_STATIC		) != 0 ) dmp.append("static ");
		if( (flags & ACC_ABSTRACT	) != 0 ) dmp.append("abstract ");
		if( (flags & ACC_NATIVE		) != 0 ) dmp.append("native ");
		if( (flags & ACC_SYNCHRONIZED) != 0 ) dmp.append("synchronized ");
		if( (flags & ACC_VOLATILE	) != 0 ) dmp.append("volatile ");
		if( (flags & ACC_TRANSIENT	) != 0 ) dmp.append("transient ");
		return dmp;
	}
}
