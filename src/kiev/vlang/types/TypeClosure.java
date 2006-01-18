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

@node
public class TypeClosureRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeClosureRefImpl extends TypeRefImpl {
		@att public NArr<TypeRef>		types;
		public TypeClosureRefImpl() {}
		public TypeClosureRefImpl(CallType tp) { super(0, tp); }
	}
	@nodeview
	public static final view TypeClosureRefView of TypeClosureRefImpl extends TypeRefView {
		public access:ro	NArr<TypeRef>			types;
	}

	@att public abstract virtual access:ro NArr<TypeRef>			types;
	
	public NodeView				getNodeView()			{ return new TypeClosureRefView((TypeClosureRefImpl)this.$v_impl); }
	public ENodeView			getENodeView()			{ return new TypeClosureRefView((TypeClosureRefImpl)this.$v_impl); }
	public TypeRefView			getTypeRefView()		{ return new TypeClosureRefView((TypeClosureRefImpl)this.$v_impl); }
	public TypeClosureRefView	getTypeClosureRefView()	{ return new TypeClosureRefView((TypeClosureRefImpl)this.$v_impl); }

	@getter public NArr<TypeRef>		get$types()		{ return this.getTypeClosureRefView().types; }
	
	public TypeClosureRef() {
		super(new TypeClosureRefImpl());
	}
	
	public TypeClosureRef(CallType tp) {
		super(new TypeClosureRefImpl(tp));
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
