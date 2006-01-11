package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
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

public abstract class ASTNode implements Constants {

	@virtual typedef NImpl = NodeImpl;
	@virtual typedef VView = NodeView;
	@virtual typedef JView = JNodeView;
	
	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	public static class NodeImpl {
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
		
		public AttrSlot[] values() {
			return this.$view.values();
		}
		public Object getVal(String name) {
			return this.$view.getVal(name);
		}
		public void setVal(String name, Object val) {
			this.$view.setVal(name, val);
		}
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
	
	public NImpl $v_impl;
	public VView getVView() alias operator(210,fy,$cast) { return new VView($v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView($v_impl); }
	
	public forward abstract virtual access:ro VView theView;
	@getter public final VView get$theView() { return getVView(); }
	
	public ASTNode(NImpl v_impl) {
		this.$v_impl = v_impl;
		this.$v_impl._self = this;
	}

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
	public boolean isAccessedFromInner() { return this.getVView().isAccessedFromInner(); }
	public void setAccessedFromInner(boolean on) { this.getVView().setAccessedFromInner(on); }
	// resolved
	public boolean isResolved() { return this.getVView().isResolved(); }
	public void setResolved(boolean on) { this.getVView().setResolved(on); }
	// hidden
	public boolean isHidden() { return this.getVView().isHidden(); }
	public void setHidden(boolean on) { this.getVView().setHidden(on); }
	// bad
	public boolean isBad() { return this.getVView().isBad(); }
	public void setBad(boolean on) { this.getVView().setBad(on); }

}

/**
 * A node that is a declaration: class, formal parameters and vars, methods, fields, etc.
 */
public abstract class DNode extends ASTNode {

	@virtual typedef NImpl = DNodeImpl;
	@virtual typedef VView = DNodeView;
	@virtual typedef JView = JDNodeView;
	
	public static final DNode[] emptyArray = new DNode[0];
	
	@node
	public static class DNodeImpl extends NodeImpl {		
		@virtual typedef ImplOf  = DNode;
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

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public DNode(NImpl v_impl) { super(v_impl); }

	public abstract void resolveDecl();
	public abstract Dumper toJavaDecl(Dumper dmp);

	public int getFlags() { return flags; }
	public short getJavaFlags() { return (short)(flags & JAVA_ACC_MASK); }
}

/**
 * An lvalue dnode (var or field)
 */
public abstract class LvalDNode extends DNode {

	@virtual typedef NImpl = LvalDNodeImpl;
	@virtual typedef VView = LvalDNodeView;
	@virtual typedef JView = JLvalDNodeView;

	@node
	public static class LvalDNodeImpl extends DNodeImpl {
		@virtual typedef ImplOf = LvalDNode;
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
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public LvalDNode(LvalDNodeImpl v_impl) { super(v_impl); }

}


/**
 * A node that may be part of expression: statements, declarations, operators,
 * type reference, and expressions themselves
 */
public /*abstract*/ class ENode extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = ENodeImpl;
	@virtual typedef VView = ENodeView;
	@virtual typedef JView = JENodeView;

	@node
	public static class ENodeImpl extends NodeImpl {
		@virtual typedef ImplOf = ENode;
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

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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

}

public final class VarDecl extends ENode implements Named {

	@dflow(out="var") private static class DFI {
	@dflow(in="this:in")	Var		var;
	}

	@virtual typedef NImpl = VarDeclImpl;
	@virtual typedef VView = VarDeclView;
	@virtual typedef JView = JVarDeclView;

	@node
	public static final class VarDeclImpl extends ENodeImpl {
		@virtual typedef ImplOf = VarDecl;
		public VarDeclImpl() {}

		@att public Var var;
	
	}
	@nodeview
	public static final view VarDeclView of VarDeclImpl extends ENodeView {
		public Var		var;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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

public final class LocalStructDecl extends ENode implements Named {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = LocalStructDeclImpl;
	@virtual typedef VView = LocalStructDeclView;
	@virtual typedef JView = JLocalStructDeclView;

	@node
	public static final class LocalStructDeclImpl extends ENodeImpl {
		@virtual typedef ImplOf = LocalStructDecl;
		public LocalStructDeclImpl() {}

		@att public Struct clazz;
	
	}
	@nodeview
	public static final view LocalStructDeclView of LocalStructDeclImpl extends ENodeView {
		public Struct		clazz;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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


public final class NopExpr extends ENode {

	@dflow(out="expr") private static class DFI {
	@dflow(in="this:in")	ENode	expr;
	}

	@virtual typedef NImpl = NopExprImpl;
	@virtual typedef VView = NopExprView;

	@node
	public static final class NopExprImpl extends ENodeImpl {
		@virtual typedef ImplOf = NopExpr;
		public NopExprImpl() {}
		@att public ENode	expr;
	}
	@nodeview
	public static final view NopExprView of NopExprImpl extends ENodeView {
		public ENode		expr;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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

public abstract class TypeDecl extends DNode implements Named {

	@virtual typedef NImpl = TypeDeclImpl;
	@virtual typedef VView = TypeDeclView;
	@virtual typedef JView = JTypeDeclView;

	@node
	public static abstract class TypeDeclImpl extends DNodeImpl {		
		@virtual typedef ImplOf = TypeDecl;
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


public class TypeRef extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeRefImpl;
	@virtual typedef VView = TypeRefView;
	@virtual typedef JView = JTypeRefView;

	@node
	public static class TypeRefImpl extends ENodeImpl {
		@virtual typedef ImplOf = TypeRef;
		public TypeRefImpl() {}
		public TypeRefImpl(int pos, Type tp) { super(pos); this.lnk = tp; }
		@ref public Type	lnk;
	}
	@nodeview
	public static view TypeRefView of TypeRefImpl extends ENodeView {
		public Type	lnk;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

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
	public Struct getStruct() { if (lnk == null) return null; return lnk.getStruct(); }
	public JType getJType() { return getType().getJType(); }

	public Type getType()
		alias operator(210,fy,$cast)
	{
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
			return;
		}
		throw new CompilerException(this,"Type "+this+" is not a singleton");
	}
	
	public static Enumeration<Type> linked_elements(NArr<TypeRef> arr) {
		Vector<Type> tmp = new Vector<Type>();
		foreach (TypeRef tr; arr) { if (tr.lnk != null) tmp.append(tr.lnk); }
		return tmp.elements();
	}
}

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

