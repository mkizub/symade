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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.GfxTreeFormatter;
import kiev.gui.swing.ANodeTree;
import kiev.vtree.ANode;

public abstract class TreeView extends UIView implements KeyListener {
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
	

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	/**
	 * @return the the_tree
	 */
	public ANodeTree getThe_tree() {
		return the_tree;
	}
	
}


