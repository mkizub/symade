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
public class TypeWithArgsRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeWithArgsRefImpl extends TypeRefImpl {
		@att public NArr<TypeRef>			args;
		@att public TypeRef					base_type;
		public TypeWithArgsRefImpl() {}
		public TypeWithArgsRefImpl(int pos) { super(pos, null); }
	}
	@nodeview
	public static final view TypeWithArgsRefView of TypeWithArgsRefImpl extends TypeRefView {
		public access:ro	NArr<TypeRef>			args;
		public				TypeRef					base_type;
	}

	@att public abstract virtual access:ro NArr<TypeRef>			args;
	@att public abstract virtual           TypeRef					base_type;
	
	public NodeView				getNodeView()				{ return new TypeWithArgsRefView((TypeWithArgsRefImpl)this.$v_impl); }
	public ENodeView			getENodeView()				{ return new TypeWithArgsRefView((TypeWithArgsRefImpl)this.$v_impl); }
	public TypeRefView			getTypeRefView()			{ return new TypeWithArgsRefView((TypeWithArgsRefImpl)this.$v_impl); }
	public TypeWithArgsRefView	getTypeWithArgsRefView()	{ return new TypeWithArgsRefView((TypeWithArgsRefImpl)this.$v_impl); }

	@getter public NArr<TypeRef>		get$args()		{ return this.getTypeWithArgsRefView().args; }
	@getter public TypeRef				get$base_type()	{ return this.getTypeWithArgsRefView().base_type; }
	@setter public void		set$base_type(TypeRef val)	{ this.getTypeWithArgsRefView().base_type = val; }
	
	public TypeWithArgsRef() {
		super(new TypeWithArgsRefImpl());
	}

	public TypeWithArgsRef(TypeRef base) {
		super(new TypeWithArgsRefImpl(base.pos));
		this.base_type = base;
	}

	public boolean isBound() {
		return true;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = base_type.getType();
		if (tp == null || !(tp instanceof BaseType))
			throw new CompilerException(this,"Type "+base_type+" is not found");
		Type[] atypes = new Type[args.length];
		for(int i=0; i < atypes.length; i++) {
			atypes[i] = args[i].getType();
			if (atypes[i] == null)
				throw new CompilerException(this,"Type "+args[i]+" is not found");
		}
		this.lnk = Type.newRefType((BaseType)tp,atypes);
		return this.lnk;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (this.lnk != null)
			sb.append(this.lnk.getClazzName());
		else
			sb.append(base_type);
		sb.append('<');
		for (int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if (i < args.length-1) sb.append(',');
		}
		return sb.append('>').toString();
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
