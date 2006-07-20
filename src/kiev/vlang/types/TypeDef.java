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

	public static final TypeDef[] emptyArray = new TypeDef[0];

	@virtual typedef This  ≤ TypeDef;

	public ArgMetaType ameta_type;
	
	@getter public TypeDecl get$child_ctx_tdecl() { return this.parent().get$child_ctx_tdecl(); }

	public abstract TypeRef[] getLowerBounds();

	public TypeDef() {}

	public TypeDef(String name) {
		id = name;
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

	public abstract Struct getStruct();
	
	public String toString() {
		return String.valueOf(id);
	}
}

@node
public final class TypeAssign extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeAssign;

	@abstract @virtual
	@ref public:ro TypeRef type_ref;
	
	@getter public TypeRef get$type_ref() {
		if (super_types.length == 0)
			return null;
		return super_types[0];
	}
	
	public TypeRef[] getLowerBounds() { return super_types; }

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
	
	public Struct getStruct() {
		if (super_types.length > 0)
			return super_types[0].getStruct();
		return null;
	}

	public void preResolveOut() {
		ANode parent = parent();
		if (parent instanceof TypeDecl) {
			foreach (TypeRef tr; parent.super_types) {
				TypeDecl td = tr.getTypeDecl();
				ASTNode@ node;
				foreach (td.resolveNameR(node,new ResInfo(this,id.sname,ResInfo.noForwards|ResInfo.noImports))) {
					ASTNode n = node;
					if !(n instanceof TypeDef) {
						Kiev.reportError(this,"Typedef extends non-typedef node");
						continue;
					}
					if (n instanceof TypeAssign)
						Kiev.reportWarning(this,"Typedef extends final typedef");
					TypeConstr tc = (TypeConstr)n;
					if (!tc.isTypeVirtual())
						Kiev.reportWarning(this,"Typedef extends non-virtual typedef");
					//if (!tc.isTypeAbstract())
					//	Kiev.reportWarning(this,"Typedef extends non-abstract typedef");
				}
			}
		}
	}
}

@node
public final class TypeConstr extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	public static final TypeConstr[] emptyArray = new TypeConstr[0];

	@virtual typedef This  = TypeConstr;

	@att public TypeRef[]			lower_bound;

	public TypeRef[] getLowerBounds() { return lower_bound; }

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
	
	public Struct getStruct() {
		foreach (TypeRef tr; super_types) {
			Struct s = tr.getStruct();
			if (s != null)
				return s;
		}
		return null;
	}
	
}


