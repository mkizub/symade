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
import kiev.be.java.JNode;
import kiev.be.java.JDNode;
import kiev.be.java.JLvalDNode;
import kiev.be.java.JENode;
import kiev.ir.java.RNopExpr;
import kiev.be.java.JVarDecl;
import kiev.ir.java.RVarDecl;
import kiev.be.java.JLocalStructDecl;
import kiev.ir.java.RLocalStructDecl;
import kiev.be.java.JTypeDecl;
import kiev.be.java.JNameRef;

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
	public NodeData nodeCopiedTo(NodeImpl node);
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

@nodeset
public abstract class ASTNode implements Constants, Cloneable {

	@virtual typedef This  = ASTNode;
	@virtual typedef NImpl = NodeImpl;
	@virtual typedef VView = NodeView;
	@virtual typedef JView = JNode;
	@virtual typedef RView = VView;
	
	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	@nodeimpl
	public static abstract class NodeImpl {
		@virtual typedef ImplOf  = ASTNode;
		public		ImplOf			_self;		
		public		int				pos;
		public		int				compileflags;
		public		ASTNode			parent;
		protected	AttrSlot		pslot;
		protected	ASTNode			pprev;
		protected	ASTNode			pnext;
		protected	NodeData[]		ndata;
		// Structures	
		public @packed:1,compileflags,16 boolean is_struct_local;
		public @packed:1,compileflags,17 boolean is_struct_anomymouse;
		public @packed:1,compileflags,18 boolean is_struct_has_pizza_cases;
		public @packed:1,compileflags,19 boolean is_struct_members_generated;
		public @packed:1,compileflags,20 boolean is_struct_pre_generated;
		public @packed:1,compileflags,21 boolean is_struct_statements_generated;
		public @packed:1,compileflags,22 boolean is_struct_generated;
		public @packed:1,compileflags,23 boolean is_struct_type_resolved;
		public @packed:1,compileflags,24 boolean is_struct_args_resolved;
		public @packed:1,compileflags,25 boolean is_struct_bytecode;	// struct was loaded from bytecode
		public @packed:1,compileflags,26 boolean is_struct_singleton;
		public @packed:1,compileflags,27 boolean is_struct_pizza_case;
		
		// Expression flags
		public @packed:1,compileflags,16 boolean is_expr_use_no_proxy;
		public @packed:1,compileflags,17 boolean is_expr_as_field;
		public @packed:1,compileflags,18 boolean is_expr_gen_void;
		public @packed:1,compileflags,19 boolean is_expr_for_wrapper;
		public @packed:1,compileflags,20 boolean is_expr_primary;
		public @packed:1,compileflags,21 boolean is_expr_super;
		public @packed:1,compileflags,22 boolean is_expr_cast_call;
		// Statement flags
		public @packed:1,compileflags,23 boolean is_stat_abrupted;
		public @packed:1,compileflags,24 boolean is_stat_breaked;
		public @packed:1,compileflags,25 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
		public @packed:1,compileflags,26 boolean is_stat_auto_returnable;
		public @packed:1,compileflags,27 boolean is_stat_break_target;
		
		// Method flags
		public @packed:1,compileflags,17 boolean is_mth_virtual_static;
		public @packed:1,compileflags,18 boolean is_mth_operator;
		public @packed:1,compileflags,19 boolean is_mth_need_fields_init;
		public @packed:1,compileflags,20 boolean is_mth_local;
		public @packed:1,compileflags,21 boolean is_mth_dispatcher;
		public @packed:1,compileflags,22 boolean is_mth_invariant;
		
		// Var/field
		public @packed:1,compileflags,16 boolean is_init_wrapper;
		public @packed:1,compileflags,17 boolean is_need_proxy;
		// Var specific
		public @packed:1,compileflags,18 boolean is_var_local_rule_var;
		public @packed:1,compileflags,19 boolean is_var_closure_proxy;
		public @packed:1,compileflags,20 boolean is_var_this;
		public @packed:1,compileflags,21 boolean is_var_super;
	
		// Field specific
		public @packed:1,compileflags,18 boolean is_fld_packer;
		public @packed:1,compileflags,19 boolean is_fld_packed;
		public @packed:1,compileflags,20 boolean is_fld_added_to_init;
	
		// General flags
		public @packed:1,compileflags,28 boolean is_accessed_from_inner;
		public @packed:1,compileflags,29 boolean is_resolved;
		public @packed:1,compileflags,30 boolean is_hidden;
		public @packed:1,compileflags,31 boolean is_bad;

		public NodeImpl() {}
		
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
			if (this.ndata != null) {
				for (int i=0; i < this.ndata.length; i++) {
					NodeData nd = this.ndata[i].nodeCopiedTo(node);
					if (nd == null)
						continue;
					if (node.ndata == null) {
						node.ndata = new NodeData[]{nd};
					} else {
						int sz = node.ndata.length;
						NodeData[] tmp = new NodeData[sz+1];
						for (int j=0; j < sz; j++)
							tmp[j] = node.ndata[j];
						tmp[sz] = nd;
						node.ndata = tmp;
					}
				}
			}
			return node;
		}

		public final int getPosLine() { return pos >>> 11; }
		
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
		
		public final void callbackAttached(NodeImpl parent, AttrSlot pslot) {
			assert(!isAttached());
			assert(parent != null && parent != this);
			// do attach
			this.parent = parent._self;
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
					NArr<ASTNode> vals = (NArr<ASTNode>)val;
					for (int i=0; i < vals.length; i++) {
						try {
							vals[i].walkTree(walker);
						} catch (ReWalkNodeException e) { i--; }
					}
				}
				else if (val instanceof ASTNode) {
				re_walk_node:;
					try {
						val.walkTree(walker);
					} catch (ReWalkNodeException e) {
						val = this.getVal(attr.name);
						if (val != null)
							goto re_walk_node;
					}
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
	
		public final ASTNode replaceWithNode(ASTNode node) {
			assert(isAttached());
			if (pslot.is_space) {
				assert(node != null);
				NArr<ASTNode> space = (NArr<ASTNode>)parent.getVal(pslot.name);
				int idx = space.indexOf(this.getNode());
				assert(idx >= 0);
				if (node.pos == 0) node.pos = this.pos;
				space[idx] = node;
			} else {
				assert(parent.getVal(pslot.name) == this._self);
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
				int idx = space.indexOf(this.getNode());
				assert(idx >= 0);
				space[idx] = this._self.getDummyNode();
				ASTNode n = fnode();
				assert(n != null);
				if (n.pos == 0) n.pos = this.pos;
				space[idx] = n;
				assert(n.isAttached());
				return n;
			} else {
				assert(parent.getVal(pslot.name) == this._self);
				parent.setVal(pslot.name, this._self.getDummyNode());
				ASTNode n = fnode();
				if (n != null && n.pos == 0) n.pos = this.pos;
				parent.setVal(pslot.name, n);
				assert(n == null || n.isAttached());
				return n;
			}
		}

		// break target (ENodes)
		public final boolean isBreakTarget() {
			return this.is_stat_break_target;
		}
		public final void setBreakTarget(boolean on) {
			if (this.is_stat_break_target != on) {
				this.is_stat_break_target = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// the (private) field/method/struct is accessed from inner class (and needs proxy access)
		@getter public final boolean isAccessedFromInner() {
			return this.is_accessed_from_inner;
		}
		@setter public final void setAccessedFromInner(boolean on) {
			if (this.is_accessed_from_inner != on) {
				this.is_accessed_from_inner = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// resolved
		@getter public final boolean isResolved() {
			return this.is_resolved;
		}
		@setter public final void setResolved(boolean on) {
			if (this.is_resolved != on) {
				this.is_resolved = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// hidden
		@getter public final boolean isHidden() {
			return this.is_hidden;
		}
		@setter public final void setHidden(boolean on) {
			if (this.is_hidden != on) {
				this.is_hidden = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// bad
		@getter public final boolean isBad() {
			return this.is_bad;
		}
		@setter public final void setBad(boolean on) {
			if (this.is_bad != on) {
				this.is_bad = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	@nodeview
	public static abstract view NodeView of NodeImpl implements Constants {
		public final ASTNode getNode() { return ((NodeImpl)this)._self; }
		public String toString() { return String.valueOf(getNode()); }
		public Dumper toJava(Dumper dmp) { return getNode().toJava(dmp); }
		
		public int			pos;
		public int			compileflags;
		public ASTNode		parent;
		public AttrSlot		pslot;
		public ASTNode		pprev;
		public ASTNode		pnext;
		
		@getter public final ASTNode get$ctx_root() {
			ASTNode parent = this.parent;
			if (parent == null)
				return this.getNode();
			return parent.get$ctx_root();
		}
		@getter public FileUnit get$ctx_file_unit() { return this.parent.get$ctx_file_unit(); }
		@getter public Struct get$ctx_clazz() { return this.parent.child_ctx_clazz; }
		@getter public Struct get$child_ctx_clazz() { return this.parent.get$child_ctx_clazz(); }
		@getter public Method get$ctx_method() { return this.parent.child_ctx_method; }
		@getter public Method get$child_ctx_method() { return this.parent.get$child_ctx_method(); }

		public AttrSlot[] values();
		public Object getVal(String name);
		public void setVal(String name, Object val);
		public final void callbackDetached();
		public final void callbackAttached(NodeImpl parent, AttrSlot pslot) { ((NodeImpl)this).callbackAttached(parent, pslot); }
		public final void callbackChildChanged(AttrSlot attr);
		public final void callbackRootChanged();
		public final NodeData getNodeData(KString id);
		public final void addNodeData(NodeData d);
		public final void delNodeData(KString id);
		public DataFlowInfo getDFlow();
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

		public final void cleanDFlow() {
			walkTree(new TreeWalker() {
				public boolean pre_exec(ASTNode n) { n.delNodeData(DataFlowInfo.ID); return true; }
			});
		}
	
		public final void walkTree(TreeWalker walker) {
			if (walker.pre_exec(getNode()))
				((NodeImpl)this).walkTree(walker);
			walker.post_exec(getNode());
		}

		public boolean preResolveIn() { return true; }
		public void preResolveOut() {}
		public boolean mainResolveIn() { return true; }
		public void mainResolveOut() {}
		public boolean preVerify() { return true; }
		public void postVerify() {}

		public boolean preGenerate() { return true; }
	}
	
	public NImpl $v_impl;
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }
	
	@virtual @forward public:ro abstract VView theView;
	@getter public final VView get$theView() { return getVView(); }
	
	public ASTNode(NImpl v_impl) {
		this.$v_impl = v_impl;
		this.$v_impl._self = this;
	}

	public final This detach()
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
	
	public abstract ASTNode getDummyNode();
	
	public final This ncopy() {
		return (This)this.copy();
	}
	public Object copy() {
		ASTNode node = (ASTNode)this.clone();
		node.$v_impl = this.$v_impl.getClass().newInstance();
		node.$v_impl._self = node;
		this.$v_impl.copyTo(node.$v_impl);
		return node;
	};

	public Type getType() { return Type.tpVoid; }

    public Dumper toJava(Dumper dmp) {
    	dmp.append("/* INTERNAL ERROR - ").append(this.getClass().toString()).append(" */");
    	return dmp;
    }
	
	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public final boolean preGenerate() { return getRView().preGenerate(); }
	
}

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
@nodeset
public abstract class DNode extends ASTNode {

	@virtual typedef This  = DNode;
	@virtual typedef NImpl = DNodeImpl;
	@virtual typedef VView = DNodeView;
	@virtual typedef JView = JDNode;
	
	public static final DNode[] emptyArray = new DNode[0];
	
	@nodeimpl
	public static abstract class DNodeImpl extends NodeImpl {		
		@virtual typedef ImplOf  = DNode;

		private static final int MASK_ACC_DEFAULT   = 0;
		private static final int MASK_ACC_PUBLIC    = ACC_PUBLIC;
		private static final int MASK_ACC_PRIVATE   = ACC_PRIVATE;
		private static final int MASK_ACC_PROTECTED = ACC_PROTECTED;
		private static final int MASK_ACC_NAMESPACE = ACC_PACKAGE;
		private static final int MASK_ACC_SYNTAX    = ACC_SYNTAX;
		
		     public		int			flags;
		@att public		MetaSet		meta;

//		public @packed:1,flags, 0 boolean is_acc_public;
//		public @packed:1,flags, 1 boolean is_acc_private;
//		public @packed:1,flags, 2 boolean is_acc_protected;
		public @packed:3,flags, 0 int     is_access;

		public @packed:1,flags, 3 boolean is_static;
		public @packed:1,flags, 4 boolean is_final;
		public @packed:1,flags, 5 boolean is_mth_synchronized;	// method
		public @packed:1,flags, 5 boolean is_struct_super;		// struct
		public @packed:1,flags, 6 boolean is_fld_volatile;		// field
		public @packed:1,flags, 6 boolean is_mth_bridge;		// method
		public @packed:1,flags, 7 boolean is_fld_transient;		// field
		public @packed:1,flags, 7 boolean is_mth_varargs;		// method
		public @packed:1,flags, 8 boolean is_mth_native;
		public @packed:1,flags, 9 boolean is_struct_interface;
		public @packed:1,flags,10 boolean is_abstract;
		public @packed:1,flags,11 boolean is_math_strict;		// strict math
		public @packed:1,flags,12 boolean is_synthetic;			// any decl that was generated (not in sources)
		public @packed:1,flags,13 boolean is_struct_annotation;
		public @packed:1,flags,14 boolean is_struct_enum;		// struct
		public @packed:1,flags,14 boolean is_fld_enum;			// field
		
		// Flags temporary used with java flags
		public @packed:1,flags,16 boolean is_forward;			// var/field/method, type is wrapper
		public @packed:1,flags,17 boolean is_virtual;			// var/field, method is 'static virtual', struct is 'view'
		public @packed:1,flags,18 boolean is_type_unerasable;	// typedecl, method/struct as parent of typedef
		
		public final boolean isPublic()				{ return this.is_access == MASK_ACC_PUBLIC; }
		public final boolean isPrivate()			{ return this.is_access == MASK_ACC_PRIVATE; }
		public final boolean isProtected()			{ return this.is_access == MASK_ACC_PROTECTED; }
		public final boolean isPkgPrivate()		{ return this.is_access == MASK_ACC_DEFAULT; }
		public final boolean isStatic()				{ return this.is_static; }
		public final boolean isFinal()				{ return this.is_final; }
		public final boolean isSynchronized()		{ return this.is_mth_synchronized; }
		public final boolean isVolatile()			{ return this.is_fld_volatile; }
		public final boolean isFieldVolatile()		{ return this.is_fld_volatile; }
		public final boolean isMethodBridge()		{ return this.is_mth_bridge; }
		public final boolean isFieldTransient()	{ return this.is_fld_transient; }
		public final boolean isMethodVarargs()		{ return this.is_mth_varargs; }
		public final boolean isStructBcLoaded()	{ return this.is_struct_bytecode; }
		public final boolean isMethodNative()		{ return this.is_mth_native; }
		public final boolean isInterface()			{ return this.is_struct_interface; }
		public final boolean isAbstract()			{ return this.is_abstract; }
		
		public final boolean isStructView()		{ return this.is_virtual; }
		public final boolean isTypeUnerasable()	{ return this.is_type_unerasable; }
		public final boolean isPackage()			{ return this.is_access == MASK_ACC_NAMESPACE; }
		public final boolean isSyntax()				{ return this.is_access == MASK_ACC_SYNTAX; }

		public void setPublic() {
			if (this.is_access != MASK_ACC_PUBLIC) {
				this.is_access = MASK_ACC_PUBLIC;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setPrivate() {
			if (this.is_access != MASK_ACC_PRIVATE) {
				this.is_access = MASK_ACC_PRIVATE;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setProtected() {
			if (this.is_access != MASK_ACC_PROTECTED) {
				this.is_access = MASK_ACC_PROTECTED;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setPkgPrivate() {
			if (this.is_access != MASK_ACC_DEFAULT) {
				this.is_access = MASK_ACC_DEFAULT;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public final void setPackage() {
			if (this.is_access != MASK_ACC_NAMESPACE) {
				this.is_access = MASK_ACC_NAMESPACE;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public final void setSyntax() {
			if (this.is_access != MASK_ACC_SYNTAX) {
				this.is_access = MASK_ACC_SYNTAX;
				this.callbackChildChanged(nodeattr$flags);
			}
		}

		public void setStatic(boolean on) {
			if (this.is_static != on) {
				this.is_static = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFinal(boolean on) {
			if (this.is_final != on) {
				this.is_final = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setSynchronized(boolean on) {
			if (this.is_mth_synchronized != on) {
				this.is_mth_synchronized = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setVolatile(boolean on) {
			if (this.is_fld_volatile != on) {
				this.is_fld_volatile = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFieldVolatile(boolean on) {
			if (this.is_fld_volatile != on) {
				this.is_fld_volatile = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodBridge(boolean on) {
			if (this.is_mth_bridge != on) {
				this.is_mth_bridge = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setFieldTransient(boolean on) {
			if (this.is_fld_transient != on) {
				this.is_fld_transient = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodVarargs(boolean on) {
			if (this.is_mth_varargs != on) {
				this.is_mth_varargs = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setMethodNative(boolean on) {
			if (this.is_mth_native != on) {
				this.is_mth_native = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setInterface(boolean on) {
			if (this.is_struct_interface != on) {
				this.is_struct_interface = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setAbstract(boolean on) {
			if (this.is_abstract != on) {
				this.is_abstract = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}

		public void setStructView() {
			if (!this.is_virtual) {
				this.is_virtual = true;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		public void setTypeUnerasable(boolean on) {
			if (this.is_type_unerasable != on) {
				this.is_type_unerasable = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}

		public final boolean isVirtual() {
			return this.is_virtual;
		}
		public final void setVirtual(boolean on) {
			if (this.is_virtual != on) {
				this.is_virtual = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}

		@getter public final boolean isForward() {
			return this.is_forward;
		}
		@setter public final void setForward(boolean on) {
			if (this.is_forward != on) {
				this.is_forward = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	@nodeview
	public static abstract view DNodeView of DNodeImpl extends NodeView {

		public final DNode getDNode() { return (DNode)getNode(); }
		public Dumper toJavaDecl(Dumper dmp) { return getDNode().toJavaDecl(dmp); }
		
		public int		flags;
		public MetaSet	meta;

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
		public final boolean isMethodNative();
		public final boolean isInterface();
		public final boolean isAbstract();
		
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
		public final void setMethodNative(boolean on);
		public final void setInterface(boolean on);
		public final void setAbstract(boolean on);
		public final void setStructView();
		public final void setTypeUnerasable(boolean on);
		public final boolean isVirtual();
		public final void setVirtual(boolean on);
		public final boolean isForward();
		public final void setForward(boolean on);
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public DNode(NImpl v_impl) { super(v_impl); }

	public ASTNode getDummyNode() {
		return DummyDNode.dummyNode;
	}
	
	public abstract void resolveDecl();
	public abstract Dumper toJavaDecl(Dumper dmp);

	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }
}

@nodeset
public final class DummyDNode extends DNode {
	public static final DummyDNode dummyNode = new DummyDNode();
	@nodeimpl
	public static final class DummyDNodeImpl extends DNodeImpl {
		@virtual typedef ImplOf = DummyDNode;
	}
	@nodeview
	public static final view DummyDNodeView of DummyDNodeImpl extends DNodeView {
	}
	private DummyDNode() { super(new DummyDNodeImpl()); }
}



/**
 * An lvalue dnode (var or field)
 */
@nodeset
public abstract class LvalDNode extends DNode {

	@virtual typedef This  = LvalDNode;
	@virtual typedef NImpl = LvalDNodeImpl;
	@virtual typedef VView = LvalDNodeView;
	@virtual typedef JView = JLvalDNode;

	@nodeimpl
	public static abstract class LvalDNodeImpl extends DNodeImpl {
		@virtual typedef ImplOf = LvalDNode;

		// init wrapper
		@getter public final boolean isInitWrapper() {
			return this.is_init_wrapper;
		}
		@setter public final void setInitWrapper(boolean on) {
			if (this.is_init_wrapper != on) {
				this.is_init_wrapper = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// need a proxy access 
		@getter public final boolean isNeedProxy() {
			return this.is_need_proxy;
		}
		@setter public final void setNeedProxy(boolean on) {
			if (this.is_need_proxy != on) {
				this.is_need_proxy = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	@nodeview
	public static abstract view LvalDNodeView of LvalDNodeImpl extends DNodeView {
		// init wrapper
		public final boolean isInitWrapper();
		public final void setInitWrapper(boolean on);
		// need a proxy access 
		public final boolean isNeedProxy();
		public final void setNeedProxy(boolean on);
	}
	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public LvalDNode(LvalDNodeImpl v_impl) { super(v_impl); }

}


/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
@nodeset
public abstract class ENode extends ASTNode {

	@dflow(out="this:in") private static class DFI {}
	
	private static final ENode dummyNode = new NopExpr();

	@virtual typedef This  = ENode;
	@virtual typedef NImpl = ENodeImpl;
	@virtual typedef VView = ENodeView;
	@virtual typedef JView = JENode;

	@nodeimpl
	public static abstract class ENodeImpl extends NodeImpl {
		@virtual typedef ImplOf = ENode;

		//
		// Expr specific
		//
	
		// use no proxy	
		public final boolean isUseNoProxy() {
			return this.is_expr_use_no_proxy;
		}
		public final void setUseNoProxy(boolean on) {
			if (this.is_expr_use_no_proxy != on) {
				this.is_expr_use_no_proxy = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// use as field (disable setter/getter calls for virtual fields)
		public final boolean isAsField() {
			return this.is_expr_as_field;
		}
		public final void setAsField(boolean on) {
			if (this.is_expr_as_field != on) {
				this.is_expr_as_field = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// expression will generate void value
		public final boolean isGenVoidExpr() {
			return this.is_expr_gen_void;
		}
		public final void setGenVoidExpr(boolean on) {
			if (this.is_expr_gen_void != on) {
				this.is_expr_gen_void = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used bt for()
		public final boolean isForWrapper() {
			return this.is_expr_for_wrapper;
		}
		public final void setForWrapper(boolean on) {
			if (this.is_expr_for_wrapper != on) {
				this.is_expr_for_wrapper = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for primary expressions, i.e. (a+b)
		public final boolean isPrimaryExpr() {
			return this.is_expr_primary;
		}
		public final void setPrimaryExpr(boolean on) {
			if (this.is_expr_primary != on) {
				this.is_expr_primary = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for super-expressions, i.e. (super.foo or super.foo())
		public final boolean isSuperExpr() {
			return this.is_expr_super;
		}
		public final void setSuperExpr(boolean on) {
			if (this.is_expr_super != on) {
				this.is_expr_super = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// used for cast calls (to check for null)
		public final boolean isCastCall() {
			return this.is_expr_cast_call;
		}
		public final void setCastCall(boolean on) {
			if (this.is_expr_cast_call != on) {
				this.is_expr_cast_call = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}

	
		//
		// Statement specific flags
		//
		
		// abrupted
		public final boolean isAbrupted() {
			return this.is_stat_abrupted;
		}
		public final void setAbrupted(boolean on) {
			if (this.is_stat_abrupted != on) {
				this.is_stat_abrupted = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// breaked
		public final boolean isBreaked() {
			return this.is_stat_breaked;
		}
		public final void setBreaked(boolean on) {
			if (this.is_stat_breaked != on) {
				this.is_stat_breaked = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// method-abrupted
		public final boolean isMethodAbrupted() {
			return this.is_stat_method_abrupted;
		}
		public final void setMethodAbrupted(boolean on) {
			if (this.is_stat_method_abrupted != on) {
				this.is_stat_method_abrupted = on;
				if (on) this.is_stat_abrupted = true;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
		// auto-returnable
		public final boolean isAutoReturnable() {
			return this.is_stat_auto_returnable;
		}
		public final void setAutoReturnable(boolean on) {
			if (this.is_stat_auto_returnable != on) {
				this.is_stat_auto_returnable = on;
				this.callbackChildChanged(nodeattr$flags);
			}
		}
	}
	@nodeview
	public static abstract view ENodeView of ENodeImpl extends NodeView {

		public final ENode getENode() { return (ENode)this.getNode(); }
		
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

		public Operator getOp() { return null; }

		public int getPriority() {
			if (isPrimaryExpr())
				return 255;
			Operator op = getOp();
			if (op == null)
				return 255;
			return op.priority;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public static final ENode[] emptyArray = new ENode[0];
	
	public ENode(ENodeImpl impl) { super(impl); }

	public Type[] getAccessTypes() {
		return new Type[]{getType()};
	}

	public ASTNode getDummyNode() {
		return ENode.dummyNode;
	}
	
	public void resolve(Type reqType) {
		throw new CompilerException(this,"Resolve call for e-node "+getClass());
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

}

@nodeset
public final class VarDecl extends ENode implements Named {

	@dflow(out="var") private static class DFI {
	@dflow(in="this:in")	Var		var;
	}

	@virtual typedef This  = VarDecl;
	@virtual typedef NImpl = VarDeclImpl;
	@virtual typedef VView = VVarDecl;
	@virtual typedef JView = JVarDecl;
	@virtual typedef RView = RVarDecl;

	@nodeimpl
	public static final class VarDeclImpl extends ENodeImpl {
		@virtual typedef ImplOf = VarDecl;
		@att public Var var;
	}
	@nodeview
	public static abstract view VarDeclView of VarDeclImpl extends ENodeView {
		public Var		var;
	}
	@nodeview
	public static final view VVarDecl of VarDeclImpl extends VarDeclView {
		public VVarDecl(VarDeclImpl impl) { super(impl); }
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public VarDecl() { super(new VarDeclImpl()); }
	
	public VarDecl(Var var) {
		super(new VarDeclImpl());
		this.var = var;
	}

	public void resolve(Type reqType) {
		var.resolveDecl();
		setResolved(true);
	}

	public NodeName getName() { return var.name; }

	public Dumper toJava(Dumper dmp) {
		var.toJavaDecl(dmp);
		return dmp;
	}
	
}

@nodeset
public final class LocalStructDecl extends ENode implements Named {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = LocalStructDecl;
	@virtual typedef NImpl = LocalStructDeclImpl;
	@virtual typedef VView = VLocalStructDecl;
	@virtual typedef JView = JLocalStructDecl;
	@virtual typedef RView = RLocalStructDecl;

	@nodeimpl
	public static final class LocalStructDeclImpl extends ENodeImpl {
		@virtual typedef ImplOf = LocalStructDecl;
		@att public Struct clazz;
	}
	@nodeview
	public static abstract view LocalStructDeclView of LocalStructDeclImpl extends ENodeView {
		public Struct		clazz;
	}
	@nodeview
	public static final view VLocalStructDecl of LocalStructDeclImpl extends LocalStructDeclView {
		public boolean preResolveIn() {
			if( ctx_method==null || ctx_method.isStatic())
				clazz.setStatic(true);
			clazz.setResolved(true);
			clazz.setLocal(true);
			Kiev.runProcessorsOn(clazz);
			return false;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }

	public LocalStructDecl() { super(new LocalStructDeclImpl()); }
	public LocalStructDecl(Struct clazz) {
		super(new LocalStructDeclImpl());
		this.clazz = clazz;
		clazz.setResolved(true);
	}
	
	public void resolve(Type reqType) {
		clazz.resolveDecl();
		setResolved(true);
	}

	public NodeName getName() { return clazz.name; }

	public Dumper toJava(Dumper dmp) {
		clazz.toJavaDecl(dmp);
		return dmp;
	}
}


@nodeset
public final class NopExpr extends ENode implements NodeData {

	public static final KString ID = KString.from("temp expr");
	public static final AttrSlot tempAttrSlot = new AttrSlot("temp expr",true,false,ENode.class);	

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	@virtual typedef This  = NopExpr;
	@virtual typedef NImpl = NopExprImpl;
	@virtual typedef VView = VNopExpr;
	@virtual typedef RView = RNopExpr;

	@nodeimpl
	public static final class NopExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NopExpr;
		@att public ENode	expr;
	}
	@nodeview
	public static abstract view NopExprView of NopExprImpl extends ENodeView {
		public ENode		expr;
	}
	@nodeview
	public static final view VNopExpr of NopExprImpl extends NopExprView {
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public RView getRView() alias operator(210,fy,$cast) { return (RView)this.$v_impl; }

	public NopExpr() { super(new NopExprImpl()); }
	public NopExpr(ENode expr) {
		this();
		this.pos = expr.pos;
		this.expr = expr;
	}
	public Type getType() {
		return expr.getType();
	}
	public void resolve(Type reqType) {
		expr.resolve(reqType);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
	
	public final KString getNodeDataId() { return ID; }
	public void nodeAttached(NodeImpl node) {}
	public void dataAttached(NodeImpl node) { this.callbackAttached(node, tempAttrSlot); }
	public void nodeDetached(NodeImpl node) {}
	public void dataDetached(NodeImpl node) { this.callbackDetached(); }
	
}

@nodeset
public abstract class TypeDecl extends DNode implements Named {

	@virtual typedef This  = TypeDecl;
	@virtual typedef NImpl = TypeDeclImpl;
	@virtual typedef VView = TypeDeclView;
	@virtual typedef JView = JTypeDecl;

	@nodeimpl
	public static class TypeDeclImpl extends DNodeImpl {		
		@virtual typedef ImplOf = TypeDecl;
		public void callbackSuperTypeChanged(TypeDeclImpl chg) {}
	}
	@nodeview
	public static view TypeDeclView of TypeDeclImpl extends DNodeView {
	}

	public TypeDecl(TypeDeclImpl impl) { super(impl); }

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


@nodeset
public class NameRef extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = NameRef;
	@virtual typedef NImpl = NameRefImpl;
	@virtual typedef VView = NameRefView;
	@virtual typedef JView = JNameRef;

	@nodeimpl
	public static class NameRefImpl extends NodeImpl {
		@virtual typedef ImplOf = NameRef;
		@att public KString name;
	}
	@nodeview
	public static view NameRefView of NameRefImpl extends NodeView {
		public KString name;
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }


	public NameRef() {
		super(new NameRefImpl());
	}

	public NameRef(KString name) {
		super(new NameRefImpl());
		this.name = name;
	}

	public NameRef(int pos, KString name) {
		super(new NameRefImpl());
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
	public CompilerException(JNode from, String msg) {
		super(msg);
		this.from = from.getNode();
	}
	public CompilerException(JNode from, CError err_id, String msg) {
		super(msg);
		this.from = from.getNode();
		this.err_id = err_id;
	}
}

public class ReWalkNodeException extends RuntimeException {
	public static final ReWalkNodeException instance = new ReWalkNodeException();
	private ReWalkNodeException() {}
}
