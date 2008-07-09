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

import kiev.fmt.DrawTerm;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

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

	public static GoPrev newGoPrev(){
		return new GoPrev();
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

	public static GoNext newGoNext(){
		return new GoNext();
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
	
	public static GoLineUp newGoLineUp(){
		return new GoLineUp();
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
	
	public static GoLineDn newGoLineDn(){
		return new GoLineDn();
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
	
	public static GoLineHome newGoLineHome(){
		return new GoLineHome();
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
	
	public static GoLineEnd newGoLineEnd(){
		return new GoLineEnd();
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
	
	public static GoPageUp newGoPageUp(){
		return new GoPageUp();
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
	
	public static GoPageDn newGoPageDn(){
		return new GoPageDn();
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
			GfxDrawTermLayoutInfo prev = uiv.cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getPrev();
			if (prev != null) {
				uiv.cur_elem.set(prev.getDrawable());
				uiv.cur_x = prev.getX();
				if (uiv.insert_mode) {
					String text = prev.getDrawable().getText();
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
			GfxDrawTermLayoutInfo next = uiv.cur_elem.dr.getFirstLeaf().getGfxFmtInfo().getNext();
			if (next != null) {
				uiv.cur_elem.set(next.getDrawable());
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
		DrawTerm dt = uiv.cur_elem.dr.getFirstLeaf();
		if (dt == null)
			return;
		GfxDrawTermLayoutInfo n = null;
		GfxDrawTermLayoutInfo prev = dt.getGfxFmtInfo().getPrev();
		while (prev != null) {
			if (prev.get$do_newline()) {
				n = prev;
				break;
			}
			prev = prev.getPrev();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			prev = n.getPrev();
			if (prev == null || prev.get$do_newline())
				break;
			w = prev.getWidth();
			if (prev.getX()+w < uiv.cur_x) 
				break;
			n = prev;
		}
		if (n != null)
			uiv.cur_elem.set(n.getDrawable());
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateDn(Editor uiv, boolean repaint) {
		DrawTerm dt = uiv.cur_elem.dr.getFirstLeaf();
		GfxDrawTermLayoutInfo n = null;
		GfxDrawTermLayoutInfo next = dt.getGfxFmtInfo();
		while (next != null) {
			if (next.get$do_newline()) {
				n = next.getNext();
				break;
			}
			next = next.getNext();
		}
		while (n != null) {
			int w = n.getWidth();
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			next = n.getNext();
			if (next == null)
				break;
			if (next.getX() > uiv.cur_x)
				break;
			if (next.get$do_newline())
				break;
			n = next;
		}
		if (n != null)
			uiv.cur_elem.set(n.getDrawable());
		if (repaint) {
			uiv.makeCurrentVisible();
			uiv.formatAndPaint(false);
		}
	}
	private void navigateLineHome(Editor uiv, boolean repaint) {
		GfxDrawTermLayoutInfo res = uiv.cur_elem.dr.getGfxFmtInfo();
		int lineno = res.getLineNo();
		for (;;) {
			GfxDrawTermLayoutInfo dr = res.getPrev();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res.getDrawable() != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res.getDrawable());
			uiv.cur_x = res.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigateLineEnd(Editor uiv, boolean repaint) {
		GfxDrawTermLayoutInfo res = uiv.cur_elem.dr.getGfxFmtInfo();
		int lineno = res.getLineNo();
		for (;;) {
			GfxDrawTermLayoutInfo dr = res.getNext();
			if (dr == null || dr.getLineNo() != lineno)
				break;
			res = dr;
		}
		if (res.getDrawable() != uiv.cur_elem.dr) {
			uiv.cur_elem.set(res.getDrawable());
			uiv.cur_x = res.getX();
		}
		if (repaint)
			uiv.formatAndPaint(false);
	}
	private void navigatePageUp(Editor uiv) {
		if (uiv.view_canvas.first_visible == null) {
			uiv.view_canvas.setFirstLine(0);
			return;
		}
		int lnlst = uiv.view_canvas.last_visible.getGfxFmtInfo().getLineNo();
		int lnfst = uiv.view_canvas.first_visible.getGfxFmtInfo().getLineNo();
		int offs = lnlst - lnfst -1;
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
		int lnlst = uiv.view_canvas.last_visible.getGfxFmtInfo().getLineNo();
		int lnfst = uiv.view_canvas.first_visible.getGfxFmtInfo().getLineNo();
		int offs = lnlst - lnfst -1;
		uiv.view_canvas.incrFirstLine(+offs);
		for (int i=offs; i >= 0; i--)
			navigateDn(uiv,i==0);
		return;
	}

}

