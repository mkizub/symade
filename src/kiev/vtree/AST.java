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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface INode {
	public ANode parent();
	public AttrSlot pslot();
	public AttrSlot[] values();
}

public enum ChildChangeType {
	UNKNOWN,
	ATTACHED,
	DETACHED,
	MODIFIED
}

public final class DataAttachInfo {
	public final   AttrSlot		p_slot;
	public final   Object		p_data;
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

public final class ExtChildrenIterator implements Enumeration<ANode> {
	public  final ANode    parent;
	public  final AttrSlot attr;
	private final Object[] ext_data;
	private       int      next_pos;
	
	ExtChildrenIterator(ANode parent, AttrSlot attr) {
		this.parent = parent;
		this.attr = attr;
		this.ext_data = parent.ext_data;
		this.next_pos = -1;
		if (ext_data != null)
			setNextPos();
	}
	public boolean hasMoreElements() {
		if (ext_data == null)
			return false;
		return next_pos < ext_data.length;
	}
	public ANode nextElement() {
		ANode n = (ANode)ext_data[next_pos];
		setNextPos();
		return n;
	}
	private void setNextPos() {
		for (next_pos++; next_pos < ext_data.length; next_pos++) {
			Object dat = ext_data[next_pos];
			if (dat instanceof ANode && (attr == null || dat.pslot() == attr))
				return;
		}
	}
}

public abstract class ANode implements INode {

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

	private static final AttrSlot[] $values = {}; // {/*ANode.nodeattr$this,*/ ANode.nodeattr$parent};

	
	public:ro @virtual @abstract ANode				ctx_root;
	public:ro @virtual @abstract FileUnit			ctx_file_unit;
	public:ro @virtual @abstract NameSpace			ctx_name_space;
	public:ro @virtual @abstract ComplexTypeDecl	ctx_tdecl;
	public:ro @virtual @abstract Method				ctx_method;

	@access:no,no,ro,rw AttrSlot				p_slot;
	@access:no,no,ro,rw ANode					p_parent;
	@access:no,no,ro,rw Object[]				ext_data;

	@abstract
	public:ro ANode			parent;

	@getter public final ANode get$parent() { return parent(); }

	public Language getCompilerLang() { return CoreLang; }
	public String getCompilerNodeName() { return "Node"; }
	
	public static class VVV implements Cloneable {
		public static final int IS_LOCKED    = 1;
		public static final int FOR_COMPILER = 2;
		public static final int FOR_EDITOR   = 4;
		int						vvv_flags;
		ASTNode.VVV				vvv_prev;
		ASTNode.VVV				vvv_next;
		AttrSlot				p_slot;
		ANode					p_parent;
		Object[]				ext_data;
		
		public VVV(ANode node) {
			this.p_slot = node.p_slot;
			this.p_parent = node.p_parent;
			this.ext_data = node.ext_data;
		}
		public Object clone() { super.clone() }
	}
	
	public static final class CopyContext {
		public static final class SymbolInfo {
			Symbol sold; // old symbol
			Symbol snew; // new symbol, copied from sold
			List<ASTNode> srefs; // new symbol ref, to be changed to point from sold to snew
			SymbolInfo(Symbol sold, Symbol snew) {
				this.sold = sold;
				this.snew = snew;
				this.srefs = List.newList<ASTNode>();
			}
			SymbolInfo(Symbol sold, ASTNode sref) {
				this.sold = sold;
				this.snew = null;
				this.srefs = List.newList(sref);
			}
		};
		private Vector<SymbolInfo> infos;
		void addSymbol(Symbol sold, Symbol snew) {
			if (infos == null)
				infos = new Vector<SymbolInfo>();
			foreach (SymbolInfo si; infos; si.sold == sold) {
				assert(si.snew == null);
				si.snew = snew;
				return;
			}
			infos.append(new SymbolInfo(sold, snew));
		}
		void addSymbolRef(ASTNode sref, Symbol sold) {
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
							sr.symbol = si.snew.symbol;
					}
					else if (n instanceof TypeArgRef) {
						TypeArgRef en = (TypeArgRef)n;
						if (en.symbol == si.sold)
							en.type_lnk = ((TypeDef)si.snew.dnode).getAType();
					}
					else if (n instanceof ENode) {
						ENode en = (ENode)n;
						if (en.symbol == si.sold)
							en.symbol = si.snew.symbol;
					}
				}
			}
		}
	}
	
	@getter @nodeData final AttrSlot get$p_slot() {
		if (ASTNode.EXECUTE_UNVERSIONED || Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			return this.p_slot;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.p_slot;
		return this.p_slot;
	}
	
	@setter @nodeData final void set$p_slot(AttrSlot value) {
		if (ASTNode.EXECUTE_UNVERSIONED || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.p_slot = value;
		else if (Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			ASTNode.openCmp((ASTNode)this).p_slot = value;
		else
			ASTNode.openEdt((ASTNode)this).p_slot = value;
	}
	
	@getter @nodeData final ANode get$p_parent() {
		if (ASTNode.EXECUTE_UNVERSIONED || Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			return this.p_parent;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.p_parent;
		return this.p_parent;
	}
	
	@setter @nodeData final void set$p_parent(ANode value) {
		if (ASTNode.EXECUTE_UNVERSIONED || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.p_parent = value;
		else if (Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			ASTNode.openCmp((ASTNode)this).p_parent = value;
		else
			ASTNode.openEdt((ASTNode)this).p_parent = value;
	}
	
	@getter @nodeData final Object[] get$ext_data() {
		if (ASTNode.EXECUTE_UNVERSIONED || Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			return this.ext_data;
		if (this instanceof ASTNode && ((ASTNode)this).v_editor != null)
			return ((ASTNode)this).v_editor.ext_data;
		return this.ext_data;
	}
	
	@setter @nodeData final void set$ext_data(Object[] value) {
		if (ASTNode.EXECUTE_UNVERSIONED || !(this instanceof ASTNode) || !((ASTNode)this).versioned)
			this.ext_data = value;
		else if (Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
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
			Object[] ext_data = this.ext_data;
			if (ext_data != null) {
				int sz = ext_data.length;
				for (int i=0; i < sz; i++) {
					Object dat = ext_data[i];
					if (dat instanceof ParentInfo) {
						ParentInfo pi = (ParentInfo)dat;
						if (pi.p_parent == parent && pi.p_slot == slot)
							return;
						assert (!pi.p_slot.parent_attr_slot.is_unique || pi.p_slot.parent_attr_slot != slot.parent_attr_slot);
					}
				}
				Object[] tmp = new Object[sz+1];
				for (int i=0; i < sz; i++)
					tmp[i] = ext_data[i];
				ParentInfo pi = new ParentInfo(parent,slot);
				tmp[sz] = pi;
				this.ext_data = tmp;
				this.callbackAttached(pi);
			} else {
				ParentInfo pi = new ParentInfo(parent,slot);
				this.ext_data = new Object[]{pi};
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
			Object[] ext_data = this.ext_data;
			if (ext_data == null)
				return;
			int sz = ext_data.length-1;
			int idx = 0;
			for (; idx <= sz; idx++) {
				Object dat = ext_data[idx];
				if (dat instanceof ParentInfo) {
					ParentInfo pi = (ParentInfo)dat;
					if (pi.p_parent == parent && pi.p_slot == slot)
						break;
				}
			}
			ParentInfo pi = (ParentInfo)ext_data[idx];
			if (sz == 0) {
				this.ext_data = null;
			} else {
				Object[] tmp = new Object[sz];
				int i;
				for (i=0; i < idx; i++) tmp[i] = ext_data[i];
				for (   ; i <  sz; i++) tmp[i] = ext_data[i+1];
				this.ext_data = tmp;
			}
			pi.p_parent.callbackChildChanged(ChildChangeType.DETACHED,slot,this);
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
		if (slot instanceof SpaceAttrSlot) {
			ANode[] arr = slot.getArray(node.parent());
			for (int i=arr.length-1; i >= 0; i--) {
				ANode n = arr[i];
				if (n == node) {
					if (i == 0)
						return null;
					return arr[i-1];
				}
			}
		}
		return null;
	}
	public static ANode getNextNode(ANode node) {
		AttrSlot slot = node.p_slot;
		if (slot instanceof SpaceAttrSlot) {
			ANode[] arr = slot.getArray(node.parent());
			for (int i=arr.length-1; i >= 0; i--) {
				ANode n = arr[i];
				if (n == node) {
					if (i >= arr.length-1)
						return null;
					return arr[i+1];
				}
			}
		}
		return null;
	}
	
	@getter public final FileUnit get$ctx_file_unit() {
		ANode self = this;
		if (self instanceof FileUnit)
			return (FileUnit)self;
		for (;;) {
			ANode p = nodeattr$syntax_parent.get(self);
			if (p != null) {
				if (p instanceof FileUnit)
					return (FileUnit)p;
				self = p;
				continue;
			}
			p = self.parent();
			if (p != null) {
				if (p instanceof FileUnit)
					return (FileUnit)p;
				self = p;
				continue;
			}
			return null;
		}
	}
	@getter public NameSpace get$ctx_name_space() {
		ANode self = this;
		if (self instanceof NameSpace)
			return (NameSpace)self;
		for (;;) {
			ANode p = nodeattr$syntax_parent.get(self);
			if (p != null) {
				if (p instanceof NameSpace)
					return (NameSpace)p;
				self = p;
				continue;
			}
			p = self.parent();
			if (p != null) {
				if (p instanceof NameSpace)
					return (NameSpace)p;
				self = p;
				continue;
			}
			return null;
		}
	}
	@getter public ComplexTypeDecl get$ctx_tdecl() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get_child_ctx_tdecl();
	}
	public ComplexTypeDecl get_child_ctx_tdecl() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get_child_ctx_tdecl();
	}
	@getter public Method get$ctx_method() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get_child_ctx_method();
	}
	public Method get_child_ctx_method() {
		ANode p = this.parent();
		if (p == null)
			return null;
		return p.get_child_ctx_method();
	}

	public AttrSlot[] values() {
		return ANode.$values;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (val instanceof SymbolRef && val.name == null)
			return false;
		if (attr.is_attr)
			return true;
		if (attr.name == "this") {
			AttrSlot slot = pslot();
			return slot == null || slot.is_attr;
		}
		return false;
	}

	public Object getVal(String name) {
		if (name == "this")
			return this;
		if (name == "parent")
			return this.parent();
		foreach (AttrSlot a; this.values(); a.name == name) {
			if (a instanceof ScalarAttrSlot)
				return a.get(this);
			else if (a instanceof SpaceAttrSlot)
				return a.getArray(this);
			else if (a instanceof ExtSpaceAttrSlot)
				return a.iterate(this);
		}
		if (ext_data != null) {
			foreach (Object dat; ext_data) {
				if (dat instanceof DataAttachInfo && dat.p_slot.name == name)
					return dat.p_data;
				else if (dat instanceof ANode && dat.pslot().name == name)
					return dat;
			}
		}
		throw new RuntimeException("No @nodeAttr value \"" + name + "\" in "+getClass().getName());
	}

	public final ExtChildrenIterator getExtChildIterator(AttrSlot attr) {
		return new ExtChildrenIterator(this, attr);
	}
	
	public final Object getExtData(AttrSlot attr) {
		if (ext_data != null) {
			foreach (Object dat; ext_data) {
				if (dat instanceof DataAttachInfo && dat.p_slot == attr)
					return dat.p_data;
				else if (dat instanceof ANode && dat.pslot() == attr)
					return dat;
			}
		}
		return null;
	}
	
	public final ANode getExtParent(ParentAttrSlot attr) {
		assert (attr.is_unique);
		if (ext_data != null) {
			foreach (ParentInfo pi; ext_data; pi.p_slot.parent_attr_slot == attr)
				return pi.p_parent;
		}
		return null;
	}
	
	public final void setExtData(Object d, AttrSlot attr) {
		assert (!(attr instanceof ExtSpaceAttrSlot));
		if (d == null) {
			delExtData(attr);
			return;
		}

		Object[] ext_data = this.ext_data;
		if (attr.is_child && attr instanceof ScalarAttrSlot) {
			ANode n = (ANode)d;
			assert(!n.isAttached());
			if (ext_data == null) {
				this.ext_data = new Object[]{n};
				n.callbackAttached(this, attr);
				return;
			}
			int sz = ext_data.length;
			for (int i=0; i < sz; i++) {
				Object dat = ext_data[i];
				if !(dat instanceof ANode)
					continue;
				if (dat.p_slot == attr) {
					if (dat == n)
						return;
					ext_data = (Object[])ext_data.clone();
					ext_data[i] = n;
					this.ext_data = ext_data;
					n.callbackAttached(this, attr);
					return;
				}
			}
			Object[] tmp = new Object[sz+1];
			for (int i=0; i < sz; i++)
				tmp[i] = ext_data[i];
			tmp[sz] = n;
			this.ext_data = tmp;
			n.callbackAttached(this, attr);
			return;
		}
		
		if (attr instanceof SpaceAttrSlot && ((ANode[])d).length == 0) {
			delExtData(attr);
			return;
		}
		
		if (ext_data == null) {
			this.ext_data = new Object[]{new DataAttachInfo(d,attr)};
			return;
		}
		int sz = ext_data.length;
		for (int i=0; i < sz; i++) {
			Object dat = ext_data[i];
			if !(dat instanceof DataAttachInfo)
				continue;
			DataAttachInfo ai = (DataAttachInfo)dat;
			if (ai.p_slot == attr) {
				if (ai.p_data == d)
					return;
				ext_data = (Object[])ext_data.clone();
				ext_data[i] = new DataAttachInfo(d,attr);
				this.ext_data = ext_data;
				return;
			}
		}
		Object[] tmp = new Object[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = ext_data[i];
		tmp[sz] = new DataAttachInfo(d,attr);
		this.ext_data = tmp;
		return;
	}

	public final void addExtData(ANode n, AttrSlot attr) {
		assert (!n.isAttached());
		assert (attr.is_child && attr instanceof ExtSpaceAttrSlot);
		Object[] ext_data = this.ext_data;
		if (ext_data == null) {
			this.ext_data = new Object[]{n};
			n.callbackAttached(this, attr);
			return;
		}
		int sz = ext_data.length;
		Object[] tmp = new Object[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = ext_data[i];
		tmp[sz] = n;
		this.ext_data = tmp;
		n.callbackAttached(this, attr);
		return;
	}

	public final void replaceExtData(ANode nold, ANode nnew, AttrSlot attr) {
		assert (nold.isAttached());
		assert (!nnew.isAttached());
		assert (attr.is_child && attr instanceof ExtSpaceAttrSlot);
		assert (ext_data != null);
		Object[] ext_data = this.ext_data;
		int sz = ext_data.length;
		for (int i=0; i < sz; i++) {
			if (ext_data[i] == nold) {
				this.ext_data = (Object[])ext_data.clone();
				this.ext_data[i] = nnew;
				nold.callbackDetached(this, attr);
				nnew.callbackAttached(this, attr);
				return;
			}
		}
		assert ("Node is not ext_data child");
		return;
	}

	public final void delExtData(AttrSlot attr) {
		Object[] ext_data = this.ext_data;
		if (ext_data == null)
			return;
		int sz = ext_data.length-1;
		if (attr.is_child && !(attr instanceof SpaceAttrSlot)) {
			for (int idx=0; idx <= sz; idx++) {
				Object dat = ext_data[idx];
				if (dat instanceof ANode) {
					ANode n = (ANode)dat;
					if (n.p_slot == attr) {
						if (sz == 0) {
							this.ext_data = null;
						} else {
							Object[] tmp = new Object[sz];
							int i;
							for (i=0; i < idx; i++) tmp[i] = ext_data[i];
							for (   ; i <  sz; i++) tmp[i] = ext_data[i+1];
							this.ext_data = tmp;
						}
						n.callbackDetached(this, attr);
						return;
					}
				}
			}
		}
		for (int idx=0; idx <= sz; idx++) {
			Object dat = ext_data[idx];
			if (dat instanceof DataAttachInfo) {
				DataAttachInfo ai = (DataAttachInfo)dat;
				if (ai.p_slot == attr) {
					if (sz == 0) {
						this.ext_data = null;
					} else {
						Object[] tmp = new Object[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = ext_data[i];
						for (   ; i <  sz; i++) tmp[i] = ext_data[i+1];
						this.ext_data = tmp;
					}
					return;
				}
			}
		}
	}

	public final void delExtData(ANode n) {
		assert (n.isAttached());
		assert (n.parent() == this);
		assert (n.pslot().is_child && !(n.pslot() instanceof SpaceAttrSlot));
		Object[] ext_data = this.ext_data;
		assert(ext_data != null);
		int sz = ext_data.length-1;
		for (int idx=0; idx <= sz; idx++) {
			if (n == ext_data[idx]) {
				if (sz == 0) {
					this.ext_data = null;
				} else {
					Object[] tmp = new Object[sz];
					int i;
					for (i=0; i < idx; i++) tmp[i] = ext_data[i];
					for (   ; i <  sz; i++) tmp[i] = ext_data[i+1];
					this.ext_data = tmp;
				}
				n.callbackDetached(this, n.pslot());
				return;
			}
		}
		assert ("Child node not found in ext_data");
	}

	public final void delExtParent(ParentAttrSlot attr) {
		assert (attr.is_unique);
		Object[] ext_data = this.ext_data;
		if (ext_data == null)
			return;
		int sz = ext_data.length-1;
		for (int idx=0; idx <= sz; idx++) {
			Object dat = ext_data[idx];
			if (dat instanceof ParentInfo) {
				ParentInfo pi = (ParentInfo)dat;
				if (pi.p_slot.parent_attr_slot == attr) {
					if (sz == 0) {
						this.ext_data = null;
					} else {
						Object[] tmp = new Object[sz];
						int i;
						for (i=0; i < idx; i++) tmp[i] = ext_data[i];
						for (   ; i <  sz; i++) tmp[i] = ext_data[i+1];
						this.ext_data = tmp;
					}
					pi.p_parent.callbackDetached(this, pi.p_slot);
					return;
				}
			}
		}
	}

	public final void walkTree(TreeWalker walker) {
//#ifndef OLD_WALKER
//#		this.walkTreeFast(walker);
//#else OLD_WALKER
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.is_child) {
				if (attr instanceof SpaceAttrSlot)
					walker.visitANodeSpace(attr.getArray(this));
				else if (attr instanceof ScalarAttrSlot)
					walker.visitANode((ANode)attr.get(this));
			}
			Object[] ext_data = this.ext_data;
			if (ext_data != null) {
				foreach (ANode n; ext_data; n.p_slot.is_external || n.p_slot instanceof ExtSpaceAttrSlot)
					walker.visitANode(n);
			}
		}
		walker.post_exec(this);
//#endif OLD_WALKER
	}

	public final void walkTreeFast(TreeWalker walker) {
		if (walker.pre_exec(this))
			this.walkTreeFastVisit(walker);
		walker.post_exec(this);
	}
	protected void walkTreeFastVisit(TreeWalker $walker) {
		Object[] ext_data = this.ext_data;
		if (ext_data == null)
			return;
		foreach (Object dat; ext_data) {
			if (dat instanceof ANode)
				$walker.visitANode((ANode)dat);
			else if (dat instanceof DataAttachInfo && dat.p_slot.is_attr && (dat.p_slot instanceof SpaceAttrSlot))
				$walker.visitANodeSpace((ANode[])dat.p_data);
		}
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
	

	public final this.type ncopy() {
		CopyContext cc = new CopyContext();
		ANode t = (ANode)this.copy(cc);
		cc.updateLinks();
		return t;
	}
	public final this.type ncopy(CopyContext cc) {
		return (ANode)this.copy(cc);
	}

	public Object copy(CopyContext cc) {
		if (this instanceof TypeInfoInterface)
			return this.copyTo(((TypeInfoInterface)this).getTypeInfoField().newInstance(), cc);
		else
			return this.copyTo(this.getClass().newInstance(), cc);
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ANode node = (ANode)to$node;
		Object[] this_ext_data = this.ext_data;
		if (this_ext_data != null) {
			int sz = this_ext_data.length;
			node.ext_data = new Object[sz];
			int j=0;
			for (int i=0; i < sz; i++) {
				Object dat = this_ext_data[i];
				if (dat instanceof ANode) {
					ANode n = (ANode)dat;
					ANode nd = n.ncopy(in$context);
					node.ext_data[j++] = nd;
					nd.callbackAttached(node, n.p_slot);
				}
				else if (dat instanceof DataAttachInfo) {
					DataAttachInfo ai = (DataAttachInfo)dat;
					if (ai.p_slot.is_attr && (ai.p_slot instanceof SpaceAttrSlot)) {
						ANode[] sarr = (ANode[])ai.p_data;
						ANode[] narr = (ANode[])sarr.clone();
						for (int x=0; x < narr.length; x++) {
							ANode n = sarr[x].ncopy(in$context);
							narr[x] = n;
							n.callbackAttached(node, ai.p_slot);
						}
						node.ext_data[j++] = new DataAttachInfo(narr,ai.p_slot);
					} else {
						node.ext_data[j++] = ai;
					}
				}
			}
			if (j < sz) {
				if (j == 0)
					node.ext_data = null;
				else
					node.ext_data = (Object[])Arrays.cloneToSize(node.ext_data,j);
			}
		}
		if (this instanceof Symbol)
			in$context.addSymbol((Symbol)this,(Symbol)node);
		else if (this instanceof SymbolRef)
			in$context.addSymbolRef((SymbolRef)node, ((SymbolRef)this).symbol);
		else if (this instanceof ENode)
			in$context.addSymbolRef((ENode)node, ((ENode)this).symbol);
		return node;
	}
	
	public final this.type detach()
		alias fy operator ~
	{
		if (isAttached()) {
			this.pslot().detach(this.parent(), this);
			assert(!isAttached());
		}
		Object[] ext_data = this.ext_data;
		if (ext_data != null) {
			foreach (ParentInfo pi; ext_data)
				pi.p_slot.detach(pi.p_parent, this);
		}
		return this;
	}
	
	public final ScalarPtr getScalarPtr(String name) {
		foreach (ScalarAttrSlot attr; this.values(); attr.name == name)
			return new ScalarPtr(this, attr);
		throw new RuntimeException("No @nodeAttr/@nodeData attribute '"+name+"' in "+getClass());
	}
	
	public final SpacePtr getSpacePtr(String name) {
		foreach (SpaceAttrSlot attr; this.values(); attr.name == name)
			return new SpacePtr(this, attr);
		throw new RuntimeException("No @nodeAttr/@nodeData space '"+name+"' in "+getClass());
	}

	public final <N extends ANode> N replaceWithNode(N node) {
		assert(isAttached());
		if (node == null) {
			this.detach();
			return null;
		}
		ANode parent = parent();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			pslot.set(parent, idx, node);
		}
		else if (pslot instanceof ExtSpaceAttrSlot) {
			parent.replaceExtData(this,node,pslot);
		}
		else if (pslot instanceof ScalarAttrSlot) {
			assert(pslot.get(parent) == this);
			pslot.set(parent, node);
		}
		assert(node == null || node.isAttached());
		if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
			((ASTNode)node).pos = ((ASTNode)this).pos;
		return node;
	}
	
	public ANode doRewrite(RewriteContext ctx) {
		if !(this instanceof ASTNode)
			return this.ncopy();
		ANode rn = (ANode)getClass().newInstance();
		foreach (AttrSlot attr; this.values(); attr.is_attr && !attr.is_not_copyable) {
			if (attr instanceof SpaceAttrSlot) {
				ANode[] vals = attr.getArray(this);
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
			else if (attr instanceof ExtSpaceAttrSlot) {
				foreach (ANode n; attr.iterate(this)) {
					Object obj = n.doRewrite(ctx);
					if (obj instanceof BlockRewr) {
						foreach (ASTNode st; obj.stats) {
							n = (ANode)ctx.fixup(attr,st);
							if (n != null)
								attr.add(rn,n);
						}
					} else {
						n = (ANode)ctx.fixup(attr,obj);
						if (n != null)
							attr.add(rn,n);
					}
				}
			}
			else if (attr instanceof ScalarAttrSlot) {
				Object val = attr.get(this);
				if (val == null)
					continue;
				else if (val instanceof ANode) {
					ANode rw = val.doRewrite(ctx);
					while (rw instanceof BlockRewr && rw.stats.length == 1)
						rw = rw.stats[0];
					attr.set(rn,ctx.fixup(attr,rw));
				}
				else
					attr.set(rn,ctx.fixup(attr,val));
			}
		}
		if (this.ext_data != null) {
			foreach (ANode n; this.ext_data) {
				AttrSlot attr = n.p_slot;
				if (attr instanceof ScalarAttrSlot && attr.is_external)
					this.setExtData(ctx.fixup(attr,n.doRewrite(ctx)),attr);
			}
		}
		return rn;
	}
	
}

public class TreeWalker {
	public boolean pre_exec(ANode n) { return true; }
	public void post_exec(ANode n) {}

	public final void visitANodeSpace(ANode[] vals) {
		for (int i=0; i < vals.length; i++)
			this.visitANode(vals[i]);
	}

	public final void visitANode(ANode val) {
		while (val != null) {
			try {
				val.walkTree(this);
				return;
			} catch (ReWalkNodeException e) {
				val = e.replacer;
			}
		}
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
public abstract class ASTNode extends ANode implements Constants {

	public static final boolean EXECUTE_UNVERSIONED = Boolean.valueOf(System.getProperty("symade.unversioned","true")).booleanValue();
	
	public static final ASTNode[] emptyArray = new ASTNode[0];

	private static final AttrSlot[] $values = {}; // {/*ANode.nodeattr$this,*/ ANode.nodeattr$parent};

//#ifndef UNVERSIONED
	@UnVersioned
	public VVV						v_editor;
//#else UNVERSIONED
//#	@UnVersioned @abstract
//#	public:ro VVV						v_editor;
//#	@getter public VVV get$v_editor() { return null;}
//#endif UNVERSIONED

	@UnVersioned
	public int						pos;
	@UnVersioned
	public int						compileflags;	// temporal flags for compilation process
	
	public int						nodeflags;		// presistent flags of the node
	
	// Uncomment to compile with symade-04g.jar
	//@nodeData @abstract
	//public:ro ANode					parent;
	
	public Language getCompilerLang() { return CoreLang; }
	public String getCompilerNodeName() { return "ASTNode"; }
	
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
		if (ASTNode.EXECUTE_UNVERSIONED || !this.versioned)
			this.nodeflags = value;
		else if (Thread.currentThread().getThreadGroup() == CompilerThreadGroup)
			ASTNode.openCmp(this).nodeflags = value;
		else
			ASTNode.openEdt(this).nodeflags = value;
	}
	
	@getter public final int get$nodeflags() {
		if (ASTNode.EXECUTE_UNVERSIONED || Thread.currentThread().getThreadGroup() == CompilerThreadGroup || this.v_editor == null)
			return this.nodeflags;
		return this.v_editor.nodeflags;
	}
	
	@setter public final void set$pos(int value) { this.pos = value; }
	@getter public final int get$pos() { return this.pos; }
	

	public @packed:3,nodeflags, 0 int     mflags_access;

	public @packed:1,nodeflags, 3 boolean mflags_is_static;
	public @packed:1,nodeflags, 4 boolean mflags_is_final;
	public @packed:1,nodeflags, 5 boolean mflags_is_mth_synchronized;	// method
	public @packed:1,nodeflags, 5 boolean mflags_is_struct_super;		// struct
	public @packed:1,nodeflags, 6 boolean mflags_is_fld_volatile;		// field
	public @packed:1,nodeflags, 6 boolean mflags_is_mth_bridge;			// method
	public @packed:1,nodeflags, 7 boolean mflags_is_fld_transient;		// field
	public @packed:1,nodeflags, 7 boolean mflags_is_mth_varargs;			// method
	public @packed:1,nodeflags, 8 boolean mflags_is_native;				// native method, backend operation/field/struct
	public @packed:1,nodeflags, 9 boolean mflags_is_struct_interface;
	public @packed:1,nodeflags,10 boolean mflags_is_abstract;
	public @packed:1,nodeflags,11 boolean mflags_is_math_strict;			// strict math
	public @packed:1,nodeflags,12 boolean mflags_is_synthetic;			// any decl that was generated (not in sources)
	public @packed:1,nodeflags,13 boolean mflags_is_struct_annotation;
	public @packed:1,nodeflags,14 boolean mflags_is_enum;				// struct/decl group/fields
		
	// Flags temporary used with java flags
	public @packed:1,nodeflags,16 boolean mflags_is_forward;				// var/field/method, type is wrapper
	public @packed:1,nodeflags,17 boolean mflags_is_virtual;				// var/field, method is 'static virtual', struct is 'view'
	public @packed:1,nodeflags,18 boolean mflags_is_type_unerasable;		// typedecl, method/struct as parent of typedef
	public @packed:1,nodeflags,19 boolean mflags_is_macro;				// macro-declarations for fields, methods, etc

	// General flags
	public @packed:1,nodeflags,21 boolean is_rewrite_target;
	public @packed:1,nodeflags,22 boolean is_auto_generated;
	public @packed:1,nodeflags,23 boolean is_interface_only;		// only node's interface was scanned/loded; no implementation

	// Structures
	public @packed:1,nodeflags,24 boolean is_struct_fe_passed;
	public @packed:1,nodeflags,25 boolean is_struct_has_pizza_cases;
	public @packed:1,nodeflags,26 boolean is_tdecl_not_loaded;	// TypeDecl was fully loaded (from src or bytecode) 
	// Method flags
	public @packed:1,nodeflags,24 boolean is_mth_virtual_static;
	public @packed:1,nodeflags,25 boolean is_mth_operator;
	public @packed:1,nodeflags,26 boolean is_mth_invariant;
	// SymbolRef/ENode/ISymRef
	public @packed:1,nodeflags,24  boolean is_qualified; // qualified or simple name, names are separated by ASCII US (Unit Separator, 037, 0x1F)
	// Expression/statement flags
	public @packed:1,nodeflags,25 boolean is_expr_as_field;
	public @packed:1,nodeflags,26 boolean is_expr_primary;
	public @packed:1,nodeflags,27 boolean is_expr_super;
	// EToken
	public @packed:1,nodeflags,27 boolean is_explicit; // explicit or implicit kind of EToken
	// Var/Field
	public @packed:7,nodeflags,24 int     mflags_var_kind;				// var/field kind
	


	// Structures
	public @packed:1,compileflags,8  boolean is_struct_type_resolved; // KievFE_Pass2
	public @packed:1,compileflags,9  boolean is_struct_args_resolved; // KievFE_Pass2
	public @packed:1,compileflags,10 boolean is_struct_members_generated; // KievFE_Pass2
	public @packed:1,compileflags,11 boolean is_struct_pre_generated; // KievME_PreGenartion

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

	// Statement flags
	public @packed:1,compileflags,11 boolean is_stat_abrupted;
	public @packed:1,compileflags,12 boolean is_stat_breaked;
	public @packed:1,compileflags,13 boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public @packed:1,compileflags,14 boolean is_stat_auto_returnable;
	public @packed:1,compileflags,15 boolean is_direct_flow_reachable; // reachable by direct control flow (with no jumps)

	// Method flags
	public @packed:1,compileflags,8  boolean is_mth_need_fields_init;
	public @packed:1,compileflags,9  boolean is_mth_dispatcher;

	// Var/field
	public @packed:1,compileflags,8  boolean is_need_proxy;
	public @packed:1,compileflags,9  boolean is_init_wrapper;
	public @packed:1,compileflags,10 boolean is_fld_added_to_init;

	// General flags
	public @packed:1,compileflags,3 boolean is_resolved;
	public @packed:1,compileflags,2 boolean is_bad;
	public @packed:1,compileflags,1 boolean versioned;
	public @packed:1,compileflags,0 boolean locked;

	public AttrSlot[] values() {
		return ASTNode.$values;
	}

	public ASTNode.VVV nodeBackup() {
		return new ASTNode.VVV(this);
	}

	public void nodeRestore(ASTNode.VVV from) {
//#ifndef UNVERSIONED
		this.compileflags = from.compileflags & ~3;
		this.nodeflags = from.nodeflags;
		super.nodeRestore(from);
//#endif UNVERSIONED
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ASTNode node = (ASTNode)super.copyTo(to$node, in$context);
		node.pos			= this.pos;
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
		ANode parent = parent();
		AttrSlot pslot = pslot();
		if (pslot instanceof SpaceAttrSlot) {
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			pslot.insert(parent, idx, n);
			assert(n.isAttached());
			return n;
		}
		else if (pslot instanceof ExtSpaceAttrSlot) {
			throw new RuntimeException("replace external node");
		}
		else if (pslot instanceof ScalarAttrSlot) {
			assert(pslot.get(parent) == this);
			pslot.set(parent, null);
			ASTNode n = fnode();
			if (n != null && n.pos == 0) n.pos = this.pos;
			pslot.set(parent, n);
			assert(n == null || n.isAttached());
			return n;
		}
		throw new RuntimeException("replace unknown kind of AttrSlot");
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

	// break target (ENodes, redefined in loops, switch, blocks)
	public boolean isBreakTarget() {
		return false;
	}

	public Type getType() { return Type.tpVoid; }

	public ASTNode() {
//#ifndef UNVERSIONED
		Transaction tr = Transaction.get();
		if (tr != null)
			tr.add(this);
//#endif UNVERSIONED
	}

	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public boolean preResolveIn() {
		foreach (AttrSlot attr; values(); attr.is_auto_resolve && SymbolRef.class.isAssignableFrom(attr.clazz)) {
			if (attr instanceof ScalarAttrSlot) {
				Object val = attr.get(this);
				if (val instanceof SymbolRef)
					val.resolveSymbol(attr.auto_resolve_severity);
			}
			else if (attr instanceof SpaceAttrSlot) {
				foreach (SymbolRef sr; attr.getArray(this))
					sr.resolveSymbol(attr.auto_resolve_severity);
			}
			else if (attr instanceof ExtSpaceAttrSlot) {
				foreach (SymbolRef sr; attr.iterate(this))
					sr.resolveSymbol(attr.auto_resolve_severity);
			}
		}
		return true;
	}
	public void preResolveOut() {}
	public boolean mainResolveIn() { return true; }
	public void mainResolveOut() {}
	public boolean preVerify() { return true; }
	public void postVerify() {}

	public Symbol[] resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.is_auto_complete) {
			foreach (AttrSlot attr; values(); attr == slot) {
				if (attr instanceof ScalarAttrSlot) {
					Object val = attr.get(this);
					if (val instanceof SymbolRef)
						return val.autoCompleteSymbol(str);
				}
				TypeInfo ti = attr.typeinfo;
				if (SymbolRef.class == ti.clazz) {
					ti = ti.getTopArgs()[0];
					return SymbolRef.autoCompleteSymbol(this, str, slot, fun (DNode dn)->boolean {
						return ti.$instanceof(dn);
					});
				}
				return null;
			}
		}
		return null;
	}

	public static <A extends ASTNode> A openCmp(A self) {
//#ifndef UNVERSIONED
		if (!self.locked)
			return self;
		ASTNode.openCmp2(self);
//#endif UNVERSIONED
		return self;
	}
//#ifndef UNVERSIONED
	private static synchronized void openCmp2(ASTNode self) {
		ASTNode.VVV v = self.nodeBackup();
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
		tr.add(self);
	}
//#endif UNVERSIONED
	public static ASTNode.VVV openEdt(ASTNode self) {
//#ifndef UNVERSIONED
		ASTNode.VVV ve = self.v_editor;
		if (ve != null && (ve.vvv_flags & VVV.IS_LOCKED) == 0)
			return ve;
		openEdt2(self);
//#endif UNVERSIONED
		return self.v_editor;
	}
//#ifndef UNVERSIONED
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
//#end UNVERSIONED

	public final void rollback(Transaction tr, boolean save_next) {
//#ifndef UNVERSIONED
		if (this.v_editor == null)
			return;
		if (Thread.currentThread().getThreadGroup() == CompilerThreadGroup) {
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
//#endif UNVERSIONED
	}

	public void mergeTree() {
//#ifndef UNVERSIONED
		// notify nodes about new root
		this.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) { if (n instanceof ASTNode) n.merge(); return true; }
		});
//#endif UNVERSIONED
	}

//#ifndef UNVERSIONED
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
//#endif UNVERSIONED
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

public class ReWalkNodeException extends RuntimeException {
	public final ANode replacer;
	public ReWalkNodeException(ANode replacer) {
		this.replacer = replacer;
	}
}

public interface DumpSerialized {
	public String qname();
	public Object getDataToSerialize();
}

