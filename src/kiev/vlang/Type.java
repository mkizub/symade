package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JType;
import kiev.be.java.JBaseType;
import kiev.be.java.JArrayType;
import kiev.be.java.JMethodType;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class Type implements StdTypes, AccessFlags {
	public static Type[]	emptyArray = new Type[0];

	public final	TypeProvider		meta_type;
	public			int					flags;
	public			JType				jtype;
	
	public abstract JType getJType();
	public abstract Type getSuperType();
	public abstract Type toTypeWithLowerBound(Type tp);
	public abstract String toString();
	public abstract boolean checkResolved();
	public abstract Type[] getAllSuperTypes();
	public abstract Type getErasedType();
	public abstract Dumper toJava(Dumper dmp);
	public abstract TVarSet bindings();
	
	// accessor.field
	public final Type applay(Type accessor) {
		return meta_type.applay(this,accessor.bindings());
	}
	public final Type applay(TVarSet bindings) {
		return meta_type.applay(this,bindings);
	}
	// instantiate new type
	public final Type bind(TVarSet bindings) {
		return meta_type.bind(this,bindings);
	}
	// rebind with lower bound or outer type, etc
	public final Type rebind(TVarSet bindings) {
		return meta_type.rebind(this,bindings);
	}
	// find bound value for an abstract type
	public final Type resolve(ArgType arg) {
		return this.bindings().resolve(arg);
	}
	public final JStructView getJStruct() {
		Struct s = getStruct();
		if (s == null)
			return null;
		return s.getJStructView();
	}
	public Struct getStruct() { return null; }
	public Meta getMeta(KString name) { return null; }

	protected Type(TypeProvider meta_type)
		require { meta_type != null; }
	{
		this.meta_type = meta_type;
		this.flags = flReference;
	}

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }

	public static boolean identity(Type t1, Type t2) alias operator (60, xfx, ≡ ) {
		return t1 == t2;
	}

	public static boolean not_identity(Type t1, Type t2) alias operator (60, xfx, ≢ ) {
		return t1 != t2;
	}

	public final boolean equals(Object to) alias operator (60, xfx, ≈ ) {
		if (to instanceof Type) return Type.type_equals(this,(Type)to);
		return false;
	}

	public static boolean type_equals(Type t1, Type t2) alias operator (60, xfx, ≈ ) {
		if (t1 ≡ null || t2 ≡ null) return false;
		if (t1 ≡ t2) return true;
		return t1.eq(t2);
	}

	public static boolean type_not_equals(Type t1, Type t2) alias operator (60, xfx, ≉ ) {
		if (t1 ≡ null || t2 ≡ null) return true;
		return !(t1 ≈ t2);
	}
	
	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }

	public boolean isInstanceOf(Type t) alias operator (60, xfx, ≥ ) {
		return this.equals(t);
	}

	public boolean isAutoCastableTo(Type t)
	{
		if( t ≡ Type.tpVoid ) return true;
		if( this.isReference() && t.isReference() && (this ≡ tpNull || t ≡ tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( this ≡ Type.tpRule && t ≡ Type.tpBoolean ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this.isReference() && !t.isReference() ) {
			if( ((CoreType)t).getRefTypeForPrimitive() ≈ this ) return true;
			else if( !Kiev.javaMode && t ≡ Type.tpInt && this ≥ Type.tpEnum )
				return true;
		}
		if( this.isReference() && !t.isReference() ) {
			if( ((CoreType)t).getRefTypeForPrimitive() ≈ this ) return true;
			else if( !Kiev.javaMode && this ≡ Type.tpInt && t ≥ Type.tpEnum ) return true;
		}
		if( this.isWrapper() || t.isWrapper() ) {
			if( this.isWrapper() && t.isWrapper() )
				return this.getWrappedType().isAutoCastableTo(t.getWrappedType());
			else if( this.isWrapper() && this.getWrappedType().isAutoCastableTo(t) )
				return true;
			else if( t.isWrapper() && t.isAutoCastableTo(t.getWrappedType()) )
				return true;
			return false;
		}
		if( this instanceof ClosureType && !(t instanceof CallableType) && ((ClosureType)this).args.length == 0 ) {
			if( ((ClosureType)this).ret.isAutoCastableTo(t) ) return true;
		}
		return false;
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
			if( t1.isWrapper() && t2.isWrapper() ) {
				Type tp1 = t1.getWrappedType();
				Type tp2 = t2.getWrappedType();
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
			tp = tp.getSuperType();
		}
		return tp;
	}

	public boolean isCastableTo(Type t) {
		if( isNumber() && t.isNumber() ) return true;
		if( this.isReference() && t.isReference() && (this ≡ tpNull || t ≡ tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( t.isInstanceOf(this) ) return true;
		if( this.isReference() && t.isReference() && (this.isInterface() || t.isInterface()) ) return true;
		if( t.isEnum())
			return this.isCastableTo(Type.tpInt);
		if( t.isArgument() && isCastableTo(t.getSuperType()) )
			return true;
		if( t.isArgument() && !this.isReference() ) {
			return true;
		}
		if( this instanceof ClosureType && !(t instanceof CallableType) && ((ClosureType)this).args.length == 0 ) {
			if( ((ClosureType)this).ret.isCastableTo(t) ) return true;
		}
		if( this.isWrapper())
			return ((WrapperType)this).getUnwrappedType().isCastableTo(t);
		if( t.isWrapper())
			return this.isCastableTo(((WrapperType)t).getUnwrappedType());
		return false;
	}

	public boolean isArgument()				{ return false; }
	
	public final boolean isReference()		{ return (flags & flReference)		!= 0 ; }
	public final boolean isArray()			{ return (flags & flArray)			!= 0 ; }
	public final boolean isIntegerInCode()	{ return (flags & flIntegerInCode)	!= 0 ; }
	public final boolean isInteger()		{ return (flags & flInteger)		!= 0 ; }
	public final boolean isFloatInCode()	{ return (flags & flFloatInCode)	!= 0 ; }
	public final boolean isFloat()			{ return (flags & flFloat)			!= 0 ; }
	public final boolean isNumber()			{ return (flags & flNumber)			!= 0 ; }
	public final boolean isDoubleSize()	{ return (flags & flDoubleSize)		!= 0 ; }
	public final boolean isResolved()		{ return (flags & flResolved)		!= 0 ; }
	public final boolean isBoolean()		{ return (flags & flBoolean)		!= 0 ; }
	public final boolean isCallable()		{ return (flags & flCallable)		!= 0 ; }
	public final boolean isAbstract()		{ return (flags & flAbstract)		!= 0 ; }
	public final boolean isUnerasable()	{ return (flags & flUnerasable)		!= 0 ; }
	public final boolean isVirtual()		{ return (flags & flVirtual)		!= 0 ; }
	public final boolean isFinal()			{ return (flags & flFinal)			!= 0 ; }
	public final boolean isStatic()			{ return (flags & flStatic)			!= 0 ; }
	public final boolean isForward()		{ return (flags & flForward)		!= 0 ; }

	public boolean isAnnotation()			{ return false; }
	public boolean isEnum()					{ return false; }
	public boolean isInterface()			{ return false; }
	public boolean isClazz()				{ return false; }
	public boolean isHasCases()				{ return false; }
	public boolean isPizzaCase()			{ return false; }
	public boolean isStaticClazz()			{ return false; }
	public boolean isAnonymouseClazz()		{ return false; }
	public boolean isLocalClazz()			{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return false; }
	
	public boolean isWrapper()						{ return false; }
	public ENode makeWrappedAccess(ASTNode from)	{ throw new RuntimeException("Type "+this+" is not a wrapper"); } 
	public Type getWrappedType()					{ throw new RuntimeException("Type "+this+" is not a wrapper"); }
	
	public static Type getRealType(Type t1, TypeRef t2) {
		return Type.getRealType(t1, t2.lnk);
	}
	public static Type getRealType(TypeRef t1, Type t2) {
		return Type.getRealType(t1.lnk, t2);
	}
	public static Type getRealType(TypeRef t1, TypeRef t2) {
		return Type.getRealType(t1.lnk, t2.lnk);
	}
	public static Type getRealType(Type t1, Type t2) {
		trace(Kiev.debugResolve,"Get real type of "+t2+" in "+t1);
		if( t1 == null || t2 == null )	return t2;
		return t2.applay(t1);
	}

	public static ConcreteType getProxyType(Type tp) {
		TVarSet set = new TVarSet();
		set.append(tpRefProxy.clazz.args[0].getAType(), tp);
		return (ConcreteType)((CompaundTypeProvider)tpRefProxy.meta_type).templ_type.bind(set);
	}

}

public final class CoreType extends Type {
	public final KString name;
	CoreType(KString name, int flags) {
		super(CoreTypeProvider.instance);
		this.flags = flags | flResolved;
		this.name = name;
	}
	protected access:no,rw,no,rw boolean eq(Type t) { return this == t; }
	public TVarSet bindings()			{ return TVarSet.emptySet; }
	public Meta getMeta(KString name)	{ return null; }
	public Type getErasedType()			{ return this; }
	public Type getSuperType()			{ return null; }
	public boolean checkResolved()		{ return true; }
	public Type[] getAllSuperTypes()	{ return Type.emptyArray; }
	public String toString()			{ return name.toString(); }
	public Dumper toJava(Dumper dmp)	{ return dmp.append(name.toString()); }

	public JType getJType()				{ return this.jtype; }

	public Type toTypeWithLowerBound(Type tp) {
		if (tp ≡ this) return this;
		throw new RuntimeException("Setting lower bound on CoreType");
	}

	public boolean isAutoCastableTo(Type t)
	{
		if( t ≡ Type.tpVoid ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this ≡ tpByte && (t ≡ tpShort || t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( (this ≡ tpShort || this ≡ tpChar) && (t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpInt && (t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpLong && ( t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpFloat && t ≡ tpDouble ) return true;
		return super.isAutoCastableTo(t);
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

	public ConcreteType getRefTypeForPrimitive() {
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

public final class ArgType extends Type {

	/** Variouse names of the type */
	public final KString			name;

	/** The class this argument belongs to */
	public final TypeDef			definer;

	public ArgType(KString name, TypeDef definer) {
		super(ArgTypeProvider.instance);
		this.name = name;
		this.definer = definer;
		this.flags = flReference;
		if (definer.isTypeAbstract())   this.flags |= flAbstract;
		if (definer.isTypeUnerasable()) this.flags |= flUnerasable;
		if (definer.isTypeVirtual())    this.flags |= flVirtual;
		if (definer.isTypeFinal())      this.flags |= flFinal;
		if (definer.isTypeStatic())     this.flags |= flStatic;
		if (definer.isTypeForward())    this.flags |= flForward;
	}
	
	public TVarSet bindings()			{ return TVarSet.emptySet; }

	public JType getJType() {
		if (jtype == null)
			jtype = getSuperType().getJType();
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(ArgType:Type type) { return this ≡ type; }

	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return getSuperType().isHasCases(); }
	public boolean isPizzaCase()					{ return getSuperType().isPizzaCase(); }
	public boolean isStaticClazz()					{ return getSuperType().isStaticClazz(); }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return getSuperType().isStructInstanceOf(s); }
	public Type getSuperType()						{ return definer.getSuperType(); }
	public Meta getMeta(KString name)				{ return getSuperType().getMeta(name); }
	public Type[] getAllSuperTypes()				{ return getSuperType().getAllSuperTypes(); }
	public Struct getStruct()						{ return getSuperType().getStruct(); }

	public Type toTypeWithLowerBound(Type tp) {
		if (tp ≡ this) return this;
		definer.setLowerBound(tp);
		return definer.getType();
	}
	
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { getSuperType().resolveNameAccessR(node, info, name) }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { getSuperType().resolveCallAccessR(node, info, name, mt) }
	
	public Type getErasedType() { return getSuperType().getErasedType(); }
	public boolean checkResolved() { return getSuperType().checkResolved(); }

	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		return getSuperType().isInstanceOf(t);
	}
}

public abstract class CompaundType extends Type {
	private							int			version;
	protected access:no,ro,ro,rw	TVarSet		bindings;

	public final Struct get$clazz() { return ((CompaundTypeProvider)meta_type).clazz; }

	CompaundType(CompaundTypeProvider meta_type, TVarSet bindings) {
		super(meta_type);
		this.bindings = bindings;
	}
	
	protected final void checkAbstract() {
		flags &= ~flAbstract;
		foreach(TVar v; this.bindings().tvars; !v.isAlias()) {
			Type r = v.result();
			if (r.isAbstract() || r == v.var)
				flags |= flAbstract;
			if (v.var.isUnerasable())
				flags |= flUnerasable;
		}
	}
	
	public final TVarSet bindings() {
		if (this.version != this.meta_type.version) {
			this.bindings = makeBindings(true);
			this.version = this.meta_type.version;
			checkAbstract();
		}
		return this.bindings;
	}
	
	protected abstract TVarSet makeBindings(boolean with_lower);

	public final JType getJType() {
		if (jtype == null)
			jtype = new JBaseType(clazz);
		return jtype;
	}

	protected final access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected final access:no,rw,no,rw boolean eq(CompaundType:Type type) {
		if (this.clazz != type.clazz) return false;
		return this.bindings().eq(type.bindings());
	}
	
	
	public Type getSuperType()					{ return clazz.super_type; }
	public Struct getStruct()					{ return clazz; }
	public Meta getMeta(KString name)			{ return clazz.meta.get(name); }
	public Type getErasedType()					{ return clazz.concr_type; }

	public boolean isAnnotation()			{ return clazz.isAnnotation(); }
	public boolean isEnum()					{ return clazz.isEnum(); }
	public boolean isInterface()			{ return clazz.isInterface(); }
	public boolean isClazz()				{ return clazz.isClazz(); }
	public boolean isHasCases()				{ return clazz.isHasCases(); }
	public boolean isPizzaCase()			{ return clazz.isPizzaCase(); }
	public boolean isStaticClazz()			{ return clazz.isStatic(); }
	public boolean isAnonymouseClazz()		{ return clazz.isAnonymouse(); }
	public boolean isLocalClazz()			{ return clazz.isLocal(); }
	public boolean isStructInstanceOf(Struct s)	{ return clazz.instanceOf(s); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name)
	{
		clazz.resolveNameR(node, info, name)
	}
	
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name)
	{
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in "+this),
		checkResolved(),
		{
			trace(Kiev.debugResolve,"Type: resolving in "+this),
			resolveNameR_1(node,info,name),	// resolve in this class
			$cut
		;	info.isSuperAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in super-type of "+this),
			resolveNameR_3(node,info,name),	// resolve in super-classes
			$cut
		;	info.isForwardsAllowed(),
			trace(Kiev.debugResolve,"Type: resolving in forwards of "+this),
			resolveNameR_4(node,info,name),	// resolve in forwards
			$cut
		}
	}
	private rule resolveNameR_1(DNode@ node, ResInfo info, KString name)
	{
		clazz instanceof Struct,
		node @= getStruct().members,
		node instanceof Field && ((Field)node).name.equals(name) && info.check(node)
	}
	private rule resolveNameR_3(DNode@ node, ResInfo info, KString name)
		Type@ sup;
	{
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= clazz.getAllSuperTypes(),
		sup.bind(this.bindings()).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(DNode@ node, ResInfo info, KString name)
		DNode@ forw;
		Type@ sup;
	{
		forw @= getStruct().members,
		forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(this).resolveNameAccessR(node,info,name)
	;	info.isSuperAllowed(),
		sup @= clazz.getAllSuperTypes(),
		forw @= sup.getStruct().members,
		forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(this).resolveNameAccessR(node,info,name)
	}

	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		clazz.resolveStructMethodR(node, info, name, mt, this)
	}
	
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
		DNode@ member;
		Type@ sup;
		Field@ forw;
	{
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving method "+name+" in "+this),
		{
			clazz instanceof Struct,
			node @= getStruct().members,
			node instanceof Method,
			((Method)node).name.equals(name),
			info.check(node),
			((Method)node).equalsByCast(name,mt,this,info)
		;
			info.isSuperAllowed(),
			info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
			sup @= clazz.getAllSuperTypes(),
			sup.bind(this.bindings()).resolveCallAccessR(node,info,name,mt)
		;
			info.isForwardsAllowed(),
			member @= getStruct().members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).type.applay(this).resolveCallAccessR(node,info,name,mt)
		;
			info.isForwardsAllowed(),
			sup @= clazz.getAllSuperTypes(),
			member @= sup.getStruct().members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).type.applay(this).resolveCallAccessR(node,info,name,mt)
		}
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(clazz.name.toString());
		int n = clazz.args.length;
		if (n > 0) {
			str.append('<');
			for(int i=0; i < n; i++) {
				str.append(resolve(clazz.args[i].getAType()));
				if( i < n-1)
					str.append(',');
			}
			str.append('>');
		}
		return str.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return clazz.toJava(dmp);
	}

	public boolean checkResolved() {
		return clazz.checkResolved();
	}

	public boolean isInstanceOf(Type _t2) {
		if( this ≡ _t2 ) return true;
		if( this.isReference() && _t2 ≈ Type.tpObject ) return true;
		if!(_t2 instanceof CompaundType) {
			if (_t2 instanceof ArgType)
				return this.isInstanceOf(_t2.getSuperType());
			return false;
		}
		CompaundType t2 = (CompaundType)_t2;
		CompaundType t1 = this;
		try {
			t1.checkResolved();
			t2.checkResolved();
		} catch(Exception e ) {
			if( Kiev.verbose ) e.printStackTrace(System.out);
			throw new RuntimeException("Unresolved type:"+e);
		}
		// Check class1 >= class2 && bindings
		if (t1.clazz.instanceOf(t2.clazz)) {
			TVarSet b1 = t1.bindings();
			TVarSet b2 = t2.bindings();
			for(int i=0; i < b2.length; i++) {
				TVar v2 = b2[i];
				if (v2.isAlias())
					continue;
				Type x = b1.resolve(v2.var);
				if (x ≡ x)
					continue;
				Type y = v2.result();
				if (y ≡ y || x ≡ y)
					continue;
				if (!x.isInstanceOf(y))
					return false;
			}
			return true;
		}
		return false;
	}

	public final Type toTypeWithLowerBound(Type tp) {
		if (tp ≡ this) return this;
		if!(tp instanceof CompaundType)
			throw new RuntimeException("Set non-compaund lower bound type on CompaundType");
		ConcreteType ctp;
		if (tp instanceof TemplateType)
			ctp = ((TemplateType)tp).clazz.concr_type;
		else if (tp instanceof ConcreteType)
			ctp = (ConcreteType)tp;
		else
			ctp = ((BaseType)tp).lower_bound;
		CompaundTypeProvider meta_type = (CompaundTypeProvider)this.meta_type;
		return new BaseType(meta_type, this.bindings(), ctp);
	}
	
	public final Type[] getAllSuperTypes() {
		return clazz.getAllSuperTypes();
	}
}

public final class TemplateType extends CompaundType {
	
	public static final TemplateType[] emptyArray = new TemplateType[0];
	
	TemplateType(CompaundTypeProvider meta_type, TVarSet bindings) {
		super(meta_type, bindings);
		checkAbstract();
	}
	
	protected final TVarSet makeBindings(boolean with_lower) {
		TVarSet vs = new TVarSet();
		foreach (TypeDef ad; clazz.args)
			vs.append(ad.getAType(), null);
		foreach (DNode d; clazz.members; d instanceof TypeDef) {
			TypeDef td = (TypeDef)d;
			vs.append(td.getAType(), null /*td.getAType().getSuperType()*/);
		}
		TypeRef st = clazz.super_bound;
		if (st.getType() ≢ null) {
			CompaundType sct = (CompaundType)st.getType();
			vs.append(sct.makeBindings(false));
			foreach (TypeRef it; clazz.interfaces) {
				sct = (CompaundType)it.getType();
				vs.append(sct.makeBindings(false));
			}
		}
		return vs;
	}
}

public final class ConcreteType extends CompaundType {
	
	ConcreteType(CompaundTypeProvider meta_type, TVarSet bindings) {
		super(meta_type, bindings);
		checkAbstract();
	}
	
	protected TVarSet makeBindings(boolean with_lower) {
		return ((CompaundTypeProvider)meta_type).templ_type.bindings().bind(this.bindings);
	}

}

public final class BaseType extends CompaundType {

	public final ConcreteType lower_bound;
	
	BaseType(CompaundTypeProvider meta_type, TVarSet bindings, ConcreteType lower_bound)
		require lower_bound ≢ null;
	{
		super(meta_type, bindings);
		this.lower_bound = (ConcreteType)lower_bound;
		checkAbstract();
	}
	
	protected TVarSet makeBindings(boolean with_lower) {
		TVarSet vs = ((CompaundTypeProvider)meta_type).templ_type.bindings().bind(this.bindings);
		if (with_lower)
			vs = vs.rebind(lower_bound.bindings());
		return vs;
	}
}



public class ArrayType extends Type {

	public final Type			arg;
	
	public static ArrayType newArrayType(Type type)
		alias operator(240,lfy,new)
	{
		return new ArrayType(type);
	}
	
	private ArrayType(Type arg) {
		super(ArrayTypeProvider.instance);
		this.arg = arg;
		this.flags |= flReference | flArray;
		if( arg.isAbstract() ) this.flags |= flAbstract;
	}

	public TVarSet bindings()			{ return new TVarSet(tpArrayArg, arg); }
	
	public JType getJType() {
		if (jtype == null) {
			jtype = new JArrayType(this.arg.getJType());
		}
		return jtype;
	}

	public Type toTypeWithLowerBound(Type tp) {
		if (!tp.isArray())
			throw new RuntimeException("Setting non-array lower bound on ArrayType");
		return new ArrayType(arg.toTypeWithLowerBound(((ArrayType)tp).arg));
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(ArrayType:Type type) {
		return this.arg ≈ type.arg;
	}

	public boolean isArgument()						{ return false; }
	public boolean isAnnotation()					{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return false; }
	public boolean isPizzaCase()					{ return false; }
	public boolean isStaticClazz()					{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return s == tpObject.clazz; }
	public Type getSuperType()						{ return tpObject; }
	public Meta getMeta(KString name)				{ return null; }
	
	public Type[] getAllSuperTypes() {
		return new TemplateType[] {
			((CompaundTypeProvider)tpObject.meta_type).templ_type,
			((CompaundTypeProvider)tpCloneable.meta_type).templ_type
		};
	}

	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		tpObject.resolveCallAccessR(node, info, name, mt)
	}
	
	public Type getErasedType() {
		return newArrayType(arg.getErasedType());
	}

	public boolean checkResolved() {
		return true;
	}

	public String toString() {
		return String.valueOf(arg)+"[]";
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(arg).append("[]");
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		if (t ≈ Type.tpObject) return true;
		if (t instanceof ArrayType)
			return arg.isInstanceOf(t.arg);
		return false;
	}

}

public class WrapperType extends Type {
	
	public static final Type tpWrappedPrologVar = newWrapperType(tpPrologVar);
	public static final Type tpWrappedRefProxy  = newWrapperType(tpRefProxy);
	
	final CompaundType unwrapped_type;

	public static Type newWrapperType(Type type) {
		return new WrapperType((CompaundType)type);
	}
	
	public WrapperType(CompaundType unwrapped_type) {
		super(WrapperTypeProvider.instance(unwrapped_type.getStruct()));
		this.unwrapped_type = unwrapped_type;
		this.flags	 = flReference | flWrapper;
		if (unwrapped_type.isAbstract()) this.flags |= flAbstract;
	}

	private Field get$wrapped_field() { return ((WrapperTypeProvider)this.meta_type).field; }
	
	public TVarSet bindings()			{ return getUnwrappedType().bindings(); }

	public JType getJType() {
		if (jtype == null)
			jtype = getUnwrappedType().getJType();
		return jtype;
	}

	public Type toTypeWithLowerBound(Type tp) {
		if (!tp.isWrapper())
			throw new RuntimeException("Setting non-wrapper lower bound on WrapperType");
		return new WrapperType((CompaundType)unwrapped_type.toTypeWithLowerBound(((WrapperType)tp).unwrapped_type));
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(WrapperType:Type type) {
		return this.getUnwrappedType() ≈ type.getUnwrappedType();
	}

	public boolean isAnnotation()					{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return true; }
	public boolean isHasCases()						{ return false; }
	public boolean isPizzaCase()					{ return false; }
	public boolean isStaticClazz()					{ return true; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return getUnwrappedType().isStructInstanceOf(s); }

	public final boolean isWrapper()					{ return true; }
	public final ENode makeWrappedAccess(ASTNode from)	{ return new IFldExpr(from.pos,(ENode)~from, wrapped_field); } 
	public final Type getWrappedType()					{ return Type.getRealType(this, wrapped_field.type); }
	
	public CompaundType getUnwrappedType()				{ return unwrapped_type; }
	
	public Struct getStruct()			{ return getUnwrappedType().getStruct(); }
	public Meta getMeta(KString name)	{ return getUnwrappedType().getMeta(name); }
	public Type getSuperType()			{ return getUnwrappedType().getSuperType(); }

	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in wrapper type "+this),
		checkResolved(),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveNameAccessR(node, info, name),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			getUnwrappedType().resolveNameAccessR(node, info, name)
		}
	;
		getUnwrappedType().resolveNameAccessR(node, info, name)
	}

	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		info.isForwardsAllowed(),$cut,
		checkResolved(),
		trace(Kiev.debugResolve, "Resolving method "+name+" in wrapper type "+this),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveCallAccessR(node, info, name, mt),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			getUnwrappedType().resolveCallAccessR(node, info, name, mt)
		}
	;
		getUnwrappedType().resolveCallAccessR(node, info, name, mt)
	}
	
	public boolean checkResolved() {
		return getUnwrappedType().checkResolved() && getWrappedType().checkResolved();
	}

	public String toString() {
		return getUnwrappedType().toString();
	}
	public Dumper toJava(Dumper dmp) {
		return getUnwrappedType().toJava(dmp);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		if (t instanceof WrapperType)
			return getUnwrappedType().isInstanceOf(t.getUnwrappedType());
		return false;
	}

	public Type[] getAllSuperTypes() {
		return getUnwrappedType().getAllSuperTypes();
	}

	public Type getErasedType() {
		return getUnwrappedType().getErasedType();
	}

}

public class OuterType extends Type {

	public final Type			outer;
	
	public static OuterType newOuterType(Struct of_clazz, Type type)
		alias operator(240,lfy,new)
	{
		return new OuterType(of_clazz.ometa_type, type);
	}
	
	private OuterType(OuterTypeProvider meta_type, Type outer) {
		super(meta_type);
		this.outer = outer;
		this.flags |= flReference;
		if( outer.isAbstract() ) this.flags |= flAbstract;
	}

	public TVarSet bindings()		{ return outer.bindings(); }
	
	public JType getJType() {
		if (jtype == null)
			jtype = outer.getJType();
		return jtype;
	}

	public Type toTypeWithLowerBound(Type tp) {
		return new OuterType((OuterTypeProvider)meta_type, outer.toTypeWithLowerBound(tp));
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(OuterType:Type type) {
		return this.outer ≈ type.outer;
	}

	public boolean isArgument()						{ return outer.isArgument(); }
	public boolean isAnnotation()					{ return outer.isAnnotation(); }
	public boolean isEnum()							{ return outer.isEnum(); }
	public boolean isInterface()					{ return outer.isInterface(); }
	public boolean isClazz()						{ return outer.isClazz(); }
	public boolean isHasCases()						{ return outer.isHasCases(); }
	public boolean isPizzaCase()					{ return outer.isPizzaCase(); }
	public boolean isStaticClazz()					{ return outer.isStaticClazz(); }
	public boolean isAnonymouseClazz()				{ return outer.isAnonymouseClazz(); }
	public boolean isLocalClazz()					{ return outer.isLocalClazz(); }
	public boolean isStructInstanceOf(Struct s)	{ return outer.isStructInstanceOf(s); }
	public Type getSuperType()						{ return outer.getSuperType(); }
	public Meta getMeta(KString name)				{ return outer.getMeta(name); }
	
	public Type[] getAllSuperTypes() {
		return outer.getAllSuperTypes();
	}

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { outer.resolveStaticNameR(node,info,name) }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { outer.resolveNameAccessR(node,info,name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { outer.resolveCallStaticR(node,info,name,mt) }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { outer.resolveCallAccessR(node,info,name,mt) }
	
	public Type getErasedType() { return outer.getErasedType(); }
	public boolean checkResolved() { return outer.checkResolved(); }
	public String toString() { return outer.toString(); }
	public Dumper toJava(Dumper dmp) { return outer.toJava(dmp); }

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		if (t instanceof OuterType && t.meta_type == this.meta_type)
			t = t.outer;
		return outer.isInstanceOf(t);
	}

}

public interface CallableType {
	@virtual public virtual access:ro Type[]	args;
	@virtual public virtual access:ro Type		ret;
}

public class ClosureType extends Type implements CallableType {
	public virtual Type[]	args;
	public virtual Type		ret;
	
	public ClosureType(Type[] args, Type ret) {
		super(CallTypeProvider.instance);
		this.args = (args != null && args.length > 0) ? args : Type.emptyArray;
		this.ret  = (ret  == null) ? Type.tpAny : ret;
		flags |= flReference | flCallable;
		foreach(Type a; args; a.isAbstract() ) { flags |= flAbstract; break; }
		if( this.ret.isAbstract() ) flags |= flAbstract;
	}

	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

	public TVarSet bindings()			{ return TVarSet.emptySet; }

	public Type toTypeWithLowerBound(Type tp) {
		if (tp ≡ this) return this;
		throw new RuntimeException("Setting lower bound on ClosureType");
	}

	public JType getJType() {
		if (jtype == null)
			jtype = Type.tpClosure.getJType();
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(ClosureType:Type type) {
		if (this.ret ≉ type.ret) return false;
		public Type[] args1 = this.args;
		public Type[] args2 = type.args;
		if (args1.length != args2.length) return false;
		int n = args1.length;
		for (int i=0; i < n; i++) {
			if (args1[i] ≉ args2[i])
				return false;
		}
		return true;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append('(');
		if( args != null && args.length > 0 ) {
			for(int i=0; i < args.length; i++) {
				str.append(args[i]);
				if( i < args.length-1)
					str.append(',');
			}
		}
		str.append(")->").append(ret);
		return str.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return Type.tpClosureClazz.toJava(dmp);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		if (t instanceof ClosureType) {
			ClosureType ct = (ClosureType)t;
			if( this.args.length != ct.args.length ) return false;
			for(int i=0; i < this.args.length; i++)
				if( !this.args[i].isInstanceOf(ct.args[i]) ) return false;
			return true;
		}
		return false;
	}

	public boolean checkResolved() {
		return true;
	}

	public Type[] getAllSuperTypes() { return Type.emptyArray; }

	public Type getErasedType() {
		return Type.tpClosure;
	}

}
	
public class MethodType extends Type implements CallableType {
	private TVarSet			bindings;
	public virtual Type[]	args;
	public virtual Type		ret;

	public MethodType(Type[] args, Type ret) {
		this(TVarSet.emptySet, args, ret);
	}
	public MethodType(TVarSet bindings, Type[] args, Type ret) {
		super(CallTypeProvider.instance);
		this.bindings = bindings;
		this.args = (args != null && args.length > 0) ? args : Type.emptyArray;
		this.ret  = (ret  == null) ? Type.tpAny : ret;
		flags |= flCallable;
		foreach(Type a; args; a.isAbstract() ) { flags |= flAbstract; break; }
		if( this.ret.isAbstract() ) flags |= flAbstract;
	}
	public static MethodType createMethodType(Type[] targs, Type[] args, Type ret)
		alias operator(210,lfy,new)
	{
		if (targs == null || targs.length == 0) return new MethodType(args,ret);
		args = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret  = (ret  == null) ? Type.tpAny : ret;
		TVarSet vs = new TVarSet();
		for (int i=0; i < targs.length; i++)
			vs.append(tpUnattachedArgs[i], targs[i]);
		return new MethodType(vs,args,ret);
	}


	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

	public TVarSet bindings()			{ return bindings; }

	public Type toTypeWithLowerBound(Type tp) {
		if (tp ≡ this) return this;
		throw new RuntimeException("Setting lower bound on MethodType");
	}

	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null) {
			JType[] jargs = JType.emptyArray;
			if (args.length > 0) {
				jargs = new JType[args.length];
				for (int i=0; i < jargs.length; i++)
					jargs[i] = args[i].getJType();
			}
			JType jret = ret.getJType();
			jtype = new JMethodType(jargs, jret);
		}
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(MethodType:Type type) {
		if (this.ret ≉ type.ret) return false;
		public Type[] args1 = this.args;
		public Type[] args2 = type.args;
		if (args1.length != args2.length) return false;
		int n = args1.length;
		for (int i=0; i < n; i++) {
			if (args1[i] ≉ args2[i])
				return false;
		}
		return true;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append('(');
		if( args != null && args.length > 0 ) {
			for(int i=0; i < args.length; i++) {
				str.append(args[i]);
				if( i < args.length-1)
					str.append(',');
			}
		}
		str.append(")->").append(ret);
		return str.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append("/* ERROR: "+this+" */");
	}

	public MethodType getMMType() {
		Type[] types = new Type[args.length];
		for(int i=0; i < types.length; i++) {
			if( !args[i].isReference() ) types[i] = args[i];
			else types[i] = Type.tpObject;
		}
		return new MethodType(types,ret);
	}

	public boolean greater(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		if( !ret.isInstanceOf(tp.ret) ) return false;
		boolean gt = false;
		for(int i=0; i < args.length; i++) {
			Type t1 = args[i];
			Type t2 = tp.args[i];
			if (t1 ≉ t2) {
				if( t1.isInstanceOf(t2) ) {
					trace(Kiev.debugMultiMethod,"Type "+args[i]+" is greater then "+t2);
					gt = true;
				} else {
					trace(Kiev.debugMultiMethod,"Types "+args[i]+" and "+tp.args[i]+" are uncomparable");
					return false;
				}
			} else {
				trace(Kiev.debugMultiMethod,"Types "+args[i]+" and "+tp.args[i]+" are equals");
			}
		}
		return gt;
	}

	public int compare(MethodType tp) {
		if( args.length != tp.args.length ) return 0;
		if( !ret.equals(tp.ret) ) return 0;
		boolean gt = false;
		boolean lt = false;
		for(int i=0; i < args.length; i++) {
			if (args[i] ≉ tp.args[i]) {
				if( args[i].isInstanceOf(tp.args[i]) ) gt = true;
				else if( tp.args[i].isInstanceOf(args[i]) ) lt = true;
				else return 0;
			}
		}
		if( gt && lt ) return 0;
		if(gt) return 1;
		if(lt) return -1;
		return 0;
	}

	public boolean isMultimethodSuper(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		if( !tp.ret.isInstanceOf(ret) ) return false;
		for(int i=0; i < args.length; i++) {
			if( !args[i].equals(tp.args[i]) )
				return false;
		}
		return true;
	}

	public boolean checkResolved() {
		return true;
	}
	
	public Type getSuperType()			{ return null; }

	public Type[] getAllSuperTypes() { return Type.emptyArray; }

	public Type getErasedType() {
		if( args.length == 0 )
			return new MethodType(Type.emptyArray,((MethodType)this).ret.getErasedType());
		Type[] targs = new Type[args.length];
		for(int i=0; i < args.length; i++)
			targs[i] = args[i].getErasedType();
		return new MethodType(targs,((MethodType)this).ret.getErasedType());
	}

}

