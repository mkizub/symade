/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/AST.java,v 1.6.2.1.2.3 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.6.2.1.2.3 $
 *
 */

// AST declarations for FileUnit, Struct-s, Import-s, Operator-s, Typedef-s, Macros-es
public interface TopLevelDecl {
	// create top-level, inner named, argument Struct-s
	//public ASTNode pass1(ASTNode pn);
	// resolve some imports, remember typedef's names, remember
	// operator declarations, remember names/operators for type macroses
	//public ASTNode pass1_1(ASTNode pn);
	// process inheritance for type arguments, create
	// Struct's for template types
	//public ASTNode pass2(ASTNode pn);
	// process Struct's inheritance (extends/implements)
	//public ASTNode pass2_2(ASTNode pn);
	// process Struct's members (fields, methods)
	public ASTNode pass3() { return (ASTNode)this; }
	// autoProxyMethods()
	public ASTNode autoProxyMethods() { return (ASTNode)this; }
	// resolveImports()
	public ASTNode resolveImports() { return (ASTNode)this; }
	// resolveFinalFields()
	public ASTNode resolveFinalFields(boolean cleanup) { return (ASTNode)this; }
};

public enum TopLevelPass /*extends int*/ {
	passStartCleanup		= 0,	// start of compilation or cleanup before next incremental compilation
	passCreateTopStruct		= 1,	// create top-level Struct
	passProcessSyntax		= 2,	// process syntax - some import, typedef, operator and macro
	passArgumentInheritance	= 3,	// inheritance of type arguments
	passStructInheritance	= 4,	// inheritance of classe/interfaces/structures
	passCreateMembers		= 5,	// create declared members of structures
	passAutoProxyMethods	= 6,	// autoProxyMethods()
	passResolveImports		= 7,	// recolve import static for import of fields and methods
	passResolveFinalFields	= 8,	// resolve final fields, to find out if they are constants
	passGenerate			= 9		// resolve, generate and so on - each file separatly
};


public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];

	private static int		parserAddrIdx;

	public int			pos;
    public ASTNode		parent;
	public int			flags;

    public ASTNode(int pos) {
		this.pos = pos;
	}

	public /*abstract*/ void cleanup() {
		parent = null;
	};

	public ASTNode(int pos, int fl) {
		this(pos);
		flags = fl;
	}

	public ASTNode(int pos, ASTNode parent) {
		this(pos);
		this.parent = parent;
	}

	public String parserAddr() {
		String addr = Integer.toHexString(++parserAddrIdx);
		while( addr.length() < 8 ) {
			addr = '0'+addr;
		}
		Kiev.parserAddresses.put(addr,this);
		return addr;
	}

	public void jjtSetParent(ASTNode n) { parent = n; }
	public ASTNode jjtGetParent() { return parent; }
	public void setParent(ASTNode n) { parent = n; }
	public ASTNode getParent() { return parent; }
	public void jjtAddChild(ASTNode n, int i) {
		Kiev.reportError(pos,"jjtAddChild not implemented for this class: "+getClass());
	}
	public void addChild(ASTNode n, int i) { jjtAddChild(n,i); }

    public final int getPos() { return pos; }
    public final int getPosLine() { return pos >>> 11; }
    public final int getPosColumn() { return pos & 0x3FF; }
    public final int setPos(int line, int column) { return pos = (line << 11) | (column & 0x3FF); }
    public final int setPos(int pos) { return this.pos = pos; }
	public Type getType() { return Type.tpVoid; }

    public Dumper toJava(Dumper dmp) {
    	dmp.append("/* INTERNAL ERROR - ").append(this.getClass().toString()).append(" */");
    	return dmp;
    }

	//public ASTNode pass1(ASTNode pn)   { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	//public ASTNode pass1_1(ASTNode pn) { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	//public ASTNode pass2(ASTNode pn)   { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	//public ASTNode pass2_2(ASTNode pn) { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public ASTNode pass3()             { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public ASTNode autoProxyMethods()  { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public ASTNode resolveImports()    { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }
	public ASTNode resolveFinalFields(boolean cleanup) { throw new CompilerException(getPos(),"Internal error ("+this.getClass()+")"); }

	public int setFlags(int fl) {
		trace(Kiev.debugFlags,"Member "+this+" flags set to 0x"+Integer.toHexString(fl)+" from "+Integer.toHexString(flags));
		flags = fl;
		if( this instanceof Struct ) {
			Struct self = (Struct)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		else if( this instanceof Method ) {
			Method self = (Method)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		else if( this instanceof Field ) {
			Field self = (Field)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		return flags;
	}
	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }

	public boolean isPublic()		{ return (flags & ACC_PUBLIC) != 0; }
	public boolean isPrivate()		{ return (flags & ACC_PRIVATE) != 0; }
	public boolean isProtected()	{ return (flags & ACC_PROTECTED) != 0; }
	public boolean isPackageVisable()	{ return (flags & (ACC_PROTECTED|ACC_PUBLIC|ACC_PROTECTED)) == 0; }
	public boolean isStatic()		{ return (flags & ACC_STATIC) != 0; }
	public boolean isFinal()		{ return (flags & ACC_FINAL) != 0; }
	public boolean isSynchronized()	{ return (flags & ACC_SYNCHRONIZED) != 0; }
	public boolean isVolatile()		{ return (flags & ACC_VOLATILE) != 0; }
	public boolean isTransient()	{ return (flags & ACC_TRANSIENT) != 0; }
	public boolean isNative()		{ return (flags & ACC_NATIVE) != 0; }
	public boolean isInterface()	{ return (flags & ACC_INTERFACE) != 0; }
	public boolean isAbstract()		{ return (flags & ACC_ABSTRACT) != 0; }
	public boolean isSuper()		{ return (flags & ACC_SUPER) != 0; }

	// Struct specific
	public boolean isPackage()		{ return (flags & ACC_PACKAGE) != 0; }
	public boolean isClazz()		{ return (flags & (ACC_PACKAGE|ACC_INTERFACE|ACC_ARGUMENT)) == 0; }
	public boolean isArgument()		{ return (flags & ACC_ARGUMENT) != 0; }
	public boolean isPizzaCase()	{ return (flags & ACC_PIZZACASE) != 0; }
	public boolean isLocal()		{ return (flags & ACC_LOCAL) != 0; }
	public boolean isAnonymouse()	{ return (flags & ACC_ANONYMOUSE) != 0; }
	public boolean isHasCases()		{ return (flags & ACC_HAS_CASES) != 0; }
	public boolean isVerified()		{ return (flags & ACC_VERIFIED) != 0; }
	public boolean isMembersGenerated()		{ return (flags & ACC_MEMBERS_GENERATED) != 0; }
	public boolean isStatementsGenerated()	{ return (flags & ACC_STATEMENTS_GENERATED) != 0; }
	public boolean isGenerated()	{ return (flags & ACC_GENERATED) != 0; }
	public boolean isEnum()			{ return (flags & ACC_ENUM) != 0; }
	public boolean isSyntax()		{ return (flags & ACC_SYNTAX) != 0; }
	public boolean isPrimitiveEnum(){ return (flags & ACC_PRIMITIVE_ENUM) != 0; }
	public boolean isWrapper()		{ return (flags & ACC_WRAPPER) != 0; }

	// Method specific
	public boolean isMultiMethod()	{ return (flags & ACC_MULTIMETHOD) != 0; }
	public boolean isVirtualStatic(){ return (flags & ACC_VIRTUALSTATIC) != 0; }
	public boolean isVarArgs()		{ return (flags & ACC_VARARGS) != 0; }
	public boolean isRuleMethod()	{ return (flags & ACC_RULEMETHOD) != 0; }
	public boolean isOperatorMethod()	{ return (flags & ACC_OPERATORMETHOD) != 0; }
	public boolean isGenPostCond()	{ return (flags & ACC_GENPOSTCOND) != 0; }
	public boolean isNeedFieldInits()	{ return (flags & ACC_NEEDFIELDINITS) != 0; }
	public boolean isInvariantMethod()	{ return (flags & ACC_INVARIANT_METHOD) != 0; }
	public boolean isLocalMethod()		{ return (flags & ACC_LOCAL_METHOD) != 0; }
	public boolean isProduction()	{ return (flags & ACC_PRODUCTION) != 0; }

	// Var specific
	public boolean isNeedProxy()	{ return (flags & ACC_NEED_PROXY) != 0; }
	public boolean isNeedRefProxy()	{ return (flags & ACC_NEED_REFPROXY) != 0; }
//	public boolean isPrologVar()	{ return (flags & ACC_PROLOGVAR) != 0; }
//	public boolean isLocalPrologVar()	{ return (flags & ACC_LOCALPROLOGVAR) != 0; }
	public boolean isLocalRuleVar()	{ return (flags & ACC_LOCALRULEVAR) != 0; }
//	public boolean isLocalPrologForVar()	{ return (flags & ACC_LOCALPROLOGFORVAR) != 0; }
	public boolean isClosureProxy()	{ return (flags & ACC_CLOSURE_PROXY) != 0; }
	public boolean isInitWrapper()	{ return (flags & ACC_INIT_WRAPPER) != 0; }

	// Field specific
	public boolean isVirtual()		{ return (flags & ACC_VIRTUAL) != 0; }
	public boolean isPackerField()	{ return (flags & ACC_PACKER_FIELD) != 0; }
	public boolean isPackedField()	{ return (flags & ACC_PACKED_FIELD) != 0; }

	// Var/field
	public boolean isForward()		{ return (flags & ACC_FORWARD) != 0; }

	// Expr specific
	public boolean isUseNoProxy()	{ return (flags & ACC_USE_NOPROXY) != 0; }
	public boolean isAsField()		{ return (flags & ACC_AS_FIELD) != 0; }
	public boolean isConstExpr()	{ return (flags & ACC_CONSTEXPR) != 0; }
	public boolean isTryResolved()	{ return (flags & ACC_TRYRESOLVED) != 0; }
	public boolean isGenResolve()	{ return (flags & ACC_GENRESOLVE) != 0; }
	public boolean isForWrapper()	{ return (flags & ACC_FOR_WRAPPER) != 0; }

	// Statement specific
	public boolean isAbrupted()	{ return (flags & ACC_ABRUPTED) != 0; }
	public boolean isBreaked()	{ return (flags & ACC_BREAKED) != 0; }
	public boolean isMethodAbrupted()	{ return (flags & ACC_METHODABRUPTED) != 0; }
	public boolean isAutoReturnable()	{ return (flags & ACC_AUTORETURNABLE) != 0; }
	public boolean isBreakTarget()	{ return (flags & ACC_BREAK_TARGET) != 0; }
	public boolean isProductionSome()	{ return (flags & ACC_PRODUCTION_SOME) != 0; }
	public boolean isProductionAny()	{ return (flags & ACC_PRODUCTION_ANY) != 0; }
	public boolean isProductionMaybe()	{ return (flags & ACC_PRODUCTION_MAYBE) != 0; }

	// General
	public boolean isAccessedFromInner()	{ return (flags & ACC_FROM_INNER) != 0; }
	public boolean isResolved()		{ return (flags & ACC_RESOLVED) != 0; }
	public boolean isHidden()		{ return (flags & ACC_HIDDEN) != 0; }
	public boolean isBad()			{ return (flags & ACC_BAD) != 0; }

	public void setPublic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PUBLIC set to "+on+" from "+((flags & ACC_PUBLIC)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PUBLIC;
	}
	public void setPrivate(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIVATE set to "+on+" from "+((flags & ACC_PRIVATE)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PRIVATE;
	}
	public void setProtected(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROTECTED set to "+on+" from "+((flags & ACC_PROTECTED)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PROTECTED;
	}
	public void setStatic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATIC set to "+on+" from "+((flags & ACC_STATIC)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_STATIC;
		else flags &= ~ACC_STATIC;
	}
	public void setFinal(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FINAL set to "+on+" from "+((flags & ACC_FINAL)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FINAL;
		else flags &= ~ACC_FINAL;
	}
	public void setSynchronized(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNCHRONIZED set to "+on+" from "+((flags & ACC_SYNCHRONIZED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SYNCHRONIZED;
		else flags &= ~ACC_SYNCHRONIZED;
	}
	public void setVolatile(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VOLATILE set to "+on+" from "+((flags & ACC_VOLATILE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VOLATILE;
		else flags &= ~ACC_VOLATILE;
	}
	public void setTransient(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRANSIENT set to "+on+" from "+((flags & ACC_TRANSIENT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_TRANSIENT;
		else flags &= ~ACC_TRANSIENT;
	}
	public void setNative(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NATIVE set to "+on+" from "+((flags & ACC_NATIVE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NATIVE;
		else flags &= ~ACC_NATIVE;
	}
	public void setInterface(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INTERFACE set to "+on+" from "+((flags & ACC_INTERFACE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INTERFACE;
		else flags &= ~ACC_INTERFACE;
	}
	public void setAbstract(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABSTRACT set to "+on+" from "+((flags & ACC_ABSTRACT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABSTRACT;
		else flags &= ~ACC_ABSTRACT;
	}
	public void setSuper(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SUPER set to "+on+" from "+((flags & ACC_SUPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SUPER;
		else flags &= ~ACC_SUPER;
	}

	// Struct specific
	public void setPackage(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKAGE set to "+on+" from "+((flags & ACC_PACKAGE)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE);
		if( on ) flags |= ACC_PACKAGE;
	}
	public void setArgument(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ARGUMENT set to "+on+" from "+((flags & ACC_ARGUMENT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ARGUMENT;
		else flags &= ~ACC_ARGUMENT;
	}
	public void setPizzaCase(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PIZZACASE set to "+on+" from "+((flags & ACC_PIZZACASE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PIZZACASE;
		else flags &= ~ACC_PIZZACASE;
	}
	public void setLocal(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCAL set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCAL;
		else flags &= ~ACC_LOCAL;
	}
	public void setAnonymouse(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ANONYMOUSE set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ANONYMOUSE;
		else flags &= ~ACC_ANONYMOUSE;
	}
	public void setHasCases(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_HAS_CASES set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_HAS_CASES;
		else flags &= ~ACC_HAS_CASES;
	}
	public void setVerified(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VERIFIED set to "+on+" from "+((flags & ACC_VERIFIED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VERIFIED;
		else flags &= ~ACC_VERIFIED;
	}
	public void setMembersGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_MEMBERS_GENERATED set to "+on+" from "+((flags & ACC_MEMBERS_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_MEMBERS_GENERATED;
		else flags &= ~ACC_MEMBERS_GENERATED;
	}
	public void setStatementsGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATEMENTS_GENERATED set to "+on+" from "+((flags & ACC_STATEMENTS_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_STATEMENTS_GENERATED;
		else flags &= ~ACC_STATEMENTS_GENERATED;
	}
	public void setGenerated(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENERATED set to "+on+" from "+((flags & ACC_GENERATED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENERATED;
		else flags &= ~ACC_GENERATED;
	}
	public void setEnum(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ENUM set to "+on+" from "+((flags & ACC_ENUM)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE|ACC_ENUM);
		if( on ) flags |= ACC_ENUM;
	}
	public void setSyntax(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNTAX set to "+on+" from "+((flags & ACC_SYNTAX)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_INTERFACE|ACC_PACKAGE|ACC_ENUM|ACC_SYNTAX);
		if( on ) flags |= ACC_SYNTAX;
	}
	public void setPrimitiveEnum(boolean on) {
		assert(this instanceof Struct && this.isEnum(),"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIMITIVE_ENUM set to "+on+" from "+((flags & ACC_PRIMITIVE_ENUM)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~ACC_PRIMITIVE_ENUM;
		if( on ) flags |= ACC_PRIMITIVE_ENUM;
	}
	public void setWrapper(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_WRAPPER set to "+on+" from "+((flags & ACC_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_WRAPPER;
		else flags &= ~ACC_WRAPPER;
	}

	// Method specific
	public void setMultiMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_MULTIMETHOD set to "+on+" from "+((flags & ACC_MULTIMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_MULTIMETHOD;
		else flags &= ~ACC_MULTIMETHOD;
	}
	public void setVirtualStatic(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VIRTUALSTATIC set to "+on+" from "+((flags & ACC_VIRTUALSTATIC)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VIRTUALSTATIC;
		else flags &= ~ACC_VIRTUALSTATIC;
	}
	public void setVarArgs(boolean on) {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VARARGS set to "+on+" from "+((flags & ACC_VARARGS)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VARARGS;
		else flags &= ~ACC_VARARGS;
	}
	public void setRuleMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_RULEMETHOD set to "+on+" from "+((flags & ACC_RULEMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_RULEMETHOD;
		else flags &= ~ACC_RULEMETHOD;
	}
	public void setOperatorMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_OPERATORMETHOD set to "+on+" from "+((flags & ACC_OPERATORMETHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_OPERATORMETHOD;
		else flags &= ~ACC_OPERATORMETHOD;
	}
	public void setGenPostCond(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENPOSTCOND set to "+on+" from "+((flags & ACC_GENPOSTCOND)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENPOSTCOND;
		else flags &= ~ACC_GENPOSTCOND;
	}
	public void setNeedFieldInits(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEEDFIELDINITS set to "+on+" from "+((flags & ACC_NEEDFIELDINITS)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEEDFIELDINITS;
		else flags &= ~ACC_NEEDFIELDINITS;
	}
	public void setInvariantMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INVARIANT_METHOD set to "+on+" from "+((flags & ACC_INVARIANT_METHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INVARIANT_METHOD;
		else flags &= ~ACC_INVARIANT_METHOD;
	}
	public void setLocalMethod(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCAL_METHOD set to "+on+" from "+((flags & ACC_LOCAL_METHOD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCAL_METHOD;
		else flags &= ~ACC_LOCAL_METHOD;
	}
	public void setProduction(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION set to "+on+" from "+((flags & ACC_PRODUCTION)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION;
		else flags &= ~ACC_PRODUCTION;
	}

	// Var specific
	public void setNeedProxy(boolean on) {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEED_PROXY set to "+on+" from "+((flags & ACC_NEED_PROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEED_PROXY;
		else flags &= ~ACC_NEED_PROXY;
	}
	public void setNeedRefProxy(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NEED_REFPROXY set to "+on+" from "+((flags & ACC_NEED_REFPROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NEED_PROXY | ACC_NEED_REFPROXY;
		else flags &= ~ACC_NEED_REFPROXY;
	}
//	public void setPrologVar(boolean on) {
//		assert(this instanceof Var,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROLOGVAR set to "+on+" from "+((flags & ACC_PROLOGVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_PROLOGVAR;
//		else flags &= ~ACC_PROLOGVAR;
//	}
//	public void setLocalPrologVar(boolean on) {
//		assert(this instanceof Var || this instanceof kiev.parser.ASTFormalParameter,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALPROLOGVAR set to "+on+" from "+((flags & ACC_LOCALPROLOGVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_LOCALPROLOGVAR;
//		else flags &= ~ACC_LOCALPROLOGVAR;
//	}
	public void setLocalRuleVar(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALRULEVAR set to "+on+" from "+((flags & ACC_LOCALRULEVAR)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_LOCALRULEVAR;
		else flags &= ~ACC_LOCALRULEVAR;
	}
//	public void setLocalPrologForVar(boolean on) {
//		assert(this instanceof Var,"For node "+this.getClass());
//		trace(Kiev.debugFlags,"Member "+this+" flag ACC_LOCALPROLOGFORVAR set to "+on+" from "+((flags & ACC_LOCALPROLOGFORVAR)!=0)+", now 0x"+Integer.toHexString(flags));
//		if( on ) flags |= ACC_LOCALPROLOGFORVAR;
//		else flags &= ~ACC_LOCALPROLOGFORVAR;
//	}
	public void setClosureProxy(boolean on) {
		assert(this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_CLOSURE_PROXY set to "+on+" from "+((flags & ACC_CLOSURE_PROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_CLOSURE_PROXY;
		else flags &= ~ACC_CLOSURE_PROXY;
	}

	// Var/field specific
	public void setInitWrapper(boolean on) {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INIT_WRAPPER set to "+on+" from "+((flags & ACC_INIT_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INIT_WRAPPER;
		else flags &= ~ACC_INIT_WRAPPER;
	}


	// Field specific
	public void setVirtual(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VIRTUAL set to "+on+" from "+((flags & ACC_VIRTUAL)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VIRTUAL;
		else flags &= ~ACC_VIRTUAL;
	}
	public void setPackerField(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKER_FIELD set to "+on+" from "+((flags & ACC_PACKER_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PACKER_FIELD;
		else flags &= ~ACC_PACKER_FIELD;
	}
	public void setPackedField(boolean on) {
		assert(this instanceof Field,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PACKED_FIELD set to "+on+" from "+((flags & ACC_PACKED_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PACKED_FIELD;
		else flags &= ~ACC_PACKED_FIELD;
	}

	// Var/field
	public void setForward(boolean on) {
		assert(this instanceof Field || this instanceof Var,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FORWARD set to "+on+" from "+((flags & ACC_FORWARD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FORWARD;
		else flags &= ~ACC_FORWARD;
	}

	// Expr specific
	public void setUseNoProxy(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_USE_NOPROXY set to "+on+" from "+((flags & ACC_USE_NOPROXY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_USE_NOPROXY;
		else flags &= ~ACC_USE_NOPROXY;
	}
	public void setAsField(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_AS_FIELD set to "+on+" from "+((flags & ACC_AS_FIELD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_AS_FIELD;
		else flags &= ~ACC_AS_FIELD;
	}
	public void setConstExpr(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_CONSTEXPR set to "+on+" from "+((flags & ACC_CONSTEXPR)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_CONSTEXPR;
		else flags &= ~ACC_CONSTEXPR;
	}
	public void setTryResolved(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRYRESOLVED set to "+on+" from "+((flags & ACC_TRYRESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_TRYRESOLVED;
		else flags &= ~ACC_TRYRESOLVED;
	}
	public void setGenResolve(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_GENRESOLVE set to "+on+" from "+((flags & ACC_GENRESOLVE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_GENRESOLVE;
		else flags &= ~ACC_GENRESOLVE;
	}
	public void setForWrapper(boolean on) {
		assert(this instanceof Expr,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FOR_WRAPPER set to "+on+" from "+((flags & ACC_FOR_WRAPPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FOR_WRAPPER;
		else flags &= ~ACC_FOR_WRAPPER;
	}

	// Statement specific
	public void setAbrupted(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABRUPTED set to "+on+" from "+((flags & ACC_ABRUPTED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABRUPTED;
		else flags &= ~ACC_ABRUPTED;
	}
	public void setBreaked(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BREAKED set to "+on+" from "+((flags & ACC_BREAKED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BREAKED;
		else flags &= ~ACC_BREAKED;
	}
	public void setMethodAbrupted(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_METHODABRUPTED set to "+on+" from "+((flags & ACC_METHODABRUPTED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABRUPTED | ACC_METHODABRUPTED;
		else flags &= ~ACC_METHODABRUPTED;
	}
	public void setAutoReturnable(boolean on) {
		assert(this instanceof Statement || this instanceof CaseLabel || this instanceof CatchInfo,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_AUTORETURNABLE set to "+on+" from "+((flags & ACC_AUTORETURNABLE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_AUTORETURNABLE;
		else flags &= ~ACC_AUTORETURNABLE;
	}
	public void setBreakTarget(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BREAK_TARGET set to "+on+" from "+((flags & ACC_BREAK_TARGET)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BREAK_TARGET;
		else flags &= ~ACC_BREAK_TARGET;
	}
	public void setProductionSome(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_SOME set to "+on+" from "+((flags & ACC_PRODUCTION_SOME)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_SOME;
		else flags &= ~ACC_PRODUCTION_SOME;
	}
	public void setProductionAny(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_ANY set to "+on+" from "+((flags & ACC_PRODUCTION_ANY)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_ANY;
		else flags &= ~ACC_PRODUCTION_ANY;
	}
	public void setProductionMaybe(boolean on) {
		assert(this instanceof Statement,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRODUCTION_MAYBE set to "+on+" from "+((flags & ACC_PRODUCTION_MAYBE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_PRODUCTION_MAYBE;
		else flags &= ~ACC_PRODUCTION_MAYBE;
	}

	// General
	public void setAccessedFromInner(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FROM_INNER set to "+on+" from "+((flags & ACC_FROM_INNER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FROM_INNER;
		else flags &= ~ACC_FROM_INNER;
	}
	public void setResolved(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_RESOLVED set to "+on+" from "+((flags & ACC_RESOLVED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_RESOLVED;
		else flags &= ~ACC_RESOLVED;
	}
	public void setHidden(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_HIDDEN set to "+on+" from "+((flags & ACC_HIDDEN)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_HIDDEN;
		else flags &= ~ACC_HIDDEN;
	}
	public void setBad(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_BAD set to "+on+" from "+((flags & ACC_BAD)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_BAD;
		else flags &= ~ACC_BAD;
	}

}

public abstract class Expr extends ASTNode {

	public static Expr[] emptyArray = new Expr[0];

	public Expr(int pos) { super(pos); }

	public Expr(int pos, ASTNode parent) { super(pos,parent); }

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public /*abstract*/ Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void		generate(Type reqType) {
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation");
	}

	public int		getPriority() { return 0; }
	public boolean	isConstantExpr() { return false; }
	public Object	getConstValue() {
    	throw new RuntimeException("Request for constant value of non-constant expression");
    }
	public /*abstract*/ ASTNode	resolve(Type reqType) {
		throw new CompilerException(pos,"Resolve call for node "+getClass());
	}
	public /*abstract*/ Expr tryResolve(Type reqType) {
		ASTNode n = resolve(reqType);
		if( n instanceof Expr ) return (Expr)n;
		else return new WrapedExpr(pos,n,reqType);
	}
	public Expr resolveExpr(Type reqType) {
		ASTNode e = tryResolve(reqType);
		if( e == null )
			throw new CompilerException(pos,"Unresolved expression "+this);
		Expr expr = null;
		if( e instanceof Expr ) expr = (Expr)e;
		if( e instanceof Struct ) expr = toExpr((Struct)e,reqType,pos,parent);
		if( e instanceof WrapedExpr ) expr = toExpr((Struct)((WrapedExpr)e).expr,reqType,pos,parent);
		if( expr == null )
			throw new CompilerException(e.pos,"Is not an expression");
		else if( reqType == null || reqType == Type.tpVoid )
			return expr;
//		if( reqType == Type.tpRule ) reqType = Type.tpBoolean;
		Type et = expr.getType();
//		if( et.isBoolean() && reqType.isBoolean() ) return expr;
		if( et.isInstanceOf(reqType) ) return expr;
		if( et.isReference() && reqType.isBoolean() )
			return new BinaryBooleanExpr(pos,BinaryOperator.Equals,expr,new ConstExpr(pos,null));
		if( et.isAutoCastableTo(reqType)
		 || et.isNumber() && reqType.isNumber()
		) return new CastExpr(pos,reqType,expr).tryResolve(reqType);
		throw new CompilerException(e.pos,"Expression "+expr+" is not auto-castable to type "+reqType);
	}
	public static Expr toExpr(Struct e, Type reqType, int pos, ASTNode parent) {
		if( e.isPizzaCase() ) {
			// Pizza case may be casted to int or to itself or super-class
			PizzaCaseAttr case_attr;
			if( e.generated_from != null )
				case_attr = (PizzaCaseAttr)(e.generated_from).getAttr(attrPizzaCase);
			else
				case_attr = (PizzaCaseAttr)(e).getAttr(attrPizzaCase);
			if( case_attr == null )
				throw new RuntimeException("Internal error - can't find case_attr");
			e = Type.getRealType(reqType,e.type).clazz;
			if( !(reqType.isInteger() || e.instanceOf(reqType.clazz)) )
				throw new CompilerException(pos,"Pizza case "+e+" cannot be casted to type "+reqType);
			if( case_attr.casefields.length != 0 )
				throw new CompilerException(pos,"Empty constructor for pizza case "+e+" not found");
			if( reqType.isInteger() ) {
				Expr expr = (Expr)new ConstExpr(pos,Kiev.newInteger(case_attr.caseno)).resolve(reqType);
				if( reqType != Type.tpInt )
					expr = (Expr)new CastExpr(pos,reqType,expr).resolve(reqType);
				return expr;
			}
			// Now, check we need add type arguments
			Type tp = Type.getRealType(reqType,e.type);
			return (Expr)new NewExpr(pos,tp,Expr.emptyArray).resolve(reqType);
/*			if( case_attr != null && case_attr.casefields.length == 0 ) {
				Field f = (Field)((Struct)e).resolveName(nameTagSelf);
				if( f != null ) {
					Expr ex = new StaticFieldAccessExpr(pos,(Struct)e,(Field)f);
					ex.parent = parent;
					ex = ex.tryResolve(reqType);
					return ex;
				} else {
					throw new RuntimeException("Field "+nameTagSelf+" not found in cased class "+e);
				}
			}
*/		}
		throw new CompilerException(pos,"Expr "+e+" is not a class's case with no fields");
	}
}

public class WrapedExpr extends Expr {

	public ASTNode	expr;
	public Type		base_type;
	public WrapedExpr(int pos, ASTNode expr) {
		super(pos);
		this.expr = expr;
	}
	public WrapedExpr(int pos, ASTNode expr, Type t) {
		super(pos);
		this.expr = expr;
		base_type = t;
	}
	public int		getPriority() { return 256; }
	public Type getType() {
		if( expr instanceof Type ) return Type.getRealType(base_type,(Type)expr);
		if( expr instanceof Struct ) return Type.getRealType(base_type,((Struct)expr).type);
		if( expr instanceof kiev.parser.ASTType ) return Type.getRealType(base_type,((kiev.parser.ASTType)expr).getType());
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
	public ASTNode resolve(Type reqType) {
		if( expr instanceof Type ) return expr;
		if( expr instanceof Struct ) return expr;
		if( expr instanceof kiev.parser.ASTType ) return ((kiev.parser.ASTType)expr).getType();
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
}

public abstract class BooleanExpr extends Expr {

	public BooleanExpr(int pos) { super(pos); }

	public BooleanExpr(int pos, ASTNode parent) { super(pos, parent); }

	public Type getType() { return Type.tpBoolean; }

	public void generate(Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating BooleanExpr: "+this);
		PassInfo.push(this);
		try {
			CodeLabel label_true = Code.newLabel();
			CodeLabel label_false = Code.newLabel();

			generate_iftrue(label_true);
			Code.addConst(0);
			Code.addInstr(Instr.op_goto,label_false);
			Code.addInstr(Instr.set_label,label_true);
			Code.addConst(1);
			Code.addInstr(Instr.set_label,label_false);
			if( reqType == Type.tpVoid ) Code.addInstr(Instr.op_pop);
		} finally { PassInfo.pop(this); }
	}

	public abstract void generate_iftrue(CodeLabel label);
	public abstract void generate_iffalse(CodeLabel label);
}

public abstract class LvalueExpr extends Expr {

	public LvalueExpr(int pos) { super(pos); }

	public LvalueExpr(int pos, ASTNode parent) { super(pos, parent); }

	public void generate(Type reqType) {
		PassInfo.push(this);
		try {
			generateLoad();
			if( reqType == Type.tpVoid )
				Code.addInstr(Instr.op_pop);
		} finally { PassInfo.pop(this); }
	}

	public int		getPriority() { return 256; }

	/** Just load value referenced by lvalue */
	public abstract void generateLoad();

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateLoadDup();

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateAccess();

	/** Stores value using previously duped info */
	public abstract void generateStore();

	/** Stores value using previously duped info, and put stored value in stack */
	public abstract void generateStoreDupValue();
}

public abstract class Statement extends ASTNode {

	public static Statement[] emptyArray = new Statement[0];

	public Statement(int pos, ASTNode parent) { super(pos, parent); }

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public void		generate(Type reqType) {
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation");
	}

	public ASTNode	resolve(Type reqType) { return this; }

}
public final class NArr<N extends ASTNode> {

    private final N 	$parent;
	private N[]		$nodes;
	
	public NArr(ASTNode parent) {
		this.$parent = parent;
		this.$nodes = new N[0];
	}
	
	public NArr(int size, ASTNode parent) {
		this.$parent = parent;
		this.$nodes = new N[size];
	}

	public int size()
		alias length
		alias get$size
		alias get$length
	{
		return $nodes.length;
	}

	public void cleanup() {
		$parent = null;
		int sz = $nodes.length;
		for (int i=0; i < sz; i++)
			$nodes[i].cleanup();
		$nodes = null;
	};
	
	public final N get(int idx)
		alias at
		alias operator(210,xfy,[])
	{
		return $nodes[idx];
	}
	
	public N set(int idx, N node)
		alias operator(210,lfy,[])
	{
		$nodes[idx] = node;
		return node;
	}

	public N add(N node)
		alias append
	{
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		return node;
	}

	public N insert(int idx, N node)
	{
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i+1] = $nodes[i];
		tmp[idx] = node;
		$nodes = tmp;
		return node;
	}

	public void del(int idx)
	{
		int sz = $nodes.length;
		N[] tmp = new N[sz-1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i-1] = $nodes[i];
		$nodes = tmp;
	}

	public void delAll() {
		if (this.$nodes.length == 0)
			return;
		this.$nodes = new N[0];
	};
	
	public boolean contains(N node) {
		for (int i=0; i < $nodes.length; i++) {
			if ($nodes[i].equals(node))
				return true;
		}
		return false;
	}

	public Enumeration<N> elements() {
		return new Enumeration<N>() {
			int current;
			public boolean hasMoreElements() { return current < NArr.this.size(); }
			public A nextElement() {
				if ( current < size() ) return NArr.this[current++];
				throw new NoSuchElementException(Integer.toString(NArr.this.size()));
			}
		};
	}

}


public interface SetBody {
	public boolean setBody(Statement body);
}

public class CompilerException extends RuntimeException {
	public int		pos;
	public Struct	clazz;
	public CompilerException(int pos, String msg) {
		super(msg);
		this.pos = pos;
		this.clazz = PassInfo.clazz;
	}
}

