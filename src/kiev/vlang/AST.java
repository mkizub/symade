/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.vlang;

import kiev.be.java15.JType;
import kiev.ir.java15.RNode;
import kiev.be.java15.JNode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface INode {

	@virtual typedef This  ≤ INode;

	public:ro @virtual ANode		ctx_root;
	public:ro @virtual FileUnit		ctx_file_unit;
	public:ro @virtual NameSpace	ctx_name_space;
	public:ro @virtual TypeDecl		ctx_tdecl;
	public:ro @virtual Method		ctx_method;

	public boolean isAttached();
	public void callbackAttached(ANode parent, AttAttrSlot slot);
	public void callbackAttached(ANode parent, DataAttachInfo pinfo);
	public void callbackAttachedToSpace(ANode parent, SpaceAttAttrSlot slot, ANode prv, ANode nxt);
	public void callbackAttached();
	public void callbackDetached();
	public void callbackChildChanged(AttrSlot attr);
	public ANode parent();
	public AttrSlot pslot();
	public AttrSlot[] values();
	public Object getExtData(AttrSlot attr);
	public void setExtData(Object d, AttrSlot attr);
	public void delExtData(AttrSlot attr);
	public void walkTree(TreeWalker walker);
	public This ncopy();
	public This detach() alias fy operator ~ ;
	public This open();
	public void rollback(boolean save_next);
	public <N extends ANode> N replaceWithNode(N node);
	public void initForEditor();
}

public abstract class ANode implements INode {

	@virtual typedef This  ≤ ANode;

	public static final ANode[] emptyArray = new ANode[0];
	private static final AttrSlot[] $values = AttrSlot.emptyArray;

	public:ro @virtual @abstract ANode			ctx_root;
	public:ro @virtual @abstract FileUnit		ctx_file_unit;
	public:ro @virtual @abstract NameSpace		ctx_name_space;
	public:ro @virtual @abstract TypeDecl		ctx_tdecl;
	public:ro @virtual @abstract Method		ctx_method;

	private VersionInfo<This>		version_info;
	private AttachInfo				p_info;
	private ANode					p_parent;
	private DataAttachInfo[]		ext_data;

	public final boolean    isAttached()    { return parent() != null; }

	public static <V extends ANode> V getVersion(V[] arr, int idx) {
		return ANode.getVersion(arr[idx]);
	}

	public static <I extends INode> I getVersion(I node) {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread)
			return node;
		if (node == null || ((ANode)node).version_info == null)
			return node;
		return (I) ((ANode)node).version_info.cur_info.editor_node;
	}

	public static <V extends ANode> V getVersion(V node) {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread)
			return node;
		if (node == null || node.version_info == null)
			return node;
		return node.version_info.cur_info.editor_node;
	}

	public final void callbackAttached(ANode parent, AttAttrSlot slot) {
		//assert (slot.is_attr);
		assert(!isAttached());
		assert(parent != null && parent != this);
		this = this.open();
		this.p_info = slot.simpleAttachInfo;
		this.p_parent = parent;
		this.callbackAttached();
	}
	public final void callbackAttached(ANode parent, DataAttachInfo pinfo) {
		assert (pinfo.p_slot.is_attr);
		assert(!isAttached());
		assert(parent != null && parent != this);
		assert(pinfo.p_data == this);
		this = this.open();
		this.p_info = pinfo;
		this.p_parent = parent;
		this.callbackAttached();
	}
	public final void callbackAttachedToSpace(ANode parent, SpaceAttAttrSlot slot, ANode prv, ANode nxt) {
		assert(!isAttached());
		assert(parent != null && parent != this);
		this = this.open();
		AttachInfo ai_prv = (prv == null ? null : prv.p_info);
		AttachInfo ai_nxt = (nxt == null ? null : nxt.p_info);
		this.p_info = new ListAttachInfo(this, slot, (ListAttachInfo)ai_prv, (ListAttachInfo)ai_nxt);
		this.p_parent = parent;
		this.callbackAttached();
	}
	public void callbackAttached() {
		// notify parent about the changed slot
		parent().callbackChildChanged(p_info.p_slot);
	}
	public void callbackDetached() {
		this = ANode.getVersion(this);
		assert(isAttached());
		this = this.open();
		// do detcah
		AttachInfo pinfo = this.p_info;
		this.p_info = null;
		ANode parent = this.p_parent;
		this.p_parent = null;
		if (pinfo instanceof ListAttachInfo)
			pinfo.unlinkInfo();
		// notify parent about the changed slot
		parent.callbackChildChanged(pinfo.p_slot);
	}


	@getter public final ANode get$ctx_root() {
		ANode n = this;
		while (n.isAttached())
			n = n.parent();
		return n;
	}
	public void callbackChildChanged(AttrSlot attr) { /* do nothing */ }

	public final ANode parent() { return getVersion(this.p_parent); }
	public final AttrSlot pslot() { return this.p_info.p_slot; }

	public static ANode getPrevNode(ANode node) {
		AttachInfo ai = node.p_info;
		if (ai instanceof ListAttachInfo)
			return ai.prev();
		return null;
	}
	public static ANode getNextNode(ANode node) {
		AttachInfo ai = node.p_info;
		if (ai instanceof ListAttachInfo)
			return ai.next();
		return null;
	}
	
	@getter public FileUnit get$ctx_file_unit() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get$ctx_file_unit();
	}
	@getter public NameSpace get$ctx_name_space() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get$ctx_name_space();
	}
	@getter public TypeDecl get$ctx_tdecl() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.child_ctx_tdecl;
	}
	@getter public TypeDecl get$child_ctx_tdecl() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get$child_ctx_tdecl();
	}
	@getter public Method get$ctx_method() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.child_ctx_method;
	}
	@getter public Method get$child_ctx_method() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get$child_ctx_method();
	}

	public AttrSlot[] values() {
		return ANode.$values;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (val instanceof SymbolRef && val.name == null)
			return false;
		if (attr.is_attr)
			return true;
		if (attr.name == "this")
			return pslot().is_attr;
		return false;
	}

	public Object getVal(String name) {
		foreach (AttrSlot a; this.values(); a.name == name)
			return a.get(this);
		if (ext_data != null) {
			foreach (DataAttachInfo ai; ext_data; ai.p_slot.name == name) {
				Object obj = ai.p_data;
				if (obj instanceof ANode)
					return ANode.getVersion((ANode)obj);
				return obj;
			}
		}
		throw new RuntimeException("No @att value \"" + name + "\" in "+getClass().getName());
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @att value \"" + name + "\" in "+getClass().getName());
	}

	public final Object getExtData(AttrSlot attr) {
		if (ext_data != null) {
			foreach (DataAttachInfo ai; ext_data; ai.p_slot.name == attr.name) {
				Object obj = ai.p_data;
				if (obj instanceof ANode)
					return ANode.getVersion((ANode)obj);
				return obj;
			}
		}
		return null;
	}
	
	public final void setExtData(Object d, AttrSlot attr) {
		if (ext_data != null) {
			DataAttachInfo[] data = this.ext_data;
			int sz = data.length;
			for (int i=0; i < sz; i++) {
				DataAttachInfo ai = data[i];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					if (ai.p_data == d)
						return;
					this = this.open();
					ext_data = (DataAttachInfo[])data.clone();
					ext_data[i] = new DataAttachInfo(d,attr);
					if (attr.is_attr) {
						if (ai.p_data instanceof ANode)
							((ANode)ai.p_data).callbackDetached();
						if (d instanceof ANode)
							d.callbackAttached(this, ext_data[i]);
					}
					return;
				}
			}
			this = this.open();
			DataAttachInfo[] tmp = new DataAttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = data[i];
			tmp[sz] = new DataAttachInfo(d,attr);
			ext_data = tmp;
			if (attr.is_attr && d instanceof ANode)
				d.callbackAttached(this, ext_data[sz]);
		} else {
			this = this.open();
			ext_data = new DataAttachInfo[]{new DataAttachInfo(d,attr)};
			if (attr.is_attr && d instanceof ANode)
				d.callbackAttached(this, ext_data[0]);
		}
	}

	public final void delExtData(AttrSlot attr) {
		DataAttachInfo[] data = this.ext_data;
		if (data != null) {
			int sz = data.length-1;
			for (int idx=0; idx <= sz; idx++) {
				DataAttachInfo ai = data[idx];
				if (ai.p_slot.name == attr.name) {
					assert(ai.p_slot == attr);
					this = this.open();
					if (sz == 0) {
						this.ext_data = null;
					} else {
						DataAttachInfo[] tmp = new DataAttachInfo[sz];
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
							getVersion(vals[i]).walkTree(walker);
						} catch (ReWalkNodeException e) {
							i--;
							this = ANode.getVersion(this);
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
						this = ANode.getVersion(this);
						val = attr.get(this);
						if (val != null)
							goto re_walk_node;
					}
				}
			}
			if (ext_data != null) {
				foreach (DataAttachInfo ai; ext_data; ai.p_slot.is_attr && ai.p_slot.is_external && ai.p_data instanceof ANode)
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
			node.ext_data = new DataAttachInfo[N];
			for (int i=0; i < N; i++) {
				DataAttachInfo ai = this.ext_data[i];
				if (ai.p_slot.is_attr && ai.p_data instanceof ASTNode) {
					ASTNode nd = ((ASTNode)ai.p_data).ncopy();
					nd.callbackAttached(node, (node.ext_data[i] = new DataAttachInfo(nd,ai.p_slot)));
				} else {
					node.ext_data[i] = ai;
				}
			}
		}
		return node;
	}
	
	public final This detach()
		alias fy operator ~
	{
		if (!isAttached())
			return this;
		this = this.open();
		ANode parent = parent();
		parent = parent.open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot)
			pslot.detach(parent, this);
		else
			pslot.clear(parent);
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
		this.p_parent = node.p_parent;
		this.ext_data = node.ext_data;
	}
	
	public final This open() {
		if (Kiev.run_batch)
			return this;
		if!(this instanceof ASTNode)
			return this;
		if (!((ASTNode)this).locked)
			return this;
		return (This)open2((ASTNode)this);
	}

	private static <T extends ASTNode> T open2(T self) { 
		if (Thread.currentThread() == CompilerThread)
			self = openCmp(self);
		else
			self = openEdt(self);
		Transaction tr = Transaction.current();
		self.locked = false;
		tr.add(self);
		return self;
	}
	private static <T extends ASTNode> T openCmp(T self) {
		CurrentVersionInfo<T> cvi = (CurrentVersionInfo<T>)self.version_info;
		if (cvi == null)
			self.version_info = cvi = new CurrentVersionInfo<T>(self);
		ASTNode node = (ASTNode)self.clone();
		node.version_info = new VersionInfo<T>(cvi,cvi.prev_node,node);
		cvi.prev_node = node;
		if (cvi.editor_node == self)
			cvi.editor_node = node;
		else if (cvi.editor_node.version_info.prev_node == self)
			cvi.editor_node.version_info.prev_node = node;
		return self;
	}
	private static <T extends ASTNode> T openEdt(T self) {
		if (self.version_info == null)
			self.version_info = new CurrentVersionInfo<T>(self);
		VersionInfo<T> vi = self.version_info;
		CurrentVersionInfo<T> cvi = vi.cur_info;
		assert (cvi.editor_node == self);
		T node = (T)self.clone();
		node.version_info = new VersionInfo<T>(cvi,self,node);
		cvi.editor_node = node;
		return node;
	}

	public final void rollback(boolean save_next) {
		if (this.version_info == null)
			return;
		if (Thread.currentThread() == CompilerThread) {
			assert (this.version_info instanceof CurrentVersionInfo);
			CurrentVersionInfo<This> cvi = (CurrentVersionInfo<This>)this.version_info;
			ANode p = cvi.prev_node;
			if (p == null)
				return;
			assert (((ASTNode)p).locked);
			this.setFrom(p);
			assert (this.version_info instanceof CurrentVersionInfo<This>);
			cvi.prev_node = p.version_info.prev_node;
		} else {
			assert (this.version_info.cur_info.editor_node == this);
			assert (this.version_info.prev_node != null);
			this.version_info.cur_info.editor_node = this.version_info.prev_node;
		}
	}

	public void mergeTree() {
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { n.merge(); return true; }
		});
	}

	void merge() {
		if (this.version_info == null)
			return;
		CurrentVersionInfo<This> cvi = this.version_info.cur_info;
		This edt = cvi.editor_node;
		VersionInfo<This> evi = edt.version_info;
		if (cvi != evi) {
			for (VersionInfo<This> vi=evi;;vi=vi.prev_node.version_info) {
				if (vi == cvi) {
					openCmp((ASTNode)cvi.node);
					break;
				}
				if (vi.prev_node == null)
					break;
			}
			cvi.node.setFrom(edt);
		} else {
			assert (cvi.editor_node == cvi.node);
		}
	}

	public final <N extends ANode> N replaceWithNode(N node) {
		assert(isAttached());
		ANode parent = parent();
		parent = parent.open();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			assert(node != null);
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, idx, node);
		} else {
			assert(pslot.get(parent) == this);
			if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
				((ASTNode)node).pos = ((ASTNode)this).pos;
			pslot.set(parent, node);
		}
		assert(node == null || node.isAttached());
		return node;
	}
	
	public ANode doRewrite(RewriteContext ctx) {
		if !(this instanceof ASTNode)
			return this.ncopy();
		ANode rn = getClass().newInstance();
		foreach (AttrSlot attr; this.values(); attr.is_attr) {
			Object val = attr.get(this);
			if (val == null)
				continue;
			if (!attr.is_attr) {
				//attr.set(rn,val);
				continue;
			}
			if (attr.is_space) {
				ANode[] vals = (ANode[])val;
				for (int i=0; i < vals.length; i++)
					((SpaceAttrSlot)attr).add(rn,(ANode)ctx.fixup(attr,getVersion(vals[i]).doRewrite(ctx)));
			}
			else if (val instanceof ANode)
				attr.set(rn,ctx.fixup(attr,getVersion((ANode)val).doRewrite(ctx)));
			else
				attr.set(rn,ctx.fixup(attr,val));
		}
		if (this.ext_data != null) {
			foreach (DataAttachInfo ai; this.ext_data; ai.p_slot.is_attr && ai.p_slot.is_external) {
				if (ai.p_data instanceof ANode)
					this.setExtData(ctx.fixup(ai.p_slot,getVersion((ANode)ai.p_data).doRewrite(ctx)),ai.p_slot);
				else
					this.setExtData(ctx.fixup(ai.p_slot,ai.p_data),ai.p_slot);
			}
		}
		return rn;
	}
	
	public boolean backendCleanup() { return true; }

	public void initForEditor() { /* by default do nothing */ }
}

public class TreeWalker {
	public boolean pre_exec(ANode n) { return true; }
	public void post_exec(ANode n) {}
}

public interface PreScanneable {
	public boolean setBody(ENode body);
}

class AttachInfo {
	final   AttrSlot	p_slot;
	AttachInfo(AttrSlot slot) {
		this.p_slot = slot;
	}
}

final class DataAttachInfo extends AttachInfo {
	final   Object		p_data;
	DataAttachInfo(Object data, AttrSlot slot) {
		super(slot);
		this.p_data = data;
	}
}

final class ListAttachInfo extends AttachInfo {
	final   ANode			p_node;
	private ListAttachInfo	p_prev;
	private ListAttachInfo	p_next;
	ListAttachInfo(ANode self, AttrSlot slot, ListAttachInfo prev, ListAttachInfo next) {
		super(slot);
		this.p_node = self;
		if (prev != null) {
			this.p_prev = prev;
			prev.p_next = this;
		}
		if (next != null) {
			next.p_prev = this;
			this.p_next = next;
		}
	}
	ANode next() { return p_next == null ? null : ANode.getVersion(p_next.p_node); }
	ANode prev() { return p_prev == null ? null : ANode.getVersion(p_prev.p_node); }
	void unlinkInfo() {
		if (p_prev != null)
			p_prev.p_next = p_next;
		if (p_next != null)
			p_next.p_prev = p_prev;
	}
}

class VersionInfo<N extends ANode> {
	final CurrentVersionInfo		cur_info;
	      N							prev_node;
	      N							node;
	VersionInfo(CurrentVersionInfo<N> cur_info, N prev_node, N node) {
		this.cur_info = cur_info;
		this.prev_node = prev_node;
		this.node = node;
	}
	VersionInfo(N node) {
		this.cur_info = (CurrentVersionInfo<N>)this;
		this.prev_node = null;
		this.node = node;
	}
}

class CurrentVersionInfo<N extends ANode> extends VersionInfo<N> {
	N editor_node;
	CurrentVersionInfo(N node) {
		super(node);
		editor_node = node;
	}
}

@node
public abstract class ASTNode extends ANode implements Constants, Cloneable {

	@virtual typedef This  ≤ ASTNode;
	@virtual typedef JView ≤ JNode;
	@virtual typedef RView ≤ RNode;
	
	public static final ASTNode[] emptyArray = new ASTNode[0];

	private static final class RefAttrSlot_this extends RefAttrSlot {
		RefAttrSlot_this(String name, TypeInfo typeinfo) { super(name, typeinfo); }
		public final void set(ANode parent, Object value) { throw new RuntimeException("@ref thisis not writeable"); }
		public final Object get(ANode parent) { return parent; }
	}
	public static final RefAttrSlot_this nodeattr$this = new RefAttrSlot_this("this", TypeInfo.newTypeInfo(ANode.class,null));

	private static final class RefAttrSlot_parent extends RefAttrSlot {
		RefAttrSlot_parent(String name, TypeInfo typeinfo) { super(name, typeinfo); }
		public final void set(ANode parent, Object value) { throw new RuntimeException("@ref parent is not writeable"); }
		public final Object get(ANode parent) { return parent.parent(); }
	}
	public static final RefAttrSlot_parent nodeattr$parent = new RefAttrSlot_parent("parent", TypeInfo.newTypeInfo(ANode.class,null));

	private static final AttrSlot[] $values = {/*nodeattr$this,*/nodeattr$parent};

	public int						pos;
	public int						compileflags;
	@ref @abstract
	public:ro ANode					parent;
	
	@getter @ref public final ANode get$this()   { return this; }
	@getter @ref public final ANode get$parent() { return parent(); }

	// SymbolRef/ENode/ISymRef
	public @packed:1,compileflags,24  boolean is_qualified; // qualified or simple name, names are separated by ASCII US (Unit Separator, 037, 0x1F)
	
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
	public @packed:1,compileflags,22 boolean is_struct_compiler_node;
	
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
	public @packed:1,compileflags,21 boolean is_rewrite_target;
	
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
	public @packed:1,compileflags,5 boolean is_rewrite_target;
	public @packed:1,compileflags,4 boolean is_accessed_from_inner;
	public @packed:1,compileflags,3 boolean is_resolved;
	public @packed:1,compileflags,2 boolean is_bad;
	public @packed:1,compileflags,0 boolean locked;

	public @packed:1,compileflags,16 boolean is_auto_generated;

	public AttrSlot[] values() {
		return ASTNode.$values;
	}

	public Object copyTo(Object to$node) {
		ASTNode node = (ASTNode)super.copyTo(to$node);
		node.pos			= this.pos;
		node.compileflags	= this.compileflags & 0xFFFF0000;
		return node;
	}

	public void setFrom(Object from$node) {
		ASTNode node = (ASTNode)from$node;
		this.pos = node.pos;
		this.compileflags = node.compileflags;
		super.setFrom(node);
	}

	public final int getPosLine() { return pos >>> 11; }
	
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
		parent = parent.open();
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
		} else {
			assert(pslot.get(parent) == this);
			pslot.set(parent, null);
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
		}
	}

	// rewrite target (ENodes)
	public final boolean isRewriteTarget() {
		return this.is_rewrite_target;
	}
	public final void setRewriteTarget(boolean on) {
		this.is_rewrite_target = on;
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
	@setter public void setAutoGenerated(boolean on) {
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
		if!(Thread.currentThread() instanceof WorkerThread)
			return;
		java.util.Hashtable dftbl = ((WorkerThread)Thread.currentThread()).dataFlowInfos;
		walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { dftbl.remove(n); return true; }
		});
	}
	
	public Type getType() { return Type.tpVoid; }

	public ASTNode() {
		Transaction tr = Transaction.get();
		if (tr != null)
			tr.add(this);
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
