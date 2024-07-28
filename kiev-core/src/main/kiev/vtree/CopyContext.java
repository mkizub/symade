package kiev.vtree;

import syntax kiev.Syntax;

public interface CopyContext {
	public <N extends INode> N copyRoot(N node);
	public <N extends INode> N copyFull(N node);
	public void addCopyInfo(INode nold, INode nnew);
	public INode getCopyOf(long uid);
	public void updateLinks();
}

public class Copier implements CopyContext {
	
	private final Hashtable<Long,INode> map = new Hashtable<Long,INode>();
	
	public <N extends INode> N copyRoot(N node) {
		N t = (N)node.copy(this);
		return t;
	}

	public <N extends INode> N copyFull(N node) {
		N t = copyRoot(node);
		updateLinks();
		return t;
	}

	public void addCopyInfo(INode nold, INode nnew) {
		if (nold != nnew)
			map.put(Long.valueOf(nold.getUID()),nnew);
	}

	public INode getCopyOf(long uid) {
		return map.get(Long.valueOf(uid));
	}

	public void updateLinks() {
		foreach (INode node; map) {
			foreach (AttrSlot attr; node.values(); !attr.isChild()) {
				if (attr instanceof SpaceAttrSlot) {
					INode[] arr = attr.getArray(node);
					for (int i=0; i < arr.length; i++) {
						INode nold = arr[i];
						INode nnew = map.get(Long.valueOf(nold.getUID()));
						if (nnew != null && nold != nnew)
							arr[i] = nnew;
					}
				}
				else if (attr instanceof ScalarAttrSlot) {
					Object nold = attr.get(node);
					if (nold instanceof INode) {
						INode nnew = map.get(Long.valueOf(nold.getUID()));
						if (nnew != null && nold != nnew)
							attr.set(node,nnew);
					}
				}
			}
			Object[] ext_data = node.asANode().getExtData();
			for (int i=0; i < ext_data.length; i++) {
				Object dat = ext_data[i];
				if (dat instanceof DataAttachInfo) {
					DataAttachInfo dai = (DataAttachInfo)dat;
					if (dai.p_data instanceof INode) {
						INode nold = (INode)dai.p_data;
						INode nnew = map.get(Long.valueOf(nold.getUID()));
						if (nnew != null && nold != nnew)
							node.setVal(dai.p_slot,nnew);
					}
				}
			}
		}
	}
}


