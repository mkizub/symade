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

@node
public class TypeDef extends TypeDecl {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class TypeDefImpl extends TypeDeclImpl {
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

	@att public abstract virtual NameRef				name;
	@att public abstract virtual TypeRef				upper_bound;
	@att public abstract virtual TypeRef				lower_bound;
	@att        abstract virtual ArgType				lnk;
	
	public NodeView			getNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()			{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDeclView		getTypeDeclView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }

	@getter public NameRef		get$name()			{ return this.getTypeDefView().name; }
	@getter public TypeRef		get$upper_bound()	{ return this.getTypeDefView().upper_bound; }
	@getter public TypeRef		get$lower_bound()	{ return this.getTypeDefView().lower_bound; }
	@getter        ArgType		get$lnk()			{ return this.getTypeDefView().lnk; }
	@setter public void		set$name(NameRef val)			{ this.getTypeDefView().name = val; }
	@setter public void		set$upper_bound(TypeRef val)	{ this.getTypeDefView().upper_bound = val; }
	@setter public void		set$lower_bound(TypeRef val)	{ this.getTypeDefView().lower_bound = val; }
	@setter        void		set$lnk(ArgType val)			{ this.getTypeDefView().lnk = val; }
	
	public TypeDef() { super(new TypeDefImpl()); }

	public TypeDef(KString nm) {
		super(new TypeDefImpl());
		name = new NameRef(nm);
	}

	public TypeDef(NameRef nm) {
		super(new TypeDefImpl(nm.getPos()));
		this.name = nm;
	}

	public TypeDef(NameRef nm, TypeRef sup) {
		super(new TypeDefImpl(nm.getPos()));
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
