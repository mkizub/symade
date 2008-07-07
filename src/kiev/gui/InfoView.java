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

import kiev.vtree.*;
import kiev.fmt.*;
import kiev.gui.event.ElementEvent;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Graphics2D;

import javax.swing.JScrollBar;

import java.util.Hashtable;


/**
 * @author Maxim Kizub
 */

public class InfoView extends UIView implements KeyListener, MouseWheelListener {

	/** The canvas to show definition of current node */
	protected Canvas		view_canvas;


	protected final java.util.Hashtable<InputEventInfo,UIActionFactory> naviMap;

	{
		this.naviMap = new Hashtable<InputEventInfo,UIActionFactory>();
//		final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
//		final int CTRL  = KeyEvent.CTRL_DOWN_MASK;
//		final int ALT   = KeyEvent.ALT_DOWN_MASK;

		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_UP),			new NavigateView.LineUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_DOWN),			new NavigateView.LineDn());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_UP),		new NavigateView.PageUp());
		this.naviMap.put(new InputEventInfo(0,					KeyEvent.VK_PAGE_DOWN),		new NavigateView.PageDn());

//		this.naviMap.put(new InputEventInfo(ALT,				KeyEvent.VK_S),				new FileActions.SaveFileAs());
//		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_S),				new FileActions.SaveFile());
//		this.naviMap.put(new InputEventInfo(ALT,				KeyEvent.VK_S),				new FileActions.LoadFileAs());

//		this.naviMap.put(new InputEventInfo(CTRL+ALT,			KeyEvent.VK_S),				new RenderActions.SyntaxFileAs());
//		this.naviMap.put(new InputEventInfo(CTRL+ALT,			KeyEvent.VK_O),				new RenderActions.OpenFoldedAll());
//		this.naviMap.put(new InputEventInfo(SHIFT+CTRL+ALT,	KeyEvent.VK_O),				new RenderActions.CloseFoldedAll());
//		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_H),				new RenderActions.ToggleShowAutoGenerated());
//		this.naviMap.put(new InputEventInfo(CTRL,				KeyEvent.VK_R),				new RenderActions.Redraw());
	}

	public InfoView(Window window, Draw_ATextSyntax syntax, Canvas view_canvas) {
		super(window, syntax);
		this.view_canvas = view_canvas;
		this.formatter = new GfxFormatter((Graphics2D)view_canvas.getGraphics());
		view_canvas.addMouseListener(this);
		view_canvas.addComponentListener(this);
		view_canvas.addKeyListener(this);
		view_canvas.addMouseWheelListener(this);
	}

	public void setRoot(ANode root) {
		this.the_root = root;
		view_root = null;
		view_canvas.dr_root = null;
		view_canvas.dlb_root = null;
		if (the_root != null) {
			formatter.format(the_root, view_root, getSyntax());
			view_canvas.dr_root = view_root = formatter.getRootDrawable();
			view_canvas.dlb_root = formatter.getRootDrawLayoutBlock();
		}
	}

	@Override
	public void formatAndPaint(boolean full) {
		this.formatter.setWidth(view_canvas.imgWidth);
		this.formatter.setShowAutoGenerated(this.show_auto_generated);
		this.formatter.setShowPlaceholders(this.show_placeholders);
		this.formatter.setHintEscapes(this.show_hint_escapes);
		view_canvas.dr_root = null;
		view_canvas.dlb_root = null;
		if (the_root != null && full) {
			formatter.format(the_root, view_root, getSyntax());
			view_canvas.dr_root = view_root = formatter.getRootDrawable();
			view_canvas.dlb_root = formatter.getRootDrawLayoutBlock();
		}
		view_canvas.repaint();
	}

	@Override
	public void formatAndPaintLater(ANode node) {
		the_root = node;
		bg_formatter.schedule_run();
	}

	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollAmount() != 0) {
			JScrollBar toScroll = view_canvas.verticalScrollBar;
			int direction = 0;
			// find which scrollbar to scroll, or return if none
			if (toScroll == null || !toScroll.isVisible()) { 
				//toScroll = scrollpane.getHorizontalScrollBar();
				//if (toScroll == null || !toScroll.isVisible()) { 
				//	return;
				//}
				return;
			}
			direction = e.getWheelRotation() < 0 ? -1 : 1;
			if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
				scrollByUnits(toScroll, direction, e.getScrollAmount());
			else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL)
				scrollByBlock(toScroll, direction);
		}
	}
	static void scrollByBlock(JScrollBar scrollbar, int direction) {
		// This method is called from BasicScrollPaneUI to implement wheel
		// scrolling, and also from scrollByBlock().
		int oldValue = scrollbar.getValue();
		int blockIncrement = scrollbar.getBlockIncrement(direction);
		int delta = blockIncrement * ((direction > 0) ? +1 : -1);
		int newValue = oldValue + delta;

		// Check for overflow.
		if (delta > 0 && newValue < oldValue) {
			newValue = scrollbar.getMaximum();
		}
		else if (delta < 0 && newValue > oldValue) {
			newValue = scrollbar.getMinimum();
		}

		scrollbar.setValue(newValue);			
	}
	static void scrollByUnits(JScrollBar scrollbar, int direction,
			int units) {
		// This method is called from BasicScrollPaneUI to implement wheel
		// scrolling, as well as from scrollByUnit().
		int delta;

		for (int i=0; i<units; i++) {
			if (direction > 0) {
				delta = scrollbar.getUnitIncrement(direction);
			}
			else {
				delta = -scrollbar.getUnitIncrement(direction);
			}

			int oldValue = scrollbar.getValue();
			int newValue = oldValue + delta;

			// Check for overflow.
			if (delta > 0 && newValue < oldValue) {
				newValue = scrollbar.getMaximum();
			}
			else if (delta < 0 && newValue > oldValue) {
				newValue = scrollbar.getMinimum();
			}
			if (oldValue == newValue) {
				break;
			}
			scrollbar.setValue(newValue);
		}
	}


	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}

	public void keyPressed(KeyEvent evt) {
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		UIActionFactory af = naviMap.get(new InputEventInfo(mask, code));
		Runnable r = (af == null) ? null : af.getAction(new UIActionViewContext(this.parent_window, this));
		if (r != null) {
			evt.consume();
			r.run();
		} else {
			if (!(code==KeyEvent.VK_SHIFT || code==KeyEvent.VK_ALT || code==KeyEvent.VK_ALT_GRAPH || code==KeyEvent.VK_CONTROL || code==KeyEvent.VK_CAPS_LOCK))
				java.awt.Toolkit.getDefaultToolkit().beep();
		}
	}

	@Override
	public void elementChanged(ElementEvent e) {
		super.elementChanged(e);
		ANode node = ((Editor)e.getSource()).cur_elem.node;
		formatAndPaintLater(node);
		
	}
}

