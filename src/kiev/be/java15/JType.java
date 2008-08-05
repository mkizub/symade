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


public class JTypeEnv {
	public final JEnv jenv;
	
	final Hashtable<KString,JType> jtypeSignHash;

	public final JFakeType tpAny;
	public final JFakeType tpVoid;
	public final JFakeType tpNull;
	
	public final JPrimitiveType tpBoolean;
	public final JPrimitiveType tpByte;
	public final JPrimitiveType tpChar;
	public final JPrimitiveType tpShort;
	public final JPrimitiveType tpInt;
	public final JPrimitiveType tpLong;
	public final JPrimitiveType tpFloat;
	public final JPrimitiveType tpDouble;
	
	public final JBaseType tpObject;
	public final JBaseType tpClass;
	public final JBaseType tpString;
	public final JBaseType tpCloneable;
	public final JBaseType tpThrowable;
	
	public final JArrayType tpArray;

	public JTypeEnv(JEnv jenv) {
		this.jenv = jenv;
		
		this.jtypeSignHash = new Hashtable<KString,JType>();

		tpAny		= new JFakeType(this, StdTypes.tpAny,  JConstants.sigAny);
		tpVoid		= new JFakeType(this, StdTypes.tpVoid, JConstants.sigVoid);
		tpNull		= new JFakeType(this, StdTypes.tpNull, JConstants.sigNull);
	
		tpBoolean	= new JPrimitiveType(this, StdTypes.tpBoolean, JConstants.sigBoolean);
		tpByte		= new JPrimitiveType(this, StdTypes.tpByte,    JConstants.sigByte);
		tpChar		= new JPrimitiveType(this, StdTypes.tpChar,    JConstants.sigChar);
		tpShort		= new JPrimitiveType(this, StdTypes.tpShort,   JConstants.sigShort);
		tpInt		= new JPrimitiveType(this, StdTypes.tpInt,     JConstants.sigInt);
		tpLong		= new JPrimitiveType(this, StdTypes.tpLong,    JConstants.sigLong);
		tpFloat		= new JPrimitiveType(this, StdTypes.tpFloat,   JConstants.sigFloat);
		tpDouble	= new JPrimitiveType(this, StdTypes.tpDouble,  JConstants.sigDouble);
	
		tpObject	= new JBaseType(this, StdTypes.tpObject.tdecl);
		tpClass		= new JBaseType(this, StdTypes.tpClass.tdecl);
		tpString	= new JBaseType(this, StdTypes.tpString.tdecl);
		tpCloneable	= new JBaseType(this, StdTypes.tpCloneable.tdecl);
		tpThrowable	= new JBaseType(this, StdTypes.tpThrowable.tdecl);
	
		tpArray		= new JArrayType(this, tpVoid);
	}
	
	public JType getJType(Struct s) {
		return new JBaseType(this,s);
	}

	public JType getJType(Type tp) {
		if (tp instanceof XType) {
			foreach (Type t; tp.getMetaSupers()) {
				JType jtype = this.getJType(t);
				if (jtype != null)
					return jtype;
			}
			return tpVoid;
		}
		if (tp instanceof CoreType) {
			if (tp == StdTypes.tpBoolean) return tpBoolean;
			if (tp == StdTypes.tpByte   ) return tpByte;
			if (tp == StdTypes.tpChar   ) return tpChar;
			if (tp == StdTypes.tpShort  ) return tpShort;
			if (tp == StdTypes.tpInt    ) return tpInt;
			if (tp == StdTypes.tpLong   ) return tpLong;
			if (tp == StdTypes.tpFloat  ) return tpFloat;
			if (tp == StdTypes.tpDouble ) return tpDouble;
			if (tp == StdTypes.tpAny    ) return tpAny;
			if (tp == StdTypes.tpVoid   ) return tpVoid;
			if (tp == StdTypes.tpNull   ) return tpNull;
			return tpVoid;
		}
		if (tp instanceof ArgType) {
			return this.getJType(tp.getErasedType());
		}
		if (tp instanceof CompaundType) {
			return new JBaseType(this,tp.tdecl);
		}
		if (tp instanceof ArrayType) {
			return new JArrayType(this,this.getJType(tp.arg));
		}
		if (tp instanceof CTimeType) {
			return this.getJType(tp.getEnclosedType());
		}
		if (tp instanceof CallType) {
			if (tp.isReference()) {
				return this.getJType(Type.tpClosure);
			} else {
				int arity = tp.arity;
				JType[] jargs = JType.emptyArray;
				if (arity > 0) {
					jargs = new JType[arity];
					for (int i=0; i < arity; i++) {
						jargs[i] = this.getJType(tp.arg(i));
						assert (!(jargs[i] instanceof JMethodType));
					}
				}
				JType jret = this.getJType(tp.ret());
				assert (!(jret instanceof JMethodType));
				return new JMethodType(this,(CallMetaType)tp.meta_type, jargs, jret);
			}
		}

		return tpVoid;
	}
}

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

	public static final int flAbstract			= StdTypes.flAbstract;
	public static final int flUnerasable		= StdTypes.flUnerasable;
	public static final int flVirtual			= StdTypes.flVirtual;
	public static final int flFinal				= StdTypes.flFinal;
	public static final int flStatic			= StdTypes.flStatic;
	public static final int flForward			= StdTypes.flForward;
	
	public final JTypeEnv		jtenv;
	public final MetaType		jmeta_type;
	public final KString		java_signature;
	public final int			flags;
	
	JType(JTypeEnv jtenv, MetaType meta_type, KString java_signature, int flags) {
		this.jtenv = jtenv;
		this.jmeta_type = meta_type;
		this.java_signature = java_signature;
		this.flags = flags;
		assert(jtenv.jtypeSignHash.get(java_signature) == null);
		jtenv.jtypeSignHash.put(java_signature, this);
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
		if( t == jtenv.tpVoid ) return true;
		if( this.isReference() && t.isReference() && (this==jtenv.tpNull || t==jtenv.tpNull) ) return true;
		if( isInstanceOf(t) ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this==jtenv.tpByte && ( t==jtenv.tpShort || t==jtenv.tpInt || t==jtenv.tpLong || t==jtenv.tpFloat || t==jtenv.tpDouble) ) return true;
		if( (this==jtenv.tpShort || this==jtenv.tpChar) && (t==jtenv.tpInt || t==jtenv.tpLong || t==jtenv.tpFloat || t==jtenv.tpDouble) ) return true;
		if( this==jtenv.tpInt && (t==jtenv.tpLong || t==jtenv.tpFloat || t==jtenv.tpDouble) ) return true;
		if( this==jtenv.tpLong && ( t==jtenv.tpFloat || t==jtenv.tpDouble) ) return true;
		if( this==jtenv.tpFloat && t==jtenv.tpDouble ) return true;
		return false;
	}

	public boolean isInstanceOf(JType t) {
		return this == t || t == jtenv.tpAny;
	}
}

public final class JFakeType extends JType {
	JFakeType(JTypeEnv jtenv, CoreType vtype, KString signature) {
		super(jtenv, new JFakeMetaType(vtype.meta_type.flags), signature, vtype.flags);
		//assert (vtype.jtype == null);
		//vtype.jtype = this;
	}
	
	public String toClassForNameString() {
		return String.valueOf(java_signature);
	}

	public JType getSuperType() { return null; }

	public String toString() { return java_signature.toString(); }
}

public class JPrimitiveType extends JType {
	
	private final CoreType vtype;
	
	JPrimitiveType(JTypeEnv jtenv, CoreType vtype, KString signature) {
		super(jtenv, new JPrimitiveMetaType(vtype.meta_type.flags), signature, vtype.flags);
		//assert (vtype.jtype == null);
		//vtype.jtype = this;
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

	private JBaseType(JTypeEnv jtenv, KString java_signature, Struct clazz) {
		super(jtenv, new JBaseMetaType(clazz, MetaType.flReference), java_signature, 0);
		this.clazz = clazz;
	}
	
	public static JBaseType newJBaseType(JTypeEnv jtenv, Struct clazz)
		alias lfy operator new
	{
		KString signature = KString.from("L"+((JStruct)clazz).bname()+";");
		JBaseType jbt = (JBaseType)jtenv.jtypeSignHash.get(signature);
		if (jbt != null)
			return jbt;
		return new JBaseType(jtenv,signature,clazz);
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
		return jtenv.getJType(sup);
	}
	
	public boolean isInstanceOf(JType _t2) {
		if( this == _t2  || _t2 == jtenv.tpAny) return true;
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
	
	private JArrayType(JTypeEnv jtenv, KString java_signature, JType jarg) {
		super(jtenv, ArrayMetaType.instance, java_signature, 0);
		this.jarg = jarg;
	}

	public static JArrayType newJArrayType(JTypeEnv jtenv, JType jarg)
		alias lfy operator new
	{
		KString signature = KString.from("["+jarg.java_signature);
		JArrayType jat = (JArrayType)jtenv.jtypeSignHash.get(signature);
		if (jat != null)
			return jat;
		return new JArrayType(jtenv,signature,jarg);
	}

	public String toClassForNameString() {
		return "["+jarg.toClassForNameString();
	}

	public String toString() {
		return jarg+"[]";
	}
	
	public JType getSuperType() {
		return jtenv.tpObject;
	}
	
	public boolean isInstanceOf(JType t) {
		if (this == t || t == jtenv.tpAny) return true;
		if (t == jtenv.tpObject) return true;
		if (t == jtenv.tpCloneable) return true;
		if (t instanceof JArrayType)
			return jarg.isInstanceOf(t.jarg);
		return false;
	}

}

public class JMethodType extends JType {
	public final JType[]			jargs;
	public final JType				jret;
	
	private JMethodType(JTypeEnv jtenv, CallMetaType meta_type, KString java_signature, JType[] jargs, JType jret) {
		super(jtenv, meta_type, java_signature, 0);
		this.jargs = jargs;
		this.jret = jret;
	}

	public static JMethodType newJMethodType(JTypeEnv jtenv, CallMetaType meta_type, JType[] jargs, JType jret)
		alias lfy operator new
	{
		KStringBuffer ksb = new KStringBuffer(64);
		ksb.append('(');
		for (int i=0; i < jargs.length; i++)
			ksb.append(jargs[i].java_signature);
		ksb.append(')');
		ksb.append(jret.java_signature);
		KString signature = ksb.toKString();
		JMethodType jmt = (JMethodType)jtenv.jtypeSignHash.get(signature);
		if (jmt != null)
			return jmt;
		return new JMethodType(jtenv,meta_type,signature,jargs,jret);
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


