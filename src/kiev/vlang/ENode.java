package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;
import kiev.ir.java15.RNopExpr;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
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
	@virtual typedef TypeOfIdent ≤ DNode;

	@att public SymbolRef<TypeOfIdent>		ident;
	
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
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// expression will generate void value
	public final boolean isGenVoidExpr() {
		return this.is_expr_gen_void;
	}
	public final void setGenVoidExpr(boolean on) {
		if (this.is_expr_gen_void != on) {
			this.is_expr_gen_void = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// used bt for()
	public final boolean isForWrapper() {
		return this.is_expr_for_wrapper;
	}
	public final void setForWrapper(boolean on) {
		if (this.is_expr_for_wrapper != on) {
			this.is_expr_for_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
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
			this.callbackChildChanged(nodeattr$flags);
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
			this.callbackChildChanged(nodeattr$flags);
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
			this.callbackChildChanged(nodeattr$flags);
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
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// breaked
	public final boolean isBreaked() {
		return this.is_stat_breaked;
	}
	public final void setBreaked(boolean on) {
		if (this.is_stat_breaked != on) {
			this.is_stat_breaked = on;
			this.callbackChildChanged(nodeattr$flags);
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
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// auto-returnable
	public final boolean isAutoReturnable() {
		return this.is_stat_auto_returnable;
	}
	public final void setAutoReturnable(boolean on) {
		if (this.is_stat_auto_returnable != on) {
			this.is_stat_auto_returnable = on;
			this.callbackChildChanged(nodeattr$flags);
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

}

@node(name="NoOp")
public final class NopExpr extends ENode {

	public static final ENode dummyNode = new NopExpr();

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = NopExpr;
	@virtual typedef RView = RNopExpr;

	public NopExpr() {}
	
	public String toString() { return ""; }

	public Type getType() {
		return Type.tpVoid;
	}
}


