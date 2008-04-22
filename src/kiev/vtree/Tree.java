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
import kiev.be.java15.JNode;

import java.lang.annotation.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class AttrPtr {
	public final ANode node;
	public final AttrSlot slot;
	public AttrPtr(ANode node, AttrSlot slot) {
		this.node = node;
		this.slot = slot;
	}
	public Object get() { return slot.get(node); }
	public void set(Object val) { return slot.set(node, val); }
}

public final class SpacePtr {
	public final ANode node;
	public final SpaceAttrSlot<ANode> slot;
	public SpacePtr(ANode node, SpaceAttrSlot<ANode> slot) {
		this.node = node;
		this.slot = slot;
	}

	@getter
	public int size()
		alias get$length
	{
		return slot.get(node).length;
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
	public final boolean         is_external; // not declared within the node (i.e., not listed in values())
	public final Class           clazz; // type of the fields
	public final TypeInfo        typeinfo; // type of the fields
	public final Object          defaultValue;
	
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
	}
	
	public final boolean isSemantic() {
		return this.parent_attr_slot == ANode.nodeattr$parent;
	}

	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
	public void clear(ANode parent) { this.set(parent, defaultValue); }
}

public final class ParentAttrSlot extends AttrSlot {
	public ParentAttrSlot(String name, boolean is_external, TypeInfo typeinfo) {
		super(name, null, false, is_external, typeinfo);
	}
	public void set(ANode parent, Object value) {
		throw new RuntimeException("@nodeParent '"+name+"' is not writeable"); 
	}
	public ANode get(ANode parent) {
		if (this == ANode.nodeattr$parent)
			return parent.parent();
		throw new RuntimeException("@nodeParent '"+name+"' is not readable"); 
	}
	public void clear(ANode parent) {
		if (this == ANode.nodeattr$parent)
			parent.callbackDetached(parent.p_parent, parent.p_slot);
		else
			throw new RuntimeException("@nodeParent '"+name+"' is not cleanable"); 
	}
}

public class ExtAttrSlot extends AttrSlot {
	public ExtAttrSlot(String name, ParentAttrSlot p_attr, boolean is_space, TypeInfo typeinfo) {
		super(name,p_attr,is_space,true,typeinfo);
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
}

public abstract class RefAttrSlot extends AttrSlot {
	public RefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, false, false, typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class AttAttrSlot extends AttrSlot {
	public AttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, ANode.nodeattr$parent, false, false, typeinfo);
		assert (this.is_attr);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class ExtRefAttrSlot extends ExtAttrSlot {
	public ExtRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, false, typeinfo);
	}
}

public abstract class ExtAttAttrSlot extends ExtAttrSlot {
	public ExtAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, ANode.nodeattr$parent, false, typeinfo);
		assert (this.is_attr);
	}
}

public abstract class SpaceAttrSlot<N extends ANode> extends AttrSlot {
	public SpaceAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo typeinfo) {
		super(name, p_attr, true, false, typeinfo);
	}
	// by default, set as extended data
	public void set(ANode parent, Object value) {
		parent.setExtData((N[])value, this);
	}
	// by default, get as extended data
	public N[] get(ANode parent) {
		Object value = parent.getExtData(this);
		if (value == null)
			return (N[])defaultValue;
		return (N[])value;
	}

	public final N[] getArray(ANode parent) {
		return this.get(parent);
	}

	public final int indexOf(ANode parent, ANode node) {
		N[] narr = get(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == node)
				return i;
		}
		return -1;
	}

	public final void detach(ANode parent, ANode old)
	{
		N[] narr = get(parent);
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
		N[] narr = get(parent);
		return narr[idx];
	}

	public abstract N set(ANode parent, int idx, N node);
	public abstract N add(ANode parent, N node);
	public abstract void del(ANode parent, int idx);
	public abstract void insert(ANode parent, int idx, N node);
	public abstract void copyFrom(ANode parent, N[] arr);
	public abstract void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc);
	public abstract void delAll(ANode parent);
	public abstract N[] delToArray(ANode parent);

}

public abstract class SpaceRefAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, null, typeinfo);
	}

	public final N set(ANode parent, int idx, N node) {
		N[] narr = (N[])get(parent).clone();
		narr[idx] = node;
		set(parent,narr);
		return node;
	}

	public final N add(ANode parent, N node) {
		N[] narr = get(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		set(parent,tmp);
		return node;
	}

	public final void del(ANode parent, int idx) {
		N[] narr = get(parent);
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		set(parent,tmp);
	}

	public final void insert(ANode parent, int idx, N node) {
		N[] narr = get(parent);
		int sz = narr.length;
		if (idx > sz) idx = sz;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		set(parent,tmp);
	}

	public final void copyFrom(ANode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n);
	}
	
	public final void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc) {
		foreach (N n; arr)
			add(parent, n);
	}
	
	public final void delAll(ANode parent) {
		N[] narr = get(parent);
		if (narr.length == 0)
			return;
		set(parent,(N[])defaultValue);
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0)
			set(parent,(N[])defaultValue);
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
		N[] narr = (N[])get(parent).clone();
		narr[idx].callbackDetached(parent, this);
		narr[idx] = node;
		set(parent,narr);
		node.callbackAttached(parent, this);
		return node;
	}

	public final N add(ANode parent, N node) {
		assert(!this.isSemantic() || !node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = get(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		set(parent,tmp);
		node.callbackAttached(parent, this);
		return node;
	}

	public final void del(ANode parent, int idx) {
		N[] narr = get(parent);
		narr[idx].callbackDetached(parent, this);
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		set(parent,tmp);
	}

	public final void insert(ANode parent, int idx, N node) {
		assert(!this.isSemantic() || !node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = get(parent);
		int sz = narr.length;
		if (idx > sz) idx = sz;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		set(parent,tmp);
		node.callbackAttached(parent, this);
	}

	public final void copyFrom(ANode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n.ncopy());
	}
	
	public final void copyFrom(ANode parent, N[] arr, ANode.CopyContext cc) {
		foreach (N n; arr)
			add(parent, n.ncopy(cc));
	}
	
	public final void delAll(ANode parent) {
		N[] narr = get(parent);
		if (narr.length == 0)
			return;
		set(parent,(N[])defaultValue);
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached(parent, this);
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0) {
			set(parent,(N[])defaultValue);
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
		if (!Kiev.run_batch) {
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
		if (!Kiev.run_batch) {
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
		if (!Kiev.run_batch)
			this.nodes = new ASTNode[64];
	}
	
	public void add(ASTNode node) {
		if (Kiev.run_batch)
			return;
		if (size >= nodes.length) {
			ASTNode[] tmp = new ASTNode[size*2];
			System.arraycopy(nodes,0,tmp,0,size);
			nodes = tmp;
		}
		nodes[size++] = node;
	}
}


