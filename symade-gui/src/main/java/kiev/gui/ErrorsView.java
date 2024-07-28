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

import java.util.ArrayList;

import kiev.fmt.*;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.ErrorInfo;
import kiev.vtree.ErrorNodeInfo;
import kiev.vtree.INode;

/**
 * Project View.
 */
public class ErrorsView extends UIView {
	
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
	public ErrorsView(Window window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIView#formatAndPaint(boolean)
	 */
	@Override
	public void formatAndPaint(boolean full) {
		getViewPeer().setCurrent(null, null);
		if (cur_dr != null) {
			INode n = cur_dr.drnode;
			while (n != null && !(n instanceof ErrorInfo))
				n = n.parent();
			getViewPeer().setCurrent(null, n);
		}
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
	 * Jump to node referred by currently selected error
	 * @param dr Drawable
	 */
	public void jumpToNode(Drawable dr) {
		if (dr instanceof DrawTerm)
			selectDrawTerm((DrawTerm)dr);
		INode n = cur_dr.drnode;
		while (n != null && !(n instanceof ErrorInfo))
			n = n.parent();
		if (!(n instanceof ErrorNodeInfo))
			return;
		ErrorNodeInfo eni = (ErrorNodeInfo)n;
		n = (INode)eni.getVal(eni.getAttrSlot("node"));
		if (n == null)
			return;
		FileUnit fu = Env.ctxFileUnit(n);
		if (fu == null)
			return;
		ArrayList<INode> path = new ArrayList<INode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
		}
		window.openEditor(fu, path.toArray(new INode[path.size()]));		
		formatAndPaint(false);
	}

}
