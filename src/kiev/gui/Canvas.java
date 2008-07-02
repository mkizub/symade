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

import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.image.VolatileImage;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

/**
 * @author mkizub
 */
public class Canvas extends JPanel implements DrawDevice, AdjustmentListener {

	static final Color defaultTextColor = Color.BLACK;
	static final Color autoGenTextColor = Color.GRAY;
	static final Color selectedNodeColor = new Color(224,224,224);
	static final Font defaultTextFont = new Font("Dialog", Font.PLAIN, 12);
	
	JScrollBar			verticalScrollBar;
	int					imgWidth;
	int					imgHeight;
	
	Drawable			dr_root;
	DrawLayoutBlock		dlb_root;
	DrawTerm			current;
	ANode				current_node;
	int					first_line;
	int					num_lines;
	int					cursor_offset = -1;

	transient VolatileImage vImg;
	
	int			lineno;
	boolean		translated;
	DrawTerm	first_visible;
	DrawTerm	last_visible;
	int			translated_y;
	int			drawed_x;
	int			drawed_y;
	int			bg_drawed_x;
	int			bg_drawed_y;
	boolean		selected;
	
	Canvas() {
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
	
	public void draw(Drawable root) {
		this.dr_root = root;
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
			if (dlb_root != null || dr_root != null) {
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
				if (dlb_root != null)
					paint(g, dlb_root);
				else
					paint(g, dr_root);
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
	
	private Rectangle calcBounds(DrawLayoutBlock n) {
		if (n.getDrawable() instanceof DrawTerm) {
			DrawTerm dt = (DrawTerm)n.getDrawable();
			if (dt.getLineNo() < first_line)
				return null;
			int w = dt.getWidth();
			int h = dt.getHeight();
			return new Rectangle(dt.getX(), dt.getY(), w, h);
		} else {
			Rectangle res = null;
			for (DrawLayoutBlock dlb: n.getBlocks()) {
				Rectangle r = calcBounds(dlb);
				if (res == null)
					res = r;
				else if (r != null)
					res = res.union(r);
			}
			return res;
		}
	}

	private void paint(Graphics2D g, DrawLayoutBlock n) {
		if (n == null)
			return;
		if (n.getDrawable() instanceof DrawTerm) {
			paintLeaf(g, (DrawTerm)n.getDrawable());
		} else {
			for (DrawLayoutBlock dlb: n.getBlocks()) {
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


	private void paint(Graphics2D g, Drawable n) {
		if (n == null || n.isUnvisible())
			return;
		if (n instanceof DrawNonTerm) {
			for(Drawable dr: ((DrawNonTerm)n).getArgs()) 
				if(!dr.isUnvisible()) {
					if (dr instanceof DrawNonTerm && current_node == dr.get$drnode()) {
						int lineno = this.lineno;
						paintBg(g, dr);
						this.lineno = lineno;
					}
					paint(g, dr);
			}
		}
		else if (n instanceof DrawCtrl)
			paint(g, ((DrawCtrl)n).getArg());
		else
			paintLeaf(g, (DrawTerm)n);
	}

	private void paintBg(Graphics2D g, Drawable n) {
		if (n == null || n.isUnvisible())
			return;
		if (n instanceof DrawNonTerm) {
			for (Drawable dr: ((DrawNonTerm)n).getArgs()) 
				if (!dr.isUnvisible())
					paintBg(g, dr);
		}
		else if (n instanceof DrawCtrl)
			paintBg(g, ((DrawCtrl)n).getArg());
		else
			paintLeafBg(g, (DrawTerm)n);
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
	
	private void paintLeafBg(Graphics2D g, DrawTerm leaf) {
		if (leaf == null || leaf.isUnvisible())
			return;
		if (lineno < first_line) {
			if (leaf.get$do_newline())
				lineno++;
			return;
		}
		if (leaf.get$do_newline())
			lineno++;
		
		int x = leaf.getX();
		int y = leaf.getY();
		int w = leaf.getWidth();
		int h = leaf.getHeight();

		if (!translated) {
			translated_y = y;
			g.translate(0, -y);
			translated = true;
		}
		if (y + h - translated_y >= getHeight())
			return;
		
		g.setColor(selectedNodeColor);

		if (bg_drawed_x < x && bg_drawed_y == y)
			g.fillRect(bg_drawed_x, y, x-bg_drawed_x+w, h);
		else
			g.fillRect(x, y, w, h);

		bg_drawed_x = x + w;
		bg_drawed_y = y;
		
	}
	
}

