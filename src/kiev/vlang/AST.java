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

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public abstract class ANode {

	@virtual typedef This  = ANode;

	private AttachInfo		p_info;
	private AttachInfo[]	ndata;
	public int				version;
	public boolean			locked;
	private This			prev_version_node;
	private This			next_version_node;

	public abstract ANode nodeCopiedTo(ANode node);

	public final boolean    isAttached()    { return p_info != null; }
	public final AttachInfo getAttachInfo() { return p_info; }

	public final void callbackAttached(ANode parent, AttrSlot pslot) {
		this.callbackAttached(new AttachInfo(this, parent, pslot));
	}
	public final void callbackAttached(AttachInfo pinfo) {
		assert (pinfo.p_slot.is_attr);
		assert(!isAttached());
		assert(pinfo.p_parent != null && pinfo.p_parent != this);
		assert(pinfo.p_data == this);
		this.p_info = pinfo;
		this.callbackAttached();
	}
	public void callbackAttached() {
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		parent().callbackChildChanged(p_info.p_slot);
	}
	public void callbackDetached() {
		assert(isAttached());
		// do detcah
		AttachInfo pinfo = this.p_info;
		this.p_info = null;
		pinfo.detach();
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.callbackRootChanged(); return true; }
		});
		// notify parent about the changed slot
		pinfo.p_parent.callbackChildChanged(pinfo.p_slot);
	}


	@getter public final ANode get$ctx_root() {
		if (!isAttached())
			return this;
		return this.getAttachInfo().get_ctx_root();
	}
	public void callbackChildChanged(AttrSlot attr) { /* do nothing */ }
	public void callbackRootChanged() { /* do nothing */ }

	public final ANode parent() { return this.p_info == null ? null : this.p_info.p_parent; }
	public final AttrSlot pslot() { return this.p_info == null ? null : this.p_info.p_slot; }
	public final ANode pprev() { return this.p_info == null ? null : this.p_info.prev(); }
	public final ANode pnext() { return this.p_info == null ? null : this.p_info.next(); }
	
	@getter public FileUnit get$ctx_file_unit() { return this.parent().get$ctx_file_unit(); }
	@getter public TypeDecl get$ctx_tdecl() { return this.parent().child_ctx_tdecl; }
	@getter public TypeDecl get$child_ctx_tdecl() { return this.parent().get$child_ctx_tdecl(); }
	@getter public Method get$ctx_method() { return this.parent().child_ctx_method; }
	@getter public Method get$child_ctx_method() { return this.parent().get$child_ctx_method(); }

	public AttrSlot[] values() {
		return AttrSlot.emptyArray;
	}
	public Object getVal(String name) {
		if (ndata != null) {
			foreach (AttachInfo ai; ndata; ai.p_slot.name == name)
				return ai.p_data;
		}
		throw new RuntimeException("No @att value \"" + name + "\" in ANode");
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @att value \"" + name + "\" in ANode");
	}

	public final Object getNodeData(AttrSlot attr) {
		assert (attr.isData());
		if (ndata != null) {
			foreach (AttachInfo ai; ndata) {
				if (ai.p_slot.name == attr.name)
					return ai.p_data;
			}
		}
		return null;
	}
	
	public final void addNodeData(Object d, AttrSlot attr) {
		assert (attr.isData());
		if (ndata != null) {
			AttachInfo[] ndata = this.ndata;
			int sz = ndata.length;
			for (int i=0; i < sz; i++) {
				AttachInfo ai = ndata[i];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (ai.p_data == d)
						return;
					if (attr.is_attr) {
						if (ai.p_data instanceof ANode)
							((ANode)ai.p_data).callbackDetached();
						if (d instanceof ANode)
							d.callbackAttached(this, attr);
					} else {
						ndata[i] = new AttachInfo(d,this,attr);
					}
					return;
				}
			}
			AttachInfo[] tmp = new AttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = ndata[i];
			tmp[sz] = new AttachInfo(d,this,attr);
			this.ndata = tmp;
		} else {
			this.ndata = new AttachInfo[]{new AttachInfo(d,this,attr)};
		}
		if (attr.is_attr && d instanceof ANode)
			d.callbackAttached(this, attr);
	}
	
	public final void delNodeData(AttrSlot attr) {
		AttachInfo[] ndata = this.ndata;
		assert (attr.isData());
		if (ndata != null) {
			int sz = ndata.length-1;
			for (int idx=0; idx <= sz; idx++) {
				AttachInfo ai = ndata[idx];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					AttachInfo[] tmp = new AttachInfo[sz];
					int i;
					for (i=0; i < idx; i++) tmp[i] = ndata[i];
					for (   ; i <  sz; i++) tmp[i] = ndata[i+1];
					this.ndata = tmp;
					if (attr.is_attr && ai.p_data instanceof ANode)
						((ANode)ai.p_data).callbackDetached();
					return;
				}
			}
		}
	}

	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.is_attr) {
				Object val = attr.get(this);
				if (val == null)
					continue;
				if (attr.is_space) {
					ASTNode[] vals = (ASTNode[])val;
					for (int i=0; i < vals.length; i++) {
						try {
							vals[i].walkTree(walker);
						} catch (ReWalkNodeException e) {
							i--;
							val = attr.get(this);
							vals = (ASTNode[])val;
						}
					}
				}
				else if (val instanceof ASTNode) {
				re_walk_node:;
					try {
						val.walkTree(walker);
					} catch (ReWalkNodeException e) {
						val = attr.get(this);
						if (val != null)
							goto re_walk_node;
					}
				}
			}
			if (ndata != null) {
				foreach (AttachInfo ai; this.ndata; ai.p_slot.is_attr && ai.p_data instanceof ANode)
					((ANode)ai.p_data).walkTree(walker);
			}
		}
		walker.post_exec(this);
	}

	public Object copyTo(Object to$node) {
		ANode node = (ANode)to$node;
		if (this.ndata != null) {
			for (int i=0; i < this.ndata.length; i++) {
				AttachInfo ai = this.ndata[i];
				if (!ai.p_slot.is_attr)
					continue;
				if !(ai.p_data instanceof ANode)
					continue;
				ANode nd = ((ANode)ai.p_data).nodeCopiedTo(node);
				if (nd == null)
					continue;
				if (node.ndata == null) {
					node.ndata = new AttachInfo[]{new AttachInfo(nd,node,ai.p_slot)};
				} else {
					int sz = node.ndata.length;
					AttachInfo[] tmp = new AttachInfo[sz+1];
					for (int j=0; j < sz; j++)
						tmp[j] = node.ndata[j];
					tmp[sz] = new AttachInfo(nd,node,ai.p_slot);
					node.ndata = tmp;
				}
				nd.callbackAttached(node, ai.p_slot);
			}
		}
		return node;
	}
	
	public final AttrPtr getAttrPtr(String name) {
		foreach (AttrSlot attr; this.values(); attr.name == name)
			return new AttrPtr(this, attr);
		throw new RuntimeException("No @att/@ref attribute '"+name+"' in "+getClass());
	}
	
	public final SpacePtr getSpacePtr(String name) {
		foreach (AttrSlot attr; this.values(); attr.name == name && attr.is_space)
			return new SpacePtr(this, (SpaceAttrSlot<ASTNode>)attr);
		throw new RuntimeException("No @att/@ref space '"+name+"' in "+getClass());
	}

	public final ANode open() {
		if (!locked)
			return this;
		This node = (This)this.clone();
		Transaction tr = Transaction.current();
		node.version = tr.version;
		node.prev_version_node = this.prev_version_node;
		node.next_version_node = this;
		this.prev_version_node = node;
		this.locked = false;
		tr.add(this);
		return this;
	}
}

public class TreeWalker {
	public boolean pre_exec(ANode n) { return true; }
	public void post_exec(ANode n) {}
}

public interface PreScanneable {
	public boolean setBody(ENode body);
}

class AttachInfo {
	final   Object		p_data;
	final   ANode		p_parent;
	final   AttrSlot	p_slot;
	private ANode		p_ctx_root;
	AttachInfo(Object data, ANode parent, AttrSlot slot) {
		this.p_data = data;
		this.p_parent = parent;
		this.p_slot = slot;
	}
	ANode get_ctx_root() {
		if (this.p_ctx_root != null)
			return this.p_ctx_root;
		ANode root = p_parent.get$ctx_root();
		this.p_ctx_root = root;
		return root;
	}
	ANode next() { return null; }
	ANode prev() { return null; }
	void detach() {
		this.p_ctx_root = null;
	}
};

class ListAttachInfo extends AttachInfo {
	private ListAttachInfo	p_prev;
	private ListAttachInfo	p_next;
	ListAttachInfo(ANode self, ANode parent, AttrSlot slot, ListAttachInfo prev, ListAttachInfo next) {
		super(self,parent,slot);
		if (prev != null) {
			this.p_prev = prev;
			prev.p_next = this;
		}
		if (next != null) {
			next.p_prev = this;
			this.p_next = next;
		}
	}
	ANode next() { return p_next == null ? null : (ANode)p_next.p_data; }
	ANode prev() { return p_prev == null ? null : (ANode)p_prev.p_data; }
	void detach() {
		if (p_prev != null)
			p_prev.p_next = p_next;
		if (p_next != null)
			p_next.p_prev = p_prev;
		super.detach();
	}
};
	

@node
public abstract class ASTNode extends ANode implements Constants, Cloneable {

	@virtual typedef This  = ASTNode;
	@virtual typedef VView = NodeView;
	@virtual typedef JView = JNode;
	@virtual typedef RView = RNode;
	
	public static ASTNode[] emptyArray = new ASTNode[0];
	private static final class RefAttrSlot_flags extends RefAttrSlot {
		RefAttrSlot_flags(String name, Class clazz) { super(name, clazz); }
		public final void set(ANode parent, Object value) { throw new RuntimeException("@ref flags is not writeable"); }
		public final Object get(ANode parent) { return new Integer(((ASTNode)parent).compileflags); }
	}
	public static final RefAttrSlot_flags nodeattr$flags = new RefAttrSlot_flags("flags", Integer.TYPE);

	private static final class RefAttrSlot_parent extends RefAttrSlot {
		RefAttrSlot_parent(String name, Class clazz) { super(name, clazz); }
		public final void set(ANode parent, Object value) { throw new RuntimeException("@ref parent is not writeable"); }
		public final Object get(ANode parent) { return parent.parent(); }
	}
	public static final RefAttrSlot_parent nodeattr$parent = new RefAttrSlot_parent("parent", ANode.class);

	private static final AttrSlot[] $values = {nodeattr$parent, nodeattr$flags};

	public int				pos;
	public int				compileflags;
	@ref @abstract
	public:ro ANode			parent;
	
	@getter public final ANode get$parent() { return parent(); }

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
	public @packed:1,compileflags,22 boolean is_mth_inlined_by_dispatcher;
	public @packed:1,compileflags,23 boolean is_mth_invariant;
	
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
		return ASTNode.$values;
	}
	public Object getVal(String name) {
		if (name == "parent")
			return parent();
		return super.getVal(name);
	}
	public void setVal(String name, Object val) { super.setVal(name,val); }
		
	public Object copyTo(Object to$node) {
		ASTNode node = (ASTNode)super.copyTo(to$node);
		node.pos			= this.pos;
		node.compileflags	= this.compileflags;
		return node;
	}

	public final int getPosLine() { return pos >>> 11; }
	
	public ANode nodeCopiedTo(ANode node) {
		return ncopy();
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

	public final void replaceWithNodeReWalk(ASTNode node) {
		node = replaceWithNode(node);
		Kiev.runProcessorsOn(node);
		throw new ReWalkNodeException(node);
	}
	public final ASTNode replaceWithNode(ASTNode node) {
		assert(isAttached());
		ANode parent = parent().open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			assert(node != null);
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			if (node.pos == 0) node.pos = this.pos;
			pslot.set(parent, idx, node);
		}
		else if (pslot.isData()) {
			assert(parent.getNodeData(pslot) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			pslot.set(parent, node);
		}
		else {
			assert(pslot.get(parent) == this);
			if (node != null && node.pos == 0) node.pos = this.pos;
			pslot.set(parent, node);
		}
		assert(node == null || node.isAttached());
		return node;
	}
	public final void replaceWithReWalk(()->ASTNode fnode) {
		ASTNode node = replaceWith(fnode);
		Kiev.runProcessorsOn(node);
		throw new ReWalkNodeException(node);
	}
	public final ASTNode replaceWith(()->ASTNode fnode) {
		assert(isAttached());
		ASTNode parent = parent().open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			pslot.set(parent, idx, this.getDummyNode());
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			pslot.set(parent, idx, n);
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
			assert(pslot.get(parent) == this);
			pslot.set(parent, this.getDummyNode());
			ASTNode n = fnode();
			if (n != null && n.pos == 0) n.pos = this.pos;
			pslot.set(parent, n);
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
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) n.delNodeData(DataFlowInfo.ATTR); return true; }
		});
	}
	
	public Type getType() { return Type.tpVoid; }

	@nodeview
	public static abstract view NodeView of ASTNode implements Constants {
		public String toString();
		public Dumper toJava(Dumper dmp);
		
		public int			pos;
		public int			compileflags;
		
		@getter public final ANode get$ctx_root();
		@getter public final FileUnit get$ctx_file_unit();
		@getter public final TypeDecl get$ctx_tdecl();
		@getter public final TypeDecl get$child_ctx_tdecl();
		@getter public final Method get$ctx_method();
		@getter public final Method get$child_ctx_method();

		public final ANode parent();
		public final AttrSlot pslot();
		public AttrSlot[] values();
		public final void callbackChildChanged(AttrSlot attr);
		public final void callbackRootChanged();
		public final Object getNodeData(AttrSlot attr);
		public final void addNodeData(ANode d, AttrSlot attr);
		public final void delNodeData(AttrSlot attr);
		public DataFlowInfo getDFlow();
		public final void    replaceWithNodeReWalk(ASTNode node);
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
	
	public ASTNode() {
		Transaction tr = Transaction.get();
		if (tr != null) {
			this.version = tr.version;
			tr.add(this);
		}
	}

	public final This detach()
		alias operator (210,fy,~)
	{
		if (!isAttached())
			return this;
		ANode parent = parent().open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot)
			pslot.detach(parent, this);
		else if (pslot.isData())
			parent.delNodeData(pslot);
		else
			pslot.set(parent,null);
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
	
	public boolean hasName(String name) {
		return false;
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

	public Object doRewrite(RewriteContext ctx) {
		throw new CompilerException(this, "Node "+this.getClass().getName()+" is not a rewriter");
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
	public final ASTNode replacer;
	public ReWalkNodeException(ASTNode replacer) {
		this.replacer = replacer;
	}
}
