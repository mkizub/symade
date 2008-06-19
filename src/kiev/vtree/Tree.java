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

import java.lang.annotation.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class ScalarPtr {
	public final ANode node;
	public final ScalarAttrSlot slot;
	public ScalarPtr(ANode node, ScalarAttrSlot slot) {
		this.node = node;
		this.slot = slot;
	}
	public Object get() { return slot.get(node); }
	public void set(Object val) { return slot.set(node, val); }
}

public final class SpacePtr {
	public final ANode node;
	public final SpaceAttrSlot slot;
	public SpacePtr(ANode node, SpaceAttrSlot slot) {
		this.node = node;
		this.slot = slot;
	}

	@getter
	public int size()
		alias get$length
	{
		return slot.getArray(node).length;
	}

	public ANode get(int idx)
		alias xfy operator []
	{
		return slot.get(node, idx);
	}

	public ANode set(int idx, ANode val)
		alias lfy operator []
	{
		return slot.set(node, idx, val);
	}

	public ANode add(ANode val)
		alias append
		alias lfy operator +=
	{
		return slot.add(node, val);
	}
}

public abstract class AttrSlot {
	public static final AttrSlot[] emptyArray = new AttrSlot[0];
	
	public final String          name; // field (property) name
	public final ParentAttrSlot  parent_attr_slot;
	public final boolean         is_attr; // @nodeAttr or @nodeData
	public final boolean         is_space; // if Node[]
	public final boolean         is_child; // @nodeAttr and Node
	public final boolean         is_external; // not a field, declared externally of the node, not in values(), stored in ANode.ext_data[]
	public final Class           clazz; // type of the fields
	public final TypeInfo        typeinfo; // type of the fields
	public final Object          defaultValue;
	
	private final boolean        is_xml_ignore;
	private final boolean        is_xml_attr;
	private final String         xml_attr_name;
	
	public AttrSlot(String name, ParentAttrSlot p_attr, boolean is_space, boolean is_external, TypeInfo typeinfo) {
		assert (name.intern() == name);
		this.name = name;
		this.parent_attr_slot = p_attr;
		this.is_attr = (p_attr != null);
		this.is_space = is_space;
		this.is_external = is_external;
		this.clazz = typeinfo.clazz;
		this.typeinfo = typeinfo;
		if (is_attr) {
			if (is_space)
				this.is_child = true;
			else if (ANode.class.isAssignableFrom(typeinfo.clazz) || INode.class.isAssignableFrom(typeinfo.clazz))
				this.is_child = true;
		}
		if (is_space) defaultValue = java.lang.reflect.Array.newInstance(clazz,0);
		else if (clazz == Boolean.class) defaultValue = Boolean.FALSE;
		else if (clazz == Character.class) defaultValue = new Character('\0');
		else if (clazz == Byte.class) defaultValue = new Byte((byte)0);
		else if (clazz == Short.class) defaultValue = new Short((short)0);
		else if (clazz == Integer.class) defaultValue = new Integer(0);
		else if (clazz == Long.class) defaultValue = new Long(0L);
		else if (clazz == Float.class) defaultValue = new Float(0.f);
		else if (clazz == Double.class) defaultValue = new Double(0.);
		else if (clazz == String.class) defaultValue = "";
		else defaultValue = null;
		
		AttrXMLDumpInfo dinfo = (AttrXMLDumpInfo)this.getClass().getAnnotation(AttrXMLDumpInfo.class);
		if (dinfo != null) {
			is_xml_ignore = dinfo.ignore();
			is_xml_attr = dinfo.attr();
			xml_attr_name = dinfo.name().intern();
			if (xml_attr_name.length() == 0)
				xml_attr_name = this.name;
		} else {
			is_xml_ignore = false;
			is_xml_attr = false;
			xml_attr_name = this.name;
		}

	}
	
	public final boolean isSemantic() {
		return this.parent_attr_slot == ANode.nodeattr$parent;
	}

	//public abstract void set(ANode parent, Object value);
	//public abstract Object get(ANode parent);
	//public void clear(ANode parent) { this.set(parent, defaultValue); }
	public abstract void detach(ANode parent, ANode old);

	public boolean isWrittable() { return true; }
	
	public boolean isXmlIgnore() { return is_xml_ignore; }
	public boolean isXmlAttr() { return is_xml_attr; }
	public String getXmlLocalName() { return xml_attr_name; }
	public String getXmlFullName() { return xml_attr_name; }
	public String getXmlNamespaceURI() { return null; }
	public Language getCompilerLang() { return null; }
}

public abstract class ScalarAttrSlot extends AttrSlot {
	public ScalarAttrSlot(String name, ParentAttrSlot p_attr, boolean is_space, boolean is_external, TypeInfo typeinfo) {
		super(name,p_attr,is_space,is_external,typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
	public void clear(ANode parent) { this.set(parent, defaultValue); }
	public void detach(ANode parent, ANode old) { this.set(parent, null); }
}

public final class ParentAttrSlot extends AttrSlot {
	
	public final boolean is_unique;
	
	public ParentAttrSlot(String name, boolean is_external, boolean is_unique, TypeInfo typeinfo) {
		super(name, null, false, is_external, typeinfo);
		this.is_unique = is_unique;
	}
	public void set(ANode node, Object value) {
		throw new RuntimeException("@nodeParent '"+name+"' is not writeable"); 
	}
	public ANode get(ANode node) {
		if (this == ANode.nodeattr$parent)
			return node.parent();
		if (this.is_unique)
			return node.getExtParent(this);
		throw new RuntimeException("@nodeParent '"+name+"' is not readable"); 
	}
	public void clear(ANode node) {
		if (this == ANode.nodeattr$parent)
			node.callbackDetached(node.p_parent, node.p_slot);
		else if (this.is_unique)
			node.delExtParent(this);
		else
			throw new RuntimeException("@nodeParent '"+name+"' is not cleanable"); 
	}
	public void detach(ANode parent, ANode node) {
		if (this == ANode.nodeattr$parent)
			node.callbackDetached(node.p_parent, node.p_slot);
		else if (this.is_unique)
			node.delExtParent(this);
		else
			throw new RuntimeException("@nodeParent '"+name+"' is not cleanable"); 
	}
}

public abstract class RefAttrSlot extends ScalarAttrSlot {
	public RefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, false, false, typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class AttAttrSlot extends ScalarAttrSlot {
	public AttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, ANode.nodeattr$parent, false, false, typeinfo);
		assert (this.is_attr);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class ExtRefAttrSlot extends ScalarAttrSlot {
	public ExtRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, false, false, typeinfo);
	}
	public final void set(ANode parent, Object value) {
		parent.setExtData(value, this);
	}
	public final Object get(ANode parent) {
		return parent.getExtData(this);
	}
	public final void clear(ANode parent) {
		return parent.delExtData(this);
	}
	public final void detach(ANode parent, ANode old) {
		return parent.delExtData(this);
	}
}

public abstract class ExtAttAttrSlot extends ScalarAttrSlot {
	public ExtAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, ANode.nodeattr$parent, false, false, typeinfo);
		assert (this.is_attr);
	}
	public final void set(ANode parent, Object value) {
		parent.setExtData(value, this);
	}
	public final Object get(ANode parent) {
		return parent.getExtData(this);
	}
	public final void clear(ANode parent) {
		return parent.delExtData(this);
	}
	public final void detach(ANode parent, ANode old) {
		return parent.delExtData(this);
	}
}

public class ExtSpaceAttrSlot<N extends ANode> extends AttrSlot {
	public ExtSpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name,p_attr,false,true,typeinfo);
	}
	public ExtSpaceAttrSlot(String name, TypeInfo typeinfo) {
		super(name,ANode.nodeattr$parent,false,true,typeinfo);
	}

	public final ExtChildrenIterator iterate(ANode parent) {
		return parent.getExtChildIterator(this);
	}
	public final void add(ANode parent, ANode value) {
		parent.addExtData(value, this);
	}
	public final void detach(ANode parent, ANode old) {
		return parent.delExtData(old);
	}
	public final void delAll(ANode parent) {
		foreach (ANode n; iterate(parent); n.isAttached())
			parent.delExtData(n);
	}
	
}

public abstract class SpaceAttrSlot<N extends ANode> extends AttrSlot {
	public SpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name, p_attr, true, false, typeinfo);
	}
	
	protected void set(ANode parent, Object value) { throw new Error("SpaceAttrSlot.set()"); }
	protected Object get(ANode parent) { throw new Error("SpaceAttrSlot.get()"); }

	public N[] getArray(ANode parent) {
		return (N[])this.get(parent);
	}

	public void setArray(ANode parent, Object/*N[]*/ arr) {
		return this.set(parent, arr);
	}

	public final int indexOf(ANode parent, ANode node) {
		N[] narr = getArray(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == node)
				return i;
		}
		return -1;
	}

	public final void detach(ANode parent, ANode old)
	{
		N[] narr = getArray(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == old) {
				this.del(parent,i);
				return;
			}
		}
		throw new RuntimeException("Not found node");
	}
	
	public final void addAll(ANode parent, N[] arr) {
		if (arr == null) return;
		for (int i=0; i < arr.length; i++)
			add(parent, arr[i]);
	}

	public final N get(ANode parent, int idx) {
		N[] narr = getArray(parent);
		return narr[idx];
	}

	public abstract N set(ANode parent, int idx, N node);
	public abstract N add(ANode parent, N node);
	public abstract void del(ANode parent, int idx);
	public abstract void insert(ANode parent, int idx, N node);
	public abstract void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc);
	public abstract void delAll(ANode parent);
	public abstract N[] delToArray(ANode parent);

}

public abstract class SpaceRefAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, typeinfo);
	}

	public final N set(ANode parent, int idx, N node) {
		N[] narr = (N[])getArray(parent).clone();
		narr[idx] = node;
		setArray(parent,narr);
		return node;
	}

	public final N add(ANode parent, N node) {
		N[] narr = getArray(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		setArray(parent,tmp);
		return node;
	}

	public final void del(ANode parent, int idx) {
		N[] narr = getArray(parent);
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		setArray(parent,tmp);
	}

	public final void insert(ANode parent, int idx, N node) {
		N[] narr = getArray(parent);
		int sz = narr.length;
		if (idx > sz) idx = sz;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		setArray(parent,tmp);
	}

	public final void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc) {
		foreach (N n; arr)
			add(parent, n);
	}
	
	public final void delAll(ANode parent) {
		N[] narr = getArray(parent);
		if (narr.length == 0)
			return;
		setArray(parent,(N[])defaultValue);
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = getArray(parent);
		if (narr.length > 0)
			setArray(parent,(N[])defaultValue);
		return narr;
	}
}

public abstract class SpaceAttAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, ANode.nodeattr$parent, typeinfo);
		assert (this.is_attr);
	}

	public SpaceAttAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name, p_attr, typeinfo);
		assert (this.is_attr && !this.isSemantic());
	}

	public final N set(ANode parent, int idx, N node) {
		assert(!this.isSemantic() || !node.isAttached());
		N[] narr = (N[])getArray(parent).clone();
		narr[idx].callbackDetached(parent, this);
		narr[idx] = node;
		setArray(parent,narr);
		node.callbackAttached(parent, this);
		return node;
	}

	public final N add(ANode parent, N node) {
		assert(!this.isSemantic() || !node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = getArray(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		setArray(parent,tmp);
		node.callbackAttached(parent, this);
		return node;
	}

	public final void del(ANode parent, int idx) {
		N[] narr = getArray(parent);
		narr[idx].callbackDetached(parent, this);
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		setArray(parent,tmp);
	}

	public final void insert(ANode parent, int idx, N node) {
		assert(!this.isSemantic() || !node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = getArray(parent);
		int sz = narr.length;
		if (idx > sz) idx = sz;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		setArray(parent,tmp);
		node.callbackAttached(parent, this);
	}

	public final void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc) {
		foreach (N n; arr)
			add(parent, n.ncopy(cc));
	}
	
	public final void delAll(ANode parent) {
		N[] narr = getArray(parent);
		if (narr.length == 0)
			return;
		setArray(parent,(N[])defaultValue);
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached(parent, this);
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = getArray(parent);
		if (narr.length > 0) {
			setArray(parent,(N[])defaultValue);
			for (int i=0; i < narr.length; i++)
				narr[i].callbackDetached(parent, this);
		}
		return narr;
	}
}

public final class Transaction {
	
	private static int currentVersion;
	private static Transaction currentTransaction;

	public static Transaction open(String name) {
		assert (currentTransaction == null);
		currentTransaction = new Transaction(name);
		return currentTransaction;
	}

	public static Transaction enter(Transaction tr, String name) {
		if (tr == null)
			return open(name);
		tr.recursion_counter++;
		return tr;
	}

	public static Transaction get() {
		return currentTransaction;
	}

	public static Transaction current() {
		assert (currentTransaction != null);
		return currentTransaction;
	}

	public void close() {
		assert (currentTransaction == this);
		if (!ASTNode.EXECUTE_UNVERSIONED) {
			ASTNode[] nodes = this.nodes;
			int n = this.size;
			for (int i=0; i < n; i++)
				nodes[i].compileflags |= 3; // locked & versioned
		}
		currentTransaction = null;
	}

	public void leave() {
		if (--recursion_counter <= 0)
			close();
	}

	public void rollback(boolean save_next) {
		if (!ASTNode.EXECUTE_UNVERSIONED) {
			ASTNode[] nodes = this.nodes;
			int n = this.size;
			for (int i=0; i < n; i++)
				nodes[i].rollback(this,save_next);
		}
		if (currentTransaction == this)
			currentTransaction = null;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}

	public final int version;
	public final String name;
	private int recursion_counter;
	private int size;
	private ASTNode[] nodes;

	private Transaction(String name) {
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
}


