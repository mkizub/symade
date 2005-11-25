package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node(copyable=false)
@dflow(in="root()")
class JMethod extends JDNode {
	
	JMethod(Method vmethod) {
		super(vmethod);
	}

	Method getVMethod() {
		return (Method)dnode;
	}
	
}


