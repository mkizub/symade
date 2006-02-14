package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.types.TypeRef.TypeRefImpl;
import kiev.vlang.types.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public class TypeCallRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeCallRef;
	@virtual typedef NImpl = TypeCallRefImpl;
	@virtual typedef VView = TypeCallRefView;

	@nodeimpl
	public static final class TypeCallRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeCallRef;
		@ref public NArr<TypeDef>			targs;
		@att public NArr<TypeRef>			args;
		@att public TypeRef					ret;

		public void callbackChildChanged(AttrSlot attr) {
			this.lnk = null;
			if (parent != null && pslot != null) {
				parent.callbackChildChanged(pslot);
			}
		}
	}
	@nodeview
	public static final view TypeCallRefView of TypeCallRefImpl extends TypeRefView {
		public:ro	NArr<TypeDef>			targs;
		public:ro	NArr<TypeRef>			args;
		public		TypeRef					ret;

		public CallType getMType() {
			if (this.lnk != null)
				return (CallType)this.lnk;
			Type rt = ret.getType();
			Type[] atypes = new Type[args.length];
			for(int i=0; i < atypes.length; i++) {
				atypes[i] = args[i].getType();
			}
			if (targs.length == 0) {
				this.lnk = new CallType(atypes,rt);
			} else {
				TVarBld vset = new TVarBld();
				foreach (TypeDef td; targs)
					vset.append(td.getAType(), null);
				this.lnk = new CallType(vset,atypes,rt,false);
			}
			return (CallType)this.lnk;
		}

		public Type getType() {
			return getMType();
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	
	public TypeCallRef() {
		super(new TypeCallRefImpl());
	}

	public TypeCallRef(CallType mt) {
		super(new TypeCallRefImpl());
		this.ret = new TypeRef(mt.ret());
		for (int i=0; i < mt.arity; i++)
			this.args += new TypeRef(mt.arg(i));
		this.lnk = mt;
	}

	public boolean isBound() {
		return true;
	}
	public Struct getStruct() {
		return null;
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
