package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

public class Identifier extends NodeImpl {
	
	public KString name;
	
	public Identifier(CreateInfo createInfo, KString name) {
    	super(createInfo);
    	this.name = name;
	}

}

