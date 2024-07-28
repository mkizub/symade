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
 * @version $Revision: 296 $
 *
 */

public interface INode {
	public static final INode[] emptyArray = new INode[0];

	public AHandle		handle();
	public long			getUID();
	public ANode		asANode();

	public INode        parent();
	public AttrSlot     pslot();
	public AttrSlot[]   values();
	public NodeTypeInfo getNodeTypeInfo();

	public AttrSlot getAttrSlot(String name);
	
	public Object getVal(AttrSlot attr);
	public void   setVal(AttrSlot attr, Object data);
	public Object getVal(AttrSlot attr, int idx);
	public void   setVal(AttrSlot attr, int idx, Object data);
	public void   addVal(AttrSlot attr, Object data);
	public void   delVal(AttrSlot attr, int idx);
	public void   insVal(AttrSlot attr, int idx, Object data);

	public Object copy(CopyContext cc);
	public this.type detach();

	public boolean isAttached();
	public boolean isAttachedBy(AttrSlot attr_slot);
	
	public void walkTree(INode parent, AttrSlot slot, ITreeWalker walker);
}

public enum ChangeType {
	THIS_ATTACHED,		// this node was attached, tree_change=true
	THIS_DETACHED,		// this node was detached, tree_change=true
	ATTR_MODIFIED,		// a child node attached/detached or raw data was changed (to scalar attribute if idx < 0), content_change=true
	ATTR_UPDATED		// a child notification about it's essential state change
}

public final class NodeChangeInfo {
	public final ChangeType  ct;
	public final INode       parent;
	public final AttrSlot    slot;
	public final Object      old_value;
	public final Object      new_value;
	public final int         idx;
	public final boolean     tree_change;
	public final boolean     content_change;
	public NodeChangeInfo(ChangeType ct, INode parent, AttrSlot slot, Object old_value, Object new_value, int idx) {
		this.ct = ct;
		this.parent = parent;
		this.slot = slot;
		this.old_value = old_value;
		this.new_value = new_value;
		this.idx = idx;
		this.tree_change = (ct == ChangeType.THIS_ATTACHED || ct == ChangeType.THIS_DETACHED);
		this.content_change = !tree_change;
	}
}


public interface ChangeListener {
	public void callbackNodeChanged(NodeChangeInfo info);
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

public class AutoCompleteOption {
	public static interface Maker {
		public Object make(Object data);
	}
	public final String text;
	public final String descr;
	public final Maker  maker;
	public final Object data;
	public AutoCompleteOption(String text, String descr, Maker maker, Object data) {
		this.text = text;
		this.descr = descr;
		this.maker = maker;
		this.data = data;
	}
	public String toString() {
		if (descr == null)
			return text;
		return text + " (" + descr + ")";
	}
}
public final class AutoCompleteResult {
	
	public final boolean strict;
	private Vector<AutoCompleteOption> options;
	public AutoCompleteResult(boolean strict) {
		this.strict = strict;
		this.options = new Vector<AutoCompleteOption>();
	}
	public void append(String text, String descr, AutoCompleteOption.Maker maker, Object data) {
		this.options.append(new AutoCompleteOption(text,descr,maker,data));
	}
	public void append(Symbol sym) {
		this.options.append(new AutoCompleteOption(sym.sname,null,null,sym));
	}
	public AutoCompleteOption[] getOptions() {
		return this.options.toArray();
	}
	public boolean containsData(Object data) {
		foreach (AutoCompleteOption o; options; data.equals(o.data))
			return true;
		return false;
	}
}

public final class SpaceIterator<+N extends INode> implements Enumeration<N> {
	public  final INode            parent;
	public  final ASpaceAttrSlot   attr; 
	public  final N[]              array;
	private       int              next_pos;
	
	SpaceIterator(INode parent, ASpaceAttrSlot attr, N[] array) {
		this.parent = parent;
		this.attr = attr;
		this.array = array;
	}
	public boolean hasMoreElements() {
		if (array == null)
			return false;
		return next_pos < array.length;
	}
	public N nextElement() {
		INode n = (INode)array[next_pos];
		next_pos += 1;
		return n;
	}
	public boolean contains(INode val) {
		if (val == null || array == null)
			return false;
		foreach (INode n; array; n == val)
			return true;
		return false;
	}
}

public final class ExtSpaceIterator<+N extends INode> implements Enumeration<N> {
	public  final ANode            parent;
	public  final ExtSpaceAttrSlot attr;
	private final Object[]         ext_data;
	private       int              ext_pos;
	private       int              next_pos;
	private       boolean          scan_flags;
	
	ExtSpaceIterator(ANode parent, ExtSpaceAttrSlot attr, Object[] ext_data) {
		this.parent = parent;
		this.attr = attr;
		this.ext_data = ext_data;
		this.ext_pos = -1;
		this.next_pos = -1;
		setNextPos();
	}
	public boolean hasMoreElements() {
		if (!scan_flags) {
			if (ext_data == null)
				return false;
			return ext_pos < ext_data.length;
		} else {
			return (next_pos < 32);
		}
	}
	public AttrSlot nextAttrSlot() {
		if (!scan_flags)
			return ((DataAttachInfo)ext_data[ext_pos]).p_slot;
		else
			return DNode.nodeattr$metas;
	}
	public N nextElement() {
		if (!scan_flags) {
			Object obj = ((DataAttachInfo)ext_data[ext_pos]).p_data;
			INode n = null;
			if (obj instanceof INode[]) {
				INode[] arr = (INode[])obj;
				n = arr[next_pos];
				next_pos += 1;
				if (next_pos >= arr.length) {
					next_pos = -1;
					setNextPos();
				}
			} else {
				n = (INode)obj;
				setNextPos();
			}
			return n;
		} else {
			Class[] flags = ((DNode)parent).getMetaFlags();
			MetaFlag flag = (MetaFlag)flags[next_pos].newInstance();
			parent.callbackMetaSet(attr,flag);
			setNextPos();
			return flag;
		}
	}
	public N[] getArray() {
		if (scan_flags)
			return null;
		return (INode[])((DataAttachInfo)ext_data[ext_pos]).p_data;
	}
	
	private void setNextPos() {
		if (!scan_flags && ext_data != null) {
			for (ext_pos++; ext_pos < ext_data.length; ext_pos++) {
				Object dat = ext_data[ext_pos];
				if (dat instanceof DataAttachInfo && ((attr == null && dat.p_slot.isChild())|| dat.p_slot == attr)) {
					if (dat.p_data instanceof INode[])
						next_pos = 0;
					return;
				}
			}
		}
		if (!scan_flags && attr == DNode.nodeattr$metas) {
			scan_flags = true;
			next_pos = -1;
		}
		if (scan_flags) {
			int nodeflags = ((DNode)parent).nodeflags;
			Class[] flags = ((DNode)parent).getMetaFlags();
			for (next_pos++; next_pos < 32; next_pos++) {
				if (flags[next_pos] != null && (nodeflags & (1<<next_pos)) != 0)
					return;
			}
		}
	}
	public boolean contains(INode val) {
		if (val == null || ext_data == null)
			return false;
		foreach (DataAttachInfo dat; ext_data) {
			if (val == dat.p_data)
				return true;
			if (dat.p_data instanceof INode[]) {
				foreach (INode n; (INode[])dat.p_data; val == n)
					return true;
			}
		}
		return false;
	}
}

public abstract class ANode extends AHandleData implements INode {
	public static final ANode[] emptyArray = new ANode[0];
	public static final Object[] emptyExtData = new Object[0];
	
	public static abstract class UnVersionedData {}
	
	static final class ChangeListenerEntry extends UnVersionedData {
		final ChangeListener listener;
		ChangeListenerEntry(ChangeListener listener) {
			this.listener = listener;
		}
	}
	static final class TextSourcePosition extends UnVersionedData {
		int						pos;
		TextSourcePosition(int pos) { this.pos = pos; }
	}

	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	static final class NodeAttr_this extends ScalarAttrSlot {
		NodeAttr_this() { super("this", null, TypeInfo.newTypeInfo(ANode.class,null)); }
		public final void set(INode parent, Object value) { throw new RuntimeException("@nodeData 'this' is not writeable"); }
		public final Object get(INode parent) { return parent; }
	}
	public static final NodeAttr_this nodeattr$this = new NodeAttr_this();

	public static final ParentAttrSlot nodeattr$parent =
			new ParentAttrSlot("parent", true, TypeInfo.newTypeInfo(ANode.class,null));

	public static final ParentAttrSlot nodeattr$syntax_parent =
			new ParentAttrSlot("syntax_parent", true, TypeInfo.newTypeInfo(ANode.class,null));

	private static final AttrSlot[] $values = {};
	
	private static final NodeTypeInfo $node_type_info = new NodeTypeInfo("kiev·vtree·ANode", CoreLang, "Node", new NodeTypeInfo[0], ANode.$values);
	
	final
	public  AHandle					p_handle_;
	private ANode					p_parent_;
	private Object					p_ext_data_;
	
	@virtual @abstract
	public:ro ANode					parent;

	@UnVersioned
	@abstract @virtual
	public int						pos;
	
	public ANode(AHandle handle, Context context) {
		super(context);
		p_handle_ = handle;
		handle.addData(this);
	}

	final
	public AHandle handle() { return p_handle_; }

	final
	public long getUID() { return p_handle_.h_uid; }

	final
	public ANode asANode() { return this; }

	static
	public ANode[] asANodes(INode[] arr) {
		if (arr instanceof ANode[])
			return (ANode[])arr;
		ANode[] tmp = new ANode[arr.length];
		for (int i=0; i < arr.length; i++)
			tmp[i] = arr[i].asANode();
		return tmp;
	}

	public Language getCompilerLang() { return null; }
	public String getCompilerNodeName() { return null; }

	public NodeTypeInfo getNodeTypeInfo() {
		return ANode.$node_type_info;
	}
	
	@getter @nodeData final public ANode get$parent() {
		return parent();
	}

	synchronized Object[] getExtData() {
		Object p_ext_data = this.p_ext_data_;
		if (p_ext_data == null)
			return emptyExtData;
		if (p_ext_data instanceof Object[])
			return (Object[])p_ext_data;
		return new Object[]{p_ext_data};
	}
	
	private void delExtElem(Object[] arr, int idx, Object value) {
		assert (idx >= 0 && idx < arr.length);
		if (arr.length == 1) {
			assert (this.p_ext_data_ == value);
			this.p_ext_data_ = null;
			return;
		}
		assert (this.p_ext_data_ == arr);
		if (arr.length == 2) {
			if (idx == 0) {
				assert (arr[0] == value);
				this.p_ext_data_ = arr[1];
			} else {
				assert (arr[1] == value);
				this.p_ext_data_ = arr[0];
			}
			return;
		}
		assert (arr[idx] == value);
		int i = 0;
		Object[] tmp = new Object[arr.length - 1];
		for (; i < idx; i++)
			tmp[i] = arr[i];
		for (; i < tmp.length; i++)
			tmp[i] = arr[i+1];
		this.p_ext_data_ = tmp;
	}
	
	private void insExtElem(Object[] arr, int idx, Object value) {
		assert (idx >= 0 && idx <= arr.length);
		if (arr.length == 0) {
			this.p_ext_data_ = value;
			return;
		}
		int i = 0;
		Object[] tmp = new Object[arr.length + 1];
		for (; i < idx; i++)
			tmp[i] = arr[i];
		tmp[i] = value;
		for (; i < arr.length; i++)
			tmp[i+1] = arr[i];
		this.p_ext_data_ = tmp;
	}
	
	private void setExtElem(Object[] arr, int idx, Object value) {
		assert (arr.length > 0 && idx >= 0 && idx < arr.length);
		if (arr.length == 1) {
			this.p_ext_data_ = value;
			return;
		}
		Object[] tmp = (Object[])arr.clone();
		tmp[idx] = value;
		this.p_ext_data_ = tmp;
	}
	
	public final UnVersionedData getUnVersionedData(Class clazz) {
		foreach (UnVersionedData uvd; getExtData(); uvd.getClass() == clazz)
			return uvd;
		return null;
	}

	public final synchronized void delUnVersionedData(Class clazz) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object ext = arr[i];
			if (ext instanceof UnVersionedData && ext.getClass() == clazz) {
				delExtElem(arr,i,ext);
				return;
			}
		}
	}

	public final synchronized void setUnVersionedData(UnVersionedData value) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object ext = arr[i];
			if (ext instanceof UnVersionedData && ext.getClass() == value.getClass()) {
				if (ext != value)
					setExtElem(arr,i,value);
				return;
			}
		}
		insExtElem(arr,arr.length,value);
	}

	public final synchronized void delListener(ChangeListener listener) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object ext = arr[i];
			if (ext instanceof ChangeListenerEntry && ext.listener == listener) {
				delExtElem(arr,i,ext);
				return;
			}
		}
	}

	public final synchronized void addListener(ChangeListener listener) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object ext = arr[i];
			if (ext instanceof ChangeListenerEntry && ext.listener == listener)
				return;
		}
		insExtElem(arr,arr.length,new ChangeListenerEntry(listener));
	}

	public final int getPosLine() { return pos >>> 11; }
	public final void setPosLine(int lineno) { pos = lineno << 11; }
	
	@getter public final int get$pos() {
		TextSourcePosition tsp = (TextSourcePosition)getUnVersionedData(TextSourcePosition.class);
		if (tsp == null)
			return 0;
		return tsp.pos;
	}

	@setter public final synchronized void set$pos(int value) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object ext = arr[i];
			if (ext instanceof TextSourcePosition) {
				if (value == 0)
					delExtElem(arr,i,ext);
				else
					ext.pos = value;
				return;
			}
		}
		if (value != 0)
			insExtElem(arr,arr.length,new TextSourcePosition(value));
	}

	public final boolean isAttached() {
		return p_parent_ != null;
	}
	public final boolean isAttachedBy(AttrSlot attr_slot) {
		if (!attr_slot.isAttr())
			return false;
		ParentAttrSlot p_attr = attr_slot.parent_attr_slot;
		if (p_attr == ANode.nodeattr$parent)
			return p_parent_ != null;
		foreach (ParentInfo pi; getExtData(); pi.p_slot.parent_attr_slot == p_attr)
			return true;
		return false;
	}
	
	public void callbackChanged(NodeChangeInfo info) {
		if (ASTNode.EXECUTE_UNVERSIONED)
			return;
		callbackChangedNotify(info);
	}
	private void callbackChangedNotify(NodeChangeInfo info) {
		// notify listeners
		//if (!ASTNode.EXECUTE_UNVERSIONED && Thread.currentThread().getThreadGroup() != CompilerThreadGroup) {
		//	Object p_data = this.p_handle_.h_data;
		//	if (p_data == null)
		//		return;
		//	if (p_data instanceof Object[]) {
		//		foreach (ChangeListenerEntry uvd; (Object[])p_data)
		//			uvd.listener.callbackNodeChanged(info);
		//	}
		//	else if (p_data instanceof ChangeListenerEntry)
		//		p_data.listener.callbackNodeChanged(info);
		//}
	}

	// idx >= 0 && old_value == null => insert into space
	// idx >= 0 && new_value == null => delete from space
	// idx >= 0 && new_value != null && old_value != null => replace in space
	// idx < 0 && old_value == null => attach scalar
	// idx < 0 && new_value == null => detach scalar
	// idx >= 0 && new_value != null && old_value != null => replace scalar value
	public final void callbackDataSet(AttrSlot slot, Object old_value, Object new_value, int idx) {
		if (slot.isChild() && old_value != null) {
			((ANode)old_value).callbackDetached(this, slot, idx);
			((ANode)old_value).callbackChanged(new NodeChangeInfo(ChangeType.THIS_DETACHED, this, slot, old_value, new_value, idx));
		}
		if (slot.isChild() && new_value != null) {
			((ANode)new_value).callbackAttached(this, slot, idx);
			((ANode)new_value).callbackChanged(new NodeChangeInfo(ChangeType.THIS_ATTACHED, this, slot, old_value, new_value, idx));
		}
		this.callbackChanged(new NodeChangeInfo(ChangeType.ATTR_MODIFIED, this, slot, old_value, new_value, idx));
	}
	public final void callbackDataSet(AttrSlot slot, int old_value, int new_value, int idx) {
		if (old_value != new_value)
			this.callbackDataSet(slot,Integer.valueOf(old_value),Integer.valueOf(new_value),idx);
	}
	public final void callbackDataSet(AttrSlot slot, long old_value, long new_value, int idx) {
		if (old_value != new_value)
			this.callbackDataSet(slot,Long.valueOf(old_value),Long.valueOf(new_value),idx);
	}
	public final void callbackDataSet(AttrSlot slot, double old_value, double new_value, int idx) {
		if (old_value != new_value)
			this.callbackDataSet(slot,Double.valueOf(old_value),Double.valueOf(new_value),idx);
	}
	public final void callbackDataSet(AttrSlot slot, boolean old_value, boolean new_value, int idx) {
		if (old_value != new_value)
			this.callbackDataSet(slot,Boolean.valueOf(old_value),Boolean.valueOf(new_value),idx);
	}
	public final void callbackMetaSet(AttrSlot slot, MNode flag) {
		assert(!flag.isAttached());
		flag.p_parent_ = this;
	}
	public final void callbackMetaDel(AttrSlot slot, MNode flag) {
		assert(flag.isAttached());
		flag.p_parent_ = null;
	}

	// attach to parent node 'parent' to parent's slot 'slot'
	private void callbackAttached(ANode parent, AttrSlot slot, int idx) {
		assert (slot.isAttr());
		assert ((slot instanceof ScalarAttrSlot && idx < 0) || idx >= 0);
		if (slot.parent_attr_slot == ANode.nodeattr$parent) {
			assert(!isAttached());
			assert(parent != null && parent != this);
			this.p_parent_ = parent;
			assert (parent() == parent);
		} else {
			Object[] arr = getExtData();
			insExtElem(arr,arr.length,new ParentInfo(parent,slot));
		}
	}
	
	private void callbackDetached(ANode parent, AttrSlot slot, int idx) {
		assert (slot.isAttr());
		assert ((slot instanceof ScalarAttrSlot && idx < 0) || idx >= 0);
		if (slot.parent_attr_slot == ANode.nodeattr$parent) {
			assert(p_parent_ == parent);
			this.p_parent_ = null;
		} else {
			Object[] arr = getExtData();
			for (int i=0; i < arr.length; i++) {
				Object dat = arr[i];
				if (dat instanceof ParentInfo && dat.p_parent == parent && dat.p_slot == slot) {
					delExtElem(arr,i,dat);
					return;
				}
			}
		}
	}

	public final void notifyParentThatIHaveChanged() {
		ANode parent = parent();
		if (parent != null)
			parent.callbackChanged(new NodeChangeInfo(ChangeType.ATTR_UPDATED, parent, pslot(), this, this, -1));
	}

	public ANode parent() {
		return p_parent_;
	}
	
	public final AttrSlot pslot() {
		ANode parent = this.parent();
		if (parent == null)
			return null;
		foreach (AttrSlot attr; parent.values(); attr.isChild() && !attr.isExtData()) {
			if (attr instanceof ASpaceAttrSlot) {
				foreach (INode n; attr.getArray(parent); n == this) {
					return attr;
				}
			} else {
				if (((ScalarAttrSlot)attr).get(parent) == this) {
					return attr;
				}
			}
		}
		foreach (DataAttachInfo dat; parent.getExtData(); dat.p_slot.isChild()) {
			if (dat.p_data == this) {
				return dat.p_slot;
			}
			if (dat.p_data instanceof INode[]) {
				foreach (INode n; (INode[])dat.p_data; n == this) {
					return dat.p_slot;
				}
			}
		}
		return null;
	}

	public AttrSlot[] values() {
		return ANode.$values;
	}
	
	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (val instanceof SymbolRef && val.name == null)
			return false;
		if (attr.isAttr())
			return true;
		if (attr.name == "this")
			return true;
		return false;
	}

	public final <N extends INode> ExtSpaceIterator<N> getExtSpaceIterator(ExtSpaceAttrSlot attr) {
		return new ExtSpaceIterator<N>(this, attr, getExtData());
	}
	
	public AttrSlot getAttrSlot(String name) {
		if (name == "this")
			return nodeattr$this;
		if (name == "parent")
			return nodeattr$parent;
		foreach (AttrSlot a; this.values(); a.name == name)
			return a;
		foreach (DataAttachInfo dat; getExtData(); dat.p_slot.name == name)
			return dat.p_slot;
		throw new RuntimeException("No @nodeAttr value \"" + name + "\" in "+getClass().getName());
	}
	
	public final Object getVal(AttrSlot attr) {
		if (attr instanceof ParentAttrSlot) {
			if (attr == ANode.nodeattr$parent)
				return parent();
			return getExtParent(attr);
		}
		if (attr.isExtData() || attr instanceof ExtSpaceAttrSlot)
			return getExtData(attr);
		if (attr instanceof ScalarAttrSlot)
			return attr.get(this);
		if (attr instanceof ASpaceAttrSlot)
			return attr.getArray(this);
		return null;
	}
	
	public final void setVal(AttrSlot attr, Object data) {
		if (attr instanceof ParentAttrSlot) {
			if (data == null)
				delExtParent((ParentAttrSlot)attr);
		}
		else if (attr.isExtData()) {
			if (attr instanceof ScalarAttrSlot)
				setExtData(attr, data);
			else
				setExtArray(attr, (INode[])data);
		}
		else if (attr instanceof ScalarAttrSlot)
			attr.set(this, data);
		else if (attr instanceof SpaceAttrSlot)
			attr.setArray(this, (INode[])data);
		else if (attr instanceof ExtSpaceAttrSlot) {
			if (data instanceof INode[])
				setExtArray((ExtSpaceAttrSlot)attr, (INode[])data);
			else
				attr.add(this, (INode)data);
		}
	}
	
	public final Object getVal(AttrSlot attr, int idx) {
		Object val = getVal(attr);
		if (val instanceof INode[])
			return ((INode[])val)[idx];
		if (idx == 0)
			return val;
		throw new ArrayIndexOutOfBoundsException(idx);
	}
	
	public final void setVal(AttrSlot attr, int idx, Object data) {
		if (attr instanceof ASpaceAttrSlot)
			spaceSlotSet((ASpaceAttrSlot)attr, idx, (INode)data);
		else if (idx == 0)
			setVal(attr, data);
		else
			throw new ArrayIndexOutOfBoundsException(idx);
	}
	
	public final void addVal(AttrSlot attr, Object data) {
		if (attr instanceof ASpaceAttrSlot)
			spaceSlotAppend((ASpaceAttrSlot)attr, (INode)data);
		else
			throw new ArrayIndexOutOfBoundsException(0);
	}
	
	public final void delVal(AttrSlot attr, int idx) {
		if (attr instanceof ASpaceAttrSlot)
			spaceSlotDelete((ASpaceAttrSlot)attr, idx);
		else
			throw new ArrayIndexOutOfBoundsException(0);
	}
	
	public final void insVal(AttrSlot attr, int idx, Object data) {
		if (attr instanceof ASpaceAttrSlot)
			spaceSlotInsert((ASpaceAttrSlot)attr, idx, (INode)data);
		else
			throw new ArrayIndexOutOfBoundsException(0);
	}
	
	private Object getExtData(AttrSlot attr) {
		foreach (DataAttachInfo dat; getExtData(); dat.p_slot == attr)
			return dat.p_data;
		return null;
	}
	
	private INode getExtParent(AttrSlot attr) {
		assert (((ParentAttrSlot)attr).is_unique);
		foreach (ParentInfo pi; getExtData(); pi.p_slot.parent_attr_slot == attr)
			return pi.p_parent;
		return null;
	}
	
	private void setExtData(AttrSlot attr, Object d) {
		assert (attr instanceof ScalarAttrSlot);
		if (d == null) {
			delExtData(attr);
			return;
		}

		Object[] arr = this.getExtData();
		for (int i=0; i < arr.length; i++) {
			Object dat = arr[i];
			if !(dat instanceof DataAttachInfo)
				continue;
			DataAttachInfo ai = (DataAttachInfo)dat;
			if (ai.p_slot == attr) {
				if (ai.p_data == d)
					return;
				setExtElem(arr,i,new DataAttachInfo(d,attr));
				callbackDataSet(attr, dat.p_data, d, -1);
				return;
			}
		}
		insExtElem(arr,arr.length,new DataAttachInfo(d,attr));
		callbackDataSet(attr, null, d, -1);
		return;
	}

	private void setExtArray(AttrSlot attr, INode[] arr) {
		assert (attr instanceof ASpaceAttrSlot);
		if (arr.length == 0) {
			delExtData(attr);
			return;
		}
		Object[] ext_data = getExtData();
		for (int i=0; i < ext_data.length; i++) {
			Object dat = ext_data[i];
			if !(dat instanceof DataAttachInfo)
				continue;
			DataAttachInfo ai = (DataAttachInfo)dat;
			if (ai.p_slot == attr) {
				if (ai.p_data == arr)
					return;
				setExtElem(ext_data,i,new DataAttachInfo(arr,attr));
				return;
			}
		}
		insExtElem(ext_data,ext_data.length,new DataAttachInfo(arr,attr));
	}

	private void delExtData(AttrSlot attr) {
		Object[] arr = getExtData();
		for (int i=0; i < arr.length; i++) {
			Object dat = arr[i];
			if (dat instanceof DataAttachInfo && dat.p_slot == attr) {
				delExtElem(arr,i,dat);
				return;
			}
		}
	}

	private void delExtParent(ParentAttrSlot attr) {
		assert (attr.is_unique);
		Object[] ext_data = getExtData();
		for (int i=0; i <= ext_data.length; i++) {
			Object dat = ext_data[i];
			if (dat instanceof ParentInfo && dat.p_slot.parent_attr_slot == attr) {
				delExtElem(ext_data,i,dat);
				dat.p_parent.callbackDataSet(dat.p_slot, this, null, -1);
				return;
			}
		}
	}
	
	private void spaceSlotSet(ASpaceAttrSlot slot, int idx, INode node) {
		INode[] narr = (INode[])slot.getArray(this).clone();
		INode old = narr[idx];
		narr[idx] = node;
		slot.setArray(this,narr);
		if (slot.isAttr())
			this.callbackDataSet(slot, old, node, idx);
	}

	private void spaceSlotAppend(ASpaceAttrSlot slot, INode node) {
		INode[] narr = slot.getArray(this);
		int sz = narr.length;
		INode[] tmp = (INode[])java.lang.reflect.Array.newInstance(slot.typeinfo.clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		slot.setArray(this,tmp);
		if (slot.isAttr())
			this.callbackDataSet(slot, null, node, sz);
	}

	private void spaceSlotDelete(ASpaceAttrSlot slot, int idx) {
		INode[] narr = slot.getArray(this);
		INode node = narr[idx];
		int sz = narr.length-1;
		INode[] tmp = (INode[])java.lang.reflect.Array.newInstance(slot.typeinfo.clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		slot.setArray(this,tmp);
		if (slot.isAttr())
			this.callbackDataSet(slot, node, null, idx);
	}

	private void spaceSlotInsert(ASpaceAttrSlot slot, int idx, INode node) {
		INode[] narr = slot.getArray(this);
		int sz = narr.length;
		if (idx > sz) idx = sz;
		INode[] tmp = (INode[])java.lang.reflect.Array.newInstance(slot.typeinfo.clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		slot.setArray(this,tmp);
		if (slot.isAttr())
			this.callbackDataSet(slot, null, node, idx);
	}

	public final void walkTree(INode parent, AttrSlot slot, ITreeWalker walker) {
		if (walker.pre_exec(this, parent, slot)) {
			foreach (AttrSlot attr; this.values(); attr.isChild()) {
				if (attr instanceof ASpaceAttrSlot)
					walker.visitINodeSpace(attr.getArray(this), this, attr);
				else if (attr instanceof ScalarAttrSlot)
					walker.visitINode((INode)attr.get(this), this, attr);
			}
			//foreach (DataAttachInfo dat; getExtData()) {
			//	AttrSlot attr = dat.p_slot;
			//	if (attr.isChild() && (attr.isExternal() || attr instanceof ExtSpaceAttrSlot)) {
			//		if (dat.p_data instanceof INode[])
			//			walker.visitINodeSpace((INode[])dat.p_data, this, attr);
			//		else
			//			walker.visitINode((INode)dat.p_data, this, attr);
			//	}
			//}
		}
		walker.post_exec(this, parent, slot);
	}

	public final void walkTree(TreeWalker walker) {
		if (walker.pre_exec(this)) {
			foreach (AttrSlot attr; this.values(); attr.isChild()) {
				if (attr instanceof ASpaceAttrSlot)
					walker.visitANodeSpace(attr.getArray(this));
				else if (attr instanceof ScalarAttrSlot)
					walker.visitANode((ANode)attr.get(this));
			}
			//foreach (DataAttachInfo dat; getExtData(); dat.p_slot.isChild() && (dat.p_slot.isExternal() || dat.p_slot instanceof ExtSpaceAttrSlot)) {
			//	if (dat.p_data instanceof INode[])
			//		walker.visitANodeSpace((INode[])dat.p_data);
			//	else
			//		walker.visitANode((ANode)dat.p_data);
			//}
		}
		walker.post_exec(this);
	}

	public Object copy(CopyContext cc) {
		INode obj = cc.getCopyOf(this.getUID());
		if (obj != null)
			return obj;
		if (this instanceof TypeInfoInterface)
			obj = (INode)((TypeInfoInterface)this).getTypeInfoField().newInstance();
		else
			obj = (INode)this.getClass().newInstance();
		cc.addCopyInfo(this,obj);
		return this.copyTo(obj, cc);
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ANode node = (ANode)to$node;
		Object[] this_ext_data = this.getExtData();
		for (int i=0; i < this_ext_data.length; i++) {
			Object dat = this_ext_data[i];
			if (dat instanceof DataAttachInfo) {
				DataAttachInfo ai = (DataAttachInfo)dat;
				if (ai.p_slot.isAttr()) {
					if (ai.p_data instanceof ANode) {
						INode n = (INode)dat.p_data;
						INode nd = (INode)n.copy(in$context);
						AttrSlot slot = ai.p_slot;
						node.setVal(slot,nd);
						continue;
					}
					else if (ai.p_slot instanceof ASpaceAttrSlot) {
						INode[] narr = (INode[])((INode[])ai.p_data).clone();
						for (int x=0; x < narr.length; x++)
							narr[x] = (INode)narr[x].copy(in$context);
						node.setVal(ai.p_slot,narr);
						for (int x=0; x < narr.length; x++)
							node.callbackDataSet(ai.p_slot, null, narr[x], x);
					}
				} else {
					node.setVal(ai.p_slot,ai.p_data);
				}
			}
		}
		return node;
	}
	
	public final this.type detach(INode parent, AttrSlot slot)
	{
		//assert (parent == parent());
		//assert (slot == pslot());
		if (isAttached()) {
			slot.detach(parent, this);
			//assert(!isAttached());
		}
		foreach (ParentInfo pi; getExtData())
			pi.p_slot.detach(pi.p_parent, this);
		return this;
	}
	
	public final this.type detach()
		operator "~ V"
	{
		return detach(parent(), pslot());
	}
	
	public final <N extends INode> N replaceWithNode(N node, INode parent, AttrSlot pslot) {
		//assert(isAttached());
		if (node == null) {
			this.detach(parent, pslot);
			return null;
		}
		//assert (parent == parent());
		//assert (pslot == pslot());
		if (pslot instanceof ASpaceAttrSlot) {
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			pslot.set(parent, idx, node);
		}
		else if (pslot instanceof ScalarAttrSlot) {
			//assert(pslot.get(parent) == this);
			pslot.set(parent, node);
		}
		//assert(node == null || node.isAttached());
		if (node instanceof ASTNode && this instanceof ASTNode && node.pos == 0)
			((ASTNode)node).pos = ((ASTNode)this).pos;
		return node;
	}
	
	public INode doRewrite(RewriteContext ctx) {
		if !(this instanceof ASTNode)
			return new Copier().copyFull(this);
		ANode rn;
		if (this instanceof TypeInfoInterface)
			rn = (ANode)((TypeInfoInterface)this).getTypeInfoField().newInstance();
		else
			rn = (ANode)this.getClass().newInstance();
		if (this instanceof DNode)
			((DNode)rn).nodeflags = ((DNode)this).nodeflags;
		foreach (AttrSlot attr; this.values(); attr.isAttr() && !attr.isNotCopyable()) {
			if (attr instanceof ASpaceAttrSlot) {
				foreach (INode n; attr.iterate(this)) {
					Object obj = n.asANode().doRewrite(ctx);
					if (obj instanceof BlockRewr) {
						foreach (ASTNode st; obj.stats) {
							n = (INode)ctx.fixup(attr,st);
							if (n != null)
								attr.add(rn,n);
						}
					} else {
						n = (INode)ctx.fixup(attr,obj);
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
					INode rw = val.doRewrite(ctx);
					while (rw instanceof BlockRewr && rw.stats.length == 1)
						rw = rw.stats[0];
					attr.set(rn,ctx.fixup(attr,rw));
				}
				else
					attr.set(rn,ctx.fixup(attr,val));
			}
		}
		//foreach (DataAttachInfo dat; this.getExtData()) {
		//	AttrSlot attr = dat.p_slot;
		//	if (attr instanceof ScalarAttrSlot && attr.isExternal())
		//		this.setExtData(attr, ctx.fixup(attr,((INode)dat.p_data).asANode().doRewrite(ctx)));
		//}
		return rn;
	}
	
	public AutoCompleteResult resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.isAutoComplete()) {
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
		if (!slot.isChild()) {
			if (slot.typeinfo.clazz == Boolean.TYPE) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "boolean", null, Boolean.FALSE); 
				result.append("true", "boolean", null, Boolean.TRUE);
				return result;
			}
			if (slot.typeinfo.clazz == Boolean.class) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				result.append("false", "Boolean", null, Boolean.FALSE); 
				result.append("true", "Boolean", null, Boolean.TRUE);
				result.append("null", "null", null, null);
				return result;
			}
			if (Enum.class.isAssignableFrom(slot.typeinfo.clazz) && Enum.class != slot.typeinfo.clazz) {
				AutoCompleteResult result = new AutoCompleteResult(true);
				foreach (Enum e; (Enum[])slot.typeinfo.clazz.getDeclaredMethod(Constants.nameEnumValues).invoke(null)) {
					result.append(e.toString(), e.getClass().getName(), null, e); 
				}
				result.append("null", "null", null, null);
				return result;
			}
		}
		return null;
	}

}

public class TreeWalker {
	public boolean pre_exec(ANode n) { return true; }
	public void post_exec(ANode n) {}

	public final void visitANodeSpace(INode[] vals) {
		for (int i=0; i < vals.length; i++)
			this.visitANode(vals[i].asANode());
	}

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

public class ITreeWalker {
	public boolean pre_exec(INode n, INode parent, AttrSlot slot) { return true; }
	public void post_exec(INode n, INode parent, AttrSlot slot) {}

	public final void visitINodeSpace(INode[] vals, INode parent, AttrSlot slot) {
		for (int i=0; i < vals.length; i++)
			this.visitINode(vals[i], parent, slot);
	}

	public final void visitINode(INode val, INode parent, AttrSlot slot) {
		while (val != null) {
			try {
				val.walkTree(parent, slot, this);
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

public final class SemContext extends Context {
	public final SemContext prev;
	public SemContext(SemContext prev) {
		this.prev = prev;
	}
	public final boolean inherits(Context ctx) {
		for (SemContext self = this; self != null; self = self.prev) {
			if (self == ctx)
				return true;
		}
		if (ctx == Context.DEFAULT)
			return true;
		return false;
	}
}

@ThisIsANode(lang=CoreLang)
public abstract class ASTNode extends ANode implements Constants {

	public static final boolean EXECUTE_UNVERSIONED = Boolean.valueOf(System.getProperty("symade.unversioned","true")).booleanValue();
	
	public static final ASTNode[] emptyArray = new ASTNode[0];

	private static final AttrSlot[] $values = {}; // {/*ANode.nodeattr$this,*/ ANode.nodeattr$parent};

	@UnVersioned
	private int compileflags;	// temporal flags for compilation process

	public Language getCompilerLang() { return null; }
	public String getCompilerNodeName() { return null; }
	
	public final void compflagsLock() { compileflags |= 3; }
	public final void compflagsClear() { compileflags &= 0xFFF0003; }
	public final void compflagsClearUnLock() { compileflags &= 0xFFF0000; }
	public final void compflagsClearAndLock() { compileflags = (compileflags & 0xFFF0000) | 3; }

	// Structures
	public @packed(1,compileflags,6)  boolean is_struct_type_resolved; // KievFE_Pass2
	public @packed(1,compileflags,7)  boolean is_struct_args_resolved; // KievFE_Pass2
	public @packed(1,compileflags,8)  boolean is_struct_members_generated; // KievFE_Pass2
	public @packed(1,compileflags,9)  boolean is_struct_pre_generated; // KievME_PreGenartion

	// Expression/statement flags
	public @packed(1,compileflags,6)  boolean is_expr_gen_void;
	public @packed(1,compileflags,7)  boolean is_expr_for_wrapper;
	public @packed(1,compileflags,8)  boolean is_expr_cast_call;
	public @packed(1,compileflags,9)  boolean is_expr_as_field;
	public @packed(1,compileflags,10) boolean is_expr_primary;

	// Statement flags
	public @packed(1,compileflags,11) boolean is_stat_abrupted;
	public @packed(1,compileflags,12) boolean is_stat_breaked;
	public @packed(1,compileflags,13) boolean is_stat_method_abrupted; // also sets is_stat_abrupted
	public @packed(1,compileflags,14) boolean is_stat_auto_returnable;
	public @packed(1,compileflags,15) boolean is_direct_flow_reachable; // reachable by direct control flow (with no jumps)

	// Method flags
	public @packed(1,compileflags,6)  boolean is_mth_need_fields_init;
	public @packed(1,compileflags,7)  boolean is_mth_dispatcher;

	// Var/field
	public @packed(1,compileflags,6)  boolean is_need_proxy;
	public @packed(1,compileflags,7)  boolean is_init_wrapper;
	public @packed(1,compileflags,8)  boolean is_fld_added_to_init;

	// General flags
	public @packed(1,compileflags,4) boolean is_auto_generated;
	public @packed(1,compileflags,3) boolean is_resolved;
	public @packed(1,compileflags,2) boolean is_bad;
	public @packed(1,compileflags,1) boolean versioned;
	public @packed(1,compileflags,0) boolean locked;

	public AttrSlot[] values() {
		return ASTNode.$values;
	}

	public Object copyTo(Object to$node, CopyContext in$context) {
		ASTNode node = (ASTNode)super.copyTo(to$node, in$context);
		node.pos			= this.pos;
		node.compflagsClearUnLock();
		return node;
	}

	public final void replaceWithNodeReWalk(ASTNode node, INode parent, AttrSlot slot) {
		node = replaceWithNode(node,parent,slot);
		Kiev.runProcessorsOn(node);
		throw new ReWalkNodeException(node);
	}
	public final ASTNode replaceWith(()->ASTNode fnode, INode parent, AttrSlot pslot) {
		//assert(isAttached());
		//assert(parent == parent());
		//assert(pslot == pslot());
		if (pslot instanceof ASpaceAttrSlot) {
			int idx = pslot.indexOf(parent, this);
			assert(idx >= 0);
			ASTNode n = fnode();
			assert(n != null);
			if (n.pos == 0) n.pos = this.pos;
			pslot.insert(parent, idx, n);
			assert(n.isAttached());
			return n;
		}
		else if (pslot instanceof ScalarAttrSlot) {
			//assert(pslot.get(parent) == this);
			pslot.set(parent, null);
			ASTNode n = fnode();
			if (n != null && n.pos == 0) n.pos = this.pos;
			pslot.set(parent, n);
			//assert(n == null || n.isAttached());
			return n;
		}
		throw new RuntimeException("replace unknown kind of AttrSlot");
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

	public Type getType(Env env) { return env.tenv.tpVoid; }

	public ASTNode() {
		this(new AHandle(), Kiev.getSemContext());
	}
	public ASTNode(AHandle handle) {
		super(handle, Kiev.getSemContext());
	}
	public ASTNode(AHandle handle, SemContext context) {
		super(handle, context);
	}
	
	public static ASTNode getActual(ASTNode node) {
		if (node == null)
			return null;
		SemContext semantic_context = Kiev.getSemContext();
		foreach (ASTNode nh; node.handle().getHandleData(); semantic_context.inherits(nh.getDataContext()))
			return nh;
		return null;
	}

	public ANode parent() {
		ANode parent = super.parent();
		return parent;
//		if (parent == null)
//			return null;
//		SemContext semantic_context = Kiev.getSemContext();
//		foreach (ASTNode nh; parent.handle().getHandleData(); semantic_context.inherits(nh.getDataContext()))
//			return nh;
//		return null;
	}

	public DFFunc newDFFuncIn(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncIn() for "+getClass()); }
	public DFFunc newDFFuncOut(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncOut() for "+getClass()); }
	public DFFunc newDFFuncTru(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncTru() for "+getClass()); }
	public DFFunc newDFFuncFls(DataFlowInfo dfi) { throw new RuntimeException("newDFFuncFls() for "+getClass()); }

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		foreach (AttrSlot attr; values(); attr.isAutoResolve() && attr.typeinfo.clazz == SymbolRef.class) {
			String resolve_in = attr.auto_resolve_in;
			ScopeOfNames scope = null;
			if (resolve_in != null && resolve_in.length() > 0) {
				Object sc = getVal(getAttrSlot(resolve_in.intern()));
				if (sc instanceof SymbolRef)
					sc = sc.dnode;
				if (sc instanceof ScopeOfNames)
					scope = (ScopeOfNames)sc;
			}
			if (attr instanceof ScalarAttrSlot) {
				SymbolRef sr = (SymbolRef)attr.get(this);
				if (Env.needResolving(sr));
					sr.resolveSymbol(attr.auto_resolve_severity, scope);
			}
			else if (attr instanceof ASpaceAttrSlot) {
				foreach (SymbolRef sr; attr.getArray(this); Env.needResolving(sr))
					sr.resolveSymbol(attr.auto_resolve_severity, scope);
			}
		}
		return true;
	}
	public void preResolveOut(Env env, INode parent, AttrSlot slot) {}
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { return true; }
	public void mainResolveOut(Env env, INode parent, AttrSlot slot) {}
	public boolean preVerify(Env env, INode parent, AttrSlot slot) { return true; }
	public void postVerify(Env env, INode parent, AttrSlot slot) {}

}


public class CompilerException extends RuntimeException {
	public ANode	from;
	public CError	err_id;
	public CompilerException(String msg) {
		super(msg);
	}
	public CompilerException(ANode from, String msg) {
		super(msg);
		this.from = from;
	}
	public CompilerException(ANode from, CError err_id, String msg) {
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

public interface ExportSerialized {
	public String qname();
	public Object getDataToSerialize();
}

public interface ExportXMLDump {
	public String qname();
	public String exportFactory();
}
