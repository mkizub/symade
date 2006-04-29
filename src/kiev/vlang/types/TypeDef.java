package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public abstract class TypeDef extends TypeDecl {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeDef;
	@virtual typedef VView = VTypeDef;

	@att public Symbol					id;
	@ref public ArgType					lnk;
	
	public abstract TypeProvider[] getAllSuperTypes();
	public abstract TypeRef[] getUpperBounds();
	public abstract TypeRef[] getLowerBounds();

	@nodeview
	public static abstract view VTypeDef of TypeDef extends VTypeDecl {
		public		Symbol				id;
		public		ArgType				lnk;

		public Struct getStruct();
		public ArgType getAType();
	}

	public TypeDef() {}

	public TypeDef(String name) {
		id = new Symbol(name);
	}

	public TypeDef(Symbol id) {
		this.pos = id.pos;
		this.id = id;
	}

	public Type getType() {
		return getAType();
	}
	public ArgType getAType() {
		if (this.lnk != null)
			return this.lnk;
		if (this.meta != null)
			this.meta.verify();
		this.lnk = new ArgType(id.uname,(TypeDef)this);
		return this.lnk;
	}

	public Symbol getName() { return id; }

	public abstract boolean checkResolved();
	
	public abstract Struct getStruct();
	
	public String toString() {
		return String.valueOf(id);
	}
	public Dumper toJava(Dumper dmp) {
		return dmp.append(this.toString());
	}
}

@node
public final class TypeAssign extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeAssign;
	@virtual typedef VView = VTypeAssign;

	@att public TypeRef				type_ref;
	@ref private TypeProvider[]		super_types;

	public void callbackSuperTypeChanged(TypeDecl chg) {
		super_types = null;
	}

	public TypeProvider[] getAllSuperTypes() {
		if (super_types != null)
			return super_types;
		Vector<TypeProvider> types = new Vector<TypeProvider>();
		addSuperTypes(type_ref, types);
		super_types = types.toArray();
		return super_types;
	}

	public TypeRef[] getUpperBounds() { return type_ref == null ? new TypeRef[0] : new TypeRef[]{type_ref}; }
	public TypeRef[] getLowerBounds() { return type_ref == null ? new TypeRef[0] : new TypeRef[]{type_ref}; }

	@nodeview
	public static final view VTypeAssign of TypeAssign extends VTypeDef {
		public:ro	TypeRef		type_ref;

		public Struct getStruct();
		public ArgType getAType();
	}

	public TypeAssign() {}

	public TypeAssign(String nm) {
		super(nm);
	}

	public TypeAssign(Symbol nm) {
		super(nm);
	}

	public TypeAssign(Symbol nm, TypeRef sup) {
		super(nm);
		this.type_ref = sup;
	}

	public TypeAssign(String nm, Type sup) {
		super(nm);
		this.type_ref = new TypeRef(sup);
	}
	
	public boolean checkResolved() {
		if (type_ref != null)
			type_ref.checkResolved();
		return true;
	}
	
	public Struct getStruct() {
		if (type_ref != null)
			return type_ref.getStruct();
		return null;
	}
}

@node
public final class TypeConstr extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeConstr;
	@virtual typedef VView = VTypeConstr;

	@att public NArr<TypeRef>			upper_bound;
	@att public NArr<TypeRef>			lower_bound;
	@ref private TypeProvider[]			super_types;

	public void callbackSuperTypeChanged(TypeDecl chg) {
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

	public TypeRef[] getUpperBounds() { return upper_bound.toArray(); }
	public TypeRef[] getLowerBounds() { return lower_bound.toArray(); }

	@nodeview
	public static final view VTypeConstr of TypeConstr extends VTypeDef {
		public:ro	NArr<TypeRef>		upper_bound;
		public:ro	NArr<TypeRef>		lower_bound;

		public Struct getStruct();
		public ArgType getAType();
	}

	public TypeConstr() {}

	public TypeConstr(String nm) {
		super(nm);
	}

	public TypeConstr(Symbol nm) {
		super(nm);
	}

	public TypeConstr(Symbol nm, TypeRef sup) {
		super(nm);
		this.upper_bound.add(sup);
	}

	public TypeConstr(String nm, Type sup) {
		super(nm);
		this.upper_bound.add(new TypeRef(sup));
	}
	
	public boolean checkResolved() {
		foreach (TypeRef tr; upper_bound)
			tr.checkResolved();
		return true;
	}
	
	public Struct getStruct() {
		foreach (TypeRef tr; upper_bound) {
			Struct s = tr.getStruct();
			if (s != null)
				return s;
		}
		return null;
	}
	
}


