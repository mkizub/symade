/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui;

import kiev.fmt.*;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.INode;

/**
 * Project View.
 */
public class ProjectView extends UIView {
	
	/**
	 * The current drawable.
	 */
	private DrawTerm cur_dr;

	/**
	 * The constructor.
	 * @param window Window
	 * @param view_canvas ICanvas
	 * @param syntax Draw_ATextSyntax
	 */
	public ProjectView(Window window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIView#formatAndPaint(boolean)
	 */
	@Override
	public void formatAndPaint(boolean full) {
		getViewPeer().setCurrent(cur_dr, null);
		if (full) {
			formatter.setWidth(getViewPeer().getImgWidth());
			getViewPeer().setDlb_root(null);
			if (getRoot() != null && full) {
				formatter.format(getRoot(), view_root, getSyntax(), getStyle());
				view_root = formatter.getRootDrawable();
				getViewPeer().setDlb_root(formatter.getRootDrawLayoutBlock());
			}
		}
		getViewPeer().repaint();
	}

	/**
	 * select Draw Term
	 * @param dr DrawTerm
	 */
	public void selectDrawTerm(DrawTerm dr) {
		cur_dr = (DrawTerm)dr;
		formatAndPaint(false);
	}

	/**
	 * toggle Item
	 * @param dr Drawable
	 */
	public void toggleItem(Drawable dr) {
		if (dr instanceof DrawTerm) cur_dr = (DrawTerm)dr;
		INode n = dr.drnode;
		if (n instanceof FileUnit) window.openEditor((FileUnit)n, INode.emptyArray);		
		if (cur_dr.parent() instanceof DrawTreeBranch) {
			DrawTreeBranch dtb = (DrawTreeBranch)cur_dr.parent();
			dtb.setDrawFolded(!dtb.getDrawFolded());
			formatAndPaint(true);
			return;
		}
		else if (cur_dr.parent() instanceof DrawNonTermSet && cur_dr.parent().parent() instanceof DrawTreeBranch) {
			DrawTreeBranch dtb = (DrawTreeBranch)cur_dr.parent().parent();
			dtb.setDrawFolded(! dtb.getDrawFolded());
			formatAndPaint(true);
			return;
		}
		formatAndPaint(false);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IUIView#formatAndPaintLater()
	 */
	public void formatAndPaintLater() {}
	
}
