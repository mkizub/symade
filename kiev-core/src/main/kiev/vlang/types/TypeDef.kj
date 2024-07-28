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
		return varianceName(at.definer.getVarianceSafe())+" type variable "+at
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

	public TypeRef[] getUpperBounds() { return super_types; }
	public TypeRef[] getLowerBounds() { return TypeRef.emptyArray; }
	public TypeVariance getVarianceSafe() { return TypeVariance.IN_VARIANT; }

	public TypeDef(Symbol symbol) {
		super(new AHandle(), symbol);
	}

	public void checkResolved(Env env) {}

	public Type getType(Env env) {
		return getAType(env);
	}
	public ArgType getAType(Env env) {
		ArgMetaType mt = (ArgMetaType)env.tenv.getExistingMetaType(this.symbol);
		if (mt != null)
			return mt.atype;
		this.verifyMetas();
		return getMetaType(env).atype;
	}

	public ArgMetaType getMetaType(Env env) {
		synchronized (env.tenv) {
			MetaType mt = env.tenv.getExistingMetaType(this.symbol);
			if (mt != null)
				return (ArgMetaType)mt;
			return new ArgMetaType(env.getTypeEnv(),this);
		}
	}
	
	public abstract Struct getStruct(Env env);
	
	public String toString() {
		return sname;
	}
}

@ThisIsANode(lang=CoreLang)
public final class TypeAssign extends TypeDef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@AttrBinDumpInfo(ignore=true)
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
		super(new Symbol());
	}
	public TypeAssign(Symbol symbol) {
		super(symbol);
	}
	public TypeAssign(Symbol symbol, TypeRef sup) {
		super(symbol);
		this.super_types.add(sup);
	}
	public TypeAssign(Symbol symbol, Type sup) {
		super(symbol);
		this.super_types.add(new TypeRef(sup));
	}
	
	public Struct getStruct(Env env) {
		if (super_types.length > 0)
			return super_types[0].getStruct(env);
		return null;
	}

	public void preResolveOut(Env env, INode parent, AttrSlot slot) {
		if (isTypeVirtual()) {
			ANode parent = parent();
			if (parent instanceof TypeDecl) {
				foreach (TypeRef tr; parent.super_types) {
					TypeDecl td = tr.getTypeDecl(env);
					ResInfo info = new ResInfo(env,this,this.sname,ResInfo.noForwards|ResInfo.noSyntaxContext);
					foreach (td.resolveNameR(info)) {
						DNode dn = info.resolvedDNode();
						if !(dn instanceof TypeDef) {
							Kiev.reportError(this,"Typedef "+parent+"."+sname+" extends non-typedef node");
							continue;
						}
						if (dn instanceof TypeAssign) {
							Kiev.reportWarning(this,"Typedef "+parent+"."+sname+" extends final typedef");
						} else {
							TypeConstr tc = (TypeConstr)dn;
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

	public void postVerify(Env env, INode parent, AttrSlot slot) {
		// check upper bounds
		foreach (TypeRef tr; getUpperBounds()) {
			Type t = tr.getType(env);
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
	
	public TypeVariance getVarianceSafe() {
		TypeVariance tv = this.variance;
		if (tv == null)
			return TypeVariance.IN_VARIANT;
		return tv;
	}

	public TypeConstr() {
		super(new Symbol());
	}
	public TypeConstr(Symbol symbol) {
		super(symbol);
	}
	public TypeConstr(Symbol symbol, TypeRef sup) {
		super(symbol);
		this.super_types.add(sup);
	}
	public TypeConstr(Symbol symbol, Type sup) {
		super(symbol);
		this.super_types.add(new TypeRef(sup));
	}

	public void cleanupOnReload() {
		super.cleanupOnReload();
		lower_bound.delAll();
		variance = null;
	}
	
	public Struct getStruct(Env env) {
		foreach (TypeRef tr; super_types) {
			Struct s = tr.getStruct(env);
			if (s != null)
				return s;
		}
		return null;
	}
	
	public void postVerify(Env env, INode parent, AttrSlot slot) {
		if !(parent() instanceof Method) {
			TypeVariance variance = getVarianceSafe();
			
			// check upper bounds
			foreach (TypeRef tr; getUpperBounds()) {
				Type t = tr.getType(env);
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
				Type t = tr.getType(env);
				VarianceCheckError err = t.checkVariance(t,variance);
				if (err != null)
					Kiev.reportWarning(this, err.toString());
			}
		}
	}
}


