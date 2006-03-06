package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

import kiev.vlang.types.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public class TypeWithArgsRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeWithArgsRef;
	@virtual typedef VView = TypeWithArgsRefView;

	@att public NArr<TypeRef>			args;
	@att public TypeRef					base_type;

	@nodeview
	public static final view TypeWithArgsRefView of TypeWithArgsRef extends TypeRefView {
		public:ro	NArr<TypeRef>			args;
		public		TypeRef					base_type;

		public Type getType() {
			if (this.lnk != null)
				return this.lnk;
			Type tp = base_type.getType();
			if (tp == null || !(tp instanceof CompaundType))
				throw new CompilerException(this,"Compaund type "+base_type+" is not found");
			TVarSet tpset = ((CompaundTypeProvider)tp.meta_type).getTemplBindings();
			TVarBld set = new TVarBld();
			int a = 0;
			for(int b=0; a < args.length && b < tpset.tvars.length; b++) {
				if (tpset.tvars[b].unalias().val != null)
					continue;
				Type bound = args[a].getType();
				if (bound == null)
					throw new CompilerException(this,"Type "+args[a]+" is not found");
				if!(bound.isInstanceOf(tpset.tvars[b].var))
					throw new CompilerException(this,"Type "+bound+" is not applayable to "+tpset.tvars[b].var);
				set.append(tpset.tvars[b].var, bound);
				a++;
			}
			if (a < args.length)
				Kiev.reportError(this,"Type "+tp+" has only "+a+" unbound type parameters");
			tp = tp.meta_type.make(set);
			this.lnk = tp;
			return this.lnk;
		}
	}

	public TypeWithArgsRef() {}

	public TypeWithArgsRef(TypeRef base) {
		this.pos = base.pos;
		this.base_type = base;
	}

	public boolean isBound() {
		return true;
	}

	public Struct getStruct() {
		if (this.lnk != null) return this.lnk.getStruct();
		return base_type.getStruct();
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
