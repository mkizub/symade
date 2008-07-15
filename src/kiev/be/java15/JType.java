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
package kiev.be.java15;
import syntax kiev.Syntax;

public class JPrimitiveMetaType extends MetaType {
	JPrimitiveMetaType(int flags) {
		super(null, flags);
	}
	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }
}

public class JFakeMetaType extends MetaType {

	JFakeMetaType(int flags) {
		super(null, flags);
	}
	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }
}

public class JBaseMetaType extends MetaType {

	public final Struct clazz;
	
	JBaseMetaType(Struct clazz, int flags) {
		super(clazz, flags);
		this.clazz = clazz;
	}
	public TemplateTVarSet getTemplBindings() { return TemplateTVarSet.emptySet; }
}


public abstract class JType {
	
	public static final JType[] emptyArray = new JType[0]; 

	static Hashtable<KString,JType>		jtypeHash = new Hashtable<KString,JType>();

	public static final int flAbstract			= StdTypes.flAbstract;
	public static final int flUnerasable		= StdTypes.flUnerasable;
	public static final int flVirtual			= StdTypes.flVirtual;
	public static final int flFinal			= StdTypes.flFinal;
	public static final int flStatic			= StdTypes.flStatic;
	public static final int flForward			= StdTypes.flForward;
	
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
	
	public static final JBaseType tpObject			= (JBaseType)StdTypes.tpObject.getJType();
	public static final JBaseType tpClass			= (JBaseType)StdTypes.tpClass.getJType();
	public static final JBaseType tpString			= (JBaseType)StdTypes.tpString.getJType();
	public static final JBaseType tpCloneable		= (JBaseType)StdTypes.tpCloneable.getJType();
	public static final JBaseType tpThrowable		= (JBaseType)StdTypes.tpThrowable.getJType();
	public static final JBaseType tpRule			= (JBaseType)StdTypes.tpRule.getJType();
	
	public static final JArrayType tpArray			= (JArrayType)StdTypes.tpArray.getJType();

	public final MetaType		jmeta_type;
	public final KString		java_signature;
	public final int			flags;
	
	JType(MetaType meta_type, KString java_signature, int flags) {
		this.jmeta_type = meta_type;
		this.java_signature = java_signature;
		this.flags = flags;
		assert(jtypeHash.get(java_signature) == null);
		jtypeHash.put(java_signature, this);
	}
	
	public abstract String toClassForNameString();
	public abstract JType getSuperType();
	public JStruct getJStruct() { return null; }
	public JTypeDecl getJTypeDecl() { return (JTypeDecl)jmeta_type.tdecl; }

	public final boolean isReference()		{ return (jmeta_type.flags & MetaType.flReference)		!= 0 ; }
	public final boolean isArray()			{ return (jmeta_type.flags & MetaType.flArray)			!= 0 ; }
	public final boolean isIntegerInCode()	{ return (jmeta_type.flags & MetaType.flIntegerInCode)	!= 0 ; }
	public final boolean isInteger()		{ return (jmeta_type.flags & MetaType.flInteger)		!= 0 ; }
	public final boolean isFloatInCode()	{ return (jmeta_type.flags & MetaType.flFloatInCode)	!= 0 ; }
	public final boolean isFloat()			{ return (jmeta_type.flags & MetaType.flFloat)			!= 0 ; }
	public final boolean isNumber()		{ return (jmeta_type.flags & MetaType.flNumber)		!= 0 ; }
	public final boolean isDoubleSize()	{ return (jmeta_type.flags & MetaType.flDoubleSize)	!= 0 ; }
	public final boolean isBoolean()		{ return (jmeta_type.flags & MetaType.flBoolean)		!= 0 ; }
	public final boolean isCallable()		{ return (jmeta_type.flags & MetaType.flCallable)		!= 0 ; }
	public final boolean isAbstract()		{ return (flags & flAbstract)							!= 0 ; }
	public final boolean isUnerasable()	{ return (flags & flUnerasable)						!= 0 ; }
	public final boolean isVirtual()		{ return (flags & flVirtual)							!= 0 ; }
	public final boolean isFinal()			{ return (flags & flFinal)								!= 0 ; }
	public final boolean isStatic()		{ return (flags & flStatic)								!= 0 ; }
	public final boolean isForward()		{ return (flags & flForward)							!= 0 ; }

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
		return this == t || t == tpAny;
	}
}

public final class JFakeType extends JType {
	JFakeType(CoreType vtype, KString signature) {
		super(new JFakeMetaType(vtype.meta_type.flags), signature, vtype.flags);
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
		super(new JPrimitiveMetaType(vtype.meta_type.flags), signature, vtype.flags);
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
		super(new JBaseMetaType(clazz, MetaType.flReference), java_signature, 0);
		this.clazz = clazz;
	}
	
	public static JBaseType newJBaseType(Struct clazz)
		alias lfy operator new
	{
		KString signature = KString.from("L"+((JStruct)clazz).bname()+";");
		JBaseType jbt = (JBaseType)jtypeHash.get(signature);
		if (jbt != null)
			return jbt;
		return new JBaseType(signature,clazz);
	}
	
	
	public JStruct getJStruct() {
		return (JStruct)clazz;
	}
	
	public String toClassForNameString() {
		Struct s = ((JBaseMetaType)this.jmeta_type).clazz;
		return ((JStruct)s).bname().toString().replace('/','.');
	}

	public String toString() {
		return clazz.sname;
	}
	
	public JType getSuperType() {
		if (clazz.super_types.length == 0)
			return null;
		Type sup = clazz.super_types[0].getType();
		return sup.getJType();
	}
	
	public boolean isInstanceOf(JType _t2) {
		if( this == _t2  || _t2 == tpAny) return true;
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
		super(ArrayMetaType.instance, java_signature, 0);
		this.jarg = jarg;
	}

	public static JArrayType newJArrayType(JType jarg)
		alias lfy operator new
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
		if (this == t || t == tpAny) return true;
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
	
	private JMethodType(CallMetaType meta_type, KString java_signature, JType[] jargs, JType jret) {
		super(meta_type, java_signature, 0);
		this.jargs = jargs;
		this.jret = jret;
	}

	public static JMethodType newJMethodType(CallMetaType meta_type, JType[] jargs, JType jret)
		alias lfy operator new
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
		return new JMethodType(meta_type,signature,jargs,jret);
	}

	public JType getSuperType() {
		return null;
	}
	
	public String toClassForNameString() { throw new RuntimeException("JMethodType.toClassForNameString()"); }
	
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


