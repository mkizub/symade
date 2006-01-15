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
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
		this.clazz = clazz;
	}
	
	public boolean isMeta() { return false; }

	public void set(ASTNode node, Object value) {
		node.setVal(name, value);
	}
	public Object get(ASTNode node) {
		return node.getVal(name);
	}
}

public class MetaAttrSlot extends AttrSlot {
	public final KString id;
	public MetaAttrSlot(KString name, Class clazz) {
		super(name.toString().intern(),true,false,clazz);
		this.id = name;
	}
	public boolean isMeta() { return true; }

	public abstract void set(ASTNode node, Object value);
	public abstract Object get(ASTNode node);
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
			arr[i] = $nodes[i].getVView();
		return arr;
	}

	public JNodeView[] toJViewArray(Class cls) {
		int sz = $nodes.length;
		JNodeView[] arr = (JNodeView[])java.lang.reflect.Array.newInstance(cls, sz);
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i].getJView();
		return arr;
	}

	@unerasable
	public <J extends JNodeView> JArr<J> toJArr() alias operator(210,fy,$cast) {
		return new JArr<J>();
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

	@unerasable
	public final class JArr<J extends JNodeView> {

		public NArr<N> getNArr() { return NArr.this; }
		
		public JNodeView getParent() {
			return (J)NArr.this.$parent_impl._self;
		}
		
		public AttrSlot getPSlot() {
			return getNArr().$pslot;
		}
		
		public int size()
			alias length
			alias get$size
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
	
		public void addUniq(J node)
			alias appendUniq
		{
			if (indexOf(node) < 0) add(node);
		}
	
		public void addUniq(J[] arr)
			alias appendUniq
		{
			foreach(J n; arr) addUniq(n);
		}
	
		public J insert(J node, int idx)
		{
			return insert(idx, node);
		}
		
		public J insert(int idx, J node)
			require { node != null; }
		{
			getNArr().insert(idx,(N)node);
			return node;
		}
	
		public void detach(J old) { getNArr().detach((N)old); }
		public void del(int idx) { getNArr().del(idx); }
		public void delAll() { getNArr().delAll(); }
		public void copyFrom(JArr<J> arr) { getNArr().copyFrom(arr.getNArr()); }
		public void moveFrom(JArr<J> arr) { getNArr().moveFrom(arr.getNArr()); }
		public boolean contains(J node) { return getNArr().contains((N)node); }
		public int indexOf(J node) { return getNArr().indexOf((N)node); }
		public J[] toArray() {
			int sz = getNArr().$nodes.length;
			J[] arr = new J[sz];
			for (int i=0; i < sz; i++)
				arr[i] = this[i];
			return arr;
		}
	
		public J[] delToArray() {
			N[] narr = getNArr().delToArray();
			int sz = narr.length;
			J[] jarr = new J[narr.length];
			for (int i=0; i < sz; i++)
				jarr[i] = (J)narr[i];
			return jarr;
		}
	
		public Enumeration<J> elements() {
			return new Enumeration<J>() {
				int current;
				public boolean hasMoreElements() { return current < JArr.this.size(); }
				public J nextElement() {
					if ( current < size() ) return JArr.this[current++];
					throw new NoSuchElementException(Integer.toString(JArr.this.size()));
				}
				/// BUG BUG BUG ///
				public Object nextElement() {
					if ( current < size() ) return JArr.this[current++];
					throw new NoSuchElementException(Integer.toString(JArr.this.size()));
				}
			};
		}
	}
}


