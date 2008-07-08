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
import java.awt.font.TextLayout;
import java.awt.image.VolatileImage;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import kiev.fmt.DrawLayoutInfo;
import kiev.fmt.DrawTerm;
import kiev.vtree.ANode;
import kiev.vtree.ASTNode;

public class Canvas extends JPanel implements kiev.gui.Canvas {
	private static final long serialVersionUID = 4713633504436057499L;
	static final Color defaultTextColor = Color.BLACK;
	static final Color autoGenTextColor = Color.GRAY;
	static final Color selectedNodeColor = new Color(224,224,224);
	static final Font defaultTextFont = new Font("Dialog", Font.PLAIN, 12);
	
	private JScrollBar verticalScrollBar;
	public int imgWidth;
	int					imgHeight;
	
	private DrawLayoutInfo dlb_root;
	public DrawTerm	current;
	public ANode current_node;
	public int first_line;
	public int num_lines;
	public int cursor_offset = -1;

	transient VolatileImage vImg;
	
	int			lineno;
	boolean		translated;
	public DrawTerm	first_visible;
	public DrawTerm	last_visible;
	public int			translated_y;
	int			drawed_x;
	int			drawed_y;
	int			bg_drawed_x;
	int			bg_drawed_y;
	boolean		selected;
	
	public Canvas() {
		super(null,false);
		this.setFocusable(true);
		this.verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
		this.verticalScrollBar.addAdjustmentListener(this);
		this.add(this.verticalScrollBar);
		this.imgWidth = 100;
		this.imgHeight = 100;
	}
	
	public void setBounds(int x, int y, int width, int height) {
		int pw = verticalScrollBar.getPreferredSize().width;
		imgWidth = width - pw;
		imgHeight = height;
		verticalScrollBar.setBounds(imgWidth,0,pw,height);
		super.setBounds(x, y, width, height);
	}
	
	public void setFirstLine(int val) {
		verticalScrollBar.setValue(val);
	}
	
	public void incrFirstLine(int val) {
		verticalScrollBar.setValue(first_line+val);
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
		if (n.getDrawable() instanceof DrawTerm) {
			DrawTerm dt = (DrawTerm)n.getDrawable();
			if (dt.getLineNo() < first_line)
				return null;
			int w = dt.getWidth();
			int h = dt.getHeight();
			return new Rectangle(dt.getX(), dt.getY(), w, h);
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
		if (n.getDrawable() instanceof DrawTerm) {
			paintLeaf(g, (DrawTerm)n.getDrawable());
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


	private void paintLeaf(Graphics2D g, DrawTerm leaf) {
		if (leaf == null || leaf.isUnvisible())
			return;
		if (lineno < first_line) {
			if (leaf.get$do_newline())
				lineno++;
			return;
		}
		if (leaf.get$do_newline())
			lineno++;
		if (first_visible == null)
			first_visible = leaf;
		
		int x = leaf.getX();
		int y = leaf.getY();
		int w = leaf.getWidth();
		int h = leaf.getHeight();
		int b = leaf.getBaseline();

		if (!translated) {
			translated_y = y;
			g.translate(0, -y);
			translated = true;
		}
		if (y + h - translated_y >= getHeight())
			return;
		
		last_visible = leaf;
		
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
		
		Color color = leaf.syntax.lout.color;
		Font  font  = leaf.syntax.lout.font;
		if (set_white)
			g.setColor(Color.WHITE);
		else if (leaf.get$drnode() instanceof ASTNode && ((ASTNode)leaf.get$drnode()).isAutoGenerated())
			g.setColor(autoGenTextColor);
		else
			g.setColor(color);
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
	
}

