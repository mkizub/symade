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
package kiev.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import kiev.fmt.DrawCtrl;
import kiev.fmt.DrawLayoutBlock;
import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawSpace;
import kiev.fmt.DrawTerm;
import kiev.fmt.DrawTreeBranch;
import kiev.fmt.Draw_SyntaxSpace;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.IFmtGfx;
import kiev.gui.INodeTree;
import kiev.gui.IUIView;
import kiev.gui.TreeView;


public class ANodeTree extends JTree implements INodeTree {
	private static final long serialVersionUID = 6245260955964490417L;

	/** The formatter of the current view */
	public final DrawableTreeCellRenderer renderer;
	public TreeView                       tree_view;
	
	ANodeTree() {
		super(new ANodeTreeModel());
		this.setEditable(false);
		this.renderer = new DrawableTreeCellRenderer();
		this.setCellRenderer(this.renderer);
		this.addTreeExpansionListener((ANodeTreeModel)this.treeModel);
	}

	public void setUIView(IUIView uiv) {
		if (uiv instanceof TreeView){
			this.tree_view = (TreeView)uiv;
			this.addMouseListener(tree_view);
			this.addComponentListener(tree_view);
			this.addKeyListener(tree_view);
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

	public IFmtGfx getFmtGraphics() {
		return new AWTGraphics2D((Graphics2D)this.getGraphics());
	}

	public void setRoot() {
		ANodeTreeModel model = (ANodeTreeModel)treeModel;
		model.setRoot(tree_view);
	}

	public void format() {
		ANodeTreeModel model = (ANodeTreeModel)treeModel;
		model.format(tree_view);
	}

	public Drawable getDrawableAt(int x, int y) {
		TreePath sel = this.getPathForLocation(x, y);
		if (sel == null || !(sel.getLastPathComponent() instanceof Drawable))
			return null;
		return (Drawable)sel.getLastPathComponent();
	}
}

final class ANodeTreeModel implements TreeModel, TreeExpansionListener {

	private TreeView tree_view;
	private EventListenerList listenerList = new EventListenerList();
	
	void format(TreeView tree_view) {
		this.tree_view = tree_view;
		Drawable old_root = tree_view.view_root;
		quick_format();
		if (tree_view.view_root != old_root)
			fireTreeStructureChanged(this, new TreePath(tree_view.view_root));
	}
	
	private void quick_format() {
		tree_view.formatter.format(tree_view.the_root, tree_view.view_root, tree_view.getSyntax());
		tree_view.view_root = tree_view.formatter.getRootDrawable();
	}

	void setRoot(TreeView tree_view) {
		this.tree_view = tree_view;
		if (tree_view.the_root == null) {
			tree_view.view_root = null;
			fireTreeStructureChanged(this, new TreePath(new DrawSpace(null,new Draw_SyntaxSpace(null),tree_view.syntax)));
		} else {
			tree_view.formatter.format(tree_view.the_root, null, tree_view.getSyntax());
			tree_view.view_root = tree_view.formatter.getRootDrawable();
			fireTreeStructureChanged(this, new TreePath(tree_view.view_root));
		}
	}

    public Object getRoot() {
		if (tree_view == null)
			return null;
		return tree_view.view_root;
	}

    public Object getChild(Object parent, int index) {
		Drawable dr = (Drawable)parent;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		if (!(dr instanceof DrawTreeBranch))
			return null;
		return ((DrawTreeBranch)dr).getSubNodes()[index];
	}
    public int getChildCount(Object parent) {
		Drawable dr = (Drawable)parent;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		if (!(dr instanceof DrawTreeBranch))
			return 0;
		DrawTreeBranch nt = (DrawTreeBranch)dr;
		if (nt.getDrawFolded()) {
			nt.setDrawFolded(false);
			quick_format();
		}
		return nt.getSubNodes().length;
	}
    public boolean isLeaf(Object node) {
		Drawable dr = (Drawable)node;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		return !(dr instanceof DrawTreeBranch);
	}
    public int getIndexOfChild(Object parent, Object child) {
		Drawable dr = (Drawable)parent;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		if (!(dr instanceof DrawTreeBranch))
			return -1;
		Drawable[] children = ((DrawTreeBranch)dr).getSubNodes();
		for (int i=0; i < children.length; i++) {
			if (children[i] == child)
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

	public void treeCollapsed(TreeExpansionEvent event) {
		TreePath path = event.getPath();
		Object obj = path.getLastPathComponent();
		while (obj instanceof DrawCtrl)
			obj = ((DrawCtrl)obj).getArg();
		if (obj instanceof DrawTreeBranch) {
			DrawTreeBranch nt = (DrawTreeBranch)obj;
			nt.setDrawFolded(true);
			quick_format();
			fireTreeStructureChanged(this, path);
		}
	}
	
	public void treeExpanded(TreeExpansionEvent event) {
	}
}

class DrawableTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 2546321918616119971L;

	private Drawable         cur_dr;
	private DrawLayoutBlock  cur_dlb;
	private Dimension        cur_dim;
	
	public Component getTreeCellRendererComponent(
		JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		assert (tree instanceof ANodeTree);
		while (value instanceof DrawCtrl)
			value = ((DrawCtrl)value).getArg();
		if (value instanceof DrawTreeBranch)
			cur_dr = ((DrawTreeBranch)value).getFolded();
		else
			cur_dr = (Drawable)value;
		cur_dlb = new DrawLayoutBlock();
		cur_dr.postFormat(cur_dlb);
		Rectangle r = calcBounds(cur_dlb);
		super.getTreeCellRendererComponent(tree,"",sel,expanded,leaf,row,hasFocus);
		cur_dim = new Dimension(r.width+6, r.height);
		setMinimumSize(cur_dim);
		return this;
	}
	
    public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if (dim == null)
			return cur_dim;
    	return new Dimension(cur_dim.width+dim.width, Math.max(cur_dim.height, dim.height));
	}

    public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		if (dim == null)
			return cur_dim;
    	return new Dimension(cur_dim.width+dim.width, Math.max(cur_dim.height, dim.height));
	}

	private Rectangle calcBounds(DrawLayoutInfo n) {
		if (n instanceof GfxDrawTermLayoutInfo) {
			GfxDrawTermLayoutInfo dtli = (GfxDrawTermLayoutInfo)n;
			int w = dtli.getWidth();
			int h = dtli.getHeight();
			return new Rectangle(dtli.getX(), dtli.getY(), w, h);
		} else {
			Rectangle res = null;
			for (DrawLayoutInfo dlb: n.getBlocks()) {
				Rectangle r = calcBounds(dlb);
				if (res == null)
					res = r;
				else if (r != null)
					res = res.union(r);
			}
			return res;
		}
	}

	private GfxDrawTermLayoutInfo getFirstLayoutInfo(DrawLayoutInfo dli) {
		if (dli instanceof GfxDrawTermLayoutInfo)
			return (GfxDrawTermLayoutInfo)dli;
		for (DrawLayoutInfo dlb: dli.getBlocks()) {
			GfxDrawTermLayoutInfo fli = getFirstLayoutInfo(dlb);
			if (fli != null)
				return fli;
		}
		return null;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		int x_offs = 0;
		int y_offs = 0;
		Icon curIcon = getIcon();
		if (curIcon != null)
		    x_offs = 4 + curIcon.getIconWidth() + Math.max(0, getIconTextGap()-1);
		GfxDrawTermLayoutInfo fli = getFirstLayoutInfo(cur_dlb);
		if (fli != null) {
			x_offs -= fli.getX(); 
			y_offs -= fli.getY();
		}
	    g.translate(+x_offs, +y_offs);
		this.paint((Graphics2D)g, cur_dlb);
		g.translate(-x_offs, -y_offs);
	}

	private void paint(Graphics2D g, DrawLayoutInfo n) {
		if (n == null)
			return;
		if (n instanceof GfxDrawTermLayoutInfo) {
			paintLeaf(g, (GfxDrawTermLayoutInfo)n);
		} else {
			for (DrawLayoutInfo dlb: n.getBlocks())
				paint(g, dlb);
		}
	}

	private void paintLeaf(Graphics2D g, GfxDrawTermLayoutInfo dtli) {
		DrawTerm leaf = dtli.getDrawable();
		if (leaf == null || leaf.isUnvisible())
			return;
		int x = dtli.getX();
		int y = dtli.getY();
		//int w = dtli.getWidth();
		//int h = dtli.getHeight();
		int b = dtli.getBaseline();
		
		boolean set_white = false;
		
		if (set_white)
			g.setColor(Color.WHITE);
		else
			g.setColor(new Color(leaf.syntax.lout.rgb_color));
		Font font  = AWTGraphics2D.decodeFont(leaf.syntax.lout.font_name);
		g.setFont(font);
		String s = leaf.getText();
		if (s == null) s = "\u25d8"; // ◘
		if (s.length() == 0)
			return;
		TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
		tl.draw(g, x, y+b);
	}
}

