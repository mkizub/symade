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
package kiev.vlang.types;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public class TypeRef extends ENode {

	public static final TypeRef[] emptyArray = new TypeRef[0];
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	public TypeRef() {}
	
	public static TypeRef newTypeRef(Type tp)
		operator "new T"
	{
		if (tp instanceof CoreType)
			return new TypeNameRef(tp.name, tp);
		if (tp instanceof ASTNodeType) {
			String name = ((ASTNodeMetaType)tp.meta_type).name;
			if (name != null)
				return new TypeASTNodeRef(name, (ASTNodeType)tp);
			return new TypeASTNodeRef((ASTNodeType)tp);
		}
		if (tp instanceof ArgType)
			return new TypeArgRef((ArgType)tp);
		if (tp instanceof ArrayType)
			return new TypeExpr(newTypeRef(tp.arg), Operator.PostTypeArray, tp);
		if (tp instanceof WrapperType)
			return new TypeExpr(newTypeRef(tp.getEnclosedType()), Operator.PostTypeWrapper, tp);
		if (tp instanceof CompaundType || tp instanceof XType)
			return new TypeNameRef(tp);
		if (tp instanceof CallType)
			return new TypeClosureRef((CallType)tp);
		throw new RuntimeException("Unknow type for TypeRef: "+tp.getClass());
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (/* dump == "api" && */ (attr.name == "type_lnk" || attr.name == "ident")) {
			boolean use_type = true;
			Type t = type_lnk;
			if (t == null)
				use_type = false;
			else if (t instanceof ArgType)
				use_type = false;
			if (attr.name == "type_lnk")
				return use_type;
			return !use_type;
		}
		return super.includeInDump(dump, attr, val);
	}

	public Type castToType()
		alias $cast
		alias operator "( T ) V"
	{
		return type_lnk;
	}

	public Type getType(Env env) {
		return type_lnk;
	}
	
	public TypeRef closeBuild() { return this; }

	public boolean isArray() { return getType(Env.getEnv()).isArray(); }
	public void checkResolved(Env env) { getType(env).checkResolved(); } 
	public Struct getStruct(Env env) { if (type_lnk == null) return null; return type_lnk.getStruct(); }
	public TypeDecl getTypeDecl(Env env) { if (type_lnk == null) return null; return type_lnk.meta_type.tdecl; }

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		((TypeRef)this).getType(env); // calls resolving
		return false;
	}

	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) {
		((TypeRef)this).getType(env); // calls resolving
		return false;
	}

	public boolean equals(Object o) {
		if (o instanceof Type) return this.type_lnk ≡ (Type)o;
		return this == o;
	}
	
	public String toString() {
		return String.valueOf(type_lnk);
	}
	
}

/*
@ThisIsANode(lang=CoreLang)
public class TypeCoreRef extends TypeRef {
	public TypeCoreRef() {}
	public TypeCoreRef(CoreType tp) {
		this.type_lnk = tp;
	}
}
*/
@ThisIsANode(lang=CoreLang)
public class TypeArgRef extends TypeRef {
	public TypeArgRef() {}
	public TypeArgRef(ArgType tp) {
		this.type_lnk = tp;
	}
}

@ThisIsANode(lang=CoreLang)
public class TypeDeclRef extends TypeRef {
	public TypeDeclRef() {}

	public Type castToType()
		alias $cast
		alias operator "( T ) V"
	{
		if (this.type_lnk != null)
			return this.type_lnk;
		ANode p = parent();
		while (p != null && !(p instanceof Var))
			p = p.parent();
		return Env.getEnv().tenv.tpVoid;
	}

	public Type getType(Env env)
	{
		if (this.type_lnk != null)
			return this.type_lnk;
		ANode p = parent();
		while (p != null && !(p instanceof Var))
			p = p.parent();
		return env.tenv.tpVoid;
	}

	public String toString() {
		return String.valueOf(getType(Env.getEnv()));
	}
}

@ThisIsANode(lang=CoreLang)
public class TypeASTNodeRef extends TypeRef {

	@nodeAttr public final TypeDecl⇑	arg;

	public TypeASTNodeRef() {
		this.arg.qualified = true;
	}

	public TypeASTNodeRef(String arg, Type tp) {
		this.arg.name = arg;
		this.type_lnk = tp;
	}

	public TypeASTNodeRef(ASTNodeType tp) {
		this.arg.name = tp.meta_type.clazz.getName().replace('·','.');
		this.arg.qualified = true;
		this.type_lnk = tp;
	}

	public Type getType(Env env) {
		if (this.type_lnk != null)
			return this.type_lnk;
		Class cls = ASTNodeMetaType.allNodes.get(arg.name);
		if (cls == null) {
			try {
				cls = Class.forName(arg.name.replace('·','.'));
			} catch (Exception e) {}
		}
		if (cls != null) {
			this.type_lnk = new ASTNodeType(cls);
			return this.type_lnk;
		}
		throw new CompilerException(this, "Cannot find ASTNodeType for name: "+arg.name);
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "type_lnk" || attr.name == "ident")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public Struct getStruct(Env env) {
		return null;
	}
	public TypeDecl getTypeDecl(Env env) {
		return (TypeDecl)env.tenv.symbolTDeclASTNodeType.dnode;
	}

	public String toString() {
		return arg.name+"#";
	}
}

