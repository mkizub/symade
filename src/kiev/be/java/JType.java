package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public class JPrimitiveTypeProvider extends TypeProvider {
	static final JPrimitiveTypeProvider instance = new JPrimitiveTypeProvider();
	JPrimitiveTypeProvider() {
	}
}

public class JFakeTypeProvider extends TypeProvider {
	static final JFakeTypeProvider instance = new JFakeTypeProvider();
	JFakeTypeProvider() {
	}
}

public class JBaseTypeProvider extends TypeProvider {
	public final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
}


public abstract class JType {
	
	public static final JType[] emptyArray = new JType[0]; 

	static Hashtable<KString,JType>		jtypeHash = new Hashtable<KString,JType>();

	public static final int flReference		= StdTypes.flReference;
	public static final int flIntegerInCode	= StdTypes.flIntegerInCode;
	public static final int flInteger			= StdTypes.flInteger;
	public static final int flFloatInCode		= StdTypes.flFloatInCode;
	public static final int flFloat				= StdTypes.flFloat;
	public static final int flNumber			= StdTypes.flNumber;
	public static final int flDoubleSize		= StdTypes.flDoubleSize;
	public static final int flArray				= StdTypes.flArray;
	public static final int flResolved			= StdTypes.flResolved;
	public static final int flBoolean			= StdTypes.flBoolean;
	public static final int flCallable			= StdTypes.flCallable;
	public static final int flAbstract			= StdTypes.flAbstract;
	public static final int flUnerasable		= StdTypes.flUnerasable;
	public static final int flVirtual			= StdTypes.flVirtual;
	public static final int flFinal				= StdTypes.flFinal;
	public static final int flStatic			= StdTypes.flStatic;
	public static final int flForward			= StdTypes.flForward;
	public static final int flFake				= 1 << 31;
	
	public static final JFakeType tpAny			= new JFakeType(StdTypes.tpAny,  JConstants.sigAny);
	public static final JFakeType tpVoid			= new JFakeType(StdTypes.tpVoid, JConstants.sigVoid);
	public static final JFakeType tpNull			= new JFakeType(StdTypes.tpNull, JConstants.sigNull);
	
	public static final JPrimitiveType tpBoolean	= new JPrimitiveType(StdTypes.tpBoolean, JConstants.sigBoolean);
	public static final JPrimitiveType tpByte		= new JPrimitiveType(StdTypes.tpByte,    JConstants.sigByte);
	public static final JPrimitiveType tpChar		= new JPrimitiveType(StdTypes.tpChar,    JConstants.sigChar);
	public static final JPrimitiveType tpShort		= new JPrimitiveType(StdTypes.tpShort,   JConstants.sigShort);
	public static final JPrimitiveType tpInt		= new JPrimitiveType(StdTypes.tpInt,     JConstants.sigInt);
	public static final JPrimitiveType tpLong		= new JPrimitiveType(StdTypes.tpLong,    JConstants.sigLong);
	public static final JPrimitiveType tpFloat		= new JPrimitiveType(StdTypes.tpFloat,   JConstants.sigFloat);
	public static final JPrimitiveType tpDouble	= new JPrimitiveType(StdTypes.tpDouble,  JConstants.sigDouble);
	
	public static final JBaseType tpObject			= ($cast JBaseType)StdTypes.tpObject.getJType();
	public static final JBaseType tpClass			= ($cast JBaseType)StdTypes.tpClass.getJType();
	public static final JBaseType tpString			= ($cast JBaseType)StdTypes.tpString.getJType();
	public static final JBaseType tpCloneable		= ($cast JBaseType)StdTypes.tpCloneable.getJType();
	public static final JBaseType tpThrowable		= ($cast JBaseType)StdTypes.tpThrowable.getJType();
	public static final JBaseType tpRule			= ($cast JBaseType)StdTypes.tpRule.getJType();
	
	public static final JArrayType tpArray			= ($cast JArrayType)StdTypes.tpArray.getJType();

	public final TypeProvider	jmeta_type;
	public final KString		java_signature;
	public final int			flags;
	
	JType(TypeProvider meta_type, KString java_signature, int flags) {
		this.jmeta_type = meta_type;
		this.java_signature = java_signature;
		this.flags = flags;
		assert(jtypeHash.get(java_signature) == null);
		jtypeHash.put(java_signature, this);
	}
	
	public abstract String toClassForNameString();
	public abstract JType getSuperType();
	public JStructView getJStruct() { return null; }

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

	public static JType leastCommonType(JType tp1, JType tp2) {
		JType tp = tp1;
		while( tp != null ) {
			if( tp1.isInstanceOf(tp) && tp2.isInstanceOf(tp) )
				return tp;
			tp = tp.getSuperType();
		}
		return tp;
	}

	public boolean isAutoCastableTo(JType t)
	{
		if( t == JType.tpVoid ) return true;
		if( this.isReference() && t.isReference() && (this==tpNull || t==tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( this == JType.tpRule && t == JType.tpBoolean ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this==tpByte && ( t==tpShort || t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( (this==tpShort || this==tpChar) && (t==tpInt || t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpInt && (t==tpLong || t==tpFloat || t==tpDouble) ) return true;
		if( this==tpLong && ( t==tpFloat || t==tpDouble) ) return true;
		if( this==tpFloat && t==tpDouble ) return true;
		return false;
	}

	public boolean isInstanceOf(JType t) {
		return this == t;
	}
}

public final class JFakeType extends JType {
	JFakeType(CoreType vtype, KString signature) {
		super(JFakeTypeProvider.instance, signature, vtype.flags | flFake);
		assert (vtype.jtype == null);
		vtype.jtype = this;
	}
	
	public String toClassForNameString() {
		return String.valueOf(java_signature);
	}

	public JType getSuperType() { return null; }

	public String toString() { return java_signature.toString(); }
}

public class JPrimitiveType extends JType {
	
	private final CoreType vtype;
	
	JPrimitiveType(CoreType vtype, KString signature) {
		super(JPrimitiveTypeProvider.instance, signature, vtype.flags);
		assert (vtype.jtype == null);
		vtype.jtype = this;
		this.vtype = vtype;
	}
	
	public String toClassForNameString() {
		return String.valueOf(java_signature);
	}

	public JType getSuperType() { return null; }

	public String toString() { return vtype.toString(); }
}


public class JBaseType extends JType {
	
	public final Struct clazz;

	private JBaseType(KString java_signature, Struct clazz) {
		super(new JBaseTypeProvider(clazz), java_signature, flReference);
		this.clazz = clazz;
	}
	
	public static JBaseType newJBaseType(Struct clazz)
		alias operator(240,lfy,new)
	{
		if (clazz.ctype ≡ Type.tpRule) {
			if (JType.tpRule == null)
				return new JBaseType(JConstants.jsigRule,clazz);
			return JType.tpRule;
		}
		KString signature = clazz.name.signature();
		JBaseType jbt = (JBaseType)jtypeHash.get(signature);
		if (jbt != null)
			return jbt;
		return new JBaseType(signature,clazz);
	}
	
	
	public JStructView getJStruct() {
		return clazz.getJView();
	}
	
	public String toClassForNameString() {
		return ((JBaseTypeProvider)this.jmeta_type).clazz.name.bytecode_name.toString().replace('/','.');
	}

	public String toString() {
		return clazz.name.short_name.toString();
	}
	
	public JType getSuperType() {
		Type sup = clazz.super_type;
		if (sup == null) return null;
		return sup.getJType();
	}
	
	public boolean isInstanceOf(JType _t2) {
		if( this == _t2 ) return true;
		if!(_t2 instanceof JBaseType) return false;
		JBaseType t2 = (JBaseType)_t2;
		JBaseType t1 = this;
		t1.clazz.checkResolved();
		t2.clazz.checkResolved();
		return t1.clazz.instanceOf(t2.clazz);
	}
}

public class JArrayType extends JType {
	public final JType				jarg;
	
	private JArrayType(KString java_signature, JType jarg) {
		super(ArrayTypeProvider.instance, java_signature, flReference | flArray);
		this.jarg = jarg;
	}

	public static JArrayType newJArrayType(JType jarg)
		alias operator(240,lfy,new)
	{
		KString signature = KString.from("["+jarg.java_signature);
		JArrayType jat = (JArrayType)jtypeHash.get(signature);
		if (jat != null)
			return jat;
		return new JArrayType(signature,jarg);
	}

	public String toClassForNameString() {
		return "["+jarg.toClassForNameString();
	}

	public String toString() {
		return jarg+"[]";
	}
	
	public JType getSuperType() {
		return JType.tpObject;
	}
	
	public boolean isInstanceOf(JType t) {
		if (this == t) return true;
		if (t == JType.tpObject) return true;
		if (t == JType.tpCloneable) return true;
		if (t instanceof JArrayType)
			return jarg.isInstanceOf(t.jarg);
		return false;
	}

}

public class JMethodType extends JType {
	public final JType[]			jargs;
	public final JType				jret;
	
	private JMethodType(KString java_signature, JType[] jargs, JType jret) {
		super(CallTypeProvider.instance, java_signature, flCallable);
		this.jargs = jargs;
		this.jret = jret;
	}

	public static JMethodType newJMethodType(JType[] jargs, JType jret)
		alias operator(240,lfy,new)
	{
		KStringBuffer ksb = new KStringBuffer(64);
		ksb.append('(');
		for (int i=0; i < jargs.length; i++)
			ksb.append(jargs[i].java_signature);
		ksb.append(')');
		ksb.append(jret.java_signature);
		KString signature = ksb.toKString();
		JMethodType jmt = (JMethodType)jtypeHash.get(signature);
		if (jmt != null)
			return jmt;
		return new JMethodType(signature,jargs,jret);
	}

	public JType getSuperType() {
		return null;
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append('(');
		for(int i=0; i < jargs.length; i++) {
			str.append(jargs[i].toClassForNameString());
			if( i < jargs.length-1)
				str.append(',');
		}
		str.append(")->").append(jret.toClassForNameString());
		return str.toString();
	}
}


