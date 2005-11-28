package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node(copyable=false)
public class JVar extends JDNode {
	KString			name;
	Type			type;
	ENode			init;
	private int		bcpos = -1;

	public static JVar newJVar(Var vv)
		alias operator(240,lfy,new)
	{
		JVar jv = (JVar)findJDNode(vv);
		if (jv == null)
			jv = new JVar(vv);
		return jv;
	}
	
	private JVar(Var vvar) {
		super(vvar);
		type = vvar.type;
		name = vvar.name.name;
	}

	Dumper toJavaModifiers(Dumper dmp) {
		if (meta != null) {
			foreach (Meta m; meta)
				m.toJavaDecl(dmp);
		}
		if( (flags & ACC_FINAL		) != 0 ) dmp.append("final ");
		return dmp;
	}

	public Dumper toJavaDecl(Dumper dmp) {
		toJavaModifiers(dmp);
		type.toJava(dmp).forsed_space().append(name);
		if( init != null ) {
			dmp.append(" = ");
			init.toJava(dmp);
		}
		return dmp;
	}
}


