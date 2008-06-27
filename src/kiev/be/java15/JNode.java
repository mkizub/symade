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
package kiev.be.java15;

import syntax kiev.Syntax;

@ViewOf(vcast=true, iface=true)
public view JNode of ASTNode implements JConstants {

	@unerasable
	public static <J extends JNode> J[] toJArray(NodeSpace<ASTNode> narr)
		alias fy operator $cast
	{
		int sz = narr.length;
		J[] jarr = new J[sz];
		for (int i=0; i < sz; i++)
			jarr[i] = (J)narr[i];
		return jarr;
	}

	@unerasable
	public static JType[] toJTypeArray(NodeSpace<ASTNode> narr)
		alias fy operator $cast
	{
		int sz = narr.length;
		JType[] jarr = new JType[sz];
		for (int i=0; i < sz; i++)
			jarr[i] = narr[i].getType().getJType();
		return jarr;
	}

	public String toString();
	
	public:ro	int			pos;
	
    public final int getPosLine();
	public final boolean isAttached();
	public final boolean isResolved();
	public final boolean isAutoGenerated();
	public final boolean isBad();
	
	@getter public final JNode get$jparent() { return (JNode)(ASTNode)((ASTNode)this).parent(); }
	@getter public final JFileUnit get$jctx_file_unit() { return (JFileUnit)((ASTNode)this).ctx_file_unit; }
	@getter public final JNameSpace get$jctx_name_space() { return (JNameSpace)((ASTNode)this).ctx_name_space; }
	@getter public final JTypeDecl get$jctx_tdecl() { return (JTypeDecl)((ASTNode)this).ctx_tdecl; }
	@getter public final JMethod get$jctx_method() { return (JMethod)((ASTNode)this).ctx_method; }

	public boolean equals(Object:Object obj) { return false; }
	public boolean equals(JNode:Object jnv) { return (ASTNode)this == (ASTNode)jnv; }

	public final Type getType() { return ((ASTNode)this).getType(); }
	
	public void generate(Code code, Type reqType) {
		throw new CompilerException(this,"Unresolved node ("+((ASTNode)this).getClass()+") generation");
	}
	public void backendCleanup() {}
}

@ViewOf(vcast=true, iface=true)
public view JDNode of DNode extends JNode {

	public:ro	MNode[]		metas;
	public:ro	String		sname;
	public		Attr[]		jattrs;

	public boolean hasName(String nm);
	public boolean hasNameStart(String nm);

	public final boolean isPublic()	;
	public final boolean isPrivate();
	public final boolean isProtected();
	public final boolean isPkgPrivate();
	public final boolean isStatic();
	public final boolean isFinal();
	public final boolean isSynchronized();
	public final boolean isFieldTransient();
	public final boolean isNative();
	public final boolean isInterface();
	public final boolean isAbstract();

	public final boolean isMacro();
	public final boolean isInterfaceOnly();

	public final boolean hasRuntimeVisibleMetas();
	public final boolean hasRuntimeInvisibleMetas();

	public short getJavaFlags();

	public void setPrivate();

	public boolean isTypeUnerasable();

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		Attr[] jattrs = this.jattrs;
		if (jattrs != null) {
			for(int i=0; i < jattrs.length; i++) {
				if(jattrs[i].name == a.name) {
					jattrs[i] = a;
					return a;
				}
			}
			this.jattrs = (Attr[])Arrays.append(jattrs,a);
		} else {
			this.jattrs = new Attr[]{a};
		}
		return a;
	}

	public Attr getAttr(KString name) {
		Attr[] jattrs = this.jattrs;
		if (jattrs != null) {
			for(int i=0; i < jattrs.length; i++)
				if( jattrs[i].name.equals(name) )
					return jattrs[i];
		}
		return null;
	}

	public void backendCleanup() {
		this.jattrs = null;
	}

}

@ViewOf(vcast=true, iface=true)
public view JSNode of SNode extends JNode {
}

@ViewOf(vcast=true, iface=true)
public view JENode of ENode extends JNode {
	
	public:ro	String			ident;
	public:ro	ISymbol			symbol;
	public:ro	DNode			dnode;

	//
	// Expr specific
	//

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
	
	public boolean isConstantExpr();
	public Object	getConstValue();
}

@ViewOf(vcast=true, iface=true)
public static final view JNopExpr of NopExpr extends JENode {
	public void generate(Code code, Type reqType) {
		// nothing to generate - noop
		if (Kiev.debugOutputA || Kiev.debugOutputT) {
			trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating NopExpr: "+this);
			code.setLinePos(this);
			code.addInstr(Instr.op_nop);
		}
	}
}

@ViewOf(vcast=true, iface=true)
public view JTypeDecl of TypeDecl extends JDNode {
	public:ro	JType[]				super_types;

	public:ro	Type				xtype;
	public:ro	JType				jtype;
	@getter
	public final JType				get$jtype()			{ return this.xtype.getJType(); }


	public final boolean isClazz();
	public final boolean isAnnotation();
	public final boolean isEnum();
	public final boolean isTypeDeclNotLoaded();
	public final boolean isStructInner();

	public void checkResolved();
	
	public boolean instanceOf(JTypeDecl cl) {
		if( cl == null ) return false;
		if( this.equals(cl) ) return true;
		foreach (JType jt; super_types; jt.getJTypeDecl().instanceOf(cl))
			return true;
		return false;
	}
}

@ViewOf(vcast=true, iface=true)
public static final view JTypeRef of TypeRef extends JENode {
	public:ro Type	type_lnk;

	public void generate(Code code, Type reqType) {
		// don't generate here
	}
}


