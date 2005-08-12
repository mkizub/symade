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
	// just resolve
	public ASTNode resolve(Type reqType);
	// dump
	public Dumper  toJavaDecl(Dumper dmp);
};

public enum TopLevelPass {
	passStartCleanup		   ,	// start of compilation or cleanup before next incremental compilation
	passCreateTopStruct		   ,	// create top-level Struct
	passProcessSyntax		   ,	// process syntax - some import, typedef, operator and macro
	passArgumentInheritance	   ,	// inheritance of type arguments
	passStructInheritance	   ,	// inheritance of classe/interfaces/structures
	passCreateMembers		   ,	// create declared members of structures
	passAutoProxyMethods	   ,	// autoProxyMethods()
	passResolveImports		   ,	// recolve import static for import of fields and methods
	passResolveFinalFields	   ,	// resolve final fields, to find out if they are constants
	passResolveMetaDefaults	   ,	// resolved default values for meta-methods
	passResolveMetaValues	   ,	// resolve values in meta-data
	passGenerate			   		// resolve, generate and so on - each file separatly
};

@node
public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];

	private static int		parserAddrIdx;

	public int				pos;
    @ref(copyable=false)
	public ASTNode			parent;
    @ref(copyable=false)
	public AttrSlot			pslot;
	public int				flags;
	
	@virtual public virtual packed:1,flags,13 boolean is_struct_annotation; // struct
	@virtual public virtual packed:1,flags,14 boolean is_struct_java_enum;  // struct
	@virtual public virtual packed:1,flags,14 boolean is_fld_enum;        // field
	// Flags temporary used with java flags
	@virtual public virtual packed:1,flags,16 boolean is_forward;         // var/field
	@virtual public virtual packed:1,flags,17 boolean is_fld_virtual;     // field
	@virtual public virtual packed:1,flags,16 boolean is_mth_multimethod; // method
	@virtual public virtual packed:1,flags,17 boolean is_mth_varargs;     // method
	@virtual public virtual packed:1,flags,18 boolean is_mth_rule;        // method
	@virtual public virtual packed:1,flags,19 boolean is_mth_invariant;   // method
	@virtual public virtual packed:1,flags,16 boolean is_struct_package;    // struct
	@virtual public virtual packed:1,flags,17 boolean is_struct_argument;   // struct
	@virtual public virtual packed:1,flags,18 boolean is_struct_pizza_case; // struct
	@virtual public virtual packed:1,flags,19 boolean is_struct_enum;       // struct
	@virtual public virtual packed:1,flags,20 boolean is_struct_syntax;     // struct
	@virtual public virtual packed:1,flags,21 boolean is_struct_wrapper;    // struct

	public int			compileflags;

	// Structures	
	@virtual public virtual packed:1,compileflags,16 boolean is_struct_local;
	@virtual public virtual packed:1,compileflags,17 boolean is_struct_anomymouse;
	@virtual public virtual packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
	@virtual public virtual packed:1,compileflags,19 boolean is_struct_verified;
	@virtual public virtual packed:1,compileflags,20 boolean is_struct_members_generated;
	@virtual public virtual packed:1,compileflags,21 boolean is_struct_statements_generated;
	@virtual public virtual packed:1,compileflags,22 boolean is_struct_generated;
	
	// Expression flags
	@virtual public virtual packed:1,compileflags,16 boolean is_expr_use_no_proxy;
	@virtual public virtual packed:1,compileflags,17 boolean is_expr_as_field;
	@virtual public virtual packed:1,compileflags,18 boolean is_expr_gen_void;
	@virtual public virtual packed:1,compileflags,19 boolean is_expr_try_resolved;
	@virtual public virtual packed:1,compileflags,20 boolean is_expr_gen_resolved;
	@virtual public virtual packed:1,compileflags,21 boolean is_expr_for_wrapper;
	
	// Method flags
	@virtual public virtual packed:1,compileflags,17 boolean is_mth_virtual_static;
	@virtual public virtual packed:1,compileflags,20 boolean is_mth_operator;
	@virtual public virtual packed:1,compileflags,21 boolean is_mth_gen_post_cond;
	@virtual public virtual packed:1,compileflags,22 boolean is_mth_need_fields_init;
	@virtual public virtual packed:1,compileflags,24 boolean is_mth_local;
	
	// Var/field
	@virtual public virtual packed:1,compileflags,16 boolean is_init_wrapper;
	@virtual public virtual packed:1,compileflags,17 boolean is_need_proxy;
	// Var specific
	@virtual public virtual packed:1,compileflags,18 boolean is_var_need_ref_proxy; // also sets is_var_need_proxy
	@virtual public virtual packed:1,compileflags,19 boolean is_var_local_rule_var;
	@virtual public virtual packed:1,compileflags,20 boolean is_var_closure_proxy;
	@virtual public virtual packed:1,compileflags,21 boolean is_var_this;
	@virtual public virtual packed:1,compileflags,22 boolean is_var_super;

	// Field specific
	@virtual public virtual packed:1,compileflags,18 boolean is_fld_packer;
	@virtual public virtual packed:1,compileflags,19 boolean is_fld_packed;

	// Statement flags
	@virtual public virtual packed:1,compileflags,16 boolean is_stat_abrupted;
	@virtual public virtual packed:1,compileflags,17 boolean is_stat_breaked;
	@virtual public virtual packed:1,compileflags,18 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	@virtual public virtual packed:1,compileflags,19 boolean is_stat_auto_returnable;
	@virtual public virtual packed:1,compileflags,20 boolean is_stat_break_target;
	// General flags
	@virtual public virtual packed:1,compileflags,28 boolean is_accessed_from_inner;
	@virtual public virtual packed:1,compileflags,29 boolean is_resolved;
	@virtual public virtual packed:1,compileflags,30 boolean is_hidden;
	@virtual public virtual packed:1,compileflags,31 boolean is_bad;

    public ASTNode() {
	}

    public ASTNode(int pos) {
		this.pos = pos;
	}

	public /*abstract*/ void cleanup() {
		parent = null;
	};
	
	public /*abstract*/ Object copy() {
		throw new CompilerException(getPos(),"Internal error: method copy() is not implemented");
	};

	public ASTNode(int pos, int fl) {
		this(pos);
		flags = fl;
	}

	public ASTNode(int pos, ASTNode parent) {
		this(pos);
		this.parent = parent;
	}
	
	public ASTNode replaceWith(ASTNode node) {
		parent.replaceVal(pslot.name, this, node);
		return node;
	}

	public String parserAddr() {
		String addr = Integer.toHexString(++parserAddrIdx);
		while( addr.length() < 8 ) {
			addr = '0'+addr;
		}
		Kiev.parserAddresses.put(addr,(ASTNode)this.copy());
		return addr;
	}

	public void setParent(ASTNode n) { parent = n; }
	public ASTNode getParent() { return parent; }

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
	@getter public final boolean get$is_struct_package()  alias isPackage  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_package;
	}
	@setter public final void set$is_struct_package(boolean on) alias setPackage {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isInterface() && ! isEnum() && !isSyntax()));
		this.is_struct_package = on;
	}
	// a class's argument	
	@getter public final boolean get$is_struct_argument()  alias isArgument  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_argument;
	}
	@setter public final void set$is_struct_argument(boolean on) alias setArgument {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_argument = on;
	}
	// a class's argument	
	@getter public final boolean get$is_struct_pizza_case()  alias isPizzaCase  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_pizza_case;
	}
	@setter public final void set$is_struct_pizza_case(boolean on) alias setPizzaCase {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_pizza_case = on;
	}
	// a local (in method) class	
	@getter public final boolean get$is_struct_local()  alias isLocal  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_local;
	}
	@setter public final void set$is_struct_local(boolean on) alias setLocal {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_local = on;
	}
	// an anonymouse (unnamed) class	
	@getter public final boolean get$is_struct_anomymouse()  alias isAnonymouse  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_anomymouse;
	}
	@setter public final void set$is_struct_anomymouse(boolean on) alias setAnonymouse {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_anomymouse = on;
	}
	// has pizza cases
	@getter public final boolean get$is_struct_has_pizza_cases()  alias isHasCases  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_has_pizza_cases;
	}
	@setter public final void set$is_struct_has_pizza_cases(boolean on) alias setHasCases {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_has_pizza_cases = on;
	}
	// verified
	@getter public final boolean get$is_struct_verified()  alias isVerified  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_verified;
	}
	@setter public final void set$is_struct_verified(boolean on) alias setVerified {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_verified = on;
	}
	// indicates that structure members were generated
	@getter public final boolean get$is_struct_members_generated()  alias isMembersGenerated  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_members_generated;
	}
	@setter public final void set$is_struct_members_generated(boolean on) alias setMembersGenerated {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_members_generated = on;
	}
	// indicates that statements in code were generated
	@getter public final boolean get$is_struct_statements_generated()  alias isStatementsGenerated  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_statements_generated;
	}
	@setter public final void set$is_struct_statements_generated(boolean on) alias setStatementsGenerated {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_statements_generated = on;
	}
	// indicates that the structrue was generared (from template)
	@getter public final boolean get$is_struct_generated()  alias isGenerated  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_generated;
	}
	@setter public final void set$is_struct_generated(boolean on) alias setGenerated {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		this.is_struct_generated = on;
	}
	// kiev enum
	@getter public final boolean get$is_struct_enum()  alias isEnum  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_enum;
	}
	@setter public final void set$is_struct_enum(boolean on) alias setEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isInterface() && !isSyntax()));
		this.is_struct_enum = on;
	}
	// kiev annotation
	@getter public final boolean get$is_struct_annotation()  alias isAnnotation  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_annotation;
	}
	@setter public final void set$is_struct_annotation(boolean on) alias setAnnotation {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isSyntax()));
		this.is_struct_annotation = on;
		if (on) this.setInterface(true);
	}
	// java enum
	@getter public final boolean get$is_struct_java_enum()  alias isJavaEnum  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_java_enum;
	}
	@setter public final void set$is_struct_java_enum(boolean on) alias setJavaEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_java_enum = on;
	}
	// kiev syntax
	@getter public final boolean get$is_struct_syntax()  alias isSyntax  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_syntax;
	}
	@setter public final void set$is_struct_syntax(boolean on) alias setSyntax {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && ! isEnum()));
		this.is_struct_syntax = on;
	}
	// kiev wrapper class
	@getter public final boolean get$is_struct_wrapper()  alias isWrapper  {
		assert(this instanceof BaseStruct,"For node "+this.getClass());
		return this.is_struct_wrapper;
	}
	@setter public final void set$is_struct_wrapper(boolean on) alias setWrapper {
		assert(this instanceof Struct,"For node "+this.getClass());
		this.is_struct_wrapper = on;
		Struct s = (Struct)this;
//		if (s.type != null) {
			if (on)
				s.type.flags |= StdTypes.flWrapper;
			else
				s.type.flags &= ~StdTypes.flWrapper;
//		}
	}

	//
	// Method specific
	//

	// multimethod	
	@getter public final boolean get$is_mth_multimethod()  alias isMultiMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_multimethod;
	}
	@setter public final void set$is_mth_multimethod(boolean on) alias setMultiMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_multimethod = on;
	}
	// virtual static method	
	@getter public final boolean get$is_mth_virtual_static()  alias isVirtualStatic  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_virtual_static;
	}
	@setter public final void set$is_mth_virtual_static(boolean on) alias setVirtualStatic {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_virtual_static = on;
	}
	// method with variable number of arguments	
	@getter public final boolean get$is_mth_varargs()  alias isVarArgs  {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration || this instanceof kiev.parser.ASTRuleDeclaration,"For node "+this.getClass());
		return this.is_mth_varargs;
	}
	@setter public final void set$is_mth_varargs(boolean on) alias setVarArgs {
		assert(this instanceof Method || this instanceof kiev.parser.ASTMethodDeclaration || this instanceof kiev.parser.ASTRuleDeclaration,"For node "+this.getClass());
		this.is_mth_varargs = on;
	}
	// logic rule method	
	@getter public final boolean get$is_mth_rule()  alias isRuleMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_rule;
	}
	@setter public final void set$is_mth_rule(boolean on) alias setRuleMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_rule = on;
	}
	// method with attached operator	
	@getter public final boolean get$is_mth_operator()  alias isOperatorMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_operator;
	}
	@setter public final void set$is_mth_operator(boolean on) alias setOperatorMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_operator = on;
	}
	// needs to call post-condition before return	
	@getter public final boolean get$is_mth_gen_post_cond()  alias isGenPostCond  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_gen_post_cond;
	}
	@setter public final void set$is_mth_gen_post_cond(boolean on) alias setGenPostCond {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_gen_post_cond = on;
	}
	// need fields initialization	
	@getter public final boolean get$is_mth_need_fields_init()  alias isNeedFieldInits  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_need_fields_init;
	}
	@setter public final void set$is_mth_need_fields_init(boolean on) alias setNeedFieldInits {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_need_fields_init = on;
	}
	// a method generated as invariant	
	@getter public final boolean get$is_mth_invariant()  alias isInvariantMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_invariant;
	}
	@setter public final void set$is_mth_invariant(boolean on) alias setInvariantMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_invariant = on;
	}
	// a local method (closure code or inner method)	
	@getter public final boolean get$is_mth_local()  alias isLocalMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_local;
	}
	@setter public final void set$is_mth_local(boolean on) alias setLocalMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		this.is_mth_local = on;
	}

	//
	// Var/field
	//
	
	// use no proxy	
	@getter public final boolean get$is_forward()  alias isForward  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_forward;
	}
	@setter public final void set$is_forward(boolean on) alias setForward {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_forward = on;
	}
	// init wrapper
	@getter public final boolean get$is_init_wrapper()  alias isInitWrapper  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_init_wrapper;
	}
	@setter public final void set$is_init_wrapper(boolean on) alias setInitWrapper {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_init_wrapper = on;
	}
	// need a proxy access 
	@getter public final boolean get$is_need_proxy()  alias isNeedProxy  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_need_proxy;
	}
	@setter public final void set$is_need_proxy(boolean on) alias setNeedProxy {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		this.is_need_proxy = on;
	}

	// Var specific
	
	// need a reference proxy access 
	@getter public final boolean get$is_var_need_ref_proxy()  alias isNeedRefProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_need_ref_proxy;
	}
	@setter public final void set$is_var_need_ref_proxy(boolean on) alias setNeedRefProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_need_ref_proxy = on;
		if (on) this.is_need_proxy = on;
	}
	// is a local var in a rule 
	@getter public final boolean get$is_var_local_rule_var()  alias isLocalRuleVar  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_local_rule_var;
	}
	@setter public final void set$is_var_local_rule_var(boolean on) alias setLocalRuleVar {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_local_rule_var = on;
	}
	// closure proxy
	@getter public final boolean get$is_var_closure_proxy()  alias isClosureProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_closure_proxy;
	}
	@setter public final void set$is_var_closure_proxy(boolean on) alias setClosureProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_closure_proxy = on;
	}
	// "this" var
	@getter public final boolean get$is_var_this()  alias isVarThis  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_this;
	}
	@setter public final void set$is_var_this(boolean on) alias setVarThis {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_this = on;
	}
	// "super" var
	@getter public final boolean get$is_var_super()  alias isVarSuper {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_super;
	}
	@setter public final void set$is_var_super(boolean on) alias setVarSuper {
		assert(this instanceof Var,"For node "+this.getClass());
		this.is_var_super = on;
	}

	//
	// Field specific
	//

	// is a virtual field
	@getter public final boolean get$is_fld_virtual()  alias isVirtual  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_virtual;
	}
	@setter public final void set$is_fld_virtual(boolean on) alias setVirtual {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_virtual = on;
	}
	// is a field of enum
	@getter public final boolean get$is_fld_enum()  alias isEnumField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_enum;
	}
	@setter public final void set$is_fld_enum(boolean on) alias setEnumField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_enum = on;
	}
	// packer field (auto-generated for packed fields)
	@getter public final boolean get$is_fld_packer()  alias isPackerField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packer;
	}
	@setter public final void set$is_fld_packer(boolean on) alias setPackerField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_packer = on;
	}
	// packed field
	@getter public final boolean get$is_fld_packed()  alias isPackedField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packed;
	}
	@setter public final void set$is_fld_packed(boolean on) alias setPackedField {
		assert(this instanceof Field,"For node "+this.getClass());
		this.is_fld_packed = on;
	}

	//
	// Expr specific
	//

	// use no proxy	
	@getter public final boolean get$is_expr_use_no_proxy()  alias isUseNoProxy  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_use_no_proxy;
	}
	@setter public final void set$is_expr_use_no_proxy(boolean on) alias setUseNoProxy {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_use_no_proxy = on;
	}
	// use as field (disable setter/getter calls for virtual fields)
	@getter public final boolean get$is_expr_as_field()  alias isAsField  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_as_field;
	}
	@setter public final void set$is_expr_as_field(boolean on) alias setAsField {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_as_field = on;
	}
	// expression will generate void value
	@getter public final boolean get$is_expr_gen_void()  alias isGenVoidExpr  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_gen_void;
	}
	@setter public final void set$is_expr_gen_void(boolean on) alias setGenVoidExpr {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_gen_void = on;
	}
	// tried to be resolved
	@getter public final boolean get$is_expr_try_resolved()  alias isTryResolved  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_try_resolved;
	}
	@setter public final void set$is_expr_try_resolved(boolean on) alias setTryResolved {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_try_resolved = on;
	}
	// resolved for generation
	@getter public final boolean get$is_expr_gen_resolved()  alias isGenResolve  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_gen_resolved;
	}
	@setter public final void set$is_expr_gen_resolved(boolean on) alias setGenResolve {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_gen_resolved = on;
	}
	// used bt for()
	@getter public final boolean get$is_expr_for_wrapper()  alias isForWrapper  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_for_wrapper;
	}
	@setter public final void set$is_expr_for_wrapper(boolean on) alias setForWrapper {
		assert(this instanceof Expr,"For node "+this.getClass());
		this.is_expr_for_wrapper = on;
	}

	//
	// Statement specific flags
	//
	
	// abrupted
	@getter public final boolean get$is_stat_abrupted()  alias isAbrupted  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_abrupted;
	}
	@setter public final void set$is_stat_abrupted(boolean on) alias setAbrupted {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_abrupted = on;
	}
	// breaked
	@getter public final boolean get$is_stat_breaked()  alias isBreaked  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_breaked;
	}
	@setter public final void set$is_stat_breaked(boolean on) alias setBreaked {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_breaked = on;
	}
	// method-abrupted
	@getter public final boolean get$is_stat_method_abrupted()  alias isMethodAbrupted  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_method_abrupted;
	}
	@setter public final void set$is_stat_method_abrupted(boolean on) alias setMethodAbrupted {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_method_abrupted = on;
		if (on) this.is_stat_abrupted = true;
	}
	// auto-returnable
	@getter public final boolean get$is_stat_auto_returnable()  alias isAutoReturnable  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_auto_returnable;
	}
	@setter public final void set$is_stat_auto_returnable(boolean on) alias setAutoReturnable {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_auto_returnable = on;
	}
	// break target
	@getter public final boolean get$is_stat_break_target()  alias isBreakTarget  {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_break_target;
	}
	@setter public final void set$is_stat_auto_returable(boolean on) alias setBreakTarget {
		assert(this instanceof Statement || this instanceof CaseLabel,"For node "+this.getClass());
		this.is_stat_break_target = on;
	}

	//
	// General flags
	//

	// the (private) field/method/struct is accessed from inner class (and needs proxy access)
	@getter public final boolean get$is_accessed_from_inner()  alias isAccessedFromInner  { return this.is_accessed_from_inner; }
	@setter public final void set$is_accessed_from_inner(boolean on) alias setAccessedFromInner { this.is_accessed_from_inner = on; }
	// resolved
	@getter public final boolean get$is_resolved()  alias isResolved  { return this.is_resolved; }
	@setter public final void set$is_resolved(boolean on) alias setResolved { this.is_resolved = on; }
	// hidden
	@getter public final boolean get$is_hidden()  alias isHidden  { return this.is_hidden; }
	@setter public final void set$is_hidden(boolean on) alias setHidden { this.is_hidden = on; }
	// bad
	@getter public final boolean get$is_bad()  alias isBad  { return this.is_bad; }
	@setter public final void set$is_bad(boolean on) alias setBad { this.is_bad = on; }

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
@cfnode
public abstract class CFNode extends ASTNode {
	@ref(copyable=false) public CFNode cf_in;
	@ref(copyable=false) public CFNode cf_out;
	public CFNode() {}
	public CFNode(int pos) { super(pos); }
	public CFNode(int pos, ASTNode parent) { super(pos,parent); }
}

@node
@cfnode
public abstract class Expr extends CFNode {

	public static Expr[] emptyArray = new Expr[0];

	public Expr() {}

	public Expr(int pos) { super(pos); }

	public Expr(int pos, ASTNode parent) { super(pos,parent); }

	public /*abstract*/ Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void		generate(Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation, expr: "+dmp);
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
		if( e instanceof BaseStruct ) expr = toExpr((BaseStruct)e,reqType,pos,parent);
		if( e instanceof WrapedExpr ) expr = toExpr((BaseStruct)((WrapedExpr)e).expr,reqType,pos,parent);
		if( expr == null )
			throw new CompilerException(e.pos,"Is not an expression");
		else if( reqType == null || reqType == Type.tpVoid )
			return expr;
//		if( reqType == Type.tpRule ) reqType = Type.tpBoolean;
		Type et = expr.getType();
		if( et == Type.tpBoolean && reqType == Type.tpInt )
			return new CastExpr(0,reqType,expr,false,true).resolveExpr(reqType);
		if( et.isInstanceOf(reqType) ) return expr;
		if( et.isReference() && reqType.isBoolean() )
			return new BinaryBoolExpr(pos,BinaryOperator.Equals,expr,new ConstNullExpr());
		if( et.isAutoCastableTo(reqType)
		 || et.isNumber() && reqType.isNumber()
		) return new CastExpr(pos,reqType,expr).tryResolve(reqType);
		throw new CompilerException(e.pos,"Expression "+expr+" is not auto-castable to type "+reqType);
	}
	public static Expr toExpr(BaseStruct bs, Type reqType, int pos, ASTNode parent) {
		if( bs.isPizzaCase() ) {
			Struct s = (Struct)bs;
			// Pizza case may be casted to int or to itself or super-class
			PizzaCaseAttr case_attr;
			if( s.generated_from != null )
				case_attr = (PizzaCaseAttr)s.generated_from.getAttr(attrPizzaCase);
			else
				case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
			if( case_attr == null )
				throw new RuntimeException("Internal error - can't find case_attr");
			s = (Struct)Type.getRealType(reqType,s.type).clazz;
			if( !(reqType.isInteger() || s.instanceOf(reqType.clazz)) )
				throw new CompilerException(pos,"Pizza case "+s+" cannot be casted to type "+reqType);
			if( case_attr.casefields.length != 0 )
				throw new CompilerException(pos,"Empty constructor for pizza case "+s+" not found");
			if( reqType.isInteger() ) {
				Expr expr = (Expr)new ConstIntExpr(case_attr.caseno).resolve(reqType);
				if( reqType != Type.tpInt )
					expr = (Expr)new CastExpr(pos,reqType,expr).resolve(reqType);
				return expr;
			}
			// Now, check we need add type arguments
			Type tp = Type.getRealType(reqType,s.type);
			return (Expr)new NewExpr(pos,tp,Expr.emptyArray).resolve(reqType);
		}
		throw new CompilerException(pos,"Expr "+bs+" is not a class's case with no fields");
	}
}

@node
@cfnode
public class WrapedExpr extends Expr {

	@ref public ASTNode		expr;
	@ref public Type		base_type;
	
	public WrapedExpr() {
	}
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
		if( expr instanceof BaseStruct ) return Type.getRealType(base_type,((BaseStruct)expr).type);
		if( expr instanceof TypeRef ) return Type.getRealType(base_type,((TypeRef)expr).getType());
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
	public ASTNode resolve(Type reqType) {
		if( expr instanceof Type ) return expr;
		if( expr instanceof BaseStruct ) return expr;
		if( expr instanceof TypeRef ) return ((TypeRef)expr).getType();
		throw new CompilerException(pos,"Unknown wrapped node of class "+expr.getClass());
	}
}

@node
@cfnode
public abstract class LvalueExpr extends Expr {

	public LvalueExpr() {}

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
@cfnode
public abstract class Statement extends CFNode {

	public static Statement[] emptyArray = new Statement[0];

	public Statement() {}

	public Statement(int pos, ASTNode parent) { super(pos, parent); }

	public void		generate(Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation, stat: "+dmp.toString());
	}

	public ASTNode	resolve(Type reqType) { return this; }

}

@node
public class RefNode<N extends ASTNode> extends ASTNode {
	@att KString					name;
	@ref public virtual forward N	lnk;
	
	public RefNode() {}
	
	public RefNode(N n) {
		this.lnk = n;
	}
	public RefNode(int pos) {
		super(pos);
	}
	public RefNode(int pos, N n) {
		super(pos);
		this.lnk = n;
	}
	public N get$lnk()
		alias operator(210,fy,$cast)
	{
		return lnk;
	}
	public void set$lnk(N n) {
		this.lnk = n;
	}
	
}

@node
public class TypeRef extends ASTNode {
	//@att KString						name;
	@ref public virtual forward Type	lnk;
	
	public TypeRef() {}
	
	public TypeRef(Type tp) {
		this.lnk = tp;
	}
	public TypeRef(int pos) {
		super(pos);
	}
	public TypeRef(int pos, Type tp) {
		super(pos);
		this.lnk = tp;
	}
	
	public boolean isBound() {
		return lnk != null;
	}

	public Type getType() {
		return lnk;
	}
	
	public Type get$lnk()
		alias operator(210,fy,$cast)
	{
		return lnk;
	}
	public void set$lnk(Type n) {
		this.lnk = n;
	}
	
	public String toString() {
		return String.valueOf(lnk);
	}
	
	public static Enumeration<Type> linked_elements(NArr<TypeRef> arr) {
		NArr<Type> tmp = new NArr<Type>();
		foreach (TypeRef tr; arr) { if (tr.lnk != null) tmp.add(tr.lnk); }
		return tmp.elements();
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

