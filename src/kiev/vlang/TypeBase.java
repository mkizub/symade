package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JType;
import kiev.be.java.JBaseType;
import kiev.be.java.JArrayType;
import kiev.be.java.JStructView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public class BaseType extends Type {
	public static BaseType[]	emptyArray = new BaseType[0];

	public final access:ro,ro,ro,rw Struct		clazz;	
	
	BaseType(KString signature, Struct clazz, TypeBinding[] bindings) {
		super(signature,bindings);
		this.clazz = clazz;
		assert(clazz != null);
	}
	
	public static BaseType newBaseType(Struct clazz) alias operator(240,lfy,new)
	{ return newBaseType(Signature.from(clazz,null,null,null), clazz, TypeBinding.emptyArray); }

	public static BaseType newBaseType(Struct clazz, TypeBinding[] bindings) alias operator(240,lfy,new)
	{ return newBaseType(Signature.from(clazz,null,null,null), clazz, bindings); }
	
	public static BaseType newBaseType(KString signature, Struct clazz) alias operator(240,lfy,new)
	{ return newBaseType(signature, clazz, TypeBinding.emptyArray); }
	
	public static BaseType newBaseType(KString signature, Struct clazz, TypeBinding[] bindings) alias operator(240,lfy,new)
	{
		if (clazz.isAnnotation())
			return new AnnotationType(signature, clazz);
		if (clazz.isEnum())
			return new EnumType(signature, clazz);
		return new BaseType(signature, clazz, bindings);
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
		return BaseType.newRefType(this.clazz,tb);
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
		super(signature,clazz,TypeBinding.emptyArray);
		assert(clazz.isAnnotation());
	}
	
	public boolean isAnnotation()			{ return true; }
	public boolean isAbstract()				{ return false; }
	public boolean isEnum()					{ return false; }
	public boolean isInterface()			{ return true; }
	public boolean isClazz()				{ return false; }

	public KString get$aname() { return clazz.name.name; }
	
	public boolean isInstanceOf(Type t2) {
		while (t2 instanceof ArgumentType)
			t2 = t2.getSuperType();
		return t2 == Type.tpObject || t2 == Type.tpAnnotation || t2 == this;
	}

	public Type[] getDirectSuperTypes() { return new Type[]{Type.tpObject,Type.tpAnnotation}; }
}

public class EnumType extends BaseType {

	EnumType(KString signature, Struct clazz) {
		super(signature,clazz,TypeBinding.emptyArray);
		assert(clazz.isEnum());
	}
	
	public boolean isAnnotation()			{ return false; }
	public boolean isAbstract()				{ return false; }
	public boolean isEnum()					{ return true; }
	public boolean isInterface()			{ return false; }
	public boolean isClazz()				{ return true; }
}


