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

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.be.java15.JTypeRef;
import kiev.be.java15.JType;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public class TypeRef extends ENode {

	public static final TypeRef[] emptyArray = new TypeRef[0];
	public static final TypeRef dummyNode = new TypeRef();
	
	@DataFlowDefinition(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ TypeRef;
	@virtual typedef JView = JTypeRef;
	
	@nodeAttr public String signature;

	public TypeRef() {}
	
	private TypeRef(CoreType tp) {
		this.type_lnk = tp;
	}
	
	private TypeRef(ArgType tp) {
		this.type_lnk = tp;
	}
	
	private TypeRef(ASTNodeType tp) {
		this.type_lnk = tp;
	}
	
	public static TypeRef newTypeRef(Type tp)
		alias lfy operator new
	{
		if (tp instanceof CoreType)
			return new TypeNameRef(tp.name, tp);
		if (tp instanceof ASTNodeType) {
			String name = ((ASTNodeMetaType)tp.meta_type).name;
			if (name != null)
				return new TypeExpr(new TypeNameRef(name),Operator.PostTypeAST,tp);
			return new TypeRef((ASTNodeType)tp);
			//return new TypeExpr(newTypeRef(tp.getStruct().xtype),Operator.PostTypeAST);
		}
		if (tp instanceof ArgType)
			return new TypeRef((ArgType)tp);
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
		if (dump == "api" && attr.name == "type_lnk") {
			Type t = type_lnk;
			if (t == null)
				return false;
			if (t instanceof ArgType)
				return false;
			if (t instanceof ASTNodeType)
				return false;
			return true;
		}
		return super.includeInDump(dump, attr, val);
	}

	public ASTNode getDummyNode() {
		return TypeRef.dummyNode;
	}

	public Type getType()
		alias fy operator $cast
	{
		return type_lnk;
	}
	
	public boolean isArray() { return getType().isArray(); }
	public boolean checkResolved() { return getType().checkResolved(); } 
	public Struct getStruct() { if (type_lnk == null) return null; return type_lnk.getStruct(); }
	public TypeDecl getTypeDecl() { if (type_lnk == null) return null; return type_lnk.meta_type.tdecl; }
	public JType getJType() { return getType().getJType(); }

	public boolean preResolveIn() {
		((TypeRef)this).getType(); // calls resolving
		return false;
	}

	public boolean mainResolveIn() {
		((TypeRef)this).getType(); // calls resolving
		return false;
	}

	public void resolve(Type reqType) {
		if (reqType ≢ null && reqType ≉ Type.tpClass)
			toExpr(reqType);
		else
			getType(); // calls resolving
	}
	
	public boolean equals(Object o) {
		if (o instanceof Type) return this.type_lnk ≡ (Type)o;
		return this == o;
	}
	
	public String toString() {
		return String.valueOf(type_lnk);
	}
	
	public void toExpr(Type reqType) {
		Type st = getType();
		TypeDecl s = st.meta_type.tdecl;
		if (s.isPizzaCase()) {
			// Pizza case may be casted to int or to itself or super-class
			PizzaCase pcase = (PizzaCase)s;
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(this,"Pizza case "+tp+" cannot be casted to type "+reqType);
			if (pcase.case_fields.length != 0)
				throw new CompilerException(this,"Empty constructor for pizza case "+tp+" not found");
			if (reqType.isInteger()) {
				ENode expr = new ConstIntExpr(pcase.tag);
				if( reqType ≢ Type.tpInt )
					expr = new CastExpr(pos,reqType,expr);
				replaceWithNodeResolve(reqType, expr);
			}
			else if (s.isSingleton()) {
				replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			}
			else {
				replaceWithResolve(reqType, fun ()->ENode {return new NewExpr(pos,tp,ENode.emptyArray);});
			}
			return;
		}
		if (s.isSingleton()) {
			replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			return;
		}
		throw new CompilerException(this,"Type "+this+" is not a singleton");
	}
}

@ThisIsANode(lang=CoreLang)
public class TypeDeclRef extends TypeRef {
	@virtual typedef This  = TypeDeclRef;
	
	public TypeDeclRef() {}

	public Type getType()
		alias fy operator $cast
	{
		if (this.type_lnk != null)
			return this.type_lnk;
		ANode p = parent();
		while (p != null && !(p instanceof Var))
			p = p.parent();
		if (p instanceof Var) {
			Var v = (Var)p;
			if (v.group != null) {
				this.type_lnk = v.group.getType();
				return this.type_lnk;
			}
		}
		return Type.tpVoid;
	}

	public String toString() {
		return String.valueOf(getType());
	}
}

