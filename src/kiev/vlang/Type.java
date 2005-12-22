package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class Type implements StdTypes, AccessFlags, Named {
	public static Type[]	emptyArray = new Type[0];

	static Hash<Type>		typeHash;

	public final Type[]		args;
	public final KString	signature;
	public int				flags;
	protected JType			jtype;
	
	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = new JType(Signature.getJavaSignature(signature));
		return jtype;
	}

	public final JStructView getJStruct() {
		Struct s = getStruct();
		if (s == null)
			return null;
		return s.getJStructView();
	}
	public Struct getStruct() { return null; }
	public MetaSet getStructMeta() { return null; }

	public abstract Type getInitialType();
	public abstract Type getInitialSuperType();
	public abstract Type getSuperType();

	protected Type(KString signature) {
		this.signature = signature;
		this.args = emptyArray;
		this.flags = flReference;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this+" with signature "+signature);
	}

	protected Type(KString signature, Type[] args) {
		this.signature = signature;
		if( args != null && args.length > 0 )
			this.args = args;
		else
			this.args = emptyArray;
		this.flags = flReference;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this+" with signature "+signature);
	}

	public static BaseType newJavaRefType(Struct clazz) {
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		BaseType t = (BaseType)typeHash.get(signature.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(signature) && t instanceof BaseType; });
		if( t != null ) {
//			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return (BaseType)t;
		}
		if (clazz.isAnnotation())
			t = new AnnotationType(signature, clazz);
		else
			t = new BaseType(signature, clazz);
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(BaseType tp) {
		return newRefType(tp.clazz);
	}
	
	public static BaseType newRefType(Struct clazz) {
		if( clazz != null && clazz.type != null && clazz.type.args.length > 0 )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		BaseType t = (BaseType)typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
//			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return (BaseType)t;
		}
		if (clazz.isAnnotation())
			t = new AnnotationType(signature, clazz);
		else
			t = new BaseType(signature, clazz);
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(BaseType tp, Type[] args) {
		return newRefType(tp.clazz, args);
	}
	
	public static BaseType newRefType(Struct clazz, Type[] args) {
		if( clazz != null && clazz.type != null && clazz.type.args.length != args.length )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		if( clazz != null && clazz.type != null ) {
			for(int i=0; i < args.length; i++) {
				if( !args[i].isInstanceOf(clazz.type.args[i]) ) {
					if( clazz.type.args[i].getSuperType() == Type.tpObject && !args[i].isReference())
						;
					else
						throw new RuntimeException("Type "+args[i]+" must be an instance of "+clazz.type.args[i]);
				}
			}
		}
		KString signature = Signature.from(clazz,null,args,null);
		BaseType t = (BaseType)typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
//			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			return (BaseType)t;
		}
		if (clazz.isAnnotation()) {
			assert(args.length == 0); 
			t = new AnnotationType(signature, clazz);
		} else {
			t = new BaseType(signature, clazz,args);
		}
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(ClazzName name) {
		return newRefType(Env.newStruct(name));
	}

	public static BaseType newRefType(ClazzName name, Type[] args) {
		return newRefType(Env.newStruct(name),args);
	}

	public static ArrayType newArrayType(Type type) {
		return ArrayType.newArrayType(type);
	}

	public static Type fromSignature(KString sig) {
		switch( sig.byteAt(0) ) {
		case 'V':		return tpVoid;
		case 'Z':		return tpBoolean;
		case 'C':		return tpChar;
		case 'B':		return tpByte;
		case 'S':		return tpShort;
		case 'I':		return tpInt;
		case 'J':		return tpLong;
		case 'F':		return tpFloat;
		case 'D':		return tpDouble;
		default:
			return Signature.getType(new KString.KStringScanner(sig));
		}
	}
	
	public abstract NodeName getName();
	public abstract ClazzName getClazzName();
	
	public void invalidate() {
		// called when clazz was changed
	}
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name)
		Type@ st;
	{
		st @= getDirectSuperTypes(),
		st.resolveStaticNameR(node, info, name)
	}
	
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name)
		Type@ st;
	{
		st @= getDirectSuperTypes(),
		st.resolveNameAccessR(node, info, name)
	}

	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt)
		Type@ st;
	{
		st @= getDirectSuperTypes(),
		st.resolveCallStaticR(node, info, name, mt)
	}
	
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
		Type@ st;
	{
		st @= getDirectSuperTypes(),
		st.resolveCallAccessR(node, info, name, mt)
	}
	

	public int hashCode() { return signature.hashCode(); }

	public abstract String toString();
	public abstract boolean checkResolved();

	public final boolean equals(Object:Object to) {
		return false;
	}

	public final boolean equals(TypeRef:Object to) {
		return this.equals(((TypeRef)to).getType());
	}

	public final boolean equals(Type:Object to) {
		if( signature.equals( ((Type)to).signature ) ) return true;
		else if (this.isBoolean() && to.isBoolean() ) return true;
		else if (this.isArgument())
			return getSuperType().equals(to);
		else if (to.isArgument())
			return this.equals(to.getSuperType());
		return false;
	}

	public boolean string_equals(Type to) {
		return signature.equals( to.signature );
	}

	public boolean isInstanceOf(Type t) {
		return this.equals(t);
	}

	public boolean codeEquivalentTo(Type t) {
		if( this.equals(t) ) return true;
		if( isIntegerInCode() && t.isIntegerInCode() ) return true;
		if( isReference() && t.isReference() && isInstanceOf(t) ) return true;
		return false;
	}

	public boolean isAutoCastableTo(Type t)
	{
		if( t == Type.tpVoid ) return true;
		if( this.isReference() && t.isReference() && (this==tpNull || t==tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( this == Type.tpRule && t == Type.tpBoolean ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(t) == this ) return true;
			else if( !Kiev.javaMode && t==Type.tpInt && this.isInstanceOf(Type.tpEnum) )
				return true;
		}
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(t) == this ) return true;
			else if( !Kiev.javaMode && this==Type.tpInt && t.isInstanceOf(Type.tpEnum) ) return true;
		}
		if( this==tpByte && ( t==tpShort || t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( (this==tpShort || this==tpChar) && (t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpInt && (t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpLong && ( t==tpFloat || t==tpDouble) ) return true;
		if( this==tpFloat && t==tpDouble ) return true;
		if( this.isWrapper() || t.isWrapper() ) {
			if( this.isWrapper() && t.isWrapper() )
				return this.getWrappedType().isAutoCastableTo(t.getWrappedType());
			else if( this.isWrapper() && this.getWrappedType().isAutoCastableTo(t) )
				return true;
			else if( t.isWrapper() && t.isAutoCastableTo(t.getWrappedType()) )
				return true;
			return false;
		}
		if( this instanceof ClosureType && !(t instanceof CallableType) && this.args.length == 0 ) {
			if( ((ClosureType)this).ret.isAutoCastableTo(t) ) return true;
		}
		return false;
	}

	public Type betterCast(Type t1, Type t2) {
		if( equals(t1) ) return t1;
		if( equals(t2) ) return t2;
		if( isBoolean() && t1.isBoolean() ) return t1;
		if( isBoolean() && t2.isBoolean() ) return t2;
		if( isNumber() ) {
			if( isInteger() ) {
				if( this == tpByte )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpChar )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpShort )
					if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpInt )
					if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
			} else {
				if( this == tpFloat )
					if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( this == tpDouble )
					if( t1==tpDouble || t2==tpDouble) return tpDouble;
					else return null;
			}
		}
		else if( this.isReference() ) {
			if( t1.isReference() && !t2.isReference() ) return t1;
			else if( !t1.isReference() && t2.isReference() ) return t2;
			else if( !t1.isReference() && !t2.isReference() ) return null;
			if( this == tpNull ) return null;
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
					if( tp_better == tp1 ) return t1;
					if( tp_better == tp2 ) return t2;
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

	public static Type upperCastNumbers(Type tp1, Type tp2) {
		assert( tp1.isNumber() );
		assert( tp2.isNumber() );
		if( tp1==Type.tpDouble || tp2==Type.tpDouble) return Type.tpDouble;
		if( tp1==Type.tpFloat || tp2==Type.tpFloat) return Type.tpFloat;
		if( tp1==Type.tpLong || tp2==Type.tpLong) return Type.tpLong;
		if( tp1==Type.tpInt || tp2==Type.tpInt) return Type.tpInt;
		if( tp1==Type.tpChar || tp2==Type.tpChar) return Type.tpChar;
		if( tp1==Type.tpShort || tp2==Type.tpShort) return Type.tpShort;
		if( tp1==Type.tpByte || tp2==Type.tpByte) return Type.tpByte;
		throw new RuntimeException("Bad number types "+tp1+" or "+tp2);
	}

	public boolean isCastableTo(Type t) {
		if( isNumber() && t.isNumber() ) return true;
		if( this.isReference() && t.isReference() && (this==tpNull || t==tpNull) ) return true;
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
		if( this instanceof ClosureType && !(t instanceof CallableType) && this.args.length == 0 ) {
			if( ((ClosureType)this).ret.isCastableTo(t) ) return true;
		}
		if( this.isWrapper())
			return ((WrapperType)this).getUnwrappedType().isCastableTo(t);
		if( t.isWrapper())
			return this.isCastableTo(((WrapperType)t).getUnwrappedType());
		return false;
	}

	public static BaseType getRefTypeForPrimitive(Type tp) {
		if( tp.isReference() ) return (BaseType)tp;
		if( tp == Type.tpBoolean ) return Type.tpBooleanRef;
//		else if( tp == Type.tpRule ) return Type.tpBooleanRef;
		else if( tp == Type.tpByte ) return Type.tpByteRef;
		else if( tp == Type.tpShort ) return Type.tpShortRef;
		else if( tp == Type.tpInt ) return Type.tpIntRef;
		else if( tp == Type.tpLong ) return Type.tpLongRef;
		else if( tp == Type.tpFloat ) return Type.tpFloatRef;
		else if( tp == Type.tpDouble ) return Type.tpDoubleRef;
		else if( tp == Type.tpChar ) return Type.tpCharRef;
		else if( tp == Type.tpVoid ) return Type.tpVoidRef;
		else
			throw new RuntimeException("Unknown primitive type "+tp);
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
	public final boolean isArgumented()	{ return (flags & flArgumented)		!= 0 ; }
	public final boolean isCallable()		{ return (flags & flCallable)		!= 0 ; }

	public boolean isAnnotation()			{ return false; }
	public boolean isAbstract()				{ return false; }
	public boolean isEnum()					{ return false; }
	public boolean isInterface()			{ return false; }
	public boolean isClazz()				{ return false; }
	public boolean isHasCases()				{ return false; }
	public boolean isPizzaCase()			{ return false; }
	public boolean isStaticClazz()			{ return false; }
	public boolean isStruct()				{ return false; }
	public boolean isAnonymouseClazz()		{ return false; }
	public boolean isLocalClazz()			{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return false; }
	
	public boolean isWrapper()						{ return false; }
	public ENode makeWrappedAccess(ASTNode from)	{ throw new RuntimeException("Type "+this+" is not a wrapper"); } 
	public Type getWrappedType()					{ throw new RuntimeException("Type "+this+" is not a wrapper"); }
	
	public abstract Type[] getDirectSuperTypes();
	
	public abstract Type getJavaType();

	private static int get_real_type_depth = 0;

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
		if( !t2.isArgumented() )		return t2;
		if( !t2.isReference() && !t2.isCallable())			return t2;
		if( t1.isArgument() )			return t2;
		// No deep recursion for rewriting rules
		if( get_real_type_depth > 32 ) return t2;
		get_real_type_depth++;
		try {
		if( t1.isArray() ) return getRealType(t1.args[0],t2);
		if( t2.isArray() ) return Type.newArrayType(getRealType(t1,t2.args[0]));
		if( t2.isArgument() ) {
			for(int i=0; i < t1.args.length && i < t1.getInitialType().args.length; i++) {
				if( t1.getInitialType().args[i].string_equals(t2) ) {
					trace(Kiev.debugResolve,"type "+t2+" is resolved as "+t1.args[i]);
					return t1.args[i];
				}
			}
			// Search in super-class and super-interfaces
			foreach (Type sup; t1.getDirectSuperTypes()) {
				Type tp = getRealType(getRealType(t1,sup),t2);
				if (tp != t2)
					return tp;
			}
			// Not found, return itself
			return t2;
		}
		// Well, isn't an argument, but may be a type with arguments
		if( t2.args.length == 0 && !(t2 instanceof CallableType) ) return t2;
		Type[] tpargs = new Type[t2.args.length];
		Type tpret = null;
		for(int i=0; i < tpargs.length; i++) {
			// Check it's not an infinite loop
			if( t2.args[i].string_equals(t2) )
				throw new RuntimeException("Ciclyc parameter # "+i+":"+t2.args[i]+" in type "+t2);
			tpargs[i] = getRealType(t1,t2.args[i]);
		}
		boolean isRewritten = false;
		if( t2 instanceof CallableType ) {
			tpret = getRealType(t1,((CallableType)t2).ret);
			if( tpret != ((CallableType)t2).ret ) isRewritten = true;
		}
		// Check if anything was rewritten
		for(int i=0; i < tpargs.length; i++) {
			if( tpargs[i] != t2.args[i] ) { isRewritten = true; break; }
		}
		if( isRewritten ) {
			Type tp;
			// Check we must return a MethodType, a ClosureType or an array
			if( t2 instanceof ArrayType )
				tp = newArrayType(tpargs[0]);
			else if( t2 instanceof ClosureType )
				tp = ClosureType.newClosureType(t2.clazz,tpargs,getRealType(t1,((ClosureType)t2).ret));
			else if( t2 instanceof MethodType )
				tp = MethodType.newMethodType(tpargs,tpret);
			else
				tp = newRefType(((BaseType)t2).clazz,tpargs);
			trace(Kiev.debugResolve,"Type "+t2+" rewritten into "+tp+" using "+t1);
			return tp;
		}
		// Nothing was rewritten...
		if( t1.getInitialSuperType() != null ) return getRealType(t1.getInitialSuperType(),t2);
		return t2;
		} finally { get_real_type_depth--; }
	}

	public static BaseType getProxyType(Type tp) {
		return newRefType(Type.tpRefProxy.clazz,new Type[]{tp});
//		if( tp.isReference() )			return Type.tpCellObject;
//		else if( tp == Type.tpBoolean )	return Type.tpCellBoolean;
//		else if( tp == Type.tpByte )	return Type.tpCellByte;
//		else if( tp == Type.tpChar )	return Type.tpCellChar;
//		else if( tp == Type.tpShort)	return Type.tpCellShort;
//		else if( tp == Type.tpInt  )	return Type.tpCellInt ;
//		else if( tp == Type.tpLong )	return Type.tpCellLong;
//		else if( tp == Type.tpFloat)	return Type.tpCellFloat;
//		else if( tp == Type.tpDouble)	return Type.tpCellDouble;
//		return tp;
	}

	public final void checkJavaSignature() {
		//assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		jtype == null;
	}

	public abstract Dumper toJava(Dumper dmp);

}

public class JType extends Type {
	
	JType(KString java_signature) {
		super(java_signature);
	}
	
	public final KString get$java_signature() { return signature; }
}

public class JBaseType extends JType {
//	public final JStruct			jstruct;
	
	JBaseType(KString java_signature, BaseType type) {
		super(java_signature);
//		this.jstruct = new JStruct(type.getStruct());
	}
}

public class JArrayType extends JType {
	public final JType				jarg;
	
	JArrayType(KString java_signature, ArrayType type) {
		super(java_signature);
		this.jarg = type.args[0].getJType();
	}
}

public class BaseType extends Type {
	public static BaseType[]	emptyArray = new BaseType[0];

	public final access:ro,ro,ro,rw Struct		clazz;	
	
	BaseType(Struct clazz) {
		super(Signature.from(clazz,null,null,null));
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	BaseType(Struct clazz, Type[] args) {
		super(Signature.from(clazz,null,args,null),args);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	BaseType(KString signature, Struct clazz) {
		super(signature);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	BaseType(KString signature, Struct clazz, Type[] args) {
		super(signature,args);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = new JBaseType(Signature.getJavaSignature(signature),this);
		return jtype;
	}

	public NodeName getName()			{ return clazz.name; }
	public ClazzName getClazzName()		{ return clazz.name; }
	public Struct getStruct()			{ return clazz; }
	public Type getInitialType()		{ return clazz.type; }
	public Type getInitialSuperType()	{ return clazz.super_type; }
	public MetaSet getStructMeta()		{ return clazz.meta; }
	public Type getSuperType()			{ return Type.getRealType(this,clazz.super_type); }
	

	public boolean isAnnotation()			{ return false; }
	public boolean isAbstract()				{ return clazz.isAbstract(); }
	public boolean isEnum()					{ return clazz.isEnum(); }
	public boolean isInterface()			{ return clazz.isInterface(); }
	public boolean isClazz()				{ return clazz.isClazz(); }
	public boolean isHasCases()				{ return clazz.isHasCases(); }
	public boolean isPizzaCase()			{ return clazz.isPizzaCase(); }
	public boolean isStaticClazz()			{ return clazz.isStatic(); }
	public boolean isStruct()				{ return clazz instanceof Struct; }
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
		;	info.isForwardsAllowed() && clazz instanceof Struct,
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
		sup @= getDirectSuperTypes(),
		info.enterSuper() : info.leaveSuper(),
		Type.getRealType(this, sup).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(DNode@ node, ResInfo info, KString name)
		DNode@ forw;
	{
			forw @= getStruct().members,
			forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
			info.enterForward(forw) : info.leaveForward(forw),
			Type.getRealType(this,((Field)forw).type).resolveNameAccessR(node,info,name)
	}

	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		clazz.resolveStructMethodR(node, info, name, mt, this)
	}
	
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
		DNode@ member;
		Type@ sup;
		Field@ forw;
		MethodType mtype;
	{
		checkResolved(),
		mtype = (MethodType)Type.getRealType(this, mt),
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
			info.enterSuper() : info.leaveSuper(),
			sup @= getDirectSuperTypes(),
			Type.getRealType(this,sup).resolveCallAccessR(node,info,name,mtype)
		;
			info.isForwardsAllowed() && clazz instanceof Struct,
			member @= getStruct().members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			Type.getRealType(this,((Field)member).type).resolveCallAccessR(node,info,name,mtype)
		}
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(clazz.name.toString());
		if( args != null && args.length > 0 ) {
			str.append('<');
			for(int i=0; i < args.length; i++) {
				str.append(args[i]);
				if( i < args.length-1)
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
		if( this.equals(_t2) ) return true;
		if( this.isReference() && _t2.equals(Type.tpObject) ) return true;
		if!(_t2 instanceof BaseType) {
			if (_t2 instanceof ArgumentType)
				return this.isInstanceOf(_t2.getSuperType());
			return false;
		}
		BaseType t2 = (BaseType)_t2;
		BaseType t1 = this;
		try {
			t1.checkResolved();
			t2.checkResolved();
		} catch(Exception e ) {
			if( Kiev.verbose ) e.printStackTrace( /* */System.out /* */ );
			throw new RuntimeException("Unresolved type:"+e);
		}
		// Check class1 == class2 && arguments
		if( t1.clazz != null && t2.clazz != null && t1.clazz.equals(t2.clazz) ) {
			int t1_args_len = t1.args==null?0:t1.args.length;
			int t2_args_len = t2.args==null?0:t2.args.length;
			if( t1_args_len != t2_args_len ) return false;
			if( t1_args_len == 0 ) return true;
			for(int i=0; i < t1.args.length; i++)
				if( !t1.args[i].isInstanceOf(t2.args[i]) ) return false;
			return true;
		}
		foreach (Type sup; t1.getDirectSuperTypes()) {
			if (Type.getRealType(t1,sup).isInstanceOf(t2))
				return true;
		}
		return false;
	}

	public Type[] getDirectSuperTypes() {
		Type st = getSuperType();
		if (st == null) return Type.emptyArray;
		Type[] sta = new Type[clazz.interfaces.length+1];
		sta[0] = st;
		for (int i=1; i < sta.length; i++)
			sta[i] = clazz.interfaces[i-1].getType();
		return sta;
	}

	public Type getJavaType() {
		if( !isReference() )
			return this;
		if( args.length == 0 ) return this;
		return newJavaRefType(clazz);
	}

}

public class AnnotationType extends BaseType {

	AnnotationType(KString signature, Struct clazz) {
		super(signature,clazz);
		assert(clazz.isAnnotation());
	}
	
	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = new JBaseType(Signature.getJavaSignature(signature),this);
		return jtype;
	}

	public boolean isAnnotation()			{ return true; }
	public boolean isAbstract()				{ return false; }
	public boolean isEnum()					{ return false; }
	public boolean isInterface()			{ return true; }
	public boolean isClazz()				{ return false; }
	public boolean isHasCases()				{ return false; }
	public boolean isPizzaCase()			{ return false; }
	public boolean isStaticClazz()			{ return true; }
	public boolean isStruct()				{ return true; }
	public boolean isAnonymouseClazz()		{ return false; }
	
	public String toString() {
		return clazz.name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return clazz.toJava(dmp);
	}

	public boolean checkResolved() {
		return clazz.checkResolved();
	}

	public boolean isInstanceOf(Type t2) {
		while (t2 instanceof ArgumentType)
			t2 = t2.getSuperType();
		return t2 == Type.tpObject || t2 == Type.tpAnnotation || t2 == this;
	}

	public Type[] getDirectSuperTypes() {
		return new Type[]{Type.tpObject,Type.tpAnnotation};
	}

	public Type getJavaType() {
		return this;
	}

}

public class ArrayType extends Type {

	private static final ClazzName cname = ClazzName.fromSignature(KString.from("Lkiev/stdlib/Array;"));
	
	public static ArrayType newArrayType(Type type) {
		KString sign = new KStringBuffer(type.signature.len).append('[').append(type.signature).toKString();
		ArrayType t = (ArrayType)typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		t = new ArrayType(sign, type);
		t.flags	 |= flReference | flArray;
		if( t.args[0].isArgumented() ) t.flags |= flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature);
		return t;
	}
	
	private ArrayType(KString signature, Type arg) {
		super(signature, new Type[]{arg});
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null) {
			KString asig = args[0].getJType().java_signature;
			KString sig = new KStringBuffer(asig.len+1).append_fast((byte)'[').append_fast(asig).toKString();
			jtype = new JArrayType(Signature.getJavaSignature(sig),this);
		}
		return jtype;
	}

	public NodeName getName()						{ return cname; }
	public ClazzName getClazzName()					{ return cname; }
	public boolean isArgument()						{ return false; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return false; }
	public boolean isPizzaCase()					{ return false; }
	public boolean isStaticClazz()					{ return false; }
	public boolean isStruct()						{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return s == tpObject.clazz; }
	public Type getSuperType()						{ return tpObject; }
	public Type getInitialType()					{ return this; }
	public Type getInitialSuperType()				{ return tpObject; }
	public MetaSet getStructMeta()					{ return tpObject.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return new Type[] {tpObject, tpCloneable}; }

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name)
	{
		false
	}
	
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		false
	}
	
	public Type getJavaType() {
		return newArrayType(args[0].getJavaType());
	}

	public boolean checkResolved() {
		return true;
	}

	public String toString() {
		return String.valueOf(args[0])+"[]";
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(args[0]).append("[]");
	}

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		if (t == Type.tpObject) return true;
		if (t instanceof ArrayType)
			return args[0].isInstanceOf(t.args[0]);
		return false;
	}

}

public class ArgumentType extends Type {

	/** Variouse names of the type */
	public ClazzName			name;

	/** Bound super-class for class arguments */
	public Type					super_type;

	private ArgumentType(KString signature, ClazzName name, Type sup) {
		super(signature);
		this.name = name;
		super_type = sup;
	}
	
	public static ArgumentType newArgumentType(ClazzName name, Type sup) {
		KString sign = KString.from("A"+name.bytecode_name+";");
		ArgumentType t = (ArgumentType)typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		if (sup == null)
			sup = tpObject;
		t = new ArgumentType(sign,name,sup);
		t.flags	|= flReference | flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type argument: "+t+" with signature "+t.signature);
		return t;
	}

	public static ArgumentType newArgumentType(Struct owner, KString name) {
		KString nm = KString.from(owner.name.name+"$"+name);
		KString bc = KString.from(owner.name.bytecode_name+"$"+name);
		ClazzName cn = new ClazzName(nm,name,bc,true,true);
		return newArgumentType(cn,null);
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = super_type.getJType();
		return jtype;
	}

	public NodeName getName()						{ return name; }
	public ClazzName getClazzName()					{ return name; }
	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return super_type.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return super_type.isHasCases(); }
	public boolean isPizzaCase()					{ return super_type.isPizzaCase(); }
	public boolean isStaticClazz()					{ return super_type.isStaticClazz(); }
	public boolean isStruct()						{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return super_type.isStructInstanceOf(s); }
	public Type getSuperType()						{ return super_type; }
	public Type getInitialType()					{ return super_type.getInitialType(); }
	public Type getInitialSuperType()				{ return super_type.getInitialSuperType(); }
	public MetaSet getStructMeta()					{ return super_type.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return super_type.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { super_type.resolveNameAccessR(node, info, name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { super_type.resolveCallAccessR(node, info, name, mt) }
	
	public Type getJavaType() {
		if (super_type == null)
			return tpObject;
		return super_type.getJavaType();
	}

	public boolean checkResolved() {
		return super_type.checkResolved();
	}

	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(name.short_name);
	}

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		return super_type.isInstanceOf(t);
	}

}

public class WrapperType extends Type {
	
	public static final Type tpWrappedPrologVar = newWrapperType(tpPrologVar);
	public static final Type tpWrappedRefProxy  = newWrapperType(tpRefProxy);
	
	final Field wrapped_field;
	final BaseType unwrapped_type;

	public static Type newWrapperType(Type type) {
		KString sign = new KStringBuffer(type.signature.len).append('%').append(type.signature).toKString();
		Type t = typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		if !(type instanceof BaseType)
			throw new RuntimeException("Wrapper of "+type.getClass());
		t = new WrapperType(sign, (BaseType)type);
		t.flags	 = type.flags | flWrapper;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature);
		return t;
	}
	
	private WrapperType(KString sign, BaseType unwrapped_type) {
		super(sign,unwrapped_type.args);
		this.unwrapped_type = unwrapped_type;
		this.wrapped_field = unwrapped_type.getStruct().getWrappedField(true);
	}
	
	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = getUnwrappedType().getJType();
		return jtype;
	}

	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return true; }
	public boolean isHasCases()						{ return false; }
	public boolean isPizzaCase()					{ return false; }
	public boolean isStaticClazz()					{ return true; }
	public boolean isStruct()						{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return getUnwrappedType().isStructInstanceOf(s); }

	public final boolean isWrapper()					{ return true; }
	public final ENode makeWrappedAccess(ASTNode from)	{ return new IFldExpr(from.pos,(ENode)~from, wrapped_field); } 
	public final Type getWrappedType()					{ return Type.getRealType(this, wrapped_field.type); }
	
	public BaseType getUnwrappedType()					{ return unwrapped_type; }
	
	public NodeName getName()			{ return getUnwrappedType().getName(); }
	public ClazzName getClazzName()		{ return getUnwrappedType().getClazzName(); }
	public Struct getStruct()			{ return getUnwrappedType().getStruct(); }
	public MetaSet getStructMeta()		{ return getUnwrappedType().getStructMeta(); }
	public Type getSuperType()			{ return getUnwrappedType().getSuperType(); }
	public Type getInitialType()		{ return getUnwrappedType().getInitialType(); }
	public Type getInitialSuperType()	{ return getUnwrappedType().getInitialSuperType(); }

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
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
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt)
		MethodType mtype;
	{
		info.isForwardsAllowed(),$cut,
		checkResolved(),
		mtype = (MethodType)Type.getRealType(this, mt),
		trace(Kiev.debugResolve, "Resolving method "+name+" in wrapper type "+this),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveCallAccessR(node, info, name, mtype),
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
		if (this == t) return true;
		if (t instanceof WrapperType)
			return getUnwrappedType().isInstanceOf(t.getUnwrappedType());
		return false;
	}

	public Type[] getDirectSuperTypes() {
		Type st = getSuperType();
		if (st == null) return Type.emptyArray;
		Struct clazz = ((BaseType)getUnwrappedType()).clazz;
		Type[] sta = new Type[clazz.interfaces.length+1];
		sta[0] = st;
		for (int i=1; i < sta.length; i++)
			sta[i] = clazz.interfaces[i-1].getType();
		return sta;
	}

	public Type getJavaType() {
		return getUnwrappedType().getJavaType();
	}

}

public interface CallableType {
	@virtual public virtual access:ro Type[]	args;
	@virtual public virtual access:ro Type		ret;
}

public class ClosureType extends BaseType implements CallableType {
	public virtual Type		ret;
	
	private ClosureType(KString signature, Struct clazz, Type[] args, Type ret) {
		super(signature,clazz,args);
		this.ret = ret;
		flags |= flReference | flCallable;
		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New closure type created: "+this+" with signature "+signature);
	}

	public static ClosureType newClosureType(Struct clazz, Type[] args, Type ret) {
		if (ret   == null) ret   = Type.tpAny;
		KString sign = Signature.from(clazz,Type.emptyArray,args,ret);
		ClosureType t = (ClosureType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && ((ClosureType)t).clazz.equals(clazz); });
		if( t != null ) return t;
		t = new ClosureType(sign,clazz,args,ret);
		return t;
	}
	
	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

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
		return clazz.toJava(dmp);
	}

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		if (t instanceof ClosureType) {
			if( this.args.length != t.args.length ) return false;
			for(int i=0; i < this.args.length; i++)
				if( !this.args[i].isInstanceOf(t.args[i]) ) return false;
			return true;
		}
		return false;
	}

	public boolean checkResolved() {
		return true;
	}

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

	public Type getJavaType() {
		return Type.tpClosure;
	}

}
	
public class MethodType extends Type implements CallableType {
	public virtual Type		ret;
	public Type[]	fargs;	// formal arguments for parametriezed methods

	private MethodType(KString signature, Type ret, Type[] args, Type[] fargs) {
		super(signature, args);
		this.ret = ret;
		this.fargs = fargs;
		flags |= flCallable;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New method type created: "+this+" with signature "+signature);
	}

	public static MethodType newMethodType(Type[] fargs, Type[] args, Type ret) {
		if (args == null) args = Type.emptyArray;
		if (fargs == null) fargs = Type.emptyArray;
		if (ret   == null) ret   = Type.tpAny;
		KString sign = Signature.from(null,fargs,args,ret);
		MethodType t = (MethodType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && t instanceof MethodType; });
		if( t != null ) return t;
		t = new MethodType(sign,ret,args,fargs);
		return t;
	}
	public static MethodType newMethodType(Type[] args, Type ret) {
		return newMethodType(null,args,ret);
	}

	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null) {
			KStringBuffer ksb = new KStringBuffer(64);
			ksb.append((byte)'(');
			for(int i=0; i < args.length; i++)
				ksb.append(args[i].getJType().java_signature);
			ksb.append((byte)')');
			ksb.append(ret.getJType().java_signature);
			jtype = new JType(ksb.toKString());
		}
		return jtype;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		if (fargs != null && fargs.length > 0) {
			str.append('<');
			for(int i=0; i < fargs.length; i++) {
				str.append(fargs[i]);
				if( i < fargs.length-1)
					str.append(',');
			}
			str.append('>');
		}
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
		return MethodType.newMethodType(fargs,types,ret);
	}

	public boolean greater(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		if( !ret.isInstanceOf(tp.ret) ) return false;
		boolean gt = false;
		for(int i=0; i < args.length; i++) {
			Type t1 = args[i];
			Type t2 = tp.args[i];
			if( !t1.string_equals(t2) ) {
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
			if( !args[i].string_equals(tp.args[i]) ) {
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
	
	public NodeName getName()			{ return null; }
	public ClazzName getClazzName()		{ return null; }
	public Type getSuperType()			{ return null; }
	public Type getInitialType()		{ return Type.tpAny; }
	public Type getInitialSuperType()	{ return null; }

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

	public Type getJavaType() {
		if( args.length == 0 )
			return MethodType.newMethodType(Type.emptyArray,((MethodType)this).ret.getJavaType());
		Type[] targs = new Type[args.length];
		for(int i=0; i < args.length; i++)
			targs[i] = args[i].getJavaType();
		return MethodType.newMethodType(targs,((MethodType)this).ret.getJavaType());
	}

}

