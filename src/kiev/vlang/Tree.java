package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import kiev.be.java15.JType;
import kiev.be.java15.JNode;

import static kiev.stdlib.Debug.*;
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
}
// syntax-tree reference field
public @interface ref {
	boolean copyable() default true;
}


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
	
	public final String   name; // field (property) name
	public final boolean  is_attr; // @att or @ref
	public final boolean  is_space; // if Node[]
	public final Class    clazz; // type of the fields
	public final TypeInfo typeinfo; // type of the fields
	public final Object   defaultValue;
	
	public AttrSlot(String name, boolean is_attr, boolean is_space, Class clazz) {
		assert (name.intern() == name);
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
		this.clazz = clazz;
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
	
	public AttrSlot(String name, boolean is_attr, boolean is_space, TypeInfo typeinfo) {
		assert (name.intern() == name);
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
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
	
	public boolean isMeta() { return false; }
	public boolean isExtData() { return false; }
	public boolean isTmpData() { return false; }

	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
	public void clear(ANode parent) { this.set(parent, defaultValue); }
}

public class ExtAttrSlot extends AttrSlot {
	public ExtAttrSlot(String name, boolean is_attr, boolean is_space, TypeInfo typeinfo) {
		super(name,is_attr,is_space,typeinfo);
	}
	public boolean isExtData() { return true; }

	public void set(ANode parent, Object value) {
		parent.addExtData((ANode)value, this);
	}
	public Object get(ANode parent) {
		return parent.getExtData(this);
	}
	public void clear(ANode parent) {
		return parent.delExtData(this);
	}
}

public class TmpAttrSlot extends AttrSlot {
	public TmpAttrSlot(String name, boolean is_attr, boolean is_space, TypeInfo typeinfo) {
		super(name,is_attr,is_space,typeinfo);
	}
	public boolean isTmpData() { return true; }

	public void set(ANode parent, Object value) {
		parent.addTmpData((ANode)value, this);
	}
	public Object get(ANode parent) {
		return parent.getTmpData(this);
	}
	public void clear(ANode parent) {
		return parent.delTmpData(this);
	}
}

public class MetaAttrSlot extends ExtAttrSlot {
	public MetaAttrSlot(String name, Class clazz) {
		super(name,true,false,TypeInfo.newTypeInfo(clazz,null));
	}
	public boolean isMeta() { return true; }
}

public abstract class RefAttrSlot extends AttrSlot {
	public RefAttrSlot(String name, Class clazz) {
		super(name, false, false, clazz);
	}
	public RefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, false, false, typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class AttAttrSlot extends AttrSlot {
	public AttAttrSlot(String name, Class clazz) {
		super(name, true, false, clazz);
	}
	public AttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, true, false, typeinfo);
	}
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class SpaceAttrSlot<N extends ANode> extends AttrSlot {
	public SpaceAttrSlot(String name, boolean is_attr, Class clazz) {
		super(name, is_attr, true, clazz);
	}
	public SpaceAttrSlot(String name, boolean is_attr, TypeInfo typeinfo) {
		super(name, is_attr, true, typeinfo);
	}
	public abstract N[] get(ANode parent);
	public abstract void set(ANode parent, Object narr);

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
	public abstract void delAll(ANode parent);
	public abstract N[] delToArray(ANode parent);

}

public class SpaceRefAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, Class clazz) {
		super(name, false, clazz);
	}
	public SpaceRefAttrSlot(String name, TypeInfo typeinfo) {
		super(name, false, typeinfo);
	}
	
	public final N set(ANode parent, int idx, N node) {
		parent.open();
		N[] narr = (N[])get(parent).clone();
		narr[idx] = node;
		set(parent,narr);
		return node;
	}

	public final N add(ANode parent, N node) {
		parent.open();
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
		parent.open();
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
		parent.open();
		N[] narr = get(parent);
		int sz = narr.length;
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
		parent.open();
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0) {
			parent.open();
			set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		}
		return narr;
	}
}

public class SpaceAttAttrSlot<N extends ANode> extends SpaceAttrSlot<N> {
	public SpaceAttAttrSlot(String name, Class clazz) {
		super(name, true, clazz);
	}
	public SpaceAttAttrSlot(String name, TypeInfo typeinfo) {
		super(name, true, typeinfo);
	}
	
	public final N set(ANode parent, int idx, N node) {
		assert(!node.isAttached());
		parent.open();
		N[] narr = (N[])get(parent).clone();
		ANode old = narr[idx];
		old.callbackDetached();
		narr[idx] = node;
		set(parent,narr);
		ListAttachInfo prv = null;
		ListAttachInfo nxt = null;
		if (idx > 0) prv = (ListAttachInfo)narr[idx-1].getAttachInfo();
		if (idx+1 < narr.length) nxt = (ListAttachInfo)narr[idx+1].getAttachInfo();
		node.callbackAttached(new ListAttachInfo(node, parent, this, prv, nxt));
		return node;
	}

	public final N add(ANode parent, N node) {
		assert(!node.isAttached());
		assert(indexOf(parent,node) < 0);
		parent.open();
		N[] narr = get(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		set(parent,tmp);
		ListAttachInfo prv = null;
		if (sz > 0) prv = (ListAttachInfo)tmp[sz-1].getAttachInfo();
		node.callbackAttached(new ListAttachInfo(node, parent, this, prv, null));
		return node;
	}

	public final void del(ANode parent, int idx) {
		parent.open();
		N[] narr = get(parent);
		ANode old = narr[idx];
		old.callbackDetached();
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
		parent.open();
		N[] narr = get(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		set(parent,tmp);
		ListAttachInfo prv = null;
		ListAttachInfo nxt = null;
		if (idx > 0) prv = (ListAttachInfo)tmp[idx-1].getAttachInfo();
		if (idx+1 < tmp.length) nxt = (ListAttachInfo)tmp[idx+1].getAttachInfo();
		node.callbackAttached(new ListAttachInfo(node, parent, this, prv, nxt));
	}

	public final void copyFrom(ANode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n.ncopy());
	}
	
	public final void delAll(ANode parent) {
		N[] narr = get(parent);
		if (narr.length == 0)
			return;
		parent.open();
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached();
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		if (narr.length > 0) {
			parent.open();
			set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
			for (int i=0; i < narr.length; i++)
				narr[i].callbackDetached();
		}
		return narr;
	}
}

public class SpaceRefDataAttrSlot<N extends ANode> extends SpaceRefAttrSlot<N> {
	private final boolean is_ext;
	
	public SpaceRefDataAttrSlot(String name, boolean is_ext, TypeInfo typeinfo) {
		super(name, typeinfo);
		this.is_ext = is_ext;
	}
	public boolean isExtData() { return is_ext; }
	public boolean isTmpData() { return !is_ext; }

	public void set(ANode parent, Object value) {
		if (is_ext)
			parent.addExtData((N[])value, this);
		else
			parent.addTmpData((N[])value, this);
	}
	public N[] get(ANode parent) {
		Object value = is_ext ? parent.getExtData(this) : parent.getTmpData(this);
		if (value == null)
			return (N[])defaultValue;
		return (N[])value;
	}
	public void clear(ANode parent) {
		if (is_ext)
			return parent.delExtData(this);
		else
			return parent.delTmpData(this);
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
		ASTNode[] nodes = this.nodes;
		int n = this.size;
		for (int i=0; i < n; i++)
			nodes[i].locked = true;
		currentTransaction = null;
	}

	public void leave() {
		if (--recursion_counter <= 0)
			close();
	}

	public void rollback(boolean save_next) {
		ASTNode[] nodes = this.nodes;
		int n = this.size;
		for (int i=0; i < n; i++)
			nodes[i].rollback(save_next);
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
		this.nodes = new ASTNode[64];
	}
	
	public void add(ASTNode node) {
		if (size >= nodes.length) {
			ASTNode[] tmp = new ASTNode[size*2];
			System.arraycopy(nodes,0,tmp,0,size);
			nodes = tmp;
		}
		nodes[size++] = node;
	}
}




@unerasable
public final class NArr<N extends ANode> {

    private final ANode		 	$parent_impl;
	private final AttrSlot		$pslot;
	public        N[]			$nodes;
	
	public NArr(ANode parent_impl, AttrSlot pslot) {}
	
	public ANode getParent() { return null; }
	
	public AttrSlot getPSlot() { return null; }
	
	@getter
	public int size()
		alias length
		alias get$length
	{
		return 0;
	}

	public final N get(int idx)
		alias at
		alias xfy operator []
	{ return null; }
	
	public N set(int idx, N node)
		alias lfy operator []
		require { node != null; }
	{ return null; }

	public N add(N node)
		alias append
		alias lfy operator +=
		require { node != null; }
	{ return null; }

	public void addAll(N[] arr)
		alias appendAll
	{
	}

	public void insert(int idx, N node) {
	}

	public void detach(ANode old)
	{
	}
	
	public void del(int idx)
	{
	}

	public void delAll() {
	}
	
	public void copyFrom(NArr<N> arr) {
	}
	
	public void copyFrom(N[] arr) {
	}
	
	public boolean contains(ANode node) {
		return false;
	}
	
	public int indexOf(ANode node) {
		return -1;
	}
	
	public N[] getArray() { return null; }

	public N[] delToArray() { return null; }

	public Type[] toTypeArray() alias fy operator $cast { return null; }

	public JType[] toJTypeArray() alias fy operator $cast { return null; }

	public Enumeration<N> elements() {
		return new Enumeration<N>() {
			int current;
			public boolean hasMoreElements() { return current < NArr.this.size(); }
			public N nextElement() {
				if ( current < size() ) return NArr.this[current++];
				throw new NoSuchElementException(Integer.toString(NArr.this.size()));
			}
		};
	}

}


