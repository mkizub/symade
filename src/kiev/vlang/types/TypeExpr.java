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

@ThisIsANode(name="TypeExpr", lang=CoreLang)
public class TypeExpr extends TypeRef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	private Object op_or_name;
	
	@nodeAttr           public TypeRef      arg;
	@nodeAttr @abstract public Operator     op;
	@nodeAttr @abstract public String       op_name;

	public TypeExpr() {}

	public TypeExpr(Type arg, Operator op) {
		this(new TypeRef(arg), op);
	}

	public TypeExpr(TypeRef arg, Operator op) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
	}

	public TypeExpr(TypeRef arg, Operator op, Type tp) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op = op;
		this.type_lnk = tp;
	}

	public TypeExpr(TypeRef arg, Token op) {
		this.arg = arg;
		if (op.kind == ParserConstants.OPERATOR_LRBRACKETS) {
			this.op = Operator.PostTypeArray;
		} else {
			this.op_name = ("T "+op.image).intern();
		}
		this.pos = op.getPos();
	}
	
	@getter public Operator get$op() {
		if (op_or_name instanceof Operator)
			return (Operator)op_or_name;
		return null;
	}

	@getter public String get$op_name() {
		if (op_or_name instanceof Operator)
			return ((Operator)op_or_name).name;
		return (String)op_or_name;
	}

	@setter public void set$op(Operator val) {
		op_or_name = val;
	}

	@setter public void set$op_name(String val) {
		if (val != null)
			val = val.intern();
		op_or_name = val;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "op")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "arg" || attr.name == "op" || attr.name == "op_name") {
			if (!this.is_expr_id_signature && this.type_lnk != null)
				this.type_lnk = null;
		}
		super.callbackChildChanged(ct, attr, data);
	}
	
	public Operator getOp() { return op; }
	public void setOp(Operator op) {
		if (!op.name.startsWith("T "))
			throw new RuntimeException("Cannot set operator "+op+" in ENode "+getClass());
		if (!this.is_expr_id_signature)
			this.type_lnk = null;
		this.op = op;
	}

	public ENode[] getArgs() { return new ENode[]{arg}; }

	public Type getType() {
		if (this.type_lnk != null)
			return this.type_lnk;
		if (this.op == null) {
			Operator op = Operator.getOperatorByName(this.op_name);
			if (op == null)
				op = Operator.getOperatorByDecl(this.op_name);
			if (op == null)
				throw new CompilerException(this, "Cannot find type operator: "+this.op_name);
			this.op = op;
		}
		if (op == Operator.PostTypeAST) {
			Class cls = ASTNodeMetaType.allNodes.get(arg.toString());
			if (cls != null) {
				this.type_lnk = new ASTNodeType(cls);
				return this.type_lnk;
			}
			throw new CompilerException(this, "Cannot find ASTNodeType for name: "+arg.toString());
		}
		TypeOpDef@ tod;
		Type t;
		ArgType a;
		if (PassInfo.resolveNameR(((TypeExpr)this),tod,new ResInfo(this,this.op_name))) {
			t = tod.dtype.getType();
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
			throw new CompilerException(this,"Typedef for type operator '"+op_name+"' not found");
		t.checkResolved();
		Type arg_type = arg.getType();
		TVarBld set = new TVarBld(a, arg_type);
		Type tp = t.applay(set);
		if (arg_type != StdTypes.tpVoid)
			this.type_lnk = tp;
		return tp;
	}

	public Struct getStruct() {
		if (this.type_lnk != null)
			return this.type_lnk.getStruct();
		if (this.op_name == Operator.PostTypeArray.name || this.op_name == Operator.PostTypeVararg.name)
			return null;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,this.op_name))) {
			if (op == Operator.PostTypeAST)
				return arg.getStruct();
			else
				throw new CompilerException(this,"Typedef for type operator "+op_name+" not found");
		}
		if (v instanceof TypeDecl)
			return ((TypeDecl)v).getStruct();
		throw new CompilerException(this,"Expected to find type for "+op_name+", but found "+v);
	}
	public TypeDecl getTypeDecl() {
		if (this.type_lnk != null)
			return this.type_lnk.meta_type.tdecl;
		if (this.op_name == Operator.PostTypeArray.name)
			return ArrayMetaType.instance.tdecl;
		if (this.op_name == Operator.PostTypeVararg.name)
			return StdTypes.tpVararg.meta_type.tdecl;
		DNode@ v;
		if (!PassInfo.resolveNameR(this,v,new ResInfo(this,this.op_name))) {
			if (op == Operator.PostTypeAST)
				return StdTypes.tdASTNodeType;
			else
				throw new CompilerException(this,"Typedef for type operator "+op_name+" not found");
		}
		if (v instanceof TypeDecl)
			return (TypeDecl)v;
		throw new CompilerException(this,"Expected to find type for "+op_name+", but found "+v);
	}

	public String toString() {
		if (this.type_lnk != null)
			return this.type_lnk.toString();
		if (op != null)
			return op.toString(this);
		return String.valueOf(arg)+this.op_name.substring(2);
	}
}

