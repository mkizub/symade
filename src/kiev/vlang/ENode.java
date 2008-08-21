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
package kiev.vlang;
import syntax kiev.Syntax;

import kiev.ir.java15.RENode;

/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@ThisIsANode(lang=CoreLang)
public abstract class ENode extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}
	
	final static class NameAndUUID {
		final String name;
		final String uuid;
		NameAndUUID(String name, String uuid) {
			this.name = name.intern();
			this.uuid = uuid.intern();
		}
	}
	
	final static class TypeSignature {
		final String name;
		final String sign;
		TypeSignature(String sign) {
			this.name = AType.getNameFromSignature(sign).intern();
			this.sign = sign;
		}
	}
	
	private Object	ident_or_symbol_or_type;
	
	@AttrXMLDumpInfo(attr=true)
	@nodeAttr @abstract public String			ident;
	@AttrXMLDumpInfo(attr=true, name="full")
	@nodeAttr @abstract public boolean			qualified; // stored ident may be qualified name
	@AttrXMLDumpInfo(attr=true, name="primary")
	@nodeAttr @abstract public boolean			primary_expr; // a primary expression; i.e. in parenthethis
	@AttrXMLDumpInfo(attr=true, name="super")
	@nodeAttr @abstract public boolean			super_expr; // a super-expression; i.e. super.something
	@nodeData @abstract public Symbol			symbol;
	@AttrXMLDumpInfo(attr=true, name="type")
	@nodeData @abstract public Type			type_lnk;
	@nodeData @abstract public:ro DNode		dnode;

	public void setNameAndUUID(String name, String uuid) {
		this.ident_or_symbol_or_type = new NameAndUUID(name, uuid);
		//this.qualified = true;
	}

	public void setTypeSignature(String signature) {
		this.ident_or_symbol_or_type = new TypeSignature(signature);
		//this.qualified = true;
	}
	
	public boolean isExptTypeSignature() {
		return this.ident_or_symbol_or_type instanceof TypeSignature;
	}
	
	@getter public final String get$ident() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof String)
			return (String)id;
		if (id instanceof Symbol) {
			if (qualified)
				return ((Symbol)id).qname();
			return ((Symbol)id).sname;
		}
		if (id instanceof Type) {
			if (qualified)
				return ((Type)id).meta_type.qname();
			return ((Type)id).meta_type.tdecl.sname;
		}
		if (id instanceof NameAndUUID) {
			return ((NameAndUUID)id).name;
		}
		if (id instanceof TypeSignature) {
			return ((TypeSignature)id).name;
		}
		return null;
	}

	@getter public final Symbol get$symbol() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Symbol)
			return (Symbol)id;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl.symbol;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getRoot().getSymbolByUUID(nid.uuid);
			if (sym != null) {
				this.symbol = sym;
				return sym;
			}
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(id.sign,true);
			this.type_lnk = tp;
			return tp.meta_type.tdecl.symbol;
		}
		return null;
	}
	
	@getter public final DNode get$dnode() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Symbol)
			return ((Symbol)id).dnode;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getRoot().getSymbolByUUID(nid.uuid);
			if (sym != null) {
				this.symbol = sym;
				return sym.dnode;
			}
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(id.sign,true);
			this.type_lnk = tp;
			return tp.meta_type.tdecl;;
		}
		return null;
	}
	
	@getter public final Type get$type_lnk() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Type) {
			Type tp = (Type)id;
			tp.bindings();
			return tp;
		}
		if (id instanceof TypeSignature) {
			Type tp = AType.fromSignature(id.sign,true);
			this.type_lnk = tp;
			return tp;
		}
		return null;
	}
	
	@setter public final void set$ident(String val) {
		if (val != null) {
			val = val.intern();
			if (val.indexOf('·') >= 0)
				qualified = true;
		}
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$symbol(Symbol val) {
		//assert (!(this instanceof TypeRef) || (this instanceof TypeNameRef));
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$type_lnk(Type val) {
		assert (this instanceof TypeRef);
		ident_or_symbol_or_type = val;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "qualified")
			return this.qualified; // do not dump <qualified>false</qualified>
		if (attr.name == "primary_expr")
			return this.primary_expr; // do not dump <primary_expr>false</primary_expr>
		if (attr.name == "super_expr")
			return this.super_expr; // do not dump <super_expr>false</super_expr>
		return super.includeInDump(dump, attr, val);
	}

	@getter public final boolean get$qualified() { is_qualified }
	@setter public final void set$qualified(boolean val) { is_qualified = val; }
	@getter public final boolean get$primary_expr() { is_expr_primary }
	@setter public final void set$primary_expr(boolean val) { is_expr_primary = val; }
	@getter public final boolean get$super_expr() { is_expr_super }
	@setter public final void set$super_expr(boolean val) { is_expr_super = val; }

	//
	// Expr specific
	//

	// use as field (disable setter/getter calls for virtual fields)
	public final boolean isAsField() {
		return this.is_expr_as_field;
	}
	public final void setAsField(boolean on) {
		if (this.is_expr_as_field != on) {
			this.is_expr_as_field = on;
		}
	}
	// expression will generate void value
	public final boolean isGenVoidExpr() {
		return this.is_expr_gen_void;
	}
	public final void setGenVoidExpr(boolean on) {
		if (this.is_expr_gen_void != on) {
			this.is_expr_gen_void = on;
		}
	}
	// used bt for()
	public final boolean isForWrapper() {
		return this.is_expr_for_wrapper;
	}
	public final void setForWrapper(boolean on) {
		if (this.is_expr_for_wrapper != on) {
			this.is_expr_for_wrapper = on;
		}
	}
	// used for primary expressions, i.e. (a+b)
	public final boolean isPrimaryExpr() {
		return this.is_expr_primary;
	}
	public final void setPrimaryExpr(boolean on) {
		if (this.is_expr_primary != on) {
			this.is_expr_primary = on;
		}
	}
	// used for super-expressions, i.e. (super.foo or super.foo())
	public final boolean isSuperExpr() {
		return this.is_expr_super;
	}
	public final void setSuperExpr(boolean on) {
		if (this.is_expr_super != on) {
			this.is_expr_super = on;
		}
	}
	// used for cast calls (to check for null)
	public final boolean isCastCall() {
		return this.is_expr_cast_call;
	}
	public final void setCastCall(boolean on) {
		if (this.is_expr_cast_call != on) {
			this.is_expr_cast_call = on;
		}
	}


	//
	// Statement specific flags
	//
	
	// abrupted
	public final boolean isAbrupted() {
		return this.is_stat_abrupted;
	}
	public final void setAbrupted(boolean on) {
		if (this.is_stat_abrupted != on) {
			this.is_stat_abrupted = on;
		}
	}
	// breaked
	public final boolean isBreaked() {
		return this.is_stat_breaked;
	}
	public final void setBreaked(boolean on) {
		if (this.is_stat_breaked != on) {
			this.is_stat_breaked = on;
		}
	}
	// method-abrupted
	public final boolean isMethodAbrupted() {
		return this.is_stat_method_abrupted;
	}
	public final void setMethodAbrupted(boolean on) {
		if (this.is_stat_method_abrupted != on) {
			this.is_stat_method_abrupted = on;
			if (on) this.is_stat_abrupted = true;
		}
	}
	// auto-returnable
	public final boolean isAutoReturnable() {
		return this.is_stat_auto_returnable;
	}
	public final void setAutoReturnable(boolean on) {
		if (this.is_stat_auto_returnable != on) {
			this.is_stat_auto_returnable = on;
		}
	}
	// reachable by direct control flow, with no jumps into
	public final boolean isDirectFlowReachable() {
		return this.is_direct_flow_reachable;
	}
	public final void setDirectFlowReachable(boolean on) {
		if (this.is_direct_flow_reachable != on) {
			this.is_direct_flow_reachable = on;
		}
	}

	private static void do_resolve(Type reqType, ASTNode node) {
		try {
			Kiev.runProcessorsOn(node);
		} catch (ReWalkNodeException e) {
			do_resolve(reqType, (ASTNode)e.replacer);
			return;
		}
		((ENode)node).resolve(reqType);
	}
	
	public final void replaceWithNodeResolve(Type reqType, ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		do_resolve(reqType,n);
	}

	public final void replaceWithResolve(Type reqType, ()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		do_resolve(reqType,n);
	}

	public final void replaceWithNodeResolve(ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		do_resolve(null,n);
	}

	public final void replaceWithResolve(()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		do_resolve(null,n);
	}
	
	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() {}

	public void initFrom(ENode node, Operator op, Method cm, ENode[] args) {
		throw new RuntimeException("Cannot init "+getClass()+" from "+node.getClass());
	}
	
	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public Operator getOp() { return null; }
	public void setOp(Operator op) { throw new RuntimeException("Cannot set operator "+op+" in ENode "+getClass()); }

	public final Method resolveMethodAndNormalize() {	
		Method m;
		if (this.dnode == null) {
			Symbol sym = getOp().resolveMethod(this);
			if (sym == null) {
				if (ctx_method == null || !ctx_method.isMacro())
					Kiev.reportError(this, "Unresolved method for operator "+getOp());
				return null;
			}
			m = (Method)sym.dnode;
			this.symbol = sym;
		} else {
			m = (Method)this.dnode;
		}
		m.normilizeExpr(this);
		return m;
	}

	public ENode[] getArgs() { return null; }

	public int getPriority() {
		if (isPrimaryExpr())
			return 255;
		Operator op = getOp();
		if (op == null)
			return 255;
		return op.priority;
	}

	public boolean valueEquals(Object o) { return false; }
	public boolean isConstantExpr() { return false; }
	public Object	getConstValue() {
		throw new RuntimeException("Request for constant value of non-constant expression");
	}
	
	public ENode closeBuild() { return this; }

	public void resolve(Type reqType) {
		((RENode)this).resolve(reqType);
	}

	public ANode doRewrite(RewriteContext ctx) {
		ENode en = (ENode)super.doRewrite(ctx);
		//en.ident_or_symbol_or_type = this.ident_or_symbol_or_type;
		String id = this.ident;
		String rw = ctx.replace(id);
		if (id != rw)
			en.ident = rw;
		return en;
	}
}

@ThisIsANode(name="NoOp", lang=CoreLang)
public final class NopExpr extends ENode {

	public static final ENode dummyNode = new NopExpr();

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public NopExpr() {}
	
	public String toString() { return ""; }

	public Type getType() {
		return Type.tpVoid;
	}
}


