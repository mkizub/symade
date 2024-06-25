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

public abstract class Type extends AType {

	@virtual typedef MType  ≤ MetaType;

	public static Type[]	emptyArray = new Type[0];

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

	public final rule resolveCallAccessR(ResInfo info, CallType mt) {
		meta_type.resolveCallAccessR(this,info,mt)
	}

	public final rule resolveNameAccessR(ResInfo info) {
		meta_type.resolveNameAccessR(this,info)
	}

	public boolean isInstanceOf(Type t2) operator "V ≥ V" {
		Type t1 = this;
		if( t1 ≡ t2 || t2 ≡ tenv.tpAny ) return true;
		if( t1.isReference() && t2 ≈ tenv.tpObject ) return true;
		if( t1 ≡ tenv.tpNull && t2.isReference() ) return true;
		if (t2 instanceof WildcardCoType) {
			return this.isInstanceOf(t2.getEnclosedType());
		}
		if (t2 instanceof ArgType) {
			ArgType at = (ArgType)t2;
			if (at.definer.super_types.length > 0) {
				foreach (TypeRef tr; at.definer.super_types) {
					if (!this.isInstanceOf(tr.getType(tenv.env)))
						return false;
				}
			}
			if (at.definer.getLowerBounds().length > 0) {
				foreach (TypeRef tr; at.definer.getLowerBounds()) {
					if (!tr.getType(tenv.env).isInstanceOf(this))
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
				if (r1 ≈ r2 || r1 ≡ tenv.tpNull && r2.isReference())
					continue;
				if (a2.name == "This" && r1.meta_type.tdecl.instanceOf(r2.meta_type.tdecl))
					continue;
				if ((a2.isCoVariant() || r2 instanceof WildcardCoType) && r1.isInstanceOf(r2))
					continue;
				if ((a2.isContraVariant() || r2 instanceof WildcardContraType)&& r2.isInstanceOf(r1))
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
		StdTypes tenv = this.tenv;
		if( t ≡ tenv.tpVoid ) return tenv.tpVoid;
		if( t ≡ tenv.tpAny ) return tenv.tpAny;
		if( this.isReference() && t.isReference() && (this ≡ tenv.tpNull || t ≡ tenv.tpNull) ) return this;
		if( this.isInstanceOf(t) ) return this;
		if( this ≡ tenv.tpRule && t ≡ tenv.tpBoolean ) return tenv.tpBoolean;
		if( this.isBoolean() && t.isBoolean() ) return tenv.tpBoolean;
		if( this.isReference() && !t.isReference() ) {
			if !(t instanceof CoreType) return null;
			if (((CoreType)t).getRefTypeForPrimitive() ≈ this) return t;
			else if (t ≡ tenv.tpInt && this ≥ tenv.tpEnum)
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
			if( this ≡ tenv.tpNull ) return null;
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
		if( this.isReference() && t ≡ tenv.tpNull || t ≡ tenv.tpAny  ) return true;
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
		set.append(tp.tenv.tpRefProxy.tdecl.args[0].getAType(tp.tenv.env), tp);
		return (CompaundType)((CompaundMetaType)tp.tenv.tpRefProxy.meta_type).make(set);
	}

	public final Field resolveField(String name) { return meta_type.tdecl.resolveField(tenv.env, name); }


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
		Debug.assert ("Unexpected variance "+at.definer.getVarianceSafe()+" of "+at);
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

	@virtual @abstract
	public:ro MetaTypeDecl		tdecl;

	@getter
	public final MetaTypeDecl get$tdecl() { return (MetaTypeDecl)meta_type.tdecl; }

	public XType(MetaType meta_type, TemplateTVarSet template, TVarBld bindings) {
		super(meta_type, template, 0, bindings);
	}
	
	public Type getErasedType() {
		foreach (Type t; getMetaSupers(); t != null && t != tenv.tpVoid)
			return t.getErasedType();
		return tenv.tpVoid;
	}

	public Struct getStruct() {
		foreach (Type t; getMetaSupers()) {
			Struct s = t.getStruct();
			if (s != null)
				return s;
		}
		return null;
	}
	
	public MNode getMeta(String name) {
		return tdecl.getMeta(name);
	}

	public String toString() {
		TypeDecl tdecl = this.tdecl;
		StringBuffer str = new StringBuffer();
		str.append(tdecl.qname().replace('·','.'));
		int n = tdecl.args.length;
		if (n > 0) {
			str.append('<');
			for(int i=0; i < n; i++) {
				str.append(resolve(tdecl.args[i].getAType(tenv.env)));
				if( i < n-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tenv.tpAny ) return true;
		if( this.isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		return super.isCastableTo(t);
	}
}

public final class CoreType extends Type {

	@virtual typedef MType = CoreMetaType;

	public final String name;
	CoreType(StdTypes tenv, String name, Type super_type, int meta_flags) {
		super(new CoreMetaType(tenv,name,super_type,meta_flags), TemplateTVarSet.emptySet, 0);
		((CoreMetaType)meta_type).core_type = this;
		this.name = name.intern();
	}
	public MNode getMeta(String name)		{ return null; }
	public Type getErasedType()				{ return this; }
	public String toString()				{ return name.toString(); }

	public Type getAutoCastTo(Type t)
	{
		StdTypes tenv = this.tenv;
		if( t ≡ tenv.tpVoid ) return tenv.tpVoid;
		if( t ≡ tenv.tpAny ) return tenv.tpAny;
		if( this.isBoolean() && t.isBoolean() ) return tenv.tpBoolean;
		if( this ≡ tenv.tpByte && (t ≡ tenv.tpShort || t ≡ tenv.tpInt || t ≡ tenv.tpLong || t ≡ tenv.tpFloat || t ≡ tenv.tpDouble) ) return t;
		if( (this ≡ tenv.tpShort || this ≡ tenv.tpChar) && (t ≡ tenv.tpInt || t ≡ tenv.tpLong || t ≡ tenv.tpFloat || t ≡ tenv.tpDouble) ) return t;
		if( this ≡ tenv.tpInt && (t ≡ tenv.tpLong || t ≡ tenv.tpFloat || t ≡ tenv.tpDouble) ) return t;
		if( this ≡ tenv.tpLong && t ≡ tenv.tpDouble ) return t;
		if( this ≡ tenv.tpFloat && t ≡ tenv.tpDouble ) return t;
		if( this ≡ tenv.tpNull && t.isReference() ) return t;
		if( !this.isReference() && t.isReference() ) {
			if( this.getRefTypeForPrimitive() ≈ t ) return t;
			//CompaundType reftp = this.getRefTypeForPrimitive();
			//if( reftp.isInstanceOf(t) ) return reftp;
			else if( this ≡ tenv.tpInt && t ≥ tenv.tpEnum ) return t;
		}
		return super.getAutoCastTo(t);
	}

	public boolean isCastableTo(Type t) {
		if( this ≡ t || t ≡ tenv.tpAny ) return true;
		if( this.isNumber() && t.isNumber() ) return true;
		if( t.isReference() && this ≡ tenv.tpNull ) return true;
		if( t.getStruct() != null && t.getStruct().isEnum())
			return this.isCastableTo(tenv.tpInt);
		return super.isCastableTo(t);
	}

	public Type betterCast(Type t1, Type t2) {
		if(this ≡ t1) return t1;
		if(this ≡ t2) return t2;
		if( isBoolean() && t1.isBoolean() ) return t1;
		if( isBoolean() && t2.isBoolean() ) return t2;
		StdTypes tenv = this.tenv;
		if( isNumber() ) {
			if( isInteger() ) {
				if( this ≡ tenv.tpByte )
					if     ( t1 ≡ tenv.tpShort  || t2 ≡ tenv.tpShort  ) return tenv.tpShort;
					else if( t1 ≡ tenv.tpInt    || t2 ≡ tenv.tpInt    ) return tenv.tpInt;
					else if( t1 ≡ tenv.tpLong   || t2 ≡ tenv.tpLong   ) return tenv.tpLong;
					else if( t1 ≡ tenv.tpFloat  || t2 ≡ tenv.tpFloat  ) return tenv.tpFloat;
					else if( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
				else if( this ≡ tenv.tpChar )
					if     ( t1 ≡ tenv.tpShort  || t2 ≡ tenv.tpShort  ) return tenv.tpShort;
					else if( t1 ≡ tenv.tpInt    || t2 ≡ tenv.tpInt    ) return tenv.tpInt;
					else if( t1 ≡ tenv.tpLong   || t2 ≡ tenv.tpLong   ) return tenv.tpLong;
					else if( t1 ≡ tenv.tpFloat  || t2 ≡ tenv.tpFloat  ) return tenv.tpFloat;
					else if( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
				else if( this ≡ tenv.tpShort )
					if     ( t1 ≡ tenv.tpInt    || t2 ≡ tenv.tpInt    ) return tenv.tpInt;
					else if( t1 ≡ tenv.tpLong   || t2 ≡ tenv.tpLong   ) return tenv.tpLong;
					else if( t1 ≡ tenv.tpFloat  || t2 ≡ tenv.tpFloat  ) return tenv.tpFloat;
					else if( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
				else if( this ≡ tenv.tpInt )
					if     ( t1 ≡ tenv.tpLong   || t2 ≡ tenv.tpLong   ) return tenv.tpLong;
					else if( t1 ≡ tenv.tpFloat  || t2 ≡ tenv.tpFloat  ) return tenv.tpFloat;
					else if( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
			} else {
				if( this ≡ tenv.tpFloat )
					if     ( t1 ≡ tenv.tpFloat  || t2 ≡ tenv.tpFloat  ) return tenv.tpFloat;
					else if( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
				else if( this ≡ tenv.tpDouble )
					if     ( t1 ≡ tenv.tpDouble || t2 ≡ tenv.tpDouble ) return tenv.tpDouble;
					else return null;
			}
		}
		return super.betterCast(t1, t2);
	}

	public static Type upperCastNumbers(Type tp1, Type tp2) {
		assert( tp1.isNumber() );
		assert( tp2.isNumber() );
		StdTypes tenv = tp1.tenv;
		if( tp1 ≡ tenv.tpDouble || tp2 ≡ tenv.tpDouble) return tenv.tpDouble;
		if( tp1 ≡ tenv.tpFloat  || tp2 ≡ tenv.tpFloat ) return tenv.tpFloat;
		if( tp1 ≡ tenv.tpLong   || tp2 ≡ tenv.tpLong  ) return tenv.tpLong;
		if( tp1 ≡ tenv.tpInt    || tp2 ≡ tenv.tpInt   ) return tenv.tpInt;
		if( tp1 ≡ tenv.tpChar   || tp2 ≡ tenv.tpChar  ) return tenv.tpChar;
		if( tp1 ≡ tenv.tpShort  || tp2 ≡ tenv.tpShort ) return tenv.tpShort;
		if( tp1 ≡ tenv.tpByte   || tp2 ≡ tenv.tpByte  ) return tenv.tpByte;
		throw new RuntimeException("Bad number types "+tp1+" or "+tp2);
	}

	public CompaundType getRefTypeForPrimitive() {
		StdTypes tenv = this.tenv;
		if     ( this ≡ tenv.tpBoolean) return tenv.tpBooleanRef;
		else if( this ≡ tenv.tpByte   ) return tenv.tpByteRef;
		else if( this ≡ tenv.tpShort  ) return tenv.tpShortRef;
		else if( this ≡ tenv.tpInt    ) return tenv.tpIntRef;
		else if( this ≡ tenv.tpLong   ) return tenv.tpLongRef;
		else if( this ≡ tenv.tpFloat  ) return tenv.tpFloatRef;
		else if( this ≡ tenv.tpDouble ) return tenv.tpDoubleRef;
		else if( this ≡ tenv.tpChar   ) return tenv.tpCharRef;
		else if( this ≡ tenv.tpVoid   ) return tenv.tpVoidRef;
		else
			throw new RuntimeException("No reference type for "+this);
	}

}

public final class ASTNodeType extends Type {

	@virtual typedef MType = ASTNodeMetaType;

	public static ASTNodeType newASTNodeType(Type tp)
		operator "new T"
	{
		Class clazz = null;
		StdTypes tenv = tp.tenv;
		if      (tp == tenv.tpBoolean)   clazz = Boolean.TYPE;
		else if (tp == tenv.tpChar)      clazz = Character.TYPE;
		else if (tp == tenv.tpByte)      clazz = Byte.TYPE;
		else if (tp == tenv.tpShort)     clazz = Short.TYPE;
		else if (tp == tenv.tpInt)       clazz = Integer.TYPE;
		else if (tp == tenv.tpLong)      clazz = Long.TYPE;
		else if (tp == tenv.tpFloat)     clazz = Float.TYPE;
		else if (tp == tenv.tpDouble)    clazz = Double.TYPE;
		else if (tp instanceof CompaundType) clazz = Class.forName(tp.tdecl.qname().replace('·','.'));
		else
			throw new RuntimeException("Can't make ASTNodeType for type "+tp);
		return new ASTNodeType(ASTNodeMetaType.instance(tenv,clazz));
	}

	public static ASTNodeType newASTNodeType(Class clazz)
		operator "new T"
	{
		StdTypes tenv = Env.getEnv().tenv;
		return new ASTNodeType(ASTNodeMetaType.instance(tenv, clazz));
	}

	public static ASTNodeType newASTNodeType(RewritePattern rp)
		operator "new T"
	{
		ASTNodeMetaType meta_type = (ASTNodeMetaType)rp.var.vtype.getType(Env.getEnv()).meta_type;
		TVarBld tvb = new TVarBld();
		foreach (RewritePattern var; rp.vars) {
			ASTNodeType ast = newASTNodeType(var);
			String name = ("attr$"+var.var.sname+"$type").intern();
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
	
	@virtual @abstract
	public:ro TypeDef		definer;

	@getter public final TypeDef get$definer() { return (TypeDef)meta_type.tdecl; }

	private static int makeFlags(ArgMetaType mt) {
		TypeDef definer = (TypeDef)mt.tdecl;
		int flags = StdTypes.flValAppliable;
		if (definer.isTypeAbstract())   flags |= StdTypes.flAbstract | StdTypes.flArgAppliable;
		if (definer.isTypeUnerasable()) flags |= StdTypes.flUnerasable;
		if (definer.isTypeVirtual())    flags |= StdTypes.flVirtual;
		if (definer.isTypeFinal())      flags |= StdTypes.flFinal;
		if (definer.isTypeStatic())     flags |= StdTypes.flStatic;
		if (definer.isTypeForward())    flags |= StdTypes.flForward;
		return flags;
	}
	
	public ArgType(ArgMetaType meta_type) {
		super(meta_type, TemplateTVarSet.emptySet, makeFlags(meta_type));
		this.name = meta_type.tdecl.sname;
	}
	
	public MNode getMeta(String name)				{ return definer.getMeta(name); }
	public Struct getStruct()						{ return definer.getStruct(); }
	public Type getErasedType() {
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tenv.tpObject;
		return up[0].getType(tenv.env).getErasedType();
	}

	public String toString() {
		return String.valueOf(definer.sname);
	}
	
	public boolean isCoVariant() { return definer.getVarianceSafe() == TypeVariance.CO_VARIANT; }
	public boolean isContraVariant() { return definer.getVarianceSafe() == TypeVariance.CONTRA_VARIANT; }
	public boolean isInVariant() { return definer.getVarianceSafe() == TypeVariance.IN_VARIANT; }

	public boolean isCastableTo(Type t) {
		if( this ≡ t || t ≡ tenv.tpAny ) return true;
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tenv.tpObject.isCastableTo(t);
		foreach (TypeRef tr; up) {
			if (tr.getType(tenv.env).isCastableTo(t))
				return true;
		}
		return false;
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tenv.tpAny) return true;
		TypeRef[] up = definer.super_types;
		if (up.length == 0)
			return tenv.tpObject.isInstanceOf(t);
		foreach (TypeRef tr; up) {
			if (tr.getType(tenv.env).isInstanceOf(t))
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
			Type bnd = tr.getType(tenv.env);
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
			Type bnd = tr.getType(tenv.env);
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

	@virtual @abstract
	public:ro Struct		tdecl;

	@getter
	public final Struct get$tdecl() { return (Struct)meta_type.tdecl; }

	public CompaundType(CompaundMetaType meta_type, TemplateTVarSet template, TVarBld bindings) {
		super(meta_type, template, 0, bindings);
	}
	
	public Struct getStruct()					{ return (Struct)tdecl; }
	public MNode getMeta(String name)			{ return tdecl.getMeta(name); }
	public Type getErasedType()					{ return tdecl.getType(tenv.env); }

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(tdecl.toString());
		int n = tdecl.args.length;
		if (n > 0) {
			str.append('<');
			for(int i=0; i < n; i++) {
				str.append(resolve(tdecl.args[i].getAType(tenv.env)));
				if( i < n-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public Type getAutoCastTo(Type t)
	{
		StdTypes tenv = this.tenv;
		if( t ≡ tenv.tpVoid ) return t;
		if( t ≡ tenv.tpAny ) return t;
		if( isInstanceOf(t) ) return this;
		if( this.tdecl.isStructView() && ((KievView)this.tdecl).view_of.getType(tenv.env).getAutoCastTo(t) != null ) return t;
		if( t instanceof CoreType && !t.isReference() ) {
			if( t.getRefTypeForPrimitive() ≈ this )
				return t;
			else if( t ≡ tenv.tpInt && this ≥ tenv.tpEnum )
				return t;
		}
		return super.getAutoCastTo(t);
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tenv.tpNull || t ≡ tenv.tpAny ) return true;
		if( this.isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( t.isReference() && t.getStruct() != null &&
			(this.getStruct().isInterface() || t.getStruct().isInterface())
			)
			return true;
		return super.isCastableTo(t);
	}
}

public class ArrayType extends Type {

	@virtual typedef MType ≤ ArrayMetaType;

	@virtual @abstract
	public:ro Type		arg;

	@getter public Type get$arg() { return this.resolveArg(0); }
	
	public static ArrayType newArrayType(Type tp)
		operator "new T"
	{
		return new ArrayType(tp, tp.meta_type.tenv);
	}
	
	private ArrayType(Type arg, StdTypes tenv) {
		super(tenv.arrayMetaType, tenv.arrayTemplBindings, 0, new TVarBld(tenv.tpArrayArg, arg));
	}
	
	protected ArrayType(VarargMetaType meta_type, TemplateTVarSet template, int flags, TVarBld bindings) {
		super(meta_type, template, flags, bindings);
	}

	public MNode getMeta(String name)				{ return null; }
	
	public Type getErasedType() {
		return newArrayType(arg.getErasedType());
	}

	public String toString() {
		return String.valueOf(arg)+"[]";
	}

	public boolean isCastableTo(Type t) {
		if( t ≡ tenv.tpNull || t ≡ tenv.tpAny ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		return super.isCastableTo(t);
	}

}

public final class VarargType extends ArrayType {

	@virtual typedef MType = VarargMetaType;

	@virtual @abstract
	public:ro Type		arg;

	@getter public Type get$arg() { return this.resolveArg(0); }
	
	public static VarargType newVarargType(Type tp)
		operator "new T"
	{
		return new VarargType(tp, tp.meta_type.tenv);
	}
	
	private VarargType(Type arg, StdTypes tenv) {
		super(tenv.varargMetaType, tenv.varargTemplBindings, 0, new TVarBld(tenv.tpVarargArg, arg));
	}

	public MNode getMeta(String name)				{ return null; }
	
	public Type getErasedType() {
		return newArrayType(arg.getErasedType());
	}

	public String toString() {
		return String.valueOf(arg)+"...";
	}

	public boolean isCastableTo(Type t) {
		if( t ≡ tenv.tpNull || t ≡ tenv.tpAny ) return true;
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
	
	public static WildcardCoType newWildcardCoType(Type tp)
		operator "new T"
	{
		return new WildcardCoType(tp, tp.meta_type.tenv);
	}
	
	private WildcardCoType(Type base_type, StdTypes tenv) {
		super(tenv.wildcardCoMetaType, 0, tenv.tpWildcardCoArg, base_type);
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
		if( t ≡ tenv.tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tenv.tpAny) return true;
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
	
	public static WildcardContraType newWildcardContraType(Type tp)
		operator "new T"
	{
		return new WildcardContraType(tp, tp.meta_type.tenv);
	}
	
	private WildcardContraType(Type base_type, StdTypes tenv) {
		super(tenv.wildcardContraMetaType, 0, tenv.tpWildcardContraArg, base_type);
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
		if( t ≡ tenv.tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tenv.tpAny) return true;
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
		super(WrapperMetaType.instance(unwrapped_type.tenv, unwrapped_type), 0, unwrapped_type.tenv.tpWrapperArg, unwrapped_type);
	}

	@virtual @abstract
	public:ro Field		wrapped_field;

	@getter
	private Field get$wrapped_field() { return ((WrapperMetaType)this.meta_type).field; }
	
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
		return Type.getRealType(getEnclosedType(), wf.getType(tenv.env));
	}
	
	public Struct getStruct()				{ return getEnclosedType().getStruct(); }
	public MNode getMeta(String name)		{ return getEnclosedType().getMeta(name); }

	public String toString() {
		return getEnclosedType().toString()+'\u229b'; // PVar<String>⊛ - wrapper type for PVar<String>
	}

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( t ≡ tenv.tpNull ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.getEnclosedType().isCastableTo(t) )
			return true;
		if( this.getUnboxedType().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tenv.tpAny) return true;
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
		assert(this.getArg(0) == tenv.tpCallRetArg);
		assert(this.getArg(1) == tenv.tpCallTupleArg);
		assert(this.resolveArg(1).meta_type == tenv.tupleMetaTypes[arity]);
	}
	
	public static CallType createCallType(Type accessor, Type[] bnd_targs, Type[] args, Type ret, boolean is_closure)
		operator "new T"
	{
		StdTypes tenv = ret.tenv;
		bnd_targs = (bnd_targs != null && bnd_targs.length > 0) ? bnd_targs : Type.emptyArray;
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? tenv.tpAny : ret;
		TVarBld vs = new TVarBld();
		vs.append(tenv.tpCallRetArg, ret);
		TVarBld ts = new TVarBld();
		for (int i=0; i < args.length; i++)
			ts.append(tenv.tpCallParamArgs[i], args[i]);
		vs.append(tenv.tpCallTupleArg, new TupleType(tenv.tupleMetaTypes[args.length], ts));
		if (accessor != null)
			vs.append(tenv.tpSelfTypeArg, accessor);
		ArgType[] targs = ArgType.emptyArray;
		if (bnd_targs.length > 0) {
			targs = new ArgType[bnd_targs.length];
			for (int i=0; i < bnd_targs.length; i++) {
				vs.append(tenv.tpUnattachedArgs[i], bnd_targs[i]);
				targs[i] = tenv.tpUnattachedArgs[i];
			}
		}
		return new CallType(CallMetaType.newCallMetaType(tenv,accessor==null,is_closure,targs), vs, args.length);
	}

	public static CallType createCallType(Method meth, Type[] args, Type ret)
		operator "new T"
	{
		StdTypes tenv = ret.tenv;
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? tenv.tpAny : ret;
		TVarBld vs = new TVarBld();
		vs.append(tenv.tpCallRetArg, ret);
		TVarBld ts = new TVarBld();
		for (int i=0; i < args.length; i++)
			ts.append(tenv.tpCallParamArgs[i], args[i]);
		vs.append(tenv.tpCallTupleArg, new TupleType(tenv.tupleMetaTypes[args.length], ts));
		Type accessor = null;
		if (!meth.mflags_is_static && !meth.is_mth_virtual_static)
			accessor = Env.ctxTDecl(meth).getType(tenv.env);
		if (accessor != null)
			vs.append(tenv.tpSelfTypeArg, tenv.tpSelfTypeArg /*accessor*/);
		ArgType[] targs = ArgType.emptyArray;
		if (meth.targs.length > 0) {
			TypeConstr[] mtargs = meth.targs;
			targs = new ArgType[mtargs.length];
			for (int i=0; i < mtargs.length; i++) {
				ArgType at = mtargs[i].getAType(tenv.env);
				vs.append(at, null);
				targs[i] = at;
			}
		}
		return new CallType(CallMetaType.newCallMetaType(tenv,accessor==null,false,targs), vs, args.length);
	}

	public CallType toCallTypeRetAny() {
		TVarBld vs = new TVarBld(tenv.tpCallRetArg, tenv.tpAny);
		vs.append(this);
		return new CallType((CallMetaType)this.meta_type, vs, this.arity);
	}
	
	public Type ret() {
		AType bindings = this.bindings();
		assert (bindings.getArg(0) ≡ tenv.tpCallRetArg);
		return bindings.resolveArg(0).applay(bindings);
	}
	
	public Type arg(int idx) {
		AType bindings = this.bindings();
		assert (bindings.getArg(1) ≡ tenv.tpCallTupleArg);
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

	public boolean isCastableTo(Type t) {
		if( this ≈ t ) return true;
		if( this.isReference() && t ≡ tenv.tpNull ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.arity == 0 && !(t instanceof CallType) && this.ret().isCastableTo(t) )
			return true;
		return super.isCastableTo(t);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t || t ≡ tenv.tpAny) return true;
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
			return tenv.tpClosure;
		if( this.arity == 0 )
			return new CallType(null,null,null,this.ret().getErasedType(),false);
		Type[] targs = new Type[this.arity];
		for(int i=0; i < this.arity; i++)
			targs[i] = this.arg(i).getErasedType();
		return new CallType(null,null,targs,this.ret().getErasedType(),false);
	}

}

