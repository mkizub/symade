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
package kiev.gui.swt;


import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

import sun.security.action.GetBooleanAction;

import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.IFmtGfx;
import kiev.gui.ICanvas;
import kiev.gui.IUIView;
import kiev.gui.UIView;
import kiev.gui.event.InputEventInfo;
import kiev.gui.swing.AWTGraphics2D;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;

public class Canvas extends Composite implements ICanvas, 
KeyListener, MouseListener, MouseWheelListener, SelectionListener, ControlListener
{
	private static final long serialVersionUID = 4713633504436057499L;
	static Color defaultTextColor;
	static java.awt.Color autoGenTextColor;
	static Color selectedNodeColor;
	static Font defaultTextFont;

	private UIView            ui_view;

	private ScrollBar        verticalScrollBar;
	private int               imgWidth;
	private int               imgHeight;

	private DrawLayoutInfo    dlb_root;
	private DrawTerm          current;
	private ANode             current_node;
	private int               first_line;
	private int               num_lines;
	private int               cursor_offset = -1;

	transient Image   vImg;

	int                       lineno;
	boolean                   translated;
	private GfxDrawTermLayoutInfo	first_visible;
	private GfxDrawTermLayoutInfo	last_visible;
	private int               translated_y;
	private int               drawed_x;
	private int               drawed_y;
	private int               bg_drawed_x;
	private int               bg_drawed_y;
	private boolean           selected;

	private Composite parent;
	final Renderer renderer = new Renderer();

	Listener paintListener = new Listener() {
		public void handleEvent(Event e) {
			if (e.gc == null) return;
			GC gc = e.gc;
			Rectangle bounds = getClientArea();
			if (bounds.width != imgWidth || bounds.height != imgHeight){
				Rectangle imgBounds = new Rectangle(bounds.x, bounds.y, imgWidth, imgHeight);				
				gc.setClipping(imgBounds);
			}
			renderer.prepareRendering(gc);
			renderOffscreen();
			renderer.render(gc);
		}
	};

	public Canvas(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
//		this.setFocusable(true);
		defaultTextColor = getDisplay().getSystemColor(SWT.COLOR_BLACK);
		autoGenTextColor = java.awt.Color.GRAY;
		selectedNodeColor = new Color(getDisplay(), 224,224,224);
		defaultTextFont = new Font(getDisplay(), "Dialog", 12, SWT.NONE);
		verticalScrollBar = getVerticalBar();
		verticalScrollBar.addSelectionListener(this);
//		this.add(this.verticalScrollBar);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		imgWidth = 100;
		imgHeight = 100;
		addListener(SWT.Paint, paintListener);
	}

	public void setUIView(IUIView uiv) {
		if (uiv instanceof UIView){
			this.ui_view = (UIView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

//	public IFmtGfx getFmtGraphics() {
//	return new Graphics2D((Graphics2D)this.getGraphics());
//	}

	public int getImgWidth() { return imgWidth; }
	public int getImgHeight() { return imgHeight; }

	public void setBounds(int x, int y, int width, int height) {
		int pw = verticalScrollBar.getSize().x;
		int oldWidth = imgWidth;
		imgWidth = width - pw;
		imgHeight = height;
//		verticalScrollBar.setBounds(imgWidth,0,pw,height);
//		super.setBounds(x, y, width, height);
		if (oldWidth != imgWidth && this.ui_view != null)
			this.ui_view.formatAndPaint(true);
	}

	public void setFirstLine(int val) {
//		verticalScrollBar.setValue(val);
	}

	public void incrFirstLine(int val) {
//		verticalScrollBar.setValue(first_line+val);
	}

	public Drawable getDrawableAt(int x, int y) {
		y += translated_y;
		GfxDrawTermLayoutInfo dr = first_visible;
		GfxDrawTermLayoutInfo last = last_visible;
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y)
				return dr.dterm;
			if (dr == last)
				return null;
		}
		return null;
	}

	public void keyReleased(KeyEvent evt) {}
	public void keyTyped(KeyEvent evt) {}
	public void keyPressed(KeyEvent evt) {
		KeyListener item_editor = (KeyListener)ui_view.getItem_editor();
		if (item_editor != null) {
			item_editor.keyPressed(evt);
			return;
		}
		boolean consume = ui_view.inputEvent(new InputEventInfo(evt));
		if (consume) {
//			evt.consume();
			return;
		}
		int code = evt.keyCode;
		int mask = evt.stateMask & (SWT.CTRL +SWT.DOWN|SWT.SHIFT +SWT.DOWN|SWT.ALT +SWT.DOWN);
		if (mask == 0) {
			if (!(code==SWT.SHIFT || code==SWT.ALT || code==SWT.ALT + SWT.PRINT_SCREEN || code==SWT.CONTROL || code==SWT.CAPS_LOCK))
				getDisplay().beep();
			return;
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent evt) {
		boolean consume = ui_view.inputEvent(new InputEventInfo(evt));
		if (consume) {
//			evt.consume();
			return;
		}
	}

	public void controlResized(ControlEvent e) {
		this.ui_view.formatAndPaint(true);
	}

//	public void adjustmentValueChanged(AdjustmentEvent e) {
//	if (e.getAdjustable() == verticalScrollBar) {
//	first_line = e.getValue();
//	if (first_line >= num_lines)
//	first_line = num_lines-1;
//	if (first_line < 0)
//	first_line = 0;
//	this.repaint();
//	}
//	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollAmount() != 0) {
			ScrollBar toScroll = getVerticalScrollBar();
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
	private static void scrollByBlock(ScrollBar scrollbar, int direction) {
		// This method is called from BasicScrollPaneUI to implement wheel
		// scrolling, and also from scrollByBlock().
		int oldValue = scrollbar.getSelection();
		int blockIncrement = scrollbar.getPageIncrement();
		int delta = blockIncrement * ((direction > 0) ? +1 : -1);
		int newValue = oldValue + delta;

		// Check for overflow.
		if (delta > 0 && newValue < oldValue) {
			newValue = scrollbar.getMaximum();
		}
		else if (delta < 0 && newValue > oldValue) {
			newValue = scrollbar.getMinimum();
		}

		scrollbar.setSelection(newValue);			
	}
	private static void scrollByUnits(ScrollBar scrollbar, int direction,
			int units) {
		// This method is called from BasicScrollPaneUI to implement wheel
		// scrolling, as well as from scrollByUnit().
		int delta;

		for (int i=0; i<units; i++) {
			if (direction > 0) {
				delta = scrollbar.getIncrement();
			}
			else {
				delta = -scrollbar.getIncrement();
			}

			int oldValue = scrollbar.getSelection();
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
			scrollbar.setSelection(newValue);
		}
	}

	public Image createImage(Device device, int width, int height) {
		System.out.println("create image "+width+" : "+height);
		Image image = new Image(device, width, height);
		GC gc = new GC(image);
		Rectangle rect = image.getBounds();
		gc.fillRectangle(rect);
		gc.drawRectangle(rect.x, rect.y, rect.width - 1, rect.height - 1);
		gc.dispose();
		return image;
	}

	void renderOffscreen() {
		Graphics2D g = renderer.getGraphics2D();
		g.setClip(0, 0, getBounds().width, getBounds().height);
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, getBounds().width, getBounds().height);
		//g.clearRect(0, 0, getWidth(), getHeight());
		if (dlb_root != null) {
			lineno = 1;
			translated = false;
			first_visible = null;
			last_visible = null;
			translated_y = 0;
			drawed_x = -1;
			drawed_y = -1;
			bg_drawed_x = -1;
			bg_drawed_y = -1;
			selected = false;
			//is_editable = true;
			paint(g, dlb_root);
			num_lines = lineno;
			int visa = 0;
			if (first_visible != null && last_visible != null) {
				visa = last_visible.getLineNo()-first_visible.getLineNo();
				if (verticalScrollBar.getSelection() != visa)
					verticalScrollBar.setSelection(visa);
			}
			if (verticalScrollBar.getMaximum() != num_lines) {
				verticalScrollBar.setMaximum(num_lines);
				verticalScrollBar.setSelection(visa);
			}
		}
		g.dispose();
	}	

	public boolean isDoubleBuffered() {
		return true;
	}

	private Rectangle calcBounds(DrawLayoutInfo n) {
		if (n instanceof GfxDrawTermLayoutInfo) {
			GfxDrawTermLayoutInfo dtli = (GfxDrawTermLayoutInfo)n;
			if (dtli.getLineNo() < first_line)
				return null;
			int w = dtli.getWidth();
			int h = dtli.getHeight();
			return new Rectangle(dtli.getX(), dtli.getY(), w, h);
		} else {
			Rectangle res = null;
			for (DrawLayoutInfo dlb: n.getBlocks()) {
				Rectangle r = calcBounds(dlb);
				if (res == null)
					res = r;
				else if (r != null)
					res = res.union(r);
			}
			return res;
		}
	}

	private void paint(Graphics2D g, DrawLayoutInfo n) {
		if (n == null)
			return;
		if (n instanceof GfxDrawTermLayoutInfo) {
			paintLeaf(g, (GfxDrawTermLayoutInfo)n);
		} else {
			for (DrawLayoutInfo dlb: n.getBlocks()) {
				paint(g, dlb);
			}
			if (false) {
				Rectangle r = calcBounds(n);
				if (r != null) {
					g.setColor(java.awt.Color.BLACK);
					g.drawRect(r.x, r.y, r.width, r.height);
				}
			}
		}
	}


	private void paintLeaf(Graphics2D g, GfxDrawTermLayoutInfo dtli) {
		DrawTerm leaf = dtli.getDrawable();
		if (leaf == null || leaf.isUnvisible())
			return;
		if (lineno < first_line) {
			if (dtli.isDoNewline())
				lineno++;
			return;
		}
		if (dtli.isDoNewline())
			lineno++;
		if (first_visible == null)
			first_visible = dtli;

		int x = dtli.getX();
		int y = dtli.getY();
		int w = dtli.getWidth();
		int h = dtli.getHeight();
		int b = dtli.getBaseline();

		if (!translated) {
			translated_y = y;
			g.translate(0, -y);
			translated = true;
		}
		if (y + h - translated_y >= getBounds().height)
			return;

		last_visible = dtli;

		boolean set_white = false;
		if (leaf == current && cursor_offset < 0) {
			g.setColor(java.awt.Color.BLACK);
			if (w > 0)
				g.fillRect(x, y, w, h);
			else
				g.fillRect(x-1, y, 2, h);
			set_white = true;
		}

		drawed_x = x + w;
		drawed_y = y;

		if (set_white)
			g.setColor(java.awt.Color.WHITE);
		else if (leaf.drnode instanceof ASTNode && ((ASTNode)leaf.drnode).isAutoGenerated())
			g.setColor(autoGenTextColor);
		else
			g.setColor(new java.awt.Color(leaf.syntax.lout.rgb_color));
		java.awt.Font font  = AWTGraphics2D.decodeFont(leaf.syntax.lout.font_name);
		g.setFont(font);
		Object term_obj = leaf.getTermObj();
		if (leaf == current && cursor_offset >= 0) {
			String s;
			if (term_obj == null || term_obj == DrawTerm.NULL_NODE || term_obj == DrawTerm.NULL_VALUE) {
				s = " ";
			} else {
				s = String.valueOf(term_obj);
			}
			if (s == null || s.length() == 0)
				s = " ";
			java.awt.font.TextLayout tl = new java.awt.font.TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
			g.translate(x, y+b);
			try {
				Shape[] carets = tl.getCaretShapes(cursor_offset);
				g.setColor(java.awt.Color.RED);
				g.draw(carets[0]);
				if (carets[1] != null) {
					g.setColor(java.awt.Color.BLACK);
					g.draw(carets[1]);
				}
			} catch (java.lang.IllegalArgumentException e) {} 
			g.translate(-x, -(y+b));
		}
		else {
			String s;
			if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
				s = "\u25d8"; // ◘
			else if (term_obj == DrawTerm.NULL_NODE)
				s = "\u25c6"; // ◆
			else
				s = String.valueOf(term_obj);
			if (s == null)
				s = "\u25d8"; // ◘
			if (s.length() == 0)
				return;
			java.awt.font.TextLayout tl = new java.awt.font.TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
		}
	}

	/**
	 * @return the verticalScrollBar
	 */
	public ScrollBar getVerticalScrollBar() {
		return verticalScrollBar;
	}

	/**
	 * @param dlb_root the dlb_root to set
	 */
	public void setDlb_root(DrawLayoutInfo dlb_root) {
		this.dlb_root = dlb_root;
	}

	/**
	 * @return the current
	 */
	public DrawTerm getCurrent() {
		return current;
	}

	/**
	 * @param current the current to set
	 */
	public void setCurrent(DrawTerm current, ANode current_node) {
		this.current = current;
		this.current_node = current_node;
	}

	/**
	 * @return the translated_y
	 */
	public int getTranslated_y() {
		return translated_y;
	}

	/**
	 * @return the last_visible
	 */
	public GfxDrawTermLayoutInfo getLast_visible() {
		return last_visible;
	}

	/**
	 * @return the first_visible
	 */
	public GfxDrawTermLayoutInfo getFirst_visible() {
		return first_visible;
	}

	/**
	 * @return the cursor_offset
	 */
	public int getCursor_offset() {
		return cursor_offset;
	}

	/**
	 * @param cursor_offset the cursor_offset to set
	 */
	public void setCursor_offset(int cursor_offset) {
		this.cursor_offset = cursor_offset;
	}

	/**
	 * @return the first_line
	 */
	public int getFirst_line() {
		return first_line;
	}

	/**
	 * @param first_line the first_line to set
	 */
	public void setFirst_line(int first_line) {
		this.first_line = first_line;
	}

	/**
	 * @return the num_lines
	 */
	public int getNum_lines() {
		return num_lines;
	}

	public void repaint() {
		// TODO Auto-generated method stub

	}

	public void requestFocus() {
		// TODO Auto-generated method stub

	}

	public void mouseDoubleClick(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseDown(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseUp(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseScrolled(MouseEvent e) {
		// TODO Auto-generated method stub

	}


	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub

	}

	public void widgetSelected(SelectionEvent e) {
		// TODO Auto-generated method stub

	}

	public IFmtGfx getFmtGraphics() {
		// TODO Auto-generated method stub
		return null;
	}

	public void controlMoved(ControlEvent e) {
		// TODO Auto-generated method stub

	}


}

