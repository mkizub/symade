package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.vlang.TypeRef.TypeRefImpl;
import kiev.vlang.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

public class TypeClosureRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeClosureRefImpl;
	@virtual typedef VView = TypeClosureRefView;

	@node
	public static final class TypeClosureRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeClosureRef;
		@att public NArr<TypeRef>		types;
		public TypeClosureRefImpl() {}
		public TypeClosureRefImpl(ClosureType tp) { super(0, tp); }
	}
	@nodeview
	public static final view TypeClosureRefView of TypeClosureRefImpl extends TypeRefView {
		public access:ro	NArr<TypeRef>			types;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	TypeClosureRef() {
		super(new TypeClosureRefImpl());
	}
	
	TypeClosureRef(ClosureType tp) {
		super(new TypeClosureRefImpl(tp));
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
        this.lnk = new ClosureType(tps,ret);
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
