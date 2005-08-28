/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class Type implements StdTypes, AccessFlags, Named {
	public static Type[]	emptyArray = new Type[0];

	static Hash<Type>		typeHash;

	@ref public /*private access:no,ro,no,rw*/ Struct	clazz;
	public Type[]			args = Type.emptyArray;
	public KString			signature;
	public KString			java_signature;
	public int				flags;

	public Struct getStruct() {
		return clazz;
	}

	public ASTNode resolveName(KString name) {
		return clazz.resolveName(name);
	}
	
	public Field resolveField(KString name) {
		return clazz.resolveField(name,true);
	}

	public Field resolveField(KString name, boolean fatal) {
		return clazz.resolveField(name,fatal);
	}

	public Method resolveMethod(KString name, KString sign) {
		return clazz.resolveMethod(name,sign,true);
	}

	public Method resolveMethod(KString name, KString sign, boolean fatal) {
		return clazz.resolveMethod(name,sign,fatal);
	}

	public Type getInitialType() {
		return clazz.type;
	}
	
	public Type getSuperType() {
		return Type.getRealType(this,clazz.super_type);
	}
	
	public MetaSet getStructMeta() {
		return clazz.meta;
	}


	Type() {}

	protected Type(Struct clazz) {
		this.clazz = clazz;
		signature = Signature.from(clazz, null, null, null);
		java_signature = Signature.getJavaSignature(signature);
		flags = flReference;
//		if( clazz.isArgument() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this+" with signature "+signature);
	}

	protected Type(Struct clazz, Type[] args) {
		this.clazz = clazz;
		signature = Signature.from(clazz, null, args, null);
		if( args != null && args.length > 0 ) {
			this.args = args;
			java_signature = Signature.from(clazz, null, null, null);
		} else {
			args = emptyArray;
			java_signature = signature;
		}
		java_signature = Signature.getJavaSignature(java_signature);
		flags = flReference;
//		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		typeHash.put(this);
		trace(Kiev.debugCreation,"New type created: "+this
			+" with signature "+signature+" / "+java_signature);
	}

	protected Type(ClazzName name, Type[] args) {
		this(Env.newStruct(name),args);
	}

	public static BaseType newJavaRefType(Struct clazz) {
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return (BaseType)t;
		}
		t = new BaseType(clazz);
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(Type tp) {
		return newRefType(tp.clazz);
	}
	
	public static BaseType newRefType(Struct clazz) {
		if( clazz != null && clazz.type != null && clazz.type.args.length > 0 )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		Type[] args = Type.emptyArray;
		KString signature = Signature.from(clazz,null,null,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			t.flags |= flReference;
			return (BaseType)t;
		}
		t = new BaseType(clazz);
		t.flags |= flReference;
		return (BaseType)t;
	}

	public static BaseType newRefType(Type tp, Type[] args) {
		return newRefType(tp.clazz, args);
	}
	
	public static BaseType newRefType(Struct clazz, Type[] args) {
		if( clazz != null && clazz.type != null && clazz.type.args.length != args.length )
			throw new RuntimeException("Class "+clazz+" requares "+clazz.type.args.length+" arguments");
		if( clazz != null && clazz.type != null ) {
			for(int i=0; i < args.length; i++) {
				if( !args[i].isInstanceOf(clazz.type.args[i]) ) {
					if( clazz.type.args[i].clazz.super_type == Type.tpObject && !args[i].isReference())
						;
					else
						throw new RuntimeException("Type "+args[i]+" must be an instance of "+clazz.type.args[i]);
				}
			}
		}
		KString signature = Signature.from(clazz,null,args,null);
		Type t = typeHash.get(signature.hashCode(),fun (Type t)->boolean { return t.signature.equals(signature); });
		if( t != null ) {
			if( t.clazz == null ) t.clazz = clazz;
			trace(Kiev.debugCreation,"Type "+t+" with signature "+t.signature+" already exists");
			return (BaseType)t;
		}
		t = new BaseType(clazz,args);
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
	
	public NodeName getName()		{ return clazz.name; }
	public ClazzName getClazzName()	{ return clazz.name; }
	
	public void invalidate() {
		// called when clazz was changed
	}
	
	public rule resolveStaticNameR(ASTNode@ node, ResInfo info, KString name)
	{
		clazz.resolveNameR(node, info, name)
	}
	
	public rule resolveNameAccessR(ASTNode@ node, ResInfo info, KString name)
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
	private rule resolveNameR_1(ASTNode@ node, ResInfo info, KString name)
	{
		clazz instanceof Struct,
		node @= getStruct().members,
		node instanceof Field && ((Field)node).name.equals(name) && info.check(node)
	}
	private rule resolveNameR_3(ASTNode@ node, ResInfo info, KString name)
		Type@ sup;
	{
		sup @= getDirectSuperTypes(),
		info.enterSuper() : info.leaveSuper(),
		Type.getRealType(this, sup).resolveNameAccessR(node,info,name)
	}

	private rule resolveNameR_4(ASTNode@ node, ResInfo info, KString name)
		ASTNode@ forw;
	{
			forw @= getStruct().members,
			forw instanceof Field && forw.isForward() && !forw.isStatic(),
			info.enterForward(forw) : info.leaveForward(forw),
			Type.getRealType(this,((Field)forw).type).resolveNameAccessR(node,info,name)
	}

	public rule resolveCallStaticR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
	{
		clazz.resolveStructMethodR(node, info, name, mt, this)
	}
	
	public rule resolveCallAccessR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
		ASTNode@ member;
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
			member instanceof Field && member.isForward(),
			info.enterForward(member) : info.leaveForward(member),
			Type.getRealType(this,((Field)member).type).resolveCallAccessR(node,info,name,mtype)
		}
	}

	public int hashCode() { return signature.hashCode(); }

	public String toString() {
		if( isArray() )
			return args[0]+"[]";
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

	public final boolean equals(Object to) {
		if(to != null && to instanceof Type ) return equals((Type)to);
		if(to instanceof TypeRef ) return equals(((TypeRef)to).getType());
		return false;
	}

	public boolean string_equals(Type to) {
		return signature.equals( to.signature );
	}

	public boolean equals(Type to) {
		if( signature.equals( ((Type)to).signature ) ) return true;
		else if (this.isBoolean() && to.isBoolean() ) return true;
		else if (this.isArgument())
			return getSuperType().equals(to);
		else if (to.isArgument())
			return this.equals(to.getSuperType());
		return false;
	}

	public boolean checkResolved() {
		clazz.checkResolved();
		return true;
	}

	public boolean isInstanceOf(Type t) {
		return isInstanceOf(this,t);
	}

	public static boolean isInstanceOf(Type t1, Type t2) {
		if( t1.equals(t2) ) return true;
		if( t1.isReference() && t2.equals(Type.tpObject) ) return true;
		try {
			t1.checkResolved();
			t2.checkResolved();
		} catch(Exception e ) {
			if( Kiev.verbose ) e.printStackTrace( /* */System.out /* */ );
			throw new RuntimeException("Unresolved type:"+e);
		}
		// Instance of closure
		if( t1.isStructInstanceOf(Type.tpClosureClazz) ) {
			if( t2 == tpClosure )
				return true;
			if( t2.isStructInstanceOf(Type.tpClosureClazz) ) {
				if( t1.args.length != t2.args.length ) return false;
				for(int i=0; i < t1.args.length; i++)
					if( !isInstanceOf(t1.args[i],t2.args[i]) ) return false;
				return true;
			}
		}
		if( t1.isArray() && t2.isArray() && isInstanceOf(t1.args[0],t2.args[0]))
			return true;
		// Check class1 == class2 && arguments
		if( t1.clazz != null && t2.clazz != null && t1.clazz.equals(t2.clazz) ) {
			int t1_args_len = t1.args==null?0:t1.args.length;
			int t2_args_len = t2.args==null?0:t2.args.length;
			if( t1_args_len != t2_args_len ) return false;
			if( t1_args_len == 0 ) return true;
			for(int i=0; i < t1.args.length; i++)
				if( !isInstanceOf(t1.args[i],t2.args[i]) ) return false;
			return true;
		}
		foreach (Type sup; t1.getDirectSuperTypes()) {
			if (isInstanceOf(Type.getRealType(t1,sup),t2))
				return true;
		}
		return false;
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
		if( this.isReference() && t.isReference()
		 && !this.isArgument()
		 && !this.isArray()
		 && this.clazz.package_clazz.isClazz()
		 && !this.clazz.isStatic() && this.clazz.package_clazz.type.isAutoCastableTo(t)
		)
			return true;
		if( this == Type.tpRule && t == Type.tpBoolean ) return true;
		if( this.isBoolean() && t.isBoolean() ) return true;
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(t) == this ) return true;
			else if( !Kiev.javaMode && t==Type.tpInt && this.isInstanceOf(Type.tpEnum) )
				return true;
		}
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(this) == t ) return true;
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
			if( tp1.isInstanceOf(tp) && tp2.isInstanceOf(tp) ) return tp;
			tp = tp.clazz.super_type;
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
		if( this.isReference() && t.isReference() && (this.clazz.name.short_name.equals(Constants.nameIdefault)) ) return true;
		if( this.isReference() && t.isReference()
		 && !this.isArgument()
		 && !this.isArray()
		 && this.clazz.package_clazz.isClazz()
		 && !this.clazz.isStatic() && this.clazz.package_clazz.type.isAutoCastableTo(t)
		)
			return true;
		if( t.isEnum())
			return this.isCastableTo(Type.tpInt);
		if( t.isArgument() && isCastableTo(t.getSuperType()) )
			return true;
		if( t.isArgument() && !this.isReference() ) {
//			Kiev.reportWarning(0,"Cast of argument to primitive type - ensure 'generate' of this type and wrapping in if( A instanceof type ) statement");
			return true;
		}
		if( this instanceof ClosureType && !(t instanceof CallableType) && this.args.length == 0 ) {
			if( ((ClosureType)this).ret.isCastableTo(t) ) return true;
		}
		return false;
	}

	public static Type getRefTypeForPrimitive(Type tp) {
		if( tp.isReference() ) return tp;
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

	public Type getNonArgsType() {
		if( isArgument() ) return getSuperType().getNonArgsType();
		if( args.length == 0 || isArray() ) return this;
		Type[] targs = clazz.type.args;
		Type[] jargs = new Type[targs.length];
		for(int i=0; i < targs.length; i++) {
			jargs[i] = targs[i].getNonArgsType();
		}
		return Type.newRefType(clazz,jargs);
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

	public boolean isAnnotation()			{ return clazz.isAnnotation(); }
	public boolean isAbstract()				{ return clazz.isAbstract(); }
	public boolean isEnum()					{ return clazz.isEnum(); }
	public boolean isJavaEnum()				{ return clazz.isJavaEnum(); }
	public boolean isInterface()			{ return clazz.isInterface(); }
	public boolean isClazz()				{ return clazz.isClazz(); }
	public boolean isHasCases()				{ return clazz.isHasCases(); }
	public boolean isPizzaCase()			{ return clazz.isPizzaCase(); }
	public boolean isStaticClazz()			{ return clazz.isStatic(); }
	public boolean isStruct()				{ return clazz instanceof Struct; }
	public boolean isAnonymouseClazz()		{ return clazz.isAnonymouse(); }
	public boolean isLocalClazz()			{ return clazz.isAnonymouse(); }
	public boolean isStructInstanceOf(Struct s)	{ return clazz.instanceOf(s); }
	
	public boolean isWrapper()						{ return false; }
	public Expr makeWrappedAccess(ASTNode from)	{ throw new RuntimeException("Type "+this+" is not a wrapper"); } 
	public Type getWrappedType()					{ throw new RuntimeException("Type "+this+" is not a wrapper"); }
	
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
		if( !isReference() ) {
//			if( this == Type.tpRule ) return Type.tpBoolean;
			return this;
		}
		if( isArray() ) return newArrayType(args[0].getJavaType());
		if( this instanceof CallableType ) {
			if( this.isStructInstanceOf(Type.tpClosureClazz) )
				return newJavaRefType(clazz);
			if( args.length == 0 )
				return MethodType.newMethodType(Type.emptyArray,((MethodType)this).ret.getJavaType());
			Type[] targs = new Type[args.length];
			for(int i=0; i < args.length; i++)
				targs[i] = args[i].getJavaType();
			return MethodType.newMethodType(targs,((MethodType)this).ret.getJavaType());
		}
		if( args.length == 0 ) return this;
		return newJavaRefType(clazz);
	}

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
		if( !t2.isReference() )			return t2;
		if( t1.isArgument() )			return t2;
		// No deep recursion for rewriting rules
		if( get_real_type_depth > 32 ) return t2;
		get_real_type_depth++;
		try {
		if( t1.isArray() ) return getRealType(t1.args[0],t2);
		if( t2.isArray() ) return Type.newArrayType(getRealType(t1,t2.args[0]));
		if( t2.isArgument() ) {
			for(int i=0; i < t1.args.length && i < t1.clazz.type.args.length; i++) {
				if( t1.clazz.type.args[i].string_equals(t2) ) {
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
			if( t2.clazz == tpArray.clazz )
				tp = newArrayType(tpargs[0]);
			else if( t2.isInstanceOf(Type.tpClosure) )
				tp = ClosureType.newClosureType(t2.clazz,tpargs,getRealType(t1,((ClosureType)t2).ret));
			else if( t2 instanceof MethodType )
				tp = MethodType.newMethodType(tpargs,tpret);
			else
				tp = newRefType(t2.clazz,tpargs);
			trace(Kiev.debugResolve,"Type "+t2+" rewritten into "+tp+" using "+t1);
			return tp;
		}
		// Nothing was rewritten...
		if( t1.clazz.super_type != null ) return getRealType(t1.clazz.super_type,t2);
		return t2;
		} finally { get_real_type_depth--; }
	}

	public static Type getProxyType(Type tp) {
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

	public void checkJavaSignature() {}

	public Dumper toJava(Dumper dmp) {
		if( isArray() )
			return dmp.append(args[0]).append("[]");
		else
			return clazz.toJava(dmp);
	}

}

public class BaseType extends Type {
	public static BaseType[]	emptyArray = new BaseType[0];

	BaseType() {
		super();
	}
	
	BaseType(Struct clazz) {
		super(clazz);
	}
	
	BaseType(Struct clazz, Type[] args) {
		super(clazz,args);
	}
	
}

public class ArrayType extends Type {

	private static final ClazzName cname = ClazzName.fromSignature(KString.from("Lkiev/stdlib/Array;"));
	
	public static ArrayType newArrayType(Type type) {
		KString sign = new KStringBuffer(type.signature.len).append('[').append(type.signature).toKString();
		ArrayType t = (ArrayType)typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		t = new ArrayType();
		t.clazz = null;
		t.args = new Type[]{type};
		t.signature = sign;
		t.java_signature = new KStringBuffer(type.java_signature.len+1).append_fast((byte)'[')
			.append_fast(type.java_signature).toKString();
		t.flags	 |= flReference | flArray;
		if( t.args[0].isArgumented() ) t.flags |= flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature+" / "+t.java_signature);
		return t;
	}

	public NodeName getName()						{ return cname; }
	public ClazzName getClazzName()					{ return cname; }
	public boolean isArgument()						{ return false; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
	public boolean isEnum()							{ return false; }
	public boolean isJavaEnum()						{ return false; }
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
	public MetaSet getStructMeta()					{ return tpObject.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return new Type[] {tpObject, tpCloneable}; }

	public rule resolveStaticNameR(ASTNode@ node, ResInfo info, KString name)
	{
		false
	}
	
	public rule resolveNameAccessR(ASTNode@ node, ResInfo info, KString name)
	{
		tpObject.resolveNameAccessR(node, info, name)
	}

	public rule resolveCallStaticR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
	{
		false
	}
	
	public rule resolveCallAccessR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
	{
		tpObject.resolveCallAccessR(node, info, name, mt)
	}
	
	public Type getJavaType() {
		return newArrayType(args[0].getJavaType());
	}

	public boolean checkResolved() {
		return true;
	}

	public void checkJavaSignature() {
		Type jt = getJavaType();
		java_signature = jt.java_signature;
	}

	public String toString() {
		return String.valueOf(args[0])+"[]";
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(args[0]).append("[]");
	}

}

public class ArgumentType extends Type {

	/** Variouse names of the type */
	public ClazzName			name;

	/** Bound super-class for class arguments */
	public Type					super_type;

	private ArgumentType(ClazzName name, Type sup) {
		this.name = name;
		super_type = sup;
	}
	
	public static ArgumentType newArgumentType(ClazzName name, Type sup) {
		KString sign = KString.from("A"+name.bytecode_name+";");
		ArgumentType t = (ArgumentType)typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		if (sup == null)
			sup = tpObject;
		t = new ArgumentType(name,sup);
		t.signature = sign;
		t.java_signature = sup.java_signature;
		t.flags	|= flReference | flArgumented;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type argument: "+t+" with signature "+t.signature+" / "+t.java_signature);
		return t;
	}

	public static ArgumentType newArgumentType(Struct owner, KString name) {
		KString nm = KString.from(owner.name.name+"$"+name);
		KString bc = KString.from(owner.name.bytecode_name+"$"+name);
		ClazzName cn = new ClazzName(nm,name,bc,true,true);
		return newArgumentType(cn,null);
	}

	public NodeName getName()						{ return name; }
	public ClazzName getClazzName()					{ return name; }
	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return super_type.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isJavaEnum()						{ return false; }
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
	public MetaSet getStructMeta()					{ return super_type.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return super_type.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(ASTNode@ node, ResInfo info, KString name)
	{
		false
	}
	
	public rule resolveNameAccessR(ASTNode@ node, ResInfo info, KString name)
	{
		super_type.resolveNameAccessR(node, info, name)
	}

	public rule resolveCallStaticR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
	{
		false
	}
	
	public rule resolveCallAccessR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
	{
		super_type.resolveCallAccessR(node, info, name, mt)
	}
	
	public Type getJavaType() {
		if (super_type == null)
			return tpObject;
		return super_type.getJavaType();
	}

	public boolean checkResolved() {
		super_type.checkResolved();
		return true;
	}

	public void checkJavaSignature() {
		Type jt = getJavaType();
		java_signature = jt.java_signature;
	}

	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.append(super_type);
	}

}

public interface CallableType {
	@virtual public virtual access:ro Type[]	args;
	@virtual public virtual access:ro Type		ret;
}

public class ClosureType extends BaseType implements CallableType {
	public virtual Type		ret;
	
	private ClosureType(Struct clazz, Type[] args, Type ret, KString sign) {
		super(clazz,args);
		this.ret = ret;
		signature = sign;
		java_signature = Signature.getJavaSignature(new KString.KStringScanner(sign));
		flags |= flReference;
		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New closure type created: "+this+" with signature "+signature+" / "+java_signature);
	}

	public static ClosureType newClosureType(Struct clazz, Type[] args, Type ret) {
		if (ret   == null) ret   = Type.tpAny;
		KString sign = Signature.from(clazz,Type.emptyArray,args,ret);
		ClosureType t = (ClosureType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && t.clazz.equals(clazz); });
		if( t != null ) return t;
		t = new ClosureType(clazz,args,ret,sign);
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

}
	
public class WrapperType extends Type {
	
	public static final Type tpWrappedPrologVar = newWrapperType(tpPrologVar);
	public static final Type tpWrappedRefProxy  = newWrapperType(tpRefProxy);
	
	Field wrapped_field;

	public static Type newWrapperType(Type type) {
		KString sign = new KStringBuffer(type.signature.len).append('%').append(type.signature).toKString();
		Type t = typeHash.get(sign.hashCode(),fun (Type t)->boolean { return t.signature.equals(sign); });
		if( t != null ) return t;
		t = new WrapperType();
		t.wrapped_field = type.getStruct().getWrappedField(true);
		t.clazz = type.clazz;
		t.args = type.args;
		t.signature = sign;
		t.java_signature = type.java_signature;
		t.flags	 = type.flags | flWrapper;
		typeHash.put(t);
		trace(Kiev.debugCreation,"New type created: "+t+" with signature "+t.signature+" / "+t.java_signature);
		return t;
	}
	
	public final boolean isWrapper()					{ return true; }
	public final Expr makeWrappedAccess(ASTNode from)	{ return new AccessExpr(from.pos,(Expr)from, wrapped_field); } 
	public final Type getWrappedType()					{ return Type.getRealType(this, wrapped_field.type); }
	
	public Type getUnwrappedType()						{ return Type.fromSignature(signature.substr(1)); }
	
	public rule resolveNameAccessR(ASTNode@ node, ResInfo info, KString name)
	{
		info.isForwardsAllowed(),$cut,
		trace(Kiev.debugResolve,"Type: Resolving name "+name+" in wrapper type "+this),
		checkResolved(),
		{
			info.enterForward(wrapped_field, 0) : info.leaveForward(wrapped_field, 0),
			getWrappedType().resolveNameAccessR(node, info, name),
			$cut
		;	info.enterSuper(10) : info.leaveSuper(10),
			super.resolveNameAccessR(node, info, name)
		}
	;
		super.resolveNameAccessR(node, info, name)
	}
	public rule resolveCallAccessR(ASTNode@ node, ResInfo info, KString name, MethodType mt)
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
			super.resolveCallAccessR(node, info, name, mt)
		}
	;
		super.resolveCallAccessR(node, info, name, mt)
	}
	
}

public class MethodType extends Type implements CallableType {
	public virtual Type		ret;
	public Type[]	fargs;	// formal arguments for parametriezed methods

	private MethodType(Type ret, Type[] args, Type[] fargs, KString sign) {
		super(tpMethodClazz,args);
		this.ret = ret;
		this.fargs = fargs;
		signature = sign;
		KStringBuffer ksb = new KStringBuffer(64);
		ksb.append((byte)'(');
		for(int i=0; i < args.length; i++)
			ksb.append(args[i].java_signature);
		ksb.append((byte)')');
		ksb.append(ret.java_signature);
		java_signature = ksb.toKString();
		if( clazz.isArgument() ) flags |= flArgumented;
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( ret.isArgumented() ) flags |= flArgumented;
		typeHash.put(this);
		trace(Kiev.debugCreation,"New method type created: "+this+" with signature "+signature+" / "+java_signature);
	}

	public static MethodType newMethodType(Type[] fargs, Type[] args, Type ret) {
		if (fargs == null) fargs = Type.emptyArray;
		if (ret   == null) ret   = Type.tpAny;
		Struct clazz = tpMethodClazz;	// BUG, if not a local variable, compile with errors
		KString sign = Signature.from(tpMethodClazz,fargs,args,ret);
		MethodType t = (MethodType)typeHash.get(sign.hashCode(),fun (Type t)->boolean {
			return t.signature.equals(sign) && t.clazz.equals(clazz); });
		if( t != null ) return t;
		t = new MethodType(ret,args,fargs,sign);
		return t;
	}
	public static MethodType newMethodType(Type[] args, Type ret) {
		return newMethodType(null,args,ret);
	}

	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

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

	public boolean argsClassesEquals(MethodType tp) {
		if( args.length != tp.args.length ) return false;
		for(int i=0; i < args.length; i++)
			if( !args[i].clazz.equals(tp.args[i].clazz) )
				return false;
		return true;
	}

	public void checkJavaSignature() {
		if( clazz == tpMethodClazz ) {
			KStringBuffer ksb = new KStringBuffer(64);
			ksb.append((byte)'(');
			for(int i=0; i < args.length; i++)
				ksb.append(args[i].java_signature);
			ksb.append((byte)')');
			ksb.append(ret.java_signature);
			java_signature = ksb.toKString();
			trace(Kiev.debugCreation,"Type "+this+" with signature "+signature+" java signature changed to "+java_signature);
		}
	}
}

