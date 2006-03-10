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

@nodeview
public view JNode of ASTNode implements Constants {

	public String toString();
	public Dumper toJava(Dumper dmp);
	
	public:ro	int			pos;
//	public:ro	int			compileflags;
	
	public final void addNodeData(NodeData d, AttrSlot attr);
	public final void delNodeData(AttrSlot attr);

    public final int getPosLine();
	public final boolean isAttached();
	public final boolean isAccessedFromInner();
	public final boolean isResolved();
	public final boolean isHidden();
	public final boolean isBad();
	
	@getter public final JNode get$jparent() { return (JNode)((ASTNode)this).parent; }
	@getter public JFileUnit get$jctx_file_unit() { return this.jparent.get$jctx_file_unit(); }
	@getter public JStruct get$jctx_clazz() { return this.jparent.child_jctx_clazz; }
	@getter public JStruct get$child_jctx_clazz() { return this.jparent.get$child_jctx_clazz(); }
	@getter public JMethod get$jctx_method() { return this.jparent.child_jctx_method; }
	@getter public JMethod get$child_jctx_method() { return this.jparent.get$child_jctx_method(); }

	public boolean equals(Object:Object obj) { return false; }
	public boolean equals(JNode:Object jnv) { return (ASTNode)this == (ASTNode)jnv; }

	public final Type getType() { return ((ASTNode)this).getType(); }
	
}

@nodeview
public view JDNode of DNode extends JNode {

	public		int			flags;
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

	public boolean isTypeUnerasable();
}

@nodeview
public view JLvalDNode of LvalDNode extends JDNode {
	public final boolean isForward();
	public final boolean isInitWrapper();
	public final boolean isNeedProxy();
}

@nodeview
public view JENode of ENode extends JNode {
	
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
	
	public boolean isConstantExpr() { return false; }
	public Object	getConstValue() {
		throw new RuntimeException("Request for constant value of non-constant expression");
    }

	public void generate(Code code, Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(this,"Unresolved node ("+((ENode)this).getClass()+") generation, expr: "+dmp);
	}

}

@nodeview
public final view JVarDecl of VarDecl extends JENode {

	public:ro	JVar	var;

	public void generate(Code code, Type reqType) {
		this.var.generate(code,Type.tpVoid);
	}
}

@nodeview
public final view JLocalStructDecl of LocalStructDecl extends JENode {
	public:ro Struct		clazz;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public view JTypeDecl of TypeDecl extends JDNode {
}

@nodeview
public static final view JTypeRef of TypeRef extends JENode {
	public:ro Type	lnk;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}

@nodeview
public static final view JNameRef of NameRef extends JNode {
	public:ro KString name;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}


