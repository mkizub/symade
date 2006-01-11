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

public class TypeWithArgsRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeWithArgsRefImpl;
	@virtual typedef VView = TypeWithArgsRefView;

	@node
	public static final class TypeWithArgsRefImpl extends TypeRefImpl {
		@virtual typedef ImplOf = TypeWithArgsRef;
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

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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
		this.lnk = tp.bind(set);
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
