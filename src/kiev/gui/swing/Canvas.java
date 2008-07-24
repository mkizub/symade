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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextLayout;
import java.awt.image.VolatileImage;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.GfxDrawTermLayoutInfo;
import kiev.fmt.IFmtGfx;
import kiev.gui.ICanvas;
import kiev.gui.IUIView;
import kiev.gui.UIView;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;

public class Canvas extends JPanel implements ICanvas, ComponentListener,
		KeyListener, MouseListener, MouseWheelListener, AdjustmentListener
{
	private static final long serialVersionUID = 4713633504436057499L;
	static final Color defaultTextColor = Color.BLACK;
	static final Color autoGenTextColor = Color.GRAY;
	static final Color selectedNodeColor = new Color(224,224,224);
	static final Font defaultTextFont = new Font("Dialog", Font.PLAIN, 12);

	private UIView            ui_view;
	
	private JScrollBar        verticalScrollBar;
	private int               imgWidth;
	private int               imgHeight;
	
	private DrawLayoutInfo    dlb_root;
	private DrawTerm          current;
	private ANode             current_node;
	private int               first_line;
	private int               num_lines;
	private int               cursor_offset = -1;

	transient VolatileImage   vImg;
	
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
	
	public Canvas() {
		super(null,false);
		this.setFocusable(true);
		this.verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
		this.verticalScrollBar.addAdjustmentListener(this);
		this.add(this.verticalScrollBar);
		this.addMouseListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
		this.imgWidth = 100;
		this.imgHeight = 100;
	}
	
	public void setUIView(IUIView uiv) {
		if (uiv instanceof UIView){
			this.ui_view = (UIView)uiv;
		} else {
			throw new RuntimeException("Wrong instance of UIView"); 
		}
	}
	
	public IFmtGfx getFmtGraphics() {
		return new AWTGraphics2D((Graphics2D)this.getGraphics());
	}
	
	public int getImgWidth() { return imgWidth; }
	public int getImgHeight() { return imgHeight; }

	public void setBounds(int x, int y, int width, int height) {
		int pw = verticalScrollBar.getPreferredSize().width;
		int oldWidth = imgWidth;
		imgWidth = width - pw;
		imgHeight = height;
		verticalScrollBar.setBounds(imgWidth,0,pw,height);
		super.setBounds(x, y, width, height);
		if (oldWidth != imgWidth && this.ui_view != null)
			this.ui_view.formatAndPaint(true);
	}
	
	public void setFirstLine(int val) {
		verticalScrollBar.setValue(val);
	}
	
	public void incrFirstLine(int val) {
		verticalScrollBar.setValue(first_line+val);
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
			evt.consume();
			return;
		}
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == 0) {
			if (!(code==KeyEvent.VK_SHIFT || code==KeyEvent.VK_ALT || code==KeyEvent.VK_ALT_GRAPH || code==KeyEvent.VK_CONTROL || code==KeyEvent.VK_CAPS_LOCK))
				Configuration.doGUIBeep();
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
			evt.consume();
			return;
		}
	}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		this.ui_view.formatAndPaint(true);
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getAdjustable() == verticalScrollBar) {
			first_line = e.getValue();
			if (first_line >= num_lines)
				first_line = num_lines-1;
			if (first_line < 0)
				first_line = 0;
			this.repaint();
		}
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollAmount() != 0) {
			JScrollBar toScroll = this.getVerticalScrollBar();
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
	private static void scrollByBlock(JScrollBar scrollbar, int direction) {
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
	private static void scrollByUnits(JScrollBar scrollbar, int direction,
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

	public VolatileImage createVolatileImage(int w, int h) {
		System.out.println("create volatile image "+w+" : "+h);
		return super.createVolatileImage(w, h);
	}
	
	public void update(Graphics gScreen) { paint(gScreen); }

	public void paintComponent(Graphics gScreen) {
		// copying from the image (here, gScreen is the Graphics
		// object for the onscreen window)
		do {
			if (vImg == null || vImg.getWidth() != imgWidth || vImg.getHeight() != imgHeight)
				vImg = createVolatileImage(imgWidth, imgHeight);
			int returnCode = vImg.validate(getGraphicsConfiguration());
			if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
				// old vImg doesn't work with new GraphicsConfig; re-create it
				vImg = createVolatileImage(imgWidth, imgHeight);
			}
			renderOffscreen();
			gScreen.drawImage(vImg, 0, 0, this);
		} while (vImg.contentsLost());		
	}
	
	void renderOffscreen() {
		do {
		    if (vImg.validate(getGraphicsConfiguration()) ==
		    	VolatileImage.IMAGE_INCOMPATIBLE)
		    {
		    	vImg = createVolatileImage(getWidth(), getHeight());
		    }
		    Graphics2D g = vImg.createGraphics();
			g.setClip(0, 0, getWidth(), getHeight());
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
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
					if (verticalScrollBar.getVisibleAmount() != visa)
						verticalScrollBar.setVisibleAmount(visa);
				}
				if (verticalScrollBar.getMaximum() != num_lines) {
					verticalScrollBar.setMaximum(num_lines);
					verticalScrollBar.setVisibleAmount(visa);
				}
			}
		    g.dispose();
		} while (vImg.contentsLost());
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
					g.setColor(Color.BLACK);
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
			if (dtli.get$do_newline())
				lineno++;
			return;
		}
		if (dtli.get$do_newline())
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
		if (y + h - translated_y >= getHeight())
			return;
		
		last_visible = dtli;
		
		boolean set_white = false;
		if (leaf == current && cursor_offset < 0) {
			g.setColor(Color.BLACK);
			if (w > 0)
				g.fillRect(x, y, w, h);
			else
				g.fillRect(x-1, y, 2, h);
			set_white = true;
		}

		drawed_x = x + w;
		drawed_y = y;
		
		if (set_white)
			g.setColor(Color.WHITE);
		else if (leaf.get$drnode() instanceof ASTNode && ((ASTNode)leaf.get$drnode()).isAutoGenerated())
			g.setColor(autoGenTextColor);
		else
			g.setColor(new Color(leaf.syntax.lout.rgb_color));
		Font font  = AWTGraphics2D.decodeFont(leaf.syntax.lout.font_name);
		g.setFont(font);
		if (leaf == current && cursor_offset >= 0) {
			String s = leaf.getText();
			if (s == null || s.length() == 0) s = " ";
			TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
			g.translate(x, y+b);
			try {
				Shape[] carets = tl.getCaretShapes(cursor_offset);
				g.setColor(Color.RED);
				g.draw(carets[0]);
				if (carets[1] != null) {
					g.setColor(Color.BLACK);
					g.draw(carets[1]);
				}
			} catch (java.lang.IllegalArgumentException e) {} 
			g.translate(-x, -(y+b));
		}
		else {
			String s = leaf.getText();
			if (s == null) s = "\u25d8"; // ◘
			if (s.length() == 0)
				return;
			TextLayout tl = new TextLayout(s, font, g.getFontRenderContext());
			tl.draw(g, x, y+b);
		}
	}
	
	/**
	 * @return the verticalScrollBar
	 */
	public JScrollBar getVerticalScrollBar() {
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

}

