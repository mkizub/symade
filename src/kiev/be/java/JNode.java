package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.ASTNode.NodeImpl;
import kiev.vlang.DNode.DNodeImpl;
import kiev.vlang.ENode.ENodeImpl;
import kiev.vlang.LvalDNode.LvalDNodeImpl;
import kiev.vlang.VarDecl.VarDeclImpl;
import kiev.vlang.LocalStructDecl.LocalStructDeclImpl;
import kiev.vlang.TypeDef.TypeDefImpl;

@nodeview
public view JNodeView of NodeImpl extends ASTNode.NodeView {
	public JNodeView(NodeImpl $view) {
		super($view);
	}
}

@nodeview
public view JDNodeView of DNodeImpl extends JNodeView {

	public final DNode getDNode() { return (DNode)this.getNode(); }
	
	public				int			flags;
	public access:ro	MetaSet		meta;

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
public view JLvalDNodeView of LvalDNodeImpl extends JDNodeView {
	public final boolean isForward() { return this.$view.is_forward; }
	public final boolean isInitWrapper() { return this.$view.is_init_wrapper; }
	public final boolean isNeedProxy() { return this.$view.is_need_proxy; }
}

@nodeview
public view JENodeView of ENodeImpl extends JNodeView {
	
	public JENodeView(ENodeImpl $view) {
		super($view);
	}

	public final ENode getENode() alias operator(210,fy,$cast) { return (ENode)this.getNode(); }
	
	//
	// Expr specific
	//

	public final boolean isUseNoProxy() { return this.$view.is_expr_use_no_proxy; }
	public final boolean isAsField() { return this.$view.is_expr_as_field; }
	public final boolean isGenVoidExpr() { return this.$view.is_expr_gen_void; }
	public final boolean isTryResolved() { return this.$view.is_expr_try_resolved; }
	public final boolean isForWrapper() { return this.$view.is_expr_for_wrapper; }
	public final boolean isPrimaryExpr() { return this.$view.is_expr_primary; }

	//
	// Statement specific flags
	//
	
	public final boolean isAbrupted() { return this.$view.is_stat_abrupted; }
	public final boolean isBreaked() { return this.$view.is_stat_breaked; }
	public final boolean isMethodAbrupted() { return this.$view.is_stat_method_abrupted; }
	public final boolean isAutoReturnable() { return this.$view.is_stat_auto_returnable; }
	public final boolean isBreakTarget() { return this.$view.is_stat_break_target; }

	public Type getType() { return this.getNode().getType(); }
	
	public void generate(Code code, Type reqType) {
		//Dumper dmp = new Dumper();
		//dmp.append(this);
		//throw new CompilerException(this,"Unresolved node ("+this.getNode().getClass()+") generation, expr: "+dmp);
		this.getENode().generate(code, reqType);
	}

}

@nodeview
public final view JVarDeclView of VarDeclImpl extends JENodeView {

	public access:ro	JVarView	var;

	public void generate(Code code, Type reqType) {
		this.var.generate(code,Type.tpVoid);
	}
}

@nodeview
public final view JLocalStructDeclView of LocalStructDeclImpl extends JENodeView {
	public access:ro Struct		clazz;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public abstract view JTypeDefView of TypeDefImpl extends JDNodeView {
	public JTypeDefView(TypeDefImpl $view) {
		super($view);
	}
}

