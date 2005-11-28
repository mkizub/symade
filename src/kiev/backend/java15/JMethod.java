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
	
	MethodType		type;
	KString			name;
	JVar[]			params;
	ENode			body;

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
		this.type = vmethod.dtype;
		this.name = vmethod.name.name;
		this.params = new JVar[vmethod.params.length];
		for (int i=0; i < this.params.length; i++)
			this.params[i] = new JVar(vmethod.params[i]);
		this.body = vmethod.body;
	}

	Method getVMethod() {
		return (Method)dnode;
	}

	protected Dumper toJavaTypeName(Dumper dmp) {
		return dmp.space().append(type.ret).forsed_space().append(name);
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		toJavaModifiers(dmp);
		toJavaTypeName(dmp);
		dmp.append('(');
		for(int i=0; i < params.length; i++) {
			params[i].toJavaDecl(dmp);
			if( i < (params.length-1) ) dmp.append(",");
		}
		dmp.append(')').space();
		if (isAbstract())
			dmp.append(';');
		else if (body == null)
			dmp.append("{ ... }");
		else
			dmp.append(body);
		return dmp.newLine();
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
	
	protected Dumper toJavaTypeName(Dumper dmp) {
		return dmp.append(((JStruct)parent).sname);
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		if (name != nameClassInit) {
			super.toJavaDecl(dmp);
		} else {
			dmp.append("static ");
			if (body == null)
				dmp.append("{ ... }");
			else
				dmp.append(body);
		}
		return dmp.newLine();
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

