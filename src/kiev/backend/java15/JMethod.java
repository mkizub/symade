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
public class JMethod extends JDNode {
	
	public static JMethod newJMethode(Method vm)
		alias operator(240,lfy,new)
	{
		JMethod jm = (JMethod)findJDNode(vm);
		if (jm == null) {
			if (vm instanceof Constructor)
				jm = new JConstructor((Constructor)vm);
			else if (vm instanceof RuleMethod)
				jm = new JRuleMethod((RuleMethod)vm);
			else
				jm = new JMethod(vm);
		}
		return jm;
	}
	
	JMethod(Method vmethod) {
		super(vmethod);
	}

	Method getVMethod() {
		return (Method)dnode;
	}
	
}

@node(copyable=false)
@dflow(in="root()")
public class JConstructor extends JMethod {
	
	JConstructor(Constructor vmethod) {
		super(vmethod);
	}

	Constructor getVConstructor() {
		return (Constructor)dnode;
	}
	
}

@node(copyable=false)
@dflow(in="root()")
public class JRuleMethod extends JMethod {
	
	JRuleMethod(RuleMethod vmethod) {
		super(vmethod);
	}

	RuleMethod getVRuleMethod() {
		return (RuleMethod)dnode;
	}
	
}

