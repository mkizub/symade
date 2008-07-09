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

import kiev.gui.EditActions.Undo;

public final class NavigateView implements Runnable {
	
	final InfoView uiv;
	final int incr;
	
	public NavigateView(InfoView uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	public void run() {
		this.uiv.view_canvas.incrFirstLine(this.incr);
	}

	public static LineUp newLineUp(){
		return new LineUp();
	}
	
	final static class LineUp implements UIActionFactory {
		public String getDescr() { return "Scroll the view one line up"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, -1);
		}
	}
	
	public static LineDn newLineDn(){
		return new LineDn();
	}

	final static class LineDn implements UIActionFactory {
		public String getDescr() { return "Scroll the view one line down"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			return new NavigateView(context.uiv, +1);
		}
	}
	
	public static PageUp newPageUp(){
		return new PageUp();
	}

	final static class PageUp implements UIActionFactory {
		public String getDescr() { return "Scroll the view one page up"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			int lnlst = uiv.view_canvas.last_visible.getGfxFmtInfo().getLineNo();
			int lnfst = uiv.view_canvas.first_visible.getGfxFmtInfo().getLineNo();
			return new NavigateView(uiv, lnfst - lnlst + 1);
		}
	}

	public static PageDn newPageDn(){
		return new PageDn();
	}
	
	final static class PageDn implements UIActionFactory {
		public String getDescr() { return "Scroll the view one page down"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			InfoView uiv = context.uiv;
			int lnlst = uiv.view_canvas.last_visible.getGfxFmtInfo().getLineNo();
			int lnfst = uiv.view_canvas.first_visible.getGfxFmtInfo().getLineNo();
			return new NavigateView(uiv, lnlst - lnfst - 1);
		}
	}
}

