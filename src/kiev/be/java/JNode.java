package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public class JNodeView extends ASTNode.NodeView {
	public JNodeView(ASTNode.NodeImpl impl) {
		super(impl);
	}
}

@nodeview
public class JDNodeView extends JNodeView {
	final DNode.DNodeImpl impl;
	public JDNodeView(DNode.DNodeImpl impl) {
		super(impl);
		this.impl = impl;
	}
	
	public final DNode getDNode() { return (DNode)this.getNode(); }
	
	@getter public final int		get$flags()				{ return this.impl.flags; }
	@getter public final MetaSet	get$meta()				{ return this.impl.meta; }
	@setter public final void		set$flags(int val)		{ this.impl.flags = val; }

	public final boolean isPublic()				{ return (flags & ACC_PUBLIC) != 0; }
	public final boolean isPrivate()			{ return (flags & ACC_PRIVATE) != 0; }
	public final boolean isProtected()			{ return (flags & ACC_PROTECTED) != 0; }
	public final boolean isPackageVisable()	{ return (flags & (ACC_PROTECTED|ACC_PUBLIC|ACC_PROTECTED)) == 0; }
	public final boolean isStatic()				{ return (flags & ACC_STATIC) != 0; }
	public final boolean isFinal()				{ return (flags & ACC_FINAL) != 0; }
	public final boolean isSynchronized()		{ return (flags & ACC_SYNCHRONIZED) != 0; }
	public final boolean isVolatile()			{ return (flags & ACC_VOLATILE) != 0; }
	public final boolean isTransient()			{ return (flags & ACC_TRANSIENT) != 0; }
	public final boolean isNative()				{ return (flags & ACC_NATIVE) != 0; }
	public final boolean isInterface()			{ return (flags & ACC_INTERFACE) != 0; }
	public final boolean isAbstract()			{ return (flags & ACC_ABSTRACT) != 0; }
	public final boolean isSuper()				{ return (flags & ACC_SUPER) != 0; }

}

@nodeview
public class JLvalDNodeView extends JDNodeView {
	public JLvalDNodeView(LvalDNode.LvalDNodeImpl impl) {
		super(impl);
	}

	@getter public final boolean isForward() { return this.impl.is_forward; }
	@getter public final boolean isInitWrapper() { return this.impl.is_init_wrapper; }
	@getter public final boolean isNeedProxy() { return this.impl.is_need_proxy; }
}

@nodeview
public class JENodeView extends JNodeView {
	public JENodeView(ENode.ENodeImpl impl) {
		super(impl);
	}

	public final ENode getENode() { return (ENode)this.getNode(); }
	
	//
	// Expr specific
	//

	public final boolean isUseNoProxy() { return this.impl.is_expr_use_no_proxy; }
	public final boolean isAsField() { return this.impl.is_expr_as_field; }
	public final boolean isGenVoidExpr() { return this.impl.is_expr_gen_void; }
	public final boolean isTryResolved() { return this.impl.is_expr_try_resolved; }
	public final boolean isForWrapper() { return this.impl.is_expr_for_wrapper; }
	public final boolean isPrimaryExpr() { return this.impl.is_expr_primary; }

	//
	// Statement specific flags
	//
	
	public final boolean isAbrupted() { return this.impl.is_stat_abrupted; }
	public final boolean isBreaked() { return this.impl.is_stat_breaked; }
	public final boolean isMethodAbrupted() { return this.impl.is_stat_method_abrupted; }
	public final boolean isAutoReturnable() { return this.impl.is_stat_auto_returnable; }
	public final boolean isBreakTarget() { return this.impl.is_stat_break_target; }

	public Type getType() { return this.getNode().getType(); }
	
	public void generate(Code code, Type reqType) {
		//Dumper dmp = new Dumper();
		//dmp.append(this);
		//throw new CompilerException(this,"Unresolved node ("+this.getNode().getClass()+") generation, expr: "+dmp);
		this.getENode().generate(code, reqType);
	}

}

@nodeview
public class JVarDeclView extends JENodeView {
	final VarDecl.VarDeclImpl impl;
	public JVarDeclView(VarDecl.VarDeclImpl impl) {
		super(impl);
		this.impl = impl;
	}
	
	@getter public final JVarView	get$var()				{ return this.impl.var.getJVarView(); }

	public void generate(Code code, Type reqType) {
		this.var.generate(code,Type.tpVoid);
	}
}

@nodeview
public static class JLocalStructDeclView extends JENodeView {
	final LocalStructDecl.LocalStructDeclImpl impl;
	public JLocalStructDeclView(LocalStructDecl.LocalStructDeclImpl impl) {
		super(impl);
		this.impl = impl;
	}
	
	@getter public final Struct		get$clazz()				{ return this.impl.clazz; }

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public class JTypeDefView extends JDNodeView {
	public JTypeDefView(TypeDef.TypeDefImpl impl) {
		super(impl);
	}
}

