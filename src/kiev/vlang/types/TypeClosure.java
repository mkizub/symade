package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeClosureRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeClosureRef;

	@att public TypeRef[]		args;
	@att public TypeRef			ret;

	public TypeClosureRef() {}
	
	public TypeClosureRef(CallType tp) {
		this.lnk = tp;
		assert (tp.isReference());
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type[] args = new Type[this.args.length];
		for(int i=0; i < args.length; i++) {
			args[i] = this.args[i].getType();
		}
		Type ret = this.ret.getType();
		this.lnk = new CallType(null,null,args,ret,true);
		return this.lnk;
	}
	
	public Struct getStruct() {
		return null;
	}
	public TypeDecl getTypeDecl() {
		return CallMetaType.instance.tdecl;
	}
}
