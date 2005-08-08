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

// control-flow simple node
public @interface cfnode {
}
// sontrol-flow loop node
public @interface cfloop {
	String cont();
	String brk();	
}
// sontrol-flow switch node
public @interface cfswitch {
	String brk();	
}
// control-flow field in/out links
public @interface cflink {
	String in();
	String out();
}
// control-flow fork (boolean) field links
public @interface cffork {
	String in();
	String yes();
	String no();
}
// control-flow field in/out links
public @interface cflist {
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

@node
@cfnode
public class CFLabel extends CFNode {
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

	public void cleanup() {
		int sz = $nodes.length;
		for (int i=0; i < sz; i++)
			$nodes[i].cleanup();
		$nodes = null;
	};
	
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
		if ($pslot != null && $pslot.is_attr) {
			node.parent = $parent;
			node.pslot = $pslot;
			$nodes[idx].pslot = null;
		}
		$nodes[idx] = node;
		return node;
	}

	public N add(N node)
		alias append
		alias operator(5, lfy, +=)
		require { node != null; }
	{
		if ($pslot != null && $pslot.is_attr)
			assert(!contains(node));
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		if ($pslot != null && $pslot.is_attr) {
			node.parent = $parent;
			node.pslot = $pslot;
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
				if ($pslot != null && $pslot.is_attr) {
					node.parent = $parent;
					node.pslot = $pslot;
					$nodes[i].pslot = null;
				}
				$nodes[i] = node;
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
		if ($pslot != null && $pslot.is_attr)
			assert(!contains(node));
		int sz = $nodes.length;
		N[] tmp = new N[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		tmp[idx] = node;
		if ($pslot != null && $pslot.is_attr) {
			node.parent = $parent;
			node.pslot = $pslot;
		}
		for (; i < sz; i++)
			tmp[i+1] = $nodes[i];
		$nodes = tmp;
		return node;
	}

	public void del(int idx)
	{
		int sz = $nodes.length-1;
		N[] tmp = new N[sz];
		if ($pslot != null && $pslot.is_attr) {
			$nodes[idx].pslot = null;
		}
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
			public A nextElement() {
				if ( current < size() ) return NArr.this[current++];
				throw new NoSuchElementException(Integer.toString(NArr.this.size()));
			}
		};
	}

}


