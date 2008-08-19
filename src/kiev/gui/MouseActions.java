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

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class MouseActions implements Runnable {

	final UIView uiv;
	final String action;
	final int x;
	final int y;
	
	public void run() {
		if (action == "request-focus") {
			uiv.getViewPeer().requestFocus();
		}
		else if (action == "select") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr instanceof DrawTerm && uiv instanceof Editor) {
				Editor edt = (Editor)uiv;
				edt.selectDrawTerm((DrawTerm)dr);
			}
			else if (dr instanceof DrawTerm && uiv instanceof ProjectView) {
				ProjectView prj = (ProjectView)uiv;
				prj.selectDrawTerm((DrawTerm)dr);
			}
		}
		else if (action == "popup-menu") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr instanceof DrawTerm && uiv instanceof Editor) {
				Editor edt = (Editor)uiv;
				edt.selectDrawTerm((DrawTerm)dr);
				UIActionFactory af =  UIManager.newFunctionExecutorFactory();
				Runnable r = af.getAction(new UIActionViewContext(edt.parent_window, null, edt));
				if (r != null)
					r.run();
				return;
			}
		}
		else if (action == "open-close") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr == null)
				return;
			if (uiv instanceof ProjectView) {
				((ProjectView)uiv).toggleItem(dr);
			} else {
				ANode n = dr.drnode;
				if (!(n instanceof FileUnit))
					return;
				uiv.parent_window.openEditor((FileUnit)n, ANode.emptyArray);
			}
		}
	}
	
	MouseActions(UIView uiv, String action) {
		this.uiv = uiv;
		this.action = action;
		this.x = -1;
		this.y = -1;
	}
	
	MouseActions(UIView uiv, String action, int x, int y) {
		this.uiv = uiv;
		this.action = action;
		this.x = x;
		this.y = y;
	}
	
	public final static class RequestFocus implements UIActionFactory {
		public String getDescr() { return "Request focus"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.ui == null)
				return null;
			if (context.evt == null || !context.evt.isMouseEvent())
				return null;
			return new MouseActions(context.ui, "request-focus");
		}
	}
	
	public final static class Select implements UIActionFactory {
		public String getDescr() { return "Select drawable"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.ui == null)
				return null;
			if (context.evt == null || !context.evt.isMouseEvent())
				return null;
			return new MouseActions(context.ui, "select", context.evt.getX(), context.evt.getY());
		}
	}
	
	public final static class PopupContextMenu implements UIActionFactory {
		public String getDescr() { return "Popup context menu"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.ui == null)
				return null;
			if (context.evt == null || !context.evt.isMouseEvent())
				return null;
			return new MouseActions(context.ui, "popup-menu", context.evt.getX(), context.evt.getY());
		}
	}
	
	public final static class TreeToggle implements UIActionFactory {
		public String getDescr() { return "Open/close selection"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (!(context.ui instanceof ProjectView))
				return null;
			if (context.evt == null || !context.evt.isMouseEvent())
				return null;
			return new MouseActions(context.ui, "open-close", context.evt.getX(), context.evt.getY());
		}
	}
	
}
