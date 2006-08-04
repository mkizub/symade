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

	@virtual typedef This  ≤ ANode;

	public static ANode[] emptyArray = new ANode[0];

	private AttachInfo		p_info;
	private AttachInfo[]	ext_data;
	private AttachInfo[]	tmp_data;

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
		this.open();
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
		this.open();
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
	public void callbackRootChanged() { if (this.p_info != null) this.p_info.p_ctx_root = null; }

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
		if (ext_data != null) {
			foreach (AttachInfo ai; ext_data; ai.p_slot.name == name)
				return ai.p_data;
		}
		if (tmp_data != null) {
			foreach (AttachInfo ai; tmp_data; ai.p_slot.name == name)
				return ai.p_data;
		}
		throw new RuntimeException("No @att value \"" + name + "\" in "+getClass().getName());
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @att value \"" + name + "\" in "+getClass().getName());
	}

	public final Object getExtData(AttrSlot attr) {
		assert (attr.isExtData());
		if (ext_data != null) {
			foreach (AttachInfo ai; ext_data) {
				if (ai.p_slot.name == attr.name)
					return ai.p_data;
			}
		}
		return null;
	}
	
	public final Object getTmpData(AttrSlot attr) {
		assert (attr.isTmpData());
		if (tmp_data != null) {
			foreach (AttachInfo ai; tmp_data) {
				if (ai.p_slot.name == attr.name)
					return ai.p_data;
			}
		}
		return null;
	}
	
	public final void addExtData(Object d, AttrSlot attr) {
		assert (attr.isExtData());
		if (ext_data != null) {
			AttachInfo[] data = this.ext_data;
			int sz = data.length;
			for (int i=0; i < sz; i++) {
				AttachInfo ai = data[i];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (ai.p_data == d)
						return;
					this.open();
					ext_data = (AttachInfo[])data.clone();
					ext_data[i] = new AttachInfo(d,this,attr);
					if (attr.is_attr) {
						if (ai.p_data instanceof ANode)
							((ANode)ai.p_data).callbackDetached();
						if (d instanceof ANode)
							d.callbackAttached(this, attr);
					}
					return;
				}
			}
			this.open();
			AttachInfo[] tmp = new AttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = data[i];
			tmp[sz] = new AttachInfo(d,this,attr);
			ext_data = tmp;
		} else {
			this.open();
			ext_data = new AttachInfo[]{new AttachInfo(d,this,attr)};
		}
		if (attr.is_attr && d instanceof ANode)
			d.callbackAttached(this, attr);
	}
	
	public final void addTmpData(Object d, AttrSlot attr) {
		assert (attr.isTmpData());
		if (tmp_data != null) {
			AttachInfo[] data = this.tmp_data;
			int sz = data.length;
			for (int i=0; i < sz; i++) {
				AttachInfo ai = data[i];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (ai.p_data == d)
						return;
					tmp_data[i] = new AttachInfo(d,this,attr);
					if (attr.is_attr) {
						if (ai.p_data instanceof ANode)
							((ANode)ai.p_data).callbackDetached();
						if (d instanceof ANode)
							d.callbackAttached(this, attr);
					}
					return;
				}
			}
			AttachInfo[] tmp = new AttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = data[i];
			tmp[sz] = new AttachInfo(d,this,attr);
			tmp_data = tmp;
		} else {
			tmp_data = new AttachInfo[]{new AttachInfo(d,this,attr)};
		}
		if (attr.is_attr && d instanceof ANode)
			d.callbackAttached(this, attr);
	}
	
	public final void delExtData(AttrSlot attr) {
		AttachInfo[] data = this.ext_data;
		assert (attr.isExtData());
		if (data != null) {
			int sz = data.length-1;
			for (int idx=0; idx <= sz; idx++) {
				AttachInfo ai = data[idx];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					this.open();
					if (sz == 0) {
						this.ext_data = null;
					} else {
						AttachInfo[] tmp = new AttachInfo[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = data[i];
						for (   ; i <  sz; i++) tmp[i] = data[i+1];
						this.ext_data = tmp;
					}
					if (attr.is_attr && ai.p_data instanceof ANode)
						((ANode)ai.p_data).callbackDetached();
					return;
				}
			}
		}
	}

	public final void delTmpData(AttrSlot attr) {
		AttachInfo[] data = this.tmp_data;
		assert (attr.isTmpData());
		if (data != null) {
			int sz = data.length-1;
			for (int idx=0; idx <= sz; idx++) {
				AttachInfo ai = data[idx];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (sz == 0) {
						this.tmp_data = null;
					} else {
						AttachInfo[] tmp = new AttachInfo[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = data[i];
						for (   ; i <  sz; i++) tmp[i] = data[i+1];
						this.tmp_data = tmp;
					}
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
					ANode[] vals = (ANode[])val;
					for (int i=0; i < vals.length; i++) {
						try {
							vals[i].walkTree(walker);
						} catch (ReWalkNodeException e) {
							i--;
							val = attr.get(this);
							vals = (ANode[])val;
						}
					}
				}
				else if (val instanceof ANode) {
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
			if (ext_data != null) {
				foreach (AttachInfo ai; ext_data; ai.p_slot.is_attr && ai.p_data instanceof ANode)
					((ANode)ai.p_data).walkTree(walker);
			}
			if (tmp_data != null) {
				foreach (AttachInfo ai; tmp_data; ai.p_slot.is_attr && ai.p_data instanceof ANode)
					((ANode)ai.p_data).walkTree(walker);
			}
		}
		walker.post_exec(this);
	}

	public final This ncopy() {
		return (This)this.copy();
	}
	public abstract Object copy();

	public Object copyTo(Object to$node) {
		ANode node = (ANode)to$node;
		if (this.ext_data != null) {
			int N = this.ext_data.length;
			node.ext_data = new AttachInfo[N];
			for (int i=0; i < N; i++) {
				AttachInfo ai = this.ext_data[i];
				if (ai.p_slot.is_attr && ai.p_data instanceof ASTNode) {
					ASTNode nd = ((ASTNode)ai.p_data).ncopy();
					node.ext_data[i] = new AttachInfo(nd,node,ai.p_slot);
					nd.callbackAttached(node, ai.p_slot);
				} else {
					node.ext_data[i] = ai;
				}
			}
		}
		if (this.tmp_data != null) {
			foreach (AttachInfo ai; this.tmp_data)
				assert (!ai.p_slot.is_attr);
		}
		return node;
	}
	
	public final This detach()
		alias fy operator ~
	{
		if (!isAttached())
			return this;
		ANode parent = parent();
		parent.open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot)
			pslot.detach(parent, this);
		else if (pslot.isExtData())
			parent.delExtData(pslot);
		else if (pslot.isTmpData())
			parent.delTmpData(pslot);
		else
			pslot.set(parent,null);
		assert(!isAttached());
		return this;
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

	public void setFrom(Object from$node) {
		ANode node = (ANode)from$node;
		this.p_info = node.p_info;
		this.ext_data = node.ext_data;
		if (this.tmp_data != null) {
			foreach (AttachInfo ai; this.tmp_data)
				assert (!ai.p_slot.is_attr);
		}
	}
	
	public void open() {}

	public final <N extends ANode> N replaceWithNode(N node) {
		assert(isAttached());
		ANode parent = parent();
		parent.open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			assert(node != null);
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, idx, node);
		}
		else if (pslot.isExtData()) {
			assert(parent.getExtData(pslot) == this);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, node);
		}
		else if (pslot.isTmpData()) {
			assert(parent.getTmpData(pslot) == this);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, node);
		}
		else {
			assert(pslot.get(parent) == this);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, node);
		}
		assert(node == null || node.isAttached());
		return node;
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
	        ANode		p_ctx_root;
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

	@virtual typedef This  ≤ ASTNode;
	@virtual typedef JView ≤ JNode;
	@virtual typedef RView ≤ RNode;
	
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
	public int				version;
	public boolean			locked;
	private This			prev_version_node;
	private This			next_version_node;
	@ref @abstract
	public:ro ANode			parent;
	
	@getter public final ANode get$parent() { return parent(); }

	// Structures	
	public @packed:1,compileflags,8  boolean is_struct_type_resolved; // KievFE_Pass2
	public @packed:1,compileflags,9  boolean is_struct_args_resolved; // KievFE_Pass2
	public @packed:1,compileflags,10 boolean is_struct_members_generated; // KievFE_Pass2
	public @packed:1,compileflags,11 boolean is_struct_pre_generated; // KievME_PreGenartion

	public @packed:1,compileflags,17 boolean is_struct_fe_passed;
	public @packed:1,compileflags,18 boolean is_struct_local;
	public @packed:1,compileflags,19 boolean is_struct_anomymouse;
	public @packed:1,compileflags,20 boolean is_struct_has_pizza_cases;
	public @packed:1,compileflags,21 boolean is_struct_bytecode;	// struct was loaded from bytecode
	public @packed:1,compileflags,22 boolean is_struct_pizza_case;
	public @packed:1,compileflags,23 boolean is_struct_compiler_node;
	
	// Expression/statement flags
	public @packed:1,compileflags,8  boolean is_expr_gen_void;
	public @packed:1,compileflags,9  boolean is_expr_for_wrapper;
	public @packed:1,compileflags,10 boolean is_expr_cast_call;

	public @packed:1,compileflags,17 boolean is_expr_as_field;
	public @packed:1,compileflags,18 boolean is_expr_primary;
	public @packed:1,compileflags,19 boolean is_expr_super;

	// Statement flags
	public @packed:1,compileflags,11 boolean is_stat_abrupted;
	public @packed:1,compileflags,12 boolean is_stat_breaked;
	public @packed:1,compileflags,13 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public @packed:1,compileflags,14 boolean is_stat_auto_returnable;

	public @packed:1,compileflags,20 boolean is_stat_break_target;
	
	// Method flags
	public @packed:1,compileflags,8  boolean is_mth_need_fields_init;
	public @packed:1,compileflags,9  boolean is_mth_dispatcher;
	public @packed:1,compileflags,10 boolean is_mth_inlined_by_dispatcher;

	public @packed:1,compileflags,17 boolean is_mth_virtual_static;
	public @packed:1,compileflags,18 boolean is_mth_operator;
	public @packed:1,compileflags,19 boolean is_mth_invariant;
	
	// Var/field
	public @packed:1,compileflags,8  boolean is_need_proxy;
	public @packed:1,compileflags,9  boolean is_init_wrapper;
	public @packed:1,compileflags,10 boolean is_fld_added_to_init;

	// Field specific
	public @packed:1,compileflags,17 boolean is_fld_packer;
	public @packed:1,compileflags,18 boolean is_fld_packed;

	// General flags
	public @packed:1,compileflags,4 boolean is_accessed_from_inner;
	public @packed:1,compileflags,3 boolean is_resolved;
	public @packed:1,compileflags,1 boolean is_bad;

	public @packed:1,compileflags,16 boolean is_auto_generated;

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

	public void setFrom(Object from$node) {
		ASTNode node = (ASTNode)from$node;
		this.pos = node.pos;
		this.compileflags = node.compileflags;
		this.version = node.version;
		this.locked = node.locked;
		this.prev_version_node = node.prev_version_node;
		this.next_version_node = node.next_version_node;
		super.setFrom(node);
	}

	public final void open() {
		if (!locked)
			return;
		ASTNode node = (ASTNode)this.clone();
		Transaction tr = Transaction.current();
		node.version = tr.version;
		node.prev_version_node = this.prev_version_node;
		node.next_version_node = this;
		this.prev_version_node = node;
		this.locked = false;
		tr.add(this);
		return;
	}

	public final void rollback(boolean save_next) {
		//assert (!locked);
		if (this.prev_version_node == null)
			return;
		assert (this.prev_version_node.locked);
		if (save_next) {
			ASTNode node = (ASTNode)this.clone();
			node.prev_version_node = this;
			this.setFrom(this.prev_version_node);
			this.next_version_node = node;
		} else {
			this.setFrom(this.prev_version_node);
			this.next_version_node = null;
		}
	}

	public final int getPosLine() { return pos >>> 11; }
	
	// build data flow for this node
	public final DataFlowInfo getDFlow() {
		DataFlowInfo df = (DataFlowInfo)DataFlowInfo.ATTR.get(this);
		if (df == null) {
			df = DataFlowInfo.newDataFlowInfo(this);
			DataFlowInfo.ATTR.set(this, df);
		}
		return df;
	}

	public final void replaceWithNodeReWalk(ASTNode node) {
		node = replaceWithNode(node);
		Kiev.runProcessorsOn(node);
		throw new ReWalkNodeException(node);
	}
	public final void replaceWithReWalk(()->ASTNode fnode) {
		ASTNode node = replaceWith(fnode);
		Kiev.runProcessorsOn(node);
		throw new ReWalkNodeException(node);
	}
	public final ASTNode replaceWith(()->ASTNode fnode) {
		assert(isAttached());
		ASTNode parent = parent();
		parent.open();
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
		else if (pslot.isExtData()) {
			assert(parent.getExtData(pslot) == this);
			parent.delExtData(pslot);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			parent.addExtData(n, pslot);
			assert(n.isAttached());
			return n;
		}
		else if (pslot.isTmpData()) {
			assert(parent.getTmpData(pslot) == this);
			parent.delTmpData(pslot);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			parent.addTmpData(n, pslot);
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
			assert(!locked);
			this.is_stat_break_target = on;
			this.callbackChildChanged(nodeattr$flags);
		}
	}

	// the (private) field/method/struct is accessed from inner class (and needs proxy access)
	@getter public final boolean isAccessedFromInner() {
		return this.is_accessed_from_inner;
	}
	@setter public final void setAccessedFromInner(boolean on) {
		this.is_accessed_from_inner = on;
	}
	// resolved
	@getter public final boolean isResolved() {
		return this.is_resolved;
	}
	@setter public final void setResolved(boolean on) {
		this.is_resolved = on;
	}
	// hidden
	@getter public final boolean isAutoGenerated() {
		return this.is_auto_generated;
	}
	@setter public final void setAutoGenerated(boolean on) {
		this.is_auto_generated = on;
	}
	// bad
	@getter public final boolean isBad() {
		return this.is_bad;
	}
	@setter public final void setBad(boolean on) {
		this.is_bad = on;
	}

	public final void cleanDFlow() {
		walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { DataFlowInfo.ATTR.clear(n); return true; }
		});
	}
	
	public Type getType() { return Type.tpVoid; }

	public ASTNode() {
		Transaction tr = Transaction.get();
		if (tr != null) {
			this.version = tr.version;
			tr.add(this);
		}
	}

	public abstract ASTNode getDummyNode();
	
	public boolean hasName(String name, boolean by_equals) {
		return false;
	}
	
	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public boolean preResolveIn() { return true; }
	public void preResolveOut() {}
	public boolean mainResolveIn() { return true; }
	public void mainResolveOut() {}
	public boolean preVerify() { return true; }
	public void postVerify() {}

	public final boolean preGenerate() { return ((RView)this).preGenerate(); }

	public Object doRewrite(RewriteContext ctx) {
		throw new CompilerException(this, "Node "+this.getClass().getName()+" is not a rewriter");
	}

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		return null;
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
