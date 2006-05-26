package kiev.be.java15;

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
public view JNode of ASTNode implements JConstants {

	@unerasable
	public static <J extends JNode> J[] toJArray(NodeSpace<ASTNode> narr)
		alias operator(210,fy,$cast)
	{
		int sz = narr.length;
		J[] jarr = new J[sz];
		for (int i=0; i < sz; i++)
			jarr[i] = (J)narr[i];
		return jarr;
	}

	@unerasable
	public static JType[] toJTypeArray(NodeSpace<ASTNode> narr)
		alias operator(210,fy,$cast)
	{
		int sz = narr.length;
		JType[] jarr = new JType[sz];
		for (int i=0; i < sz; i++)
			jarr[i] = narr[i].getType().getJType();
		return jarr;
	}

	public String toString();
	public Dumper toJava(Dumper dmp);
	
	public:ro	int			pos;
//	public:ro	int			compileflags;
	
	public final void addNodeData(ANode d, AttrSlot attr);
	public final void delNodeData(AttrSlot attr);
	public final Object getNodeData(AttrSlot attr);

    public final int getPosLine();
	public final boolean isAttached();
	public final boolean isAccessedFromInner();
	public final boolean isResolved();
	public final boolean isHidden();
	public final boolean isBad();
	
	@getter public final JNode get$jparent() { return (JNode)(ASTNode)((ASTNode)this).parent(); }
	@getter public JFileUnit get$jctx_file_unit() { return this.jparent.get$jctx_file_unit(); }
	@getter public JTypeDecl get$jctx_tdecl() { return this.jparent.child_jctx_tdecl; }
	@getter public JTypeDecl get$child_jctx_tdecl() { return this.jparent.get$child_jctx_tdecl(); }
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
	public:ro	Symbol		id;

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

	public final boolean isMacro();

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
	
	public:ro	SymbolRef			ident;

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
public view JTypeDecl of TypeDecl extends JDNode {
	public:ro	JType[]				super_types;
	public:ro	JNode[]				members;

	@getter public JTypeDecl get$child_jctx_tdecl() { return this; }

	public:ro	Type				xtype;
	public:ro	JType				jtype;
	@getter
	public final JType				get$jtype()			{ return this.xtype.getJType(); }


	public final boolean isClazz();
	public final boolean isPackage();
	public final boolean isLocal();
	public final boolean isAnonymouse();
	public final boolean isAnnotation();
	public final boolean isEnum();
	public final boolean isSyntax()	;
	public final boolean isLoadedFromBytecode();

	public boolean checkResolved();
	
	public boolean instanceOf(JTypeDecl cl) {
		if( cl == null ) return false;
		if( this.equals(cl) ) return true;
		foreach (JType jt; super_types; jt.getJTypeDecl().instanceOf(cl))
			return true;
		return false;
	}

}

@nodeview
public static final view JTypeRef of TypeRef extends JENode {
	public:ro Type	lnk;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}


