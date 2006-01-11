package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

import kiev.vlang.TypeRef.TypeRefImpl;
import kiev.vlang.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

public class TypeCallRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeCallRefImpl;
	@virtual typedef VView = TypeCallRefView;

	@node
	public static final class TypeCallRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeCallRef;
		@ref public NArr<TypeDef>			targs;
		@att public NArr<TypeRef>			args;
		@att public TypeRef					ret;
		public TypeCallRefImpl() {}
	}
	@nodeview
	public static final view TypeCallRefView of TypeCallRefImpl extends TypeRefView {
		public access:ro	NArr<TypeDef>			targs;
		public access:ro	NArr<TypeRef>			args;
		public				TypeRef					ret;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public TypeCallRef() {
		super(new TypeCallRefImpl());
	}

	public TypeCallRef(MethodType mt) {
		super(new TypeCallRefImpl());
		this.ret = new TypeRef(mt.ret);
		foreach (Type a; mt.args)
			this.args += new TypeRef(a);
		this.lnk = mt;
	}

	public boolean isBound() {
		return true;
	}

	public void callbackChildChanged(AttrSlot attr) {
		this.lnk = null;
		if (parent != null && pslot != null) {
			parent.callbackChildChanged(pslot);
		}
	}

	public MethodType getMType() {
		if (this.lnk != null)
			return (MethodType)this.lnk;
		Type rt = ret.getType();
		Type[] atypes = new Type[args.length];
		for(int i=0; i < atypes.length; i++) {
			atypes[i] = args[i].getType();
		}
		if (targs.length == 0) {
			this.lnk = new MethodType(atypes,rt);
		} else {
			TVarSet vset = new TVarSet();
			foreach (TypeDef td; targs)
				vset.append(td.getAType(), null);
			this.lnk = new MethodType(vset,atypes,rt);
		}
		return (MethodType)this.lnk;
	}
	public Type getType() {
		return getMType();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (targs.length > 0) {
			sb.append('<');
			for(int i=0; i < targs.length; i++) {
				sb.append(targs[i]);
				if( i < targs.length-1)
					sb.append(',');
			}
			sb.append('>');
		}
		sb.append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1)
				sb.append(',');
		}
		sb.append(")->").append(ret);
		return sb.toString();
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
