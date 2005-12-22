package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public abstract class TypeProvider {

	public abstract Type getInitialType(Type tp);
	public abstract Type getInitialSuperType(Type tp);
	public abstract Type getSuperType(Type tp);

	public abstract String toString(Type tp);
	public abstract Dumper toJava(Type tp, Dumper dmp);
	public abstract boolean checkResolved(Type tp);
	public abstract Type[] getDirectSuperTypes(Type tp);
	
	public abstract Type getJavaType(Type tp);
	public abstract JType getJType(Type tp);

	public boolean isArgument(Type tp)						{ return false; }
	public boolean isReference(Type tp)					{ return false; }
	public boolean isArray(Type tp)							{ return false; }
	public boolean isIntegerInCode(Type tp)				{ return false; }
	public boolean isInteger(Type tp)						{ return false; }
	public boolean isFloatInCode(Type tp)					{ return false; }
	public boolean isFloat(Type tp)							{ return false; }
	public boolean isNumber(Type tp)						{ return false; }
	public boolean isDoubleSize(Type tp)					{ return false; }
	public boolean isResolved(Type tp)						{ return false; }
	public boolean isBoolean(Type tp)						{ return false; }
	public boolean isCallable(Type tp)						{ return false; }
	public boolean isWrapper(Type tp)						{ return false; }

	public boolean isArgumented(Type tp)					{ return false; }

	public boolean isAnnotation(Type tp)					{ return false; }
	public boolean isAbstract(Type tp)						{ return false; }
	public boolean isEnum(Type tp)							{ return false; }
	public boolean isInterface(Type tp)					{ return false; }
	public boolean isClazz(Type tp)							{ return false; }
	public boolean isHasCases(Type tp)						{ return false; }
	public boolean isPizzaCase(Type tp)					{ return false; }
	public boolean isStaticClazz(Type tp)					{ return false; }
	public boolean isStruct(Type tp)						{ return false; }
	public boolean isAnonymouseClazz(Type tp)				{ return false; }
	public boolean isLocalClazz(Type tp)					{ return false; }
	public boolean isStructInstanceOf(Type tp, Struct s)	{ return false; }
}

public class BaseTypeProvider extends TypeProvider {
	final Struct clazz;
	
	final JBaseTypeProvider java_meta_type;

	BaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
		java_meta_type = new JBaseTypeProvider(clazz);
	}
	
	public boolean isReference(Type tp)					{ return true; }

	public boolean isArgumented(Type tp)					{ return (tp.flags & StdTypes.flArgumented)		!= 0 ; }

	public boolean isAnnotation(Type tp)					{ return clazz.isAnnotation(); }
	public boolean isAbstract(Type tp)						{ return clazz.isAbstract(); }
	public boolean isEnum(Type tp)							{ return clazz.isEnum(); }
	public boolean isInterface(Type tp)					{ return clazz.isInterface(); }
	public boolean isClazz(Type tp)							{ return clazz.isClazz(); }
	public boolean isHasCases(Type tp)						{ return clazz.isHasCases(); }
	public boolean isPizzaCase(Type tp)					{ return clazz.isPizzaCase(); }
	public boolean isStaticClazz(Type tp)					{ return clazz.isStatic(); }
	public boolean isAnonymouseClazz(Type tp)				{ return clazz.isAnonymouse(); }
	public boolean isLocalClazz(Type tp)					{ return clazz.isLocal(); }
	public boolean isStructInstanceOf(Type tp, Struct s)	{ return clazz.instanceOf(s); }
}

public class ArrayTypeProvider extends TypeProvider {
	static final ArrayTypeProvider instance = new ArrayTypeProvider();
	private ArrayTypeProvider() {}
	public boolean isReference(Type tp)					{ return true; }
	public boolean isArray(Type tp)							{ return true; }

	public boolean isStructInstanceOf(Type tp, Struct s)	{ return s == Type.tpObject; }
}

public class ArgumentTypeProvider extends TypeProvider {
	public boolean isArgument(Type tp)						{ return true; }
	public boolean isReference(Type tp)					{ return true; }
	public boolean isArgumented(Type tp)					{ return true; }
}

public class WrapperTypeProvider extends TypeProvider {
	static final WrapperTypeProvider instance = new WrapperTypeProvider();
	private WrapperTypeProvider() {}
	public boolean isReference(Type tp)					{ return true; }
	public boolean isWrapper(Type tp)						{ return true; }
}

public class ClosureTypeProvider extends TypeProvider {
	static final ClosureTypeProvider instance = new ClosureTypeProvider(null);
	final Struct clazz;
	ClosureTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
	public boolean isReference(Type tp)					{ return true; }
	public boolean isCallable(Type tp)						{ return true; }
}

public class MethodTypeProvider extends TypeProvider {
	public static final MethodTypeProvider instance = new MethodTypeProvider();
	MethodTypeProvider() {}
	public boolean isReference(Type tp)					{ return true; }
	public boolean isCallable(Type tp)						{ return true; }
}

public class MethodWithArgsTypeProvider extends MethodTypeProvider {
	final Method method;

	MethodWithArgsTypeProvider(Method method) {
		this.method = method;
	}
	
	public boolean isReference(Type tp)					{ return true; }
	public boolean isCallable(Type tp)						{ return true; }
}

public class JBaseTypeProvider extends TypeProvider {
	final Struct clazz;
	JBaseTypeProvider(Struct clazz) {
		this.clazz = clazz;
	}
	
	public boolean isReference(Type tp)					{ return true; }

	public boolean isArgumented(Type tp)					{ return (tp.flags & StdTypes.flArgumented)		!= 0 ; }

	public boolean isAnnotation(Type tp)					{ return clazz.isAnnotation(); }
	public boolean isAbstract(Type tp)						{ return clazz.isAbstract(); }
	public boolean isEnum(Type tp)							{ return clazz.isEnum(); }
	public boolean isInterface(Type tp)					{ return clazz.isInterface(); }
	public boolean isClazz(Type tp)							{ return clazz.isClazz(); }
	public boolean isHasCases(Type tp)						{ return clazz.isHasCases(); }
	public boolean isPizzaCase(Type tp)					{ return clazz.isPizzaCase(); }
	public boolean isStaticClazz(Type tp)					{ return clazz.isStatic(); }
	public boolean isAnonymouseClazz(Type tp)				{ return clazz.isAnonymouse(); }
	public boolean isLocalClazz(Type tp)					{ return clazz.isLocal(); }
	public boolean isStructInstanceOf(Type tp, Struct s)	{ return clazz.instanceOf(s); }
}

public class JMethodTypeProvider extends TypeProvider {
	public static final JMethodTypeProvider instance = new JMethodTypeProvider();
	JMethodTypeProvider() {}
	public boolean isReference(Type tp)					{ return true; }
	public boolean isCallable(Type tp)						{ return true; }
}


