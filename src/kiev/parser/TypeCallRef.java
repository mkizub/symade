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

@node
public class TypeCallRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeCallRefImpl extends TypeRefImpl {
		@att public NArr<TypeRef>			args;
		@att public TypeRef					ret;
		public TypeCallRefImpl() {}
	}
	@nodeview
	public static final view TypeCallRefView of TypeCallRefImpl extends TypeRefView {
		public access:ro	NArr<TypeRef>			args;
		public				TypeRef					ret;
	}

	@att public abstract virtual access:ro NArr<TypeRef>			args;
	@att public abstract virtual           TypeRef					ret;
	
	public NodeView			getNodeView()			{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()			{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()		{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }
	public TypeCallRefView	getTypeCallRefView()	{ return new TypeCallRefView((TypeCallRefImpl)this.$v_impl); }

	@getter public NArr<TypeRef>		get$args()		{ return this.getTypeCallRefView().args; }
	@getter public TypeRef				get$ret()		{ return this.getTypeCallRefView().ret; }
	@setter public void		set$ret(TypeRef val)		{ this.getTypeCallRefView().ret = val; }
	
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
		this.lnk = MethodType.newMethodType(null,atypes,rt);
		return (MethodType)this.lnk;
	}
	public Type getType() {
		return getMType();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
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
