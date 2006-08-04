package kiev.gui;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import javax.swing.JTree;
import javax.swing.tree.*;


/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public class TreeView extends UIView {
	
	protected final ANodeTree the_tree;
	
	public TreeView(Window window, ANodeTree the_tree) {
		super(window, null, null);
		this.the_tree = the_tree;
	}

	public void setRoot(ANode root) {
		this.the_root = root;
		this.the_tree.setRoot(root);
	}
	
	public void formatAndPaint(boolean full) {
	}
}

class ANodeTree extends JTree {
	
	ANodeTree() {
		super(new ATreeNode(null,null));
		setEditable(false);
	}

	public void setRoot(ANode root) {
		DefaultTreeModel model = (DefaultTreeModel)treeModel;
		model.setRoot(new ATreeNode(null,root));
	}
}

abstract class XTreeNode implements TreeNode {
	final XTreeNode		parent;
	      ANode			node;
	      TreeNode[]	children;
	XTreeNode(XTreeNode parent, ANode node) {
		this.parent = parent;
		this.node = node;
	}

    public abstract int getChildCount();
    public abstract boolean getAllowsChildren();
    public abstract TreeNode getChildAt(int idx);

    public boolean isLeaf() { return !getAllowsChildren(); }

    public TreeNode getParent() { return parent; }

    public int getIndex(TreeNode n) {
		if (children != null) {
			for (int i=0; i < children.length; i++) {
				if (children[i] == n)
					return i;
			}
		}
		return -1;
	}
	
	class XTreeEnumeration extends java.util.Enumeration {
		int idx;
		public boolean hasMoreElements() {
			return children != null && idx < children.length;
		}
		public Object nextElement() throws NoSuchElementException {
			if (children == null && idx >= children.length)
				throw new NoSuchElementException();
			return getChildAt(idx);
		}
	}

    public java.util.Enumeration children() {
		return new XTreeEnumeration();
	}
}

class ATreeNode extends XTreeNode {

	AttrSlot[] values;
	
	ATreeNode(XTreeNode parent, ANode node) {
		super(parent, node);
		makeValues();
		this.children = new TreeNode[getChildCount()];
	}

	private void makeValues() {
		if (node == null) {
			values = AttrSlot.emptyArray;
			return;
		}
		AttrSlot[] attrs = node.values();
		int count = 0;
		for (int i=0; i < attrs.length; i++) {
			if (attrs[i].name == "parent")
				continue;
			count++;
		}
		if (count == 0) {
			values = AttrSlot.emptyArray;
			return;
		}
		values = new AttrSlot[count];
		for (int i=0, j=0; i < attrs.length; i++) {
			if (attrs[i].name == "parent")
				continue;
			values[j++] = attrs[i];
		}
		this.children = new TreeNode[getChildCount()];
	}
	
    public int getChildCount() { return values.length; }

    public boolean getAllowsChildren() { return getChildCount() > 0; }

    public TreeNode getChildAt(int idx) {
		if (node == null)
			return null;
		if (children[idx] != null)
			return children[idx];
		AttrSlot slot = values[idx];
		if (slot instanceof SpaceAttrSlot) {
			children[idx] = new SpaceTreeNode(this, node, (SpaceAttrSlot)slot);
			return children[idx];
		}
		Object val = slot.get(node);
		if (slot.is_attr && val instanceof ANode)
			children[idx] = new ATreeNode(this, (ANode)val);
		else
			children[idx] = new OTreeNode(this, node, slot, val);
		return children[idx];
	}

	public String toString() {
		if (node == null)
			return "<root>";
		String name = node.getClass().getName();
		int idx = name.lastIndexOf('.');
		if (idx >= 0)
			name = name.substring(idx+1);
		AttrSlot slot = node.pslot();
		if (slot == null || slot instanceof SpaceAttrSlot)
			return "{ "+name+" }";
		return slot.name + ": "+name;
	}
}

class SpaceTreeNode extends XTreeNode {
	final SpaceAttrSlot	slot;

	SpaceTreeNode(XTreeNode parent, ANode node, SpaceAttrSlot slot) {
		super(parent, node);
		this.slot = slot;
		this.children = new TreeNode[getChildCount()];
	}
	
	private Object[] getObjects() { return slot.get(node); }

    public boolean getAllowsChildren() { return true; }

    public int getChildCount() { return getObjects().length; }

    public TreeNode getChildAt(int idx) {
		if (children[idx] != null)
			return children[idx];
		Object val = slot.get(node, idx);
		if (slot.is_attr && val instanceof ANode)
			children[idx] = new ATreeNode(this, (ANode)val);
		else
			children[idx] = new OTreeNode(this, node, slot, val);
		return children[idx];
	}

	public String toString() {
		return slot.name;
	}
}

class OTreeNode extends XTreeNode {
	final ANode		node;
	final AttrSlot	slot;
	      Object	val;

	OTreeNode(XTreeNode parent, ANode node, AttrSlot slot, Object val) {
		super(parent, node);
		this.slot = slot;
		this.val = val;
	}

    public int getChildCount() { return 0; }
    public boolean getAllowsChildren() { return false; }
    public TreeNode getChildAt(int idx) { return null; }
	
	public String toString() {
		String str;
		if (val instanceof ANode) {
			str = val.getClass().getName();
			int idx = str.lastIndexOf('.');
			if (idx >= 0)
				str = str.substring(idx+1);
		} else {
			str = String.valueOf(val);
		}
		if (slot instanceof SpaceAttrSlot)
			return str;
		return slot.name + ": " + str;
	}
}

