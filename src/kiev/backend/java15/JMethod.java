package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node
@dflow(in="root()")
public class JMethod extends DNode implements Named,Typed,ScopeOfNames,ScopeOfMethods,Accessable {
	
	@ref
	public final Method			vmethod;
	
	public JMethod() {}
	
	public JMethod(Method vmethod) {
		super(vmethod.pos);
		this.vmethod = vmethod;
	}
}


