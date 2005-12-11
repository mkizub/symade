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
	public final ASTNode	node;
	public NodeData			prev;
	public NodeData			next;
	public NodeData(KString id, ASTNode node) {
		this.id = id;
		this.node = node;
	}
	public void nodeAttached() {}
	public void dataAttached() {}
	public void nodeDetached() {}
	public void dataDetached() {}
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

	public static class NodeImpl {
		protected ASTNode			_self;		
		protected int				pos;
		protected int				compileflags;
		protected ASTNode			parent;
		protected AttrSlot			pslot;
		protected ASTNode			pprev;
		protected ASTNode			pnext;
		protected NodeContext		pctx;
		protected NodeData			ndata;
		// Structures	
		packed:1,compileflags,16 boolean is_struct_local;
		packed:1,compileflags,17 boolean is_struct_anomymouse;
		packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
		packed:1,compileflags,19 boolean is_struct_verified;
		packed:1,compileflags,20 boolean is_struct_members_generated;
		packed:1,compileflags,21 boolean is_struct_pre_generated;
		packed:1,compileflags,22 boolean is_struct_statements_generated;
		packed:1,compileflags,23 boolean is_struct_generated;
		
		// Expression flags
		packed:1,compileflags,16 boolean is_expr_use_no_proxy;
		packed:1,compileflags,17 boolean is_expr_as_field;
		packed:1,compileflags,18 boolean is_expr_gen_void;
		packed:1,compileflags,19 boolean is_expr_try_resolved;
		packed:1,compileflags,20 boolean is_expr_for_wrapper;
		packed:1,compileflags,21 boolean is_expr_primary;
		// Statement flags
		packed:1,compileflags,22 boolean is_stat_abrupted;
		packed:1,compileflags,23 boolean is_stat_breaked;
		packed:1,compileflags,24 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
		packed:1,compileflags,25 boolean is_stat_auto_returnable;
		packed:1,compileflags,26 boolean is_stat_break_target;
		
		// Method flags
		packed:1,compileflags,17 boolean is_mth_virtual_static;
		packed:1,compileflags,20 boolean is_mth_operator;
		packed:1,compileflags,21 boolean is_mth_gen_post_cond;
		packed:1,compileflags,22 boolean is_mth_need_fields_init;
		packed:1,compileflags,24 boolean is_mth_local;
		
		// Var/field
		packed:1,compileflags,16 boolean is_init_wrapper;
		packed:1,compileflags,17 boolean is_need_proxy;
		// Var specific
		packed:1,compileflags,18 boolean is_var_need_ref_proxy; // also sets is_var_need_proxy
		packed:1,compileflags,19 boolean is_var_local_rule_var;
		packed:1,compileflags,20 boolean is_var_closure_proxy;
		packed:1,compileflags,21 boolean is_var_this;
		packed:1,compileflags,22 boolean is_var_super;
	
		// Field specific
		packed:1,compileflags,18 boolean is_fld_packer;
		packed:1,compileflags,19 boolean is_fld_packed;
	
		// General flags
		packed:1,compileflags,28 boolean is_accessed_from_inner;
		packed:1,compileflags,29 boolean is_resolved;
		packed:1,compileflags,30 boolean is_hidden;
		packed:1,compileflags,31 boolean is_bad;

		public NodeImpl() {}
		public NodeImpl(int pos) {
			this.pos = pos;
		}
		// the node is attached
		public final boolean isAttached()  {
			return parent != null;
		}
		public final void callbackDetached() {
			assert(isAttached());
			// notify node data that we are detached
			NodeData nd = ndata;
			while (nd != null) {
				NodeData nx = nd.next;
				nd.nodeDetached();
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
			_self.walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { n.setupContext(); return true; }
			});
			// notify nodes about new root
			_self.walkTree(new TreeWalker() {
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
			// set new root of the attached tree
			_self.walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { n.setupContext(); return true; }
			});
			// notify nodes about new root
			_self.walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { n.callbackRootChanged(); return true; }
			});
			// notify node data that we are attached
			NodeData nd = ndata;
			while (nd != null) {
				NodeData nx = nd.next;
				nd.nodeAttached();
				nd = nx;
			}
			// notify parent about the changed slot
			parent.callbackChildChanged(pslot);
		}
	}
	@nodeview
	public static class NodeView extends ASTNode {
		final NodeImpl impl;
		public NodeView(NodeImpl impl) {
			super();
			this.impl = impl;
		}
		@getter public final int			get$pos()			{ return this.impl.pos; }
		@getter public final int			get$compileflags()	{ return this.impl.compileflags; }
		@getter public final ASTNode		get$parent()		{ return this.impl.parent; }
		@getter public final AttrSlot		get$pslot()			{ return this.impl.pslot; }
		@getter public final ASTNode		get$pprev()			{ return this.impl.pprev; }
		@getter public final ASTNode		get$pnext()			{ return this.impl.pnext; }
		@getter public final NodeContext	get$pctx()			{ return this.impl.pctx; }
		@getter public final NodeData		get$ndata()			{ return this.impl.ndata; }
		
		@setter public final void set$pos(int val)				{ this.impl.pos = val; }
		@setter public final void set$compileflags(int val)	{ this.impl.compileflags = val; }
		@setter public final void set$parent(ASTNode val)		{ this.impl.parent = val; }
		@setter public final void set$pslot(AttrSlot val)		{ this.impl.pslot = val; }
		@setter public final void set$pprev(ASTNode val)		{ this.impl.pprev = val; }
		@setter public final void set$pnext(ASTNode val)		{ this.impl.pnext = val; }
		@setter public final void set$pctx(NodeContext val)	{ this.impl.pctx = val; }
		@setter public final void set$ndata(NodeData val)		{ this.impl.ndata = val; }

		// the (private) field/method/struct is accessed from inner class (and needs proxy access)
		@getter public final boolean isAccessedFromInner() {
			return this.impl.is_accessed_from_inner;
		}
		@setter public final void setAccessedFromInner(boolean on) {
			if (this.impl.is_accessed_from_inner != on) {
				this.impl.is_accessed_from_inner = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// resolved
		@getter public final boolean isResolved() {
			return this.impl.is_resolved;
		}
		@setter public final void setResolved(boolean on) {
			if (this.impl.is_resolved != on) {
				this.impl.is_resolved = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// hidden
		@getter public final boolean isHidden() {
			return this.impl.is_hidden;
		}
		@setter public final void setHidden(boolean on) {
			if (this.impl.is_hidden != on) {
				this.impl.is_hidden = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// bad
		@getter public final boolean isBad() {
			return this.impl.is_bad;
		}
		@setter public final void setBad(boolean on) {
			if (this.impl.is_bad != on) {
				this.impl.is_bad = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	
	public NodeImpl $v_impl;
	public NodeView getNodeView() { return new NodeView(this.$v_impl); }
	
	@virtual
	public abstract virtual							int				pos;
	@virtual
	public abstract virtual							int				compileflags;
	@ref(copyable=false) @virtual
	public abstract virtual access:ro,ro,ro,rw		ASTNode			parent;
	@ref(copyable=false) @virtual
	public abstract virtual access:ro,ro,ro,rw		AttrSlot		pslot;
	@ref(copyable=false) @virtual
	public abstract virtual							ASTNode			pprev;
	@ref(copyable=false) @virtual
	public abstract virtual							ASTNode			pnext;
	@ref(copyable=false) @virtual
	public abstract virtual access:ro,ro,rw,rw		NodeContext		pctx;
	@ref(copyable=false) @virtual
	public abstract virtual access:no,no,no,rw		NodeData		ndata;

	// for NodeView only
	/*private*/ ASTNode() {}

	public ASTNode(NodeImpl v_impl) {
		this.$v_impl = v_impl;
		this.$v_impl._self = this;
		setupContext();
	}

	@getter public int			get$pos()			{ return this.getNodeView().get$pos(); }
	@getter public int			get$compileflags()	{ return this.getNodeView().get$compileflags(); }
	@getter public ASTNode		get$parent()		{ return this.getNodeView().get$parent(); }
	@getter public AttrSlot		get$pslot()			{ return this.getNodeView().get$pslot(); }
	@getter public ASTNode		get$pprev()			{ return this.getNodeView().get$pprev(); }
	@getter public ASTNode		get$pnext()			{ return this.getNodeView().get$pnext(); }
	@getter public NodeContext	get$pctx()			{ return this.getNodeView().get$pctx(); }
	@getter public NodeData		get$ndata()			{ return this.getNodeView().get$ndata(); }
	
	@setter public void set$pos(int val)			{ this.getNodeView().set$pos(val); }
	@setter public void set$compileflags(int val)	{ this.getNodeView().set$compileflags(val); }
	@setter public void set$parent(ASTNode val)	{ this.getNodeView().set$parent(val); }
	@setter public void set$pslot(AttrSlot val)	{ this.getNodeView().set$pslot(val); }
	@setter public void set$pprev(ASTNode val)		{ this.getNodeView().set$pprev(val); }
	@setter public void set$pnext(ASTNode val)		{ this.getNodeView().set$pnext(val); }
	@setter public void set$pctx(NodeContext val)	{ this.getNodeView().set$pctx(val); }
	@setter public void set$ndata(NodeData val)	{ this.getNodeView().set$ndata(val); }

	public void setupContext() {
		if (this.parent == null)
			this.pctx = new NodeContext(this);
		else
			this.pctx = this.parent.pctx;
	}

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
		node.compileflags	= this.compileflags;
		return node;
	};

	public void callbackDetached() {
		this.$v_impl.callbackDetached();
	}
	
	public void callbackAttached(NodeImpl parent_impl, AttrSlot pslot) {
		this.$v_impl.callbackAttached(parent_impl._self, pslot);
	}
	public void callbackAttached(ASTNode parent, AttrSlot pslot) {
		this.$v_impl.callbackAttached(parent, pslot);
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
				nd.dataDetached();
				d.dataAttached();
				return;
			}
		}
		d.next = ndata;
		if (d.next != null) d.next.prev = d;
		ndata = d;
		d.dataAttached();
	}
	
	public void delNodeData(KString id) {
		for (NodeData nd = ndata; nd != null; nd = nd.next) {
			if (nd.id == id) {
				if (ndata == nd) ndata = nd.next;
				if (nd.prev != null) nd.prev.next = nd.next;
				if (nd.next != null) nd.next.prev = nd.prev;
				nd.prev = null;
				nd.next = null;
				nd.dataDetached();
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
	public boolean isAccessedFromInner() { return this.getNodeView().isAccessedFromInner(); }
	public void setAccessedFromInner(boolean on) { this.getNodeView().setAccessedFromInner(on); }
	// resolved
	public boolean isResolved() { return this.getNodeView().isResolved(); }
	public void setResolved(boolean on) { this.getNodeView().setResolved(on); }
	// hidden
	public boolean isHidden() { return this.getNodeView().isHidden(); }
	public void setHidden(boolean on) { this.getNodeView().setHidden(on); }
	// bad
	public boolean isBad() { return this.getNodeView().isBad(); }
	public void setBad(boolean on) { this.getNodeView().setBad(on); }

}

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@node
public abstract class DNode extends ASTNode {

	public static final DNode[] emptyArray = new DNode[0];
	
	@node
	public static class DNodeImpl extends NodeImpl {		
		     protected int		flags;
		@att protected MetaSet	meta;

		packed:1,flags,13 boolean is_struct_annotation; // struct
		packed:1,flags,14 boolean is_struct_enum;       // struct
		packed:1,flags,14 boolean is_fld_enum;        // field
		// Flags temporary used with java flags
		packed:1,flags,16 boolean is_forward;         // var/field
		packed:1,flags,17 boolean is_fld_virtual;     // field
		packed:1,flags,16 boolean is_mth_multimethod; // method
		packed:1,flags,17 boolean is_mth_varargs;     // method
		packed:1,flags,18 boolean is_mth_rule;        // method
		packed:1,flags,19 boolean is_mth_invariant;   // method
		packed:1,flags,16 boolean is_struct_package;    // struct
		packed:1,flags,17 boolean is_struct_argument;   // struct
		packed:1,flags,18 boolean is_struct_pizza_case; // struct
		packed:1,flags,20 boolean is_struct_syntax;     // struct
		packed:1,flags,22 boolean is_struct_bytecode;    // struct was loaded from bytecode

		public DNodeImpl() {}
		public DNodeImpl(int pos) {
			super(pos);
		}
		public DNodeImpl(int pos, int fl) {
			super(pos);
			this.flags = fl;
		}
	}
	@nodeview
	public static class DNodeView extends NodeView {
		final DNodeImpl impl;
		public DNodeView(DNodeImpl impl) {
			super(impl);
			this.impl = impl;
		}
		@getter public final int		get$flags()				{ return this.impl.flags; }
		@getter public final MetaSet	get$meta()				{ return this.impl.meta; }
		@setter public final void		set$flags(int val)		{ this.impl.flags = val; }
		@setter public final void		set$meta(MetaSet val)	{ this.impl.meta = val; }

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
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_NATIVE set to "+on+" from "+((flags & ACC_NATIVE)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_NATIVE;
			else flags &= ~ACC_NATIVE;
			this.callbackChildChanged(nodeattr$flags);
		}
		public void setInterface(boolean on) {
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

	/** java flags */
	public virtual abstract			int			flags;	
	/** Meta-information (annotations) of this structure */
	@att public virtual abstract	MetaSet		meta;

	public NodeView getNodeView() { return new DNodeView((DNodeImpl)this.$v_impl); }
	public DNodeView getDNodeView() { return new DNodeView((DNodeImpl)this.$v_impl); }

	public DNode(DNodeImpl v_impl) { super(v_impl); }

	@getter public int			get$flags()			{ return this.getDNodeView().get$flags(); }
	@getter public MetaSet		get$meta()			{ return this.getDNodeView().get$meta(); }
	
	@setter public void set$flags(int val)			{ this.getDNodeView().set$flags(val); }
	@setter public void set$meta(MetaSet val)		{ this.getDNodeView().set$meta(val); }

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

	public boolean isPublic()			{ return this.getDNodeView().isPublic(); }
	public boolean isPrivate()			{ return this.getDNodeView().isPrivate(); }
	public boolean isProtected()		{ return this.getDNodeView().isProtected(); }
	public boolean isPackageVisable()	{ return this.getDNodeView().isPackageVisable(); }
	public boolean isStatic()			{ return this.getDNodeView().isStatic(); }
	public boolean isFinal()			{ return this.getDNodeView().isFinal(); }
	public boolean isSynchronized()		{ return this.getDNodeView().isSynchronized(); }
	public boolean isVolatile()			{ return this.getDNodeView().isVolatile(); }
	public boolean isTransient()		{ return this.getDNodeView().isTransient(); }
	public boolean isNative()			{ return this.getDNodeView().isNative(); }
	public boolean isInterface()		{ return this.getDNodeView().isInterface(); }
	public boolean isAbstract()			{ return this.getDNodeView().isAbstract(); }
	public boolean isSuper()			{ return this.getDNodeView().isSuper(); }

	public void setPublic(boolean on)		{ this.getDNodeView().setPublic(on); }
	public void setPrivate(boolean on)		{ this.getDNodeView().setPrivate(on); }
	public void setProtected(boolean on)	{ this.getDNodeView().setProtected(on); }
	public void setStatic(boolean on)		{ this.getDNodeView().setStatic(on); }
	public void setFinal(boolean on)		{ this.getDNodeView().setFinal(on); }
	public void setSynchronized(boolean on){ this.getDNodeView().setSynchronized(on); }
	public void setVolatile(boolean on)	{ this.getDNodeView().setVolatile(on); }
	public void setTransient(boolean on)	{ this.getDNodeView().setTransient(on); }
	public void setNative(boolean on)		{ this.getDNodeView().setNative(on); }
	public void setInterface(boolean on)	{ this.getDNodeView().setInterface(on); }
	public void setAbstract(boolean on)	{ this.getDNodeView().setAbstract(on); }
	public void setSuper(boolean on)		{ this.getDNodeView().setSuper(on); }

}

/**
 * An lvalue dnode (var or field)
 */
@node
public abstract class LvalDNode extends DNode {

	@node
	public static class LvalDNodeImpl extends DNodeImpl {
		public LvalDNodeImpl() {}
		public LvalDNodeImpl(int pos) { super(pos); }
		public LvalDNodeImpl(int pos, int fl) { super(pos, fl); }
	}
	@nodeview
	public static class LvalDNodeView extends DNodeView {
		public LvalDNodeView(LvalDNodeImpl impl) {
			super(impl);
		}

		// use no proxy	
		@getter public final boolean isForward() {
			return this.impl.is_forward;
		}
		@setter public final void setForward(boolean on) {
			if (this.impl.is_forward != on) {
				this.impl.is_forward = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// init wrapper
		@getter public final boolean isInitWrapper() {
			return this.impl.is_init_wrapper;
		}
		@setter public final void setInitWrapper(boolean on) {
			if (this.impl.is_init_wrapper != on) {
				this.impl.is_init_wrapper = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// need a proxy access 
		@getter public final boolean isNeedProxy() {
			return this.impl.is_need_proxy;
		}
		@setter public final void setNeedProxy(boolean on) {
			if (this.impl.is_need_proxy != on) {
				this.impl.is_need_proxy = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		{ return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	{ return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }

	public LvalDNode(LvalDNodeImpl v_impl) { super(v_impl); }

	// use no proxy	
	public boolean isForward() { return getLvalDNodeView().isForward(); }
	public void setForward(boolean on) { getLvalDNodeView().setForward(on); }
	// init wrapper
	public boolean isInitWrapper() { return getLvalDNodeView().isInitWrapper(); }
	public void setInitWrapper(boolean on) { getLvalDNodeView().setInitWrapper(on); }
	// need a proxy access 
	public boolean isNeedProxy() { return getLvalDNodeView().isNeedProxy(); }
	public void setNeedProxy(boolean on) { getLvalDNodeView().setNeedProxy(on); }

}


/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@node
public /*abstract*/ class ENode extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static class ENodeImpl extends NodeImpl {
		public ENodeImpl() {}
		public ENodeImpl(int pos) { super(pos); }
	}
	@nodeview
	public static class ENodeView extends NodeView {
		public ENodeView(ENodeImpl impl) {
			super(impl);
		}

		//
		// Expr specific
		//
	
		// use no proxy	
		public final boolean isUseNoProxy() {
			return this.impl.is_expr_use_no_proxy;
		}
		public final void setUseNoProxy(boolean on) {
			if (this.impl.is_expr_use_no_proxy != on) {
				this.impl.is_expr_use_no_proxy = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// use as field (disable setter/getter calls for virtual fields)
		public final boolean isAsField() {
			return this.impl.is_expr_as_field;
		}
		public final void setAsField(boolean on) {
			if (this.impl.is_expr_as_field != on) {
				this.impl.is_expr_as_field = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// expression will generate void value
		public final boolean isGenVoidExpr() {
			return this.impl.is_expr_gen_void;
		}
		public final void setGenVoidExpr(boolean on) {
			if (this.impl.is_expr_gen_void != on) {
				this.impl.is_expr_gen_void = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// tried to be resolved
		public final boolean isTryResolved() {
			return this.impl.is_expr_try_resolved;
		}
		public final void setTryResolved(boolean on) {
			if (this.impl.is_expr_try_resolved != on) {
				this.impl.is_expr_try_resolved = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used bt for()
		public final boolean isForWrapper() {
			return this.impl.is_expr_for_wrapper;
		}
		public final void setForWrapper(boolean on) {
			if (this.impl.is_expr_for_wrapper != on) {
				this.impl.is_expr_for_wrapper = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for primary expressions, i.e. (a+b)
		public final boolean isPrimaryExpr() {
			return this.impl.is_expr_primary;
		}
		public final void setPrimaryExpr(boolean on) {
			if (this.impl.is_expr_primary != on) {
				this.impl.is_expr_primary = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	
		//
		// Statement specific flags
		//
		
		// abrupted
		public final boolean isAbrupted() {
			return this.impl.is_stat_abrupted;
		}
		public final void setAbrupted(boolean on) {
			if (this.impl.is_stat_abrupted != on) {
				this.impl.is_stat_abrupted = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// breaked
		public final boolean isBreaked() {
			return this.impl.is_stat_breaked;
		}
		public final void setBreaked(boolean on) {
			if (this.impl.is_stat_breaked != on) {
				this.impl.is_stat_breaked = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// method-abrupted
		public final boolean isMethodAbrupted() {
			return this.impl.is_stat_method_abrupted;
		}
		public final void setMethodAbrupted(boolean on) {
			if (this.impl.is_stat_method_abrupted != on) {
				this.impl.is_stat_method_abrupted = on;
				if (on) this.impl.is_stat_abrupted = true;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// auto-returnable
		public final boolean isAutoReturnable() {
			return this.impl.is_stat_auto_returnable;
		}
		public final void setAutoReturnable(boolean on) {
			if (this.impl.is_stat_auto_returnable != on) {
				this.impl.is_stat_auto_returnable = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// break target
		public final boolean isBreakTarget() {
			return this.impl.is_stat_break_target;
		}
		public final void setBreakTarget(boolean on) {
			if (this.impl.is_stat_break_target != on) {
				this.impl.is_stat_break_target = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		{ return new ENodeView((ENodeImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new ENodeView((ENodeImpl)this.$v_impl); }

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() { super(new ENodeImpl()); }
	public ENode(int pos) { super(new ENodeImpl(pos)); }

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
	public boolean isUseNoProxy() { return this.getENodeView().isUseNoProxy(); }
	public void setUseNoProxy(boolean on) { this.getENodeView().setUseNoProxy(on); }
	// use as field (disable setter/getter calls for virtual fields)
	public boolean isAsField() { return this.getENodeView().isAsField(); }
	public void setAsField(boolean on) { this.getENodeView().setAsField(on); }
	// expression will generate void value
	public boolean isGenVoidExpr() { return this.getENodeView().isGenVoidExpr(); }
	public void setGenVoidExpr(boolean on) { this.getENodeView().setGenVoidExpr(on); }
	// tried to be resolved
	public boolean isTryResolved() { return this.getENodeView().isTryResolved(); }
	public void setTryResolved(boolean on) { this.getENodeView().setTryResolved(on); }
	// used bt for()
	public boolean isForWrapper() { return this.getENodeView().isForWrapper(); }
	public void setForWrapper(boolean on) { this.getENodeView().setForWrapper(on); }
	// used for primary expressions, i.e. (a+b)
	public boolean isPrimaryExpr() { return this.getENodeView().isPrimaryExpr(); }
	public void setPrimaryExpr(boolean on) { this.getENodeView().setPrimaryExpr(on); }

	//
	// Statement specific flags
	//
	
	// abrupted
	public boolean isAbrupted() { return this.getENodeView().isAbrupted(); }
	public void setAbrupted(boolean on) { this.getENodeView().setAbrupted(on); }
	// breaked
	public boolean isBreaked() { return this.getENodeView().isBreaked(); }
	public void setBreaked(boolean on) { this.getENodeView().setBreaked(on); }
	// method-abrupted
	public boolean isMethodAbrupted() { return this.getENodeView().isMethodAbrupted(); }
	public void setMethodAbrupted(boolean on) { this.getENodeView().setMethodAbrupted(on); }
	// auto-returnable
	public boolean isAutoReturnable() { return this.getENodeView().isAutoReturnable(); }
	public void setAutoReturnable(boolean on) { this.getENodeView().setAutoReturnable(on); }
	// break target
	public boolean isBreakTarget() { return this.getENodeView().isBreakTarget(); }
	public void setBreakTarget(boolean on) { this.getENodeView().setBreakTarget(on); }

}

@node
public final class VarDecl extends ENode implements Named {

	@dflow(out="var") private static class DFI {
	@dflow(in="this:in")	Var		var;
	}

	@att Var var;
	
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
public final class LocalStructDecl extends ENode implements Named {

	@dflow(out="this:in") private static class DFI {}

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
public class NopExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	@att
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
public class InitializerShadow extends ENode {

	@dflow(out="this:in") private static class DFI {}

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

	@node
	public static class TypeDefImpl extends DNodeImpl {		
		public TypeDefImpl() {}
		public TypeDefImpl(int pos) { super(pos); }
		public TypeDefImpl(int pos, int fl) { super(pos, fl); }
	}
	@nodeview
	public static class TypeDefView extends DNodeView {
		public TypeDefView(TypeDefImpl impl) {
			super(impl);
		}
	}
	public NodeView			getNodeView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new TypeDefView((TypeDefImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()	{ return new TypeDefView((TypeDefImpl)this.$v_impl); }

	public TypeDef(TypeDefImpl v_impl) { super(v_impl); }

	public abstract NodeName	getName();
	public abstract boolean		checkResolved();
}


@node
public class TypeRef extends ENode {

	@dflow(out="this:in") private static class DFI {}

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
public class NameRef extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	public KString name;

	public NameRef() {
		super(new NodeImpl());
	}

	public NameRef(KString name) {
		super(new NodeImpl());
		this.name = name;
	}

	public NameRef(int pos, KString name) {
		super(new NodeImpl(pos));
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

