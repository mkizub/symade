package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;

import kiev.be.java.JNodeView;

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
// syntax-tree attribute field
public @interface att {
	boolean copyable() default true;
}
// syntax-tree reference field
public @interface ref {
	boolean copyable() default true;
}

public final class AttrSlot {
	public final String  name; // field (property) name
	public final boolean is_attr; // @att or @ref
	public final boolean is_space; // if NArr<Node>
	public final Class   clazz; // type of the fields
	
	public AttrSlot(String name, boolean is_attr, boolean is_space, Class clazz) {
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
		this.clazz = clazz;
	}
}

@unerasable
public final class NArr<N extends ASTNode> {

    private final ASTNode.NodeImpl 	$parent_impl;
	private final AttrSlot				$pslot;
	private N[]							$nodes;
	
	public NArr(ASTNode.NodeImpl parent_impl, AttrSlot pslot) {
		this.$parent_impl = parent_impl;
		this.$pslot = pslot;
		this.$nodes = new N[0];
		if (parent_impl == null)
			assert (pslot == null || !pslot.is_attr);
	}
	
	public ASTNode getParent() {
		return $parent_impl._self;
	}
	
	public AttrSlot getPSlot() {
		return $pslot;
	}
	
	public int size()
		alias length
		alias get$size
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
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		assert(!is_attr || !node.isAttached());
		if (is_attr) {
			ASTNode old = $nodes[idx];
			old.callbackDetached();
		}
		$nodes[idx] = node;
		if (is_attr) {
			if (idx > 0) {
				$nodes[idx-1].pnext = node;
				node.pprev = $nodes[idx-1];
			}
			if (idx+1 < size()) {
				$nodes[idx+1].pprev = node;
				node.pnext = $nodes[idx+1];
			}
			node.callbackAttached(getParent(), $pslot);
		}
		return node;
	}

	public N add(N node)
		alias append
		alias operator(5, lfy, +=)
		require { node != null; }
	{
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
			if (sz > 0) {
				$nodes[sz-1].pnext = node;
				node.pprev = $nodes[sz-1];
			}
			node.callbackAttached(getParent(), $pslot);
		}
		return node;
	}

	public void addAll(NArr<N> arr)
		alias appendAll
	{
		foreach(N n; arr) add(n);
	}

	public void addAll(N[] arr)
		alias appendAll
	{
		foreach(N n; arr) add(n);
	}

	public void addUniq(N node)
		alias appendUniq
	{
		if (indexOf(node) < 0) add(node);
	}

	public void addUniq(NArr<N> arr)
		alias appendUniq
	{
		foreach(N n; arr) addUniq(n);
	}

	public void addUniq(N[] arr)
		alias appendUniq
	{
		foreach(N n; arr) addUniq(n);
	}

	public N insert(N node, int idx)
	{
		return insert(idx, node);
	}
	
	public N insert(int idx, N node)
		require { node != null; }
	{
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
			if (idx > 0) {
				$nodes[idx-1].pnext = node;
				node.pprev = $nodes[idx-1];
			}
			if (idx+1 < size()) {
				$nodes[idx+1].pprev = node;
				node.pnext = $nodes[idx+1];
			}
			node.callbackAttached(getParent(), $pslot);
		}
		return node;
	}

	public void detach(ASTNode old)
	{
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
		ASTNode old = $nodes[idx];
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		if (is_attr) {
			ASTNode old_pprev = old.pprev;
			ASTNode old_pnext = old.pnext;
			old.callbackDetached();
			if (old_pprev != null) {
				assert (idx > 0 && $nodes[idx-1] == old_pprev);
				old_pprev.pnext = old_pnext;
			}
			if (old_pnext != null) {
				assert (idx+1 < size() && $nodes[idx+1] == old_pnext);
				old_pnext.pprev = old_pprev;
			}
		}
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
		if (this.$nodes.length == 0)
			return;
		if ($pslot != null && $pslot.is_attr) {
			foreach (N node; $nodes) {
				node.callbackDetached();
			}
		}
		this.$nodes = new N[0];
	};
	
	public void copyFrom(NArr<N> arr) {
		if ($pslot != null && $pslot.is_attr) {
			foreach (N n; arr)
				append((N)n.copy());
		} else {
			foreach (N n; arr)
				append(n);
		}
	}
	
	public void moveFrom(NArr<N> arr) {
		if ($pslot != null && $pslot.is_attr) {
			foreach (N n; arr.$nodes)
				append((N)~n);
		} else {
			foreach (N n; arr.$nodes)
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
		int sz = $nodes.length;
		for (int i=0; i < sz; i++) {
			if ($nodes[i] == node)
				return i;
		}
		return -1;
	}
	
	public N[] toArray() {
		int sz = $nodes.length;
		N[] arr = new N[sz];
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i];
		return arr;
	}

	public N[] delToArray() {
		int sz = $nodes.length;
		N[] arr = $nodes;
		$nodes = new N[0];
		for (int i=0; i < sz; i++) {
			N node = arr[i];
			node.callbackDetached();
			node.pnext = null;
			node.pprev = null;
		}
		return arr;
	}

	public ASTNode.NodeView[] toViewArray(Class cls) {
		int sz = $nodes.length;
		ASTNode.NodeView[] arr = (ASTNode.NodeView[])java.lang.reflect.Array.newInstance(cls, sz);
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getNodeView();
		return arr;
	}

	public JNodeView[] toJViewArray(Class cls) {
		int sz = $nodes.length;
		JNodeView[] arr = (JNodeView[])java.lang.reflect.Array.newInstance(cls, sz);
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getJNodeView();
		return arr;
	}

	public Type[] toTypeArray() {
		int sz = $nodes.length;
		Type[] arr = new Type[sz];
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getType();
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
			/// BUG BUG BUG ///
			public Object nextElement() {
				if ( current < size() ) return NArr.this[current++];
				throw new NoSuchElementException(Integer.toString(NArr.this.size()));
			}
		};
	}
}


