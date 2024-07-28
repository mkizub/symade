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

/**
 * Navigate View UI Action.
 */
public final class NavigateView implements UIAction {
	
	/**
	 * The view.
	 */
	private final IUIView uiv;
	
	/**
	 * The increment.
	 */
	private final int incr;
	
	/**
	 * The constructor.
	 * @param uiv the view
	 * @param incr the increment
	 */
	public NavigateView(IUIView uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		((UIView)this.uiv).getViewPeer().incrVertOffset(this.incr);
	}

	
	/**
	 * Line Up UI Action Factory.
	 */
	public final static class LineUp implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Scroll the view one line up"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new NavigateView(context.ui, -20);
		}
	}
	
	/**
	 * Line Down UI Action Factory.
	 */
	public final static class LineDn implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Scroll the view one line down"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			return new NavigateView(context.ui, +20);
		}
	}
	
	/**
	 * Page Up UI Action Factory.
	 */
	public final static class PageUp implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Scroll the view one page up"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			UIView uiv = (UIView)context.ui;
			return new NavigateView(uiv, -uiv.getViewPeer().getImgHeight());
		}
	}
	
	/**
	 * Page Down UI Action Factory.
	 */
	public final static class PageDn implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Scroll the view one page down"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			UIView uiv = (UIView)context.ui;
			return new NavigateView(uiv, uiv.getViewPeer().getImgHeight());
		}
	}
}

