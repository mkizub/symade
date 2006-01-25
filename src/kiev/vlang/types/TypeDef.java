package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import syntax kiev.Syntax;

import kiev.vlang.TypeDecl.TypeDeclImpl;
import kiev.vlang.TypeDecl.TypeDeclView;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public class TypeDef extends TypeDecl {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeDefImpl;
	@virtual typedef VView = TypeDefView;

	@nodeimpl
	public static final class TypeDefImpl extends TypeDeclImpl {
		@virtual typedef ImplOf = TypeDef;
		@att public NameRef					name;
		@att public TypeRef					upper_bound;
		@att public TypeRef					lower_bound;
		@att public ArgType					lnk;
		public TypeDefImpl() {}
		public TypeDefImpl(int pos) { super(pos); }
	}
	@nodeview
	public static final view TypeDefView of TypeDefImpl extends TypeDeclView {
		public NameRef				name;
		public TypeRef				upper_bound;
		public TypeRef				lower_bound;
		public ArgType				lnk;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public TypeDef() { super(new TypeDefImpl()); }

	public TypeDef(KString nm) {
		super(new TypeDefImpl());
		name = new NameRef(nm);
	}

	public TypeDef(NameRef nm) {
		super(new TypeDefImpl(nm.pos));
		this.name = nm;
	}

	public TypeDef(NameRef nm, TypeRef sup) {
		super(new TypeDefImpl(nm.pos));
		this.name = nm;
		this.upper_bound = sup;
	}

	public TypeDef(KString nm, Type sup) {
		super(new TypeDefImpl());
		this.name = new NameRef(nm);
		this.upper_bound = new TypeRef(sup);
	}

	public NodeName getName() {
		return new NodeName(name.name);
	}

	public boolean checkResolved() {
		Type t = this.getType();
		if (t != null && t.getStruct() != null)
			return t.getStruct().checkResolved();
		return true;
	}
	
	public Type getSuperType() {
		Type sup = Type.tpObject;
		if (upper_bound != null)
			sup = upper_bound.getType();
		return sup;
	}

	public Struct getStruct() {
		return getSuperType().getStruct();
	}
	
	public void setLowerBound(Type tp) {
		this.lower_bound = new TypeRef(tp);
		this.lnk = null;
	}
	
	public Type getType() {
		return getAType();
	}
	public ArgType getAType() {
		if (this.lnk != null)
			return this.lnk;
		if (this.meta != null)
			this.meta.verify();
		this.lnk = new ArgType(name.name,this);
		return this.lnk;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

