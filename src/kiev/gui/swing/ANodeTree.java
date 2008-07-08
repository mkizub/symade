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

import javax.swing.JTree;


public class ANodeTree extends JTree {
	private static final long serialVersionUID = 6245260955964490417L;
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
