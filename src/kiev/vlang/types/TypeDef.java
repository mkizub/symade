/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.vlang.types;

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

	public TypeDef(String name) {
		super(name);
	}

	public String qname() {
		return sname;
	}

	public boolean checkResolved() {
		return true;
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
		return sname;
	}
}

@node
public final class TypeAssign extends TypeDef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeAssign;

	@abstract @virtual
	@ref public TypeRef type_ref;
	
	@getter @ref public TypeRef get$type_ref() {
		if (super_types.length == 0)
			return null;
		return ANode.getVersion(super_types[0]);
	}
	
	@setter public void set$type_ref(TypeRef tr) {
		if (super_types.length == 0)
			super_types.add(tr);
		else
			super_types[0] = tr;
	}
	
	public TypeRef[] getLowerBounds() { return super_types; }

	public TypeAssign() {
		super(null);
	}
	public TypeAssign(String name) {
		super(name);
	}
	public TypeAssign(String name, TypeRef sup) {
		super(name);
		this.super_types.add(sup);
	}
	public TypeAssign(String name, Type sup) {
		super(name);
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
				ResInfo info = new ResInfo(this,this.sname,ResInfo.noForwards|ResInfo.noImports);
				foreach (td.resolveNameR(node,info)) {
					ASTNode n = node;
					if !(n instanceof TypeDef) {
						Kiev.reportError(this,"Typedef "+parent+"."+sname+" extends non-typedef node");
						continue;
					}
					if (n instanceof TypeAssign)
						Kiev.reportWarning(this,"Typedef "+parent+"."+sname+" extends final typedef");
					TypeConstr tc = (TypeConstr)n;
					if (!tc.isTypeVirtual())
						Kiev.reportWarning(this,"Typedef "+parent+"."+sname+" extends non-virtual typedef");
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

	public TypeConstr() {
		super(null);
	}
	public TypeConstr(String name) {
		super(name);
	}
	public TypeConstr(String name, TypeRef sup) {
		super(name);
		this.super_types.add(sup);
	}
	public TypeConstr(String name, Type sup) {
		super(name);
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


