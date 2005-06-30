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
	passStartCleanup		   ,	// start of compilation or cleanup before next incremental compilation
	passCreateTopStruct		   ,	// create top-level Struct
	passProcessSyntax		   ,	// process syntax - some import, typedef, operator and macro
	passArgumentInheritance	   ,	// inheritance of type arguments
	passStructInheritance	   ,	// inheritance of classe/interfaces/structures
	passCreateMembers		   ,	// create declared members of structures
	passAutoProxyMethods	   ,	// autoProxyMethods()
	passResolveImports		   ,	// recolve import static for import of fields and methods
	passResolveFinalFields	   ,	// resolve final fields, to find out if they are constants
	passGenerate			   		// resolve, generate and so on - each file separatly
};

@node
public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];

	private static int		parserAddrIdx;

	public int			pos;
    public ASTNode		parent;
	public int			flags;
	
	public virtual packed:1,flags,13 boolean is_struct_annotation; // struct
	public virtual packed:1,flags,14 boolean is_struct_java_enum;  // struct
	public virtual packed:1,flags,14 boolean is_fld_enum;        // field
	// Flags temporary used with java flags
	public virtual packed:1,flags,16 boolean is_forward;         // var/field
	public virtual packed:1,flags,17 boolean is_fld_virtual;     // field
	public virtual packed:1,flags,16 boolean is_mth_multimethod; // method
	public virtual packed:1,flags,17 boolean is_mth_varargs;     // method
	public virtual packed:1,flags,18 boolean is_mth_rule;        // method
	public virtual packed:1,flags,19 boolean is_mth_invariant;   // method
	public virtual packed:1,flags,16 boolean is_struct_package;    // struct
	public virtual packed:1,flags,17 boolean is_struct_argument;   // struct
	public virtual packed:1,flags,18 boolean is_struct_pizza_case; // struct
	public virtual packed:1,flags,19 boolean is_struct_enum;       // struct
	public virtual packed:1,flags,20 boolean is_struct_syntax;     // struct
	public virtual packed:1,flags,21 boolean is_struct_wrapper;    // struct

	public int			compileflags;

	// Structures	
	public virtual packed:1,compileflags,16 boolean is_struct_local;
	public virtual packed:1,compileflags,17 boolean is_struct_anomymouse;
	public virtual packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
	public virtual packed:1,compileflags,19 boolean is_struct_verified;
	public virtual packed:1,compileflags,20 boolean is_struct_members_generated;
	public virtual packed:1,compileflags,21 boolean is_struct_statements_generated;
	public virtual packed:1,compileflags,22 boolean is_struct_generated;
	public virtual packed:1,compileflags,23 boolean is_struct_enum_primitive;
	
	// Expression flags
	public virtual packed:1,compileflags,16 boolean is_expr_use_no_proxy;
	public virtual packed:1,compileflags,17 boolean is_expr_as_field;
	//public virtual packed:1,compileflags,18 boolean is_expr_const_expr;
	public virtual packed:1,compileflags,19 boolean is_expr_try_resolved;
	public virtual packed:1,compileflags,20 boolean is_expr_gen_resolved;
	public virtual packed:1,compileflags,21 boolean is_expr_for_wrapper;
	
	// Method flags
	public virtual packed:1,compileflags,17 boolean is_mth_virtual_static;
	public virtual packed:1,compileflags,20 boolean is_mth_operator;
	public virtual packed:1,compileflags,21 boolean is_mth_gen_post_cond;
	public virtual packed:1,compileflags,22 boolean is_mth_need_fields_init;
	public virtual packed:1,compileflags,24 boolean is_mth_local;
	
	// Var/field
	public virtual packed:1,compileflags,16 boolean is_init_wrapper;
	public virtual packed:1,compileflags,17 boolean is_need_proxy;
	// Var specific
	public virtual packed:1,compileflags,18 boolean is_var_need_ref_proxy; // also sets is_var_need_proxy
	public virtual packed:1,compileflags,19 boolean is_var_local_rule_var;
	public virtual packed:1,compileflags,20 boolean is_var_closure_proxy;

	// Field specific
	public virtual packed:1,compileflags,18 boolean is_fld_packer;
	public virtual packed:1,compileflags,19 boolean is_fld_packed;

	// Statement flags
	public virtual packed:1,compileflags,16 boolean is_stat_abrupted;
	public virtual packed:1,compileflags,17 boolean is_stat_breaked;
	public virtual packed:1,compileflags,18 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public virtual packed:1,compileflags,19 boolean is_stat_auto_returnable;
	public virtual packed:1,compileflags,20 boolean is_stat_break_target;
	// General flags
	public virtual packed:1,compileflags,28 boolean is_accessed_from_inner;
	public virtual packed:1,compileflags,29 boolean is_resolved;
	public virtual packed:1,compileflags,30 boolean is_hidden;
	public virtual packed:1,compileflags,31 boolean is_bad;

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

	//
	// Struct specific
	//
	public boolean isClazz()		{
		return !isPackage() && !isInterface() && ! isArgument();
	}
	
	// package	
	public final boolean get$is_struct_package()  alias isPackage  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_package;
	}
	public final void set$is_struct_package(boolean on) alias setPackage {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isInterface() && ! isEnum() && !isSyntax()));
		this.is_struct_package = on;
	}
	// a class's argument	
	public final boolean get$is_struct_argument()  alias isArgument  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_argument;
	}
	public final void set$is_struct_argument(boolean on) alias setArgument {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_argument = on;
	}
	// a class's argument	
	public final boolean get$is_struct_pizza_case()  alias isPizzaCase  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_pizza_case;
	}
	public final void set$is_struct_pizza_case(boolean on) alias setPizzaCase {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_pizza_case = on;
	}
	// a local (in method) class	
	public final boolean get$is_struct_local()  alias isLocal  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_local;
	}
	public final void set$is_struct_local(boolean on) alias setLocal {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_local = on;
	}
	// an anonymouse (unnamed) class	
	public final boolean get$is_struct_anomymouse()  alias isAnonymouse  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_anomymouse;
	}
	public final void set$is_struct_anomymouse(boolean on) alias setAnonymouse {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_anomymouse = on;
	}
	// has pizza cases
	public final boolean get$is_struct_has_pizza_cases()  alias isHasCases  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_has_pizza_cases;
	}
	public final void set$is_struct_has_pizza_cases(boolean on) alias setHasCases {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_has_pizza_cases = on;
	}
	// verified
	public final boolean get$is_struct_verified()  alias isVerified  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_verified;
	}
	public final void set$is_struct_verified(boolean on) alias setVerified {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_verified = on;
	}
	// indicates that structure members were generated
	public final boolean get$is_struct_members_generated()  alias isMembersGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_members_generated;
	}
	public final void set$is_struct_members_generated(boolean on) alias setMembersGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_members_generated = on;
	}
	// indicates that statements in code were generated
	public final boolean get$is_struct_statements_generated()  alias isStatementsGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_statements_generated;
	}
	public final void set$is_struct_statements_generated(boolean on) alias setStatementsGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_statements_generated = on;
	}
	// indicates that the structrue was generared (from template)
	public final boolean get$is_struct_generated()  alias isGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_generated;
	}
	public final void set$is_struct_generated(boolean on) alias setGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_generated = on;
	}
	// kiev enum
	public final boolean get$is_struct_enum()  alias isEnum  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_enum;
	}
	public final void set$is_struct_enum(boolean on) alias setEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isInterface() && !isSyntax()));
		this.is_struct_enum = on;
	}
	// kiev annotation
	public final boolean get$is_struct_annotation()  alias isAnnotation  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_annotation;
	}
	public final void set$is_struct_annotation(boolean on) alias setAnnotation {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isSyntax()));
		this.is_struct_annotation = on;
		if (on) this.setInterface(true);
	}
	// java enum
	public final boolean get$is_struct_java_enum()  alias isJavaEnum  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_java_enum;
	}
	public final void set$is_struct_java_enum(boolean on) alias setJavaEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_java_enum = on;
	}
	// kiev enum that extends int
	public final boolean get$is_struct_enum_primitive()  alias isPrimitiveEnum  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_enum_primitive;
	}
	public final void set$is_struct_enum_primitive(boolean on) alias setPrimitiveEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_enum_primitive = on;
	}
	// kiev syntax
	public final boolean get$is_struct_syntax()  alias isSyntax  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_syntax;
	}
	public final void set$is_struct_syntax(boolean on) alias setSyntax {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && ! isEnum()));
		this.is_struct_syntax = on;
	}
	// kiev wrapper class
	public final boolean get$is_struct_wrapper()  alias isWrapper  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_wrapper;
	}
	public final void set$is_struct_wrapper(boolean on) alias setWrapper {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_wrapper = on;
	}

	//
	// Method specific
	//

	// multimethod	
	public final boolean get$is_mth_multimethod()  alias isMultiMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_multimethod;
	}
	public final void set$is_mth_multimethod(boolean on) alias setMultiMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_multimethod = on;
	}
	// virtual static method	
	public final boolean get$is_mth_virtual_static()  alias isVirtualStatic  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_virtual_static;
	}
	public final void set$is_mth_virtual_static(boolean on) alias setVirtualStatic {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_virtual_static = on;
	}
	// method with variable number of arguments	
	public final boolean get$is_mth_varargs()  alias isVarArgs  {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration || this instanceof kiev.parser.ASTRuleDeclaration,"For node "+this.getClass());
		return this.is_mth_varargs;
	}
	public final void set$is_mth_varargs(boolean on) alias setVarArgs {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration || this instanceof kiev.parser.ASTRuleDeclaration,"For node "+this.getClass());
		this.is_mth_varargs = on;
	}
	// logic rule method	
	public final boolean get$is_mth_rule()  alias isRuleMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_rule;
	}
	public final void set$is_mth_rule(boolean on) alias setRuleMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_rule = on;
	}
	// method with attached operator	
	public final boolean get$is_mth_operator()  alias isOperatorMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_operator;
	}
	public final void set$is_mth_operator(boolean on) alias setOperatorMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_operator = on;
	}
	// needs to call post-condition before return	
	public final boolean get$is_mth_gen_post_cond()  alias isGenPostCond  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_gen_post_cond;
	}
	public final void set$is_mth_gen_post_cond(boolean on) alias setGenPostCond {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_gen_post_cond = on;
	}
	// need fields initialization	
	public final boolean get$is_mth_need_fields_init()  alias isNeedFieldInits  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_need_fields_init;
	}
	public final void set$is_mth_need_fields_init(boolean on) alias setNeedFieldInits {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_need_fields_init = on;
	}
	// a method generated as invariant	
	public final boolean get$is_mth_invariant()  alias isInvariantMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_invariant;
	}
	public final void set$is_mth_invariant(boolean on) alias setInvariantMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_invariant = on;
	}
	// a local method (closure code or inner method)	
	public final boolean get$is_mth_local()  alias isLocalMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_local;
	}
	public final void set$is_mth_local(boolean on) alias setLocalMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_local = on;
	}

	//
	// Var/field
	//
	
	// use no proxy	
	public final boolean get$is_forward()  alias isForward  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_forward;
	}
	public final void set$is_forward(boolean on) alias setForward {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_forward = on;
	}
	// init wrapper
	public final boolean get$is_init_wrapper()  alias isInitWrapper  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_init_wrapper;
	}
	public final void set$is_init_wrapper(boolean on) alias setInitWrapper {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_init_wrapper = on;
	}
	// need a proxy access 
	public final boolean get$is_need_proxy()  alias isNeedProxy  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_need_proxy;
	}
	public final void set$is_need_proxy(boolean on) alias setNeedProxy {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_need_proxy = on;
	}

	// Var specific
	
	// need a reference proxy access 
	public final boolean get$is_var_need_ref_proxy()  alias isNeedRefProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_need_ref_proxy;
	}
	public final void set$is_var_need_ref_proxy(boolean on) alias setNeedRefProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_need_ref_proxy = on;
		if (on) this.is_need_proxy = on;
	}
	// is a local var in a rule 
	public final boolean get$is_var_local_rule_var()  alias isLocalRuleVar  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_local_rule_var;
	}
	public final void set$is_var_local_rule_var(boolean on) alias setLocalRuleVar {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_local_rule_var = on;
	}
	// closure proxy
	public final boolean get$is_var_closure_proxy()  alias isClosureProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_closure_proxy;
	}
	public final void set$is_var_closure_proxy(boolean on) alias setClosureProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_closure_proxy = on;
	}

	//
	// Field specific
	//

	// is a virtual field
	public final boolean get$is_fld_virtual()  alias isVirtual  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_virtual;
	}
	public final void set$is_fld_virtual(boolean on) alias setVirtual {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_virtual = on;
	}
	// is a field of enum
	public final boolean get$is_fld_enum()  alias isEnumField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_enum;
	}
	public final void set$is_fld_enum(boolean on) alias setEnumField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_enum = on;
	}
	// packer field (auto-generated for packed fields)
	public final boolean get$is_fld_packer()  alias isPackerField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packer;
	}
	public final void set$is_fld_packer(boolean on) alias setPackerField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_packer = on;
	}
	// packed field
	public final boolean get$is_fld_packed()  alias isPackedField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packed;
	}
	public final void set$is_fld_packed(boolean on) alias setPackedField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_packed = on;
	}

	//
	// Expr specific
	//

	// use no proxy	
	public final boolean get$is_expr_use_no_proxy()  alias isUseNoProxy  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_use_no_proxy;
	}
	public final void set$is_expr_use_no_proxy(boolean on) alias setUseNoProxy {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_use_no_proxy = on;
	}
	// use as field (disable setter/getter calls for virtual fields)
	public final boolean get$is_expr_as_field()  alias isAsField  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_as_field;
	}
	public final void set$is_expr_as_field(boolean on) alias setAsField {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_as_field = on;
	}
	// constant expression
	//public final boolean get$is_expr_const_expr()  alias isConstExpr  {
	//	assert(this instanceof Expr,"For node "+this.getClass());
	//	return this.is_expr_const_expr;
	//}
	//public final void set$is_expr_const_expr(boolean on) alias setConstExpr {
	//	assert(this instanceof Expr,"For node "+this.getClass());
	//	this.is_expr_const_expr = on;
	//}
	// tried to be resolved
	public final boolean get$is_expr_try_resolved()  alias isTryResolved  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_try_resolved;
	}
	public final void set$is_expr_try_resolved(boolean on) alias setTryResolved {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_try_resolved = on;
	}
	// resolved for generation
	public final boolean get$is_expr_gen_resolved()  alias isGenResolve  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_gen_resolved;
	}
	public final void set$is_expr_gen_resolved(boolean on) alias setGenResolve {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_gen_resolved = on;
	}
	// used bt for()
	public final boolean get$is_expr_for_wrapper()  alias isForWrapper  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_for_wrapper;
	}
	public final void set$is_expr_for_wrapper(boolean on) alias setForWrapper {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_for_wrapper = on;
	}

	//
	// Statement specific flags
	//
	
	// abrupted
	public final boolean get$is_stat_abrupted()  alias isAbrupted  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_abrupted;
	}
	public final void set$is_stat_abrupted(boolean on) alias setAbrupted {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_abrupted = on;
	}
	// breaked
	public final boolean get$is_stat_breaked()  alias isBreaked  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_breaked;
	}
	public final void set$is_stat_breaked(boolean on) alias setBreaked {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_breaked = on;
	}
	// method-abrupted
	public final boolean get$is_stat_method_abrupted()  alias isMethodAbrupted  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_method_abrupted;
	}
	public final void set$is_stat_method_abrupted(boolean on) alias setMethodAbrupted {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_method_abrupted = on;
		if (on) this.is_stat_abrupted = true;
	}
	// auto-returnable
	public final boolean get$is_stat_auto_returnable()  alias isAutoReturnable  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_auto_returnable;
	}
	public final void set$is_stat_auto_returnable(boolean on) alias setAutoReturnable {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_auto_returnable = on;
	}
	// break target
	public final boolean get$is_stat_break_target()  alias isBreakTarget  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_break_target;
	}
	public final void set$is_stat_auto_returable(boolean on) alias setBreakTarget {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_break_target = on;
	}

	//
	// General flags
	//

	// the (private) field/method/struct is accessed from inner class (and needs proxy access)
	public final boolean get$is_accessed_from_inner()  alias isAccessedFromInner  { return this.is_accessed_from_inner; }
	public final void set$is_accessed_from_inner(boolean on) alias setAccessedFromInner { this.is_accessed_from_inner = on; }
	// resolved
	public final boolean get$is_resolved()  alias isResolved  { return this.is_resolved; }
	public final void set$is_resolved(boolean on) alias setResolved { this.is_resolved = on; }
	// hidden
	public final boolean get$is_hidden()  alias isHidden  { return this.is_hidden; }
	public final void set$is_hidden(boolean on) alias setHidden { this.is_hidden = on; }
	// bad
	public final boolean get$is_bad()  alias isBad  { return this.is_bad; }
	public final void set$is_bad(boolean on) alias setBad { this.is_bad = on; }

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
		if( on ) flags |= ACC_INTERFACE | ACC_ABSTRACT;
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

}

@node
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

@node
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

@node
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

@node
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

@node
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

