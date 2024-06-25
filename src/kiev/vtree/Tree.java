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

import java.lang.annotation.*;

/**
 * @author Maxim Kizub
 *
 */

public final class ScalarPtr {
	public final INode node;
	public final ScalarAttrSlot slot;
	public ScalarPtr(INode node, ScalarAttrSlot slot) {
		this.node = node.asANode();
		this.slot = slot;
	}
	public Object get() { return slot.get(node); }
	public void set(Object val) { return slot.set(node, val); }
}

public final class SpacePtr {
	@virtual @abstract
	public:ro int		length;

	public final INode node;
	public final SpaceAttrSlot slot;
	public SpacePtr(INode node, SpaceAttrSlot slot) {
		this.node = node.asANode();
		this.slot = slot;
	}

	@getter
	public int size()
		alias get$length
	{
		return slot.getArray(node).length;
	}

	public INode get(int idx)
		operator "V [ V ]"
	{
		return slot.get(node, idx);
	}

	public INode set(int idx, INode val)
		operator "V [ V ] = V"
	{
		slot.set(node, idx, val);
		return val;
	}

	public INode add(INode val)
		operator "V += V"
		alias append
	{
		slot.add(node, val);
		return val;
	}
}

public abstract class AttrSlot {
	public static final AttrSlot[] emptyArray = new AttrSlot[0];
	
	private static final int IS_ATTR  = 1;
	private static final int IS_CHILD = 2;
	private static final int IS_SYMREF = 4;
	private static final int IS_EXT_DATA = 8;
	//private static final int IS_EXTERNAL = 16;
	private static final int IS_NOT_COPYABLE = 32;
	private static final int IS_AUTO_COMPLETE = 64;
	private static final int IS_AUTO_RESOLVE = 128;
	private static final int IS_XML_IGNORE = 256;
	private static final int IS_XML_ATTR = 512;
	private static final int IS_BIN_IGNORE = 1024;
	private static final int IS_BIN_LEADING = 2048;
	private static final int IS_BIN_EXTENDED = 4096;
	
	private final int            flags;
	public final String          name; // field (property) name
	public final ParentAttrSlot  parent_attr_slot;
	public final TypeInfo        typeinfo; // type of the fields
	public final Object          defaultValue;

	public final String          auto_complete_in;
	public final Class[]         auto_complete_scopes;
	public final String          auto_resolve_in;
	public final SeverError      auto_resolve_severity;

	private final String         xml_attr_name;
	
	public AttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		assert (name.intern() == name);
		this.name = name;
		this.parent_attr_slot = p_attr;
		if (p_attr != null)
			this.flags |= IS_ATTR;
		if (this instanceof ExtAttrSlot || this instanceof ExtSpaceAttrSlot)
			this.flags |= IS_EXT_DATA;
		if (this instanceof PScalarAttrSlot || this instanceof PSpaceAttrSlot || this instanceof PExtSpaceAttrSlot)
			this.flags |= IS_EXT_DATA;
		this.typeinfo = typeinfo;
		if (isAttr()) {
			if (this instanceof SpaceAttrSlot || this instanceof ExtSpaceAttrSlot)
				this.flags |= IS_CHILD;
			else if (ANode.class.isAssignableFrom(typeinfo.clazz) || INode.class.isAssignableFrom(typeinfo.clazz))
				this.flags |= IS_CHILD;
		}
		if (this instanceof SpaceAttrSlot || this instanceof ExtSpaceAttrSlot) defaultValue = java.lang.reflect.Array.newInstance(typeinfo.clazz,0);
		else if (typeinfo.clazz == Boolean.class) defaultValue = Boolean.FALSE;
		else if (typeinfo.clazz == Character.class) defaultValue = new Character('\0');
		else if (typeinfo.clazz == Byte.class) defaultValue = new Byte((byte)0);
		else if (typeinfo.clazz == Short.class) defaultValue = new Short((short)0);
		else if (typeinfo.clazz == Integer.class) defaultValue = new Integer(0);
		else if (typeinfo.clazz == Long.class) defaultValue = new Long(0L);
		else if (typeinfo.clazz == Float.class) defaultValue = new Float(0.f);
		else if (typeinfo.clazz == Double.class) defaultValue = new Double(0.);
		else if (typeinfo.clazz == String.class) defaultValue = "";
		else defaultValue = null;
		
		{
			nodeAttr att = this.getClass().getAnnotation(nodeAttr.class);
			if (att != null && !att.copyable())
				this.flags |= IS_NOT_COPYABLE;
			nodeData dat = this.getClass().getAnnotation(nodeData.class);
			if (dat != null && !dat.copyable())
				this.flags |= IS_NOT_COPYABLE;
			nodeSRef ref = this.getClass().getAnnotation(nodeSRef.class);
			if (ref != null)
				this.flags |= IS_SYMREF;
			if (ref != null && !ref.copyable())
				this.flags |= IS_NOT_COPYABLE;
		}
		
		{
			SymbolRefAutoComplete aci = this.getClass().getAnnotation(SymbolRefAutoComplete.class);
			if (aci != null) {
				if (aci.value())
					this.flags |= IS_AUTO_COMPLETE;
				auto_complete_in = aci.in();
				auto_complete_scopes = aci.scopes();
			}
		}
		
		{
			SymbolRefAutoResolve ari = this.getClass().getAnnotation(SymbolRefAutoResolve.class);
			if (ari != null) {
				if (ari.value())
					this.flags |= IS_AUTO_RESOLVE;
				auto_resolve_in = ari.in();
				auto_resolve_severity = ari.sever();
			}
		}
		
		AttrXMLDumpInfo dinfo = this.getClass().getAnnotation(AttrXMLDumpInfo.class);
		if (dinfo != null) {
			if (dinfo.ignore())
				this.flags |= IS_XML_IGNORE;
			if (dinfo.attr())
				this.flags |= IS_XML_ATTR;
			xml_attr_name = dinfo.name().intern();
			if (xml_attr_name.length() == 0)
				xml_attr_name = this.name;
		} else {
			xml_attr_name = this.name;
		}
		
		AttrBinDumpInfo dinfo = this.getClass().getAnnotation(AttrBinDumpInfo.class);
		if (dinfo != null) {
			if (dinfo.ignore())
				this.flags |= IS_BIN_IGNORE;
			if (dinfo.leading())
				this.flags |= IS_BIN_LEADING;
			if (dinfo.extended())
				this.flags |= IS_BIN_EXTENDED;
		}
	}
	
	// make proxy slot
	public AttrSlot(AttrSlot slot) {
		this.name = slot.name;
		this.parent_attr_slot = slot.parent_attr_slot;
		this.flags = slot.flags | IS_EXT_DATA & ~(IS_AUTO_COMPLETE | IS_AUTO_RESOLVE);
		if (slot.isChild())
			this.typeinfo = TypeInfo.newTypeInfo(ANode.class,null);
		else
			this.typeinfo = slot.typeinfo;
		if (this instanceof SpaceAttrSlot)
			defaultValue = ANode.emptyArray;
		else
			defaultValue = slot.defaultValue;
	}
	
	public final boolean isAttr() { return (flags & IS_ATTR) != 0; }
	public final boolean isChild() { return (flags & IS_CHILD) != 0; }
	public final boolean isSymRef() { return (flags & IS_SYMREF) != 0; }
	public final boolean isExtData() { return (flags & IS_EXT_DATA) != 0; }
	//public final boolean isExternal() { return (flags & IS_EXTERNAL) != 0; }
	public final boolean isNotCopyable() { return (flags & IS_NOT_COPYABLE) != 0; }
	public final boolean isAutoComplete() { return (flags & IS_AUTO_COMPLETE) != 0; }
	public final boolean isAutoResolve() { return (flags & IS_AUTO_RESOLVE) != 0; }
	
	
	public final boolean isSemantic() {
		return this.parent_attr_slot == ANode.nodeattr$parent;
	}

	public void detach(INode parent, INode old) {
		throw new RuntimeException("No @nodeAttr value \"" + name + "\" is not detachable");
	}

	public boolean isWrittable() { return true; }
	
	public final boolean isXmlIgnore() { return (flags & IS_XML_IGNORE) != 0; }
	public final boolean isXmlAttr() { return (flags & IS_XML_ATTR) != 0; }
	public final boolean isBinIgnore() { return (flags & IS_BIN_IGNORE) != 0; }
	public final boolean isBinLeading() { return (flags & IS_BIN_LEADING) != 0; }
	public final boolean isBinExtended() { return (flags & IS_BIN_EXTENDED) != 0; }
	
	public String getXmlLocalName() { return xml_attr_name; }
	public String getXmlFullName() { return xml_attr_name; }
	public String getXmlNamespaceURI() { return null; }
	public Language getCompilerLang() { return null; }
}

public abstract class ScalarAttrSlot extends AttrSlot {
	public ScalarAttrSlot(ScalarAttrSlot slot) {
		super(slot);
	}
	public ScalarAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name,p_attr,typeinfo);
	}
	public abstract void set(INode parent, Object value);
	public abstract Object get(INode parent);
	public void detach(INode parent, INode old) { this.set(parent, null); }
	public void clear(INode parent) { this.set(parent, defaultValue); }
}

public class ParentAttrSlot extends AttrSlot {
	
	public final boolean is_unique;
	
	public ParentAttrSlot(String name, boolean is_unique, TypeInfo typeinfo) {
		super(name, null, typeinfo);
		this.is_unique = is_unique;
	}
	public void set(ANode node, Object value) {
		throw new RuntimeException("@nodeParent '"+name+"' is not writeable"); 
	}
	public ANode get(ANode node) {
		if (this == ANode.nodeattr$parent)
			return node.parent();
		if (this.is_unique)
			return (ANode)node.getVal(this);
		throw new RuntimeException("@nodeParent '"+name+"' is not readable"); 
	}
	public void clear(ANode node) {
		if (this == ANode.nodeattr$parent)
			node.parent().callbackDataSet(node.pslot(), node, null, -1);
		else if (this.is_unique)
			node.setVal(this, null);
		else
			throw new RuntimeException("@nodeParent '"+name+"' is not cleanable"); 
	}
	public void detach(INode parent, INode node) {
		if (this == ANode.nodeattr$parent)
			node.parent().asANode().callbackDataSet(node.pslot(), node, null, -1);
		else if (this.is_unique)
			node.setVal(this, null);
		else
			throw new RuntimeException("@nodeParent '"+name+"' is not cleanable"); 
	}
}

public abstract class ExtAttrSlot extends ScalarAttrSlot {
	public ExtAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name, p_attr, typeinfo);
	}
	public final void set(INode parent, Object value) {
		parent.setVal(this, value);
	}
	public final Object get(INode parent) {
		return parent.getVal(this);
	}
	public final void clear(INode parent) {
		return parent.setVal(this, null);
	}
	public final void detach(INode parent, INode old) {
		return parent.setVal(this, null);
	}
}

public abstract class ASpaceAttrSlot<N extends INode> extends AttrSlot {
	public ASpaceAttrSlot(ASpaceAttrSlot slot) {
		super(slot);
	}
	public ASpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name,p_attr,typeinfo);
	}
	public ASpaceAttrSlot(String name, TypeInfo typeinfo) {
		super(name,ANode.nodeattr$parent,typeinfo);
	}

	public abstract N[] getArray(INode parent);
	public abstract void setArray(INode parent, Object/*N[]*/ arr);

	public final int indexOf(INode parent, INode node) {
		N[] narr = getArray(parent);
		for (int i=0; i < narr.length; i++) {
			if (narr[i] == node)
				return i;
		}
		return -1;
	}
	public final int length(INode parent) {
		return getArray(parent).length;
	}
	public final boolean isEmpty(INode parent) {
		return length(parent) == 0;
	}
	public Enumeration<N> iterate(INode parent) {
		return new SpaceIterator<N>(parent, this, getArray(parent));
	}

	public void detach(INode parent, INode old) {
		N[] narr = getArray(parent);
		for (int i=0; i < narr.length; i++) {
			if (narr[i] == old) {
				this.del(parent,i);
				return;
			}
		}
		throw new RuntimeException("Not found node");
	}
	
	public final void delAll(INode parent) {
		N[] narr = getArray(parent);
		if (narr.length == 0)
			return;
		setArray(parent,(N[])defaultValue);
		if (isAttr()) {
			for (int i=narr.length-1; i >= 0; i--)
				parent.asANode().callbackDataSet(this, narr[i], null, i);
		}
	}
	public final N[] delToArray(INode parent) {
		N[] narr = getArray(parent);
		if (narr.length > 0) {
			setArray(parent,(N[])defaultValue);
			if (isAttr()) {
				for (int i=narr.length-1; i >= 0; i--)
					parent.asANode().callbackDataSet(this, narr[i], null, i);
			}
		}
		return narr;
	}

	public abstract void set(INode parent, int idx, INode node);
	public abstract void add(INode parent, INode value);
	public abstract void del(INode parent, int idx);
	public abstract void insert(INode parent, int idx, INode value);
}

public class ExtSpaceAttrSlot<N extends INode> extends ASpaceAttrSlot<N> {
	public final boolean ASSERT_MORE = Kiev.debug;

	public ExtSpaceAttrSlot(ExtSpaceAttrSlot slot) {
		super(slot);
	}
	public ExtSpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name,p_attr,typeinfo);
	}
	public ExtSpaceAttrSlot(String name, TypeInfo typeinfo) {
		super(name,ANode.nodeattr$parent,typeinfo);
	}

	public N[] getArray(INode parent) {
		Object arr = parent.getVal(this);
		if (arr == null)
			return (N[])defaultValue;
		return (N[])arr;
	}

	public void setArray(INode parent, Object/*N[]*/ arr) {
		parent.setVal(this,arr);
	}

	public void set(INode parent, int idx, INode node) {
		parent.setVal(this, idx, node);
	}

	public void add(INode parent, INode value) {
		parent.addVal(this, value);
	}

	public void del(INode parent, int idx) {
		parent.delVal(this, idx);
	}

	public void insert(INode parent, int idx, INode value) {
		parent.insVal(this, idx, value);
	}
}

public abstract class SpaceAttrSlot<N extends INode> extends ASpaceAttrSlot<N> {
	public final boolean ASSERT_MORE = Kiev.debug;

	public SpaceAttrSlot(SpaceAttrSlot slot) {
		super(slot);
	}
	public SpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name, p_attr, typeinfo);
	}
	
	public abstract N[] getArray(INode parent);

	public abstract void setArray(INode parent, Object/*N[]*/ arr);

	public final void addAll(INode parent, N[] arr) {
		if (arr == null) return;
		for (int i=0; i < arr.length; i++)
			add(parent, arr[i]);
	}

	public final N get(INode parent, int idx) {
		N[] narr = getArray(parent);
		return narr[idx];
	}

	public final void set(INode parent, int idx, INode node) {
		assert(!isAttr() || (!this.isSemantic() || !node.isAttached()));
		parent.setVal(this,idx,node);
	}
	public final void add(INode parent, INode node) {
		if (isAttr()) {
			assert(!this.isSemantic() || !node.isAttached());
			if (ASSERT_MORE) assert(indexOf(parent,node) < 0);
		}
		parent.addVal(this,node);
	}
	public final void del(INode parent, int idx) {
		parent.delVal(this,idx);
	}
	public final void insert(INode parent, int idx, INode node) {
		if (isAttr()) {
			assert(!this.isSemantic() || !node.isAttached());
			assert(indexOf(parent,node) < 0);
		}
		parent.insVal(this,idx,node);
	}
	public final void copyFrom(INode parent, N[] arr, CopyContext cc) {
		if (isAttr()) {
			foreach (N n; arr)
				add(parent, (INode)n.copy(cc));
		} else {
			foreach (N n; arr)
				add(parent, n);
		}
	}

}

public final class Transaction {
	
	private static int currentVersion;

	public final int       version;
	public final String    name;
	private      int       recursion_counter;
	private      int       size;
	private      ASTNode[] nodes;

	public static Transaction open(String name) {
		return open(name, (WorkerThreadGroup)Thread.currentThread().getThreadGroup());
	}
	public static Transaction open(String name, WorkerThreadGroup wtg) {
		assert (wtg.transaction == null);
		wtg.transaction = new Transaction(name, wtg);
		return wtg.transaction;
	}

	public static Transaction enter(Transaction tr, String name) {
		if (tr == null)
			return open(name);
		tr.recursion_counter++;
		return tr;
	}

	public static Transaction get() {
		return ((WorkerThreadGroup)Thread.currentThread().getThreadGroup()).transaction;
	}

	public void close(WorkerThreadGroup wtg) {
		assert (wtg.transaction == this, "Closing "+this+" while current is "+wtg.transaction);
		if (!ASTNode.EXECUTE_UNVERSIONED) {
			ASTNode[] nodes = this.nodes;
			int n = this.size;
			for (int i=0; i < n; i++)
				nodes[i].compflagsLock();
		}
		wtg.transaction = null;
	}

	public void leave() {
		if (--recursion_counter <= 0)
			close((WorkerThreadGroup)Thread.currentThread().getThreadGroup());
	}

	public void rollback(boolean save_next) {
	}
	
	public boolean isEmpty() {
		return size == 0;
	}

	private Transaction(String name, WorkerThreadGroup wtg) {
		this.version = ++currentVersion;
		this.name = name;
		this.recursion_counter = 1;
		if (!ASTNode.EXECUTE_UNVERSIONED)
			this.nodes = new ASTNode[64];
	}
	
	public void add(ASTNode node) {
		if (ASTNode.EXECUTE_UNVERSIONED)
			return;
		if (size >= nodes.length) {
			ASTNode[] tmp = new ASTNode[size*2];
			System.arraycopy(nodes,0,tmp,0,size);
			nodes = tmp;
		}
		nodes[size++] = node;
	}
	
	public String toString() {
		return "transaction "+name+"/"+version;
	}
}


