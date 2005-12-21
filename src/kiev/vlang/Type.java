package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public final class TypeRules implements StdTypes {
	private TypeRules() {}
	
	public static boolean isAutoCastableTo(Type t1, Type t2)
	{
		if( t2 == Type.tpVoid ) return true;
		if( t1.isReference() && t2.isReference() && (t1==tpNull || t2==tpNull) ) return true;
		if( t1.isInstanceOf(t2) ) return true;
		if( t1 == Type.tpRule && t2 == Type.tpBoolean ) return true;
		if( t1.isBoolean() && t2.isBoolean() ) return true;
		if( t1.isReference() && !t2.isReference() ) {
			if( getRefTypeForPrimitive(t2) == t1 ) return true;
			else if( !Kiev.javaMode && t2==Type.tpInt && t1.isInstanceOf(Type.tpEnum) )
				return true;
		}
		if( t2.isReference() && !t1.isReference() ) {
			if( getRefTypeForPrimitive(t1) == t2 ) return true;
			else if( !Kiev.javaMode && t1==Type.tpInt && t2.isInstanceOf(Type.tpEnum) )
				return true;
		}
		if( t1==tpByte && ( t2==tpShort || t2==tpInt || t2==tpLong || t2==tpFloat || t2==tpDouble) ) return true;
		if( (t1==tpShort || t1==tpChar) && (t2==tpInt || t2==tpLong || t2==tpFloat || t2==tpDouble) ) return true;
		if( t1==tpInt && (t2==tpLong || t2==tpFloat || t2==tpDouble) ) return true;
		if( t1==tpLong && ( t2==tpFloat || t2==tpDouble) ) return true;
		if( t1==tpFloat && t2==tpDouble ) return true;
		if( t1.isWrapper() || t2.isWrapper() ) {
			if( t1.isWrapper() && t2.isWrapper() )
				return isAutoCastableTo(t1.getWrappedType(),t2.getWrappedType());
			else if( t1.isWrapper() && isAutoCastableTo(t1.getWrappedType(),t2) )
				return true;
			else if( t2.isWrapper() && isAutoCastableTo(t2,t1.getWrappedType()) )
				return true;
			return false;
		}
		if( t1 instanceof ClosureType && !(t2 instanceof CallableType) && ((ClosureType)t1).cargs.length == 0 ) {
			if( isAutoCastableTo(((ClosureType)t1).ret,t2) ) return true;
		}
		return false;
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

	public static boolean isCastableTo(Type t1, Type t2) {
		if( t1.isNumber() && t2.isNumber() ) return true;
		if( t1.isReference() && t2.isReference() && (t1==tpNull || t2==tpNull) ) return true;
		if( t1.isInstanceOf(t2) ) return true;
		if( t2.isInstanceOf(t1) ) return true;
		if( t1.isReference() && t2.isReference() && (t1.isInterface() || t2.isInterface()) ) return true;
		if( t1.isEnum())
			return isCastableTo(t1, Type.tpInt);
		if( t2.isArgument() && isCastableTo(t1, t2.getSuperType()) )
			return true;
		if( t2.isArgument() && !t1.isReference() ) {
			return true;
		}
		if( t1 instanceof ClosureType && !(t2 instanceof CallableType) && ((ClosureType)t1).cargs.length == 0 ) {
			if( isCastableTo(((ClosureType)t1).ret, t2) ) return true;
		}
		if( t1.isWrapper())
			return isCastableTo(((WrapperType)t1).getUnwrappedType(),t2);
		if( t2.isWrapper())
			return isCastableTo(t1,((WrapperType)t2).getUnwrappedType());
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

	public static Type betterCast(Type t0, Type t1, Type t2) {
		if( t0.equals(t1) ) return t1;
		if( t0.equals(t2) ) return t2;
		if( t0.isBoolean() && t1.isBoolean() ) return t1;
		if( t0.isBoolean() && t2.isBoolean() ) return t2;
		if( t0.isNumber() ) {
			if( t0.isInteger() ) {
				if( t0 == tpByte )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( t0 == tpChar )
					if( t1==tpShort || t2==tpShort ) return tpShort;
					else if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( t0 == tpShort )
					if( t1==tpInt || t2==tpInt ) return tpInt;
					else if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( t0 == tpInt )
					if( t1==tpLong || t2==tpLong ) return tpLong;
					else if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
			} else {
				if( t0 == tpFloat )
					if( t1==tpFloat || t2==tpFloat ) return tpFloat;
					else if( t1==tpDouble || t2==tpDouble ) return tpDouble;
					else return null;
				else if( t0 == tpDouble )
					if( t1==tpDouble || t2==tpDouble) return tpDouble;
					else return null;
			}
		}
		else if( t0.isReference() ) {
			if( t1.isReference() && !t2.isReference() ) return t1;
			else if( !t1.isReference() && t2.isReference() ) return t2;
			else if( !t1.isReference() && !t2.isReference() ) return null;
			if( t0 == tpNull ) return null;
			if( t0.isInstanceOf(t1) ) {
				if( !t0.isInstanceOf(t2) ) return t1;
				else if( t2.isInstanceOf(t1) ) return t2;
				else return t1;
			}
			else if( t0.isInstanceOf(t2) ) return t2;
			if( t1.isWrapper() && t2.isWrapper() ) {
				Type tp1 = t1.getWrappedType();
				Type tp2 = t2.getWrappedType();
				Type tp_better = betterCast(t0,tp1,tp2);
				if( tp_better != null ) {
					if( tp_better == tp1 ) return t1;
					if( tp_better == tp2 ) return t2;
				}
			}
			return null;
		}
		return null;
	}

	public static Type getReal(Type t1, TypeRef t2) {
		return getReal(t1, t2.lnk);
	}
	public static Type getReal(TypeRef t1, Type t2) {
		return getReal(t1.lnk, t2);
	}
	public static Type getReal(TypeRef t1, TypeRef t2) {
		return getReal(t1.lnk, t2.lnk);
	}
	public static Type getReal(Type t1, Type t2) {
		trace(Kiev.debugResolve,"Get real type of "+t2+" in "+t1);
		if( t1 == null || t2 == null )	return t2;
		if( !t2.isArgumented() )		return t2;
		if( !t2.isReference() && !t2.isCallable()) return t2;
		TypeBinding[] tb = new TypeBinding[t2.bindings.length];
next_b1:for (int i=0; i < t2.bindings.length; i++) {
			TypeBinding b2 = t2.bindings[i];
			for (int j=0; j < t1.bindings.length; j++) {
				TypeBinding b1 = t1.bindings[j];
				if (b1.arg == b2.arg) {
					tb[i] = b1.applay(b2);
					continue next_b1;
				}
			}
			tb[i] = b2;
		}
		t2 = t2.newWithBindings(tb);
		return null;
	}

}


public abstract class Type implements StdTypes, AccessFlags {
	public static Type[]	emptyArray = new Type[0];

	static Hash<Type>		typeHash;

	public final TypeBinding[]		bindings;
	public final KString			signature;
	public int						flags;
	protected JType					jtype;
	
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

	public abstract Type getTemplateType();
	public abstract Type getSuperType();

	protected Type(KString signature) {
		this.signature = signature;
		this.bindings = TypeBinding.emptyArray;
		this.flags = flReference;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this+" with signature "+signature);
	}

	protected Type(KString signature, TypeBinding[] bindings) {
		this.signature = signature;
		if( bindings != null && bindings.length > 0 )
			this.bindings = bindings;
		else
			this.bindings = TypeBinding.emptyArray;
		this.flags = flReference;
		foreach(Type a; bindings; a.isArgumented() ) { flags |= flArgumented; break; }
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
		if( clazz.args.length > 0 )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.args.length+" arguments");
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
	
	public static BaseType createRefType(Struct clazz, TypeBinding[] args) {
		KString signature = Signature.from(clazz,null,args,null);
		BaseType t = (BaseType)typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			return (BaseType)t;
		}
		if (clazz.isAnnotation()) {
			assert(args.length == 0); 
			t = new AnnotationType(signature, clazz);
		} else {
			t = new BaseType(signature, clazz, args);
		}
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(Struct clazz, Type[] args) {
		if( clazz.args.length != args.length )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.args.length+" arguments");
		if( clazz != null && clazz.type != null ) {
			for(int i=0; i < args.length; i++) {
				if( !args[i].isInstanceOf(clazz.args[i].getType()) ) {
					if( clazz.args[i].getType().getSuperType() == Type.tpObject && !args[i].isReference())
						;
					else
						throw new RuntimeException("Type "+args[i]+" must be an instance of "+clazz.args[i]);
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
			t = new BaseType(signature, clazz, TypeBinding.emptyArray);
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
	
	public boolean isWrapper()						{ return false; }
	public ENode makeWrappedAccess(ASTNode from)	{ throw new RuntimeException("Type "+this+" is not a wrapper"); } 
	public Type getWrappedType()					{ throw new RuntimeException("Type "+this+" is not a wrapper"); }
	
	public abstract Type[] getDirectSuperTypes();
	
	public abstract Type getErasedType();

	private static int get_real_type_depth = 0;

	public final void checkJavaSignature() {
		jtype == null;
	}

	public abstract Dumper toJava(Dumper dmp);

	public abstract Type newWithBindings(TypeBinding[] tb);
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
		this.jarg = type.bindings[0].getJType();
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
	
	BaseType(Struct clazz, TypeBinding[] bindings) {
		super(Signature.from(clazz,null,bindings,null),bindings);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	BaseType(KString signature, Struct clazz) {
		super(signature);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	BaseType(KString signature, Struct clazz, TypeBinding[] bindings) {
		super(signature,bindings);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = new JBaseType(Signature.getJavaSignature(signature),this);
		return jtype;
	}

	public Struct getStruct()			{ return clazz; }
	public Type getTemplateType()		{ return clazz.type; }
	public Type getSuperType()			{ return TypeRules.getReal(this,clazz.super_type); }
	
	public Type newWithBindings(TypeBinding[] tb) {
		return Type.newRefType(this.clazz,tb);
	}

	public boolean isAnnotation()			{ return false; }
	public boolean isAbstract()				{ return clazz.isAbstract(); }
	public boolean isEnum()					{ return clazz.isEnum(); }
	public boolean isInterface()			{ return clazz.isInterface(); }
	public boolean isClazz()				{ return clazz.isClazz(); }
	
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
		TypeRules.getReal(this, sup).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(DNode@ node, ResInfo info, KString name)
		DNode@ forw;
	{
			forw @= getStruct().members,
			forw instanceof Field && ((Field)forw).isForward() && !forw.isStatic(),
			info.enterForward(forw) : info.leaveForward(forw),
			TypeRules.getReal(this,((Field)forw).type).resolveNameAccessR(node,info,name)
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
		mtype = (MethodType)TypeRules.getReal(this, mt),
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
			TypeRules.getReal(this,sup).resolveCallAccessR(node,info,name,mtype)
		;
			info.isForwardsAllowed() && clazz instanceof Struct,
			member @= getStruct().members,
			member instanceof Field && ((Field)member).isForward(),
			info.enterForward(member) : info.leaveForward(member),
			TypeRules.getReal(this,((Field)member).type).resolveCallAccessR(node,info,name,mtype)
		}
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(clazz.name.toString());
		if( bindings.length > 0 ) {
			str.append('<');
			for(int i=0; i < bindings.length; i++) {
				str.append(bindings[i]);
				if( i < bindings.length-1)
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
			int t1_args_len = t1.bindings.length;
			int t2_args_len = t2.bindings.length;
			if( t1_args_len != t2_args_len ) return false;
			if( t1_args_len == 0 ) return true;
			for(int i=0; i < t1.bindings.length; i++)
				if( !t1.bindings[i].isInstanceOf(t2.bindings[i]) ) return false;
			return true;
		}
		foreach (Type sup; t1.getDirectSuperTypes()) {
			if (TypeRules.getReal(t1,sup).isInstanceOf(t2))
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

	public Type getErasedType() {
		if( !isReference() )
			return this;
		if( bindings.length == 0 ) return this;
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

	public KString get$aname() { return clazz.name.name; }
	
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

	public Type getErasedType() {
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
		if( t.bindings[0].isArgumented() ) t.flags |= flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature);
		return t;
	}
	
	public static ArrayType newArrayType(TypeBinding type) {
		KString sign = new KStringBuffer(type.signature.len).append('[').append(type.signature).toKString();
		ArrayType t = (ArrayType)typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		t = new ArrayType(sign, type);
		t.flags	 |= flReference | flArray;
		if( t.bindings[0].isArgumented() ) t.flags |= flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature);
		return t;
	}
	
	private ArrayType(KString signature, Type arg) {
		super(signature, new TypeBinding[]{new TypeBinding(tpArrayArg,new TypeConstraint.Upper(arg))});
	}
	private ArrayType(KString signature, TypeBinding arg) {
		super(signature, new TypeBinding[]{arg});
	}

	public Type newWithBindings(TypeBinding[] tb) {
		return newArrayType(tb[0]);
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null) {
			KString asig = bindings[0].getJType().java_signature;
			KString sig = new KStringBuffer(asig.len+1).append_fast((byte)'[').append_fast(asig).toKString();
			jtype = new JArrayType(Signature.getJavaSignature(sig),this);
		}
		return jtype;
	}

	public boolean isArgument()						{ return false; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public Type getSuperType()						{ return tpObject; }
	public Type getTemplateType()					{ return this; }
	public Type[] getDirectSuperTypes()			{ return new Type[] {tpObject, tpCloneable}; }

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name)
	{
		false
	}
	
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt)
	{
		false
	}
	
	public Type getErasedType() {
		return newArrayType(bindings[0].getErasedType());
	}

	public boolean checkResolved() {
		return true;
	}

	public String toString() {
		return String.valueOf(bindings[0])+"[]";
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(bindings[0]).append("[]");
	}

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		if (t == Type.tpObject) return true;
		if (t instanceof ArrayType)
			return bindings[0].isInstanceOf(t.bindings[0]);
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

	public Type newWithBindings(TypeBinding[] tb) {
		return newArgumentType(this.name, tb[0]);
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = super_type.getJType();
		return jtype;
	}

	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return super_type.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public Type getSuperType()						{ return super_type; }
	public Type getTemplateType()					{ return super_type.getTemplateType(); }
	public Type[] getDirectSuperTypes()			{ return super_type.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { super_type.resolveNameAccessR(node, info, name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { super_type.resolveCallAccessR(node, info, name, mt) }
	
	public Type getErasedType() {
		if (super_type == null)
			return tpObject;
		return super_type.getErasedType();
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
		super(sign,unwrapped_type.bindings);
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

	public final boolean isWrapper()					{ return true; }
	public final ENode makeWrappedAccess(ASTNode from)	{ return new IFldExpr(from.pos,(ENode)~from, wrapped_field); } 
	public final Type getWrappedType()					{ return TypeRules.getReal(this, wrapped_field.type); }
	
	public BaseType getUnwrappedType()					{ return unwrapped_type; }
	
	public Struct getStruct()			{ return getUnwrappedType().getStruct(); }
	public Type getSuperType()			{ return getUnwrappedType().getSuperType(); }
	public Type getTemplateType()		{ return getUnwrappedType().getTemplateType(); }

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
		mtype = (MethodType)TypeRules.getReal(this, mt),
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

	public Type getErasedType() {
		return getUnwrappedType().getErasedType();
	}

}

public interface CallableType {
	@virtual public virtual access:ro Type[]	cargs;
	@virtual public virtual access:ro Type		ret;
}

public class ClosureType extends BaseType implements CallableType {
	public virtual Type[]	cargs;	// call argument types
	public virtual Type		ret;
	
	private ClosureType(KString signature, Struct clazz, Type[] args, Type ret) {
		super(signature,clazz,TypeBinding.emptyArray);
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
	
	@getter public Type[]	get$args()	{ return cargs; }
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
			ClosureType ct = (ClosureType)t;
			if( this.cargs.length != ct.cargs.length ) return false;
			for(int i=0; i < this.cargs.length; i++)
				if( !this.cargs[i].isInstanceOf(ct.cargs[i]) ) return false;
			return true;
		}
		return false;
	}

	public boolean checkResolved() {
		return true;
	}

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

	public Type getErasedType() {
		return Type.tpClosure;
	}

}
	
public class MethodType extends Type implements CallableType {
	public virtual Type[]	cargs;	// call argument types
	public virtual Type		ret;

	private MethodType(KString signature, Type ret, Type[] cargs, TypeBinding[] bindings) {
		super(signature, bindings);
		this.ret = ret;
		this.cargs = cargs;
		flags |= flCallable;
		foreach(Type a; cargs; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New method type created: "+this+" with signature "+signature);
	}

	public static MethodType newMethodType(TypeBinding[] bindings, Type[] args, Type ret) {
		if (args == null) args = Type.emptyArray;
		if (bindings == null) bindings = TypeBinding.emptyArray;
		if (ret   == null) ret   = Type.tpAny;
		KString sign = Signature.from(null,bindings,args,ret);
		MethodType t = (MethodType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && t instanceof MethodType; });
		if( t != null ) return t;
		t = new MethodType(sign,ret,args,bindings);
		return t;
	}
	public static MethodType newMethodType(Type[] args, Type ret) {
		return newMethodType(null,args,ret);
	}

	@getter public Type[]	get$args()	{ return cargs; }
	@getter public Type		get$ret()	{ return ret; }

	public Type newWithBindings(TypeBinding[] tb) {
		return newMethodType(tb, cargs, ret);
	}

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
		if (bindings.length > 0) {
			str.append('<');
			for(int i=0; i < bindings.length; i++) {
				str.append(bindings[i]);
				if( i < bindings.length-1)
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
		return MethodType.newMethodType(bindings,types,ret);
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
	
	public Type getSuperType()			{ return null; }
	public Type getTemplateType()		{ return Type.tpAny; }

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

	public Type getErasedType() {
		if( args.length == 0 )
			return MethodType.newMethodType(Type.emptyArray,((MethodType)this).ret.getErasedType());
		Type[] targs = new Type[args.length];
		for(int i=0; i < args.length; i++)
			targs[i] = args[i].getErasedType();
		return MethodType.newMethodType(targs,((MethodType)this).ret.getErasedType());
	}

}

