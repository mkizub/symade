package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
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
import kiev.vlang.TypeDecl.TypeDeclImpl;
import kiev.vlang.types.TypeRef.TypeRefImpl;
import kiev.vlang.NameRef.NameRefImpl;

@nodeview
public view JNode of NodeImpl implements Constants {
	@virtual typedef ViewOf  = ASTNode;
	public final ViewOf getNode() alias operator(210,fy,$cast) {
		return ((NodeImpl)this)._self;
	}
	
	public String toString() { return String.valueOf(getNode()); }
	public Dumper toJava(Dumper dmp) { return getNode().toJava(dmp); }
	
	public:ro	int			pos;
//	public:ro	int			compileflags;
	
    public final int getPosLine();
	public final boolean isAttached();
	public final boolean isAccessedFromInner();
	public final boolean isResolved();
	public final boolean isHidden();
	public final boolean isBad();
	
	@getter public final JNode get$jparent() { return (JNode)((NodeImpl)this).parent; }
	@getter public JFileUnit get$jctx_file_unit() { return this.jparent.get$jctx_file_unit(); }
	@getter public JStruct get$jctx_clazz() { return this.jparent.child_jctx_clazz; }
	@getter public JStruct get$child_jctx_clazz() { return this.jparent.get$child_jctx_clazz(); }
	@getter public JMethod get$jctx_method() { return this.jparent.child_jctx_method; }
	@getter public JMethod get$child_jctx_method() { return this.jparent.get$child_jctx_method(); }

	public boolean equals(Object:Object obj) { return false; }
	public boolean equals(JNode:Object jnv) { return (NodeImpl)this == (NodeImpl)jnv; }

}

@nodeview
public view JDNode of DNodeImpl extends JNode {

	public final DNode getDNode() { return (DNode)this.getNode(); }
	
	public				int			flags;
	public:ro	MetaSet		meta;

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

	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }

	public void setPrivate();
}

@nodeview
public view JLvalDNode of LvalDNodeImpl extends JDNode {
	public final boolean isForward();
	public final boolean isInitWrapper();
	public final boolean isNeedProxy();
}

@nodeview
public view JENode of ENodeImpl extends JNode {
	
	public final ENode getENode() alias operator(210,fy,$cast) { return (ENode)this.getNode(); }
	
	//
	// Expr specific
	//

	public final boolean isUseNoProxy();
	public final boolean isAsField();
	public final boolean isGenVoidExpr();
	public final boolean isForWrapper();
	public final boolean isPrimaryExpr();
	public final boolean isSuperExpr();
	public final boolean isCastCall();

	//
	// Statement specific flags
	//
	
	public final boolean isAbrupted();
	public final boolean isBreaked();
	public final boolean isMethodAbrupted();
	public final boolean isAutoReturnable();
	public final boolean isBreakTarget();
	public final void setAutoReturnable(boolean on);
	
	public Type getType() { return this.getNode().getType(); }
	
	public boolean isConstantExpr() { return false; }
	public Object	getConstValue() {
		throw new RuntimeException("Request for constant value of non-constant expression");
    }

	public void generate(Code code, Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(this,"Unresolved node ("+this.getNode().getClass()+") generation, expr: "+dmp);
	}

}

@nodeview
public final view JVarDecl of VarDeclImpl extends JENode {

	public:ro	JVar	var;

	public void generate(Code code, Type reqType) {
		this.var.generate(code,Type.tpVoid);
	}
}

@nodeview
public final view JLocalStructDecl of LocalStructDeclImpl extends JENode {
	public:ro Struct		clazz;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public view JTypeDecl of TypeDeclImpl extends JDNode {
}

@nodeview
public static final view JTypeRef of TypeRefImpl extends JENode {
	public:ro Type	lnk;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public static final view JNameRef of NameRefImpl extends JNode {
	public:ro KString name;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}


