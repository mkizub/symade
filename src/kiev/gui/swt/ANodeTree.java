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
package kiev.gui.swt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;

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
import kiev.gui.event.EventListenerList;
import kiev.gui.event.InputEventInfo;
import kiev.gui.swing.AWTGraphics2D;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;


public class ANodeTree extends TreeViewer implements INodeTree, MouseListener {

	/** The formatter of the current view */
	public final DrawableEntry entry = new DrawableEntry();
	public TreeView tree_view;
	ANodeTreeContentProvider treeContentProvider;

	class DrawableEntry {
		private Drawable         cur_dr;
		private DrawLayoutBlock  cur_dlb;
		private Dimension        cur_dim;
		final Renderer renderer = new Renderer();
		private Rectangle bounds;

		public void draw(Event event, Drawable value) {
			if (event.gc == null) return;
			GC gc = event.gc;
			while (value instanceof DrawCtrl)
				value = ((DrawCtrl)value).getArg();
			if (value instanceof DrawTreeBranch)
				cur_dr = ((DrawTreeBranch)value).getFolded();
			else
				cur_dr = (Drawable)value;
			cur_dlb = new DrawLayoutBlock();
			cur_dr.postFormat(cur_dlb);
			Rectangle r = calcBounds(cur_dlb);
			setBounds(r);
			//painting should be here
			gc.setClipping(r);
			renderer.prepareRendering(gc);
			paint(renderer.getGraphics2D());
			renderer.render(gc);
			cur_dim = new Dimension(r.width+6, r.height);
//			setMinimumSize(cur_dim);
		}

		public int getHeight(Event event, Drawable value) {
			while (value instanceof DrawCtrl)
				value = ((DrawCtrl)value).getArg();
			if (value instanceof DrawTreeBranch)
				cur_dr = ((DrawTreeBranch)value).getFolded();
			else
				cur_dr = (Drawable)value;
			cur_dlb = new DrawLayoutBlock();
			cur_dr.postFormat(cur_dlb);
			Rectangle r = calcBounds(cur_dlb);
			cur_dim = new Dimension(r.width+6, r.height);
			return cur_dim.height;
		}

		public int getWidth(Event event, Drawable value) {
			while (value instanceof DrawCtrl)
				value = ((DrawCtrl)value).getArg();
			if (value instanceof DrawTreeBranch)
				cur_dr = ((DrawTreeBranch)value).getFolded();
			else
				cur_dr = (Drawable)value;
			cur_dlb = new DrawLayoutBlock();
			cur_dr.postFormat(cur_dlb);
			Rectangle r = calcBounds(cur_dlb);
			cur_dim = new Dimension(r.width+6, r.height);
			return cur_dim.width;
		}

		protected Dimension getPreferredSize() {
			Rectangle b = getBounds();
			Dimension dim = new Dimension(b.width, b.height);
			if (dim == null)
				return cur_dim;
			return new Dimension(cur_dim.width+dim.width, Math.max(cur_dim.height, dim.height));
		}

		protected Dimension getMinimumSize() {
			Rectangle b = getBounds();
			Dimension dim = new Dimension(b.width, b.height);
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

		public void paint(Graphics g) {
			int x_offs = 0;
			int y_offs = 0;
//			Icon curIcon = getIcon();
//			if (curIcon != null)
//			x_offs = 4 + curIcon.getIconWidth() + Math.max(0, getIconTextGap()-1);
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
			Object term_obj = leaf.getTermObj();
			String s;
			if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
				s = "\u25d8"; // ◘
			else if (term_obj == DrawTerm.NULL_NODE)
				s = "\u25c6"; // ◆
			else {
				s = String.valueOf(term_obj);
				if (s == null)
					s = "\u25d8"; // ◘
			}
			if (s.length() == 0)
				return;
			TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
		}


		/**
		 * @return the bounds
		 */
		public Rectangle getBounds() {
			return bounds;
		}


		/**
		 * @param bounds the bounds to set
		 */
		public void setBounds(Rectangle bounds) {
			this.bounds = bounds;
		}
	}

	ANodeTree(Composite parent, int style) {
		super(parent, style);
		treeContentProvider = new ANodeTreeContentProvider();
		setContentProvider(treeContentProvider); 
		setLabelProvider(new OwnerDrawLabelProvider() {
			protected void measure(Event event, Object element) {
				Drawable dr = (Drawable) element;
				event.setBounds(new Rectangle(event.x, event.y, entry.getWidth(event, dr),
						entry.getHeight(event, dr)));
			}
			protected void paint(Event event, Object element) {
				Drawable dr = (Drawable) element;
				entry.draw(event, dr);
			}
		});

		addTreeListener(treeContentProvider);
		getControl().addMouseListener(this);
		OwnerDrawLabelProvider.setUpOwnerDraw(this);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH);

		getControl().setLayoutData(data);

//		setSelection(new StructuredSelection(treeContentProvider.getRoot()));
	}

	public void setUIView(IUIView uiv) {
		if (uiv instanceof TreeView){
			this.tree_view = (TreeView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

	public IFmtGfx getFmtGraphics() {
		return new AWTGraphics2D(entry.renderer.getGraphics2D());
	}

	public void setRoot() {
		treeContentProvider.setRoot(tree_view);
	}

	public void format() {
		treeContentProvider.format(tree_view);
	}

	public Drawable getDrawableAt(int x, int y) {
		TreePath sel = getPathForLocation(x, y);
		if (sel == null || !(sel.getLastSegment() instanceof Drawable))
			return null;
		return (Drawable)sel.getLastSegment();
	}

	private TreePath getPathForLocation(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	public void repaint() {
		// TODO Auto-generated method stub

	}

	public void requestFocus() {
		// TODO Auto-generated method stub

	}

	public void mouseDoubleClick(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseDown(MouseEvent evt) {
		tree_view.inputEvent(new InputEventInfo(evt));
	}

	public void mouseUp(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}
}

class ANodeTreeContentProvider implements ITreeContentProvider, ITreeViewerListener {

	private TreeView tree_view;
	private EventListenerList listenerList = new EventListenerList();
	private static final Object[] EMPTY = new Object[] {};

	void format(TreeView tree_view) {
		this.tree_view = tree_view;
		Drawable old_root = tree_view.view_root;
		quick_format();
		if (tree_view.view_root != old_root)
			fireTreeContentChanged(this, new TreePath(new Object[]{tree_view.view_root}));
	}

	private void quick_format() {
		tree_view.formatter.format(tree_view.the_root, tree_view.view_root, tree_view.getSyntax());
		tree_view.view_root = tree_view.formatter.getRootDrawable();
	}

	void setRoot(TreeView tree_view) {
		this.tree_view = tree_view;
		if (tree_view.the_root == null) {
			tree_view.view_root = null;
			fireTreeContentChanged(this, new TreePath(new Object[]{new DrawSpace(null,new Draw_SyntaxSpace(null),tree_view.syntax)}));
		} else {
			tree_view.formatter.format(tree_view.the_root, null, tree_view.getSyntax());
			tree_view.view_root = tree_view.formatter.getRootDrawable();
			fireTreeContentChanged(this, new TreePath(new Object[]{tree_view.view_root}));
		}
	}

	Object getRoot() {
		if (tree_view == null)
			return null;
		return tree_view.view_root;
	}

	protected Object getChild(Object parent, int index) {
		Drawable dr = (Drawable)parent;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		if (!(dr instanceof DrawTreeBranch))
			return null;
		return ((DrawTreeBranch)dr).getSubNodes()[index];
	}

	protected int getChildCount(Object parent) {
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

	private boolean isLeaf(Object node) {
		Drawable dr = (Drawable)node;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		return !(dr instanceof DrawTreeBranch);
	}

	protected int getIndexOfChild(Object parent, Object child) {
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

	public void addTreeContentListener(TreeContentListener l) {
		listenerList.add(TreeContentListener.class, l);
	}

	public void removeTreeContentListener(TreeContentListener l) {
		listenerList.remove(TreeContentListener.class, l);
	}

	private void fireTreeContentChanged(Object source, TreePath path) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		TreeContentEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==TreeContentListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new TreeContentEvent(source, path);
				((TreeContentListener)listeners[i+1]).treeStructureChanged(e);
			}
		}
	}

	public void treeCollapsed(TreeExpansionEvent event) {
		TreeViewer treeViewer = (TreeViewer)event.getTreeViewer();
		Object element = event.getElement();
		TreePath[] treePaths = treeViewer.getExpandedTreePaths();
		TreePath path = null;
		for (TreePath p: treePaths){
			for (int i = 0; i< p.getSegmentCount(); i++)
				if (p.getSegment(i) == element){
					path = p;
					break;
				}
		}
		Object obj = path.getLastSegment();
		while (obj instanceof DrawCtrl)
			obj = ((DrawCtrl)obj).getArg();
		if (obj instanceof DrawTreeBranch) {
			DrawTreeBranch nt = (DrawTreeBranch)obj;
			nt.setDrawFolded(true);
			quick_format();
			fireTreeContentChanged(this, path);
		}
	}

	public void treeExpanded(TreeExpansionEvent event) {
	}

	public Object[] getElements(Object root) {
		return getChildren(getRoot());
	}

	public void dispose() {		
	}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {		
	}

	public Object[] getChildren(Object parent) {
		Drawable dr = (Drawable)parent;
		while (dr != null && dr instanceof DrawCtrl)
			dr = ((DrawCtrl)dr).getArg();
		if (!(dr instanceof DrawTreeBranch))
			return EMPTY;
		Drawable[] subNodes = ((DrawTreeBranch)dr).getSubNodes();
		if (subNodes != null) return subNodes;
		return EMPTY;
	}

	public Object getParent(Object child) {
		Drawable dr = (Drawable)child;
		if (dr == getRoot()) return getRoot();
		return dr.parent();
	}

	public boolean hasChildren(Object node) {
		return !isLeaf(node);
	}

}


