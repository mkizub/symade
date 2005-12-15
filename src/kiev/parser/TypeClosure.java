package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeClosureRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

    @att public final NArr<TypeRef>	types;

	TypeClosureRef() {}
	
	TypeClosureRef(ClosureType tp) {
		this.lnk = tp;
	}
	
	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type[] tps = new Type[types.length-1];
        for(int i=0; i < tps.length; i++) {
			tps[i] = types[i].getType();
		}
        Type ret = types[types.length-1].getType();
        this.lnk = ClosureType.newClosureType(Type.tpClosureClazz,tps,ret);
		return this.lnk;
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
