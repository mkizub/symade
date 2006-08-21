package kiev.gui;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
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
	static final Font defaultTextFont = new Font("Dialog", Font.PLAIN, 12);
	
	JScrollBar	verticalScrollBar;
	int			imgWidth;
	int			imgHeight;
	
	Drawable	root;
	DrawTerm	current;
	int			first_line;
	int			num_lines;
	int			cursor_offset = -1;

	transient VolatileImage vImg;
	
	int			lineno;
	boolean		translated;
	DrawTerm	first_visible;
	DrawTerm	last_visible;
	int			translated_y;
	int			drawed_x;
	int			drawed_y;
	boolean		selected;
//	boolean		is_editable;
	
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
		this.root = root;
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
			if (root != null) {
				lineno = 1;
				translated = false;
				first_visible = null;
				last_visible = null;
				translated_y = 0;
				selected = false;
				//is_editable = true;
				paint(g, root);
				num_lines = lineno;
				if (verticalScrollBar.getMaximum() != num_lines) {
					verticalScrollBar.setMaximum(num_lines);
					verticalScrollBar.setVisibleAmount(last_visible.lineno-first_visible.lineno);
				}
			}
		    g.dispose();
		} while (vImg.contentsLost());
	}	
	
	public boolean isDoubleBuffered() {
		return true;
	}
	
	private void paint(Graphics2D g, Drawable n) {
		if (n == null || n.isUnvisible())
			return;
		if (n instanceof DrawNonTerm) {
			foreach(Drawable dr; n.args; !dr.isUnvisible()) {
				if (dr instanceof DrawNonTerm) {
					if (dr == current) {
						selected = true;
						paint(g, dr);
						selected = false;
					} else {
						paint(g, dr);
					}
				} else {
					paint(g, dr);
				}
			}
		}
		else if (n instanceof DrawCtrl)
			paint(g, n.arg);
		else
			paintLeaf(g, (DrawTerm)n);
	}

	private void paintLeaf(Graphics2D g, DrawTerm leaf) {
		if (leaf == null || leaf.isUnvisible())
			return;
		if (lineno < first_line) {
			if (leaf.do_newline > 0)
				lineno++;
			return;
		}
		if (leaf.do_newline > 0)
			lineno++;
		if (first_visible == null)
			first_visible = leaf;
		
		int x = leaf.x;
		int y = leaf.y;
		int w = leaf.w;
		int h = leaf.h;
		int b = leaf.b;

		if (!translated) {
			translated_y = y;
			g.translate(0, -y);
			translated = true;
		}
		if (y + h - translated_y >= getHeight())
			return;
		
		last_visible = leaf;
		
//		if (is_editable && drawed_x < x && drawed_y == y) {
//			g.setColor(Color.LIGHT_GRAY);
//			g.fillRect(drawed_x, y, x-drawed_x, h);
//		}

		boolean set_white = false;
		if ((selected || leaf == current) && cursor_offset < 0) {
			g.setColor(Color.BLACK);
			g.fillRect(x, y, w, h);
			set_white = true;
		}
//		else if (is_editable) {
//			g.setColor(Color.LIGHT_GRAY);
//			g.fillRect(x, y, w, h);
//		}
		drawed_x = x + w;
		drawed_y = y;
		
//		if (leaf instanceof DrawSpace)
//			return;
		
		Color color = leaf.syntax.lout.color;
		Font  font  = leaf.syntax.lout.font;
		if (set_white)
			g.setColor(Color.WHITE);
		else if (leaf.node instanceof ASTNode && ((ASTNode)leaf.node).isAutoGenerated())
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
			Shape[] carets = tl.getCaretShapes(cursor_offset);
			g.setColor(Color.RED);
			g.draw(carets[0]);
			if (carets[1] != null) {
				g.setColor(Color.BLACK);
				g.draw(carets[1]);
			}
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
	
}

