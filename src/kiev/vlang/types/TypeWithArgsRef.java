package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeWithArgsRef extends TypeNameRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeWithArgsRef;
	@virtual typedef VView = VTypeWithArgsRef;

	@att public TypeRef[]			args;

	@nodeview
	public static final view VTypeWithArgsRef of TypeWithArgsRef extends VTypeNameRef {
		public:ro	TypeRef[]			args;
	}

	public TypeWithArgsRef() {}

	public TypeWithArgsRef(TypeRef outer, SymbolRef nm) {
		super(outer, nm);
	}

	public TypeWithArgsRef(TypeNameRef tnr) {
		super(tnr);
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp = super.getType();
		if (tp == null)
			throw new CompilerException(this,"Type "+super.toString()+" is not found");
		TVarSet tpset = tp.meta_type.getTemplBindings();
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

	public boolean isBound() {
		return true;
	}

	public String toString() {
		if (this.lnk != null) {
			return String.valueOf(this.lnk);
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(super.toString());
			sb.append('<');
			for (int i=0; i < args.length; i++) {
				sb.append(args[i]);
				if (i < args.length-1) sb.append(',');
			}
			return sb.append('>').toString();
		}
	}
}
