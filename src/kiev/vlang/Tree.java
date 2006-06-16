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
	public final SpaceAttrSlot<ASTNode> slot;
	public SpacePtr(ANode node, SpaceAttrSlot<ASTNode> slot) {
		this.node = node;
		this.slot = slot;
	}

	public ASTNode get(int idx)
		alias operator(210,xfy,[])
	{
		return slot.get(node, idx);
	}

	public ASTNode set(int idx, ASTNode val)
		alias operator(210,lfy,[])
	{
		return slot.set(node, idx, val);
	}

	public ASTNode add(ASTNode val)
		alias append
		alias operator(5, lfy, +=)
	{
		return slot.add(node, val);
	}
}

public abstract class AttrSlot {
	public static final AttrSlot[] emptyArray = new AttrSlot[0];
	
	public final String  name; // field (property) name
	public final boolean is_attr; // @att or @ref
	public final boolean is_space; // if Node[]
	public final Class   clazz; // type of the fields
	public final Object  defaultValue;
	
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
	
	public boolean isMeta() { return false; }
	public boolean isData() { return false; }

	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
	public void clear(ANode parent) { this.set(parent, defaultValue); }
}

public class DataAttrSlot extends AttrSlot {
	public DataAttrSlot(String name, boolean is_attr, boolean is_space, Class clazz) {
		super(name,is_attr,is_space,clazz);
	}
	public boolean isMeta() { return false; }
	public boolean isData() { return true; }

	public void set(ANode parent, Object value) {
		parent.addNodeData((ANode)value, this);
	}
	public Object get(ANode parent) {
		return parent.getNodeData(this);
	}
	public void clear(ANode parent) {
		return parent.delNodeData(this);
	}
}

public class MetaAttrSlot extends DataAttrSlot {
	public MetaAttrSlot(String name, Class clazz) {
		super(name,true,false,clazz);
	}
	public boolean isMeta() { return true; }
	public boolean isData() { return true; }
}

public abstract class RefAttrSlot extends AttrSlot {
	public RefAttrSlot(String name, Class clazz) {
		super(name, false, false, clazz);
	}

	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class AttAttrSlot extends AttrSlot {
	public AttAttrSlot(String name, Class clazz) {
		super(name, true, false, clazz);
	}
	
	public abstract void set(ANode parent, Object value);
	public abstract Object get(ANode parent);
}

public abstract class SpaceAttrSlot<N extends ASTNode> extends AttrSlot {
	public SpaceAttrSlot(String name, boolean is_attr, Class clazz) {
		super(name, is_attr, true, clazz);
	}
	public abstract N[] get(ANode parent);
	public abstract void set(ANode parent, Object narr);

	public final N[] getArray(ANode parent) {
		return this.get(parent);
	}

	public final int indexOf(ANode parent, ASTNode node) {
		N[] narr = get(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == node)
				return i;
		}
		return -1;
	}

	public final void detach(ANode parent, ASTNode old)
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

public class SpaceRefAttrSlot<N extends ASTNode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, Class clazz) {
		super(name, false, clazz);
	}
	
	public final N set(ANode parent, int idx, N node) {
		N[] narr = get(parent);
		narr[idx] = node;
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
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		return narr;
	}
}
public class SpaceRefDataAttrSlot<N extends ASTNode> extends SpaceRefAttrSlot<N> {
	public SpaceRefDataAttrSlot(String name, Class clazz) {
		super(name, clazz);
	}
	public boolean isData() { return true; }

	public void set(ANode parent, Object value) {
		parent.addNodeData(value, this);
	}
	public N[] get(ANode parent) {
		Object value = parent.getNodeData(this);
		if (value == null)
			return (N[])defaultValue;
		return (N[])value;
	}
	public void clear(ANode parent) {
		return parent.delNodeData(this);
	}
}

public class SpaceAttAttrSlot<N extends ASTNode> extends SpaceAttrSlot<N> {
	public SpaceAttAttrSlot(String name, Class clazz) {
		super(name, true, clazz);
	}
	
	public final N set(ANode parent, int idx, N node) {
		assert(!node.isAttached());
		N[] narr = get(parent);
		ASTNode old = narr[idx];
		old.callbackDetached();
		narr[idx] = node;
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
		N[] narr = get(parent);
		ASTNode old = narr[idx];
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
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached();
	}
	
	public final N[] delToArray(ANode parent) {
		N[] narr = get(parent);
		set(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached();
		return narr;
	}
}
public class SpaceAttDataAttrSlot<N extends ASTNode> extends SpaceAttAttrSlot<N> {
	public SpaceAttDataAttrSlot(String name, Class clazz) {
		super(name, clazz);
	}
	public boolean isData() { return true; }

	public void set(ANode parent, Object value) {
		parent.addNodeData(value, this);
	}
	public N[] get(ANode parent) {
		Object value = parent.getNodeData(this);
		if (value == null)
			return (N[])defaultValue;
		return (N[])value;
	}
	public void clear(ANode parent) {
		return parent.delNodeData(this);
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

	public static Transaction current() {
		assert (currentTransaction != null);
		return currentTransaction;
	}

	public static void close() {
		assert (currentTransaction != null);
		ASTNode[] nodes = currentTransaction.nodes;
		int n = currentTransaction.size;
		for (int i=0; i < n; i++)
			nodes[i].locked = true;
		currentTransaction = null;
	}

	public int version;
	private int size;
	private ASTNode[] nodes;

	private Transaction() {
		version = ++currentVersion;
		nodes = new ASTNode[64];
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
public final class NArr<N extends ASTNode> {

    private final ANode		 	$parent_impl;
	private final AttrSlot		$pslot;
	public        N[]			$nodes;
	
	public NArr(ANode parent_impl, AttrSlot pslot) {
		this.$parent_impl = parent_impl;
		this.$pslot = pslot;
		this.$nodes = new N[0];
		if (parent_impl == null)
			assert (pslot == null || !pslot.is_attr);
	}
	
	public ANode getParent() {
		return $parent_impl;
	}
	
	public AttrSlot getPSlot() {
		return $pslot;
	}
	
	@getter
	public int size()
		alias length
		alias get$length
	{
		return $nodes.length;
	}

	public final N get(int idx)
		alias at
		alias operator(210,xfy,[])
	{
		return $nodes[idx];
	}
	
	public N set(int idx, N node)
		alias operator(210,lfy,[])
		require { node != null; }
	{
		// for compatibility with kiev-0.4b
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		assert(!is_attr || !node.isAttached());
		if (is_attr) {
			ASTNode old = $nodes[idx];
			old.callbackDetached();
		}
		$nodes[idx] = node;
		if (is_attr) {
			ListAttachInfo prv = null;
			ListAttachInfo nxt = null;
			if (idx > 0) prv = (ListAttachInfo)$nodes[idx-1].getAttachInfo();
			if (idx+1 < size()) nxt = (ListAttachInfo)$nodes[idx+1].getAttachInfo();
			node.callbackAttached(new ListAttachInfo(node, $parent_impl, $pslot, prv, nxt));
		}
		return node;
	}

	public N add(N node)
		alias append
		alias operator(5, lfy, +=)
		require { node != null; }
	{
		// for compatibility with kiev-0.4b
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		assert(!is_attr || !node.isAttached());
		if (is_attr)
			assert(indexOf(node) < 0);
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		if (is_attr) {
			ListAttachInfo prv = null;
			if (sz > 0) prv = (ListAttachInfo)$nodes[sz-1].getAttachInfo();
			node.callbackAttached(new ListAttachInfo(node, $parent_impl, $pslot, prv, null));
		}
		return node;
	}

	public void addAll(N[] arr)
		alias appendAll
	{
		// for compatibility with kiev-0.4b
		foreach(N n; arr) add(n);
	}

	public void insert(int idx, N node) {
		// for compatibility with kiev-0.4b
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		assert(!is_attr || !node.isAttached());
		if (is_attr)
			assert(indexOf(node) < 0);
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = $nodes[i];
		$nodes = tmp;
		if (is_attr) {
			ListAttachInfo prv = null;
			ListAttachInfo nxt = null;
			if (idx > 0) prv = (ListAttachInfo)$nodes[idx-1].getAttachInfo();
			if (idx+1 < size()) nxt = (ListAttachInfo)$nodes[idx+1].getAttachInfo();
			node.callbackAttached(new ListAttachInfo(node, $parent_impl, $pslot, prv, nxt));
		}
	}

	public void detach(ASTNode old)
	{
		// for compatibility with kiev-0.4b
		int sz = $nodes.length;
		for (int i=0; i < sz; i++) {
			if ($nodes[i] == old) {
				this.del(i);
				return;
			}
		}
		throw new RuntimeException("Not found node");
	}
	
	public void del(int idx)
	{
		// for compatibility with kiev-0.4b
		ASTNode old = $nodes[idx];
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		if (is_attr)
			old.callbackDetached();
		int sz = $nodes.length-1;
		N[] tmp = new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (; i < sz; i++)
			tmp[i] = $nodes[i+1];
		$nodes = tmp;
	}

	public void delAll() {
		// for compatibility with kiev-0.4b
		if (this.$nodes.length == 0)
			return;
		if ($pslot != null && $pslot.is_attr) {
			foreach (N node; $nodes)
				node.callbackDetached();
		}
		this.$nodes = new N[0];
	};
	
	public void copyFrom(NArr<N> arr) {
		// for compatibility with kiev-0.4b
		copyFrom(arr.getArray());
	}
	
	public void copyFrom(N[] arr) {
		// for compatibility with kiev-0.4b
		if ($pslot != null && $pslot.is_attr) {
			foreach (N n; arr)
				append(n.ncopy());
		} else {
			foreach (N n; arr)
				append(n);
		}
	}
	
	public boolean contains(ASTNode node) {
		for (int i=0; i < $nodes.length; i++) {
			if ($nodes[i].equals(node))
				return true;
		}
		return false;
	}
	
	public int indexOf(ASTNode node) {
		// for compatibility with kiev-0.4b
		int sz = $nodes.length;
		for (int i=0; i < sz; i++) {
			if ($nodes[i] == node)
				return i;
		}
		return -1;
	}
	
	public N[] getArray() {
		return $nodes;
	}

	public N[] delToArray() {
		// for compatibility with kiev-0.4b
		int sz = $nodes.length;
		N[] arr = $nodes;
		$nodes = new N[0];
		for (int i=0; i < sz; i++) {
			N node = arr[i];
			node.callbackDetached();
		}
		return arr;
	}

	public Type[] toTypeArray() alias operator(210,fy,$cast) {
		int sz = $nodes.length;
		Type[] arr = new Type[sz];
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getType();
		return arr;
	}

	public JType[] toJTypeArray() alias operator(210,fy,$cast) {
		int sz = $nodes.length;
		JType[] arr = new JType[sz];
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getType().getJType();
		return arr;
	}

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


