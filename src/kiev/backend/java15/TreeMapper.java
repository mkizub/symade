package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import java.io.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public class TreeMapper {
	Hashtable<ASTNode,ASTNode> theMap = new Hashtable<ASTNode,ASTNode>();

	public Object map(Object vnode) {
		if (vnode == null)
			return vnode;
		if!(vnode instanceof ASTNode)
			return vnode;
		if (vnode instanceof Struct)
			return mapStruct((Struct)vnode);
		Class vc = vnode.getClass();
		ASTNode jnode = (ASTNode)vc.newInstance();
		theMap.put((ASTNode)vnode,jnode);
		return jnode;
	}

	public JStruct mapStruct(Struct vs) {
		// map the structure object itself
		JStruct js = (JStruct)JDNode.findJDNode(vs);
		if (js != null)
			return js; // already mapped
		if (vs.isPackage())
			js = new JPackage(vs);
		else if (vs.isAnnotation())
			js = new JAnnotation(vs);
		else if (vs.isInterface())
			js = new JInterface(vs);
		else if (vs.isEnum())
			js = new JEnum(vs);
		else
			js = new JClazz(vs);
		// map fields, methods, constructors, initializers, etc
		foreach (DNode vd; vs.members) {
			JDNode jd;
			if (vd instanceof Field)
				jd = new JField((Field)vd);
			else if (vd instanceof Constructor)
				jd = new JConstructor((Constructor)vd);
			else if (vd instanceof RuleMethod)
				jd = new JRuleMethod((RuleMethod)vd);
			else if (vd instanceof Method)
				jd = new JMethod((Method)vd);
			else if (vd instanceof Initializer)
				jd = new JInitializer((Initializer)vd);
			else if (vd instanceof Struct)
				jd = mapStruct((Struct)vd);
			else {
				Kiev.reportError(vd, "Cannot map "+vd.getClass()+" to java backend tree");
				continue;
			}
			js.addMember(jd);
		}
		return js;
	}
}

