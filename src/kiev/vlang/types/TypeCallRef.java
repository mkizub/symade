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

@node
public class TypeCallRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeCallRefImpl extends TypeRefImpl {
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

	@ref public abstract virtual access:ro NArr<TypeDef>			targs;
	@att public abstract virtual access:ro NArr<TypeRef>			args;
	@att public abstract virtual           TypeRef					ret;
	
	public NodeView			getNodeView()			{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public TypeCallRefView	getTypeCallRefView()	{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }

	@getter public NArr<TypeDef>		get$targs()		{ return this.getTypeCallRefView().targs; }
	@getter public NArr<TypeRef>		get$args()		{ return this.getTypeCallRefView().args; }
	@getter public TypeRef				get$ret()		{ return this.getTypeCallRefView().ret; }
	@setter public void		set$ret(TypeRef val)		{ this.getTypeCallRefView().ret = val; }
	
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

	public void callbackChildChanged(AttrSlot attr) {
		this.lnk = null;
		if (parent != null && pslot != null) {
			parent.callbackChildChanged(pslot);
		}
	}

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
