package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node(copyable=false)
class JField extends JDNode {
	
	JField(Field vfield) {
		super(vfield);
	}

	Field getVField() {
		return (Field)dnode;
	}
	
}


