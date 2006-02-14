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

	@virtual typedef This  = TypeDef;
	@virtual typedef NImpl = TypeDefImpl;
	@virtual typedef VView = TypeDefView;

	@nodeimpl
	public static final class TypeDefImpl extends TypeDeclImpl {
		@virtual typedef ImplOf = TypeDef;
		@att public NameRef					name;
		@att public NArr<TypeRef>			upper_bound;
		@att public NArr<TypeRef>			lower_bound;
		@ref public ArgType					lnk;
		@ref private TypeProvider[]			super_types;

		public void callbackSuperTypeChanged(TypeDeclImpl chg) {
			super_types = null;
		}
		
		public TypeProvider[] getAllSuperTypes() {
			if (super_types != null)
				return super_types;
			Vector<TypeProvider> types = new Vector<TypeProvider>();
			foreach (TypeRef up; upper_bound)
				addSuperTypes(up, types);
			if (types.length == 0)
				super_types = TypeProvider.emptyArray;
			else
				super_types = types.toArray();
			return super_types;
		}
	}
	@nodeview
	public static final view TypeDefView of TypeDefImpl extends TypeDeclView {
		public		NameRef				name;
		public:ro	NArr<TypeRef>		upper_bound;
		public:ro	NArr<TypeRef>		lower_bound;
		public		ArgType				lnk;

		public Struct getStruct() {
			foreach (TypeRef tr; upper_bound) {
				Struct s = tr.getStruct();
				if (s != null)
					return s;
			}
			return null;
		}
	
		public Type getType() {
			return getAType();
		}
		public ArgType getAType() {
			if (this.lnk != null)
				return this.lnk;
			if (this.meta != null)
				this.meta.verify();
			this.lnk = new ArgType(name.name,((TypeDefImpl)this)._self);
			return this.lnk;
		}
	
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }

	public TypeDef() { super(new TypeDefImpl()); }

	public TypeDef(KString nm) {
		super(new TypeDefImpl());
		name = new NameRef(nm);
	}

	public TypeDef(NameRef nm) {
		this();
		this.pos = nm.pos;
		this.name = nm;
	}

	public TypeDef(NameRef nm, TypeRef sup) {
		this();
		this.pos = nm.pos;
		this.name = nm;
		this.upper_bound.add(sup);
	}

	public TypeDef(KString nm, Type sup) {
		super(new TypeDefImpl());
		this.name = new NameRef(nm);
		this.upper_bound.add(new TypeRef(sup));
	}

	public NodeName getName() {
		return new NodeName(name.name);
	}

	public boolean checkResolved() {
		foreach (TypeRef tr; upper_bound)
			tr.checkResolved();
		return true;
	}
	
	public Struct getStruct() {
		return getVView().getStruct();
	}
	
	public void setLowerBound(Type tp) {
		this.lower_bound.add(new TypeRef(tp));
		this.lnk = null;
	}

	public String toString() {
		return String.valueOf(name.name);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

