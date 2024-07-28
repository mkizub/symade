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
import kiev.fmt.DrawValueTerm;
import kiev.fmt.common.DrawLayoutInfo;

/**
 * Navigate Editor UI Action.
 */
public final class NavigateEditor implements UIAction {

	/**
	 * The editor.
	 */
	private final Editor uiv;
	
	/**
	 * The increment.
	 */
	private final int incr;
	
	/**
	 * The constructor.
	 * @param uiv the view
	 * @param incr the increment
	 */
	public NavigateEditor(Editor uiv, int incr) {
		this.uiv = uiv;
		this.incr = incr;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		switch (incr) {
		case -1: navigatePrev(true); break;
		case +1: navigateNext(true); break;
		case -2: navigateUp(true); break;
		case +2: navigateDn(true); break;
		case -3: navigateWordHome(true); break;
		case +3: navigateWordEnd(true); break;
		case -4: navigateLineHome(true); break;
		case +4: navigateLineEnd(true); break;
		case -5: navigatePageUp(); break;
		case +5: navigatePageDn(); break;
		}
	}

	/**
	 * GoPrev Action Factory.
	 */
	public final static class GoPrev implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to the previous element"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-1);
			return null;
		}
	}

	/**
	 * GoNext Action Factory.
	 */
	public final static class GoNext implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to the next element"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+1);
			return null;
		}
	}
	
	/**
	 * Go Line Up UI Action Factory.
	 */
	public final static class GoLineUp implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to an element above"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-2);
			return null;
		}
	}
	
	/**
	 * Go Line Dn UI Action Factory.
	 */
	public final static class GoLineDn implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to an element below"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+2);
			return null;
		}
	}
	
	/**
	 * Go Line Home UI Action Factory.
	 */
	public final static class GoLineHome implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to the first element on the line"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null) {
				Editor editor = context.editor;
				if (editor.isInTextEditMode()) {
					String text = editor.getEditText();
					int offs = editor.getEditOffset();
					if (text != null && offs > 0)
						return new NavigateEditor(context.editor,-3);
				}
				return new NavigateEditor(context.editor,-4);
			}
			return null;
		}
	}
	
	/**
	 * Go Line End UI Action Factory.
	 */
	public final static class GoLineEnd implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to the last element on the line"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null) {
				Editor editor = context.editor;
				if (editor.isInTextEditMode()) {
					String text = editor.getEditText();
					int offs = editor.getEditOffset();
					if (text != null && offs < text.length())
						return new NavigateEditor(context.editor,+3);
				}
				return new NavigateEditor(context.editor,+4);
			}
			return null;
		}
	}
	
	/**
	 * Go Page Up UI Action Factory.
	 */
	public final static class GoPageUp implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to an element one screen above"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,-5);
			return null;
		}
	}
	
	/**
	 * Go Page Down UI Action Factory.
	 */
	public final static class GoPageDn implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Go to an element one screen below"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new NavigateEditor(context.editor,+5);
			return null;
		}
	}

	private void repaintUIV() {
		uiv.makeCurrentVisible();
		uiv.formatAndPaint(false);
	}
	
	/**
	 * Navigate Previous.
	 * @param repaint force repaint
	 */
	private void navigatePrev(boolean repaint) {
		DrawTerm curr = uiv.getDrawTerm();
		if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr instanceof DrawValueTerm) {
			int cursor = uiv.getViewPeer().getCursor_offset();
			if (cursor > 0) {
				uiv.getViewPeer().setCursor_offset(cursor-1);
				if (repaint) repaintUIV();
				return;
			}
		}
		DrawLayoutInfo prev = uiv.getDrawTerm().getFirstLeaf().getGfxFmtInfo().getPrevLeaf();
		if (prev != null) {
			uiv.setDrawTerm((DrawTerm)prev.getDrawable());
			uiv.cur_x = prev.getX();
			curr = uiv.getDrawTerm();
			if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr instanceof DrawValueTerm) {
				Object term_obj = ((DrawTerm)prev.getDrawable()).getTermObj();
				if (term_obj == null || term_obj == DrawTerm.NULL_NODE || term_obj == DrawTerm.NULL_VALUE) {
					uiv.getViewPeer().setCursor_offset(-1);
				} else {
					String text = String.valueOf(term_obj);
					if (text == null)
						uiv.getViewPeer().setCursor_offset(-1);
					else
						uiv.getViewPeer().setCursor_offset(text.length());
				}
			}
		}
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Next.
	 * @param repaint force repaint
	 */
	private void navigateNext(boolean repaint) {
		DrawTerm curr = uiv.getDrawTerm();
		if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr  instanceof DrawValueTerm) {
			Object term_obj = curr.getTermObj();
			String text;
			if (term_obj == null || term_obj == DrawTerm.NULL_NODE || term_obj == DrawTerm.NULL_VALUE)
				text = "";
			else
				text = String.valueOf(term_obj);
			if (text == null)
				text = "";
			int cursor = uiv.getViewPeer().getCursor_offset();
			if (cursor < text.length()) {
				uiv.getViewPeer().setCursor_offset(cursor+1);
				if (repaint) repaintUIV();
				return;
			}
		}
		DrawLayoutInfo next = curr.getFirstLeaf().getGfxFmtInfo().getNextLeaf();
		if (next != null) {
			uiv.setDrawTerm((DrawTerm)next.getDrawable());
			uiv.cur_x = next.getX();
			curr = uiv.getDrawTerm();
			if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr  instanceof DrawValueTerm)
				uiv.getViewPeer().setCursor_offset(0);
			else
				uiv.getViewPeer().setCursor_offset(-1);
		}
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Up.
	 * @param repaint force repaint
	 */
	private void navigateUp(boolean repaint) {
		DrawTerm dt = uiv.getDrawTerm().getFirstLeaf();
		if (dt == null)
			return;
		DrawLayoutInfo n = null;
		DrawLayoutInfo prev = dt.getGfxFmtInfo().getPrevLeaf();
		while (prev != null) {
			if (prev.isDoNewline()) {
				n = prev;
				break;
			}
			prev = prev.getPrevLeaf();
		}
		while (n != null) {
			int w = n.width;
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			prev = n.getPrevLeaf();
			if (prev == null || prev.isDoNewline())
				break;
			w = prev.width;
			if (prev.getX()+w < uiv.cur_x) 
				break;
			n = prev;
		}
		if (n != null)
			uiv.setDrawTerm((DrawTerm)n.getDrawable());
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Down.
	 * @param repaint force repaint
	 */
	private void navigateDn(boolean repaint) {
		DrawTerm dt = uiv.getDrawTerm().getFirstLeaf();
		DrawLayoutInfo n = null;
		DrawLayoutInfo next = dt.getGfxFmtInfo();
		while (next != null) {
			if (next.isDoNewline()) {
				n = next.getNextLeaf();
				break;
			}
			next = next.getNextLeaf();
		}
		while (n != null) {
			int w = n.width;
			if (n.getX() <= uiv.cur_x && n.getX()+w >= uiv.cur_x) 
				break;
			next = n.getNextLeaf();
			if (next == null)
				break;
			if (next.getX() > uiv.cur_x)
				break;
			if (next.isDoNewline())
				break;
			n = next;
		}
		if (n != null)
			uiv.setDrawTerm((DrawTerm)n.getDrawable());
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Previous.
	 * @param repaint force repaint
	 */
	private void navigateWordHome(boolean repaint) {
		DrawTerm curr = uiv.getDrawTerm();
		if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr instanceof DrawValueTerm) {
			uiv.getViewPeer().setCursor_offset(0);
			if (repaint) repaintUIV();
		}
	}
	
	/**
	 * Navigate Next.
	 * @param repaint force repaint
	 */
	private void navigateWordEnd(boolean repaint) {
		DrawTerm curr = uiv.getDrawTerm();
		if ((uiv.isInInsertMode() || uiv.isInTextEditMode()) && curr  instanceof DrawValueTerm) {
			Object term_obj = curr.getTermObj();
			String text;
			if (term_obj == null || term_obj == DrawTerm.NULL_NODE || term_obj == DrawTerm.NULL_VALUE)
				text = "";
			else
				text = String.valueOf(term_obj);
			if (text == null)
				text = "";
			uiv.getViewPeer().setCursor_offset(text.length());
			if (repaint) repaintUIV();
			return;
		}
	}
	
	/**
	 * Navigate Line Home.
	 * @param repaint force repaint
	 */
	private void navigateLineHome(boolean repaint) {
		DrawLayoutInfo res = uiv.getDrawTerm().getGfxFmtInfo();
		int line_y = res.getY();
		for (;;) {
			DrawLayoutInfo dr = res.getPrevLeaf();
			if (dr == null || dr.getY() != line_y)
				break;
			res = dr;
		}
		if (uiv.isInInsertMode() || uiv.isInTextEditMode())
			uiv.getViewPeer().setCursor_offset(0);
		if (res.getDrawable() != uiv.getDrawTerm()) {
			uiv.setDrawTerm((DrawTerm)res.getDrawable());
			uiv.cur_x = res.getX();
		}
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Line End.
	 * @param repaint force repaint
	 */
	private void navigateLineEnd(boolean repaint) {
		DrawLayoutInfo res = uiv.getDrawTerm().getGfxFmtInfo();
		int line_y = res.getY();
		for (;;) {
			DrawLayoutInfo dr = res.getNextLeaf();
			if (dr == null || dr.getY() != line_y)
				break;
			res = dr;
		}
		if (uiv.isInInsertMode() || uiv.isInTextEditMode()) {
			String text = uiv.getEditText();
			if (text != null)
				uiv.getViewPeer().setCursor_offset(text.length());
		}
		if (res.getDrawable() != uiv.getDrawTerm()) {
			uiv.setDrawTerm((DrawTerm)res.getDrawable());
			uiv.cur_x = res.getX();
		}
		if (repaint) repaintUIV();
	}
	
	/**
	 * Navigate Page Up.
	 */
	private void navigatePageUp() {
		if (uiv.getViewPeer().getFirst_visible() == null) {
			uiv.getViewPeer().setVertOffset(0);
			return;
		}
		int pos_y = uiv.getViewPeer().getVertOffset() - uiv.getViewPeer().getImgHeight();
		if (pos_y < 0)
			pos_y = 0;
		uiv.getViewPeer().setVertOffset(pos_y);
		DrawTerm last_dt = uiv.getDrawTerm();
		while (last_dt.getGfxFmtInfo().getY() > pos_y) {
			navigateUp(false);
			DrawTerm cur_dt = uiv.getDrawTerm();
			if (last_dt == cur_dt)
				break;
			last_dt = cur_dt;
		}
		repaintUIV();
	}
	
	/**
	 * Navigate Page Down.
	 */
	private void navigatePageDn() {
		if (uiv.getViewPeer().getFirst_visible() == null) {
			uiv.getViewPeer().setVertOffset(0);
			return;
		}
		int pos_y = uiv.getViewPeer().getVertOffset() + uiv.getViewPeer().getImgHeight();
		uiv.getViewPeer().setVertOffset(pos_y);
		DrawTerm last_dt = uiv.getDrawTerm();
		while (last_dt.getGfxFmtInfo().getY() < pos_y) {
			navigateDn(false);
			DrawTerm cur_dt = uiv.getDrawTerm();
			if (last_dt == cur_dt)
				break;
			last_dt = cur_dt;
		}
		repaintUIV();
	}

}

