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
package kiev.ir.java15;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public view RNode of ASTNode implements Constants {
	public String toString();
	
	public int			pos;
	
	@getter public final ANode get$ctx_root();
	@getter public final FileUnit get$ctx_file_unit();
	@getter public final NameSpace get$ctx_name_space();
	@getter public final TypeDecl get$ctx_tdecl();
	@getter public final Method get$ctx_method();

	public final ANode parent();
	public AttrSlot[] values();
	public final void callbackChildChanged(AttrSlot attr);
	public final <N extends ANode> N replaceWithNode(N node);
	public final ASTNode replaceWith(()->ASTNode fnode);
	public final boolean isAttached();
	public final boolean isBreakTarget();
	public final void    setBreakTarget(boolean on);
	public final boolean isResolved();
	public final void    setResolved(boolean on);
	public final boolean isAutoGenerated();
	public final void    setAutoGenerated(boolean on);
	public final boolean isBad();
	public final void    setBad(boolean on);

	public final Type getType();
	public final SpacePtr getSpacePtr(String name);

	public boolean preGenerate() { return true; }
}

public static view RSNode of SNode extends RNode {
	public void resolveDecl() {}
}

public static final view RDeclGroup of DeclGroup extends RSNode {
	public:ro MetaSet	meta;
	public:ro	DNode[]		decls;

	public void resolveDecl() {
		if( isResolved() ) return;
		foreach (DNode dn; decls)
			dn.resolveDecl();
		DataFlowInfo.getDFlow((DeclGroup)this).out();
		setResolved(true);
	}
}

public static view RDNode of DNode extends RNode {

	public:ro MetaSet	meta;
	public:ro String	sname;

	public final boolean isPublic()	;
	public final boolean isPrivate();
	public final boolean isProtected();
	public final boolean isPkgPrivate();
	public final boolean isStatic();
	public final boolean isFinal();
	public final boolean isSynchronized();
	public final boolean isFieldVolatile();
	public final boolean isMethodBridge();
	public final boolean isFieldTransient();
	public final boolean isMethodVarargs();
	public final boolean isStructBcLoaded();
	public final boolean isNative();
	public final boolean isInterface();
	public final boolean isAbstract();
	public final boolean isMathStrict();
	public final boolean isSynthetic();
	
	public final boolean isStructView();
	public final boolean isTypeUnerasable();
	public final boolean isPackage();
	public final boolean isSyntax();

	public final void setPublic();
	public final void setPrivate();
	public final void setProtected();
	public final void setPkgPrivate();
	public final void setStatic(boolean on);
	public final void setFinal(boolean on);
	public final void setSynchronized(boolean on);
	public final void setFieldVolatile(boolean on);
	public final void setMethodBridge(boolean on);
	public final void setFieldTransient(boolean on);
	public final void setMethodVarargs(boolean on);
	public final void setNative(boolean on);
	public final void setAbstract(boolean on);
	public final void setTypeUnerasable(boolean on);
	public final boolean isVirtual();
	public final void setVirtual(boolean on);
	public final boolean isForward();
	public final void setForward(boolean on);

	public final boolean isMacro();
	
	public boolean preGenerate() { return true; }
	public void resolveDecl() { /* empty */ }
}

public static view RENode of ENode extends RNode {

	public		String			ident;
	public		ISymbol			symbol;
	public:ro	DNode			dnode;
	
	//
	// Expr specific
	//

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
	// reachable by direct control flow, with no jumps into
	public final boolean isDirectFlowReachable();
	public final void setDirectFlowReachable(boolean on);

	public final void replaceWithNodeResolve(Type reqType, ENode node);
	public final void replaceWithResolve(Type reqType, ()->ENode fnode);
	public final void replaceWithNodeResolve(ENode node);
	public final void replaceWithResolve(()->ENode fnode);

	public final Operator getOp();
	public final ENode[] getArgs();
	public final int getPriority();
	public final boolean valueEquals(Object o);
	public final boolean isConstantExpr();
	public final Object	getConstValue();

	public void resolve(Type reqType) {
		throw new CompilerException(this,"Resolve call for e-node "+getClass());
	}
}

public final view RNopExpr of NopExpr extends RENode {

	public void resolve(Type reqType) {
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

public view RTypeDecl of TypeDecl extends RDNode {
	public:ro			TypeRef[]				super_types;
	public:ro			TypeDef[]				args;
	public:ro			ASTNode[]				members;
	public:ro			MetaType				xmeta_type;
	public:ro			Type					xtype;

	public MetaType[] getAllSuperTypes();
	public final String qname();
	public boolean isClazz();
	public final Field[] getAllFields();
	public final boolean isStructInner();
}


