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
package kiev.vtree;

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
	public void callbackAttached(ANode parent, AttrSlot slot);
	public void callbackAttached(ParentInfo pi);
	public void callbackDetached(ANode parent, AttrSlot slot);
	public void callbackChildChanged(AttrSlot attr);
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data);
	public ANode parent();
	public AttrSlot pslot();
	public AttrSlot[] values();
	public Object getExtData(AttrSlot attr);
	public void setExtData(Object d, AttrSlot attr);
	public void delExtData(AttrSlot attr);
	public void walkTree(TreeWalker walker);
	public This ncopy();
	public This detach() alias fy operator ~ ;
	public <N extends ANode> N replaceWithNode(N node);
	public void initForEditor();
}

public enum ChildChangeType {
	UNKNOWN,
	ATTACHED,
	DETACHED,
	MODIFIED
}

public abstract class ANode implements INode {

	@virtual typedef This  ≤ ANode;
	
	public static final ANode[] emptyArray = new ANode[0];
	
	static final class RefAttrSlot_this extends RefAttrSlot {
		RefAttrSlot_this(String name, TypeInfo typeinfo) { super(name, typeinfo); }
		public final void set(ANode parent, Object value) { throw new RuntimeException("@nodeData 'this' is not writeable"); }
		public final Object get(ANode parent) { return parent; }
	}
	public static final RefAttrSlot_this nodeattr$this = new RefAttrSlot_this("this", TypeInfo.newTypeInfo(ANode.class,null));

	public static final ParentAttrSlot nodeattr$parent =
			new ParentAttrSlot("parent", false, true, TypeInfo.newTypeInfo(ANode.class,null));

	public static final ParentAttrSlot nodeattr$syntax_parent =
			new ParentAttrSlot("syntax_parent", true, true, TypeInfo.newTypeInfo(ANode.class,null));

	private static final AttrSlot[] $values = {/*ANode.nodeattr$this,*/ ANode.nodeattr$parent};

	
	public:ro @virtual @abstract ANode			ctx_root;
	public:ro @virtual @abstract FileUnit		ctx_file_unit;
	public:ro @virtual @abstract NameSpace		ctx_name_space;
	public:ro @virtual @abstract TypeDecl		ctx_tdecl;
	public:ro @virtual @abstract Method		ctx_method;

	AttrSlot				p_slot;
	ANode					p_parent;
	DataAttachInfo[]		ext_data;
	ParentInfo[]			ext_parent;

	public static class VVV implements Cloneable {
		public static final int IS_LOCKED    = 1;
		public static final int FOR_COMPILER = 2;
		public static final int FOR_EDITOR   = 4;
		int						vvv_flags;
		ASTNode.VVV				vvv_prev;
		ASTNode.VVV				vvv_next;
		AttrSlot				p_slot;
		ANode					p_parent;
		DataAttachInfo[]		ext_data;
		ParentInfo[]			ext_parent;
		
		public VVV(ANode node) {
			this.p_slot = node.p_slot;
			this.p_parent = node.p_parent;
			this.ext_data = node.ext_data;
			this.ext_parent = node.ext_parent;
		}
		public Object clone() { super.clone() }
	}
	
	public static final class CopyContext {
		public static final class SymbolInfo {
			ISymbol sold; // old symbol
			ISymbol snew; // new symbol, copied from sold
			List<ASTNode> srefs; // new symbol ref, to be changed to point from sold to snew
			SymbolInfo(ISymbol sold, ISymbol snew) {
				this.sold = sold;
				this.snew = snew;
				this.srefs = List.Nil;
			}
			SymbolInfo(ISymbol sold, ASTNode sref) {
				this.sold = sold;
				this.snew = null;
				this.srefs = List.newList(sref);
			}
		};
		private Vector<SymbolInfo> infos;
		void addSymbol(ISymbol sold, ISymbol snew) {
			if (infos == null)
				infos = new Vector<SymbolInfo>();
			foreach (SymbolInfo si; infos; si.sold == sold) {
				assert(si.snew == null);
				si.snew = snew;
				return;
			}
			infos.append(new SymbolInfo(sold, snew));
		}
		void addSymbolRef(ASTNode sref, ISymbol sold) {
			if (sold == null)
				return;
			if (infos == null)
				infos = new Vector<SymbolInfo>();
			foreach (SymbolInfo si; infos; si.sold == sold) {
				si.srefs = new List.Cons<ASTNode>(sref, si.srefs);
				return;
			}
			infos.append(new SymbolInfo(sold, sref));
		}
		public ANode hasCopyOf(ANode node) {
			if (infos == null)
				return null;
			foreach (SymbolInfo si; infos; si.sold == node)
				return (ANode)si.snew;
			return null;
		}
		public void updateLinks() {
			if (infos == null)
				return;
			foreach (SymbolInfo si; infos; si.snew != null) {
				foreach (ASTNode n; si.srefs) {
					if (n instanceof SymbolRef) {
						SymbolRef sr = (SymbolRef)n;
						if (sr.symbol == si.sold)
							sr.symbol = si.snew;
					}
					else if (n instanceof TypeArgRef) {
						TypeArgRef en = (TypeArgRef)n;
						if (en.symbol == si.sold)
							en.type_lnk = ((TypeDef)si.snew).getAType();
					}
					else if (n instanceof ENode) {
						ENode en = (ENode)n;
						if (en.symbol == si.sold)
							en.symbol = si.snew;
					}
				}
			}
		}
	}
	
	@getter @nodeData final AttrSlot get$p_slot() {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread)
			return this.p_slot;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.p_slot;
		return this.p_slot;
	}
	
	@setter @nodeData final void set$p_slot(AttrSlot value) {
		if (Kiev.run_batch || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.p_slot = value;
		else if (Thread.currentThread() == CompilerThread)
			ASTNode.openCmp((ASTNode)this).p_slot = value;
		else
			ASTNode.openEdt((ASTNode)this).p_slot = value;
	}
	
	@getter @nodeData final ANode get$p_parent() {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread)
			return this.p_parent;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.p_parent;
		return this.p_parent;
	}
	
	@setter @nodeData final void set$p_parent(ANode value) {
		if (Kiev.run_batch || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.p_parent = value;
		else if (Thread.currentThread() == CompilerThread)
			ASTNode.openCmp((ASTNode)this).p_parent = value;
		else
			ASTNode.openEdt((ASTNode)this).p_parent = value;
	}
	
	@getter @nodeData final DataAttachInfo[] get$ext_data() {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread)
			return this.ext_data;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.ext_data;
		return this.ext_data;
	}
	
	@setter @nodeData final void set$ext_data(DataAttachInfo[] value) {
		if (Kiev.run_batch || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.ext_data = value;
		else if (Thread.currentThread() == CompilerThread)
			ASTNode.openCmp((ASTNode)this).ext_data = value;
		else
			ASTNode.openEdt((ASTNode)this).ext_data = value;
	}
	
	public final boolean    isAttached()    { return parent() != null; }

	// attach to parent node 'parent' to parent's slot 'slot'
	public final void callbackAttached(ANode parent, AttrSlot slot) {
		assert (slot.is_attr);
		if (slot.parent_attr_slot == ANode.nodeattr$parent) {
			assert(!isAttached());
			assert(parent != null && parent != this);
			this.p_slot = slot;
			this.p_parent = parent;
			this.callbackAttached(new ParentInfo(parent,slot));
		} else {
			if (ext_parent != null) {
				ParentInfo[] data = this.ext_parent;
				int sz = data.length;
				for (int i=0; i < sz; i++) {
					ParentInfo pi = data[i];
					if (pi.p_parent == parent && pi.p_slot == slot)
						return;
					assert (!pi.p_slot.parent_attr_slot.is_unique || pi.p_slot.parent_attr_slot != slot.parent_attr_slot);
				}
				ParentInfo[] tmp = new ParentInfo[sz+1];
				for (int i=0; i < sz; i++)
					tmp[i] = data[i];
				tmp[sz] = new ParentInfo(parent,slot);
				ext_data = tmp;
				this.callbackAttached(tmp[sz]);
			} else {
				ParentInfo pi = new ParentInfo(parent,slot);
				ext_parent = new ParentInfo[]{pi};
				this.callbackAttached(pi);
			}
		}
	}
	public void callbackAttached(ParentInfo pi) {
		// notify parent about the changed slot
		pi.p_parent.callbackChildChanged(ChildChangeType.ATTACHED,pi.p_slot,this);
	}
	
	public void callbackDetached(ANode parent, AttrSlot slot) {
		assert (slot.is_attr);
		if (slot.parent_attr_slot == ANode.nodeattr$parent) {
			assert(isAttached());
			assert(p_parent == parent);
			assert(p_slot == slot);
			this.p_slot = null;
			this.p_parent = null;
			// notify parent about the changed slot
			parent.callbackChildChanged(ChildChangeType.DETACHED,slot,this);
		} else {
			ParentInfo[] data = this.ext_parent;
			if (data == null)
				return;
			int sz = data.length-1;
			for (int idx=0; idx <= sz; idx++) {
				ParentInfo pi = data[idx];
				if (pi.p_parent == parent && pi.p_slot == slot) {
					if (sz == 0) {
						this.ext_parent = null;
					} else {
						ParentInfo[] tmp = new ParentInfo[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = data[i];
						for (   ; i <  sz; i++) tmp[i] = data[i+1];
						this.ext_parent = tmp;
					}
					pi.p_parent.callbackChildChanged(ChildChangeType.DETACHED,slot,this);
					return;
				}
			}
		}
	}


	@getter public final ANode get$ctx_root() {
		ANode n = this;
		while (n.isAttached())
			n = n.parent();
		return n;
	}
	public final void callbackChildChanged(AttrSlot attr) { callbackChildChanged(ChildChangeType.UNKNOWN, attr, null); }
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) { /* do nothing */ }

	public final ANode parent() { return this.p_parent; }
	public final AttrSlot pslot() { return this.p_slot; }

	public static ANode getPrevNode(ANode node) {
		AttrSlot slot = node.p_slot;
		if (slot == null || !slot.is_space)
			return null;
		ANode[] arr = slot.get(node.parent());
		for (int i=arr.length-1; i >= 0; i--) {
			ANode n = arr[i];
			if (n == node) {
				if (i == 0)
					return null;
				return arr[i-1];
			}
		}
		return null;
	}
	public static ANode getNextNode(ANode node) {
		AttrSlot slot = node.p_slot;
		if (slot == null || !slot.is_space)
			return null;
		ANode[] arr = slot.get(node.parent());
		for (int i=arr.length-1; i >= 0; i--) {
			ANode n = arr[i];
			if (n == node) {
				if (i >= arr.length-1)
					return null;
				return arr[i+1];
			}
		}
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
			foreach (DataAttachInfo ai; ext_data; ai.p_slot.name == name)
				return ai.p_data;
		}
		throw new RuntimeException("No @nodeAttr value \"" + name + "\" in "+getClass().getName());
	}
	public void setVal(String name, Object val) {
		throw new RuntimeException("No @nodeAttr value \"" + name + "\" in "+getClass().getName());
	}

	public final Object getExtData(AttrSlot attr) {
		if (ext_data != null) {
			foreach (DataAttachInfo ai; ext_data; ai.p_slot.name == attr.name)
				return ai.p_data;
		}
		return null;
	}
	
	public final ANode getExtParent(ParentAttrSlot attr) {
		assert (attr.is_unique);
		if (ext_parent != null) {
			foreach (ParentInfo pi; ext_parent; pi.p_slot.parent_attr_slot == attr)
				return pi.p_parent;
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
					ext_data = (DataAttachInfo[])data.clone();
					ext_data[i] = new DataAttachInfo(d,attr);
					if (attr.is_attr) {
						if (ai.p_data instanceof ANode)
							((ANode)ai.p_data).callbackDetached(this, attr);
						if (d instanceof ANode)
							d.callbackAttached(this, attr);
					}
					return;
				}
			}
			DataAttachInfo[] tmp = new DataAttachInfo[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = data[i];
			tmp[sz] = new DataAttachInfo(d,attr);
			ext_data = tmp;
			if (attr.is_attr && d instanceof ANode)
				d.callbackAttached(this, attr);
		} else {
			ext_data = new DataAttachInfo[]{new DataAttachInfo(d,attr)};
			if (attr.is_attr && d instanceof ANode)
				d.callbackAttached(this, attr);
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
						((ANode)ai.p_data).callbackDetached(this, attr);
					return;
				}
			}
		}
	}

	public final void delExtParent(ParentAttrSlot attr) {
		assert (attr.is_unique);
		ParentInfo[] data = this.ext_parent;
		if (data != null) {
			int sz = data.length-1;
			for (int idx=0; idx <= sz; idx++) {
				ParentInfo pi = data[idx];
				if (pi.p_slot.parent_attr_slot == attr) {
					if (sz == 0) {
						this.ext_parent = null;
					} else {
						ParentInfo[] tmp = new ParentInfo[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = data[i];
						for (   ; i <  sz; i++) tmp[i] = data[i+1];
						this.ext_parent = tmp;
					}
					pi.p_parent.callbackDetached(this, pi.p_slot);
					return;
				}
			}
		}
	}

	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.is_child) {
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
				foreach (DataAttachInfo ai; ext_data; ai.p_slot.is_attr && ai.p_slot.is_external && ai.p_data instanceof ANode)
					((ANode)ai.p_data).walkTree(walker);
			}
		}
		walker.post_exec(this);
	}

	public void setFrom(Object from) {
		throw new Error(); // redundant code, method shell be removed
	}
	public ASTNode.VVV nodeBackup() {
		throw new Error();
	}
	public void nodeRestore(ASTNode.VVV from) {
		this.p_slot = from.p_slot;
		this.p_parent = from.p_parent;
		this.ext_data = from.ext_data;
	}
	

	public final This ncopy() {
		CopyContext cc = new CopyContext();
		This t = (This)this.copy(cc);
		cc.updateLinks();
		return t;
	}
	public final This ncopy(CopyContext cc) {
		return (This)this.copy(cc);
	}

	public Object copy(CopyContext cc) {
		if (this instanceof TypeInfoInterface)
			return this.copyTo(((TypeInfoInterface)this).getTypeInfoField().newInstance(), cc);
		else
			return this.copyTo(this.getClass().newInstance(), cc);
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ANode node = (ANode)to$node;
		if (this.ext_data != null) {
			int N = this.ext_data.length;
			node.ext_data = new DataAttachInfo[N];
			for (int i=0; i < N; i++) {
				DataAttachInfo ai = this.ext_data[i];
				if (ai.p_slot.is_attr && ai.p_data instanceof ASTNode) {
					ASTNode nd = ((ASTNode)ai.p_data).ncopy(in$context);
					node.ext_data[i] = new DataAttachInfo(nd,ai.p_slot);
					nd.callbackAttached(node, ai.p_slot);
				} else {
					node.ext_data[i] = ai;
				}
			}
		}
		if (this instanceof ISymbol)
			in$context.addSymbol((ISymbol)this,(ISymbol)node);
		else if (this instanceof SymbolRef)
			in$context.addSymbolRef((SymbolRef)node, ((SymbolRef)this).symbol);
		else if (this instanceof ENode)
			in$context.addSymbolRef((ENode)node, ((ENode)this).symbol);
		return node;
	}
	
	public final This detach()
		alias fy operator ~
	{
		if (!isAttached())
			return this;
		ANode parent = parent();
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
		throw new RuntimeException("No @nodeAttr/@nodeData attribute '"+name+"' in "+getClass());
	}
	
	public final SpacePtr getSpacePtr(String name) {
		foreach (AttrSlot attr; this.values(); attr.name == name && attr.is_space)
			return new SpacePtr(this, (SpaceAttrSlot<ASTNode>)attr);
		throw new RuntimeException("No @nodeAttr/@nodeData space '"+name+"' in "+getClass());
	}

	public final <N extends ANode> N replaceWithNode(N node) {
		assert(isAttached());
		ANode parent = parent();
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
				for (int i=0; i < vals.length; i++) {
					ANode n = vals[i].doRewrite(ctx);
					if (n instanceof BlockRewr) {
						foreach (ASTNode st; n.stats) {
							n = (ANode)ctx.fixup(attr,st);
							if (n != null)
								((SpaceAttrSlot)attr).add(rn,n);
						}
					} else {
						n = (ANode)ctx.fixup(attr,n);
						if (n != null)
							((SpaceAttrSlot)attr).add(rn,n);
					}
				}
			}
			else if (attr.name == "meta" && val instanceof MetaSet) {
				MetaSet ms = (MetaSet)ctx.fixup(attr,val.doRewrite(ctx));
				MetaSet rs = (MetaSet)attr.get(rn);
				foreach (MNode mn; ms.metas.delToArray())
					rs.setMeta(mn);
			}
			else if (val instanceof ANode) {
				ANode rw = val.doRewrite(ctx);
				while (rw instanceof BlockRewr && rw.stats.length == 1)
					rw = rw.stats[0];
				attr.set(rn,ctx.fixup(attr,rw));
			}
			else
				attr.set(rn,ctx.fixup(attr,val));
		}
		if (this.ext_data != null) {
			foreach (DataAttachInfo ai; this.ext_data; ai.p_slot.is_attr && ai.p_slot.is_external) {
				if (ai.p_data instanceof ANode) {
					Object o = ctx.fixup(ai.p_slot,((ANode)ai.p_data).doRewrite(ctx));
					if (o != null)
						this.setExtData(o,ai.p_slot);
				} else
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

final class DataAttachInfo {
	final   AttrSlot	p_slot;
	final   Object		p_data;
	DataAttachInfo(Object data, AttrSlot slot) {
		this.p_slot = slot;
		this.p_data = data;
	}
}

public final class ParentInfo {
	public final   ANode			p_parent;
	public final   AttrSlot			p_slot;
	ParentInfo(ANode parent, AttrSlot slot) {
		this.p_parent = parent;
		this.p_slot = slot;
	}
	public boolean isSemantic() {
		return this.p_slot.isSemantic();
	}
}

class VersionInfo {
	final CurrentVersionInfo	cur_info;
	final ANode					node;
	      ANode					prev_node;
	VersionInfo(CurrentVersionInfo cur_info, ANode prev_node, ANode node) {
		this.cur_info = cur_info;
		this.prev_node = prev_node;
		this.node = node;
	}
	VersionInfo(ANode node) {
		this.cur_info = (CurrentVersionInfo)this;
		this.prev_node = null;
		this.node = node;
	}
}

class CurrentVersionInfo extends VersionInfo {
	ANode compiler_node;
	ANode editor_node;
	CurrentVersionInfo(ANode node) {
		super(node);
		compiler_node = node;
		editor_node = node;
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class ASTNode extends ANode implements Constants, Cloneable {

	@virtual typedef This  ≤ ASTNode;
	@virtual typedef JView ≤ JNode;
	@virtual typedef RView ≤ RNode;
	
	public static final ASTNode[] emptyArray = new ASTNode[0];

	private static final AttrSlot[] $values = {/*ANode.nodeattr$this,*/ ANode.nodeattr$parent};

	@UnVersioned
	public VVV						v_editor;
	@UnVersioned
	public int						transaction_id;
	@UnVersioned
	public int						pos;
	@UnVersioned
	public int						compileflags;	// temporal flags for compilation process
	
	public int						nodeflags;		// presistent flags of the node
	@nodeData @abstract
	public:ro ANode					parent;
	
	public static class VVV extends ANode.VVV {
		int						transaction_id;
		int						compileflags;
		int						nodeflags;

		public VVV(ASTNode node) {
			super(node);
			this.compileflags = node.compileflags;
			this.nodeflags = node.nodeflags;
		}
	}
	
	@setter public final void set$nodeflags(int value) {
		if (Kiev.run_batch || !this.versioned)
			this.nodeflags = value;
		else if (Thread.currentThread() == CompilerThread)
			ASTNode.openCmp(this).nodeflags = value;
		else
			ASTNode.openEdt(this).nodeflags = value;
	}
	
	@getter public final int get$nodeflags() {
		if (Kiev.run_batch || Thread.currentThread() == CompilerThread || this.v_editor == null)
			return this.nodeflags;
		return this.v_editor.nodeflags;
	}
	
	@setter public final void set$pos(int value) { this.pos = value; }
	@getter public final int get$pos() { return this.pos; }
	
	@getter public final ANode get$parent() { return parent(); }

	// SymbolRef/ENode/ISymRef
	public @packed:1,nodeflags,24  boolean is_qualified; // qualified or simple name, names are separated by ASCII US (Unit Separator, 037, 0x1F)
	// EToken
	public @packed:1,nodeflags,20  boolean is_explicit; // explicit or implicit kind of EToken
	
	// Structures	
	public @packed:1,compileflags,8  boolean is_struct_type_resolved; // KievFE_Pass2
	public @packed:1,compileflags,9  boolean is_struct_args_resolved; // KievFE_Pass2
	public @packed:1,compileflags,10 boolean is_struct_members_generated; // KievFE_Pass2
	public @packed:1,compileflags,11 boolean is_struct_pre_generated; // KievME_PreGenartion

	public @packed:1,nodeflags,17 boolean is_struct_fe_passed;
	public @packed:1,nodeflags,18 boolean is_struct_local;
	public @packed:1,nodeflags,19 boolean is_struct_anomymouse;
	public @packed:1,nodeflags,20 boolean is_struct_has_pizza_cases;
	public @packed:1,nodeflags,21 boolean is_struct_bytecode;	// struct was loaded from bytecode
	public @packed:1,nodeflags,22 boolean is_struct_compiler_node;

	// EToken (unresolved e-node tokens) flags
	public @packed:1,compileflags,8  boolean is_token_ident;
	public @packed:1,compileflags,9  boolean is_token_operator;
	public @packed:1,compileflags,10 boolean is_token_keyword;
	public @packed:1,compileflags,11 boolean is_token_constant;
	public @packed:1,compileflags,12 boolean is_token_type_decl;

	// Expression/statement flags
	public @packed:1,compileflags,8  boolean is_expr_gen_void;
	public @packed:1,compileflags,9  boolean is_expr_for_wrapper;
	public @packed:1,compileflags,10 boolean is_expr_cast_call;

	public @packed:1,nodeflags,17 boolean is_expr_as_field;
	public @packed:1,nodeflags,18 boolean is_expr_primary;
	public @packed:1,nodeflags,19 boolean is_expr_super;

	// Statement flags
	public @packed:1,compileflags,11 boolean is_stat_abrupted;
	public @packed:1,compileflags,12 boolean is_stat_breaked;
	public @packed:1,compileflags,13 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public @packed:1,compileflags,14 boolean is_stat_auto_returnable;
	public @packed:1,compileflags,15 boolean is_direct_flow_reachable; // reachable by direct control flow (with no jumps)

	public @packed:1,nodeflags,20 boolean is_stat_break_target;

	// Method flags
	public @packed:1,compileflags,8  boolean is_mth_need_fields_init;
	public @packed:1,compileflags,9  boolean is_mth_dispatcher;

	public @packed:1,nodeflags,17 boolean is_mth_virtual_static;
	public @packed:1,nodeflags,18 boolean is_mth_operator;
	public @packed:1,nodeflags,19 boolean is_mth_invariant;
	
	// Var/field
	public @packed:1,compileflags,8  boolean is_need_proxy;
	public @packed:1,compileflags,9  boolean is_init_wrapper;
	public @packed:1,compileflags,10 boolean is_fld_added_to_init;

	// Field specific
	public @packed:1,nodeflags,17 boolean is_fld_packer;
	public @packed:1,nodeflags,18 boolean is_fld_packed;

	// General flags
	public @packed:1,nodeflags,4 boolean is_rewrite_target;
	public @packed:1,compileflags,3 boolean is_resolved;
	public @packed:1,compileflags,2 boolean is_bad;
	public @packed:1,compileflags,1 boolean versioned;
	public @packed:1,compileflags,0 boolean locked;

	public @packed:1,nodeflags,16 boolean is_auto_generated;

	public AttrSlot[] values() {
		return ASTNode.$values;
	}

	public ASTNode.VVV nodeBackup() {
		return new ASTNode.VVV(this);
	}

	public void nodeRestore(ASTNode.VVV from) {
		this.transaction_id = from.transaction_id;
		this.compileflags = from.compileflags & ~3;
		this.nodeflags = from.nodeflags;
		super.nodeRestore(from);
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ASTNode node = (ASTNode)super.copyTo(to$node, in$context);
		node.compileflags	= 0;
		node.nodeflags		= this.nodeflags;
		return node;
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
	// resolved
	public final boolean isResolved() {
		return this.is_resolved;
	}
	public final void setResolved(boolean on) {
		this.is_resolved = on;
	}
	// hidden
	public final boolean isAutoGenerated() {
		return this.is_auto_generated;
	}
	public void setAutoGenerated(boolean on) {
		this.is_auto_generated = on;
	}
	// bad
	public final boolean isBad() {
		return this.is_bad;
	}
	public final void setBad(boolean on) {
		this.is_bad = on;
	}

	public Type getType() { return Type.tpVoid; }

	public ASTNode() {
		Transaction tr = Transaction.get();
		if (tr != null) {
			this.transaction_id = tr.version;
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

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		return null;
	}

	public static <A extends ASTNode> A openCmp(A self) {
		if (!self.locked)
			return self;
		ASTNode.openCmp2(self);
		return self;
	}
	private static synchronized void openCmp2(ASTNode self) {
		ASTNode.VVV v = self.nodeBackup();
		v.transaction_id = self.transaction_id;
		v.vvv_flags = VVV.IS_LOCKED|VVV.FOR_COMPILER;
		if (self.v_editor == null) {
			self.v_editor = v;
		} else {
			ASTNode.VVV n;
			for (n = self.v_editor; n.vvv_next != null; n = n.vvv_next);
			n.vvv_next = v;
			v.vvv_prev = n;
		}
		self.locked = false;
		Transaction tr = Transaction.get();
		assert (tr != null);
		self.transaction_id = tr.version;
		tr.add(self);
	}
	public static ASTNode.VVV openEdt(ASTNode self) {
		ASTNode.VVV ve = self.v_editor;
		if (ve != null && (ve.vvv_flags & VVV.IS_LOCKED) == 0)
			return ve;
		openEdt2(self);
		return self.v_editor;
	}
	private static synchronized void openEdt2(ASTNode self) {
		ASTNode.VVV ve = self.v_editor;
		if (ve != null) {
			ve.vvv_flags |= VVV.FOR_EDITOR;
			ASTNode.VVV v = (ASTNode.VVV)ve.clone();
			ve.vvv_flags &= ~VVV.IS_LOCKED;
			v.vvv_prev = ve;
			v.vvv_next = ve.vvv_next;
			ve.vvv_next = v;
			if (v.vvv_next != null)
				v.vvv_next.vvv_prev = v;
			Transaction tr = Transaction.get();
			if (tr != null)
				v.transaction_id = tr.version;
			self.v_editor = ve = v;
		} else {
			ve = self.nodeBackup();
			ve.vvv_flags = VVV.FOR_EDITOR;
			self.v_editor = ve;
		}
		Transaction tr = Transaction.get();
		if (tr != null) {
			ve.transaction_id = tr.version;
			tr.add(self);
		}
	}

	public final void rollback(Transaction tr, boolean save_next) {
		if (this.v_editor == null)
			return;
		if (Thread.currentThread() == CompilerThread) {
			assert (this.transaction_id == tr.version);
			// scan back from the end of the list, find the latest saved
			VVV v = this.v_editor;
			while (v.vvv_next != null)
				v = v.vvv_next;
			while (v != null && (v.vvv_flags & VVV.FOR_COMPILER) == 0)
				v = v.vvv_prev;
			assert (v != null && (v.compileflags & 3) == 3); // backup is locked & versioned
			this.versioned = false;
			this.nodeRestore(v);
			this.compileflags |= 3; // locked & versioned
			v.vvv_flags &= ~VVV.FOR_COMPILER;
			if ((v.vvv_flags & VVV.FOR_EDITOR) == 0) {
				// delete unused VVV
				if (v.vvv_prev != null)
					v.vvv_prev.vvv_next = v.vvv_next;
				if (v.vvv_next != null)
					v.vvv_next.vvv_prev = v.vvv_prev;
				// v_editor may point v, iff it's the last backup
				if (this.v_editor == v) {
					assert (v.vvv_prev == null && v.vvv_next == null);
					this.v_editor = null;
				}
			}
		} else {
			// scan back from the current and find previous saved
			VVV v = this.v_editor;
			while (v != null) {
				if ((v.vvv_flags & VVV.FOR_EDITOR) != 0 && v.transaction_id == tr.version)
					break;
				if ((v.vvv_flags & VVV.FOR_EDITOR) != 0)
					System.out.println("Error: rolling back transaction "+tr.version+", but found backup of transaction "+v.transaction_id);
				v = v.vvv_prev;
			}
			assert (v != null);
			// delete unused VVV
			if (v.vvv_prev != null)
				v.vvv_prev.vvv_next = v.vvv_next;
			if (v.vvv_next != null)
				v.vvv_next.vvv_prev = v.vvv_prev;
			if (this.v_editor == v) {
				if (v.vvv_prev != null)
					this.v_editor = v.vvv_prev;
				else
					this.v_editor = v.vvv_next;
			}
		}
	}

	public void mergeTree() {
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) n.merge(); return true; }
		});
	}

	final void merge() {
		VVV v = this.v_editor;
		if (v == null)
			return;
		assert (this.versioned);
		this.versioned = false;
		this.nodeRestore(v);
		this.compileflags |= 3; // locked & versioned
		// remove all compiler's nodes
		while (v.vvv_next != null)
			v = v.vvv_next;
		while (v != null) {
			VVV prev = v.vvv_prev;
			v.vvv_flags &= ~VVV.FOR_COMPILER;
			if ((v.vvv_flags & VVV.FOR_EDITOR) == 0) {
				// delete unused VVV
				if (v.vvv_prev != null)
					v.vvv_prev.vvv_next = v.vvv_next;
				if (v.vvv_next != null)
					v.vvv_next.vvv_prev = v.vvv_prev;
				// v_editor may point v, iff it's the last backup
				if (this.v_editor == v) {
					assert (v.vvv_prev == null && v.vvv_next == null);
					this.v_editor = null;
				}
			}
			v = prev;
		}
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
