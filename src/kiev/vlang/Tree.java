/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import java.lang.annotation.*;

/**
 * @author Maxim Kizub
 *
 */

// syntax-tree node
public @interface node {
	boolean copyable() default true;
}
// syntax-tree attribute field
public @interface att {
	boolean copyable() default true;
}
// syntax-tree reference field
public @interface ref {
	boolean copyable() default true;
}

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

// AST declarations for FileUnit, Struct-s, Import-s, Operator-s, Typedef-s, Macros-es
@node(copyable=false)
public class Tree extends ASTNode {
	@att public final NArr<Struct>	members;
	
	public Tree() {
	}

	public Object copy() {
		throw new CompilerException(getPos(),"Tree node cannot be copied");
	};

}

public final class AttrSlot {
	public final String  name; // field (property) name
	public final boolean is_attr; // @att or @ref
	public final boolean is_space; // if NArr<Node>
	
	public AttrSlot(String name, boolean is_attr, boolean is_space) {
		this.name = name;
		this.is_attr = is_attr;
		this.is_space = is_space;
	}
}

public final class NArr<N extends ASTNode> {

    private final ASTNode 	$parent;
	private final AttrSlot	$pslot;
	private N[]				$nodes;
	
	public NArr() {
		this.$nodes = new N[0];
	}
	
	public NArr(ASTNode parent, AttrSlot pslot) {
		this.$parent = parent;
		this.$pslot = pslot;
		this.$nodes = new N[0];
		if (parent == null)
			assert (pslot == null || !pslot.is_attr);
	}
	
	public ASTNode getParent() {
		return $parent;
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
		if (is_attr) {
			ASTNode old = $nodes[idx];
			old.callbackDetached();
			//old.parent = null;
			//old.pslot = null;
			old.pprev = null;
			old.pnext = null;
		}
		$nodes[idx] = node;
		if (is_attr) {
			node.parent = $parent;
			node.pslot = $pslot;
			//assert (node.pprev == null);
			//assert (node.pnext == null);
			if (idx > 0) {
				$nodes[idx-1].pnext = node;
				node.pprev = $nodes[idx-1];
			}
			if (idx+1 < size()) {
				$nodes[idx+1].pprev = node;
				node.pnext = $nodes[idx+1];
			}
			node.callbackAttached();
		}
		return node;
	}

	public N add(N node)
		alias append
		alias operator(5, lfy, +=)
		require { node != null; }
	{
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		if (is_attr)
			assert(!contains(node));
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		if (is_attr) {
			node.parent = $parent;
			node.pslot = $pslot;
			//assert (node.pprev == null);
			//assert (node.pnext == null);
			if (sz > 0) {
				$nodes[sz-1].pnext = node;
				node.pprev = $nodes[sz-1];
			}
			node.callbackAttached();
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
		if (!contains(node)) add(node);
	}

	public void addUniq(NArr<N> arr)
		alias appendUniq
	{
		foreach(N n; arr; !contains(n)) add(n);
	}

	public void addUniq(N[] arr)
		alias appendUniq
	{
		foreach(N n; arr; !contains(n)) add(n);
	}

	public void replace(Object old, N node)
	{
		int sz = $nodes.length;
		for (int i=0; i < sz; i++) {
			if ($nodes[i] == old) {
				this.set(i, node);
				return;
			}
		}
		throw new RuntimeException("Not found node");
	}
	
	public N insert(N node, int idx)
	{
		return insert(idx, node);
	}
	
	public N insert(int idx, N node)
		require { node != null; }
	{
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		if (is_attr)
			assert(!contains(node));
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
			node.parent = $parent;
			node.pslot = $pslot;
			//assert (node.pprev == null);
			//assert (node.pnext == null);
			if (idx > 0) {
				$nodes[idx-1].pnext = node;
				node.pprev = $nodes[idx-1];
			}
			if (idx+1 < size()) {
				$nodes[idx+1].pprev = node;
				node.pnext = $nodes[idx+1];
			}
			node.callbackAttached();
		}
		return node;
	}

	public void del(int idx)
	{
		ASTNode old = $nodes[idx];
		final boolean is_attr = ($pslot != null && $pslot.is_attr);
		if (is_attr) {
			old.callbackDetached();
			if (old.pprev != null) {
				assert (idx > 0 && $nodes[idx-1] == old.pprev);
				old.pprev.pnext = old.pnext;
			}
			if (old.pnext != null) {
				assert (idx+1 < size() && $nodes[idx+1] == old.pnext);
				old.pnext.pprev = old.pprev;
			}
			//old.parent = null;
			//old.pslot = null;
			old.pnext = null;
			old.pprev = null;
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
			foreach (N node; $nodes) node.pslot = null;
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
	
	public boolean contains(ASTNode node) {
		for (int i=0; i < $nodes.length; i++) {
			if ($nodes[i].equals(node))
				return true;
		}
		return false;
	}
	
	public N[] toArray() {
		int sz = $nodes.length;
		N[] arr = new N[sz];
		for (int i=0; i < sz; i++)
			arr[i] = $nodes[i];
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



