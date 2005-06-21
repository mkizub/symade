package kiev.tree;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public final $wrapper class VNode<N extends Node> {

	private forward access:ro,rw N $node;
	
	public VNode() {
	}
	
	public VNode(N node) {
		this.$node = node;
	}
	
	public final N getNode()
		alias operator(210,fy,$cast)
	{
		return this.$node;
	}
	
	public final void setNode(N node) {
		assert(impl.vnode == null);
		node.vnode = this;
		this.$node = node;
	}
}

public final class VNodeArray<N extends Node> {

    private VNode<Node> $parent := null;
	private VNode<Node>[]  $nodes;
	
	public VNodeArray(Node# parent) {
		this.$parent := parent;
		this.$nodes = new VNide<Node>[0];
	}
	
	public VNodeArray(int size, Node# parent) {
		this.$parent := parent;
		this.$nodes = new VNide<Node>[size];
	}
	
	public void set$parent(Node# p) {
		assert(parent == null);
		parent := p;
	}

	public /*abstract*/ void cleanup() {
		$parent := null;
		$nodes = null;
	};
	
	public final VNode<N> get(int idx)
		alias at
		alias operator(210,xfy,[])
	{
		return $nodes[idx];
	}
	
	public VNode<N> set(int idx, VNode<N> node)
		alias operator(210,lfy,[])
	{
		$nodes[idx] = node;
		return node;
	}

	public VNode<N> add(VNode<N> node)
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNide<N>[sz+1];
		int i;
		for (i=0; i < sz; i++)
			tmp[i] = $nodes[i];
		$nodes = tmp;
		$nodes[sz] = node;
		return node;
	}

	public VNode<N> insert(int idx, VNode<N> node)
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNide<N>[sz+1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i+1] = $nodes[i];
		tmp[idx] = node;
		$nodes = tmp;
		return node;
	}

	public VNode<N> del(int idx)
	{
		int sz = $nodes.length;
		VNode<N>[] tmp = new VNide<N>[sz-1];
		int i;
		for (i=0; i < idx; i++)
			tmp[i] = $nodes[i];
		for (i++; i < sz; i++)
			tmp[i-1] = $nodes[i];
		VNode<N> ret = $nodes[idx];
		$nodes = tmp;
		return ret;
	}

}

public final $wrapper class VNodeRef<R extends Node> {

	private forward access:ro,rw VNode<R> $ref := null;
	
	public VNodeRef() {
	}
	
	public VNodeRef(VNode<R> ref) {
		this.$ref = ref;
	}
	
	public final VNode<R> getRef()
		alias operator(210,fy,$cast)
	{
		return this.$ref;
	}
	
	public final void setRef(VNode<R> ref) {
		this.$ref = ref;
	}
}

public abstract class CreateInfo {
}

public abstract class Node {
	// the node which encapsulates this implementation (version)
    public VNode<Node>		vnode;
	// the reason of creating and other information about this node
	public CreateInfo		src_info;
    // the parent node in the tree
    public VNode<Node>		parent;
	// node flags
	public int				flags;

	public Node(Node# parent) {
		this.parent = parent;
		new VNode<Node>(this);
	}
	
	public Node() {
		this(null);
	}
	
	public final Node# getVNode()
		alias operator(210,fy,$cast)
	{
		return this.vnode;
	}
	
	public void set$parent(Node# p) {
		assert(parent == null);
		parent = p;
	}

	public /*abstract*/ void cleanup() {
		parent = null;
		src_info = null;
	};
	
}
