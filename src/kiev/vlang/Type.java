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
	public abstract Type getInitialType();
	public abstract Type getInitialSuperType();
	public abstract Type getSuperType();
	public abstract String toString();
	public abstract boolean checkResolved();
	public abstract Type[] getDirectSuperTypes();
	public abstract Type getErasedType();
	public abstract Dumper toJava(Dumper dmp);
	public abstract TVarSet bindings();
	public abstract ArgumentType getOuterArg();
	
	public final Type rebind(TVarSet bindings) {
		return meta_type.rebind(this,bindings);
	}
	public final Type bind(TVarSet bindings) {
		return meta_type.bind(this,bindings);
	}
	public final Type resolve(ArgumentType arg) {
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

	public void invalidate() { /* called when clazz was changed */ }
	
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
	

	public static boolean identity(Type t1, Type t2) alias operator (60, xfx, ≡ ) {
		return t1 == t2;
	}

	public static boolean not_identity(Type t1, Type t2) alias operator (60, xfx, ≢ ) {
		return t1 != t2;
	}

	public final boolean equals(Object to) alias operator (60, xfx, ≈ ) {
		if (to instanceof Type) return this.eq((Type)to);
		return false;
	}

	public static boolean type_equals(Type t1, Type t2) alias operator (60, xfx, ≈ ) {
		if (t1 ≡ null || t2 ≡ null) return false;
		return t1.eq(t2);
	}

	public static boolean type_not_equals(Type t1, Type t2) alias operator (60, xfx, ≉ ) {
		if (t1 ≡ null || t2 ≡ null) return true;
		return !t1.eq(t2);
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
			if( getRefTypeForPrimitive(t) ≈ this ) return true;
			else if( !Kiev.javaMode && t ≡ Type.tpInt && this ≥ Type.tpEnum )
				return true;
		}
		if( this.isReference() && !t.isReference() ) {
			if( getRefTypeForPrimitive(t) ≈ this ) return true;
			else if( !Kiev.javaMode && this ≡ Type.tpInt && t ≥ Type.tpEnum ) return true;
		}
		if( this ≡ tpByte && (t ≡ tpShort || t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( (this ≡ tpShort || this ≡ tpChar) && (t ≡ tpInt || t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpInt && (t ≡ tpLong || t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpLong && ( t ≡ tpFloat || t ≡ tpDouble) ) return true;
		if( this ≡ tpFloat && t ≡ tpDouble ) return true;
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
		else if( this.isReference() ) {
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

	public static BaseType getRefTypeForPrimitive(Type tp) {
		if( tp.isReference() ) return (BaseType)tp;
		if     ( tp ≡ Type.tpBoolean ) return Type.tpBooleanRef;
//		else if( tp ≡ Type.tpRule   ) return Type.tpBooleanRef;
		else if( tp ≡ Type.tpByte   ) return Type.tpByteRef;
		else if( tp ≡ Type.tpShort  ) return Type.tpShortRef;
		else if( tp ≡ Type.tpInt    ) return Type.tpIntRef;
		else if( tp ≡ Type.tpLong   ) return Type.tpLongRef;
		else if( tp ≡ Type.tpFloat  ) return Type.tpFloatRef;
		else if( tp ≡ Type.tpDouble ) return Type.tpDoubleRef;
		else if( tp ≡ Type.tpChar   ) return Type.tpCharRef;
		else if( tp ≡ Type.tpVoid   ) return Type.tpVoidRef;
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
	public final boolean isCallable()		{ return (flags & flCallable)		!= 0 ; }
	public final boolean isArgumented()	{ return (flags & flArgumented)		!= 0 ; }
	public final boolean isRtArgumented()	{ return (flags & flRtArgumented)	!= 0 ; }
	public final boolean isArgVirtual()	{ return (flags & flArgVirtual)		!= 0 ; }
	public final boolean isArgForward()	{ return (flags & flArgForward)		!= 0 ; }

	public boolean isAnnotation()			{ return false; }
	public boolean isAbstract()				{ return false; }
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
		return t2.rebind(t1.bindings());
	}

	public static BaseType getProxyType(Type tp) {
		TVarSet set = new TVarSet();
		set.append(tpRefProxy.clazz.args[0].getAType(), tp);
		return (BaseType)tpRefProxy.bind(set);
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
	public ArgumentType getOuterArg()	{ return null; }
	public TVarSet bindings()			{ return TVarSet.emptySet; }
	public Meta getMeta(KString name)	{ return null; }
	public Type getInitialType()		{ return this; }
	public Type getErasedType()			{ return this; }
	public Type getInitialSuperType()	{ return null; }
	public Type getSuperType()			{ return null; }
	public boolean checkResolved()		{ return true; }
	public Type[] getDirectSuperTypes(){ return Type.emptyArray; }
	public String toString()			{ return name.toString(); }
	public Dumper toJava(Dumper dmp)	{ return dmp.append(name.toString()); }

	public JType getJType()				{ return this.jtype; }
}

public final class BaseType extends Type {

	private int			version;
	private TVarSet		bindings;
	
	public TVarSet bindings() {
		if (this.version != this.meta_type.version)
			updateBindings();
		return this.bindings;
	}
	
	private void updateBindings() {
		if (this ≡ clazz.type) {
			TVarSet vs = new TVarSet();
			foreach (TypeDef ad; clazz.args) {
				vs.append(ad.getAType(), null);
			}
			foreach (DNode d; clazz.members; d instanceof TypeDef) {
				TypeDef td = (TypeDef)d;
				vs.append(td.getAType(), td.getAType().super_type);
			}
			foreach (Type st; this.getDirectSuperTypes()) {
				vs.append(st.bindings());
			}
			this.bindings = vs;
		} else {
			TVarSet vs = clazz.type.bindings().bind(this.bindings);
			this.bindings = vs;
		}
		this.version = this.meta_type.version;
		checkArgumented();
	}
	
	BaseType(Struct clazz) {
		this(clazz.imeta_type, TVarSet.emptySet);
	}
	
	BaseType(BaseTypeProvider meta_type, TVarSet bindings) {
		super(meta_type);
		this.bindings = bindings;
		checkArgumented();
	}
	
	private void checkArgumented() {
		flags &= ~(flArgumented | flRtArgumented);
		foreach(TVar v; this.bindings().tvars; !v.isAlias()) {
			if (v.result().isArgumented()) {
				flags |= flArgumented;
				if (v.var.isRtArgumented()) {
					flags |= flRtArgumented;
					break;
				}
			}
		}
	}
	
//	public static BaseType newRefType(Struct clazz, TVarSet bindings)
//		alias operator(240,lfy,new)
//	{
//		Type ct = clazz.type;
//		if (ct.bindings().length != bindings.length )
//			throw new RuntimeException("Class "+clazz+" requares "+ct.bindings().length+" type bindings");
//		BaseType t = new BaseType(clazz.imeta_type, bindings);
//		return (BaseType)t;
//	}

	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = new JBaseType(clazz);
		return jtype;
	}

	public final Struct get$clazz() { return ((BaseTypeProvider)meta_type).clazz; }

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(BaseType:Type type) {
		if (this.clazz != type.clazz) return false;
		return this.bindings().eq(type.bindings());
	}
	
	public ArgumentType getOuterArg() {
		Struct pkg = clazz.package_clazz;
		if (clazz.isStatic() || clazz.isPackage() || clazz.package_clazz.isPackage())
			return null;
		int n = 0;
		KString name = KString.from(Constants.nameThis+"$"+n+"$type");
		TVar[] tvars = bindings().tvars;
		for (int i=0; i < tvars.length; i++) {
			if (tvars[i].var.name == name)
				return tvars[i].var;
		}
		return null;
	}
	
	
	public Struct getStruct()			{ return clazz; }
	public Type getInitialType()		{ return clazz.type; }
	public Type getInitialSuperType()	{ return clazz.super_type; }
	public Meta getMeta(KString name)	{ return clazz.meta.get(name); }
	public Type getSuperType()			{ return clazz.super_type; }

	public boolean isAnnotation()			{ return clazz.isAnnotation(); }
	public boolean isAbstract()				{ return clazz.isAbstract(); }
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
			if( Kiev.verbose ) e.printStackTrace(System.out);
			throw new RuntimeException("Unresolved type:"+e);
		}
		// Check class1 == class2 && arguments
		if (t1.clazz == t2.clazz) {
			TVarSet b1 = t1.bindings();
			TVarSet b2 = t2.bindings();
			for(int i=0; i < b1.length; i++) {
				if (!b1[i].result().isInstanceOf(b2[i].result()))
					return false;
			}
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

	public Type getErasedType() {
		if( !isReference() )
			return this;
		return clazz.type;
	}

}

public class ArrayType extends Type {

	private static final ClazzName cname = ClazzName.fromSignature(KString.from("Lkiev/stdlib/Array;"));
	
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
		if( arg.isArgumented() ) this.flags |= flArgumented;
	}

	public TVarSet bindings()			{ return new TVarSet(tpArrayArg, arg); /*arg.bindings()*/; }
	public ArgumentType getOuterArg()	{ return arg.getOuterArg(); }
	
	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null) {
			jtype = new JArrayType(this.arg.getJType());
		}
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(ArrayType:Type type) {
		return this.arg ≈ type.arg;
	}

	public boolean isArgument()						{ return false; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
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
	public Type getInitialType()					{ return this; }
	public Type getInitialSuperType()				{ return tpObject; }
	public Meta getMeta(KString name)				{ return null; }
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

public class ArgumentType extends Type {

	/** Variouse names of the type */
	public final KString			name;

	/** The class this argument belongs to */
	public final TypeDecl			definer;

	/** Bound super-class for class arguments */
	public Type						super_type;
	
	public ArgumentType(KString name, TypeDecl definer, Type sup, boolean is_unerasable, boolean is_virtual, boolean is_forward) {
		super(ArgumentTypeProvider.instance);
		this.name = name;
		this.definer = definer;
		this.super_type = (sup == null) ? tpObject : sup;
		this.flags |= flReference | flArgumented;
		if (is_unerasable) this.flags |= flRtArgumented;
		if (is_virtual) this.flags |= flArgVirtual;
		if (is_forward) this.flags |= flArgForward;
	}
	
	public TVarSet bindings()			{ return TVarSet.emptySet; }
	public ArgumentType getOuterArg()	{ return null; }

	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = super_type.getJType();
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(ArgumentType:Type type) {
		return this ≡ type;
	}

	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return super_type.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return super_type.isHasCases(); }
	public boolean isPizzaCase()					{ return super_type.isPizzaCase(); }
	public boolean isStaticClazz()					{ return super_type.isStaticClazz(); }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return super_type.isStructInstanceOf(s); }
	public Type getSuperType()						{ return super_type; }
	public Type getInitialType()					{ return super_type.getInitialType(); }
	public Type getInitialSuperType()				{ return super_type.getInitialSuperType(); }
	public Meta getMeta(KString name)				{ return super_type.getMeta(name); }
	public Type[] getDirectSuperTypes()			{ return super_type.getDirectSuperTypes(); }
	public Struct getStruct()						{ return super_type.getStruct(); }
	
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
		return dmp.append(name);
	}

	public boolean isInstanceOf(Type t) {
		if (this ≡ t) return true;
		return super_type.isInstanceOf(t);
	}

}

public class WrapperType extends Type {
	
	public static final Type tpWrappedPrologVar = newWrapperType(tpPrologVar);
	public static final Type tpWrappedRefProxy  = newWrapperType(tpRefProxy);
	
	final BaseType unwrapped_type;

	public static Type newWrapperType(Type type) {
		return new WrapperType((BaseType)type);
	}
	
	public WrapperType(BaseType unwrapped_type) {
		super(WrapperTypeProvider.instance(unwrapped_type.getStruct()));
		this.unwrapped_type = unwrapped_type;
		this.flags	 = flReference | flWrapper;
		if (unwrapped_type.isArgumented()) this.flags |= flArgumented;
		if (unwrapped_type.isRtArgumented()) this.flags |= flRtArgumented;
	}

	private Field get$wrapped_field() { return ((WrapperTypeProvider)this.meta_type).field; }
	
	public TVarSet bindings()			{ return getUnwrappedType().bindings(); }
	public ArgumentType getOuterArg()	{ return getUnwrappedType().getOuterArg(); }

	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = getUnwrappedType().getJType();
		return jtype;
	}

	protected access:no,rw,no,rw boolean eq(Type:Type t) { return false; }
	protected access:no,rw,no,rw boolean eq(WrapperType:Type type) {
		return this.getUnwrappedType() ≈ type.getUnwrappedType();
	}

	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return false; }
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
	
	public BaseType getUnwrappedType()					{ return unwrapped_type; }
	
	public Struct getStruct()			{ return getUnwrappedType().getStruct(); }
	public Meta getMeta(KString name)	{ return getUnwrappedType().getMeta(name); }
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
		if (this ≡ t) return true;
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
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( this.ret.isArgumented() ) flags |= flArgumented;
	}

	@getter public Type[]	get$args()	{ return args; }
	@getter public Type		get$ret()	{ return ret; }

	public TVarSet bindings()			{ return TVarSet.emptySet; }
	public ArgumentType getOuterArg()	{ return null; }

	public JType getJType() {
//		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
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

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

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
		foreach(Type a; args; a.isArgumented() ) { flags |= flArgumented; break; }
		if( this.ret.isArgumented() ) flags |= flArgumented;
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

	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }

	public TVarSet bindings()			{ return bindings; }
	public ArgumentType getOuterArg()	{ return null; }

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
	public Type getInitialType()		{ return Type.tpAny; }
	public Type getInitialSuperType()	{ return null; }

	public Type[] getDirectSuperTypes() { return Type.emptyArray; }

	public Type getErasedType() {
		if( args.length == 0 )
			return new MethodType(Type.emptyArray,((MethodType)this).ret.getErasedType());
		Type[] targs = new Type[args.length];
		for(int i=0; i < args.length; i++)
			targs[i] = args[i].getErasedType();
		return new MethodType(targs,((MethodType)this).ret.getErasedType());
	}

}

