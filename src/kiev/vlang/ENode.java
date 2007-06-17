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

import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RNopExpr;
import kiev.be.java15.JNopExpr;

import syntax kiev.Syntax;

/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@node
public abstract class ENode extends ASTNode {

	@dflow(out="this:in") private static class DFI {}
	
	@virtual typedef This  ≤ ENode;
	@virtual typedef JView ≤ JENode;
	@virtual typedef RView ≤ RENode;

	private Object	ident_or_symbol_or_type;
	
	@att @abstract public String			ident;
	@att @abstract public boolean			qualified; // stored ident may be qualified name
	@ref @abstract public ISymbol			symbol;
	@ref @abstract public Type				type_lnk;
	@ref @abstract public:ro DNode			dnode;
	
	@getter @att public final String get$ident() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof String)
			return (String)id;
		if (id instanceof ISymbol) {
			if (qualified)
				return ((ISymbol)id).qname;
			return ((ISymbol)id).sname;
		}
		if (id instanceof Type) {
			if (qualified)
				return ((Type)id).meta_type.qname();
			return ((Type)id).meta_type.tdecl.sname;
		}
		return null;
	}

	@getter @ref public final ISymbol get$symbol() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof ISymbol)
			return (ISymbol)ANode.getVersion((ANode)id);
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		return null;
	}
	
	@getter @ref public final DNode get$dnode() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof DNode)
			return ANode.getVersion((DNode)id);
		if (id instanceof ISymbol)
			return ANode.getVersion(((ISymbol)id).dnode);
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		return null;
	}
	
	@getter @ref public final Type get$type_lnk() {
		Object id = this.ident_or_symbol_or_type;
		if (id == null)
			return null;
		if (id instanceof Type)
			return (Type)id;
		return null;
	}
	
	@setter public final void set$ident(String val) {
		if (val != null) {
			val = val.intern();
			if (val.indexOf('\u001f') >= 0)
				qualified = true;
		}
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$symbol(ISymbol val) {
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$type_lnk(Type val) {
		ident_or_symbol_or_type = val;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "qualified")
			return this.qualified; // do not dump <qualified>false</qualified>
		return super.includeInDump(dump, attr, val);
	}

	@getter @att public final boolean get$qualified() {
		return is_qualified;
	}

	@setter public final void set$qualified(boolean val) {
		is_qualified = val;
	}

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
			assert(!locked);
			this.is_expr_primary = on;
		}
	}
	// used for super-expressions, i.e. (super.foo or super.foo())
	public final boolean isSuperExpr() {
		return this.is_expr_super;
	}
	public final void setSuperExpr(boolean on) {
		if (this.is_expr_super != on) {
			assert(!locked);
			this.is_expr_super = on;
		}
	}
	// used for cast calls (to check for null)
	public final boolean isCastCall() {
		return this.is_expr_cast_call;
	}
	public final void setCastCall(boolean on) {
		if (this.is_expr_cast_call != on) {
			assert(!locked);
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
			do_resolve(reqType, e.replacer);
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

	public ASTNode getDummyNode() {
		return NopExpr.dummyNode;
	}

	public Operator getOp() { return null; }
	public void setOp(Operator op) { throw new RuntimeException("Cannot set operator "+op+" in ENode "+getClass()); }
	
	
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
		((RView)this).resolve(reqType);
	}

	public ANode doRewrite(RewriteContext ctx) {
		ENode en = (ENode)super.doRewrite(ctx);
		String id = this.ident;
		String rw = ctx.replace(id);
		if (id != rw)
			en.ident = rw;
		return en;
	}
}

@node(name="NoOp")
public final class NopExpr extends ENode {

	public static final ENode dummyNode = new NopExpr();

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = NopExpr;
	@virtual typedef RView = RNopExpr;
	@virtual typedef JView = JNopExpr;

	public NopExpr() {}
	
	public String toString() { return ""; }

	public Type getType() {
		return Type.tpVoid;
	}
}


