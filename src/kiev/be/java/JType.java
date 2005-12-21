package kiev.be.java;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;

import kiev.vlang.Var.VarImpl;

public class JType extends Type {
	
	public final boolean isDoubleSize()	{ return (flags & flDoubleSize)		!= 0 ; }
	
	JType(KString java_signature) {
		super(java_signature);
	}
	
	public final KString get$java_signature() { return signature; }
}

public class JBaseType extends JType {
//	public final JStruct			jstruct;
	
	JBaseType(KString java_signature, BaseType type) {
		super(java_signature);
		this.flags = type.flags;
//		this.jstruct = new JStruct(type.getStruct());
	}
}

public class JArrayType extends JType {
	public final JType				jarg;
	
	JArrayType(KString java_signature, ArrayType type) {
		super(java_signature);
		this.flags = type.flags;
		this.jarg = type.bindings[0].getJType();
	}
}



