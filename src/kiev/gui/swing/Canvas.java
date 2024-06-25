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
package kiev.gui.swing;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextLayout;
import java.awt.geom.PathIterator;
import java.awt.image.VolatileImage;
import java.util.TimerTask;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import kiev.fmt.common.*;
import kiev.fmt.proj.KievExprNode;
import kiev.fmt.DrawTerm;
import kiev.fmt.DrawValueTerm;
import kiev.fmt.Drawable;
import kiev.fmt.SyntaxLineType;
import kiev.gui.ICanvas;
import kiev.gui.IEditor;
import kiev.gui.IMenu;
import kiev.gui.IPopupMenuListener;
import kiev.gui.IPopupMenuPeer;
import kiev.gui.IUIView;
import kiev.gui.NodeUtil;
import kiev.gui.UIView;
import kiev.vtree.AutoCompleteOption;
import kiev.vtree.AutoCompleteResult;
import kiev.vtree.INode;
import kiev.vtree.ASTNode;
import kiev.vtree.Symbol;

/**
 * Canvas.
 */
@SuppressWarnings("serial")
public class Canvas extends JPanel 
implements ICanvas, ComponentListener, KeyListener, MouseListener, MouseWheelListener, AdjustmentListener
{
	/**
	 * Default text color.
	 */
	private static final Color defaultTextColor = Color.BLACK;

	/**
	 * Auto-generated text color.
	 */
	private static final Color autoGenTextColor = Color.GRAY;

	/**
	 * Current node background
	 */
	private static final Color currentNodeBackground = Color.LIGHT_GRAY;

	/**
	 * Valid, unsaved background
	 */
	private static final Color unsavedBackground = Color.YELLOW;

	/**
	 * Error background
	 */
	private static final Color errorBackground = new Color(0x404000);

	/**
	 * Selected node color.
	 */
	@SuppressWarnings("unused")
	private static final Color selectedNodeColor = new Color(224,224,224);

	/**
	 * Default text font.
	 */
	@SuppressWarnings("unused")
	private static final Font defaultTextFont = new Font("Dialog", Font.PLAIN, 12);
	
	/*
	 * Timer & TimerTask to blink cursor 
	 */
	class CursorTimerTask extends TimerTask {
		@Override
		public void run() {
			Rectangle r = current_leaf_rect;
			if (r == null
				|| ui_view == null
				|| ui_view.getWindow() == null
				|| ui_view.getWindow().getCurrentView() == null) {
				this.cancel();
				return;
			}
			if (ui_view.getWindow().getCurrentView().getViewPeer() == Canvas.this) {
				cursorOff = !cursorOff;
				Canvas.this.repaint(r);
			}
		}
	};
	
	private TimerTask cursorBlinkTask;
	private boolean cursorOff;

	/**
	 * The formatter graphics.
	 */
	private AWTGraphics2D gfx;
	
	/**
	 * The view.
	 */
	private UIView ui_view;
	
	/**
	 * The popup menu for this canvas
	 */
	private PopupMenu popupMenu;

	/**
	 * The combo-box.
	 */
	AutoCompleteMenu combo;
	boolean in_combo;
	
	/**
	 * The vertical scroll bar.
	 */
	private JScrollBar verticalScrollBar;

	/**
	 * Image width. 
	 */
	private int imgWidth;

	/**
	 * Image height.
	 */
	private int imgHeight;

	/**
	 * Draw layout info.
	 */
	private DrawLayoutInfo dlb_root;

	/**
	 * Current drawable.
	 */
	private DrawTerm current;
	private Rectangle current_leaf_rect; 

	/**
	 * Current AST node.
	 */
	private INode current_node;

	/**
	 * Vertical offset (scroll down) in pixels.
	 */
	private int vert_offset;

	/**
	 * Horizontal offset (scroll right) in pixels.
	 */
	private int horiz_offset;

	/**
	 * Cursor offset.
	 */
	private int cursor_offset = -1;
	
	/**
	 * Visible lines of text graphic coordinates.
	 */
	private DrawLayoutInfo	first_visible, last_visible, prev_visible;
	
	/**
	 * Current background
	 */
	private Color background;

	/**
	 * The constructor.
	 */
	public Canvas() {
		super(null,false);
		setFocusable(true);
		verticalScrollBar = new JScrollBar(Adjustable.VERTICAL);
		verticalScrollBar.addAdjustmentListener(this);
		add(this.verticalScrollBar);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		addComponentListener(this);
		imgWidth = 100;
		imgHeight = 100;
		popupMenu = new PopupMenu(this);
		combo = new AutoCompleteMenu(this);
		//this.add(combo);
	}
	
	public IPopupMenuPeer getPopupMenu(IPopupMenuListener listener, IMenu menu) {
		popupMenu.init(listener, menu);
		return popupMenu;
	}
	
	public void setPopupComboContent(AutoCompleteResult autocomplete_result, boolean qualified) {
		combo.setVisible(false);
		if (autocomplete_result == null || autocomplete_result.getOptions().length == 0) {
			return;
		}
		//combo.setEditor(this);
		//combo.configureEditor(this, null);
		combo.removeAllItems();
		DrawLayoutInfo info = ui_view.getDrawTerm().getGfxFmtInfo();
		int x = info.getX();
		int y = info.getY() - getVertOffset();
		//int w = info.getWidth();
		int h = info.height;
		boolean popup = false;
		for (AutoCompleteOption opt: autocomplete_result.getOptions()) {
			if (qualified && opt.data instanceof Symbol)
				combo.addItem(((Symbol)opt.data).qname());
			else
				combo.addItem(opt);
			popup = true;
		}
		if (popup) {
			if (! in_combo)
				combo.selectNone();
			combo.setVisible(true);
			combo.pack();
			combo.display(x, y+h);
			//setBounds(x, y, w+100, h+1);
		} else {
			in_combo = false;
		}
	}

	/**
	 * Scroll By Block.
	 * @param scrollbar the scroll bar
	 * @param direction the direction
	 */
	private static final void scrollByBlock(JScrollBar scrollbar, int direction) {
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

	/**
	 * Scroll By Units.
	 * @param scrollbar the scroll bar
	 * @param direction the direction
	 * @param units the number of units
	 */
	private static final void scrollByUnits(JScrollBar scrollbar, int direction,
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

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setUIView(kiev.gui.IUIView)
	 */
	public void setUIView(IUIView uiv) {
		if (uiv instanceof UIView){
			ui_view = (UIView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getFmtGraphics()
	 */
	public IFmtGfx getFmtGraphics() {
		if (gfx == null) gfx = new AWTGraphics2D((Graphics2D)this.getGraphics());
		return gfx;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getImgWidth()
	 */
	public int getImgWidth() { return imgWidth; }

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getImgHeight()
	 */
	public int getImgHeight() { return imgHeight; }

	/* (non-Javadoc)
	 * @see java.awt.Component#setBounds(int, int, int, int)
	 */
	@Override
	public void setBounds(int x, int y, int width, int height) {
		int pw = verticalScrollBar.getPreferredSize().width;
		int oldWidth = imgWidth;
		imgWidth = width - pw;
		imgHeight = height;
		verticalScrollBar.setBounds(imgWidth,0,pw,height);
		super.setBounds(x, y, width, height);
		if (oldWidth != imgWidth && this.ui_view != null) {
			ui_view.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
				public void run() {
					ui_view.formatAndPaint(true);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setVertOffset(int)
	 */
	public void setVertOffset(int val) {
		if (val < 0)
			val = 0;
		verticalScrollBar.setValue(val);
		this.vert_offset = val;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getVertOffset()
	 */
	public int getVertOffset() {
		return vert_offset;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setHorizOffset(int)
	 */
	public void setHorizOffset(int val) {
		if (val < 0)
			val = 0;
		//horizontalScrollBar.setValue(val);
		this.horiz_offset = val;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getHorizOffset()
	 */
	public int getHorizOffset() {
		return horiz_offset;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#incrFirstLine(int)
	 */
	public void incrVertOffset(int val) {
		verticalScrollBar.setValue(vert_offset+val);
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getDrawableAt(int, int)
	 */
	public Drawable getDrawableAt(int x, int y) {
		y += vert_offset;
		x += horiz_offset;
		DrawLayoutInfo dr = first_visible;
		DrawLayoutInfo last = last_visible;
		for (; dr != null; dr = dr.getNextLeaf()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.getX() < x && dr.getY() < y && dr.getX()+w >= x && dr.getY()+h >= y)
				return dr.getDrawable();
			if (dr == last)
				return null;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent evt) {
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent evt) {
		if (ui_view instanceof IEditor && ((IEditor)ui_view).isInTextEditMode()) {
			if ((evt.getModifiersEx() & ~InputEvent.SHIFT_DOWN_MASK) != 0) 	return;
			final int ch = evt.getKeyChar();
			if (ch == KeyEvent.CHAR_UNDEFINED) return;
			evt.consume();
			final IEditor edt = (IEditor)ui_view;
			ui_view.getWindow().getEditorThreadGroup().runTaskLater(new Runnable() {
				public void run() {
					edt.editTypeChar((char)ch);
				}
			});
			return;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(final KeyEvent evt) {
		ui_view.getWindow().getEditorThreadGroup().runTask(new Runnable() {
			public void run() {
				if (ui_view instanceof IEditor && ((IEditor)ui_view).isInTextEditMode()) {
					keyPressedForEditor(evt);
					if (evt.isConsumed())
						return;
				}
				if (ui_view.inputEvent(new InputEventInfo(evt)))
					evt.consume();
			}
		});
		if (evt.isConsumed())
			return;
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK);
		if (mask == 0) {
			if (!(code==KeyEvent.VK_SHIFT || code==KeyEvent.VK_ALT || code==KeyEvent.VK_ALT_GRAPH || code==KeyEvent.VK_CONTROL || code==KeyEvent.VK_CAPS_LOCK))
				java.awt.Toolkit.getDefaultToolkit().beep();
			return;
		}
	}
	
	private void keyPressedForEditor(KeyEvent evt) {
		IEditor editor = (IEditor)ui_view;
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK|InputEvent.ALT_DOWN_MASK);
		if (mask == InputEvent.CTRL_DOWN_MASK) {
			if (code == KeyEvent.VK_SPACE) {
				evt.consume();
				//showAutoComplete(true);
				return;
			}
			if (code == KeyEvent.VK_PERIOD) {
				evt.consume();
				editor.editTypeChar('·');
				return;
			}
		}
		if (mask != 0) return;
		switch (code) {
		default:
			return;
		case KeyEvent.VK_DOWN:
			if (in_combo) {
				evt.consume();
				int count = combo.getItemCount();
				if (count == 0) {
					in_combo = false;
					combo.setVisible(false);
					break;
				}
				combo.moveDown();
				break;
			}
			else if (combo.isVisible() && combo.getItemCount() > 0) {
				evt.consume();
				in_combo = true;
				combo.moveStart();
			}
			else
				return;
			break;
		case KeyEvent.VK_UP:
			if (in_combo) {
				evt.consume();
				int count = combo.getItemCount();
				if (count == 0) {
					in_combo = false;
					combo.setVisible(false);
					break;
				}
				combo.moveUp();
				break;
			}
			else if (combo.isVisible() && combo.getItemCount() > 0) {
				evt.consume();
				in_combo = true;
				combo.moveEnd();
			}
			break;
		case KeyEvent.VK_ENTER:
			evt.consume();
			if (in_combo) {
				this.setItem(combo.getSelectedItem());
				in_combo = false;
				//text = getText();
				//setEditOffset(text.length());
			}
			combo.setVisible(false);
			editor.stopTextEditMode();
			break;
		case KeyEvent.VK_ESCAPE:
			evt.consume();
			if (in_combo) {
				in_combo = false;
				combo.selectNone();
			}
			combo.setVisible(false);
			editor.stopTextEditMode();
			break;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent evt) {
		boolean consume = ui_view.inputEvent(new InputEventInfo(evt));
		if (consume) {
			evt.consume();
			return;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
	 */
	public void componentHidden(ComponentEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
	 */
	public void componentMoved(ComponentEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
	 */
	public void componentShown(ComponentEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
	 */
	public void componentResized(ComponentEvent e) {
		if (gfx != null)
			verticalScrollBar.setBlockIncrement(imgHeight-gfx.textHeight());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.AdjustmentListener#adjustmentValueChanged(java.awt.event.AdjustmentEvent)
	 */
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getAdjustable() == verticalScrollBar) {
			vert_offset = e.getValue();
			DrawLayoutInfo dlb_root = this.dlb_root;
			if (dlb_root != null) {
				Rectangle r = dlb_root.getBounds();
				if (vert_offset >= r.height)
					vert_offset = r.height;
			}
			if (vert_offset < 0)
				vert_offset = 0;
			this.repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollAmount() != 0) {
			JScrollBar toScroll = this.verticalScrollBar;
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

	/* (non-Javadoc)
	 * @see java.awt.Component#createVolatileImage(int, int)
	 */
	@Override
	public VolatileImage createVolatileImage(int w, int h) {
		System.out.println("create volatile image "+w+" : "+h);
		return super.createVolatileImage(w, h);
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#update(java.awt.Graphics)
	 */
	@Override
	public void update(Graphics gScreen) { paint(gScreen); }

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics gScreen) {
		if (current_leaf_rect != null) {
			Rectangle r = gScreen.getClipBounds();
			if (r != null && r.equals(current_leaf_rect)) {
				Graphics2D g = (Graphics2D)gScreen.create();
				g.setColor(Color.WHITE);
				g.fillRect(current_leaf_rect.x, current_leaf_rect.y, current_leaf_rect.width, current_leaf_rect.height);
				paintCurrentLeaf(g, false);
				return;
			}
		}
		render((Graphics2D)gScreen);
	}

	/**
	 * Render.
	 * @param g the graphics
	 */
	private void render(Graphics2D g) {
		g = (Graphics2D)g.create();
		g.setClip(0, 0, imgWidth, imgHeight);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, imgWidth, imgHeight);
		//g.clearRect(0, 0, getWidth(), getHeight());
		DrawLayoutInfo dlb_root = this.dlb_root;
		if (dlb_root != null) {
			first_visible = null;
			last_visible = null;
			current_leaf_rect = null;
			background = null;
			//is_editable = true;
			g.translate(-horiz_offset, -vert_offset);
			paint(g, dlb_root);
			g.translate(horiz_offset, vert_offset);
			paintCurrentLeaf(g, true);
			int total_height = 0;
			Rectangle rect = dlb_root.getBounds();
			if (rect != null) {
				total_height = rect.height;
				int visa = imgHeight;
				if (total_height < visa)
					total_height = visa;
				if (verticalScrollBar.getVisibleAmount() != visa)
					verticalScrollBar.setVisibleAmount(visa);
				if (verticalScrollBar.getMaximum() != total_height+gfx.textHeight()) {
					verticalScrollBar.setMaximum(total_height+gfx.textHeight());
					verticalScrollBar.setVisibleAmount(visa);
					verticalScrollBar.setBlockIncrement(imgHeight-gfx.textHeight());
					verticalScrollBar.setUnitIncrement(gfx.textHeight());
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#isDoubleBuffered()
	 */
	@Override
	public boolean isDoubleBuffered() {
		return true;
	}
	
	private void drawLine(Graphics2D g, SyntaxLineType lt, int x1, int y1, int x2, int y2, int xi, int yi) {
		if (lt == null || lt == SyntaxLineType.NONE)
			return;
		if (lt == SyntaxLineType.SINGLE || lt == SyntaxLineType.DOUBLE)
			g.drawLine(x1, y1, x2, y2);
		if (lt == SyntaxLineType.DOUBLE)
			g.drawLine(x1+xi, y1+yi, x2+xi, y2+yi);
	}

	/**
	 * Paint.
	 * @param g the graphics
	 * @param n the draw layout info
	 */
	private void paint(Graphics2D g, DrawLayoutInfo n) {
		if (n == null)
			return;
		Color new_bg = null;
		Color old_bg = background;
		INode node = n.getDrawable() == null ? null : n.getDrawable().drnode;
		if (node != null) {
			if (node instanceof KievExprNode) {
				KievExprNode en = (KievExprNode)node;
				if (en.isInvalid())
					new_bg = errorBackground;
				else if (en.isUnsaved())
					new_bg = unsavedBackground;
			}
			if (node == current_node && background == null)
				new_bg = currentNodeBackground;
		}

		if (new_bg != null)
			background = new_bg;
		if (n.getDrawable() instanceof DrawTerm) {
			paintLeaf(g, n);
		} else {
			for (DrawLayoutInfo dlb: n.getBlocks()) {
				paint(g, dlb);
			}
			Draw_Paragraph p = n.getParagraph();
			if (p != null) {
				Draw_ParLines l = p.getLines();
				if (l != null) {
					Rectangle r = n.getBounds();
					if (r != null /* && new Rectangle(horiz_offset, vert_offset, imgWidth, imgHeight).contains(r) */) {
						g.setColor(Color.BLACK);
						drawLine(g, l.top, r.x, r.y, r.x+r.width, r.y, 0, 2);
						drawLine(g, l.bottom, r.x, r.y+r.height, r.x+r.width, r.y+r.height, 0, -2);
						drawLine(g, l.left, r.x, r.y, r.x, r.y+r.height, 2, 0);
						drawLine(g, l.right, r.x+r.width, r.y, r.x+r.width, r.y+r.height, -2, 0);
					}
				}
			}
		}
		if (new_bg != null)
			background = old_bg;
	}

	/**
	 * Paint Leaf.
	 * @param g Graphics2D
	 * @param dtli DrawTermLayoutInfo
	 */
	private void paintLeaf(Graphics2D g, DrawLayoutInfo dtli) {
		DrawTerm leaf = (DrawTerm)dtli.getDrawable();
		if (leaf == null || leaf.isUnvisible())
			return;

		int x = dtli.getX();
		int y = dtli.getY();
		int w = dtli.width;
		int h = dtli.height;
		int b = dtli.baseline;
		if (y + h - vert_offset < 0)
			return;
		if (first_visible == null)
			first_visible = dtli;
		if (y + h - vert_offset >= imgHeight)
			return;

		last_visible = dtli;

		//if (cursor_offset < 0) {
			if (background != null) {
				g.setColor(background);
				if (prev_visible != null && prev_visible.getY() == y)
					g.fillRect(prev_visible.getX()+prev_visible.width, prev_visible.getY(), x-(prev_visible.getX()+prev_visible.width), h);
				if (w > 0)
					g.fillRect(x, y, w+1, h);
				prev_visible = dtli;
			} else {
				prev_visible = null;
			}
			/*
			if (NodeUtil.isA(leaf.drnode, current_node)) {
				g.setColor(Color.LIGHT_GRAY);
				if (prev_visible != null && prev_visible.getY() == y)
					g.fillRect(prev_visible.getX()+prev_visible.getWidth(), prev_visible.getY(), x-(prev_visible.getX()+prev_visible.getWidth()), h);
				if (w > 0)
					g.fillRect(x, y, w+1, h);
				prev_visible = dtli;
			} else {
				prev_visible = null;
			}
			*/
		//}

		Draw_Style style = dtli.style;
		if (leaf.drnode instanceof ASTNode && ((ASTNode)leaf.drnode).isAutoGenerated())
			g.setColor(autoGenTextColor);
		else if (style != null)
			g.setColor(AWTGraphics2D.decodeColor(style.color));
		else
			g.setColor(defaultTextColor);
		Font font;
		if (style != null)
			font = AWTGraphics2D.decodeFont(style.font);
		else
			font = AWTGraphics2D.decodeFont(null);
		g.setFont(font);
		Object term_obj = leaf.getTermObj();
		if (term_obj instanceof Draw_Icon) {
			Draw_Icon di = (Draw_Icon)term_obj;
			Image img = AWTGraphics2D.decodeImage(di);
			g.drawImage(img, x, y, null);
		} else {
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
			TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
		}
	}

	private boolean drawTextCursor() {
		if (ui_view instanceof IEditor && ((IEditor)ui_view).isInTextEditMode())
			return true;
		if (!ui_view.isInInsertMode())
			return false;
		return current instanceof DrawValueTerm;
	}
	private boolean drawBoxCursor() {
		return ui_view instanceof IEditor && current instanceof DrawValueTerm;
	}
	
	/**
	 * Paint Leaf.
	 * @param g Graphics2D
	 * @param dtli DrawTermLayoutInfo
	 */
	private void paintCurrentLeaf(Graphics2D g, boolean scheduleCursorTask) {
		if (current == null || current.isUnvisible())
			return;
		DrawLayoutInfo dtli = current.dr_dli;

		int x = dtli.getX() - horiz_offset;
		int y = dtli.getY() - vert_offset;
		int w = dtli.width;
		int h = dtli.height;
		int b = dtli.baseline;

		if (scheduleCursorTask && cursorBlinkTask != null) {
			cursorBlinkTask.cancel();
			cursorBlinkTask = null;
			current_leaf_rect = null;
			Window.guiTimer.purge();
		}
		Draw_Style style = dtli.style;
		Font font;
		if (style != null)
			font = AWTGraphics2D.decodeFont(style.font);
		else
			font = AWTGraphics2D.decodeFont(null);
		g.setFont(font);

		boolean set_white = false;
		if (!drawTextCursor()) {
			if (drawBoxCursor()) {
				if (scheduleCursorTask) {
					current_leaf_rect = new Rectangle(x, y, w+1, h+1);
					cursorBlinkTask = new CursorTimerTask();
					cursorOff = false;
					Window.guiTimer.schedule(cursorBlinkTask, 500L, 500L);
				}
				if (w <= 0)
					w = 1;
				if (NodeUtil.isA(current.drnode, current_node)) {
					g.setColor(Color.LIGHT_GRAY);
					g.fillRect(x, y, w+1, h);
				}
				if (!cursorOff) {
					g.setColor(Color.BLACK);
					g.drawRect(x, y, w, h);
				}
			} else {
				g.setColor(Color.BLACK);
				set_white = true;
				if (w > 0)
					g.fillRect(x, y, w, h);
				else
					g.fillRect(x-1, y, 2, h);
			}
		}

		if (set_white)
			g.setColor(Color.WHITE);
		else if (current.drnode instanceof ASTNode && ((ASTNode)current.drnode).isAutoGenerated())
			g.setColor(autoGenTextColor);
		else if (style != null)
			g.setColor(AWTGraphics2D.decodeColor(style.color));
		else
			g.setColor(defaultTextColor);
		Object term_obj = current.getTermObj();
		if (term_obj instanceof Draw_Icon) {
			Draw_Icon di = (Draw_Icon)term_obj;
			Image img = AWTGraphics2D.decodeImage(di);
			g.drawImage(img, x, y, null);
		} else {
			String s;
			if (term_obj == null || term_obj == DrawTerm.NULL_VALUE)
				s = "\u25d8"; // ◘
			else if (term_obj == DrawTerm.NULL_NODE)
				s = "\u25c6"; // ◆
			else
				s = String.valueOf(term_obj);
			if (s == null)
				s = "\u25d8"; // ◘
			TextLayout tl;
			if (s.length() == 0) {
				tl = new TextLayout(" ", font, g.getFontRenderContext());
			} else {
				tl = new TextLayout(s, font, g.getFontRenderContext());
				tl.draw(g, x, y+b);
			}
			if (drawTextCursor()) {
				if (cursor_offset >= 0 && scheduleCursorTask) {
					current_leaf_rect = new Rectangle(x, y, w, h);
					cursorBlinkTask = new CursorTimerTask();
					cursorOff = false;
					Window.guiTimer.schedule(cursorBlinkTask, 500L, 500L);
				}
				if (cursor_offset >= 0 && !cursorOff) {
					g.translate(x, y+b);
					try {
						Shape[] carets = tl.getCaretShapes(cursor_offset);
						g.setColor(Color.BLACK);
						drawCaret(g, carets[0], s.length());
						if (carets[1] != null) {
							g.setColor(Color.RED);
							drawCaret(g, carets[1], s.length());
						}
					}
					catch (java.lang.IllegalArgumentException e) {}
					finally { g.translate(-x, -(y+b)); }
				}
			}
		}
	}
	
	private void drawCaret(Graphics2D g, Shape caret, int text_length) {
		//g.draw(caret);
		int x0=0, y0=0, x1=0, y1=0;
		for (PathIterator pi = caret.getPathIterator(null); !pi.isDone(); pi.next()) {
			float[] coords = new float[6];
			int type = pi.currentSegment(coords);
			if (type == PathIterator.SEG_MOVETO) {
				x0 = (int)coords[0];
				y0 = (int)coords[1];
			}
			else if (type == PathIterator.SEG_LINETO) {
				x1 = (int)coords[0];
				y1 = (int)coords[1]-1;
				break;
			}
		}
		g.drawLine(x0, y0, x1, y1);
		if (cursor_offset > 0) {
			g.drawLine(x0-2, y0, x0, y0);
			g.drawLine(x1-2, y1, x1, y1);
		}
		if (cursor_offset < text_length) {
			g.drawLine(x0, y0, x0+2, y0);
			g.drawLine(x1, y1, x1+2, y1);
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setDlb_root(kiev.fmt.DrawLayoutInfo)
	 */
	public void setDlb_root(DrawLayoutInfo dlb_root) {
		this.dlb_root = dlb_root;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getCurrent()
	 */
	public DrawTerm getCurrent() {
		return current;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setCurrent(kiev.fmt.DrawTerm, kiev.vtree.INode)
	 */
	public void setCurrent(DrawTerm current, INode current_node) {
		if (current != this.current) {
			if (in_combo || combo.isVisible()) {
				combo.setVisible(false);
				in_combo = false;
			}
		}
		this.current = current;
		this.current_node = current_node;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getLast_visible()
	 */
	public DrawLayoutInfo getLast_visible() {
		return last_visible;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getFirst_visible()
	 */
	public DrawLayoutInfo getFirst_visible() {
		return first_visible;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getCursor_offset()
	 */
	public int getCursor_offset() {
		return cursor_offset;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setCursor_offset(int)
	 */
	public void setCursor_offset(int cursor_offset) {
		this.cursor_offset = cursor_offset;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getUIView()
	 */
	public IUIView getUIView() {
		return ui_view;
	}

	public void setItem(Object item) {
		if (item == null)
			return;
		if (ui_view instanceof IEditor && ((IEditor)ui_view).isInTextEditMode()) {
			IEditor editor = (IEditor)ui_view;
			editor.editSetItem(item);
		}
	}
}

