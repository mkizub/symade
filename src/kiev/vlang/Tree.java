package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import kiev.be.java15.JType;
import kiev.be.java15.JNode;

import static kiev.stdlib.Debug.*;
import java.lang.annotation.*;

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

public class AttrSlot {
	public static final AttrSlot[] emptyArray = new AttrSlot[0];
	
	public final String  name; // field (property) name
	public final boolean is_attr; // @att or @ref
	public final boolean is_space; // if NArr<Node>
	public final Class   clazz; // type of the fields
	
	public AttrSlot(String name, boolean is_attr, boolean is_space, Class clazz) {
		assert (name.intern() == name);
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
		this.clazz = clazz;
	}
	
	public boolean isMeta() { return false; }
	public boolean isData() { return false; }

	public void set(ASTNode node, Object value) {
		node.setVal(name, value);
	}
	public Object get(ASTNode node) {
		return node.getVal(name);
	}
}

public class DataAttrSlot extends AttrSlot {
	public DataAttrSlot(String name, boolean is_attr, Class clazz) {
		super(name,is_attr,false,clazz);
	}
	public boolean isMeta() { return false; }
	public boolean isData() { return true; }
}

public class MetaAttrSlot extends AttrSlot {
	public MetaAttrSlot(String name, Class clazz) {
		super(name,true,false,clazz);
	}
	public boolean isMeta() { return true; }
	public boolean isData() { return true; }

	public abstract void set(ASTNode node, Object value);
	public abstract Object get(ASTNode node);
}

public abstract class SpaceAttrSlot<N extends ASTNode> extends AttrSlot {
	public SpaceAttrSlot(String name, boolean is_attr, Class clazz) {
		super(name, is_attr, true, clazz);
	}
	protected N[] getArr(ASTNode parent) { return ((NArr<N>)parent.getVal(this.name)).$nodes; }
	protected void setArr(ASTNode parent, N[] narr) { ((NArr<N>)parent.getVal(this.name)).$nodes = narr; }

	public final N[] getArray(ASTNode parent) {
		return this.getArr(parent);
	}

	public final int indexOf(ASTNode parent, ASTNode node) {
		N[] narr = getArr(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == node)
				return i;
		}
		return -1;
	}

	public final void detach(ASTNode parent, ASTNode old)
	{
		N[] narr = getArr(parent);
		int sz = narr.length;
		for (int i=0; i < sz; i++) {
			if (narr[i] == old) {
				this.del(parent,i);
				return;
			}
		}
		throw new RuntimeException("Not found node");
	}
	
	public final void addAll(ASTNode parent, N[] arr) {
		for (int i=0; i < arr.length; i++)
			add(parent, arr[i]);
	}

	public abstract N set(ASTNode parent, int idx, N node);
	public abstract N add(ASTNode parent, N node);
	public abstract void del(ASTNode parent, int idx);
	public abstract void insert(ASTNode parent, int idx, N node);
	public abstract void copyFrom(ASTNode parent, N[] arr);
	public abstract void delAll(ASTNode parent);
	public abstract N[] delToArray(ASTNode parent);

}

public class SpaceRefAttrSlot<N extends ASTNode> extends SpaceAttrSlot<N> {
	public SpaceRefAttrSlot(String name, Class clazz) {
		super(name, false, clazz);
	}
	
	public N set(ASTNode parent, int idx, N node) {
		N[] narr = getArr(parent);
		narr[idx] = node;
		return node;
	}

	public N add(ASTNode parent, N node) {
		N[] narr = getArr(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		setArr(parent,tmp);
		return node;
	}

	public void del(ASTNode parent, int idx) {
		N[] narr = getArr(parent);
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		setArr(parent,tmp);
	}

	public void insert(ASTNode parent, int idx, N node) {
		N[] narr = getArr(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		setArr(parent,tmp);
	}

	public void copyFrom(ASTNode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n);
	}
	
	public void delAll(ASTNode parent) {
		N[] narr = getArr(parent);
		if (narr.length == 0)
			return;
		setArr(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
	}
	
	public N[] delToArray(ASTNode parent) {
		N[] narr = getArr(parent);
		setArr(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		return narr;
	}
}

public class SpaceAttAttrSlot<N extends ASTNode> extends SpaceAttrSlot<N> {
	public SpaceAttAttrSlot(String name, Class clazz) {
		super(name, true, clazz);
	}
	
	public N set(ASTNode parent, int idx, N node) {
		assert(!node.isAttached());
		N[] narr = getArr(parent);
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

	public N add(ASTNode parent, N node) {
		assert(!node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = getArr(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		for (int i=0; i < sz; i++)
			tmp[i] = narr[i];
		tmp[sz] = node;
		setArr(parent,tmp);
		ListAttachInfo prv = null;
		if (sz > 0) prv = (ListAttachInfo)tmp[sz-1].getAttachInfo();
		node.callbackAttached(new ListAttachInfo(node, parent, this, prv, null));
		return node;
	}

	public void del(ASTNode parent, int idx) {
		N[] narr = getArr(parent);
		ASTNode old = narr[idx];
		old.callbackDetached();
		int sz = narr.length-1;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz); //new N[sz];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		for (; i < sz; i++)
			tmp[i] = narr[i+1];
		setArr(parent,tmp);
	}

	public void insert(ASTNode parent, int idx, N node) {
		assert(!node.isAttached());
		assert(indexOf(parent,node) < 0);
		N[] narr = getArr(parent);
		int sz = narr.length;
		N[] tmp = (N[])java.lang.reflect.Array.newInstance(clazz,sz+1); //new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = narr[i];
		tmp[idx] = node;
		for (; i < sz; i++)
			tmp[i+1] = narr[i];
		setArr(parent,tmp);
		ListAttachInfo prv = null;
		ListAttachInfo nxt = null;
		if (idx > 0) prv = (ListAttachInfo)tmp[idx-1].getAttachInfo();
		if (idx+1 < tmp.length) nxt = (ListAttachInfo)tmp[idx+1].getAttachInfo();
		node.callbackAttached(new ListAttachInfo(node, parent, this, prv, nxt));
	}

	public void copyFrom(ASTNode parent, N[] arr) {
		foreach (N n; arr)
			add(parent, n.ncopy());
	}
	
	public void delAll(ASTNode parent) {
		N[] narr = getArr(parent);
		if (narr.length == 0)
			return;
		setArr(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached();
	}
	
	public N[] delToArray(ASTNode parent) {
		N[] narr = getArr(parent);
		setArr(parent,(N[])java.lang.reflect.Array.newInstance(clazz,0));
		for (int i=0; i < narr.length; i++)
			narr[i].callbackDetached();
		return narr;
	}
}

@unerasable
public final class NArr<N extends ASTNode> {

    private final ASTNode	 	$parent_impl;
	private final AttrSlot		$pslot;
	public        N[]			$nodes;
	
	public NArr(ASTNode parent_impl, AttrSlot pslot) {
		this.$parent_impl = parent_impl;
		this.$pslot = pslot;
		this.$nodes = new N[0];
		if (parent_impl == null)
			assert (pslot == null || !pslot.is_attr);
	}
	
	public ASTNode getParent() {
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

	@unerasable
	public <J extends JNode> JArr<J> toJArr() alias operator(210,fy,$cast) {
		return new JArr<J>();
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

	@unerasable
	public final class JArr<J extends JNode> {

		public NArr<N> getNArr() { return NArr.this; }
		
		public JNode getParent() {
			return (J)NArr.this.$parent_impl;
		}
		
		public AttrSlot getPSlot() {
			return getNArr().$pslot;
		}
		
		@getter
		public int size()
			alias length
			alias get$length
		{
			return getNArr().size();
		}
	
		public final J get(int idx)
			alias at
			alias operator(210,xfy,[])
		{
			return (J)getNArr().get(idx);
		}
		
		public J set(int idx, J node)
			alias operator(210,lfy,[])
			require { node != null; }
		{
			getNArr().set(idx, (N)node);
			return node;
		}
	
		public J add(J node)
			alias append
			alias operator(5, lfy, +=)
			require { node != null; }
		{
			getNArr().add((N)node);
			return node;
		}
	
		public void addAll(J[] arr)
			alias appendAll
		{
			foreach(J n; arr) add(n);
		}
	
		public J insert(int idx, J node)
			require { node != null; }
		{
			getNArr().insert(idx,(N)node);
			return node;
		}
	
		public void detach(J old) { getNArr().detach((N)old); }
		public void del(int idx) { getNArr().del(idx); }
		public boolean contains(J node) { return getNArr().contains((N)node); }
		public J[] toArray() {
			int sz = getNArr().$nodes.length;
			J[] arr = new J[sz];
			for (int i=0; i < sz; i++)
				arr[i] = this[i];
			return arr;
		}
	
		public Enumeration<J> elements() {
			return new Enumeration<J>() {
				int current;
				public boolean hasMoreElements() { return current < JArr.this.size(); }
				public J nextElement() {
					if ( current < size() ) return JArr.this[current++];
					throw new NoSuchElementException(Integer.toString(JArr.this.size()));
				}
			};
		}
	}
}


