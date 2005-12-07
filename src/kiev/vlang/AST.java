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
	public void nodeAttached(ASTNode n) {}
	public void dataAttached(ASTNode n) {}
	public void nodeDetached(ASTNode n) {}
	public void dataDetached(ASTNode n) {}
	public void subnodeInserted(NArr<ASTNode> space, ASTNode n) {}
	public void subnodeRemoved(NArr<ASTNode> space, ASTNode n) {}
};

public final class NodeContext {
	public final ASTNode	root;
	public final FileUnit	file_unit;
	public final Struct		clazz;
	public final Struct		outer_clazz;
	public final Method		method;
	public final Method		outer_method;
	private NodeContext(ASTNode root,
						FileUnit file_unit,
						Struct clazz,
						Struct outer_clazz,
						Method method,
						Method outer_method)
	{
		this.root = root;
		this.file_unit = file_unit;
		this.clazz = clazz;
		this.outer_clazz = outer_clazz;
		this.method = method;
		this.outer_method = outer_method;
	}
	public NodeContext(ASTNode root) {
		this.root = root;
	}
	public NodeContext(FileUnit fu) {
		this.root = fu;
		this.file_unit = fu;
	}
	public NodeContext enter(Struct s) {
		return new NodeContext(	this.root,
								this.file_unit,
								s,
								this.clazz,
								null,
								this.method);
	}
	public NodeContext enter(Method m) {
		return new NodeContext(	this.root,
								this.file_unit,
								this.clazz,
								this.outer_clazz,
								m,
								this.outer_method);
	}
};


public class TreeWalker {
	public boolean pre_exec(ASTNode n) { return true; }
	public void post_exec(ASTNode n) {}
}

@node
public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	public int				pos;
	
	@ref(copyable=false)
	public access:ro,ro,ro,rw ASTNode			parent;
	@ref(copyable=false)
	public access:ro,ro,ro,rw AttrSlot			pslot;
	@ref(copyable=false)
	public ASTNode								pprev;
	@ref(copyable=false)
	public ASTNode								pnext;
	@ref(copyable=false)
	public access:ro,ro,rw,rw NodeContext		pctx;
	
	@ref(copyable=false)
	public NodeData			ndata;

	public int				compileflags;
	
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
	// Statement flags
	@virtual public virtual packed:1,compileflags,22 boolean is_stat_abrupted;
	@virtual public virtual packed:1,compileflags,23 boolean is_stat_breaked;
	@virtual public virtual packed:1,compileflags,24 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	@virtual public virtual packed:1,compileflags,25 boolean is_stat_auto_returnable;
	@virtual public virtual packed:1,compileflags,26 boolean is_stat_break_target;
	
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

	// General flags
	@virtual public virtual packed:1,compileflags,28 boolean is_accessed_from_inner;
	@virtual public virtual packed:1,compileflags,29 boolean is_resolved;
	@virtual public virtual packed:1,compileflags,30 boolean is_hidden;
	@virtual public virtual packed:1,compileflags,31 boolean is_bad;

    public ASTNode() {
		this.setupContext();
	}

    public ASTNode(int pos) {
		this.pos = pos;
		this.setupContext();
	}

	public void setupContext() {
		if (this.parent == null)
			this.pctx = new NodeContext(this);
		else
			this.pctx = this.parent.pctx;
	}

	public void cleanup() {
		// do nothing
	};
	
	public ASTNode detach()
		alias operator (210,fy,~)
	{
		if (!isAttached())
			return this;
		if (pslot.is_space) {
			((NArr<ASTNode>)parent.getVal(pslot.name)).detach(this);
		} else {
			parent.setVal(pslot.name,null);
		}
		assert(!isAttached());
		return this;
	}
	
	public /*abstract*/ Object copy() {
		throw new CompilerException(this,"Internal error: method copy() is not implemented");
	};

	public /*abstract*/ Object copyTo(Object to$node) {
        ASTNode node = (ASTNode)to$node;
		node.pos			= this.pos;
//		node.flags			= this.flags;
		node.compileflags	= this.compileflags;
		return node;
	};

	public final void callbackDetached() {
		assert(isAttached());
		// notify node data that we are detached
		NodeData nd = ndata;
		while (nd != null) {
			NodeData nx = nd.next;
			nd.nodeDetached(this);
			nd = nx;
		}
		// do detcah
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		this.parent = null;
		this.pslot = null;
		this.pprev = null;
		this.pnext = null;
		// set new root of the detached tree
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.setupContext(); return true; }
		});
		// notify nodes about new root
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		parent.callbackChildChanged(pslot);
	}
	
	public final void callbackAttached(ASTNode parent, AttrSlot pslot) {
		assert(!isAttached());
		assert(parent != null && parent != this);
		// do attach
		this.parent = parent;
		this.pslot = pslot;
		this.parent = parent;
		// set new root of the attached tree
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.setupContext(); return true; }
		});
		// notify nodes about new root
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.callbackRootChanged(); return true; }
		});
		// notify node data that we are attached
		NodeData nd = ndata;
		while (nd != null) {
			NodeData nx = nd.next;
			nd.nodeAttached(this);
			nd = nx;
		}
		// notify parent about the changed slot
		parent.callbackChildChanged(pslot);
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		// by default do nothing
	}
	
	public void callbackRootChanged() {
		// by default do nothing
	}
	
	public final ASTNode replaceWithNode(ASTNode node) {
		assert(isAttached());
		if (pslot.is_space) {
			assert(node != null);
			NArr<ASTNode> space = (NArr<ASTNode>)parent.getVal(pslot.name);
			int idx = space.indexOf(this);
			assert(idx >= 0);
			if (node.pos == 0) node.pos = this.pos;
			space[idx] = node;
		} else {
			assert(parent.getVal(pslot.name) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			parent.setVal(pslot.name, node);
		}
		assert(node == null || node.isAttached());
		return node;
	}
	public final ASTNode replaceWith(()->ASTNode fnode) {
		assert(isAttached());
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		if (pslot.is_space) {
			NArr<ASTNode> space = (NArr<ASTNode>)parent.getVal(pslot.name);
			int idx = space.indexOf(this);
			assert(idx >= 0);
			space[idx] = (ASTNode)pslot.clazz.newInstance();
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			space[idx] = n;
			assert(n.isAttached());
			return n;
		} else {
			assert(parent.getVal(pslot.name) == this);
			parent.setVal(pslot.name, pslot.clazz.newInstance());
			ASTNode n = fnode();
			if (n != null && n.pos == 0) n.pos = this.pos;
			parent.setVal(pslot.name, n);
			assert(n == null || n.isAttached());
			return n;
		}
	}

	// the node is attached
	public final boolean isAttached()  {
		return parent != null;
	}

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
				if (nd == d)
					return;
				d.prev = nd.prev;
				d.next = nd.next;
				if (nd.prev != null) { d.prev.next = d; nd.prev = null; }
				if (nd.next != null) { d.next.prev = d; nd.next = null; }
				nd.dataDetached(this);
				d.dataAttached(this);
				return;
			}
		}
		d.next = ndata;
		if (d.next != null) d.next.prev = d;
		ndata = d;
		d.dataAttached(this);
	}
	
	public void delNodeData(KString id) {
		for (NodeData nd = ndata; nd != null; nd = nd.next) {
			if (nd.id == id) {
				if (ndata == nd) ndata = nd.next;
				if (nd.prev != null) nd.prev.next = nd.next;
				if (nd.next != null) nd.next.prev = nd.prev;
				nd.prev = null;
				nd.next = null;
				nd.dataDetached(this);
				return;
			}
		}
	}
	
	public void cleanDFlow() {
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.delNodeData(DataFlowInfo.ID); return true; }
		});
	}
	
	// build data flow for this node
	public DataFlowInfo getDFlow() {
		DataFlowInfo df = (DataFlowInfo)getNodeData(DataFlowInfo.ID);
		if (df == null) {
			df = DataFlowInfo.newDataFlowInfo(this);
			this.addNodeData(df);
		}
		return df;
	}
	
	// get outgoing data flow for this node
	private static java.util.regex.Pattern join_pattern = java.util.regex.Pattern.compile("join ([\\:a-zA-Z_0-9\\(\\)]+) ([\\:a-zA-Z_0-9\\(\\)]+)");
	
	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public boolean preResolveIn(TransfProcessor proc) { return true; }
	public void preResolveOut() {}
	public boolean mainResolveIn(TransfProcessor proc) { return true; }
	public void mainResolveOut() {}
	public boolean	preGenerate()	{ return true; }
	
	
	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.is_attr) {
				Object val = this.getVal(attr.name);
				if (val == null)
					continue;
				if (attr.is_space) {
					foreach (ASTNode n; (NArr<ASTNode>)val)
						n.walkTree(walker);
				}
				else if (val instanceof ASTNode) {
					((ASTNode)val).walkTree(walker);
				}
			}
		}
		walker.post_exec(this);
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

}

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@node
public abstract class DNode extends ASTNode {

	public static final DNode[] emptyArray = new DNode[0];
	
	public int				flags;
	
	/** Meta-information (annotations) of this structure */
	@att public MetaSet								meta;

	@virtual public virtual packed:1,flags,13 boolean is_struct_annotation; // struct
	@virtual public virtual packed:1,flags,14 boolean is_struct_enum;       // struct
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
	@virtual public virtual packed:1,flags,20 boolean is_struct_syntax;     // struct
//	@virtual public virtual packed:1,flags,21 boolean is_struct_wrapper;    // struct
	@virtual public virtual packed:1,flags,22 boolean is_struct_bytecode;    // struct was loaded from bytecode

	public DNode() {}
	public DNode(int pos) { super(pos); }
	public DNode(int pos, int fl) { super(pos); this.flags = fl; }

	public abstract void resolveDecl();
	public abstract Dumper toJavaDecl(Dumper dmp);

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
 * An lvalue dnode (var or field)
 */
@node
public abstract class LvalDNode extends DNode {

	public LvalDNode() {}
	public LvalDNode(int pos) { super(pos); }
	public LvalDNode(int pos, int fl) { super(pos,fl); }

	//
	// Var/field
	//
	
	// use no proxy	
	@getter public final boolean get$is_forward()  alias isForward  {
		return this.is_forward;
	}
	@setter public final void set$is_forward(boolean on) alias setForward {
		if (this.is_forward != on) {
			this.is_forward = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// init wrapper
	@getter public final boolean get$is_init_wrapper()  alias isInitWrapper  {
		return this.is_init_wrapper;
	}
	@setter public final void set$is_init_wrapper(boolean on) alias setInitWrapper {
		if (this.is_init_wrapper != on) {
			this.is_init_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// need a proxy access 
	@getter public final boolean get$is_need_proxy()  alias isNeedProxy  {
		return this.is_need_proxy;
	}
	@setter public final void set$is_need_proxy(boolean on) alias setNeedProxy {
		if (this.is_need_proxy != on) {
			this.is_need_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

}


/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@node
@dflow(out="this:in")
public /*abstract*/ class ENode extends ASTNode {

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() {}
	public ENode(int pos) { super(pos); }

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void resolve(Type reqType) {
		throw new CompilerException(this,"Resolve call for e-node "+getClass());
	}
	
	public void generate(Code code, Type reqType) {
		Dumper dmp = new Dumper();
		dmp.append(this);
		throw new CompilerException(this,"Unresolved node ("+this.getClass()+") generation, expr: "+dmp);
	}

	public Operator getOp() { return null; }
	public int getPriority() {
		if (isPrimaryExpr())
			return 255;
		Operator op = getOp();
		if (op == null)
			return 255;
		return op.priority;
	}

	public boolean valueEquals(Object o) { return false; }
	public boolean isConstantExpr() { return false; }
	public Object	getConstValue() {
		throw new RuntimeException("Request for constant value of non-constant expression");
    }

	public final void replaceWithNodeResolve(Type reqType, ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		((ENode)n).resolve(reqType);
	}

	public final void replaceWithResolve(Type reqType, ()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		((ENode)n).resolve(reqType);
	}

	public final void replaceWithNodeResolve(ENode node) {
		assert(isAttached());
		ASTNode n = this.replaceWithNode(node);
		assert(n == node);
		assert(n.isAttached());
		((ENode)n).resolve(null);
	}

	public final void replaceWithResolve(()->ENode fnode) {
		assert(isAttached());
		ASTNode n = this.replaceWith(fnode);
		assert(n.isAttached());
		((ENode)n).resolve(null);
	}

	//
	// Expr specific
	//

	// use no proxy	
	@getter public final boolean get$is_expr_use_no_proxy()  alias isUseNoProxy  {
		return this.is_expr_use_no_proxy;
	}
	@setter public final void set$is_expr_use_no_proxy(boolean on) alias setUseNoProxy {
		if (this.is_expr_use_no_proxy != on) {
			this.is_expr_use_no_proxy = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// use as field (disable setter/getter calls for virtual fields)
	@getter public final boolean get$is_expr_as_field()  alias isAsField  {
		return this.is_expr_as_field;
	}
	@setter public final void set$is_expr_as_field(boolean on) alias setAsField {
		if (this.is_expr_as_field != on) {
			this.is_expr_as_field = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// expression will generate void value
	@getter public final boolean get$is_expr_gen_void()  alias isGenVoidExpr  {
		return this.is_expr_gen_void;
	}
	@setter public final void set$is_expr_gen_void(boolean on) alias setGenVoidExpr {
		if (this.is_expr_gen_void != on) {
			this.is_expr_gen_void = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// tried to be resolved
	@getter public final boolean get$is_expr_try_resolved()  alias isTryResolved  {
		return this.is_expr_try_resolved;
	}
	@setter public final void set$is_expr_try_resolved(boolean on) alias setTryResolved {
		if (this.is_expr_try_resolved != on) {
			this.is_expr_try_resolved = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// used bt for()
	@getter public final boolean get$is_expr_for_wrapper()  alias isForWrapper  {
		return this.is_expr_for_wrapper;
	}
	@setter public final void set$is_expr_for_wrapper(boolean on) alias setForWrapper {
		if (this.is_expr_for_wrapper != on) {
			this.is_expr_for_wrapper = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// used for primary expressions, i.e. (a+b)
	@getter public final boolean get$is_expr_primary()  alias isPrimaryExpr  {
		return this.is_expr_primary;
	}
	@setter public final void set$is_expr_primary(boolean on) alias setPrimaryExpr {
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
		return this.is_stat_abrupted;
	}
	@setter public final void set$is_stat_abrupted(boolean on) alias setAbrupted {
		if (this.is_stat_abrupted != on) {
			this.is_stat_abrupted = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// breaked
	@getter public final boolean get$is_stat_breaked()  alias isBreaked  {
		return this.is_stat_breaked;
	}
	@setter public final void set$is_stat_breaked(boolean on) alias setBreaked {
		if (this.is_stat_breaked != on) {
			this.is_stat_breaked = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// method-abrupted
	@getter public final boolean get$is_stat_method_abrupted()  alias isMethodAbrupted  {
		return this.is_stat_method_abrupted;
	}
	@setter public final void set$is_stat_method_abrupted(boolean on) alias setMethodAbrupted {
		if (this.is_stat_method_abrupted != on) {
			this.is_stat_method_abrupted = on;
			if (on) this.is_stat_abrupted = true;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// auto-returnable
	@getter public final boolean get$is_stat_auto_returnable()  alias isAutoReturnable  {
		return this.is_stat_auto_returnable;
	}
	@setter public final void set$is_stat_auto_returnable(boolean on) alias setAutoReturnable {
		if (this.is_stat_auto_returnable != on) {
			this.is_stat_auto_returnable = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}
	// break target
	@getter public final boolean get$is_stat_break_target()  alias isBreakTarget  {
		return this.is_stat_break_target;
	}
	@setter public final void set$is_stat_break_target(boolean on) alias setBreakTarget {
		if (this.is_stat_break_target != on) {
			this.is_stat_break_target = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

}

@node
@dflow(out="var")
public final class VarDecl extends ENode implements Named {

	@att @dflow Var var;
	
	public VarDecl() {}
	
	public VarDecl(Var var) {
		this.var = var;
	}

	public void resolve(Type reqType) {
		var.resolveDecl();
	}

	public NodeName getName() { return var.name; }
	public void generate(Code code, Type reqType) {
		var.generate(code,Type.tpVoid);
	}
	public Dumper toJava(Dumper dmp) {
		var.toJavaDecl(dmp);
		return dmp;
	}
	
}

@node
@dflow(out="this:in")
public final class LocalStructDecl extends ENode implements Named {

	@att Struct clazz;
	
	public LocalStructDecl() {}
	public LocalStructDecl(Struct clazz) {
		this.clazz = clazz;
		clazz.setResolved(true);
	}

	public boolean preResolveIn(TransfProcessor proc) {
		if( pctx.method==null || pctx.method.isStatic())
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
	public void generate(Code code, Type reqType) {
		// don't generate here
	}
	public Dumper toJava(Dumper dmp) {
		clazz.toJavaDecl(dmp);
		return dmp;
	}
}


@node
@dflow(out="expr")
public class NopExpr extends ENode {

	@att
	@dflow(in="this:in")
	public ENode	expr;
	
	public NopExpr() {
	}
	public NopExpr(ENode expr) {
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
public abstract class LvalueExpr extends ENode {

	public LvalueExpr() {}

	public LvalueExpr(int pos) { super(pos); }

	public void generate(Code code, Type reqType) {
		code.setLinePos(this.getPosLine());
		generateLoad(code);
		if( reqType == Type.tpVoid )
			code.addInstr(Instr.op_pop);
	}

	/** Just load value referenced by lvalue */
	public abstract void generateLoad(Code code);

	/** Load value and dup info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateLoadDup(Code code);

	/** Load info needed for generateStore or generateStoreDupValue
		(the caller MUST provide one of Store call after a while) */
	public abstract void generateAccess(Code code);

	/** Stores value using previously duped info */
	public abstract void generateStore(Code code);

	/** Stores value using previously duped info, and put stored value in stack */
	public abstract void generateStoreDupValue(Code code);
}

@node
@dflow(out="this:in")
public class InitializerShadow extends ENode {

	@ref Initializer init;
	
	public InitializerShadow() {}
	public InitializerShadow(Initializer init) {
		this.init = init;
		this.setResolved(true);
	}
	public void resolve(Type reqType) {
	}

	public void generate(Code code, Type reqType) {
		init.generate(code,reqType);
	}
	public Dumper toJava(Dumper dmp) {
		dmp.append("/* ");
		init.toJavaDecl(dmp);
		dmp.append(" */");
		return dmp;
	}
}


@node
public abstract class TypeDef extends DNode implements Named {
	public TypeDef() {}
	
	public TypeDef(int pos) {
		super(pos);
	}
	public TypeDef(int pos, int fl) {
		super(pos, fl);
	}

	public abstract NodeName	getName();
	public abstract boolean		checkResolved();
}


@node
@dflow(out="this:in")
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
//		if (lnk == null)
//			throw new CompilerException(this,"Type "+this+" is not found");
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
	
	public boolean preResolveIn(TransfProcessor proc) {
		getType(); // calls resolving
		return false;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
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
//			PizzaCaseAttr case_attr;
//			case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
//			if (case_attr == null)
//				throw new RuntimeException("Internal error - can't find case_attr");
			MetaPizzaCase meta = s.getMetaPizzaCase();
			if (meta == null)
				throw new RuntimeException("Internal error - can't find pizza case meta attr");
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(this,"Pizza case "+tp+" cannot be casted to type "+reqType);
//			if (case_attr.casefields.length != 0)
			if (meta.getFields().length != 0)
				throw new CompilerException(this,"Empty constructor for pizza case "+tp+" not found");
			if (reqType.isInteger()) {
//				ENode expr = new ConstIntExpr(case_attr.caseno);
				ENode expr = new ConstIntExpr(meta.getTag());
				if( reqType != Type.tpInt )
					expr = new CastExpr(pos,reqType,expr);
				replaceWithNodeResolve(reqType, expr);
				return;
			}
			// Now, check we need add type arguments
			replaceWithResolve(reqType, fun ()->ENode {return new NewExpr(pos,tp,ENode.emptyArray);});
			return;
		}
		throw new CompilerException(this,"Type "+this+" is not a class's case with no fields");
	}
	
	public static Enumeration<Type> linked_elements(NArr<TypeRef> arr) {
		Vector<Type> tmp = new Vector<Type>();
		foreach (TypeRef tr; arr) { if (tr.lnk != null) tmp.append(tr.lnk); }
		return tmp.elements();
	}
}

@node
@dflow(out="this:in")
public class NameRef extends ASTNode {
	public KString name;

	public NameRef() {
	}

	public NameRef(KString name) {
		this.name = name;
	}

	public NameRef(int pos, KString name) {
		this.pos = pos;
		this.name = name;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = KString.from(t.image);
	}
	
	public Type getType() {
		return Type.tpVoid;
	}

	public KString toKString() {
		return name;
	}
    
	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
}

public interface SetBody {
	public boolean setBody(ENode body);
}

public class CompilerException extends RuntimeException {
	public ASTNode	from;
	public CError	err_id;
	public CompilerException(String msg) {
		super(msg);
	}
	public CompilerException(ASTNode from, String msg) {
		super(msg);
		this.from = from;
	}
	public CompilerException(ASTNode from, CError err_id, String msg) {
		super(msg);
		this.from = from;
		this.err_id = err_id;
	}
}

