package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.be.java.JType;
import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JVarDeclView;
import kiev.be.java.JLocalStructDeclView;
import kiev.be.java.JLvalueExprView;
import kiev.be.java.JTypeDefView;
import kiev.be.java.JTypeRefView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public enum TopLevelPass {
	passStartCleanup		   ,	// start of compilation or cleanup before next incremental compilation
	passProcessSyntax		   ,	// process syntax - some import, typedef, operator and macro
	passStructTypes			   ,	// inheritance and types of structures
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

public class TreeWalker {
	public boolean pre_exec(ASTNode n) { return true; }
	public void post_exec(ASTNode n) {}
}

@node
public abstract class ASTNode implements Constants {

	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	public static class NodeImpl {
		public		ASTNode			_self;		
		public		int				pos;
		public		int				compileflags;
		public		ASTNode			parent;
		protected	AttrSlot		pslot;
		protected	ASTNode			pprev;
		protected	ASTNode			pnext;
		protected	NodeData		ndata;
		// Structures	
		public packed:1,compileflags,16 boolean is_struct_local;
		public packed:1,compileflags,17 boolean is_struct_anomymouse;
		public packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
		public packed:1,compileflags,19 boolean is_struct_verified;
		public packed:1,compileflags,20 boolean is_struct_members_generated;
		public packed:1,compileflags,21 boolean is_struct_pre_generated;
		public packed:1,compileflags,22 boolean is_struct_statements_generated;
		public packed:1,compileflags,23 boolean is_struct_generated;
		public packed:1,compileflags,24 boolean is_struct_type_resolved;
		public packed:1,compileflags,25 boolean is_struct_args_resolved;
		public packed:1,compileflags,26 boolean is_struct_rt_arg_typed;
		
		// Expression flags
		public packed:1,compileflags,16 boolean is_expr_use_no_proxy;
		public packed:1,compileflags,17 boolean is_expr_as_field;
		public packed:1,compileflags,18 boolean is_expr_gen_void;
		public packed:1,compileflags,19 boolean is_expr_for_wrapper;
		public packed:1,compileflags,20 boolean is_expr_primary;
		public packed:1,compileflags,21 boolean is_expr_super;
		public packed:1,compileflags,22 boolean is_expr_cast_call;
		// Statement flags
		public packed:1,compileflags,23 boolean is_stat_abrupted;
		public packed:1,compileflags,24 boolean is_stat_breaked;
		public packed:1,compileflags,25 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
		public packed:1,compileflags,26 boolean is_stat_auto_returnable;
		public packed:1,compileflags,27 boolean is_stat_break_target;
		
		// Method flags
		public packed:1,compileflags,17 boolean is_mth_virtual_static;
		public packed:1,compileflags,18 boolean is_mth_operator;
		public packed:1,compileflags,19 boolean is_mth_need_fields_init;
		public packed:1,compileflags,20 boolean is_mth_local;
		
		// Var/field
		public packed:1,compileflags,16 boolean is_init_wrapper;
		public packed:1,compileflags,17 boolean is_need_proxy;
		// Var specific
		public packed:1,compileflags,18 boolean is_var_local_rule_var;
		public packed:1,compileflags,19 boolean is_var_closure_proxy;
		public packed:1,compileflags,20 boolean is_var_this;
		public packed:1,compileflags,21 boolean is_var_super;
	
		// Field specific
		public packed:1,compileflags,18 boolean is_fld_packer;
		public packed:1,compileflags,19 boolean is_fld_packed;
	
		// General flags
		public packed:1,compileflags,28 boolean is_accessed_from_inner;
		public packed:1,compileflags,29 boolean is_resolved;
		public packed:1,compileflags,30 boolean is_hidden;
		public packed:1,compileflags,31 boolean is_bad;

		public NodeImpl() {}
		public NodeImpl(int pos) {
			this.pos = pos;
		}
		
		public Object copyTo(Object to$node) {
			NodeImpl node = (NodeImpl)to$node;
			node.pos			= this.pos;
			node.compileflags	= this.compileflags;
			return node;
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
		public void callbackChildChanged(AttrSlot attr) {
			// do nothing
		}
		public void callbackRootChanged() {
			// do nothing
		}	
	}
	@nodeview
	public static view NodeView of NodeImpl implements Constants {
		
		public final ASTNode getNode() { return $view._self; }
		public String toString() { return String.valueOf($view._self); }
		public Dumper toJava(Dumper dmp) { return getNode().toJava(dmp); }
		
		public int			pos;
		public int			compileflags;
		public ASTNode		parent;
		public AttrSlot		pslot;
		public ASTNode		pprev;
		public ASTNode		pnext;
		public NodeData		ndata;
		
		// the (private) field/method/struct is accessed from inner class (and needs proxy access)
		@getter public final boolean isAccessedFromInner() {
			return this.$view.is_accessed_from_inner;
		}
		@setter public final void setAccessedFromInner(boolean on) {
			if (this.$view.is_accessed_from_inner != on) {
				this.$view.is_accessed_from_inner = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// resolved
		@getter public final boolean isResolved() {
			return this.$view.is_resolved;
		}
		@setter public final void setResolved(boolean on) {
			if (this.$view.is_resolved != on) {
				this.$view.is_resolved = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// hidden
		@getter public final boolean isHidden() {
			return this.$view.is_hidden;
		}
		@setter public final void setHidden(boolean on) {
			if (this.$view.is_hidden != on) {
				this.$view.is_hidden = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// bad
		@getter public final boolean isBad() {
			return this.$view.is_bad;
		}
		@setter public final void setBad(boolean on) {
			if (this.$view.is_bad != on) {
				this.$view.is_bad = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	
	public NodeImpl $v_impl;
	public NodeView		getNodeView()	alias operator(210,fy,$cast) { return new NodeView($v_impl); }
	public JNodeView	getJNodeView()	alias operator(210,fy,$cast) { return new JNodeView($v_impl); }
	
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
	public abstract virtual access:no,no,no,rw		NodeData		ndata;

	// for NodeView only
	/*private*/ ASTNode() {}

	public ASTNode(NodeImpl v_impl) {
		this.$v_impl = v_impl;
		this.$v_impl._self = this;
	}

	@getter public int			get$pos()			{ return this.getNodeView().pos; }
	@getter public int			get$compileflags()	{ return this.getNodeView().compileflags; }
	@getter public ASTNode		get$parent()		{ return this.getNodeView().parent; }
	@getter public AttrSlot		get$pslot()			{ return this.getNodeView().pslot; }
	@getter public ASTNode		get$pprev()			{ return this.getNodeView().pprev; }
	@getter public ASTNode		get$pnext()			{ return this.getNodeView().pnext; }
	@getter public NodeData		get$ndata()			{ return this.getNodeView().ndata; }
	
	@setter public void set$pos(int val)			{ this.getNodeView().pos = val; }
	@setter public void set$compileflags(int val)	{ this.getNodeView().compileflags = val; }
	@setter public void set$parent(ASTNode val)	{ this.getNodeView().parent = val; }
	@setter public void set$pslot(AttrSlot val)	{ this.getNodeView().pslot = val; }
	@setter public void set$pprev(ASTNode val)		{ this.getNodeView().pprev = val; }
	@setter public void set$pnext(ASTNode val)		{ this.getNodeView().pnext = val; }
	@setter public void set$ndata(NodeData val)	{ this.getNodeView().ndata = val; }

	@getter public final ASTNode get$ctx_root() {
		ASTNode parent = this.parent;
		if (parent == null)
			return this;
		return parent.get$ctx_root();
	}
	@getter public FileUnit get$ctx_file_unit() { return this.parent.get$ctx_file_unit(); }
	@getter public Struct get$ctx_clazz() { return this.parent.child_ctx_clazz; }
	@getter public Struct get$child_ctx_clazz() { return this.parent.get$child_ctx_clazz(); }
	@getter public Method get$ctx_method() { return this.parent.child_ctx_method; }
	@getter public Method get$child_ctx_method() { return this.parent.get$child_ctx_method(); }

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
		$v_impl.copyTo(node.$v_impl);
		return node;
	};

	public final void callbackDetached() {
		this.$v_impl.callbackDetached();
	}
	
	public final void callbackAttached(NodeImpl parent_impl, AttrSlot pslot) {
		this.$v_impl.callbackAttached(parent_impl._self, pslot);
	}
	public final void callbackAttached(ASTNode parent, AttrSlot pslot) {
		this.$v_impl.callbackAttached(parent, pslot);
	}
	
	public void callbackChildChanged(AttrSlot attr) {
		this.$v_impl.callbackChildChanged(attr);
	}
	
	public void callbackRootChanged() {
		this.$v_impl.callbackRootChanged();
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
		     public		int			flags;
		@att public		MetaSet		meta;

		public packed:1,flags,13 boolean is_struct_annotation; // struct
		public packed:1,flags,14 boolean is_struct_enum;       // struct
		public packed:1,flags,14 boolean is_fld_enum;        // field
		// Flags temporary used with java flags
		public packed:1,flags,16 boolean is_forward;         // var/field
		public packed:1,flags,17 boolean is_fld_virtual;     // field
		public packed:1,flags,16 boolean is_mth_multimethod; // method
		public packed:1,flags,17 boolean is_mth_varargs;     // method
		public packed:1,flags,18 boolean is_mth_rule;        // method
		public packed:1,flags,19 boolean is_mth_invariant;   // method
		public packed:1,flags,16 boolean is_struct_package;    // struct
		public packed:1,flags,17 boolean is_struct_argument;   // struct
		public packed:1,flags,18 boolean is_struct_pizza_case; // struct
		public packed:1,flags,19 boolean is_struct_singleton;  // struct
		public packed:1,flags,20 boolean is_struct_syntax;     // struct
		public packed:1,flags,23 boolean is_struct_bytecode;   // struct was loaded from bytecode

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
	public static view DNodeView of DNodeImpl extends NodeView {

		public final DNode getDNode() { return (DNode)getNode(); }
		public Dumper toJavaDecl(Dumper dmp) { return getDNode().toJavaDecl(dmp); }
		
		public int		flags;
		public MetaSet	meta;

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
		public final boolean isView()				{ return (flags & ACC_VIEW) != 0; }

		public void setPublic(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_PUBLIC set to "+on+" from "+((flags & ACC_PUBLIC)!=0)+", now 0x"+Integer.toHexString(flags));
			flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
			if( on ) flags |= ACC_PUBLIC;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setPrivate(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_PRIVATE set to "+on+" from "+((flags & ACC_PRIVATE)!=0)+", now 0x"+Integer.toHexString(flags));
			flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
			if( on ) flags |= ACC_PRIVATE;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setProtected(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_PROTECTED set to "+on+" from "+((flags & ACC_PROTECTED)!=0)+", now 0x"+Integer.toHexString(flags));
			flags &= ~(ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
			if( on ) flags |= ACC_PROTECTED;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setStatic(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_STATIC set to "+on+" from "+((flags & ACC_STATIC)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_STATIC;
			else flags &= ~ACC_STATIC;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setFinal(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_FINAL set to "+on+" from "+((flags & ACC_FINAL)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_FINAL;
			else flags &= ~ACC_FINAL;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setSynchronized(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_SYNCHRONIZED set to "+on+" from "+((flags & ACC_SYNCHRONIZED)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_SYNCHRONIZED;
			else flags &= ~ACC_SYNCHRONIZED;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setVolatile(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_VOLATILE set to "+on+" from "+((flags & ACC_VOLATILE)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_VOLATILE;
			else flags &= ~ACC_VOLATILE;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setTransient(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_TRANSIENT set to "+on+" from "+((flags & ACC_TRANSIENT)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_TRANSIENT;
			else flags &= ~ACC_TRANSIENT;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setNative(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_NATIVE set to "+on+" from "+((flags & ACC_NATIVE)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_NATIVE;
			else flags &= ~ACC_NATIVE;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setInterface(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_INTERFACE set to "+on+" from "+((flags & ACC_INTERFACE)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_INTERFACE | ACC_ABSTRACT;
			else flags &= ~ACC_INTERFACE;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setAbstract(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_ABSTRACT set to "+on+" from "+((flags & ACC_ABSTRACT)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_ABSTRACT;
			else flags &= ~ACC_ABSTRACT;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
		public void setSuper(boolean on) {
			trace(Kiev.debugFlags,"Member "+this+" flag ACC_SUPER set to "+on+" from "+((flags & ACC_SUPER)!=0)+", now 0x"+Integer.toHexString(flags));
			if( on ) flags |= ACC_SUPER;
			else flags &= ~ACC_SUPER;
			this.$view.callbackChildChanged(nodeattr$flags);
		}
	}

	/** java flags */
	public virtual abstract			int			flags;	
	/** Meta-information (annotations) of this structure */
	@att public virtual abstract	MetaSet		meta;

	public NodeView		getNodeView()		{ return new DNodeView((DNodeImpl)this.$v_impl); }
	public DNodeView	getDNodeView()		{ return new DNodeView((DNodeImpl)this.$v_impl); }
	public JNodeView	getJNodeView()		{ return new JDNodeView((DNodeImpl)this.$v_impl); }
	public JDNodeView	getJDNodeView()		{ return new JDNodeView((DNodeImpl)this.$v_impl); }

	public DNode(DNodeImpl v_impl) { super(v_impl); }

	@getter public int			get$flags()			{ return this.getDNodeView().flags; }
	@getter public MetaSet		get$meta()			{ return this.getDNodeView().meta; }
	
	@setter public void set$flags(int val)			{ this.getDNodeView().flags = val; }
	@setter public void set$meta(MetaSet val)		{ this.getDNodeView().meta = val; }

	public abstract void resolveDecl();
	public abstract Dumper toJavaDecl(Dumper dmp);

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
	public boolean isView()				{ return this.getDNodeView().isView(); }

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
	public static view LvalDNodeView of LvalDNodeImpl extends DNodeView {
		public LvalDNodeView(LvalDNodeImpl $view) {
			super($view);
		}

		// use no proxy	
		@getter public final boolean isForward() {
			return this.$view.is_forward;
		}
		@setter public final void setForward(boolean on) {
			if (this.$view.is_forward != on) {
				this.$view.is_forward = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// init wrapper
		@getter public final boolean isInitWrapper() {
			return this.$view.is_init_wrapper;
		}
		@setter public final void setInitWrapper(boolean on) {
			if (this.$view.is_init_wrapper != on) {
				this.$view.is_init_wrapper = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// need a proxy access 
		@getter public final boolean isNeedProxy() {
			return this.$view.is_need_proxy;
		}
		@setter public final void setNeedProxy(boolean on) {
			if (this.$view.is_need_proxy != on) {
				this.$view.is_need_proxy = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()		alias operator(210,fy,$cast) { return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		alias operator(210,fy,$cast) { return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public LvalDNodeView	getLvalDNodeView()	alias operator(210,fy,$cast) { return new LvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		alias operator(210,fy,$cast) { return new JLvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public JDNodeView		getJDNodeView()		alias operator(210,fy,$cast) { return new JLvalDNodeView((LvalDNodeImpl)this.$v_impl); }
	public JLvalDNodeView	getJLvalDNodeView()	alias operator(210,fy,$cast) { return new JLvalDNodeView((LvalDNodeImpl)this.$v_impl); }

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
	public static view ENodeView of ENodeImpl extends NodeView {
		public ENodeView(ENodeImpl $view) {
			super($view);
		}

		public final ENode getENode() { return (ENode)this.getNode(); }
		
		//
		// Expr specific
		//
	
		// use no proxy	
		public final boolean isUseNoProxy() {
			return this.$view.is_expr_use_no_proxy;
		}
		public final void setUseNoProxy(boolean on) {
			if (this.$view.is_expr_use_no_proxy != on) {
				this.$view.is_expr_use_no_proxy = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// use as field (disable setter/getter calls for virtual fields)
		public final boolean isAsField() {
			return this.$view.is_expr_as_field;
		}
		public final void setAsField(boolean on) {
			if (this.$view.is_expr_as_field != on) {
				this.$view.is_expr_as_field = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// expression will generate void value
		public final boolean isGenVoidExpr() {
			return this.$view.is_expr_gen_void;
		}
		public final void setGenVoidExpr(boolean on) {
			if (this.$view.is_expr_gen_void != on) {
				this.$view.is_expr_gen_void = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// used bt for()
		public final boolean isForWrapper() {
			return this.$view.is_expr_for_wrapper;
		}
		public final void setForWrapper(boolean on) {
			if (this.$view.is_expr_for_wrapper != on) {
				this.$view.is_expr_for_wrapper = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for primary expressions, i.e. (a+b)
		public final boolean isPrimaryExpr() {
			return this.$view.is_expr_primary;
		}
		public final void setPrimaryExpr(boolean on) {
			if (this.$view.is_expr_primary != on) {
				this.$view.is_expr_primary = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for super-expressions, i.e. (super.foo or super.foo())
		public final boolean isSuperExpr() {
			return this.$view.is_expr_super;
		}
		public final void setSuperExpr(boolean on) {
			if (this.$view.is_expr_super != on) {
				this.$view.is_expr_super = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for cast calls (to check for null)
		public final boolean isCastCall() {
			return this.$view.is_expr_cast_call;
		}
		public final void setCastCall(boolean on) {
			if (this.$view.is_expr_cast_call != on) {
				this.$view.is_expr_cast_call = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}

	
		//
		// Statement specific flags
		//
		
		// abrupted
		public final boolean isAbrupted() {
			return this.$view.is_stat_abrupted;
		}
		public final void setAbrupted(boolean on) {
			if (this.$view.is_stat_abrupted != on) {
				this.$view.is_stat_abrupted = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// breaked
		public final boolean isBreaked() {
			return this.$view.is_stat_breaked;
		}
		public final void setBreaked(boolean on) {
			if (this.$view.is_stat_breaked != on) {
				this.$view.is_stat_breaked = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// method-abrupted
		public final boolean isMethodAbrupted() {
			return this.$view.is_stat_method_abrupted;
		}
		public final void setMethodAbrupted(boolean on) {
			if (this.$view.is_stat_method_abrupted != on) {
				this.$view.is_stat_method_abrupted = on;
				if (on) this.$view.is_stat_abrupted = true;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// auto-returnable
		public final boolean isAutoReturnable() {
			return this.$view.is_stat_auto_returnable;
		}
		public final void setAutoReturnable(boolean on) {
			if (this.$view.is_stat_auto_returnable != on) {
				this.$view.is_stat_auto_returnable = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// break target
		public final boolean isBreakTarget() {
			return this.$view.is_stat_break_target;
		}
		public final void setBreakTarget(boolean on) {
			if (this.$view.is_stat_break_target != on) {
				this.$view.is_stat_break_target = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	public NodeView			getNodeView()			alias operator(210,fy,$cast) { return new ENodeView((ENodeImpl)this.$v_impl); }
	public ENodeView		getENodeView()			alias operator(210,fy,$cast) { return new ENodeView((ENodeImpl)this.$v_impl); }
	public JNodeView		getJNodeView()			alias operator(210,fy,$cast) { return new JENodeView((ENodeImpl)this.$v_impl); }
	public JENodeView		getJENodeView()			alias operator(210,fy,$cast) { return new JENodeView((ENodeImpl)this.$v_impl); }

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode() { super(new ENodeImpl()); }
	public ENode(int pos) { super(new ENodeImpl(pos)); }
	public ENode(ENodeImpl impl) { super(impl); }

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public void resolve(Type reqType) {
		throw new CompilerException(this,"Resolve call for e-node "+getClass());
	}
	
//	public void generate(Code code, Type reqType) {
//		this.getJENodeView().generate(code, reqType);
//	}

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
	// used bt for()
	public boolean isForWrapper() { return this.getENodeView().isForWrapper(); }
	public void setForWrapper(boolean on) { this.getENodeView().setForWrapper(on); }
	// used for primary expressions, i.e. (a+b)
	public boolean isPrimaryExpr() { return this.getENodeView().isPrimaryExpr(); }
	public void setPrimaryExpr(boolean on) { this.getENodeView().setPrimaryExpr(on); }
	// used for super-expressions, i.e. (super.foo or super.foo())
	public boolean isSuperExpr() { return this.getENodeView().isSuperExpr(); }
	public void setSuperExpr(boolean on) { this.getENodeView().setSuperExpr(on); }
	// used for cast calls (to check for null)
	public boolean isCastCall() { return this.getENodeView().isCastCall(); }
	public void setCastCall(boolean on) { this.getENodeView().setCastCall(on); }

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

	@node
	public static final class VarDeclImpl extends ENodeImpl {
		public VarDeclImpl() {}

		@att public Var var;
	
	}
	@nodeview
	public static final view VarDeclView of VarDeclImpl extends ENodeView {
		public Var		var;
	}

	@att public virtual abstract	Var		var;
	
	public NodeView		getNodeView()		{ return new VarDeclView((VarDeclImpl)this.$v_impl); }
	public ENodeView	getENodeView()		{ return new VarDeclView((VarDeclImpl)this.$v_impl); }
	public VarDeclView	getVarDeclView()	{ return new VarDeclView((VarDeclImpl)this.$v_impl); }
	public JNodeView	getJNodeView()		{ return new JVarDeclView((VarDeclImpl)this.$v_impl); }
	public JENodeView	getJENodeView()		{ return new JVarDeclView((VarDeclImpl)this.$v_impl); }
	public JVarDeclView	getJVarDeclView()	{ return new JVarDeclView((VarDeclImpl)this.$v_impl); }

	@getter public Var		get$var()				{ return this.getVarDeclView().var; }
	@setter public void		set$var(Var val)		{ this.getVarDeclView().var = val; }
	
	public VarDecl() { super(new VarDeclImpl()); }
	
	public VarDecl(Var var) {
		super(new VarDeclImpl());
		this.var = var;
	}

	public void resolve(Type reqType) {
		var.resolveDecl();
	}

	public NodeName getName() { return var.name; }

	public Dumper toJava(Dumper dmp) {
		var.toJavaDecl(dmp);
		return dmp;
	}
	
}

@node
public final class LocalStructDecl extends ENode implements Named {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static final class LocalStructDeclImpl extends ENodeImpl {
		public LocalStructDeclImpl() {}

		@att public Struct clazz;
	
	}
	@nodeview
	public static final view LocalStructDeclView of LocalStructDeclImpl extends ENodeView {
		public Struct		clazz;
	}

	@att public abstract virtual Struct clazz;
	
	public NodeView					getNodeView()				{ return new LocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new LocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }
	public LocalStructDeclView		getLocalStructDeclView()	{ return new LocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }
	public JNodeView				getJNodeView()				{ return new JLocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }
	public JENodeView				getJENodeView()				{ return new JLocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }
	public JLocalStructDeclView		getJLocalStructDeclView()	{ return new JLocalStructDeclView((LocalStructDeclImpl)this.$v_impl); }

	@getter public Struct	get$clazz()					{ return this.getLocalStructDeclView().clazz; }
	@setter public void		set$clazz(Struct val)		{ this.getLocalStructDeclView().clazz = val; }
	
	
	public LocalStructDecl() { super(new LocalStructDeclImpl()); }
	public LocalStructDecl(Struct clazz) {
		super(new LocalStructDeclImpl());
		this.clazz = clazz;
		clazz.setResolved(true);
	}

	public boolean preResolveIn(TransfProcessor proc) {
		if( ctx_method==null || ctx_method.isStatic())
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

	public Dumper toJava(Dumper dmp) {
		clazz.toJavaDecl(dmp);
		return dmp;
	}
}


@node
public final class NopExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	@node
	public static final class NopExprImpl extends ENodeImpl {
		public NopExprImpl() {}
		@att public ENode	expr;
	}
	@nodeview
	public static final view NopExprView of NopExprImpl extends ENodeView {
		public ENode		expr;
	}

	@att public abstract virtual ENode expr;
	
	public NodeView			getNodeView()		{ return new NopExprView((NopExprImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new NopExprView((NopExprImpl)this.$v_impl); }
	public NopExprView		getNopExprView()	{ return new NopExprView((NopExprImpl)this.$v_impl); }

	@getter public ENode	get$expr()				{ return this.getNopExprView().expr; }
	@setter public void		set$expr(ENode val)		{ this.getNopExprView().expr = val; }
	
	public NopExpr() { super(new NopExprImpl()); }
	public NopExpr(ENode expr) {
		super(new NopExprImpl());
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

//@node
//public final class InitializerShadow extends ENode {
//
//	@dflow(out="this:in") private static class DFI {}
//
//	@ref Initializer init;
//	
//	public InitializerShadow() {}
//	public InitializerShadow(Initializer init) {
//		this.init = init;
//		this.setResolved(true);
//	}
//	public void resolve(Type reqType) {
//	}
//
//	public void generate(Code code, Type reqType) {
//		init.generate(code,reqType);
//	}
//	public Dumper toJava(Dumper dmp) {
//		dmp.append("/* ");
//		init.toJavaDecl(dmp);
//		dmp.append(" */");
//		return dmp;
//	}
//}


@node
public abstract class TypeDef extends DNode implements Named {

	@node
	public static class TypeDefImpl extends DNodeImpl {		
		public TypeDefImpl() {}
		public TypeDefImpl(int pos) { super(pos); }
		public TypeDefImpl(int pos, int fl) { super(pos, fl); }
	}
	@nodeview
	public static view TypeDefView of TypeDefImpl extends DNodeView {
		public TypeDefView(TypeDefImpl $view) {
			super($view);
		}
	}
	public abstract TypeDefView		getTypeDefView();
	public abstract JTypeDefView	getJTypeDefView();

	public TypeDef(TypeDefImpl impl) { super(impl); }

	public abstract NodeName	getName();
	public abstract boolean		checkResolved();
}


@node
public class TypeRef extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@node
	public static class TypeRefImpl extends ENodeImpl {
		public TypeRefImpl() {}
		public TypeRefImpl(int pos, Type tp) { super(pos); this.lnk = tp; }
		@ref public Type	lnk;
	}
	@nodeview
	public static view TypeRefView of TypeRefImpl extends ENodeView {
		public Type	lnk;
	}

	@ref public abstract virtual forward Type	lnk;
	
	public NodeView			getNodeView()		{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public ENodeView		getENodeView()		{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public TypeRefView		getTypeRefView()	{ return new TypeRefView((TypeRefImpl)this.$v_impl); }
	public JNodeView		getJNodeView()		{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }
	public JENodeView		getJENodeView()		{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }
	public JTypeRefView		getJTypeRefView()	{ return new JTypeRefView((TypeRefImpl)this.$v_impl); }

	@getter public Type		get$lnk()			{ return this.getTypeRefView().lnk; }
	@setter public void		set$lnk(Type val)	{ this.getTypeRefView().lnk = val; }
	
	public TypeRef() {
		super(new TypeRefImpl());
	}
	
	public TypeRef(TypeRefImpl $view) {
		super($view);
	}

	public TypeRef(Type tp) {
		super(new TypeRefImpl(0, tp));
	}
	public TypeRef(int pos) {
		super(new TypeRefImpl(pos, null));
	}
	public TypeRef(int pos, Type tp) {
		super(new TypeRefImpl(pos, tp));
	}
	
	public boolean isBound() {
		return lnk != null;
	}
	
	public boolean isArray() { return getType().isArray(); }
	public boolean checkResolved() { return getType().checkResolved(); } 
	public Struct getStruct() { return getType().getStruct(); }
	public JType getJType() { return getType().getJType(); }

	public Type getType()
		alias operator(210,fy,$cast)
	{
//		if (lnk == null)
//			throw new CompilerException(this,"Type "+this+" is not found");
		return lnk;
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
		if (reqType ≢ null && reqType ≉ Type.tpClass)
			toExpr(reqType);
		else
			getType(); // calls resolving
	}
	
	public boolean equals(Object o) {
		if (o instanceof Type) return this.lnk ≡ (Type)o;
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
		Struct s = st.getStruct();
		if (s != null && s.isPizzaCase()) {
			// Pizza case may be casted to int or to itself or super-class
			MetaPizzaCase meta = s.getMetaPizzaCase();
			if (meta == null)
				throw new RuntimeException("Internal error - can't find pizza case meta attr");
			Type tp = Type.getRealType(reqType,st);
			if !(reqType.isInteger() || tp.isInstanceOf(reqType))
				throw new CompilerException(this,"Pizza case "+tp+" cannot be casted to type "+reqType);
			if (meta.getFields().length != 0)
				throw new CompilerException(this,"Empty constructor for pizza case "+tp+" not found");
			if (reqType.isInteger()) {
				ENode expr = new ConstIntExpr(meta.getTag());
				if( reqType ≢ Type.tpInt )
					expr = new CastExpr(pos,reqType,expr);
				replaceWithNodeResolve(reqType, expr);
			}
			else if (s.isSingleton()) {
				replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
			}
			else {
				replaceWithResolve(reqType, fun ()->ENode {return new NewExpr(pos,tp,ENode.emptyArray);});
			}
			return;
		}
		if (s != null && s.isSingleton()) {
			replaceWithNodeResolve(reqType, new SFldExpr(pos, s.resolveField(nameInstance)));
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
	
	public Type getType() { return Type.tpVoid; }

	public KString toKString() alias operator(210,fy,$cast) { return name; }
    
	public String toString() { return name.toString(); }

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
	public CompilerException(ASTNode.NodeView from, String msg) {
		super(msg);
		this.from = from.getNode();
	}
	public CompilerException(ASTNode.NodeView from, CError err_id, String msg) {
		super(msg);
		this.from = from.getNode();
		this.err_id = err_id;
	}
	public CompilerException(JNodeView from, String msg) {
		super(msg);
		this.from = from.getNode();
	}
	public CompilerException(JNodeView from, CError err_id, String msg) {
		super(msg);
		this.from = from.getNode();
		this.err_id = err_id;
	}
}

