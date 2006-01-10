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

	public Struct getStruct() {
		if (this.lnk != null) return this.lnk.getStruct();
		return base_type.getStruct();
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = base_type.getType();
		if (tp == null || !(tp instanceof CompaundType))
			throw new CompilerException(this,"Compaund type "+base_type+" is not found");
		tp = ((CompaundTypeProvider)tp.meta_type).templ_type;
		TVarSet tpset = tp.bindings();
		TVarSet set = new TVarSet();
		int a = 0;
		for(int b=0; a < args.length && b < tpset.length; b++) {
			if (tpset[b].isBound())
				continue;
			Type bound = args[a].getType();
			if (bound == null)
				throw new CompilerException(this,"Type "+args[a]+" is not found");
			if!(bound.isInstanceOf(tpset[b].var))
				throw new CompilerException(this,"Type "+bound+" is not applayable to "+tpset[b].var);
			set.append(tpset[b].var, bound);
			a++;
		}
		if (a < args.length)
			Kiev.reportError(this,"Type "+tp+" has only "+a+" unbound type parameters");
		tp = tp.bind(set);
		this.lnk = tp;
		return this.lnk;
	}

	public String toString() {
		if (this.lnk != null) {
			return String.valueOf(this.lnk);
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(base_type);
			sb.append('<');
			for (int i=0; i < args.length; i++) {
				sb.append(args[i]);
				if (i < args.length-1) sb.append(',');
			}
			return sb.append('>').toString();
		}
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}
