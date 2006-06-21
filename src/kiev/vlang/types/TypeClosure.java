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
	@virtual typedef VView = VTypeClosureRef;

	@att public TypeRef[]		types;

	@nodeview
	public static final view VTypeClosureRef of TypeClosureRef extends VTypeRef {
		public:ro	TypeRef[]			types;
	}

	public TypeClosureRef() {}
	
	public TypeClosureRef(CallType tp) {
		this.lnk = tp;
		assert (tp.isReference());
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type[] tps = new Type[types.length-1];
		for(int i=0; i < tps.length; i++) {
			tps[i] = types[i].getType();
		}
		Type ret = types[types.length-1].getType();
		this.lnk = new CallType(null,null,tps,ret,true);
		return this.lnk;
	}
	
	public boolean isBound() {
		return true;
	}
	public Struct getStruct() {
		return null;
	}
	public TypeDecl getTypeDecl() {
		return CallMetaType.instance.tdecl;
	}
}
