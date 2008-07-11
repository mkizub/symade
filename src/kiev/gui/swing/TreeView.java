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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Drawable;
import kiev.fmt.GfxTreeFormatter;
import kiev.gui.IWindow;
import kiev.gui.UIView;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class TreeView extends UIView implements KeyListener {
	protected final ANodeTree the_tree;
	
	public TreeView(IWindow window, Draw_ATextSyntax syntax, ANodeTree the_tree) {
		super(window, syntax);
		this.the_tree = the_tree;
		this.formatter = new GfxTreeFormatter(the_tree.getFmtGraphics());
		this.the_tree.tree_view = this;
		this.the_tree.addKeyListener(this);
		this.the_tree.addMouseListener(this);
		this.setRoot(null);
	}

	public void setRoot(ANode root) {
		this.the_root = root;
		this.the_tree.setRoot();
	}
	
	@Override
	public void formatAndPaint(boolean full) {
		the_tree.format();
		the_tree.repaint();
	}

	@Override
	public void formatAndPaintLater(ANode node) {
		this.the_root = node;
		this.bg_formatter.schedule_run();
	}
	
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() >= 2) {
			TreePath sel = the_tree.getPathForLocation(e.getX(), e.getY());
			if (sel == null || !(sel.getLastPathComponent() instanceof Drawable))
				return;
			Drawable dr = (Drawable)sel.getLastPathComponent();
			java.util.Vector<ANode> v = new java.util.Vector<ANode>();
			ANode n = dr.get$drnode();
			//if (n instanceof DNode)
			//	n = n.id;
			v.add(n);
			while (n != null && !(n instanceof FileUnit)) {
				n = n.parent();
				v.add(n);
			}
			if (!(n instanceof FileUnit))
				return;
			parent_window.openEditor((FileUnit)n, v.toArray(new ANode[v.size()]));
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
				m.add(new JMenuItem(new RenderActions.SetSyntaxAction(this,"Project Tree Syntax", "stx-fmt\u001fsyntax-for-project-tree", false)));
				m.add(new JMenuItem(new RenderActions.SetSyntaxAction(this,"Project Tree Syntax  (current)", "stx-fmt\u001fsyntax-for-project-tree", true)));
				m.show(the_tree, 0, 0);
				break;
				}
			}
		}
	}
}


