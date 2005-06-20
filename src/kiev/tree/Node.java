package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

public final class VersionBranch {
	private static int id_counter;
	
	public static final VersionBranch Src   = new VersionBranch(null);
	
	public final VersionBranch parent;
	public final int id;
	
	private VersionBranch(VersionBranch parent) {
		this.parent = parent;
		this.id = id_counter++;
	}
	
}

public abstract class CreateInfo {
}

// versioned node
public interface VNode {

	public static final VNode[] emptyArray = new VNode[0];

	public NodeImpl getImpl(VersionBranch branch);
	
	public void setImpl(VersionBranch branch, NodeImpl impl);
}

public abstract class Node implements VNode {

	public static final Node[] emptyArray = new Node[0];

	private NodeImpl[] impls = new NodeImpl[2];
	
	public final NodeImpl getImpl(VersionBranch branch) {
		while (impls.length >= branch.id || impls[branch.id] == null)
			branch = branch.parent;
		return impls[branch.id];
	}
	
	private void expandImpls(int sz) {
		NodeImpl[] tmp = new NodeImpl[sz];
		System.arraycopy(impls, 0, tmp, 0, impls.length);
		impls = tmp;
	}
	
	public final void setImpl(VersionBranch branch, NodeImpl impl) {
		if (impls.length >= branch.id)
			expandImpls(branch.id+1);
		impl.branch = branch;
		impl.vnode = this;
		impls[branch.id] = impl;
	}
}

public abstract class NodeImpl {

	// the branch (revision) this node implementation belongs to
	public VersionBranch branch;
	// the node which encapsulates this implementation (version)
    public VNode		vnode;
    // the parent node in the tree
    public VNode		pnode;
    // the source code position or the node this one was generated from
	public CreateInfo	src_info;
	// node flags
	public int			flags;

	public NodeImpl(CreateInfo src) {
		this.src_info = src;
	}

	public /*abstract*/ void cleanup() {
		vnode = null;
		pnode = null;
		src_info = null;
	};
	
}
