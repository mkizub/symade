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

import kiev.be.java15.JType;
import kiev.be.java15.JBaseType;
import kiev.be.java15.JArrayType;
import kiev.be.java15.JMethodType;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class Type extends AType {

	@virtual typedef MType  ≤ MetaType;

	public static Type[]	emptyArray = new Type[0];

	public			JType				jtype;
	
	public abstract JType getJType();
	public abstract String toString();
	public abstract Type getErasedType();
	
	public final Type[] getMetaSupers() {
		return meta_type.getMetaSupers(this);
	}

	// construct new type from this one
	public final Type make(TVarBld set) {
		bindings(); // update this type, if outdated
		return meta_type.make(set);
	}
	// accessor.field
	public final Type applay(TVSet bindings) {
		//bindings(); // update this type, if outdated
		return meta_type.applay(this,bindings);
	}
	// rebind with lower bound or outer type, etc
	public final Type rebind(TVarBld set) {
		bindings(); // update this type, if outdated
		return meta_type.rebind(this,set);
	}
	
	public Struct getStruct() { return null; }
	public MNode getMeta(String name) { return null; }

	protected Type(MType meta_type, TemplateTVarSet template, int flags, TVarBld bindings)
		require { meta_type != null; }
	{
		super(meta_type, template, flags, bindings);
	}

	protected Type(MType meta_type, TemplateTVarSet template, int flags)
		require { meta_type != null; }
	{
		super(meta_type, template, flags);
	}

	public final rule resolveCallAccessR(Method@ node, ResInfo info, CallType mt) {
		meta_type.resolveCallAccessR(this,node,info,mt)
	}

	public final rule resolveNameAccessR(ASTNode@ node, ResInfo info) {
		meta_type.resolveNameAccessR(this,node,info)
	}

	public boolean isInstanceOf(Type t2) alias xfx operator ≥ {
		Type t1 = this;
		if( t1 ≡ t2 || t2 ≡ Type.tpAny ) return true;
		if( t1.isReference() && t2 ≈ Type.tpObject ) return true;
		if( t1 ≡ Type.tpNull && t2.isReference() ) return true;
		if (t2 instanceof WildcardCoType) {
			return this.isInstanceOf(t2.getEnclosedType());
		}
		if (t2 instanceof ArgType) {
			ArgType at = (ArgType)t2;
			if (at.definer.super_types.length > 0) {
				foreach (TypeRef tr; at.definer.super_types) {
					if (!this.isInstanceOf(tr.getType()))
						return false;
				}
			}
			if (at.definer.getLowerBounds().length > 0) {
				foreach (TypeRef tr; at.definer.getLowerBounds()) {
					if (!tr.getType().isInstanceOf(this))
						return false;
				}
			}
			return true;
		}
		if (t1.meta_type.tdecl.instanceOf(t2.meta_type.tdecl)) {
			t1.bindings();
			t2.bindings();
			int n = t2.getArgsLength();
			for(int i=0; i < n; i++) {
				if (t2.isAliasArg(i))
					continue;
				ArgType a2 = t2.getArg(i);
				Type r2 = t2.resolveArg(i);
				if (a2 ≡ r2)
					continue;
				Type r1 = t1.resolve(a2);
				if (r1 ≈ r2 || r1 ≡ Type.tpNull && r2.isReference())
					continue;
				if (a2.name == "This" && r1.meta_type.tdecl.instanceOf(r2.meta_type.tdecl))
					continue;
				if (a2.isCoVariant() && r1.isInstanceOf(r2))
					continue;
				if (a2.isContraVariant() && r2.isInstanceOf(r1))
					continue;
				// before we can declare N to be covariant in NodeSpace<+N extends ANode>
				if (t1.isArray() && t2.isArray() && r1.isInstanceOf(r2))
					continue;
				if ((r1 instanceof ArgType || r2 instanceof ArgType) && r1.isInstanceOf(r2))
					continue;
				return false;
			}
			return true;
		}
		return false;
	}

	public Type getAutoCastTo(Type t)
	{
		if( t ≡ tpVoid ) return tpVoid;
		if( t ≡ tpAny ) return tpAny;
		if( this.isReference() && t.isReference() && (this ≡ tpNull || t ≡ tpNull) ) return this;
		if( this.isInstanceOf(t) ) return this;
		if( this ≡ tpRule && t ≡ tpBoolean ) return tpBoolean;
		if( this.isBoolean() && t.isBoolean() ) return tpBoolean;
		if( this.isReference() && !t.isReference() ) {
			if !(t instanceof CoreType) return null;
			if (((CoreType)t).getRefTypeForPrimitive() ≈ this) return t;
			else if (t ≡ Type.tpInt && this ≥ Type.tpEnum)
				return t;
		}
		if( this instanceof CTimeType || t instanceof CTimeType ) {
			if( this instanceof CTimeType && t instanceof CTimeType )
				return this.getUnboxedType().getAutoCastTo(t.getUnboxedType());
			else if( this instanceof CTimeType )
				return this.getUnboxedType().getAutoCastTo(t);
			//else if( t instanceof CTimeType )
			//	return this.getAutoCastTo(t.getUnboxedType());
		}
		return null;
	}

	public Type betterCast(Type t1, Type t2) {
		if( equals(t1) ) return t1;
		if( equals(t2) ) return t2;
		if( isBoolean() && t1.isBoolean() ) return t1;
		if( isBoolean() && t2.isBoolean() ) return t2;
		if( this.isReference() ) {
			if( t1.isReference() && !t2.isReference() ) return t1;
			else if( !t1.isReference() && t2.isReference() ) return t2;
			else if( !t1.isReference() && !t2.isReference() ) return null;
			if( this ≡ tpNull ) return null;
			if( isInstanceOf(t1) ) {
				if( !isInstanceOf(t2) ) return t1;
				else if( t2.isInstanceOf(t1) ) return t2;
				else return t1;
			}
			else if( isInstanceOf(t2) ) return t2;
			if( t1 instanceof CTimeType && t2 instanceof CTimeType ) {
				Type tp1 = t1.getUnboxedType();
				Type tp2 = t2.getUnboxedType();
				Type tp_better = betterCast(tp1,tp2);
				if( tp_better != null ) {
					if( tp_better ≡ tp1 ) return t1;
					if( tp_better ≡ tp2 ) return t2;
				}
			}
			return null;
		}
		return null;
	}

	public static Type leastCommonType(Type tp1, Type tp2) {
		Type tp = tp1;
		while( tp != null ) {
			if( tp1.isInstanceOf(tp) && tp2.isInstanceOf(tp) )
				return tp;
			Type[] stps = tp.getMetaSupers();
			if (stps.length > 0)
				tp = stps[0];
			else
				return null;
		}
		return tp;
	}

	public boolean isCastableTo(Type t) {
		if( this.isReference() && t ≡ tpNull || t ≡ Type.tpAny  ) return true;
		foreach (Type st; t.getMetaSupers(); this.isCastableTo(st))
			return true;
		return false;
	}

	public Type getUnboxedType()					{ throw new RuntimeException("Type "+this+" is not a box type"); }
	public Type getEnclosedType()					{ throw new RuntimeException("Type "+this+" is not a box type"); }
	public ENode makeUnboxedExpr(ENode from)		{ throw new RuntimeException("Type "+this+" is not a box type"); } 
	
	public static Type getRealType(Type t1, TypeRef t2) {
		return Type.getRealType(t1, t2.type_lnk);
	}
	public static Type getRealType(TypeRef t1, Type t2) {
		return Type.getRealType(t1.type_lnk, t2);
	}
	public static Type getRealType(TypeRef t1, TypeRef t2) {
		return Type.getRealType(t1.type_lnk, t2.type_lnk);
	}
	public static Type getRealType(Type t1, Type t2) {
		trace(Kiev.debug && Kiev.debugResolve,"Get real type of "+t2+" in "+t1);
		if( t1 == null || t2 == null )	return t2;
		return t2.applay(t1);
	}

	public static CompaundType getProxyType(Type tp) {
		TVarBld set = new TVarBld();
		set.append(tpRefProxy.tdecl.args[0].getAType(), tp);
		return (CompaundType)((CompaundMetaType)tpRefProxy.meta_type).make(set);
	}

	public final Field resolveField(String name) { return meta_type.tdecl.resolveField(name); }


	// checks the type 'base' for argument 'at' to confirm variance 'variance'
	private static VarianceCheckError checkArgVariance(Type base, ArgType at, TypeVariance variance) {
		if (at.isInVariant())
			return null;
		if (at.isCoVariant()) {
			if (variance == TypeVariance.CO_VARIANT)
				return null;
			return new VarianceCheckError(base, at, variance);
		}
		if (at.isContraVariant()) {
			if (variance == TypeVariance.CONTRA_VARIANT)
				return null;
			return new VarianceCheckError(base, at, variance);
		}
		assert ("Unexpected variance "+at.definer.getVariance()+" of "+at);
		return null;
	}
	
	public VarianceCheckError checkVariance(TypeVariance variance) {
		return checkVariance(this,variance);
	}
	public VarianceCheckError checkVariance(Type base, TypeVariance variance) {
/*		if (this instanceof ArgType)
			return checkArgVariance(base,(ArgType)this,variance);
		foreach (TVar tv; this.bindings().tvars; !tv.isAlias()) {
			Type t = tv.unalias(this).result();
			TypeVariance check_variance;
			if (tv.var.isInVariant())
				check_variance = TypeVariance.IN_VARIANT;
			else if (tv.var.isCoVariant())
				check_variance = variance;
			else if (variance == TypeVariance.CO_VARIANT)
				check_variance = TypeVariance.CONTRA_VARIANT;
			else if (variance == TypeVariance.CONTRA_VARIANT)
				check_variance = TypeVariance.CO_VARIANT;
			else
				check_variance = variance;
			if (t instanceof ArgType) {
				VarianceCheckError err = checkArgVariance(base,(ArgType)t,check_variance);
				if (err != null)
					return err;
			} else {
				VarianceCheckError err = t.checkVariance(base,check_variance);
				if (err != null)
					return err;
			}
		}
*/		return null;
	}
	
}

public final class XType extends Type {

	@virtual typedef MType = XMetaType;

	@getter
	public final MetaTypeDecl get$tdecl() { return (MetaTypeDecl)meta_type.tdecl; }

	public XType(MetaType meta_type, TemplateTVarSet template, TVarBld bindings) {
		super(meta_type, template, 0, bindings);
	}
	
	public JType getJType() {
		if (jtype == null) {
			foreach (Type t; getMetaSupers()) {
				jtype = t.getJType();
				if (jtype != null)
					return jtype;
			}
		}
		return jtype;
	}
	public Type getErasedType() {
		foreach (Type t; getMetaSupers(); t != null && t != Type.tpVoid)
			return t.getErasedType();
		return Type.tpVoid;
	}

	public Struct getStruct()					{ return null; }
	public MNode getMeta(String name)			{ return null; }

	public String toString() {
		TypeDecl tdecl = this.tdecl;
		StringBuffer str = new StringBuffer();
		str.append(tdecl.qname().replace('\u001f','.'));
		int n = tdecl.args.length;
		if (n > 0) {
			str.append('<');
			for(int i=0; i < n; i++) {
				str.append(resolve(tdecl.args[i].getAType()));
				if( i < n-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ Type.tpAny ) return true;
		if( this.isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		return super.isCastableTo(t);
	}
}

public final class CoreType extends Type {

	@virtual typedef MType = CoreMetaType;

	public final String name;
	CoreType(String name, Type super_type, int meta_flags) {
		super(new CoreMetaType(name,super_type,meta_flags), TemplateTVarSet.emptySet, 0);
		((CoreMetaType)meta_type).core_type = this;
		((CoreMetaType)meta_type).tdecl.xtype = this;
		this.name = name.intern();
	}
	public MNode getMeta(String name)		{ return null; }
	public Type getErasedType()				{ return this; }
	public String toString()				{ return name.toString(); }

	public JType getJType()					{ return this.jtype; }
	
	public Type getAutoCastTo(Type t)
	{
		if( t ≡ tpVoid ) return tpVoid;
		if( t ≡ tpAny ) return tpAny;
		if( this.isBoolean() && t.isBoolean() ) return tpBoolean;
		if( this ≡ tpByte && (t ≡ tpShort || t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return t;
		if( (this ≡ tpShort || this ≡ tpChar) && (t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return t;
		if( this ≡ tpInt && (t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return t;
		if( this ≡ tpLong && t ≡ tpDouble ) return t;
		if( this ≡ tpFloat && t ≡ tpDouble ) return t;
		if( this ≡ tpNull && t.isReference() ) return t;
		if( !this.isReference() && t.isReference() ) {
			if( this.getRefTypeForPrimitive() ≈ t ) return t;
			else if( this ≡ Type.tpInt && t ≥ Type.tpEnum ) return t;
		}
		return super.getAutoCastTo(t);
	}

	public boolean isCastableTo(Type t) {
		if( this ≡ t || t ≡ Type.tpAny ) return true;
		if( this.isNumber() && t.isNumber() ) return true;
		if( t.isReference() && this ≡ tpNull ) return true;
		if( t.getStruct() != null && t.getStruct().isEnum())
			return this.isCastableTo(Type.tpInt);
		return super.isCastableTo(t);
	}

	public Type betterCast(Type t1, Type t2) {
		if(this ≡ t1) return t1;
		if(this ≡ t2) return t2;
		if( isBoolean() && t1.isBoolean() ) return t1;
		if( isBoolean() && t2.isBoolean() ) return t2;
		if( isNumber() ) {
			if( isInteger() ) {
				if( this ≡ tpByte )
					if     ( t1 ≡ tpShort  || t2 ≡ tpShort  ) return tpShort;
					else if( t1 ≡ tpInt    || t2 ≡ tpInt    ) return tpInt;
					else if( t1 ≡ tpLong   || t2 ≡ tpLong   ) return tpLong;
					else if( t1 ≡ tpFloat  || t2 ≡ tpFloat  ) return tpFloat;
					else if( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
				else if( this ≡ tpChar )
					if     ( t1 ≡ tpShort  || t2 ≡ tpShort  ) return tpShort;
					else if( t1 ≡ tpInt    || t2 ≡ tpInt    ) return tpInt;
					else if( t1 ≡ tpLong   || t2 ≡ tpLong   ) return tpLong;
					else if( t1 ≡ tpFloat  || t2 ≡ tpFloat  ) return tpFloat;
					else if( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
				else if( this ≡ tpShort )
					if     ( t1 ≡ tpInt    || t2 ≡ tpInt    ) return tpInt;
					else if( t1 ≡ tpLong   || t2 ≡ tpLong   ) return tpLong;
					else if( t1 ≡ tpFloat  || t2 ≡ tpFloat  ) return tpFloat;
					else if( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
				else if( this ≡ tpInt )
					if     ( t1 ≡ tpLong   || t2 ≡ tpLong   ) return tpLong;
					else if( t1 ≡ tpFloat  || t2 ≡ tpFloat  ) return tpFloat;
					else if( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
			} else {
				if( this ≡ tpFloat )
					if     ( t1 ≡ tpFloat  || t2 ≡ tpFloat  ) return tpFloat;
					else if( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
				else if( this ≡ tpDouble )
					if     ( t1 ≡ tpDouble || t2 ≡ tpDouble ) return tpDouble;
					else return null;
			}
		}
		return super.betterCast(t1, t2);
	}

	public static Type upperCastNumbers(Type tp1, Type tp2) {
		assert( tp1.isNumber() );
		assert( tp2.isNumber() );
		if( tp1 ≡ Type.tpDouble || tp2 ≡ Type.tpDouble) return Type.tpDouble;
		if( tp1 ≡ Type.tpFloat  || tp2 ≡ Type.tpFloat ) return Type.tpFloat;
		if( tp1 ≡ Type.tpLong   || tp2 ≡ Type.tpLong  ) return Type.tpLong;
		if( tp1 ≡ Type.tpInt    || tp2 ≡ Type.tpInt   ) return Type.tpInt;
		if( tp1 ≡ Type.tpChar   || tp2 ≡ Type.tpChar  ) return Type.tpChar;
		if( tp1 ≡ Type.tpShort  || tp2 ≡ Type.tpShort ) return Type.tpShort;
		if( tp1 ≡ Type.tpByte   || tp2 ≡ Type.tpByte  ) return Type.tpByte;
		throw new RuntimeException("Bad number types "+tp1+" or "+tp2);
	}

	public CompaundType getRefTypeForPrimitive() {
		if     ( this ≡ Type.tpBoolean) return Type.tpBooleanRef;
		else if( this ≡ Type.tpByte   ) return Type.tpByteRef;
		else if( this ≡ Type.tpShort  ) return Type.tpShortRef;
		else if( this ≡ Type.tpInt    ) return Type.tpIntRef;
		else if( this ≡ Type.tpLong   ) return Type.tpLongRef;
		else if( this ≡ Type.tpFloat  ) return Type.tpFloatRef;
		else if( this ≡ Type.tpDouble ) return Type.tpDoubleRef;
		else if( this ≡ Type.tpChar   ) return Type.tpCharRef;
		else if( this ≡ Type.tpVoid   ) return Type.tpVoidRef;
		else
			throw new RuntimeException("No reference type for "+this);
	}

}

public final class ASTNodeType extends Type {

	@virtual typedef MType = ASTNodeMetaType;

	public static ASTNodeType newASTNodeType(Type tp)
		alias lfy operator new
	{
		Class clazz = null;
		if      (tp == StdTypes.tpBoolean)   clazz = Boolean.TYPE;
		else if (tp == StdTypes.tpChar)      clazz = Character.TYPE;
		else if (tp == StdTypes.tpByte)      clazz = Byte.TYPE;
		else if (tp == StdTypes.tpShort)     clazz = Short.TYPE;
		else if (tp == StdTypes.tpInt)       clazz = Integer.TYPE;
		else if (tp == StdTypes.tpLong)      clazz = Long.TYPE;
		else if (tp == StdTypes.tpFloat)     clazz = Float.TYPE;
		else if (tp == StdTypes.tpDouble)    clazz = Double.TYPE;
		else if (tp instanceof CompaundType) clazz = Class.forName(tp.getJType().toClassForNameString());
		else
			throw new RuntimeException("Can't make ASTNodeType for type "+tp);
		return new ASTNodeType(ASTNodeMetaType.instance(clazz));
	}

	public static ASTNodeType newASTNodeType(Class clazz)
		alias lfy operator new
	{
		return new ASTNodeType(ASTNodeMetaType.instance(clazz));
	}

	public static ASTNodeType newASTNodeType(RewritePattern rp)
		alias lfy operator new
	{
		ASTNodeMetaType meta_type = (ASTNodeMetaType)rp.vtype.getType().meta_type;
		TVarBld tvb = new TVarBld();
		foreach (RewritePattern var; rp.vars) {
			ASTNodeType ast = newASTNodeType(var);
			String name = ("attr$"+var.sname+"$type").intern();
			foreach (TVar tv; meta_type.getTemplBindings().tvars; tv.var.name == name) {
				tvb.append(tv.var, ast);
				break;
			}
		}
		return new ASTNodeType(meta_type, tvb);
	}

	private ASTNodeType(ASTNodeMetaType meta_type) {
		super(meta_type, null, 0, null);
	}

	private ASTNodeType(ASTNodeMetaType meta_type, TVarBld bindings) {
		super(meta_type, meta_type.getTemplBindings(), 0, bindings);
	}

	public MNode getMeta(String name)		{ return null; }
	public Type getErasedType()				{ return this; }
	public String toString()				{ return ((ASTNodeMetaType)meta_type).clazz.getName()+"#"; }

	public JType getJType()					{ throw new RuntimeException("ASTNodeType.getJType()"); }
	
	public Struct getStruct() {
		return null;
	}

	public boolean isCastableTo(Type t) {
		if( this ≡ t) return true;
		return super.isCastableTo(t);
	}

}

public final class ArgType extends Type {

	@virtual typedef MType = ArgMetaType;

	public static final ArgType[] emptyArray = new ArgType[0];
	
	public final String name;
	
	@getter public final TypeDef get$definer() { return (TypeDef)meta_type.tdecl; }

	private static int makeFlags(ArgMetaType mt) {
		TypeDef definer = (TypeDef)mt.tdecl;
		int flags = flValAppliable;
		if (definer.isTypeAbstract())   flags |= flAbstract | flArgAppliable;
		if (definer.isTypeUnerasable()) flags |= flUnerasable;
		if (definer.isTypeVirtual())    flags |= flVirtual;
		if (definer.isTypeFinal())      flags |= flFinal;
		if (definer.isTypeStatic())     flags |= flStatic;
		if (definer.isTypeForward())    flags |= flForward;
		return flags;
	}
	
	public ArgType(ArgMetaType meta_type) {
		super(meta_type, TemplateTVarSet.emptySet, makeFlags(meta_type));
		this.name = meta_type.tdecl.sname;
	}
	
	public JType getJType() {
		if (jtype == null)
			jtype = getErasedType().getJType();
		return jtype;
	}

	public MNode getMeta(String name)				{ return definer.meta == null ? null : definer.getMeta(name); }
	public Struct getStruct()						{ return definer.getStruct(); }
	public Type getErasedType() {
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tpObject;
		return up[0].getType().getErasedType();
	}

	public String toString() {
		return String.valueOf(definer.sname);
	}
	
	public boolean isCoVariant() { return definer.getVariance() == TypeVariance.CO_VARIANT; }
	public boolean isContraVariant() { return definer.getVariance() == TypeVariance.CONTRA_VARIANT; }
	public boolean isInVariant() { return definer.getVariance() == TypeVariance.IN_VARIANT; }

	public boolean isCastableTo(Type t) {
		if( this ≡ t || t ≡ Type.tpAny ) return true;
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tpObject.isCastableTo(t);
		foreach (TypeRef tr; up) {
			if (tr.getType().isCastableTo(t))
				return true;
		}
		return false;
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tpAny) return true;
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tpObject.isInstanceOf(t);
		foreach (TypeRef tr; up) {
			if (tr.getType().isInstanceOf(t))
				return true;
		}
		return false;
	}
	
	public boolean checkBindings(Type base, Type t) {
		if (this ≡ t) return true;
		return checkUpperBounds(base, t) && checkLowerBounds(base, t);
	}
	
	private boolean checkUpperBounds(Type base, Type t) {
		TypeRef[] types = definer.getUpperBounds();
		foreach (TypeRef tr; types) {
			Type bnd = tr.getType();
			while (bnd instanceof ArgType) {
				Type res = base.resolve((ArgType)bnd);
				if (res == bnd)
					break;
				bnd = res;
			}
			if (!t.isInstanceOf(bnd))
				return false;
		}
		return true;
	}

	private boolean checkLowerBounds(Type base, Type t) {
		TypeRef[] types = definer.getLowerBounds();
		foreach (TypeRef tr; types) {
			Type bnd = tr.getType();
			while (bnd instanceof ArgType) {
				Type res = base.resolve((ArgType)bnd);
				if (res == bnd)
					break;
				bnd = res;
			}
			if (!bnd.isInstanceOf(t))
				return false;
		}
		return true;
	}
}

public final class CompaundType extends Type {

	@virtual typedef MType = CompaundMetaType;

	@getter
	public final Struct get$tdecl() { return (Struct)meta_type.tdecl; }

	public CompaundType(CompaundMetaType meta_type, TemplateTVarSet template, TVarBld bindings) {
		super(meta_type, template, 0, bindings);
	}
	
	public final JType getJType() {
		if (jtype == null)
			jtype = new JBaseType((Struct)tdecl);
		return jtype;
	}

	public Struct getStruct()					{ return (Struct)tdecl; }
	public MNode getMeta(String name)			{ return tdecl.getMeta(name); }
	public Type getErasedType()					{ return tdecl.xtype; }

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(tdecl.toString());
		int n = tdecl.args.length;
		if (n > 0) {
			str.append('<');
			for(int i=0; i < n; i++) {
				str.append(resolve(tdecl.args[i].getAType()));
				if( i < n-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public Type getAutoCastTo(Type t)
	{
		if( t ≡ tpVoid ) return t;
		if( t ≡ tpAny ) return t;
		if( isInstanceOf(t) ) return this;
		if( this.tdecl.isStructView() && ((KievView)this.tdecl).view_of.getType().getAutoCastTo(t) != null ) return t;
		if( t instanceof CoreType && !t.isReference() ) {
			if( t.getRefTypeForPrimitive() ≈ this )
				return t;
			else if( t ≡ Type.tpInt && this ≥ Type.tpEnum )
				return t;
		}
		return super.getAutoCastTo(t);
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tpNull || t ≡ Type.tpAny ) return true;
		if( this.isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( t.isReference() && t.getStruct() != null &&
			(this.getStruct().isInterface() || t.getStruct().isInterface())
			)
			return true;
		return super.isCastableTo(t);
	}
}

public final class ArrayType extends Type {

	@virtual typedef MType = ArrayMetaType;

	@getter public Type get$arg() { return this.resolveArg(0); }
	
	public static ArrayType newArrayType(Type tp)
		alias lfy operator new
	{
		return new ArrayType(tp);
	}
	
	private ArrayType(Type arg) {
		super(ArrayMetaType.instance, ArrayMetaType.instance.getTemplBindings(), 0, new TVarBld(tpArrayArg, arg));
	}

	public JType getJType() {
		if (jtype == null) {
			jtype = new JArrayType(this.arg.getJType());
		}
		return jtype;
	}

	public MNode getMeta(String name)				{ return null; }
	
	public Type getErasedType() {
		return newArrayType(arg.getErasedType());
	}

	public String toString() {
		return String.valueOf(arg)+"[]";
	}

	public boolean isCastableTo(Type t) {
		if( t ≡ tpNull || t ≡ tpAny ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		return super.isCastableTo(t);
	}

}

public abstract class CTimeType extends Type {

	protected CTimeType(MType meta_type, int flags, ArgType arg, Type enclosed_type) {
		super(meta_type, meta_type.getTemplBindings(), flags, new TVarBld(arg, enclosed_type));
	}

	public abstract ENode makeUnboxedExpr(ENode from); // returns an expression of unboxed type
	public abstract ENode makeInitExpr(Var dn, ENode init); // returns an expression of enclosed type 
	public abstract Type getUnboxedType();

	public Type getEnclosedType()	{ return this.resolveArg(0); }
}

public final class WildcardCoType extends CTimeType {

	@virtual typedef MType = WildcardCoMetaType;
	
	public WildcardCoType(Type base_type) {
		super(WildcardCoMetaType.instance, 0, tpWildcardCoArg, base_type);
	}
	
	public JType getJType() {
		if (jtype == null)
			jtype = getEnclosedType().getJType();
		return jtype;
	}

	public final ENode makeUnboxedExpr(ENode from) { from }
	public final ENode makeInitExpr(Var dn, ENode init) { init }
	public final Type getUnboxedType()	{ getEnclosedType() }
	
	public Struct getStruct()				{ return getEnclosedType().getStruct(); }
	public MNode getMeta(String name)		{ return getEnclosedType().getMeta(name); }

	public String toString() {
		return getEnclosedType() + "\u207a"; // Type⁺ superscript ⁺
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tpAny) return true;
		if (getEnclosedType().isInstanceOf(t))
			return true;
		return false;
	}

	public Type getErasedType() {
		return getEnclosedType().getErasedType();
	}

}

public final class WildcardContraType extends CTimeType {

	@virtual typedef MType = WildcardContraMetaType;
	
	public WildcardContraType(Type base_type) {
		super(WildcardContraMetaType.instance, 0, tpWildcardContraArg, base_type);
	}
	
	public JType getJType() {
		if (jtype == null)
			jtype = getEnclosedType().getJType();
		return jtype;
	}

	public final ENode makeUnboxedExpr(ENode from) { from }
	public final ENode makeInitExpr(Var dn, ENode init) { init }
	public final Type getUnboxedType()	{ getEnclosedType() }
	
	public Struct getStruct()				{ return getEnclosedType().getStruct(); }
	public MNode getMeta(String name)		{ return getEnclosedType().getMeta(name); }

	public String toString() {
		return getEnclosedType() + "\u207b"; // Type⁻ superscript ⁻
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tpAny) return true;
		if (t.isInstanceOf(getEnclosedType()))
			return true;
		return false;
	}

	public Type getErasedType() {
		return getEnclosedType().getErasedType();
	}

}

public final class WrapperType extends CTimeType {

	@virtual typedef MType = WrapperMetaType;
	
	public static Type newWrapperType(Type tp) {
		return new WrapperType(tp);
	}
	
	public WrapperType(Type unwrapped_type) {
		super(WrapperMetaType.instance(unwrapped_type), 0, tpWrapperArg, unwrapped_type);
	}

	@getter
	private Field get$wrapped_field() { return ((WrapperMetaType)this.meta_type).field; }
	
	public JType getJType() {
		if (jtype == null)
			jtype = getEnclosedType().getJType();
		return jtype;
	}

	public final ENode makeUnboxedExpr(ENode from) {
		Field wf = wrapped_field;
		if (wf == null)
			return from;
		return new IFldExpr(from.pos, ~from, wf);
	} 
	public final ENode makeInitExpr(Var dn, ENode init) {
		if (dn.isInitWrapper())
			return init;
		dn.setInitWrapper(true);
		ENode e;
		if (init != null && init.isForWrapper())
			e = init;
		else if (init == null)
			e = new NewExpr(dn.pos,getEnclosedType(),ENode.emptyArray);
		else
			e = new NewExpr(init.pos,getEnclosedType(),new ENode[]{~init});
		e = new ReinterpExpr(e.pos,this,~e);
		e.setForWrapper(true);
		
		return e;
	}
	
	public final Type getUnboxedType()	{
		Field wf = wrapped_field;
		if (wf == null)
			return getEnclosedType();
		return Type.getRealType(getEnclosedType(), wf.getType());
	}
	
	public Struct getStruct()				{ return getEnclosedType().getStruct(); }
	public MNode getMeta(String name)		{ return getEnclosedType().getMeta(name); }

	public String toString() {
		return getEnclosedType().toString()+'\u229b'; // PVar<String>⊛ - wrapper type for PVar<String>
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		if( this.getUnboxedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tpAny) return true;
		if (t instanceof WrapperType)
			return getEnclosedType().isInstanceOf(t.getEnclosedType());
		return false;
	}

	public Type getErasedType() {
		return getEnclosedType().getErasedType();
	}

}

public final class TupleType extends Type {

	@virtual typedef MType = TupleMetaType;

	public  final int		arity;
	
	TupleType(TupleMetaType meta_type, TVarBld bindings) {
		super(meta_type,meta_type.getTemplBindings(),0,bindings);
		this.arity = meta_type.arity;
	}

	public JType getJType() { StdTypes.tpVoid.getJType() }

	public Type getErasedType() {
		if (arity == 0)
			return this;
		TVarBld set = new TVarBld();
		for (int i=0; i < arity; i++)
			set.append(getArg(i), resolveArg(i).getErasedType());
		return new TupleType((TupleMetaType)meta_type,set);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for (int i=0; i < arity; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(resolveArg(i));
		}
		sb.append(')');
		return sb.toString();
	}
}

public final class CallType extends Type {

	@virtual typedef MType = CallMetaType;

	public  final int		arity;

	CallType(CallMetaType meta_type, TVarBld bld, int arity)
	{
		super(meta_type, meta_type.getTemplBindings(), 0, bld);
		this.arity = arity;
		assert(this.getArgsLength() >= 2);
		assert(this.getArg(0) == tpCallRetArg);
		assert(this.getArg(1) == tpCallTupleArg);
		assert(this.resolveArg(1).meta_type == TupleMetaType.instancies[arity]);
	}
	
	public static CallType createCallType(Type accessor, Type[] bnd_targs, Type[] args, Type ret, boolean is_closure)
		alias lfy operator new
	{
		bnd_targs = (bnd_targs != null && bnd_targs.length > 0) ? bnd_targs : Type.emptyArray;
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? Type.tpAny : ret;
		TVarBld vs = new TVarBld();
		vs.append(tpCallRetArg, ret);
		TVarBld ts = new TVarBld();
		for (int i=0; i < args.length; i++)
			ts.append(tpCallParamArgs[i], args[i]);
		vs.append(tpCallTupleArg, new TupleType(TupleMetaType.instancies[args.length], ts));
		if (accessor != null)
			vs.append(tpSelfTypeArg, accessor);
		ArgType[] targs = ArgType.emptyArray;
		if (bnd_targs.length > 0) {
			targs = new ArgType[bnd_targs.length];
			for (int i=0; i < bnd_targs.length; i++) {
				vs.append(tpUnattachedArgs[i], bnd_targs[i]);
				targs[i] = tpUnattachedArgs[i];
			}
		}
		return new CallType(CallMetaType.newCallMetaType(accessor==null,is_closure,targs), vs, args.length);
	}

	public static CallType createCallType(Method meth, Type[] args, Type ret)
		alias lfy operator new
	{
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? Type.tpAny : ret;
		TVarBld vs = new TVarBld();
		vs.append(tpCallRetArg, ret);
		TVarBld ts = new TVarBld();
		for (int i=0; i < args.length; i++)
			ts.append(tpCallParamArgs[i], args[i]);
		vs.append(tpCallTupleArg, new TupleType(TupleMetaType.instancies[args.length], ts));
		Type accessor = null;
		if (!meth.meta.is_static && !meth.is_mth_virtual_static)
			accessor = meth.ctx_tdecl.xtype;
		if (accessor != null)
			vs.append(tpSelfTypeArg, tpSelfTypeArg /*accessor*/);
		ArgType[] targs = ArgType.emptyArray;
		if (meth.targs.length > 0) {
			TypeConstr[] mtargs = meth.targs;
			targs = new ArgType[mtargs.length];
			for (int i=0; i < mtargs.length; i++) {
				ArgType at = mtargs[i].getAType();
				vs.append(at, null);
				targs[i] = at;
			}
		}
		return new CallType(CallMetaType.newCallMetaType(accessor==null,false,targs), vs, args.length);
	}

	public CallType toCallTypeRetAny() {
		TVarBld vs = rebind_bld(new TVarBld(tpCallRetArg, tpAny));
		return new CallType((CallMetaType)this.meta_type, vs, this.arity);
	}
	
	public Type ret() {
		AType bindings = this.bindings();
		assert (bindings.getArg(0) ≡ tpCallRetArg);
		return bindings.resolveArg(0).applay(bindings);
	}
	
	public Type arg(int idx) {
		AType bindings = this.bindings();
		assert (bindings.getArg(1) ≡ tpCallTupleArg);
		TupleType tp = (TupleType)bindings.resolveArg(1);
		return tp.resolveArg(idx).applay(bindings);
	}
	
	public Type[] params() {
		int arity = this.arity;
		if (arity == 0)
			return Type.emptyArray;
		Type[] params = new Type[arity];
		for (int i=0; i < arity; i++)
			params[i] = this.arg(i);
		return params;
	}

	public JType getJType() {
		if (jtype == null) {
			if (this.isReference()) {
				jtype = Type.tpClosure.getJType();
			} else {
				JType[] jargs = JType.emptyArray;
				if (arity > 0) {
					jargs = new JType[arity];
					for (int i=0; i < arity; i++) {
						jargs[i] = arg(i).getJType();
						assert (!(jargs[i] instanceof JMethodType));
					}
				}
				JType jret = ret().getJType();
				assert (!(jret instanceof JMethodType));
				jtype = new JMethodType((CallMetaType)meta_type, jargs, jret);
			}
		}
		return jtype;
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( this.isReference() && t ≡ tpNull ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.arity == 0 && !(t instanceof CallType) && this.ret().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tpAny) return true;
		if (t instanceof CallType) {
			CallType ct = (CallType)t;
			if( this.arity != ct.arity ) return false;
			for(int i=0; i < this.arity; i++)
				if( !ct.arg(i).isInstanceOf(this.arg(i)) ) return false;
			if( !this.ret().isInstanceOf(ct.ret()) ) return false;
			return true;
		}
		if (this.isReference())
			return super.isInstanceOf(t);
		return false;
	}

	public Type getAutoCastTo(Type t)
	{
		Type r = super.getAutoCastTo(t);
		if (r != null ) return r;
		if (this.arity == 0 && !(t instanceof CallType))
			return this.ret().getAutoCastTo(t);
		return null;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append('(');
		if (arity > 0) {
			for(int i=0; i < arity; i++) {
				str.append(arg(i));
				if( i < arity-1)
					str.append(',');
			}
		}
		str.append(")->").append(ret());
		return str.toString();
	}

	public boolean greater(CallType tp) {
		if( this.arity != tp.arity ) return false;
		if( !ret().isInstanceOf(tp.ret()) ) return false;
		boolean gt = false;
		for(int i=0; i < arity; i++) {
			Type t1 = this.arg(i);
			Type t2 = tp.arg(i);
			if (t1 ≉ t2) {
				if( t1.isInstanceOf(t2) ) {
					trace(Kiev.debug && Kiev.debugMultiMethod,"Type "+t1+" is greater then "+t2);
					gt = true;
				} else {
					trace(Kiev.debug && Kiev.debugMultiMethod,"Types "+t1+" and "+t2+" are uncomparable");
					return false;
				}
			} else {
				trace(Kiev.debug && Kiev.debugMultiMethod,"Types "+t1+" and "+t2+" are equals");
			}
		}
		return gt;
	}

	public boolean isMultimethodSuper(CallType tp) {
		if( this.arity != tp.arity ) return false;
		if( tp.ret() ≉ this.ret() ) return false;
		for(int i=0; i < arity; i++) {
			if( !this.arg(i).equals(tp.arg(i)) )
				return false;
		}
		return true;
	}

	public Type getErasedType() {
		if (this.isReference())
			return Type.tpClosure;
		if( this.arity == 0 )
			return new CallType(null,null,null,this.ret().getErasedType(),false);
		Type[] targs = new Type[this.arity];
		for(int i=0; i < this.arity; i++)
			targs[i] = this.arg(i).getErasedType();
		return new CallType(null,null,targs,this.ret().getErasedType(),false);
	}

}

