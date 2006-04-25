package kiev.vlang;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.be.java15.JType;
import kiev.ir.java15.RNode;
import kiev.be.java15.JNode;
import kiev.be.java15.JSymbolRef;
import kiev.ir.java15.RSymbolRef;

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
	passPreResolve			   ,	// pre-resolve nodes
	passMainResolve			   ,	// main resolve for vlang
	passVerify				   ,	// verify the tree before generation
	passPreGenerate			   ,	// prepare tree for generation phase
	passGenerate			   		// resolve, generate and so on - each file separatly
};

public interface NodeData {
	public AttrSlot	getNodeDataId();
	public NodeData nodeCopiedTo(ASTNode node);
	public void callbackAttached(ASTNode parent, AttrSlot pslot);
	public void callbackDetached();
	public void callbackRootChanged();
	public void walkTree(TreeWalker walker);
};

public class TreeWalker {
	public boolean pre_exec(NodeData n) { return true; }
	public void post_exec(NodeData n) {}
}

public interface SetBody {
	public boolean setBody(ENode body);
}

@node
public abstract class ASTNode implements NodeData, Constants, Cloneable {

	@virtual typedef This  = ASTNode;
	@virtual typedef VView = NodeView;
	@virtual typedef JView = JNode;
	@virtual typedef RView = RNode;
	
	public static ASTNode[] emptyArray = new ASTNode[0];
    public static final AttrSlot nodeattr$flags = new AttrSlot("flags", false, false, Integer.TYPE);

	public					int				pos;
	public					int				compileflags;
	public:ro,ro,ro,rw		ASTNode			parent;
	public:ro,ro,ro,rw		AttrSlot		pslot;
	public:ro,ro,rw,rw		ASTNode			pprev;
	public:ro,ro,rw,rw		ASTNode			pnext;
	public:ro,ro,ro,rw		ASTNode			ctx_root;
	private:no,no,no,rw		NodeData[]		ndata;
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
		ASTNode node = (ASTNode)to$node;
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
				nd.callbackAttached(node, this.ndata[i].getNodeDataId());
			}
		}
		return node;
	}

	public final int getPosLine() { return pos >>> 11; }
	
	// the node is attached
	public final boolean isAttached()  {
		return parent != null;
	}

	public AttrSlot getNodeDataId() {
		return pslot;
	}

	public NodeData nodeCopiedTo(ASTNode node) {
		return ncopy();
	}

	public void callbackDetached() {
		assert(isAttached());
		// do detcah
		ASTNode parent = this.parent;
		AttrSlot pslot = this.pslot;
		this.parent = null;
		this.pslot = null;
		this.ctx_root = this;
		this.pprev = null;
		this.pnext = null;
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(NodeData n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		parent.callbackChildChanged(pslot);
	}
	
	public void callbackAttached(ASTNode parent, AttrSlot pslot) {
		if (pslot.is_attr) {
			assert(!isAttached());
			assert(parent != null && parent != this);
			// do attach
			this.parent = parent;
			this.pslot = pslot;
			this.ctx_root = parent.ctx_root;
			// notify nodes about new root
			this.walkTree(new TreeWalker() {
				public boolean pre_exec(NodeData n) { n.callbackRootChanged(); return true; }
			});
			// notify parent about the changed slot
			parent.callbackChildChanged(pslot);
		}
	}
	public void callbackChildChanged(AttrSlot attr) {
		// do nothing
	}
	public void callbackRootChanged() {
		// do nothing
	}	

	public final NodeData getNodeData(AttrSlot attr) {
		assert (attr.isData());
		if (ndata != null) {
			foreach (NodeData nd; ndata) {
				if (nd.getNodeDataId().name == attr.name)
					return nd;
			}
		}
		return null;
	}
	
	public final void addNodeData(NodeData d, AttrSlot attr) {
		if (ndata != null) {
			assert (attr.isData());
			NodeData[] ndata = this.ndata;
			int sz = ndata.length;
			for (int i=0; i < sz; i++) {
				NodeData nd = ndata[i];
				if (nd.getNodeDataId().name == attr.name) {
					if (nd == d)
						return;
					nd.callbackDetached();
					d.callbackAttached(this, attr);
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
		d.callbackAttached(this, attr);
	}
	
	public final void delNodeData(AttrSlot attr) {
		NodeData[] ndata = this.ndata;
		assert (attr.isData());
		if (ndata != null) {
			int sz = ndata.length-1;
			for (int idx=0; idx <= sz; idx++) {
				NodeData nd = ndata[idx];
				if (nd.getNodeDataId().name == attr.name) {
					NodeData[] tmp   = new NodeData[sz];
					nd.callbackDetached();
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
		if (walker.pre_exec(this)) {
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
		walker.post_exec(this);
	}

	// build data flow for this node
	public final DataFlowInfo getDFlow() {
		DataFlowInfo df = (DataFlowInfo)getNodeData(DataFlowInfo.ATTR);
		if (df == null) {
			df = DataFlowInfo.newDataFlowInfo(this);
			this.addNodeData(df, DataFlowInfo.ATTR);
		}
		return df;
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
		}
		else if (pslot.isData()) {
			assert(parent.getNodeData(pslot) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			parent.addNodeData(node, pslot);
		}
		else {
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
			space[idx] = this.getDummyNode();
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			space[idx] = n;
			assert(n.isAttached());
			return n;
		}
		else if (pslot.isData()) {
			assert(parent.getNodeData(pslot) == this);
			parent.delNodeData(pslot);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			parent.addNodeData(n, pslot);
			assert(n.isAttached());
			return n;
		}
		else {
			assert(parent.getVal(pslot.name) == this);
			parent.setVal(pslot.name, this.getDummyNode());
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

	public final void cleanDFlow() {
		walkTree(new TreeWalker() {
			public boolean pre_exec(NodeData n) { if (n instanceof ASTNode) n.delNodeData(DataFlowInfo.ATTR); return true; }
		});
	}
	
	public Type getType() { return Type.tpVoid; }

		
	@getter public final ASTNode get$ctx_root() {
		if (this.ctx_root != null)
			return this.ctx_root;
		ASTNode root;
		ASTNode parent = this.parent;
		if (parent == null)
			root = this;
		else
			root = parent.get$ctx_root();
		this.ctx_root = root;
		return root;
	}
	@getter public FileUnit get$ctx_file_unit() { return this.parent.get$ctx_file_unit(); }
	@getter public Struct get$ctx_clazz() { return this.parent.child_ctx_clazz; }
	@getter public Struct get$child_ctx_clazz() { return this.parent.get$child_ctx_clazz(); }
	@getter public Method get$ctx_method() { return this.parent.child_ctx_method; }
	@getter public Method get$child_ctx_method() { return this.parent.get$child_ctx_method(); }

	@nodeview
	public static abstract view NodeView of ASTNode implements Constants {
		public String toString();
		public Dumper toJava(Dumper dmp);
		
		public int			pos;
		public int			compileflags;
		public:ro ASTNode	parent;
		public:ro AttrSlot	pslot;
		public:ro ASTNode	pprev;
		public:ro ASTNode	pnext;
		
		@getter public final ASTNode get$ctx_root();
		@getter public final FileUnit get$ctx_file_unit();
		@getter public final Struct get$ctx_clazz();
		@getter public final Struct get$child_ctx_clazz();
		@getter public final Method get$ctx_method();
		@getter public final Method get$child_ctx_method();

		public AttrSlot[] values();
		public Object getVal(String name);
		public void setVal(String name, Object val);
		public final void callbackDetached();
		public final void callbackAttached(ASTNode parent, AttrSlot pslot);
		public final void callbackChildChanged(AttrSlot attr);
		public final void callbackRootChanged();
		public final NodeData getNodeData(AttrSlot attr);
		public final void addNodeData(NodeData d, AttrSlot attr);
		public final void delNodeData(AttrSlot attr);
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

		public final Type getType();

		public boolean preResolveIn() { return true; }
		public void preResolveOut() {}
		public boolean mainResolveIn() { return true; }
		public void mainResolveOut() {}
		public boolean preVerify() { return true; }
		public void postVerify() {}
	}
	
	public ASTNode() {}

	public final This detach()
		alias operator (210,fy,~)
	{
		if (!isAttached())
			return this;
		if (pslot.is_space) {
			((NArr<ASTNode>)parent.getVal(pslot.name)).detach(this);
		}
		else if (pslot.isData()) {
			parent.delNodeData(pslot);
		}
		else {
			parent.setVal(pslot.name,null);
		}
		assert(!isAttached());
		return this;
	}
	
	public abstract ASTNode getDummyNode();
	
	public final This ncopy() {
		return (This)this.copy();
	}
	public abstract Object copy();

    public Dumper toJava(Dumper dmp) {
    	dmp.append("/* INTERNAL ERROR - ").append(this.getClass().toString()).append(" */");
    	return dmp;
    }
	
	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public final boolean preResolveIn() { return ((VView)this).preResolveIn(); }
	public final void preResolveOut() { ((VView)this).preResolveOut(); }
	public final boolean mainResolveIn() { return ((VView)this).mainResolveIn(); }
	public final void mainResolveOut() { ((VView)this).mainResolveOut(); }
	public final boolean preVerify() { return ((VView)this).preVerify(); }
	public final void postVerify() { ((VView)this).postVerify(); }

	public final boolean preGenerate() { return ((RView)this).preGenerate(); }
	
}


@node
public class SymbolRef extends ASTNode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = SymbolRef;
	@virtual typedef VView = VSymbolRef;
	@virtual typedef JView = JSymbolRef;
	@virtual typedef RView = RSymbolRef;

	@att public KString		name; // unresolved name
	@ref public Symbol		symbol; // resolved symbol

	@nodeview
	public static view VSymbolRef of SymbolRef extends NodeView {
		public KString	name;
		public Symbol	symbol;
	}

	public SymbolRef() {}

	public SymbolRef(KString name) {
		this.name = name;
	}

	public SymbolRef(int pos, KString name) {
		this.pos = pos;
		this.name = name;
	}

	public boolean equals(Object nm) {
		if (nm instanceof Symbol) return nm == this.name;
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof KString) return nm == this.name;
		return false;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = KString.from(t.image);
	}
	
	public KString toKString() alias operator(210,fy,$cast) { return name; }
    
	public String toString() { return name.toString(); }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
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
		this.from = (ASTNode)from;
	}
	public CompilerException(ASTNode.NodeView from, CError err_id, String msg) {
		super(msg);
		this.from = (ASTNode)from;
		this.err_id = err_id;
	}
	public CompilerException(JNode from, String msg) {
		super(msg);
		this.from = (ASTNode)from;
	}
	public CompilerException(JNode from, CError err_id, String msg) {
		super(msg);
		this.from = (ASTNode)from;
		this.err_id = err_id;
	}
}

public class ReWalkNodeException extends RuntimeException {
	public static final ReWalkNodeException instance = new ReWalkNodeException();
	private ReWalkNodeException() {}
}
