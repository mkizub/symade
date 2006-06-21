package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 359 $
 *
 */

@nodeview
public view RNode of ASTNode implements Constants {
	public String toString();
	
	public int			pos;
	public int			compileflags;
	
	@getter public final ANode get$ctx_root();
	@getter public final FileUnit get$ctx_file_unit();
	@getter public final TypeDecl get$ctx_tdecl();
	@getter public final TypeDecl get$child_ctx_tdecl();
	@getter public final Method get$ctx_method();
	@getter public final Method get$child_ctx_method();

	public final ANode parent();
	public AttrSlot[] values();
	public final void callbackChildChanged(AttrSlot attr);
	public final void callbackRootChanged();
	public final Object getNodeData(AttrSlot attr);
	public final void addNodeData(ANode d, AttrSlot attr);
	public final void delNodeData(AttrSlot attr);
	public final DataFlowInfo getDFlow();
	public final ASTNode replaceWithNode(ASTNode node);
	public final ASTNode replaceWith(()->ASTNode fnode);
	public final boolean isAttached();
	public final boolean isBreakTarget();
	public final void    setBreakTarget(boolean on);
	public final boolean isAccessedFromInner();
	public final void    setAccessedFromInner(boolean on);
	public final boolean isResolved();
	public final void    setResolved(boolean on);
	public final boolean isHidden();
	public final void    setHidden(boolean on);
	public final boolean isBad();
	public final void    setBad(boolean on);

	public final Type getType();
	public final SpacePtr getSpacePtr(String name);
	public void open() { ((ASTNode)this).open(); }

	public boolean preGenerate() { return true; }
}

@nodeview
public static view RDNode of DNode extends RNode {

	public:ro int		flags;
	public:ro MetaSet	meta;
	public:ro Symbol	id;

	public final boolean isPublic()	;
	public final boolean isPrivate();
	public final boolean isProtected();
	public final boolean isPkgPrivate();
	public final boolean isStatic();
	public final boolean isFinal();
	public final boolean isSynchronized();
	public final boolean isVolatile();
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
	public final void setPackage();
	public final void setSyntax();
	public final void setStatic(boolean on);
	public final void setFinal(boolean on);
	public final void setSynchronized(boolean on);
	public final void setVolatile(boolean on);
	public final void setFieldVolatile(boolean on);
	public final void setMethodBridge(boolean on);
	public final void setFieldTransient(boolean on);
	public final void setMethodVarargs(boolean on);
	public final void setNative(boolean on);
	public final void setInterface(boolean on);
	public final void setAbstract(boolean on);
	public final void setStructView();
	public final void setTypeUnerasable(boolean on);
	public final boolean isVirtual();
	public final void setVirtual(boolean on);
	public final boolean isForward();
	public final void setForward(boolean on);

	public final boolean isMacro();
	
	public boolean preGenerate() { return true; }
	public void resolveDecl() { /* empty */ }
}

@nodeview
public abstract view RLvalDNode of LvalDNode extends RDNode {
	// init wrapper
	public final boolean isInitWrapper();
	public final void setInitWrapper(boolean on);
	// need a proxy access 
	public final boolean isNeedProxy();
	public final void setNeedProxy(boolean on);
}

@nodeview
public static view RENode of ENode extends RNode {

	public	SymbolRef			ident;
	
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
	public final ENode[] getArgs();
	public final int getPriority();
	public final boolean valueEquals(Object o);
	public final boolean isConstantExpr();
	public final Object	getConstValue();

	public void resolve(Type reqType) {
		throw new CompilerException(this,"Resolve call for e-node "+getClass());
	}
}

@nodeview
public final view RNopExpr of NopExpr extends RENode {

	public void resolve(Type reqType) {
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public view RTypeDecl of TypeDecl extends RDNode {
	public:ro			TypeRef[]				super_types;
	public:ro			TypeDef[]				args;
	public:ro			ASTNode[]				members;
	public:ro			MetaType				xmeta_type;
	public:ro			Type					xtype;

	public MetaType[] getAllSuperTypes();
	public final String qname();
	public boolean isClazz();
}


