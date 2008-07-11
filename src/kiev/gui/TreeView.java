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

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.GfxTreeFormatter;
import kiev.vtree.ANode;

public class TreeView extends UIView {
	protected final INodeTree the_tree;
	
	public TreeView(IWindow window, INodeTree the_tree, Draw_ATextSyntax syntax) {
		super(window, the_tree, syntax);
		this.the_tree = the_tree;
		this.the_tree.setUIView(this);
		this.formatter = new GfxTreeFormatter(the_tree.getFmtGraphics());
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
	
	/**
	 * @return the view_tree
	 */
	public INodeTree getView_tree() {
		return the_tree;
	}

}


