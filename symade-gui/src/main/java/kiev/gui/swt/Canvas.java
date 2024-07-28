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
package kiev.gui.swt;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.common.DrawLayoutInfo;
import kiev.fmt.common.Draw_Icon;
import kiev.fmt.common.Draw_Style;
import kiev.fmt.common.IFmtGfx;
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * The canvas widget is the place where we draw.
 */
public class Canvas extends org.eclipse.swt.widgets.Canvas 
implements ICanvas, KeyListener, MouseListener, SelectionListener, ControlListener
{
	/**
	 * Default text color.
	 */
	private static  Color defaultTextColor;
	
	/**
	 * Auto-generated text color.
	 */
	private static Color autoGenTextColor;
	
	/**
	 * Selected node color.
	 */
	private static Color selectedNodeColor;
	
	/**
	 * Default text font.
	 */
	private static Font defaultTextFont;

	/**
	 * The view.
	 */
	private UIView ui_view;

	/**
	 * The vertical scroll bar.
	 */
	private ScrollBar verticalScrollBar;

	/**
	 *  Draw layout info.
	 */
	private DrawLayoutInfo dlb_root;
	
	/**
	 * Current drawable.
	 */
	private DrawTerm current;
	
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
	 * The combo-box.
	 */
	PopupList combo;
	boolean in_combo;
	
	/**
	 *  Cursor offset.
	 */
	private int cursor_offset = -1;

	/**
	 * Visible lines of text graphic coordinates.
	 */
	private DrawLayoutInfo	first_visible;
	private DrawLayoutInfo	last_visible;
	private DrawLayoutInfo	prev_visible;

	/**
	 * Black color.
	 */
	private Color swtColorBlack; 
	
	/**
	 * White color.
	 */
	private Color swtColorWhite; 
	
	/**
	 * Grey color. 
	 */
	@SuppressWarnings("unused")
	private Color swtColorGray;
	
	/**
	 * Grey color. 
	 */
	private Color swtColorYellow;
	
	/**
	 * Default font.
	 */
	@SuppressWarnings("unused")
	private Font swtDefaultFont; 
	
	/**
	 * The graphics context.
	 */
	private GC gc;
	
	/**
	 * The formatter graphics.
	 */
	private SWTGraphics2D gfx;
	
	/**
	 * The transform engine.
	 */
	private Transform tr;
	
	/**
	 * The layout of text.
	 */
	private TextLayout tl;
	
	/**
	 * The caret.
	 */
	private Caret caret;
		
	/**
	 * Off-screen image.
	 */
	@SuppressWarnings("unused")
	private Image vImg;

	/**
	 * Scroll bar increment supposed moving down.
	 */
	private int downIncrement;

	/**
	 * Scroll bar increment supposed moving up.
	 */
	private int upIncrement;

	private DrawLayoutInfo penultimate_visible;

	/**
	 * Converts int to RGB.
	 */
	private final class ColorDecoder {
		
		/**
		 * The value contains a color representation.
		 */
		private final int value;
		
		/**
		 * RGB.
		 */
		private final RGB rgb;
		
		/**
		 * The constructor. 
		 * @param value the color
		 */
		ColorDecoder(int value) {
			this.value = 0xff000000 | value;
			rgb = new RGB(getRed(),getGreen(),getBlue());
		}
		
		/**
		 * Returns the blue component of a color.
		 * @return int
		 */
		int getBlue() {
			return (value >> 0) & 0xFF;
		}
		
		/**
		 * Returns the green component of a color.
		 * @return int
		 */
		int getGreen() {
			return (value >> 8) & 0xFF;
		}
		
		/**
		 * Returns the red component of a color.
		 * @return int
		 */
		int getRed() {
			return (value >> 16) & 0xFF;
		}
		
		/**
		 * Returns the RGB object.
		 * @return <code>RGB</code>
		 */
		RGB getRGB(){
			return rgb;
		}
	}
		

	/**
	 * The constructor. 
	 * @param parent the parent <code>Composite</code>
	 * @param style the SWT style
	 */
	public Canvas(Composite parent, int style) {
		super(parent, style);
		defaultTextColor = getDisplay().getSystemColor(SWT.COLOR_BLACK);
		autoGenTextColor = getDisplay().getSystemColor(SWT.COLOR_GRAY);
		selectedNodeColor = new Color(getDisplay(), 224,224,224);
		defaultTextFont = new Font(getDisplay(), "Dialog", 12, SWT.NORMAL);
		verticalScrollBar = getVerticalBar();
		verticalScrollBar.addSelectionListener(this);
		verticalScrollBar.setMinimum(0);				
		addMouseListener(this);
		addKeyListener(this);
		addControlListener(this);
		swtColorWhite = getDisplay().getSystemColor(SWT.COLOR_WHITE); 
		swtColorBlack = getDisplay().getSystemColor(SWT.COLOR_BLACK);	
		swtColorGray = getDisplay().getSystemColor(SWT.COLOR_GRAY);
		swtColorYellow = getDisplay().getSystemColor(SWT.COLOR_YELLOW);
		swtDefaultFont = getDisplay().getSystemFont();
		gc = new GC(this);
		tl = new TextLayout(gc.getDevice());
			addPaintListener(new PaintListener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
			 */
			public void paintControl(PaintEvent e) {
				if (e.gc == null) return;
//				if (vImg == null || vImg.getBounds().width != getClientArea().width || vImg.getBounds().height != getClientArea().height){
//					vImg = new Image(e.gc.getDevice(), getClientArea().width, getClientArea().height);				  
//				}
//				GC gc = new GC(vImg);
				if (tr != null){
					tr.dispose();
				}
				tr  = new Transform(e.gc.getDevice());					
				paint(e.gc);
//				e.gc.drawImage(vImg, 0, 0);
			}
		});
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getUIView()
	 */
	public IUIView getUIView() {
		return ui_view;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setUIView(kiev.gui.IUIView)
	 */
	public void setUIView(IUIView uiv) {
		if (uiv instanceof UIView){
			ui_view = (UIView)uiv;
		} else {
			throw new RuntimeException(Window.resources.getString("Canvas_Exception_wrong_instance")); 
		}
	}

	public IPopupMenuPeer getPopupMenu(IPopupMenuListener listener, IMenu menu) {
		return new PopupMenu(this, listener, menu);
}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getImgWidth()
	 */
	public int getImgWidth() { return getClientArea().width; }
	
	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getImgHeight()
	 */
	public int getImgHeight() { return getClientArea().height; }

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#setVertOffset(int)
	 */
	public void setVertOffset(int val) {
		if (val < 0)
			val = 0;
		verticalScrollBar.setSelection(val);
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
		//horizontalScrollBar.setSelection(val);
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
		verticalScrollBar.setSelection(vert_offset+val);
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
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent evt) {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyPressed(KeyEvent evt) {
		if (ui_view instanceof IEditor && ((IEditor)ui_view).isInTextEditMode()) {
			keyPressedForEditor(evt);
			return;
		}
		boolean consume = ui_view.inputEvent(new InputEventInfo(evt));
		if (consume) {
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

	private void keyPressedForEditor(KeyEvent evt) {
		IEditor editor = (IEditor)ui_view;
		int code = evt.keyCode;
		if ((evt.stateMask & SWT.CTRL) != 0) {
			if (code == ' ') {
				//showAutoComplete(true);
				return;
			}
			if (code == '.') {
				editor.editTypeChar('·');
				return;
			}
		}
		if ((evt.keyCode & (SWT.CTRL | SWT.SHIFT | SWT.ALT)) != 0)
			return;
		switch (code) {
		case SWT.ARROW_DOWN:
			if (in_combo) {
				int count = combo.list.getItemCount();
				if (count == 0) {
					in_combo = false;
					break;
				}
				int idx = combo.list.getSelectionIndex();
				idx++;
				if (idx >= count)
					idx = 0;
				combo.list.select(idx);
				break;
			}
			else if (combo != null && combo.list.getItemCount() > 0) {
				in_combo = true;
				if (combo.list.getSelectionIndex() < 0)
					combo.list.select(0);
			}
			break;
		case SWT.ARROW_UP:
			if (in_combo) {
				int count = combo.list.getItemCount();
				if (count == 0) {
					in_combo = false;
					break;
				}
				int idx = combo.list.getSelectionIndex();
				idx--;
				if (idx < 0)
					idx = count-1;
				combo.list.select(idx);
				break;
			}
			else if (combo != null && combo.list.getItemCount() > 0) {
				in_combo = true;
				if (combo.list.getSelectionIndex() < 0)
					combo.list.select(combo.list.getItemCount()-1);
			}
			break;
		case SWT.DEL:
			editor.editTypeChar((char)127);
			return;
		case SWT.BS:
			editor.editTypeChar((char)8);
			return;
		default:
			if ((evt.stateMask & ~SWT.SHIFT) != 0) return;
			if (code == SWT.NONE) return;
			if (code < 32 || code == 127) return;
			editor.editTypeChar((char)code);
			return;
		case SWT.CR:
			if (in_combo) {
				in_combo = false;
				int idx = combo.list.getSelectionIndex();
				if (idx >= 0) {
					editor.editSetItem(combo.list.getItem(idx));
				}
				combo.shell.setVisible(false);
				break;
			} else {
				editor.stopTextEditMode();
				if (combo != null && Helper.okToUse(combo.shell))
					combo.shell.setVisible(false);
				caret.setVisible(false);
				return;
			}
		case SWT.ESC:
			if (in_combo) {
				in_combo = false;
				if (Helper.okToUse(combo.shell)){
					combo.list.select(-1);
					combo.shell.setVisible(false);
				}				
				break;
			} else {
				editor.stopTextEditMode();
				if (combo != null && Helper.okToUse(combo.shell))
					combo.shell.setVisible(false);
				caret.setVisible(false);
				return;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ControlListener#controlResized(org.eclipse.swt.events.ControlEvent)
	 */
	public void controlResized(ControlEvent e) {
		verticalScrollBar.setPageIncrement(getClientArea().height-gfx.textHeight());
//		this.ui_view.formatAndPaint(true);
	}

		
	/**
	 * Paint in the given graphics context.
	 * @param gc the graphical context
	 */
	private void paint(GC gc) {
		gc.setClipping(new Rectangle(0, 0, getClientArea().width, getClientArea().height));
		gc.setBackground(swtColorWhite);
		gc.fillRectangle(0, 0, getClientArea().width, getClientArea().height);
		if (dlb_root != null) {
			first_visible = null;
			last_visible = null;
			downIncrement = 0;
			upIncrement = 0;
			tr.translate(-horiz_offset, -vert_offset);
			gc.setTransform(tr);
			paint(gc, dlb_root);
			tr.translate(horiz_offset, vert_offset);
			gc.setTransform(tr);
			int total_height = 0;
			total_height = dlb_root.getBounds().height;
			if (verticalScrollBar.getMaximum() != total_height+gfx.textHeight()) {
				verticalScrollBar.setMaximum(total_height+gfx.textHeight());
				verticalScrollBar.setPageIncrement(getClientArea().height-gfx.textHeight());
				verticalScrollBar.setIncrement(downIncrement);
				verticalScrollBar.setThumb(getClientArea().height);
			}
		}
	}	

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#isDoubleBuffered()
	 */
	public boolean isDoubleBuffered() {
		return false;
	}

	/**
	 * Paint in the given graphics context the <code>DrawLayoutInfo</code>
	 * @param gc the graphics context
	 * @param n the drawable
	 */
	private void paint(GC gc, DrawLayoutInfo n) {
		if (n == null)
			return;
		if (n.getDrawable() instanceof DrawTerm) {
			paintLeaf(gc, n);
		} else {
			for (DrawLayoutInfo dlb: n.getBlocks()) {
				paint(gc, dlb);
			}			
			if (false) {
				java.awt.Rectangle r = n.getBounds();
				if (r != null && new java.awt.Rectangle(horiz_offset, vert_offset, getImgWidth(), getImgHeight()).contains(r)) {
					gc.setForeground(swtColorBlack);
					gc.drawRectangle(r.x, r.y, r.width, r.height);
				}
			}

		}
	}

	/**
	 * Paint in the given graphics context the <code>DrawTermLayoutInfo</code>.
	 * @param gc the graphics context.
	 * @param dtli the drawable leaf.
	 */
	private void paintLeaf(GC gc, DrawLayoutInfo dtli) {
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
		
		if (y + h - vert_offset >= getImgHeight())
			return;

		if (last_visible != null && downIncrement == 0 && dtli.getY() > last_visible.getY())
			downIncrement = dtli.getY() - last_visible.getY();
					
		if (last_visible != null && penultimate_visible != null && last_visible.getY() > penultimate_visible.getY())
			upIncrement = last_visible.getY() - penultimate_visible.getY();
			
		// remember last visible 
		penultimate_visible = last_visible;
		last_visible = dtli;
		
		boolean set_white = false;
		if (cursor_offset < 0) {
			if (NodeUtil.isA(leaf.drnode, current_node)) {
				gc.setBackground(swtColorYellow);
				if (prev_visible != null && prev_visible.getY() == y)
					gc.fillRectangle(prev_visible.getX()+prev_visible.width, prev_visible.getY(), x-(prev_visible.getX()+prev_visible.width), h);
				if (w > 0)
					gc.fillRectangle(x, y, w, h);
				prev_visible = dtli;
			} else {
				prev_visible = null;
			}
			if (leaf == current) {
				gc.setBackground(swtColorBlack);
				set_white = true;
				if (w > 0)
					gc.fillRectangle(x, y, w, h);
				else
					gc.fillRectangle(x-1, y, 2, h);
			}
		}

		Draw_Style style = dtli.style;
		if (set_white)
			gc.setForeground(swtColorWhite);
		else if (leaf.drnode instanceof ASTNode && ((ASTNode)leaf.drnode).isAutoGenerated())
			gc.setForeground(autoGenTextColor);
		else if (style != null && style.color != null) {
			ColorDecoder cd = new ColorDecoder(style.color.rgb_color);
			RGB rgb = cd.getRGB();
			Color c = new Color(gc.getDevice(), rgb);
			gc.setForeground(c);
			c.dispose();
		}
		else
			gc.setForeground(defaultTextColor);

		Font font;
		if (style != null)
			font = SWTGraphics2D.decodeFont(gc.getDevice(), style.font);
		else
			font = SWTGraphics2D.decodeFont(gc.getDevice(), null);
		gc.setFont(font);
		TextStyle tstyle = new TextStyle(font, null, null);		
		Object term_obj = leaf.getTermObj();
		if (term_obj instanceof Draw_Icon) {
			Draw_Icon di = (Draw_Icon)term_obj;
			Image img = SWTGraphics2D.decodeImage(gc.getDevice(), di);
			gc.drawImage(img, x, y);
		} else if (leaf == current && cursor_offset >= 0) {
			String s;
			if (term_obj == null || term_obj == DrawTerm.NULL_NODE || term_obj == DrawTerm.NULL_VALUE) {
				s = " ";
			} else {
				s = String.valueOf(term_obj);
			}
			if (s == null || s.length() == 0)
				s = " ";
			// draw text here
			tl.setText(s);
			tl.setStyle(tstyle, 0, s.length());
			tl.setAscent(b);
			tl.draw(gc, x, y);
			// set caret position 
			caret = getCaret();
			if (caret != null)
				caret.setBounds (x+w-horiz_offset, y-vert_offset, 2, h);		
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
			// draw text here
			tl.setText(s);
			tl.setStyle(tstyle, 0, s.length());
			tl.setAscent(b);
			tl.draw(gc, x, y);
		}
	}
	
	/**
	 * Returns the vertical scroll bar.
	 * @return the <code>verticalScrollBar</code>
	 */
	public ScrollBar getVerticalScrollBar() {
		return verticalScrollBar;
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
	 * @see kiev.gui.ICanvas#setCurrent(kiev.fmt.DrawTerm, kiev.vtree.ANode)
	 */
	public void setCurrent(DrawTerm current, INode current_node) {
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
	 * @see kiev.gui.ICanvas#repaint()
	 */
	public void repaint() {
		redraw();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#requestFocus()
	 */
	public void requestFocus() {
		setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent e) {
		ui_view.inputEvent(new InputEventInfo(e));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseUp(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
		ScrollBar sb = (ScrollBar)e.getSource();
		if (sb == verticalScrollBar) {
			vert_offset = sb.getSelection();
			repaint();				
			switch (e.detail){
			case SWT.ARROW_DOWN:
				sb.setIncrement(downIncrement);
				break;
			case SWT.ARROW_UP:
				sb.setIncrement(upIncrement);
				break;
			case SWT.PAGE_DOWN:
				sb.setIncrement(downIncrement);
					break;
			case SWT.PAGE_UP:
				sb.setIncrement(upIncrement);
				break;
			case SWT.END:
				sb.setIncrement(upIncrement);
				break;
			case SWT.HOME:
				sb.setIncrement(downIncrement);
				break;
			case SWT.DRAG:
				break;
			}		
		}
	}

	/* (non-Javadoc)
	 * @see kiev.gui.ICanvas#getFmtGraphics()
	 */
	public IFmtGfx getFmtGraphics() { 
		if (gfx == null) gfx = new SWTGraphics2D(gc);
		return gfx;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ControlListener#controlMoved(org.eclipse.swt.events.ControlEvent)
	 */
	public void controlMoved(ControlEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose(){
		tr.dispose();
		tl.dispose();
		gc.dispose();
		selectedNodeColor.dispose();
		defaultTextFont.dispose();
		super.dispose();
	}

	public void setPopupComboContent(AutoCompleteResult autocomplete_result, boolean qualified) {
		DrawLayoutInfo info = current.getGfxFmtInfo();
		final int x = info.getX();
		final int y = info.getY() - vert_offset;
		final int w = info.width;
		final int h = info.height;
		if (combo == null || ! Helper.okToUse(combo.shell)) {			
			combo = new PopupList(this.getShell());
			combo.list.addKeyListener(this);
			combo.list.addMouseListener(this);
			this.addMouseListener(new MouseListener() {
				public void mouseDoubleClick(MouseEvent e) {}
				public void mouseDown(MouseEvent e) {}
				public void mouseUp(MouseEvent e){
					if (! Helper.okToUse(combo.shell)) return;
					Rectangle shellSize = combo.shell.getClientArea();
					if ((e.x < shellSize.x || e.x > shellSize.x + shellSize.width) &&
							(e.y < shellSize.y || e.y > shellSize.y + shellSize.height)){
						in_combo = false;
						combo.shell.setVisible (false);
					}						
				}
			});
		} else {
			if (! Helper.okToUse(combo.shell)) return;
			combo.list.removeAll();
		}
		boolean popup = false;
		for (AutoCompleteOption opt: autocomplete_result.getOptions()) {
			if (qualified && opt.data instanceof Symbol)
				combo.list.add(((Symbol)opt.data).qname());
			else
				combo.list.add(opt.text);
			popup = true;
		}
		if (popup) {
			if (! in_combo)
				combo.list.select(-1);
			this.getDisplay().asyncExec(new Runnable(){
				public void run() {
					if (! Helper.okToUse(combo.shell)) return;
					Point loc = Canvas.this.getDisplay().map(Canvas.this, null, new Point(x, y));
					Point listSize = combo.list.computeSize (SWT.DEFAULT, SWT.DEFAULT, false);
					combo.open(new Rectangle(loc.x+w, loc.y, listSize.x+4, h));					
				}				
			});			
		} else {
			in_combo = false;
		}
	}

}