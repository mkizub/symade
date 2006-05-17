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

	public ArgMetaType ameta_type;
	
	public abstract TypeRef[] getLowerBounds();

	@nodeview
	public static abstract view VTypeDef of TypeDef extends VTypeDecl {
		public		ArgMetaType				ameta_type;

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
		if (this.ameta_type != null)
			return this.ameta_type.atype;
		if (this.meta != null)
			this.meta.verify();
		this.ameta_type = new ArgMetaType(this);
		return this.ameta_type.atype;
	}

//	public abstract boolean checkResolved();
	
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

	public TypeRef[] getLowerBounds() { return super_types.getArray(); }

	@nodeview
	public static final view VTypeAssign of TypeAssign extends VTypeDef {
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
		this.super_types.add(sup);
	}

	public TypeAssign(String nm, Type sup) {
		super(nm);
		this.super_types.add(new TypeRef(sup));
	}
	
//	public boolean checkResolved() {
//		foreach (TypeRef tr; super_types)
//			tr.checkResolved();
//		return true;
//	}
	
	public Struct getStruct() {
		if (super_types.length > 0)
			return super_types[0].getStruct();
		return null;
	}
}

@node
public final class TypeConstr extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeConstr;
	@virtual typedef VView = VTypeConstr;

	@att public NArr<TypeRef>			lower_bound;

	public TypeRef[] getLowerBounds() { return lower_bound.getArray(); }

	@nodeview
	public static final view VTypeConstr of TypeConstr extends VTypeDef {
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
		this.super_types.add(sup);
	}

	public TypeConstr(String nm, Type sup) {
		super(nm);
		this.super_types.add(new TypeRef(sup));
	}
	
//	public boolean checkResolved() {
//		foreach (TypeRef tr; super_types)
//			tr.checkResolved();
//		foreach (TypeRef tr; lower_bound)
//			tr.checkResolved();
//		return true;
//	}
	
	public Struct getStruct() {
		foreach (TypeRef tr; super_types) {
			Struct s = tr.getStruct();
			if (s != null)
				return s;
		}
		return null;
	}
	
}


