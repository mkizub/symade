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
import kiev.be.java15.JNode;

import java.lang.annotation.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface dflow {
	String in() default "";
	String tru() default "";
	String fls() default "";
	String out() default "";
	String jmp() default "";
	String seq() default "";
	String[] links() default {};
}

// syntax-tree node
public @interface node {
	String name() default "";
	boolean copyable() default true;
}
// syntax-tree node view
public @interface nodeview {}
// syntax-tree node set (ASTNode sub-classes)
public @interface nodeset {}
// syntax-tree node implementation
public @interface nodeimpl {}
// syntax-tree attribute field
public @interface att {
	boolean copyable() default true;
	boolean ext_data() default false;
}
// syntax-tree reference field
public @interface ref {
	boolean copyable() default true;
	boolean ext_data() default false;
}


public final class AttrPtr {
	public final ANode node;
	public final AttrSlot slot;
	public AttrPtr(ANode node, AttrSlot slot) {
		this.node = node;
		this.slot = slot;
	}
	public Object get() { return slot.get(ANode.getVersion(node)); }
	public void set(Object val) { return slot.set(ANode.getVersion(node), val); }
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
		return slot.get(ANode.getVersion(node)).length;
	}

	public ANode get(int idx)
		alias xfy operator []
	{
		return slot.get(ANode.getVersion(node), idx);
	}

	public ANode set(int idx, ANode val)
		alias lfy operator []
	{
		return slot.set(ANode.getVersion(node), idx, val);
	}

	public ANode add(ANode val)
		alias append
		alias lfy operator +=
	{
		return slot.add(ANode.getVersion(node), val);
	}
}

public abstract class AttrSlot {
	public static final AttrSlot[] emptyArray = new AttrSlot[0];
	
	public final String   name; // field (property) name
	public final boolean  is_attr; // @att or @ref
	public final boolean  is_space; // if Node[]
	public final boolean  is_external; // not declared within the node (i.e., not listed in values())
	public final Class    clazz; // type of the fields
	public final TypeInfo typeinfo; // type of the fields
	public final Object   defaultValue;
	
	public AttrSlot(String name, boolean is_attr, boolean is_space, TypeInfo typeinfo) {
		this(name, is_attr, is_space, false, typeinfo);
	}
	public AttrSlot(String name, boolean is_attr, boolean is_space, boolean is_external, TypeInfo typeinfo) {
		assert (name.intern() == name);
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
		this.is_external = is_external;
		this.clazz = typeinfo.clazz;
		this.typeinfo = typeinfo;
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
	
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
	public void clear(ANode parent) { this.set(parent, defaultValue); }
}

public class ExtAttrSlot extends AttrSlot {
	public ExtAttrSlot(String name, boolean is_attr, boolean is_space, TypeInfo typeinfo) {
		super(name,is_attr,is_space,typeinfo);
	}
	public ExtAttrSlot(String name, boolean is_attr, boolean is_space, boolean is_external, TypeInfo typeinfo) {
		super(name,is_attr,is_space,is_external,typeinfo);
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
		super(name, false, false, typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class AttAttrSlot extends AttrSlot {
	public final AttachInfo simpleAttachInfo;
	public AttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, true, false, typeinfo);
		this.simpleAttachInfo = new AttachInfo(this);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class ExtAttAttrSlot extends ExtAttrSlot {
	public ExtAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, true, false, typeinfo);
	}
}

public abstract class ExtRefAttrSlot extends ExtAttrSlot {
	public ExtRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, false, false, typeinfo);
	}
}

public abstract class SpaceAttrSlot<N extends ANode> extends AttrSlot {
	public SpaceAttrSlot(String name, boolean is_attr, TypeInfo typeinfo) {
		super(name, is_attr, true, typeinfo);
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
		node = ANode.getVersion(node);
		N[] narr = get(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (ANode.getVersion(narr[i]) == node)
				return i;
		}
		return -1;
	}

	public final void detach(ANode parent, ANode old)
	{
		old = ANode.getVersion(old);
		N[] narr = get(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (ANode.getVersion(narr[i]) == old) {
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
		return ANode.getVersion(narr[idx]);
	}

	public abstract N set(ANode parent, int idx, N node);
	public abstract N add(ANode parent, N node);
	public abstract void del(ANode parent, int idx);
	public abstract void insert(ANode parent, int idx, N node);
	public abstract void copyFrom(ANode parent, N[] arr);
	public abstract void delAll(ANode parent);
	public abstract N[] delToArray(ANode parent);

}

public abstract class SpaceRefAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, false, typeinfo);
	}

	public final N set(ANode parent, int idx, N node) {
		parent = parent.open();
		N[] narr = (N[])get(parent).clone();
		narr[idx] = node;
		set(parent,narr);
		return node;
	}

	public final N add(ANode parent, N node) {
		parent = parent.open();
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
		parent = parent.open();
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
		parent = parent.open();
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
	
	public final void delAll(ANode parent) {
		N[] narr = get(parent);
		if (narr.length == 0)
			return;
		parent = parent.open();
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0) {
			parent = parent.open();
			set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		}
		return narr;
	}
}

public abstract class SpaceAttAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, true, typeinfo);
	}

	public final N set(ANode parent, int idx, N node) {
		assert(!node.isAttached());
		parent = parent.open();
		N[] narr = (N[])get(parent).clone();
		ANode.getVersion(narr[idx]).callbackDetached();
		narr[idx] = node;
		set(parent,narr);
		ANode prv = null;
		ANode nxt = null;
		if (idx > 0) prv = narr[idx-1];
		if (idx+1 < narr.length) nxt = narr[idx+1];
		node.callbackAttachedToSpace(parent, this, prv, nxt);
		return node;
	}

	public final N add(ANode parent, N node) {
		assert(!node.isAttached());
		assert(indexOf(parent,node) < 0);
		parent = parent.open();
		N[] narr = get(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		set(parent,tmp);
		ANode prv = null;
		if (sz > 0) prv = tmp[sz-1];
		node.callbackAttachedToSpace(parent, this, prv, null);
		return node;
	}

	public final void del(ANode parent, int idx) {
		parent = parent.open();
		N[] narr = get(parent);
		ANode.getVersion(narr[idx]).callbackDetached();
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
		assert(!node.isAttached());
		assert(indexOf(parent,node) < 0);
		parent = parent.open();
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
		ANode prv = null;
		ANode nxt = null;
		if (idx > 0) prv = tmp[idx-1];
		if (idx+1 < tmp.length) nxt = tmp[idx+1];
		node.callbackAttachedToSpace(parent, this, prv, nxt);
	}

	public final void copyFrom(ANode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n.ncopy());
	}
	
	public final void delAll(ANode parent) {
		N[] narr = get(parent);
		if (narr.length == 0)
			return;
		parent = parent.open();
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			ANode.getVersion(narr[i]).callbackDetached();
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0) {
			parent = parent.open();
			set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
			for (int i=0; i < narr.length; i++)
				ANode.getVersion(narr[i]).callbackDetached();
		}
		return narr;
	}
}

public final class Transaction {
	private static int currentVersion;
	private static Transaction currentTransaction;

	public static Transaction open() {
		assert (currentTransaction == null);
		currentTransaction = new Transaction();
		return currentTransaction;
	}

	public static Transaction enter(Transaction tr) {
		if (tr == null)
			return open();
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
				nodes[i].locked = true;
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
				nodes[i].rollback(save_next);
		}
		if (currentTransaction == this)
			currentTransaction = null;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}

	public int version;
	private int recursion_counter;
	private int size;
	private ASTNode[] nodes;

	private Transaction() {
		this.version = ++currentVersion;
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


