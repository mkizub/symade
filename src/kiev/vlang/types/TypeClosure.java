package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import kiev.vlang.types.TypeRef.TypeRefImpl;
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
	@virtual typedef NImpl = TypeClosureRefImpl;
	@virtual typedef VView = TypeClosureRefView;

	@nodeimpl
	public static final class TypeClosureRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeClosureRef;
		@att public NArr<TypeRef>		types;
	}
	@nodeview
	public static final view TypeClosureRefView of TypeClosureRefImpl extends TypeRefView {
		public:ro	NArr<TypeRef>			types;
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }

	public TypeClosureRef() {
		super(new TypeClosureRefImpl());
	}
	
	public TypeClosureRef(CallType tp) {
		this();
		this.lnk = tp;
		assert (tp.isReference());
	}
	
	public boolean isBound() {
		return true;
	}
	public Struct getStruct() {
		return null;
	}

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
