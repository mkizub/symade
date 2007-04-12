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
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node(name="TypeExpr")
public class TypeExpr extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	public static final Hashtable<String,Struct>	AllNodes = new Hashtable<String,Struct>(256);

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
		this.ident = op.name;
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
			Struct s = AllNodes.get(arg.toString());
			if (s != null) {
				arg.lnk = s.xtype;
				this.lnk = new ASTNodeType(s);
				return this.lnk;
			}
		}
		Type tp = arg.getType();
		DNode@ v;
		if (op == Operator.PostTypeArray) {
			tp = new ArrayType(tp);
		}
		else if (op == Operator.PostTypeWrapper) {
			tp = new WrapperType((CompaundType)tp);
		}
		else if (op == Operator.PostTypeAST) {
			tp = new ASTNodeType(tp.getStruct());
		}
		else {
			Type t;
			ArgType a = null;
			if (!PassInfo.resolveNameR(((TypeExpr)this),v,new ResInfo(this,this.ident))) {
				if (op == Operator.PostTypePVar) {
					t = WrapperType.tpWrappedPrologVar;
					a = StdTypes.tpPrologVar.meta_type.tdecl.args[0].getAType();
				}
				else if (op == Operator.PostTypeVararg) {
					t = StdTypes.tpVararg;
				}
				else if (op == Operator.PostTypeSpace) {
					t = ((TypeDecl)Env.resolveGlobalDNode("kiev.vlang.NodeSpace")).xtype;
				}
				else if (op == Operator.PostTypeRef) {
					Kiev.reportWarning(this, "Typedef for "+op+" not found, assuming wrapper of "+Type.tpRefProxy);
					t = WrapperType.tpWrappedRefProxy;
					a = StdTypes.tpRefProxy.meta_type.tdecl.args[0].getAType();
				}
				else
					throw new CompilerException(this,"Typedef for type operator "+ident+" not found");
			} else {
				if (v instanceof TypeDecl)
					t = ((TypeDecl)v).getType();
				else
					throw new CompilerException(this,"Expected to find type for "+ident+", but found "+v);
			}
			t.checkResolved();
			TVarBld set = new TVarBld();
			if (a == null) {
				if (t.meta_type.tdecl.args.length != 1)
					throw new CompilerException(this,"Type '"+t+"' of type operator "+ident+" must have 1 argument");
				a = t.meta_type.tdecl.args[0].getAType();
			}
			set.append(a, tp);
			tp = t.applay(set);
		}
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
			if (op == Operator.PostTypePVar)
				return WrapperType.tpWrappedPrologVar.getStruct();
			else if (op == Operator.PostTypeRef)
				return WrapperType.tpWrappedRefProxy.getStruct();
			else if (op == Operator.PostTypeAST)
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
			if (op == Operator.PostTypePVar)
				return WrapperType.tpWrappedPrologVar.meta_type.tdecl;
			else if (op == Operator.PostTypeRef)
				return WrapperType.tpWrappedRefProxy.meta_type.tdecl;
			else if (op == Operator.PostTypeAST)
				return ASTNodeMetaType.instance(arg.getStruct()).tdecl;
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

