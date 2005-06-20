package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

public class Catch extends NodeImpl {
	public VNode par;
	public VNode body;

	public Catch(CreateInfo src, VNode par, VNode body) {
		super(src);
		this.par = par;
		this.body = body;
	}

}
