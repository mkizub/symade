package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node(copyable=false)
public class JField extends JDNode {
	
	public static JField newJField(Field vf)
		alias operator(240,lfy,new)
	{
		JField jf = (JField)findJDNode(vf);
		if (jf == null)
			jf = new JField(vf);
		return jf;
	}
	
	private JField(Field vfield) {
		super(vfield);
	}

	Field getVField() {
		return (Field)dnode;
	}
	
}

@node(copyable=false)
public class JInitializer extends JDNode {
	
	public static JInitializer newJInitializer(Initializer vi)
		alias operator(240,lfy,new)
	{
		JInitializer ji = (JInitializer)findJDNode(vi);
		if (ji == null)
			ji = new JInitializer(vi);
		return ji;
	}
	
	private JInitializer(Initializer init) {
		super(init);
	}

	Initializer getVInitializer() {
		return (Initializer)dnode;
	}
	
}


