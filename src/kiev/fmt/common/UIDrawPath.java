package kiev.fmt.common;

import java.util.Collection;

import kiev.vtree.INode;

public class UIDrawPath {

	public INode[] path;
	public int cursor;
	
	public UIDrawPath(Collection<INode> path) {
		this(path.toArray(new INode[path.size()]), -1);
	}
	public UIDrawPath(INode[] path) {
		this(path, -1);
	}
	public UIDrawPath(INode[] path, int cursor) {
		this.path = path;
		this.cursor = cursor;
	}
}
