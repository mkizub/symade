package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

public class BlockSt extends NodeImpl {
	public VNode[]	stats = VNode.emptyArray;

	public BlockSt(CreateInfo src, VNode[] stats) {
		super(src);
		this.stats = stats;
	}

}

public class BreakSt extends NodeImpl {
	public VNode target;

	public BreakSt(CreateInfo src, VNode target) {
		super(src);
		this.target = target;
	}

}

