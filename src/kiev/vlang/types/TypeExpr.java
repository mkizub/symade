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

@node(name="TypeExpr")
public class TypeExpr extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeExpr;

	@att public TypeRef			arg;
	@att public Operator		op;

	public TypeExpr() {}

	public TypeExpr(Type arg, Operator op) {
		this(new TypeRef(arg), op);
	}

	public TypeExpr(TypeRef arg, Operator op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
		this.ident = op.name;
	}

	public TypeExpr(TypeRef arg, Operator op, Type lnk) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
		this.ident = op.name;
		this.lnk = lnk;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS) {
			this.op = Operator.PostTypeArray;
			this.ident = this.op.name;
		} else {
			this.ident = ("T "+op.image).intern();
		}
		this.pos = op.getPos();
	}

	public Operator getOp() { return op; }

	public ENode[] getArgs() { return new ENode[]{arg}; }

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		this = this.open();
		if (this.op == null) {
			Operator op = Operator.getOperatorByName(this.ident);
			if (op == null)
				op = Operator.getOperatorByDecl(this.ident);
			if (op == null)
				throw new CompilerException(this, "Cannot find type operator: "+this.ident);
			this.op = op;
		}
		if (op == Operator.PostTypeAST) {
			Class cls = ASTNodeMetaType.allNodes.get(arg.toString());
			if (cls != null) {
				this.lnk = new ASTNodeType(cls);
				return this.lnk;
			}
			throw new CompilerException(this, "Cannot find ASTNodeType for name: "+arg.toString());
		}
		TypeOpDef@ tod;
		Type t;
		ArgType a;
		if (PassInfo.resolveNameR(((TypeExpr)this),tod,new ResInfo(this,this.ident))) {
			t = tod.type.getType();
			a = tod.arg.getAType();
		}
		else if (op == Operator.PostTypeArray) {
			t = StdTypes.tpArray;
			a = StdTypes.tpArrayArg;
		}
		else if (op == Operator.PostTypeVararg) {
			t = StdTypes.tpVararg;
			a = StdTypes.tpVarargArg;
		}
		else
			throw new CompilerException(this,"Typedef for type operator '"+ident+"' not found");
		t.checkResolved();
		TVarBld set = new TVarBld(a, arg.getType());
		Type tp = t.applay(set);
		this.lnk = tp;
		return tp;
	}

	public Struct getStruct() {
		if (this.lnk != null)
			return this.lnk.getStruct();
		if (this.ident == Operator.PostTypeArray.name || this.ident == Operator.PostTypeVararg.name)
			return null;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,this.ident))) {
			if (op == Operator.PostTypeAST)
				return arg.getStruct();
			else
				throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
		}
		if (v instanceof TypeDecl)
			return ((TypeDecl)v).getStruct();
		throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
	}
	public TypeDecl getTypeDecl() {
		if (this.lnk != null)
			return this.lnk.meta_type.tdecl;
		if (this.ident == Operator.PostTypeArray.name)
			return ArrayMetaType.instance.tdecl;
		if (this.ident == Operator.PostTypeVararg.name)
			return StdTypes.tpVararg.meta_type.tdecl;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,this.ident))) {
			if (op == Operator.PostTypeAST)
				return StdTypes.tdASTNodeType;
			else
				throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
		}
		if (v instanceof TypeDecl)
			return (TypeDecl)v;
		throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
	}

	public String toString() {
		if (this.lnk != null)
			return this.lnk.toString();
		if (op != null)
			return op.toString(this);
		return String.valueOf(arg)+this.ident.substring(2);
	}
}

