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

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import kiev.transf.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JENode;
import kiev.be.java15.JTypeRef;
import kiev.be.java15.JType;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeRef extends ENode {

	public static final TypeRef[] emptyArray = new TypeRef[0];
	public static final TypeRef dummyNode = new TypeRef();
	
	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ TypeRef;
	@virtual typedef JView = JTypeRef;

	@ref public Type	lnk;

	public TypeRef() {}
	
	private TypeRef(CoreType tp) {
		this.ident = tp.name;
		this.symbol = tp.meta_type.tdecl;
		this.lnk = tp;
	}
	
	private TypeRef(ArgType tp) {
		this.ident = tp.name;
		this.symbol = tp.meta_type.tdecl;
		this.lnk = tp;
	}
	
	public static TypeRef newTypeRef(Type tp)
		alias lfy operator new
	{
		if (tp instanceof CoreType)
			return new TypeRef((CoreType)tp);
		if (tp instanceof ASTNodeType)
			return new TypeExpr(newTypeRef(tp.getStruct().xtype),Operator.PostTypeAST);
		if (tp instanceof ArgType)
			return new TypeRef((ArgType)tp);
		if (tp instanceof ArrayType)
			return new TypeExpr(newTypeRef(tp.arg), Operator.PostTypeArray);
		if (tp instanceof WrapperType)
			return new TypeExpr(newTypeRef(tp.getEnclosedType()), Operator.PostTypeWrapper);
		if (tp instanceof CompaundType || tp instanceof XType)
			return new TypeNameRef(tp);
		if (tp instanceof CallType)
			return new TypeClosureRef((CallType)tp);
		throw new RuntimeException("Unknow type for TypeRef: "+tp.getClass());
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "lnk") {
			Type t = lnk;
			if (lnk == null)
				return false;
			if (lnk instanceof ArgType)
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
		return lnk;
	}
	
	public boolean isArray() { return getType().isArray(); }
	public boolean checkResolved() { return getType().checkResolved(); } 
	public Struct getStruct() { if (lnk == null) return null; return lnk.getStruct(); }
	public TypeDecl getTypeDecl() { if (lnk == null) return null; return lnk.meta_type.tdecl; }
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
		if (o instanceof Type) return this.lnk ≡ (Type)o;
		return this == o;
	}
	
	public String toString() {
		return String.valueOf(lnk);
	}
	
	public void toExpr(Type reqType) {
		Type st = getType();
		Struct s = st.getStruct();
		if (s != null && s.isPizzaCase()) {
			// Pizza case may be casted to int or to itself or super-class
			PizzaCase pcase = (PizzaCase)s.variant;
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(this,"Pizza case "+tp+" cannot be casted to type "+reqType);
			if (pcase.group.decls.length != 0)
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
		if (s != null && s.isSingleton()) {
			replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			return;
		}
		throw new CompilerException(this,"Type "+this+" is not a singleton");
	}
}

@node
public class TypeDeclRef extends TypeRef {
	@virtual typedef This  = TypeDeclRef;
	
	public TypeDeclRef() {}

	public Type getType()
		alias fy operator $cast
	{
		if (this.lnk != null)
			return this.lnk;
		for (ANode p = parent(); p!= null; p = p.parent()) {
			if (p instanceof DeclGroup) {
				this.lnk = p.getType();
				return this.lnk;
			}
		}
		return Type.tpVoid;
	}

	public String toString() {
		return String.valueOf(getType());
	}
}

