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

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.event.*;


/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public class TreeView extends UIView implements KeyListener {

		
	protected final ANodeTree the_tree;
	
	public TreeView(Window window, ATextSyntax syntax, ANodeTree the_tree) {
		super(window, syntax);
		this.the_tree = the_tree;
		this.formatter = new GfxFormatter(syntax, (Graphics2D)the_tree.getGraphics());
		this.the_tree.tree_view = this;
		this.the_tree.addKeyListener(this);
		this.the_tree.addMouseListener(this);
		this.setRoot(null);
	}

	public void setRoot(ANode root) {
		this.the_root = root;
		this.the_tree.setRoot();
	}
	
	public void formatAndPaint(boolean full) {
		the_tree.format();
		the_tree.repaint();
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() >= 2) {
			TreePath sel = the_tree.getPathForLocation(e.getX(), e.getY());
			if (sel == null || !(sel.getLastPathComponent() instanceof Drawable))
				return;
			Drawable dr = (Drawable)sel.getLastPathComponent();
			Vector<ANode> v = new Vector<ANode>();
			ANode n = dr.drnode;
			if (n instanceof DNode)
				n = n.id;
			v.append(n);
			while (n != null && !(n instanceof FileUnit)) {
				n = n.parent();
				v.append(n);
			}
			if !(n instanceof FileUnit)
				return;
			parent_window.openEditor((FileUnit)n, v.toArray());
			e.consume();
		}
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	
	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == (KeyEvent.CTRL_DOWN_MASK|KeyEvent.ALT_DOWN_MASK)) {
			switch (code) {
			case KeyEvent.VK_S: {
				evt.consume();
				// build a menu of types to instantiate
				JPopupMenu m = new JPopupMenu();
				m.add(new JMenuItem(new SetSyntaxAction("Project Tree Syntax", "stx-fmt.syntax-for-project-tree")));
				m.add(new JMenuItem(new LoadSyntaxAction("Project Tree Syntax (java-tree.xml)", "java-tree.xml", "test.syntax-for-project-tree")));
				m.show(the_tree, 0, 0);
				break;
				}
			}
		}
	}
}

class ANodeTree extends JTree {
	
	TreeView tree_view;
	
	ANodeTree() {
		super(new ANodeTreeModel());
		setEditable(false);
		setCellRenderer(new DrawableTreeCellRenderer());
	}

	public void setRoot() {
		ANodeTreeModel model = (ANodeTreeModel)treeModel;
		model.setRoot(tree_view);
	}

	public void format() {
		ANodeTreeModel model = (ANodeTreeModel)treeModel;
		model.format(tree_view);
	}
}

final class ANodeTreeModel implements TreeModel {

	private TreeView tree_view;
	private EventListenerList listenerList = new EventListenerList();
	
	void format(TreeView tree_view) {
		this.tree_view = tree_view;
		Drawable old_root = tree_view.view_root;
		tree_view.view_root = tree_view.formatter.format(tree_view.the_root, tree_view.view_root);
		if (tree_view.view_root != old_root)
			fireTreeStructureChanged(this, new TreePath(tree_view.view_root));
	}

	void setRoot(TreeView tree_view) {
		this.tree_view = tree_view;
		tree_view.view_root = tree_view.formatter.format(tree_view.the_root, null);
		fireTreeStructureChanged(this, new TreePath(tree_view.view_root));
	}

    public Object getRoot() {
		if (tree_view == null)
			return null;
		return tree_view.view_root;
	}

    public Object getChild(Object parent, int index) {
		DrawNonTerm nt = (DrawNonTerm)parent;
		return nt.args[index];
	}
    public int getChildCount(Object parent) {
		if !(parent instanceof DrawNonTerm)
			return 0;
		DrawNonTerm nt = (DrawNonTerm)parent;
		if (nt.draw_folded) {
			nt.draw_folded = false;
			tree_view.formatter.format(nt.drnode, nt);
		}
		return nt.args.length;
	}
    public boolean isLeaf(Object node) {
		Drawable dr = (Drawable)node;
		if !(dr instanceof DrawNonTerm)
			return true;
		DrawNonTerm nt = (DrawNonTerm)dr;
		if (nt.folded == null)
			return true;
		return false;
	}
    public int getIndexOfChild(Object parent, Object child) {
		DrawNonTerm nt = (DrawNonTerm)parent;
		for (int i=0; i < nt.args.length; i++) {
			if (nt.args[i] == child)
				return i;
		}
		return -1;
	}

    public void valueForPathChanged(TreePath path, Object newValue) {
		// tree is not editable now
	}

	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}
	
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}

    private void fireTreeStructureChanged(Object source, TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path);
                ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
            }
        }
    }
}

class DrawableTreeCellRenderer extends DefaultTreeCellRenderer {
	public Component getTreeCellRendererComponent(
		JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		assert (tree instanceof ANodeTree);
		if (value instanceof Drawable)
			value = ((Drawable)value).getText();
		return super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
	}
}


/*
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
*/
