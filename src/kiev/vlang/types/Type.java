package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

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

public abstract class Type extends AType {
	public static Type[]	emptyArray = new Type[0];

	public			JType				jtype;
	
	public abstract JType getJType();
	public abstract Type getSuperType();
	public abstract String toString();
	public abstract boolean checkResolved();
	public abstract TypeProvider[] getAllSuperTypes();
	public abstract Type getErasedType();
	public abstract Dumper toJava(Dumper dmp);
	
	// accessor.field
	public final Type applay(TVSet bindings) {
		return meta_type.applay(this,bindings);
	}
	// instantiate new type
	public final Type bind(TVSet bindings) {
		return meta_type.bind(this,bindings);
	}
	// rebind with lower bound or outer type, etc
	public final Type rebind(TVSet bindings) {
		return meta_type.rebind(this,bindings);
	}
	
	public final JStructView getJStruct() {
		Struct s = getStruct();
		if (s == null)
			return null;
		return s.getJStructView();
	}
	public Struct getStruct() { return null; }
	public Meta getMeta(KString name) { return null; }

	protected Type(TypeProvider meta_type, int flags, TVarBld bindings)
		require { meta_type != null; }
	{
		super(meta_type, flags, bindings);
	}

	protected Type(TypeProvider meta_type, int flags, TVar[] tvars, TArg[] appls)
		require { meta_type != null; }
	{
		super(meta_type, flags, tvars, appls);
	}

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, CallType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt) { false }

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
		if( this instanceof CallType && !(t instanceof CallType) && ((CallType)this).arity == 0 ) {
			if( ((CallType)this).ret().isAutoCastableTo(t) ) return true;
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
		if( this instanceof CallType && !(t instanceof CallType) && ((CallType)this).arity == 0 ) {
			if( ((CallType)this).ret().isCastableTo(t) ) return true;
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
	public final boolean isHidden()			{ return (flags & flHidden)			!= 0 ; }

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

	public static CompaundType getProxyType(Type tp) {
		TVarBld set = new TVarBld();
		set.append(tpRefProxy.clazz.args[0].getAType(), tp);
		return (CompaundType)((CompaundTypeProvider)tpRefProxy.meta_type).make(set);
	}

}

public final class CoreType extends Type {
	public final KString name;
	CoreType(KString name, int flags) {
		super(new CoreTypeProvider(), flags | flResolved, TVar.emptyArray, TArg.emptyArray);
		((CoreTypeProvider)meta_type).core_type = this;
		this.name = name;
	}
	public Meta getMeta(KString name)	{ return null; }
	public Type getErasedType()			{ return this; }
	public Type getSuperType()			{ return null; }
	public boolean checkResolved()		{ return true; }
	public TypeProvider[] getAllSuperTypes()	{ return TypeProvider.emptyArray; }
	public String toString()			{ return name.toString(); }
	public Dumper toJava(Dumper dmp)	{ return dmp.append(name.toString()); }

	public JType getJType()				{ return this.jtype; }
	
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

public final class ArgType extends Type {

	public static final ArgType[] emptyArray = new ArgType[0];
	
	/** Variouse names of the type */
	public final KString			name;

	/** The class this argument belongs to */
	public final TypeDef			definer;

	public ArgType(KString name, TypeDef definer) {
		super(ArgTypeProvider.instance, flReference, TVar.emptyArray, TArg.emptyArray);
		this.name = name;
		this.definer = definer;
		if (definer.isTypeAbstract())   this.flags |= flAbstract;
		if (definer.isTypeUnerasable()) this.flags |= flUnerasable;
		if (definer.isTypeVirtual())    this.flags |= flVirtual;
		if (definer.isTypeFinal())      this.flags |= flFinal;
		if (definer.isTypeStatic())     this.flags |= flStatic;
		if (definer.isTypeForward())    this.flags |= flForward;
	}
	
	public JType getJType() {
		if (jtype == null)
			jtype = getSuperType().getJType();
		return jtype;
	}

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
	public TypeProvider[] getAllSuperTypes()		{ return getSuperType().getAllSuperTypes(); }
	public Struct getStruct()						{ return getSuperType().getStruct(); }

	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { getSuperType().resolveNameAccessR(node, info, name) }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt) { getSuperType().resolveCallAccessR(node, info, name, mt) }
	
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

public final class CompaundType extends Type {
	public final Struct get$clazz() { return ((CompaundTypeProvider)meta_type).clazz; }

	public CompaundType(CompaundTypeProvider meta_type, TVarBld bindings) {
		super(meta_type, flReference, bindings);
	}
	
	public final JType getJType() {
		if (jtype == null)
			jtype = new JBaseType(clazz);
		return jtype;
	}

	public Type getSuperType()					{ return clazz.super_type; }
	public Struct getStruct()					{ return clazz; }
	public Meta getMeta(KString name)			{ return clazz.meta.get(name); }
	public Type getErasedType()					{ return clazz.ctype; }

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
		TypeProvider@ sup;
	{
		info.enterSuper(1, ResInfo.noSuper|ResInfo.noForwards) : info.leaveSuper(),
		sup @= clazz.getAllSuperTypes(),
		sup.make(this.bindings()).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(DNode@ node, ResInfo info, KString name)
		DNode@ forw;
		TypeProvider@ sup;
	{
		forw @= getStruct().members,
		forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(this).resolveNameAccessR(node,info,name)
	;	info.isSuperAllowed(),
		sup @= clazz.getAllSuperTypes(),
		sup instanceof CompaundTypeProvider,
		forw @= ((CompaundTypeProvider)sup).clazz.members,
		forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
		info.enterForward(forw) : info.leaveForward(forw),
		((Field)forw).type.applay(this).resolveNameAccessR(node,info,name)
	}

	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, CallType mt)
	{
		clazz.resolveStructMethodR(node, info, name, mt, this)
	}
	
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt)
		DNode@ member;
		TypeProvider@ sup;
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
			sup.make(this.bindings()).resolveCallAccessR(node,info,name,mt)
		;
			info.isForwardsAllowed(),
			member @= getStruct().members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			((Field)member).type.applay(this).resolveCallAccessR(node,info,name,mt)
		;
			info.isForwardsAllowed(),
			sup @= clazz.getAllSuperTypes(),
			sup instanceof CompaundTypeProvider,
			member @= ((CompaundTypeProvider)sup).clazz.members,
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
			if (t1.clazz != t2.clazz)
				return true; // if it extends the class, it's always an instance of it
			// if clazz is the same, check all bindings to be instanceof upper bindings
			AType b1 = t1.bindings();
			AType b2 = t2.bindings();
			for(int i=0; i < b2.tvars.length; i++) {
				TVar v2 = b2.tvars[i];
				if (v2.isAlias())
					continue;
				Type r2 = v2.result();
				if (v2.var ≡ r2)
					continue;
				Type r1 = b1.resolve(v2.var);
				if (r1 ≡ r2)
					continue;
				if (!r1.isInstanceOf(r2))
					return false;
			}
			return true;
		}
		return false;
	}

	public final TypeProvider[] getAllSuperTypes() {
		return clazz.getAllSuperTypes();
	}
}

public final class ArrayType extends Type {

	@getter public Type get$arg() { return this.tvars[0].unalias().result(); }
	
	public static ArrayType newArrayType(Type type)
		alias operator(240,lfy,new)
	{
		return new ArrayType(type);
	}
	
	private ArrayType(Type arg) {
		super(ArrayTypeProvider.instance, flReference | flArray, new TVarBld(tpArrayArg, arg).close());
	}

	public Type make(TVarSet bindings) { return meta_type.make(bindings); }
	
	public JType getJType() {
		if (jtype == null) {
			jtype = new JArrayType(this.arg.getJType());
		}
		return jtype;
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
	
	public TypeProvider[] getAllSuperTypes() {
		return new TypeProvider[] {
			tpObject.meta_type,
			tpCloneable.meta_type
		};
	}

	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt)
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

public final class WrapperType extends Type {
	
	public static final Type tpWrappedPrologVar = newWrapperType(tpPrologVar);
	public static final Type tpWrappedRefProxy  = newWrapperType(tpRefProxy);
	
	public static Type newWrapperType(Type type) {
		return new WrapperType((CompaundType)type);
	}
	
	public WrapperType(CompaundType unwrapped_type) {
		super(WrapperTypeProvider.instance(unwrapped_type.getStruct()), flReference | flWrapper, new TVarBld(tpWrapperArg, unwrapped_type).close());
	}

	private Field get$wrapped_field() { return ((WrapperTypeProvider)this.meta_type).field; }
	
	public Type make(TVarSet bindings) { return meta_type.make(bindings); }

	public JType getJType() {
		if (jtype == null)
			jtype = getUnwrappedType().getJType();
		return jtype;
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
	public final Type getWrappedType()					{ return Type.getRealType(getUnwrappedType(), wrapped_field.type); }
	
	public CompaundType getUnwrappedType()				{ return (CompaundType)this.tvars[0].unalias().result(); }
	
	public Struct getStruct()			{ return getUnwrappedType().getStruct(); }
	public Meta getMeta(KString name)	{ return getUnwrappedType().getMeta(name); }
	public Type getSuperType()			{ return getUnwrappedType().getSuperType(); }

	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in wrapper type "+this),
		checkResolved(),
		info.enterDewrap() : info.leaveDewrap(),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveNameAccessR(node, info, name),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			getUnwrappedType().resolveNameAccessR(node, info, name)
		}
	;
		info.enterDewrap() : info.leaveDewrap(),
		getUnwrappedType().resolveNameAccessR(node, info, name)
	}

	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve, "Resolving method "+name+" in wrapper type "+this),
		checkResolved(),
		info.enterDewrap() : info.leaveDewrap(),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveCallAccessR(node, info, name, mt),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			getUnwrappedType().resolveCallAccessR(node, info, name, mt)
		}
	;
		info.enterDewrap() : info.leaveDewrap(),
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

	public TypeProvider[] getAllSuperTypes() {
		return getUnwrappedType().getAllSuperTypes();
	}

	public Type getErasedType() {
		return getUnwrappedType().getErasedType();
	}

}

public final class OuterType extends Type {

	public static OuterType newOuterType(Struct of_clazz, Type type)
		alias operator(240,lfy,new)
	{
		return new OuterType(of_clazz.ometa_type, type);
	}
	
	private OuterType(OuterTypeProvider meta_type, Type outer) {
		super(meta_type, flReference, new TVarBld(meta_type.tdef.getAType(), outer).close());
	}

	public Type get$outer()			{ return this.tvars[0].unalias().result(); }
	
	public JType getJType() {
		if (jtype == null)
			jtype = outer.getJType();
		return jtype;
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
	
	public TypeProvider[] getAllSuperTypes() {
		return outer.getAllSuperTypes();
	}

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { outer.resolveStaticNameR(node,info,name) }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { outer.resolveNameAccessR(node,info,name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, CallType mt) { outer.resolveCallStaticR(node,info,name,mt) }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, CallType mt) { outer.resolveCallAccessR(node,info,name,mt) }
	
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

public final class CallType extends Type {
	public  final int		arity;

	CallType(TVarBld bld, int arity, boolean is_closure)
	{
		super(CallTypeProvider.instance, flCallable, bld);
		this.arity = arity;
		if (is_closure)
			flags |= flReference;
	}
	
	public static CallType createCallType(Type[] args, Type ret)
		alias operator(210,lfy,new)
	{
		return createCallType(null,args,ret,false);
	}
	public static CallType createCallType(Type[] args, Type ret, boolean is_closure)
		alias operator(210,lfy,new)
	{
		return createCallType(null,args,ret,is_closure);
	}
	public static CallType createCallType(Type[] targs, Type[] args, Type ret)
		alias operator(210,lfy,new)
	{
		return createCallType(targs,args,ret,false);
	}
	public static CallType createCallType(Type[] targs, Type[] args, Type ret, boolean is_closure)
		alias operator(210,lfy,new)
	{
		targs = (targs != null && targs.length > 0) ? targs : Type.emptyArray;
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? Type.tpAny : ret;
		TVarBld vs = new TVarBld();
		for (int i=0; i < targs.length; i++)
			vs.append(tpUnattachedArgs[i], targs[i]);
		vs.append(tpCallRetArg, ret);
		for (int i=0; i < args.length; i++)
			vs.append(tpCallParamArgs[i], args[i]);
		return new CallType(vs.close(),args.length,is_closure);
	}
	public static CallType createCallType(TVarBld vs, Type[] args, Type ret, boolean is_closure)
		alias operator(210,lfy,new)
	{
		args  = (args != null && args.length > 0) ? args : Type.emptyArray;
		ret   = (ret  == null) ? Type.tpAny : ret;
		vs.append(tpCallRetArg, ret);
		for (int i=0; i < args.length; i++)
			vs.append(tpCallParamArgs[i], args[i]);
		return new CallType(vs.close(),args.length,is_closure);
	}

	public CallType toCallTypeRetAny() {
		TVarBld vs = bindings().rebind_bld(new TVarBld(tpCallRetArg, tpAny));
		return new CallType(vs, this.arity, this.isReference());
	}
	
	public Type ret() {
		AType bindings = this.bindings();
		foreach (TVar tv; bindings.tvars; tv.var ≡ tpCallRetArg)
			return tv.unalias().result().applay(bindings);
		return tpAny;
	}
	
	public Type arg(int idx) {
		ArgType param = tpCallParamArgs[idx];
		AType bindings = this.bindings();
		foreach (TVar tv; bindings.tvars; tv.var ≡ param)
			return tv.unalias().result().applay(bindings);
		throw new NoSuchElementException("Method param "+idx);
	}
	
	public Type[] params() {
		if (this.arity == 0)
			return Type.emptyArray;
		Type[] params = new Type[this.arity];
		int i=0;
		foreach (TVar tv; this.tvars; tv.var ≡ tpCallParamArgs[i]) {
			params[i++] = tv.unalias().result();
			if (i >= this.arity)
				break;
		}
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
				jtype = new JMethodType(jargs, jret);
			}
		}
		return jtype;
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		if (t instanceof CallType) {
			CallType ct = (CallType)t;
			if( this.arity != ct.arity ) return false;
			for(int i=0; i < this.arity; i++)
				if( !ct.arg(i).isInstanceOf(this.arg(i)) ) return false;
			if( !this.ret().isInstanceOf(ct.ret()) ) return false;
			return true;
		}
		return false;
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

	public Dumper toJava(Dumper dmp) {
		if (this.isReference())
			return Type.tpClosureClazz.toJava(dmp);
		else
			return dmp.append("/* ERROR: "+this+" */");
	}

	public CallType getMMType() {
		Type[] types = new Type[arity];
		for(int i=0; i < types.length; i++) {
			if( !arg(i).isReference() ) types[i] = arg(i);
			else types[i] = Type.tpObject;
		}
		return new CallType(types,ret(),isReference());
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
					trace(Kiev.debugMultiMethod,"Type "+t1+" is greater then "+t2);
					gt = true;
				} else {
					trace(Kiev.debugMultiMethod,"Types "+t1+" and "+t2+" are uncomparable");
					return false;
				}
			} else {
				trace(Kiev.debugMultiMethod,"Types "+t1+" and "+t2+" are equals");
			}
		}
		return gt;
	}

	public boolean isMultimethodSuper(CallType tp) {
		if( this.arity != tp.arity ) return false;
		if( !tp.ret().isInstanceOf(this.ret()) ) return false;
		for(int i=0; i < arity; i++) {
			if( !this.arg(i).equals(tp.arg(i)) )
				return false;
		}
		return true;
	}

	public boolean checkResolved() {
		return true;
	}
	
	public Type getSuperType()			{ return null; }

	public TypeProvider[] getAllSuperTypes() { return TypeProvider.emptyArray; }

	public Type getErasedType() {
		if (this.isReference())
			return Type.tpClosure;
		if( this.arity == 0 )
			return new CallType(Type.emptyArray,this.ret().getErasedType(),isReference());
		Type[] targs = new Type[this.arity];
		for(int i=0; i < this.arity; i++)
			targs[i] = this.arg(i).getErasedType();
		return new CallType(targs,this.ret().getErasedType(),isReference());
	}

}

