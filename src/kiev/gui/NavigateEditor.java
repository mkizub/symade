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

public class NavigateEditor implements Runnable {

	final Editor uiv;
	final int incr;
	
	public NavigateEditor(Editor uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	public void run() {
		switch (incr) {
		case -1: navigatePrev(uiv,true); break;
		case +1: navigateNext(uiv,true); break;
		case -2: navigateUp(uiv,true); break;
		case +2: navigateDn(uiv,true); break;
		case -3: navigateLineHome(uiv,true); break;
		case +3: navigateLineEnd(uiv,true); break;
		case -4: navigatePageUp(uiv); break;
		case +4: navigatePageDn(uiv); break;
		}
	}
	
	final static class GoPrev implements UIActionFactory {
		public String getDescr() { return "Go to the previous element"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-1);
			return null;
		}
	}
	final static class GoNext implements UIActionFactory {
		public String getDescr() { return "Go to the next element"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+1);
			return null;
		}
	}
	final static class GoLineUp implements UIActionFactory {
		public String getDescr() { return "Go to an element above"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-2);
			return null;
		}
	}
	final static class GoLineDn implements UIActionFactory {
		public String getDescr() { return "Go to an element below"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+2);
			return null;
		}
	}
	final static class GoLineHome implements UIActionFactory {
		public String getDescr() { return "Go to the first element on the line"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-3);
			return null;
		}
	}
	final static class GoLineEnd implements UIActionFactory {
		public String getDescr() { return "Go to the last element on the line"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+3);
			return null;
		}
	}
	final static class GoPageUp implements UIActionFactory {
		public String getDescr() { return "Go to an element one screen above"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-4);
			return null;
		}
	}
	final static class GoPageDn implements UIActionFactory {
		public String getDescr() { return "Go to an element one screen below"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+4);
			return null;
		}
	}

	private void navigatePrev(Editor uiv, boolean repaint) {
		if (uiv.insert_mode && uiv.view_canvas.cursor_offset > 0) {
			uiv.view_canvas.cursor_offset --;
		} else {
			DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf().getPrevLeaf();
			if (prev != null) {
				uiv.cur_elem.set(prev);
				uiv.cur_x = prev.getX();
				if (uiv.insert_mode) {
					String text = prev.getText();
					if (text != null)
						uiv.view_canvas.cursor_offset = text.length();
					else
						uiv.view_canvas.cursor_offset = 0;
				}
			}
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateNext(Editor uiv, boolean repaint) {
		DrawTerm curr = uiv.cur_elem.dr;
		if (curr != null && curr.getText() != null && uiv.insert_mode && uiv.view_canvas.cursor_offset < curr.getText().length()) {
			uiv.view_canvas.cursor_offset ++;
		} else {
			DrawTerm next = uiv.cur_elem.dr.getFirstLeaf().getNextLeaf();
			if (next != null) {
				uiv.cur_elem.set(next);
				uiv.cur_x = next.getX();
				uiv.view_canvas.cursor_offset = 0;
			}
		}
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateUp(Editor uiv, boolean repaint) {
		DrawTerm n = null;
		DrawTerm prev = uiv.cur_elem.dr.getFirstLeaf();
		if (prev != null)
			prev = prev.getPrevLeaf();
		while (prev != null) {
			if (prev.get$do_newline()) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.get$do_newline())
				break;
			w = prev.getWidth();
			if (prev.getX()+w < uiv.cur_x) 
				break;
			n = prev;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateDn(Editor uiv, boolean repaint) {
		DrawTerm n = null;
		DrawTerm next = uiv.cur_elem.dr.getFirstLeaf();
		while (next != null) {
			if (next.get$do_newline()) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.getX() > uiv.cur_x)
				break;
			if (next.get$do_newline())
				break;
			n = next;
		}
		if (n != null)
			uiv.cur_elem.set(n);
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateLineHome(Editor uiv, boolean repaint) {
		int lineno = uiv.cur_elem.dr.getFirstLeaf().getLineNo();
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getPrevLeaf();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(Editor uiv, boolean repaint) {
		int lineno = uiv.cur_elem.dr.getFirstLeaf().getLineNo();
		DrawTerm res = uiv.cur_elem.dr;
		for (;;) {
			DrawTerm dr = res.getNextLeaf();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res);
			uiv.cur_x = uiv.cur_elem.dr.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int offs = uiv.view_canvas.last_visible.getLineNo() - uiv.view_canvas.first_visible.getLineNo() -1;
		uiv.view_canvas.incrFirstLine(-offs);
		for (int i=offs; i >= 0; i--)
			navigateUp(uiv,i==0);
		return;
	}
	private void navigatePageDn(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int offs = uiv.view_canvas.last_visible.getLineNo() - uiv.view_canvas.first_visible.getLineNo() -1;
		uiv.view_canvas.incrFirstLine(+offs);
		for (int i=offs; i >= 0; i--)
			navigateDn(uiv,i==0);
		return;
	}

}

