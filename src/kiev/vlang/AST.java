package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.ASTNode.NodeImpl;

import kiev.be.java.JType;
import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JENodeView;
import kiev.be.java.JVarDeclView;
import kiev.be.java.JLocalStructDeclView;
import kiev.be.java.JLvalueExprView;
import kiev.be.java.JTypeDeclView;

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

public interface NodeData {
	public KString	getNodeDataId();
	public void nodeAttached(NodeImpl node);
	public void dataAttached(NodeImpl node);
	public void nodeDetached(NodeImpl node);
	public void dataDetached(NodeImpl node);
	public void walkTree(TreeWalker walker);
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
		protected	NodeData[]		ndata;
		// Structures	
		public packed:1,compileflags,16 boolean is_struct_local;
		public packed:1,compileflags,17 boolean is_struct_anomymouse;
		public packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
		public packed:1,compileflags,19 boolean is_struct_members_generated;
		public packed:1,compileflags,20 boolean is_struct_pre_generated;
		public packed:1,compileflags,21 boolean is_struct_statements_generated;
		public packed:1,compileflags,22 boolean is_struct_generated;
		public packed:1,compileflags,23 boolean is_struct_type_resolved;
		public packed:1,compileflags,24 boolean is_struct_args_resolved;
		public packed:1,compileflags,25 boolean is_struct_bytecode;	// struct was loaded from bytecode
		public packed:1,compileflags,26 boolean is_struct_singleton;
		public packed:1,compileflags,27 boolean is_struct_pizza_case;
		
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
		public packed:1,compileflags,21 boolean is_mth_dispatcher;
		public packed:1,compileflags,22 boolean is_mth_invariant;
		
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
		
		public final ASTNode getNode() {
			return this._self;
		}
		
		public AttrSlot[] values() {
			return AttrSlot.emptyArray;
		}
		public Object getVal(String name) {
			throw new RuntimeException("No @att value \"" + name + "\" in NodeImpl");
		}
		public void setVal(String name, Object val) {
			throw new RuntimeException("No @att value \"" + name + "\" in NodeImpl");
		}
		
		public Object copyTo(Object to$node) {
			NodeImpl node = (NodeImpl)to$node;
			node.pos			= this.pos;
			node.compileflags	= this.compileflags;
			if (this.ndata != null)
				node.ndata = (NodeData[])this.ndata.clone();
			return node;
		}

		// the node is attached
		public final boolean isAttached()  {
			return parent != null;
		}
		public final void callbackDetached() {
			assert(isAttached());
			// notify node data that we are detached
			NodeData[] ndata = this.ndata;
			if (ndata != null) {
				foreach (NodeData nd; ndata)
					nd.nodeDetached(this);
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
			NodeData[] ndata = this.ndata;
			if (ndata != null) {
				foreach (NodeData nd; ndata)
					nd.nodeAttached(this);
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

		public NodeData getNodeData(KString id) {
			if (ndata != null) {
				foreach (NodeData nd; ndata) {
					if (nd.getNodeDataId() == id)
						return nd;
				}
			}
			return null;
		}
		
		public void addNodeData(NodeData d) {
			if (ndata != null) {
				KString id = d.getNodeDataId();
				NodeData[] ndata = this.ndata;
				int sz = ndata.length;
				for (int i=0; i < sz; i++) {
					NodeData nd = ndata[i];
					if (nd.getNodeDataId() == id) {
						if (nd == d)
							return;
						nd.dataDetached(this);
						d.dataAttached(this);
						return;
					}
				}
				NodeData[] tmp = new NodeData[sz+1];
				for (int i=0; i < sz; i++)
					tmp[i] = ndata[i];
				tmp[sz] = d;
				this.ndata = tmp;
			} else {
				this.ndata = new NodeData[]{d};
			}
			d.dataAttached(this);
		}
		
		public void delNodeData(KString id) {
			NodeData[] ndata = this.ndata;
			if (ndata != null) {
				int sz = ndata.length-1;
				for (int idx=0; idx <= sz; idx++) {
					NodeData nd = ndata[idx];
					if (nd.getNodeDataId() == id) {
						NodeData[] tmp   = new NodeData[sz];
						nd.dataDetached(this);
						int i;
						for (i=0; i < idx; i++) tmp[i] = ndata[i];
						for (   ; i <  sz; i++) tmp[i] = ndata[i+1];
						this.ndata = tmp;
						return;
					}
				}
			}
		}

		public final void walkTree(TreeWalker walker) {
			foreach (AttrSlot attr; this.values(); attr.is_attr) {
				Object val = this.getVal(attr.name);
				if (val == null)
					continue;
				if (attr.is_space) {
					foreach (ASTNode n; (NArr<ASTNode>)val)
						n.walkTree(walker);
				}
				else if (val instanceof ASTNode) {
					val.walkTree(walker);
				}
			}
			if (ndata != null) {
				foreach (NodeData nd; this.ndata)
					nd.walkTree(walker);
			}
		}

		// build data flow for this node
		final DataFlowInfo getDFlow() {
			DataFlowInfo df = (DataFlowInfo)getNodeData(DataFlowInfo.ID);
			if (df == null) {
				df = DataFlowInfo.newDataFlowInfo(this);
				this.addNodeData(df);
			}
			return df;
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
	
	@setter public void set$pos(int val)			{ this.getNodeView().pos = val; }
	@setter public void set$compileflags(int val)	{ this.getNodeView().compileflags = val; }
	@setter public void set$parent(ASTNode val)	{ this.getNodeView().parent = val; }
	@setter public void set$pslot(AttrSlot val)	{ this.getNodeView().pslot = val; }
	@setter public void set$pprev(ASTNode val)		{ this.getNodeView().pprev = val; }
	@setter public void set$pnext(ASTNode val)		{ this.getNodeView().pnext = val; }

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
		return this.$v_impl.getNodeData(id);
	}
	
	public void addNodeData(NodeData d) {
		this.$v_impl.addNodeData(d);
	}
	
	public void delNodeData(KString id) {
		this.$v_impl.delNodeData(id);
	}
	
	public void cleanDFlow() {
		walkTree(new TreeWalker() {
			public boolean pre_exec(ASTNode n) { n.delNodeData(DataFlowInfo.ID); return true; }
		});
	}
	
	// build data flow for this node
	public final DataFlowInfo getDFlow() {
		return this.$v_impl.getDFlow();
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
	public boolean preVerify() { return true; }
	public void postVerify() {}
	public boolean preGenerate() { return true; }
	
	
	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this))
			this.$v_impl.walkTree(walker);
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

//		public packed:1,flags, 0 boolean is_acc_public;
//		public packed:1,flags, 1 boolean is_acc_private;
//		public packed:1,flags, 2 boolean is_acc_protected;
		public packed:3,flags, 0 int     is_access;

		public packed:1,flags, 3 boolean is_static;
		public packed:1,flags, 4 boolean is_final;
		public packed:1,flags, 5 boolean is_mth_synchronized;	// method
		public packed:1,flags, 5 boolean is_struct_super;		// struct
		public packed:1,flags, 6 boolean is_fld_volatile;		// field
		public packed:1,flags, 6 boolean is_mth_bridge;		// method
		public packed:1,flags, 7 boolean is_fld_transient;		// field
		public packed:1,flags, 7 boolean is_mth_varargs;		// method
		public packed:1,flags, 8 boolean is_mth_native;
		public packed:1,flags, 9 boolean is_struct_interface;
		public packed:1,flags,10 boolean is_abstract;
		public packed:1,flags,11 boolean is_math_strict;		// strict math
		public packed:1,flags,12 boolean is_synthetic;			// any decl that was generated (not in sources)
		public packed:1,flags,13 boolean is_struct_annotation;
		public packed:1,flags,14 boolean is_struct_enum;		// struct
		public packed:1,flags,14 boolean is_fld_enum;			// field
		
		// Flags temporary used with java flags
		public packed:1,flags,16 boolean is_forward;			// var/field/method, type is wrapper
		public packed:1,flags,17 boolean is_virtual;			// var/field, method is 'static virtual', struct is 'view'
		public packed:1,flags,18 boolean is_type_unerasable;	// typedecl, method/struct as parent of typedef
		
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

		private static final int MASK_ACC_DEFAULT   = 0;
		private static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
		private static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
		private static final int MASK_ACC_PROTECTED = ACC_PROTECTED;
		private static final int MASK_ACC_NAMESPACE = ACC_PACKAGE;
		private static final int MASK_ACC_SYNTAX    = ACC_SYNTAX;
		
		public final DNode getDNode() { return (DNode)getNode(); }
		public Dumper toJavaDecl(Dumper dmp) { return getDNode().toJavaDecl(dmp); }
		
		public int		flags;
		public MetaSet	meta;

		public final boolean isPublic()				{ return this.$view.is_access == MASK_ACC_PUBLIC; }
		public final boolean isPrivate()			{ return this.$view.is_access == MASK_ACC_PRIVATE; }
		public final boolean isProtected()			{ return this.$view.is_access == MASK_ACC_PROTECTED; }
		public final boolean isPkgPrivate()		{ return this.$view.is_access == MASK_ACC_DEFAULT; }
		public final boolean isStatic()				{ return this.$view.is_static; }
		public final boolean isFinal()				{ return this.$view.is_final; }
		public final boolean isSynchronized()		{ return this.$view.is_mth_synchronized; }
		public final boolean isVolatile()			{ return this.$view.is_fld_volatile; }
		public final boolean isFieldVolatile()		{ return this.$view.is_fld_volatile; }
		public final boolean isMethodBridge()		{ return this.$view.is_mth_bridge; }
		public final boolean isFieldTransient()	{ return this.$view.is_fld_transient; }
		public final boolean isMethodVarargs()		{ return this.$view.is_mth_varargs; }
		public final boolean isStructBcLoaded()	{ return this.$view.is_struct_bytecode; }
		public final boolean isMethodNative()		{ return this.$view.is_mth_native; }
		public final boolean isInterface()			{ return this.$view.is_struct_interface; }
		public final boolean isAbstract()			{ return this.$view.is_abstract; }
		
		public final boolean isStructView()		{ return this.$view.is_virtual; }
		public final boolean isTypeUnerasable()	{ return this.$view.is_type_unerasable; }
		public final boolean isPackage()			{ return this.$view.is_access == MASK_ACC_NAMESPACE; }
		public final boolean isSyntax()				{ return this.$view.is_access == MASK_ACC_SYNTAX; }

		public void setPublic() {
			if (this.$view.is_access != MASK_ACC_PUBLIC) {
				this.$view.is_access = MASK_ACC_PUBLIC;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setPrivate() {
			if (this.$view.is_access != MASK_ACC_PRIVATE) {
				this.$view.is_access = MASK_ACC_PRIVATE;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setProtected() {
			if (this.$view.is_access != MASK_ACC_PROTECTED) {
				this.$view.is_access = MASK_ACC_PROTECTED;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setPkgPrivate() {
			if (this.$view.is_access != MASK_ACC_DEFAULT) {
				this.$view.is_access = MASK_ACC_DEFAULT;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public final void setPackage() {
			if (this.$view.is_access != MASK_ACC_NAMESPACE) {
				this.$view.is_access = MASK_ACC_NAMESPACE;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public final void setSyntax() {
			if (this.$view.is_access != MASK_ACC_SYNTAX) {
				this.$view.is_access = MASK_ACC_SYNTAX;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}

		public void setStatic(boolean on) {
			if (this.$view.is_static != on) {
				this.$view.is_static = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFinal(boolean on) {
			if (this.$view.is_final != on) {
				this.$view.is_final = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setSynchronized(boolean on) {
			if (this.$view.is_mth_synchronized != on) {
				this.$view.is_mth_synchronized = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setVolatile(boolean on) {
			if (this.$view.is_fld_volatile != on) {
				this.$view.is_fld_volatile = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFieldVolatile(boolean on) {
			if (this.$view.is_fld_volatile != on) {
				this.$view.is_fld_volatile = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodBridge(boolean on) {
			if (this.$view.is_mth_bridge != on) {
				this.$view.is_mth_bridge = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFieldTransient(boolean on) {
			if (this.$view.is_fld_transient != on) {
				this.$view.is_fld_transient = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodVarargs(boolean on) {
			if (this.$view.is_mth_varargs != on) {
				this.$view.is_mth_varargs = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodNative(boolean on) {
			if (this.$view.is_mth_native != on) {
				this.$view.is_mth_native = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setInterface(boolean on) {
			if (this.$view.is_struct_interface != on) {
				this.$view.is_struct_interface = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setAbstract(boolean on) {
			if (this.$view.is_abstract != on) {
				this.$view.is_abstract = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}

		public void setStructView() {
			if (!this.$view.is_virtual) {
				this.$view.is_virtual = true;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setTypeUnerasable(boolean on) {
			if (this.$view.is_type_unerasable != on) {
				this.$view.is_type_unerasable = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}

		public final boolean isVirtual() {
			return this.$view.is_virtual;
		}
		public final void setVirtual(boolean on) {
			if (this.$view.is_virtual != on) {
				this.$view.is_virtual = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}

		@getter public final boolean isForward() {
			return this.$view.is_forward;
		}
		@setter public final void setForward(boolean on) {
			if (this.$view.is_forward != on) {
				this.$view.is_forward = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
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
	public boolean isPkgPrivate()		{ return this.getDNodeView().isPkgPrivate(); }
	public boolean isStatic()			{ return this.getDNodeView().isStatic(); }
	public boolean isFinal()			{ return this.getDNodeView().isFinal(); }
	public boolean isSynchronized()		{ return this.getDNodeView().isSynchronized(); }
	public boolean isVolatile()			{ return this.getDNodeView().isVolatile(); }
	public boolean isFieldVolatile()	{ return this.getDNodeView().isFieldVolatile(); }
	public boolean isMethodBridge()		{ return this.getDNodeView().isMethodBridge(); }
	public boolean isFieldTransient()	{ return this.getDNodeView().isFieldTransient(); }
	public boolean isMethodVarargs()	{ return this.getDNodeView().isMethodVarargs(); }
	public boolean isStructBcLoaded()	{ return this.getDNodeView().isStructBcLoaded(); }
	public boolean isMethodNative()		{ return this.getDNodeView().isMethodNative(); }
	public boolean isInterface()		{ return this.getDNodeView().isInterface(); }
	public boolean isAbstract()			{ return this.getDNodeView().isAbstract(); }

	public boolean isStructView()		{ return this.getDNodeView().isStructView(); }
	public boolean isTypeUnerasable()	{ return this.getDNodeView().isTypeUnerasable(); }
	public boolean isPackage()			{ return this.getDNodeView().isPackage(); }
	public boolean isSyntax()			{ return this.getDNodeView().isSyntax(); }
	public boolean isVirtual() 			{ return this.getDNodeView().isVirtual(); }
	public boolean isForward()			{ return this.getDNodeView().isForward(); }

	public void setPublic()						{ this.getDNodeView().setPublic(); }
	public void setPrivate()					{ this.getDNodeView().setPrivate(); }
	public void setProtected()					{ this.getDNodeView().setProtected(); }
	public void setPkgPrivate()					{ this.getDNodeView().setPkgPrivate(); }
	public void setStatic(boolean on)			{ this.getDNodeView().setStatic(on); }
	public void setFinal(boolean on)			{ this.getDNodeView().setFinal(on); }
	public void setSynchronized(boolean on)	{ this.getDNodeView().setSynchronized(on); }
	public void setVolatile(boolean on)		{ this.getDNodeView().setVolatile(on); }
	public void setFieldVolatile(boolean on)	{ this.getDNodeView().setFieldVolatile(on); }
	public void setMethodBridge(boolean on)	{ this.getDNodeView().setMethodBridge(on); }
	public void setFieldTransient(boolean on)	{ this.getDNodeView().setFieldTransient(on); }
	public void setMethodVarargs(boolean on)	{ this.getDNodeView().setMethodVarargs(on); }
	public void setMethodNative(boolean on)	{ this.getDNodeView().setMethodNative(on); }
	public void setInterface(boolean on)		{ this.getDNodeView().setInterface(on); }
	public void setAbstract(boolean on)		{ this.getDNodeView().setAbstract(on); }
	
	public void setTypeUnerasable(boolean on)	{ this.getDNodeView().setTypeUnerasable(on); }
	public void setStructView()					{ this.getDNodeView().setStructView(); }
	public void setPackage()					{ this.getDNodeView().setPackage(); }
	public void setSyntax()						{ this.getDNodeView().setSyntax(); }
	public void setVirtual(boolean on)			{ this.getDNodeView().setVirtual(on); }
	public void setForward(boolean on)			{ this.getDNodeView().setForward(on); }
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
public final class NopExpr extends ENode implements NodeData {

	public static final KString ID = KString.from("temp expr");
	public static final AttrSlot tempAttrSlot = new AttrSlot("temp expr",true,false,ENode.class);	

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
	
	public final KString getNodeDataId() { return ID; }
	public void nodeAttached(NodeImpl node) {}
	public void dataAttached(NodeImpl node) { this.callbackAttached(node.getNode(), tempAttrSlot); }
	public void nodeDetached(NodeImpl node) {}
	public void dataDetached(NodeImpl node) { this.callbackDetached(); }
	
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
public abstract class TypeDecl extends DNode implements Named {

	@node
	public static abstract class TypeDeclImpl extends DNodeImpl {		
		public TypeDeclImpl() {}
		public TypeDeclImpl(int pos) { super(pos); }
		public TypeDeclImpl(int pos, int fl) { super(pos, fl); }

		public void callbackSuperTypeChanged(TypeDeclImpl chg) {}
	}
	@nodeview
	public static view TypeDeclView of TypeDeclImpl extends DNodeView {
		public TypeDeclView(TypeDeclImpl $view) {
			super($view);
		}
		public void callbackSuperTypeChanged(TypeDeclImpl chg) {
			this.$view.callbackSuperTypeChanged(chg);
		}
	}
	public abstract TypeDeclView	getTypeDeclView();
	public abstract JTypeDeclView	getJTypeDeclView();

	public TypeDecl(TypeDeclImpl impl) { super(impl); }

	public void callbackSuperTypeChanged(TypeDeclImpl chg) {
		this.getTypeDeclView().callbackSuperTypeChanged(chg);
	}
		
	public abstract NodeName	getName();
	public abstract boolean		checkResolved();
	public abstract Type		getSuperType();
	public abstract Struct		getStruct();

	public final boolean isTypeAbstract()		{ return this.isAbstract(); }
	public final boolean isTypeVirtual()		{ return this.isVirtual(); }
	public final boolean isTypeFinal()			{ return this.isFinal(); }
	public final boolean isTypeStatic()		{ return this.isStatic(); }
	public final boolean isTypeForward()		{ return this.isForward(); }
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

