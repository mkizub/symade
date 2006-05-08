package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import kiev.ir.java15.RENode;
import kiev.be.java15.JENode;
//import kiev.be.java15.JVarDecl;
//import kiev.ir.java15.RVarDecl;
//import kiev.be.java15.JLocalStructDecl;
//import kiev.ir.java15.RLocalStructDecl;
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
	
	private static final ENode dummyNode = new NopExpr();

	@virtual typedef This  = ENode;
	@virtual typedef VView = VENode;
	@virtual typedef JView = JENode;
	@virtual typedef RView = RENode;

	//
	// Expr specific
	//

	// use no proxy	
	public final boolean isUseNoProxy() {
		return this.is_expr_use_no_proxy;
	}
	public final void setUseNoProxy(boolean on) {
		if (this.is_expr_use_no_proxy != on) {
			this.is_expr_use_no_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
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

	public final void replaceWithNodeResolve(Type reqType, ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		((ENode)n).resolve(reqType);
	}

	public final void replaceWithResolve(Type reqType, ()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		((ENode)n).resolve(reqType);
	}

	public final void replaceWithNodeResolve(ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		((ENode)n).resolve(null);
	}

	public final void replaceWithResolve(()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		((ENode)n).resolve(null);
	}
	
	@nodeview
	public static abstract view VENode of ENode extends NodeView {

		public final ENode getENode() { return (ENode)this; }
		
		//
		// Expr specific
		//
	
		// use no proxy	
		public final boolean isUseNoProxy();
		public final void setUseNoProxy(boolean on);
		// use as field (disable setter/getter calls for virtual fields)
		public final boolean isAsField();
		public final void setAsField(boolean on);
		// expression will generate void value
		public final boolean isGenVoidExpr();
		public final void setGenVoidExpr(boolean on);
		// used bt for()
		public final boolean isForWrapper();
		public final void setForWrapper(boolean on);
		// used for primary expressions, i.e. (a+b)
		public final boolean isPrimaryExpr();
		public final void setPrimaryExpr(boolean on);
		// used for super-expressions, i.e. (super.foo or super.foo())
		public final boolean isSuperExpr();
		public final void setSuperExpr(boolean on);
		// used for cast calls (to check for null)
		public final boolean isCastCall();
		public final void setCastCall(boolean on);
	
		//
		// Statement specific flags
		//
		
		// abrupted
		public final boolean isAbrupted();
		public final void setAbrupted(boolean on);
		// breaked
		public final boolean isBreaked();
		public final void setBreaked(boolean on);
		// method-abrupted
		public final boolean isMethodAbrupted();
		public final void setMethodAbrupted(boolean on);
		// auto-returnable
		public final boolean isAutoReturnable();
		public final void setAutoReturnable(boolean on);

		public final void replaceWithNodeResolve(Type reqType, ENode node);
		public final void replaceWithResolve(Type reqType, ()->ENode fnode);
		public final void replaceWithNodeResolve(ENode node);
		public final void replaceWithResolve(()->ENode fnode);

		public final Operator getOp();
		public final int getPriority();
		public final boolean valueEquals(Object o);
		public final boolean isConstantExpr();
		public final Object	getConstValue();
	}

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() {}

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public ASTNode getDummyNode() {
		return ENode.dummyNode;
	}

	public Operator getOp() { return null; }

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
	
	public void resolve(Type reqType) {
		((RView)this).resolve(reqType);
	}

}

@node
public final class NopExpr extends ENode {

	public static final AttrSlot ATTR = new DataAttrSlot("temp expr",true,ENode.class);	

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = NopExpr;
	@virtual typedef VView = VNopExpr;
	@virtual typedef RView = RNopExpr;

	@nodeview
	public static final view VNopExpr of NopExpr extends VENode {
	}

	public NopExpr() {}
	
	public String toString() { return ""; }

	public Dumper toJava(Dumper dmp) { return dmp; }

	public Type getType() {
		return Type.tpVoid;
	}
}


