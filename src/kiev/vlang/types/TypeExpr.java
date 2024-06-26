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

	@nodeAttr           public TypeRef      arg;
	@AttrXMLDumpInfo(attr=true, name="op-name")
	@nodeAttr           public String       op_name;
	@AttrXMLDumpInfo(attr=true, name="base-type")
	@nodeAttr           public Type         base_type;

	public TypeExpr() {}

	public TypeExpr(Type arg, Operator op, Type tp) {
		this(new TypeRef(arg), op, tp);
	}

	public TypeExpr(TypeRef arg, Operator op, Type tp) {
		this.pos = arg.pos;
		this.arg = arg;
		this.op_name = op.name;
		this.type_lnk = tp;
	}

	@setter public void set$op_name(String val) {
		this.op_name = (val==null) ? null : val.intern();
	}

	public void callbackChanged(NodeChangeInfo info) {
		if (info.content_change) {
			if (info.slot.name == "arg" || info.slot.name == "op_name") {
				if (!this.isExptTypeSignature() && this.type_lnk != null)
					this.type_lnk = null;
			}
		}
		super.callbackChanged(info);
	}
	
	public void postVerify(Env env, INode parent, AttrSlot slot) {
		if (op_name == "T #" || getType(env) instanceof ASTNodeType) {
			this.replaceWithNode(new TypeASTNodeRef((ASTNodeType)getType(env)), parent, slot);
		}
	}
	
	public ENode[] getEArgs() { return new ENode[]{arg}; }

	public Opdef getFakeOpdef(Env env) {
		Opdef opd = null;
		try {
			ResInfo<Opdef> info = new ResInfo<Opdef>(env,this,op_name);
			SyntaxScope ss = Env.ctxSyntaxScope(this);
			if (ss != null) {
				foreach (ImportSyntax imp; ss.syntaxes) {
					KievSyntax stx = imp.name.dnode;
					if (stx != null && stx.resolveNameR(info))
						return info.resolvedDNode();
				}
			}
			KievSyntax kiev_stx = (KievSyntax) env.loadAnyDecl("kiev·Syntax");
			if (kiev_stx != null && kiev_stx.resolveNameR(info))
				return info.resolvedDNode();
		} catch (Throwable t) {}
		return null;
	}

	public Type getType(Env env) {
		if (this.type_lnk != null)
			return this.type_lnk;
		if (this.op_name == "T #") {
			Class cls = ASTNodeMetaType.allNodes.get(arg.toString());
			if (cls != null) {
				this.type_lnk = new ASTNodeType(cls);
				return this.type_lnk;
			}
			throw new CompilerException(this, "Cannot find ASTNodeType for name: "+arg.toString());
		}
		if (base_type == null) {
			ResInfo<TypeOpDef> info = new ResInfo<TypeOpDef>(env,this,this.op_name);
			if (PassInfo.resolveNameR((TypeExpr)this,info)) {
				TypeOpDef tod = info.resolvedDNode();
				base_type = tod.dtype.getType(env);
			}
			else
				throw new CompilerException(this,"Typedef for type operator '"+op_name+"' not found");
		}
		base_type.checkResolved();
		Type arg_type = arg.getType(env);
		TVarBld set = new TVarBld(base_type.tenv.tpTypeOpDefArg, arg_type);
		Type tp = base_type.applay(set);
		if (arg_type != arg_type.tenv.tpVoid)
			this.type_lnk = tp;
		return tp;
	}

	public Struct getStruct(Env env) {
		if (this.type_lnk != null)
			return this.type_lnk.getStruct();
		ResInfo info = new ResInfo(env,this,this.op_name);
		if (!PassInfo.resolveNameR(this,info)) {
			if (op_name == "T #")
				return arg.getStruct(env);
			else
				throw new CompilerException(this,"Typedef for type operator "+op_name+" not found");
		}
		if (info.resolvedDNode() instanceof TypeDecl)
			return ((TypeDecl)info.resolvedDNode()).getStruct();
		throw new CompilerException(this,"Expected to find type for "+op_name+", but found "+info.resolvedSymbol());
	}
	public TypeDecl getTypeDecl(Env env) {
		if (this.type_lnk != null)
			return this.type_lnk.meta_type.tdecl;
		ResInfo info = new ResInfo(env,this,this.op_name);
		if (!PassInfo.resolveNameR(this,info)) {
			if (op_name == "T #")
				return (TypeDecl)env.tenv.symbolTDeclASTNodeType.dnode;
			else
				throw new CompilerException(this,"Typedef for type operator "+op_name+" not found");
		}
		if (info.resolvedDNode() instanceof TypeDecl)
			return (TypeDecl)info.resolvedDNode();
		throw new CompilerException(this,"Expected to find type for "+op_name+", but found "+info.resolvedSymbol());
	}

	public String toString() {
		if (this.type_lnk != null)
			return this.type_lnk.toString();
		//if (op != null)
		//	return op.toString(this);
		return String.valueOf(arg)+this.op_name.substring(2);
	}
}

