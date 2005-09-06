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
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public enum TopLevelPass {
	passStartCleanup		   ,	// start of compilation or cleanup before next incremental compilation
	passCreateTopStruct		   ,	// create top-level Struct
	passProcessSyntax		   ,	// process syntax - some import, typedef, operator and macro
	passArgumentInheritance	   ,	// inheritance of type arguments
	passStructInheritance	   ,	// inheritance of classe/interfaces/structures
	passResolveMetaDecls	   ,	// resolved meta types declarations
	passResolveMetaDefaults	   ,	// resolved default values for meta-methods
	passResolveMetaValues	   ,	// resolve values in meta-data
	passCreateMembers		   ,	// create declared members of structures
	passAutoGenerateMembers	   ,	// generation of members
	passResolveImports		   ,	// recolve import static for import of fields and methods
	passResolveFinalFields	   ,	// resolve final fields, to find out if they are constants
	passVerify				   ,	// verify the tree before generation
	passPreGenerate			   ,	// prepare tree for generation phase
	passGenerate			   		// resolve, generate and so on - each file separatly
};

public abstract class NodeData {
	public final KString	id;
	public NodeData			prev;
	public NodeData			next;
	public NodeData(KString id) {
		this.id = id;
	}
};

@node
public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false);

	public int				pos;
    @ref(copyable=false)
	public ASTNode			parent;
    @ref(copyable=false)
	public AttrSlot			pslot;
	public NodeData			ndata;

	public int				flags;
	public int			compileflags;
	
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
//	@virtual public virtual packed:1,flags,21 boolean is_struct_wrapper;    // struct

	// Structures	
	@virtual public virtual packed:1,compileflags,16 boolean is_struct_local;
	@virtual public virtual packed:1,compileflags,17 boolean is_struct_anomymouse;
	@virtual public virtual packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
	@virtual public virtual packed:1,compileflags,19 boolean is_struct_verified;
	@virtual public virtual packed:1,compileflags,20 boolean is_struct_members_generated;
	@virtual public virtual packed:1,compileflags,21 boolean is_struct_pre_generated;
	@virtual public virtual packed:1,compileflags,22 boolean is_struct_statements_generated;
	@virtual public virtual packed:1,compileflags,23 boolean is_struct_generated;
	
	// Expression flags
	@virtual public virtual packed:1,compileflags,16 boolean is_expr_use_no_proxy;
	@virtual public virtual packed:1,compileflags,17 boolean is_expr_as_field;
	@virtual public virtual packed:1,compileflags,18 boolean is_expr_gen_void;
	@virtual public virtual packed:1,compileflags,19 boolean is_expr_try_resolved;
	@virtual public virtual packed:1,compileflags,20 boolean is_expr_for_wrapper;
	@virtual public virtual packed:1,compileflags,21 boolean is_expr_primary;
	
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

	public void cleanup() {
		// do nothing
	};
	
	public /*abstract*/ Object copy() {
		throw new CompilerException(getPos(),"Internal error: method copy() is not implemented");
	};

	public /*abstract*/ Object copyTo(Object to$node) {
        ASTNode node = (ASTNode)to$node;
		node.pos			= this.pos;
		node.flags			= this.flags;
		node.compileflags	= this.compileflags;
		return node;
	};

	public void callbackChildChanged(AttrSlot attr) {
		// by default do nothing
	}
	
	public ASTNode(int pos, int fl) {
		this(pos);
		flags = fl;
	}

	public ASTNode(int pos, ASTNode parent) {
		this(pos);
		this.parent = parent;
	}
	
	public final ASTNode replaceWithNode(ASTNode node) {
		parent.replaceVal(pslot.name, this, node);
		return node;
	}
	public final ASTNode replaceWith(()->ASTNode fnode) {
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		ASTNode n = fnode();
		parent.replaceVal(pslot.name, this, n);
		return n;
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
	
	public NodeData getNodeData(KString id) {
		for (NodeData nd = ndata; nd != null; nd = nd.next) {
			if (nd.id == id)
				return nd;
		}
		return null;
	}
	
	public void addNodeData(NodeData d) {
		for (NodeData nd = ndata; nd != null; nd = nd.next) {
			if (nd.id == d.id) {
				d.prev = nd.prev;
				d.next = nd.next;
				if (nd.prev != null) { d.prev.next = d; nd.prev = null; }
				if (nd.next != null) { d.next.prev = d; nd.next = null; }
				return;
			}
		}
		d.next = ndata;
		if (d.next != null) d.next.prev = d;
		ndata = d;
	}
	
	public void delNodeData(KString id) {
		for (NodeData nd = ndata; nd != null; nd = nd.next) {
			if (nd.id == id) {
				if (ndata == nd) ndata = nd.next;
				if (nd.prev != null) nd.prev.next = nd.next;
				if (nd.next != null) nd.next.prev = nd.prev;
				nd.prev = null;
				nd.next = null;
				return;
			}
		}
	}
	
	// get data flow for a child node
	public DFState getDFlowIn(ASTNode child) {
		return getDFlowIn();
		//throw new CompilerException(pos,"Internal error: getDFlowIn(child) not implemented for "+getClass());
	}
	
	
	// build data flow for this node
	public DataFlow getDFlow() {
		DataFlow df = (DataFlow)getNodeData(DataFlow.ID);
		if (df == null)
			df = new DataFlow(this);
		return df;
	}
	
	// get incoming data flow for this node
	public DFState getDFlowIn() {
		DataFlow df = getDFlow();
		if (df.in == null)
			df.in = parent.getDFlowIn(this);
		return df.in;
	}
	
	// get outgoing data flow for this node
	public DFState getDFlowOut() {
		DataFlow df = getDFlow();
		if (df.out == null)
			df.out = getDFlowIn();
		return df.out;
	}
	
	// get outgoing data flow for this node
	public DFState getDFlowTru() {
		DataFlow df = getDFlow();
		return df.out;
	}
	
	// get outgoing data flow for this node
	public DFState getDFlowFls() {
		DataFlow df = getDFlow();
		return df.fls;
	}
	
	public boolean preGenerate()	{ return true; }
	public boolean preResolve()		{ return true; }
	
	public void walkTree((ASTNode)->boolean exec) {
		PassInfo.push(this);
		try {
			if (exec(this)) {
				foreach (AttrSlot attr; this.values(); attr.is_attr) {
					Object val = this.getVal(attr.name);
					if (val == null)
						continue;
					if (attr.is_space) {
						foreach (ASTNode n; (NArr<ASTNode>)val)
							n.walkTree(exec);
					}
					else if (val instanceof ASTNode) {
						((ASTNode)val).walkTree(exec);
					}
				}
			}
		} finally { PassInfo.pop(this); }
	}

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
			self.setStatic((fl & ACC_STATIC) != 0);
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		else if( this instanceof Field ) {
			Field self = (Field)this;
			self.acc = new Access(0);
			self.acc.verifyAccessDecl(self);
		}
		this.callbackChildChanged(nodeattr$flags);
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
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_package;
	}
	@setter public final void set$is_struct_package(boolean on) alias setPackage {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isInterface() && ! isEnum() && !isSyntax()));
		if (this.is_struct_package != on) {
			this.is_struct_package = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a class's argument	
	@getter public final boolean get$is_struct_argument()  alias isArgument  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_argument;
	}
	@setter public final void set$is_struct_argument(boolean on) alias setArgument {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_argument != on) {
			this.is_struct_argument = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a class's argument	
	@getter public final boolean get$is_struct_pizza_case()  alias isPizzaCase  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_pizza_case;
	}
	@setter public final void set$is_struct_pizza_case(boolean on) alias setPizzaCase {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_pizza_case != on) {
			this.is_struct_pizza_case = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local (in method) class	
	@getter public final boolean get$is_struct_local()  alias isLocal  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_local;
	}
	@setter public final void set$is_struct_local(boolean on) alias setLocal {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_local != on) {
			this.is_struct_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// an anonymouse (unnamed) class	
	@getter public final boolean get$is_struct_anomymouse()  alias isAnonymouse  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_anomymouse;
	}
	@setter public final void set$is_struct_anomymouse(boolean on) alias setAnonymouse {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_anomymouse != on) {
			this.is_struct_anomymouse = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// has pizza cases
	@getter public final boolean get$is_struct_has_pizza_cases()  alias isHasCases  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_has_pizza_cases;
	}
	@setter public final void set$is_struct_has_pizza_cases(boolean on) alias setHasCases {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_has_pizza_cases != on) {
			this.is_struct_has_pizza_cases = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// verified
	@getter public final boolean get$is_struct_verified()  alias isVerified  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_verified;
	}
	@setter public final void set$is_struct_verified(boolean on) alias setVerified {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_verified != on) {
			this.is_struct_verified = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were generated
	@getter public final boolean get$is_struct_members_generated()  alias isMembersGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_members_generated;
	}
	@setter public final void set$is_struct_members_generated(boolean on) alias setMembersGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_members_generated != on) {
			this.is_struct_members_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that structure members were pre-generated
	@getter public final boolean get$is_struct_pre_generated()  alias isMembersPreGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_pre_generated;
	}
	@setter public final void set$is_struct_pre_generated(boolean on) alias setMembersPreGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_pre_generated != on) {
			this.is_struct_pre_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	
	// indicates that statements in code were generated
	@getter public final boolean get$is_struct_statements_generated()  alias isStatementsGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_statements_generated;
	}
	@setter public final void set$is_struct_statements_generated(boolean on) alias setStatementsGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_statements_generated != on) {
			this.is_struct_statements_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// indicates that the structrue was generared (from template)
	@getter public final boolean get$is_struct_generated()  alias isGenerated  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_generated;
	}
	@setter public final void set$is_struct_generated(boolean on) alias setGenerated {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_generated != on) {
			this.is_struct_generated = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev enum
	@getter public final boolean get$is_struct_enum()  alias isEnum  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_enum;
	}
	@setter public final void set$is_struct_enum(boolean on) alias setEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isInterface() && !isSyntax()));
		if (this.is_struct_enum != on) {
			this.is_struct_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev annotation
	@getter public final boolean get$is_struct_annotation()  alias isAnnotation  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_annotation;
	}
	@setter public final void set$is_struct_annotation(boolean on) alias setAnnotation {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && !isSyntax()));
		if (this.is_struct_annotation != on) {
			this.is_struct_annotation = on;
			if (on) this.setInterface(true);
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// java enum
	@getter public final boolean get$is_struct_java_enum()  alias isJavaEnum  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_java_enum;
	}
	@setter public final void set$is_struct_java_enum(boolean on) alias setJavaEnum {
		assert(this instanceof Struct,"For node "+this.getClass());
		if (this.is_struct_java_enum != on) {
			this.is_struct_java_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// kiev syntax
	@getter public final boolean get$is_struct_syntax()  alias isSyntax  {
		assert(this instanceof Struct,"For node "+this.getClass());
		return this.is_struct_syntax;
	}
	@setter public final void set$is_struct_syntax(boolean on) alias setSyntax {
		assert(this instanceof Struct,"For node "+this.getClass());
		assert(!on || (!isPackage() && ! isEnum()));
		if (this.is_struct_syntax != on) {
			this.is_struct_syntax = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
		if (this.is_mth_multimethod != on) {
			this.is_mth_multimethod = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// virtual static method	
	@getter public final boolean get$is_mth_virtual_static()  alias isVirtualStatic  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_virtual_static;
	}
	@setter public final void set$is_mth_virtual_static(boolean on) alias setVirtualStatic {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_virtual_static != on) {
			this.is_mth_virtual_static = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// method with variable number of arguments	
	@getter public final boolean get$is_mth_varargs()  alias isVarArgs  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_varargs;
	}
	@setter public final void set$is_mth_varargs(boolean on) alias setVarArgs {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_varargs != on) {
			this.is_mth_varargs = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// logic rule method	
	@getter public final boolean get$is_mth_rule()  alias isRuleMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_rule;
	}
	@setter public final void set$is_mth_rule(boolean on) alias setRuleMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_rule != on) {
			this.is_mth_rule = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// method with attached operator	
	@getter public final boolean get$is_mth_operator()  alias isOperatorMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_operator;
	}
	@setter public final void set$is_mth_operator(boolean on) alias setOperatorMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_operator != on) {
			this.is_mth_operator = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// needs to call post-condition before return	
	@getter public final boolean get$is_mth_gen_post_cond()  alias isGenPostCond  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_gen_post_cond;
	}
	@setter public final void set$is_mth_gen_post_cond(boolean on) alias setGenPostCond {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_gen_post_cond != on) {
			this.is_mth_gen_post_cond = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// need fields initialization	
	@getter public final boolean get$is_mth_need_fields_init()  alias isNeedFieldInits  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_need_fields_init;
	}
	@setter public final void set$is_mth_need_fields_init(boolean on) alias setNeedFieldInits {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_need_fields_init != on) {
			this.is_mth_need_fields_init = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a method generated as invariant	
	@getter public final boolean get$is_mth_invariant()  alias isInvariantMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_invariant;
	}
	@setter public final void set$is_mth_invariant(boolean on) alias setInvariantMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_invariant != on) {
			this.is_mth_invariant = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// a local method (closure code or inner method)	
	@getter public final boolean get$is_mth_local()  alias isLocalMethod  {
		assert(this instanceof Method,"For node "+this.getClass());
		return this.is_mth_local;
	}
	@setter public final void set$is_mth_local(boolean on) alias setLocalMethod {
		assert(this instanceof Method,"For node "+this.getClass());
		if (this.is_mth_local != on) {
			this.is_mth_local = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
		if (this.is_forward != on) {
			this.is_forward = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// init wrapper
	@getter public final boolean get$is_init_wrapper()  alias isInitWrapper  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_init_wrapper;
	}
	@setter public final void set$is_init_wrapper(boolean on) alias setInitWrapper {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		if (this.is_init_wrapper != on) {
			this.is_init_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// need a proxy access 
	@getter public final boolean get$is_need_proxy()  alias isNeedProxy  {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		return this.is_need_proxy;
	}
	@setter public final void set$is_need_proxy(boolean on) alias setNeedProxy {
		assert(this instanceof Var || this instanceof Field,"For node "+this.getClass());
		if (this.is_need_proxy != on) {
			this.is_need_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	// Var specific
	
	// need a reference proxy access 
	@getter public final boolean get$is_var_need_ref_proxy()  alias isNeedRefProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_need_ref_proxy;
	}
	@setter public final void set$is_var_need_ref_proxy(boolean on) alias setNeedRefProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		if (this.is_var_need_ref_proxy != on) {
			this.is_var_need_ref_proxy = on;
			if (on) this.is_need_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// is a local var in a rule 
	@getter public final boolean get$is_var_local_rule_var()  alias isLocalRuleVar  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_local_rule_var;
	}
	@setter public final void set$is_var_local_rule_var(boolean on) alias setLocalRuleVar {
		assert(this instanceof Var,"For node "+this.getClass());
		if (this.is_var_local_rule_var != on) {
			this.is_var_local_rule_var = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// closure proxy
	@getter public final boolean get$is_var_closure_proxy()  alias isClosureProxy  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_closure_proxy;
	}
	@setter public final void set$is_var_closure_proxy(boolean on) alias setClosureProxy {
		assert(this instanceof Var,"For node "+this.getClass());
		if (this.is_var_closure_proxy != on) {
			this.is_var_closure_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// "this" var
	@getter public final boolean get$is_var_this()  alias isVarThis  {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_this;
	}
	@setter public final void set$is_var_this(boolean on) alias setVarThis {
		assert(this instanceof Var,"For node "+this.getClass());
		if (this.is_var_this != on) {
			this.is_var_this = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// "super" var
	@getter public final boolean get$is_var_super()  alias isVarSuper {
		assert(this instanceof Var,"For node "+this.getClass());
		return this.is_var_super;
	}
	@setter public final void set$is_var_super(boolean on) alias setVarSuper {
		assert(this instanceof Var,"For node "+this.getClass());
		if (this.is_var_super != on) {
			this.is_var_super = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
		if (this.is_fld_virtual != on) {
			this.is_fld_virtual = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// is a field of enum
	@getter public final boolean get$is_fld_enum()  alias isEnumField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_enum;
	}
	@setter public final void set$is_fld_enum(boolean on) alias setEnumField {
		assert(this instanceof Field,"For node "+this.getClass());
		if (this.is_fld_enum != on) {
			this.is_fld_enum = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// packer field (auto-generated for packed fields)
	@getter public final boolean get$is_fld_packer()  alias isPackerField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packer;
	}
	@setter public final void set$is_fld_packer(boolean on) alias setPackerField {
		assert(this instanceof Field,"For node "+this.getClass());
		if (this.is_fld_packer != on) {
			this.is_fld_packer = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// packed field
	@getter public final boolean get$is_fld_packed()  alias isPackedField  {
		assert(this instanceof Field,"For node "+this.getClass());
		return this.is_fld_packed;
	}
	@setter public final void set$is_fld_packed(boolean on) alias setPackedField {
		assert(this instanceof Field,"For node "+this.getClass());
		if (this.is_fld_packed != on) {
			this.is_fld_packed = on;
			this.callbackChildChanged(nodeattr$flags);
		}
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
		if (this.is_expr_use_no_proxy != on) {
			this.is_expr_use_no_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// use as field (disable setter/getter calls for virtual fields)
	@getter public final boolean get$is_expr_as_field()  alias isAsField  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_as_field;
	}
	@setter public final void set$is_expr_as_field(boolean on) alias setAsField {
		assert(this instanceof Expr,"For node "+this.getClass());
		if (this.is_expr_as_field != on) {
			this.is_expr_as_field = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// expression will generate void value
	@getter public final boolean get$is_expr_gen_void()  alias isGenVoidExpr  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_gen_void;
	}
	@setter public final void set$is_expr_gen_void(boolean on) alias setGenVoidExpr {
		assert(this instanceof Expr,"For node "+this.getClass());
		if (this.is_expr_gen_void != on) {
			this.is_expr_gen_void = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// tried to be resolved
	@getter public final boolean get$is_expr_try_resolved()  alias isTryResolved  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_try_resolved;
	}
	@setter public final void set$is_expr_try_resolved(boolean on) alias setTryResolved {
		assert(this instanceof Expr,"For node "+this.getClass());
		if (this.is_expr_try_resolved != on) {
			this.is_expr_try_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// used bt for()
	@getter public final boolean get$is_expr_for_wrapper()  alias isForWrapper  {
		assert(this instanceof Expr,"For node "+this.getClass());
		return this.is_expr_for_wrapper;
	}
	@setter public final void set$is_expr_for_wrapper(boolean on) alias setForWrapper {
		assert(this instanceof Expr,"For node "+this.getClass());
		if (this.is_expr_for_wrapper != on) {
			this.is_expr_for_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// used for primary expressions, i.e. (a+b)
	@getter public final boolean get$is_expr_primary()  alias isPrimaryExpr  {
		assert(this instanceof ENode,"For node "+this.getClass());
		return this.is_expr_primary;
	}
	@setter public final void set$is_expr_primary(boolean on) alias setPrimaryExpr {
		assert(this instanceof ENode,"For node "+this.getClass());
		if (this.is_expr_primary != on) {
			this.is_expr_primary = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	//
	// Statement specific flags
	//
	
	// abrupted
	@getter public final boolean get$is_stat_abrupted()  alias isAbrupted  {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_abrupted;
	}
	@setter public final void set$is_stat_abrupted(boolean on) alias setAbrupted {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		if (this.is_stat_abrupted != on) {
			this.is_stat_abrupted = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// breaked
	@getter public final boolean get$is_stat_breaked()  alias isBreaked  {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_breaked;
	}
	@setter public final void set$is_stat_breaked(boolean on) alias setBreaked {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		if (this.is_stat_breaked != on) {
			this.is_stat_breaked = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// method-abrupted
	@getter public final boolean get$is_stat_method_abrupted()  alias isMethodAbrupted  {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_method_abrupted;
	}
	@setter public final void set$is_stat_method_abrupted(boolean on) alias setMethodAbrupted {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		if (this.is_stat_method_abrupted != on) {
			this.is_stat_method_abrupted = on;
			if (on) this.is_stat_abrupted = true;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// auto-returnable
	@getter public final boolean get$is_stat_auto_returnable()  alias isAutoReturnable  {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_auto_returnable;
	}
	@setter public final void set$is_stat_auto_returnable(boolean on) alias setAutoReturnable {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		if (this.is_stat_auto_returnable != on) {
			this.is_stat_auto_returnable = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// break target
	@getter public final boolean get$is_stat_break_target()  alias isBreakTarget  {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		return this.is_stat_break_target;
	}
	@setter public final void set$is_stat_break_target(boolean on) alias setBreakTarget {
		assert(this instanceof Statement || this instanceof BlockExpr || this instanceof CaseLabel,"For node "+this.getClass());
		if (this.is_stat_break_target != on) {
			this.is_stat_break_target = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	//
	// General flags
	//

	// the (private) field/method/struct is accessed from inner class (and needs proxy access)
	@getter public final boolean get$is_accessed_from_inner()  alias isAccessedFromInner  {
		return this.is_accessed_from_inner;
	}
	@setter public final void set$is_accessed_from_inner(boolean on) alias setAccessedFromInner {
		if (this.is_accessed_from_inner != on) {
			this.is_accessed_from_inner = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// resolved
	@getter public final boolean get$is_resolved()  alias isResolved  {
		return this.is_resolved;
	}
	@setter public final void set$is_resolved(boolean on) alias setResolved {
		if (this.is_resolved != on) {
			this.is_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// hidden
	@getter public final boolean get$is_hidden()  alias isHidden  {
		return this.is_hidden;
	}
	@setter public final void set$is_hidden(boolean on) alias setHidden {
		if (this.is_hidden != on) {
			this.is_hidden = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// bad
	@getter public final boolean get$is_bad()  alias isBad  {
		return this.is_bad;
	}
	@setter public final void set$is_bad(boolean on) alias setBad {
		if (this.is_bad != on) {
			this.is_bad = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	public void setPublic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PUBLIC set to "+on+" from "+((flags & ACC_PUBLIC)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PUBLIC;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setPrivate(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIVATE set to "+on+" from "+((flags & ACC_PRIVATE)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PRIVATE;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setProtected(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROTECTED set to "+on+" from "+((flags & ACC_PROTECTED)!=0)+", now 0x"+Integer.toHexString(flags));
		flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
		if( on ) flags |= ACC_PROTECTED;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setStatic(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATIC set to "+on+" from "+((flags & ACC_STATIC)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_STATIC;
		else flags &= ~ACC_STATIC;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setFinal(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_FINAL set to "+on+" from "+((flags & ACC_FINAL)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_FINAL;
		else flags &= ~ACC_FINAL;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setSynchronized(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNCHRONIZED set to "+on+" from "+((flags & ACC_SYNCHRONIZED)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SYNCHRONIZED;
		else flags &= ~ACC_SYNCHRONIZED;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setVolatile(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_VOLATILE set to "+on+" from "+((flags & ACC_VOLATILE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_VOLATILE;
		else flags &= ~ACC_VOLATILE;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setTransient(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRANSIENT set to "+on+" from "+((flags & ACC_TRANSIENT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_TRANSIENT;
		else flags &= ~ACC_TRANSIENT;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setNative(boolean on) {
		assert(this instanceof Method,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_NATIVE set to "+on+" from "+((flags & ACC_NATIVE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_NATIVE;
		else flags &= ~ACC_NATIVE;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setInterface(boolean on) {
		assert(this instanceof Struct,"For node "+this.getClass());
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_INTERFACE set to "+on+" from "+((flags & ACC_INTERFACE)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_INTERFACE | ACC_ABSTRACT;
		else flags &= ~ACC_INTERFACE;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setAbstract(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABSTRACT set to "+on+" from "+((flags & ACC_ABSTRACT)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_ABSTRACT;
		else flags &= ~ACC_ABSTRACT;
		this.callbackChildChanged(nodeattr$flags);
	}
	public void setSuper(boolean on) {
		trace(Kiev.debugFlags,"Member "+this+" flag ACC_SUPER set to "+on+" from "+((flags & ACC_SUPER)!=0)+", now 0x"+Integer.toHexString(flags));
		if( on ) flags |= ACC_SUPER;
		else flags &= ~ACC_SUPER;
		this.callbackChildChanged(nodeattr$flags);
	}

}

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@node
public abstract class DNode extends ASTNode {

	public static final DNode[] emptyArray = new DNode[0];
	
	/** Meta-information (annotations) of this structure */
	@att public MetaSet								meta;

	public DNode() {}
	public DNode(int pos) { super(pos); }
	public DNode(int pos, int fl) { super(pos,fl); }
	public DNode(int pos, ASTNode parent) { super(pos,parent); }

	public abstract void resolveDecl();
	public abstract Dumper toJavaDecl(Dumper dmp);
}

/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@node
public abstract class ENode extends ASTNode {

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() {}
	public ENode(int pos) { super(pos); }
	public ENode(int pos, ASTNode parent) { super(pos,parent); }

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void resolve(Type reqType) {
		throw new CompilerException(pos,"Resolve call for e-node "+getClass());
	}
	
	public void generate(Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation, expr: "+dmp);
	}

	public int getPriority() { return 255; }

	public boolean isConstantExpr() { return false; }
	public Object	getConstValue() {
		throw new RuntimeException("Request for constant value of non-constant expression");
    }
	
	public final void replaceWithNodeResolve(Type reqType, ENode node) {
		parent.replaceVal(pslot.name, this, node);
		node.resolve(reqType);
	}

	public final void replaceWithResolve(Type reqType, ()->ENode fnode) {
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		ENode n = fnode();
		parent.replaceVal(pslot.name, this, n);
		n.resolve(reqType);
	}

	public final void replaceWithNodeResolve(ENode node) {
		parent.replaceVal(pslot.name, this, node);
		node.resolve(null);
	}

	public final void replaceWithResolve(()->ENode fnode) {
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		ENode n = fnode();
		parent.replaceVal(pslot.name, this, n);
		n.resolve(null);
	}

}

@node
public final class VarDecl extends ENode implements Named {

	@att Var var;
	
	public VarDecl() {}
	public VarDecl(Var var) {
		this.var = var;
	}

	public DFState getDFlowOut() {
		DataFlow df = getDFlow();
		if (df.out == null)
			df.out = var.getDFlowOut();
		return df.out;
	}
	
	public void resolve(Type reqType) {
		var.resolveDecl();
	}

	public NodeName getName() { return var.name; }
	public void generate(Type reqType) {
		var.generate(Type.tpVoid);
	}
	public Dumper toJava(Dumper dmp) {
		var.toJavaDecl(dmp);
		return dmp;
	}
	
}

@node
public final class LocalStructDecl extends ENode implements Named {

	@att Struct clazz;
	
	public LocalStructDecl() {}
	public LocalStructDecl(Struct clazz) {
		this.clazz = clazz;
		clazz.setResolved(true);
	}

	public boolean preResolve() {
		if( PassInfo.method==null || PassInfo.method.isStatic())
			clazz.setStatic(true);
		clazz.setResolved(true);
		clazz.setLocal(true);
		Kiev.runProcessorsOn(clazz);
		return false;
	}
	
	public void resolve(Type reqType) {
		clazz.resolveDecl();
	}

	public NodeName getName() { return clazz.name; }
	public void generate(Type reqType) {
		// don't generate here
	}
	public Dumper toJava(Dumper dmp) {
		clazz.toJavaDecl(dmp);
		return dmp;
	}
}


@node
public abstract class Expr extends ENode {

	public static Expr[] emptyArray = new Expr[0];

	public Expr() {}

	public Expr(int pos) { super(pos); }

	public Expr(int pos, ASTNode parent) { super(pos,parent); }

	public Operator getOp() { return null; }

	public int getPriority() {
		if (isPrimaryExpr())
			return 255;
		Operator op = getOp();
		if (op == null)
			return 255;
		return op.priority;
	}

	public Object	getConstValue() {
    	throw new RuntimeException("Request for constant value of non-constant expression");
    }
}

@node
public class NopExpr extends Expr {

	@att public Expr		expr;
	
	public NopExpr() {
	}
	public NopExpr(Expr expr) {
		this.pos = expr.pos;
		this.expr = expr;
	}
	public Type getType() {
		return expr.getType();
	}
	public void resolve(Type reqType) {
		expr.resolve(reqType);
	}
}

@node
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
public abstract class Statement extends ENode {

	public static Statement[] emptyArray = new Statement[0];

	public Statement() {}

	public Statement(int pos, ASTNode parent) { super(pos, parent); }

	public void		generate(Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(pos,"Unresolved node ("+this.getClass()+") generation, stat: "+dmp.toString());
	}

}

@node
public class InitializerShadow extends Statement {

	@ref Initializer init;
	
	public InitializerShadow() {}
	public InitializerShadow(Initializer init) {
		this.init = init;
		this.setResolved(true);
	}
	public void resolve(Type reqType) {
	}

	public void generate(Type reqType) {
		init.generate(reqType);
	}
	public Dumper toJava(Dumper dmp) {
		dmp.append("/* ");
		init.toJavaDecl(dmp);
		dmp.append(" */");
		return dmp;
	}
}


@node
public class TypeRef extends ENode {
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
	
	public final boolean preResolve() {
		getType(); // calls resolving
		return false;
	}
	
	public void resolve(Type reqType) {
		if (reqType != null && reqType != Type.tpClass)
			toExpr(reqType);
		else
			getType(); // calls resolving
	}
	
	public boolean equals(Object o) {
		if (o instanceof Type) return this.lnk == o;
		return this == o;
	}
	
	public String toString() {
		return String.valueOf(lnk);
	}
	
	public Dumper toJava(Dumper dmp) {
		return lnk.toJava(dmp);
	}
	
	public void toExpr(Type reqType) {
		Type st = getType();
		if (st.isPizzaCase()) {
			Struct s = st.getStruct();
			// Pizza case may be casted to int or to itself or super-class
			PizzaCaseAttr case_attr;
			case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
			if (case_attr == null)
				throw new RuntimeException("Internal error - can't find case_attr");
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(pos,"Pizza case "+tp+" cannot be casted to type "+reqType);
			if (case_attr.casefields.length != 0)
				throw new CompilerException(pos,"Empty constructor for pizza case "+tp+" not found");
			if (reqType.isInteger()) {
				Expr expr = new ConstIntExpr(case_attr.caseno);
				if( reqType != Type.tpInt )
					expr = new CastExpr(pos,reqType,expr);
				replaceWithNodeResolve(reqType, expr);
				return;
			}
			// Now, check we need add type arguments
			replaceWithResolve(reqType, fun ()->ENode {return new NewExpr(pos,tp,Expr.emptyArray);});
			return;
		}
		throw new CompilerException(pos,"Type "+this+" is not a class's case with no fields");
	}
	
	public static Enumeration<Type> linked_elements(NArr<TypeRef> arr) {
		Vector<Type> tmp = new Vector<Type>();
		foreach (TypeRef tr; arr) { if (tr.lnk != null) tmp.append(tr.lnk); }
		return tmp.elements();
	}
}

public interface SetBody {
	public boolean setBody(Statement body);
}

public class CompilerException extends RuntimeException {
	public int		pos;
	public Struct	clazz;
	public CError	err_id;
	public CompilerException(int pos, String msg) {
		super(msg);
		this.pos = pos;
		this.clazz = PassInfo.clazz;
	}
	public CompilerException(int pos, CError err_id, String msg) {
		super(msg);
		this.pos = pos;
		this.err_id = err_id;
		this.clazz = PassInfo.clazz;
	}
}

