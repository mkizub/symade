package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.vlang.types.TypeRef.TypeRefView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public class TypeClosureRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeClosureRef;
	@virtual typedef VView = TypeClosureRefView;

	@att public NArr<TypeRef>		types;

	@nodeview
	public static final view TypeClosureRefView of TypeClosureRef extends TypeRefView {
		public:ro	NArr<TypeRef>			types;

		public Type getType() {
			if (this.lnk != null)
				return this.lnk;
			Type[] tps = new Type[types.length-1];
			for(int i=0; i < tps.length; i++) {
				tps[i] = types[i].getType();
			}
			Type ret = types[types.length-1].getType();
			this.lnk = new CallType(tps,ret,true);
			return this.lnk;
		}
	}

	public TypeClosureRef() {}
	
	public TypeClosureRef(CallType tp) {
		this.lnk = tp;
		assert (tp.isReference());
	}
	
	public boolean isBound() {
		return true;
	}
	public Struct getStruct() {
		return null;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append('(');
		for(int i=0; i < types.length-1; i++) {
			dmp.append(types[i]);
			if( i < types.length-2) dmp.append(',').space();
		}
		dmp.append(")->").append(types[types.length-1]).space();
		return dmp;
	}
}
