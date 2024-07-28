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

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.vlang.FileUnit;
import kiev.vtree.INode;

/**
 * Mouse Actions UI Action.
 */
public class MouseActions implements UIAction {

	/**
	 * The view.
	 */
	private final IUIView uiv;
	
	/**
	 * The action.
	 */
	private final String action;
	
	/**
	 * The X coordinate.
	 */
	private final int x;
	
	/**
	 * Y coordinate.
	 */
	private final int y;
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		if (action == "request-focus") {
			uiv.getViewPeer().requestFocus();
		}
		else if (action == "select") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr instanceof DrawTerm) {
				uiv.selectDrawTerm((DrawTerm)dr);
			}
		}
		else if (action == "popup-menu") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr instanceof DrawTerm){ 
				UIActionFactory af;
				if(uiv instanceof Editor) {			
					Editor edt = (Editor)uiv;
					edt.selectDrawTerm((DrawTerm)dr);
					af =  new FunctionExecutor.Factory();
				} else
				if (uiv instanceof ProjectView) {
					af = new ProjectActions.Factory();
				} else return;
				UIAction action = af.getAction(new UIActionViewContext(uiv.getWindow(), null, uiv));
				if (action != null) action.exec();
				return;
			}
		}
		else if (action == "open-close") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr == null) return;
			if (uiv instanceof ProjectView) {
				((ProjectView)uiv).toggleItem(dr);
			} else {
				INode n = dr.drnode;
				if (! (n instanceof FileUnit)) return;
				uiv.getWindow().openEditor((FileUnit)n, INode.emptyArray);
			}
		}
		else if (action == "jump-to-node") {
			uiv.getViewPeer().requestFocus();
			Drawable dr = uiv.getViewPeer().getDrawableAt(x, y);
			if (dr == null) return;
			if (uiv instanceof ErrorsView) {
				((ErrorsView)uiv).jumpToNode(dr);
			}
		}
	}
	
	/**
	 * The constructor.
	 * @param uiv the view
	 * @param action the action
	 */
	public MouseActions(IUIView uiv, String action) {
		this.uiv = uiv;
		this.action = action;
		this.x = -1;
		this.y = -1;
	}
	
	/**
	 * The constructor.
	 * @param uiv the view
	 * @param action the action
	 * @param x the X coordinate
	 * @param y the Y coordinate
	 */
	public MouseActions(IUIView uiv, String action, int x, int y) {
		this.uiv = uiv;
		this.action = action;
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Request Focus UI Action Factory.
	 */
	public final static class RequestFocus implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Request focus"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui == null) return null;
			if (context.evt == null || ! context.evt.isMouseEvent()) return null;
			return new MouseActions(context.ui, "request-focus");
		}
	}
	
	/**
	 * Select UI Action Factory.
	 */
	public final static class Select implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Select drawable"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui == null) return null;
			if (context.evt == null || ! context.evt.isMouseEvent()) return null;
			return new MouseActions(context.ui, "select", context.evt.getX(), context.evt.getY());
		}
	}
	
	/**
	 * Pop-up Context Menu UI Action Factory.
	 */
	public final static class PopupContextMenu implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Popup context menu"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.ui == null) return null;
			if (context.evt == null || ! context.evt.isMouseEvent()) return null;
			return new MouseActions(context.ui, "popup-menu", context.evt.getX(), context.evt.getY());
		}
	}
	
	/**
	 * Tree Toggle UI Action Factory.
	 */
	public final static class TreeToggle implements UIActionFactory {
		public String getDescr() { return "Open/close selection"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (! (context.ui instanceof ProjectView)) return null;
			if (context.evt == null || ! context.evt.isMouseEvent()) return null;
			return new MouseActions(context.ui, "open-close", context.evt.getX(), context.evt.getY());
		}
	}

	/**
	 * Goto by reference.
	 */
	public final static class GotoByRef implements UIActionFactory {
		
		public String getDescr() { return "Jump to node"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (! (context.ui instanceof ErrorsView)) return null;
			if (context.evt == null || ! context.evt.isMouseEvent()) return null;
			return new MouseActions(context.ui, "jump-to-node", context.evt.getX(), context.evt.getY());
		}
	}
}
