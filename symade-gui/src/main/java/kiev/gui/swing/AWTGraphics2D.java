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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.font.TextLayout;
import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.swing.ImageIcon;

import kiev.fmt.common.Draw_Color;
import kiev.fmt.common.Draw_Font;
import kiev.fmt.common.Draw_Icon;
import kiev.fmt.common.IFmtGfx;

/**
 * AWT Graphics 2D.
 */
@SuppressWarnings("serial")
public final class AWTGraphics2D implements IFmtGfx {
	
	/**
	 * font cache
	 */
	private static final HashMap<String, Font> fontMap = new HashMap<String, Font>();
	
	/**
	 * color cache
	 */
	private static final HashMap<Integer, Color> colorMap = new HashMap<Integer, Color>();
	
	/**
	 * image cache
	 */
	private static final HashMap<String, Image> imageMap = new HashMap<String, Image>();
	
	/**
	 * image component
	 */
	private static final Component img_component = new Component() {};
		
	/**
	 * default Font.
	 */
	private static final Font defaultFont = new Font("Dialog", Font.PLAIN, 12);
	
	/**
	 * last Font.
	 */
	private static Font lastFont;
	
	/**
	 * last Font Name.
	 */
	private static String lastFontName;
	
	/**
	 * default Color.
	 */
	private static final Color defaultColor = Color.black;
	
	/**
	 * default Image.
	 */
	private static final Image	defaultImage;
	
	
	/*
	 * initializer.
	 */
	static {
		defaultImage = Toolkit.getDefaultToolkit().getImage("stx-fmt/fail.png");
		MediaTracker mt = new MediaTracker(img_component);
		mt.addImage(defaultImage, 1);
		try { mt.waitForAll(); } catch (InterruptedException e) {}
	}
	
	/**
	 * last Image.
	 */
	private static Image lastImage;
	
	/**
	 * last Image Name.
	 */
	private static String lastImageName;

	/**
	 * AWT graphics.
	 */
	private final Graphics2D gfx;
	
	/**
	 * text Width.
	 */
	private int textWidth;
	
	/**
	 * text Height.
	 */
	private int textHeight;
	
	/**
	 * text Base line.
	 */
	private int textBaseline;
	
	/**
	 * Returns the font from draw font element. Caches it.
	 * @param font the draw font element
	 * @return Font
	 */
	static Font decodeFont(Draw_Font font) {
		if (font == null)
			return defaultFont;
		if (font.font_object != null)
			if (font.font_object instanceof Font)
				return (Font)font.font_object;
		String name = font.font_name;
		if (name == null || name == "") {
			font.font_object = defaultFont;
			return defaultFont;
		}
		if (name == lastFontName) {
			font.font_object = lastFont;
			return lastFont;
		}
		lastFontName = name;
		lastFont = fontMap.get(name);
		if (lastFont != null) {
			font.font_object = lastFont;
			return lastFont;
		}
		lastFont = Font.decode(name);
		font.font_object = lastFont;
		fontMap.put(lastFontName, lastFont);
		return lastFont;
	}
	
	/**
	 * Returns the font from draw font element. Caches it.
	 * @param font the draw font element
	 * @return Font
	 */
	static Color decodeColor(Draw_Color color) {
		if (color == null)
			return defaultColor;
		if (color.color_object != null)
			return (Color)color.color_object;
		Color c = colorMap.get(color.rgb_color);
		if (c != null) {
			color.color_object = c;
			return c;
		}
		c = new Color(color.rgb_color);
		color.color_object = c;
		colorMap.put(color.rgb_color, c);
		return c;
	}
	
	/**
	 * Returns the image from the draw icon element. Caches it.
	 * @param img the draw icon element 
	 * @return Image
	 */
	static Image decodeImage(Draw_Icon img) {
		if (img == null)
			return defaultImage;
		if (img.icon_object != null)
			return (Image)img.icon_object;
		String name = img.icon_name;
		if (name == null || name == "") {
			img.icon_object = defaultImage;
			return defaultImage;
		}
		if (name == lastImageName) {
			img.icon_object = lastImage;
			return lastImage;
		}
		lastImageName = name;
		lastImage = imageMap.get(name);
		if (lastImage != null) {
			img.icon_object = lastImage;
			return lastImage;
		}
		lastImage = Toolkit.getDefaultToolkit().getImage(name);
		MediaTracker mt = new MediaTracker(img_component);
		mt.addImage(lastImage, 1);
		try { 
			mt.waitForAll(); 
			if (mt.isErrorAny()){
				try {
					lastImage = new ImageIcon(new File(name).toURI().toURL()).getImage();
				} catch (MalformedURLException e){
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {}
		img.icon_object = lastImage;
		imageMap.put(lastImageName, lastImage);
		return lastImage;
	}
	
	/**
	 * The constructor.
	 * @param gfx the graphics
	 */
	public AWTGraphics2D(Graphics2D gfx) {
		this.gfx = gfx;
	}

	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#getNative()
	 */
	public Graphics2D getNative() { return gfx; }
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#layoutIcon(kiev.fmt.Draw_Icon)
	 */
	public void layoutIcon(Draw_Icon image) {
		if (gfx == null) return;
		Image img = decodeImage(image);
		this.textWidth = img.getWidth(null);
		this.textHeight = img.getHeight(null);
		this.textBaseline = 0;
	}
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#layoutText(java.lang.String, kiev.fmt.Draw_Font)
	 */
	public void layoutText(String text, Draw_Font font) {
		if (gfx == null) return;
		TextLayout tl = new TextLayout(text, decodeFont(font), gfx.getFontRenderContext());
		this.textWidth = (int)Math.ceil(tl.getAdvance());
		this.textHeight = (int)Math.ceil(tl.getAscent()+tl.getDescent()+tl.getLeading());
		this.textBaseline = (int)Math.ceil(tl.getAscent()+tl.getLeading());
	}
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textWidth()
	 */
	public int  textWidth() { return textWidth; }
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textHeight()
	 */
	public int  textHeight() { return textHeight; }
	
	/* (non-Javadoc)
	 * @see kiev.fmt.IFmtGfx#textBaseline()
	 */
	public int  textBaseline() { return textBaseline; }

}
