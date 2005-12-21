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
		this(arg, new TypeConstraint.Upper(arg.super_type));
	}
	public TypeBinding(ArgumentType arg, TypeConstraint cs) {
		super(KString.from("A"+arg.name.bytecode_name+cs.signature+";"));
		this.arg = arg;
		this.cs = cs;
	}

	public JType getJType() {
		assert(Kiev.passGreaterEquals(TopLevelPass.passPreGenerate));
		if (jtype == null)
			jtype = cs.getJType();
		return jtype;
	}

	public boolean isArgument()						{ return cs.isArgument(); }
	public boolean isAnnotation()					{ return false; }
	public boolean isAbstract()						{ return cs.isAbstract(); }
	public boolean isEnum()							{ return cs.isEnum(); }
	public boolean isInterface()					{ return cs.isInterface(); }
	public boolean isClazz()						{ return cs.isClazz(); }
	public Type getSuperType()						{ return cs.getSuperType(); }
	public Type getTemplateType()					{ return cs.getTemplateType(); }
	public Type[] getDirectSuperTypes()			{ return cs.getDirectSuperTypes(); }
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) { cs.resolveNameAccessR(node, info, name) }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) { cs.resolveCallAccessR(node, info, name, mt) }
	
	public Type getErasedType() { return cs.getErasedType(); }
	public boolean checkResolved() { return cs.checkResolved(); }
	public String toString() { return cs.toString(); }
	public Dumper toJava(Dumper dmp) { return cs.toJava(dmp); }

	public boolean isInstanceOf(Type t) {
		if (this == t) return true;
		return this.cs.isInstanceOf(t);
	}
	
	public TypeBinding applay(TypeBinding tb) {
		assert(this.arg == tb.arg);
		return new TypeBinding(this.arg, this.cs.applay(tb.cs));
	}
}

public abstract class TypeConstraint {
	public final KString signature;
	
	public case Upper(Type t);
	public case Equal(Type t);
	
	public TypeConstraint() {
		switch(this) {
		case Upper(Type t):
			this.signature = KString.from(":<"+t.signature);
			break;
		case Equal(Type t):
			this.signature = KString.from(":="+t.signature);
			break;
		}
	}
	
	public JType getJType()	 {
		switch(this) {
		case Upper(Type t): return t.getJType();
		case Equal(Type t): return t.getJType();
		}
	}
	public boolean isArgument() {
		switch(this) {
		case Upper(Type t): return t.isArgument();
		case Equal(Type t): return t.isArgument();
		}
	}
	public boolean isAnnotation() {
		return false;
	}
	public boolean isAbstract() {
		switch(this) {
		case Upper(Type t): return t.isAbstract();
		case Equal(Type t): return t.isAbstract();
		}
	}
	public boolean isEnum() {
		switch(this) {
		case Upper(Type t): return t.isEnum();
		case Equal(Type t): return t.isEnum();
		}
	}
	public boolean isInterface() {
		switch(this) {
		case Upper(Type t): return t.isInterface();
		case Equal(Type t): return t.isInterface();
		}
	}
	public boolean isClazz() {
		switch(this) {
		case Upper(Type t): return t.isClazz();
		case Equal(Type t): return t.isClazz();
		}
	}
	public Type getSuperType() {
		switch(this) {
		case Upper(Type t): return t;
		case Equal(Type t): return t;
		}
	}
	public Type getTemplateType() {
		switch(this) {
		case Upper(Type t): return t.getTemplateType();
		case Equal(Type t): return t.getTemplateType();
		}
	}
	public Type[] getDirectSuperTypes() {
		switch(this) {
		case Upper(Type t): return t.getDirectSuperTypes();
		case Equal(Type t): return t.getDirectSuperTypes();
		}
	}
	public Type getErasedType() {
		switch(this) {
		case Upper(Type t): return t.getErasedType();
		case Equal(Type t): return t.getErasedType();
		}
	}
	public String toString() {
		switch(this) {
		case Upper(Type t): return t.toString();
		case Equal(Type t): return t.toString();
		}
	}
	public Dumper toJava(Dumper dmp) {
		switch(this) {
		case Upper(Type t): return t.toJava(dmp);
		case Equal(Type t): return t.toJava(dmp);
		}
	}
	public boolean isInstanceOf(Type tp) {
		switch(this) {
		case Upper(Type t): return t.isInstanceOf(tp);
		case Equal(Type t): return t.isInstanceOf(tp);
		}
	}
	public boolean checkResolved() {
		switch(this) {
		case Upper(Type t): return t.checkResolved();
		case Equal(Type t): return t.checkResolved();
		}
	}
	
	public rule resolveStaticNameR(DNode@ node, ResInfo info, KString name) { false }
	public rule resolveCallStaticR(DNode@ node, ResInfo info, KString name, MethodType mt) { false }
	public rule resolveNameAccessR(DNode@ node, ResInfo info, KString name) {
		this instanceof Upper, ((Upper)this).t.resolveNameAccessR(node, info, name)
	;	this instanceof Equal, ((Equal)this).t.resolveNameAccessR(node, info, name)
	}
	public rule resolveCallAccessR(DNode@ node, ResInfo info, KString name, MethodType mt) {
		this instanceof Upper, ((Upper)this).t.resolveCallAccessR(node, info, name, mt)
	;	this instanceof Equal, ((Equal)this).t.resolveCallAccessR(node, info, name, mt)
	}
	
	TypeConstraint applay(TypeConstraint tc) {
		switch (this) {
		case Upper(Type t1):
			{
				switch(tc) {
				case Upper(Type t2):
					assert(t1.isInstanceOf(t2));
					return this;
				case Equal(Type t2):
					assert(t1.isInstanceOf(t2));
					return this;
				}
			}
		case Equal(Type t1):
			{
				switch(tc) {
				case Upper(Type t2):
					assert(t1.isInstanceOf(t2));
					return this;
				case Equal(Type t2):
					assert(t1.isInstanceOf(t2));
					return this;
				}
			}
		}
	}
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

//public class TypeLowerBoundConstraint extends TypeConstraint {
//	public Type		t;
//	public TypeLowerBoundConstraint(Type t) { this.t = t; }
//}


