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

public enum TypeVariance {
	IN_VARIANT,
	CO_VARIANT,
	CONTRA_VARIANT
}

public class VarianceCheckError {
	// the type in which we've found missmatch
	public final Type			base;
	// the type we check
	public final ArgType		at;
	// the expected variance
	public final TypeVariance	variance;
	
	public VarianceCheckError(Type base, ArgType at, TypeVariance variance) {
		this.base = base;
		this.at = at;
		this.variance = variance;
	}
	
	public String toString() {
		return varianceName(at.definer.getVariance())+" type variable "+at
				+" found at "+varianceName(variance)+" position in type "+base;
	}
	private static String varianceName(TypeVariance variance) {
		if (variance == TypeVariance.CO_VARIANT)
			return "co-variant";
		if (variance == TypeVariance.CONTRA_VARIANT)
			return "contra-variant";
		return "in-variant";
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class TypeDef extends TypeDecl {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final TypeDef[] emptyArray = new TypeDef[0];

	@getter public ComplexTypeDecl get$child_ctx_tdecl() { return this.parent().get$child_ctx_tdecl(); }

	public TypeRef[] getUpperBounds() { return super_types; }
	public TypeRef[] getLowerBounds() { return TypeRef.emptyArray; }
	public TypeVariance getVariance() { return TypeVariance.IN_VARIANT; }

	public TypeDef(String name) {
		super(name);
	}

	public void checkResolved() {}

	public Type getType() {
		return getAType();
	}
	public ArgType getAType() {
		if (this.xtype != null)
			return (ArgType)this.xtype;
		this.verifyMetas();
		this.xmeta_type = new ArgMetaType(this);
		return (ArgType)this.xtype;
	}

	public abstract Struct getStruct();
	
	public String toString() {
		return sname;
	}
}

@ThisIsANode(lang=CoreLang)
public final class TypeAssign extends TypeDef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@abstract @virtual
	@nodeData public TypeRef type_ref;
	
	@getter public final TypeRef get$type_ref() {
		if (super_types.length == 0)
			return null;
		return super_types[0];
	}
	
	@setter public final void set$type_ref(TypeRef tr) {
		if (super_types.length == 0)
			super_types.add(tr);
		else
			super_types[0] = tr;
	}
	
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
		if (isTypeVirtual()) {
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
						if (n instanceof TypeAssign) {
							Kiev.reportWarning(this,"Typedef "+parent+"."+sname+" extends final typedef");
						} else {
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
	}

	public void postVerify() {
		// check upper bounds
		foreach (TypeRef tr; getUpperBounds()) {
			Type t = tr.getType();
			VarianceCheckError err = t.checkVariance(t,TypeVariance.IN_VARIANT);
			if (err != null)
				Kiev.reportWarning(this, err.toString());
		}
	}
}

@ThisIsANode(lang=CoreLang)
public final class TypeConstr extends TypeDef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final TypeConstr[] emptyArray = new TypeConstr[0];

	@nodeAttr public TypeRef∅			lower_bound;
	@nodeAttr public TypeVariance		variance;

	public TypeRef[] getLowerBounds() { return lower_bound; }
	
	public TypeVariance getVariance() {
		TypeVariance tv = this.variance;
		if (tv == null)
			return TypeVariance.IN_VARIANT;
		return tv;
	}

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
	
	public void postVerify() {
		if !(parent() instanceof Method) {
			TypeVariance variance = getVariance();
			
			// check upper bounds
			foreach (TypeRef tr; getUpperBounds()) {
				Type t = tr.getType();
				VarianceCheckError err = t.checkVariance(t,variance);
				if (err != null)
					Kiev.reportWarning(this, err.toString());
			}
			
			// check lower bounds with inverted variance
			if (variance == TypeVariance.CO_VARIANT)
				variance = TypeVariance.CONTRA_VARIANT;
			else if (variance == TypeVariance.CONTRA_VARIANT)
				variance = TypeVariance.CO_VARIANT;
			foreach (TypeRef tr; getLowerBounds()) {
				Type t = tr.getType();
				VarianceCheckError err = t.checkVariance(t,variance);
				if (err != null)
					Kiev.reportWarning(this, err.toString());
			}
		}
	}
}


