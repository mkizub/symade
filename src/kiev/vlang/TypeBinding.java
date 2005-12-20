package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public class TypeBinding extends Type {
	public static TypeBinding[] emptyArray = new TypeBinding[0];
	
	public ArgumentType			arg;
	public TypeConstraint		cs;
	
	public TypeBinding(ArgumentType arg) {
		this(arg, new TypeUpperBoundConstraint(arg.super_type));
	}
	public TypeBinding(ArgumentType arg, TypeConstraint cs) {
		super(KString.from("A"+arg.name.bytecode_name+":<"+cs.signature+";"));
		this.arg = arg;
		this.cs = cs;
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = cs.getJType();
		return jtype;
	}

	public NodeName getName()						{ return cs.getName(); }
	public ClazzName getClazzName()					{ return cs.getClazzName(); }
	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return cs.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return cs.isHasCases(); }
	public boolean isPizzaCase()					{ return cs.isPizzaCase(); }
	public boolean isStaticClazz()					{ return cs.isStaticClazz(); }
	public boolean isStruct()						{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return cs.isStructInstanceOf(s); }
	public Type getSuperType()						{ return cs.getSuperType(); }
	public Type getInitialType()					{ return cs.getInitialType(); }
	public Type getInitialSuperType()				{ return cs.getInitialSuperType(); }
	public MetaSet getStructMeta()					{ return cs.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return cs.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { cs.resolveNameAccessR(node, info, name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { cs.resolveCallAccessR(node, info, name, mt) }
	
	public Type getJavaType() {
		return cs.getJavaType();
	}

	public boolean checkResolved() {
		return cs.checkResolved();
	}

	public String toString() {
		return cs.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return cs.toJava(dmp);
	}

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		return this.cs.isInstanceOf(t);
	}
}

public abstract class TypeConstraint {
	public final KString signature;
	
	public TypeConstraint(KString signature) {
		this.signature = signature;
	}
	
	public abstract JType getJType();
	public abstract NodeName getName();
	public abstract ClazzName getClazzName();
	public abstract boolean isArgument();
	public abstract boolean isAnnotation();
	public abstract boolean isAbstract();
	public abstract boolean isEnum();
	public abstract boolean isInterface();
	public abstract boolean isClazz();
	public abstract boolean isHasCases();
	public abstract boolean isPizzaCase();
	public abstract boolean isStaticClazz();
	public abstract boolean isStruct();
	public abstract boolean isAnonymouseClazz();
	public abstract boolean isLocalClazz();
	public abstract boolean isStructInstanceOf(Struct s);
	public abstract Type getSuperType();
	public abstract Type getInitialType();
	public abstract Type getInitialSuperType();
	public abstract MetaSet getStructMeta();
	public abstract Type[] getDirectSuperTypes();
	
	public abstract rule resolveStaticNameR(DNode@ node, ResInfo info, KString name);
	public abstract rule resolveNameAccessR(DNode@ node, ResInfo info, KString name);
	public abstract rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt);
	public abstract rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt);
	
	public abstract Type getJavaType();
	public abstract boolean checkResolved();
	public abstract String toString();
	public abstract Dumper toJava(Dumper dmp);
	public abstract boolean isInstanceOf(Type t);
}

//public class TypeAndConstraint extends TypeConstraint {
//	public TypeConstraint	c1;
//	public TypeConstraint	c2;
//}
//
//public class TypeOrConstraint extends TypeConstraint {
//	public TypeConstraint	c1;
//	public TypeConstraint	c2;
//}

public class TypeUpperBoundConstraint extends TypeConstraint {
	public Type		t;
	public TypeUpperBoundConstraint(Type t) {
		super(KString.from("~"+t.signature));
		this.t = t;
	}

	public JType getJType()							{ return t.getJType(); }
	public NodeName getName()						{ return t.getName(); }
	public ClazzName getClazzName()					{ return t.getClazzName(); }
	public boolean isArgument()						{ return true; }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return t.isAbstract(); }
	public boolean isEnum()							{ return false; }
	public boolean isInterface()					{ return false; }
	public boolean isClazz()						{ return false; }
	public boolean isHasCases()						{ return t.isHasCases(); }
	public boolean isPizzaCase()					{ return t.isPizzaCase(); }
	public boolean isStaticClazz()					{ return t.isStaticClazz(); }
	public boolean isStruct()						{ return false; }
	public boolean isAnonymouseClazz()				{ return false; }
	public boolean isLocalClazz()					{ return false; }
	public boolean isStructInstanceOf(Struct s)	{ return t.isStructInstanceOf(s); }
	public Type getSuperType()						{ return t; }
	public Type getInitialType()					{ return t.getInitialType(); }
	public Type getInitialSuperType()				{ return t.getInitialSuperType(); }
	public MetaSet getStructMeta()					{ return t.getStructMeta(); }
	public Type[] getDirectSuperTypes()			{ return t.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { t.resolveNameAccessR(node, info, name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { t.resolveCallAccessR(node, info, name, mt) }
	
	public Type getJavaType()						{ return t.getJavaType(); }
	public boolean checkResolved()					{ return t.checkResolved(); }
	public String toString()						{ return t.toString(); }
	public Dumper toJava(Dumper dmp)				{ return t.toJava(dmp); }
	public boolean isInstanceOf(Type t)			{ return this.t.isInstanceOf(t); }

}

//public class TypeLowerBoundConstraint extends TypeConstraint {
//	public Type		t;
//	public TypeLowerBoundConstraint(Type t) { this.t = t; }
//}
//
//public class TypeEqualBoundConstraint extends TypeConstraint {
//	public Type		t;
//	public TypeEqualBoundConstraint(Type t) { this.t = t; }
//}



